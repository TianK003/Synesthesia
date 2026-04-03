package dev.tiank003.synesthesia.feature.visualizations.frequency

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import dev.tiank003.synesthesia.core.audio.AudioFrame
import dev.tiank003.synesthesia.core.dsp.FrequencyFrame
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.withLock
import kotlin.math.log10

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

    override fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame) {
        val mags = frequency.magnitudes
        if (mags.isEmpty()) return

        val col = writeCol.getAndUpdate { (it + 1) % COLUMNS }
        val rowStep = mags.size.toFloat() / ROWS

        lock.withLock {
            for (row in 0 until ROWS) {
                val binIndex = ((ROWS - 1 - row) * rowStep).toInt().coerceIn(0, mags.size - 1)
                val mag = mags[binIndex].coerceAtLeast(1e-10f)
                val db = (20f * log10(mag)).coerceIn(DB_MIN, DB_MAX)
                val normalized = (db - DB_MIN) / (DB_MAX - DB_MIN)   // 0..1

                // Map normalized → hue: 240° (blue) at 0, 0° (red) at 1
                val hue = (1f - normalized) * 240f
                val brightness = (normalized * 0.8f + 0.2f)
                paint.color = android.graphics.Color.HSVToColor(
                    floatArrayOf(hue, 1f, brightness)
                )
                bitmapCanvas.drawPoint(col.toFloat(), row.toFloat(), paint)
            }
        }

        writeCol.set((col + 1) % COLUMNS)
    }

    @Composable
    override fun Content(modifier: Modifier) {
        // Snapshot the bitmap state for this frame — safe because we hold the lock briefly
        Canvas(modifier = modifier.fillMaxSize()) {
            lock.withLock {
                val snapshot = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                drawImage(snapshot.asImageBitmap())
            }
        }
    }
}
