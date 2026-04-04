package dev.tiank003.synesthesia.feature.visualizations

import dev.tiank003.synesthesia.core.audio.AudioHistoryBuffer

/**
 * Optional extension of [SoundVisualization] for vizzes that support
 * scrollable audio history and post-recording scrubbing.
 *
 * Vizzes implementing this interface get a scrub bar in the Lab screen
 * when audio stops and recorded history is available.
 */
interface HistoryAwareViz {
    val historyBuffer: AudioHistoryBuffer
}
