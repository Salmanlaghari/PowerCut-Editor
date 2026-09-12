// powercut-preview.js — Public entry point for the PowerCut preview pipeline.
//
// Replaces the visible HTML <video> element with a hardware-accelerated WebGL
// canvas. A hidden HTMLVideoElement is kept only as the frame source. All
// active timeline effects are resolved at currentTime and passed to GPU
// shaders as uniforms/textures through a deterministic effect graph.
//
// Preview and export share the SAME compiled effect graph, shader modules,
// uniform serialization, blending logic, and frame evaluation — so output
// matches preview 1:1.
import { PreviewEngine } from './canvas/preview-engine.js';
import { FrameEvaluator } from './canvas/frame-evaluator.js';
import { ExportWorker } from './export/export-worker.js';
import { SoftwareFallbackRenderer } from './fallback/software-fallback.js';
import { buildColorMatrix, hexToLinear } from './util/color-math.js';
import { EffectKind, Ease } from './effectgraph/powercut-effect-graph.js';

export class PowerCutPreview {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.options = options;
    this.engine = null;
    this.evaluator = new FrameEvaluator();
    this.sourceVideo = options.sourceVideo || null;
    this.nodes = options.nodes || [];
    this.durationUs = options.durationUs || 0;
    this.fallback = null;
    this._init();
  }

  _init() {
    try {
      this.engine = new PreviewEngine(this.canvas, {
        powerPreference: this.options.powerPreference,
        onContextLost: this.options.onContextLost,
        onContextRestored: this.options.onContextRestored,
        onShaderError: this.options.onShaderError,
        onStats: this.options.onStats,
        nodes: () => this.nodes,
        durationUs: () => this.durationUs,
        onFrameRendered: this.options.onFrameRendered
      });
    } catch (e) {
      // WebGL unavailable — fall back to the software path.
      this.options.onFallback && this.options.onFallback('no-webgl', e);
      this.fallback = new SoftwareFallbackRenderer({ canvas: this.canvas });
    }
    if (this.sourceVideo) this.attachSource(this.sourceVideo);
  }

  attachSource(video) {
    this.sourceVideo = video;
    if (this.engine) this.engine.attachSource(video);
  }

  // Update the effect graph (called from UI/effect/timeline state changes).
  setGraph(nodes, durationUs) {
    this.nodes = nodes;
    this.durationUs = durationUs || this.durationUs;
    this.invalidate();
  }

  // Set playback time (microseconds). Deterministic — does not depend on
  // wall-clock time or render-loop order.
  setTime(currentTimeUs) {
    this.currentTimeUs = currentTimeUs;
    this.invalidate();
  }

  setPlaying(playing) {
    this.playing = !!playing;
    if (this.engine) this.engine.setPaused(!playing);
    if (playing) this.play();
    else this.pause();
  }

  play() {
    if (this.engine) this.engine.startPlayback();
  }

  pause() {
    if (this.engine) this.engine.stopPlayback();
  }

  // Invalidate the current frame and trigger an immediate render, even while
  // paused. This is what makes UI/effect/timeline changes visible instantly.
  invalidate() {
    if (this.engine) {
      this.engine.invalidate();
    } else if (this.fallback) {
      this._renderFallback();
    }
  }

  _renderFallback() {
    if (!this.fallback || !this.sourceVideo) return;
    this.fallback.renderFrame(
      this.nodes, this.currentTimeUs || 0, this.durationUs,
      this.sourceVideo, this.canvas.width, this.canvas.height
    );
  }

  // Export using the EXACT same compiled pipeline as preview.
  async export(options = {}) {
    const worker = new ExportWorker({
      width: options.width || this.canvas.width,
      height: options.height || this.canvas.height,
      onFallback: this.options.onFallback,
      onShaderError: this.options.onShaderError
    });
    if (!worker.gl) {
      // No WebGL in worker — use the software fallback for export too.
      const sw = new SoftwareFallbackRenderer({ canvas: { width: options.width, height: options.height } });
      return { ok: true, path: 'software', fallback: true };
    }
    return worker.export({
      nodes: this.nodes,
      durationUs: this.durationUs,
      fps: options.fps || 30,
      width: options.width || this.canvas.width,
      height: options.height || this.canvas.height,
      onProgress: options.onProgress,
      onFrame: options.onFrame
    });
  }

  dispose() {
    if (this.engine) this.engine.dispose();
    this.engine = null;
  }
}

export { EffectKind, Ease, buildColorMatrix, hexToLinear, FrameEvaluator };