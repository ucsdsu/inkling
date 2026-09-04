package dev.inkling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * @param mode [PinMode.UNLOCK] checks the stored PIN, [PinMode.SET] takes the same 4 digits twice
 *   and only then calls [onSetPin].
 */
@Composable
fun PinScreen(mode: PinMode, tryPin: suspend (String) -> Boolean, lockoutSeconds: suspend () -> Int, onSetPin: (String) -> Unit, onUnlocked: () -> Unit, onBack: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var locked by remember { mutableIntStateOf(0) }
    var flow by remember { mutableStateOf(SetPinFlow()) }
    var message by remember { mutableStateOf(if (mode == PinMode.UNLOCK) "Enter parent PIN" else SET_PIN_FIRST) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { locked = lockoutSeconds() }

    fun submit(p: String) = scope.launch {
        if (mode == PinMode.SET) {
            when (val step = flow.submit(p)) {
                is SetPinStep.NeedConfirm -> { flow = step.flow; message = step.flow.prompt; pin = "" }
                is SetPinStep.Mismatch -> { flow = step.flow; message = SET_PIN_MISMATCH; pin = "" }
                is SetPinStep.Done -> { onSetPin(step.pin); onUnlocked() }
            }
            return@launch
        }
        if (tryPin(p)) onUnlocked() else {
            locked = lockoutSeconds()
            message = if (locked > 0) "Try again in $locked seconds" else "That's not it"
            pin = ""
        }
    }

    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("‹ Back", color = InklingColors.Ink2, modifier = Modifier.clickable(onClick = onBack).align(Alignment.Start))
        Spacer(Modifier.height(24.dp))
        Text(message, color = InklingColors.Ink2, fontSize = 16.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                Box(Modifier.size(16.dp).border(2.dp, InklingColors.Ink, CircleShape).background(if (i < pin.length) InklingColors.Ink else InklingColors.Paper, CircleShape))
            }
        }
        Spacer(Modifier.height(24.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier.size(72.dp, 60.dp)
                            .border(1.5.dp, if (k.isEmpty()) InklingColors.Paper else InklingColors.Ink2, RoundedCornerShape(8.dp))
                            .clickable(enabled = k.isNotEmpty() && locked == 0) {
                                if (k == "⌫") pin = pin.dropLast(1)
                                else if (pin.length < 4) { pin += k; if (pin.length == 4) submit(pin) }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(k, fontSize = 22.sp, color = InklingColors.Ink) }
                }
            }
        }
    }
}
