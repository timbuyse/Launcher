package be.launcher.niagara.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import be.launcher.niagara.AppEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IconCache {
    private const val ICON_PX = 108
    private val cache = LruCache<String, ImageBitmap>(400)

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }

    fun clear() = cache.evictAll()

    suspend fun load(context: Context, app: AppEntry): ImageBitmap? = withContext(Dispatchers.Default) {
        cache.get(app.key) ?: runCatching {
            context.packageManager.getActivityIcon(app.component)
                .toBitmap(ICON_PX, ICON_PX)
                .asImageBitmap()
        }.getOrNull()?.also { cache.put(app.key, it) }
    }
}

suspend fun IconCache.loadPackage(context: Context, packageName: String): ImageBitmap? =
    withContext(Dispatchers.Default) {
        val key = "pkg:$packageName"
        get(key) ?: runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(108, 108).asImageBitmap()
        }.getOrNull()?.also { put(key, it) }
    }

@Composable
fun PackageIcon(packageName: String, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState(IconCache.get("pkg:$packageName"), packageName) {
        if (value == null) value = IconCache.loadPackage(context, packageName)
    }
    val style = LocalIconStyle.current
    Box(modifier.size(size).clip(iconShapeFor(style.shape))) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (style.gray) GrayFilter else null,
            )
        }
    }
}

@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val style = LocalIconStyle.current
    val outline = style.shape == ICON_SHAPE_OUTLINE
    val cacheKey = if (outline) "o:${app.key}" else app.key
    val bitmap by produceState(IconCache.get(cacheKey), cacheKey) {
        if (value == null) value = if (outline) IconCache.loadOutline(context, app) else IconCache.load(context, app)
    }
    // De lijntekening heeft geen vorm of vulling nodig, de omtrek is het icoon zelf
    val shape = if (outline) RectangleShape else iconShapeFor(style.shape)
    Box(modifier.size(size).clip(shape)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                colorFilter = if (style.gray && !outline) GrayFilter else null,
            )
        }
    }
}

/**
 * Tekent de omtrek van het icoon in wit: we nemen de vorm (alpha) van de voorgrondlaag en houden
 * alleen de randpixels over. Heeft het icoon geen losse vorm, dan komt er een cirkel in de plaats.
 */
suspend fun IconCache.loadOutline(context: Context, app: AppEntry): ImageBitmap? =
    withContext(Dispatchers.Default) {
        val key = "o:${app.key}"
        get(key) ?: runCatching { buildOutline(context, app) }.getOrNull()?.also { put(key, it) }
    }

private fun buildOutline(context: Context, app: AppEntry): ImageBitmap {
    val drawable = context.packageManager.getActivityIcon(app.component)
    val adaptive = drawable is AdaptiveIconDrawable
    val layer = (drawable as? AdaptiveIconDrawable)?.foreground ?: drawable

    val full = 216
    val raw = Bitmap.createBitmap(full, full, Bitmap.Config.ARGB_8888)
    layer.setBounds(0, 0, full, full)
    layer.draw(Canvas(raw))

    // Een adaptief icoon is 108 dp breed, waarvan alleen het middelste tweederde zichtbaar is
    val crop = if (adaptive) full / 6 else 0
    val side = full - 2 * crop
    val scaled = Bitmap.createScaledBitmap(Bitmap.createBitmap(raw, crop, crop, side, side), OUTLINE_PX, OUTLINE_PX, true)

    val pixels = IntArray(OUTLINE_PX * OUTLINE_PX)
    scaled.getPixels(pixels, 0, OUTLINE_PX, 0, 0, OUTLINE_PX, OUTLINE_PX)
    val mask = BooleanArray(pixels.size) { (pixels[it] ushr 24) > 110 }
    val filled = mask.count { it }

    val result = Bitmap.createBitmap(OUTLINE_PX, OUTLINE_PX, Bitmap.Config.ARGB_8888)
    if (filled < pixels.size / 50 || filled > pixels.size * 85 / 100) {
        // Geen bruikbare losse vorm (leeg of het hele vlak gevuld): een ring als vervanger
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        Canvas(result).drawCircle(OUTLINE_PX / 2f, OUTLINE_PX / 2f, OUTLINE_PX * 0.44f, paint)
        return result.asImageBitmap()
    }

    val out = IntArray(pixels.size)
    val white = android.graphics.Color.WHITE
    for (y in 0 until OUTLINE_PX) {
        for (x in 0 until OUTLINE_PX) {
            if (!mask[y * OUTLINE_PX + x]) continue
            var edge = false
            for (dy in -1..1) {
                for (dx in -1..1) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until OUTLINE_PX || ny !in 0 until OUTLINE_PX || !mask[ny * OUTLINE_PX + nx]) edge = true
                }
            }
            if (edge) {
                // Twee pixels dik, zodat de lijn leesbaar blijft op klein formaat
                for (oy in 0..1) {
                    for (ox in 0..1) {
                        val px = x + ox
                        val py = y + oy
                        if (px < OUTLINE_PX && py < OUTLINE_PX) out[py * OUTLINE_PX + px] = white
                    }
                }
            }
        }
    }
    result.setPixels(out, 0, OUTLINE_PX, 0, 0, OUTLINE_PX, OUTLINE_PX)
    return result.asImageBitmap()
}

private const val OUTLINE_PX = 108
