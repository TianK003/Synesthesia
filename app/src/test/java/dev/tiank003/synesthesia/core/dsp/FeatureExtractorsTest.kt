package dev.tiank003.synesthesia.core.dsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class FeatureExtractorsTest {

    private val epsilon = 1e-4f

    // --- RMS ---

    @Test
    fun `rms of silence is zero`() {
        val silence = FloatArray(1024) { 0f }
        assertEquals(0f, FeatureExtractors.rms(silence), epsilon)
    }

    @Test
    fun `rms of empty array is zero`() {
        assertEquals(0f, FeatureExtractors.rms(FloatArray(0)), epsilon)
    }

    @Test
    fun `rms of unit amplitude square wave is 1`() {
        // [1, -1, 1, -1, ...] → each sample² = 1 → RMS = 1
        val squareWave = FloatArray(1024) { i -> if (i % 2 == 0) 1f else -1f }
        assertEquals(1f, FeatureExtractors.rms(squareWave), epsilon)
    }

    @Test
    fun `rms of constant value is that value`() {
        val signal = FloatArray(512) { 0.5f }
        assertEquals(0.5f, FeatureExtractors.rms(signal), epsilon)
    }

    // --- Spectral Centroid ---

    @Test
    fun `centroid of flat spectrum is near Nyquist divided by 2`() {
        val fftSize = 1024
        val sampleRate = 44100
        val magnitudes = FloatArray(fftSize / 2) { 1f }
        val centroid = FeatureExtractors.spectralCentroid(magnitudes, sampleRate, fftSize)
        // Flat spectrum → centroid ≈ sampleRate / 4
        val expected = sampleRate / 4f
        assertEquals(expected, centroid, expected * 0.05f)  // within 5%
    }

    @Test
    fun `centroid of silent spectrum is zero`() {
        val magnitudes = FloatArray(512) { 0f }
        assertEquals(0f, FeatureExtractors.spectralCentroid(magnitudes, 44100, 1024), epsilon)
    }

    // --- Dominant Frequency ---

    @Test
    fun `dominant frequency of single-peak spectrum returns correct Hz`() {
        val fftSize = 1024
        val sampleRate = 44100
        val peakBin = 50
        val magnitudes = FloatArray(fftSize / 2) { 0f }
        magnitudes[peakBin] = 100f
        val expectedHz = peakBin.toFloat() * sampleRate / fftSize
        assertEquals(expectedHz, FeatureExtractors.dominantFrequency(magnitudes, sampleRate, fftSize), epsilon)
    }

    @Test
    fun `dominant frequency of silence is zero`() {
        val magnitudes = FloatArray(512) { 0f }
        assertEquals(0f, FeatureExtractors.dominantFrequency(magnitudes, 44100, 1024), epsilon)
    }

    // --- Onset Strength ---

    @Test
    fun `onset strength of identical frames is zero`() {
        val mags = FloatArray(512) { it.toFloat() }
        assertEquals(0f, FeatureExtractors.onsetStrength(mags, mags), epsilon)
    }

    @Test
    fun `onset strength is positive when energy increases`() {
        val prev = FloatArray(512) { 0f }
        val curr = FloatArray(512) { 1f }
        assertTrue(FeatureExtractors.onsetStrength(prev, curr) > 0f)
    }

    @Test
    fun `onset strength ignores decreasing energy bins`() {
        val prev = FloatArray(4) { 10f }
        val curr = FloatArray(4) { 0f }
        // All bins decreased → onset strength = 0
        assertEquals(0f, FeatureExtractors.onsetStrength(prev, curr), epsilon)
    }
}
