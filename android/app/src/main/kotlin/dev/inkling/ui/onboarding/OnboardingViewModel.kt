package dev.inkling.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.inkling.core.Placement
import dev.inkling.core.WordOutcome
import dev.inkling.data.Repo
import dev.inkling.speech.Recognizer
import dev.inkling.ui.ListenSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The three onboarding screens as one piece of state: who the child is, how the placement read
 * went, and the child that came out of it. Nothing here is scored or shown to him as wrong.
 *
 * @property retried true while the current word still owes him a second attempt after an unclear
 * @property stage null until all six words are behind him; then the shelf he starts at
 * @property createdChildId set once the row exists, so tapping through twice does not add twins
 */
data class OnboardingState(
    val name: String = "",
    val age: Int = 4,
    val interests: Set<String> = emptySet(),
    val wordIndex: Int = 0,
    val outcomes: List<WordOutcome> = emptyList(),
    val listening: Boolean = false,
    val retried: Boolean = false,
    val lastOutcome: WordOutcome? = null,
    val stage: Int? = null,
    val createdChildId: Long? = null,
)

/** The chips on the profile screen. Order is the order they are shown and stored in. */
val INTERESTS = listOf("dinosaurs", "trucks", "chess", "ocean", "space", "dogs", "fairies", "building", "bugs", "cooking")

/** The name is the only thing onboarding cannot make up for him. */
fun canContinue(state: OnboardingState): Boolean = state.name.isNotBlank()

/**
 * Folds one word's result into the read. Pure.
 *
 * A first UNCLEAR is not an answer about the child, it is an answer about the room, so the word
 * stays up and he gets one more go. Anything else is taken at face value and the read moves on.
 * The sixth result closes the read and fixes the starting stage.
 */
fun onWordResult(state: OnboardingState, outcome: WordOutcome): OnboardingState {
    if (outcome == WordOutcome.UNCLEAR && !state.retried) {
        return state.copy(listening = false, retried = true, lastOutcome = outcome)
    }
    val outcomes = state.outcomes + outcome
    val done = outcomes.size >= Placement.WORDS.size
    return state.copy(
        wordIndex = state.wordIndex + 1,
        outcomes = outcomes,
        listening = false,
        retried = false,
        lastOutcome = outcome,
        stage = if (done) Placement.stageFor(outcomes) else null,
    )
}

/**
 * The one line under the word after each attempt, or null before the first one. "Wrong" is not one
 * of the options: a word he does not have yet is where his shelf starts, which is the whole point.
 */
fun resultLine(state: OnboardingState): String? = when {
    state.lastOutcome == null -> null
    state.lastOutcome == WordOutcome.UNCLEAR && state.retried -> "One more try"
    state.lastOutcome == WordOutcome.OK -> "Got it"
    else -> "We'll start there"
}

/**
 * The parent's recap of the read: "Read cat, pen. Skipped pig." Words he missed or mumbled are not
 * named, because naming them turns a placement into a report card.
 */
fun recapLine(outcomes: List<WordOutcome>): String {
    fun words(want: WordOutcome) = outcomes.withIndex()
        .filter { (_, o) -> o == want }
        .mapNotNull { (i, _) -> Placement.WORDS.getOrNull(i)?.word }
    val read = words(WordOutcome.OK)
    val skipped = words(WordOutcome.SKIP)
    return listOfNotNull(
        read.takeIf { it.isNotEmpty() }?.let { "Read ${it.joinToString(", ")}." },
        skipped.takeIf { it.isNotEmpty() }?.let { "Skipped ${it.joinToString(", ")}." },
    ).joinToString(" ")
}

/**
 * Drives onboarding: the profile fields, the microphone during the placement read, and the one
 * write that turns all of it into a child.
 *
 * @param context application context; the recognizer outlives any one screen
 */
class OnboardingViewModel(private val repo: Repo, context: Context) : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    private val recognizer = Recognizer(context)
    private val session = ListenSession()

    /** False means nobody has been onboarded, which is what sends Nav to the profile screen. */
    suspend fun hasChild(): Boolean = repo.activeChild() != null

    /**
     * Clears a finished run so the parent's "+ add child" starts blank. A run still in progress is
     * left alone: this also fires when he backs out of the placement read to fix a typo.
     */
    fun resetIfComplete() {
        if (_state.value.createdChildId != null) _state.value = OnboardingState()
    }

    fun setName(name: String) { _state.value = _state.value.copy(name = name) }

    fun setAge(age: Int) { _state.value = _state.value.copy(age = age) }

    fun toggleInterest(tag: String) {
        val cur = _state.value.interests
        _state.value = _state.value.copy(interests = if (tag in cur) cur - tag else cur + tag)
    }

    /** Opens the mic for the word on screen. Called on entry to each word, and by the mic button. */
    fun listen() {
        val s = _state.value
        val word = Placement.WORDS.getOrNull(s.wordIndex)?.word ?: return
        if (s.listening) return
        _state.value = s.copy(listening = true, lastOutcome = null)
        session.start()
        recognizer.listen(
            onResult = { transcript, confidence ->
                // A negative score means the engine rated nothing, not that it heard nothing.
                if (session.accept()) result(Placement.outcome(word, transcript, if (confidence < 0) 1f else confidence))
            },
            // An engine error sounds the same to a four-year-old as silence: it buys him the retry.
            onError = { if (session.accept()) result(WordOutcome.UNCLEAR) },
        )
    }

    /** Skip, from the parent or the child. Whatever the mic half-heard is thrown away. */
    fun skip() {
        stopListening()
        result(WordOutcome.SKIP)
    }

    /** Leaving the placement screen. The mic stops and its result is not wanted. */
    fun stopListening() {
        recognizer.cancel()
        session.cancel()
        if (_state.value.listening) _state.value = _state.value.copy(listening = false)
    }

    private fun result(outcome: WordOutcome) {
        _state.value = onWordResult(_state.value, outcome)
    }

    /**
     * Writes the child and makes them active, once. [onDone] runs on the main thread after the row
     * exists, so the shelf it navigates to already has somebody to load.
     */
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        if (s.createdChildId == null) {
            val child = repo.addChild(
                name = s.name.trim(),
                ageYears = s.age,
                interests = INTERESTS.filter { it in s.interests }.joinToString(","),
                startStage = s.stage ?: 0,
            )
            _state.value = _state.value.copy(createdChildId = child.id)
        }
        onDone()
    }

    override fun onCleared() {
        recognizer.destroy()
    }
}
