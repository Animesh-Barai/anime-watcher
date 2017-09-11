package brunodles.animewatcher.explorer

import brunodles.kotlin.annotation.NoArgs
import java.io.Serializable

@NoArgs
data class CurrentEpisode(
        val video: String, val description: String, val link: String = "") : Serializable {
    companion object {
        private const val serialVersionUid: Long = 1L
    }
}