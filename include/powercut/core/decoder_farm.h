#pragma once
// =============================================================================
// PowerCut Core — Decoder Farm stub header.
//
// Manages a pool of hardware/software decoders. get_original_frame() always
// decodes from the ORIGINAL source (never a proxy) at microsecond precision.
// get_audio_samples() decodes PCM audio for a given material at a timeline
// position, used by the export audio mixer.
// =============================================================================
#include "powercut/core/dag.h"

namespace PowerCut {

class DecoderFarm {
public:
    // Decode the original video frame for material mat_id at source time src_t.
    // Returns an RGBAFrame* or nullptr if decoding fails.
    RGBAFrame* get_original_frame(int mat_id, TimeMicros src_t) {
        (void)mat_id;
        (void)src_t;
        return nullptr;  // stub — full build returns decoded frame
    }

    // Decode audio samples for material mat_id at the given source time.
    // Returns a PCMFrame* with float samples (interleaved stereo) or nullptr.
    // The export engine uses this to mix all audio tracks per video frame.
    PCMFrame* get_audio_samples(int mat_id, TimeMicros src_t, int num_samples) {
        (void)mat_id; (void)src_t; (void)num_samples;
        return nullptr;  // stub — full build returns decoded PCM
    }
};

// Global decoder farm instance (defined in the full core build).
extern DecoderFarm* global_decoder_farm;

}  // namespace PowerCut
