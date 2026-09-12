// shader-manager.js — Compiles and caches GLSL fragment shaders, validates
// programs, and passes effect parameters dynamically as uniforms/textures.
// Shared by preview (main thread) and export (worker) so both compile the
// EXACT same program graph.
import {
  CHROMA_KEY_FRAG, FILTER_FRAG, TRANSITION_FRAG, VFX3D_FRAG, BLEND_FRAG,
  SCREEN_VERT
} from './shader-sources.js';

export class ShaderManager {
  constructor(gl, options = {}) {
    this.gl = gl;
    this.options = options;
    this.programs = new Map();      // name -> WebGLProgram
    this.shaders = new Map();       // name -> WebGLShader
    this.errors = [];
    this.onCompileError = options.onCompileError || (() => {});
  }

  // Fetch a shader source by name. Override this to load from a different
  // location (e.g. a remote shader server) without changing callers.
  getShaderSource(name) {
    return {
      chromaKey: CHROMA_KEY_FRAG,
      filter: FILTER_FRAG,
      transition: TRANSITION_FRAG,
      vfx3d: VFX3D_FRAG,
      blend: BLEND_FRAG,
      screen: SCREEN_VERT
    }[name];
  }

  compileShader(type, source) {
    const gl = this.gl;
    const shader = gl.createShader(type);
    if (!shader) return null;
    gl.shaderSource(shader, source);
    gl.compileShader(shader);
    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
      const log = gl.getShaderInfoLog(shader);
      gl.deleteShader(shader);
      const err = `${type === gl.VERTEX_SHADER ? 'vertex' : 'fragment'} compile error: ${log}`;
      this.errors.push(err);
      this.onCompileError(err);
      return null;
    }
    return shader;
  }

  getProgram(name, vertName = 'screen') {
    const cacheKey = `${vertName}+${name}`;
    if (this.programs.has(cacheKey)) return this.programs.get(cacheKey);

    const gl = this.gl;
    const vertSrc = this.getShaderSource(vertName);
    const fragSrc = this.getShaderSource(name);
    if (!vertSrc || !fragSrc) return null;

    const vert = this.compileShader(gl.VERTEX_SHADER, vertSrc);
    const frag = this.compileShader(gl.FRAGMENT_SHADER, fragSrc);
    if (!vert || !frag) return null;

    const program = gl.createProgram();
    if (!program) return null;
    gl.attachShader(program, vert);
    gl.attachShader(program, frag);
    gl.linkProgram(program);

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      const log = gl.getProgramInfoLog(program);
      const err = `program link error (${name}): ${log}`;
      this.errors.push(err);
      this.onCompileError(err);
      gl.deleteProgram(program);
      return null;
    }

    // Validate once (cheap sanity check on the active program).
    gl.validateProgram(program);
    this.programs.set(cacheKey, program);
    return program;
  }

  // Uniform location cache keyed by program name + uniform name.
  getUniformLocation(program, name) {
    return this.gl.getUniformLocation(program, name);
  }

  dispose() {
    for (const p of this.programs.values()) this.gl.deleteProgram(p);
    for (const s of this.shaders.values()) this.gl.deleteShader(s);
    this.programs.clear();
    this.shaders.clear();
  }
}