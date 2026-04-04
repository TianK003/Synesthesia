package dev.tiank003.synesthesia.feature.visualizations.generative

import android.graphics.Paint as NativePaint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FeatureExtractors
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

/**
 * Perlin-noise flow field with 800 particles. Noise parameters (scale, speed, turbulence)
 * are modulated by RMS energy and spectral centroid.
 *
 * Particle state is stored in parallel FloatArrays for cache efficiency.
 * All particle updates happen on the audio thread; the Compose draw just reads via AtomicReference.
 */
@Singleton
class ParticleFlowFieldViz @Inject constructor() : SoundVisualization {

    override val id = "particle_flow_field"
    override val displayName = "Particle Flow Field"
    override val description = "800 particles steered by a Perlin-noise flow field driven by audio."
    override val category = VizCategory.GENERATIVE

    private val numParticles = 800
    private val rng = java.util.Random()
    private data class ParticleState(val x: FloatArray, val y: FloatArray, val age: FloatArray)
    private val _particles = AtomicReference(
        ParticleState(
            FloatArray(numParticles) { java.util.Random().nextFloat() },
            FloatArray(numParticles) { java.util.Random().nextFloat() },
            FloatArray(numParticles) { java.util.Random().nextFloat() }
        )
    )

    private var timeAccum = 0f

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val rms = FeatureExtractors.rms(audio.pcm)
        val centroid = FeatureExtractors.spectralCentroid(
            frequency.magnitudes, frequency.sampleRate, frequency.fftSize
        )
        val nyquist = frequency.sampleRate / 2f
        val centroidNorm = (centroid / nyquist).coerceIn(0f, 1f)

        val speed = 0.002f + rms * 0.008f
        val scale = 3f + centroidNorm * 4f
        timeAccum += speed * 0.5f

        val prev = _particles.get()
        val nx = prev.x.copyOf()
        val ny = prev.y.copyOf()
        val nage = prev.age.copyOf()

        for (i in 0 until numParticles) {
            // Simple gradient noise approximation using sin/cos
            val angle = perlinAngle(nx[i], ny[i], timeAccum, scale)
            nx[i] += (kotlin.math.cos(angle) * speed).toFloat()
            ny[i] += (sin(angle) * speed).toFloat()
            nage[i] += 0.005f

            // Wrap or reset particles that leave the canvas or age out
            if (nx[i] < 0f || nx[i] > 1f || ny[i] < 0f || ny[i] > 1f || nage[i] > 1f) {
                nx[i] = rng.nextFloat()
                ny[i] = rng.nextFloat()
                nage[i] = 0f
            }
        }
        _particles.set(ParticleState(nx, ny, nage))
    }

    /** Smooth angle field using layered sines — approximates Perlin noise cheaply. */
    private fun perlinAngle(x: Float, y: Float, t: Float, scale: Float): Double {
        val s = scale.toDouble()
        return 2.0 * PI * (
            0.5 * sin(x * s + t) * sin(y * s * 0.7 + t * 1.3) +
            0.3 * sin(x * s * 1.6 + t * 0.5) * sin(y * s * 1.3) +
            0.2 * sin(x * s * 3.0 - t * 0.7)
        )
    }

    // Pre-allocated flat [x0,y0, x1,y1, ...] buffer for native drawPoints — zero alloc in draw
    private val pointCoords = FloatArray(numParticles * 2)

    @Composable
    override fun Content(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        // Pre-allocate native Paint once; update color when theme changes
        val nativePaint = remember { NativePaint().apply { strokeWidth = 3f } }
        nativePaint.color = primary.copy(alpha = 0.7f).toArgb()

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val state = _particles.get()
            for (i in 0 until numParticles) {
                pointCoords[i * 2]     = state.x[i] * size.width
                pointCoords[i * 2 + 1] = state.y[i] * size.height
            }
            // drawPoints(FloatArray, Paint) is zero-allocation — no boxing, no List wrapper
            drawContext.canvas.nativeCanvas.drawPoints(pointCoords, nativePaint)
        }
    }
}
