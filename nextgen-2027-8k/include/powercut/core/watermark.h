// =============================================================================
// PowerCut Pro 2027 8K — watermark system (kept working backend)
// File: include/powercut/core/watermark.h
// =============================================================================
#pragma once
#include "powercut/core/types.h"
#include <cstdint>

namespace powercut::core {

// Burns the PowerCut watermark into the bottom-right of an RGBA frame when
// the export config has remove_watermark == false (free tier). The PRO path
// (remove_watermark == true) returns the buffer untouched. Original behavior
// preserved.
class Watermark {
public:
    // in-place. width/height in pixels, stride in bytes (== width*4 for tight).
    static void apply(uint8_t* rgba, int width, int height, int stride,
                      bool remove_watermark);
    static constexpr int kBadgeW = 96;
    static constexpr int kBadgeH = 28;
};

} // namespace powercut::core
