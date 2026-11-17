package com.creovid.app.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.creovid.app.engine.GradeParams
import com.creovid.app.engine.Wheel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/** Home screen ka ek "Recent Project": video ka link + uski grading settings. */
data class Project(
    val id: String,
    val title: String,
    val videoUri: String,
    val durationMs: Long,
    val updatedAt: Long,
    val thumbPath: String?,
    val params: GradeParams,
    val presetName: String,
    val lutPath: String?,
    val lutName: String?,
)

class VideoMeta(val durationMs: Long, val thumbPath: String?)

/** Projects ko app ki apni storage me JSON file + thumbnail images ki tarah rakhta hai. */
class ProjectStore(private val context: Context) {

    private val file = File(context.filesDir, "projects.json")
    private val thumbDir = File(context.filesDir, "thumbs")
    private val lutDir = File(context.filesDir, "luts")

    init {
        thumbDir.mkdirs()
        lutDir.mkdirs()
    }

    fun load(): List<Project> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length())
                .mapNotNull { i -> runCatching { arr.getJSONObject(i).toProject() }.getOrNull() }
                .sortedByDescending { it.updatedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<Project>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        val text = arr.toString()
        val tmp = File(file.parentFile, "projects.json.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.writeText(text)
            tmp.delete()
        }
    }

    /** Video ka duration aur ek chhota thumbnail (1 second ke aas-paas ka frame). */
    fun makeMeta(uri: Uri, id: String): VideoMeta {
        val r = MediaMetadataRetriever()
        var duration = 0L
        try {
            r.setDataSource(context, uri)
            duration = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val atUs = min(1_000_000L, duration * 1000L / 2)
            val frame = r.getFrameAtTime(atUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(0)
                ?: return VideoMeta(duration, null)
            val k = 480f / max(frame.width, frame.height)
            val bmp = if (k < 1f) {
                Bitmap.createScaledBitmap(
                    frame,
                    (frame.width * k).toInt().coerceAtLeast(1),
                    (frame.height * k).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                frame
            }
            // Har baar naya naam, taaki UI purani cached image na dikhaye
            val out = File(thumbDir, "$id-${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            return VideoMeta(duration, out.absolutePath)
        } catch (e: Exception) {
            return VideoMeta(duration, null)
        } finally {
            try {
                r.release()
            } catch (_: Exception) {
            }
        }
    }

    /** Kya ye video abhi bhi khul sakti hai? (user ne hata di ho ya access chala gaya ho) */
    fun isAccessible(uri: Uri): Boolean = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    /** LUT ki copy app storage me rakhta hai, taaki project dobara kholne par wahi look mile. */
    fun copyLut(id: String, uri: Uri): String? {
        return try {
            val out = File(lutDir, "$id.cube")
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            out.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun deleteFiles(p: Project) {
        p.thumbPath?.let { File(it).delete() }
        File(lutDir, "${p.id}.cube").delete()
    }

    fun deleteFile(path: String?) {
        if (path != null) File(path).delete()
    }
}

// ---------------------------------------------------------------------
// JSON
// ---------------------------------------------------------------------

private fun Project.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("videoUri", videoUri)
    put("durationMs", durationMs)
    put("updatedAt", updatedAt)
    putOpt("thumbPath", thumbPath)
    put("params", params.toJson())
    put("presetName", presetName)
    putOpt("lutPath", lutPath)
    putOpt("lutName", lutName)
}

private fun JSONObject.optStr(key: String): String? =
    if (has(key) && !isNull(key)) getString(key) else null

private fun JSONObject.toProject(): Project = Project(
    id = getString("id"),
    title = getString("title"),
    videoUri = getString("videoUri"),
    durationMs = optLong("durationMs", 0L),
    updatedAt = optLong("updatedAt", 0L),
    thumbPath = optStr("thumbPath"),
    params = optJSONObject("params")?.toParams() ?: GradeParams(),
    presetName = optString("presetName", ""),
    lutPath = optStr("lutPath"),
    lutName = optStr("lutName"),
)

private fun Wheel.toJson(): JSONObject = JSONObject().apply {
    put("h", hue.toDouble())
    put("a", amount.toDouble())
    put("l", luma.toDouble())
}

private fun JSONObject.toWheel(): Wheel = Wheel(
    hue = optDouble("h", 0.0).toFloat(),
    amount = optDouble("a", 0.0).toFloat(),
    luma = optDouble("l", 0.0).toFloat(),
)

private fun GradeParams.toJson(): JSONObject = JSONObject().apply {
    put("exposure", exposure.toDouble())
    put("contrast", contrast.toDouble())
    put("highlights", highlights.toDouble())
    put("shadows", shadows.toDouble())
    put("saturation", saturation.toDouble())
    put("vibrance", vibrance.toDouble())
    put("temperature", temperature.toDouble())
    put("tint", tint.toDouble())
    put("fade", fade.toDouble())
    put("filmCurve", filmCurve.toDouble())
    put("lift", lift.toJson())
    put("gamma", gamma.toJson())
    put("gain", gain.toJson())
    put("lutIntensity", lutIntensity.toDouble())
    put("grain", grain.toDouble())
    put("bloom", bloom.toDouble())
    put("halation", halation.toDouble())
    put("vignette", vignette.toDouble())
    put("letterbox", letterbox.toDouble())
}

private fun JSONObject.f(key: String, def: Float): Float = optDouble(key, def.toDouble()).toFloat()

private fun JSONObject.wheel(key: String): Wheel = optJSONObject(key)?.toWheel() ?: Wheel()

private fun JSONObject.toParams(): GradeParams {
    val d = GradeParams()
    return GradeParams(
        exposure = f("exposure", d.exposure),
        contrast = f("contrast", d.contrast),
        highlights = f("highlights", d.highlights),
        shadows = f("shadows", d.shadows),
        saturation = f("saturation", d.saturation),
        vibrance = f("vibrance", d.vibrance),
        temperature = f("temperature", d.temperature),
        tint = f("tint", d.tint),
        fade = f("fade", d.fade),
        filmCurve = f("filmCurve", d.filmCurve),
        lift = wheel("lift"),
        gamma = wheel("gamma"),
        gain = wheel("gain"),
        lutIntensity = f("lutIntensity", d.lutIntensity),
        grain = f("grain", d.grain),
        bloom = f("bloom", d.bloom),
        halation = f("halation", d.halation),
        vignette = f("vignette", d.vignette),
        letterbox = f("letterbox", d.letterbox),
    )
}
