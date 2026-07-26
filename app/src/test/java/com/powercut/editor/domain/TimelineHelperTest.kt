package com.powercut.editor.domain

import com.powercut.editor.domain.timeline.TimelineHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class TimelineHelperTest {

    @Test
    fun formatMillis_underOneMinute_returnsCorrectFormat() {
        val ms = 45000L // 45 seconds
        val formatted = TimelineHelper.formatMillis(ms)
        assertEquals("00:45", formatted)
    }

    @Test
    fun formatMillis_overOneMinute_returnsCorrectFormat() {
        val ms = 125000L // 2 mins 5 secs
        val formatted = TimelineHelper.formatMillis(ms)
        assertEquals("02:05", formatted)
    }

    @Test
    fun formatMillis_overOneHour_returnsCorrectFormat() {
        val ms = 3665000L // 1 hour, 1 min, 5 secs
        val formatted = TimelineHelper.formatMillis(ms)
        assertEquals("01:01:05", formatted)
    }

    @Test
    fun formatMillis_zero_returnsCorrectFormat() {
        val ms = 0L
        val formatted = TimelineHelper.formatMillis(ms)
        assertEquals("00:00", formatted)
    }
}
