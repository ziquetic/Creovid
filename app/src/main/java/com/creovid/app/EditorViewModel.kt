package com.creovid.app

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
import kotlinx.coroutines.withContext
import java.io.IOException

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
    var exportProgress by mutableStateOf<Float?>(null)
        private set
    var exportedUri by mutableStateOf<Uri?>(null)
    var message by mutableStateOf<String?>(null)

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

    // ---------- Video ----------

    fun loadVideo(uri: Uri) {
        videoUri = uri
        exportedUri = null
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    /** Editor band karke home screen par wapas (grade settings yaad rehti hain). */
    fun closeVideo() {
        if (exportJob?.isActive == true) return
        _player?.let {
            it.stop()
            it.clearMediaItems()
        }
        videoUri = null
        exportedUri = null
    }

    // ---------- Grading ----------

    /** Slider/wheel se koi bhi value badalne ke liye. */
    fun update(block: (GradeParams) -> GradeParams) {
        params = block(params)
        presetName = ""
        push()
        refreshFrame()
    }

    fun applyPreset(p: Preset) {
        params = p.params.copy(lutIntensity = params.lutIntensity)
        presetName = p.name
        push()
        refreshFrame(force = true)
    }

    fun resetAll() {
        params = GradeParams()
        presetName = "Original"
        state.setLut(null)
        lutName = null
        push()
        refreshFrame(force = true)
    }

    fun setComparing(value: Boolean) {
        comparing = value
        push()
        refreshFrame(force = true)
    }

    private fun push() {
        state.params = if (comparing) offParams else params
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
                val name = withContext(Dispatchers.IO) { displayName(app, uri) } ?: "LUT"
                val lut = withContext(Dispatchers.IO) {
                    val stream = app.contentResolver.openInputStream(uri) ?: throw IOException("File khul nahi payi")
                    stream.use { CubeLut.parse(name, it) }
                }
                state.setLut(lut)
                lutName = name
                params = params.copy(lutIntensity = 1f)
                presetName = ""
                push()
                refreshFrame(force = true)
            } catch (e: Exception) {
                message = "LUT load nahi hui: ${e.message}"
            }
        }
    }

    fun clearLut() {
        state.setLut(null)
        lutName = null
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

    fun cancelExport() {
        exportJob?.cancel()
    }

    override fun onCleared() {
        exportJob?.cancel()
        _player?.release()
        _player = null
    }
}
