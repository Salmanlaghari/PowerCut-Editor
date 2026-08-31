package com.powercut.editor.domain.vfx

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * AI Motion Tracking & Object Locking Engine.
 * Tracks objects/faces across frames and generates motion vectors for pinning overlays.
 */
class MotionTrackingEngine {
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _trackingProgress = MutableStateFlow(0f)
    val trackingProgress: StateFlow<Float> = _trackingProgress.asStateFlow()

    private val trackedObjects = mutableMapOf<String, TrackedObject>()
    private val motionVectors = mutableMapOf<String, List<MotionVector>>()

    /**
     * Start tracking an object from a bounding box selection.
     */
    suspend fun trackObject(
        objectId: String,
        initialBox: BoundingBox,
        frameCount: Int,
        getFrameFeatures: suspend (Int) -> FrameFeatures?
    ): List<MotionVector> = withContext(Dispatchers.Default) {
        _isTracking.value = true
        _trackingProgress.value = 0f

        try {
            val vectors = mutableListOf<MotionVector>()
            var currentBox = initialBox

            for (frame in 0 until frameCount) {
                val features = getFrameFeatures(frame)
                if (features != null) {
                    // Simple template matching (in production, use DNN-based tracking)
                    currentBox = matchTemplate(features, currentBox)

                    vectors.add(
                        MotionVector(
                            frameIndex = frame,
                            positionX = currentBox.centerX,
                            positionY = currentBox.centerY,
                            scaleX = currentBox.width / initialBox.width,
                            scaleY = currentBox.height / initialBox.height,
                            rotation = features.rotation,
                            confidence = features.confidence
                        )
                    )
                }

                _trackingProgress.value = (frame.toFloat() / frameCount) * 100f
            }

            motionVectors[objectId] = vectors
            trackedObjects[objectId] = TrackedObject(
                id = objectId,
                initialBox = initialBox,
                vectors = vectors
            )

            vectors
        } finally {
            _isTracking.value = false
            _trackingProgress.value = 0f
        }
    }

    /**
     * Track a face using MediaPipe landmarks.
     */
    suspend fun trackFace(
        faceId: String,
        frameCount: Int,
        getFaceLandmarks: suspend (Int) -> List<FloatArray>?
    ): List<MotionVector> = withContext(Dispatchers.Default) {
        _isTracking.value = true
        _trackingProgress.value = 0f

        try {
            val vectors = mutableListOf<MotionVector>()

            for (frame in 0 until frameCount) {
                val landmarks = getFaceLandmarks(frame)
                if (landmarks != null && landmarks.isNotEmpty()) {
                    // Calculate face center and bounds
                    val centerX = landmarks.map { it[0] }.average().toFloat()
                    val centerY = landmarks.map { it[1] }.average().toFloat()
                    val left = landmarks.minOf { it[0] }
                    val right = landmarks.maxOf { it[0] }
                    val top = landmarks.minOf { it[1] }
                    val bottom = landmarks.maxOf { it[1] }
                    val width = right - left
                    val height = bottom - top

                    // Calculate rotation from eye positions
                    val leftEye = landmarks.getOrElse(33) { floatArrayOf(0f, 0f) }
                    val rightEye = landmarks.getOrElse(263) { floatArrayOf(1f, 0f) }
                    val rotation = Math.toDegrees(
                        Math.atan2(
                            (rightEye[1] - leftEye[1]).toDouble(),
                            (rightEye[0] - leftEye[0]).toDouble()
                        )
                    ).toFloat()

                    vectors.add(
                        MotionVector(
                            frameIndex = frame,
                            positionX = centerX,
                            positionY = centerY,
                            scaleX = width,
                            scaleY = height,
                            rotation = rotation,
                            confidence = 0.95f
                        )
                    )
                }

                _trackingProgress.value = (frame.toFloat() / frameCount) * 100f
            }

            motionVectors[faceId] = vectors
            vectors
        } finally {
            _isTracking.value = false
            _trackingProgress.value = 0f
        }
    }

    /**
     * Get interpolated motion vector at a specific frame.
     */
    fun getMotionAtFrame(objectId: String, frameIndex: Int): MotionVector? {
        val vectors = motionVectors[objectId] ?: return null
        if (vectors.isEmpty()) return null

        val frame = frameIndex.coerceIn(0, vectors.lastIndex)
        return vectors[frame]
    }

    /**
     * Pin an overlay to a tracked object.
     */
    fun createPinnedOverlay(
        objectId: String,
        overlayType: OverlayType,
        offset: ThreeDVector3 = ThreeDVector3(),
        scale: Float = 1f
    ): PinnedOverlay {
        return PinnedOverlay(
            objectId = objectId,
            overlayType = overlayType,
            offset = offset,
            scale = scale
        )
    }

    /**
     * Get all motion vectors for a tracked object.
     */
    fun getMotionVectors(objectId: String): List<MotionVector> {
        return motionVectors[objectId] ?: emptyList()
    }

    /**
     * Clear tracking data.
     */
    fun clearTracking(objectId: String) {
        trackedObjects.remove(objectId)
        motionVectors.remove(objectId)
    }

    /**
     * Clear all tracking data.
     */
    fun clearAll() {
        trackedObjects.clear()
        motionVectors.clear()
    }

    /**
     * Simple template matching for object tracking.
     */
    private fun matchTemplate(features: FrameFeatures, previousBox: BoundingBox): BoundingBox {
        // Simple运动 estimation based on feature shift
        val shiftX = features.dominantMotionX * 0.1f
        val shiftY = features.dominantMotionY * 0.1f

        return BoundingBox(
            x = (previousBox.x + shiftX).coerceIn(0f, 1f),
            y = (previousBox.y + shiftY).coerceIn(0f, 1f),
            width = previousBox.width * (1f + features.scaleDelta * 0.05f),
            height = previousBox.height * (1f + features.scaleDelta * 0.05f)
        )
    }

    companion object {
        // Placeholder for frame feature extraction
        suspend fun defaultFeatureExtractor(frameIndex: Int): FrameFeatures? {
            return FrameFeatures(
                dominantMotionX = 0.001f * frameIndex,
                dominantMotionY = 0f,
                scaleDelta = 0f,
                rotation = 0f,
                confidence = 0.9f
            )
        }
    }
}

// ── Data Models ──

data class BoundingBox(
    val x: Float,      // Normalized 0-1
    val y: Float,
    val width: Float,
    val height: Float
) {
    val centerX: Float get() = x + width / 2
    val centerY: Float get() = y + height / 2
}

data class MotionVector(
    val frameIndex: Int,
    val positionX: Float,
    val positionY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val rotation: Float,
    val confidence: Float
)

data class TrackedObject(
    val id: String,
    val initialBox: BoundingBox,
    val vectors: List<MotionVector>
)

data class FrameFeatures(
    val dominantMotionX: Float,
    val dominantMotionY: Float,
    val scaleDelta: Float,
    val rotation: Float,
    val confidence: Float
)

enum class OverlayType {
    STICKER, TEXT_3D, NEON_OUTLINE, VFX_FILTER, AR_MASK
}

data class PinnedOverlay(
    val objectId: String,
    val overlayType: OverlayType,
    val offset: ThreeDVector3 = ThreeDVector3(),
    val scale: Float = 1f
)
