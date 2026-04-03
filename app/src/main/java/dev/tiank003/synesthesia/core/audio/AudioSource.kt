package dev.tiank003.synesthesia.core.audio

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over any audio input — microphone or decoded file.
 *
 * Implementations emit [AudioFrame] objects from a cold [Flow] that runs on
 * [kotlinx.coroutines.Dispatchers.Default]. Callers must never collect on Main.
 */
interface AudioSource {
    val sampleRate: Int
    val bufferSize: Int

    /** Begin capturing/decoding audio. Must be called before collecting [audioFrames]. */
    fun start()

    /** Stop capturing/decoding and release resources. Safe to call multiple times. */
    fun stop()

    /**
     * Cold flow of audio frames. Runs on [kotlinx.coroutines.Dispatchers.Default].
     * Cancelling the collector stops the flow cooperatively.
     */
    fun audioFrames(): Flow<AudioFrame>
}
