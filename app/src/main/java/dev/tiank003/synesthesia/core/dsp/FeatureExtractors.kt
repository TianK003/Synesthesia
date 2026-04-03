package dev.tiank003.synesthesia.core.dsp

import kotlin.math.sqrt

/**
 * Pure functions for extracting audio features from PCM and frequency data.
 * All functions are allocation-free except where a new array is explicitly documented.
 */
object FeatureExtractors {

    /**
     * Root Mean Square energy — measures overall loudness.
     * Returns 0f for empty or silent input.
     */
    fun rms(pcm: FloatArray): Float {
        if (pcm.isEmpty()) return 0f
        var sum = 0.0
        for (sample in pcm) sum += sample.toDouble() * sample
        return sqrt(sum / pcm.size).toFloat()
    }

    /**
     * Spectral centroid — the "centre of mass" of the spectrum, expressed in Hz.
     * High centroid = bright/high-pitched content; low centroid = bass-heavy content.
     */
    fun spectralCentroid(magnitudes: FloatArray, sampleRate: Int, fftSize: Int): Float {
        var weightedSum = 0.0
        var totalMagnitude = 0.0
        val binHz = sampleRate.toDouble() / fftSize
        for (k in magnitudes.indices) {
            val freq = k * binHz
            weightedSum += freq * magnitudes[k]
            totalMagnitude += magnitudes[k]
        }
        return if (totalMagnitude < 1e-10) 0f else (weightedSum / totalMagnitude).toFloat()
    }

    /**
     * Spectral flux (onset strength) — positive difference in magnitude between
     * the current and previous frames. Large values signal a transient or beat.
     */
    fun onsetStrength(prevMagnitudes: FloatArray, currMagnitudes: FloatArray): Float {
        require(prevMagnitudes.size == currMagnitudes.size) {
            "Magnitude arrays must be the same length"
        }
        var flux = 0f
        for (k in prevMagnitudes.indices) {
            val diff = currMagnitudes[k] - prevMagnitudes[k]
            if (diff > 0f) flux += diff
        }
        return flux
    }

    /**
     * Dominant frequency — the frequency bin with the highest magnitude, in Hz.
     * Returns 0f for a silent frame.
     */
    fun dominantFrequency(magnitudes: FloatArray, sampleRate: Int, fftSize: Int): Float {
        if (magnitudes.isEmpty()) return 0f
        var maxMag = 0f
        var maxBin = 0
        for (k in magnitudes.indices) {
            if (magnitudes[k] > maxMag) {
                maxMag = magnitudes[k]
                maxBin = k
            }
        }
        if (maxMag < 1e-10f) return 0f
        return maxBin.toFloat() * sampleRate / fftSize
    }
}
