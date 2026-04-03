package dev.tiank003.synesthesia.feature.visualizations.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.LocalAudioTick
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classic scrolling oscilloscope — draws the raw PCM waveform as a polyline.
 *
 * Thread safety: [_currentPcm] is an [AtomicReference] — written on
 * [kotlinx.coroutines.Dispatchers.Default] by [onAudioFrame], read on Main in [Content].
 * No locks needed: the latest frame is always visible without race conditions.
 */
@Singleton
class ClassicWaveformViz @Inject constructor() : SoundVisualization {

    override val id = "classic_waveform"
    override val displayName = "Classic Waveform"
    override val description = "Scrolling oscilloscope — raw PCM amplitude over time."
    override val category = VizCategory.WAVEFORM

    private val _currentPcm = AtomicReference(FloatArray(2048)) // flat line until first frame

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        _currentPcm.set(audio.pcm)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        LocalAudioTick.current // recompose on every audio frame via top-down Compose flow
        // Pre-allocate Path outside DrawScope — never allocate inside the draw lambda
        val path = remember { Path() }
        val color = MaterialTheme.colorScheme.primary

        Canvas(modifier = modifier.fillMaxSize()) {
            val pcm = _currentPcm.get()
            if (pcm.size < 2) return@Canvas

            val cx = size.width / 2f
            val cy = size.height / 2f
            val xStep = size.width / pcm.size.toFloat()
            val yRange = cy * 0.9f  // 90% of half-height

            // Auto-gain: normalize to peak so even quiet input fills the canvas
            val peak = pcm.maxOf { kotlin.math.abs(it) }
            val yScale = if (peak > 0.005f) yRange / peak else yRange * 20f

            path.reset()
            path.moveTo(0f, cy - (pcm[0] * yScale).coerceIn(-yRange, yRange))
            for (i in 1 until pcm.size) {
                path.lineTo(i * xStep, cy - (pcm[i] * yScale).coerceIn(-yRange, yRange))
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
