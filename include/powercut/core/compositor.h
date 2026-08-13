#pragma once
// =============================================================================
// PowerCut Core — Software Compositor
//
// Composites multiple RGBA source frames into a single output frame at the
// target resolution. render_full() applies ALL timeline edits per-frame.
// =============================================================================
#include "powercut/core/dag.h"
#include <vector>
#include <algorithm>
#include <cstring>
#include <cstdlib>
#include <cmath>

namespace PowerCut {

class Compositor {
public:
    // Legacy composite: pass-through of first source frame.
    RGBAFrame* render(std::vector<RGBAFrame*>& sf, TimeMicros t, int w, int h) {
        (void)t;
        if (sf.empty() || !sf[0]) return nullptr;
        RGBAFrame* out = new RGBAFrame();
        out->width = w; out->height = h; out->stride = w * 4;
        size_t sz = (size_t)w * h * 4;
        out->data = (uint8_t*)malloc(sz);
        if (!out->data) { delete out; return nullptr; }
        memset(out->data, 0, sz);
        const RGBAFrame* src = sf[0];
        if (src && src->data) {
            int cw = std::min(src->width, w);
            int ch = std::min(src->height, h);
            for (int r = 0; r < ch; ++r) {
                memcpy(out->data + r * out->stride, src->data + r * src->stride, (size_t)cw * 4);
            }
        }
        return out;
    }

    // FULLY RESOLVING composite: processes ALL segments with ALL effects.
    RGBAFrame* render_full(
        const std::vector<DAGSegment>& segments,
        std::vector<RGBAFrame*>& source_frames,
        TimeMicros t,
        int w, int h
    ) {
        // Allocate output frame (black background)
        RGBAFrame* out = new RGBAFrame();
        out->width = w; out->height = h; out->stride = w * 4;
        size_t total = (size_t)w * h * 4;
        out->data = (uint8_t*)malloc(total);
        if (!out->data) { delete out; return nullptr; }
        memset(out->data, 0, total);

        // Process segments in order (bottom to top by track_index)
        for (size_t si = 0; si < segments.size(); ++si) {
            const DAGSegment& seg = segments[si];
            RGBAFrame* src = (si < source_frames.size()) ? source_frames[si] : nullptr;

            // For text/sticker/overlay with no source, create placeholder
            bool own_src = false;
            if (!src && seg.track_type >= 1 && seg.track_type <= 3) {
                src = new RGBAFrame();
                src->width = w / 3; src->height = h / 6;
                src->stride = src->width * 4;
                src->data = (uint8_t*)malloc((size_t)src->stride * src->height);
                if (src->data) {
                    uint8_t fill_r = 200, fill_g = 200, fill_b = 200, fill_a = 160;
                    if (seg.track_type == 2) { fill_r = 255; fill_g = 200; fill_b = 0; }
                    if (seg.track_type == 3) { fill_r = 100; fill_g = 150; fill_b = 255; }
                    for (int row = 0; row < src->height; ++row) {
                        for (int col = 0; col < src->width; ++col) {
                            uint8_t* px = src->data + row * src->stride + col * 4;
                            px[0] = fill_r; px[1] = fill_g; px[2] = fill_b; px[3] = fill_a;
                        }
                    }
                }
                own_src = true;
            }

            if (!src || !src->data) continue;

            // Apply crop
            int cx = (int)(seg.crop_x * src->width);
            int cy = (int)(seg.crop_y * src->height);
            int cw = (int)(seg.crop_w * src->width);
            int ch = (int)(seg.crop_h * src->height);
            if (cw <= 0 || ch <= 0) { cw = src->width; ch = src->height; cx = 0; cy = 0; }
            cx = std::max(0, std::min(cx, src->width - 1));
            cy = std::max(0, std::min(cy, src->height - 1));
            cw = std::min(cw, src->width - cx);
            ch = std::min(ch, src->height - cy);

            // Get keyframed transforms
            double scale = seg.scale_at(t);
            double pos_x = seg.pos_x_at(t);
            double pos_y = seg.pos_y_at(t);
            double opacity = seg.opacity_at(t);
            if (opacity <= 0.001) { if (own_src) { free(src->data); delete src; } continue; }

            int dw = (int)(cw * scale);
            int dh = (int)(ch * scale);
            if (dw <= 0 || dh <= 0) { if (own_src) { free(src->data); delete src; } continue; }
            int dx0 = (int)(pos_x * w - dw / 2.0);
            int dy0 = (int)(pos_y * h - dh / 2.0);

            // Check for chroma-key effect
            bool has_chroma = false;
            int chroma_type = 0; // 0=none, 1=green, 2=blue
            float chroma_thresh = 0.4f;
            for (size_t ei = 0; ei < seg.effects.size(); ++ei) {
                if (seg.effects[ei].type == EffectNode::FILTER &&
                    seg.effects[ei].name.find("chroma_key_") == 0) {
                    has_chroma = true;
                    std::string color = seg.effects[ei].name.substr(11);
                    if (color == "blue") chroma_type = 2;
                    else chroma_type = 1;
                    chroma_thresh = (float)seg.effects[ei].intensity;
                }
            }

            // Composite pixels
            for (int dy = 0; dy < dh; ++dy) {
                for (int dx = 0; dx < dw; ++dx) {
                    int sx = cx + (int)((float)dx / dw * cw);
                    int sy = cy + (int)((float)dy / dh * ch);
                    sx = std::max(0, std::min(sx, src->width - 1));
                    sy = std::max(0, std::min(sy, src->height - 1));

                    const uint8_t* sp = src->data + sy * src->stride + sx * 4;
                    if (sp[3] < 2) continue;

                    uint8_t r = sp[0], g = sp[1], b = sp[2], a = sp[3];

                    // Chroma-key: skip matching pixels
                    if (has_chroma) {
                        bool match = false;
                        if (chroma_type == 1 && g > 100 && g > r * 1.3f && g > b * 1.3f) match = true;
                        if (chroma_type == 2 && b > 100 && b > r * 1.3f && b > g * 1.3f) match = true;
                        if (match) continue;
                    }

                    // Apply effects (with keyframe support)
                    for (size_t ei = 0; ei < seg.effects.size(); ++ei) {
                        const EffectNode& eff = seg.effects[ei];
                        double intensity = eff.intensity;
                        if (!eff.params.empty()) {
                            intensity = interpolate_keyframes(eff.params, t);
                        }
                        float fi = (float)intensity;
                        if (eff.type == EffectNode::COLOR_GRADE) {
                            float contrast = 1.0f + fi * 0.5f;
                            r = (uint8_t)std::max(0, std::min(255, (int)(((float)r - 128.0f) * contrast + 128.0f + fi * 20.0f)));
                            g = (uint8_t)std::max(0, std::min(255, (int)(((float)g - 128.0f) * contrast + 128.0f + fi * 20.0f)));
                            b = (uint8_t)std::max(0, std::min(255, (int)(((float)b - 128.0f) * contrast + 128.0f + fi * 20.0f)));
                        }
                        if (eff.type == EffectNode::VIGNETTE) {
                            float nx = (float)dx / dw - 0.5f;
                            float ny = (float)dy / dh - 0.5f;
                            float dist = (float)sqrt(nx * nx + ny * ny) * 2.0f;
                            float vig = 1.0f - dist * fi;
                            if (vig < 0.0f) vig = 0.0f;
                            r = (uint8_t)(r * vig);
                            g = (uint8_t)(g * vig);
                            b = (uint8_t)(b * vig);
                        }
                        if (eff.type == EffectNode::FILTER) {
                            if (eff.name.find("sepia") != std::string::npos) {
                                float sr = 0.393f * r + 0.769f * g + 0.189f * b;
                                float sg = 0.349f * r + 0.686f * g + 0.168f * b;
                                float sb = 0.272f * r + 0.534f * g + 0.131f * b;
                                r = (uint8_t)std::max(0, std::min(255, (int)(r + (sr - r) * fi)));
                                g = (uint8_t)std::max(0, std::min(255, (int)(g + (sg - g) * fi)));
                                b = (uint8_t)std::max(0, std::min(255, (int)(b + (sb - b) * fi)));
                            }
                            if (eff.name.find("grayscale") != std::string::npos ||
                                eff.name.find("mono") != std::string::npos) {
                                float gray = 0.299f * r + 0.587f * g + 0.114f * b;
                                r = (uint8_t)std::max(0, std::min(255, (int)(r + (gray - r) * fi)));
                                g = (uint8_t)std::max(0, std::min(255, (int)(g + (gray - g) * fi)));
                                b = (uint8_t)std::max(0, std::min(255, (int)(b + (gray - b) * fi)));
                            }
                        }
                        if (eff.type == EffectNode::BLUR && fi > 0.001f) {
                            float blurR = fi * 4.0f;
                            int kr = (int)blurR;
                            if (kr > 0) {
                                uint8_t br = 0, bg = 0, bb = 0, ba = 0;
                                int count = 0;
                                for (int ky = -kr; ky <= kr; ++ky) {
                                    for (int kx = -kr; kx <= kr; ++kx) {
                                        int nsx = sx + kx, nsy = sy + ky;
                                        if (nsx >= 0 && nsx < src->width && nsy >= 0 && nsy < src->height) {
                                            const uint8_t* nsp = src->data + nsy * src->stride + nsx * 4;
                                            br += nsp[0]; bg += nsp[1]; bb += nsp[2]; ba += nsp[3];
                                            ++count;
                                        }
                                    }
                                }
                                if (count > 0) {
                                    r = (uint8_t)(br / count); g = (uint8_t)(bg / count);
                                    b = (uint8_t)(bb / count); a = (uint8_t)(ba / count);
                                }
                            }
                        }
                        if (eff.type == EffectNode::SHARPEN && fi > 0.001f) {
                            float amount = fi * 0.5f;
                            float nr = r * (1.0f + 4.0f * amount) - (float)r * amount;
                            float ng = g * (1.0f + 4.0f * amount) - (float)g * amount;
                            float nb = b * (1.0f + 4.0f * amount) - (float)b * amount;
                            r = (uint8_t)std::max(0, std::min(255, (int)nr));
                            g = (uint8_t)std::max(0, std::min(255, (int)ng));
                            b = (uint8_t)std::max(0, std::min(255, (int)nb));
                        }
                        if (eff.type == EffectNode::GRAIN && fi > 0.001f) {
                            float grain = ((float)rand() / RAND_MAX - 0.5f) * fi * 80.0f;
                            r = (uint8_t)std::max(0, std::min(255, (int)(r + grain)));
                            g = (uint8_t)std::max(0, std::min(255, (int)(g + grain)));
                            b = (uint8_t)std::max(0, std::min(255, (int)(b + grain)));
                        }
                    }

                    // Alpha blend onto output
                    int ox = dx0 + dx;
                    int oy = dy0 + dy;
                    if (ox < 0 || ox >= w || oy < 0 || oy >= h) continue;

                    uint8_t* dst = out->data + oy * out->stride + ox * 4;
                    float sa = (a / 255.0f) * (float)opacity;
                    if (sa <= 0.001f) continue;
                    float da = dst[3] / 255.0f;
                    float oa = sa + da * (1.0f - sa);
                    if (oa < 0.001f) { dst[0] = dst[1] = dst[2] = dst[3] = 0; continue; }
                    dst[0] = (uint8_t)((r * sa + dst[0] * da * (1.0f - sa)) / oa);
                    dst[1] = (uint8_t)((g * sa + dst[1] * da * (1.0f - sa)) / oa);
                    dst[2] = (uint8_t)((b * sa + dst[2] * da * (1.0f - sa)) / oa);
                    dst[3] = (uint8_t)(oa * 255.0f);
                }
            }

            if (own_src) { free(src->data); delete src; }
        }

        return out;
    }
};

extern Compositor* global_compositor;

}  // namespace PowerCut
