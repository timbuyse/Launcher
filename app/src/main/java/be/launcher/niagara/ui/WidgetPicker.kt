package be.launcher.niagara.ui

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class WidgetGroup(
    val packageName: String,
    val label: String,
    val widgets: List<AppWidgetProviderInfo>,
)

/** Schermvullende widget-kiezer, gegroepeerd per app. */
@Composable
fun WidgetPicker(
    providers: List<AppWidgetProviderInfo>,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)

    val pm = LocalContext.current.packageManager
    val groups = remember(providers) {
        providers
            .groupBy { it.provider.packageName }
            .map { (pkg, list) ->
                WidgetGroup(pkg, appLabel(pm, pkg), list.sortedBy { it.loadLabel(pm).lowercase() })
            }
            .sortedBy { it.label.lowercase() }
    }
    var expanded by remember { mutableStateOf<String?>(null) }

    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 56.dp, bottom = 48.dp),
        ) {
            item {
                Text("Kies een widget", style = lightText(48.sp), modifier = Modifier.padding(bottom = 12.dp))
            }
            item {
                Text(
                    "Handige info binnen handbereik, zonder apps te openen.",
                    style = lightText(17.sp, SubtleText, FontWeight.Normal),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
            items(groups, key = { it.packageName }) { group ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (group.widgets.size == 1) onPick(group.widgets[0])
                                else expanded = if (expanded == group.packageName) null else group.packageName
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PackageIcon(group.packageName, 48.dp)
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(group.label, style = lightText(20.sp, weight = FontWeight.Normal), maxLines = 1)
                            Text(
                                if (group.widgets.size == 1) "1 widget" else "${group.widgets.size} widgets",
                                style = lightText(14.sp, SubtleText, FontWeight.Normal),
                            )
                        }
                    }
                    if (expanded == group.packageName) {
                        group.widgets.forEach { widget ->
                            Text(
                                text = widget.loadLabel(pm),
                                style = lightText(17.sp, weight = FontWeight.Normal),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(widget) }
                                    .padding(start = 68.dp, top = 12.dp, bottom = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun appLabel(pm: PackageManager, packageName: String): String =
    try {
        pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }
