package dev.tiank003.synesthesia.feature.visualizations.frequency

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.ContinuousCanvas
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock
import kotlin.math.log10
import kotlin.math.pow

/**
 * Scrolling spectrogram — a heat-map of frequency content over time.
 *
 * Uses a ring-buffer [Bitmap] of [COLUMNS] × [ROWS] pixels. Each new [FrequencyFrame]
 * paints one column. The bitmap is protected by a [ReentrantLock] because it is written
 * on [kotlinx.coroutines.Dispatchers.Default] and read on Main.
 *
 * Color mapping: magnitude (in dB) → hue (blue=quiet → green → yellow → red=loud).
 */
@Singleton
class SpectrogramViz @Inject constructor() : SoundVisualization {

    override val id = "spectrogram"
    override val displayName = "Spectrogram"
    override val description = "Scrolling heat-map of frequency content over time."
    override val category = VizCategory.FREQUENCY

    private companion object {
        const val COLUMNS = 256   // time columns
        const val ROWS = 128      // frequency rows (downsampled from FFT)
        const val DB_MIN = -80f
        const val DB_MAX = 0f
    }

    private val bitmap = Bitmap.createBitmap(COLUMNS, ROWS, Bitmap.Config.ARGB_8888)
    private val bitmapCanvas = AndroidCanvas(bitmap)
    private val paint = AndroidPaint()
    private val lock = ReentrantLock()
    private val writeCol = AtomicInteger(0)

    // Pre-allocated snapshot bitmap so Content() never allocates a new one per frame
    private val snapshotBitmap = Bitmap.createBitmap(COLUMNS, ROWS, Bitmap.Config.ARGB_8888)
    private val snapshotCanvas = AndroidCanvas(snapshotBitmap)

    // Reused HSV buffer — avoids floatArrayOf() allocation × 128 per onAudioFrame call
    private val hsvBuf = FloatArray(3)

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return

        val col = writeCol.getAndUpdate { (it + 1) % COLUMNS }
        val minFreq = 20f
        val maxFreq = (frequency.sampleRate / 2f).coerceAtMost(20000f)
        val nyquistBin = mags.size

        lock.withLock {
            for (row in 0 until ROWS) {
                // Logarithmic frequency mapping: gives more rows to low/mid frequencies
                val t = (ROWS - 1 - row).toFloat() / (ROWS - 1).coerceAtLeast(1)
                val freq = minFreq * (maxFreq / minFreq).pow(t)
                val binIndex = ((freq / maxFreq) * nyquistBin).toInt().coerceIn(0, mags.size - 1)
                val mag = mags[binIndex].coerceAtLeast(1e-10f)
                val db = (20f * log10(mag)).coerceIn(DB_MIN, DB_MAX)
                val normalized = (db - DB_MIN) / (DB_MAX - DB_MIN)   // 0..1

                // Map normalized → hue: 240° (blue) at 0, 0° (red) at 1
                hsvBuf[0] = (1f - normalized) * 240f
                hsvBuf[1] = 1f
                hsvBuf[2] = normalized * 0.8f + 0.2f
                paint.color = android.graphics.Color.HSVToColor(hsvBuf)
                bitmapCanvas.drawPoint(col.toFloat(), row.toFloat(), paint)
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        ContinuousCanvas(modifier = modifier.fillMaxSize()) {
            val col = writeCol.get()
            val w = size.width.toInt()
            val h = size.height.toInt()

            // Copy ring buffer into pre-allocated snapshot (no new Bitmap allocation)
            lock.withLock { snapshotCanvas.drawBitmap(bitmap, 0f, 0f, null) }
            val img = snapshotBitmap.asImageBitmap()

            // Render as scrolling spectrogram: oldest columns on the left, newest on the right.
            // Left slice: columns [col..COLUMNS-1] → drawn in the left portion of screen
            // Right slice: columns [0..col-1]      → drawn in the right portion of screen
            val leftCols = COLUMNS - col
            val rightCols = col
            val leftScreenW = (leftCols.toFloat() / COLUMNS * w).toInt()
            val rightScreenW = w - leftScreenW

            if (leftCols > 0) {
                drawImage(
                    image = img,
                    srcOffset = IntOffset(col, 0),
                    srcSize = IntSize(leftCols, ROWS),
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(leftScreenW, h)
                )
            }
            if (rightCols > 0) {
                drawImage(
                    image = img,
                    srcOffset = IntOffset(0, 0),
                    srcSize = IntSize(rightCols, ROWS),
                    dstOffset = IntOffset(leftScreenW, 0),
                    dstSize = IntSize(rightScreenW, h)
                )
            }
        }
    }
}
