// preview-engine.js — Full preview engine wiring.
//
// Composes: hidden HTMLVideoElement (frame source) -> WebGL canvas ->
// ShaderManager -> WebGLRenderer -> FrameEvaluator -> PreviewLoop.
//
// This is the component the UI mounts. It replaces the visible <video> element
// with a hardware-accelerated WebGL canvas and renders every processed frame
// through the deterministic effect graph.
export class PreviewEngine {
  constructor(canvas, options = {}) {
    this.canvas = canvas;
    this.options = options;
    this.gl = null;
    this.sm = null;
    this.renderer = null;
    this.evaluator = null;
    this.loop = null;
    this.sourceVideo = null;
    this.contextLost = false;
    this._init();
  }

  _init() {
    const attrs = {
      preserveDrawingBuffer: false,
      antialias: false,
      stencil: false,
      depth: false,
      powerPreference: this.options.powerPreference || 'high-performance'
    };
    let gl = this.canvas.getContext('webgl2', attrs);
    if (!gl) {
      gl = this.canvas.getContext('webgl', attrs);
      if (!gl) throw new Error('WebGL unavailable — preview engine cannot start');
    }
    this.gl = gl;
    this.sm = new (this.options.ShaderManager || ShaderManager)(gl, {
      onCompileError: this.options.onShaderError
    });
    this.renderer = new WebGLRenderer(gl, this.sm, this.options);
    this.evaluator = new FrameEvaluator();
    this._setupContextLoss();
    this._buildScreenQuad();
  }

  _setupContextLoss() {
    this.canvas.addEventListener('webglcontextlost', (e) => {
      e.preventDefault();
      this.contextLost = true;
      this.options.onContextLost && this.options.onContextLost();
    });
    this.canvas.addEventListener('webglcontextrestored', () => {
      this._rebuildAfterLoss();
      this.options.onContextRestored && this.options.onContextRestored();
    });
  }

  _rebuildAfterLoss() {
    this.sm = new ShaderManager(this.gl, { onCompileError: this.options.onShaderError });
    this.renderer = new WebGLRenderer(this.gl, this.sm, this.options);
    this.contextLost = false;
  }

  _buildScreenQuad() {
    const gl = this.gl;
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
      -1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1
    ]), gl.STATIC_DRAW);
    this._quadBuffer = buf;
  }

  // Bind a hidden HTMLVideoElement as the frame source (never displayed).
  attachSource(video) {
    this.sourceVideo = video;
    video.style.display = 'none';
    video.setAttribute('aria-hidden', 'true');
    if (this.loop) this.loop.video = video;
  }

  // Start the frame-sync loop. Falls back to requestAnimationFrame when
  // requestVideoFrameCallback is unavailable.
  startPlayback() {
    if (this.loop) {
      this.loop.start();
      return;
    }
    this.loop = new PreviewLoop(this.sourceVideo, this, {
      onFrame: (t, dirty) => this._onFrame(t, dirty),
      onStats: this.options.onStats
    });
    this.loop.start();
  }

  stopPlayback() {
    if (this.loop) this.loop.stop();
  }

  setPaused(p) {
    if (this.loop) this.loop.setPaused(p);
  }

  // Invalidate the current frame and re-render immediately (even while paused).
  invalidate() {
    if (this.loop) this.loop.invalidate();
    else this._render();
  }

  // Evaluate the effect graph at currentTimeUs and render one frame.
  _onFrame(currentTimeUs, dirty) {
    if (this.contextLost) return;
    const nodes = this.options.nodes || [];
    const durationUs = this.options.durationUs || 0;
    const calls = this.evaluator.evaluate(nodes, currentTimeUs, durationUs, null);
    this._renderCalls(calls, currentTimeUs);
    this.options.onFrameRendered && this.options.onFrameRendered(currentTimeUs);
  }

  _render() {
    const t = this.loop ? this.loop.lastFrameTime : 0;
    this._onFrame(t, true);
  }

  _renderCalls(calls, timeUs) {
    const gl = this.gl;
    const w = this.canvas.width || 1920;
    const h = this.canvas.height || 1080;
    // Render each draw call into a ping-pong FBO chain.
    let current = this._getSourceTexture();
    for (let i = 0; i < calls.length; i++) {
      const call = calls[i];
      const out = this._getFBO(i);
      this.renderer.drawCall(call, current, out, w, h);
      current = out.texture;
    }
    // Present the final result to the screen.
    gl.bindFramebuffer(gl.FRAMEBUFFER, null);
    gl.viewport(0, 0, w, h);
    gl.clearColor(0, 0, 0, 1);
    gl.clear(gl.COLOR_BUFFER_BIT);
    if (current) {
      this._presentTexture(current);
    }
  }

  _getSourceTexture() {
    if (!this._sourceTex) {
      this._sourceTex = this.renderer.acquireTexture(
        this.canvas.width || 1920, this.canvas.height || 1080
      );
    }
    return this._sourceTex;
  }

  _getFBO(index) {
    if (!this._fbos) this._fbos = [];
    if (!this._fbos[index]) {
      const gl = this.gl;
      const fb = gl.createFramebuffer();
      const tex = this.renderer.acquireTexture(
        this.canvas.width || 1920, this.canvas.height || 1080
      );
      gl.bindFramebuffer(gl.FRAMEBUFFER, fb);
      gl.framebufferTexture2D(gl.FRAMEBUFFER, gl.COLOR_ATTACHMENT0, gl.TEXTURE_2D, tex, 0);
      this._fbos[index] = { framebuffer: fb, texture: tex };
    }
    return this._fbos[index];
  }

  _presentTexture(texture) {
    const gl = this.gl;
    const program = this.sm.getProgram('blend') || this.sm.getProgram('filter');
    if (!program) return;
    gl.useProgram(program);
    gl.bindBuffer(gl.ARRAY_BUFFER, this._quadBuffer);
    const posLoc = gl.getAttribLocation(program, 'a_position');
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 4, gl.FLOAT, false, 0, 0);
    gl.activeTexture(gl.TEXTURE0);
    gl.bindTexture(gl.TEXTURE_2D, texture);
    const u = gl.getUniformLocation(program, 'u_src');
    if (u) gl.uniform1i(u, 0);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
  }

  dispose() {
    this.stopPlayback();
    if (this.loop) this.loop = null;
    if (this.renderer) this.renderer.dispose();
    if (this.sm) this.sm.dispose();
  }
}

// Re-export for tree-shaking convenience.
export { ShaderManager } from './shader-manager.js';
export { WebGLRenderer } from './webgl-renderer.js';
export { FrameEvaluator } from './frame-evaluator.js';
export { PreviewLoop } from './preview-loop.js';