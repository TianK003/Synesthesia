package dev.tiank003.synesthesia.feature.visualizations.frequency

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Frequency bars arranged radially around the center — like a circular equalizer.
 * Bars radiate outward; bar length represents energy in that frequency band.
 */
@Singleton
class RadialSpectrumViz @Inject constructor() : SoundVisualization {

    override val id = "radial_spectrum"
    override val displayName = "Radial Spectrum"
    override val description = "Frequency bars arranged radially around the center."
    override val category = VizCategory.FREQUENCY

    private val numBands = 64
    private val _bandEnergies = AtomicReference(FloatArray(numBands))
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        val fftSize = frequency.fftSize
        val sr = frequency.sampleRate
        if (mags.isEmpty()) return

        val rawBands = FloatArray(numBands)
        val minFreq = 20f
        val maxFreq = (sr / 2f).coerceAtMost(20000f)

        for (b in 0 until numBands) {
            val freqLow = minFreq * (maxFreq / minFreq).pow(b.toFloat() / numBands)
            val freqHigh = minFreq * (maxFreq / minFreq).pow((b + 1f) / numBands)
            val binLow = (freqLow * fftSize / sr).toInt().coerceIn(0, mags.size - 1)
            val binHigh = (freqHigh * fftSize / sr).toInt().coerceIn(binLow, mags.size - 1)
            var sum = 0f
            for (k in binLow..binHigh) sum += mags[k]
            rawBands[b] = if (binHigh > binLow) sum / (binHigh - binLow + 1) else sum
        }

        val prev = _bandEnergies.get()
        _bandEnergies.set(FloatArray(numBands) { i -> prev[i] * 0.75f + rawBands[i] * 0.25f })
        _renderTick.update { it + 1 }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick by _renderTick.collectAsState()
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = modifier.fillMaxSize()) {
            val bands = _bandEnergies.get()
            val maxVal = bands.max().coerceAtLeast(1e-6f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val innerRadius = minOf(size.width, size.height) * 0.2f
            val maxBarLen = minOf(size.width, size.height) * 0.3f

            for (i in bands.indices) {
                val angle = (2.0 * PI * i / numBands - PI / 2.0).toFloat()
                val normalizedLen = (bands[i] / maxVal).coerceIn(0f, 1f)
                val barLen = normalizedLen * maxBarLen

                val startX = cx + cos(angle) * innerRadius
                val startY = cy + sin(angle) * innerRadius
                val endX = cx + cos(angle) * (innerRadius + barLen)
                val endY = cy + sin(angle) * (innerRadius + barLen)

                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(primary, tertiary),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY)
                    ),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = (size.width / numBands * 0.6f).coerceAtLeast(2f)
                )
            }
        }
    }
}
