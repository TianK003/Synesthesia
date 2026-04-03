package dev.tiank003.synesthesia.core.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowFunctionTest {

    private val size = 1024
    private val epsilon = 1e-4f

    @Test
    fun `Hann window first sample is zero`() {
        val hann = WindowFunction.Hann(size)
        val signal = FloatArray(size) { 1f }
        val result = hann.apply(signal)
        assertEquals(0f, result[0], epsilon)
    }

    @Test
    fun `Hann window last sample is zero`() {
        val hann = WindowFunction.Hann(size)
        val signal = FloatArray(size) { 1f }
        val result = hann.apply(signal)
        assertEquals(0f, result[size - 1], epsilon)
    }

    @Test
    fun `Hann window centre sample is approximately 1`() {
        val hann = WindowFunction.Hann(size)
        val signal = FloatArray(size) { 1f }
        val result = hann.apply(signal)
        assertEquals(1f, result[size / 2], 0.01f)
    }

    @Test
    fun `Rectangular window is identity`() {
        val signal = FloatArray(size) { it.toFloat() }
        val result = WindowFunction.Rectangular.apply(signal)
        for (i in signal.indices) {
            assertEquals(signal[i], result[i], epsilon)
        }
    }

    @Test
    fun `output size matches input size`() {
        val hann = WindowFunction.Hann(size)
        val signal = FloatArray(size) { 1f }
        assertEquals(size, hann.apply(signal).size)
    }

    @Test
    fun `Hamming endpoints are non-zero but near zero`() {
        val hamming = WindowFunction.Hamming(size)
        val signal = FloatArray(size) { 1f }
        val result = hamming.apply(signal)
        // Hamming endpoint ≈ 0.08 (not exactly 0 like Hann)
        assertTrue("Start should be near 0", result[0] < 0.1f)
        assertTrue("End should be near 0", result[size - 1] < 0.1f)
    }

    @Test
    fun `size mismatch throws`() {
        val hann = WindowFunction.Hann(512)
        try {
            hann.apply(FloatArray(256))
            assertTrue("Should have thrown", false)
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
