package dev.tiank003.synesthesia.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.tiank003.synesthesia.R
import dev.tiank003.synesthesia.feature.explore.ExploreScreen
import dev.tiank003.synesthesia.feature.input.InputScreen
import dev.tiank003.synesthesia.feature.lab.LabScreen
import dev.tiank003.synesthesia.feature.learn.LearnScreen
import dev.tiank003.synesthesia.ui.theme.StitchTokens
import kotlinx.serialization.Serializable

// ── Type-safe route objects ───────────────────────────────────────────────────

@Serializable object Explore
@Serializable object Lab
@Serializable object Learn
@Serializable object Input
@Serializable data class Player(val vizId: String)

// ── Bottom nav item descriptor ────────────────────────────────────────────────

private data class NavItem(
    val label: String,
    val iconResId: Int,
    val iconFilledResId: Int,
    val route: Any
)

private val navItems = listOf(
    NavItem("Explore", R.drawable.ic_nav_explore, R.drawable.ic_nav_explore_filled, Explore),
    NavItem("Lab",     R.drawable.ic_nav_lab,     R.drawable.ic_nav_lab_filled,     Lab),
    NavItem("Learn",   R.drawable.ic_nav_learn,   R.drawable.ic_nav_learn_filled,   Learn),
    NavItem("Input",   R.drawable.ic_nav_input,   R.drawable.ic_nav_input_filled,   Input),
)

// ── Root NavGraph ─────────────────────────────────────────────────────────────

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    // Hide the bottom nav on the full-screen Player destination
    val showBottomNav = currentDest?.hasRoute(Player::class) != true

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                SynesthesiaBottomNav(
                    currentDest = currentDest,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Explore,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Explore> {
                ExploreScreen(
                    onOpenVisualization = { vizId ->
                        navController.navigate(Player(vizId))
                    }
                )
            }
            composable<Lab> { LabScreen(onBack = null) }
            composable<Learn> { LearnScreen() }
            composable<Input> { InputScreen() }
            composable<Player> { backStackEntry ->
                val player: Player = backStackEntry.toRoute()
                LabScreen(
                    vizId = player.vizId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun SynesthesiaBottomNav(
    currentDest: androidx.navigation.NavDestination?,
    onNavigate: (Any) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 0.dp
    ) {
        navItems.forEach { item ->
            val selected = when (item.route) {
                is Explore -> currentDest?.hasRoute(Explore::class) == true
                is Lab     -> currentDest?.hasRoute(Lab::class) == true
                is Learn   -> currentDest?.hasRoute(Learn::class) == true
                is Input   -> currentDest?.hasRoute(Input::class) == true
                else       -> false
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        painter = painterResource(
                            if (selected) item.iconFilledResId else item.iconResId
                        ),
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    }
}
