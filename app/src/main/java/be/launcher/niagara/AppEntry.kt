package be.launcher.niagara

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.util.Locale

data class AppEntry(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val component: ComponentName get() = ComponentName(packageName, activityName)
    val key: String get() = "$packageName/$activityName"

    /** Eerste letter in hoofdletters, of '#' voor alles wat geen letter is. */
    val letter: Char
        get() = label.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'
}

object AppRepository {
    fun load(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                )
            }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    fun launch(context: Context, app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = app.component
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // App is net verwijderd of niet meer startbaar
        }
    }
}
