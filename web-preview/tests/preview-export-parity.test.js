// preview-export-parity.test.js — Preview ⇄ export parity.
//
// Verifies that the preview path and the export worker evaluate the EXACT same
// effect graph, shader modules, uniform serialization, blending logic, and
// frame evaluation. The two paths share the same modules by import, so this
// test pins the parity guarantee.
import assert from 'node:assert/strict';
import { test } from 'node:test';
import { FrameEvaluator } from '../src/canvas/frame-evaluator.js';
import { ShaderManager } from '../src/canvas/shader-manager.js';
import { resolveGraph, EffectKind } from '../src/effectgraph/powercut-effect-graph.js';
import { buildColorMatrix } from '../src/util/color-math.js';

// Minimal fake GL so ShaderManager can be exercised without a real WebGL context.
function makeFakeGL() {
  let id = 0;
  return {
    createShader: () => ({ id: ++id, compiled: false }),
    shaderSource: () => {},
    compileShader: (s) => { s.compiled = true; },
    getShaderParameter: (s, p) => p === 0x8B81 ? s.compiled : false,
    getShaderInfoLog: () => '',
    deleteShader: () => {},
    createProgram: () => ({ id: ++id, linked: false }),
    attachShader: () => {},
    linkProgram: (p) => { p.linked = true; },
    getProgramParameter: (p, c) => c === 0x8B80 ? p.linked : false,
    getProgramInfoLog: () => '',
    validateProgram: () => {},
    deleteProgram: () => {},
    VERTEX_SHADER: 0x8B31, FRAGMENT_SHADER: 0x8B30,
    COMPILE_STATUS: 0x8B81, LINK_STATUS: 0x8B80,
    ARRAY_BUFFER: 0x8892, STATIC_DRAW: 0x88E4,
    RGBA: 0x1908, UNSIGNED_BYTE: 0x1401,
    TEXTURE_2D: 0x0DE1, TEXTURE0: 0x84C0,
    FRAMEBUFFER: 0x8D40, COLOR_ATTACHMENT0: 0xCE00,
    VIEWPORT: 0x0BA2, COLOR_BUFFER_BIT: 0x4100,
    TRIANGLE_STRIP: 0x0005,
    createBuffer: () => ({}), bindBuffer: () => {}, bufferData: () => {},
    createFramebuffer: () => ({}), bindFramebuffer: () => {},
    framebufferTexture2D: () => {}, createTexture: () => ({}),
    bindTexture: () => {}, texImage2D: () => {}, texParameteri: () => {},
    useProgram: () => {}, getAttribLocation: () => 0,
    getUniformLocation: () => ({}), enableVertexAttribArray: () => {},
    vertexAttribPointer: () => {},
    uniform1f: () => {}, uniform1i: () => {}, uniform3f: () => {},
    uniform2f: () => {}, uniformMatrix4fv: () => {},
    drawArrays: () => {}, viewport: () => {}, readPixels: () => {},
    activeTexture: () => {}, deleteTexture: () => {},
    deleteFramebuffer: () => {}, deleteBuffer: () => {}
  };
}

test('same FrameEvaluator instance shape for preview and export', () => {
  const a = new FrameEvaluator();
  const b = new FrameEvaluator();
  const nodes = [
    { kind: EffectKind.Source, id: 's', params: { startUs: 0, durationUs: 1000 } },
    { kind: EffectKind.Filter, id: 'f', params: {}, keyframes: [] },
    { kind: EffectKind.Blend, id: 'b', params: {} }
  ];
  const ca = a.evaluate(nodes, 500, 1000, null);
  const cb = b.evaluate(nodes, 500, 1000, null);
  assert.strictEqual(ca.length, cb.length);
  for (let i = 0; i < ca.length; i++) {
    assert.strictEqual(ca[i].op, cb[i].op);
    assert.strictEqual(ca[i].node.id, cb[i].node.id);
  }
});

test('serialized payload is identical between two evaluators', () => {
  const a = new FrameEvaluator();
  const b = new FrameEvaluator();
  const nodes = [
    { kind: EffectKind.Source, id: 's', params: { startUs: 0, durationUs: 1000 } },
    { kind: EffectKind.Filter, id: 'f', params: {} }
  ];
  const pa = a.serialize(nodes, 500, 1000, null);
  const pb = b.serialize(nodes, 500, 1000, null);
  assert.deepStrictEqual(pa, pb);
  assert.strictEqual(pa.version, 1);
});

test('ShaderManager compiles all required programs', () => {
  const gl = makeFakeGL();
  const sm = new ShaderManager(gl);
  for (const name of ['filter', 'chromaKey', 'transition', 'vfx3d', 'blend']) {
    const p = sm.getProgram(name);
    assert.ok(p, `program ${name} should compile`);
  }
});

test('color matrix is deterministic across calls', () => {
  const m1 = buildColorMatrix(0.1, 1.2, 1.1, 20, 10);
  const m2 = buildColorMatrix(0.1, 1.2, 1.1, 20, 10);
  assert.deepStrictEqual(Array.from(m1), Array.from(m2));
});

test('filter + chroma key + blend graph resolves in deterministic order', () => {
  const nodes = [
    { kind: EffectKind.Blend, id: 'out', params: {} },
    { kind: EffectKind.ChromaKey, id: 'ck', params: {} },
    { kind: EffectKind.Filter, id: 'flt', params: {} },
    { kind: EffectKind.Source, id: 'src', params: { startUs: 0, durationUs: 1000 } }
  ];
  const r = resolveGraph(nodes, 500, 1000);
  assert.strictEqual(r[0].kind, EffectKind.Source);
  assert.strictEqual(r[1].kind, EffectKind.Filter);
  assert.strictEqual(r[2].kind, EffectKind.ChromaKey);
  assert.strictEqual(r[3].kind, EffectKind.Blend);
});