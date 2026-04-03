package dev.tiank003.synesthesia.feature.lab

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tiank003.synesthesia.R
import dev.tiank003.synesthesia.core.audio.AudioRepository
import dev.tiank003.synesthesia.feature.visualizations.LocalAudioTick

/**
 * Full-screen visualization player.
 *
 * When [vizId] is non-null (navigated from the Explore carousel) the visualization
 * is selected immediately and microphone capture starts automatically.
 * When null (Lab tab) it shows the last selected viz or an empty state.
 *
 * Audio controls are rendered as a translucent bottom overlay so the viz fills the
 * entire screen edge-to-edge. Both mic and file input are available from the overlay.
 */
@Composable
fun LabScreen(
    vizId: String? = null,
    onBack: (() -> Unit)? = null,
    viewModel: LabViewModel = hiltViewModel()
) {
    val currentViz by viewModel.currentViz.collectAsStateWithLifecycle()
    val audioMode by viewModel.audioMode.collectAsStateWithLifecycle()
    val audioTick by viewModel.audioTick.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startMic()
    }

    fun startMicWithPermissionCheck() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startMic()
        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.startFile(uri)
    }

    LaunchedEffect(vizId) {
        if (vizId != null) {
            viewModel.selectVisualization(vizId)
        }
    }

    // Provide tick top-down: when audioTick changes (every audio frame), any viz Content()
    // that reads LocalAudioTick.current recomposes via standard Compose reactive flow.
    CompositionLocalProvider(LocalAudioTick provides audioTick) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Visualization content (edge-to-edge, under overlays) ──────────────
        Crossfade(
            targetState = currentViz,
            animationSpec = tween(durationMillis = 500),
            modifier = Modifier.fillMaxSize()
        ) { viz ->
            if (viz != null) {
                viz.Content(modifier = Modifier.fillMaxSize())
            } else {
                EmptyState(modifier = Modifier.fillMaxSize())
            }
        }

        // ── Top overlay: back button + viz name ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
            currentViz?.let { viz ->
                Text(
                    text = viz.displayName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(start = if (onBack != null) 4.dp else 16.dp)
                )
            }
        }

        // ── Bottom overlay: audio controls ────────────────────────────────────
        AudioControls(
            mode = audioMode,
            onStartMic = { startMicWithPermissionCheck() },
            onStop = viewModel::stopAudio,
            onPickFile = { filePicker.launch(arrayOf("audio/*")) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
                .padding(horizontal = 24.dp, vertical = 20.dp)
        )
    }
    } // end CompositionLocalProvider
}

@Composable
private fun AudioControls(
    mode: AudioRepository.AudioMode,
    onStartMic: () -> Unit,
    onStop: () -> Unit,
    onPickFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (mode) {
            AudioRepository.AudioMode.Idle -> {
                MicButton(active = false, onClick = onStartMic)
                FileButton(onClick = onPickFile)
            }
            AudioRepository.AudioMode.Mic -> {
                MicButton(active = true, onClick = onStop)
                Text(
                    text = "Listening…",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            is AudioRepository.AudioMode.File -> {
                MicButton(active = true, onClick = onStop)
                Text(
                    text = "Playing file",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun MicButton(active: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(
                color = if (active) MaterialTheme.colorScheme.primary
                else Color.Black.copy(alpha = 0.55f),
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(
                if (active) R.drawable.ic_nav_input_filled else R.drawable.ic_nav_input
            ),
            contentDescription = if (active) "Stop" else "Start microphone",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun FileButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .background(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nav_explore),
            contentDescription = "Pick audio file",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No visualization selected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Browse the Explore tab to pick one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
