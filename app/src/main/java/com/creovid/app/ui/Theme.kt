package com.creovid.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Creovid logo + Savi jaisa palette: kaala background, neela se baingani gradient. */
object CV {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF0C0C14)
    val Surface2 = Color(0xFF15152A)
    val Outline = Color(0xFF2A2A48)

    val Sky = Color(0xFF4DB8FF)
    val Blue = Color(0xFF3D6BFF)
    val Violet = Color(0xFF7C4DFF)
    val Lilac = Color(0xFFC084FC)

    val Text = Color(0xFFF2F2FA)
    val TextDim = Color(0xFF9A9AB8)

    val Gradient: Brush
        get() = Brush.linearGradient(listOf(Sky, Blue, Violet, Lilac))

    val GradientH: Brush
        get() = Brush.horizontalGradient(listOf(Blue, Violet, Lilac))
}

private val scheme = darkColorScheme(
    primary = CV.Violet,
    onPrimary = Color.White,
    secondary = CV.Sky,
    background = CV.Bg,
    onBackground = CV.Text,
    surface = CV.Surface,
    onSurface = CV.Text,
    surfaceVariant = CV.Surface2,
    onSurfaceVariant = CV.TextDim,
    outline = CV.Outline,
)

@Composable
fun CreovidTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
