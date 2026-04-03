package dev.tiank003.synesthesia.feature.visualizations

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.tiank003.synesthesia.feature.visualizations.waveform.ClassicWaveformViz

/**
 * Hilt multibindings module for all [SoundVisualization] implementations.
 *
 * Each visualization is bound via `@Binds @IntoSet`. Adding a new visualization:
 *   1. Create `class FooViz @Inject constructor(...) : SoundVisualization { ... }`
 *   2. Add `@Binds @IntoSet abstract fun bindFoo(impl: FooViz): SoundVisualization`
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisualizationModule {

    @Multibinds
    abstract fun visualizations(): Set<SoundVisualization>

    // ---- Waveform ----
    @Binds @IntoSet
    abstract fun bindClassicWaveform(impl: ClassicWaveformViz): SoundVisualization
}
