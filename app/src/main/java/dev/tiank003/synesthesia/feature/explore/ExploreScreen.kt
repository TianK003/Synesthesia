package dev.tiank003.synesthesia.feature.explore

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tiank003.synesthesia.R
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import dev.tiank003.synesthesia.ui.theme.StitchTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenVisualization: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val visualizations by viewModel.visualizations.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "SYNESTHESIA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_notifications),
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        if (visualizations.isNotEmpty()) {
            VizPager(
                visualizations = visualizations,
                onOpenVisualization = onOpenVisualization,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun VizPager(
    visualizations: List<SoundVisualization>,
    onOpenVisualization: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { visualizations.size })

    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val viz = visualizations[page]
            Box(modifier = Modifier.fillMaxSize()) {
                // Static category-colored background (live viz previews cause crashes
                // and excessive resource use from multiple ContinuousCanvas instances)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    vizCategoryColor(viz.category).copy(alpha = 0.3f),
                                    Color.Black
                                )
                            )
                        )
                )

                // Gradient info overlay at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                        .padding(horizontal = 24.dp)
                        .padding(top = 48.dp, bottom = 24.dp)
                ) {
                    Column {
                        Text(
                            text = viz.category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = viz.displayName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = viz.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Button(
                            onClick = { onOpenVisualization(viz.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(StitchTokens.RadiusButton),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "OPEN MODULE",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Pagination dots — sitting above the info overlay
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 200.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(visualizations.size) { index ->
                val isActive = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isActive) StitchTokens.DotActiveWidth else StitchTokens.DotInactiveSize,
                    label = "dot_width"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(StitchTokens.DotHeight)
                        .width(width)
                        .clip(RoundedCornerShape(StitchTokens.RadiusPill))
                        .background(
                            if (isActive) Color.White
                            else Color.White.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}

private fun vizCategoryColor(category: VizCategory): Color = when (category) {
    VizCategory.WAVEFORM -> Color(0xFF4FC3F7)
    VizCategory.FREQUENCY -> Color(0xFFAB47BC)
    VizCategory.PHYSICS -> Color(0xFF66BB6A)
    VizCategory.GENERATIVE -> Color(0xFFFF7043)
    VizCategory.ARTISTIC -> Color(0xFFFFCA28)
}
