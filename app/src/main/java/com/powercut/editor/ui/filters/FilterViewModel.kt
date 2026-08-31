package com.powercut.editor.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powercut.editor.domain.ai.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel managing filter state and connecting the UI filter drawer
 * to the AI filter pipeline.
 */
class FilterViewModel : ViewModel() {

    private val pipeline = AIFilterPipeline()

    private val _selectedFilter = MutableStateFlow<FilterPreset?>(null)
    val selectedFilter: StateFlow<FilterPreset?> = _selectedFilter.asStateFlow()

    private val _filterIntensity = MutableStateFlow(0.75f)
    val filterIntensity: StateFlow<Float> = _filterIntensity.asStateFlow()

    private val _selectedTab = MutableStateFlow(EditorTab.FILTERS)
    val selectedTab: StateFlow<EditorTab> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow(FilterCategory.COLOR_LUTS)
    val selectedCategory: StateFlow<FilterCategory> = _selectedCategory.asStateFlow()

    private val _filterConfig = MutableStateFlow(AIFilterConfig())
    val filterConfig: StateFlow<AIFilterConfig> = _filterConfig.asStateFlow()

    val pipelineState: StateFlow<AIFilterPipelineState> = pipeline.pipelineState

    private val _beautyEnabled = MutableStateFlow(false)
    private val _segmentationEnabled = MutableStateFlow(false)
    private val _colorGradingEnabled = MutableStateFlow(false)
    private val _audioReactiveEnabled = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            combine(
                _selectedFilter,
                _filterIntensity,
                _beautyEnabled,
                _segmentationEnabled,
                _colorGradingEnabled,
                _audioReactiveEnabled
            ) { values: Array<*> ->
                val filter = values[0] as? FilterPreset
                val intensity = values[1] as? Float ?: 0.75f
                val beauty = values[2] as? Boolean ?: false
                val seg = values[3] as? Boolean ?: false
                val color = values[4] as? Boolean ?: false
                val audio = values[5] as? Boolean ?: false

                AIFilterConfig(
                    beautyEnabled = beauty,
                    smoothIntensity = if (beauty) intensity else 0f,
                    brightenIntensity = if (beauty) intensity * 0.8f else 0f,
                    reshapeIntensity = if (beauty) intensity * 0.3f else 0f,
                    segmentationEnabled = seg,
                    backgroundBlurIntensity = if (seg) intensity else 0f,
                    neonGlowEnabled = filter == FilterPreset.NEON_OUTLINE,
                    neonGlowIntensity = if (filter == FilterPreset.NEON_OUTLINE) intensity else 0f,
                    colorGradingEnabled = color,
                    colorGradingPreset = mapFilterToPreset(filter),
                    colorGradingIntensity = if (color) intensity else 1f,
                    audioReactiveEnabled = audio,
                    audioPulseIntensity = if (audio) intensity else 0f,
                    audioGlowIntensity = if (audio) intensity * 0.6f else 0f
                )
            }.collect { _filterConfig.value = it }
        }
    }

    fun selectFilter(filter: FilterPreset) {
        _selectedFilter.value = filter
        _filterIntensity.value = 0.75f

        when (filter.category) {
            FilterCategory.AI_FX -> {
                _beautyEnabled.value = filter == FilterPreset.AI_BEAUTY
                _segmentationEnabled.value = filter in listOf(FilterPreset.DEPTH_BOKEH, FilterPreset.FACE_MESH)
                _audioReactiveEnabled.value = filter == FilterPreset.AUDIO_PULSE
                _colorGradingEnabled.value = false
            }
            FilterCategory.COLOR_LUTS -> {
                _colorGradingEnabled.value = true
                _beautyEnabled.value = filter == FilterPreset.BEAUTY_FILTER
                _segmentationEnabled.value = false
                _audioReactiveEnabled.value = false
            }
            FilterCategory.BEAUTY -> {
                _beautyEnabled.value = true
                _segmentationEnabled.value = false
                _colorGradingEnabled.value = false
                _audioReactiveEnabled.value = false
            }
            FilterCategory.BACKGROUND -> {
                _segmentationEnabled.value = true
                _beautyEnabled.value = false
                _colorGradingEnabled.value = false
                _audioReactiveEnabled.value = false
            }
            FilterCategory.AR_MASKS -> {
                _segmentationEnabled.value = true
                _beautyEnabled.value = false
                _colorGradingEnabled.value = false
                _audioReactiveEnabled.value = false
            }
            FilterCategory.AUDIO_FX -> {
                _audioReactiveEnabled.value = true
                _beautyEnabled.value = false
                _segmentationEnabled.value = false
                _colorGradingEnabled.value = false
            }
        }
    }

    fun updateIntensity(intensity: Float) {
        _filterIntensity.value = intensity
    }

    fun selectTab(tab: EditorTab) {
        _selectedTab.value = tab
        // Update category based on tab
        when (tab) {
            EditorTab.FILTERS -> _selectedCategory.value = FilterCategory.COLOR_LUTS
            EditorTab.EFFECTS -> _selectedCategory.value = FilterCategory.AI_FX
            EditorTab.ADJUST -> _selectedCategory.value = FilterCategory.BEAUTY
            EditorTab.STICKERS -> _selectedCategory.value = FilterCategory.AR_MASKS
            EditorTab.MUSIC -> _selectedCategory.value = FilterCategory.AUDIO_FX
        }
    }

    fun selectCategory(category: FilterCategory) {
        _selectedCategory.value = category
    }

    fun clearFilter() {
        _selectedFilter.value = null
        _beautyEnabled.value = false
        _segmentationEnabled.value = false
        _colorGradingEnabled.value = false
        _audioReactiveEnabled.value = false
    }

    private fun mapFilterToPreset(filter: FilterPreset?): ColorGradingPreset {
        return when (filter) {
            FilterPreset.TEAL_ORANGE -> ColorGradingPreset.TEAL_ORANGE
            FilterPreset.VINTAGE_FILM -> ColorGradingPreset.VINTAGE_FILM
            FilterPreset.CYBER_NEON -> ColorGradingPreset.CYBERPUNK
            FilterPreset.FILM_NOIR -> ColorGradingPreset.FILM_NOIR
            FilterPreset.GOLDEN_HOUR -> ColorGradingPreset.GOLDEN_HOUR
            FilterPreset.RETRO_FILM -> ColorGradingPreset.RETRO_FILM
            FilterPreset.COOL_TONE -> ColorGradingPreset.COOL_TONE
            FilterPreset.DRAMATIC -> ColorGradingPreset.TEAL_ORANGE
            FilterPreset.CINEMATIC -> ColorGradingPreset.VINTAGE_FILM
            FilterPreset.BW_DRAMA -> ColorGradingPreset.FILM_NOIR
            FilterPreset.RETRO_80S -> ColorGradingPreset.CYBERPUNK
            else -> ColorGradingPreset.NONE
        }
    }

    override fun onCleared() {
        super.onCleared()
        pipeline.release()
    }
}
