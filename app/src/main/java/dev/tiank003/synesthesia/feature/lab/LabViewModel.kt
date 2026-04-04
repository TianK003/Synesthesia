package dev.tiank003.synesthesia.feature.lab

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tiank003.synesthesia.core.audio.AudioHistoryBuffer
import dev.tiank003.synesthesia.core.audio.AudioRepository
import dev.tiank003.synesthesia.core.pipeline.AudioPipeline
import dev.tiank003.synesthesia.feature.visualizations.HistoryAwareViz
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VisualizationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabViewModel @Inject constructor(
    private val registry: VisualizationRegistry,
    private val pipeline: AudioPipeline,
    private val audioRepository: AudioRepository,
    private val historyBuffer: AudioHistoryBuffer
) : ViewModel() {

    private val _currentViz = MutableStateFlow<SoundVisualization?>(null)
    val currentViz: StateFlow<SoundVisualization?> = _currentViz.asStateFlow()

    /** Mirror of [AudioRepository.mode] so the Lab screen observes audio state. */
    val audioMode: StateFlow<AudioRepository.AudioMode> = audioRepository.mode

    /** Scrub position (0..1) for history-aware visualizations. */
    val scrubFraction: StateFlow<Float> = historyBuffer.scrubFraction

    /** True when the scrub bar should be visible. */
    val showScrubBar: StateFlow<Boolean> = combine(audioMode, _currentViz) { mode, viz ->
        mode == AudioRepository.AudioMode.Idle
            && viz is HistoryAwareViz
            && historyBuffer.recordedDurationSec() > 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** Total recorded duration in seconds. */
    fun recordedDurationSec(): Float = historyBuffer.recordedDurationSec()

    init {
        // Forward pipeline frames to whichever visualization is currently displayed.
        // Each viz's ContinuousCanvas drives its own render loop at vsync rate,
        // so we only need to push data — no recomposition tick required.
        viewModelScope.launch(Dispatchers.Default) {
            pipeline.framesPair.collect { pair ->
                if (pair != null) {
                    _currentViz.value?.onAudioFrame(pair.first, pair.second)
                }
            }
        }

        // Track recording state changes to manage the history buffer.
        viewModelScope.launch {
            audioMode.collect { mode ->
                when (mode) {
                    AudioRepository.AudioMode.Idle -> {
                        historyBuffer.setRecording(false)
                        historyBuffer.setScrub(1f)
                    }
                    else -> {
                        historyBuffer.clear()
                        historyBuffer.setRecording(true)
                    }
                }
            }
        }
    }

    fun selectVisualization(vizId: String) {
        _currentViz.value = registry.byId(vizId)
    }

    fun onScrub(fraction: Float) {
        historyBuffer.setScrub(fraction)
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
