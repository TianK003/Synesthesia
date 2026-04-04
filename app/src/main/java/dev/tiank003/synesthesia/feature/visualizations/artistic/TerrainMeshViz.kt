package dev.tiank003.synesthesia.feature.visualizations.artistic

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Terrain mesh — isometric-style height-map where elevation = frequency magnitude.
 *
 * Draws a grid of horizontal lines ("terrain rows") where each row's y-offset
 * is modulated by the magnitude of a frequency band, creating a 3D terrain illusion.
 *
 * TODO: Replace with a full OpenGL ES 3.0 implementation using GLSurfaceView + AndroidView
 * for true 3D rendering at 60 fps.
 */
@Singleton
class TerrainMeshViz @Inject constructor() : SoundVisualization {

    override val id = "terrain_mesh"
    override val displayName = "Terrain Mesh"
    override val description = "Isometric terrain where elevation maps to frequency magnitude."
    override val category = VizCategory.ARTISTIC

    private val _magnitudes = AtomicReference(FloatArray(0))

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        _magnitudes.set(frequency.magnitudes.copyOf())
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val secondary = MaterialTheme.colorScheme.secondary
        val primaryContainer = MaterialTheme.colorScheme.primaryContainer

        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val mags = _magnitudes.get()
            val cols = 32
            val rows = 16

            val maxMag = if (mags.isNotEmpty()) mags.max().coerceAtLeast(1e-6f) else 1f
            val cellW = size.width / cols
            val rowSpacing = size.height / (rows + 2)
            val maxElevation = rowSpacing * 2.5f

            // Draw from back to front (painter's algorithm)
            for (row in 0 until rows) {
                val baseY = size.height * 0.9f - row * rowSpacing
                val path = Path()

                for (col in 0..cols) {
                    val bandIdx = if (mags.isNotEmpty()) {
                        (col * mags.size / cols).coerceIn(0, mags.size - 1)
                    } else 0
                    val elevation = if (mags.isNotEmpty()) {
                        (mags[bandIdx] / maxMag) * maxElevation * (1f - row.toFloat() / rows * 0.5f)
                    } else 0f
                    val x = col * cellW
                    val y = baseY - elevation

                    if (col == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                val depthRatio = row.toFloat() / rows
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        colors = listOf(primary.copy(alpha = 0.3f + depthRatio * 0.5f), secondary),
                        start = Offset(0f, baseY),
                        end = Offset(size.width, baseY)
                    ),
                    style = Stroke(width = 1.5f + depthRatio * 2f)
                )
            }
        }
    }
}
