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

    var hiddenApps by mutableStateOf(prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet())
        private set
    var sortByUsage by mutableStateOf(prefs.getBoolean(KEY_SORT_USAGE, true))
        private set
    var showFrequent by mutableStateOf(prefs.getBoolean(KEY_FREQUENT, true))
        private set
    var haptics by mutableStateOf(prefs.getBoolean(KEY_HAPTICS, true))
        private set
    var allowRotation by mutableStateOf(prefs.getBoolean(KEY_ROTATION, false))
        private set
    var quickLock by mutableStateOf(prefs.getBoolean(KEY_QUICK_LOCK, false))
        private set
    var iconShape by mutableStateOf(prefs.getInt(KEY_ICON_SHAPE, 0))
        private set
    var grayIcons by mutableStateOf(prefs.getBoolean(KEY_GRAY, false))
        private set
    var dim by mutableStateOf(prefs.getFloat(KEY_DIM, 0.15f))
        private set
    var background by mutableStateOf(prefs.getInt(KEY_BACKGROUND, 0))
        private set
    var backgroundVersion by mutableStateOf(0)
        private set

    fun updateBackground(value: Int) {
        background = value
        prefs.edit().putInt(KEY_BACKGROUND, value).apply()
    }

    /** Laat de eigen foto opnieuw inlezen na het kiezen van een nieuwe. */
    fun bumpBackgroundVersion() {
        backgroundVersion += 1
    }

    fun toggleHidden(key: String) {
        hiddenApps = if (key in hiddenApps) hiddenApps - key else hiddenApps + key
        prefs.edit().putStringSet(KEY_HIDDEN, hiddenApps).apply()
    }

    fun updateSortByUsage(value: Boolean) {
        sortByUsage = value
        prefs.edit().putBoolean(KEY_SORT_USAGE, value).apply()
    }

    fun updateShowFrequent(value: Boolean) {
        showFrequent = value
        prefs.edit().putBoolean(KEY_FREQUENT, value).apply()
    }

    fun updateHaptics(value: Boolean) {
        haptics = value
        prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
    }

    fun updateAllowRotation(value: Boolean) {
        allowRotation = value
        prefs.edit().putBoolean(KEY_ROTATION, value).apply()
    }

    fun updateQuickLock(value: Boolean) {
        quickLock = value
        prefs.edit().putBoolean(KEY_QUICK_LOCK, value).apply()
    }

    fun updateIconShape(value: Int) {
        iconShape = value
        prefs.edit().putInt(KEY_ICON_SHAPE, value).apply()
    }

    fun updateGrayIcons(value: Boolean) {
        grayIcons = value
        prefs.edit().putBoolean(KEY_GRAY, value).apply()
    }

    fun updateDim(value: Float) {
        dim = value
        prefs.edit().putFloat(KEY_DIM, value).apply()
    }

    fun applyTheme(theme: LauncherTheme) {
        updateDim(theme.dim)
        updateClockStyle(theme.clockStyle)
        updateIconShape(theme.iconShape)
        updateGrayIcons(theme.grayIcons)
        updateBackground(theme.background)
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
        const val KEY_HIDDEN = "hidden_apps"
        const val KEY_SORT_USAGE = "sort_usage"
        const val KEY_FREQUENT = "frequent"
        const val KEY_HAPTICS = "haptics"
        const val KEY_ROTATION = "rotation"
        const val KEY_QUICK_LOCK = "quick_lock"
        const val KEY_ICON_SHAPE = "icon_shape"
        const val KEY_GRAY = "gray_icons"
        const val KEY_DIM = "dim"
        const val KEY_BACKGROUND = "background"
    }
}
