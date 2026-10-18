package be.launcher.niagara.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.MediaInfo

private enum class MediaIcon { Previous, Play, Pause, Next }

@Composable
fun MediaCard(info: MediaInfo, modifier: Modifier = Modifier) {
    val controls = info.controller.transportControls
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                info.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
            )
            if (info.artist.isNotBlank()) {
                Text(info.artist, color = Color(0xFF9E9E9E), fontSize = 14.sp, maxLines = 1)
            }
        }
        MediaButton(MediaIcon.Previous) { controls.skipToPrevious() }
        MediaButton(if (info.playing) MediaIcon.Pause else MediaIcon.Play) {
            if (info.playing) controls.pause() else controls.play()
        }
        MediaButton(MediaIcon.Next) { controls.skipToNext() }
    }
}

@Composable
private fun MediaButton(icon: MediaIcon, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            when (icon) {
                MediaIcon.Play -> drawPath(triangle(w * 0.15f, 0f, w * 0.15f, h, w, h / 2), Color.White)
                MediaIcon.Pause -> {
                    drawRect(Color.White, Offset(w * 0.15f, 0f), Size(w * 0.25f, h))
                    drawRect(Color.White, Offset(w * 0.6f, 0f), Size(w * 0.25f, h))
                }
                MediaIcon.Next -> {
                    drawPath(triangle(0f, 0f, 0f, h, w * 0.75f, h / 2), Color.White)
                    drawRect(Color.White, Offset(w * 0.85f, 0f), Size(w * 0.15f, h))
                }
                MediaIcon.Previous -> {
                    drawPath(triangle(w, 0f, w, h, w * 0.25f, h / 2), Color.White)
                    drawRect(Color.White, Offset(0f, 0f), Size(w * 0.15f, h))
                }
            }
        }
    }
}

private fun triangle(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
    Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
        lineTo(x3, y3)
        close()
    }
