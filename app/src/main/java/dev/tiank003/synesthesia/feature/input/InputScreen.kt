package dev.tiank003.synesthesia.feature.input

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.tiank003.synesthesia.R
import dev.tiank003.synesthesia.core.audio.AudioRepository
import dev.tiank003.synesthesia.ui.theme.StitchTokens

@Composable
fun InputScreen(viewModel: InputViewModel = hiltViewModel()) {
    val audioMode by viewModel.audioMode.collectAsStateWithLifecycle()

    // Permission launcher for RECORD_AUDIO
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startMic()
    }

    // File picker for audio files
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.startFile(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Text(
            text = "INPUT SOURCE",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = androidx.compose.ui.unit.TextUnit(
                    2f, androidx.compose.ui.unit.TextUnitType.Sp
                )
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Status badge ──────────────────────────────────────────────────────
        StatusBadge(mode = audioMode)

        Spacer(Modifier.height(4.dp))

        // ── Microphone card ───────────────────────────────────────────────────
        SourceCard(
            title = "Microphone",
            subtitle = "Capture live audio from the device mic",
            iconResId = R.drawable.ic_nav_input,
            active = audioMode is AudioRepository.AudioMode.Mic,
            onStart = {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onStop = viewModel::stop
        )

        // ── File card ─────────────────────────────────────────────────────────
        SourceCard(
            title = "Audio File",
            subtitle = when (val m = audioMode) {
                is AudioRepository.AudioMode.File -> m.uri.lastPathSegment ?: "Playing file"
                else -> "Pick a file from device storage"
            },
            iconResId = R.drawable.ic_nav_explore,
            active = audioMode is AudioRepository.AudioMode.File,
            onStart = { filePicker.launch(arrayOf("audio/*")) },
            onStop = viewModel::stop
        )
    }
}

@Composable
private fun StatusBadge(mode: AudioRepository.AudioMode) {
    val (label, color) = when (mode) {
        AudioRepository.AudioMode.Idle ->
            "No active source" to MaterialTheme.colorScheme.onSurfaceVariant
        AudioRepository.AudioMode.Mic ->
            "Microphone active" to MaterialTheme.colorScheme.primary
        is AudioRepository.AudioMode.File ->
            "File playing" to MaterialTheme.colorScheme.tertiary
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(StitchTokens.RadiusButton)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Composable
private fun SourceCard(
    title: String,
    subtitle: String,
    iconResId: Int,
    active: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(StitchTokens.RadiusCard),
        colors = CardDefaults.cardColors(
            containerColor = if (active)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (active) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (active) {
                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(StitchTokens.RadiusButton)
                ) {
                    Text("Stop")
                }
            } else {
                OutlinedButton(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(StitchTokens.RadiusButton)
                ) {
                    Text("Start")
                }
            }
        }
    }
}
