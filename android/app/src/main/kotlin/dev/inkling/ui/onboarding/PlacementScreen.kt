package dev.inkling.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.core.Placement
import dev.inkling.ui.Andika
import dev.inkling.ui.InklingColors
import dev.inkling.ui.MIC_DENIED

/**
 * Step 2: the placement read. One word at a time, big, in the same face the books use. The mic
 * opens by itself so a four-year-old holding the tablet has nothing to press, and "Skip" is a
 * first-class answer rather than a way out.
 *
 * @param micDenied the parent said no to the microphone; skipping is then the whole read
 */
@Composable
fun PlacementScreen(
    state: OnboardingState,
    micDenied: Boolean,
    onListen: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
) {
    val word = Placement.WORDS.getOrNull(state.wordIndex)?.word.orEmpty()
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        OnboardStrip(onBack)
        StepLabel("2 of 3 · hand the tablet to ${state.name.trim()}")
        Text("Read this word.", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = InklingColors.Ink)
        Text(
            "It's fine to skip any. This finds a starting shelf, nothing else.",
            fontSize = 14.sp, color = InklingColors.Ink2, modifier = Modifier.padding(top = 4.dp),
        )
        Column(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(word, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 40.sp, color = InklingColors.Ink)
            Spacer(Modifier.height(12.dp))
            Box(Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                val line = when {
                    micDenied -> MIC_DENIED
                    state.notice != null -> state.notice
                    state.listening -> "Listening…"
                    else -> resultLine(state).orEmpty()
                }
                Text(line, fontSize = 15.sp, color = InklingColors.Ink2, textAlign = TextAlign.Center)
            }
        }
        Text(
            "word ${(state.wordIndex + 1).coerceAtMost(Placement.WORDS.size)} of ${Placement.WORDS.size}",
            fontSize = 11.sp, color = InklingColors.Ink3, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        WideButton(if (state.listening) "Listening…" else "Read it", filled = true, enabled = !micDenied, onClick = onListen)
        WideButton("Skip", filled = false, onClick = onSkip)
    }
}
