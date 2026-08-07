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

/** Editor + export shared state. Holds the project, selection, export config. */
class EditorViewModel : ViewModel() {
    private val _project = MutableStateFlow(
        VideoProject(
            name = "My 8K Edit",
            durationUs = 30_000_000L,
            tracks = listOf(
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

    fun togglePlay() { _isPlaying.update { !it } }
    fun setZoom(z: Float) { _zoom.value = z.coerceIn(0.5f, 4f) }
    fun selectTrack(id: String?) { _selectedTrackId.value = id }

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
}
