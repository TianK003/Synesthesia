package dev.tiank003.synesthesia.feature.visualizations.waveform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.audio.AudioHistoryBuffer
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.HistoryAwareViz
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classic scrolling oscilloscope — shows a 15-second window of PCM audio.
 *
 * During live recording the latest 15 seconds scroll right-to-left.
 * After recording stops, the waveform freezes and a scrub bar appears
 * (controlled by [AudioHistoryBuffer.scrubFraction]).
 */
@Singleton
class ClassicWaveformViz @Inject constructor(
    override val historyBuffer: AudioHistoryBuffer
) : SoundVisualization, HistoryAwareViz {

    override val id = "classic_waveform"
    override val displayName = "Classic Waveform"
    override val description = "Scrolling oscilloscope — 15 seconds of audio history."
    override val category = VizCategory.WAVEFORM

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        historyBuffer.appendSamples(audio.pcm)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val path = remember { Path() }
        val color = MaterialTheme.colorScheme.primary
        val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

        // Pre-allocated envelope buffers — reused every frame, no allocation in draw
        val envMin = remember { FloatArray(4096) }
        val envMax = remember { FloatArray(4096) }

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val scrubFrac = if (historyBuffer.isRecording) 1f
                else historyBuffer.scrubFraction.value

            val columns = size.width.toInt().coerceIn(1, 4096)
            val validCols = historyBuffer.getEnvelopeWindow(
                windowSec = 15f,
                scrubFrac = scrubFrac,
                outMin = envMin,
                outMax = envMax,
                outCount = columns
            )
            if (validCols < 2) return@ContinuousCanvas

            drawEnvelope(path, envMin, envMax, validCols, color, fillColor)
        }
    }

    private fun DrawScope.drawEnvelope(
        path: Path,
        envMin: FloatArray,
        envMax: FloatArray,
        validCols: Int,
        color: Color,
        fillColor: Color
    ) {
        val cy = size.height / 2f
        val yRange = cy * 0.9f
        // Fixed 2× gain — quiet sounds are small, loud sounds fill the screen
        val yScale = yRange * 2f
        val xStep = size.width / validCols.toFloat()

        // Filled envelope band (min to max)
        path.reset()
        path.moveTo(0f, cy - (envMax[0] * yScale).coerceIn(-yRange, yRange))
        for (i in 1 until validCols) {
            path.lineTo(i * xStep, cy - (envMax[i] * yScale).coerceIn(-yRange, yRange))
        }
        for (i in validCols - 1 downTo 0) {
            path.lineTo(i * xStep, cy - (envMin[i] * yScale).coerceIn(-yRange, yRange))
        }
        path.close()
        drawPath(path, color = fillColor)

        // Center line (midpoint of envelope)
        path.reset()
        val mid0 = ((envMax[0] + envMin[0]) / 2f * yScale).coerceIn(-yRange, yRange)
        path.moveTo(0f, cy - mid0)
        for (i in 1 until validCols) {
            val mid = ((envMax[i] + envMin[i]) / 2f * yScale).coerceIn(-yRange, yRange)
            path.lineTo(i * xStep, cy - mid)
        }
        drawPath(path, color = color, style = Stroke(width = 2f))
    }
}
