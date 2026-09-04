# Inkling Phase 1c Implementation Plan: Onboarding, Profiles, Next Up

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** First launch walks a parent through a child profile (name, age, interests), a 6-word placement read, and the child's first shelf. Several children can live on one device and the parent switches between them. The parent's Next up tab recommends 3 things with a reason each.

**Architecture:** `Child` gains `interests` and `startStage`. A one-row `DeviceState` holds the active child. `Repo.ensureChild()` is replaced by `activeChild(): Child?`; callers treat null as "not onboarded". Pure logic in `core/`: `Placement` (words to stage) and `NextUp` (rules to recommendations). Three new screens under `ui/onboarding/`. DB version 3, destructive migration (pre-release).

**Tech Stack:** as 1a and 1b.

**Spec:** `docs/superpowers/specs/2026-09-04-inkling-phase1-design.md`. Screens and copy: `docs/prototype/inkling-sim.html` views `ob1`, `ob2`, `ob3`, and the parent "Next up" pane.

## Global Constraints

- Everything from the 1a and 1b Global Constraints applies.
- No account, no email, no network in onboarding.
- Kid-facing placement copy: "Read this word." One word at a time at 40sp. Buttons: mic (auto-starts) and "Skip". Never "wrong".
- Parent-facing copy from the prototype: "Who's this for?", "what they're into (pick a few)", "Read these words out loud.", "Cove starts at short e.", "Already on the device".
- The "Making 3 books" block from the prototype is phase 2; render it as one line: "New books made just for Cove come in a later update."

---

### Task 1: Data and pure logic (one agent)

**Files:**
- Modify: `data/Entities.kt` (Child + `interests: String = ""`, `startStage: Int = 0`; new `DeviceState(@PrimaryKey id: Int = 1, activeChildId: Long?)`), `data/Daos.kt`, `data/InklingDb.kt` (version 3, register DeviceState), `data/Repo.kt`
- Modify every `ensureChild` call site: `service/KidsModeService.kt`, `ui/HomeViewModel.kt`, `ui/ParentViewModel.kt`, `ui/ReaderViewModel.kt`
- Create: `core/Placement.kt`, `core/NextUp.kt`
- Tests: update `data/RepoTest.kt`, `data/ReadingRepoTest.kt`, `ui/ParentViewModelPinTest.kt` (replace `ensureChild()` with `addChild("Cove", 4, "", 0)`); create `core/PlacementTest.kt`, `core/NextUpTest.kt`, and add `RepoTest` cases for children.

**Interfaces produced:**
```kotlin
// Repo
suspend fun activeChild(): Child?                       // DeviceState.activeChildId, else null
suspend fun children(): List<Child>
suspend fun addChild(name: String, ageYears: Int, interests: String, startStage: Int): Child   // creates Settings row, sets active
suspend fun setActiveChild(id: Long)
suspend fun setStartStage(childId: Long, stage: Int)

// core/Placement.kt
data class PlacementWord(val word: String, val stageIndex: Int)
enum class WordOutcome { OK, MISS, SKIP, UNCLEAR }
object Placement {
    val WORDS = listOf(PlacementWord("cat", 0), PlacementWord("pen", 1), PlacementWord("pig", 2), PlacementWord("hop", 3), PlacementWord("bug", 4), PlacementWord("ship", 5))
    /** Stage index to start at: the stage of the first word not read OK; all OK means the last stage. UNCLEAR counts as not OK only if it happens twice for the same word; the screen retries once. */
    fun stageFor(outcomes: List<WordOutcome>): Int
    fun outcome(word: String, transcript: String, confidence: Float): WordOutcome   // via ReadingDiff.score on the single word
}

// core/NextUp.kt
data class Recommendation(val title: String, val why: String, val bookId: String?)
object NextUp {
    /** Up to 3, in this order when applicable:
     *  1. The first TRY_IT book at the current stage: "Introduces <stage label>. One new pattern is the right step."
     *  2. A JUST_RIGHT book with accuracy under 0.90: "<Title> was <N>% last time. A second pass usually pushes it past 90."
     *  3. The first STRETCH book: "Too many new patterns for now. Keep it as a 'read to me' book."
     *  If missedTwice is non-empty, item 2's why gets appended: " Words to watch: pen, ten."
     *  Fewer than 3 when the shelf has nothing that fits. */
    fun recommend(rows: List<ShelfEntry>, missedTwice: List<String>): List<Recommendation>
}
data class ShelfEntry(val bookId: String, val title: String, val stageLabel: String, val tag: Tag, val lastAccuracy: Float?)
```
`ShelfEntry` lives in `core/NextUp.kt` so `core/` stays Android-free; `ui` maps `ShelfRow` to it.

**Call-site rules:** `KidsModeService.handle` returns early when `activeChild()` is null. `HomeViewModel.refresh` and `ReaderViewModel` and `ParentViewModel.refresh` set a `noChild = true` flag in their state when null; Nav (Task 2) routes to onboarding. `ParentViewModel.tryPin` returns false when null.

**Tests (write first):**
- `PlacementTest`: all OK → 5; first miss at index 1 → 1; skip at 0 → 0; `outcome("red","wed",0.9f)` OK; `outcome("pen","pin",0.9f)` MISS; `outcome("pen","",0.9f)` UNCLEAR; `outcome("pen","pin",0.3f)` UNCLEAR.
- `NextUpTest`: fresh shelf (one TRY_IT, rest STRETCH) → 2 items (Try it, Stretch) with the exact why strings; a JUST_RIGHT at 0.88 with missedTwice ["pen","ten"] → item 2 present with "88%" and "Words to watch: pen, ten."; everything EASY → empty list.
- `RepoTest`: `activeChildIsNullOnFreshDb`, `addChildSetsActiveAndCreatesSettings`, `setActiveChildSwitches`, `childrenListsInCreationOrder`.

Commit: `feat(data,core): child profiles, placement, next-up rules; db v3`.

---

### Task 2: Onboarding screens and routing (after Task 1; parallel with Task 3)

**Files:**
- Create: `ui/onboarding/OnboardingViewModel.kt`, `ui/onboarding/ProfileScreen.kt`, `ui/onboarding/PlacementScreen.kt`, `ui/onboarding/FirstShelfScreen.kt`
- Modify: `ui/Nav.kt` (routes `onboard/profile`, `onboard/placement`, `onboard/done`; start destination is `onboard/profile` when `activeChild()` is null, decided once at startup in a `LaunchedEffect` that shows blank paper until known), `MainActivity.kt` (factory), `ui/KidHome.kt` unchanged.
- Test: `ui/onboarding/OnboardingStateTest.kt` for the pure reducer.

**State:**
```kotlin
data class OnboardingState(
    val name: String = "", val age: Int = 4, val interests: Set<String> = emptySet(),
    val wordIndex: Int = 0, val outcomes: List<WordOutcome> = emptyList(), val listening: Boolean = false,
    val retried: Boolean = false, val lastOutcome: WordOutcome? = null, val stage: Int? = null, val createdChildId: Long? = null,
)
val INTERESTS = listOf("dinosaurs","trucks","chess","ocean","space","dogs","fairies","building","bugs","cooking")
fun onWordResult(state: OnboardingState, outcome: WordOutcome): OnboardingState
// UNCLEAR first time: retried = true, stay on the word. UNCLEAR again or MISS/OK/SKIP: append, advance wordIndex. After the 6th: stage = Placement.stageFor(outcomes).
```
Tests: OK advances; first UNCLEAR stays with retried; second UNCLEAR appends UNCLEAR and advances; after six outcomes `stage` is set; `canContinue(state)` false when name is blank.

**Screens** (copy and layout from the prototype `ob1`..`ob3`, IBM Plex Sans is not shipped, so use the default sans for parent text and Andika for the placement word):
- Profile: step label "1 of 3 · about them", "Who's this for?", a `TextField` for the name (single line, capitalize words, imeAction Done), age row 3 4 5 6 7+, interest chips (multi-select), "Next ›" filled button disabled until name is non-blank.
- Placement: "2 of 3 · hand the tablet to Cove", "Read this word." then the word at 40sp Andika centered, a filled mic button that auto-starts listening on entry to each word (request RECORD_AUDIO the same way the reader does, reusing the permission pattern from Nav's reader route), status "Listening…", and a "Skip" outlined button. Shows a one-line result after each word: "Got it", "We'll start there", "One more try" (for the first UNCLEAR). Never "wrong".
- First shelf: "3 of 3 · first shelf", "<Name> starts at <stage label>.", the recap line "Read cat, pen. Skipped pig." built from outcomes, an "Already on the device" box listing "6 decodables · starter pack", the phase 2 line, and "Go to <Name>'s shelf ›" which calls `repo.addChild(...)` (if not already created) and navigates to `home` clearing the back stack.
- Route `onboard/profile` is also reachable from the parent screen (Task 3's "+ add child"); in that case a new child is added and made active.

Emulator check: fresh install (DB wipes) opens on the profile screen; type a name via `adb shell input text`, pick 4 and two interests, Next; placement: with mic denied, tap Skip six times → "starts at short a"; Go to shelf → kid home says "Hi, <Name>." Screenshots `1c_profile.png`, `1c_placement.png`, `1c_first_shelf.png`, `1c_home.png`.

Commit: `feat(onboarding): profile, placement, first shelf`.

---

### Task 3: Parent Children row and Next up tab (after Task 1; parallel with Task 2)

**Files:**
- Modify: `ui/ParentViewModel.kt` (`children: List<Child>`, `activeChildId`, `nextUp: List<Recommendation>`; `switchChild(id)`), `ui/ParentScreen.kt` (tab "Next up" between Reading and Apps; Rules > Children row becomes the list of children with the active one marked and a "+ add child" link that calls `onAddChild`), `ui/Nav.kt` (`onAddChild` navigates to `onboard/profile`; if Task 2 is not merged yet, leave a TODO-free call to a route string and note it in the report), `ui/ReaderViewModel.kt` (`currentStageIndex` = max(history-derived, child.startStage)).
- Test: `ui/ParentStateTest.kt` add `nextUpMapsShelfRowsToEntries`; `ui/ReaderStateTest.kt` add `startStageRaisesCurrentStage`.

Next up pane layout from the prototype: each recommendation is a bordered card, bold title, grey why line. Empty state: "Nothing to suggest yet. Read a book first."

Emulator check: after a finished book, Next up shows 2 or 3 cards with the why strings; Rules > Children lists the active child; screenshots `1c_nextup.png`, `1c_children.png`.

Commit: `feat(parent): Next up tab and children list`.

---

### Task 4: Integration check (after 2 and 3 merge)

Fresh install → onboarding → shelf → finish a book → parent: Reading, Next up, Rules > Children → "+ add child" → second child → kid home greets the second child → switch back in Rules. Screenshot `1c_switch.png`. Update `docs/plans/progress.md`.

## Self-review

Covers the prototype's ob1..ob3 and the Next up pane, the "Children · + add" row, and the spec's "data model supports many children" with the switch. Placement uses the six starter stages, one word each. Deferred: generated books (phase 2), phonics map (needs data), interests are stored but only used by phase 2 generation.
