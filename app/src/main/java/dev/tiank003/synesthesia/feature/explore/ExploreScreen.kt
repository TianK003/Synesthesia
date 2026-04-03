package dev.tiank003.synesthesia.feature.explore

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import dev.tiank003.synesthesia.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
        // ── Top App Bar ───────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "SYNESTHESIA",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Hero Section ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(horizontal = StitchTokens.SpacingM)
                    .padding(top = StitchTokens.SpacingL, bottom = StitchTokens.SpacingM)
            ) {
                Text(
                    text = "SYNESTHESIA LEARNING HUB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "EXPLORATION\nHUB",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Understand the science of sound through interactive visual translations.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
                Spacer(Modifier.height(StitchTokens.SpacingM))
                // "X Modules Active" badge
                ActiveModulesBadge(count = visualizations.size)
            }

            // ── Visualization Carousel ────────────────────────────────────────
            if (visualizations.isNotEmpty()) {
                VizCarousel(
                    visualizations = visualizations,
                    onOpenVisualization = onOpenVisualization
                )
            }

            // ── Categories ────────────────────────────────────────────────────
            Spacer(Modifier.height(StitchTokens.SpacingL))
            Text(
                text = "EXPLORE CATEGORIES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = StitchTokens.SpacingM)
            )
            Spacer(Modifier.height(StitchTokens.SpacingM))
            CategoryChips(categories = VizCategory.entries)

            Spacer(Modifier.height(StitchTokens.SpacingXXL))
        }
    }
}

@Composable
private fun ActiveModulesBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(StitchTokens.RadiusPill),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = RoundedCornerShape(StitchTokens.RadiusPill)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Animated pulse dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "$count MODULES ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VizCarousel(
    visualizations: List<SoundVisualization>,
    onOpenVisualization: (String) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { visualizations.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 56.dp),
        pageSpacing = StitchTokens.SpacingM,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        VizCard(
            visualization = visualizations[page],
            onOpen = { onOpenVisualization(visualizations[page].id) }
        )
    }

    // Pagination dots
    Spacer(Modifier.height(StitchTokens.SpacingM))
    Row(
        modifier = Modifier.fillMaxWidth(),
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
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun VizCard(
    visualization: SoundVisualization,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(StitchTokens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = StitchTokens.ElevationCard)
    ) {
        Column(modifier = Modifier.padding(StitchTokens.SpacingL)) {
            // Preview area — 4:5 aspect ratio, shows a static canvas preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(StitchTokens.VizCardAspectW / StitchTokens.VizCardAspectH)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                // Static preview composable from the visualization itself
                visualization.Content(
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(StitchTokens.SpacingL))

            // Category label
            Text(
                text = visualization.category.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))

            // Visualization name
            Text(
                text = visualization.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))

            // Description
            Text(
                text = visualization.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(StitchTokens.SpacingL))

            // CTA button
            Button(
                onClick = onOpen,
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

@Composable
private fun CategoryChips(categories: List<VizCategory>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = StitchTokens.SpacingM),
        horizontalArrangement = Arrangement.spacedBy(StitchTokens.SpacingM)
    ) {
        items(categories) { category ->
            CategoryChip(category = category)
        }
    }
}

@Composable
private fun CategoryChip(category: VizCategory) {
    val (bgColor, textColor, borderColor) = when (category) {
        VizCategory.WAVEFORM   -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        VizCategory.FREQUENCY  -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
        VizCategory.PHYSICS    -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.onTertiaryContainer,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        )
        VizCategory.GENERATIVE -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        VizCategory.ARTISTIC   -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.onSecondaryContainer,
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
        )
    }

    Box(
        modifier = Modifier
            .width(StitchTokens.CategoryChipWidth)
            .height(StitchTokens.CategoryChipHeight)
            .clip(RoundedCornerShape(StitchTokens.RadiusCategoryChip))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(StitchTokens.RadiusCategoryChip))
            .clickable { }
            .padding(StitchTokens.SpacingM),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
