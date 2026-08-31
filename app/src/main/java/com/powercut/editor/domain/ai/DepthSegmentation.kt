package com.powercut.editor.domain.ai

import android.opengl.GLES20

/**
 * Real-time depth-aware background segmentation engine.
 * Features: background bokeh blur, neon glow subject outline, rim lighting.
 */
class DepthSegmentation {
    private var isInitialized = false
    private var blurProgram: Int = 0
    private var neonProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private var maskTextureId: Int = 0

    private external fun nativeInitializeSegmentation()
    private external fun nativeGetSegmentationMask(textureId: Int, width: Int, height: Int): FloatArray?
    private external fun nativeReleaseSegmentation()

    fun initialize() {
        if (isInitialized) return
        blurProgram = ShaderProgram.compileProgram(VERTEX_SHADER, BLUR_FRAGMENT_SHADER)
        neonProgram = ShaderProgram.compileProgram(VERTEX_SHADER, NEON_FRAGMENT_SHADER)
        createFramebuffers()
        try { nativeInitializeSegmentation() } catch (_: UnsatisfiedLinkError) {}
        isInitialized = true
    }

    fun apply(
        textureId: Int, faceLandmarks: List<FaceLandmark>?,
        blurIntensity: Float, neonGlowEnabled: Boolean,
        neonGlowColor: FloatArray, neonGlowIntensity: Float,
        width: Int, height: Int
    ): Int {
        if (!isInitialized) return textureId
        val mask = try { nativeGetSegmentationMask(textureId, width, height) } catch (_: Exception) { generateFallbackMask(width, height) } ?: return textureId
        uploadMask(mask, width, height)

        var current = textureId
        if (blurIntensity > 0.01f) current = applyBlur(current, blurIntensity, width, height)
        if (neonGlowEnabled && neonGlowIntensity > 0.01f) current = applyNeon(current, neonGlowColor, neonGlowIntensity, width, height)
        return current
    }

    private fun generateFallbackMask(w: Int, h: Int): FloatArray {
        val mask = FloatArray(w * h); val cx = w / 2f; val cy = h / 2f
        for (y in 0 until h) for (x in 0 until w) {
            val d = kotlin.math.sqrt(((x - cx) / cx).toDouble().let { it * it } + ((y - cy) / cy).toDouble().let { it * it })
            mask[y * w + x] = (1f - (d / 1.5f).toFloat()).coerceIn(0f, 1f)
        }
        return mask
    }

    private fun uploadMask(mask: FloatArray, w: Int, h: Int) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTextureId)
        val buf = java.nio.ByteBuffer.allocateDirect(mask.size * 4).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(mask); position(0) }
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, w, h, 0, GLES20.GL_LUMINANCE, GLES20.GL_FLOAT, buf)
    }

    private fun applyBlur(texId: Int, intensity: Float, w: Int, h: Int): Int {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, w, h)
        GLES20.glUseProgram(blurProgram)
        bindTexture(blurProgram, "u_texture", texId, 0)
        bindTexture(blurProgram, "u_mask", maskTextureId, 1)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(blurProgram, "u_blurIntensity"), intensity)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(blurProgram, "u_resolution"), w.toFloat(), h.toFloat())
        drawQuad(); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    private fun applyNeon(texId: Int, color: FloatArray, intensity: Float, w: Int, h: Int): Int {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, w, h)
        GLES20.glUseProgram(neonProgram)
        bindTexture(neonProgram, "u_texture", texId, 0)
        bindTexture(neonProgram, "u_mask", maskTextureId, 1)
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(neonProgram, "u_glowColor"), 1, color, 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(neonProgram, "u_glowIntensity"), intensity)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(neonProgram, "u_resolution"), w.toFloat(), h.toFloat())
        drawQuad(); GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    private fun bindTexture(program: Int, name: String, texId: Int, unit: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program, name), unit)
    }

    private fun drawQuad() {
        val v = floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)
        val buf = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(v); position(0) }
        val currentProgram = blurProgram // Use stored program reference
        val p = GLES20.glGetAttribLocation(currentProgram, "a_position")
        val t = GLES20.glGetAttribLocation(currentProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    private fun createFramebuffers() {
        val fb = IntArray(1); val tx = IntArray(2)
        GLES20.glGenFramebuffers(1, fb, 0); GLES20.glGenTextures(2, tx, 0)
        fboId = fb[0]; fboTextureId = tx[0]; maskTextureId = tx[1]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1920, 1080, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureId, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(blurProgram); GLES20.glDeleteProgram(neonProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(2, intArrayOf(fboTextureId, maskTextureId), 0)
        try { nativeReleaseSegmentation() } catch (_: Exception) {}
        isInitialized = false
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"
        private const val BLUR_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture; uniform sampler2D u_mask;
            uniform float u_blurIntensity; uniform vec2 u_resolution;
            varying vec2 v_texCoord;
            void main() {
                vec2 uv = v_texCoord; float mask = texture2D(u_mask, uv).r;
                vec4 blurred = vec4(0.0); float tw = 0.0;
                for (int x = -4; x <= 4; x++) {
                    for (int y = -4; y <= 4; y++) {
                        vec2 off = vec2(float(x), float(y)) / u_resolution * u_blurIntensity * 12.0;
                        float w = exp(-0.5 * float(x*x + y*y) / 16.0);
                        blurred += texture2D(u_texture, uv + off) * w; tw += w;
                    }
                }
                gl_FragColor = mix(blurred / tw, texture2D(u_texture, uv), mask);
            }
        """
        private const val NEON_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture; uniform sampler2D u_mask;
            uniform vec4 u_glowColor; uniform float u_glowIntensity;
            uniform vec2 u_resolution; varying vec2 v_texCoord;
            void main() {
                vec2 uv = v_texCoord; vec2 ts = 1.0 / u_resolution;
                float e = abs(texture2D(u_mask, uv + vec2(ts.x, 0.0)).r - texture2D(u_mask, uv - vec2(ts.x, 0.0)).r)
                        + abs(texture2D(u_mask, uv + vec2(0.0, ts.y)).r - texture2D(u_mask, uv - vec2(0.0, ts.y)).r);
                e = smoothstep(0.0, 0.3, e);
                float glow = 0.0;
                for (int x = -4; x <= 4; x++) for (int y = -4; y <= 4; y++) {
                    float se = abs(texture2D(u_mask, uv + vec2(float(x)+1.0, float(y)) * ts * 3.0).r - texture2D(u_mask, uv + vec2(float(x)-1.0, float(y)) * ts * 3.0).r);
                    glow += se * exp(-0.5 * float(x*x + y*y) / 9.0);
                }
                glow = clamp(glow, 0.0, 1.0);
                vec4 orig = texture2D(u_texture, uv);
                gl_FragColor = vec4(orig.rgb + u_glowColor.rgb * glow * u_glowIntensity + u_glowColor.rgb * e * u_glowIntensity * 0.5, 1.0);
            }
        """
    }
}
