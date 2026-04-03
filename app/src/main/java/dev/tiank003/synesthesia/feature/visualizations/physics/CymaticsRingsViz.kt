package dev.tiank003.synesthesia.feature.visualizations.physics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FeatureExtractors
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Concentric standing-wave rings. Each ring's radius and thickness respond to
 * a different frequency band, simulating the rings seen in cymatics experiments.
 */
@Singleton
class CymaticsRingsViz @Inject constructor() : SoundVisualization {

    override val id = "cymatics_rings"
    override val displayName = "Cymatics Rings"
    override val description = "Concentric standing-wave rings responding to frequency peaks."
    override val category = VizCategory.PHYSICS

    private val numRings = 8
    private data class RingState(
        val bandEnergies: FloatArray,
        val rms: Float,
        val timeMs: Long
    )
    private val _state = AtomicReference(
        RingState(FloatArray(numRings), 0f, System.currentTimeMillis())
    )
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return
        val rms = FeatureExtractors.rms(audio.pcm)

        val bandSize = mags.size / numRings
        val bands = FloatArray(numRings) { b ->
            var sum = 0f
            val start = b * bandSize
            val end = (start + bandSize).coerceAtMost(mags.size)
            for (k in start until end) sum += mags[k]
            sum / bandSize.coerceAtLeast(1)
        }

        val prev = _state.get()
        val smoothed = FloatArray(numRings) { i ->
            prev.bandEnergies[i] * 0.8f + bands[i] * 0.2f
        }
        _state.set(RingState(smoothed, rms, System.currentTimeMillis()))
        _renderTick.update { it + 1 }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick by _renderTick.collectAsState()
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxRadius = minOf(size.width, size.height) * 0.48f
            val maxEnergy = state.bandEnergies.max().coerceAtLeast(1e-6f)
            val time = (state.timeMs % 10000L) / 10000f  // 0..1 repeating

            for (i in 0 until numRings) {
                val normalized = state.bandEnergies[i] / maxEnergy
                val baseRadius = maxRadius * (i + 1f) / numRings
                // Sinusoidal modulation: each ring pulses at its own rate
                val modulation = sin(2.0 * PI * time * (i + 1) + i * 0.5).toFloat() * normalized
                val radius = (baseRadius + modulation * maxRadius * 0.08f).coerceAtLeast(2f)
                val strokeWidth = (2f + normalized * 8f)

                val color = when {
                    i % 3 == 0 -> primary
                    i % 3 == 1 -> secondary
                    else -> tertiary
                }
                drawCircle(
                    color = color.copy(alpha = (0.3f + normalized * 0.7f).coerceIn(0f, 1f)),
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}
