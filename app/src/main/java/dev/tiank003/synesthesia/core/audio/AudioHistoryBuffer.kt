package dev.tiank003.synesthesia.core.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ring buffer storing up to [MAX_DURATION_SEC] seconds of raw PCM audio,
 * plus a downsampled min/max envelope for efficient waveform rendering.
 *
 * Thread safety: [appendSamples] is called on [kotlinx.coroutines.Dispatchers.Default].
 * [getEnvelopeWindow] is called on Main (inside a draw scope). Atomic counters provide
 * happens-before ordering; envelope reads on Main may lag one frame — visually imperceptible.
 */
@Singleton
class AudioHistoryBuffer @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 44_100
        const val MAX_DURATION_SEC = 60
        const val CAPACITY = SAMPLE_RATE * MAX_DURATION_SEC  // 2,646,000 samples
        const val ENVELOPE_COLS = 4096
        const val SAMPLES_PER_COL = CAPACITY / ENVELOPE_COLS  // ~646 samples per column
    }

    // --- Ring buffer ---
    private val samples = FloatArray(CAPACITY)
    private val writeHead = AtomicInteger(0)
    private val totalWritten = AtomicLong(0)

    // --- Min/max envelope: interleaved [min0, max0, min1, max1, ...] ---
    private val envelope = FloatArray(ENVELOPE_COLS * 2)

    // --- Scrub position: 1.0 = right edge at latest data ---
    private val _scrubFraction = MutableStateFlow(1f)
    val scrubFraction: StateFlow<Float> = _scrubFraction.asStateFlow()

    private val _isRecording = AtomicBoolean(false)
    val isRecording: Boolean get() = _isRecording.get()

    /**
     * Append a PCM frame to the ring buffer and update affected envelope columns.
     * Called on Dispatchers.Default.
     */
    fun appendSamples(pcm: FloatArray) {
        val head = writeHead.get()
        val len = pcm.size
        // Write samples into ring buffer
        for (i in 0 until len) {
            samples[(head + i) % CAPACITY] = pcm[i]
        }
        writeHead.set((head + len) % CAPACITY)
        val newTotal = totalWritten.addAndGet(len.toLong())

        // Update envelope columns affected by this write
        val startCol = ((head.toLong()) / SAMPLES_PER_COL).toInt() % ENVELOPE_COLS
        val endCol = (((head + len - 1).toLong()) / SAMPLES_PER_COL).toInt() % ENVELOPE_COLS

        // Recompute each affected column by scanning its samples
        var col = startCol
        while (true) {
            recomputeEnvelopeCol(col)
            if (col == endCol) break
            col = (col + 1) % ENVELOPE_COLS
        }
    }

    private fun recomputeEnvelopeCol(col: Int) {
        val base = col * SAMPLES_PER_COL
        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        for (i in 0 until SAMPLES_PER_COL) {
            val s = samples[(base + i) % CAPACITY]
            if (s < min) min = s
            if (s > max) max = s
        }
        envelope[col * 2] = min
        envelope[col * 2 + 1] = max
    }

    fun clear() {
        writeHead.set(0)
        totalWritten.set(0)
        samples.fill(0f)
        envelope.fill(0f)
    }

    fun setScrub(fraction: Float) {
        _scrubFraction.value = fraction.coerceIn(0f, 1f)
    }

    fun setRecording(active: Boolean) {
        _isRecording.set(active)
    }

    /** Total recorded duration in seconds (capped at [MAX_DURATION_SEC]). */
    fun recordedDurationSec(): Float {
        val total = totalWritten.get()
        return minOf(total, CAPACITY.toLong()).toFloat() / SAMPLE_RATE
    }

    /**
     * Read envelope data for a [windowSec]-wide window positioned by [scrubFrac].
     *
     * [scrubFrac] = 1.0 means the right edge of the window is at the latest data.
     * [scrubFrac] = 0.0 means the right edge is at [windowSec] from the start of the buffer.
     *
     * Writes into pre-allocated [outMin] and [outMax] arrays.
     * [outCount] is the desired number of output columns (typically screen width in pixels).
     * Returns the number of valid columns written.
     */
    fun getEnvelopeWindow(
        windowSec: Float,
        scrubFrac: Float,
        outMin: FloatArray,
        outMax: FloatArray,
        outCount: Int
    ): Int {
        val total = totalWritten.get()
        if (total == 0L) return 0

        val recordedSamples = minOf(total, CAPACITY.toLong())
        val recordedSec = recordedSamples.toFloat() / SAMPLE_RATE
        val effectiveWindowSec = windowSec.coerceAtMost(recordedSec)

        // Window size in envelope columns
        val windowCols = ((effectiveWindowSec / MAX_DURATION_SEC) * ENVELOPE_COLS).toInt()
            .coerceIn(1, ENVELOPE_COLS)

        // The latest envelope column index (where writeHead points)
        val headCol = ((writeHead.get().toLong()) / SAMPLES_PER_COL).toInt() % ENVELOPE_COLS

        // How far back from the latest data the right edge should be
        val recordedCols = ((recordedSamples / SAMPLES_PER_COL).toInt()).coerceIn(1, ENVELOPE_COLS)
        val maxOffset = (recordedCols - windowCols).coerceAtLeast(0)
        val scrollBackCols = ((1f - scrubFrac) * maxOffset).toInt().coerceIn(0, maxOffset)

        // Right edge of the window in envelope space
        val rightCol = (headCol - scrollBackCols + ENVELOPE_COLS) % ENVELOPE_COLS
        // Left edge
        val leftCol = (rightCol - windowCols + ENVELOPE_COLS) % ENVELOPE_COLS

        // Map windowCols envelope columns to outCount output columns
        val validOut = outCount.coerceAtMost(windowCols)
        for (i in 0 until validOut) {
            // Which envelope column(s) this output pixel maps to
            val envStart = (i.toLong() * windowCols / validOut).toInt()
            val envEnd = (((i + 1).toLong() * windowCols / validOut).toInt() - 1).coerceAtLeast(envStart)

            var minVal = Float.MAX_VALUE
            var maxVal = -Float.MAX_VALUE
            for (e in envStart..envEnd) {
                val col = (leftCol + e) % ENVELOPE_COLS
                val eMin = envelope[col * 2]
                val eMax = envelope[col * 2 + 1]
                if (eMin < minVal) minVal = eMin
                if (eMax > maxVal) maxVal = eMax
            }
            outMin[i] = if (minVal == Float.MAX_VALUE) 0f else minVal
            outMax[i] = if (maxVal == -Float.MAX_VALUE) 0f else maxVal
        }
        return validOut
    }
}
