package com.powercut.editor.domain.processing

import android.opengl.Matrix
import android.util.Log
import androidx.media3.common.util.GlUtil

// ═══════════════════════════════════════════════════════════════════════════════
//  FFmpeg → ColorEffect CONVERTER
//  Parses FFmpeg -vf filter chains and converts them to ColorEffect (RgbMatrix)
//  objects for real-time GPU preview via ExoPlayer.setVideoEffects().
//
//  Each FFmpeg sub-filter is mapped to equivalent brightness/contrast/saturation/
//  temperature/tint adjustments that produce visually identical results on the
//  OpenGL pipeline. Effects that can't be approximated with color matrices
//  (blur, noise, edge detect, pixelate, emboss, convolution, geometric
//  distortion) are ONLY applied at export via the REAL FFmpeg preview/export
//  path — they are never returned here, so the live preview can only "fake"
//  an effect it can genuinely reproduce on the GPU.
//
//  Supported FFmpeg filters → ColorEffect mappings:
//  - eq= (brightness, contrast, saturation, gamma)
//  - colorbalance= (temperature/tint approximation)
//  - colorchannelmixer= (sepia → desaturation + warm tint)
//  - hue= (hue rotation, saturation)
//  - vignette= (brightness reduction + contrast boost)
//  - unsharp= (contrast boost approximation)
//  - negate (invert → contrast=-1)
//  - curves= (preset → color adjustments)
// ══════════════════════════════════════════════════ffmpeg═════════════════════════════

object EffectGLConverter {

    private const val TAG = "EffectGLConverter"

    /**
     * Converts a full FFmpeg -vf chain string to a list of ColorEffect objects.
     * Each sub-filter in the comma-separated chain is parsed independently.
     *
     * @param chain The FFmpeg -vf filter chain
     * @return List of ColorEffect objects for ExoPlayer.setVideoEffects()
     */
    fun convertChain(chain: String): List<ColorEffect> {
        if (chain.isBlank()) return emptyList()

        val effects = mutableListOf<ColorEffect>()
        for (subFilter in splitFfmpegChain(chain)) {
            val filter = subFilter.trim()
            if (filter.isEmpty()) continue
            val effect = convertSingleFilter(filter)
            if (effect != null) effects.add(effect)
        }

        Log.d(TAG, "Converted FFmpeg chain to ${effects.size} ColorEffect(s)")
        return effects
    }

    private fun convertSingleFilter(filter: String): ColorEffect? {
        return when {
            filter.startsWith("eq=") -> parseEq(filter)
            filter.startsWith("colorbalance=") -> parseColorBalance(filter)
            filter.startsWith("colorchannelmixer=") -> parseColorChannelMixer(filter)
            filter.startsWith("hue=") -> parseHue(filter)
            filter.startsWith("vignette") -> parseVignette(filter)
            filter.startsWith("unsharp=") -> parseUnsharp(filter)
            filter == "negate" -> parseNegate()
            filter.startsWith("curves=") -> parseCurves(filter)
            else -> {
                Log.d(TAG, "Unknown/skipped GPU-opaque filter: $filter")
                null
            }
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  FILTER PARSERS
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun parseEq(filter: String): ColorEffect {
        val params = parseParams(filter.removePrefix("eq="))
        val brightness = params["brightness"]?.toFloatOrNull() ?: 0f
        val contrast = params["contrast"]?.toFloatOrNull() ?: 1f
        val saturation = params["saturation"]?.toFloatOrNull() ?: 1f
        val gamma = params["gamma"]?.toFloatOrNull() ?: 1f
        return createColorMatrixEffect(brightness, contrast, saturation, gamma = gamma)
    }

    private fun parseColorBalance(filter: String): ColorEffect {
        val params = parseParams(filter.removePrefix("colorbalance="))
        val rs = params["rs"]?.toFloatOrNull() ?: 0f
        val gs = params["gs"]?.toFloatOrNull() ?: 0f
        val bs = params["bs"]?.toFloatOrNull() ?: 0f
        val rm = params["rm"]?.toFloatOrNull() ?: 0f
        val gm = params["gm"]?.toFloatOrNull() ?: 0f
        val bm = params["bm"]?.toFloatOrNull() ?: 0f
        val temperature = ((rs + rm) - (bs + bm)) * 50f
        val tint = ((gs + gm) - ((rs + rm + bs + bm) / 2f)) * 50f
        return createColorMatrixEffect(
            temperature = temperature.coerceIn(-100f, 100f),
            tint = tint.coerceIn(-100f, 100f)
        )
    }

    private fun parseColorChannelMixer(filter: String): ColorEffect {
        val values = filter.removePrefix("colorchannelmixer=")
            .split(":")
            .mapNotNull { it.toFloatOrNull() }
        if (values.size >= 9 && values[0] in 0.35f..0.45f) {
            return createColorMatrixEffect(saturation = 0f, temperature = 15f, brightness = 0.02f)
        }
        return createColorMatrixEffect(saturation = 0.8f)
    }

    private fun parseHue(filter: String): ColorEffect {
        val params = parseParams(filter.removePrefix("hue="))
        val s = params["s"]?.toFloatOrNull()
        val h = params["h"]?.toFloatOrNull()
        if (s != null && s == 0f) return createColorMatrixEffect(saturation = 0f)
        if (s != null) return createColorMatrixEffect(saturation = s)
        if (h != null) return createColorMatrixEffect(temperature = h * 0.5f)
        return createColorMatrixEffect()
    }

    private fun parseVignette(filter: String): ColorEffect =
        createColorMatrixEffect(brightness = -0.06f, contrast = 1.15f)

    private fun parseUnsharp(filter: String): ColorEffect {
        val parts = filter.removePrefix("unsharp=").split(":")
        val strength = parts.getOrNull(2)?.toFloatOrNull() ?: 1f
        return createColorMatrixEffect(contrast = 1f + strength * 0.15f)
    }

    private fun parseNegate(): ColorEffect {
        // contrast=-1 produces out = 1-in via the mid-gray offset, which is a
        // correct color inversion. Do NOT add brightness=1 (that clamps to white).
        return createColorMatrixEffect(contrast = -1f)
    }

    private fun parseCurves(filter: String): ColorEffect {
        val params = parseParams(filter.removePrefix("curves="))
        return when (params["preset"]) {
            "strong_contrast" -> createColorMatrixEffect(contrast = 1.3f)
            "lighter" -> createColorMatrixEffect(brightness = 0.08f, contrast = 0.95f)
            "vintage" -> createColorMatrixEffect(saturation = 0.7f, contrast = 0.9f, brightness = 0.05f, temperature = 8f)
            "cross_process" -> createColorMatrixEffect(saturation = 1.3f, contrast = 1.1f, temperature = 10f)
            "negative" -> createColorMatrixEffect(contrast = -1f)
            else -> createColorMatrixEffect(contrast = 1.15f)
        }
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  COLOR MATRIX BUILDER
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    fun createColorMatrixEffect(
        brightness: Float = 0f,
        contrast: Float = 1f,
        saturation: Float = 1f,
        temperature: Float = 0f,
        tint: Float = 0f,
        gamma: Float = 1f
    ): ColorEffect {
        val brightnessM = GlUtil.create4x4IdentityMatrix()
        if (brightness != 0f) Matrix.translateM(brightnessM, 0, brightness, brightness, brightness)

        val contrastM = GlUtil.create4x4IdentityMatrix()
        if (contrast != 1f) {
            Matrix.scaleM(contrastM, 0, contrast, contrast, contrast)
            val mid = 0.5f - 0.5f * contrast
            contrastM[12] = mid; contrastM[13] = mid; contrastM[14] = mid
        }

        val saturationM = GlUtil.create4x4IdentityMatrix()
        if (saturation != 1f) {
            val r = 0.2126f; val g = 0.7152f; val b = 0.0722f; val inv = 1f - saturation
            saturationM[0] = saturation + inv * r; saturationM[4] = inv * g; saturationM[8] = inv * b
            saturationM[1] = inv * r; saturationM[5] = saturation + inv * g; saturationM[9] = inv * b
            saturationM[2] = inv * r; saturationM[6] = inv * g; saturationM[10] = saturation + inv * b
        }

        val temperatureM = GlUtil.create4x4IdentityMatrix()
        if (temperature != 0f) {
            val t = (temperature / 100f).coerceIn(-1f, 1f)
            Matrix.scaleM(temperatureM, 0, (1f + t).coerceAtLeast(0f), 1f, (1f - t).coerceAtLeast(0f))
        }

        val tintM = GlUtil.create4x4IdentityMatrix()
        if (tint != 0f) {
            val ti = (tint / 100f).coerceIn(-1f, 1f)
            Matrix.scaleM(tintM, 0, (1f + ti * 0.5f).coerceAtLeast(0f), (1f - ti).coerceAtLeast(0f), (1f + ti * 0.5f).coerceAtLeast(0f))
        }

        val gammaM = GlUtil.create4x4IdentityMatrix()
        if (gamma != 1f) {
            val gFactor = 1f / gamma
            Matrix.scaleM(gammaM, 0, gFactor, gFactor, gFactor)
            val offset = 0.5f - 0.5f * gFactor
            gammaM[12] = offset; gammaM[13] = offset; gammaM[14] = offset
        }

        var combined = multiplyMatrices(gammaM, brightnessM)
        combined = multiplyMatrices(contrastM, combined)
        combined = multiplyMatrices(saturationM, combined)
        combined = multiplyMatrices(temperatureM, combined)
        combined = multiplyMatrices(tintM, combined)

        return ColorEffect(brightness, contrast, saturation, temperature, tint, combined)
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    //  UTILITY
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

    private fun splitFfmpegChain(chain: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0; var current = StringBuilder()
        for (ch in chain) {
            when (ch) {
                '(', '[' -> { depth++; current.append(ch) }
                ')', ']' -> { depth--; current.append(ch) }
                ',' -> if (depth == 0) { result.add(current.toString()); current = StringBuilder() } else current.append(ch)
                else -> current.append(ch)
            }
        }
        if (current.isNotEmpty()) result.add(current.toString())
        return result
    }

    private fun parseParams(paramStr: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        for (kv in paramStr.split(":")) {
            val parts = kv.split("=", limit = 2)
            if (parts.size == 2) params[parts[0].trim()] = parts[1].trim()
            else if (parts.size == 1 && parts[0].isNotEmpty()) params["__pos${params.size}__"] = parts[0].trim()
        }
        return params
    }

    private fun multiplyMatrices(lhs: FloatArray, rhs: FloatArray): FloatArray {
        val result = FloatArray(16)
        Matrix.multiplyMM(result, 0, lhs, 0, rhs, 0)
        return result
    }
}
