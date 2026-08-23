package com.powercut.editor.data

import java.util.UUID

data class TemplatePreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val category: String = "",
    val aspectRatio: String = "9:16",
    val fps: Int = 30,
    val durationMs: Long = 10000L,
    val effects: List<String> = emptyList(),
    val transitions: List<String> = emptyList(),
    val textAnimation: String = "none",
    val defaultMusicId: String = "",
    val description: String = ""
)