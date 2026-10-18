package be.launcher.niagara.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/** Lettertypen voor de klok, allemaal gratis of ingebouwd in Android. */
enum class ClockStyle(
    private val family: FontFamily,
    private val weight: FontWeight,
    private val italic: Boolean = false,
) {
    MEDIUM(Outfit, FontWeight.Medium),
    LIGHT(Outfit, FontWeight.Light),
    BOLD(Outfit, FontWeight.Bold),
    ITALIC(Outfit, FontWeight.Normal, italic = true),
    SERIF(FontFamily.Serif, FontWeight.Normal),
    MONO(FontFamily.Monospace, FontWeight.Medium),
    HANDWRITTEN(FontFamily.Cursive, FontWeight.Normal);

    fun textStyle(size: TextUnit, color: Color = Color.White) = TextStyle(
        color = color,
        fontSize = size,
        fontFamily = family,
        fontWeight = weight,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
    )

    companion object {
        fun fromId(id: Int): ClockStyle = values().getOrElse(id) { MEDIUM }
    }
}
