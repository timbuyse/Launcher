package be.launcher.niagara.ui

import android.content.Context
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
    Box(modifier.size(size).clip(CircleShape)) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
fun AppIcon(app: AppEntry, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState(IconCache.get(app.key), app.key) {
        if (value == null) value = IconCache.load(context, app)
    }
    Box(modifier.size(size).clip(CircleShape)) {
        bitmap?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
