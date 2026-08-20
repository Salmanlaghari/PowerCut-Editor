package com.powercut.editor.domain.processing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the fix for the systemic "video too short" preview blocker.
 *
 * Before the fix the preview renderers rejected ANY clip whose duration probe
 * returned `null` (i.e. `durationMs == null || durationMs/1000 < MIN_SOURCE_SEC`)
 * and used floors of 0.5 s (filter) / 2.4 s (transition, animation). That
 * misclassified perfectly valid short clips — and any clip whose duration
 * `MediaMetadataRetriever` failed to read — as "too short", permanently
 * showing the blocking overlay even though the edit state updated.
 *
 * After the fix only a *measured* duration at or below the degenerate floor is
 * rejected, and an *unknown* duration (`null`) is explicitly allowed.
 */
class PreviewDurationPolicyTest {

    @Test
    fun normalThirteenSecondClipIsNotDegenerate() {
        assertFalse(
            "a 13 s clip must NOT be rejected by the preview gate",
            PreviewDurationPolicy.isDegenerate(13_000L)
        )
    }

    @Test
    fun shortThreeSecondClipIsNotDegenerate() {
        assertFalse(
            "a 3 s clip must NOT be rejected by the preview gate",
            PreviewDurationPolicy.isDegenerate(3_000L)
        )
    }

    @Test
    fun shortOneSecondClipIsNotDegenerate() {
        assertFalse(
            "a 1 s clip must NOT be rejected by the preview gate",
            PreviewDurationPolicy.isDegenerate(1_000L)
    )
    }

    @Test
    fun unknownDurationIsAllowed_notBlockedAsTooShort() {
        assertFalse(
            "a clip whose duration cannot be read must NOT be blocked",
            PreviewDurationPolicy.isDegenerate(null)
        )
    }

    @Test
    fun trulyDegenerateClipIsRejected() {
        assertTrue(
            "a 100 ms clip is genuinely degenerate and should be skipped",
            PreviewDurationPolicy.isDegenerate(100L)
        )
    }

    @Test
    fun exactlyAtFloorIsAllowed() {
        // The floor is exclusive: exactly DEGENERATE_FLOOR_MS is treated as usable.
        assertFalse(
            "a clip exactly at the floor should still render",
            PreviewDurationPolicy.isDegenerate(PreviewDurationPolicy.DEGENERATE_FLOOR_MS)
        )
    }
}
