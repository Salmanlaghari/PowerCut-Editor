// powercut-filter.frag — Per-channel color filter (brightness / contrast / saturation /
// temperature / tint / hue-shift). Driven by a 4x4 color matrix uniform so the
// GPU filter and the export chain always evaluate the same transform.
precision highp float;
varying vec2 v_texCoord;
uniform sampler2D u_src;
uniform mat4 u_colorMatrix;

void main() {
  vec4 c = texture2D(u_src, v_texCoord);
  gl_FragColor = u_colorMatrix * c;
}