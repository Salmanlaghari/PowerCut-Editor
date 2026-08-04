#pragma once
// =============================================================================
// PowerCut Core — Decoder Farm stub header.
//
// Manages a pool of hardware/software decoders. get_original_frame() always
// decodes from the ORIGINAL source (never a proxy) at microsecond precision.
// =============================================================================
#include "powercut/core/dag.h"

namespace PowerCut {

class DecoderFarm {
public:
    // Decode the original frame for material mat_id at source time src_t.
    // Returns an RGBAFrame* or nullptr if decoding fails.
    RGBAFrame* get_original_frame(int mat_id, TimeMicros src_t) {
        (void)mat_id;
        (void)src_t;
        return nullptr;  // stub — full build returns decoded frame
    }
};

// Global decoder farm instance (defined in the full core build).
extern DecoderFarm* global_decoder_farm;

}  // namespace PowerCut
