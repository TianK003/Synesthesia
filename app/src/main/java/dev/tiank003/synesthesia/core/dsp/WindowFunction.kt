package dev.tiank003.synesthesia.core.dsp

import kotlin.math.PI
import kotlin.math.cos

/**
 * Windowing functions applied to PCM data before FFT to reduce spectral leakage.
 * Each implementation pre-computes coefficients for a given [size] at construction
 * time — trig calls happen once, not per frame.
 */
sealed interface WindowFunction {
    fun apply(samples: FloatArray): FloatArray

    /** Hann window — best general-purpose choice. Zero at both endpoints. */
    class Hann(private val size: Int) : WindowFunction {
        private val coefficients = FloatArray(size) { n ->
            (0.5 * (1.0 - cos(2.0 * PI * n / (size - 1)))).toFloat()
        }

        override fun apply(samples: FloatArray): FloatArray {
            require(samples.size == size) {
                "Window size mismatch: expected $size, got ${samples.size}"
            }
            return FloatArray(size) { i -> samples[i] * coefficients[i] }
        }
    }

    /** Hamming window — slightly higher side-lobe suppression than Hann. */
    class Hamming(private val size: Int) : WindowFunction {
        private val coefficients = FloatArray(size) { n ->
            (0.54 - 0.46 * cos(2.0 * PI * n / (size - 1))).toFloat()
        }

        override fun apply(samples: FloatArray): FloatArray {
            require(samples.size == size) {
                "Window size mismatch: expected $size, got ${samples.size}"
            }
            return FloatArray(size) { i -> samples[i] * coefficients[i] }
        }
    }

    /** Blackman window — lowest side-lobes at the cost of wider main lobe. */
    class Blackman(private val size: Int) : WindowFunction {
        private val coefficients = FloatArray(size) { n ->
            val x = 2.0 * PI * n / (size - 1)
            (0.42 - 0.5 * cos(x) + 0.08 * cos(2.0 * x)).toFloat()
        }

        override fun apply(samples: FloatArray): FloatArray {
            require(samples.size == size) {
                "Window size mismatch: expected $size, got ${samples.size}"
            }
            return FloatArray(size) { i -> samples[i] * coefficients[i] }
        }
    }

    /** Rectangular window — no windowing (identity). Use only for testing. */
    object Rectangular : WindowFunction {
        override fun apply(samples: FloatArray): FloatArray = samples.copyOf()
    }
}
