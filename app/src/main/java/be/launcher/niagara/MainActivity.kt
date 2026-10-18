package be.launcher.niagara

import android.content.BroadcastReceiver
import android.content.Context
import android.app.role.RoleManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import be.launcher.niagara.ui.AppDrawer
import be.launcher.niagara.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private val drawerOpen = mutableStateOf(false)
    private val listenerEnabled = mutableStateOf(false)
    private val isDefaultLauncher = mutableStateOf(false)
    private lateinit var widgets: WidgetController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgets = WidgetController(this)
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

            // Op het startscherm doet Terug niets, zoals bij een echte launcher
            BackHandler { open = false }

            // 0 = startscherm, 1 = app-lijst; stuurt de verduistering en het wegschuiven van het startscherm
            val progress by animateFloatAsState(
                targetValue = if (open) 1f else 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
                label = "drawerProgress",
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(ComposeColor.Black.copy(alpha = 0.30f + 0.45f * progress)),
            ) {
                HomeScreen(
                    favorites = favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } },
                    widgets = widgets,
                    listenerEnabled = listenerEnabled.value,
                    isDefaultLauncher = isDefaultLauncher.value,
                    onLaunch = { AppRepository.launch(context, it) },
                    onOpenDrawer = { open = true },
                    onRequestNotificationAccess = this@MainActivity::requestNotificationAccess,
                    onRequestDefaultLauncher = this@MainActivity::requestDefaultLauncher,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - progress
                        val scale = 1f - 0.06f * progress
                        scaleX = scale
                        scaleY = scale
                    },
                )
                AnimatedVisibility(
                    visible = open,
                    enter = slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 4 } +
                        fadeIn(tween(320)),
                    exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 4 } +
                        fadeOut(tween(260)),
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

    override fun onStart() {
        super.onStart()
        widgets.startListening()
    }

    override fun onStop() {
        widgets.stopListening()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        listenerEnabled.value = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val current = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        isDefaultLauncher.value = current?.activityInfo?.packageName == packageName
    }

    @Deprecated("Nodig voor het binden en instellen van widgets")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgets.handleResult(requestCode, resultCode)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun requestNotificationAccess() {
        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    }

    private fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                @Suppress("DEPRECATION")
                startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), REQ_ROLE)
                return
            }
        }
        runCatching { startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Home-knop: terug naar het startscherm
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            drawerOpen.value = false
        }
    }

    private companion object {
        const val REQ_ROLE = 4003
    }
}
