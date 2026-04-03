package dev.tiank003.synesthesia.feature.input

import android.annotation.SuppressLint
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tiank003.synesthesia.core.audio.AudioRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InputViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    val audioMode: StateFlow<AudioRepository.AudioMode> = audioRepository.mode

    /** Call only after RECORD_AUDIO permission has been granted. */
    @SuppressLint("MissingPermission")
    fun startMic() = audioRepository.startMic()

    fun startFile(uri: Uri) = audioRepository.startFile(uri)

    fun stop() = audioRepository.stop()
}
