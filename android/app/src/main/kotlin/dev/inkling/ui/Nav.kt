package dev.inkling.ui

import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.inkling.service.SetupCheck
import dev.inkling.spike.SpikeScreen
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InklingNav(home: HomeViewModel, parent: ParentViewModel) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    NavHost(nav, startDestination = "home",
        enterTransition = { EnterTransition.None }, exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None }, popExitTransition = { ExitTransition.None }) {
        composable("home") {
            val s by home.state.collectAsState()
            // Not LaunchedEffect: coming back from a launched app does not recompose the route,
            // so the tiles kept yesterday's minutes until the process restarted.
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { home.refresh() }
            KidHome(
                state = s,
                onOpen = { t ->
                    if (t.done) nav.navigate("done/${t.label}")
                    else ctx.packageManager.getLaunchIntentForPackage(t.packageName)?.let { ctx.startActivity(it) }
                },
                onRead = { Toast.makeText(ctx, "Books come next.", Toast.LENGTH_SHORT).show() },
                onGearLongPress = { nav.navigate("pin") },
            )
        }
        composable("done/{label}") { back ->
            DoneScreen(appLabel = back.arguments?.getString("label") ?: "", onPickBook = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable("pin") {
            val s by parent.state.collectAsState()
            LaunchedEffect(Unit) { parent.refresh() }
            // Blank paper until the settings row is read. Rendering the pad early would show the
            // "choose a PIN" flow to a parent who already has one.
            when (val mode = pinMode(s)) {
                null -> Box(Modifier.fillMaxSize().background(InklingColors.Paper))
                else -> PinScreen(
                    mode = mode,
                    tryPin = { parent.tryPin(it, System.currentTimeMillis()) },
                    lockoutSeconds = { parent.lockoutRemainingSeconds(System.currentTimeMillis()) },
                    onSetPin = { parent.setPin(it) },
                    onUnlocked = { nav.navigate("parent") { popUpTo("home") } },
                    onBack = { nav.popBackStack() },
                )
            }
        }
        composable("parent") {
            val s by parent.state.collectAsState()
            LaunchedEffect(Unit) { parent.setupProblems = { SetupCheck.problems(ctx) }; parent.refresh() }
            ParentScreen(
                state = s,
                onKidsMode = { parent.setKidsMode(it) },
                onApp = { row, en, cap -> parent.setApp(row, en, cap) },
                onCeiling = { parent.setCeiling(it) },
                onOpenBooxHome = { SetupCheck.stockHomeIntent(ctx)?.let { ctx.startActivity(it) } ?: Toast.makeText(ctx, "No other home app found", Toast.LENGTH_SHORT).show() },
                onFixSetup = { ctx.startActivity(SetupCheck.fixIntent(it)) },
                onSpeechTest = { nav.navigate("spike") },
                onExportSpike = {
                    scope.launch {
                        val f = File(ctx.getExternalFilesDir(null), "spike.csv")
                        f.writeText(parent.spikeCsv())
                        Toast.makeText(ctx, "Saved ${f.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                },
                onChangePin = { nav.navigate("pin/set") },
                onLock = { nav.navigate("home") { popUpTo("home") { inclusive = true } } },
            )
        }
        composable("pin/set") {
            LaunchedEffect(Unit) { parent.refresh() }
            PinScreen(
                mode = PinMode.SET,
                tryPin = { false },
                lockoutSeconds = { 0 },
                onSetPin = { parent.setPin(it) },
                onUnlocked = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
        }
        composable("spike") { SpikeScreen(dev.inkling.InklingApp.instance.repo) }
    }
}
