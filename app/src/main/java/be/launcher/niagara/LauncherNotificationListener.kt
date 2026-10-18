package be.launcher.niagara

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification

/**
 * Leest meldingen en actieve mediasessies uit voor de indicatoren en de mediabediening.
 * De gebruiker moet hiervoor meldingstoegang geven in de systeeminstellingen.
 */
class LauncherNotificationListener : NotificationListenerService() {

    private val main = Handler(Looper.getMainLooper())
    private var sessionManager: MediaSessionManager? = null
    private var controllers: List<MediaController> = emptyList()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishMedia()
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishMedia()
        override fun onSessionDestroyed() = refreshSessions()
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> setControllers(list.orEmpty()) }

    private val component get() = ComponentName(this, LauncherNotificationListener::class.java)

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        try {
            sessionManager?.addOnActiveSessionsChangedListener(sessionsListener, component, main)
        } catch (_: SecurityException) {
            // Toegang nog niet volledig verleend
        }
        refreshSessions()
        publishCounts()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        release()
        main.post { NotificationHub.clear() }
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publishCounts()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publishCounts()

    private fun release() {
        runCatching { sessionManager?.removeOnActiveSessionsChangedListener(sessionsListener) }
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = emptyList()
    }

    private fun refreshSessions() {
        val list = try {
            sessionManager?.getActiveSessions(component).orEmpty()
        } catch (_: SecurityException) {
            emptyList()
        }
        setControllers(list)
    }

    private fun setControllers(list: List<MediaController>) {
        controllers.forEach { it.unregisterCallback(controllerCallback) }
        controllers = list
        list.forEach { it.registerCallback(controllerCallback, main) }
        publishMedia()
    }

    private fun publishMedia() {
        val candidates = controllers.filter { it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) != null }
        val pick = candidates.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: candidates.firstOrNull()
        val info = pick?.let {
            val meta = it.metadata
            MediaInfo(
                title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                playing = it.playbackState?.state == PlaybackState.STATE_PLAYING,
                controller = it,
            )
        }
        main.post { NotificationHub.media = info }
    }

    private fun publishCounts() {
        val active = try {
            activeNotifications.orEmpty().toList()
        } catch (_: Exception) {
            emptyList()
        }
        val counts = active
            .filter { !it.isOngoing && (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .groupingBy { it.packageName }
            .eachCount()
        main.post { NotificationHub.counts = counts }
    }
}
