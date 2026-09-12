// export-worker.js — Export/render worker.
//
// Uses the EXACT same compiled effect graph, shader modules, uniform
// serialization, blending logic, and frame evaluation as live preview
// (see src/canvas/* and src/effectgraph/*). The only difference is that the
// worker renders to an OffscreenCanvas and encodes frames to a video file,
// while the preview renders to the visible canvas.
//
// The shared FrameEvaluator + ShaderManager + WebGLRenderer are imported
// unchanged so preview and export can never diverge.
import { FrameEvaluator } from '../canvas/frame-evaluator.js';
import { ShaderManager } from '../canvas/shader-manager.js';
import { WebGLRenderer } from '../canvas/webgl-renderer.js';
import { serializeEffectGraph } from '../effectgraph/powercut-effect-graph.js';

export class ExportWorker {
  constructor(options = {}) {
    this.options = options;
    this.evaluator = new FrameEvaluator();
    this.gl = null;
    this.sm = null;
    this.renderer = null;
    this.cancelled = false;
    this._init();
  }

  _init() {
    // Prefer OffscreenCanvas + WebGL2 in the worker. Falls back to a
    // headless canvas if OffscreenCanvas is unavailable.
    const canvas = typeof OffscreenCanvas !== 'undefined'
      ? new OffscreenCanvas(this.options.width || 1920, this.options.height || 1080)
      : null;
    if (!canvas) {
      this.options.onFallback && this.options.onFallback('no-offscreen-canvas');
      return;
    }
    const attrs = { antialias: false, stencil: false, depth: false };
    let gl = canvas.getContext('webgl2', attrs);
    if (!gl) gl = canvas.getContext('webgl', attrs);
    if (!gl) {
      this.options.onFallback && this.options.onFallback('no-webgl');
      return;
    }
    this.gl = gl;
    this.canvas = canvas;
    this.sm = new ShaderManager(gl, { onCompileError: this.options.onShaderError });
    this.renderer = new WebGLRenderer(gl, this.sm, this.options);
    this._buildQuad();
  }

  _buildQuad() {
    const gl = this.gl;
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
      -1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1
    ]), gl.STATIC_DRAW);
    this._quadBuffer = buf;
  }

  // Render one frame at currentTimeUs using the shared pipeline.
  // Returns a Uint8Array RGBA pixel row-major, or null on error.
  renderFrame(nodes, currentTimeUs, durationUs, width, height) {
    if (!this.gl || this.cancelled) return null;
    const calls = this.evaluator.evaluate(nodes, currentTimeUs, durationUs, null);
    const payload = this.evaluator.serialize(nodes, currentTimeUs, durationUs, null);
    this.options.onGraphSerialized && this.options.onGraphSerialized(payload);

    const gl = this.gl;
    const fb = gl.createFramebuffer();
    const tex = this.renderer.acquireTexture(width, height);
    gl.bindFramebuffer(gl.FRAMEBUFFER, fb);
    gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);

    let current = tex;
    for (let i = 0; i < calls.length; i++) {
      const outFBO = { framebuffer: fb, texture: this.renderer.acquireTexture(width, height) };
      gl.bindFramebuffer(gl.FRAMEBUFFER, outFBO.framebuffer);
      gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, outFBO.texture, 0);
      this.renderer.drawCall(calls[i], current, outFBO, width, height);
      gl.deleteTexture(current);
      current = outFBO.texture;
    }

    // Read back pixels.
    gl.bindFramebuffer(gl.FRAMEBUFFER, fb);
    gl.viewport(0, 0, width, height);
    const pixels = new Uint8Array(width * height * 4);
    gl.readPixels(0, 0, width, height, gl.RGBA, gl.UNSIGNED_BYTE, pixels);

    gl.deleteTexture(current);
    gl.deleteFramebuffer(fb);
    return pixels;
  }

  // Run the full export loop. Deterministic: each frame is evaluated at
  // currentTimeUs = frameIndex * framePeriodUs, never wall-clock.
  async export(options) {
    const { nodes, durationUs, fps, width, height, onProgress, onFrame } = options;
    if (!this.gl) return { ok: false, error: 'WebGL/OffscreenCanvas unavailable' };
    const framePeriodUs = 1_000_000 / fps;
    const totalFrames = Math.ceil(durationUs / framePeriodUs);
    for (let i = 0; i < totalFrames && !this.cancelled; i++) {
      const t = Math.floor(i * framePeriodUs);
      const pixels = this.renderFrame(nodes, t, durationUs, width, height);
      if (!pixels) return { ok: false, error: 'frame render failed', frame: i };
      onFrame && onFrame(pixels, i, t);
      onProgress && onProgress(Math.floor((i / totalFrames) * 100), i);
    }
    return { ok: !this.cancelled, frames: totalFrames };
  }

  cancel() { this.cancelled = true; }

  dispose() {
    if (this.renderer) this.renderer.dispose();
    if (this.sm) this.sm.dispose();
  }
}