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
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Singleton
class WaveInterferenceViz @Inject constructor() : SoundVisualization {

    override val id = "wave_interference"
    override val displayName = "Wave Interference"
    override val description = "Three audio-driven point sources creating ripple interference."
    override val category = VizCategory.PHYSICS

    private data class WaveState(
        val rms: Float, val freq1: Float, val freq2: Float, val freq3: Float, val timeMs: Long
    )
    private val _state = AtomicReference(WaveState(0f, 440f, 660f, 880f, System.currentTimeMillis()))
    private val _renderTick = MutableStateFlow(0)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return
        val rms = FeatureExtractors.rms(audio.pcm)
        val third = mags.size / 3

        fun bandFreq(start: Int, end: Int): Float {
            var maxM = 0f; var maxB = start
            for (k in start until end) if (mags[k] > maxM) { maxM = mags[k]; maxB = k }
            return maxB.toFloat() * frequency.sampleRate / frequency.fftSize
        }

        _state.set(WaveState(
            rms = rms,
            freq1 = bandFreq(0, third).coerceIn(20f, 2000f),
            freq2 = bandFreq(third, 2 * third).coerceIn(20f, 2000f),
            freq3 = bandFreq(2 * third, mags.size).coerceIn(20f, 2000f),
            timeMs = System.currentTimeMillis()
        ))
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
            context.assets.open("shaders/wave_interference.agsl").bufferedReader().readText()
        }
        val shader = remember { android.graphics.RuntimeShader(shaderSrc) }

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            val t = (state.timeMs % 100_000L) / 1000f
            shader.setFloatUniform("uTime", t)
            shader.setFloatUniform("uResolution", size.width, size.height)
            shader.setFloatUniform("uAmplitude", state.rms.coerceIn(0f, 1f))
            shader.setFloatUniform("uFrequency1", state.freq1)
            shader.setFloatUniform("uFrequency2", state.freq2)
            shader.setFloatUniform("uFrequency3", state.freq3)
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
            drawRect(surface)
            val state = _state.get()
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Simple rasterized interference — 3 sources, concentric ring approximation
            val sources = listOf(
                Pair(cx * 0.5f, cy * 0.6f),
                Pair(cx * 1.5f, cy * 0.6f),
                Pair(cx, cy * 1.5f)
            )
            val freqs = listOf(state.freq1, state.freq2, state.freq3)
            val t = (state.timeMs % 10_000L) / 1000f

            val cols = 60; val rows = 60
            val cw = size.width / cols; val ch = size.height / rows

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val px = col * cw + cw / 2
                    val py = row * ch + ch / 2
                    var wave = 0f
                    for (s in sources.indices) {
                        val d = sqrt((px - sources[s].first) * (px - sources[s].first) +
                                     (py - sources[s].second) * (py - sources[s].second))
                        wave += sin(d * freqs[s] * 0.002f - t * 3f).toFloat()
                    }
                    wave /= 3f
                    if (wave > 0.3f) {
                        drawRect(
                            color = primary.copy(alpha = ((wave - 0.3f) * 2f).coerceIn(0f, 0.8f)),
                            topLeft = androidx.compose.ui.geometry.Offset(col * cw, row * ch),
                            size = androidx.compose.ui.geometry.Size(cw, ch)
                        )
                    }
                }
            }
        }
    }
}
