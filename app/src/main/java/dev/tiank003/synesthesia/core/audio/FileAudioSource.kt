package dev.tiank003.synesthesia.core.audio

import android.content.Context
import android.media.AudioFormat
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio source that decodes an audio file to PCM via Media3 ExoPlayer.
 *
 * A [PcmCaptureProcessor] is inserted into the ExoPlayer audio renderer chain to
 * intercept decoded PCM. Frames are bridged from the ExoPlayer thread to the
 * coroutine collector via a [Channel].
 *
 * Call [startPlayback] (suspend) to set the URI and start the player; call [stop]
 * to release ExoPlayer. The [audioFrames] flow stays live as long as the channel is open.
 */
@Singleton
class FileAudioSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AudioSource {

    override val sampleRate: Int = 44100
    override val bufferSize: Int = 2048

    private val frameChannel = Channel<AudioFrame>(Channel.BUFFERED)

    @Volatile private var player: ExoPlayer? = null
    @Volatile private var running = false

    /** Start the ExoPlayer with [uri]. Must be called from a coroutine (switches to Main). */
    suspend fun startPlayback(uri: Uri) {
        stop() // release any previous player
        running = true

        withContext(Dispatchers.Main) {
            val captureProcessor = PcmCaptureProcessor(
                targetSampleRate = sampleRate,
                bufferSize = bufferSize,
                channel = frameChannel,
                isRunning = { running }
            )

            val renderersFactory = object : DefaultRenderersFactory(context) {
                override fun buildAudioSink(
                    context: Context,
                    enableFloatOutput: Boolean,
                    enableAudioTrackPlaybackParams: Boolean
                ): AudioSink? = DefaultAudioSink.Builder(context)
                    .setAudioProcessorChain(
                        DefaultAudioSink.DefaultAudioProcessorChain(captureProcessor)
                    )
                    .setEnableFloatOutput(true)
                    .build()
            }

            player = ExoPlayer.Builder(context, renderersFactory).build().also { p ->
                p.setMediaItem(MediaItem.fromUri(uri))
                p.prepare()
                p.play()
            }
        }
    }

    // AudioSource contract — start/stop used by AudioPipeline
    override fun start() { running = true }

    override fun stop() {
        running = false
        player?.stop()
        player?.release()
        player = null
        // Drain stale frames so the next session starts clean
        while (frameChannel.tryReceive().isSuccess) { /* drain */ }
    }

    override fun audioFrames(): Flow<AudioFrame> =
        frameChannel.receiveAsFlow().flowOn(Dispatchers.Default)
}

// ── PCM capture AudioProcessor ────────────────────────────────────────────────

/**
 * Passthrough [AudioProcessor] that copies every decoded PCM chunk into [channel]
 * as [AudioFrame] objects. Handles PCM_FLOAT and PCM_16BIT encodings, mixing down
 * to mono.
 */
private class PcmCaptureProcessor(
    private val targetSampleRate: Int,
    private val bufferSize: Int,
    private val channel: Channel<AudioFrame>,
    private val isRunning: () -> Boolean
) : AudioProcessor {

    private var format = AudioProcessor.AudioFormat.NOT_SET
    private var configured = false
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false
    private val accumulator = ByteArrayOutputStream()

    override fun configure(inputFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        format = inputFormat
        configured = true
        accumulator.reset()
        return inputFormat // passthrough — do not change format
    }

    override fun isActive(): Boolean = configured

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return

        val bytes = ByteArray(inputBuffer.remaining())
        inputBuffer.get(bytes)

        // Deliver captured frames to channel
        if (configured && isRunning()) captureBytes(bytes)

        // Passthrough: expose the same bytes as output
        outputBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder())
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean = inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER

    override fun queueEndOfStream() { inputEnded = true }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        accumulator.reset()
    }

    override fun reset() {
        flush()
        configured = false
        format = AudioProcessor.AudioFormat.NOT_SET
    }

    private fun captureBytes(bytes: ByteArray) {
        val fmt = format
        if (fmt == AudioProcessor.AudioFormat.NOT_SET) return

        val frameBytes = bufferSize * fmt.bytesPerFrame
        accumulator.write(bytes)

        while (accumulator.size() >= frameBytes) {
            val all = accumulator.toByteArray()
            val chunk = all.copyOf(frameBytes)
            accumulator.reset()
            if (all.size > frameBytes) accumulator.write(all, frameBytes, all.size - frameBytes)

            val floats = toMonoFloat(chunk, fmt)
            channel.trySend(AudioFrame(floats, targetSampleRate, System.currentTimeMillis()))
        }
    }

    private fun toMonoFloat(bytes: ByteArray, fmt: AudioProcessor.AudioFormat): FloatArray {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(bufferSize)
        var outIdx = 0
        when (fmt.encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val fb = buf.asFloatBuffer()
                while (fb.hasRemaining() && outIdx < bufferSize) {
                    var s = 0f
                    repeat(fmt.channelCount) { if (fb.hasRemaining()) s += fb.get() }
                    result[outIdx++] = (s / fmt.channelCount).coerceIn(-1f, 1f)
                }
            }
            AudioFormat.ENCODING_PCM_16BIT -> {
                val sb = buf.asShortBuffer()
                while (sb.hasRemaining() && outIdx < bufferSize) {
                    var s = 0f
                    repeat(fmt.channelCount) { if (sb.hasRemaining()) s += sb.get() / 32768f }
                    result[outIdx++] = (s / fmt.channelCount).coerceIn(-1f, 1f)
                }
            }
        }
        return result
    }
}
