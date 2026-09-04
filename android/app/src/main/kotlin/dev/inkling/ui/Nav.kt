package dev.inkling.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.inkling.BuildConfig
import dev.inkling.service.SetupCheck
import dev.inkling.spike.SpikeScreen
import dev.inkling.ui.onboarding.FirstShelfScreen
import dev.inkling.ui.onboarding.OnboardingViewModel
import dev.inkling.ui.onboarding.PlacementScreen
import dev.inkling.ui.onboarding.ProfileScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun InklingNav(
    home: HomeViewModel,
    parent: ParentViewModel,
    reader: ReaderViewModel,
    onboarding: OnboardingViewModel,
    homePresses: StateFlow<Int>,
) {
    val nav = rememberNavController()
    val presses by homePresses.collectAsState()
    // A device with no child starts in onboarding. Blank paper until the row is read, because
    // guessing "home" first would flash a kid screen with nobody's name on it.
    var start by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { start = if (onboarding.hasChild()) "home" else ONBOARD_PROFILE }
    LaunchedEffect(presses, start) {
        if (presses > 0 && start != null) nav.navigate("home") { popUpTo("home") { inclusive = true } }
    }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val startDestination = start ?: return Box(Modifier.fillMaxSize().background(InklingColors.Paper))
    NavHost(nav, startDestination = startDestination,
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
                onRead = { nav.navigate("shelf") },
                onGearLongPress = { nav.navigate("pin") },
            )
        }
        composable("done/{label}") { back ->
            DoneScreen(appLabel = back.arguments?.getString("label") ?: "", onPickBook = { nav.navigate("shelf") }, onBack = { nav.popBackStack() })
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
                onAddChild = { nav.navigate("onboard/profile") },
                onSwitchChild = { parent.switchChild(it) },
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
        composable("shelf") {
            val rows by reader.shelf.collectAsState()
            LaunchedEffect(Unit) { reader.loadShelf() }
            ShelfScreen(rows = rows, onOpen = { nav.navigate("reader/$it") }, onBack = { nav.popBackStack() })
        }
        composable("reader/{bookId}") { back ->
            val s by reader.state.collectAsState()
            val bookId = back.arguments?.getString("bookId").orEmpty()
            LaunchedEffect(bookId) { reader.open(bookId) }
            // The mic is asked for on the first tap, not at install. A refusal says why on the
            // card the child already knows, and never calls it his mistake.
            // Hardware Back pops the route without touching our back arrow, which left the TTS
            // talking into an empty screen and lost the page from the reading log. A rotation
            // disposes this route too, and that is not leaving the book.
            val activity = ctx.activity()
            DisposableEffect(bookId) {
                onDispose { if (activity?.isChangingConfigurations != true) reader.leave() }
            }
            val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) reader.listen() else reader.micDenied()
            }
            // Forward off the last page is the finish: log that page, then back to the shelf.
            val forward: () -> Unit = {
                if (s.page >= (s.book?.pages?.size ?: 1) - 1) {
                    reader.finish()
                    nav.popBackStack()
                } else {
                    reader.turn(1)
                }
            }
            ReaderScreen(
                state = s,
                onBack = { reader.finish(); nav.popBackStack() },
                onTurn = { if (it > 0) forward() else reader.turn(it) },
                onNext = forward,
                onSpeak = { reader.speakLine() },
                onListen = {
                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) reader.listen() else askMic.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStopListening = { reader.stopListening() },
                onReplayChunks = { reader.replayChunks() },
                onDebugFake = if (BuildConfig.DEBUG) ({ reader.fakeRecognition() }) else null,
            )
        }
        composable(ONBOARD_PROFILE) {
            val s by onboarding.state.collectAsState()
            LaunchedEffect(Unit) { onboarding.resetIfComplete() }
            ProfileScreen(
                state = s,
                onName = { onboarding.setName(it) },
                onAge = { onboarding.setAge(it) },
                onInterest = { onboarding.toggleInterest(it) },
                onNext = { nav.navigate("onboard/placement") },
                // No back arrow on first launch: there is nothing behind onboarding to go back to.
                onBack = if (nav.previousBackStackEntry == null) null else ({ nav.popBackStack(); Unit }),
            )
        }
        composable("onboard/placement") {
            val s by onboarding.state.collectAsState()
            // The reader's mic pattern: asked for on arrival, and a refusal is said in the parent's
            // words, never as something the child did.
            var micDenied by remember { mutableStateOf(false) }
            val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) onboarding.listen() else micDenied = true
            }
            val listen: () -> Unit = {
                val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) onboarding.listen() else if (!micDenied) askMic.launch(Manifest.permission.RECORD_AUDIO)
            }
            // The mic opens itself on each new word, and again on the one retry an unclear buys.
            LaunchedEffect(s.wordIndex, s.retried) { if (s.stage == null && !micDenied) listen() }
            LaunchedEffect(s.stage) { if (s.stage != null) nav.navigate("onboard/done") }
            DisposableEffect(Unit) { onDispose { onboarding.stopListening() } }
            PlacementScreen(
                state = s, micDenied = micDenied, onListen = listen,
                onSkip = { onboarding.skip() }, onBack = { nav.popBackStack() },
            )
        }
        composable("onboard/done") {
            val s by onboarding.state.collectAsState()
            FirstShelfScreen(
                state = s,
                onGo = {
                    onboarding.finish {
                        nav.navigate("home") { popUpTo(nav.graph.id) { inclusive = true } }
                    }
                },
            )
        }
        composable("spike") { SpikeScreen(dev.inkling.InklingApp.instance.repo) }
    }
}

/** Where onboarding starts. Named so the parent screen's "+ add child" cannot misspell it. */
const val ONBOARD_PROFILE = "onboard/profile"

/** Sends the parent to a new child's profile. What "+ add child" calls. */
fun NavHostController.goOnboard() = navigate(ONBOARD_PROFILE)

/** The activity behind a composition's context, through however many wrappers. */
private fun Context.activity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
