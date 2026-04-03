package dev.tiank003.synesthesia.feature.visualizations.artistic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FeatureExtractors
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.LocalAudioTick
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

/**
 * Lissajous curves — parametric curves x = A·sin(a·t + δ), y = B·sin(b·t).
 *
 * The frequency ratio a/b is driven by the ratio of the two strongest spectral peaks.
 * The phase δ shifts over time proportional to RMS energy.
 */
@Singleton
class LissajousCurvesViz @Inject constructor() : SoundVisualization {

    override val id = "lissajous_curves"
    override val displayName = "Lissajous Curves"
    override val description = "Parametric curves whose frequency ratio shifts with audio peaks."
    override val category = VizCategory.ARTISTIC

    private data class LissajousState(val a: Float, val b: Float, val delta: Float)

    private val _state = AtomicReference(LissajousState(1f, 2f, 0f))
    private var phaseAccumulator = 0f

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return

        val rms = FeatureExtractors.rms(audio.pcm)

        // Find top two peaks for frequency ratio
        var peak1Bin = 0; var peak1Val = 0f
        var peak2Bin = 0; var peak2Val = 0f
        for (k in mags.indices) {
            if (mags[k] > peak1Val) { peak2Bin = peak1Bin; peak2Val = peak1Val; peak1Bin = k; peak1Val = mags[k] }
            else if (mags[k] > peak2Val) { peak2Bin = k; peak2Val = mags[k] }
        }

        // Derive simple integer ratios from the two peaks
        val ratio = if (peak2Bin > 0) peak1Bin.toFloat() / peak2Bin.coerceAtLeast(1) else 1f
        val a = ((ratio * 3f).toInt().coerceIn(1, 5)).toFloat()
        val b = ((1f / ratio * 3f).toInt().coerceIn(1, 5)).toFloat()

        phaseAccumulator += rms * 0.05f
        _state.set(LissajousState(a, b, phaseAccumulator))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        LocalAudioTick.current
        val path = remember { Path() }
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rx = size.width * 0.4f
            val ry = size.height * 0.4f
            val steps = 1024

            path.reset()
            for (i in 0..steps) {
                val t = 2.0 * PI * i / steps
                val x = cx + rx * sin(state.a * t + state.delta).toFloat()
                val y = cy + ry * sin(state.b * t).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()

            drawPath(
                path = path,
                brush = Brush.sweepGradient(
                    colors = listOf(primary, secondary, primary),
                    center = Offset(cx, cy)
                ),
                style = Stroke(width = 2f)
            )
        }
    }
}
