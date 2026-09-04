# Inkling Phase 1b Implementation Plan: Reader and Tutor

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Read tile opens a shelf of decodable books; a page shows one line of big text; "Read to me" speaks it with word highlighting; "I'll read" listens, diffs, and coaches one missed word by sounding it out; everything is logged so the parent's Reading tab shows what he read and what he missed.

**Architecture:** Books are JSON in `assets/books/`, loaded by `BookStore`. Pure logic (`Chunker`, `ShelfTags`) in `core/`. Two new Room tables (`ReadEvent`, `TutorAttempt`), DB version 2 with destructive migration (pre-release). `Speaker` wraps Android TTS; `Recognizer` moves from `spike/` to `speech/` and is shared. Screens: `ShelfScreen`, `ReaderScreen` (with the inline tutor), plus a Reading tab on the parent screen.

**Tech Stack:** as phase 1a (Kotlin 2.0, Compose, Room, Robolectric). Plus `kotlinx-serialization-json` for the book JSON.

**Spec:** `docs/superpowers/specs/2026-09-04-inkling-phase1-design.md`. Screens and copy: `docs/prototype/inkling-sim.html` (Shelf, Reader, tutor states).

## Global Constraints

- Everything from the phase 1a plan's Global Constraints still applies (no animations, no rewards, Andika, `core/` has no Android imports, copy from the prototype, commit per task with the trailer).
- Kid text on the page is 30sp or larger. Tap targets are at least 60dp tall.
- The coach never says "wrong", "no", or "incorrect". Copy: "Let's sound it out:" then chunks, then "Tap the mic and try the line again." Good state: "Nice reading!" with a Next page button.
- Low-confidence or empty recognition shows "I didn't catch that. Try again." and flags nothing.
- All speech and TTS stay on-device when an offline engine exists; the online fallback from the icons/speech commit stays labeled and is only for the emulator.
- Onboarding, placement, and generated books are out of scope (phase 1c and 2).

---

### Task 1: Starter pack and BookStore

**Files:**
- Create: `android/app/src/main/assets/books/short-a-cat.json`, `short-e-hen.json`, `short-i-pig.json`, `short-o-fox.json`, `short-u-bug.json`, `sh-ship.json`
- Create: `android/app/src/main/kotlin/dev/inkling/books/Book.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/books/BookStore.kt`
- Modify: `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts` (serialization plugin + json)
- Test: `android/app/src/test/kotlin/dev/inkling/books/BookStoreTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable data class Book(val id: String, val title: String, val stage: String, val target: String, val pages: List<String>)
  class BookStore(assets: AssetManager) { fun all(): List<Book>; fun byId(id: String): Book? }
  ```
  Stages, in order: `cvc-a`, `cvc-e`, `cvc-i`, `cvc-o`, `cvc-u`, `digraph-sh`. `target` is the grapheme the coach highlights (`a`, `e`, `i`, `o`, `u`, `sh`).

- [ ] **Step 1: Add serialization**

`libs.versions.toml`: add `serialization = "1.7.3"` under versions, `kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }` under libraries, and `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }` under plugins. Root `build.gradle.kts`: `alias(libs.plugins.kotlin.serialization) apply false`. App: `alias(libs.plugins.kotlin.serialization)` and `implementation(libs.kotlinx.serialization.json)`.

- [ ] **Step 2: Write the six books**

Each file is one object. Every word must be decodable at that stage or in this sight-word list: `the, a, is, in, on, to, and, I, has, can, not, it, up`. Ten pages each, one sentence per page, 4 to 8 words. Original text, no Seuss.

`short-a-cat.json`:
```json
{"id":"short-a-cat","title":"The Cat and the Hat","stage":"cvc-a","target":"a","pages":[
"The cat has a hat.","The hat is tan.","The cat sat on a mat.","A rat ran at the cat.",
"The cat is mad.","The rat has the hat.","The cat ran and ran.","The rat sat on the mat.",
"The cat has a nap.","The rat has a nap."]}
```

`short-e-hen.json`:
```json
{"id":"short-e-hen","title":"The Big Red Hen","stage":"cvc-e","target":"e","pages":[
"The hen is red.","The hen is in the pen.","Ten eggs sat in the pen.","The hen fed the ten.",
"A wet leg. A wet bed.","The hen met a pet.","The pet is a cat.","The cat is not a hen.",
"The hen and the cat sat.","The ten are fed."]}
```

`short-i-pig.json`:
```json
{"id":"short-i-pig","title":"The Pig Can Dig","stage":"cvc-i","target":"i","pages":[
"The pig can dig.","The pig is big.","It can dig in the pit.","The pig has a wig.",
"The wig is on the pig.","The kid has a bib.","The kid can sit.","The pig can not sit.",
"The pig did a jig.","The kid and the pig sit."]}
```

`short-o-fox.json`:
```json
{"id":"short-o-fox","title":"The Fox on the Log","stage":"cvc-o","target":"o","pages":[
"The fox is on a log.","The dog is on the log.","The fox can hop.","The dog can not hop.",
"The fox got a pot.","The pot is hot.","The dog got a mop.","The fox and the dog jog.",
"The fox is in the fog.","The dog is on top."]}
```

`short-u-bug.json`:
```json
{"id":"short-u-bug","title":"The Bug in the Mud","stage":"cvc-u","target":"u","pages":[
"The bug is in the mud.","The bug can run.","The sun is up.","The bug has a cup.",
"The cup is in the mud.","The pup can dig.","The pup dug up the cup.","The bug can hug the pup.",
"The pup and the bug run.","The sun is up. It is fun."]}
```

`sh-ship.json`:
```json
{"id":"sh-ship","title":"The Ship in the Shop","stage":"digraph-sh","target":"sh","pages":[
"The ship is in the shop.","The fish is on the ship.","The fish can not shut it.","Shut the shop.",
"The ship has a dish.","A fish is in the dish.","The fish has a wish.","The fish can rush.",
"Shh. The fish can nap.","The ship and the fish."]}
```

- [ ] **Step 3: Failing test**

`BookStoreTest.kt` (Robolectric, reads real assets):
```kotlin
package dev.inkling.books

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookStoreTest {
    private val store = BookStore(ApplicationProvider.getApplicationContext<Context>().assets)
    private val sight = setOf("the","a","is","in","on","to","and","i","has","can","not","it","up")

    @Test fun loadsSixBooksInStageOrder() {
        assertEquals(listOf("cvc-a","cvc-e","cvc-i","cvc-o","cvc-u","digraph-sh"), store.all().map { it.stage })
        assertTrue(store.all().all { it.pages.size == 10 })
    }

    @Test fun everyWordIsDecodableOrSight() {
        for (b in store.all()) for (p in b.pages) for (w in p.lowercase().split(" ")) {
            val word = w.filter { it.isLetter() }
            if (word in sight || word.isEmpty()) continue
            val vowels = word.count { it in "aeiou" }
            assertTrue("$word in ${b.id} has $vowels vowels", vowels == 1)
            if (b.stage.startsWith("cvc")) assertTrue("$word in ${b.id} wrong vowel", word.contains(b.target))
        }
    }

    @Test fun byIdFindsAndMisses() {
        assertEquals("The Big Red Hen", store.byId("short-e-hen")?.title)
        assertEquals(null, store.byId("nope"))
    }
}
```

- [ ] **Step 4: Implement**

`Book.kt`:
```kotlin
package dev.inkling.books

import kotlinx.serialization.Serializable

/** One decodable book. [target] is the grapheme the coach highlights when a word is missed. */
@Serializable
data class Book(val id: String, val title: String, val stage: String, val target: String, val pages: List<String>)

/** Stage order for the shelf. */
val STAGES = listOf("cvc-a", "cvc-e", "cvc-i", "cvc-o", "cvc-u", "digraph-sh")
```

`BookStore.kt`:
```kotlin
package dev.inkling.books

import android.content.res.AssetManager
import kotlinx.serialization.json.Json

class BookStore(private val assets: AssetManager) {
    private val json = Json { ignoreUnknownKeys = true }
    private val books: List<Book> by lazy {
        assets.list("books").orEmpty().filter { it.endsWith(".json") }
            .map { name -> assets.open("books/$name").bufferedReader().use { json.decodeFromString(Book.serializer(), it.readText()) } }
            .sortedBy { STAGES.indexOf(it.stage) }
    }
    fun all(): List<Book> = books
    fun byId(id: String): Book? = books.firstOrNull { it.id == id }
}
```

- [ ] **Step 5: Run, commit**

Run: `./gradlew :app:testDebugUnitTest --tests 'dev.inkling.books.*'` → 3 pass. If `everyWordIsDecodableOrSight` fails, fix the book text, not the test.

```bash
git commit -am "feat(books): six-book starter pack and BookStore" # plus trailer
```

---

### Task 2: Chunker and shelf tags (pure Kotlin)

**Files:**
- Create: `android/app/src/main/kotlin/dev/inkling/core/Chunker.kt`
- Create: `android/app/src/main/kotlin/dev/inkling/core/ShelfTags.kt`
- Test: `android/app/src/test/kotlin/dev/inkling/core/ChunkerTest.kt`, `ShelfTagsTest.kt`

**Interfaces:**
```kotlin
data class Chunk(val text: String, val highlight: Boolean)
object Chunker { fun chunk(word: String, target: String): List<Chunk> }
enum class Tag { TRY_IT, JUST_RIGHT, EASY, STRETCH }
data class BookHistory(val bookId: String, val finished: Int, val lastAccuracy: Float?)   // accuracy 0..1, null if never tutored
object ShelfTags { fun tag(history: BookHistory?, stageIndex: Int, currentStageIndex: Int): Tag }
```

- [ ] **Step 1: Tests**

`ChunkerTest.kt`:
```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ChunkerTest {
    private fun s(chunks: List<Chunk>) = chunks.joinToString("|") { (if (it.highlight) "*" else "") + it.text }

    @Test fun cvcSplitsPerLetterAndHighlightsTarget() { assertEquals("p|*e|n", s(Chunker.chunk("pen", "e"))) }
    @Test fun digraphStaysTogether() { assertEquals("*sh|i|p", s(Chunker.chunk("ship", "sh"))); assertEquals("f|i|*sh", s(Chunker.chunk("fish", "sh"))) }
    @Test fun otherDigraphsStayTogetherUnhighlighted() { assertEquals("th|*i|n", s(Chunker.chunk("thin", "i"))); assertEquals("b|*a|ck", s(Chunker.chunk("back", "a"))) }
    @Test fun punctuationAndCaseAreStripped() { assertEquals("*h|e|n", s(Chunker.chunk("Hen.", "h"))) }
}
```

`ShelfTagsTest.kt`:
```kotlin
package dev.inkling.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfTagsTest {
    @Test fun neverOpenedAtCurrentStageIsTryIt() { assertEquals(Tag.TRY_IT, ShelfTags.tag(null, 1, 1)) }
    @Test fun aboveCurrentStageIsStretch() { assertEquals(Tag.STRETCH, ShelfTags.tag(null, 3, 1)) }
    @Test fun belowCurrentStageIsEasy() { assertEquals(Tag.EASY, ShelfTags.tag(null, 0, 2)) }
    @Test fun finishedWithHighAccuracyIsEasy() { assertEquals(Tag.EASY, ShelfTags.tag(BookHistory("x", 1, 0.96f), 1, 1)) }
    @Test fun openedButNotMasteredIsJustRight() { assertEquals(Tag.JUST_RIGHT, ShelfTags.tag(BookHistory("x", 0, 0.8f), 1, 1)) }
    @Test fun lowAccuracyIsStretch() { assertEquals(Tag.STRETCH, ShelfTags.tag(BookHistory("x", 0, 0.6f), 1, 1)) }
}
```

- [ ] **Step 2: Implement**

`Chunker.kt`:
```kotlin
package dev.inkling.core

data class Chunk(val text: String, val highlight: Boolean)

/** Splits a decodable word into the pieces a teacher would point at: letters, with common digraphs kept together. */
object Chunker {
    private val DIGRAPHS = listOf("sh", "ch", "th", "ck", "ng", "wh", "ph", "qu")

    fun chunk(word: String, target: String): List<Chunk> {
        val w = word.lowercase().filter { it.isLetter() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < w.length) {
            val two = if (i + 1 < w.length) w.substring(i, i + 2) else ""
            if (two in DIGRAPHS) { out += two; i += 2 } else { out += w[i].toString(); i += 1 }
        }
        return out.map { Chunk(it, it == target.lowercase()) }
    }
}
```

`ShelfTags.kt`:
```kotlin
package dev.inkling.core

enum class Tag { TRY_IT, JUST_RIGHT, EASY, STRETCH }

data class BookHistory(val bookId: String, val finished: Int, val lastAccuracy: Float?)

/**
 * The five-finger rule, made visible. About 1 unknown word in 20 is "just right":
 * accuracy at or above 0.95 is EASY, below 0.75 is STRETCH, in between is JUST_RIGHT.
 * Books above the child's stage are STRETCH until proven otherwise; below it, EASY.
 */
object ShelfTags {
    fun tag(history: BookHistory?, stageIndex: Int, currentStageIndex: Int): Tag {
        val acc = history?.lastAccuracy
        if (acc != null) return when {
            acc >= 0.95f -> Tag.EASY
            acc < 0.75f -> Tag.STRETCH
            else -> Tag.JUST_RIGHT
        }
        return when {
            stageIndex > currentStageIndex -> Tag.STRETCH
            stageIndex < currentStageIndex -> Tag.EASY
            else -> Tag.TRY_IT
        }
    }
}
```

- [ ] **Step 3: Run, commit** (`dev.inkling.core.*` all green; NoAndroidImportsTest still passes).

---

### Task 3: Reading data (DB v2)

**Files:**
- Modify: `data/Entities.kt`, `data/Daos.kt`, `data/InklingDb.kt` (version 2, `fallbackToDestructiveMigration()`), `data/Repo.kt`
- Test: `data/ReadingRepoTest.kt`

**Interfaces:**
```kotlin
@Entity data class ReadEvent(id: Long = 0, childId: Long, bookId: String, page: Int, startedAt: Long, endedAt: Long?, mode: String)  // mode: "tts" | "self" | "look"
@Entity data class TutorAttempt(id: Long = 0, childId: Long, bookId: String, page: Int, transcript: String, confidence: Float, missed: String, lowConfidence: Boolean, at: Long)
// Repo
suspend fun logPage(childId: Long, bookId: String, page: Int, mode: String, startedAt: Long, endedAt: Long)
suspend fun logAttempt(a: TutorAttempt)
suspend fun booksFinishedToday(childId: Long, dayStart: Long): Int          // distinct bookIds with a ReadEvent on the last page today
suspend fun history(childId: Long, bookId: String, pageCount: Int): BookHistory   // finished = times last page logged; lastAccuracy from the most recent 10 attempts on that book: 1 - missedWords/totalWords, null if no attempts
suspend fun missedTwice(childId: Long): List<String>                        // words appearing in >= 2 attempts' missed lists, most frequent first
suspend fun recentAttempts(childId: Long, limit: Int): List<TutorAttempt>
```
`missed` is space-separated words. Accuracy needs total words: store `totalWords: Int` on `TutorAttempt` too.

- [ ] **Step 1: Tests** (`ReadingRepoTest.kt`, Robolectric, same setup pattern as `RepoTest`):
  - `historyIsNullWithoutAttempts`, `accuracyIsOneMinusMissedShare` (2 attempts, 8 words each, 1 and 3 missed → 0.75), `finishedCountsLastPageOnly`, `missedTwiceOrdersByFrequency` ("pen" in 3 attempts, "ten" in 2, "hen" in 1 → `["pen","ten"]`), `booksFinishedTodayIgnoresYesterday`.
- [ ] **Step 2: Implement** the entities, DAOs (`@Query` over `TutorAttempt WHERE childId AND bookId ORDER BY at DESC LIMIT 10`, `ReadEvent WHERE page = :last AND startedAt >= :dayStart`), and Repo methods. Bump `@Database(version = 2)` and add `.fallbackToDestructiveMigration()` to the builder with a comment: pre-release, remove before any public build.
- [ ] **Step 3: Run, commit.**

---

### Task 4: Speaker (TTS with word highlighting) and Recognizer move

**Files:**
- Create: `speech/Speaker.kt`
- Move: `spike/Recognizer.kt` → `speech/Recognizer.kt` (package `dev.inkling.speech`; fix imports in `SpikeScreen`)
- Test: none automated (Android TTS). Emulator check in Task 6.

`Speaker.kt`:
```kotlin
package dev.inkling.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Reads a line aloud and reports which word is being spoken, for highlighting.
 * Word index comes from onRangeStart's character offset mapped onto the line's words.
 * Also speaks a missed word in chunks ("p. e. n. pen") for the coach.
 */
class Speaker(context: Context, private val onReady: (Boolean) -> Unit) {
    private var ready = false
    private val tts = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale.US
        onReady(ready)
    }

    fun speakLine(line: String, onWord: (Int) -> Unit, onDone: () -> Unit) {
        if (!ready) { onDone(); return }
        val starts = wordStarts(line)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val idx = starts.indexOfLast { it <= start }
                if (idx >= 0) onWord(idx)
            }
            override fun onDone(utteranceId: String?) { onDone() }
            override fun onError(utteranceId: String?) { onDone() }
            override fun onStart(utteranceId: String?) {}
        })
        tts.setSpeechRate(0.85f)
        tts.speak(line, TextToSpeech.QUEUE_FLUSH, Bundle(), "line")
    }

    /** "p. e. n. pen": chunks with pauses, then the whole word. Letter names, not phonemes, until we ship phoneme clips. */
    fun speakChunks(chunks: List<String>, word: String, onDone: () -> Unit) {
        if (!ready) { onDone(); return }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) { if (utteranceId == "word") onDone() }
            override fun onError(utteranceId: String?) { onDone() }
            override fun onStart(utteranceId: String?) {}
        })
        tts.speak(chunks.joinToString(". ") + ".", TextToSpeech.QUEUE_FLUSH, Bundle(), "chunks")
        tts.playSilentUtterance(400, TextToSpeech.QUEUE_ADD, "gap")
        tts.speak(word, TextToSpeech.QUEUE_ADD, Bundle(), "word")
    }

    fun stop() { tts.stop() }
    fun shutdown() { tts.shutdown() }

    private fun wordStarts(line: String): List<Int> {
        val out = mutableListOf<Int>()
        var inWord = false
        line.forEachIndexed { i, c -> if (!c.isWhitespace() && !inWord) { out += i; inWord = true } else if (c.isWhitespace()) inWord = false }
        return out
    }
}
```

Add `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>` to the manifest. Commit.

---

### Task 5: Shelf and Reader screens with the inline tutor

**Files:**
- Create: `ui/ShelfScreen.kt`, `ui/ReaderScreen.kt`, `ui/ReaderViewModel.kt`
- Modify: `ui/Nav.kt` (routes `shelf`, `reader/{bookId}`; Read tile and "Pick a book" navigate to `shelf`), `MainActivity.kt` (factory for `ReaderViewModel(repo, BookStore(assets))`), `ui/HomeViewModel.kt` (`booksToday` from `repo.booksFinishedToday`)
- Test: `ui/ReaderStateTest.kt` for the pure reducer below

**Reader state machine** (pure, in `ReaderViewModel.kt`, tested):
```kotlin
enum class TutorPhase { IDLE, LISTENING, COACH, UNCLEAR, GOOD }
data class ReaderState(
    val book: Book? = null, val page: Int = 0, val speakingWord: Int = -1,
    val phase: TutorPhase = TutorPhase.IDLE, val missedWord: String? = null, val chunks: List<Chunk> = emptyList(),
    val speechMode: String = "",
)
/** What happens when recognition returns. Pure. */
fun onRecognized(state: ReaderState, transcript: String, confidence: Float): ReaderState {
    val line = state.book!!.pages[state.page]
    val r = ReadingDiff.score(line, transcript, if (confidence < 0) 1f else confidence)
    return when {
        r.lowConfidence -> state.copy(phase = TutorPhase.UNCLEAR, missedWord = null, chunks = emptyList())
        r.missed.isEmpty() -> state.copy(phase = TutorPhase.GOOD, missedWord = null, chunks = emptyList())
        else -> { val w = r.missed.first(); state.copy(phase = TutorPhase.COACH, missedWord = w, chunks = Chunker.chunk(w, state.book.target)) }
    }
}
```
Tests: exact read → GOOD; "pin" for "pen" → COACH with missedWord "pen" and chunks p|*e|n; empty transcript → UNCLEAR; two misses → COACH on the first only.

**ReaderViewModel** holds `Speaker`, `Recognizer`, page timing (log a `ReadEvent` on every page leave with mode `tts` if Read to me was used, `self` if the tutor was, else `look`), and logs a `TutorAttempt` on every recognition (missed joined by space, `totalWords` = word count of the line).

**ShelfScreen**: list from `BookStore.all()`, each row: cover square with the first letter of the title on a stage color (`Rust, Teal, Ochre, Moss, Paper2, Teal`), title 17sp bold, subtitle "Decodable · short e · 10 pages" (stage rendered as "short a" / "sh digraph"), pill on the right with the tag text: Try it, Just right, Easy, Stretch, colors as in the theme (`Moss` for Just right, `Ochre` for Stretch, `Paper2` for Easy, `Teal` for Try it). Current stage index = the lowest stage whose tag is not EASY; compute via `repo.history` per book. Tap → `reader/{id}`. Back → home.

**ReaderScreen**: strip with "‹ Books" and the title; the page: a bordered box, text centered vertically at 32sp Andika, the word at `speakingWord` bold, the `missedWord` (when phase is COACH) with an `Ochre` background; invisible tap zones left 38% / right 38% of the box for previous/next; below: two buttons, "I'll read" filled (mic icon) and "Read to me" outlined (speaker icon). When phase is LISTENING the mic button turns `Rust` and says "Listening…"; tapping it again stops early. COACH: card under the buttons: speaker icon, "Let's sound it out:", the chunks as bordered boxes with the highlighted one on `Teal`, and "Tap the mic and try the line again." Tapping the card replays the chunks. UNCLEAR: card "I didn't catch that. Try again." GOOD: `Moss` card "Nice reading!" with "Next page ›" on the right; tapping it advances. Page number "page 3 of 10" at the bottom in Ink3. Page turn resets phase to IDLE and stops speech. Leaving the last page forward logs the finish and returns to the shelf.

- [ ] Steps: reducer test red → implement reducer → green → screens → nav → `HomeViewModel.booksToday` → build → commit per logical piece (reducer, screens, nav).

---

### Task 6: Parent Reading tab and emulator journey

**Files:**
- Modify: `ui/ParentScreen.kt` (tab "Reading" between Today and Apps), `ui/ParentViewModel.kt` (`recent: List<TutorAttempt>`, `missedTwice: List<String>`, per-book last accuracy and finished count for the six books)

Reading tab layout: "LAST BOOKS" section: rows `title` … `finished×N · 91%` (or `not started`); "WORDS HE MISSED TWICE" as a mono line joined by " · "; "READ-ALOUD ATTEMPTS TODAY" count and average accuracy. No phonics map yet (phase 1c, needs more data).

- [ ] Emulator journey, screenshots to `shots/1b_*.png`:
  1. Read tile → shelf with 6 books, first tagged Try it, the rest Stretch (fresh DB).
  2. Open The Big Red Hen, page 1 text at 32sp, "page 1 of 10". Tap right zone → page 2.
  3. Read to me: words bold one by one (screenshot mid-utterance; if TTS is silent on the emulator, `adb shell dumpsys audio | grep -i tts` or a logcat line proves it ran).
  4. I'll read: tap, status Listening; with no mic → UNCLEAR card "I didn't catch that. Try again." Then, to prove COACH and GOOD without a mic, add a debug-only long-press on the mic button (only when `BuildConfig.DEBUG`) that cycles a fake transcript: first "the hen is wed" (articulation OK → GOOD), then "the hen is bed" (COACH on "red"... wait, page 1 is "The hen is red." so use "the hen is bed" → COACH on "red", chunks r|*e|d). Screenshot both cards.
  5. Finish a book by tapping through; shelf now tags it; kid home says "1 books today" (fix the plural: "1 book today" / "N books today").
  6. Parent → Reading tab shows the book, attempts, and the missed word.

Update `docs/plans/progress.md`. Commit. Do not push.

## Self-review

Spec coverage for 1b: reader with starter decodables (T1, T5), TTS with word highlight (T4, T5), inline tutor with one-miss coach and unclear state (T5), reading log (T3, T5), Reading tab (T6). Onboarding and Next up are explicitly deferred to 1c. Types: `Chunk`, `Tag`, `BookHistory` defined in T2 and used in T3/T5/T6 with the same names; `ReaderState.onRecognized` uses `ReadingDiff.score` from 1a and `Chunker.chunk` from T2.
