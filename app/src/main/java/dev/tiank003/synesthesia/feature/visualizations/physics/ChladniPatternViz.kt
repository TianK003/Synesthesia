package dev.tiank003.synesthesia.feature.visualizations.physics

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FeatureExtractors
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs
import kotlin.math.sin

@Singleton
class ChladniPatternViz @Inject constructor() : SoundVisualization {

    override val id = "chladni_patterns"
    override val displayName = "Chladni Patterns"
    override val description = "Nodal patterns on a vibrating plate driven by dominant frequency."
    override val category = VizCategory.PHYSICS

    private data class ChladniState(val frequency: Float, val timeMs: Long)
    private val _state = AtomicReference(ChladniState(440f, System.currentTimeMillis()))
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val dominant = FeatureExtractors.dominantFrequency(
            frequency.magnitudes, frequency.sampleRate, frequency.fftSize
        ).coerceIn(20f, 4000f)
        _state.set(ChladniState(dominant, System.currentTimeMillis()))
        _renderTick.update { it + 1 }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        @Suppress("UNUSED_VARIABLE")
        val tick by _renderTick.collectAsState()
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
        val surface = MaterialTheme.colorScheme.surface

        val shaderSrc = remember {
            context.assets.open("shaders/chladni.agsl").bufferedReader().readText()
        }
        val shader = remember { android.graphics.RuntimeShader(shaderSrc) }

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val timeSec = (state.timeMs % 100_000L) / 1000f

            shader.setFloatUniform("uFrequency", state.frequency)
            shader.setFloatUniform("uTime", timeSec)
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uColorPrimary", primary.red, primary.green, primary.blue, 1f)
            shader.setFloatUniform("uColorSurface", surface.red, surface.green, surface.blue, 1f)

            drawRect(brush = ShaderBrush(shader))
        }
    }

    @Composable
    private fun CanvasFallbackContent(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val surface = MaterialTheme.colorScheme.surface

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val freqNorm = (state.frequency / 4000f).coerceIn(0f, 1f)
            val m = (1 + (freqNorm * 6f).toInt()).toFloat()
            val n = (2 + ((1f - freqNorm) * 5f).toInt()).toFloat()

            // Draw background
            drawRect(surface)

            // Rasterize nodal lines by sampling a grid
            val gridSize = 80
            val cellW = size.width / gridSize
            val cellH = size.height / gridSize

            for (gx in 0 until gridSize) {
                for (gy in 0 until gridSize) {
                    val x = gx.toFloat() / gridSize
                    val y = gy.toFloat() / gridSize
                    val z = sin(m * PI.toFloat() * x) * sin(n * PI.toFloat() * y) -
                            sin(n * PI.toFloat() * x) * sin(m * PI.toFloat() * y)
                    if (abs(z) < 0.1f) {
                        drawRect(
                            color = primary,
                            topLeft = androidx.compose.ui.geometry.Offset(gx * cellW, gy * cellH),
                            size = androidx.compose.ui.geometry.Size(cellW, cellH)
                        )
                    }
                }
            }
        }
    }
}
