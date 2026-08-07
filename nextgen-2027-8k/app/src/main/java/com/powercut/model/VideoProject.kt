package com.powercut.model

import java.util.UUID

/**
 * A renderable node in the effect DAG. The native DAG resolver consumes the
 * project's serialized DAG (topological order). This class is the Kotlin
 * source of truth; the JSON it emits is opaque to C++.
 */
data class DAGNode(
    val kind: Kind,
    val id: String = UUID.randomUUID().toString(),
    val paramsJson: String = "{}",
    val deps: List<String> = emptyList()
) {
    enum class Kind { Source, Filter, Effect, Effect3D, ChromaKey, VFX, AI, Transition }
}

/**
 * A single timeline track. Colors match the editor timeline glass cards:
 * Video = orange, Audio = blue (+waveform), Subtitle = purple, Sticker = yellow.
 */
data class TimelineTrack(
    val id: String,
    val type: TrackType,
    val label: String,
    val startUs: Long,
    val durationUs: Long,
    val clipUri: String? = null
) {
    enum class TrackType { VIDEO, AUDIO, SUBTITLE, STICKER }
}

/**
 * The user's editing project. Holds the effect DAG + timeline tracks.
 * Held as a JNI GlobalRef for the duration of an export (see native_export.cpp).
 */
class VideoProject(
    var name: String = "Untitled",
    var durationUs: Long = 60_000_000L, // 60s default
    val tracks: MutableList<TimelineTrack> = mutableListOf(),
    val dag: MutableList<DAGNode> = mutableListOf()
) {
    /** Serialize the DAG to JSON for the native resolver. Opaque to C++. */
    fun getDagJson(): String {
        if (dag.isEmpty()) return "[]"
        val sb = StringBuilder("[")
        dag.forEachIndexed { i, n ->
            if (i > 0) sb.append(',')
            sb.append("{\"kind\":\"").append(n.kind.name.lowercase())
            sb.append("\",\"id\":\"").append(n.id).append('"')
            sb.append(",\"deps\":[")
            n.deps.forEachIndexed { j, d -> if (j > 0) sb.append(','); sb.append('"').append(d).append('"') }
            sb.append("]}")
        }
        sb.append(']')
        return sb.toString()
    }

    fun getDurationUs(): Long = durationUs
}
