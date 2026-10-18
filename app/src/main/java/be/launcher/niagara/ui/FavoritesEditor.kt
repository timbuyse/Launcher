package be.launcher.niagara.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import be.launcher.niagara.AppEntry

private val InfoBlue = Color(0xFF8FD3F4)
private val InfoDark = Color(0xFF05303F)
private val ROW_HEIGHT = 64.dp

/** Scherm om favorieten te kiezen en met slepen een volgorde te geven. */
@Composable
fun FavoritesEditor(
    apps: List<AppEntry>,
    favorites: List<String>,
    onToggle: (String) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)

    val selected = favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
    val suggestions = apps.filter { it.packageName !in favorites }

    var draggingPackage by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }
    val currentFavorites by rememberUpdatedState(favorites)
    val currentOnMove by rememberUpdatedState(onMove)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 56.dp, bottom = 140.dp),
        ) {
            item {
                Text(
                    "Jouw favorieten",
                    style = lightText(52.sp),
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
            item { InfoRow(Icons.Outlined.Star, "Favorieten verschijnen op je startscherm voor snelle toegang") }
            item { InfoRow(Icons.Outlined.CheckCircle, "De meeste mensen kiezen hun 4 tot 8 meest gebruikte apps") }

            if (selected.isNotEmpty()) {
                item { SectionTitle("Geselecteerd") }
                items(selected, key = { "sel-" + it.key }) { app ->
                    val pkg = app.packageName
                    val dragging = draggingPackage == pkg
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ROW_HEIGHT)
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer { translationY = if (dragging) dragOffset else 0f },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FavCheckbox(checked = true) { onToggle(pkg) }
                        Spacer(Modifier.width(12.dp))
                        AppIcon(app, 52.dp)
                        Spacer(Modifier.width(20.dp))
                        Text(
                            app.label,
                            style = lightText(20.sp, weight = FontWeight.Normal),
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        DragHandle(
                            Modifier.pointerInput(pkg) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingPackage = pkg
                                        dragOffset = 0f
                                    },
                                    onDragEnd = {
                                        draggingPackage = null
                                        dragOffset = 0f
                                    },
                                    onDragCancel = {
                                        draggingPackage = null
                                        dragOffset = 0f
                                    },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val index = currentFavorites.indexOf(pkg)
                                    if (dragOffset > rowPx / 2 && index < currentFavorites.lastIndex) {
                                        currentOnMove(index, index + 1)
                                        dragOffset -= rowPx
                                    } else if (dragOffset < -rowPx / 2 && index > 0) {
                                        currentOnMove(index, index - 1)
                                        dragOffset += rowPx
                                    }
                                }
                            },
                        )
                    }
                }
            }

            item { SectionTitle("Suggesties") }
            items(suggestions, key = { "sug-" + it.key }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ROW_HEIGHT)
                        .clickable { onToggle(app.packageName) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FavCheckbox(checked = false) { onToggle(app.packageName) }
                    Spacer(Modifier.width(12.dp))
                    AppIcon(app, 52.dp)
                    Spacer(Modifier.width(20.dp))
                    Text(app.label, style = lightText(20.sp, weight = FontWeight.Normal), maxLines = 1)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 32.dp, bottom = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF55C7F7))
                .clickable(onClick = onDone)
                .padding(horizontal = 32.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = InfoDark)
            Spacer(Modifier.width(16.dp))
            Text("Voltooid", style = lightText(20.sp, InfoDark, FontWeight.Medium))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = lightText(22.sp, weight = FontWeight.Medium),
        modifier = Modifier.padding(top = 28.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(InfoBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = InfoDark)
        }
        Spacer(Modifier.width(20.dp))
        Text(text, style = lightText(17.sp, weight = FontWeight.Normal), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FavCheckbox(checked: Boolean, onChange: () -> Unit) {
    Checkbox(
        checked = checked,
        onCheckedChange = { onChange() },
        colors = CheckboxDefaults.colors(
            checkedColor = Color.White,
            uncheckedColor = Color.White,
            checkmarkColor = Color.Black,
        ),
    )
}

/** Vier puntjes als sleepgreep. */
@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            val r = 2.5.dp.toPx()
            listOf(
                Offset(size.width * 0.25f, size.height * 0.25f),
                Offset(size.width * 0.75f, size.height * 0.25f),
                Offset(size.width * 0.25f, size.height * 0.75f),
                Offset(size.width * 0.75f, size.height * 0.75f),
            ).forEach { drawCircle(Color.White, r, it) }
        }
    }
}
