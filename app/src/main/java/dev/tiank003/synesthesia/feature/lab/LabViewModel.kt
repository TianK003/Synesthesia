package dev.tiank003.synesthesia.feature.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tiank003.synesthesia.core.pipeline.AudioPipeline
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VisualizationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AudioState {
    data object Idle : AudioState
    data object Recording : AudioState
    data object PlayingFile : AudioState
}

@HiltViewModel
class LabViewModel @Inject constructor(
    private val registry: VisualizationRegistry,
    private val audioPipeline: AudioPipeline
) : ViewModel() {

    private val _currentViz = MutableStateFlow<SoundVisualization?>(null)
    val currentViz: StateFlow<SoundVisualization?> = _currentViz.asStateFlow()

    private val _audioState = MutableStateFlow<AudioState>(AudioState.Idle)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()

    init {
        // Forward pipeline frames to whichever visualization is currently displayed
        viewModelScope.launch {
            combine(
                audioPipeline.audioFrame,
                audioPipeline.frequencyFrame
            ) { audio, freq -> audio to freq }.collect { (audio, freq) ->
                if (audio != null && freq != null) {
                    _currentViz.value?.onAudioFrame(audio, freq)
                }
            }
        }
    }

    fun selectVisualization(vizId: String) {
        _currentViz.value = registry.byId(vizId)
    }

    override fun onCleared() {
        super.onCleared()
        audioPipeline.stop()
    }
}
