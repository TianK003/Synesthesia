package dev.tiank003.synesthesia.feature.visualizations

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame

/**
 * Incremented by LabViewModel each time a new audio frame is processed.
 * Viz [SoundVisualization.Content] composables read this value so Compose's
 * top-down recomposition mechanism triggers a redraw on every audio frame.
 * Defaults to 0 (no audio — e.g. in the Explore gallery).
 */
val LocalAudioTick = compositionLocalOf { 0 }

/**
 * Contract that every visualization must implement.
 *
 * [onAudioFrame] is called on [kotlinx.coroutines.Dispatchers.Default] — implementations
 * must use thread-safe state (e.g. [java.util.concurrent.atomic.AtomicReference]) and
 * never block. [Content] is called on the Main thread inside a Compose DrawScope.
 */
interface SoundVisualization {
    val id: String
    val displayName: String

    /** One-liner shown in the gallery card. */
    val description: String

    val category: VizCategory

    /**
     * Receives a new audio+frequency frame. Called on [kotlinx.coroutines.Dispatchers.Default].
     * Must be non-blocking and allocation-free in the hot path.
     */
    fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame)

    /** Self-contained composable that renders the visualization. Called on Main. */
    @Composable
    fun Content(modifier: Modifier)
}

enum class VizCategory(val displayName: String) {
    WAVEFORM("Waveform"),
    FREQUENCY("Frequency"),
    PHYSICS("Physics"),
    GENERATIVE("Generative"),
    ARTISTIC("Artistic")
}
