// =============================================================================
// PowerCut Pro 2027 8K — audio mix pipeline (kept working backend)
// File: include/powercut/core/audio_mixer.h
// =============================================================================
#pragma once
#include "powercut/core/types.h"
#include <vector>
#include <cstdint>

namespace powercut::core {

// Mixes N audio tracks (PCM float32 planar) into a single interleaved float32
// buffer at the export sample rate. Original contract preserved; the per-track
// gain + fade envelope math is unchanged.
struct AudioTrack {
    const float* samples = nullptr;   // interleaved float32
    int64_t      frame_count = 0;     // per channel
    int          channels   = 2;
    int          sample_rate = 48000;
    float        gain       = 1.0f;
    double       start_offset_sec = 0.0;
};

class AudioMixer {
public:
    // dst must be allocated by caller (see ExportEngine for the 2x clamp fix).
    // Returns frames written per channel. Never writes more than dst_capacity.
    static int64_t mix(const std::vector<AudioTrack>& tracks,
                       float* dst, int64_t dst_capacity,
                       int out_channels, int out_sample_rate);
};

} // namespace powercut::core
