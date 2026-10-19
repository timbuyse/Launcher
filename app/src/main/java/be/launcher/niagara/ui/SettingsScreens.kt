package be.launcher.niagara.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import be.launcher.niagara.AppEntry
import be.launcher.niagara.LauncherBackgrounds
import be.launcher.niagara.LauncherSettings
import be.launcher.niagara.PRESET_THEMES
import be.launcher.niagara.WidgetController
import kotlin.math.roundToInt

enum class SettingsPage { HOME, PRODUCTIVITY, THEMES, APPEARANCE, ICON_SHAPES, BACKGROUNDS, HIDE_APPS, ADVANCED }

/** Wat de instellingenpagina's van de activity nodig hebben. */
class SettingsActions(
    val onOpenClockStyle: () -> Unit,
    val onAddWidget: () -> Unit,
    val onEditFavorites: () -> Unit,
    val onNotificationAccess: () -> Unit,
    val onDefaultLauncher: () -> Unit,
    val onQuickLockChanged: (Boolean) -> Unit,
    val onRestart: () -> Unit,
    val onPickBackground: () -> Unit,
)

private val ScreenBackground = Color(0xFF02171E)
private val Accent = Color(0xFF55C7F7)
private val Muted = Color(0xFF9FB6C2)
private val Ink = Color(0xFFCDEBFF)

@Composable
fun SettingsScreen(
    page: SettingsPage,
    onNavigate: (SettingsPage) -> Unit,
    onBack: () -> Unit,
    settings: LauncherSettings,
    widgets: WidgetController,
    apps: List<AppEntry>,
    listenerEnabled: Boolean,
    isDefaultLauncher: Boolean,
    actions: SettingsActions,
) {
    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .pointerInput(Unit) { detectTapGestures { } }
            .safeDrawingPadding(),
    ) {
        when (page) {
            SettingsPage.HIDE_APPS -> HideAppsPage(settings, apps)
            else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 48.dp)) {
                when (page) {
                    SettingsPage.HOME -> HomePage(onNavigate)
                    SettingsPage.PRODUCTIVITY -> ProductivityPage(
                        settings, widgets, listenerEnabled, onNavigate, actions,
                    )
                    SettingsPage.THEMES -> ThemesPage(settings, onNavigate)
                    SettingsPage.APPEARANCE -> AppearancePage(settings, onNavigate, actions)
                    SettingsPage.ICON_SHAPES -> IconShapesPage(settings)
                    SettingsPage.BACKGROUNDS -> BackgroundsPage(settings, actions)
                    SettingsPage.ADVANCED -> AdvancedPage(isDefaultLauncher, actions)
                    SettingsPage.HIDE_APPS -> Unit
                }
            }
        }
    }
}

// ---------- pagina's ----------

@Composable
private fun HomePage(onNavigate: (SettingsPage) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 72.dp, bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(percent = 30)).background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("L", style = lightText(52.sp, Color(0xFF05303F), FontWeight.Medium))
        }
        Text(
            "Launcher instellingen",
            style = lightText(28.sp, Ink),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
    SettingRow(Icons.Outlined.Star, "Productiviteit", "Blijf efficiënt en georganiseerd") {
        onNavigate(SettingsPage.PRODUCTIVITY)
    }
    SettingRow(Icons.Outlined.Create, "Thema's", "Geef je startscherm een persoonlijk tintje") {
        onNavigate(SettingsPage.THEMES)
    }
    SettingRow(Icons.Outlined.MoreVert, "Geavanceerd") { onNavigate(SettingsPage.ADVANCED) }
}

@Composable
private fun ProductivityPage(
    settings: LauncherSettings,
    widgets: WidgetController,
    listenerEnabled: Boolean,
    onNavigate: (SettingsPage) -> Unit,
    actions: SettingsActions,
) {
    PageTitle("Productiviteit")

    SectionLabel("Directe toegang")
    SettingSwitch(
        Icons.Outlined.PlayArrow, "Mediaspeler", "Bedien muziek en video's vanaf je startscherm",
        settings.showMedia, settings::updateShowMedia,
    )
    SettingRow(
        Icons.Outlined.Notifications, "Meldingen op het startscherm",
        if (listenerEnabled) "Ingeschakeld" else "Niet ingeschakeld",
        onClick = actions.onNotificationAccess,
    )

    SectionLabel("Apps organiseren")
    SettingRow(
        Icons.Outlined.Close, "Apps verbergen",
        if (settings.hiddenApps.isEmpty()) "Verberg apps in je app-lijst" else "${settings.hiddenApps.size} verborgen",
    ) { onNavigate(SettingsPage.HIDE_APPS) }
    SettingSwitch(
        Icons.Outlined.Star, "Vaak gebruikte apps weergeven", "Directe toegang tot veelgebruikte apps onder je favorieten",
        settings.showFrequent, settings::updateShowFrequent,
    )
    SettingSwitch(
        Icons.Outlined.Refresh, "Sorteer alle apps op gebruik", "Prioriteer de meest gebruikte apps voor elke letter",
        settings.sortByUsage, settings::updateSortByUsage,
    )

    SectionLabel("Snelkoppelingen & gebaren")
    SettingSwitch(
        Icons.Outlined.Lock, "Snel vergrendelen", "Dubbeltik op het alfabet om je telefoon te vergrendelen",
        settings.quickLock, actions.onQuickLockChanged,
    )

    SectionLabel("Geavanceerd")
    SettingRow(Icons.Outlined.Add, "Aangepaste widget toevoegen", onClick = actions.onAddWidget)
    widgets.widgetIds.toList().forEach { id ->
        SettingRow(Icons.Outlined.Delete, "Verwijder: ${widgets.label(id)}") { widgets.remove(id) }
    }
    SettingRow(Icons.Outlined.Star, "Favorieten bewerken", onClick = actions.onEditFavorites)
    SettingSwitch(
        Icons.Outlined.Info, "Batterijpercentage weergeven", null,
        settings.showBattery, settings::updateShowBattery,
    )
    SettingSwitch(
        Icons.Outlined.Refresh, "Startscherm rotatie toestaan", "Wanneer het apparaat wordt gedraaid",
        settings.allowRotation, settings::updateAllowRotation,
    )
    SettingSwitch(
        Icons.Outlined.Notifications, "Haptische feedback toestaan", "Trillen bij letters en lang indrukken",
        settings.haptics, settings::updateHaptics,
    )
}

@Composable
private fun ThemesPage(settings: LauncherSettings, onNavigate: (SettingsPage) -> Unit) {
    PageTitle("Thema's")
    SettingRow(Icons.Outlined.Create, "Huidig thema bewerken", "Verfijn je instellingen") {
        onNavigate(SettingsPage.APPEARANCE)
    }
    SectionLabel("Kant-en-klare thema's")
    PRESET_THEMES.forEach { theme ->
        val active = settings.dim == theme.dim && settings.clockStyle == theme.clockStyle &&
            settings.iconShape == theme.iconShape && settings.grayIcons == theme.grayIcons &&
            settings.background == theme.background
        SettingRow(
            icon = if (active) Icons.Outlined.Check else null,
            title = theme.name,
            subtitle = theme.description,
        ) { settings.applyTheme(theme) }
    }
}

@Composable
private fun AppearancePage(settings: LauncherSettings, onNavigate: (SettingsPage) -> Unit, actions: SettingsActions) {
    PageTitle("Huidig thema")
    SettingRow(
        Icons.Outlined.Info, "Klok stijl", ClockStyle.fromId(settings.clockStyle).name.lowercase()
            .replaceFirstChar { it.uppercase() },
        onClick = actions.onOpenClockStyle,
    )
    SettingRow(
        Icons.Outlined.Star, "App pictogrammen", ICON_SHAPE_NAMES.getOrElse(settings.iconShape) { "Cirkel" },
    ) { onNavigate(SettingsPage.ICON_SHAPES) }
    SettingRow(
        Icons.Outlined.Home, "Achtergrond", LauncherBackgrounds.names.getOrElse(settings.background) { "Donker verloop" },
    ) { onNavigate(SettingsPage.BACKGROUNDS) }
    SettingSwitch(
        Icons.Outlined.Home, "App-iconen tonen", "Zet uit voor een lijst met alleen tekst",
        settings.showIcons, settings::updateShowIcons,
    )
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
        Text(
            "Verduistering van de achtergrond: ${(settings.dim * 100).roundToInt()}%",
            style = lightText(17.sp, Ink, FontWeight.Normal),
        )
        Slider(
            value = settings.dim,
            onValueChange = { settings.updateDim((it * 20).roundToInt() / 20f) },
            valueRange = 0f..0.8f,
            colors = SliderDefaults.colors(
                thumbColor = Accent,
                activeTrackColor = Accent,
                inactiveTrackColor = Color(0xFF0B3A4A),
            ),
        )
    }
}

@Composable
private fun IconShapesPage(settings: LauncherSettings) {
    PageTitle("App pictogrammen")
    ICON_SHAPE_NAMES.forEachIndexed { index, name ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .clickable { settings.updateIconShape(index) }
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index == ICON_SHAPE_OUTLINE) {
                Box(
                    Modifier.size(44.dp).border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(16.dp).border(2.dp, Color.White, RoundedCornerShape(5.dp)))
                }
            } else {
                Box(
                    Modifier.size(44.dp).clip(iconShapeFor(index)).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF05303F)))
                }
            }
            Spacer(Modifier.width(24.dp))
            Text(name, style = lightText(20.sp, Ink), modifier = Modifier.weight(1f))
            RadioButton(
                selected = settings.iconShape == index,
                onClick = { settings.updateIconShape(index) },
                colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = Muted),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    SettingSwitch(
        Icons.Outlined.Info, "Grijstinten", "Toon alle iconen zonder kleur",
        settings.grayIcons, settings::updateGrayIcons,
    )
}

@Composable
private fun BackgroundsPage(settings: LauncherSettings, actions: SettingsActions) {
    PageTitle("Achtergrond")
    LauncherBackgrounds.names.forEachIndexed { index, name ->
        val isCustom = index == LauncherBackgrounds.CUSTOM
        val isSystem = index == LauncherBackgrounds.SYSTEM
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardBackground)
                .clickable {
                    if (isCustom) actions.onPickBackground() else settings.updateBackground(index)
                }
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val swatchColors = LauncherBackgrounds.colors(index)
                ?: if (isSystem) listOf(Color(0xFF7BC6CC), Color(0xFFBE93C5)) else listOf(Color(0xFF3B4650), Color(0xFF1C2229))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(swatchColors)),
                contentAlignment = Alignment.Center,
            ) {
                if (isCustom) Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White)
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = lightText(20.sp, Ink))
                val hint = when {
                    isSystem -> "De wallpaper van je telefoon"
                    isCustom -> "Kies een foto uit je galerij"
                    else -> null
                }
                if (hint != null) Text(hint, style = lightText(14.sp, Muted, FontWeight.Normal))
            }
            RadioButton(
                selected = settings.background == index,
                onClick = { if (isCustom) actions.onPickBackground() else settings.updateBackground(index) },
                colors = RadioButtonDefaults.colors(selectedColor = Accent, unselectedColor = Muted),
            )
        }
    }
}

@Composable
private fun HideAppsPage(settings: LauncherSettings, apps: List<AppEntry>) {
    Column(Modifier.fillMaxSize()) {
        PageTitle("Apps verbergen")
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 48.dp)) {
            items(apps, key = { it.key }) { app ->
                val hidden = app.key in settings.hiddenApps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clickable { settings.toggleHidden(app.key) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = hidden,
                        onCheckedChange = { settings.toggleHidden(app.key) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Accent,
                            uncheckedColor = Muted,
                            checkmarkColor = Color(0xFF05303F),
                        ),
                    )
                    Spacer(Modifier.width(12.dp))
                    AppIcon(app, 40.dp)
                    Spacer(Modifier.width(20.dp))
                    Text(app.label, style = lightText(18.sp, Ink, FontWeight.Normal), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AdvancedPage(isDefaultLauncher: Boolean, actions: SettingsActions) {
    val context = LocalContext.current
    var licensesOpen by remember { mutableStateOf(false) }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }

    PageTitle("Geavanceerd")
    SettingRow(Icons.Outlined.Info, "App info", version?.let { "Versie $it" }) {
        runCatching {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    SettingRow(
        Icons.Outlined.Home, "Wijzig je standaard launcher",
        if (isDefaultLauncher) "Launcher is nu je startscherm" else "Launcher is nog niet je startscherm",
        onClick = actions.onDefaultLauncher,
    )
    SettingRow(Icons.Outlined.Delete, "Verwijder launcher") {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    SettingRow(Icons.Outlined.Refresh, "Herstart launcher", "Heb je geprobeerd het aan en uit te zetten?", onClick = actions.onRestart)
    SettingRow(null, "Opensource-licenties") { licensesOpen = !licensesOpen }
    if (licensesOpen) {
        Text(
            "Outfit lettertype, © 2021 The Outfit Project Authors (https://github.com/Outfitio/Outfit-Fonts). " +
                "Gebruikt onder de SIL Open Font License, versie 1.1. De volledige tekst staat in licenses/OFL-Outfit.txt in de broncode.",
            style = lightText(14.sp, Muted, FontWeight.Normal),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

// ---------- bouwstenen ----------

@Composable
private fun PageTitle(text: String) {
    Box(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 24.dp)) {
        Text(
            text,
            style = lightText(28.sp, Ink, FontWeight.Normal),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(36.dp))
                .background(CardBackground)
                .padding(vertical = 18.dp),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = lightText(17.sp, Accent, FontWeight.Normal),
        modifier = Modifier.padding(start = 88.dp, top = 28.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = Ink)
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = lightText(20.sp, Ink, FontWeight.Normal))
            if (subtitle != null) {
                Text(subtitle, style = lightText(15.sp, Muted, FontWeight.Normal), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            if (icon != null) Icon(icon, contentDescription = null, tint = Ink)
        }
        Spacer(Modifier.width(24.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = lightText(20.sp, Ink, FontWeight.Normal))
            if (subtitle != null) {
                Text(subtitle, style = lightText(15.sp, Muted, FontWeight.Normal), modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF05303F),
                checkedTrackColor = Accent,
                uncheckedThumbColor = Color(0xFF05303F),
                uncheckedTrackColor = Muted,
                uncheckedBorderColor = Muted,
            ),
        )
    }
}
