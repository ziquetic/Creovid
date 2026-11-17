package com.creovid.app.ui

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Player ki live position (ms). Timeline aur clock isi ko padhte hain. */
@Stable
class PlayheadState {
    var posMs by mutableLongStateOf(0L)
    var durMs by mutableLongStateOf(0L)
    var playing by mutableStateOf(false)
    var scrubbing by mutableStateOf(false)
}

/** 75000 -> "1:15" */
fun clockText(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0L)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

private const val PPS_DP = 56f
private val RULER_H = 22.dp
private val CLIP_GAP = 4.dp
private val CLIP_H = 46.dp
private val ROW_GAP = 6.dp
private val GRADE_H = 32.dp
private val BOTTOM_PAD = 6.dp
private val SIDE_W = 46.dp

/**
 * Alight-Motion jaisi timeline: beech me fixed playhead, video clip (thumbnails ke saath)
 * aur uske neeche "Grade" track. Drag karo = scrub, tap karo = wahan jump.
 * Poori cheez Canvas me bani hai, isliye lambi video par bhi layout crash nahi hota.
 */
@Composable
fun Timeline(
    ph: PlayheadState,
    thumbPath: String?,
    gradeLabel: String,
    gradeHidden: Boolean,
    onToggleGrade: () -> Unit,
    onSeek: (Long) -> Unit,
    onScrubStart: () -> Unit,
    onScrubEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumb = remember(thumbPath) {
        thumbPath?.let { p -> runCatching { BitmapFactory.decodeFile(p)?.asImageBitmap() }.getOrNull() }
    }
    val paint = remember { Paint().apply { isAntiAlias = true } }
    val seek by rememberUpdatedState(onSeek)
    val scrubStart by rememberUpdatedState(onScrubStart)
    val scrubEnd by rememberUpdatedState(onScrubEnd)

    Row(
        modifier
            .fillMaxWidth()
            .height(RULER_H + CLIP_GAP + CLIP_H + ROW_GAP + GRADE_H + BOTTOM_PAD)
            .background(CV.Surface),
    ) {
        // Left side: track icons
        Column(Modifier.width(SIDE_W).fillMaxHeight()) {
            Spacer(Modifier.height(RULER_H + CLIP_GAP))
            Box(Modifier.fillMaxWidth().height(CLIP_H), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Movie, contentDescription = "Video", tint = CV.TextDim, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(ROW_GAP))
            Box(Modifier.fillMaxWidth().height(GRADE_H), contentAlignment = Alignment.Center) {
                IconButton(onClick = onToggleGrade, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (gradeHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Show or hide grade",
                        tint = if (gradeHidden) CV.TextDim else CV.Sky,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Canvas(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .pointerInput(Unit) {
                    val pxPerMs = PPS_DP.dp.toPx() / 1000f
                    detectTapGestures(onTap = { o ->
                        val d = ph.durMs
                        if (d > 0L) {
                            val target = ph.posMs + ((o.x - size.width / 2f) / pxPerMs).toLong()
                            seek(target.coerceIn(0L, d))
                        }
                    })
                }
                .pointerInput(Unit) {
                    val pxPerMs = PPS_DP.dp.toPx() / 1000f
                    detectHorizontalDragGestures(
                        onDragStart = { scrubStart() },
                        onDragEnd = { scrubEnd() },
                        onDragCancel = { scrubEnd() },
                        onHorizontalDrag = { change, dx ->
                            change.consume()
                            val d = ph.durMs
                            if (d > 0L) {
                                seek((ph.posMs - (dx / pxPerMs).toLong()).coerceIn(0L, d))
                            }
                        },
                    )
                },
        ) {
            val w = size.width
            val h = size.height
            val dn = density
            val pps = PPS_DP * dn
            val cx = w / 2f
            val durSec = ph.durMs / 1000f
            val posSec = ph.posMs / 1000f
            val x0 = cx - posSec * pps
            val x1 = x0 + durSec * pps

            val rulerH = RULER_H.toPx()
            val clipTop = rulerH + CLIP_GAP.toPx()
            val clipH = CLIP_H.toPx()
            val clipBottom = clipTop + clipH
            val gradeTop = clipBottom + ROW_GAP.toPx()
            val gradeH = GRADE_H.toPx()
            val corner = 8.dp.toPx()

            // ---------- Ruler ----------
            if (durSec > 0f) {
                val tick = CV.TextDim.copy(alpha = 0.55f)
                paint.color = CV.TextDim.toArgb()
                paint.textSize = 10.sp.toPx()
                val first = max(0, floor((-x0 - 30f * dn) / pps).toInt())
                val last = min(floor((w - x0) / pps).toInt(), floor(durSec).toInt())
                for (s in first..last) {
                    val x = x0 + s * pps
                    val major = s % 2 == 0
                    val len = if (major) 8.dp.toPx() else 4.dp.toPx()
                    drawLine(tick, Offset(x, rulerH - len), Offset(x, rulerH), strokeWidth = 1.dp.toPx())
                    if (major) {
                        drawContext.canvas.nativeCanvas.drawText(clockText(s * 1000L), x + 3.dp.toPx(), 11.dp.toPx(), paint)
                    }
                }
            }

            // ---------- Clip + Grade tracks ----------
            val l = max(x0, -40f)
            val r = min(x1, w + 40f)
            if (durSec > 0f && r > l) {
                // Video clip
                drawRoundRect(CV.Surface2, Offset(l, clipTop), Size(r - l, clipH), CornerRadius(corner))
                if (thumb != null) {
                    val path = Path().apply { addRoundRect(RoundRect(l, clipTop, r, clipBottom, corner, corner)) }
                    clipPath(path) {
                        val side = min(thumb.width, thumb.height)
                        val sx = (thumb.width - side) / 2
                        val sy = (thumb.height - side) / 2
                        val tile = clipH
                        val i0 = max(0, floor((l - x0) / tile).toInt())
                        val i1 = floor((r - x0) / tile).toInt()
                        for (i in i0..i1) {
                            drawImage(
                                image = thumb,
                                srcOffset = IntOffset(sx, sy),
                                srcSize = IntSize(side, side),
                                dstOffset = IntOffset((x0 + i * tile).roundToInt(), clipTop.roundToInt()),
                                dstSize = IntSize(tile.roundToInt() + 1, clipH.roundToInt()),
                            )
                        }
                    }
                }
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(CV.Sky, CV.Violet), startX = l, endX = r),
                    topLeft = Offset(l, clipTop),
                    size = Size(r - l, clipH),
                    cornerRadius = CornerRadius(corner),
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Grade track
                val gradeBrush: Brush = if (gradeHidden) {
                    SolidColor(CV.Outline)
                } else {
                    Brush.horizontalGradient(listOf(CV.Blue, CV.Violet, CV.Lilac), startX = l, endX = r)
                }
                drawRoundRect(
                    brush = gradeBrush,
                    topLeft = Offset(l, gradeTop),
                    size = Size(r - l, gradeH),
                    cornerRadius = CornerRadius(corner),
                )
                paint.color = (if (gradeHidden) CV.TextDim else Color.White).toArgb()
                paint.textSize = 11.sp.toPx()
                val label = if (gradeHidden) "$gradeLabel (hidden)" else gradeLabel
                clipRect(l, gradeTop, r, gradeTop + gradeH) {
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        max(l, 0f) + 10.dp.toPx(),
                        gradeTop + gradeH / 2f + paint.textSize * 0.35f,
                        paint,
                    )
                }
            }

            // ---------- Playhead ----------
            drawLine(Color.White, Offset(cx, 0f), Offset(cx, h), strokeWidth = 2.dp.toPx())
            drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(cx, 5.dp.toPx()))
        }
    }
}
