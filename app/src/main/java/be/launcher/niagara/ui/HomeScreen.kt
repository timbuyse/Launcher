package be.launcher.niagara.ui

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import be.launcher.niagara.NotificationHub
import be.launcher.niagara.WidgetController
import be.launcher.niagara.WidgetPickerDialog
import be.launcher.niagara.WidgetView
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SWIPE_UP_THRESHOLD_PX = 120f
private val SubtleText = Color(0xFFC7C7C7)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    favorites: List<AppEntry>,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
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
    val time = now.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val date = now.format(DateTimeFormatter.ofPattern("EEEE d MMMM", locale))

    var menuOpen by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }
    val counts = NotificationHub.counts
    val media = NotificationHub.media
    val totalNotifications = counts.values.sum()

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
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box {
                Column(
                    Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                    ),
                ) {
                    Text(text = time, style = lightText(68.sp))
                    Text(
                        text = date.replaceFirstChar { it.uppercase(locale) },
                        style = lightText(16.sp, SubtleText),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(if (widgets.widgetId == -1) "Widget toevoegen" else "Widget vervangen") },
                        onClick = {
                            menuOpen = false
                            pickerOpen = true
                        },
                    )
                    if (widgets.widgetId != -1) {
                        DropdownMenuItem(
                            text = { Text("Widget verwijderen") },
                            onClick = {
                                menuOpen = false
                                widgets.remove()
                            },
                        )
                    }
                    if (!listenerEnabled) {
                        DropdownMenuItem(
                            text = { Text("Meldingstoegang geven") },
                            onClick = {
                                menuOpen = false
                                onRequestNotificationAccess()
                            },
                        )
                    }
                    if (!isDefaultLauncher) {
                        DropdownMenuItem(
                            text = { Text("Standaard-launcher instellen") },
                            onClick = {
                                menuOpen = false
                                onRequestDefaultLauncher()
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Klein infoblokje rechtsboven: batterij en aantal meldingen
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 10.dp)) {
                battery?.let { Text("$it%", style = lightText(16.sp, SubtleText)) }
                if (listenerEnabled && totalNotifications > 0) {
                    Text(
                        text = if (totalNotifications == 1) "1 melding" else "$totalNotifications meldingen",
                        style = lightText(14.sp, SubtleText),
                    )
                }
            }
        }

        WidgetView(widgets, Modifier.padding(top = 16.dp))

        Spacer(Modifier.weight(1f))

        if (media != null) {
            MediaCard(media, Modifier.padding(bottom = 20.dp))
        }

        if (favorites.isEmpty()) {
            Text(
                text = "Veeg omhoog en houd een app ingedrukt om hem hier te zetten.",
                style = lightText(16.sp, SubtleText),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                favorites.forEach { app ->
                    Row(
                        modifier = Modifier
                            .clickable { onLaunch(app) }
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val count = counts[app.packageName] ?: 0
                        if (count > 0) {
                            NotificationBadge(count)
                            Spacer(Modifier.width(14.dp))
                        }
                        Text(
                            text = app.label,
                            style = lightText(38.sp),
                            maxLines = 1,
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (!listenerEnabled) {
            Hint("Meldingen en muziek tonen: tik om toegang te geven", onRequestNotificationAccess)
        }
        if (!isDefaultLauncher) {
            Hint("Tik om Launcher je startscherm te maken", onRequestDefaultLauncher)
        }
        Text(
            text = "Alle apps",
            style = lightText(16.sp, SubtleText),
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenDrawer)
                .padding(vertical = 8.dp),
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
        style = lightText(13.sp, Color(0xFFA0A0A0)),
        textAlign = TextAlign.End,
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
