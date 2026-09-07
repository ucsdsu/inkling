package dev.inkling.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.inkling.core.Recommendation
import dev.inkling.data.Child

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
    onAddChild: () -> Unit,
    onSwitchChild: (Long) -> Unit,
) {
    var tab by remember { mutableStateOf("Manage apps") }
    val s = state.settings ?: return
    Column(Modifier.fillMaxSize().background(InklingColors.Paper).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("‹ Lock", color = InklingColors.Ink2, modifier = Modifier.heightIn(min = 60.dp).clickable(onClick = onLock).padding(vertical = 16.dp))
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
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
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
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Manage apps", "Today", "Reading", "Next up", "Rules").forEach { t ->
                Box(
                    Modifier.border(1.5.dp, if (tab == t) InklingColors.Ink else InklingColors.Ink3, RoundedCornerShape(14.dp))
                        .background(if (tab == t) InklingColors.Paper2 else InklingColors.Paper, RoundedCornerShape(14.dp))
                        .heightIn(min = 48.dp).clickable { tab = t }.padding(horizontal = 12.dp, vertical = 12.dp),
                ) { Text(t, fontSize = 12.sp, color = if (tab == t) InklingColors.Ink else InklingColors.Ink2) }
            }
        }
        Spacer(Modifier.height(12.dp))
        when (tab) {
            "Today" -> TodayTab(state)
            "Reading" -> ReadingTab(state.reading)
            "Next up" -> NextUpTab(state.nextUp)
            "Manage apps" -> AppsTab(state, onApp)
            "Rules" -> RulesTab(s.deviceCeilingMinutes, state.children, state.activeChildId, onCeiling, onChangePin, onSpeechTest, onExportSpike, onAddChild, onSwitchChild)
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
private fun NextUpTab(recs: List<Recommendation>) {
    if (recs.isEmpty()) {
        Text("Nothing to suggest yet. Read a book first.", fontSize = 13.sp, color = InklingColors.Ink2)
        return
    }
    Column {
        recs.forEach { r ->
            Column(
                Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    .border(1.5.dp, InklingColors.Ink3, RoundedCornerShape(10.dp)).padding(12.dp),
            ) {
                Text(r.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(r.why, fontSize = 12.sp, color = InklingColors.Ink2)
            }
        }
    }
}

@Composable
private fun AppsTab(state: ParentState, onApp: (AppRow, Boolean, Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Choose apps", fontFamily = Andika, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Switch on an installed app to put it on your child's home screen.", fontSize = 16.sp)
        }
        if (state.apps.isEmpty()) item {
            Text("No apps found yet. Install an app from the normal device home, then return here.", fontSize = 16.sp)
        }
        items(state.apps, key = { it.packageName }) { a ->
            Column(Modifier.fillMaxWidth().border(1.5.dp, InklingColors.Ink, RoundedCornerShape(24.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    a.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(48.dp)) }
                    Column(Modifier.weight(1f)) {
                        Text(a.label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (a.enabled) "On home screen" else "Not on home screen", fontSize = 14.sp)
                    }
                    Switch(checked = a.enabled,
                        modifier = Modifier.semantics { contentDescription = "Allow ${a.label} on home screen" },
                        onCheckedChange = { onApp(a, it, a.cap) })
                }
                if (a.enabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (a.cap == 0) "No daily app limit" else "${a.cap} min per day", Modifier.weight(1f), fontSize = 16.sp)
                        Stepper(value = a.cap, enabled = true, onChange = { onApp(a, true, it) })
                    }
                }
            }
        }
        item {
            Text("Need another app? Turn Kids mode off, open the normal device home, and install it. Return here to approve it. New apps stay off until you choose them.", fontSize = 16.sp)
        }
    }
}

@Composable
private fun RulesTab(
    ceiling: Int,
    children: List<Child>,
    activeChildId: Long,
    onCeiling: (Int) -> Unit,
    onChangePin: () -> Unit,
    onSpeechTest: () -> Unit,
    onExportSpike: () -> Unit,
    onAddChild: () -> Unit,
    onSwitchChild: (Long) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Whole-device daily ceiling", fontSize = 13.sp)
            Stepper(value = ceiling, enabled = true, onChange = onCeiling)
        }
        KeyValue("Warning before a cap", "2 min")
        KeyValue("Games unlock after", "off")
        KeyValue("Quiet hours", "7:30 pm – 7:00 am")
        ChildrenRow(children, activeChildId, onAddChild, onSwitchChild)
        Spacer(Modifier.height(12.dp))
        BigButton("Change PIN", filled = false, onClick = onChangePin)
        BigButton("Speech test", filled = false, onClick = onSpeechTest)
        BigButton("Export speech test CSV", filled = false, onClick = onExportSpike)
    }
}

/**
 * Every child on the device. The active one is bold; tapping another switches to them, which is
 * the only way a second child is any use before phase 2's per-child home screens.
 */
@Composable
private fun ChildrenRow(children: List<Child>, activeChildId: Long, onAddChild: () -> Unit, onSwitchChild: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Children", fontSize = 13.sp, color = InklingColors.Ink2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            children.forEach { c ->
                val active = c.id == activeChildId
                Text(
                    c.name, fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) InklingColors.Ink else InklingColors.Ink2,
                    modifier = Modifier.clickable(enabled = !active) { onSwitchChild(c.id) },
                )
            }
            Text(
                "+ add child", fontSize = 13.sp, color = InklingColors.Ink2,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onAddChild),
            )
        }
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
        Text("−", Modifier.semantics { contentDescription = "Decrease daily minutes" }.clickable(enabled && value > 0) { onChange(value - 5) }.background(InklingColors.Paper2).padding(horizontal = 16.dp, vertical = 14.dp), color = ink)
        Text("$value", Modifier.width(44.dp).padding(vertical = 3.dp), fontSize = 12.sp, color = ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("+", Modifier.semantics { contentDescription = "Increase daily minutes" }.clickable(enabled && value < 600) { onChange(value + 5) }.background(InklingColors.Paper2).padding(horizontal = 16.dp, vertical = 14.dp), color = ink)
    }
}
