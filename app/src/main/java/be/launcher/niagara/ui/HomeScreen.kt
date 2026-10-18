package be.launcher.niagara.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val SWIPE_UP_THRESHOLD_PX = 120f

@Composable
fun HomeScreen(
    favorites: List<AppEntry>,
    onLaunch: (AppEntry) -> Unit,
    onOpenDrawer: () -> Unit,
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

        Spacer(Modifier.weight(1f))

        if (favorites.isEmpty()) {
            Text(
                text = "Veeg omhoog en houd een app ingedrukt om hem hier te zetten.",
                color = Color(0xFF757575),
                fontSize = 16.sp,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                favorites.forEach { app ->
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunch(app) }
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Alle apps",
            color = Color(0xFF9E9E9E),
            fontSize = 16.sp,
            modifier = Modifier
                .clickable(onClick = onOpenDrawer)
                .padding(vertical = 8.dp),
        )
    }
}
