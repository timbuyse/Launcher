package be.launcher.niagara.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import kotlinx.coroutines.launch

private val LETTERS: List<Char> = ('A'..'Z').toList() + '#'

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

    val visible = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val searching = query.isNotBlank()

    fun scrollToLetter(letter: Char) {
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
            .background(Color.Black)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false,
            ) {
                items(visible, key = { it.packageName + "/" + it.activityName }) { app ->
                    Box {
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { onLaunch(app) },
                                    onLongClick = { menuFor = app },
                                )
                                .padding(start = 32.dp, end = 56.dp, top = 10.dp, bottom = 10.dp),
                        )
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
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    visible.firstOrNull()?.let(onLaunch)
                }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("Zoeken", color = Color(0xFF757575), fontSize = 18.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1C))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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

        activeLetter?.let { letter ->
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC303030)),
                contentAlignment = Alignment.Center,
            ) {
                Text(letter.toString(), color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

@Composable
private fun LetterBar(
    activeLetter: Char?,
    onLetter: (Char) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    fun letterAt(y: Float, height: Int): Char {
        val i = (y / height * LETTERS.size).toInt().coerceIn(0, LETTERS.lastIndex)
        return LETTERS[i]
    }

    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
            .padding(end = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onLetter(letterAt(offset.y, size.height))
                        tryAwaitRelease()
                        onRelease()
                    },
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset -> onLetter(letterAt(offset.y, size.height)) },
                    onDragEnd = onRelease,
                    onDragCancel = onRelease,
                ) { change, _ -> onLetter(letterAt(change.position.y, size.height)) }
            }
            .padding(horizontal = 12.dp),
    ) {
        LETTERS.forEach { letter ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = letter.toString(),
                    color = if (letter == activeLetter) Color.White else Color(0xFF9E9E9E),
                    fontSize = 11.sp,
                    fontWeight = if (letter == activeLetter) FontWeight.Bold else FontWeight.Normal,
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
