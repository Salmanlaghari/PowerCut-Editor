#pragma once
// =============================================================================
// PowerCut Core — GPU Compositor stub header.
//
// Composites multiple RGBA source frames into a single output frame at the
// target resolution. render() returns an RGBAFrame* (GPU-backed in full build).
// =============================================================================
#include "powercut/core/dag.h"
#include <vector>

namespace PowerCut {

class Compositor {
public:
    // Composite source frames sf at time t into w x h output.
    RGBAFrame* render(std::vector<RGBAFrame*>& sf, TimeMicros t, int w, int h) {
        (void)t;
        (void)w;
        (void)h;
        (void)sf;
        return nullptr;  // stub — full build returns composited frame
    }
};

// Global compositor instance (defined in the full core build).
extern Compositor* global_compositor;

}  // namespace PowerCut
