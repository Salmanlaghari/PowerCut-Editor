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
// =============================================================================
#include "powercut/core/dag.h"
#include <vector>

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
    // For each segment (already sorted by track_index ascending = bottom→top):
    //   1. Apply crop region to the source frame
    //   2. Apply effect chain (color grade, LUT, filter, blur, etc.)
    //   3. Apply keyframed transform (scale, position, rotation, opacity)
    //   4. Alpha-composite onto the accumulating output (Z-order)
    // Text/sticker segments are composited last (on top).
    //
    // Returns the FULLY EDITED RGBAFrame* ready for encoding.
    RGBAFrame* render_full(
        const std::vector<DAGSegment>& segments,
        std::vector<RGBAFrame*>& source_frames,
        TimeMicros t,
        int w, int h
    ) {
        (void)segments; (void)source_frames; (void)t; (void)w; (void)h;
        return nullptr;  // stub — full build returns fully edited frame
    }
};

// Global compositor instance (defined in the full core build).
extern Compositor* global_compositor;

}  // namespace PowerCut
