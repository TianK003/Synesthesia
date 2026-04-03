package dev.tiank003.synesthesia.feature.visualizations

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the complete set of registered [SoundVisualization] implementations.
 *
 * The set is populated at compile time via Hilt multibindings (`@IntoSet`).
 * Adding a new visualization only requires:
 *   1. Creating an `@Inject`-able class implementing [SoundVisualization].
 *   2. Adding one `@Binds @IntoSet` binding in [VisualizationModule].
 *
 * The `@JvmSuppressWildcards` annotation on the `Set` parameter is mandatory —
 * without it Hilt generates `Set<? extends SoundVisualization>` in Java bytecode
 * which fails type matching during graph validation.
 */
@Singleton
class VisualizationRegistry @Inject constructor(
    val visualizations: Set<@JvmSuppressWildcards SoundVisualization>
) {
    fun byId(id: String): SoundVisualization? = visualizations.find { it.id == id }

    fun byCategory(category: VizCategory): List<SoundVisualization> =
        visualizations.filter { it.category == category }.sortedBy { it.displayName }

    fun allSorted(): List<SoundVisualization> =
        visualizations.sortedWith(compareBy({ it.category.ordinal }, { it.displayName }))
}
