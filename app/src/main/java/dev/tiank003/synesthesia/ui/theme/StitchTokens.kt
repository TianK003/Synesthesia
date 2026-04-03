package dev.tiank003.synesthesia.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens extracted from docs/design_reference.html.
 *
 * TODO: Replace with Stitch-exported tokens when the Stitch design file is available.
 * Until then these values are hand-measured from the HTML mockup.
 */
object StitchTokens {
    // Spacing
    val SpacingXS = 4.dp
    val SpacingS = 8.dp
    val SpacingM = 16.dp
    val SpacingL = 24.dp
    val SpacingXL = 32.dp
    val SpacingXXL = 48.dp

    // Corner radii
    val RadiusCard = 24.dp          // rounded-3xl — viz cards
    val RadiusCategoryChip = 16.dp  // rounded-2xl — category tiles
    val RadiusButton = 12.dp        // rounded-xl — CTA buttons
    val RadiusPill = 999.dp         // rounded-full — badges, nav pills
    val RadiusBottomSheet = 40.dp   // rounded-t-[2.5rem] — bottom nav bar

    // Elevation / shadow equivalents (used for Material3 tonalElevation)
    val ElevationCard = 2.dp
    val ElevationBottomNav = 8.dp

    // Component sizes
    val TopAppBarHeight = 64.dp
    val BottomNavHeight = 80.dp
    val VizCardAspectW = 4f          // aspect ratio 4:5
    val VizCardAspectH = 5f
    val CategoryChipWidth = 160.dp
    val CategoryChipHeight = 96.dp

    // Pagination dots
    val DotActiveWidth = 32.dp
    val DotHeight = 6.dp
    val DotInactiveSize = 6.dp
}
