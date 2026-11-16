package com.creovid.app.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Export quality: shortSide = chhota side pixels me (0 = original). */
enum class ExportQuality(val label: String, val shortSide: Int) {
    P720("720p", 720),
    P1080("1080p", 1080),
    ORIGINAL("Original", 0),
}

@OptIn(UnstableApi::class)
object Exporter {

    /** Video ka dikhne wala size (rotation ke baad). */
    fun displaySize(context: Context, uri: Uri): Pair<Int, Int> {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            return if (rot == 90 || rot == 270) h to w else w to h
        } finally {
            r.release()
        }
    }

    /** Export karke cache file banata hai aur use gallery (Movies/Creovid) me save karke uri deta hai. */
    suspend fun export(
        context: Context,
        source: Uri,
        state: GradeState,
        quality: ExportQuality,
        onProgress: (Float) -> Unit,
    ): Uri {
        val (srcW, srcH) = withContext(Dispatchers.IO) { displaySize(context, source) }

        var outW = srcW
        var outH = srcH
        if (quality.shortSide > 0 && srcW > 0 && srcH > 0 && min(srcW, srcH) > quality.shortSide) {
            val k = quality.shortSide.toFloat() / min(srcW, srcH)
            outW = even((srcW * k).roundToInt())
            outH = even((srcH * k).roundToInt())
        }

        val effects = mutableListOf<Effect>()
        if (outW != srcW || outH != srcH) {
            effects += Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT)
        }
        effects += GradeEffect(state)

        val shortSide = if (srcW > 0 && srcH > 0) min(outW, outH) else quality.shortSide
        val bitrate = when {
            shortSide <= 720 -> 12_000_000
            shortSide <= 1080 -> 24_000_000
            else -> 40_000_000
        }

        val outFile = File(context.cacheDir, "creovid_${System.currentTimeMillis()}.mp4")
        val edited = EditedMediaItem.Builder(MediaItem.fromUri(source))
            .setEffects(Effects(emptyList(), effects))
            .build()

        val done = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setEncoderFactory(
                DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(bitrate).build(),
                    )
                    .build(),
            )
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    done.complete(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    done.completeExceptionally(exportException)
                }
            })
            .build()

        try {
            transformer.start(edited, outFile.absolutePath)
            val holder = ProgressHolder()
            while (!done.isCompleted) {
                if (transformer.getProgress(holder) != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    onProgress(holder.progress / 100f)
                }
                delay(250)
            }
            done.await()
        } catch (e: CancellationException) {
            transformer.cancel()
            outFile.delete()
            throw e
        } catch (e: Exception) {
            outFile.delete()
            throw e
        }

        onProgress(1f)
        return withContext(Dispatchers.IO) {
            try {
                saveToGallery(context, outFile)
            } finally {
                outFile.delete()
            }
        }
    }

    private fun even(v: Int): Int = max(2, v - (v % 2))

    private fun saveToGallery(context: Context, file: File): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Creovid_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Creovid")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Gallery me file nahi ban payi")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } ?: throw IllegalStateException("Gallery file khul nahi payi")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        return uri
    }
}
