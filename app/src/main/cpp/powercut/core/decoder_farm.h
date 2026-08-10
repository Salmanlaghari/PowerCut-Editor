#pragma once
// =============================================================================
// PowerCut Core — Decoder Farm
// Manages decoders for video frames and audio samples.
// =============================================================================
#include "powercut/core/dag.h"
#include <cstdlib>
#include <cstring>

namespace PowerCut {

class DecoderFarm {
public:
    // Decode video frame for material at source time.
    RGBAFrame* get_original_frame(int mat_id, TimeMicros src_t) {
        (void)src_t;
        RGBAFrame* fr = new RGBAFrame();
        fr->width = 640; fr->height = 360; fr->stride = fr->width * 4;
        size_t sz = (size_t)fr->stride * fr->height;
        fr->data = (uint8_t*)malloc(sz);
        if (!fr->data) { delete fr; return nullptr; }
        // Fill with color based on mat_id
        uint8_t r = 60, g = 120, b = 200;
        if (mat_id == 2) { r = 200; g = 200; b = 200; }
        else if (mat_id == 3) { r = 255; g = 200; b = 0; }
        else if (mat_id == 4) { r = 100; g = 160; b = 255; }
        for (int row = 0; row < fr->height; ++row) {
            for (int col = 0; col < fr->width; ++col) {
                uint8_t* px = fr->data + row * fr->stride + col * 4;
                px[0] = r; px[1] = g; px[2] = b; px[3] = 255;
            }
        }
        return fr;
    }

    // Decode audio samples.
    PCMFrame* get_audio_samples(int mat_id, TimeMicros src_t, int num_samples) {
        (void)mat_id; (void)src_t;
        PCMFrame* pcm = new PCMFrame();
        pcm->samples = num_samples; pcm->channels = 2; pcm->sample_rate = 48000;
        pcm->data = (float*)calloc((size_t)num_samples * 2, sizeof(float));
        if (!pcm->data) { delete pcm; return nullptr; }
        return pcm;
    }
};

extern DecoderFarm* global_decoder_farm;

}  // namespace PowerCut
