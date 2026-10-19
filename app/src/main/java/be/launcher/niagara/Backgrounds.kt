package be.launcher.niagara

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Achtergronden die in de app zitten (getekend in code), plus je systeem-wallpaper en een eigen foto. */
object LauncherBackgrounds {
    const val SYSTEM = 5
    const val CUSTOM = 6

    val names = listOf(
        "Donker verloop",
        "Oceaan",
        "Middernacht",
        "Zonsondergang",
        "Zwart",
        "Mijn achtergrond",
        "Eigen foto",
    )

    /** Kleuren boven naar onder, of null voor de systeem-wallpaper en de eigen foto. */
    fun colors(id: Int): List<Color>? = when (id) {
        0 -> listOf(Color(0xFF4A4A4A), Color(0xFF0A0A0A))
        1 -> listOf(Color(0xFF0E4B5E), Color(0xFF02111A))
        2 -> listOf(Color(0xFF26264F), Color(0xFF05050F))
        3 -> listOf(Color(0xFF6B2C52), Color(0xFF140A1E))
        4 -> listOf(Color.Black, Color.Black)
        else -> null
    }

    fun customFile(context: Context) = File(context.filesDir, "background.jpg")

    /** Kopieert een gekozen foto verkleind naar de app, zodat hij blijft werken als het origineel verdwijnt. */
    suspend fun importCustom(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 2400 || bounds.outHeight / sample > 2400) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: return@runCatching false
            customFile(context).outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            true
        }.getOrDefault(false)
    }
}
