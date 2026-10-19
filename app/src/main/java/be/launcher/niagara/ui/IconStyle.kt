package be.launcher.niagara.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape

/** Vorm en kleur van de app-iconen, gekozen onder Thema's. */
data class IconStyle(val shape: Int = 0, val gray: Boolean = false)

val LocalIconStyle = compositionLocalOf { IconStyle() }

val ICON_SHAPE_NAMES = listOf("Cirkel", "Afgerond vierkant", "Vierkant")

fun iconShapeFor(id: Int): Shape = when (id) {
    1 -> RoundedCornerShape(percent = 28)
    2 -> RoundedCornerShape(percent = 12)
    else -> CircleShape
}

val GrayFilter: ColorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
