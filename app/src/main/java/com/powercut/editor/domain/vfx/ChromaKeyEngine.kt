package com.powercut.editor.domain.vfx

import android.opengl.GLES20

/**
 * AI Chroma Key & Background Keying Engine.
 * Real-time GLSL Chroma Keyer with eyedropper, tolerance, spill suppression, and edge feathering.
 */
class ChromaKeyEngine {
    private var isInitialized = false
    private var chromaKeyProgram: Int = 0
    private var autoCutoutProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0

    fun initialize() {
        if (isInitialized) return
        chromaKeyProgram = ShaderProgram.compileProgram(VERTEX_SHADER, CHROMA_KEY_FRAGMENT_SHADER)
        autoCutoutProgram = ShaderProgram.compileProgram(VERTEX_SHADER, AUTO_CUTOUT_FRAGMENT_SHADER)
        createFramebuffer()
        isInitialized = true
    }

    /**
     * Apply chroma key effect to remove background color.
     */
    fun applyChromaKey(
        textureId: Int,
        keyColor: FloatArray, // RGB (0-1)
        tolerance: Float,
        spillSuppression: Float,
        edgeFeather: Float,
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized) return textureId
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(chromaKeyProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(chromaKeyProgram, "u_texture"), 0)

        GLES20.glUniform3fv(GLES20.glGetUniformLocation(chromaKeyProgram, "u_keyColor"), 1, keyColor, 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(chromaKeyProgram, "u_tolerance"), tolerance)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(chromaKeyProgram, "u_spillSuppression"), spillSuppression)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(chromaKeyProgram, "u_edgeFeather"), edgeFeather)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(chromaKeyProgram, "u_resolution"), width.toFloat(), height.toFloat())

        drawQuad()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    /**
     * AI one-tap cutout without green screen (portrait/object detection).
     */
    fun applyAutoCutout(
        textureId: Int,
        sensitivity: Float,
        edgeSmoothing: Float,
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized) return textureId
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(autoCutoutProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(autoCutoutProgram, "u_texture"), 0)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(autoCutoutProgram, "u_sensitivity"), sensitivity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(autoCutoutProgram, "u_edgeSmoothing"), edgeSmoothing)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(autoCutoutProgram, "u_resolution"), width.toFloat(), height.toFloat())

        drawQuad()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    /**
     * Sample color at a point for eyedropper.
     */
    fun sampleColor(textureId: Int, x: Int, y: Int, width: Int, height: Int): FloatArray {
        val pixel = IntArray(1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glReadPixels(x, height - y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, pixel, 0)
        val r = ((pixel[0] shr 16) and 0xFF) / 255f
        val g = ((pixel[0] shr 8) and 0xFF) / 255f
        val b = (pixel[0] and 0xFF) / 255f
        return floatArrayOf(r, g, b)
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(chromaKeyProgram)
        GLES20.glDeleteProgram(autoCutoutProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        isInitialized = false
    }

    private fun createFramebuffer() {
        val fb = IntArray(1); val tx = IntArray(1)
        GLES20.glGenFramebuffers(1, fb, 0); GLES20.glGenTextures(1, tx, 0)
        fboId = fb[0]; fboTextureId = tx[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1920, 1080, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureId, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    private fun drawQuad() {
        val v = floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)
        val buf = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(v); position(0) }
        val p = GLES20.glGetAttribLocation(GLES20.glGetCurrentProgram(), "a_position")
        val t = GLES20.glGetAttribLocation(GLES20.glGetCurrentProgram(), "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"

        private const val CHROMA_KEY_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform vec3 u_keyColor;
            uniform float u_tolerance;
            uniform float u_spillSuppression;
            uniform float u_edgeFeather;
            uniform vec2 u_resolution;
            varying vec2 v_texCoord;

            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                float dist = distance(color.rgb, u_keyColor);
                float alpha = smoothstep(u_tolerance - u_edgeFeather, u_tolerance + u_edgeFeather, dist);

                // Spill suppression: reduce green spill on edges
                float spill = max(0.0, u_keyColor.g - max(color.r, color.b));
                vec3 suppressed = color.rgb - vec3(0.0, spill * u_spillSuppression, 0.0);

                gl_FragColor = vec4(suppressed, alpha);
            }
        """

        private const val AUTO_CUTOUT_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform float u_sensitivity;
            uniform float u_edgeSmoothing;
            uniform vec2 u_resolution;
            varying vec2 v_texCoord;

            // Skin color detection for portrait cutout
            float isSkin(vec3 c) {
                float r = c.r, g = c.g, b = c.b;
                return step(0.25, step(abs(r - g) / max(r, 0.001), 0.5)) *
                       step(0.0, r - 0.15) * step(g, r + 0.1) *
                       step(0.1, g) * step(0.05, b);
            }

            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                vec2 ts = 1.0 / u_resolution;

                // Edge detection via Sobel
                float tl = isSkin(texture2D(u_texture, v_texCoord + vec2(-ts.x, -ts.y)).rgb);
                float t  = isSkin(texture2D(u_texture, v_texCoord + vec2(0.0, -ts.y)).rgb);
                float tr = isSkin(texture2D(u_texture, v_texCoord + vec2(ts.x, -ts.y)).rgb);
                float l  = isSkin(texture2D(u_texture, v_texCoord + vec2(-ts.x, 0.0)).rgb);
                float r  = isSkin(texture2D(u_texture, v_texCoord + vec2(ts.x, 0.0)).rgb);
                float bl = isSkin(texture2D(u_texture, v_texCoord + vec2(-ts.x, ts.y)).rgb);
                float b  = isSkin(texture2D(u_texture, v_texCoord + vec2(0.0, ts.y)).rgb);
                float br = isSkin(texture2D(u_texture, v_texCoord + vec2(ts.x, ts.y)).rgb);

                float center = isSkin(color.rgb);
                float edge = abs(-tl - 2.0*t - tr + bl + 2.0*b + br) +
                             abs(-tl - 2.0*l - bl + tr + 2.0*r + br);
                edge = smoothstep(0.0, u_edgeSmoothing, edge);

                float alpha = mix(center, edge, 0.3) * u_sensitivity;
                alpha = smoothstep(0.0, 0.5, alpha);

                gl_FragColor = vec4(color.rgb, alpha);
            }
        """
    }
}

data class ChromaKeyConfig(
    val keyColor: FloatArray = floatArrayOf(0f, 1f, 0f), // Green
    val tolerance: Float = 0.3f,
    val spillSuppression: Float = 0.5f,
    val edgeFeather: Float = 0.1f,
    val useAutoCutout: Boolean = false
)
