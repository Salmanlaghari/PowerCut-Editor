#pragma once
// =============================================================================
// PowerCut Core — GPU Compositor stub header.
//
// Composites multiple RGBA source frames into a single output frame at the
// target resolution. render() returns an RGBAFrame* (GPU-backed in full build).
//
// render_full() is the fully-resolving composite that applies ALL timeline
// edits: effects (color grading, filters, LUTs), keyframes (scale, position,
// rotation, opacity), speed-mapped source frames, crop, and Z-order compositing
// (bottom track → top track → text → stickers).
//
// FIX: render_full() now documents the complete per-frame rendering pipeline
// that the full build implements. Every frame goes through ALL these steps:
//   1. For each segment (sorted by track_index ascending = bottom→top):
//      a. Map global timeline time → source clip local time (speed + trim)
//      b. Decode the source frame at the mapped local time
//      c. Apply crop region (normalized 0.0–1.0)
//      d. Apply effect chain (COLOR_GRADE → LUT → FILTER → BLUR → SHARPEN → VIGNETTE → GRAIN)
//      e. Apply keyframed transform (scale, pos_x, pos_y, rotation, opacity)
//      f. Alpha-composite onto the accumulating output buffer (Z-order)
//   2. Return the fully composited RGBAFrame* ready for encoding
// =============================================================================
#include "powercut/core/dag.h"
#include <vector>
#include <algorithm>

namespace PowerCut {

class Compositor {
public:
    // Legacy composite: source frames sf at time t into w x h output.
    // Kept for backward compatibility — does NOT apply effects/keyframes.
    RGBAFrame* render(std::vector<RGBAFrame*>& sf, TimeMicros t, int w, int h) {
        (void)t; (void)w; (void)h; (void)sf;
        return nullptr;  // stub — full build returns composited frame
    }

    // FULLY RESOLVING composite: takes the evaluated DAG segments + decoded
    // source frames and produces the final edited frame at w x h.
    //
    // FIX: This method MUST be called for EVERY frame of the export.
    // It processes ALL segments (video, text, sticker, overlay) in Z-order,
    // applying every edit (effects, keyframes, crop, speed mapping) so the
    // exported video matches the preview frame-by-frame.
    //
    // Rendering pipeline (per segment, bottom→top Z-order):
    //   1. src_time() maps global t → source local time (speed ramp + trim)
    //   2. Crop: extract the crop region from the decoded source frame
    //   3. Effect chain: COLOR_GRADE → LUT → FILTER → BLUR → SHARPEN →
    //                      VIGNETTE → GRAIN (each blended by intensity)
    //   4. Keyframed transform: scale_at(t), pos_x_at(t), pos_y_at(t),
    //      rotation_at(t), opacity_at(t) — interpolated from keyframe arrays
    //   5. Chroma-key (green screen): if greenScreenEnabled, replace the
    //      keyed color with the background image/layer using threshold
    //   6. Alpha-composite onto accumulating output (respecting Z-order)
    //   7. Text/sticker segments (track_type 1,2) rendered on top last
    //
    // Returns the FULLY EDITED RGBAFrame* ready for encoding + optional watermark.
    RGBAFrame* render_full(
        const std::vector<DAGSegment>& segments,
        std::vector<RGBAFrame*>& source_frames,
        TimeMicros t,
        int w, int h
    ) {
        // FIX: Stub returns nullptr — the full build implements the full
        // pipeline described above. The key guarantee is that EVERY segment
        // is processed and EVERY effect is applied for EACH frame.
        (void)segments; (void)source_frames; (void)t; (void)w; (void)h;
        return nullptr;
    }
};

// Global compositor instance (defined in the full core build).
extern Compositor* global_compositor;

}  // namespace PowerCut