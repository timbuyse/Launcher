package be.launcher.niagara

import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import be.launcher.niagara.ui.AppActionsSheet
import be.launcher.niagara.ui.AppDrawer
import be.launcher.niagara.ui.ClockStylePicker
import be.launcher.niagara.ui.FavoritesEditor
import be.launcher.niagara.ui.HomeScreen
import be.launcher.niagara.ui.HomeSettingsSheet
import be.launcher.niagara.ui.IconCache
import be.launcher.niagara.ui.IconStyle
import be.launcher.niagara.ui.LetterBar
import be.launcher.niagara.ui.LocalHapticsEnabled
import be.launcher.niagara.ui.LocalIconStyle
import be.launcher.niagara.ui.NotificationAccessSheet
import be.launcher.niagara.ui.STAR
import be.launcher.niagara.ui.SettingsActions
import be.launcher.niagara.ui.SettingsPage
import be.launcher.niagara.ui.SettingsScreen
import be.launcher.niagara.ui.WidgetPicker
import be.launcher.niagara.ui.buildRows
import be.launcher.niagara.ui.rowIndexForLetter
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

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
            val usage = remember { UsageStore(context) }
            var apps by remember { mutableStateOf(AppRepository.load(context)) }
            var favorites by remember { mutableStateOf(store.load()) }
            var open by drawerOpen

            var sheetApp by remember { mutableStateOf<AppEntry?>(null) }
            var editingFavorites by remember { mutableStateOf(false) }
            var pickerOpen by remember { mutableStateOf(false) }
            var quickMenuOpen by remember { mutableStateOf(false) }
            var clockPickerOpen by remember { mutableStateOf(false) }
            // null = dicht, 0 = uitleg, 1 = hulp bij beperkte instellingen
            var notificationSheet by remember { mutableStateOf<Int?>(null) }
            // Lege lijst = instellingenscherm dicht, anders de gestapelde pagina's
            var settingsStack by remember { mutableStateOf(emptyList<SettingsPage>()) }

            val listState = rememberLazyListState()
            var query by remember { mutableStateOf("") }
            var activeLetter by remember { mutableStateOf<Char?>(null) }
            val scope = rememberCoroutineScope()
            val haptic = LocalHapticFeedback.current

            // Verborgen apps staan niet in de lijst, maar je kunt ze nog wel als favoriet hebben
            val hidden = settings.hiddenApps
            val listApps = remember(apps, hidden) { apps.filter { it.key !in hidden } }
            val visibleApps = remember(listApps, query) {
                if (query.isBlank()) listApps else listApps.filter { it.label.contains(query.trim(), ignoreCase = true) }
            }
            val rows = remember(visibleApps, query, usage.counts, settings.sortByUsage) {
                buildRows(visibleApps, grouped = query.isBlank(), counts = usage.counts, byUsage = settings.sortByUsage)
            }
            val favoriteApps = favorites.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
            val frequent = remember(apps, favorites, hidden, usage.counts, settings.showFrequent) {
                if (!settings.showFrequent) {
                    emptyList()
                } else {
                    apps
                        .filter { it.packageName !in favorites && it.key !in hidden && (usage.counts[it.key] ?: 0) >= 3 }
                        .sortedByDescending { usage.counts[it.key] ?: 0 }
                        .take(3)
                }
            }

            fun launchApp(app: AppEntry) {
                usage.record(app.key)
                AppRepository.launch(context, app)
            }

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

            // Schermrotatie volgt de instelling
            LaunchedEffect(settings.allowRotation) {
                this@MainActivity.requestedOrientation = if (settings.allowRotation) {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }

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

            fun changeQuickLock(enabled: Boolean) {
                settings.updateQuickLock(enabled)
                if (enabled && !LockAccessibilityService.isEnabled) {
                    Toast.makeText(
                        context,
                        "Zet Launcher aan onder Toegankelijkheid om te kunnen vergrendelen",
                        Toast.LENGTH_LONG,
                    ).show()
                    runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                }
            }

            // De letterbalk werkt ook vanaf het startscherm: aanraken opent de lijst bij die letter
            fun onLetter(letter: Char) {
                if (letter != activeLetter && settings.haptics) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
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

            val settingsActions = SettingsActions(
                onOpenClockStyle = { clockPickerOpen = true },
                onAddWidget = { pickerOpen = true },
                onEditFavorites = { editingFavorites = true },
                onNotificationAccess = { notificationSheet = 0 },
                onDefaultLauncher = this@MainActivity::requestDefaultLauncher,
                onQuickLockChanged = ::changeQuickLock,
                onRestart = { recreate() },
            )

            CompositionLocalProvider(
                LocalHapticsEnabled provides settings.haptics,
                LocalIconStyle provides IconStyle(settings.iconShape, settings.grayIcons),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ComposeColor.Black.copy(alpha = (settings.dim + 0.45f * progress).coerceAtMost(0.95f))),
                ) {
                    HomeScreen(
                        favorites = favoriteApps,
                        frequent = frequent,
                        settings = settings,
                        widgets = widgets,
                        listenerEnabled = listenerEnabled.value,
                        isDefaultLauncher = isDefaultLauncher.value,
                        onLaunch = ::launchApp,
                        onOpenDrawer = { open = true },
                        onAppMenu = { sheetApp = it },
                        onOpenSettings = { quickMenuOpen = true },
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
                                launchApp(it)
                                open = false
                            },
                            onAppMenu = { sheetApp = it },
                        )
                    }

                    val overlayOpen = editingFavorites || pickerOpen || clockPickerOpen || settingsStack.isNotEmpty()
                    if (query.isBlank() && !overlayOpen) {
                        LetterBar(
                            activeLetter = activeLetter,
                            onLetter = ::onLetter,
                            onRelease = { activeLetter = null },
                            onDoubleTap = if (settings.quickLock) {
                                {
                                    open = false
                                    if (!LockAccessibilityService.lockScreen()) {
                                        Toast.makeText(
                                            context,
                                            "Snel vergrendelen staat nog niet aan onder Toegankelijkheid",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            } else {
                                null
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .safeDrawingPadding()
                                .fillMaxHeight(0.62f)
                                .padding(end = 4.dp),
                        )
                    }

                    if (settingsStack.isNotEmpty()) {
                        SettingsScreen(
                            page = settingsStack.last(),
                            onNavigate = { settingsStack = settingsStack + it },
                            onBack = { settingsStack = settingsStack.dropLast(1) },
                            settings = settings,
                            widgets = widgets,
                            apps = apps,
                            listenerEnabled = listenerEnabled.value,
                            isDefaultLauncher = isDefaultLauncher.value,
                            actions = settingsActions,
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

                if (quickMenuOpen) {
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
                        onOpenAllSettings = { settingsStack = listOf(SettingsPage.HOME) },
                        onDismiss = { quickMenuOpen = false },
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
                        onOpenSettings = { settingsStack = listOf(SettingsPage.HOME) },
                        onDismiss = { sheetApp = null },
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
