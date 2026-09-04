package dev.inkling.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.ui.Andika
import dev.inkling.ui.InklingColors

/**
 * Step 1: who the child is. Name, age, a few interests. No account, no email, and no reading-level
 * dropdown for the parent to guess at; the next screen finds that out from the child himself.
 *
 * @param onBack null on first launch, where there is nothing behind this screen to go back to
 */
@Composable
fun ProfileScreen(
    state: OnboardingState,
    onName: (String) -> Unit,
    onAge: (Int) -> Unit,
    onInterest: (String) -> Unit,
    onNext: () -> Unit,
    onBack: (() -> Unit)?,
) {
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        OnboardStrip(onBack)
        StepLabel("1 of 3 · about them")
        Text("Who's this for?", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = InklingColors.Ink)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            singleLine = true,
            label = { Text("name") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        StepLabel("age")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AGES.forEach { (years, label) ->
                Pill(label, on = state.age == years, onClick = { onAge(years) })
            }
        }
        Spacer(Modifier.height(14.dp))
        StepLabel("what they're into (pick a few)")
        ChipFlow(INTERESTS, state.interests, onInterest)
        Spacer(Modifier.weight(1f))
        WideButton("Next ›", filled = true, enabled = canContinue(state), onClick = onNext)
    }
}

/** The age row. "7+" is one bucket: past seven the placement read is doing the work anyway. */
private val AGES = listOf(3 to "3", 4 to "4", 5 to "5", 6 to "6", 7 to "7+")

@Composable
internal fun OnboardStrip(onBack: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (onBack != null) Text("‹ Back", color = InklingColors.Ink2, modifier = Modifier.clickable(onClick = onBack))
        else Spacer(Modifier.width(1.dp))
        Text("New child", color = InklingColors.Ink2)
        Spacer(Modifier.width(1.dp))
    }
}

@Composable
internal fun StepLabel(text: String) {
    Text(text, fontSize = 11.sp, color = InklingColors.Ink2, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun Pill(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 46.dp, height = 40.dp)
            .border(1.5.dp, if (on) InklingColors.Ink else InklingColors.Ink3, RoundedCornerShape(10.dp))
            .background(if (on) InklingColors.Paper2 else InklingColors.Paper, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 15.sp, color = if (on) InklingColors.Ink else InklingColors.Ink2) }
}

/**
 * The interest chips, wrapped by hand into rows of three. FlowRow is still experimental and this
 * list is fixed at ten, so the hand-rolled version costs nothing and cannot move under us.
 */
@Composable
private fun ChipFlow(all: List<String>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        all.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tag ->
                    val on = tag in selected
                    Box(
                        Modifier.border(1.5.dp, if (on) InklingColors.Ink else InklingColors.Ink3, RoundedCornerShape(14.dp))
                            .background(if (on) InklingColors.Paper2 else InklingColors.Paper, RoundedCornerShape(14.dp))
                            .clickable { onToggle(tag) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) { Text(tag, fontSize = 13.sp, color = if (on) InklingColors.Ink else InklingColors.Ink2) }
                }
            }
        }
    }
}

/**
 * The full-width button at the bottom of every onboarding screen. [BigButton] is 70% wide and
 * centred for the child's screens; the parent's steps want the whole line.
 */
@Composable
internal fun WideButton(label: String, filled: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val ink = if (enabled) InklingColors.Ink else InklingColors.Ink3
    Box(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .background(if (filled && enabled) ink else InklingColors.Paper, RoundedCornerShape(10.dp))
            .border(2.dp, ink, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = Andika, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            color = if (filled && enabled) InklingColors.Paper else ink,
        )
    }
}
