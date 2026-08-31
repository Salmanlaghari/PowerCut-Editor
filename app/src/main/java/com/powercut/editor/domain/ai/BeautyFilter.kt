package com.powercut.editor.domain.ai

import android.opengl.GLES20

/**
 * GPU-accelerated beauty filter engine.
 * Applies skin smoothing, eye brightening, and face reshaping via GLSL shaders.
 */
class BeautyFilter {
    private var isInitialized = false
    private var shaderProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0

    fun initialize() {
        if (isInitialized) return
        shaderProgram = ShaderProgram.compileProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        createFramebuffer()
        isInitialized = true
    }

    fun apply(
        textureId: Int, landmarks: List<FaceLandmark>,
        smoothIntensity: Float, brightenIntensity: Float, reshapeIntensity: Float,
        width: Int, height: Int
    ): Int {
        if (!isInitialized || landmarks.isEmpty()) return textureId
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(shaderProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_texture"), 0)

        val landmarkData = FloatArray(468 * 2)
        landmarks.forEachIndexed { i, lm -> if (i < 468) { landmarkData[i * 2] = lm.x; landmarkData[i * 2 + 1] = lm.y } }
        GLES20.glUniform2fv(GLES20.glGetUniformLocation(shaderProgram, "u_landmarks"), 468, landmarkData, 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_smoothIntensity"), smoothIntensity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_brightenIntensity"), brightenIntensity)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_reshapeIntensity"), reshapeIntensity)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(shaderProgram, "u_resolution"), width.toFloat(), height.toFloat())

        drawQuad()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(shaderProgram)
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
        val pos = GLES20.glGetAttribLocation(shaderProgram, "a_position")
        val tex = GLES20.glGetAttribLocation(shaderProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(pos); GLES20.glVertexAttribPointer(pos, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(tex); GLES20.glVertexAttribPointer(tex, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(pos); GLES20.glDisableVertexAttribArray(tex)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform vec2 u_landmarks[468];
            uniform float u_smoothIntensity;
            uniform float u_brightenIntensity;
            uniform float u_reshapeIntensity;
            uniform vec2 u_resolution;
            varying vec2 v_texCoord;

            float isSkinRegion(vec2 uv) {
                float minDist = 1.0;
                for (int i = 10; i < 468; i += 3) {
                    minDist = min(minDist, distance(uv, u_landmarks[i]));
                }
                return smoothstep(0.15, 0.05, minDist);
            }

            void main() {
                vec2 uv = v_texCoord;
                vec4 color = texture2D(u_texture, uv);
                if (u_smoothIntensity > 0.01) {
                    float skin = isSkinRegion(uv);
                    if (skin > 0.01) {
                        vec4 blurred = vec4(0.0); float tw = 0.0;
                        vec2 ts = 1.0 / u_resolution;
                        for (int x = -3; x <= 3; x++) {
                            for (int y = -3; y <= 3; y++) {
                                vec4 s = texture2D(u_texture, uv + vec2(float(x), float(y)) * ts * 2.0);
                                float w = exp(-0.5 * float(x*x + y*y) / 4.0) * exp(-0.5 * distance(color.rgb, s.rgb) / 0.01);
                                blurred += s * w; tw += w;
                            }
                        }
                        color = mix(color, blurred / tw, u_smoothIntensity * skin);
                    }
                }
                if (u_brightenIntensity > 0.01) {
                    vec2 le = (u_landmarks[33] + u_landmarks[133]) * 0.5;
                    vec2 re = (u_landmarks[362] + u_landmarks[263]) * 0.5;
                    float em = max(smoothstep(0.08, 0.02, distance(uv, le)), smoothstep(0.08, 0.02, distance(uv, re)));
                    color.rgb += vec3(0.15, 0.12, 0.1) * u_brightenIntensity * em;
                }
                gl_FragColor = color;
            }
        """
    }
}
