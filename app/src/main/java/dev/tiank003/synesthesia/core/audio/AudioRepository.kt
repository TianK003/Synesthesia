package dev.tiank003.synesthesia.core.audio

import android.Manifest
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import androidx.annotation.RequiresPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tiank003.synesthesia.core.pipeline.AudioPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped coordinator for audio source lifecycle.
 *
 * Responsibilities:
 * - Owns start/stop of [MicAudioSource] and [FileAudioSource]
 * - Manages [AudioPipeline] start/stop as sources change
 * - Requests / abandons audio focus for file playback
 * - Exposes [mode] so UI layers can observe the current audio state
 *
 * Audio focus policy:
 * - **Mic mode**: no focus request (we capture, not play)
 * - **File mode**: `AUDIOFOCUS_GAIN` — we play audio through ExoPlayer
 */
@Singleton
class AudioRepository @Inject constructor(
    private val micSource: MicAudioSource,
    private val fileSource: FileAudioSource,
    private val pipeline: AudioPipeline,
    @param:ApplicationContext private val context: Context
) {
    sealed interface AudioMode {
        data object Idle : AudioMode
        data object Mic : AudioMode
        data class File(val uri: Uri) : AudioMode
    }

    private val _mode = MutableStateFlow<AudioMode>(AudioMode.Idle)
    val mode: StateFlow<AudioMode> = _mode.asStateFlow()

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Start microphone capture. Call only after [RECORD_AUDIO] permission is granted. */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startMic() {
        stopSources()
        micSource.start()
        pipeline.start(micSource)
        _mode.value = AudioMode.Mic
    }

    /** Decode and visualize an audio file. ExoPlayer will also play it audibly. */
    fun startFile(uri: Uri) {
        stopSources()
        requestAudioFocus()
        scope.launch {
            fileSource.startPlayback(uri)
            pipeline.start(fileSource)
            _mode.value = AudioMode.File(uri)
        }
    }

    /** Stop all audio and return to Idle. */
    fun stop() {
        stopSources()
        abandonAudioFocus()
        _mode.value = AudioMode.Idle
    }

    private fun stopSources() {
        pipeline.stop()
        micSource.stop()
        fileSource.stop()
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS ||
                    change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stop()
                }
            }
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
