package com.powercut.editor.domain.vfx

import android.opengl.GLES20

/**
 * Advanced 3D & AI VFX Transitions Engine.
 * GPU-accelerated transitions: 3D Cube Flip, Sphere Warp, Page Turn, Prism Shift, Zoom Tunnel, etc.
 */
class VFXTransitionEngine {
    private var isInitialized = false
    private var transitionProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0

    fun initialize() {
        if (isInitialized) return
        transitionProgram = ShaderProgram.compileProgram(VERTEX_SHADER, TRANSITION_FRAGMENT_SHADER)
        createFramebuffer()
        isInitialized = true
    }

    /**
     * Apply a VFX transition between two frames.
     */
    fun applyTransition(
        fromTexture: Int,
        toTexture: Int,
        transition: VFXTransition,
        progress: Float, // 0-1
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized) return fromTexture
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(transitionProgram)

        // Bind source textures
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fromTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(transitionProgram, "u_fromTexture"), 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, toTexture)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(transitionProgram, "u_toTexture"), 1)

        // Pass transition parameters
        GLES20.glUniform1f(GLES20.glGetUniformLocation(transitionProgram, "u_progress"), progress)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(transitionProgram, "u_transitionType"), transition.shaderId)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(transitionProgram, "u_resolution"), width.toFloat(), height.toFloat())

        // Transition-specific uniforms
        transition.uniforms.forEach { (name, value) ->
            when (value) {
                is Float -> GLES20.glUniform1f(GLES20.glGetUniformLocation(transitionProgram, name), value)
                is FloatArray -> GLES20.glUniform2fv(GLES20.glGetUniformLocation(transitionProgram, name), 1, value, 0)
            }
        }

        drawQuad()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(transitionProgram)
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
        val p = GLES20.glGetAttribLocation(transitionProgram, "a_position")
        val t = GLES20.glGetAttribLocation(transitionProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"

        private const val TRANSITION_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_fromTexture;
            uniform sampler2D u_toTexture;
            uniform float u_progress;
            uniform int u_transitionType;
            uniform vec2 u_resolution;
            varying vec2 v_texCoord;

            // 3D Cube Flip
            vec4 cubeFlip(vec2 uv, float t) {
                float angle = t * 3.14159;
                vec3 normal = vec3(0.0, 0.0, 1.0);
                vec3 rotatedNormal = vec3(sin(angle), 0.0, cos(angle));

                if (rotatedNormal.z > 0.0) {
                    vec2 transformedUV = uv;
                    transformedUV.x = (transformedUV.x - 0.5) / cos(angle) + 0.5;
                    return texture2D(u_fromTexture, transformedUV);
                } else {
                    vec2 transformedUV = uv;
                    transformedUV.x = (transformedUV.x - 0.5) / cos(3.14159 - angle) + 0.5;
                    return texture2D(u_toTexture, transformedUV);
                }
            }

            // Sphere Warp
            vec4 sphereWarp(vec2 uv, float t) {
                vec2 center = uv - 0.5;
                float dist = length(center);
                float radius = 0.5 * (1.0 - t);
                if (dist < radius) {
                    float z = sqrt(radius * radius - dist * dist);
                    vec2 sphereUV = center * (z / radius) + 0.5;
                    return mix(texture2D(u_fromTexture, sphereUV), texture2D(u_toTexture, sphereUV), t);
                }
                return texture2D(u_toTexture, uv);
            }

            // Page Turn
            vec4 pageTurn(vec2 uv, float t) {
                float fold = 1.0 - t;
                if (uv.x > fold) {
                    vec2 turnedUV = vec2(2.0 * fold - uv.x, uv.y);
                    turnedUV.x = clamp(turnedUV.x, 0.0, 1.0);
                    return texture2D(u_toTexture, turnedUV);
                }
                return texture2D(u_fromTexture, uv);
            }

            // Prism Shift
            vec4 prismShift(vec2 uv, float t) {
                float offset = t * 0.05;
                float r = texture2D(u_toTexture, uv + vec2(offset, 0.0)).r;
                float g = texture2D(u_toTexture, uv).g;
                float b = texture2D(u_toTexture, uv - vec2(offset, 0.0)).b;
                return mix(texture2D(u_fromTexture, uv), vec4(r, g, b, 1.0), t);
            }

            // Zoom Tunnel
            vec4 zoomTunnel(vec2 uv, float t) {
                vec2 center = uv - 0.5;
                float dist = length(center);
                float angle = atan(center.y, center.x);
                float zoom = 1.0 + t * 3.0;
                vec2 tunnelUV = vec2(cos(angle) * dist * zoom, sin(angle) * dist * zoom) + 0.5;
                tunnelUV = clamp(tunnelUV, 0.0, 1.0);
                return mix(texture2D(u_fromTexture, uv), texture2D(u_toTexture, tunnelUV), t);
            }

            // Whip Pan
            vec4 whipPan(vec2 uv, float t) {
                float blur = abs(t - 0.5) * 0.1;
                vec2 offset = vec2((t - 0.5) * 2.0, 0.0);
                vec4 from = texture2D(u_fromTexture, uv - offset);
                vec4 to = texture2D(u_toTexture, uv + offset);
                return mix(from, to, smoothstep(0.3, 0.7, t));
            }

            // RGB Split Glitch
            vec4 rgbSplitGlitch(vec2 uv, float t) {
                float split = t * 0.05 * sin(t * 20.0);
                float r = texture2D(u_toTexture, uv + vec2(split, 0.0)).r;
                float g = texture2D(u_toTexture, uv).g;
                float b = texture2D(u_toTexture, uv - vec2(split, 0.0)).b;
                return mix(texture2D(u_fromTexture, uv), vec4(r, g, b, 1.0), t);
            }

            void main() {
                vec2 uv = v_texCoord;
                vec4 result;

                if (u_transitionType == 0) { result = cubeFlip(uv, u_progress);
                } else if (u_transitionType == 1) { result = sphereWarp(uv, u_progress);
                } else if (u_transitionType == 2) { result = pageTurn(uv, u_progress);
                } else if (u_transitionType == 3) { result = prismShift(uv, u_progress);
                } else if (u_transitionType == 4) { result = zoomTunnel(uv, u_progress);
                } else if (u_transitionType == 5) { result = whipPan(uv, u_progress);
                } else if (u_transitionType == 6) { result = rgbSplitGlitch(uv, u_progress);
                } else { result = mix(texture2D(u_fromTexture, uv), texture2D(u_toTexture, uv), u_progress); }

                gl_FragColor = result;
            }
        """
    }
}

// ── Transition Definitions ──

data class VFXTransition(
    val id: String,
    val name: String,
    val category: TransitionCategory,
    val shaderId: Int,
    val durationMs: Long = 500,
    val easing: String = "EASE_IN_OUT",
    val uniforms: Map<String, Any> = emptyMap()
)

enum class TransitionCategory(val displayName: String) {
    THREE_D("3D Transitions"),
    OPTICAL_FLOW("Optical Flow"),
    MOTION_BLUR("Motion Blur"),
    GLITCH("Glitch & FX"),
    COLOR("Color Effects")
}

object VFXTransitionPresets {
    val transitions = listOf(
        // 3D Transitions
        VFXTransition("cube_flip", "Cube Flip", TransitionCategory.THREE_D, 0),
        VFXTransition("sphere_warp", "Sphere Warp", TransitionCategory.THREE_D, 1),
        VFXTransition("page_turn", "Page Turn", TransitionCategory.THREE_D, 2),
        VFXTransition("prism_shift", "Prism Shift", TransitionCategory.THREE_D, 3),
        VFXTransition("zoom_tunnel", "Zoom Tunnel", TransitionCategory.THREE_D, 4),

        // Optical Flow & Motion Blur
        VFXTransition("whip_pan", "Whip Pan", TransitionCategory.OPTICAL_FLOW, 5),
        VFXTransition("speed_warp", "Speed Warp", TransitionCategory.MOTION_BLUR, 5,
            uniforms = mapOf("u_blurAmount" to 0.05f)),

        // Glitch & FX
        VFXTransition("rgb_split", "RGB Color Split", TransitionCategory.GLITCH, 6),
        VFXTransition("glitch_displace", "Glitch Displace", TransitionCategory.GLITCH, 6,
            uniforms = mapOf("u_glitchIntensity" to 0.3f))
    )

    fun getByCategory(category: TransitionCategory): List<VFXTransition> {
        return transitions.filter { it.category == category }
    }
}
