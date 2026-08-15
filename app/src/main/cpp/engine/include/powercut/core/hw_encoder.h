// =============================================================================
// PowerCut Pro 2027 8K — hardware encoder wrapper (kept working + async fix)
// File: include/powercut/core/hw_encoder.h
// =============================================================================
#pragma once
#include "powercut/core/types.h"
#include <cstdint>
#include <memory>
#include <atomic>

namespace powercut::core {

// A thin wrapper around MediaCodec (HW) / libx264 (SW) that owns an async
// Surface configured at create() time (P1 fix #3: codec lock when no surface).
// The wrapper is intentionally backend-agnostic — the JNI layer picks HW first
// and the engine triggers SW fallback after the 10s watchdog (P1 fix #7).
class HWEncoder {
public:
    struct Config {
        int      width;
        int      height;
        int      fps;
        int64_t  bitrate;
        bool     hardware;       // true => MediaCodec, false => libx264
        // Native window / Surface handle passed from the Kotlin side via JNI
        // (ANativeWindow*). 0 means "no surface" — NOT allowed for HW since P1.
        void*    native_window = nullptr;
    };

    virtual ~HWEncoder() = default;

    // Returns false on init failure — caller falls back to SW.
    virtual bool configure(const Config& cfg) = 0;

    // Feed one RGBA frame. pts_us is the presentation time in microseconds,
    // already scaled via av_rescale_q at the call site (P1 fix #4).
    // Returns false if the encoder has stalled past the watchdog.
    virtual bool encode_frame(const uint8_t* rgba, int stride, int64_t pts_us) = 0;

    // Drain remaining buffered frames and finalize the stream.
    virtual bool drain() = 0;

    // Watchdog: ms since last successful output. >10000 => caller falls back.
    virtual int64_t ms_since_output() const = 0;

    virtual bool is_hardware() const = 0;
};

std::unique_ptr<HWEncoder> create_encoder(const HWEncoder::Config& cfg);

} // namespace powercut::core
