// =============================================================================
// PowerCut Core — Global instances.
//
// Provides the global compositor and decoder farm used by the export engine
// and JNI preview bridge. Initialized with working implementations so the
// export pipeline produces real composited frames.
// =============================================================================
#include "powercut/core/decoder_farm.h"
#include "powercut/core/compositor.h"

namespace PowerCut {

// FIX: Initialize decoder farm so export engine can get source frames.
static DecoderFarm default_decoder_farm;
DecoderFarm* global_decoder_farm = &default_decoder_farm;

// FIX: Initialize compositor so render_full() produces real output.
static Compositor default_compositor;
Compositor* global_compositor = &default_compositor;

}  // namespace PowerCut
