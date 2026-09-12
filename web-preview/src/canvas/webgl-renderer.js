// WebGL renderer — executes the draw calls produced by FrameEvaluator.
// Texture pool, FBO chain, uniform binding, and cleanup. Shared by preview
// and export so both render the EXACT same frame.
export class WebGLRenderer {
  constructor(gl, shaderManager, options = {}) {
    this.gl = gl;
    this.sm = shaderManager;
    this.options = options;
    this.texturePool = [];
    this.fbPool = [];
    this.maxTextures = options.maxTextures || 8;
    this._initFramebuffers();
  }

  _initFramebuffers() {
    const gl = this.gl;
    this._fb = gl.createFramebuffer();
    gl.bindFramebuffer(gl.FRAMEBUFFER, this._fb);
  }

  acquireTexture(w, h) {
    const gl = this.gl;
    const t = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, t);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, null);
    return t;
  }

  bindTexture(slot, texture) {
    const gl = this.gl;
    gl.activeTexture(gl.TEXTURE0 + slot);
    gl.bindTexture(gl.TEXTURE_2D, texture);
  }

  drawCall(call, inputTexture, outputFBO, width, height) {
    const gl = this.gl;
    const node = call.node;
    const u = node.uniforms || {};
    const name = call.op;
    const program = this.sm.getProgram(name);
    if (!program) return false;
    gl.useProgram(program);

    const posLoc = gl.getAttribLocation(program, 'a_position');
    gl.bindBuffer(gl.ARRAY_BUFFER, this._quadBuffer || this._getQuadBuffer());
    gl.enableVertexAttribArray(posLoc);
    gl.vertexAttribPointer(posLoc, 4, gl.FLOAT, false, 0, 0);

    this.bindTexture(0, inputTexture);
    gl.uniform1i(gl.getUniformLocation(program, 'u_src'), 0);

    this._bindUniforms(program, name, u);
    gl.bindFramebuffer(gl.FRAMEBUFFER, outputFBO);
    gl.viewport(0, 0, width, height);
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
    return true;
  }

  _getQuadBuffer() {
    const gl = this.gl;
    const buf = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, buf);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([
      -1, -1, 0, 1, 1, -1, 0, 1, -1, 1, 0, 1, 1, 1, 0, 1
    ]), gl.STATIC_DRAW);
    this._quadBuffer = buf;
    return buf;
  }

  _bindUniforms(program, name, u) {
    const gl = this.gl;
    const set1f = (n, v) => gl.uniform1f(gl.getUniformLocation(program, n), v);
    const set3f = (n, a, b, c) => gl.uniform3f(gl.getUniformLocation(program, n), a, b, c);
    const set1i = (n, v) => gl.uniform1i(gl.getUniformLocation(program, n), v);

    switch (name) {
      case 'filter':
        if (u.colorMatrix) {
          gl.uniformMatrix4fv(gl.getUniformLocation(program, 'u_colorMatrix'), false, u.colorMatrix);
        }
        break;
      case 'chromaKey':
        set3f('u_keyColor', u.keyColorR || 0.0, u.keyColorG || 1.0, u.keyColorB || 0.0);
        set1f('u_tolerance', u.tolerance != null ? u.tolerance : 0.4);
        set1f('u_softness', u.softness != null ? u.softness : 0.3);
        set1f('u_spill', u.spill != null ? u.spill : 0.5);
        set1f('u_hasBg', u.hasBg ? 1.0 : 0.0);
        set3f('u_bgColor', u.bgColorR || 0.0, u.bgColorG || 0.0, u.bgColorB || 0.0);
        set1i('u_blendMode', u.blendMode != null ? u.blendMode : 0);
        break;
      case 'transition':
        set1f('u_progress', u.progress != null ? u.progress : 0.0);
        set1i('u_type', u.type != null ? u.type : 0);
        set1i('u_ease', u.ease != null ? u.ease : 0);
        gl.uniform2f(gl.getUniformLocation(program, 'u_dir'), u.dirX || 0.0, u.dirY || 1.0);
        set1f('u_scale', u.scale != null ? u.scale : 1.2);
        break;
      case 'vfx3d':
        set1f('u_intensity', u.intensity != null ? u.intensity : 1.0);
        set1f('u_rotation', u.rotation || 0.0);
        set1f('u_tilt', u.tilt || 0.0);
        set1f('u_aberration', u.aberration || 0.0);
        set1f('u_grain', u.grain || 0.0);
        set1f('u_scanlines', u.scanlines || 0.0);
        set1f('u_time', u.time || 0.0);
        gl.uniform2f(gl.getUniformLocation(program, 'u_resolution'), u.width || 1920, u.height || 1080);
        break;
      case 'blend':
        set1f('u_bgAlpha', u.bgAlpha != null ? u.bgAlpha : 1.0);
        set1i('u_blendMode', u.blendMode != null ? u.blendMode : 0);
        break;
    }
  }

  dispose() {
    const gl = this.gl;
    if (this._quadBuffer) gl.deleteBuffer(this._quadBuffer);
    if (this._fb) gl.deleteFramebuffer(this._fb);
    for (const t of this.texturePool) gl.deleteTexture(t);
    this.sm.dispose();
  }
}