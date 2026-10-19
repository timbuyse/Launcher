package be.launcher.niagara

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Telt hoe vaak je elke app vanuit de launcher start. Blijft op het toestel, geen extra rechten nodig. */
class UsageStore(context: Context) {
    private val prefs = context.getSharedPreferences("usage", Context.MODE_PRIVATE)

    var counts by mutableStateOf(load())
        private set

    fun record(key: String) {
        val next = (counts[key] ?: 0) + 1
        counts = counts + (key to next)
        prefs.edit().putInt(key, next).apply()
    }

    private fun load(): Map<String, Int> =
        prefs.all.mapNotNull { (key, value) -> (value as? Int)?.let { key to it } }.toMap()
}
