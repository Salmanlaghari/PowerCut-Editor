// =============================================================================
// PowerCut Pro 2027 8K — audio mix impl (kept working backend, clamp-safe)
// File: src/core/audio_mixer.cpp
// =============================================================================
#include "powercut/core/audio_mixer.h"
#include <algorithm>
#include <cstring>
#include <cmath>

namespace powercut::core {

namespace {
inline float lerp(float a, float b, float t) { return a + (b - a) * t; }
} // namespace

int64_t AudioMixer::mix(const std::vector<AudioTrack>& tracks,
                        float* dst, int64_t dst_capacity,
                        int out_channels, int out_sample_rate) {
    if (!dst || dst_capacity <= 0 || out_channels <= 0 || out_sample_rate <= 0)
        return 0;

    const int64_t total_samples = dst_capacity * out_channels;
    std::memset(dst, 0, (size_t)total_samples * sizeof(float));

    int64_t max_frames_written = 0;

    for (const auto& t : tracks) {
        if (!t.samples || t.frame_count <= 0 || t.channels <= 0 || t.sample_rate <= 0)
            continue;

        const double start_frame_d = t.start_offset_sec * (double)out_sample_rate;
        const int64_t start_frame  = (int64_t)std::llround(start_frame_d);
        if (start_frame >= dst_capacity) continue;
        if (start_frame < 0) continue; // clamp: tracks before 0 skipped

        // Resample ratio (nearest + linear interp — kept-working simple path;
        // ExportEngine wraps this with the 2x allocation + swr_convert clamp
        // for the FFmpeg-backed path).
        const double ratio = (double)t.sample_rate / (double)out_sample_rate;
        const int64_t dst_frames_avail = dst_capacity - start_frame;
        const int64_t src_frames_for_dst = (int64_t)((double)dst_frames_avail * ratio);
        const int64_t src_frames = std::min<int64_t>(src_frames_for_dst, t.frame_count);

        for (int64_t i = 0; i < dst_frames_avail; ++i) {
            const double src_pos = (double)i * ratio;
            const int64_t src_i0 = (int64_t)src_pos;
            if (src_i0 >= src_frames - 1) break;
            const double frac = src_pos - (double)src_i0;
            const float* s0 = t.samples + (size_t)src_i0  * t.channels;
            const float* s1 = t.samples + (size_t)(src_i0 + 1) * t.channels;

            const int64_t dst_frame = start_frame + i;
            float* d = dst + (size_t)dst_frame * out_channels;

            // down/up-mix channels
            for (int c = 0; c < out_channels; ++c) {
                const int sc = (t.channels == 1) ? 0
                              : (c < t.channels ? c : t.channels - 1);
                const float v = lerp(s0[sc], s1[sc], (float)frac) * t.gain;
                d[c] += v;
            }
            if (dst_frame + 1 > max_frames_written)
                max_frames_written = dst_frame + 1;
        }
    }

    // Soft clip to [-1,1] to avoid inter-sample peaks.
    for (int64_t i = 0; i < max_frames_written * out_channels; ++i) {
        float v = dst[i];
        if (v > 1.0f) v = 1.0f;
        else if (v < -1.0f) v = -1.0f;
        dst[i] = v;
    }
    return max_frames_written;
}

} // namespace powercut::core
