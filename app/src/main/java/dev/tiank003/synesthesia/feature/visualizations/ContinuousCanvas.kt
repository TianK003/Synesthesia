package dev.tiank003.synesthesia.feature.visualizations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Drop-in replacement for [Canvas] that continuously redraws at vsync rate.
 *
 * Uses [rememberInfiniteTransition] to produce a continuously-changing animated
 * value. Reading that value inside the draw scope causes Compose's snapshot
 * system to auto-invalidate the draw lambda each frame — only the draw pass
 * re-runs, no recomposition or layout pass needed.
 */
@Composable
fun ContinuousCanvas(
    modifier: Modifier = Modifier,
    onDraw: DrawScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "viz_loop")
    val frameTick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame_tick"
    )

    Canvas(modifier = modifier) {
        frameTick  // snapshot state read in draw scope → auto-invalidates every frame
        onDraw()
    }
}
