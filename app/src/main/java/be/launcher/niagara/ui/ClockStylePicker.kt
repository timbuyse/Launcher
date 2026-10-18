package be.launcher.niagara.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Raster met alle klokstijlen. Tik op een stijl om hem te kiezen. */
@Composable
fun ClockStylePicker(
    current: Int,
    onPick: (Int) -> Unit,
    onDone: () -> Unit,
) {
    BackHandler(onBack = onDone)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF031820))
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Klok stijl",
                style = lightText(30.sp, Color(0xFF55C7F7), FontWeight.Normal),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, bottom = 32.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(ClockStyle.values().toList()) { index, style ->
                    val shape = RoundedCornerShape(28.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.65f)
                            .clip(shape)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF0098C2), Color(0xFF006680))),
                            )
                            .then(if (index == current) Modifier.border(3.dp, Color.White, shape) else Modifier)
                            .clickable { onPick(index) },
                    ) {
                        Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                            Text("16:00", style = style.textStyle(38.sp))
                            Text("do 30 aug  100%", style = lightText(13.sp, Color.White, FontWeight.Normal))
                        }
                    }
                }
            }
        }
    }
}
