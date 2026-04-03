package dev.tiank003.synesthesia.core.audio

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audio source backed by the device microphone via [AudioRecord].
 *
 * Uses [AudioFormat.ENCODING_PCM_FLOAT] (API 21+, well within minSdk 29) so samples
 * arrive normalized to -1f..1f without manual conversion.
 */
@Singleton
class MicAudioSource @Inject constructor() : AudioSource {

    override val sampleRate: Int = 44100
    override val bufferSize: Int = 2048

    private val minBufferBytes = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_FLOAT
    )
    // AudioRecord buffer must be at least minBufferBytes; use a multiple for safety
    private val recordBufferBytes = maxOf(bufferSize * 4 * Float.SIZE_BYTES, minBufferBytes)

    @Volatile
    private var audioRecord: AudioRecord? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun start() {
        if (audioRecord != null) return
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            recordBufferBytes
        ).also { it.startRecording() }
    }

    override fun stop() {
        audioRecord?.let {
            if (it.state == AudioRecord.STATE_INITIALIZED) {
                it.stop()
                it.release()
            }
        }
        audioRecord = null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun audioFrames(): Flow<AudioFrame> = flow {
        val readBuffer = FloatArray(bufferSize)
        val record = requireNotNull(audioRecord) {
            "Call start() before collecting audioFrames()"
        }
        while (currentCoroutineContext().isActive) {
            val read = record.read(readBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING)
            if (read > 0) {
                emit(
                    AudioFrame(
                        pcm = readBuffer.copyOf(read),
                        sampleRate = sampleRate,
                        timestampMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }.flowOn(Dispatchers.Default)
}
