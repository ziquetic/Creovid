package com.creovid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.creovid.app.engine.Wheel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Label + value + slider. Label par double-tap karne se value default par aa jaati hai. */
@Composable
fun GradeSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit = {},
    default: Float = 0f,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                color = CV.Text,
                fontSize = 13.sp,
                modifier = Modifier.pointerInput(default) {
                    detectTapGestures(onDoubleTap = {
                        onChange(default)
                        onFinished()
                    })
                },
            )
            Text(
                String.format("%.2f", value),
                color = if (value == default) CV.TextDim else CV.Sky,
                fontSize = 12.sp,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = CV.Violet,
                inactiveTrackColor = CV.Outline,
            ),
            modifier = Modifier.height(28.dp),
        )
    }
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(CV.GradientH)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun OutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, CV.Outline, RoundedCornerShape(50))
            .background(CV.Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = CV.Text, fontSize = 13.sp)
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(CV.GradientH)
                else Modifier.background(CV.Surface2).border(1.dp, CV.Outline, shape),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) Color.White else CV.TextDim,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Colour wheel: kinare ki taraf = zyada colour, beech = neutral.
 * Drag/tap se hue + amount badalta hai, double-tap se reset.
 */
@Composable
fun ColorWheel(
    title: String,
    wheel: Wheel,
    onChange: (Wheel) -> Unit,
    onFinished: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val current by rememberUpdatedState(wheel)
    val callback by rememberUpdatedState(onChange)
    val finished by rememberUpdatedState(onFinished)
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun handle(p: Offset) {
        val r = size.width / 2f
        if (r <= 0f) return
        val d = p - Offset(r, size.height / 2f)
        val angle = Math.toDegrees(atan2(d.y, d.x).toDouble()).toFloat()
        callback(
            current.copy(
                hue = (angle + 360f) % 360f,
                amount = (hypot(d.x, d.y) / r).coerceIn(0f, 1f),
            ),
        )
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = CV.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth(0.86f)
                .aspectRatio(1f)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            handle(it)
                            finished()
                        },
                        onDoubleTap = {
                            callback(current.copy(amount = 0f))
                            finished()
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { handle(it) },
                        onDragEnd = { finished() },
                        onDrag = { change, _ ->
                            change.consume()
                            handle(change.position)
                        },
                    )
                },
        ) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
                val hues = (0..12).map { Color.hsv((it * 30f) % 360f, 0.85f, 1f) }
                drawCircle(Brush.sweepGradient(hues))
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color(0xFF1A1A28), Color(0x001A1A28)),
                        center = center,
                        radius = this.size.minDimension / 2f,
                    ),
                )
                drawCircle(CV.Outline, style = Stroke(width = 2f))
                // Handle
                val r = this.size.minDimension / 2f
                val a = Math.toRadians(wheel.hue.toDouble())
                val pos = Offset(
                    center.x + (cos(a) * wheel.amount * r).toFloat(),
                    center.y + (sin(a) * wheel.amount * r).toFloat(),
                )
                drawCircle(Color.White, radius = 9f, center = pos, style = Stroke(width = 3f))
                drawCircle(Color.Black.copy(alpha = 0.35f), radius = 6f, center = pos)
            }
        }
        GradeSlider(
            label = "Luma",
            value = wheel.luma,
            range = -0.5f..0.5f,
            onChange = { onChange(wheel.copy(luma = it)) },
            onFinished = onFinished,
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 8) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}
