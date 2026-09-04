package dev.inkling.spike

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.inkling.core.DiffResult
import dev.inkling.core.ReadingDiff
import dev.inkling.data.Repo
import dev.inkling.data.SpikeRow
import kotlinx.coroutines.launch

/**
 * Gate 0. Shows one line, listens, diffs, and asks the parent for a verdict.
 * Rows go to the SpikeRow table; Task 8 adds a CSV export button on the parent screen.
 */
@Composable
fun SpikeScreen(repo: Repo) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val recognizer = remember { Recognizer(ctx) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    var index by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Tap Listen, then read the line.") }
    var transcript by remember { mutableStateOf("") }
    var confidence by remember { mutableStateOf(-1f) }
    var result by remember { mutableStateOf<DiffResult?>(null) }
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val line = SpikeLines.lines[index % SpikeLines.lines.size]

    fun record(verdict: String) {
        val r = result ?: return
        scope.launch {
            repo.addSpike(SpikeRow(expected = line, transcript = transcript, confidence = confidence,
                flagged = r.missed.joinToString(" "), verdict = verdict, at = System.currentTimeMillis()))
            index += 1; result = null; transcript = ""; status = "Saved. Next line."
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Speech test  ${index + 1} of ${SpikeLines.lines.size}", fontSize = 14.sp)
        Text(line, fontSize = 30.sp)
        if (!recognizer.available) Text("This device has no speech recognizer.")
        if (!granted) Button(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone") }
        Button(enabled = granted && recognizer.available, onClick = {
            status = "Listening…"
            recognizer.listen(
                onResult = { t, c ->
                    // The recognizer reports -1 when it gives no confidence at all. The spike exists
                    // to measure how often that happens, so a missing score is scored as 1f and the
                    // line is still diffed and flagged. The raw -1 is what gets stored and shown.
                    transcript = t; confidence = c
                    result = ReadingDiff.score(line, t, if (c < 0) 1f else c)
                    status = "Heard: \"$t\"  conf=" + if (c < 0) "n/a" else "%.2f".format(c)
                },
                onError = { status = it },
            )
        }) { Text("Listen") }
        Text(status)
        result?.let { r ->
            Text(if (r.lowConfidence) "Low confidence, nothing flagged" else "Flagged: ${r.missed.ifEmpty { listOf("none") }.joinToString(" ")}")
            Text("Did he actually read it right?", fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { record("correct") }) { Text("Read it right") }
                Button(onClick = { record("wrong") }) { Text("Missed a word") }
                Button(onClick = { record("unclear") }) { Text("Unclear") }
            }
        }
    }
}
