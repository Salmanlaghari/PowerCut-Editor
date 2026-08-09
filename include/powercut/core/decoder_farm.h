#pragma once
// =============================================================================
// PowerCut Core — Decoder Farm
//
// Manages a pool of hardware/software decoders. get_original_frame() decodes
// from the ORIGINAL source at microsecond precision. get_audio_samples()
// decodes PCM audio for export audio mixing.
//
// When FFmpeg is available, uses real video decoding. Otherwise, creates
// colored placeholder frames so the compositor pipeline is testable.
// =============================================================================
#include "powercut/core/dag.h"
#include <cstdlib>
#include <cstring>
#include <cmath>

namespace PowerCut {

class DecoderFarm {
public:
    // Decode the original video frame for material mat_id at source time src_t.
    // Returns an RGBAFrame* or nullptr if decoding fails.
    // In the full build, this uses hardware-accelerated FFmpeg decoding.
    // In the stub build, creates a colored placeholder frame.
    RGBAFrame* get_original_frame(int mat_id, TimeMicros src_t) {
        (void)src_t;
        // Create a placeholder frame for testing the compositor pipeline.
        // The full build replaces this with real FFmpeg decoder output.
        RGBAFrame* fr = new RGBAFrame();
        fr->width = 640;
        fr->height = 360;
        fr->stride = fr->width * 4;
        size_t sz = (size_t)fr->stride * fr->height;
        fr->data = (uint8_t*)calloc(sz, 1);
        if (!fr->data) { delete fr; return nullptr; }

        // Fill with a color based on mat_id so different segments are visible
        uint8_t r = 0, g = 0, b = 0;
        switch (mat_id % 8) {
            case 0: r = 40; g = 40; b = 60; break;   // dark blue-gray (background)
            case 1: r = 60; g = 120; b = 200; break;  // blue (main video)
            case 2: r = 200; g = 200; b = 200; break;  // light gray (text)
            case 3: r = 255; g = 200; b = 0; break;    // gold (sticker)
            case 4: r = 100; g = 160; b = 255; break;  // light blue (overlay)
            case 5: r = 80; g = 80; b = 80; break;     // gray (audio/BGM)
            case 6: r = 0; g = 200; b = 100; break;    // green (chroma-key)
            case 7: r = 180; g = 60; b = 60; break;    // red (effect)
        }
        for (int row = 0; row < fr->height; ++row) {
            for (int col = 0; col < fr->width; ++col) {
                uint8_t* px = fr->data + row * fr->stride + col * 4;
                px[0] = r; px[1] = g; px[2] = b; px[3] = 255;
            }
        }
        return fr;
    }

    // Decode audio samples for material mat_id at the given source time.
    // Returns a PCMFrame* with float samples (interleaved stereo) or nullptr.
    // The export engine uses this to mix all audio tracks per video frame.
    PCMFrame* get_audio_samples(int mat_id, TimeMicros src_t, int num_samples) {
        (void)mat_id; (void)src_t;
        // Create a silent PCM frame. The full build uses FFmpeg audio decoding.
        PCMFrame* pcm = new PCMFrame();
        pcm->samples = num_samples;
        pcm->channels = 2;
        pcm->sample_rate = 48000;
        pcm->data = (float*)calloc((size_t)num_samples * 2, sizeof(float));
        if (!pcm->data) { delete pcm; return nullptr; }
        // Data is zeroed (silence) — real audio comes from FFmpeg decoder
        return pcm;
    }
};

// Global decoder farm instance (defined in core_globals.cpp).
extern DecoderFarm* global_decoder_farm;

}  // namespace PowerCut
