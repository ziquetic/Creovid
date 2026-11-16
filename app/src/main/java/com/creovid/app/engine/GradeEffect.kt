package com.creovid.app.engine

import android.content.Context
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Media3 effect: ExoPlayer preview aur Transformer export dono me lagta hai. */
@OptIn(UnstableApi::class)
class GradeEffect(private val state: GradeState) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        GradeShaderProgram(useHdr, state)
}

@OptIn(UnstableApi::class)
private class GradeShaderProgram(
    useHdr: Boolean,
    private val state: GradeState,
) : BaseGlShaderProgram(useHdr, 1) {

    private val program: GlProgram = try {
        GlProgram(GradeShader.VERTEX, GradeShader.FRAGMENT)
    } catch (e: GlUtil.GlException) {
        throw VideoFrameProcessingException(e)
    }

    private var width = 1
    private var height = 1
    private var lutTex = 0
    private var uploadedVersion = -1
    private var lutSize = 2f
    private var useLut = 0f

    init {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        lutTex = ids[0]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        uploadDummy()
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        width = inputWidth
        height = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            syncLut()
            val p = state.params
            val seconds = ((presentationTimeUs / 1_000_000.0) % 1000.0).toFloat()

            program.use()
            program.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            program.setSamplerTexIdUniform("uLut", lutTex, 1)

            program.setFloatUniform("uExposure", p.exposure)
            program.setFloatUniform("uContrast", p.contrast)
            program.setFloatUniform("uHighlights", p.highlights)
            program.setFloatUniform("uShadows", p.shadows)
            program.setFloatUniform("uSaturation", p.saturation)
            program.setFloatUniform("uVibrance", p.vibrance)
            program.setFloatUniform("uTemperature", p.temperature)
            program.setFloatUniform("uTint", p.tint)
            program.setFloatUniform("uFade", p.fade)
            program.setFloatUniform("uFilmCurve", p.filmCurve)
            program.setFloatsUniform("uLift", p.lift.rgb(0.25f))
            program.setFloatsUniform("uGamma", p.gamma.rgb(0.6f))
            program.setFloatsUniform("uGain", p.gain.rgb(0.5f))
            program.setFloatUniform("uLutIntensity", p.lutIntensity)
            program.setFloatUniform("uLutSize", lutSize)
            program.setFloatUniform("uUseLut", useLut)
            program.setFloatUniform("uGrain", p.grain)
            program.setFloatUniform("uBloom", p.bloom)
            program.setFloatUniform("uHalation", p.halation)
            program.setFloatUniform("uVignette", p.vignette)
            program.setFloatUniform("uLetterbox", p.letterbox)
            program.setFloatUniform("uFrameAspect", width.toFloat() / height.toFloat())
            program.setFloatsUniform("uResolution", floatArrayOf(width.toFloat(), height.toFloat()))
            program.setFloatUniform("uTime", seconds)

            program.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE,
            )
            program.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            program.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
        GLES20.glDeleteTextures(1, intArrayOf(lutTex), 0)
    }

    /** LUT badli ho to GL thread pe nayi texture upload karta hai. */
    private fun syncLut() {
        val version = state.lutVersion
        if (version == uploadedVersion) return
        val lut = state.lut
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTex)
        if (lut == null) {
            uploadDummy()
        } else {
            val buf = ByteBuffer.allocateDirect(lut.rgba.size).order(ByteOrder.nativeOrder())
            buf.put(lut.rgba).position(0)
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                lut.width, lut.height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf,
            )
            lutSize = lut.size.toFloat()
            useLut = 1f
        }
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        uploadedVersion = version
    }

    private fun uploadDummy() {
        val buf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buf,
        )
        lutSize = 2f
        useLut = 0f
    }
}
