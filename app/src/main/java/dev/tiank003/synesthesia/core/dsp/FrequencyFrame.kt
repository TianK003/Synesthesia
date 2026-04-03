package dev.tiank003.synesthesia.core.dsp

/**
 * FFT output for a single audio frame.
 *
 * Contains only the positive-frequency half of the spectrum (size = fftSize / 2).
 * Magnitudes are linear (not dB) by default.
 *
 * @param magnitudes FFT magnitude spectrum, length = fftSize / 2.
 * @param sampleRate Sample rate of the source audio, in Hz.
 * @param fftSize The full FFT size used to produce this frame (power of 2).
 */
data class FrequencyFrame(
    val magnitudes: FloatArray,
    val sampleRate: Int,
    val fftSize: Int
) {
    /** Frequency resolution in Hz per bin. */
    val binHz: Float get() = sampleRate.toFloat() / fftSize

    /** Convert a bin index to its centre frequency in Hz. */
    fun binToHz(bin: Int): Float = bin * binHz

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
