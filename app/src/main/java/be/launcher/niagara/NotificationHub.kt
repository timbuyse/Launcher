package be.launcher.niagara

import android.media.session.MediaController
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class MediaInfo(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val controller: MediaController,
)

/** Gedeelde staat die [LauncherNotificationListener] vult en de UI leest. */
object NotificationHub {
    /** Aantal (niet-lopende) meldingen per package. */
    var counts by mutableStateOf<Map<String, Int>>(emptyMap())
    var media by mutableStateOf<MediaInfo?>(null)

    /** Laatste melding per package als korte tekst (afzender en bericht). */
    var previews by mutableStateOf<Map<String, String>>(emptyMap())

    fun clear() {
        counts = emptyMap()
        previews = emptyMap()
        media = null
    }
}
