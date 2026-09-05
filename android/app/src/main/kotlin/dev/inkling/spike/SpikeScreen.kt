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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.inkling.data.Repo
import dev.inkling.speech.Recognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Gate 0. Shows one line, listens, diffs, and asks the parent for a verdict.
 * Rows go to the SpikeRow table; Task 8 adds a CSV export button on the parent screen.
 */
@Composable
fun SpikeScreen(repo: Repo) {
    val ctx = LocalContext.current
    val run: SpikeRunViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SpikeRunViewModel(repo) as T
        },
    )
    val state by run.state.collectAsState()
    val recognizer = remember { Recognizer(ctx) }
    DisposableEffect(Unit) { onDispose { recognizer.destroy(); run.cancelLiveAttempt() } }

    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val line = SpikeLines.lines[state.lineIndex]

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (state.complete) {
            val summary = summarizeSpike(state.savedRows)
            Text("Speech test complete", fontSize = 24.sp)
            if (summary.falseFlagRate == null) {
                Text("Insufficient data: this run had no parent-confirmed correct reads.")
            } else {
                Text("False flags: ${summary.falselyFlagged} of ${summary.correctReads} parent-confirmed correct reads (${"%.0f".format(summary.falseFlagRate * 100)}%).")
            }
            Text(
                if (summary.hasGateSample) "Gate sample complete: 20 correct reads measured."
                else "Gate sample incomplete: ${summary.correctReads} correct reads measured; 20 are required.",
            )
            summary.passesGate?.let { passed ->
                Text(if (passed) "Gate result: pass (10% or fewer false flags)." else "Gate result: fail (over 10% false flags).")
            }
            Text("All 20 rows were saved. Reopen this screen for a fresh run; earlier rows stay in the CSV.")
        } else {
            Text("Speech test  ${state.lineIndex + 1} of ${SpikeLines.lines.size}", fontSize = 14.sp)
            Text(line, fontSize = 30.sp)
            if (!recognizer.available) Text("This device has no speech recognizer.")
            if (!granted) Button(onClick = { ask.launch(Manifest.permission.RECORD_AUDIO) }) { Text("Allow microphone") }
            Button(enabled = granted && recognizer.available && !state.saving, onClick = {
                recognizer.cancel()
                val token = run.beginListen() ?: return@Button
                recognizer.listen(
                onResult = { t, c ->
                    run.onResult(token, t, c)
                },
                onError = { message ->
                    run.onError(token, message)
                },
                )
            }) { Text("Listen") }
            Text(state.status)
            state.pending?.let { pending ->
                val r = pending.diff
                Text(if (r.lowConfidence) "Low confidence, nothing flagged" else "Flagged: ${r.missed.ifEmpty { listOf("none") }.joinToString(" ")}")
                Text("Did he actually read it right?", fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(enabled = !state.saving, onClick = { run.record("correct") }) { Text("Read it right") }
                    Button(enabled = !state.saving, onClick = { run.record("wrong") }) { Text("Missed a word") }
                    Button(enabled = !state.saving, onClick = { run.record("unclear") }) { Text("Unclear") }
                }
            }
        }
    }
}
