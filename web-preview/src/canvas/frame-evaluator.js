// frame-evaluator.js — Deterministic effect-graph traversal.
//
// Evaluates the resolved effect graph at an exact currentTime and produces the
// draw calls for the renderer. Pure function: same inputs -> same draw calls,
// regardless of wall-clock time or render-loop order. Preview and export use
// the EXACT same instance so output matches preview 1:1.
import { resolveGraph, EffectKind } from '../effectgraph/powercut-effect-graph.js';

export class FrameEvaluator {
  constructor() {
    this.drawCalls = [];
  }

  // Evaluate the graph at currentTimeUs. Returns an ordered list of draw calls.
  evaluate(nodes, currentTimeUs, durationUs, inputFrame) {
    const resolved = resolveGraph(nodes, currentTimeUs, durationUs);
    const calls = [];
    let current = inputFrame; // source texture -> filters -> 3D/VFX -> chroma key -> blend

    for (const node of resolved) {
      switch (node.kind) {
        case EffectKind.Filter:
          calls.push({ op: 'filter', node, texture: current });
          break;
        case EffectKind.VFX3D:
          calls.push({ op: 'vfx3d', node, texture: current });
          break;
        case EffectKind.Animation:
          calls.push({ op: 'animation', node, texture: current });
          break;
        case EffectKind.ChromaKey:
          calls.push({ op: 'chromaKey', node, texture: current });
          break;
        case EffectKind.Transition:
          calls.push({ op: 'transition', node, texture: current });
          break;
        case EffectKind.Blend:
          calls.push({ op: 'blend', node, texture: current });
          break;
        default:
          break;
      }
    }

    this.drawCalls = calls;
    return calls;
  }

  // Serialize the evaluation for the export worker (versioned, transferable).
  serialize(nodes, currentTimeUs, durationUs, inputFrame) {
    const calls = this.evaluate(nodes, currentTimeUs, durationUs, inputFrame);
    return {
      version: 1,
      currentTimeUs,
      durationUs,
      drawCalls: calls.map(c => ({
        op: c.op,
        id: c.node.id,
        uniforms: c.node.uniforms || {}
      }))
    };
  }
}