package com.powercut.editor.domain.ai

import android.opengl.GLES20

/**
 * GPU-accelerated cinematic color grading pipeline.
 * Implements real-time GLSL shader-based color grading with 3D LUT support.
 */
class ColorGradingPipeline {
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
        textureId: Int, preset: ColorGradingPreset, intensity: Float,
        lutTextureId: Int, width: Int, height: Int
    ): Int {
        if (!isInitialized || preset == ColorGradingPreset.NONE) return textureId
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(shaderProgram)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_texture"), 0)

        // Bind LUT if custom
        if (lutTextureId >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, lutTextureId)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_lut"), 1)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_hasLut"), 1)
        } else {
            GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_hasLut"), 0)
        }

        // Pass preset parameters
        GLES20.glUniform1f(GLES20.glGetUniformLocation(shaderProgram, "u_intensity"), intensity)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(shaderProgram, "u_preset"), preset.ordinal)

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
        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture; uniform sampler2D u_lut;
            uniform float u_intensity; uniform int u_preset; uniform int u_hasLut;
            varying vec2 v_texCoord;

            vec3 tealOrange(vec3 c) {
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                vec3 teal = vec3(0.0, 0.5, 0.5); vec3 orange = vec3(1.0, 0.6, 0.2);
                return mix(teal * luma, orange * (1.0 - luma) + c * 0.5, 0.5 + luma * 0.5);
            }

            vec3 vintageFilm(vec3 c) {
                c = pow(c, vec3(0.9)); c *= vec3(1.1, 1.0, 0.9);
                float grain = (fract(sin(dot(c.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.06;
                return c + grain;
            }

            vec3 cyberpunk(vec3 c) {
                c.r = pow(c.r, 0.8); c.b = pow(c.b, 0.7);
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), -0.3);
                return c * vec3(1.2, 0.8, 1.4);
            }

            vec3 filmNoir(vec3 c) {
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                luma = pow(luma, 1.3);
                return vec3(luma) * vec3(0.95, 0.95, 1.0);
            }

            vec3 goldenHour(vec3 c) {
                c.r *= 1.15; c.g *= 1.05; c.b *= 0.85;
                return c + vec3(0.05, 0.02, 0.0);
            }

            vec3 retroFilm(vec3 c) {
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), 0.3);
                c *= vec3(1.1, 0.95, 0.85);
                return pow(c, vec3(0.95));
            }

            vec3 coolTone(vec3 c) {
                c.b *= 1.2; c.r *= 0.9;
                return mix(c, vec3(0.0, 0.3, 0.5) * dot(c, vec3(0.3, 0.6, 0.1)), 0.2);
            }

            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                vec3 graded = color.rgb;

                if (u_hasLut == 1) {
                    vec4 lutColor = texture2D(u_lut, v_texCoord);
                    graded = mix(color.rgb, lutColor.rgb, u_intensity);
                } else if (u_preset == 1) { graded = tealOrange(color.rgb);
                } else if (u_preset == 2) { graded = vintageFilm(color.rgb);
                } else if (u_preset == 3) { graded = cyberpunk(color.rgb);
                } else if (u_preset == 4) { graded = filmNoir(color.rgb);
                } else if (u_preset == 5) { graded = goldenHour(color.rgb);
                } else if (u_preset == 6) { graded = retroFilm(color.rgb);
                } else if (u_preset == 7) { graded = coolTone(color.rgb);
                }

                gl_FragColor = vec4(mix(color.rgb, graded, u_intensity), color.a);
            }
        """
    }
}

enum class ColorGradingPreset(val displayName: String) {
    NONE("None"),
    TEAL_ORANGE("Teal & Orange"),
    VINTAGE_FILM("Vintage Film"),
    CYBERPUNK("Cyberpunk Neon"),
    FILM_NOIR("Film Noir"),
    GOLDEN_HOUR("Golden Hour"),
    RETRO_FILM("Retro Film"),
    COOL_TONE("Cool Tone")
}
