package be.launcher.niagara.ui

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
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

/** Zet de apps in rijen, met een letterkopje per groep (niet tijdens het zoeken). */
fun buildRows(apps: List<AppEntry>, grouped: Boolean): List<DrawerRow> {
    if (!grouped) return apps.map { DrawerRow.App(it) }
    val rows = ArrayList<DrawerRow>(apps.size + 27)
    var current: Char? = null
    for (app in apps) {
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
) {
    val haptic = LocalHapticFeedback.current
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
                    .fillMaxWidth(),
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
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
