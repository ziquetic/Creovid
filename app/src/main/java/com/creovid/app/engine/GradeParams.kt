package com.creovid.app.engine

import kotlin.math.abs

/**
 * Ek colour wheel: hue (0..360), amount (0..1) aur luma (-1..1).
 * Shader ko RGB offset chahiye, wo rgb() se milta hai.
 */
data class Wheel(
    val hue: Float = 0f,
    val amount: Float = 0f,
    val luma: Float = 0f,
) {
    fun rgb(scale: Float): FloatArray {
        val h = (((hue % 360f) + 360f) % 360f) / 60f
        val x = 1f - abs(h % 2f - 1f)
        val r: Float
        val g: Float
        val b: Float
        when (h.toInt()) {
            0 -> { r = 1f; g = x; b = 0f }
            1 -> { r = x; g = 1f; b = 0f }
            2 -> { r = 0f; g = 1f; b = x }
            3 -> { r = 0f; g = x; b = 1f }
            4 -> { r = x; g = 0f; b = 1f }
            else -> { r = 1f; g = 0f; b = x }
        }
        val mean = (r + g + b) / 3f
        return floatArrayOf(
            ((r - mean) * amount + luma) * scale,
            ((g - mean) * amount + luma) * scale,
            ((b - mean) * amount + luma) * scale,
        )
    }
}

/** Poori grading ki saari settings. Immutable hai, isliye GL thread pe safe. */
data class GradeParams(
    // Basic
    val exposure: Float = 0f,      // -2..2 (EV)
    val contrast: Float = 0f,      // -1..1
    val highlights: Float = 0f,    // -1..1
    val shadows: Float = 0f,       // -1..1
    val saturation: Float = 0f,    // -1..1
    val vibrance: Float = 0f,      // -1..1
    val temperature: Float = 0f,   // -1..1 (cool..warm)
    val tint: Float = 0f,          // -1..1 (green..magenta)
    val fade: Float = 0f,          // 0..1 (matte look)
    val filmCurve: Float = 0f,     // 0..1 (S-curve)
    // Colour wheels
    val lift: Wheel = Wheel(),     // shadows
    val gamma: Wheel = Wheel(),    // midtones
    val gain: Wheel = Wheel(),     // highlights
    // Look (LUT)
    val lutIntensity: Float = 1f,  // 0..1
    // Effects
    val grain: Float = 0f,         // 0..1
    val bloom: Float = 0f,         // 0..1
    val halation: Float = 0f,      // 0..1
    val vignette: Float = 0f,      // 0..1
    val letterbox: Float = 0f,     // 0 = off, warna target aspect (2.39 etc.)
) {
    companion object {
        val Neutral = GradeParams()
    }
}

/**
 * Preview aur export dono isi object ko padhte hain, isliye jo dikhta hai wahi export hota hai.
 * LUT GL thread pe upload hoti hai (lutVersion badalne par).
 */
class GradeState {
    @Volatile
    var params: GradeParams = GradeParams()

    @Volatile
    var lut: CubeLut? = null
        private set

    @Volatile
    var lutVersion: Int = 0
        private set

    fun setLut(newLut: CubeLut?) {
        lut = newLut
        lutVersion += 1
    }
}
