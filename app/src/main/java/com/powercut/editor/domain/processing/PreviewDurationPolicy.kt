package com.powercut.editor.domain.processing

/**
 * Pure (JVM-testable) policy for the LIVE preview duration gate.
 *
 * The previous preview renderers rejected a clip the moment its duration
 * probe returned `null` (treating "could not read duration" as "video too
 * short"). That misclassification blocked EVERY clip whose duration
 * `MediaMetadataRetriever` failed to read — which happens for many perfectly
 * valid short clips (Reels/Shorts/WhatsApp clips, or any `content://` source
 * the retriever chokes on) — permanently showing the
 * "Filter / effect preview unavailable (video too short)" overlay even though
 * the underlying edit state updated correctly.
 *
 * The fix (per the audit): only reject genuinely degenerate sources — a clip
 * whose duration we actually measured and found to be ≤ [DEGENERATE_FLOOR_MS].
 * A clip whose duration we *cannot* measure is NOT rejected: we attempt the
 * render anyway (the FFmpeg segment extraction clamps to the real file
 * length). Real-world short-form clips are 5–60 s, so any guard must have a
 * far lower floor than the old 0.5 s / 2.4 s thresholds.
 */
object PreviewDurationPolicy {
    /** Clips at or below this duration (ms) are treated as degenerate/unusable. */
    const val DEGENERATE_FLOOR_MS = 250L

    /**
     * @return true only when a clip's duration was *measured* and found to be
     *         at or below the degenerate floor. `null` (duration unknown) is
     *         explicitly allowed — an unreadable duration must never block a
     *         valid clip from previewing.
     */
    fun isDegenerate(durationMs: Long?): Boolean =
        durationMs != null && durationMs < DEGENERATE_FLOOR_MS
}
