package be.launcher.niagara.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import be.launcher.niagara.NotificationHub

sealed interface DrawerRow {
    data class Header(val letter: Char) : DrawerRow
    data class App(val app: AppEntry) : DrawerRow
}

/**
 * Zet de apps in rijen, met een letterkopje per groep (niet tijdens het zoeken).
 * Binnen een letter staan de meest gebruikte apps bovenaan als [byUsage] aan staat.
 */
fun buildRows(
    apps: List<AppEntry>,
    grouped: Boolean,
    counts: Map<String, Int> = emptyMap(),
    byUsage: Boolean = false,
): List<DrawerRow> {
    fun used(app: AppEntry) = if (byUsage) -(counts[app.key] ?: 0) else 0
    if (!grouped) {
        val ordered = if (byUsage) apps.sortedWith(compareBy<AppEntry>({ used(it) }, { it.label.lowercase() })) else apps
        return ordered.map { DrawerRow.App(it) }
    }
    // Eén reeks per letter, ook voor symbolen en niet-Latijnse letters (#), zodat kopjes uniek blijven
    val sorted = apps.sortedWith(
        compareBy<AppEntry>({ BAR_LETTERS.indexOf(it.letter) }, { used(it) }, { it.label.lowercase() }),
    )
    val rows = ArrayList<DrawerRow>(sorted.size + 27)
    var current: Char? = null
    for (app in sorted) {
        if (app.letter != current) {
            current = app.letter
            rows += DrawerRow.Header(app.letter)
        }
        rows += DrawerRow.App(app)
    }
    return rows
}

/** Rij waar je naartoe springt voor een letter, of anders de eerstvolgende letter die voorkomt. */
fun rowIndexForLetter(rows: List<DrawerRow>, letter: Char): Int {
    val start = BAR_LETTERS.indexOf(letter).coerceAtLeast(0)
    for (candidate in BAR_LETTERS.drop(start)) {
        val index = rows.indexOfFirst { it is DrawerRow.Header && it.letter == candidate }
        if (index >= 0) return index
    }
    return rows.lastIndex
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawer(
    rows: List<DrawerRow>,
    listState: LazyListState,
    query: String,
    onQueryChange: (String) -> Unit,
    showIcons: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    focusLetter: Char? = null,
) {
    val buzz = rememberHaptics()

    // Tijdens het slepen over de letterbalk tonen we kort alleen de eerste apps van die letter.
    // Laat je los, dan komt de hele lijst op die plek terug.
    val focusRows = remember(rows, focusLetter) { focusGroup(rows, focusLetter) }
    var shownRows by remember { mutableStateOf<List<DrawerRow>?>(null) }
    SideEffect { if (focusRows != null) shownRows = focusRows }
    val focusAlpha by animateFloatAsState(
        targetValue = if (focusRows != null) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "letterFocus",
    )
    var searchOpen by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(searchOpen) { if (searchOpen) focusRequester.requestFocus() }

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
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f - focusAlpha },
            ) {
                items(
                    rows,
                    key = {
                        when (it) {
                            is DrawerRow.Header -> "header-${it.letter}"
                            is DrawerRow.App -> it.app.key
                        }
                    },
                ) { row ->
                    DrawerRowView(row, showIcons, onLaunch, onAppMenu, buzz)
                }
            }

            if (searchOpen) BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = lightText(18.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    rows.firstNotNullOfOrNull { (it as? DrawerRow.App)?.app }?.let(onLaunch)
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
                    .focusRequester(focusRequester)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x66000000))
                    .padding(horizontal = 22.dp, vertical = 14.dp),
            )
        }

        val shown = shownRows
        if (shown != null && focusAlpha > 0.01f) {
            Column(Modifier.fillMaxSize().graphicsLayer { alpha = focusAlpha }) {
                Spacer(Modifier.weight(0.28f))
                Column(Modifier.weight(0.72f)) {
                    shown.forEach { row -> DrawerRowView(row, showIcons, onLaunch, onAppMenu, buzz) }
                }
            }
        }

        if (!searchOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 76.dp, bottom = 24.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0x66A9C7CF))
                    .clickable { searchOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Search, contentDescription = "Zoeken", tint = Color.White)
            }
        }
    }
}

/** Het kopje van de gekozen letter met de eerste [maxApps] apps eronder. */
fun focusGroup(rows: List<DrawerRow>, letter: Char?, maxApps: Int = 6): List<DrawerRow>? {
    if (letter == null || letter == STAR) return null
    val start = rowIndexForLetter(rows, letter)
    if (start < 0 || rows.getOrNull(start) !is DrawerRow.Header) return null
    val group = ArrayList<DrawerRow>()
    group += rows[start]
    var apps = 0
    for (i in start + 1 until rows.size) {
        val row = rows[i]
        if (row is DrawerRow.Header || apps >= maxApps) break
        group += row
        apps++
    }
    return group
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerRowView(
    row: DrawerRow,
    showIcons: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onAppMenu: (AppEntry) -> Unit,
    buzz: (HapticFeedbackType) -> Unit,
) {
    when (row) {
        is DrawerRow.Header -> Text(
            text = row.letter.toString(),
            style = lightText(17.sp, SubtleText, FontWeight.Normal),
            modifier = Modifier.padding(start = 32.dp, top = 22.dp, bottom = 4.dp),
        )

        is DrawerRow.App -> {
            val app = row.app
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onLaunch(app) },
                        onLongClick = {
                            buzz(HapticFeedbackType.LongPress)
                            onAppMenu(app)
                        },
                    )
                    .padding(start = 32.dp, end = 72.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showIcons) {
                    AppIcon(app, 40.dp)
                    Spacer(Modifier.width(20.dp))
                }
                Text(
                    text = app.label,
                    style = lightText(20.sp, weight = FontWeight.Normal),
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val count = NotificationHub.counts[app.packageName] ?: 0
                if (count > 0) {
                    Spacer(Modifier.width(12.dp))
                    NotificationBadge(count)
                }
            }
        }
    }
}
