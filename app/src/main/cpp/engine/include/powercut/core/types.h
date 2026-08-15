// =============================================================================
// PowerCut Pro 2027 8K — core types (kept working backend, no breaking changes)
// File: include/powercut/core/types.h
// =============================================================================
#pragma once

#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <atomic>
#include <mutex>
#include <functional>

namespace powercut::core {

// Resolution ladder (8K top). 1080p is the default selected value in UI.
enum class Resolution : int {
    P480  = 480,
    P720  = 720,
    P1080 = 1080,
    P2K   = 1440,
    P4K   = 2160,
    P8K   = 4320
};

enum class FrameRate : int { FPS24 = 24, FPS30 = 30, FPS60 = 60, FPS120 = 120 };
enum class Container : int { MP4 = 0, MOV = 1, WEBM = 2 };

// GPU hardware encoder preference. AUTO picks the first working HW codec and
// transparently falls back to software on the 10s watchdog timeout (P1 fix #7).
enum class EncoderKind : int { AUTO = 0, HARDWARE = 1, SOFTWARE = 2 };

struct ExportConfig {
    Resolution  resolution      = Resolution::P1080;
    FrameRate   fps             = FrameRate::FPS30;
    Container   container       = Container::MP4;
    EncoderKind encoder         = EncoderKind::AUTO;   // HW first, SW fallback
    int64_t     video_bitrate   = 12'000'000;           // bps (scaled by res)
    int         audio_bitrate   = 192'000;              // bps
    int         audio_channels  = 2;
    int         audio_sample_rate = 48'000;
    bool        remove_watermark = false;                // PRO unlock
    bool        priority_hw     = false;                 // PRO priority queue
    std::string out_path;                                // sanitized by Kotlin
};

// A renderable node in the effect DAG. The DAG resolver walks these in
// topological order — this contract is preserved from the original backend.
struct DAGNode {
    enum class Kind { Source, Filter, Effect, Effect3D, ChromaKey, VFX, AI, Transition };
    Kind        kind;
    std::string id;
    std::string params_json;     // opaque params consumed by the node impl
    std::vector<std::string> deps; // upstream node ids
};

// Progress callback (called from a non-render thread after P1 fix #5).
// percent: 0..100 ; message: human-readable state.
using ProgressFn = std::function<void(int percent, const std::string& message)>;

// Cancellation token — checked by the render loop every frame.
// Must be lock-free to avoid the cancel deadlock (P1 fix #9).
struct CancelToken {
    std::atomic<bool> cancelled{false};
    void cancel() { cancelled.store(true, std::memory_order_release); }
    bool is_cancelled() const { return cancelled.load(std::memory_order_acquire); }
};

} // namespace powercut::core
