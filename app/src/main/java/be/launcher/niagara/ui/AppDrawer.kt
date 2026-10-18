package be.launcher.niagara.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import be.launcher.niagara.NotificationHub
import kotlinx.coroutines.launch
import kotlin.math.abs

private val LETTERS: List<Char> = ('A'..'Z').toList() + '#'
private val SubtleText = Color(0xFFC7C7C7)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    favorites: List<String>,
    onLaunch: (AppEntry) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppEntry?>(null) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val visible = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val searching = query.isNotBlank()

    fun scrollToLetter(letter: Char) {
        if (letter != activeLetter) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        activeLetter = letter
        // Zoek de eerste app met deze letter, of anders de eerstvolgende
        val start = LETTERS.indexOf(letter)
        val index = LETTERS.drop(start)
            .firstNotNullOfOrNull { l -> visible.indexOfFirst { it.letter == l }.takeIf { it >= 0 } }
            ?: visible.lastIndex
        if (index >= 0) scope.launch { listState.scrollToItem(index) }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Voorkomt dat aanrakingen op lege plekken het startscherm eronder bereiken
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(visible, key = { it.packageName + "/" + it.activityName }) { app ->
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
                                .padding(start = 32.dp, end = 72.dp, top = 9.dp, bottom = 9.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val count = NotificationHub.counts[app.packageName] ?: 0
                            if (count > 0) {
                                NotificationBadge(count)
                                Spacer(Modifier.width(12.dp))
                            }
                            Text(
                                text = app.label,
                                style = lightText(28.sp),
                                maxLines = 1,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        DropdownMenu(
                            expanded = menuFor == app,
                            onDismissRequest = { menuFor = null },
                        ) {
                            val isFav = app.packageName in favorites
                            DropdownMenuItem(
                                text = { Text(if (isFav) "Van startscherm halen" else "Op startscherm zetten") },
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
                            DropdownMenuItem(
                                text = { Text("Verwijderen") },
                                onClick = {
                                    uninstall(context, app)
                                    menuFor = null
                                },
                            )
                        }
                    }
                }
            }

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = lightText(18.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    visible.firstOrNull()?.let(onLaunch)
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("Zoeken", style = lightText(18.sp, Color(0xFFA0A0A0)))
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            )
        }

        if (!searching) {
            LetterBar(
                activeLetter = activeLetter,
                onLetter = ::scrollToLetter,
                onRelease = { activeLetter = null },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 72.dp),
            )
        }
    }
}

/**
 * Letterbalk met golf-effect: letters bij je vinger schuiven naar links en worden groter,
 * hoe verder weg hoe minder.
 */
@Composable
private fun LetterBar(
    activeLetter: Char?,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnLetter by rememberUpdatedState(onLetter)
    val currentOnRelease by rememberUpdatedState(onRelease)
    var touchY by remember { mutableFloatStateOf(0f) }
    var barHeight by remember { mutableIntStateOf(1) }
    val strengthState = animateFloatAsState(
        targetValue = if (activeLetter != null) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "letterWave",
    )

    fun touch(y: Float, height: Int) {
        touchY = y
        val i = (y / height * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
        currentOnLetter(LETTERS[i])
    }

    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
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
            .padding(horizontal = 14.dp),
    ) {
        LETTERS.forEachIndexed { index, letter ->
            Box(
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val slot = barHeight.toFloat() / LETTERS.size
                        val center = (index + 0.5f) * slot
                        val closeness = (1f - abs(touchY - center) / (slot * 3.5f)).coerceAtLeast(0f)
                        val wave = closeness * closeness * strengthState.value
                        translationX = -wave * 56.dp.toPx()
                        scaleX = 1f + wave * 0.9f
                        scaleY = 1f + wave * 0.9f
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter.toString(),
                    style = lightText(12.sp, if (letter == activeLetter) Color.White else SubtleText),
                )
            }
        }
    }
}

private fun openAppInfo(context: Context, app: AppEntry) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun uninstall(context: Context, app: AppEntry) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
