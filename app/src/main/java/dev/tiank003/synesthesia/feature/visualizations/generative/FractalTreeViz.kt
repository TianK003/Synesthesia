package dev.tiank003.synesthesia.feature.visualizations.generative

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Recursive fractal tree whose branching angle, trunk length, and depth
 * all respond to different frequency bands.
 *
 * The branch geometry is pre-baked into a list of line segments on the audio thread
 * so the Compose draw call is a simple loop with no recursion.
 */
@Singleton
class FractalTreeViz @Inject constructor() : SoundVisualization {

    override val id = "fractal_tree"
    override val displayName = "Fractal Tree"
    override val description = "Recursive branching tree driven by low, mid, and high frequencies."
    override val category = VizCategory.GENERATIVE

    private data class Branch(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val depth: Int)
    private data class TreeState(val branches: List<Branch>, val maxDepth: Int)
    private val _state = AtomicReference(TreeState(emptyList(), 0))

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return

        val rms = FeatureExtractors.rms(audio.pcm)
        val third = mags.size / 3

        // Low band drives left-branch angle, high band drives right-branch angle
        val lowEnergy = mags.slice(0 until third).average().toFloat()
        val midEnergy = mags.slice(third until 2 * third).average().toFloat()
        val highEnergy = mags.slice(2 * third until mags.size).average().toFloat()

        val maxBandVal = maxOf(lowEnergy, midEnergy, highEnergy).coerceAtLeast(1e-6f)
        val leftAngle = (PI / 4 + (lowEnergy / maxBandVal) * PI / 6).toFloat()
        val rightAngle = (PI / 4 + (highEnergy / maxBandVal) * PI / 6).toFloat()
        val maxDepth = (4 + (rms * 50f).toInt()).coerceIn(4, 9)

        // Build branch list on this background thread
        val branches = mutableListOf<Branch>()
        buildTree(
            branches = branches,
            x = 0f, y = 0f,            // normalized coords — scaled in draw
            angle = (-PI / 2).toFloat(),
            length = 1f,
            depth = 0,
            maxDepth = maxDepth,
            leftAngle = leftAngle,
            rightAngle = rightAngle,
            branchFactor = 0.67f
        )
        _state.set(TreeState(branches, maxDepth))
    }

    private fun buildTree(
        branches: MutableList<Branch>,
        x: Float, y: Float,
        angle: Float, length: Float,
        depth: Int, maxDepth: Int,
        leftAngle: Float, rightAngle: Float,
        branchFactor: Float
    ) {
        if (depth >= maxDepth || length < 0.005f) return

        val x2 = x + cos(angle) * length
        val y2 = y + sin(angle) * length
        branches.add(Branch(x, y, x2, y2, depth))

        val nextLen = length * branchFactor
        buildTree(branches, x2, y2, angle - leftAngle, nextLen, depth + 1, maxDepth, leftAngle, rightAngle, branchFactor)
        buildTree(branches, x2, y2, angle + rightAngle, nextLen, depth + 1, maxDepth, leftAngle, rightAngle, branchFactor)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val primary = MaterialTheme.colorScheme.primary
        val tertiary = MaterialTheme.colorScheme.tertiary
        val surface = MaterialTheme.colorScheme.surfaceContainerLow

        Canvas(modifier = modifier.fillMaxSize()) {
            val state = _state.get()
            if (state.branches.isEmpty()) return@Canvas

            val scaleX = size.width
            val scaleY = size.height * 0.9f
            val originX = size.width / 2f
            val originY = size.height

            for (branch in state.branches) {
                val depthRatio = branch.depth.toFloat() / state.maxDepth.coerceAtLeast(1)
                val color = lerp(primary, tertiary, depthRatio)
                val strokeWidth = ((1f - depthRatio) * 8f + 1f)
                drawLine(
                    color = color,
                    start = Offset(originX + branch.x1 * scaleX, originY + branch.y1 * scaleY),
                    end = Offset(originX + branch.x2 * scaleX, originY + branch.y2 * scaleY),
                    strokeWidth = strokeWidth
                )
            }
        }
    }

    private fun lerp(a: Color, b: Color, t: Float): Color {
        val tt = t.coerceIn(0f, 1f)
        return Color(
            red   = a.red   + (b.red   - a.red)   * tt,
            green = a.green + (b.green - a.green) * tt,
            blue  = a.blue  + (b.blue  - a.blue)  * tt,
            alpha = 1f
        )
    }
}
