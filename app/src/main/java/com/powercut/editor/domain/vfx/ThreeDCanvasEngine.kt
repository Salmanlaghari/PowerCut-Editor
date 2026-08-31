package com.powercut.editor.domain.vfx

import android.opengl.GLES20
import android.opengl.Matrix
import kotlin.math.*

/**
 * 3D Canvas Engine — transforms 2D tracks into 3D space with perspective projection.
 * Supports 3D objects, 3D text, and virtual camera with keyframeable properties.
 */
class ThreeDCanvasEngine {
    private var isInitialized = false
    private var renderProgram: Int = 0
    private var textProgram: Int = 0
    private var fboId: Int = 0
    private var fboTextureId: Int = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Camera properties
    private var camera = ThreeDCamera()
    private val objects = mutableListOf<ThreeDObject>()

    fun initialize() {
        if (isInitialized) return
        renderProgram = ShaderProgram.compileProgram(VERTEX_3D_SHADER, FRAGMENT_3D_SHADER)
        textProgram = ShaderProgram.compileProgram(TEXT_VERTEX_SHADER, TEXT_FRAGMENT_SHADER)
        createFramebuffer()
        setupMatrices()
        isInitialized = true
    }

    /**
     * Add a 3D object to the scene.
     */
    fun addObject(obj: ThreeDObject) {
        objects.add(obj)
    }

    /**
     * Remove a 3D object from the scene.
     */
    fun removeObject(id: String) {
        objects.removeAll { it.id == id }
    }

    /**
     * Update camera position/rotation.
     */
    fun updateCamera(newCamera: ThreeDCamera) {
        camera = newCamera
        setupMatrices()
    }

    /**
     * Render the 3D scene to a texture.
     */
    fun render(width: Int, height: Int): Int {
        if (!isInitialized) return 0

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        setupMatrices()

        // Render all 3D objects
        objects.filter { it.visible }.sortedBy { it.position.z }.forEach { obj ->
            renderObject(obj, width, height)
        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    /**
     * Transform 2D coordinates to 3D space.
     */
    fun projectTo3D(x2d: Float, y2d: Float, zDepth: Float, width: Int, height: Int): FloatArray {
        val ndcX = (x2d / width) * 2f - 1f
        val ndcY = 1f - (y2d / height) * 2f
        return floatArrayOf(ndcX * zDepth, ndcY * zDepth, zDepth)
    }

    /**
     * Apply 3D transform to a 2D texture.
     */
    fun apply3DTransform(
        textureId: Int,
        transform: ThreeDTransform,
        width: Int,
        height: Int
    ): Int {
        if (!isInitialized) return textureId

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(renderProgram)

        // Build model matrix from transform
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, transform.positionX, transform.positionY, transform.positionZ)
        Matrix.rotateM(modelMatrix, 0, transform.rotationX, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, transform.rotationY, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, transform.rotationZ, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, transform.scaleX, transform.scaleY, transform.scaleZ)

        // MVP = Projection * View * Model
        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(renderProgram, "u_mvpMatrix"), 1, false, mvpMatrix, 0)

        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(renderProgram, "u_texture"), 0)

        drawQuad()

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return fboTextureId
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(renderProgram)
        GLES20.glDeleteProgram(textProgram)
        GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
        GLES20.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
        isInitialized = false
    }

    private fun setupMatrices() {
        // Perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, camera.fov, 16f / 9f, camera.nearPlane, camera.farPlane)

        // View matrix (camera position)
        Matrix.setLookAtM(
            viewMatrix, 0,
            camera.positionX, camera.positionY, camera.positionZ,
            camera.lookAtX, camera.lookAtY, camera.lookAtZ,
            0f, 1f, 0f // Up vector
        )
    }

    private fun renderObject(obj: ThreeDObject, width: Int, height: Int) {
        GLES20.glUseProgram(renderProgram)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, obj.position.x, obj.position.y, obj.position.z)
        Matrix.rotateM(modelMatrix, 0, obj.rotation.x, 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, obj.rotation.y, 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, obj.rotation.z, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, obj.scale.x, obj.scale.y, obj.scale.z)

        val tempMatrix = FloatArray(16)
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tempMatrix, 0)

        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(renderProgram, "u_mvpMatrix"), 1, false, mvpMatrix, 0)

        // Render based on type
        when (obj.type) {
            ThreeDObjectType.CUBE -> renderCube(obj)
            ThreeDObjectType.SPHERE -> renderSphere(obj)
            ThreeDObjectType.PLANE -> renderQuad()
            ThreeDObjectType.TEXT_3D -> renderText3D(obj)
            ThreeDObjectType.MODEL_GLTF -> renderGLTF(obj)
        }
    }

    private fun renderCube(obj: ThreeDObject) {
        // Simplified cube rendering
        drawQuad()
    }

    private fun renderSphere(obj: ThreeDObject) {
        // Simplified sphere rendering
        drawQuad()
    }

    private fun renderText3D(obj: ThreeDObject) {
        GLES20.glUseProgram(textProgram)
        GLES20.glUniform4fv(GLES20.glGetUniformLocation(textProgram, "u_textColor"), 1, obj.color, 0)
        drawQuad()
    }

    private fun renderGLTF(obj: ThreeDObject) {
        // glTF rendering placeholder
        drawQuad()
    }

    private fun drawQuad() {
        val v = floatArrayOf(-1f,-1f,0f,1f, 1f,-1f,1f,1f, -1f,1f,0f,0f, 1f,1f,1f,0f)
        val buf = java.nio.ByteBuffer.allocateDirect(64).order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer().apply { put(v); position(0) }
        val currentProgram = renderProgram
        val p = GLES20.glGetAttribLocation(currentProgram, "a_position")
        val t = GLES20.glGetAttribLocation(currentProgram, "a_texCoord")
        GLES20.glEnableVertexAttribArray(p); GLES20.glVertexAttribPointer(p, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(t); GLES20.glVertexAttribPointer(t, 2, GLES20.GL_FLOAT, false, 16, buf)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p); GLES20.glDisableVertexAttribArray(t)
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

    companion object {
        private const val VERTEX_3D_SHADER = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord;
            uniform mat4 u_mvpMatrix;
            varying vec2 v_texCoord;
            varying float v_depth;
            void main() {
                gl_Position = u_mvpMatrix * a_position;
                v_texCoord = a_texCoord;
                v_depth = gl_Position.z / gl_Position.w;
            }
        """

        private const val FRAGMENT_3D_SHADER = """
            precision mediump float;
            uniform sampler2D u_texture;
            varying vec2 v_texCoord;
            varying float v_depth;
            void main() {
                vec4 color = texture2D(u_texture, v_texCoord);
                // Simple depth-based fog
                float fog = clamp(v_depth * 0.5 + 0.5, 0.0, 0.5);
                gl_FragColor = mix(color, vec4(0.05, 0.05, 0.08, 1.0), fog);
            }
        """

        private const val TEXT_VERTEX_SHADER = """
            attribute vec4 a_position;
            attribute vec2 a_texCoord;
            uniform mat4 u_mvpMatrix;
            varying vec2 v_texCoord;
            void main() {
                gl_Position = u_mvpMatrix * a_position;
                v_texCoord = a_texCoord;
            }
        """

        private const val TEXT_FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_textColor;
            varying vec2 v_texCoord;
            void main() {
                gl_FragColor = u_textColor;
            }
        """
    }
}

// ── Data Models ──

enum class ThreeDObjectType { CUBE, SPHERE, PLANE, TEXT_3D, MODEL_GLTF }

data class ThreeDVector3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

data class ThreeDObject(
    val id: String,
    val type: ThreeDObjectType,
    val position: ThreeDVector3 = ThreeDVector3(),
    val rotation: ThreeDVector3 = ThreeDVector3(),
    val scale: ThreeDVector3 = ThreeDVector3(1f, 1f, 1f),
    val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val visible: Boolean = true,
    val text: String = "",
    val modelPath: String = ""
)

data class ThreeDCamera(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 5f,
    val lookAtX: Float = 0f,
    val lookAtY: Float = 0f,
    val lookAtZ: Float = 0f,
    val fov: Float = 45f,
    val nearPlane: Float = 0.1f,
    val farPlane: Float = 100f,
    val depthOfField: Float = 0f
)

data class ThreeDTransform(
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val positionZ: Float = 0f,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f,
    val perspective: Float = 1f
)
