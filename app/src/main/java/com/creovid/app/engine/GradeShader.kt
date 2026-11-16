package com.creovid.app.engine

/** Creovid ka poora colour grading shader (GLSL ES 1.00). Preview aur export dono me yahi chalta hai. */
object GradeShader {

    const val VERTEX = """
attribute vec4 aFramePosition;
varying vec2 vTexCoord;
void main() {
  gl_Position = aFramePosition;
  vTexCoord = aFramePosition.xy * 0.5 + 0.5;
}
"""

    const val FRAGMENT = """
precision highp float;

uniform sampler2D uTexSampler;
uniform sampler2D uLut;
varying vec2 vTexCoord;

uniform float uExposure;
uniform float uContrast;
uniform float uHighlights;
uniform float uShadows;
uniform float uSaturation;
uniform float uVibrance;
uniform float uTemperature;
uniform float uTint;
uniform float uFade;
uniform float uFilmCurve;
uniform vec3 uLift;
uniform vec3 uGamma;
uniform vec3 uGain;
uniform float uLutIntensity;
uniform float uLutSize;
uniform float uUseLut;
uniform float uGrain;
uniform float uBloom;
uniform float uHalation;
uniform float uVignette;
uniform float uLetterbox;
uniform float uFrameAspect;
uniform vec2 uResolution;
uniform float uTime;

float luma(vec3 c) {
  return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

float hash(vec2 p) {
  vec3 p3 = fract(vec3(p.xyx) * 0.1031);
  p3 += dot(p3, p3.yzx + 33.33);
  return fract((p3.x + p3.y) * p3.z);
}

vec2 lutUv(vec2 rg, float slice) {
  float x = (slice * uLutSize + rg.r * (uLutSize - 1.0) + 0.5) / (uLutSize * uLutSize);
  float y = (rg.g * (uLutSize - 1.0) + 0.5) / uLutSize;
  return vec2(x, y);
}

vec3 applyLut(vec3 c) {
  float b = c.b * (uLutSize - 1.0);
  float b0 = floor(b);
  float b1 = min(b0 + 1.0, uLutSize - 1.0);
  float f = b - b0;
  vec3 a = texture2D(uLut, lutUv(c.rg, b0)).rgb;
  vec3 d = texture2D(uLut, lutUv(c.rg, b1)).rgb;
  return mix(a, d, f);
}

vec3 glowTap(vec2 p) {
  vec3 s = texture2D(uTexSampler, p).rgb * exp2(uExposure);
  return s * smoothstep(0.6, 1.0, luma(s));
}

void main() {
  vec2 uv = vTexCoord;
  vec3 c = texture2D(uTexSampler, uv).rgb;

  // 1. Exposure + white balance
  c *= exp2(uExposure);
  c.r *= 1.0 + 0.20 * uTemperature;
  c.b *= 1.0 - 0.20 * uTemperature;
  c.g *= 1.0 - 0.15 * uTint;

  // 2. Shadows / highlights
  float l = clamp(luma(c), 0.0, 1.0);
  c += uShadows * 0.30 * pow(1.0 - l, 2.0);
  c += uHighlights * 0.30 * pow(l, 2.0);

  // 3. Contrast + film S-curve
  c = (c - 0.5) * (1.0 + uContrast) + 0.5;
  vec3 cc = clamp(c, 0.0, 1.0);
  c = mix(c, cc * cc * (3.0 - 2.0 * cc), uFilmCurve);

  // 4. Lift / Gamma / Gain colour wheels
  c = max(c, vec3(0.0));
  c += uLift * (1.0 - c);
  c = pow(max(c, vec3(0.0001)), 1.0 / max(vec3(1.0) + uGamma, vec3(0.2)));
  c *= vec3(1.0) + uGain;

  // 5. Saturation + vibrance
  l = luma(c);
  float mx = max(c.r, max(c.g, c.b));
  float mn = min(c.r, min(c.g, c.b));
  float s = 1.0 + uSaturation + uVibrance * (1.0 - clamp(mx - mn, 0.0, 1.0));
  c = mix(vec3(l), c, max(s, 0.0));

  // 6. Fade (matte blacks)
  c = mix(c, c * 0.86 + 0.07, uFade);

  // 7. LUT look
  if (uUseLut > 0.5) {
    vec3 lc = applyLut(clamp(c, 0.0, 1.0));
    c = mix(c, lc, uLutIntensity);
  }

  // 8. Bloom + halation
  if (uBloom + uHalation > 0.001) {
    vec2 o1 = vec2(0.012 / uFrameAspect, 0.012);
    vec2 o2 = o1 * 2.6;
    vec3 g = vec3(0.0);
    for (int i = 0; i < 8; i++) {
      float a = float(i) * 0.785398;
      vec2 d = vec2(cos(a), sin(a));
      g += glowTap(uv + d * o1) + glowTap(uv + d * o2);
    }
    g /= 16.0;
    c += g * uBloom * 1.4;
    c += g * uHalation * 1.6 * vec3(1.0, 0.30, 0.08);
  }

  // 9. Vignette
  vec2 dv = (uv - 0.5) * vec2(uFrameAspect, 1.0);
  c *= 1.0 - uVignette * smoothstep(0.25, 1.0, length(dv));

  // 10. Film grain (24 fps pe badalta hai)
  if (uGrain > 0.001) {
    float fr = floor(uTime * 24.0);
    vec2 gp = uv * uResolution;
    float n = hash(gp + fr * 13.37) + hash(gp * 0.7 + fr * 7.13) - 1.0;
    float ll = clamp(luma(c), 0.0, 1.0);
    c += n * uGrain * 0.10 * (0.4 + 0.6 * (1.0 - abs(ll - 0.5) * 2.0));
  }

  // 11. Letterbox (cinema bars)
  if (uLetterbox > 0.0) {
    float vis = min(uFrameAspect / uLetterbox, 1.0);
    if (abs(uv.y - 0.5) > vis * 0.5) {
      c = vec3(0.0);
    }
  }

  gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
"""
}
