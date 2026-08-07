// =============================================================================
// PowerCut Pro 2027 8K — watermark impl (kept working backend)
// File: src/core/watermark.cpp
// =============================================================================
#include "powercut/core/watermark.h"
#include <cstring>

namespace powercut::core {

namespace {
inline void blend(uint8_t* dst, uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    const int ia = 255 - a;
    dst[0] = static_cast<uint8_t>((dst[0] * ia + r * a) / 255);
    dst[1] = static_cast<uint8_t>((dst[1] * ia + g * a) / 255);
    dst[2] = static_cast<uint8_t>((dst[2] * ia + b * a) / 255);
    // alpha stays 255
}
} // namespace

void Watermark::apply(uint8_t* rgba, int width, int height, int stride,
                      bool remove_watermark) {
    if (remove_watermark) return;            // PRO: untouched
    if (!rgba || width <= 0 || height <= 0) return;
    if (width  < kBadgeW + 16) return;
    if (height < kBadgeH + 16) return;

    const int x0 = width  - kBadgeW - 16;     // bottom-right
    const int y0 = height - kBadgeH - 16;

    // Translucent dark pill background (alpha 150) + orange→purple "PRO" hint.
    for (int y = 0; y < kBadgeH; ++y) {
        uint8_t* row = rgba + (size_t)(y0 + y) * stride + (size_t)x0 * 4;
        for (int x = 0; x < kBadgeW; ++x) {
            const uint8_t bg_a = 150;
            // gradient orange(0xFF5A3C) -> purple(0x9D4EDD) across width
            const float t = (float)x / (float)kBadgeW;
            const uint8_t r = (uint8_t)(0xFF * (1 - t) + 0x9D * t);
            const uint8_t g = (uint8_t)(0x5A * (1 - t) + 0x4E * t);
            const uint8_t b = (uint8_t)(0x3C * (1 - t) + 0xDD * t);
            blend(row + x * 4, r, g, b, bg_a);
        }
    }
    // Simple "PRO" glyph strip — three vertical bars in white-ish, alpha 200.
    auto bar = [&](int bx, int by, int bw, int bh) {
        for (int y = 0; y < bh; ++y) {
            uint8_t* row = rgba + (size_t)(y0 + by + y) * stride + (size_t)(x0 + bx) * 4;
            for (int x = 0; x < bw; ++x) blend(row + x * 4, 245, 245, 250, 200);
        }
    };
    const int cy = kBadgeH / 2 - 6;
    bar(18, cy,      6, 12); // P
    bar(34, cy,      6, 12); // R
    bar(50, cy,      6, 12); // O
}

} // namespace powercut::core
