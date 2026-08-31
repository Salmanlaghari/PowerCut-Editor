package com.powercut.editor.domain.ai

import android.opengl.GLES20

/**
 * Audio-reactive visual effects engine.
 * Uses Web Audio API / FFT spectrum analysis to modulate shader parameters
 * based on beat detection for pulse, glow, and zoom effects.
 */
class AudioReactiveEngine {
    private var isInitialized = false
    private var shaderProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private var lastBeatTime = 0L
    private var beatIntensity = 0f
    private val freqBands = FloatArray(8) // 8 frequency bands

    private external fun nativeInitializeAudio()
    private external fun nativeGetSpectrumData(): FloatArray?
    private external fun nativeReleaseAudio()

    fun initialize() {
        if (isInitialized) return
        shaderProgram = ShaderProgram.compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        createFramebuffer()
        try { nativeInitializeAudio() } catch (_: UnsatisfiedLinkError) {}
        isInitialized = true
    }

    fun analyzeBeat(timestampMs: Long) {
        val spectrum = try { nativeGetSpectrumData() } catch (_: Exception) { null }
        if (spectrum != null && spectrum.size >= 8) {
            System.arraycopy(spectrum, 0, freqBands, 0, 8)
        }
        // Simple beat detection: threshold on bass band
        val bassEnergy = freqBands.getOrElse(0) { 0f }
        if (bassEnergy > 0.7f && timestampMs - lastBeatTime > 200) {
            lastBeatTime = timestampMs
            beatIntensity = 1f
        }
        beatIntensity *= 0.92f // Decay
    }

    fun apply(
        textureId: Int, timestampMs: Long,
        pulseIntensity: Float, glowIntensity: Float,
        width: Int, height: Int
    ): Int {
        if (!isInitialized) return textureId
        analyzeBeat(timestampMs)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(shaderProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_texture"), 0)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_time"), timestampMs / 1000f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_beat"), beatIntensity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_pulseIntensity"), pulseIntensity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_glowIntensity"), glowIntensity)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(shaderProgram, "u_resolution"), width.toFloat(), height.toFloat())
        GLES20.glUniform1fv(GLES20.glGetUniformLocation(shaderProgram, "u_freqBands"), 8, freqBands, 0)

        drawQuad()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(shaderProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        try { nativeReleaseAudio() } catch (_: Exception) {}
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
        val p = GLES20.glGetAttribLocation(shaderProgram, "a_position")
        val t = GLES20.glGetAttribLocation(shaderProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform float u_time; uniform float u_beat;
            uniform float u_pulseIntensity; uniform float u_glowIntensity;
            uniform vec2 u_resolution; uniform float u_freqBands[8];
            varying vec2 v_texCoord;

            void main() {
                vec2 uv = v_texCoord;
                vec2 center = vec2(0.5);
                float bass = u_freqBands[0]; float mid = u_freqBands[3]; float high = u_freqBands[6];

                // Beat-synced zoom pulse
                float zoom = 1.0 + u_beat * u_pulseIntensity * 0.05;
                uv = (uv - center) / zoom + center;

                // Hue shift on beat
                float hueShift = u_beat * u_pulseIntensity * 0.1;
                vec4 color = texture2D(u_texture, uv);
                color.r += sin(u_time * 2.0 + hueShift * 6.28) * 0.05 * u_beat;
                color.b += cos(u_time * 1.5 + hueShift * 6.28) * 0.05 * u_beat;

                // Neon glow pulse
                float glow = u_beat * u_glowIntensity;
                color.rgb += vec3(0.0, 0.8, 1.0) * glow * 0.15;

                // Frequency-based brightness modulation
                float brightness = 1.0 + bass * u_pulseIntensity * 0.1 + mid * u_pulseIntensity * 0.05;
                color.rgb *= brightness;

                // Vignette pulse
                float dist = distance(uv, center);
                float vignette = 1.0 - dist * 0.5 * (1.0 + u_beat * 0.3);
                color.rgb *= vignette;

                gl_FragColor = color;
            }
        """
    }
}
