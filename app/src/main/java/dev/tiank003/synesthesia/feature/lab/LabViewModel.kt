package dev.tiank003.synesthesia.feature.lab

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tiank003.synesthesia.core.audio.AudioRepository
import dev.tiank003.synesthesia.core.pipeline.AudioPipeline
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VisualizationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabViewModel @Inject constructor(
    private val registry: VisualizationRegistry,
    private val pipeline: AudioPipeline,
    private val audioRepository: AudioRepository
) : ViewModel() {

    private val _currentViz = MutableStateFlow<SoundVisualization?>(null)
    val currentViz: StateFlow<SoundVisualization?> = _currentViz.asStateFlow()

    /** Mirror of [AudioRepository.mode] so the Lab screen observes audio state. */
    val audioMode: StateFlow<AudioRepository.AudioMode> = audioRepository.mode

    init {
        // Forward pipeline frames to whichever visualization is currently displayed
        viewModelScope.launch {
            combine(
                pipeline.audioFrame,
                pipeline.frequencyFrame
            ) { audio, freq -> audio to freq }
                .collect { (audio, freq) ->
                    if (audio != null && freq != null) {
                        _currentViz.value?.onAudioFrame(audio, freq)
                    }
                }
        }
    }

    fun selectVisualization(vizId: String) {
        _currentViz.value = registry.byId(vizId)
    }

    /** Start microphone capture. Call only after RECORD_AUDIO permission is granted. */
    @SuppressLint("MissingPermission")
    fun startMic() = audioRepository.startMic()

    fun startFile(uri: Uri) = audioRepository.startFile(uri)

    fun stopAudio() = audioRepository.stop()

    override fun onCleared() {
        super.onCleared()
        audioRepository.stop()
    }
}
