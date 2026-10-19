package be.launcher.niagara

import android.content.Context

/** Bewaart de favorieten (package-namen, in volgorde) in SharedPreferences. */
class FavoritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)

    fun load(): List<String> =
        prefs.getString(KEY, "")!!.split(SEPARATOR).filter { it.isNotBlank() }

    fun save(packages: List<String>) {
        prefs.edit().putString(KEY, packages.joinToString(SEPARATOR)).apply()
    }

    private companion object {
        const val KEY = "packages"
        const val SEPARATOR = "\n"
    }
}
