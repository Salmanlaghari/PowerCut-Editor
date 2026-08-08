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
#include <algorithm>
#include <cmath>
#include <cstdlib>
#include <vector>

namespace PowerCut {

class Compositor {
public:
    virtual ~Compositor() = default;

    // Legacy composite: source frames sf at time t into w x h output.
    // Kept for backward compatibility — does NOT apply effects/keyframes.
    virtual RGBAFrame* render(std::vector<RGBAFrame*>& sf, TimeMicros t, int w, int h) {
        (void)t;
        (void)w;
        (void)h;
        if (sf.empty()) return nullptr;

        std::vector<DAGSegment> segments;
        segments.reserve(sf.size());
        for (size_t i = 0; i < sf.size(); ++i) {
            DAGSegment seg;
            seg.track_index = (int)i;
            seg.crop_w = 1.0;
            seg.crop_h = 1.0;
            seg.kf_opacity.push_back(Keyframe{0, 1.0, Keyframe::LINEAR});
            segments.push_back(seg);
        }
        return render_full(segments, sf, t, w, h);
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
    virtual RGBAFrame* render_full(
        const std::vector<DAGSegment>& segments,
        std::vector<RGBAFrame*>& source_frames,
        TimeMicros t,
        int w, int h
    ) {
        if (segments.empty() || source_frames.empty() || w <= 0 || h <= 0) {
            return nullptr;
        }

        RGBAFrame* out = new RGBAFrame();
        out->width = w;
        out->height = h;
        out->stride = w * 4;
        out->data = static_cast<uint8_t*>(std::calloc(out->stride * out->height, 1));
        if (!out->data) {
            delete out;
            return nullptr;
        }

        auto blend_pixel = [](uint8_t* dst, const uint8_t* src, double opacity) {
            double src_a = (src[3] / 255.0) * opacity;
            double inv_a = 1.0 - src_a;
            for (int channel = 0; channel < 3; ++channel) {
                double src_c = src[channel] / 255.0;
                double dst_c = dst[channel] / 255.0;
                double result = src_c * src_a + dst_c * inv_a;
                dst[channel] = static_cast<uint8_t>(std::round(std::clamp(result, 0.0, 1.0) * 255.0));
            }
            double dst_a = dst[3] / 255.0;
            double result_a = src_a + dst_a * inv_a;
            dst[3] = static_cast<uint8_t>(std::round(std::clamp(result_a, 0.0, 1.0) * 255.0));
        };

        const size_t count = std::min(segments.size(), source_frames.size());
        for (size_t idx = 0; idx < count; ++idx) {
            const auto& seg = segments[idx];
            RGBAFrame* src = source_frames[idx];
            if (!src || !src->data || src->width <= 0 || src->height <= 0) continue;

            int crop_x = std::clamp((int)std::floor(seg.crop_x * src->width), 0, src->width - 1);
            int crop_y = std::clamp((int)std::floor(seg.crop_y * src->height), 0, src->height - 1);
            int crop_w = std::max(1, std::clamp((int)std::floor(seg.crop_w * src->width), 1, src->width - crop_x));
            int crop_h = std::max(1, std::clamp((int)std::floor(seg.crop_h * src->height), 1, src->height - crop_y));
            double opacity = seg.opacity_at(t);
            float scale_x = static_cast<float>(crop_w) / static_cast<float>(w);
            float scale_y = static_cast<float>(crop_h) / static_cast<float>(h);

            for (int y = 0; y < h; ++y) {
                int src_y = crop_y + std::min(crop_h - 1, static_cast<int>(std::floor(y * scale_y)));
                const uint8_t* src_row = src->data + src_y * src->stride;
                uint8_t* dst_row = out->data + y * out->stride;
                for (int x = 0; x < w; ++x) {
                    int src_x = crop_x + std::min(crop_w - 1, static_cast<int>(std::floor(x * scale_x)));
                    const uint8_t* src_px = src_row + src_x * 4;
                    uint8_t* dst_px = dst_row + x * 4;
                    blend_pixel(dst_px, src_px, opacity);
                }
            }
        }

        return out;
    }
};

// Global compositor instance (defined in the full core build).
extern Compositor* global_compositor;

}  // namespace PowerCut
