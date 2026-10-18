package be.launcher.niagara.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import be.launcher.niagara.LauncherSettings
import be.launcher.niagara.NotificationHub
import be.launcher.niagara.WidgetController
import be.launcher.niagara.WidgetPickerDialog
import be.launcher.niagara.WidgetView
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SWIPE_UP_THRESHOLD_PX = 120f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    favorites: List<AppEntry>,
    settings: LauncherSettings,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember { mutableStateOf(batteryPercent(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            battery = batteryPercent(context)
            delay(1_000)
        }
    }
    val locale = Locale.getDefault()
    val time = now.format(DateTimeFormatter.ofPattern("H:mm", locale))
    val date = now.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
        .replaceFirstChar { it.uppercase(locale) }
    val dateLine = if (settings.showBattery && battery != null) "$date   $battery%" else date

    var settingsOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    val counts = NotificationHub.counts
    val previews = NotificationHub.previews
    val media = NotificationHub.media

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (total < -SWIPE_UP_THRESHOLD_PX) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenDrawer()
                        }
                    },
                    onDragCancel = { total = 0f },
                ) { _, dy -> total += dy }
            }
            .pointerInput(Unit) {
                // Alleen op lege plekken: favorieten en widgets verwerken hun eigen aanrakingen
                detectTapGestures(onLongPress = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    settingsOpen = true
                })
            }
            .padding(start = 32.dp, end = 64.dp, top = 24.dp, bottom = 24.dp),
    ) {
        Text(text = time, style = lightText(64.sp, weight = FontWeight.Normal))
        Text(text = dateLine, style = lightText(16.sp, SubtleText, FontWeight.Normal))

        // Widgets krijgen de ruimte tussen klok en favorieten en scrollen als ze samen te hoog worden
        val hasWidgets = widgets.widgetIds.isNotEmpty()
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (hasWidgets) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            widgets.widgetIds.toList().forEach { id ->
                key(id) { WidgetView(widgets, id) }
            }
        }

        if (settings.showMedia && media != null) {
            MediaCard(media, Modifier.padding(bottom = 20.dp))
        }

        if (favorites.isEmpty()) {
            Text(
                text = "Veeg omhoog of trek aan de letters rechts en houd een app ingedrukt om hem hier te zetten.",
                style = lightText(16.sp, SubtleText, FontWeight.Normal),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                favorites.forEach { app ->
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onLaunch(app) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuFor = app
                                    },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (settings.showIcons) {
                                AppIcon(app, 44.dp)
                                Spacer(Modifier.width(20.dp))
                            }
                            Column {
                                Text(
                                    text = app.label,
                                    style = lightText(22.sp, weight = FontWeight.Normal),
                                    maxLines = 1,
                                )
                                val preview = previews[app.packageName]
                                if (preview != null) {
                                    Text(
                                        text = preview,
                                        style = lightText(13.sp, SubtleText, FontWeight.Normal),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        DropdownMenu(expanded = menuFor == app, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text("Van startscherm halen") },
                                onClick = {
                                    onToggleFavorite(app.packageName)
                                    menuFor = null
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("App-info") },
                                onClick = {
                                    openAppInfo(context, app)
                                    menuFor = null
                                },
                            )
                        }
                    }
                }
            }
        }

        if (!listenerEnabled) {
            Spacer(Modifier.height(12.dp))
            Hint("Meldingen en muziek tonen: tik om toegang te geven", onRequestNotificationAccess)
        }
        if (!isDefaultLauncher) {
            Hint("Tik om Launcher je startscherm te maken", onRequestDefaultLauncher)
        }
    }

    if (settingsOpen) {
        HomeSettingsDialog(
            settings = settings,
            widgets = widgets,
            listenerEnabled = listenerEnabled,
            isDefaultLauncher = isDefaultLauncher,
            onAddWidget = { pickerOpen = true },
            onRequestNotificationAccess = onRequestNotificationAccess,
            onRequestDefaultLauncher = onRequestDefaultLauncher,
            onDismiss = { settingsOpen = false },
        )
    }

    if (pickerOpen) {
        WidgetPickerDialog(
            providers = remember { widgets.providers() },
            onPick = {
                pickerOpen = false
                widgets.add(it)
            },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun Hint(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = lightText(13.sp, Color(0xFFA0A0A0), FontWeight.Normal),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    )
}

private fun batteryPercent(context: Context): Int? {
    val intent: Intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return level * 100 / scale
}
