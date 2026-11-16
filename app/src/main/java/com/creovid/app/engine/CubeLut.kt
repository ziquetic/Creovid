package com.creovid.app.engine

import java.io.InputStream

/**
 * 3D LUT (.cube) ko ek 2D strip texture (size*size x size) me badal deta hai.
 * Strip layout: x = blue*size + red, y = green. RGBA bytes.
 */
class CubeLut(
    val name: String,
    val size: Int,
    val rgba: ByteArray,
) {
    val width: Int get() = size * size
    val height: Int get() = size

    companion object {
        const val MAX_SIZE = 64

        fun parse(name: String, input: InputStream): CubeLut {
            var size = 0
            val dMin = floatArrayOf(0f, 0f, 0f)
            val dMax = floatArrayOf(1f, 1f, 1f)
            var values = FloatArray(0)
            var count = 0

            input.bufferedReader().useLines { lines ->
                for (raw in lines) {
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#")) continue
                    val first = line[0]
                    if (first.isLetter()) {
                        val parts = line.split(Regex("\\s+"))
                        when (parts[0].uppercase()) {
                            "LUT_3D_SIZE" -> {
                                size = parts.getOrNull(1)?.toIntOrNull()
                                    ?: throw IllegalArgumentException("LUT_3D_SIZE galat hai")
                                if (size < 2 || size > MAX_SIZE) {
                                    throw IllegalArgumentException("LUT size 2 se $MAX_SIZE ke beech hona chahiye")
                                }
                                values = FloatArray(size * size * size * 3)
                            }
                            "LUT_1D_SIZE" -> throw IllegalArgumentException("Sirf 3D LUT (.cube) chalti hai, ye 1D LUT hai")
                            "DOMAIN_MIN" -> for (i in 0..2) dMin[i] = parts.getOrNull(i + 1)?.toFloatOrNull() ?: 0f
                            "DOMAIN_MAX" -> for (i in 0..2) dMax[i] = parts.getOrNull(i + 1)?.toFloatOrNull() ?: 1f
                            else -> Unit // TITLE etc.
                        }
                    } else {
                        if (size == 0) throw IllegalArgumentException("Data se pehle LUT_3D_SIZE chahiye")
                        val parts = line.split(Regex("\\s+"))
                        if (parts.size >= 3 && count + 3 <= values.size) {
                            values[count++] = parts[0].toFloat()
                            values[count++] = parts[1].toFloat()
                            values[count++] = parts[2].toFloat()
                        }
                    }
                }
            }

            if (size == 0 || count != values.size) {
                throw IllegalArgumentException("LUT file adhuri ya kharab hai")
            }

            val n = size
            val w = n * n
            val out = ByteArray(w * n * 4)
            for (i in 0 until n * n * n) {
                val r = i % n
                val g = (i / n) % n
                val b = i / (n * n)
                val x = b * n + r
                val y = g
                val o = (y * w + x) * 4
                for (c in 0..2) {
                    val range = (dMax[c] - dMin[c]).let { if (it == 0f) 1f else it }
                    val v = ((values[i * 3 + c] - dMin[c]) / range).coerceIn(0f, 1f)
                    out[o + c] = (v * 255f + 0.5f).toInt().toByte()
                }
                out[o + 3] = 255.toByte()
            }
            return CubeLut(name, n, out)
        }
    }
}
