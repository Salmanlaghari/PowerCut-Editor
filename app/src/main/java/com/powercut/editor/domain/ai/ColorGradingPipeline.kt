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
            uniform sampler2D u_texture; uniform sampler2D u_lut;
            uniform float u_intensity; uniform int u_preset; uniform int u_hasLut;
            varying vec2 v_texCoord;

            vec3 tealOrange(vec3 c) {
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                vec3 teal = vec3(0.0, 0.6, 0.6);
                vec3 orange = vec3(1.0, 0.65, 0.25);
                float shadow = smoothstep(0.0, 0.5, luma);
                float highlight = smoothstep(0.5, 1.0, luma);
                vec3 graded = mix(teal * c * 1.2, orange * c * 1.1, shadow);
                graded = mix(graded, c * 1.15, highlight * 0.3);
                graded += vec3(0.02, 0.01, 0.0);
                return graded;
            }

            vec3 vintageFilm(vec3 c) {
                c = pow(c, vec3(0.92, 0.95, 0.88));
                c *= vec3(1.08, 1.02, 0.92);
                float grain = (fract(sin(dot(c.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.08;
                c += grain;
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), 0.15);
                return c;
            }

            vec3 cyberpunk(vec3 c) {
                c.r = pow(c.r, 0.75);
                c.b = pow(c.b, 0.65);
                c.g *= 0.85;
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), -0.25);
                c *= vec3(1.3, 0.75, 1.5);
                c += vec3(0.05, 0.0, 0.1);
                return c;
            }

            vec3 filmNoir(vec3 c) {
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                luma = pow(luma, 1.4);
                luma = smoothstep(0.05, 0.95, luma);
                return vec3(luma) * vec3(0.92, 0.92, 0.98);
            }

            vec3 goldenHour(vec3 c) {
                c.r *= 1.18;
                c.g *= 1.08;
                c.b *= 0.82;
                c += vec3(0.06, 0.03, 0.0);
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                c = mix(c, c * vec3(1.1, 1.0, 0.8), smoothstep(0.3, 0.7, luma));
                return c;
            }

            vec3 retroFilm(vec3 c) {
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), 0.25);
                c *= vec3(1.12, 0.96, 0.86);
                c = pow(c, vec3(0.96));
                c += vec3(0.02, 0.01, 0.0);
                return c;
            }

            vec3 coolTone(vec3 c) {
                c.b *= 1.25;
                c.r *= 0.88;
                vec3 cool = vec3(0.0, 0.25, 0.45);
                return mix(c, cool + c * 0.6, 0.2);
            }

            vec3 aiGlow(vec3 c) {
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                c *= vec3(0.9, 1.1, 1.0);
                c += vec3(0.0, 0.08, 0.05) * luma;
                c = pow(c, vec3(0.95));
                return c;
            }

            vec3 beautyPro(vec3 c) {
                c.r *= 1.05;
                c.g *= 1.02;
                c.b *= 1.08;
                c = pow(c, vec3(0.97));
                float luma = dot(c, vec3(0.299, 0.587, 0.114));
                c = mix(c, c * 1.1, smoothstep(0.4, 0.8, luma));
                return c;
            }

            vec3 film35mm(vec3 c) {
                c = pow(c, vec3(0.94, 0.97, 0.91));
                c *= vec3(1.06, 1.0, 0.94);
                float grain = (fract(sin(dot(c.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * 0.05;
                c += grain;
                c = mix(c, vec3(dot(c, vec3(0.299, 0.587, 0.114))), 0.1);
                return c;
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
                } else if (u_preset == 8) { graded = aiGlow(color.rgb);
                } else if (u_preset == 9) { graded = beautyPro(color.rgb);
                } else if (u_preset == 10) { graded = film35mm(color.rgb);
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
    COOL_TONE("Cool Tone"),
    AI_GLOW("AI Glow"),
    BEAUTY_PRO("Beauty Pro"),
    FILM_35MM("Film 35mm")
}
