package dev.tiank003.synesthesia.feature.visualizations.frequency

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

/**
 * Classic equalizer frequency bars with logarithmic band spacing.
 *
 * 32 bands are displayed. Log-spacing means low frequencies (where the ear is most
 * sensitive) get proportionally more bars than ultra-high frequencies.
 * Bar heights are smoothed with a simple decay to avoid jarring jumps.
 */
@Singleton
class FrequencyBarsViz @Inject constructor() : SoundVisualization {

    override val id = "frequency_bars"
    override val displayName = "Frequency Bars"
    override val description = "Classic equalizer bars with logarithmic band spacing."
    override val category = VizCategory.FREQUENCY

    private val numBands = 32
    // Smoothed band energies — written on audio thread, read on render thread
    private val _bandEnergies = AtomicReference(FloatArray(numBands))

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        val fftSize = frequency.fftSize
        val sr = frequency.sampleRate
        if (mags.isEmpty()) return

        val rawBands = FloatArray(numBands)
        // Logarithmic band edges from 20 Hz to Nyquist
        val nyquist = sr / 2f
        val minFreq = 20f
        val maxFreq = nyquist.coerceAtMost(20000f)

        for (b in 0 until numBands) {
            val freqLow = minFreq * (maxFreq / minFreq).pow(b.toFloat() / numBands)
            val freqHigh = minFreq * (maxFreq / minFreq).pow((b + 1f) / numBands)
            val binLow = (freqLow * fftSize / sr).toInt().coerceIn(0, mags.size - 1)
            val binHigh = (freqHigh * fftSize / sr).toInt().coerceIn(binLow, mags.size - 1)
            var sum = 0f
            for (k in binLow..binHigh) sum += mags[k]
            rawBands[b] = if (binHigh > binLow) sum / (binHigh - binLow + 1) else sum
        }

        // Simple exponential smoothing (decay toward new value)
        val prev = _bandEnergies.get()
        val smoothed = FloatArray(numBands) { i ->
            prev[i] * 0.7f + rawBands[i] * 0.3f
        }
        _bandEnergies.set(smoothed)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val bands = _bandEnergies.get()
            if (bands.isEmpty()) return@ContinuousCanvas

            // Find max for normalization
            val maxVal = bands.max().coerceAtLeast(1e-6f)
            val barWidth = size.width / numBands
            val gap = barWidth * 0.15f

            for (i in bands.indices) {
                val normalizedHeight = (bands[i] / maxVal).coerceIn(0f, 1f)
                val barHeight = normalizedHeight * size.height * 0.9f
                val x = i * barWidth + gap / 2f
                val y = size.height - barHeight

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(tertiary, primary),
                        startY = y,
                        endY = size.height
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth - gap, barHeight)
                )
            }
        }
    }
}
