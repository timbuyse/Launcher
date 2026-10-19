package be.launcher.niagara.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Klok met een grote weekdag als dunne omtrek. De tijd, datum en batterij staan in kleine,
 * ruim gespatieerde tekst midden door de letters, die op die plek zijn doorgesneden.
 */
@Composable
fun OutlineClock(
    weekday: String,
    time: String,
    date: String,
    battery: String?,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val smallStyle = lightText((13 * scale).sp, Color.White, FontWeight.Medium).copy(letterSpacing = (3 * scale).sp)
    val bandHeight = 28 * scale

    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = weekday,
            style = TextStyle(
                color = Color.White,
                fontSize = (120 * scale).sp,
                fontFamily = Outfit,
                fontWeight = FontWeight.Light,
                drawStyle = Stroke(width = 3f * scale),
            ),
            modifier = Modifier
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Strook in het midden wissen, zodat de kleine tekst vrij staat
                    val band = bandHeight.dp.toPx()
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(0f, size.height / 2f - band / 2f),
                        size = Size(size.width, band),
                        blendMode = BlendMode.Clear,
                    )
                },
        )
        Row(horizontalArrangement = Arrangement.spacedBy((22 * scale).dp)) {
            Text(time, style = smallStyle)
            Text(date, style = smallStyle)
            if (battery != null) Text(battery, style = smallStyle)
        }
    }
}
