package dev.tiank003.synesthesia.feature.visualizations

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import dev.tiank003.synesthesia.feature.visualizations.artistic.KaleidoscopeViz
import dev.tiank003.synesthesia.feature.visualizations.artistic.LissajousCurvesViz
import dev.tiank003.synesthesia.feature.visualizations.artistic.TerrainMeshViz
import dev.tiank003.synesthesia.feature.visualizations.frequency.FrequencyBarsViz
import dev.tiank003.synesthesia.feature.visualizations.frequency.RadialSpectrumViz
import dev.tiank003.synesthesia.feature.visualizations.frequency.SpectrogramViz
import dev.tiank003.synesthesia.feature.visualizations.generative.FractalTreeViz
import dev.tiank003.synesthesia.feature.visualizations.generative.ParticleFlowFieldViz
import dev.tiank003.synesthesia.feature.visualizations.generative.VoronoiCellsViz
import dev.tiank003.synesthesia.feature.visualizations.physics.ChladniPatternViz
import dev.tiank003.synesthesia.feature.visualizations.physics.CymaticsRingsViz
import dev.tiank003.synesthesia.feature.visualizations.physics.WaveInterferenceViz
import dev.tiank003.synesthesia.feature.visualizations.waveform.CircularWaveformViz
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

    @Binds @IntoSet
    abstract fun bindCircularWaveform(impl: CircularWaveformViz): SoundVisualization

    // ---- Frequency ----
    @Binds @IntoSet
    abstract fun bindFrequencyBars(impl: FrequencyBarsViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindSpectrogram(impl: SpectrogramViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindRadialSpectrum(impl: RadialSpectrumViz): SoundVisualization

    // ---- Physics ----
    @Binds @IntoSet
    abstract fun bindChladniPattern(impl: ChladniPatternViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindCymaticsRings(impl: CymaticsRingsViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindWaveInterference(impl: WaveInterferenceViz): SoundVisualization

    // ---- Generative ----
    @Binds @IntoSet
    abstract fun bindVoronoiCells(impl: VoronoiCellsViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindParticleFlowField(impl: ParticleFlowFieldViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindFractalTree(impl: FractalTreeViz): SoundVisualization

    // ---- Artistic ----
    @Binds @IntoSet
    abstract fun bindKaleidoscope(impl: KaleidoscopeViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindTerrainMesh(impl: TerrainMeshViz): SoundVisualization

    @Binds @IntoSet
    abstract fun bindLissajousCurves(impl: LissajousCurvesViz): SoundVisualization
}
