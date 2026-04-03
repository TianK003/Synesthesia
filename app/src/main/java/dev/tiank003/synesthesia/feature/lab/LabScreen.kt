package dev.tiank003.synesthesia.feature.lab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Full-screen visualization player.
 *
 * When [vizId] is non-null (navigated from the Explore carousel), it selects that
 * visualization immediately. When null (navigated from the bottom nav Lab tab), it
 * shows the last selected visualization or an empty state.
 */
@Composable
fun LabScreen(
    vizId: String? = null,
    viewModel: LabViewModel = hiltViewModel()
) {
    LaunchedEffect(vizId) {
        if (vizId != null) viewModel.selectVisualization(vizId)
    }

    val currentViz by viewModel.currentViz.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (currentViz != null) {
            currentViz!!.Content(modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = "Select a visualization from Explore",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
