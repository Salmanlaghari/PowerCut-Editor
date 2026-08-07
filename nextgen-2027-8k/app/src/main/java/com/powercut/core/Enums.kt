package com.powercut.core

/** Resolution ladder. 1080p is the default selected value in the export UI. */
enum class Resolution(val pixels: Int) {
    P480(480), P720(720), P1080(1080), P2K(1440), P4K(2160), P8K(4320)
}

/** Target frame rate. 30fps default. */
enum class FrameRate(val fps: Int) { FPS24(24), FPS30(30), FPS60(60), FPS120(120) }

/** Output container. MP4 is the default + recommended. */
enum class Container(val index: Int) { MP4(0), MOV(1), WEBM(2) }

/**
 * Encoder preference. AUTO = hardware first, transparent software fallback
 * after the 10s watchdog (see export_engine.cpp P1 fix #7).
 */
enum class EncoderKind(val index: Int) { AUTO(0), HARDWARE(1), SOFTWARE(2) }
