package be.launcher.niagara.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableLongStateOf
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
import kotlin.math.exp
import kotlin.math.roundToInt

const val STAR = '☆'

/** Letters van de balk: favorieten (ster), cijfers en symbolen (#) en dan A tot Z. */
val BAR_LETTERS: List<Char> = listOf(STAR, '#') + ('A'..'Z')

/**
 * Verticale letterbalk met golf-effect: de hele balk buigt in een brede, zachte boog naar links,
 * met de grootste uitwijking bij je vinger, en een rond bolletje met de letter ernaast.
 */
@Composable
fun LetterBar(
    activeLetter: Char?,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (() -> Unit)? = null,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    var lastTapUp by remember { mutableLongStateOf(0L) }
    val currentOnRelease by rememberUpdatedState(onRelease)
    var touchY by remember { mutableFloatStateOf(0f) }
    var barHeight by remember { mutableIntStateOf(1) }
    var shownLetter by remember { mutableStateOf('A') }
    SideEffect { if (activeLetter != null) shownLetter = activeLetter }
    // De boog volgt je vinger met een lichte veer, zodat hij zacht meegolft
    val waveY = animateFloatAsState(
        targetValue = touchY,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
        label = "waveY",
    )
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

                        // Twee korte tikken achter elkaar: dubbeltik (bijvoorbeeld om te vergrendelen)
                        val doubleTap = currentOnDoubleTap
                        if (doubleTap != null && down.uptimeMillis - lastTapUp < 350L) {
                            lastTapUp = 0L
                            doubleTap()
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                            return@awaitEachGesture
                        }

                        touch(down.position.y, size.height)
                        var moved = false
                        var lastUp = down.uptimeMillis
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (change.pressed) {
                                change.consume()
                                if (abs(change.position.y - down.position.y) > 24f) moved = true
                                touch(change.position.y, size.height)
                            }
                            lastUp = change.uptimeMillis
                        } while (event.changes.any { it.pressed })
                        lastTapUp = if (moved) 0L else lastUp
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
                            // Brede, zachte klokvorm: grootste uitwijking bij je vinger, bijna nul aan de uiteinden
                            val slot = barHeight.toFloat() / BAR_LETTERS.size
                            val center = (index + 0.5f) * slot
                            val distance = waveY.value - center
                            val sigma = barHeight * 0.28f
                            val wave = exp(-(distance * distance) / (sigma * sigma)) * strengthState.value
                            translationX = -wave * 64.dp.toPx()
                            scaleX = 1f + wave * 0.1f
                            scaleY = 1f + wave * 0.1f
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter.toString(),
                        style = lightText(
                            size = 15.sp,
                            color = Color.White,
                            weight = FontWeight.Medium,
                        ),
                    )
                }
            }
        }

        // Bolletje met de gekozen letter, links van je vinger
        Box(
            Modifier
                .offset { IntOffset(-(110.dp.roundToPx()), (touchY - 24.dp.toPx()).roundToInt()) }
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
