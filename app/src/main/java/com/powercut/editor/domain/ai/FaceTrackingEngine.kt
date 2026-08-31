package com.powercut.editor.domain.ai

import android.graphics.PointF
import android.opengl.GLES20

/**
 * Real-time facial landmark detection engine using MediaPipe Face Mesh.
 * Provides 468 3D face landmarks for beauty FX, face reshaping, and AR mask positioning.
 */
class FaceTrackingEngine {

    private var isInitialized = false
    private var faceMeshFilter: Int = 0

    private external fun nativeInitialize()
    private external fun nativeDetectLandmarks(textureId: Int, width: Int, height: Int): FloatArray?
    private external fun nativeRelease()

    fun initialize() {
        if (isInitialized) return
        faceMeshFilter = ShaderProgram.compileProgram(
            vertexShaderSource = FACE_MESH_VERTEX_SHADER,
            fragmentShaderSource = FACE_MESH_FRAGMENT_SHADER
        )
        try { nativeInitialize() } catch (_: UnsatisfiedLinkError) {}
        isInitialized = true
    }

    fun detectLandmarks(textureId: Int, width: Int, height: Int): List<FaceLandmark>? {
        if (!isInitialized) return null
        return try {
            val raw = nativeDetectLandmarks(textureId, width, height)
            if (raw != null && raw.size >= 468 * 3) {
                (0 until 468).map { i ->
                    FaceLandmark(raw[i * 3], raw[i * 3 + 1], raw[i * 3 + 2], i)
                }
            } else null
        } catch (_: Exception) { null }
    }

    fun getFeatureRegions(landmarks: List<FaceLandmark>): FaceFeatureRegions {
        fun pts(indices: List<Int>) = landmarks.filter { it.index in indices }.map { PointF(it.x, it.y) }
        return FaceFeatureRegions(
            leftEye = pts(LEFT_EYE_INDICES), rightEye = pts(RIGHT_EYE_INDICES),
            lips = pts(LIPS_INDICES), nose = pts(NOSE_INDICES),
            faceContour = pts(FACE_CONTOUR_INDICES), forehead = pts(FOREHEAD_INDICES)
        )
    }

    fun release() {
        if (!isInitialized) return
        GLES20.glDeleteProgram(faceMeshFilter)
        try { nativeRelease() } catch (_: Exception) {}
        isInitialized = false
    }

    companion object {
        private val LEFT_EYE_INDICES = listOf(33, 7, 163, 144, 145, 153, 154, 155, 133, 173, 157, 158, 159, 160, 161, 246)
        private val RIGHT_EYE_INDICES = listOf(362, 382, 381, 380, 374, 373, 390, 249, 263, 466, 388, 387, 386, 385, 384, 398)
        private val LIPS_INDICES = listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185)
        private val NOSE_INDICES = listOf(1, 2, 98, 327, 4, 5, 195, 54, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142)
        private val FACE_CONTOUR_INDICES = listOf(10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109)
        private val FOREHEAD_INDICES = listOf(10, 151, 9, 8, 107, 336, 296, 334, 293, 300, 283, 282, 295, 285)
        private const val FACE_MESH_VERTEX_SHADER = "attribute vec4 a_position; attribute vec2 a_texCoord; varying vec2 v_texCoord; void main(){gl_Position=a_position;v_texCoord=a_texCoord;}"
        private const val FACE_MESH_FRAGMENT_SHADER = "precision mediump float; uniform sampler2D u_texture; varying vec2 v_texCoord; void main(){gl_FragColor=texture2D(u_texture,v_texCoord);}"
    }
}

data class FaceLandmark(val x: Float, val y: Float, val z: Float, val index: Int)

data class FaceFeatureRegions(
    val leftEye: List<PointF>, val rightEye: List<PointF>, val lips: List<PointF>,
    val nose: List<PointF>, val faceContour: List<PointF>, val forehead: List<PointF>
) {
    val faceCenter: PointF
        get() = if (faceContour.isEmpty()) PointF(0.5f, 0.5f)
        else PointF(faceContour.map { it.x }.average().toFloat(), faceContour.map { it.y }.average().toFloat())
    val faceWidth: Float
        get() = if (faceContour.isEmpty()) 0.3f
        else { val xs = faceContour.map { it.x }; xs.max() - xs.min() }
}
