package be.launcher.niagara

import android.content.BroadcastReceiver
import android.content.Context
import android.app.role.RoleManager
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import be.launcher.niagara.ui.AppActionsSheet
import be.launcher.niagara.ui.AppDrawer
import be.launcher.niagara.ui.ClockStylePicker
import be.launcher.niagara.ui.FavoritesEditor
import be.launcher.niagara.ui.HomeSettingsSheet
import be.launcher.niagara.ui.NotificationAccessSheet
import be.launcher.niagara.ui.WidgetPicker
import be.launcher.niagara.ui.IconCache
import be.launcher.niagara.ui.LetterBar
import be.launcher.niagara.ui.STAR
import be.launcher.niagara.ui.buildRows
import be.launcher.niagara.ui.rowIndexForLetter
import kotlinx.coroutines.launch
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
            val settings = remember { LauncherSettings(context) }
            var apps by remember { mutableStateOf(AppRepository.load(context)) }
            var favorites by remember { mutableStateOf(store.load()) }
            var open by drawerOpen

            var sheetApp by remember { mutableStateOf<AppEntry?>(null) }
            var editingFavorites by remember { mutableStateOf(false) }
            var pickerOpen by remember { mutableStateOf(false) }
            var settingsOpen by remember { mutableStateOf(false) }
            var clockPickerOpen by remember { mutableStateOf(false) }
            // null = dicht, 0 = uitleg, 1 = hulp bij beperkte instellingen
            var notificationSheet by remember { mutableStateOf<Int?>(null) }

            val listState = rememberLazyListState()
            var query by remember { mutableStateOf("") }
            var activeLetter by remember { mutableStateOf<Char?>(null) }
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current

            val visibleApps = remember(apps, query) {
                if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
            }
            val rows = remember(visibleApps, query) { buildRows(visibleApps, grouped = query.isBlank()) }

            // Lijst verversen als er apps worden (de)installeerd
            DisposableEffect(Unit) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, i: Intent) {
                        IconCache.clear()
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

            // Uitleg sluiten zodra meldingstoegang aan staat
            LaunchedEffect(listenerEnabled.value) { if (listenerEnabled.value) notificationSheet = null }

            // Zoekopdracht wissen als de lijst dicht gaat
            LaunchedEffect(open) { if (!open) query = "" }

            fun toggleFavorite(packageName: String) {
                favorites = if (packageName in favorites) favorites - packageName
                else favorites + packageName
                store.save(favorites)
            }

            fun moveFavorite(from: Int, to: Int) {
                favorites = favorites.toMutableList().also { list ->
                    val item = list.removeAt(from)
                    list.add(to, item)
                }
                store.save(favorites)
            }

            // De letterbalk werkt ook vanaf het startscherm: aanraken opent de lijst bij die letter
            fun onLetter(letter: Char) {
                if (letter != activeLetter) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                activeLetter = letter
                if (letter == STAR) {
                    open = false
                    return
                }
                open = true
                val index = rowIndexForLetter(rows, letter)
                if (index >= 0) {
                    // Kopje van de letter komt op ongeveer 30% van de hoogte te staan
                    val offset = -(listState.layoutInfo.viewportSize.height * 0.3f).toInt()
                    scope.launch { listState.scrollToItem(index, offset) }
                }
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
                    settings = settings,
                    widgets = widgets,
                    listenerEnabled = listenerEnabled.value,
                    isDefaultLauncher = isDefaultLauncher.value,
                    onLaunch = { AppRepository.launch(context, it) },
                    onOpenDrawer = { open = true },
                    onAppMenu = { sheetApp = it },
                    onOpenSettings = { settingsOpen = true },
                    onEditFavorites = { editingFavorites = true },
                    onRequestNotificationAccess = { notificationSheet = 0 },
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
                        rows = rows,
                        listState = listState,
                        query = query,
                        onQueryChange = { query = it },
                        showIcons = settings.showIcons,
                        onLaunch = {
                            AppRepository.launch(context, it)
                            open = false
                        },
                        onAppMenu = { sheetApp = it },
                    )
                }

                if (query.isBlank() && !editingFavorites && !pickerOpen && !clockPickerOpen) {
                    LetterBar(
                        activeLetter = activeLetter,
                        onLetter = ::onLetter,
                        onRelease = { activeLetter = null },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .safeDrawingPadding()
                            .fillMaxHeight(0.62f)
                            .padding(end = 4.dp),
                    )
                }

                if (editingFavorites) {
                    FavoritesEditor(
                        apps = apps,
                        favorites = favorites,
                        onToggle = ::toggleFavorite,
                        onMove = ::moveFavorite,
                        onDone = { editingFavorites = false },
                    )
                }

                if (clockPickerOpen) {
                    ClockStylePicker(
                        current = settings.clockStyle,
                        onPick = { settings.updateClockStyle(it) },
                        onDone = { clockPickerOpen = false },
                    )
                }

                if (pickerOpen) {
                    WidgetPicker(
                        providers = remember { widgets.providers() },
                        onPick = {
                            pickerOpen = false
                            widgets.add(it)
                        },
                        onDismiss = { pickerOpen = false },
                    )
                }
            }

            if (settingsOpen) {
                HomeSettingsSheet(
                    settings = settings,
                    widgets = widgets,
                    listenerEnabled = listenerEnabled.value,
                    isDefaultLauncher = isDefaultLauncher.value,
                    onOpenClockStyle = { clockPickerOpen = true },
                    onAddWidget = { pickerOpen = true },
                    onEditFavorites = { editingFavorites = true },
                    onRequestNotificationAccess = { notificationSheet = 0 },
                    onRequestDefaultLauncher = this@MainActivity::requestDefaultLauncher,
                    onDismiss = { settingsOpen = false },
                )
            }

            notificationSheet?.let { stage ->
                NotificationAccessSheet(
                    stage = stage,
                    onEnable = {
                        requestNotificationAccess()
                        notificationSheet = 1
                    },
                    onOpenAppInfo = {
                        runCatching {
                            startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", packageName, null)),
                            )
                        }
                    },
                    onRetry = { requestNotificationAccess() },
                    onDismiss = { notificationSheet = null },
                )
            }

            sheetApp?.let { app ->
                AppActionsSheet(
                    app = app,
                    isFavorite = app.packageName in favorites,
                    onToggleFavorite = { toggleFavorite(app.packageName) },
                    onOpenSettings = { settingsOpen = true },
                    onDismiss = { sheetApp = null },
                )
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
