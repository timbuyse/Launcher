package be.launcher.niagara.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import be.launcher.niagara.LauncherBackgrounds
import be.launcher.niagara.LauncherSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tekent de gekozen achtergrond. Bij 'Mijn achtergrond' tekenen we niets: het venster van de app
 * laat dan de wallpaper van je telefoon zien.
 */
@Composable
fun LauncherBackground(settings: LauncherSettings, modifier: Modifier = Modifier) {
    val id = settings.background
    val colors = LauncherBackgrounds.colors(id)
    when {
        colors != null -> Box(modifier.background(Brush.verticalGradient(colors)))
        id == LauncherBackgrounds.CUSTOM -> {
            val context = LocalContext.current
            val bitmap by produceState<ImageBitmap?>(null, settings.backgroundVersion) {
                value = withContext(Dispatchers.IO) {
                    runCatching {
                        BitmapFactory.decodeFile(LauncherBackgrounds.customFile(context).path)?.asImageBitmap()
                    }.getOrNull()
                }
            }
            val loaded = bitmap
            Box(modifier.background(Color.Black)) {
                if (loaded != null) {
                    Image(
                        bitmap = loaded,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
        else -> Unit
    }
}
