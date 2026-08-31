package com.powercut.editor.domain.vfx

import android.opengl.GLES20
import kotlin.math.*

/**
 * GPU Particle Engine — real-time particle shaders for Fire, Smoke, Neon Dust, Light Rays, and Lens Flares.
 */
class ParticleEngine {
    private var isInitialized = false
    private var particleProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private val emitters = mutableListOf<ParticleEmitter>()

    fun initialize() {
        if (isInitialized) return
        particleProgram = ShaderProgram.compileProgram(VERTEX_SHADER, PARTICLE_FRAGMENT_SHADER)
        createFramebuffer()
        isInitialized = true
    }

    /**
     * Add a particle emitter to the scene.
     */
    fun addEmitter(emitter: ParticleEmitter) {
        emitters.add(emitter)
    }

    /**
     * Remove a particle emitter.
     */
    fun removeEmitter(id: String) {
        emitters.removeAll { it.id == id }
    }

    /**
     * Render all particle effects.
     */
    fun render(timeMs: Long, width: Int, height: Int): Int {
        if (!isInitialized) return 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)

        GLES20.glUseProgram(particleProgram)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(particleProgram, "u_time"), timeMs / 1000f)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(particleProgram, "u_resolution"), width.toFloat(), height.toFloat())

        emitters.filter { it.active }.forEach { emitter ->
            renderEmitter(emitter, timeMs)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    private fun renderEmitter(emitter: ParticleEmitter, timeMs: Long) {
        GLES20.glUniform3fv(GLES20.glGetUniformLocation(particleProgram, "u_emitterPos"), 1,
            floatArrayOf(emitter.position.x, emitter.position.y, emitter.position.z), 0)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(particleProgram, "u_emitterLife"),
            ((timeMs - emitter.startTimeMs).toFloat() / emitter.lifetimeMs).coerceIn(0f, 1f))
        GLES20.glUniform1f(GLES20.glGetUniformLocation(particleProgram, "u_emitterIntensity"), emitter.intensity)
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(particleProgram, "u_particleColor"), 1, emitter.color, 0)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(particleProgram, "u_particleType"), emitter.type.shaderId)

        drawQuad()
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(particleProgram)
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
        val p = GLES20.glGetAttribLocation(particleProgram, "a_position")
        val t = GLES20.glGetAttribLocation(particleProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"

        private const val PARTICLE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform float u_time;
            uniform vec2 u_resolution;
            uniform vec3 u_emitterPos;
            uniform float u_emitterLife;
            uniform float u_emitterIntensity;
            uniform vec4 u_particleColor;
            uniform int u_particleType;
            varying vec2 v_texCoord;

            // Hash function for randomness
            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            // Fire particles
            vec4 fireEffect(vec2 uv, vec2 center, float time) {
                vec2 d = uv - center;
                float dist = length(d);
                float flame = 0.0;
                for (int i = 0; i < 5; i++) {
                    float fi = float(i);
                    float speed = 1.0 + fi * 0.5;
                    float size = 0.1 - fi * 0.015;
                    float flicker = sin(time * speed * 5.0 + fi * 2.0) * 0.5 + 0.5;
                    vec2 offset = vec2(sin(time * 2.0 + fi) * 0.02, time * speed * 0.3);
                    float n = hash(uv * 10.0 + time + fi);
                    flame += smoothstep(size, 0.0, dist + n * 0.02) * flicker;
                }
                vec3 fireColor = mix(vec3(1.0, 0.3, 0.0), vec3(1.0, 0.8, 0.2), flame);
                return vec4(fireColor * flame * u_emitterIntensity, flame);
            }

            // Smoke particles
            vec4 smokeEffect(vec2 uv, vec2 center, float time) {
                vec2 d = uv - center;
                float dist = length(d);
                float smoke = 0.0;
                for (int i = 0; i < 4; i++) {
                    float fi = float(i);
                    float drift = sin(time + fi * 1.5) * 0.03;
                    float rise = time * 0.2 * (1.0 + fi * 0.3);
                    vec2 offset = vec2(drift, rise);
                    float n = hash(uv * 8.0 + time * 0.5 + fi);
                    smoke += smoothstep(0.15 - fi * 0.02, 0.0, dist + n * 0.03);
                }
                return vec4(vec3(0.3, 0.3, 0.35) * smoke * u_emitterIntensity, smoke * 0.6);
            }

            // Neon dust particles
            vec4 neonDustEffect(vec2 uv, float time) {
                float dust = 0.0;
                for (int i = 0; i < 20; i++) {
                    float fi = float(i);
                    vec2 pos = vec2(
                        hash(vec2(fi, 0.0)) + sin(time * 0.5 + fi * 0.7) * 0.1,
                        hash(vec2(0.0, fi)) + cos(time * 0.3 + fi * 0.9) * 0.1
                    );
                    float size = 0.005 + hash(vec2(fi, fi)) * 0.005;
                    dust += smoothstep(size, 0.0, length(uv - pos));
                }
                vec3 color = mix(u_particleColor.rgb, vec3(0.0, 1.0, 0.8), sin(time) * 0.5 + 0.5);
                return vec4(color * dust * u_emitterIntensity, dust);
            }

            // Light rays
            vec4 lightRaysEffect(vec2 uv, vec2 center, float time) {
                vec2 d = uv - center;
                float angle = atan(d.y, d.x);
                float dist = length(d);
                float rays = 0.0;
                for (int i = 0; i < 8; i++) {
                    float fi = float(i);
                    float rayAngle = fi * 3.14159 / 4.0 + time * 0.3;
                    float rayWidth = 0.1;
                    rays += smoothstep(rayWidth, 0.0, abs(angle - rayAngle)) * smoothstep(1.0, 0.0, dist);
                }
                return vec4(vec3(1.0, 0.95, 0.8) * rays * u_emitterIntensity, rays * 0.5);
            }

            // Anamorphic lens flare
            vec4 lensFlareEffect(vec2 uv, vec2 center, float time) {
                vec2 d = uv - center;
                float dist = length(d);
                float flare = 0.0;
                // Horizontal streak
                flare += smoothstep(0.5, 0.0, abs(d.y)) * smoothstep(1.0, 0.0, abs(d.x)) * 0.3;
                // Central glow
                flare += smoothstep(0.2, 0.0, dist) * 0.5;
                // Rainbow artifacts
                vec3 rainbow = vec3(
                    sin(d.x * 20.0 + time) * 0.5 + 0.5,
                    sin(d.x * 20.0 + time + 2.094) * 0.5 + 0.5,
                    sin(d.x * 20.0 + time + 4.189) * 0.5 + 0.5
                );
                return vec4(rainbow * flare * u_emitterIntensity, flare * 0.4);
            }

            void main() {
                vec2 uv = v_texCoord;
                vec2 center = vec2(0.5);
                vec4 result = vec4(0.0);

                if (u_particleType == 0) { result = fireEffect(uv, center, u_time);
                } else if (u_particleType == 1) { result = smokeEffect(uv, center, u_time);
                } else if (u_particleType == 2) { result = neonDustEffect(uv, u_time);
                } else if (u_particleType == 3) { result = lightRaysEffect(uv, center, u_time);
                } else if (u_particleType == 4) { result = lensFlareEffect(uv, center, u_time);
                }

                gl_FragColor = result;
            }
        """
    }
}

// ── Data Models ──

enum class ParticleType(val displayName: String, val shaderId: Int) {
    FIRE("Fire", 0),
    SMOKE("Smoke", 1),
    NEON_DUST("Neon Dust", 2),
    LIGHT_RAYS("Light Rays", 3),
    LENS_FLARE("Lens Flare", 4)
}

data class ParticleEmitter(
    val id: String,
    val type: ParticleType,
    val position: ThreeDVector3 = ThreeDVector3(),
    val color: FloatArray = floatArrayOf(1f, 0.5f, 0f, 1f),
    val intensity: Float = 1f,
    val lifetimeMs: Long = 2000,
    val startTimeMs: Long = 0,
    val active: Boolean = true
)
