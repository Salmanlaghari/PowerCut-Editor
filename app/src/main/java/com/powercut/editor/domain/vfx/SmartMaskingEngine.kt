package com.powercut.editor.domain.vfx

import android.opengl.GLES20

/**
 * Precision Smart Masking Engine.
 * Shape Masking (Circle, Rectangle, Linear Gradient) + Freehand AI Brush Masking
 * with inverted mask toggles and feather controls.
 */
class SmartMaskingEngine {
    private var isInitialized = false
    private var maskProgram: Int = 0
    private var brushProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0
    private var maskTextureId: Int = 0
    private val masks = mutableListOf<SmartMask>()

    fun initialize() {
        if (isInitialized) return
        maskProgram = ShaderProgram.compileProgram(VERTEX_SHADER, MASK_FRAGMENT_SHADER)
        brushProgram = ShaderProgram.compileProgram(VERTEX_SHADER, BRUSH_FRAGMENT_SHADER)
        createFramebuffers()
        isInitialized = true
    }

    /**
     * Add a shape mask.
     */
    fun addShapeMask(shape: MaskShape, feather: Float = 0.1f, inverted: Boolean = false): SmartMask {
        val mask = SmartMask(
            id = "mask_${System.currentTimeMillis()}",
            type = MaskType.SHAPE,
            shape = shape,
            feather = feather,
            inverted = inverted
        )
        masks.add(mask)
        return mask
    }

    /**
     * Add a freehand brush mask.
     */
    fun addBrushMask(strokePoints: List<Pair<Float, Float>>, feather: Float = 0.05f, inverted: Boolean = false): SmartMask {
        val mask = SmartMask(
            id = "mask_${System.currentTimeMillis()}",
            type = MaskType.BRUSH,
            strokePoints = strokePoints,
            feather = feather,
            inverted = inverted
        )
        masks.add(mask)
        return mask
    }

    /**
     * Remove a mask.
     */
    fun removeMask(id: String) {
        masks.removeAll { it.id == id }
    }

    /**
     * Apply all masks to a texture.
     */
    fun applyMasks(
        textureId: Int,
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized || masks.isEmpty()) return textureId
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)

        var currentTexture = textureId

        masks.filter { it.active }.forEach { mask ->
            GLES20.glUseProgram(if (mask.type == MaskType.SHAPE) maskProgram else brushProgram)

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, currentTexture)
            val program = if (mask.type == MaskType.SHAPE) maskProgram else brushProgram
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_texture"), 0)

            // Pass mask parameters
            GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_feather"), mask.feather)
            GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_inverted"), if (mask.inverted) 1 else 0)

            when (mask.shape) {
                is MaskShape.Circle -> {
                    GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_shapeType"), 0)
                    GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_shapeCenter"),
                        mask.shape.centerX, mask.shape.centerY)
                    GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_shapeRadius"), mask.shape.radius)
                }
                is MaskShape.Rectangle -> {
                    GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_shapeType"), 1)
                    GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_shapeCenter"),
                        mask.shape.centerX, mask.shape.centerY)
                    GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_shapeSize"),
                        mask.shape.width, mask.shape.height)
                    GLES20.glUniform1f(GLES20.glGetUniformLocation(program, "u_shapeRotation"), mask.shape.rotation)
                }
                is MaskShape.LinearGradient -> {
                    GLES20.glUniform1i(GLES20.glGetUniformLocation(program, "u_shapeType"), 2)
                    GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_gradientStart"), mask.shape.startX, mask.shape.startY)
                    GLES20.glUniform2f(GLES20.glGetUniformLocation(program, "u_gradientEnd"), mask.shape.endX, mask.shape.endY)
                }
                else -> {}
            }

            drawQuad(program)
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    /**
     * Render brush stroke to mask texture.
     */
    fun renderBrushMask(
        strokePoints: List<Pair<Float, Float>>,
        brushSize: Float,
        width: Int,
        height: Int
    ) {
        if (!isInitialized) return
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(brushProgram)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUniform1f(GLES20.glGetUniformLocation(brushProgram, "u_brushSize"), brushSize)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(brushProgram, "u_isBrushStroke"), 1)

        // Render each stroke point
        strokePoints.forEach { (x, y) ->
            GLES20.glUniform2f(GLES20.glGetUniformLocation(brushProgram, "u_brushPos"), x, y)
            drawQuad(brushProgram)
        }

        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(maskProgram)
        GLES20.glDeleteProgram(brushProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(2, intArrayOf(fboTextureId, maskTextureId), 0)
        isInitialized = false
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

    private fun drawQuad(program: Int) {
        val v = floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)
        val buf = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(v); position(0) }
        val p = GLES20.glGetAttribLocation(program, "a_position")
        val t = GLES20.glGetAttribLocation(program, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
    }

    companion object {
        private const val VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"

        private const val MASK_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform float u_feather;
            uniform int u_inverted;
            uniform int u_shapeType;
            uniform vec2 u_shapeCenter;
            uniform float u_shapeRadius;
            uniform vec2 u_shapeSize;
            uniform float u_shapeRotation;
            uniform vec2 u_gradientStart;
            uniform vec2 u_gradientEnd;
            varying vec2 v_texCoord;

            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                float mask = 0.0;

                if (u_shapeType == 0) {
                    // Circle
                    float dist = distance(v_texCoord, u_shapeCenter);
                    mask = 1.0 - smoothstep(u_shapeRadius - u_feather, u_shapeRadius + u_feather, dist);
                } else if (u_shapeType == 1) {
                    // Rectangle
                    vec2 d = v_texCoord - u_shapeCenter;
                    float angle = radians(u_shapeRotation);
                    vec2 rotated = vec2(d.x * cos(angle) - d.y * sin(angle), d.x * sin(angle) + d.y * cos(angle));
                    vec2 halfSize = u_shapeSize * 0.5;
                    mask = 1.0 - smoothstep(0.0, u_feather, max(abs(rotated.x) - halfSize.x, abs(rotated.y) - halfSize.y));
                } else if (u_shapeType == 2) {
                    // Linear gradient
                    vec2 dir = u_gradientEnd - u_gradientStart;
                    float len = length(dir);
                    vec2 norm = dir / len;
                    float t = dot(v_texCoord - u_gradientStart, norm) / len;
                    mask = smoothstep(-u_feather, 1.0 + u_feather, t);
                }

                if (u_inverted == 1) mask = 1.0 - mask;

                gl_FragColor = vec4(color.rgb, color.a * mask);
            }
        """

        private const val BRUSH_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            uniform float u_feather;
            uniform int u_inverted;
            uniform float u_brushSize;
            uniform vec2 u_brushPos;
            uniform int u_isBrushStroke;
            varying vec2 v_texCoord;

            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                float mask = 0.0;

                if (u_isBrushStroke == 1) {
                    float dist = distance(v_texCoord, u_brushPos);
                    mask = smoothstep(u_brushSize, u_brushSize - u_feather, dist);
                } else {
                    mask = color.a;
                }

                if (u_inverted == 1) mask = 1.0 - mask;

                gl_FragColor = vec4(color.rgb, mask);
            }
        """
    }
}

// ── Data Models ──

enum class MaskType { SHAPE, BRUSH }

sealed class MaskShape {
    data class Circle(val centerX: Float, val centerY: Float, val radius: Float) : MaskShape()
    data class Rectangle(val centerX: Float, val centerY: Float, val width: Float, val height: Float, val rotation: Float = 0f) : MaskShape()
    data class LinearGradient(val startX: Float, val startY: Float, val endX: Float, val endY: Float) : MaskShape()
}

data class SmartMask(
    val id: String,
    val type: MaskType,
    val shape: MaskShape? = null,
    val strokePoints: List<Pair<Float, Float>> = emptyList(),
    val feather: Float = 0.1f,
    val inverted: Boolean = false,
    val active: Boolean = true
)
