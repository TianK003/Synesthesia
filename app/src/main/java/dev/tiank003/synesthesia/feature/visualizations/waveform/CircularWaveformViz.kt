package dev.tiank003.synesthesia.feature.visualizations.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.cos
import kotlin.math.sin

/**
 * PCM waveform mapped to polar coordinates — forms an organic closed loop
 * whose shape changes in real-time with the audio amplitude.
 */
@Singleton
class CircularWaveformViz @Inject constructor() : SoundVisualization {

    override val id = "circular_waveform"
    override val displayName = "Circular Waveform"
    override val description = "PCM waveform mapped around a circle, modulated by amplitude."
    override val category = VizCategory.WAVEFORM

    private val _currentPcm = AtomicReference(FloatArray(0))
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        _currentPcm.set(audio.pcm)
        _renderTick.update { it + 1 }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick by _renderTick.collectAsState()
        val path = remember { Path() }
        val primary = MaterialTheme.colorScheme.primary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer

        Canvas(modifier = modifier.fillMaxSize()) {
            val pcm = _currentPcm.get()
            if (pcm.isEmpty()) return@Canvas

            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = minOf(size.width, size.height) * 0.3f
            val amplitudeScale = minOf(size.width, size.height) * 0.15f

            path.reset()
            val step = pcm.size.toFloat() / 360   // sample per degree

            for (deg in 0 until 360) {
                val sampleIdx = (deg * step).toInt().coerceIn(0, pcm.size - 1)
                val angle = (2.0 * PI * deg / 360 - PI / 2.0).toFloat()
                val r = baseRadius + pcm[sampleIdx] * amplitudeScale

                val x = cx + cos(angle) * r
                val y = cy + sin(angle) * r

                if (deg == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            // Inner fill
            drawPath(path, color = primaryContainer.copy(alpha = 0.3f))
            // Outline
            drawPath(path, color = primary, style = Stroke(width = 3f))
        }
    }
}
