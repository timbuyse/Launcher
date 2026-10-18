package be.launcher.niagara

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import be.launcher.niagara.ui.AppDrawer
import be.launcher.niagara.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private val drawerOpen = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val context = LocalContext.current
            val store = remember { FavoritesStore(context) }
            var apps by remember { mutableStateOf(AppRepository.load(context)) }
            var favorites by remember { mutableStateOf(store.load()) }
            var open by drawerOpen

            // Lijst verversen als er apps worden (de)installeerd
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, i: Intent) {
                        apps = AppRepository.load(context)
                    }
                }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_PACKAGE_ADDED)
                    addAction(Intent.ACTION_PACKAGE_REMOVED)
                    addAction(Intent.ACTION_PACKAGE_REPLACED)
                    addDataScheme("package")
                }
                ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
                onDispose { context.unregisterReceiver(receiver) }
            }

            fun toggleFavorite(packageName: String) {
                favorites = if (packageName in favorites) favorites - packageName
                else favorites + packageName
                store.save(favorites)
            }

            BackHandler(enabled = open) { open = false }

            Box(Modifier.fillMaxSize().background(ComposeColor.Black)) {
                HomeScreen(
                    favorites = favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } },
                    onLaunch = { AppRepository.launch(context, it) },
                    onOpenDrawer = { open = true },
                )
                AnimatedVisibility(
                    visible = open,
                    enter = slideInVertically { it / 3 } + fadeIn(),
                    exit = slideOutVertically { it / 3 } + fadeOut(),
                ) {
                    AppDrawer(
                        apps = apps,
                        favorites = favorites,
                        onLaunch = {
                            AppRepository.launch(context, it)
                            open = false
                        },
                        onToggleFavorite = ::toggleFavorite,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home-knop: terug naar het startscherm
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            drawerOpen.value = false
        }
    }
}
