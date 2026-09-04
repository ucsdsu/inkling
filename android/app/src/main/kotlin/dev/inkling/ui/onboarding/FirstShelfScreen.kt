package dev.inkling.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.books.STAGES
import dev.inkling.ui.InklingColors
import dev.inkling.ui.stageLabel

/** Every stage in [STAGES] ships one decodable, so this is what "already on the device" means. */
private const val STARTER_BOOKS = 6

/**
 * Step 3: what the read decided, and what he can open right now. The generated-books block from
 * the prototype is phase 2, so it is one honest line here instead of a promise with a price tag.
 */
@Composable
fun FirstShelfScreen(state: OnboardingState, onGo: () -> Unit) {
    val name = state.name.trim()
    val label = stageLabel(STAGES.getOrElse(state.stage ?: 0) { STAGES.first() })
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        OnboardStrip(onBack = null)
        StepLabel("3 of 3 · first shelf")
        Text("$name starts at $label.", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = InklingColors.Ink)
        val recap = recapLine(state.outcomes)
        if (recap.isNotEmpty()) {
            Text(recap, fontSize = 14.sp, color = InklingColors.Ink2, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().border(1.5.dp, InklingColors.Ink3, RoundedCornerShape(10.dp)).padding(12.dp)) {
            Text("Already on the device", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = InklingColors.Ink)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$STARTER_BOOKS decodables", fontSize = 13.sp, color = InklingColors.Ink)
                Text("starter pack", fontSize = 13.sp, color = InklingColors.Ink2)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "New books made just for $name come in a later update.",
            fontSize = 13.sp, color = InklingColors.Ink2,
        )
        Spacer(Modifier.weight(1f))
        WideButton("Go to $name's shelf ›", filled = true, onClick = onGo)
    }
}
