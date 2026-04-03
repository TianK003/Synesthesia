package dev.tiank003.synesthesia.core.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class FFTProcessorTest {

    private val fftSize = 1024
    private val processor = FFTProcessor(fftSize)
    private val epsilon = 1e-2f  // tolerance for floating-point comparison

    @Test
    fun `output length is fftSize divided by 2`() {
        val signal = FloatArray(fftSize) { 0f }
        val magnitudes = processor.process(signal)
        assertEquals(fftSize / 2, magnitudes.size)
    }

    @Test
    fun `silence produces all-zero magnitudes`() {
        val silence = FloatArray(fftSize) { 0f }
        val magnitudes = processor.process(silence)
        for (m in magnitudes) {
            assertTrue("Expected ~0, got $m", m < epsilon)
        }
    }

    @Test
    fun `DC signal produces peak in bin 0`() {
        val dc = FloatArray(fftSize) { 1f }
        val magnitudes = processor.process(dc)
        // Bin 0 should hold almost all the energy
        val max = magnitudes.max()
        assertEquals("Bin 0 should be the peak", max, magnitudes[0], epsilon * fftSize)
    }

    @Test
    fun `sine at known bin produces peak at that bin`() {
        // Generate sin(2π * k / N) — this puts energy exactly in bin k
        val targetBin = 10
        val signal = FloatArray(fftSize) { n ->
            sin(2.0 * PI * targetBin * n / fftSize).toFloat()
        }
        val magnitudes = processor.process(signal)
        val peakBin = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: -1
        assertEquals("Peak should be at bin $targetBin", targetBin, peakBin)
    }

    @Test
    fun `magnitude of unit sine is approximately N divided by 2`() {
        // For a full-cycle sine with amplitude 1, the one-sided FFT magnitude ≈ N/2
        val targetBin = 8
        val signal = FloatArray(fftSize) { n ->
            sin(2.0 * PI * targetBin * n / fftSize).toFloat()
        }
        val magnitudes = processor.process(signal)
        val expected = fftSize / 2f
        assertEquals(expected, magnitudes[targetBin], expected * 0.05f)  // within 5%
    }

    @Test
    fun `invalid fftSize throws`() {
        try {
            FFTProcessor(1000)  // not a power of 2
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `mismatched input length throws`() {
        val processor512 = FFTProcessor(512)
        try {
            processor512.process(FloatArray(256))
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
