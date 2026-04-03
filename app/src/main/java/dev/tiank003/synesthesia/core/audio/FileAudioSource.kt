package dev.tiank003.synesthesia.core.audio

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio source that decodes an audio file to PCM via Media3 ExoPlayer.
 *
 * A [PcmCaptureProcessor] is inserted into the ExoPlayer audio renderer chain to
 * intercept decoded PCM. Frames are bridged from the ExoPlayer thread to the
 * coroutine collector via a [Channel].
 *
 * TODO (Phase: File Input): Complete the ExoPlayer RenderersFactory wiring in [startAsync].
 * The [PcmCaptureProcessor] and [Channel] bridge are in place; the ExoPlayer build
 * and renderer configuration need to target the correct Media3 1.6.0 API surface.
 */
@Singleton
class FileAudioSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AudioSource {

    override val sampleRate: Int = 44100
    override val bufferSize: Int = 2048

    @Volatile private var uri: Uri? = null
    @Volatile private var running = false

    private val pcmChannel = Channel<AudioFrame>(Channel.BUFFERED)

    fun setUri(uri: Uri) {
        this.uri = uri
    }

    override fun start() {
        running = true
        // Full Media3 ExoPlayer wiring implemented in startAsync (requires Main thread).
    }

    override fun stop() {
        running = false
        // TODO: release ExoPlayer instance
    }

    override fun audioFrames(): Flow<AudioFrame> = flow {
        for (frame in pcmChannel) {
            emit(frame)
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Send a pre-decoded [AudioFrame] into the pipeline. Called by the ExoPlayer audio
     * processor on the player thread. Thread-safe via [Channel.trySend].
     */
    internal fun deliverFrame(frame: AudioFrame) {
        if (running) pcmChannel.trySend(frame)
    }
}
