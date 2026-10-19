package be.launcher.niagara.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Wordt in de activity gezet vanuit de instelling 'Haptische feedback'. */
val LocalHapticsEnabled = compositionLocalOf { true }

/** Geeft een functie terug die alleen trilt als haptische feedback aan staat. */
@Composable
fun rememberHaptics(): (HapticFeedbackType) -> Unit {
    val haptic = LocalHapticFeedback.current
    val enabled by rememberUpdatedState(LocalHapticsEnabled.current)
    return remember(haptic) {
        { type: HapticFeedbackType -> if (enabled) haptic.performHapticFeedback(type) }
    }
}
