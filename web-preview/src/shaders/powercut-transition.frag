// powercut-transition.frag — Dual-texture transition blending.
//
// Blends between two overlapping clips using the normalized overlap progress
// (0 = fully A, 1 = fully B) and an explicit transition type + easing.
//
// Uniforms:
//   u_progress  float 0..1  normalized overlap progress
//   u_type      int    0=dissolve, 1=fade, 2=slide, 3=zoom, 4=wipe
//   u_ease      int    0=linear,1=in,2=out,3=inout
//   u_dir       vec2   slide/wipe direction (normalized)
//   u_scale     float  zoom peak scale
precision highp float;
varying vec2 v_texCoord;
uniform sampler2D u_srcA;
uniform sampler2D u_srcB;
uniform float u_progress;
uniform int u_type;
uniform int u_ease;
uniform vec2 u_dir;
uniform float u_scale;

float ease(float p, int e) {
  if (e == 1) return p * p;
  if (e == 2) return 1.0 - (1.0 - p) * (1.0 - p);
  if (e == 3) return p * p * (3.0 - 2.0 * p);
  return p;
}

void main() {
  float p = ease(clamp(u_progress, 0.0, 1.0), u_ease);
  vec4 a = texture2D(u_srcA, v_texCoord);
  vec4 b = texture2D(u_srcB, v_texCoord);

  vec4 out;
  if (u_type == 0) {          // dissolve
    float noise = fract(sin(dot(v_texCoord, vec2(12.9898, 78.233))) * 43758.5453);
    float thresh = mix(0.5, 1.0, p);
    out = mix(a, b, step(thresh, noise));
  } else if (u_type == 1) {   // fade
    out = mix(a, b, p);
  } else if (u_type == 2) {   // slide
    vec2 off = u_dir * p;
    vec4 bShifted = texture2D(u_srcB, v_texCoord - off);
    out = mix(a, bShifted, p);
  } else if (u_type == 3) {   // zoom
    vec2 center = vec2(0.5);
    vec2 scaled = (v_texCoord - center) * (1.0 + u_scale * p) + center;
    vec4 bZoom = texture2D(u_srcB, scaled);
    out = mix(a, bZoom, p);
  } else if (u_type == 4) {   // wipe (directional reveal)
    float edge = v_texCoord.x + v_texCoord.y;
    float reveal = mix(-1.0, 2.0, p);
    float mask = smoothstep(edge - 0.05, edge + 0.05, reveal);
    out = mix(b, a, mask);
  } else {
    out = mix(a, b, p);
  }
  gl_FragColor = out;
}