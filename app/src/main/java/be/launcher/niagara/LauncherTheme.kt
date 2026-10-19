package be.launcher.niagara

/** Kant-en-klare combinatie van de uiterlijk-instellingen. */
data class LauncherTheme(
    val name: String,
    val description: String,
    val dim: Float,
    val clockStyle: Int,
    val iconShape: Int,
    val grayIcons: Boolean,
)

val PRESET_THEMES = listOf(
    LauncherTheme("Oceaan", "Standaard: kleurrijk en rustig", 0.30f, 0, 0, false),
    LauncherTheme("Nacht", "Extra donker voor 's avonds", 0.65f, 1, 0, false),
    LauncherTheme("Minimalist", "Lichte klok en grijze vierkante iconen", 0.45f, 1, 2, true),
    LauncherTheme("Zacht", "Afgeronde vierkante iconen", 0.35f, 0, 1, false),
    LauncherTheme("Retro", "Monospace klok en grijze iconen", 0.40f, 5, 2, true),
    LauncherTheme("Editie", "Serif klok met afgeronde iconen", 0.30f, 4, 1, false),
)
