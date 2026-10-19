package be.launcher.niagara.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import be.launcher.niagara.R

/** Outfit (SIL Open Font License), een ronde geometrische sans in de buurt van Niagara. */
@OptIn(ExperimentalTextApi::class)
val Outfit = FontFamily(
    Font(R.font.outfit, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.outfit, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.outfit, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.outfit, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

val SubtleText = Color(0xFFC7C7C7)

fun lightText(
    size: TextUnit,
    color: Color = Color.White,
    weight: FontWeight = FontWeight.Light,
) = TextStyle(
    color = color,
    fontSize = size,
    fontFamily = Outfit,
    fontWeight = weight,
    letterSpacing = 0.2.sp,
)
