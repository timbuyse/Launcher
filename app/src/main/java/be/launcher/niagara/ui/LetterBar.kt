package be.launcher.niagara.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

const val STAR = '☆'

/** Letters van de balk: favorieten (ster), cijfers en symbolen (#) en dan A tot Z. */
val BAR_LETTERS: List<Char> = listOf(STAR, '#') + ('A'..'Z')

/**
 * Verticale letterbalk met golf-effect: letters rond je vinger buigen ver naar links weg en
 * worden groter, met een rond bolletje met de letter naast je vinger.
 */
@Composable
fun LetterBar(
    activeLetter: Char?,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var touchY by remember { mutableFloatStateOf(0f) }
    var barHeight by remember { mutableIntStateOf(1) }
    var shownLetter by remember { mutableStateOf('A') }
    SideEffect { if (activeLetter != null) shownLetter = activeLetter }
    val strengthState = animateFloatAsState(
        targetValue = if (activeLetter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "letterWave",
    )

    fun touch(y: Float, height: Int) {
        touchY = y
        val i = (y / height * BAR_LETTERS.size).toInt().coerceIn(0, BAR_LETTERS.lastIndex)
        currentOnLetter(BAR_LETTERS[i])
    }

    Box(modifier) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .onSizeChanged { barHeight = it.height.coerceAtLeast(1) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        touch(down.position.y, size.height)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                touch(change.position.y, size.height)
                            }
                        } while (event.changes.any { it.pressed })
                        currentOnRelease()
                    }
                }
                .padding(horizontal = 12.dp),
        ) {
            BAR_LETTERS.forEachIndexed { index, letter ->
                Box(
                    Modifier
                        .weight(1f)
                        .graphicsLayer {
                            val slot = barHeight.toFloat() / BAR_LETTERS.size
                            val center = (index + 0.5f) * slot
                            val closeness = (1f - abs(touchY - center) / (slot * 7f)).coerceAtLeast(0f)
                            val wave = closeness * closeness * strengthState.value
                            translationX = -wave * 120.dp.toPx()
                            scaleX = 1f + wave * 0.7f
                            scaleY = 1f + wave * 0.7f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        style = lightText(
                            size = 12.sp,
                            color = if (letter == activeLetter) Color.White else SubtleText,
                            weight = if (letter == activeLetter) FontWeight.Medium else FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        // Bolletje met de gekozen letter, links van je vinger
        Box(
            Modifier
                .offset { IntOffset(-(150.dp.roundToPx()), (touchY - 24.dp.toPx()).roundToInt()) }
                .graphicsLayer { alpha = strengthState.value }
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xCC3B3F58)),
            contentAlignment = Alignment.Center,
        ) {
            Text(shownLetter.toString(), style = lightText(22.sp, weight = FontWeight.Medium))
        }
    }
}
