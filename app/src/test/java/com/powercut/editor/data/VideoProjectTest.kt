package com.powercut.editor.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoProjectTest {

    @Test
    fun isTrimmed_withNoTrimBoundaries_returnsFalse() {
        val project = VideoProject(
            videoPath = "/path/to/video.mp4",
            durationMs = 10000L,
            trimStartMs = 0L,
            trimEndMs = 10000L
        )
        assertFalse(project.isTrimmed)
    }

    @Test
    fun isTrimmed_withTrimStart_returnsTrue() {
        val project = VideoProject(
            videoPath = "/path/to/video.mp4",
            durationMs = 10000L,
            trimStartMs = 1000L,
            trimEndMs = 10000L
        )
        assertTrue(project.isTrimmed)
    }

    @Test
    fun isTrimmed_withTrimEnd_returnsTrue() {
        val project = VideoProject(
            videoPath = "/path/to/video.mp4",
            durationMs = 10000L,
            trimStartMs = 0L,
            trimEndMs = 9000L
        )
        assertTrue(project.isTrimmed)
    }

    @Test
    fun targetResolution_defaultIs1080p() {
        val project = VideoProject(videoPath = "/path/to/video.mp4")
        assertEquals("1080p", project.targetResolution)
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
