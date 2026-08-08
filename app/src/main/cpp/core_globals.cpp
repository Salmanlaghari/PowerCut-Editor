// =============================================================================
// PowerCut Core — Global instances stub.
//
// In the full native build these are managed by the core engine lifecycle.
// This stub provides the linker symbols so the export engine and JNI bridge
// link cleanly. The pointers are null until the full core is wired.
// =============================================================================
#include "powercut/core/decoder_farm.h"
#include "powercut/core/compositor.h"

namespace PowerCut {

DecoderFarm* global_decoder_farm = nullptr;
Compositor* global_compositor = nullptr;

}  // namespace PowerCut
