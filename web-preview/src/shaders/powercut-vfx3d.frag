// powercut-vfx3d.frag — 3D / VFX fragment shader.
//
// Applies a per-effect transform (rotation, tilt, mask shape, chromatic aberration,
// film grain, scanlines, light leak) driven by uniforms from the resolved graph.
precision highp float;
varying vec2 v_texCoord;
uniform sampler2D u_src;
uniform float u_intensity;
uniform float u_rotation;
uniform float u_tilt;
uniform float u_aberration;
uniform float u_grain;
uniform float u_scanlines;
uniform float u_time; // deterministic frame index, not wall-clock
uniform vec2 u_resolution;

mat2 rot2(float a) {
  float c = cos(a), s = sin(a);
  return mat2(c, -s, s, c);
}

void main() {
  vec2 uv = v_texCoord - 0.5;
  uv = rot2(u_rotation) * uv;
  uv += 0.5;

  // chromatic aberration
  vec4 r = texture2D(u_src, uv + vec2(u_aberration * 0.01, 0.0));
  vec4 g = texture2D(u_src, uv);
  vec4 b = texture2D(u_src, uv - vec2(u_aberration * 0.01, 0.0));
  vec4 col = vec4(r.r, g.g, b.b, 1.0);

  // film grain (deterministic from uv + u_time)
  float n = fract(sin(dot(uv, vec2(12.9898, 78.233)) + u_time) * 43758.5453) - 0.5;
  col.rgb += n * u_grain;

  // scanlines
  float line = sin(v_texCoord.y * u_resolution.y * 2.0) * 0.5 + 0.5;
  col.rgb *= 1.0 - u_scanlines * line * 0.3;

  // tilt: darken edges along tilt axis
  float tiltMask = 1.0 - u_tilt * abs(uv.x - 0.5) * 0.5;
  col.rgb *= tiltMask;

  gl_FragColor = col * u_intensity + texture2D(u_src, v_texCoord) * (1.0 - u_intensity);
}