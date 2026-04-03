# SYNESTHESIA

**Transform sound into a gallery of real-time visual experiences.**

Synesthesia is an Android app that converts audio input — from your microphone or a file — into 14 distinct, real-time visualizations. From classical spectrograms to Chladni nodal patterns to generative particle fields, each visualization is a different lens on the same signal.

---

## Screenshots

> Coming soon — screenshots will be added once the first set of visualizations is complete.

---

## Features

- **14 unique visualizations** spanning waveform, frequency, physics, generative, and artistic categories
- **Live microphone input** via Android `AudioRecord`
- **Audio file playback** via Media3 ExoPlayer with PCM extraction
- **Pure Kotlin DSP pipeline** — Cooley-Tukey FFT, Hann windowing, RMS, spectral centroid, onset detection
- **GPU-accelerated rendering** via AGSL shaders on API 33+ with Compose Canvas fallback on API 29–32
- **Exploration Hub** — browse and launch any visualization from the gallery carousel
- **Light theme** with a soft teal + warm grey Material 3 palette

### Visualizations

| Category | Visualization |
|---|---|
| Waveform | Classic Waveform, Circular Waveform |
| Frequency | Spectrogram, Frequency Bars, Radial Spectrum |
| Physics | Chladni Patterns, Cymatics Rings, Wave Interference |
| Generative | Voronoi Cells, Particle Flow Field, Fractal Tree |
| Artistic | Kaleidoscope, Terrain Mesh (OpenGL ES), Lissajous Curves |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3.20 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.57.1 (KSP, not KAPT) |
| Navigation | Navigation Compose 2.9.0 (type-safe routes) |
| Audio capture | Android `AudioRecord` (PCM Float) |
| Audio files | Media3 ExoPlayer 1.6.0 |
| DSP | Pure Kotlin Cooley-Tukey radix-2 FFT |
| GPU rendering | AGSL `RuntimeShader` (API 33+) |
| 3D rendering | OpenGL ES 3.0 via `GLSurfaceView` |
| Concurrency | Kotlin Coroutines + `StateFlow` |

---

## Architecture

```
Mic / File
  → AudioSource (interface)
    → AudioFrame (PCM FloatArray, 2048 samples)
      → DSP Bus:
          1. Hann Window
          2. FFT (Cooley-Tukey radix-2)
          3. Feature Extraction (RMS, spectral centroid, dominant freq)
        → FrequencyFrame (magnitude spectrum)
          → VisualizationEngine
            → Compose Canvas / AGSL Shader / OpenGL ES
```

Clean Architecture layers: `core/` (audio, DSP, pipeline) → `feature/` (UI, ViewModels) → no reverse dependencies.

```
app/src/main/kotlin/dev/tiank003/synesthesia/
├── core/
│   ├── audio/          AudioSource, MicAudioSource, FileAudioSource, AudioFrame
│   ├── dsp/            FFTProcessor, WindowFunction, FeatureExtractors, FrequencyFrame
│   ├── pipeline/       AudioPipeline
│   └── di/             AudioModule (Hilt)
├── feature/
│   ├── explore/        ExploreScreen, ExploreViewModel (gallery hub)
│   ├── lab/            LabScreen, LabViewModel (full-screen player)
│   ├── learn/          LearnScreen (placeholder)
│   ├── input/          InputScreen (audio source selection)
│   └── visualizations/ SoundVisualization interface, VisualizationRegistry, 14 implementations
└── ui/
    ├── theme/          Color, Type, Theme, StitchTokens
    ├── components/     Shared composables
    └── navigation/     NavGraph
```

---

## Building

**Requirements:**
- Android Studio Meerkat (2024.3.1) or later
- JDK 11+
- Android device or emulator running API 29 (Android 10) or higher
- AGSL shader visualizations require API 33+ (Android 13)

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests (DSP layer, no device needed)
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

---

## Design Reference

The UI design mockup is available at [`docs/design_reference.html`](docs/design_reference.html) — open it in any browser to preview the intended look and feel.

**Design tokens:**
- Primary: `#3f675a` (soft teal)
- Surface: `#f9f9f7` (warm grey)
- Headline font: Space Grotesk
- Body font: Inter

---

## Permissions

| Permission | Required for |
|---|---|
| `RECORD_AUDIO` | Live microphone visualization |
| `READ_MEDIA_AUDIO` | Audio file picker (API 33+) |
| `READ_EXTERNAL_STORAGE` | Audio file picker (API 29–32) |

---

## Contributing

Branch naming: `feat/viz-chladni`, `fix/fft-buffer`, `chore/update-deps`

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `refactor:`, `test:`, `docs:`

One visualization per PR when adding new viz types.

---

## License

MIT License — see `LICENSE` for details.
