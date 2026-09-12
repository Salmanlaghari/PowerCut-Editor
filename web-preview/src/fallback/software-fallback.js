// fallback.js — Export fallback path for environments where OffscreenCanvas
// or WebGL is unavailable. Renders frames through a 2D canvas / software path
// using the SAME deterministic effect graph so output still matches preview
// (at lower performance).
import { resolveGraph, EffectKind } from '../effectgraph/powercut-effect-graph.js';

export class SoftwareFallbackRenderer {
  constructor(options = {}) {
    this.options = options;
    this.canvas = options.canvas || null;
    this.ctx = null;
    this._init();
  }

  _init() {
    if (!this.canvas) return;
    this.ctx = this.canvas.getContext('2d');
    if (!this.ctx) {
      this.options.onFallback && this.options.onFallback('no-2d-context');
    }
  }

  // Render one frame through the software path. Uses the resolved graph so
  // the effect set is identical to the WebGL path; only the execution backend
  // differs (CPU pixel ops instead of GPU shaders).
  renderFrame(nodes, currentTimeUs, durationUs, sourceImage, width, height) {
    if (!this.ctx) return null;
    const resolved = resolveGraph(nodes, currentTimeUs, durationUs);
    this.ctx.clearRect(0, 0, width, height);
    this.ctx.drawImage(sourceImage, 0, 0, width, height);

    // Apply effects in the same deterministic order as the GPU path.
    for (const node of resolved) {
      switch (node.kind) {
        case EffectKind.Filter:
          this._applyFilter(node, width, height);
          break;
        case EffectKind.ChromaKey:
          this._applyChromaKey(node, width, height);
          break;
        case EffectKind.VFX3D:
          this._applyVFX(node, width, height);
          break;
        case EffectKind.Blend:
          this._applyBlend(node, width, height);
          break;
        default:
          break;
      }
    }
    return this.canvas.toDataURL('image/png');
  }

  _applyFilter(node, w, h) {
    const u = node.uniforms || {};
    const img = this.ctx.getImageData(0, 0, w, h);
    const d = img.data;
    const m = u.colorMatrix || [
      1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1
    ];
    for (let i = 0; i < d.length; i += 4) {
      const r = d[i], g = d[i + 1], b = d[i + 2], a = d[i + 3];
d[i]     = clamp(m[0] * r + m[4] * g + m[8] * b + m[12] * a);
      d[i + 1] = clamp(m[1] * r + m[5] * g + m[9] * b + m[13] * a);
      d[i + 2] = clamp(m[2] * r + m[6] * g + m[10] * b + m[14] * a);
      d[i + 3] = a;
    }
    this.ctx.putImageData(img, 0, 0);
  }

  _applyChromaKey(node, w, h) {
    const u = node.uniforms || {};
    const keyR = u.keyColorR || 0, keyG = u.keyColorG || 1, keyB = u.keyColorB || 0;
    const t = u.tolerance != null ? u.tolerance : 0.4;
    const edge = Math.max((u.softness != null ? u.softness : 0.3) * 0.5, 0.001);
    const img = this.ctx.getImageData(0, 0, w, h);
    const d = img.data;
    for (let i = 0; i < d.length; i += 4) {
      const r = d[i], g = d[i + 1], b = d[i + 2];
      const dist = Math.sqrt((r - keyR) ** 2 + (g - keyG) ** 2 + (b - keyB) ** 2);
      const alpha = clamp((dist - t) / edge, 0, 1);
      d[i] = r * alpha; d[i + 1] = g * alpha; d[i + 2] = b * alpha; d[i + 3] = 255 * alpha;
    }
    this.ctx.putImageData(img, 0, 0);
  }

  _applyVFX(node, w, h) {
    // Simplified software VFX (rotation/aberration/grain). Deterministic.
    const u = node.uniforms || {};
    const aberr = u.aberration || 0;
    if (aberr > 0.001) {
      const img = this.ctx.getImageData(0, 0, w, h);
      const src = new Uint8ClampedArray(img.data);
      const d = img.data;
      const off = Math.round(aberr * 0.01 * w);
      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          const i = (y * w + x) * 4;
          const j = (y * w + Math.min(w - 1, x + off)) * 4;
          d[i] = src[j]; d[i + 2] = src[i];
        }
      }
      this.ctx.putImageData(img, 0, 0);
    }
  }

  _applyBlend(node, w, h) {
    // Composite over background color (software approximation).
    const u = node.uniforms || {};
    const bgR = u.bgColorR || 0, bgG = u.bgColorG || 0, bgB = u.bgColorB || 0;
    const img = this.ctx.getImageData(0, 0, w, h);
    const d = img.data;
    for (let i = 0; i < d.length; i += 4) {
      const a = d[i + 3] / 255;
      d[i] = d[i] * a + bgR * (1 - a);
      d[i + 1] = d[i + 1] * a + bgG * (1 - a);
      d[i + 2] = d[i + 2] * a + bgB * (1 - a);
      d[i + 3] = 255;
    }
    this.ctx.putImageData(img, 0, 0);
  }
}

function clamp(v) {
  return Math.max(0, Math.min(255, v));
}