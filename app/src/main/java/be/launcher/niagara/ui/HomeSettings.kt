package be.launcher.niagara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.LauncherSettings
import be.launcher.niagara.WidgetController

/** Menu dat opent als je lang indrukt op het startscherm. */
@Composable
fun HomeSettingsDialog(
    settings: LauncherSettings,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    onAddWidget: () -> Unit,
    onEditFavorites: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1C),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text("Launcher", style = lightText(24.sp)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SwitchRow("Batterijpercentage tonen", settings.showBattery, settings::updateShowBattery)
                SwitchRow("Muziek tonen als er iets speelt", settings.showMedia, settings::updateShowMedia)
                SwitchRow("App-iconen tonen", settings.showIcons, settings::updateShowIcons)

                ActionRow("Favorieten bewerken") {
                    onDismiss()
                    onEditFavorites()
                }
                ActionRow("Widget toevoegen") {
                    onDismiss()
                    onAddWidget()
                }
                widgets.widgetIds.toList().forEach { id ->
                    ActionRow("Verwijder: ${widgets.label(id)}") { widgets.remove(id) }
                }
                if (!listenerEnabled) {
                    ActionRow("Meldingstoegang geven") {
                        onDismiss()
                        onRequestNotificationAccess()
                    }
                }
                if (!isDefaultLauncher) {
                    ActionRow("Standaard-launcher instellen") {
                        onDismiss()
                        onRequestDefaultLauncher()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Sluiten", color = Color.White) }
        },
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = lightText(16.sp), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = lightText(16.sp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
