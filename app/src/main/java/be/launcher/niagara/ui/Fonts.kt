package be.launcher.niagara.ui

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

/** Lichte, ruime tekst zoals bij Niagara. */
fun lightText(size: TextUnit, color: Color = Color.White) = TextStyle(
    color = color,
    fontSize = size,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Light,
    letterSpacing = 0.2.sp,
)
