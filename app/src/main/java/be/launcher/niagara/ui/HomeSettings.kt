package be.launcher.niagara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.LauncherSettings
import be.launcher.niagara.WidgetController

internal val CardBackground = Color(0xFF0A3240)
internal val SheetBackgroundDeep = Color(0xFF04161F)
internal val SheetTextColor = Color(0xFFCDEBFF)

/** Instellingenmenu dat van onderaf opent als je lang indrukt op het startscherm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSettingsSheet(
    settings: LauncherSettings,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    onOpenClockStyle: () -> Unit,
    onAddWidget: () -> Unit,
    onEditFavorites: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBackgroundDeep,
        contentColor = SheetTextColor,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 24.dp),
        ) {
            Text("Launcher", style = lightText(32.sp, SheetTextColor), modifier = Modifier.padding(bottom = 16.dp))

            ChevronCard({ ClockIcon() }, "Klok stijl") {
                onDismiss()
                onOpenClockStyle()
            }
            ChevronCard({ SheetIcon(Icons.Outlined.Add) }, "Widget toevoegen") {
                onDismiss()
                onAddWidget()
            }
            widgets.widgetIds.toList().forEach { id ->
                ChevronCard({ SheetIcon(Icons.Outlined.Delete) }, "Verwijder: ${widgets.label(id)}", chevron = false) {
                    widgets.remove(id)
                }
            }
            SwitchCard(
                { SheetIcon(Icons.Outlined.PlayArrow) },
                "Muziek tonen",
                "Tijdens het afspelen van muziek",
                settings.showMedia,
                settings::updateShowMedia,
            )
            SwitchCard(
                { BatteryIcon() },
                "Batterijpercentage tonen",
                null,
                settings.showBattery,
                settings::updateShowBattery,
            )
            SwitchCard(
                { SheetIcon(Icons.Outlined.Info) },
                "App-iconen tonen",
                null,
                settings.showIcons,
                settings::updateShowIcons,
            )

            Spacer(Modifier.height(20.dp))

            ChevronCard({ SheetIcon(Icons.Outlined.Star) }, "Favorieten bewerken") {
                onDismiss()
                onEditFavorites()
            }
            ChevronCard(
                { SheetIcon(Icons.Outlined.Notifications) },
                "Meldingen op het startscherm",
                if (listenerEnabled) "Ingeschakeld" else "Niet ingeschakeld",
            ) {
                onDismiss()
                onRequestNotificationAccess()
            }
            if (!isDefaultLauncher) {
                ChevronCard({ SheetIcon(Icons.Outlined.Home) }, "Standaard-launcher instellen") {
                    onDismiss()
                    onRequestDefaultLauncher()
                }
            }
        }
    }
}

@Composable
private fun CardRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = lightText(20.sp, SheetTextColor))
            if (subtitle != null) {
                Text(subtitle, style = lightText(15.sp, Color(0xFF9FB6C2)), modifier = Modifier.padding(top = 4.dp))
            }
        }
        trailing()
    }
}

@Composable
private fun ChevronCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    CardRow(icon, title, subtitle, onClick) {
        if (chevron) SheetIcon(Icons.Outlined.KeyboardArrowRight)
    }
}

@Composable
private fun SwitchCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    CardRow(icon, title, subtitle, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05303F),
                checkedTrackColor = Color(0xFF8FD3F4),
                uncheckedThumbColor = Color(0xFF05303F),
                uncheckedTrackColor = Color(0xFF9FB6C2),
                uncheckedBorderColor = Color(0xFF9FB6C2),
            ),
        )
    }
}

@Composable
private fun SheetIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, tint = SheetTextColor)
}

@Composable
private fun ClockIcon() {
    Canvas(Modifier.size(26.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(SheetTextColor, radius = size.minDimension / 2 - 1.dp.toPx(), style = stroke)
        val c = Offset(size.width / 2, size.height / 2)
        drawLine(SheetTextColor, c, Offset(c.x, c.y - size.height * 0.28f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(SheetTextColor, c, Offset(c.x + size.width * 0.2f, c.y + size.height * 0.1f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun BatteryIcon() {
    Canvas(Modifier.size(26.dp)) {
        val stroke = Stroke(width = 2.dp.toPx())
        val bodyW = size.width * 0.88f
        val bodyH = size.height * 0.5f
        val top = (size.height - bodyH) / 2
        drawRoundRect(SheetTextColor, Offset(1.dp.toPx(), top), Size(bodyW, bodyH), CornerRadius(4.dp.toPx()), style = stroke)
        drawRect(SheetTextColor, Offset(bodyW + 1.dp.toPx(), top + bodyH * 0.3f), Size(2.dp.toPx(), bodyH * 0.4f))
    }
}
