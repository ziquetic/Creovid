package com.creovid.app

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.creovid.app.data.Project
import com.creovid.app.data.ProjectStore
import com.creovid.app.engine.CubeLut
import com.creovid.app.engine.Exporter
import com.creovid.app.engine.ExportQuality
import com.creovid.app.engine.GradeEffect
import com.creovid.app.engine.GradeParams
import com.creovid.app.engine.GradeState
import com.creovid.app.engine.Preset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

@OptIn(UnstableApi::class)
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    /** Preview aur export dono isi ko padhte hain. */
    val state = GradeState()

    var params by mutableStateOf(GradeParams())
        private set
    var videoUri by mutableStateOf<Uri?>(null)
        private set
    var lutName by mutableStateOf<String?>(null)
        private set
    var presetName by mutableStateOf("Original")
        private set
    var comparing by mutableStateOf(false)
        private set
    /** Timeline ki "eye" button: true ho to preview me grading band (original dikhta hai). */
    var gradeHidden by mutableStateOf(false)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    /** Har successful LUT import par badhta hai (UI Filters panel kholne ke liye). */
    var lutImportCount by mutableIntStateOf(0)
        private set
    var exportProgress by mutableStateOf<Float?>(null)
        private set
    var exportedUri by mutableStateOf<Uri?>(null)
    var message by mutableStateOf<String?>(null)

    /** Home screen ke Recent Projects (sabse naya sabse upar). */
    val projects = mutableStateListOf<Project>()

    val currentTitle: String
        get() = projects.firstOrNull { it.id == currentId }?.title ?: ""

    val currentThumbPath: String?
        get() = projects.firstOrNull { it.id == currentId }?.thumbPath

    private val store = ProjectStore(app)
    private val saveMutex = Mutex()
    private var currentId: String? = null
    private var currentLutPath: String? = null

    // ---------- Undo / Redo ----------

    private class Snapshot(
        val params: GradeParams,
        val presetName: String,
        val lut: CubeLut?,
        val lutName: String?,
        val lutPath: String?,
    ) {
        fun sameAs(o: Snapshot): Boolean =
            params == o.params && presetName == o.presetName &&
                lut === o.lut && lutName == o.lutName && lutPath == o.lutPath
    }

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var baseline = Snapshot(GradeParams(), "Original", null, null, null)
    private var dirty = false

    private val offParams = GradeParams(lutIntensity = 0f)
    private var exportJob: Job? = null
    private var lastRefresh = 0L
    private var _player: ExoPlayer? = null

    val player: ExoPlayer
        get() = _player ?: ExoPlayer.Builder(getApplication<Application>()).build().also {
            it.repeatMode = Player.REPEAT_MODE_ONE
            it.setVideoEffects(listOf<Effect>(GradeEffect(state)))
            _player = it
        }

    init {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { store.load() }
            // Agar isi beech user ne naya project bana liya ho to use mat mitao
            val known = projects.map { it.id }.toSet()
            projects.addAll(saved.filter { it.id !in known })
            projects.sortByDescending { it.updatedAt }
        }
    }

    // ---------- Video ----------

    /** Naya video chuna gaya: naya project banta hai, grading zero se shuru. */
    fun loadVideo(uri: Uri) {
        val id = UUID.randomUUID().toString()
        currentId = id
        currentLutPath = null
        params = GradeParams()
        presetName = "Original"
        comparing = false
        gradeHidden = false
        state.setLut(null)
        lutName = null
        push()
        resetHistory()

        projects.add(
            0,
            Project(
                id = id,
                title = nextTitle(),
                videoUri = uri.toString(),
                durationMs = 0L,
                updatedAt = System.currentTimeMillis(),
                thumbPath = null,
                params = params,
                presetName = presetName,
                lutPath = null,
                lutName = null,
            ),
        )
        persist()
        startPlayback(uri)

        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) { store.makeMeta(uri, id) }
            mutate(id) { it.copy(durationMs = meta.durationMs, thumbPath = meta.thumbPath) }
            persist()
        }
    }

    /** Recent Projects se purana project kholna: video + uski saari grading wapas. */
    fun openProject(p: Project) {
        viewModelScope.launch {
            val uri = Uri.parse(p.videoUri)
            val ok = withContext(Dispatchers.IO) { store.isAccessible(uri) }
            if (!ok) {
                message = "Is project ki original video nahi mil rahi. Project hata kar video dobara chuno."
                return@launch
            }
            var lut: CubeLut? = null
            val lutPath = p.lutPath
            if (lutPath != null) {
                lut = withContext(Dispatchers.IO) {
                    runCatching {
                        File(lutPath).inputStream().use { CubeLut.parse(p.lutName ?: "LUT", it) }
                    }.getOrNull()
                }
            }
            currentId = p.id
            currentLutPath = if (lut != null) lutPath else null
            state.setLut(lut)
            lutName = if (lut != null) p.lutName else null
            params = p.params
            presetName = p.presetName
            comparing = false
            gradeHidden = false
            push()
            resetHistory()
            startPlayback(uri)
        }
    }

    private fun startPlayback(uri: Uri) {
        videoUri = uri
        exportedUri = null
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    /** Editor band karke home screen par wapas. Grading project me save ho jaati hai. */
    fun closeVideo() {
        if (exportJob?.isActive == true) return
        commit()
        saveCurrent()
        _player?.let {
            it.stop()
            it.clearMediaItems()
        }
        videoUri = null
        exportedUri = null
        currentId = null
        gradeHidden = false
    }

    // ---------- Projects ----------

    /** Current grading ko project me save karta hai (sirf tab jab kuch badla ho). */
    fun saveCurrent() {
        val id = currentId ?: return
        val i = projects.indexOfFirst { it.id == id }
        if (i < 0) return
        val old = projects[i]
        if (old.params == params && old.presetName == presetName &&
            old.lutName == lutName && old.lutPath == currentLutPath
        ) return
        val updated = old.copy(
            params = params,
            presetName = presetName,
            lutName = lutName,
            lutPath = currentLutPath,
            updatedAt = System.currentTimeMillis(),
        )
        projects.removeAt(i)
        projects.add(0, updated)
        persist()
    }

    fun renameProject(id: String, title: String) {
        val t = title.trim().take(30)
        if (t.isEmpty()) return
        mutate(id) { it.copy(title = t) }
        persist()
    }

    /** Editor ke upar wale naam box se. */
    fun renameCurrent(title: String) {
        val id = currentId ?: return
        val t = title.trim().take(30)
        if (t.isEmpty() || projects.firstOrNull { it.id == id }?.title == t) return
        renameProject(id, t)
    }

    fun deleteProject(id: String) {
        val p = projects.firstOrNull { it.id == id } ?: return
        projects.remove(p)
        persist()
        viewModelScope.launch(Dispatchers.IO) { store.deleteFiles(p) }
    }

    private fun nextTitle(): String {
        val used = projects.mapNotNull { Regex("Project (\\d+)").matchEntire(it.title)?.groupValues?.get(1)?.toIntOrNull() }
        return "Project ${(used.maxOrNull() ?: 0) + 1}"
    }

    private fun mutate(id: String, block: (Project) -> Project) {
        val i = projects.indexOfFirst { it.id == id }
        if (i >= 0) projects[i] = block(projects[i])
    }

    private fun persist() {
        val snapshot = projects.toList()
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock { store.save(snapshot) }
        }
    }

    // ---------- History ----------

    private fun snapshot() = Snapshot(params, presetName, state.lut, lutName, currentLutPath)

    private fun resetHistory() {
        undoStack.clear()
        redoStack.clear()
        dirty = false
        baseline = snapshot()
        syncHistoryFlags()
    }

    private fun syncHistoryFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    /** Pending badlav ko ek undo step bana deta hai (slider/wheel chhodne par bulao). */
    fun commit() {
        if (!dirty) return
        dirty = false
        val now = snapshot()
        if (now.sameAs(baseline)) return
        undoStack.addLast(baseline)
        if (undoStack.size > 60) undoStack.removeFirst()
        redoStack.clear()
        baseline = now
        syncHistoryFlags()
    }

    /** Slider/wheel chhodne par: undo step banao aur frame refresh karo. */
    fun finishEdit() {
        commit()
        refreshFrame(force = true)
    }

    /** Preset/LUT/reset jaise ek-jhatke ke badlav: seedha ek alag undo step. */
    private fun discrete(block: () -> Unit) {
        commit()
        block()
        dirty = true
        commit()
    }

    fun undo() {
        commit()
        val prev = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(baseline)
        restore(prev)
    }

    fun redo() {
        commit()
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(baseline)
        restore(next)
    }

    private fun restore(s: Snapshot) {
        baseline = s
        params = s.params
        presetName = s.presetName
        state.setLut(s.lut)
        lutName = s.lutName
        currentLutPath = s.lutPath
        push()
        syncHistoryFlags()
        refreshFrame(force = true)
    }

    // ---------- Grading ----------

    /** Slider/wheel se koi bhi value badalne ke liye. */
    fun update(block: (GradeParams) -> GradeParams) {
        params = block(params)
        presetName = ""
        dirty = true
        push()
        refreshFrame()
    }

    fun applyPreset(p: Preset) {
        discrete {
            params = p.params.copy(lutIntensity = params.lutIntensity)
            presetName = p.name
        }
        push()
        refreshFrame(force = true)
    }

    fun resetAll() {
        discrete {
            params = GradeParams()
            presetName = "Original"
            state.setLut(null)
            lutName = null
            currentLutPath = null
        }
        push()
        refreshFrame(force = true)
    }

    fun holdOriginal(value: Boolean) {
        comparing = value
        push()
        refreshFrame(force = true)
    }

    fun toggleGradeHidden() {
        gradeHidden = !gradeHidden
        push()
        refreshFrame(force = true)
    }

    private fun push() {
        state.params = if (comparing || gradeHidden) offParams else params
    }

    /** Pause me bhi naya grade dikhane ke liye current frame dobara draw karwata hai. */
    fun refreshFrame(force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastRefresh < 70) return
        lastRefresh = now
        val p = _player ?: return
        if (!p.isPlaying && p.mediaItemCount > 0) {
            p.seekTo(p.currentPosition)
        }
    }

    // ---------- LUT ----------

    fun loadLut(uri: Uri) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val id = currentId
                val name = withContext(Dispatchers.IO) { displayName(app, uri) } ?: "LUT"
                val lut = withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openInputStream(uri) ?: throw IOException("File khul nahi payi")
                    stream.use { CubeLut.parse(name, it) }
                }
                val saved = if (id != null) withContext(Dispatchers.IO) { store.copyLut(id, uri) } else null
                discrete {
                    state.setLut(lut)
                    lutName = name
                    currentLutPath = saved
                    params = params.copy(lutIntensity = 1f)
                    presetName = ""
                }
                push()
                refreshFrame(force = true)
                lutImportCount += 1
            } catch (e: Exception) {
                message = "LUT load nahi hui: ${e.message}"
            }
        }
    }

    fun clearLut() {
        discrete {
            state.setLut(null)
            lutName = null
            currentLutPath = null
        }
        refreshFrame(force = true)
    }

    private fun displayName(app: Application, uri: Uri): String? =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    // ---------- Export ----------

    fun export(quality: ExportQuality) {
        val src = videoUri ?: return
        if (exportJob?.isActive == true) return
        player.pause()
        commit()
        saveCurrent()
        // Export hamesha graded hota hai, isliye preview ki eye wapas on
        gradeHidden = false
        push()
        val projectId = currentId
        // Export ke dauraan slider chalane se output na badle, isliye copy bana lete hain
        val snapshot = GradeState().apply {
            params = this@EditorViewModel.params
            setLut(this@EditorViewModel.state.lut)
        }
        exportProgress = 0f
        exportedUri = null
        exportJob = viewModelScope.launch {
            try {
                val out = Exporter.export(getApplication<Application>(), src, snapshot, quality) {
                    exportProgress = it
                }
                exportedUri = out
                message = "Video gallery me save ho gayi (Movies/Creovid)"
                if (projectId != null) runCatching { refreshThumbnail(projectId, out) }
            } catch (e: CancellationException) {
                message = "Export cancel ho gaya"
                throw e
            } catch (e: Exception) {
                message = "Export fail hua: ${e.message}"
            } finally {
                exportProgress = null
            }
        }
    }

    /** Export hui (graded) video ka frame project ki thumbnail bana deta hai. */
    private suspend fun refreshThumbnail(id: String, exported: Uri) {
        val meta = withContext(Dispatchers.IO) { store.makeMeta(exported, id) }
        val newThumb = meta.thumbPath ?: return
        val old = projects.firstOrNull { it.id == id }?.thumbPath
        mutate(id) { it.copy(thumbPath = newThumb) }
        persist()
        if (old != null) withContext(Dispatchers.IO) { store.deleteFile(old) }
    }

    fun cancelExport() {
        exportJob?.cancel()
    }

    override fun onCleared() {
        exportJob?.cancel()
        _player?.release()
        _player = null
    }
}
