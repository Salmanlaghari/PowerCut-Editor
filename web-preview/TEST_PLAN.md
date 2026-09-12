# PowerCut Preview Pipeline — Test Plan

Covers: paused-frame updates, transitions, chroma key, animations, context loss, and preview/export parity.

## 1. Paused-frame updates
- Set up a project with a filter active, pause playback, then change the filter parameter.
- Assert: the current frame re-renders immediately (no play required) and the new parameter is visible.
- Determinism check: evaluate the graph at the same currentTime twice; the two draw-call lists must be byte-identical.
- Regression: while playing, parameter changes must not cause frame drops or reordering.

## 2. Transitions
- Create two overlapping clips with a dissolve transition; set progress = 0.5.
- Assert: the output frame is a 50/50 blend of clip A and clip B (sample pixels at the overlap region).
- Easing check: progress 0.0 -> 0.25 with EASE_IN_OUT must produce a value of 0.15625 (ease(frac) = frac*frac*(3 - 2*frac)); verify the shader uniform matches the JS `ease()` function).
- Directional wipe: with dir=(0,1), progress=1.0 must fully reveal clip B.

## 3. Chroma Key
- Feed a frame with a pure green background; set keyColor=green, tolerance=0.3, softness=0.2.
- Assert: background pixels have alpha ~0; subject pixels have alpha ~1.
- Edge softness: at the color-distance boundary, alpha must transition smoothly over the softness band (not a hard cliff).
- Spill suppression: a green-tinted skin pixel must retain more of its red/blue channels than without spill.
- Background blend: with hasBg=1 and a background texture, keyed-out pixels must show the background color, not black.

## 4. Animations (keyframes)
- Add keyframes: opacity 0@0us, 1@500us, 0@1000us with EASE_IN_OUT.
- Assert: at t=0 -> 0, t=125us -> ~0.0156, t=250us -> ~0.15625, t=500us -> 1, t=750us -> ~0.84375, t=1000us -> 0.
- Property binding: changing the `scale` keyframe must update the `u_intensity`/matrix uniform on the next render.
- Determinism: the same keyframe set evaluated at the same t must always return the same value.

## 5. Context loss
- Dispatch `webglcontextlost` on the canvas; assert the engine sets `contextLost=true` and calls `onContextLost`.
- Dispatch `webglcontextrestored`; assert programs/textures/FBOs are rebuilt and `onContextRestored` fires.
- After restoration, render one frame and assert it is not blank (no stale black frame).

## 6. Preview/export parity
- Build a graph with filter + chroma key + blend; render one frame via the preview engine and via the export worker.
- Assert: the two RGBA outputs are identical (byte-for-byte) for the same currentTimeUs.
- Assert: the serialized graph payload from `serializeEffectGraph` is identical between preview and export (same version, same node order, same uniforms).
- Assert: the export worker uses the same `ShaderManager`/`WebGLRenderer`/`FrameEvaluator` modules as preview (import identity check).

## 7. WebGL fallback
- Mock a context where `canvas.getContext('webgl')` returns null.
- Assert: `PowerCutPreview` falls back to `SoftwareFallbackRenderer` and still produces a non-null frame.
- Assert: the software fallback applies the same color matrix as the GPU path (compare against `buildColorMatrix`).

## 8. Hidden video element
- Assert: the source `<video>` has `display:none` and `aria-hidden=true` and is never painted to the screen.
- Assert: the visible element is the WebGL `<canvas>`, not the `<video>`.

## 9. Performance / hardware
- Assert: the preview loop uses `requestVideoFrameCallback` when available (check `_useRaf === false`), and falls back to `requestAnimationFrame` otherwise.
- Assert: `powerPreference: 'high-performance'` is passed to `getContext`.
- Assert: `preserveDrawingBuffer` is false (no double-buffering overhead).

## 10. Run
```bash
cd web-preview && npm test
```
All tests must pass before the integration is considered complete.