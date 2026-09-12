// powercut-chroma-key.frag — Real-time chroma-key / green-screen fragment shader.
//
// Removes a custom key color using tolerance, edge softness, spill suppression,
// and blends the keyed result over a background track.
//
// Uniforms (all driven by the resolved effect graph):
//   u_keyColor  vec3   key color in linear space (default green 0,1,0)
//   u_tolerance float 0..1  color-distance threshold
//   u_softness  float 0..1  edge falloff width
//   u_spill     float 0..1  spill suppression strength
//   u_hasBg     float 0/1  whether a background texture is bound to u_bg
//   u_bgColor   vec3   flat background color when u_hasBg == 0
//   u_blendMode int    0=over, 1=multiply, 2=screen, 3=add
precision highp float;
varying vec2 v_texCoord;
uniform sampler2D u_src;
uniform sampler2D u_bg;
uniform vec3 u_keyColor;
uniform float u_tolerance;
uniform float u_softness;
uniform float u_spill;
uniform float u_hasBg;
uniform vec3 u_bgColor;
uniform int u_blendMode;

float colorDist(vec3 a, vec3 b) {
  vec3 d = a - b;
  return sqrt(dot(d, d));
}

vec3 suppressSpill(vec3 c, vec3 key) {
  float k = max(key.r, max(key.g, key.b));
  if (k < 0.001) return c;
  float spill = clamp(u_spill, 0.0, 1.0);
  float denom = (1.0 - key.r) + (1.0 - key.g) + (1.0 - key.b);
  float other = (c.r * (1.0 - key.r) + c.g * (1.0 - key.g) + c.b * (1.0 - key.b))
              / max(denom, 0.001);
  return mix(c, vec3(other), spill);
}

void main() {
  vec4 src = texture2D(u_src, v_texCoord);
  vec3 rgb = src.rgb;
  vec3 lin = pow(rgb, vec3(2.2));
  float d = colorDist(lin, u_keyColor);

  float t = u_tolerance;
  float edge = max(u_softness * 0.5, 0.001);
  float alpha = clamp((d - t) / edge, 0.0, 1.0);

  vec3 keyed = suppressSpill(rgb, u_keyColor);

  vec3 bg;
  if (u_hasBg > 0.5) {
    bg = texture2D(u_bg, v_texCoord).rgb;
  } else {
    bg = u_bgColor;
  }

  vec3 out;
  if (u_blendMode == 1) {
    out = mix(bg, keyed * bg, alpha);
  } else if (u_blendMode == 2) {
    out = vec3(1.0) - (vec3(1.0) - mix(bg, keyed, alpha)) * (vec3(1.0) - bg);
  } else if (u_blendMode == 3) {
    out = mix(bg, keyed, alpha) + bg * (1.0 - alpha);
  } else {
    out = keyed * alpha + bg * (1.0 - alpha);
  }

  gl_FragColor = vec4(out, 1.0);
}