package be.launcher.niagara.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF55C7F7)

/**
 * Uitleg voor meldingstoegang. Stap 0 legt uit wat je krijgt, stap 1 helpt als Android de
 * toegang blokkeert voor een zelf geïnstalleerde app ("Beperkte instellingen").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAccessSheet(
    stage: Int,
    onEnable: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetBackgroundDeep,
        contentColor = SheetTextColor,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(top = 40.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (stage == 0) {
                Text(
                    "Lees meldingen direct op je startscherm",
                    style = lightText(30.sp, SheetTextColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardBackground)
                        .padding(24.dp),
                ) {
                    Text("Berichten · nu", style = lightText(18.sp, SheetTextColor, FontWeight.Medium))
                    Text(
                        "Anna · Zin om straks af te spreken?",
                        style = lightText(16.sp, Color(0xFF9FB6C2), FontWeight.Normal),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    "Zo zie je wat er binnenkomt zonder je telefoon te ontgrendelen of de meldingenbalk te openen. " +
                        "Alles blijft op dit toestel: Launcher heeft geen internettoegang.",
                    style = lightText(16.sp, Color(0xFF9FB6C2), FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
                PillButton("Inschakelen", onEnable)
                TextAction("Uitgeschakeld houden", onDismiss)
            } else {
                Text(
                    "Beperkte instellingen",
                    style = lightText(30.sp, SheetTextColor),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                Text(
                    "Android blokkeert meldingstoegang soms voor apps die je zelf hebt geïnstalleerd. " +
                        "Open de app-info van Launcher, tik rechtsboven op de drie puntjes en kies " +
                        "\"Beperkte instellingen toestaan\". Probeer het daarna opnieuw.",
                    style = lightText(16.sp, Color(0xFF9FB6C2), FontWeight.Normal),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 28.dp),
                )
                PillButton("App-info openen", onOpenAppInfo)
                TextAction("Opnieuw proberen", onRetry)
            }
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(36.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(CardBackground)
            .border(3.dp, Accent, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = lightText(20.sp, Accent, FontWeight.Medium))
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = lightText(20.sp, Accent, FontWeight.Medium),
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
    )
}
