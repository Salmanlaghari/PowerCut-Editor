package com.powercut.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.core.EncoderKind
import com.powercut.core.FrameRate
import com.powercut.core.Resolution
import com.powercut.export.ExportConfig
import com.powercut.export.ExportEngine
import com.powercut.model.DAGNode
import com.powercut.model.TimelineTrack
import com.powercut.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** Editor + export shared state. Holds the project, selection, export config. */
class EditorViewModel : ViewModel() {
    val frameProvider = com.powercut.ui.components.EditorPreviewFrameProvider(this)

    private val _project = MutableStateFlow(
        VideoProject(
            name = "My 8K Edit",
            durationUs = 30_000_000L,
            tracks = mutableListOf(
                TimelineTrack("t1", TimelineTrack.TrackType.VIDEO, "Main Clip",
                    0L, 30_000_000L, "content://media/external/video/1"),
                TimelineTrack("t2", TimelineTrack.TrackType.AUDIO, "Bass Loop",
                    2_000_000L, 20_000_000L),
                TimelineTrack("t3", TimelineTrack.TrackType.SUBTITLE, "Captions",
                    5_000_000L, 25_000_000L),
                TimelineTrack("t4", TimelineTrack.TrackType.STICKER, "🔥 Sticker",
                    8_000_000L, 6_000_000L)
            )
        )
    )
    val project: StateFlow<VideoProject> = _project.asStateFlow()

    // Revision counter — bumps on every mutation so UI recomposes (VideoProject
    // is a mutable class; StateFlow's == guard would otherwise swallow updates).
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision.asStateFlow()
    private fun bump() { _revision.value = _revision.value + 1 }

    private val _selectedTrackId = MutableStateFlow<String?>(_project.value.tracks.firstOrNull()?.id)
    val selectedTrackId: StateFlow<String?> = _selectedTrackId.asStateFlow()

    private val _exportConfig = MutableStateFlow(ExportConfig())
    val exportConfig: StateFlow<ExportConfig> = _exportConfig.asStateFlow()

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    // Current playhead position in microseconds for keyframe editing
    private val _playheadPositionUs = MutableStateFlow(0L)
    val playheadPositionUs: StateFlow<Long> = _playheadPositionUs.asStateFlow()

    // Selected effect/filter for keyframe animation
    private val _selectedEffectId = MutableStateFlow<String?>(null)
    val selectedEffectId: StateFlow<String?> = _selectedEffectId.asStateFlow()

    fun togglePlay() { _isPlaying.update { !it } }
    fun setZoom(z: Float) { _zoom.value = z.coerceIn(0.5f, 4f) }
    fun selectTrack(id: String?) { _selectedTrackId.value = id }
    fun updatePlayhead(positionUs: Long) { _playheadPositionUs.value = positionUs }
    fun selectEffect(id: String?) { _selectedEffectId.value = id }

    fun updateResolution(r: Resolution) { _exportConfig.update { it.copy(resolution = r) } }
    fun updateFps(f: FrameRate) { _exportConfig.update { it.copy(fps = f) } }
    fun updateRemoveWatermark(v: Boolean) { _exportConfig.update { it.copy(removeWatermark = v) } }

    /** Add an effect node to the DAG (wired through to the native resolver). */
    fun addDagNode(kind: DAGNode.Kind, params: String = "{}") {
        val p = _project.value
        val srcId = p.dag.firstOrNull { it.kind == DAGNode.Kind.Source }?.id
        val node = DAGNode(kind = kind, paramsJson = params,
                           deps = listOfNotNull(srcId))
        p.dag.add(node)
        bump()
    }

    /** Add a new timeline track (video/audio/text/sticker/overlay). */
    fun addTrack(type: TimelineTrack.TrackType, label: String, startUs: Long = 0L, 
                 durationUs: Long = 5_000_000L, clipUri: String? = null) {
        val p = _project.value
        val track = TimelineTrack(
            id = UUID.randomUUID().toString(),
            type = type,
            label = label,
            startUs = startUs,
            durationUs = durationUs,
            clipUri = clipUri
        )
        p.tracks.add(track)
        bump()
    }

    /** Update an existing track's properties. */
    fun updateTrack(trackId: String, label: String? = null, startUs: Long? = null,
                    durationUs: Long? = null, clipUri: String? = null) {
        val p = _project.value
        val trackIndex = p.tracks.indexOfFirst { it.id == trackId }
        if (trackIndex >= 0) {
            val track = p.tracks[trackIndex]
            p.tracks[trackIndex] = track.copy(
                label = label ?: track.label,
                startUs = startUs ?: track.startUs,
                durationUs = durationUs ?: track.durationUs,
                clipUri = clipUri ?: track.clipUri
            )
            bump()
        }
    }

    /** Remove a track from the timeline. */
    fun removeTrack(trackId: String) {
        val p = _project.value
        p.tracks.removeAll { it.id == trackId }
        if (_selectedTrackId.value == trackId) {
            _selectedTrackId.value = p.tracks.firstOrNull()?.id
        }
        bump()
    }

    /** Add keyframe animation to an effect at specific time position. */
    fun addKeyframe(effectId: String, timeUs: Long, value: Float, property: String = "opacity") {
        val p = _project.value
        val effectIndex = p.dag.indexOfFirst { it.id == effectId }
        if (effectIndex >= 0) {
            val effect = p.dag[effectIndex]
            val params = effect.paramsJson.let { json ->
                try {
                    val map = android.util.JsonReader(java.io.StringReader(json)).use { reader ->
                        parseJsonObject(reader)
                    }
                    val keyframes = map.getOrPut("keyframes") { mutableMapOf<String, MutableList<Pair<Long, Float>>>() }
                        as MutableMap<String, MutableList<Pair<Long, Float>>>
                    keyframes.getOrPut(property) { mutableListOf() }.add(timeUs to value)
                    keyframes[property]?.sortBy { it.first }
                    mapToJson(map)
                } catch (e: Exception) {
                    "{\"$property\":[[$timeUs,$value]]}"
                }
            }
            p.dag[effectIndex] = effect.copy(paramsJson = params)
            bump()
        }
    }

    /** Apply speed change to video track (slowmo/fast forward). */
    fun applySpeedChange(trackId: String, speedFactor: Float) {
        val p = _project.value
        val trackIndex = p.tracks.indexOfFirst { it.id == trackId && it.type == TimelineTrack.TrackType.VIDEO }
        if (trackIndex >= 0) {
            val track = p.tracks[trackIndex]
            val newDuration = (track.durationUs / speedFactor).toLong()
            p.tracks[trackIndex] = track.copy(
                durationUs = newDuration,
                label = "${track.label} (${speedFactor}x)"
            )
            bump()
        }
    }

    /** Helper to parse JSON object into mutable map. */
    @Suppress("UNCHECKED_CAST")
    private fun parseJsonObject(reader: android.util.JsonReader): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            when (val value = reader.peek()) {
                android.util.JsonToken.STRING -> map[name] = reader.nextString()
                android.util.JsonToken.NUMBER -> map[name] = reader.nextDouble()
                android.util.JsonToken.BOOLEAN -> map[name] = reader.nextBoolean()
                android.util.JsonToken.NULL -> reader.nextNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return map
    }

    /** Helper to convert map back to JSON string. */
    private fun mapToJson(map: Map<String, Any>): String {
        val sb = StringBuilder("{")
        map.forEachIndexed { i, (key, value) ->
            if (i > 0) sb.append(",")
            sb.append("\"$key\":")
            when (value) {
                is String -> sb.append("\"$value\"")
                is Number -> sb.append(value)
                is Boolean -> sb.append(value)
                else -> sb.append("\"$value\"")
            }
        }
        sb.append("}")
        return sb.toString()
    }

    override fun onCleared() {
        super.onCleared()
        frameProvider.dispose()
    }
}
