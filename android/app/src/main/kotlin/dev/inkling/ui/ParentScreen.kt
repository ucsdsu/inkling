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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ParentScreen(
    state: ParentState,
    onKidsMode: (Boolean) -> Unit,
    onApp: (AppRow, Boolean, Int) -> Unit,
    onCeiling: (Int) -> Unit,
    onOpenBooxHome: () -> Unit,
    onFixSetup: (String) -> Unit,
    onSpeechTest: () -> Unit,
    onExportSpike: () -> Unit,
    onChangePin: () -> Unit,
    onLock: () -> Unit,
) {
    var tab by remember { mutableStateOf("Today") }
    val s = state.settings ?: return
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹ Lock", color = InklingColors.Ink2, modifier = Modifier.clickable(onClick = onLock))
            Text("Parent", color = InklingColors.Ink2)
            Spacer(Modifier.width(1.dp))
        }
        Spacer(Modifier.height(12.dp))
        state.setupProblems.forEach { p ->
            Row(Modifier.fillMaxWidth().background(InklingColors.Rust, RoundedCornerShape(8.dp)).clickable { onFixSetup(p) }.padding(10.dp)) {
                Text("$p. Tap to fix.", color = InklingColors.Ink)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(
            Modifier.fillMaxWidth().border(2.dp, InklingColors.Ink, RoundedCornerShape(10.dp)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Kids mode", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(if (s.kidsModeOn) "On. Only the apps switched on below can open." else "Off. Device returns to the normal Boox home screen for you.", fontSize = 12.sp, color = InklingColors.Ink2)
            }
            Switch(checked = s.kidsModeOn, onCheckedChange = onKidsMode)
        }
        if (!s.kidsModeOn) {
            Spacer(Modifier.height(8.dp))
            BigButton("Open Boox home", filled = false, onClick = onOpenBooxHome)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Today", "Reading", "Apps", "Rules").forEach { t ->
                Box(
                    Modifier.border(1.5.dp, if (tab == t) InklingColors.Ink else InklingColors.Ink3, RoundedCornerShape(14.dp))
                        .background(if (tab == t) InklingColors.Paper2 else InklingColors.Paper, RoundedCornerShape(14.dp))
                        .clickable { tab = t }.padding(horizontal = 10.dp, vertical = 5.dp),
                ) { Text(t, fontSize = 12.sp, color = if (tab == t) InklingColors.Ink else InklingColors.Ink2) }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            "Today" -> TodayTab(state)
            "Reading" -> ReadingTab(state.reading)
            "Apps" -> AppsTab(state, onApp)
            "Rules" -> RulesTab(s.deviceCeilingMinutes, onCeiling, onChangePin, onSpeechTest, onExportSpike)
        }
    }
}

@Composable
private fun TodayTab(state: ParentState) {
    Column {
        KeyValue("Screen time today", "${state.totalMinutes} min")
        Spacer(Modifier.height(10.dp))
        Text("BY APP", fontSize = 11.sp, color = InklingColors.Ink2)
        val max = (state.today.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)
        state.today.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.label, Modifier.width(92.dp), fontSize = 13.sp)
                Box(Modifier.weight(1f).height(10.dp).background(InklingColors.Paper2, RoundedCornerShape(5.dp))) {
                    Box(Modifier.fillMaxWidth(r.minutes.toFloat() / max).height(10.dp).background(InklingColors.Ink, RoundedCornerShape(5.dp)))
                }
                Text("${r.minutes}", Modifier.width(44.dp), fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
private fun ReadingTab(r: ReadingState) {
    Column {
        Text("LAST BOOKS", fontSize = 11.sp, color = InklingColors.Ink2)
        r.books.forEach { b -> KeyValue(b.title, readingRowValue(b)) }
        Spacer(Modifier.height(12.dp))
        Text("WORDS HE MISSED TWICE", fontSize = 11.sp, color = InklingColors.Ink2)
        Text(
            if (r.missedTwice.isEmpty()) "none yet" else r.missedTwice.joinToString(" · "),
            fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = InklingColors.Ink,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("READ-ALOUD ATTEMPTS TODAY", fontSize = 11.sp, color = InklingColors.Ink2)
        KeyValue("Lines he read", "${r.attemptsToday}")
        KeyValue("Average accuracy", r.accuracyToday?.let { "${Math.round(it * 100)}%" } ?: "—")
    }
}

@Composable
private fun AppsTab(state: ParentState, onApp: (AppRow, Boolean, Int) -> Unit) {
    LazyColumn {
        items(state.apps, key = { it.packageName }) { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(a.label, Modifier.weight(1f), fontSize = 13.sp, color = if (a.enabled) InklingColors.Ink else InklingColors.Ink3)
                Stepper(value = a.cap, enabled = a.enabled, onChange = { onApp(a, a.enabled, it) })
                Spacer(Modifier.width(10.dp))
                Switch(checked = a.enabled, onCheckedChange = { onApp(a, it, a.cap) })
            }
        }
        item { Text("Switch on = tile on the home screen. Minutes = daily cap, 0 = no cap.", fontSize = 11.sp, color = InklingColors.Ink3, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun RulesTab(ceiling: Int, onCeiling: (Int) -> Unit, onChangePin: () -> Unit, onSpeechTest: () -> Unit, onExportSpike: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Whole-device daily ceiling", fontSize = 13.sp)
            Stepper(value = ceiling, enabled = true, onChange = onCeiling)
        }
        KeyValue("Warning before a cap", "2 min")
        KeyValue("Games unlock after", "off")
        KeyValue("Quiet hours", "7:30 pm – 7:00 am")
        KeyValue("Children", "Cove")
        Spacer(Modifier.height(12.dp))
        BigButton("Change PIN", filled = false, onClick = onChangePin)
        BigButton("Speech test", filled = false, onClick = onSpeechTest)
        BigButton("Export speech test CSV", filled = false, onClick = onExportSpike)
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k, fontSize = 13.sp, color = InklingColors.Ink2)
        Text(v, fontSize = 13.sp)
    }
}

@Composable
private fun Stepper(value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    val ink = if (enabled) InklingColors.Ink else InklingColors.Ink3
    Row(Modifier.border(1.5.dp, ink, RoundedCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        Text("−", Modifier.clickable(enabled) { onChange(value - 5) }.background(InklingColors.Paper2).padding(horizontal = 10.dp, vertical = 3.dp), color = ink)
        Text("$value", Modifier.width(44.dp).padding(vertical = 3.dp), fontSize = 12.sp, color = ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("+", Modifier.clickable(enabled) { onChange(value + 5) }.background(InklingColors.Paper2).padding(horizontal = 10.dp, vertical = 3.dp), color = ink)
    }
}
