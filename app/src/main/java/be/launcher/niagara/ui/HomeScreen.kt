package be.launcher.niagara.ui

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    favorites: List<AppEntry>,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onRequestDefaultLauncher: () -> Unit,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total < -SWIPE_UP_THRESHOLD_PX) onOpenDrawer() },
                    onDragCancel = { total = 0f },
                ) { _, dy -> total += dy }
            }
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Box {
            Column(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { menuOpen = true }),
            ) {
                Text(
                    text = time,
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                )
                Text(
                    text = date.replaceFirstChar { it.uppercase(locale) },
                    color = Color(0xFF9E9E9E),
                    fontSize = 16.sp,
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

        WidgetView(widgets, Modifier.padding(top = 16.dp))

        Spacer(Modifier.weight(1f))

        if (media != null) {
            MediaCard(media, Modifier.padding(bottom = 16.dp))
        }

        if (favorites.isEmpty()) {
            Text(
                text = "Veeg omhoog en houd een app ingedrukt om hem hier te zetten.",
                color = Color(0xFF757575),
                fontSize = 16.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                favorites.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(app) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val count = counts[app.packageName] ?: 0
                        if (count > 0) {
                            Spacer(Modifier.width(12.dp))
                            NotificationBadge(count)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        if (!listenerEnabled) {
            Hint("Meldingen en muziek tonen: tik om toegang te geven", onRequestNotificationAccess)
        }
        if (!isDefaultLauncher) {
            Hint("Tik om Launcher je startscherm te maken", onRequestDefaultLauncher)
        }
        Text(
            text = "Alle apps",
            color = Color(0xFF9E9E9E),
            fontSize = 16.sp,
            modifier = Modifier
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
        color = Color(0xFF757575),
        fontSize = 13.sp,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    )
}
