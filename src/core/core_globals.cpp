// =============================================================================
// PowerCut Core — Global instances.
//
// Provides the global compositor and decoder farm used by the export engine
// and JNI preview bridge. The compositor processes ALL timeline effects;
// the decoder farm provides source frames for the compositor.
// =============================================================================
#include "powercut/core/decoder_farm.h"
#include "powercut/core/compositor.h"

namespace PowerCut {

// FIX: Initialize decoder farm so export engine can get source frames.
// In the full build this is replaced by a real FFmpeg-backed decoder.
static DecoderFarm default_decoder_farm;
DecoderFarm* global_decoder_farm = &default_decoder_farm;

static Compositor default_compositor;
Compositor* global_compositor = &default_compositor;

}  // namespace PowerCut
