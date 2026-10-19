package be.launcher.niagara

/** Kant-en-klare combinatie van de uiterlijk-instellingen. */
data class LauncherTheme(
    val name: String,
    val description: String,
    val dim: Float,
    val clockStyle: Int,
    val iconShape: Int,
    val grayIcons: Boolean,
    val background: Int,
)

val PRESET_THEMES = listOf(
    LauncherTheme("Oceaan", "Standaard: kleurrijk en rustig", 0.15f, 0, 0, false, 1),
    LauncherTheme("Nacht", "Zwart met een lichte klok", 0.30f, 1, 0, false, 4),
    LauncherTheme("Minimalist", "Donker verloop, grijze vierkante iconen", 0.15f, 1, 2, true, 0),
    LauncherTheme("Zacht", "Afgeronde vierkante iconen op zonsondergang", 0.15f, 0, 1, false, 3),
    LauncherTheme("Retro", "Monospace klok, grijze iconen op zwart", 0.20f, 5, 2, true, 4),
    LauncherTheme("Editie", "Serif klok op middernacht", 0.15f, 4, 1, false, 2),
    LauncherTheme("Omtrek", "Grote omtrek-weekdag met lijntekening-iconen", 0.05f, 7, 3, false, 0),
)
