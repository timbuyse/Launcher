package be.launcher.niagara

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** Regelt het kiezen, binden en tonen van één widget op het startscherm. */
class WidgetController(private val activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("widget", Context.MODE_PRIVATE)
    val host = AppWidgetHost(activity, HOST_ID)
    val manager: AppWidgetManager = AppWidgetManager.getInstance(activity)

    /** Alle widgets op het startscherm, in volgorde van toevoegen. */
    val widgetIds = mutableStateListOf<Int>().apply { addAll(load()) }

    private var pendingId = NO_WIDGET
    private var pendingProvider: AppWidgetProviderInfo? = null

    init {
        // Widgets waarvan de app is verwijderd opruimen
        widgetIds.toList().filter { manager.getAppWidgetInfo(it) == null }.forEach { remove(it) }
    }

    private fun load(): List<Int> {
        val stored = prefs.getString(KEY_IDS, null)
        if (stored != null) return stored.split(",").mapNotNull { it.toIntOrNull() }
        // Eerste versie bewaarde maar één widget
        val old = prefs.getInt(KEY_ID, NO_WIDGET)
        return if (old != NO_WIDGET) listOf(old) else emptyList()
    }

    private fun save() {
        prefs.edit().putString(KEY_IDS, widgetIds.joinToString(",")).remove(KEY_ID).apply()
    }

    fun label(id: Int): String =
        manager.getAppWidgetInfo(id)?.loadLabel(activity.packageManager)?.toString() ?: "Widget"

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    fun providers(): List<AppWidgetProviderInfo> {
        val pm = activity.packageManager
        return manager.installedProviders.sortedBy { it.loadLabel(pm).lowercase() }
    }

    fun add(provider: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingId = id
        pendingProvider = provider
        if (manager.bindAppWidgetIdIfAllowed(id, provider.provider)) {
            afterBind()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
            @Suppress("DEPRECATION")
            activity.startActivityForResult(intent, REQ_BIND)
        }
    }

    fun remove(id: Int) {
        host.deleteAppWidgetId(id)
        widgetIds.remove(id)
        save()
    }

    /** Geeft true terug als het resultaat bij deze controller hoorde. */
    fun handleResult(requestCode: Int, resultCode: Int): Boolean {
        when (requestCode) {
            REQ_BIND -> if (resultCode == Activity.RESULT_OK) afterBind() else cancelPending()
            REQ_CONFIGURE -> if (resultCode == Activity.RESULT_OK) commit(pendingId) else cancelPending()
            else -> return false
        }
        return true
    }

    private fun afterBind() {
        val provider = pendingProvider ?: return
        if (provider.configure != null) {
            @Suppress("DEPRECATION")
            host.startAppWidgetConfigureActivityForResult(activity, pendingId, 0, REQ_CONFIGURE, null)
        } else {
            commit(pendingId)
        }
    }

    private fun commit(id: Int) {
        if (id !in widgetIds) widgetIds.add(id)
        save()
        pendingId = NO_WIDGET
        pendingProvider = null
    }

    private fun cancelPending() {
        if (pendingId != NO_WIDGET) host.deleteAppWidgetId(pendingId)
        pendingId = NO_WIDGET
        pendingProvider = null
    }

    companion object {
        const val REQ_BIND = 4001
        const val REQ_CONFIGURE = 4002
        private const val HOST_ID = 1024
        private const val KEY_ID = "id"
        private const val KEY_IDS = "ids"
        private const val NO_WIDGET = -1
    }
}

@Composable
fun WidgetView(controller: WidgetController, id: Int, modifier: Modifier = Modifier) {
    val info = remember(id) { controller.manager.getAppWidgetInfo(id) } ?: return
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = with(density) { info.minHeight.toDp() }.value.toInt().coerceIn(80, 260)
        AndroidView(
            factory = { ctx -> controller.host.createView(ctx, id, info) },
            update = { it.updateAppWidgetSize(Bundle(), widthDp, heightDp, widthDp, heightDp) },
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp.dp),
        )
    }
}
