// preview-loop.js — Active frame-sync loop using requestVideoFrameCallback with
// requestAnimationFrame fallback. Draws the current video frame to the WebGL
// context on every clock tick. Preserves a hidden HTMLVideoElement only as the
// frame source; never displays it.
export class PreviewLoop {
  constructor(video, engine, options = {}) {
    this.video = video;
    this.engine = engine;
    this.options = options;
    this.onFrame = options.onFrame || (() => {});
    this.onStats = options.onStats || (() => {});
    this.running = false;
    this.paused = false;
    this.frameDirty = true;
    this.lastFrameTime = 0;
    this.frameCount = 0;
    this.fpsSum = 0;
    this.fpsSamples = 0;
    this._rvfcHandle = null;
    this._rafHandle = null;
    this._useRaf = false;
  }

  start() {
    if (this.running) return;
    this.running = true;
    this.paused = false;
    this._schedule();
  }

  stop() {
    this.running = false;
    if (this._rvfcHandle != null && typeof this.video.cancelVideoFrameCallback === 'function') {
      this.video.cancelVideoFrameCallback(this._rvfcHandle);
      this._rvfcHandle = null;
    }
    if (this._rafHandle != null) {
      cancelAnimationFrame(this._rafHandle);
      this._rafHandle = null;
    }
  }

  setPaused(p) {
    this.paused = !!p;
    // Paused state still invalidates the frame on demand via invalidate().
  }

  // Force a re-render of the CURRENT frame even while paused.
  invalidate() {
    this.frameDirty = true;
    if (this.paused) {
      this._render();
    }
  }

  _schedule() {
    if (!this.running) return;
    if (typeof this.video.requestVideoFrameCallback === 'function') {
      this._useRaf = false;
      this._rvfcHandle = this.video.requestVideoFrameCallback((now, meta) => {
        this.lastFrameTime = meta.mediaTime;
        this.frameDirty = true;
        this._render();
        this._schedule();
      });
    } else {
      this._useRaf = true;
      this._rafHandle = requestAnimationFrame((ts) => {
        if (!this.running) return;
        this.lastFrameTime = ts / 1000;
        this.frameDirty = true;
        this._render();
        this._schedule();
      });
    }
  }

  _render() {
    const t0 = performance.now();
    this.onFrame(this.lastFrameTime, this.frameDirty);
    this.frameDirty = false;
    const dt = performance.now() - t0;
    this.frameCount++;
    this.fpsSum += dt;
    this.fpsSamples++;
    if (this.fpsSamples >= 30) {
      const avg = this.fpsSum / this.fpsSamples;
      this.onStats({ frame: this.frameCount, avgRenderMs: avg, fps: 1000 / avg });
      this.fpsSum = 0;
      this.fpsSamples = 0;
    }
  }
}