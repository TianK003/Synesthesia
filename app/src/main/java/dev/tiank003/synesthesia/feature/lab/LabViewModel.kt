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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    /** Incremented on every audio frame — collected by LabScreen to drive recomposition. */
    private val _audioTick = MutableStateFlow(0)
    val audioTick: StateFlow<Int> = _audioTick.asStateFlow()

    init {
        // Forward pipeline frames to whichever visualization is currently displayed.
        // Also bump _audioTick so LabScreen can provide LocalAudioTick top-down.
        viewModelScope.launch(Dispatchers.Default) {
            pipeline.framesPair.collect { pair ->
                if (pair != null) {
                    _currentViz.value?.onAudioFrame(pair.first, pair.second)
                    _audioTick.update { it + 1 }
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
