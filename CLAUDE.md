# CLAUDE.md — Synesthesia

## Project Identity
**Synesthesia** is an Android app that transforms sound into a gallery of diverse, real-time visualizations. The core thesis: a single audio signal can produce radically different visual experiences — from scientific spectrograms to organic particle systems to mathematical Chladni patterns. The app supports both live microphone input and audio file playback.

---

## Tech Stack & Architecture

### Platform
- **Language:** Kotlin 2.3.20 (no Java unless wrapping legacy code)
- **Min SDK:** 29 (Android 10) | **Target SDK:** 35
- **Build:** Gradle with Kotlin DSL (`build.gradle.kts`), version catalogs (`libs.versions.toml`)
- **Architecture:** MVVM + Clean Architecture layers (data → domain → presentation)

### Core Dependencies (All Current Stable — No Deprecated Libraries)

```toml
# libs.versions.toml — pinned versions (update quarterly)
[versions]
kotlin              = "2.3.20"
agp                 = "8.13.0"
ksp                 = "2.3.20-1.0.31"      # Must match Kotlin version prefix
composeBom          = "2026.03.00"
material3           = "1.5.0"               # Compose Material 3
hilt                = "2.57.1"
hiltNavigationCompose = "1.3.0"
lifecycleRuntime    = "2.9.0"
navigationCompose   = "2.9.0"
coroutines          = "1.10.1"
media3              = "1.6.0"               # For audio file decoding (replaces old MediaExtractor patterns)
activityCompose     = "1.10.1"

[libraries]
compose-bom                 = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-material3           = { group = "androidx.compose.material3", name = "material3" }
compose-ui                  = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics         = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling-preview  = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-foundation          = { group = "androidx.compose.foundation", name = "foundation" }
lifecycle-runtime-compose    = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntime" }
lifecycle-viewmodel-compose  = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntime" }
navigation-compose          = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android                = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler               = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose     = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
activity-compose            = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
coroutines-core             = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
coroutines-android          = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
media3-exoplayer            = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-common               = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt-android        = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp                 = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### Dependency Rules
- **Use KSP everywhere** — never KAPT (KAPT is deprecated and slower)
- **Use version catalogs** (`libs.versions.toml`) — no hardcoded version strings in build files
- **Compose Compiler** is configured via the `kotlin-compose` Gradle plugin (not the old `compose.compiler` extension)
- **No Accompanist** — most Accompanist libraries are deprecated; use platform APIs or Compose equivalents (e.g., `rememberLauncherForActivityResult` for permissions)
- **No TarsosDSP** — it's Java-only, not published to Maven Central reliably, and overkill for what we need. Instead, implement a pure Kotlin Cooley-Tukey FFT (radix-2, ~80 lines) in `core/dsp/`. This avoids JNI, GPL licensing concerns, and external dependency risk.
- **Use Media3/ExoPlayer** for audio file decoding instead of raw `MediaExtractor` — it handles formats, codecs, and lifecycle correctly

### Audio Capture & Playback
- **Mic input:** Android `AudioRecord` API (available since API 3, rock solid)
- **File input:** Media3 ExoPlayer for decoding → PCM extraction via `AudioProcessor` or `RenderersFactory`
- No need for Oboe (C++ NDK library) — we're visualizing, not synthesizing, so AudioRecord's latency is fine

### Rendering Strategy
- **Compose Canvas** — primary renderer for 2D visualizations (waveform, bars, radial, fractal, Lissajous)
- **AGSL Shaders** (`RuntimeShader`) — for GPU-accelerated viz (Chladni, Voronoi, kaleidoscope, wave interference). **Requires API 33+.**
- **Fallback for API 29–32:** Use Compose Canvas software rendering for shader-based viz, or OpenGL ES 3.0 via `AndroidView` wrapping `GLSurfaceView`
- Always check `Build.VERSION.SDK_INT >= 33` before using `RuntimeShader`

### Design System
- UI design will be provided as code guidelines exported from **Google Stitch**
- When Stitch output is available, extract the theme (colors, typography, shapes, spacing tokens) into `ui/theme/` and use it as the single source of truth
- Until then, use a minimal Material 3 dynamic-color theme with a dark-mode-first approach
- Design philosophy: **clean, bold, content-first** — the visualization IS the UI; chrome should disappear

---

## Audio Pipeline (Critical Path)

```
Mic / File → AudioSource (interface)
  → AudioFrame(pcmSamples: FloatArray, sampleRate: Int, timestamp: Long)
    → DSP Bus (applied in order):
        1. Windowing (Hann)
        2. FFT → FrequencyFrame(magnitudes: FloatArray, phases: FloatArray)
        3. Optional feature extractors (RMS energy, spectral centroid, onset detection, beat tracking)
    → VisualizationEngine (consumes AudioFrame + FrequencyFrame)
      → Compose Canvas / AGSL Shader render
```

### Key Contracts
```kotlin
// Core data flowing through the pipeline
data class AudioFrame(
    val pcm: FloatArray,        // raw PCM samples, normalized -1f..1f
    val sampleRate: Int,
    val timestampMs: Long
)

data class FrequencyFrame(
    val magnitudes: FloatArray,  // FFT magnitude spectrum (linear or dB)
    val sampleRate: Int,
    val fftSize: Int
)

// Every visualization implements this
interface SoundVisualization {
    val id: String
    val displayName: String
    val description: String       // one-liner shown in gallery
    val category: VizCategory     // WAVEFORM, FREQUENCY, PHYSICS, GENERATIVE, ARTISTIC
    fun onAudioFrame(audio: AudioFrame, frequency: FrequencyFrame)
    @Composable
    fun Content(modifier: Modifier)  // self-contained composable
}

enum class VizCategory {
    WAVEFORM, FREQUENCY, PHYSICS, GENERATIVE, ARTISTIC
}
```

### Rules
- Audio capture and FFT run on `Dispatchers.Default` coroutine, NEVER on Main
- Visualizations receive data via `StateFlow` collected on Main — they must not block
- Buffer size: 1024 or 2048 samples per frame (configurable); FFT size matches
- All audio processing must be testable without Android framework (pure Kotlin modules)
- Permissions: use `rememberLauncherForActivityResult` + `ActivityResultContracts.RequestPermission` for `RECORD_AUDIO`. Do NOT use Accompanist permissions (deprecated).

---

## Visualizations (Minimum 12)

Each visualization must be a standalone implementation of `SoundVisualization`. Group by category:

### Waveform / Time-Domain
1. **Classic Waveform** — scrolling oscilloscope-style PCM plot
2. **Circular Waveform** — PCM mapped around a circle, radius modulated by amplitude

### Frequency-Domain
3. **Spectrogram** — scrolling heat-map (time × frequency × magnitude), color-mapped
4. **Frequency Bars** — classic equalizer bars (logarithmic frequency bands)
5. **Radial Spectrum** — frequency bars arranged radially, mirrored

### Physics-Inspired
6. **Chladni Patterns** — simulate nodal patterns on a vibrating plate driven by dominant frequency (AGSL shader with Canvas fallback)
7. **Cymatics Rings** — concentric standing-wave rings responding to frequency peaks
8. **Wave Interference** — multiple point-source ripples whose frequencies are driven by audio bands

### Generative / Algorithmic
9. **Voronoi Cells** — seed points move/multiply based on amplitude & spectral centroid; cells colored by frequency band energy
10. **Particle Flow Field** — Perlin-noise flow field where noise parameters (scale, speed, turbulence) are modulated by audio features
11. **Fractal Tree** — recursive branching where branch angle, length, and depth respond to different frequency bands

### Artistic
12. **Kaleidoscope** — mirror-symmetry shader that distorts a procedural texture by audio amplitude
13. **Terrain Mesh** — 3D height-map terrain where elevation = frequency magnitude (OpenGL ES path)
14. **Lissajous Curves** — parametric curves whose frequency ratio shifts with audio spectral peaks

> When adding a new visualization: create a new file in `feature/visualizations/`, implement `SoundVisualization`, and register it in the `VisualizationRegistry`. That's it — the gallery screen auto-discovers registered visualizations.

---

## Project Structure

```
app/
├── build.gradle.kts
├── src/main/kotlin/com/synesthesia/
│   ├── App.kt                          # @HiltAndroidApp Application
│   ├── MainActivity.kt                 # Single Activity, setContent → NavHost
│   │
│   ├── core/
│   │   ├── audio/
│   │   │   ├── AudioSource.kt          # Interface: mic vs file
│   │   │   ├── MicAudioSource.kt       # Uses AudioRecord
│   │   │   ├── FileAudioSource.kt      # Uses Media3 ExoPlayer for decoding
│   │   │   └── AudioFrame.kt
│   │   ├── dsp/
│   │   │   ├── FFTProcessor.kt         # Pure Kotlin Cooley-Tukey radix-2 FFT
│   │   │   ├── FrequencyFrame.kt
│   │   │   ├── WindowFunction.kt       # Hann, Hamming, Blackman
│   │   │   └── FeatureExtractors.kt    # RMS, spectral centroid, onset, beat
│   │   ├── pipeline/
│   │   │   └── AudioPipeline.kt        # Wires source → DSP → StateFlows
│   │   └── di/
│   │       └── AudioModule.kt          # Hilt @Module
│   │
│   ├── feature/
│   │   ├── home/                        # Gallery / launcher screen
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   ├── player/                      # Full-screen viz + controls overlay
│   │   │   ├── PlayerScreen.kt
│   │   │   └── PlayerViewModel.kt
│   │   └── visualizations/
│   │       ├── VisualizationRegistry.kt # Hilt @IntoSet provided set of all viz
│   │       ├── waveform/
│   │       │   ├── ClassicWaveformViz.kt
│   │       │   └── CircularWaveformViz.kt
│   │       ├── frequency/
│   │       │   ├── SpectrogramViz.kt
│   │       │   ├── FrequencyBarsViz.kt
│   │       │   └── RadialSpectrumViz.kt
│   │       ├── physics/
│   │       │   ├── ChladniPatternViz.kt
│   │       │   ├── CymaticsRingsViz.kt
│   │       │   └── WaveInterferenceViz.kt
│   │       ├── generative/
│   │       │   ├── VoronoiCellsViz.kt
│   │       │   ├── ParticleFlowFieldViz.kt
│   │       │   └── FractalTreeViz.kt
│   │       └── artistic/
│   │           ├── KaleidoscopeViz.kt
│   │           ├── TerrainMeshViz.kt
│   │           └── LissajousCurvesViz.kt
│   │
│   └── ui/
│       ├── theme/
│       │   ├── Theme.kt
│       │   ├── Color.kt
│       │   ├── Type.kt
│       │   └── StitchTokens.kt         # Mapped from Stitch export when available
│       ├── components/                   # Shared composables (audio controls, source picker)
│       └── navigation/
│           └── NavGraph.kt
│
├── src/main/assets/shaders/             # .agsl shader files
│   ├── chladni.agsl
│   ├── voronoi.agsl
│   ├── kaleidoscope.agsl
│   └── ...
│
├── src/test/                            # Unit tests (DSP, pipeline, feature extractors)
└── src/androidTest/                     # Instrumented tests (audio capture, rendering)

# Root level
├── gradle/
│   └── libs.versions.toml              # Single source of truth for all dependency versions
├── build.gradle.kts                     # Root build file
├── settings.gradle.kts
└── CLAUDE.md                            # This file
```

---

## Coding Standards

### General
- Pure Kotlin, no `!!` (use `requireNotNull` with message if truly impossible-null)
- Prefer `sealed interface` for closed type hierarchies
- All public API has KDoc; internal implementation can skip it
- Max function length ~40 lines; extract composables aggressively

### Compose
- State hoisting: ViewModel owns state, Screen observes, Viz receives via parameter
- Use `remember` / `derivedStateOf` to avoid recomposition storms
- Canvas drawing: pre-allocate `Paint`, `Path` objects outside `DrawScope` lambdas
- AGSL shaders: load once, update uniforms per frame
- Use the Kotlin Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`), NOT the old `composeOptions { kotlinCompilerExtensionVersion }` block — that pattern is deprecated

### Performance (Non-Negotiable)
- **Target: 60 fps** on mid-range devices (Pixel 6a class) for ALL visualizations
- Profile with `FrameTimingMetric` before marking a visualization "done"
- Allocate zero objects in the render hot path — reuse buffers
- FFT and feature extraction must complete within 5ms per frame on target hardware

### Testing
- Unit test every DSP function with known input/output pairs (sine waves, silence, white noise)
- Snapshot-test at least 3 visualizations with deterministic audio input
- Integration test: mic permission grant → audio flowing → visualization rendering

---

## Git & Workflow
- Branch naming: `feat/viz-chladni`, `fix/fft-buffer-overflow`, `chore/update-deps`
- Commit messages: conventional commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`)
- One visualization per PR when adding new viz
- `main` branch must always build and pass tests

---

## Key Decisions Log
| Decision | Rationale |
|---|---|
| Pure Kotlin FFT over TarsosDSP | TarsosDSP is Java-only, GPL-licensed FFT internals, not on Maven Central. A Cooley-Tukey radix-2 is ~80 lines of Kotlin and zero dependencies |
| KSP over KAPT | KAPT is deprecated; KSP is 2x faster and the recommended path for Hilt |
| Version catalogs (`libs.versions.toml`) | Single source of truth, better IDE support, Gradle best practice |
| Compose Canvas over custom View | Declarative, integrates with Compose state, good enough for 2D at 60fps |
| AGSL with Canvas fallback | AGSL requires API 33+; min SDK is 29, so fallback is mandatory |
| Media3 ExoPlayer for file audio | Modern, maintained, handles all codecs and formats correctly |
| No Accompanist | Most Accompanist libs are deprecated; use platform APIs directly |
| StateFlow over callback | Backpressure-friendly, lifecycle-aware, testable |
| Dark-mode first | Visualizations pop on dark backgrounds; less eye strain |
| Kotlin Compose compiler plugin | Replaces the old `composeOptions` block, which is deprecated as of Kotlin 2.0+ |

---

## Deprecated Patterns to AVOID
- ❌ `kapt()` — use `ksp()` instead
- ❌ `composeOptions { kotlinCompilerExtensionVersion = "..." }` — use the `kotlin-compose` plugin
- ❌ Accompanist Permissions — use `rememberLauncherForActivityResult`
- ❌ Accompanist SystemUiController — use `enableEdgeToEdge()` and `WindowInsetsController`
- ❌ `LocalLifecycleOwner.current` in old patterns — use `lifecycle-runtime-compose` extensions
- ❌ XML layouts, fragments, or the old View system
- ❌ Hardcoded dependency versions in `build.gradle.kts` — use `libs.versions.toml`
- ❌ `Thread.sleep` for timing — use coroutine delays or `Choreographer`

---

## Commands Reference
```bash
# Build
./gradlew assembleDebug

# Run tests
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug

# Install on connected device
./gradlew installDebug

# Run specific test class
./gradlew testDebugUnitTest --tests "com.synesthesia.core.dsp.FFTProcessorTest"

# Dependency updates check (if using versions plugin)
./gradlew dependencyUpdates
```

---

## What NOT to Do
- ❌ Don't use any deprecated library (check migration guides before adding deps)
- ❌ Don't process audio on the main thread
- ❌ Don't allocate in draw loops (no `FloatArray()` inside `onDraw`/`DrawScope`)
- ❌ Don't hardcode colors — always use theme tokens
- ❌ Don't add a visualization without registering it in `VisualizationRegistry`
- ❌ Don't skip the Hann window before FFT (spectral leakage will ruin frequency viz)
- ❌ Don't ignore audio focus — respect other apps' audio sessions
- ❌ Don't use `RuntimeShader` without checking `SDK_INT >= 33`