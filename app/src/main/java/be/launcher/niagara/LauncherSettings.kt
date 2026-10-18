package be.launcher.niagara

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Instellingen die je via lang indrukken op het startscherm aanpast. */
class LauncherSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var showBattery by mutableStateOf(prefs.getBoolean(KEY_BATTERY, true))
        private set
    var showMedia by mutableStateOf(prefs.getBoolean(KEY_MEDIA, true))
        private set
    var showIcons by mutableStateOf(prefs.getBoolean(KEY_ICONS, true))
        private set

    var clockStyle by mutableStateOf(prefs.getInt(KEY_CLOCK, 0))
        private set

    fun updateClockStyle(value: Int) {
        clockStyle = value
        prefs.edit().putInt(KEY_CLOCK, value).apply()
    }

    fun updateShowBattery(value: Boolean) {
        showBattery = value
        prefs.edit().putBoolean(KEY_BATTERY, value).apply()
    }

    fun updateShowMedia(value: Boolean) {
        showMedia = value
        prefs.edit().putBoolean(KEY_MEDIA, value).apply()
    }

    fun updateShowIcons(value: Boolean) {
        showIcons = value
        prefs.edit().putBoolean(KEY_ICONS, value).apply()
    }

    private companion object {
        const val KEY_BATTERY = "battery"
        const val KEY_MEDIA = "media"
        const val KEY_ICONS = "icons"
        const val KEY_CLOCK = "clock_style"
    }
}
