package com.powercut.editor.domain.filter

import androidx.compose.ui.graphics.ColorMatrix

/**
 * Parses a real FFmpeg color-grade chain (eq / colorbalance /
 * colorchannelmixer / negate / hue=s=0) into an approximate Compose
 * [ColorMatrix] so a filter is VISIBLE in the live preview — not only at
 * export. Ops with no direct ColorMatrix equivalent (boxblur, noise,
 * vignette, unsharp, tblend, hue rotation) are intentionally ignored for
 * the preview; their tone contribution still shows via eq/colorbalance.
 *
 * This is shared by the live preview ([com.powercut.editor.ui.editor.NextGenEditorScreen])
 * and is unit-tested, guaranteeing that every filter in [FilterCatalog]
 * produces a visible, non-identity preview change that matches the export
 * chain (both read [FilterCatalog.ffmpeg]).
 */
internal fun filterPreviewMatrix(chain: String): ColorMatrix? {
    if (chain.isBlank()) return null

    // Direct swaps that map 1:1 to a matrix.
    if (chain.contains("negate")) {
        return ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 1f,
                0f, -1f, 0f, 0f, 1f,
                0f, 0f, -1f, 0f, 1f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    var brightness = 0f
    var contrast = 1f
    var saturation = 1f
    var cbRs = 0f; var cbGs = 0f; var cbBs = 0f
    var cbRm = 0f; var cbGm = 0f; var cbBm = 0f
    val hasHueGray = chain.contains("hue=s=0")

    // colorchannelmixer directly defines the RGB transform matrix.
    var ccm: ColorMatrix? = null
    for (sub in chain.split(",")) {
        val f = sub.trim()
        when {
            f.startsWith("colorchannelmixer=") -> {
                val p = f.removePrefix("colorchannelmixer=").split(":").map { it.toFloatOrNull() ?: 0f }
                if (p.size >= 12) {
                    ccm = ColorMatrix(
                        floatArrayOf(
                            p[0], p[1], p[2], 0f, p[3],
                            p[4], p[5], p[6], 0f, p[7],
                            p[8], p[9], p[10], 0f, p[11],
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                }
            }
            f.startsWith("eq=") -> {
                for (kv in f.removePrefix("eq=").split(":")) {
                    val parts = kv.split("=")
                    if (parts.size != 2) continue
                    val v = parts[1].trim().toFloatOrNull() ?: continue
                    when (parts[0].trim()) {
                        "brightness" -> brightness = v
                        "contrast" -> contrast = v
                        "saturation" -> saturation = v
                    }
                }
            }
            f.startsWith("colorbalance=") -> {
                for (kv in f.removePrefix("colorbalance=").split(":")) {
                    val parts = kv.split("=")
                    if (parts.size != 2) continue
                    val v = parts[1].trim().toFloatOrNull() ?: continue
                    when (parts[0].trim()) {
                        "rs" -> cbRs = v; "gs" -> cbGs = v; "bs" -> cbBs = v
                        "rm" -> cbRm = v; "gm" -> cbGm = v; "bm" -> cbBm = v
                    }
                }
            }
        }
    }

    val hasEq = brightness != 0f || contrast != 1f || saturation != 1f
    val hasCb = cbRs != 0f || cbGs != 0f || cbBs != 0f || cbRm != 0f || cbGm != 0f || cbBm != 0f
    if (ccm == null && !hasEq && !hasCb && !hasHueGray) return null

    // Start from the colorchannelmixer matrix (or identity).
    val mat = ccm ?: ColorMatrix()

    // Tone (contrast + brightness) and colorbalance tints, applied in the
    // same order premiumLookPreviewMatrix uses: tone first, then tint.
    val contrastShift = (0.5f - 0.5f * contrast)
    val brightnessAdd = brightness
    val rShift = (cbRs + cbRm) * 0.15f
    val gShift = (cbGs + cbGm) * 0.15f
    val bShift = (cbBs + cbBm) * 0.15f

    val tone = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, contrastShift + brightnessAdd + rShift,
            0f, contrast, 0f, 0f, contrastShift + brightnessAdd + gShift,
            0f, 0f, contrast, 0f, contrastShift + brightnessAdd + bShift,
            0f, 0f, 0f, 1f, 0f
        )
    )
    mat *= tone
    if (hasHueGray) {
        mat *= ColorMatrix().apply { setToSaturation(0f) }
    } else if (saturation != 1f) {
        mat *= ColorMatrix().apply { setToSaturation(saturation) }
    }
    return mat
}

/** Preview [ColorMatrix] for a filter id, or null when the id is "none"/unknown. */
internal fun filterPreviewMatrixForId(id: String): ColorMatrix? {
    val chain = FilterCatalog.ffmpeg(id)
    if (chain.isBlank()) return null
    return filterPreviewMatrix(chain)
}
