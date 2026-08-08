// =============================================================================
// PowerCut Core — Global instances stub.
//
// In the full native build these are managed by the core engine lifecycle.
// This stub provides the linker symbols so the export engine and JNI bridge
// link cleanly. When the full compositor is unavailable, we fall back to an
// inline software compositor for timeline export.
// =============================================================================
#include "powercut/core/decoder_farm.h"
#include "powercut/core/compositor.h"

namespace PowerCut {

DecoderFarm* global_decoder_farm = nullptr;
static Compositor default_compositor;
Compositor* global_compositor = &default_compositor;

}  // namespace PowerCut
