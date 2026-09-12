# PowerCut Android Integration Guide

This guide wires the WebGL preview pipeline (`web-preview/`) into the existing
Android app (`app/src/main/java/com/powercut/editor/`).

## Architecture

```
Hidden HTMLVideoElement (frame source)
    |
    v
requestVideoFrameCallback / requestAnimationFrame
    |
    v
WebGL Canvas (replaces visible <video>)
    |
    v
ShaderManager (compile + cache GLSL, validate programs)
    |
    v
FrameEvaluator (deterministic effect-graph traversal)
    |
    v
WebGLRenderer (texture pool, FBO chain, uniform binding)
    |
    v
PreviewLoop (frame-sync, paused-frame invalidation)
```

Preview and export share the SAME `FrameEvaluator`, `ShaderManager`,
`WebGLRenderer`, and `resolveGraph` — so output matches preview 1:1.

## File-level integration steps

### 1. Replace the visible video element with a WebGL canvas

In `NextGenEditorScreen.kt`, replace the `AndroidView({ PlayerView })` block
at lines 1197-1213 with a `PowerCutWebGLPreview` Composable:

```kotlin
// The visible element is now a hardware-accelerated WebGL canvas.
// The ExoPlayer is retained ONLY as the hidden frame source.
PowerCutWebGLPreview(
    canvas = canvas,
    sourceVideo = exoPlayer, // hidden, used only as frame source
    nodes = vm.effectGraph.value,
    durationUs = vm.durationUs.value,
    currentTimeUs = vm.playheadUs.value,
    isPlaying = isPlaying,
    onFrameRendered = { /* update scrubber */ }
)
```

### 2. Add the shared effect-graph state to EditorViewModel

Add a `MutableStateFlow<List<EffectNode>>` that holds the canonical graph.
Every UI/effect/timeline mutation updates it and calls `invalidate()`:

```kotlin
private val _effectGraph = MutableStateFlow<List<EffectNode>>(emptyList())
val effectGraph: StateFlow<List<EffectNode>> = _effectGraph.asStateFlow()

fun updateFilter(filterId: String) {
    pushUndoState()
    projectRepository.updateProject { it.copy(selectedFilter = filterId) }
    rebuildGraph()
    invalidatePreview() // triggers immediate re-render, even while paused
}
```

### 3. Wire chroma key through the GPU shader

Replace `Media3EffectPipeline.buildChromaDesaturateEffect` (a documented
approximation) with a `ChromaKey` node in the effect graph. The GPU shader
(`powercut-chroma-key.frag`) removes the key color using tolerance, edge
softness, spill suppression, and blends over a background track.

### 4. Wire transitions through dual-texture blending

For overlapping clips, add a `Transition` node with `progress`, `type`, `ease`,
`dir`, `scale`. The `powercut-transition.frag` shader blends the two source
textures using the normalized overlap progress and explicit easing.

### 5. Wire animations through keyframe evaluation

Replace the per-property `evaluateKeyframes` in `NextGenEditorScreen.kt` with
the shared `evaluateKeyframes` from `powercut-effect-graph.js`. Keyframes are
evaluated at `currentTimeUs` (deterministic) and converted to uniforms.

### 6. Export worker uses the SAME pipeline

`ExportWorker` imports the same `FrameEvaluator`, `ShaderManager`, and
`WebGLRenderer` as preview. The serialized graph payload
(`serializeEffectGraph`) is versioned and shared.

### 7. Fallback path

When WebGL/OffscreenCanvas is unavailable, `SoftwareFallbackRenderer` applies
the same color matrix and chroma-key math in 2D canvas. Output still matches
preview (lower performance).

## Compatibility notes

- **WebGL**: requires WebGL 1 (ES 2.0) or WebGL 2. `powerPreference:
  'high-performance'` is passed to `getContext`.
- **requestVideoFrameCallback**: preferred; falls back to `requestAnimationFrame`.
- **CORS / cross-origin isolation**: the hidden `<video>` source must be
  same-origin or CORS-enabled. `crossOrigin="anonymous"` is set automatically
  by `attachSource`. For opaque cross-origin video, the WebGL readback path
  is unavailable — use the software fallback.
- **Hardware acceleration**: `preserveDrawingBuffer: false` (no double-buffer
  overhead). Context loss is handled via `webglcontextlost`/`restored`.
- **devicePixelRatio**: the canvas backing-store is sized to
  `clientWidth * devicePixelRatio` to avoid blur on high-DPR screens.
- **Texture cleanup**: textures and FBOs are released on `dispose()` and on
  context loss. Shader programs are cached per name and rebuilt on restore.

## Test plan

See `web-preview/TEST_PLAN.md`. Run with:

```bash
cd web-preview && npm test
```

All 18 tests must pass before integration is considered complete.