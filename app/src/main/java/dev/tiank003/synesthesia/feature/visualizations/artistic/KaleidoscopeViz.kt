package dev.tiank003.synesthesia.feature.visualizations.artistic

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FeatureExtractors
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KaleidoscopeViz @Inject constructor() : SoundVisualization {

    override val id = "kaleidoscope"
    override val displayName = "Kaleidoscope"
    override val description = "Mirror-symmetry shader distorted by audio amplitude."
    override val category = VizCategory.ARTISTIC

    private data class KState(val rms: Float, val centroid: Float, val timeMs: Long)
    private val _state = AtomicReference(KState(0f, 0.5f, System.currentTimeMillis()))
    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val rms = FeatureExtractors.rms(audio.pcm)
        val centroid = FeatureExtractors.spectralCentroid(
            frequency.magnitudes, frequency.sampleRate, frequency.fftSize
        )
        val nyquist = frequency.sampleRate / 2f
        _state.set(KState(rms.coerceIn(0f, 1f), (centroid / nyquist).coerceIn(0f, 1f), System.currentTimeMillis()))
    }

    @Composable
    override fun Content(modifier: Modifier) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AgslContent(modifier)
        } else {
            CanvasFallbackContent(modifier)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @Composable
    private fun AgslContent(modifier: Modifier) {
        val context = LocalContext.current
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val shaderSrc = remember {
            context.assets.open("shaders/kaleidoscope.agsl").bufferedReader().readText()
        }
        val shader = remember { android.graphics.RuntimeShader(shaderSrc) }

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val t = (state.timeMs % 100_000L) / 1000f
            shader.setFloatUniform("uTime", t)
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uAmplitude", state.rms)
            shader.setFloatUniform("uCentroid", state.centroid)
            shader.setFloatUniform("uColorPrimary", primary.red, primary.green, primary.blue, 1f)
            shader.setFloatUniform("uColorSecondary", secondary.red, secondary.green, secondary.blue, 1f)
            drawRect(brush = ShaderBrush(shader))
        }
    }

    @Composable
    private fun CanvasFallbackContent(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val cx = size.width / 2f
            val cy = size.height / 2f
            val t = (state.timeMs % 10_000L) / 10_000f * 2f * Math.PI.toFloat()
            val numSegments = 8
            val segAngle = 360f / numSegments

            for (seg in 0 until numSegments) {
                val baseAngle = seg * segAngle
                val distort = 0.05f + state.rms * 0.2f
                val radiusScale = 0.3f + distort * kotlin.math.sin(t + seg).toFloat()
                val radius = minOf(size.width, size.height) * radiusScale

                val color = if (seg % 2 == 0)
                    primary.copy(alpha = 0.3f + state.rms * 0.5f)
                else
                    secondary.copy(alpha = 0.2f + state.rms * 0.4f)

                drawArc(
                    color = color,
                    startAngle = baseAngle + t,
                    sweepAngle = segAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }
    }
}
