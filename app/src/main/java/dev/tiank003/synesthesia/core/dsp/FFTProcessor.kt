package dev.tiank003.synesthesia.core.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Iterative Cooley-Tukey radix-2 Decimation-in-Time (DIT) FFT.
 *
 * All internal arrays ([re], [im], twiddle factors, bit-reversal table) are allocated
 * once at construction and reused across calls — zero allocation in [process].
 *
 * @param fftSize Must be a power of 2 (512, 1024, 2048, 4096, …).
 */
class FFTProcessor(val fftSize: Int) {

    init {
        require(fftSize > 0 && fftSize and (fftSize - 1) == 0) {
            "fftSize must be a power of 2, got $fftSize"
        }
    }

    // Working buffers — reused every call, never allocated in process()
    private val re = FloatArray(fftSize)
    private val im = FloatArray(fftSize)

    // Pre-computed bit-reversal permutation
    private val bitRev = IntArray(fftSize).also { table ->
        val bits = Integer.numberOfTrailingZeros(fftSize)
        for (i in 0 until fftSize) {
            var x = i
            var rev = 0
            repeat(bits) {
                rev = (rev shl 1) or (x and 1)
                x = x shr 1
            }
            table[i] = rev
        }
    }

    // Pre-computed twiddle factors: cos/sin for each stage
    // twiddleCos[k] = cos(-2π*k/fftSize), twiddleSin[k] = sin(-2π*k/fftSize)
    private val twiddleCos = FloatArray(fftSize / 2) { k ->
        cos(-2.0 * PI * k / fftSize).toFloat()
    }
    private val twiddleSin = FloatArray(fftSize / 2) { k ->
        sin(-2.0 * PI * k / fftSize).toFloat()
    }

    /**
     * Compute the magnitude spectrum of [windowed].
     *
     * @param windowed Pre-windowed PCM samples; must be exactly [fftSize] elements.
     * @return Magnitude array of length [fftSize] / 2 (positive-frequency bins only).
     *         The returned array is a new allocation — callers own it.
     */
    fun process(windowed: FloatArray): FloatArray {
        require(windowed.size == fftSize) {
            "Input length must equal fftSize ($fftSize), got ${windowed.size}"
        }

        // Copy input into working buffers with bit-reversal permutation
        for (i in 0 until fftSize) {
            re[bitRev[i]] = windowed[i]
            im[bitRev[i]] = 0f
        }

        // Iterative Cooley-Tukey butterfly
        var halfLen = 1
        while (halfLen < fftSize) {
            val len = halfLen shl 1
            val step = fftSize / len   // twiddle factor step through the pre-computed table
            var i = 0
            while (i < fftSize) {
                for (j in 0 until halfLen) {
                    val twiddleIdx = j * step
                    val tCos = twiddleCos[twiddleIdx]
                    val tSin = twiddleSin[twiddleIdx]
                    val uRe = re[i + j]
                    val uIm = im[i + j]
                    val vRe = re[i + j + halfLen] * tCos - im[i + j + halfLen] * tSin
                    val vIm = re[i + j + halfLen] * tSin + im[i + j + halfLen] * tCos
                    re[i + j] = uRe + vRe
                    im[i + j] = uIm + vIm
                    re[i + j + halfLen] = uRe - vRe
                    im[i + j + halfLen] = uIm - vIm
                }
                i += len
            }
            halfLen = len
        }

        // Compute magnitude for positive-frequency half only
        val half = fftSize / 2
        return FloatArray(half) { k ->
            sqrt(re[k] * re[k] + im[k] * im[k])
        }
    }
}
