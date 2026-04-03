package dev.tiank003.synesthesia.feature.visualizations.generative

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
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class VoronoiCellsViz @Inject constructor() : SoundVisualization {

    override val id = "voronoi_cells"
    override val displayName = "Voronoi Cells"
    override val description = "Seed points drift by amplitude; cells colored by frequency band."
    override val category = VizCategory.GENERATIVE

    private data class VoronoiState(val rms: Float, val centroid: Float, val timeMs: Long)
    private val _state = AtomicReference(VoronoiState(0f, 0.5f, System.currentTimeMillis()))
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val rms = FeatureExtractors.rms(audio.pcm)
        val centroid = FeatureExtractors.spectralCentroid(
            frequency.magnitudes, frequency.sampleRate, frequency.fftSize
        )
        val nyquist = frequency.sampleRate / 2f
        _state.set(VoronoiState(rms.coerceIn(0f, 1f), (centroid / nyquist).coerceIn(0f, 1f), System.currentTimeMillis()))
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
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val shaderSrc = remember {
            context.assets.open("shaders/voronoi.agsl").bufferedReader().readText()
        }
        val shader = remember { android.graphics.RuntimeShader(shaderSrc) }

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val t = (state.timeMs % 100_000L) / 1000f
            shader.setFloatUniform("uTime", t)
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uAmplitude", state.rms)
            shader.setFloatUniform("uCentroid", state.centroid)
            shader.setFloatUniform("uColorPrimary", primary.red, primary.green, primary.blue, 1f)
            shader.setFloatUniform("uColorSecondary", secondary.red, secondary.green, secondary.blue, 1f)
            shader.setFloatUniform("uColorTertiary", tertiary.red, tertiary.green, tertiary.blue, 1f)
            drawRect(brush = ShaderBrush(shader))
        }
    }

    @Composable
    private fun CanvasFallbackContent(modifier: Modifier) {
        // Simple O(n²) Voronoi with 12 seeds
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val seedColors = listOf(primary, secondary, tertiary)

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val t = (state.timeMs % 10_000L) / 10_000f
            val numSeeds = 12

            // Compute seed positions
            val seeds = Array(numSeeds) { i ->
                val baseX = ((i * 7 + 3) % 11).toFloat() / 11f
                val baseY = ((i * 5 + 1) % 9).toFloat() / 9f
                val drift = 0.05f + state.rms * 0.1f
                Offset(
                    (baseX + kotlin.math.sin(t * 2 * Math.PI.toFloat() * (i % 3 + 1) * 0.3f + i) * drift).coerceIn(0f, 1f) * size.width,
                    (baseY + kotlin.math.cos(t * 2 * Math.PI.toFloat() * (i % 4 + 1) * 0.2f + i) * drift).coerceIn(0f, 1f) * size.height
                )
            }

            // Rasterize at low resolution
            val cols = 50; val rows = 50
            val cw = size.width / cols; val ch = size.height / rows
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val px = col * cw + cw / 2; val py = row * ch + ch / 2
                    var minDist = Float.MAX_VALUE; var closest = 0
                    for (s in seeds.indices) {
                        val d = sqrt((px - seeds[s].x) * (px - seeds[s].x) + (py - seeds[s].y) * (py - seeds[s].y))
                        if (d < minDist) { minDist = d; closest = s }
                    }
                    drawRect(
                        color = seedColors[closest % 3].copy(alpha = 0.3f + state.rms * 0.4f),
                        topLeft = Offset(col * cw, row * ch),
                        size = androidx.compose.ui.geometry.Size(cw, ch)
                    )
                }
            }
        }
    }
}
