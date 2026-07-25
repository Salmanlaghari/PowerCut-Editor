package com.powercut.editor.domain.timeline

import java.util.Locale

object TimelineHelper {
    /**
     * Formats milliseconds into HH:MM:SS or MM:SS representation.
     */
    fun formatMillis(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600

        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
}
