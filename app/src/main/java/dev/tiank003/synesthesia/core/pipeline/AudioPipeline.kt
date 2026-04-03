package dev.tiank003.synesthesia.core.pipeline

import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.audio.AudioSource
import dev.tiank003.synesthesia.core.dsp.FFTProcessor
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.core.dsp.WindowFunction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wires an [AudioSource] through the DSP chain and exposes results as [StateFlow].
 *
 * Processing runs entirely on [Dispatchers.Default]. Consumers collect on Main and
 * receive the latest available frame — the [conflate] operator drops frames if the
 * downstream is slower than the source.
 *
 * Thread safety: all mutable state is accessed only from [Dispatchers.Default] except
 * the [StateFlow] emissions which are thread-safe by design.
 */
@Singleton
class AudioPipeline @Inject constructor(
    private val fftProcessor: FFTProcessor,
    private val windowFunction: WindowFunction
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pipelineJob: Job? = null

    // Pre-allocated window buffer — never re-allocated in the processing loop
    private val windowBuffer = FloatArray(fftProcessor.fftSize)

    private val _audioFrame = MutableStateFlow<AudioFrame?>(null)
    val audioFrame: StateFlow<AudioFrame?> = _audioFrame.asStateFlow()

    private val _frequencyFrame = MutableStateFlow<FrequencyFrame?>(null)
    val frequencyFrame: StateFlow<FrequencyFrame?> = _frequencyFrame.asStateFlow()

    /** Emits a matched (AudioFrame, FrequencyFrame) pair once per processed frame. */
    private val _framesPair = MutableStateFlow<Pair<AudioFrame, FrequencyFrame>?>(null)
    val framesPair: StateFlow<Pair<AudioFrame, FrequencyFrame>?> = _framesPair.asStateFlow()

    val isRunning: Boolean get() = pipelineJob?.isActive == true

    /**
     * Start processing frames from [source]. Cancels any previously running pipeline.
     * [source.start] must have been called before invoking this.
     */
    fun start(source: AudioSource) {
        pipelineJob?.cancel()
        pipelineJob = scope.launch {
            source.audioFrames()
                .conflate()  // drop frames if processing falls behind
                .collect { frame ->
                    processFrame(frame)
                }
        }
    }

    /** Stop the current pipeline. The [AudioSource] is NOT stopped here — caller owns it. */
    fun stop() {
        pipelineJob?.cancel()
        pipelineJob = null
        _audioFrame.value = null
        _frequencyFrame.value = null
        _framesPair.value = null
    }

    private fun processFrame(frame: AudioFrame) {
        // Apply window into the pre-allocated buffer
        val pcm = frame.pcm
        val len = minOf(pcm.size, fftProcessor.fftSize)
        val fftSize = fftProcessor.fftSize

        // Zero-pad if frame is shorter than fftSize; copy windowed samples in-place
        val windowed = windowFunction.apply(
            if (pcm.size == fftSize) pcm
            else FloatArray(fftSize).also { pcm.copyInto(it, 0, 0, len) }
        )

        val freqFrame = FrequencyFrame(
            magnitudes = fftProcessor.process(windowed),
            sampleRate = frame.sampleRate,
            fftSize = fftSize
        )

        _audioFrame.value = frame
        _frequencyFrame.value = freqFrame
        _framesPair.value = frame to freqFrame  // single emission with matched pair
    }

    fun release() {
        stop()
        scope.cancel()
    }
}
