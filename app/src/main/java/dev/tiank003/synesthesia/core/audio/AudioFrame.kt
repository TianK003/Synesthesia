package dev.tiank003.synesthesia.core.audio

/**
 * A single captured audio buffer from any [AudioSource].
 *
 * Note: [FloatArray] does not implement structural equality, so two [AudioFrame]
 * instances with identical PCM data will NOT be equal via `==`. This is intentional —
 * equality checks on hot-path data classes would be expensive.
 *
 * @param pcm Raw PCM samples, normalized to -1f..1f.
 * @param sampleRate Sample rate in Hz (e.g. 44100).
 * @param timestampMs Wall-clock time when this frame was captured.
 */
data class AudioFrame(
    val pcm: FloatArray,
    val sampleRate: Int,
    val timestampMs: Long
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
