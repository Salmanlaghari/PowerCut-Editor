// Determinism + keyframe + graph-resolution tests (no WebGL needed).
import assert from 'node:assert/strict';
import { test } from 'node:test';
import {
  resolveGraph, evaluateKeyframes, makeNode, makeKeyframe,
  EffectKind, Ease, serializeEffectGraph
} from '../src/effectgraph/powercut-effect-graph.js';
import { buildColorMatrix, hexToLinear } from '../src/util/color-math.js';

test('evaluateKeyframes: empty returns default', () => {
  assert.strictEqual(evaluateKeyframes([], 1000, 0.5), 0.5);
});

test('evaluateKeyframes: single keyframe', () => {
  assert.strictEqual(evaluateKeyframes([makeKeyframe(0, 0.2)], 500, 1), 0.2);
});

test('evaluateKeyframes: linear interpolation', () => {
  const kfs = [makeKeyframe(0, 0), makeKeyframe(1000, 1)];
  assert.strictEqual(evaluateKeyframes(kfs, 500, 0), 0.5);
  assert.strictEqual(evaluateKeyframes(kfs, 0, 0), 0);
  assert.strictEqual(evaluateKeyframes(kfs, 1000, 0), 1);
});

test('evaluateKeyframes: ease-in-out at 0.25 -> 0.15625', () => {
  const kfs = [makeKeyframe(0, 0, 'opacity', Ease.EASE_IN_OUT), makeKeyframe(1000, 1, 'opacity', Ease.EASE_IN_OUT)];
  const v = evaluateKeyframes(kfs, 250, 0);
  assert.ok(Math.abs(v - 0.15625) < 1e-6, `expected 0.15625 got ${v}`);
});

test('evaluateKeyframes: ease-in at 0.5 -> 0.25', () => {
  const kfs = [makeKeyframe(0, 0, 'opacity', Ease.EASE_IN), makeKeyframe(1000, 1, 'opacity', Ease.EASE_IN)];
  const v = evaluateKeyframes(kfs, 500, 0);
  assert.ok(Math.abs(v - 0.25) < 1e-6, `expected 0.25 got ${v}`);
});

test('evaluateKeyframes: ease-out at 0.5 -> 0.75', () => {
  const kfs = [makeKeyframe(0, 0, 'opacity', Ease.EASE_OUT), makeKeyframe(1000, 1, 'opacity', Ease.EASE_OUT)];
  const v = evaluateKeyframes(kfs, 500, 0);
  assert.ok(Math.abs(v - 0.75) < 1e-6, `expected 0.75 got ${v}`);
});

test('resolveGraph: deterministic order by kind then id', () => {
  const nodes = [
    makeNode(EffectKind.Blend, 'b', {}),
    makeNode(EffectKind.Filter, 'a', {}),
    makeNode(EffectKind.Source, 's', { startUs: 0, durationUs: 1000 })
  ];
  const r = resolveGraph(nodes, 500, 1000);
  assert.strictEqual(r.length, 3);
  assert.strictEqual(r[0].kind, EffectKind.Source);
  assert.strictEqual(r[1].kind, EffectKind.Filter);
  assert.strictEqual(r[2].kind, EffectKind.Blend);
});

test('resolveGraph: same input -> identical output (determinism)', () => {
  const nodes = [
    makeNode(EffectKind.Source, 's', { startUs: 0, durationUs: 1000 }),
    makeNode(EffectKind.Filter, 'f', {}, [], [makeKeyframe(0, 0.5)])
  ];
  const a = serializeEffectGraph(nodes, 500, 1000);
  const b = serializeEffectGraph(nodes, 500, 1000);
  assert.deepStrictEqual(a, b);
});

test('resolveGraph: nodes outside time window are excluded', () => {
  const nodes = [
    makeNode(EffectKind.Source, 's', { startUs: 0, durationUs: 1000 }),
    makeNode(EffectKind.Filter, 'f', { startUs: 2000, durationUs: 1000 })
  ];
  const r = resolveGraph(nodes, 500, 3000);
  assert.strictEqual(r.length, 1);
  assert.strictEqual(r[0].id, 's');
});

test('buildColorMatrix: identity when all defaults', () => {
  const m = buildColorMatrix();
  for (let i = 0; i < 16; i++) {
    const exp = i % 5 === 0 ? 1 : 0;
    assert.ok(Math.abs(m[i] - exp) < 1e-6, `index ${i} expected ${exp} got ${m[i]}`);
  }
});

test('hexToLinear: #00FF00 -> [0,1,0]', () => {
  const [r, g, b] = hexToLinear('#00FF00');
  assert.ok(Math.abs(r - 0) < 1e-6);
  assert.ok(Math.abs(g - 1) < 1e-6);
  assert.ok(Math.abs(b - 0) < 1e-6);
});

test('hexToLinear: #FF0000 -> [1,0,0]', () => {
  const [r, g, b] = hexToLinear('#FF0000');
  assert.ok(Math.abs(r - 1) < 1e-6);
  assert.ok(Math.abs(g - 0) < 1e-6);
  assert.ok(Math.abs(b - 0) < 1e-6);
});

test('serializeEffectGraph: versioned payload shape', () => {
  const nodes = [makeNode(EffectKind.Source, 's', { startUs: 0, durationUs: 1000 })];
  const p = serializeEffectGraph(nodes, 500, 1000);
  assert.strictEqual(p.version, 1);
  assert.strictEqual(p.currentTimeUs, 500);
  assert.strictEqual(p.durationUs, 1000);
  assert.strictEqual(p.nodes.length, 1);
  assert.strictEqual(p.nodes[0].id, 's');
});