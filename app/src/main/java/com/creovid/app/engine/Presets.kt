package com.creovid.app.engine

/** Ek cinematic look. swatchA/swatchB sirf chip ka gradient dikhane ke liye hain. */
data class Preset(
    val name: String,
    val params: GradeParams,
    val swatchA: Long,
    val swatchB: Long,
)

object Presets {

    val all: List<Preset> = listOf(
        Preset("Original", GradeParams(), 0xFF3A3A4A, 0xFF8A8A9A),

        Preset(
            "Teal & Orange",
            GradeParams(
                contrast = 0.15f, saturation = 0.10f, vibrance = 0.15f, filmCurve = 0.30f,
                lift = Wheel(190f, 0.40f, -0.02f),
                gain = Wheel(28f, 0.38f, 0f),
                vignette = 0.25f, grain = 0.15f,
            ),
            0xFF1E9AA8, 0xFFE8883A,
        ),
        Preset(
            "Blockbuster",
            GradeParams(
                contrast = 0.25f, saturation = -0.05f, vibrance = 0.20f, filmCurve = 0.50f,
                lift = Wheel(200f, 0.28f, -0.05f),
                gain = Wheel(35f, 0.25f, 0f),
                bloom = 0.15f, vignette = 0.30f, grain = 0.12f, letterbox = 2.39f,
            ),
            0xFF234E6B, 0xFFD9A25C,
        ),
        Preset(
            "Moody Blue",
            GradeParams(
                exposure = -0.20f, temperature = -0.35f, contrast = 0.20f, saturation = -0.15f,
                fade = 0.20f, filmCurve = 0.25f,
                lift = Wheel(220f, 0.42f, 0f),
                gain = Wheel(210f, 0.15f, 0f),
                vignette = 0.40f, grain = 0.20f,
            ),
            0xFF16275A, 0xFF5E86D6,
        ),
        Preset(
            "Golden Hour",
            GradeParams(
                exposure = 0.10f, temperature = 0.45f, tint = 0.05f, highlights = -0.20f, vibrance = 0.25f,
                lift = Wheel(20f, 0.15f, 0f),
                gain = Wheel(35f, 0.30f, 0f),
                bloom = 0.25f, halation = 0.20f, vignette = 0.20f, grain = 0.10f,
            ),
            0xFFB8561E, 0xFFFFC66B,
        ),
        Preset(
            "Vintage Film",
            GradeParams(
                contrast = -0.05f, saturation = -0.20f, temperature = 0.20f, fade = 0.45f, filmCurve = 0.40f,
                lift = Wheel(200f, 0.20f, 0f),
                gain = Wheel(45f, 0.20f, 0f),
                halation = 0.30f, vignette = 0.35f, grain = 0.50f,
            ),
            0xFF6E5A3C, 0xFFD8C08A,
        ),
        Preset(
            "Noir B&W",
            GradeParams(
                saturation = -1f, contrast = 0.45f, shadows = -0.10f, filmCurve = 0.50f,
                vignette = 0.50f, grain = 0.40f,
            ),
            0xFF050505, 0xFFE6E6E6,
        ),
        Preset(
            "Cyberpunk",
            GradeParams(
                temperature = -0.10f, contrast = 0.20f, saturation = 0.35f, filmCurve = 0.30f,
                lift = Wheel(290f, 0.45f, 0f),
                gain = Wheel(180f, 0.40f, 0f),
                bloom = 0.40f, vignette = 0.30f, grain = 0.10f,
            ),
            0xFFB020D8, 0xFF20D8E0,
        ),
        Preset(
            "Bleach Bypass",
            GradeParams(
                saturation = -0.50f, contrast = 0.40f, highlights = -0.10f, filmCurve = 0.40f,
                vignette = 0.25f, grain = 0.25f,
            ),
            0xFF3E4448, 0xFFB5BCC0,
        ),
        Preset(
            "Matte Cinema",
            GradeParams(
                contrast = 0.10f, saturation = -0.10f, fade = 0.60f, filmCurve = 0.30f,
                lift = Wheel(210f, 0.20f, 0f),
                gain = Wheel(40f, 0.15f, 0f),
                vignette = 0.20f, grain = 0.15f, letterbox = 2.0f,
            ),
            0xFF2C3A46, 0xFFB9A98C,
        ),
        Preset(
            "Emerald Forest",
            GradeParams(
                contrast = 0.15f, saturation = 0.10f, vibrance = 0.20f, fade = 0.15f,
                lift = Wheel(140f, 0.30f, 0f),
                gamma = Wheel(100f, 0.10f, 0f),
                gain = Wheel(60f, 0.20f, 0f),
                vignette = 0.25f, grain = 0.10f,
            ),
            0xFF0F4A32, 0xFFB5C96A,
        ),
        Preset(
            "Day for Night",
            GradeParams(
                exposure = -0.90f, temperature = -0.50f, contrast = 0.20f, saturation = -0.30f,
                lift = Wheel(225f, 0.50f, 0f),
                vignette = 0.50f, grain = 0.25f,
            ),
            0xFF0B1230, 0xFF3A56A8,
        ),
        Preset(
            "Dream Glow",
            GradeParams(
                contrast = 0.10f, vibrance = 0.15f, fade = 0.20f,
                bloom = 0.35f, halation = 0.30f, vignette = 0.15f,
            ),
            0xFF7A5CD6, 0xFFF0B8E8,
        ),
    )
}
