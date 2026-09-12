// color-math.js — Shared color-matrix math used by BOTH the GPU filter shader
// and the software fallback so the two paths evaluate the same transform.
// Pure functions, no DOM, no WebGL — safe to import from worker and main thread.

export const IDENTITY_MATRIX = [
  1, 0, 0, 0,
  0, 1, 0, 0,
  0, 0, 1, 0,
  0, 0, 0, 1
];

// Build a 4x4 color-adjustment matrix from perceptual params.
// brightness -1..1, contrast 0..4, saturation 0..4,
// temperature -100..100, tint -100..100.
export function buildColorMatrix(brightness = 0, contrast = 1, saturation = 1,
                                 temperature = 0, tint = 0) {
  const b = clamp(brightness, -1, 1);
  const c = clamp(contrast, 0, 4);
  const s = clamp(saturation, 0, 4);
  const t = clamp(temperature / 100, -1, 1);
  const ti = clamp(tint / 100, -1, 1);

  // Brightness (translate)
  const B = [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, b, b, b, 1];
  // Contrast (scale + offset)
  const mid = 0.5 - 0.5 * c;
  const C = [c, 0, 0, 0, 0, c, 0, 0, 0, 0, c, 0, mid, mid, mid, 1];
  // Saturation
  const inv = 1 - s;
  const r = 0.2126, g = 0.7152, bl = 0.0722;
  const S = [
    s + inv * r, inv * g, inv * bl, 0,
    inv * r, s + inv * g, inv * bl, 0,
    inv * r, inv * g, s + inv * bl, 0,
    0, 0, 0, 1
  ];
  // Temperature (warm/cool)
  const T = [
    (1 + t) > 0 ? (1 + t) : 0, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, (1 - t) > 0 ? (1 - t) : 0, 0,
    0, 0, 0, 1
  ];
  // Tint
  const Ti = [
    1 + ti * 0.5 > 0 ? 1 + ti * 0.5 : 0, 0, 0, 0,
    0, 1, 0, 0,
    0, 0, 1 - ti > 0 ? 1 - ti : 0, 0,
    0, 0, 0, 1
  ];

  return multiplyMatrices(Ti, multiplyMatrices(T, multiplyMatrices(S, multiplyMatrices(C, B))));
}

// Multiply two 4x4 matrices (row-major Float32-compatible).
export function multiplyMatrices(a, b) {
  const out = new Array(16);
  for (let i = 0; i < 4; i++) {
    for (let j = 0; j < 4; j++) {
      let sum = 0;
      for (let k = 0; k < 4; k++) {
        sum += a[i * 4 + k] * b[k * 4 + j];
      }
      out[i * 4 + j] = sum;
    }
  }
  return out;
}

// Convert a hex color (#RRGGBB or #AARRGGBB) to linear RGB 0..1 triple.
export function hexToLinear(hex) {
  const s = hex.replace('#', '');
  let r, g, b;
  if (s.length === 6) {
    r = parseInt(s.slice(0, 2), 16) / 255;
    g = parseInt(s.slice(2, 4), 16) / 255;
    b = parseInt(s.slice(4, 6), 16) / 255;
  } else if (s.length === 8) {
    r = parseInt(s.slice(2, 4), 16) / 255;
    g = parseInt(s.slice(4, 6), 16) / 255;
    b = parseInt(s.slice(6, 8), 16) / 255;
  } else {
    return [0, 1, 0]; // default green
  }
  return [r, g, b];
}

function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }