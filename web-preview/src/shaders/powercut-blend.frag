// powercut-blend.frag — Final output blend + composite over background track.
// Renders the fully processed frame (source -> filters -> 3D/VFX -> chroma key)
// over a background track with per-layer alpha compositing.
precision highp float;
varying vec2 v_texCoord;
uniform sampler2D u_frame;
uniform sampler2D u_bg;
uniform float u_bgAlpha;
uniform int u_blendMode;

void main() {
  vec4 f = texture2D(u_frame, v_texCoord);
  vec4 bg = texture2D(u_bg, v_texCoord);
  vec4 out;
  if (u_blendMode == 1) {        // multiply
    out = vec4(f.rgb * bg.rgb, f.a);
  } else if (u_blendMode == 2) { // screen
    out = vec4(vec3(1.0) - (vec3(1.0) - f.rgb) * (vec3(1.0) - bg.rgb), f.a);
  } else if (u_blendMode == 3) { // add
    out = vec4(f.rgb + bg.rgb, f.a);
  } else {                        // over
    float a = f.a + bg.a * (1.0 - f.a);
    out = vec4((f.rgb * f.a + bg.rgb * bg.a * (1.0 - f.a)) / max(a, 0.001), a);
  }
  out = mix(bg, out, u_bgAlpha);
  gl_FragColor = out;
}