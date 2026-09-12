// powercut-effect-graph.js — Shared, versioned effect-graph format.
// Consumed by BOTH live preview (Canvas/WebGL) and export (WebWorker).
// Version 1. Deterministic: evaluating the same graph at the same currentTime
// always yields the same uniforms/texture set, regardless of wall-clock or
// render-loop order.
export const EFFECT_GRAPH_VERSION = 1;

export const EffectKind = {
  Source: 0,
  Filter: 1,
  Transition: 2,
  ChromaKey: 3,
  Animation: 4,
  VFX3D: 5,
  Blend: 6
};

export const Ease = {
  LINEAR: 0,
  EASE_IN: 1,
  EASE_OUT: 2,
  EASE_IN_OUT: 3
};

// A single keyframe on the timeline.
export function makeKeyframe(timeUs, value, property = 'intensity', ease = Ease.LINEAR) {
  return { timeUs, value, property, ease };
}

// Evaluate a sorted keyframe array at currentTimeUs (deterministic, no Date()).
// Returns the interpolated value. Empty array -> defaultValue.
export function evaluateKeyframes(keyframes, currentTimeUs, defaultValue = 1.0) {
  if (!keyframes || keyframes.length === 0) return defaultValue;
  const t = currentTimeUs;
  if (t <= keyframes[0].timeUs) return keyframes[0].value;
  if (t >= keyframes[keyframes.length - 1].timeUs) return keyframes[keyframes.length - 1].value;
  for (let i = 1; i < keyframes.length; i++) {
    const a = keyframes[i - 1];
    const b = keyframes[i];
    if (t <= b.timeUs) {
      const span = b.timeUs - a.timeUs;
      let frac = span <= 0 ? 1 : (t - a.timeUs) / span;
      if (a.ease === Ease.EASE_IN_OUT) frac = frac * frac * (3 - 2 * frac);
      else if (a.ease === Ease.EASE_IN) frac = frac * frac;
      else if (a.ease === Ease.EASE_OUT) frac = 1 - (1 - frac) * (1 - frac);
      return a.value + (b.value - a.value) * frac;
    }
  }
  return keyframes[keyframes.length - 1].value;
}

// Build a node in the canonical graph.
export function makeNode(kind, id, params = {}, deps = [], keyframes = []) {
  return {
    kind,
    id,
    params: { ...params },
    deps: [...deps],
    keyframes: keyframes.map(k => ({ ...k }))
  };
}

// Resolve the active effect graph at currentTimeUs.
// Returns a topologically-sorted list of nodes whose time window contains t,
// with each node's keyframe-derived uniforms pre-computed (deterministic).
export function resolveGraph(nodes, currentTimeUs, durationUs) {
  const active = [];
  for (const n of nodes) {
    const start = n.params.startUs != null ? n.params.startUs : 0;
    const dur = n.params.durationUs != null ? n.params.durationUs : durationUs;
    if (currentTimeUs < start || currentTimeUs > start + dur) continue;
    const resolved = {
      ...n,
      params: { ...n.params },
      uniforms: resolveUniforms(n, currentTimeUs)
    };
    active.push(resolved);
  }
  // Deterministic order: by kind priority then id (stable, never wall-clock).
  const kindOrder = {
    [EffectKind.Source]: 0, [EffectKind.Filter]: 1, [EffectKind.VFX3D]: 2,
    [EffectKind.ChromaKey]: 3, [EffectKind.Animation]: 4,
    [EffectKind.Transition]: 5, [EffectKind.Blend]: 6
  };
  active.sort((a, b) => {
    const ka = kindOrder[a.kind] ?? 9;
    const kb = kindOrder[b.kind] ?? 9;
    if (ka !== kb) return ka - kb;
    return a.id < b.id ? -1 : a.id > b.id ? 1 : 0;
  });
  return active;
}

// Pre-compute a node's GPU uniforms from its keyframes at currentTimeUs.
function resolveUniforms(node, t) {
  const u = {};
  const kfByProp = {};
  for (const kf of node.keyframes || []) {
    (kfByProp[kf.property] ||= []).push(kf);
  }
  for (const [prop, kfs] of Object.entries(kfByProp)) {
    kfs.sort((a, b) => a.timeUs - b.timeUs);
    u[prop] = evaluateKeyframes(kfs, t, node.params[prop] != null ? node.params[prop] : 1.0);
  }
  // Carry static params through as uniforms too (filters, transitions, chroma).
  for (const [k, v] of Object.entries(node.params)) {
    if (!(k in u)) u[k] = v;
  }
  return u;
}

// Serialize the resolved graph to a versioned, transferable payload for the
// export worker. Same function used by preview -> export path.
export function serializeEffectGraph(nodes, currentTimeUs, durationUs) {
  const resolved = resolveGraph(nodes, currentTimeUs, durationUs);
  return {
    version: EFFECT_GRAPH_VERSION,
    currentTimeUs,
    durationUs,
    nodes: resolved.map(n => ({
      kind: n.kind, id: n.id, params: n.params, uniforms: n.uniforms, deps: n.deps
    }))
  };
}