package dev.tiank003.synesthesia.feature.explore

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tiank003.synesthesia.feature.visualizations.SoundVisualization
import dev.tiank003.synesthesia.feature.visualizations.VizCategory
import dev.tiank003.synesthesia.feature.visualizations.VisualizationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    registry: VisualizationRegistry
) : ViewModel() {

    private val _visualizations = MutableStateFlow(registry.allSorted())
    val visualizations: StateFlow<List<SoundVisualization>> = _visualizations.asStateFlow()

    val categories: List<VizCategory> = VizCategory.entries
}
