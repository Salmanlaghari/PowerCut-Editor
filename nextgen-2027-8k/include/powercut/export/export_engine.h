// =============================================================================
// PowerCut Pro 2027 8K — Export Engine (CRITICAL FIXES — P1)
// File: include/powercut/export/export_engine.h
//
// This header is the single source of truth for the export pipeline contract.
// The original backend (DAG resolve, watermark, audio mix, HW encoders,
// LevelDB cache) is PRESERVED. The 7 crash fixes live in export_engine.cpp:
//
//   #1 JNI local reference overflow   -> PushLocalFrame/PopLocalFrame per frame
//   #2 Audio buffer miscalc           -> 2x allocation + swr_convert clamp
//   #3 MediaCodec lock @10%           -> async Surface configured at create()
//   #4 PTS scaling bug                -> av_rescale_q (no manual *2)
//   #5 LevelDB on render thread       -> background low-prio worker queue
//   #6 FindClass every frame          -> cached jclass/jmethodID globals in setup
//   #7 10s watchdog                   -> auto software fallback
//   #8 null checks before every free  -> guarded release helpers
//   #9 cancel deadlock                -> lock-free atomic flag + non-blocking drain
// =============================================================================
#pragma once

#include "powercut/core/types.h"
#include "powercut/core/dag_resolver.h"
#include "powercut/core/hw_encoder.h"
#include <string>
#include <vector>
#include <memory>

// AVRational is an FFmpeg type. We vendor a tiny ABI-compatible definition so
// the engine compiles & links even when FFmpeg is not present (CI builds).
// When POWERCUT_FFMPEG_ENABLED=1 the real libavutil/rational.h wins via -I.
struct AVRational { int num; int den; };

namespace powercut::export_ {

struct ExportResult {
    bool        ok            = false;
    bool        fell_back_sw  = false;   // true if HW->SW watchdog fallback fired
    std::string out_path;
    int64_t     file_size_bytes = 0;
    int64_t     elapsed_ms    = 0;
    std::string error;                  // empty on success
    int         frames_written = 0;
};

class ExportEngine {
public:
    ExportEngine();
    ~ExportEngine();

    // No copy — owns native + JNI state.
    ExportEngine(const ExportEngine&) = delete;
    ExportEngine& operator=(const ExportEngine&) = delete;

    // One-time setup. Caches jclass/jmethodID/jfieldID as GLOBAL refs (P1 #6)
    // and opens the LevelDB cache. Must be called once before any export.
    // jvm is the JavaVM* passed from JNI_OnLoad.
    bool setup_enc(void* jvm);

    // Kick off a render. progress is invoked from a non-render thread.
    // cancel is polled every frame (lock-free). Returns when done or cancelled.
    ExportResult run(const core::ExportConfig& cfg,
                     const std::vector<core::DAGNode>& dag,
                     core::ProgressFn progress,
                     core::CancelToken& cancel);

    // Tear down everything. Idempotent. Called from JNI_OnUnload / dtor.
    void teardown();

private:
    struct Impl;
    std::unique_ptr<Impl> impl_;
};

} // namespace powercut::export_
