# Inkling progress

Working file. Each phase ends by updating this. Delete when the project ships.

## 2026-09-04

Shipped:
- Repo scaffold, MIT license, prototype v4 copied to docs/prototype.
- Phase 1 spec and phase 1a plan written.

Decisions:
- Option B kiosk (default launcher + accessibility blocker), no reset.
- No stars, no reading-gated games.
- Speech: Android SpeechRecognizer offline first, Vosk fallback.

Blocked on Jon:
- Create github.com/ucsdsu/inkling (gh is logged in as airbo-jon).
- Accept Android SDK licenses and install platform 34.

Emulator journey (boox7 AVD, Android 14, 2026-09-04), tasks 5-8:
- Kid home renders as HOME app after `cmd package set-home-activity`; "Hi, Cove." plus the Read tile.
- Long-press on the gear opens the PIN pad; first entry sets the PIN.
- Parent screen: setup banner, Kids mode switch, Today / Apps / Rules tabs all work.
- Two apps switched on (Settings with a 5-minute cap, Voice Search uncapped) show as tiles.
- Tapping a tile opens the app; hardware home returns to kid home.
- Blocker proven with a control: service off, Play Store stays in front for 3 s; service on,
  focus is back on dev.inkling/MainActivity within 0.5 s. Same for the capped app once over cap.
- Done for today verified by injecting a 6-minute UsageEvent into inkling.db (no sqlite3 on device,
  so the row went in through `run-as ... cat`).
- Force-stopping the app unbinds the accessibility service and Android does not rebind it. Worth
  watching on the Boox.

## Phase 1b tasks 5-6 (2026-09-04)

Shipped:
- `ui/ShelfScreen.kt`, `ui/ReaderScreen.kt`, `ui/ReaderViewModel.kt`. The Read tile and the
  "Pick a book" button on Done for today both open the shelf; a row opens `reader/{bookId}`.
- Pure reducer `onRecognized(state, transcript, confidence)` plus `buildShelf` / `currentStageIndex`,
  11 tests in `ui/ReaderStateTest.kt`.
- Parent gets a Reading tab between Today and Apps, off pure `buildReading` (3 tests).
- Kid home counts finished books and says "1 book today." in the singular.

Decisions:
- `ReaderViewModel` takes the application Context and owns Speaker and Recognizer for the process
  lifetime. Building them per screen would have made the first "Read to me" of every book silent
  while the TTS engine came up.
- The Reading tab leaves low-confidence attempts out of both the count and the average. The
  recognizer named nothing on those, so scoring them as clean reads would flatter him.
- Current stage = the first book he has not read at 95% or better. Books below it read Easy,
  above it Stretch, the stage itself Try it until he reads it aloud.

Emulator journey (boox7, emulator-5554, Android 14, 2026-09-04), shots/1b_*.png:
- Read tile opens a shelf of 6 books on a fresh DB: The Cat and the Hat "Try it", the other five
  "Stretch" (1b_shelf.png).
- The Big Red Hen opens at "The hen is red." 32sp, "page 1 of 10"; the right tap zone turns the
  page (1b_reader_page1.png, 1b_reader_page2.png).
- Read to me works and highlights word by word: hen, is, the, pen bold in sequence
  (1b_tts_highlight.png, pulled from screenrecord because a still screencap is slower than the
  utterance). Audibility is not provable on the emulator; logcat shows
  `GoogleTTSServiceImpl: TTS dispatch: en-us-x-iog-seanet-embedded`, so an on-device voice ran.
- I'll read turns the button rust and says "Listening…" (1b_listening.png). The emulator has no
  offline pack (`SodaSpeechRecognizer: Failed to get language pack ... error 13`), so the online
  retry fires and, with no mic audio, returns NO_SPEECH_DETECTED. The reader shows
  "I didn't catch that. Try again." and the page footer labels the attempt "online speech"
  (1b_unclear.png).
- Debug-only long-press on the mic (BuildConfig.DEBUG) cycles two fake transcripts:
  "the hen is wed" reads as GOOD, because r-to-w is articulation, not a decoding miss
  (1b_good.png); "the hen is bed" coaches "red" with chunks r | e | d, the e on teal
  (1b_coach.png).
- Tapping through to page 10 and once more finishes the book and returns to the shelf, which now
  tags The Big Red Hen "Just right" at 88% (1b_shelf_after.png). Kid home reads "1 book today."
  (1b_home_one_book.png).
- Parent, Reading tab: The Big Red Hen "finished×1 · 88%", the other five "not started", "red"
  under WORDS HE MISSED TWICE, 4 read-aloud attempts today at 88% (1b_parent_reading.png).

Found and fixed during the journey: the forward tap zone clamped at the last page, so a book could
not be finished by turning pages. `Nav.kt` now routes any forward move off the last page through
the same finish path as the "Next page" button.

Residual for the next round: `Recognizer` puts a Long in
`EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS`, which the engine expects as an Int, so it
falls back to 0 ("Key ... expected Integer but value was a java.lang.Long" in logcat). It came in
with the phase 1a spike, not with these tasks.

## Review round 1b (2026-09-04)

A review of the reader (phase 1b) found 9 defects. One commit each, each with a unit test or an
emulator check that fails if the fix is taken out. 111 tests before, 118 after.

- R1 Nothing on the reader path ever asked for RECORD_AUDIO; only the speech spike did. "I'll
  read" on a fresh install started a recognizer with no permission and the child got silence. The
  reader route now checks the permission on the tap, launches the system dialog when it is
  missing, and starts listening from the callback. A refusal shows the unclear card with
  "Inkling needs the microphone to hear you read." That copy is parent-facing on purpose and
  still never says wrong or no. `ReaderState.notice` carries it.
- R2 `history()` pooled every recent attempt, unclear ones included. Three mumbles flagged no
  words, so they scored 1.0 and promoted him a stage for saying nothing. `recentForBook` now
  selects `lowConfidence = 0` rows only; with none, lastAccuracy stays null and the shelf still
  says "try it".
- R3 `stopListening()` called `stopListening` on the engine, which is the "take what you have"
  call, so a line he abandoned still came back, coached him, and reached the parent's log. It now
  calls `Recognizer.cancel()` and marks a `ListenSession` cancelled; the next result or error is
  dropped once, in `handleRecognition` and in the error path.
- R4 Hardware Back popped the reader while the TTS kept reading to an empty screen, and the page
  he was on never reached the reading log. The route calls `reader.leave()` on dispose: cancel the
  recognizer, stop the speaker, log the page with its mode, clear the book. A configuration change
  disposes the route too, so that case is excluded by `isChangingConfigurations`. `finish()` is
  the same call now.
- R5 `finished×N` counted every ReadEvent on the last page, so paging back and forward over page
  10 read as five finishes. `timesFinished` counts distinct days
  (`COUNT(DISTINCT startedAt / 86400000)`).
- R6 Rotation re-ran `LaunchedEffect(bookId)`, which reopened the book at page 0. `open()` is a
  no-op when that book is already open; `shouldOpen()` holds the rule.
- R7 "‹ Books" and "‹ Home" were a 15sp line with 6dp of padding, about 28dp of target. Both are
  boxes at the shared `TapTarget` minimum now, text centred. `TapTarget` moved to Theme.kt so the
  shelf and the reader use one number.
- R8 One var took whichever button was tapped last, so "Read to me" then "I'll read" logged the
  page as `self` and overstated how much he decoded alone. Two flags per page feed `pageMode()`:
  tts wins, because he heard the line.
- R9 The debug long-press faked "the hen is wed" on every book, so on any other title both fakes
  scored as misses and the coach named a word that is not on the page. `fakeTranscripts(line)`
  derives both from the open line: r read as w (forgiven, "nice reading"), then the last word
  started with the wrong letter (coached).

Emulator evidence (boox7, emulator-5554, 2026-09-04):
- R1: `pm revoke dev.inkling android.permission.RECORD_AUDIO`, open The Big Red Hen, tap
  "I'll read" -> "Allow Inkling to record audio?" (shots/r1b_mic_prompt.png). "Don't allow" ->
  the card reads "Inkling needs the microphone to hear you read." `pm grant`, tap again ->
  "Listening…".
- R4: tap the coach card, which speaks the chunks and then the word. Left alone it dispatches
  twice, 14:14:39.625 and 14:14:41.143. With Back pressed 1.0s in, one dispatch at 14:14:08.015,
  the back mark at 14:14:09.052, and nothing after. The DB gained
  `15 short-e-hen page 0 tts 14:13:00 -> 14:14:09`: the page was logged by the hardware key, with
  tts as its mode.
- R5: two last-page ReadEvents for short-e-hen on one day; the Reading tab reads "finished×1 ·
  88%" (shots/r1b_parent_reading.png).
- R6: page 4 of 10, `user_rotation 1`, still page 4 with the same line.
- R7: the clickable node behind "‹ Home" is [40,88][148,208] and behind "‹ Books" is
  [32,80][142,200]. 120px at density 320 is 60dp. (shots/r1b_back_target.png)
- R9: on "The hen is red.", the first long-press gives "Nice reading!" and the second coaches
  "red" with chunks r|e|d (shots/r1b_good_fake.png, r1b_coach_fake.png).
- Journey: kid home -> shelf -> The Big Red Hen -> Read to me -> I'll read (granted, listens,
  lands on unclear because the emulator feeds the mic nothing) -> both fakes -> forward off page
  10 back to the shelf -> parent PIN -> Reading tab.

Not proven here: R2 has no live evidence, because every unclear reading on the emulator came from
a recognizer error, and the error path logs no attempt at all. The unit test covers it.

Residual from R4: the Home key is not covered. Inkling is the HOME app, so KEYCODE_HOME
re-delivers the intent to the singleTask activity instead of stopping it. The reader route is
never disposed, no lifecycle callback fires, and the TTS keeps talking: chunk replay dispatched
at 14:21:00.882, Home at 14:21:01.922, another dispatch at 14:21:02.440. Back is fixed; Home
needs a different hook, probably the HOME intent arriving in onNewIntent.

## Review round 2 (2026-09-04)

A second review found 4 defects in the service's timing design. Fixed by taking timers out, not
by adding more. One commit each, each with a test or an emulator check that fails without it.

- R1 Every window event spawned its own delay(60s) cap-check coroutine, and each pass spawned
  another, so chains multiplied through a session. Each chain also read `lastPkg` outside the
  Mutex that guards evaluation, so a switch mid-delay could re-evaluate a stale package. One
  ticker started in onServiceConnected now loops `delay(30_000)` and re-evaluates `lastPkg` under
  that same Mutex; it is cancelled in onDestroy. Every read and write of `lastPkg` and `warnedFor`
  happens inside the lock. handle() split into onForeground() and evaluate(pkg, openSpan): the
  ticker passes openSpan=false, because the span it would open is already open.
  `ServiceState.onTick` is the pure decision, unit tested at cap and under cap.
- R2 `ignoreEvent` guessed with a clock: it dropped every event from our own package for 5 seconds
  after the warning overlay went up, so a real switch to Inkling inside that window was lost and a
  late overlay event was handled. Replaced by `isForegroundChange(pkg, className, self)`: only
  `dev.inkling.MainActivity` counts as us coming to the front, since the overlay reports its widget
  class. The round-1 F4b note above describes the deleted mechanism, not the current one. A test
  pins the constant to `MainActivity::class.java.name`.
- R3 `Budget.MAX_SPAN_MS` clamped closed spans too, so a real 3-hour session was billed as 2 hours.
  The clamp now applies only when `endedAt` is null, which is the case it exists for.
  `closeStaleSpans` still writes abandoned spans down to start+2h when it closes them, so the guess
  is made once, at close, not on every read.
- R4 A span left running at bedtime ran all night. The service now registers a receiver for
  ACTION_SCREEN_OFF (close the open span, clear lastPkg) and ACTION_USER_PRESENT (clear lastPkg, so
  the first event after unlocking opens a fresh span), unregistered in onDestroy.

Emulator evidence (boox7, emulator-5554, 2026-09-04):
- Screen off/on (R4): span opened 13:13:27, KEYCODE_SLEEP at 13:14:33 closed the row at 13:14:34;
  KEYCODE_WAKEUP at 13:14:48 opened a new span at 13:14:48. The 15 seconds asleep charge nobody.
- Cap (R1, R2): Settings at 8 of a 10-minute cap opened 13:18:14, "2 minutes left" overlay at once,
  bounced to dev.inkling/MainActivity at 13:20:31 without the child touching anything. The row ran
  13:18:14 to 13:20:31 unbroken, so the overlay no longer splits the span. Kid home showed the tile
  greyed "Done for today" (shots/r2_cap.png).
- PIN 1234 unlocks; the parent Today tab read "Screen time today 8 min" against an injected closed
  8-minute span, which is R3's arithmetic.
- Play Store launched 13:17:49.834, focus back on dev.inkling at 13:17:49.999 (165 ms).

Note for the next round: after `adb install -r`, toggle the accessibility service off and on. The
old binding survives the install pointing at a dead process, and the service silently does nothing.

Next:
- Run the speech spike with Cove on the Boox, then the device journey on real hardware.

Spike result: not run yet with Cove. Screen verified on the boox7 emulator (Android 14,
Play Store image) on 2026-09-04: SpikeScreen renders, the RECORD_AUDIO prompt appears, and
tapping Listen returns `recognizer error 13` (ERROR_LANGUAGE_NOT_SUPPORTED) because the
emulator ships no offline en-US model. Engine choice stays open until the Boox run.

## Review round 1 (2026-09-04)

An independent review of phase 1a found 11 defects. All fixed, one commit each,
each with a unit test or an emulator check that fails if the fix is removed.

- F1 PIN overwritten on cold start. ParentState gained `loaded`; the pin route shows blank
  paper until it is true and `pinMode()` decides SET/UNLOCK from loaded state only. refresh()
  publishes settings before enumerating installed apps, which moved to Dispatchers.IO.
- F2 Change PIN used to clear the hash first. PinScreen now takes an explicit PinMode; SET
  asks for the digits twice through the pure `SetPinFlow` machine and stores nothing until
  they match. setPin rejects an empty PIN.
- F3 Stale open spans ate the day. `Budget.MAX_SPAN_MS` (2 h) clamps every span at read time
  and `Repo.closeStaleSpans` closes leftovers at app start and at service connect.
- F4 Mid-session cap re-check died after one minute. scheduleCapCheck no longer nulls lastPkg,
  handle() is idempotent via `Repo.openSpanIfChanged`, and the warning branch re-arms the chain.
- F4b Found while verifying F4 on the emulator, from the UsageEvent rows: the warning overlay is
  a window in Inkling's own package, so posting it raised a window event for dev.inkling while the
  tracked app was still in front. handle() closed the running span and pointed lastPkg at us, which
  killed the chain a second way. ServiceState.ignoreEvent drops our own window events for 5 seconds
  after the overlay goes up.
- F5 Kid home never refreshed after returning from an app. LifecycleEventEffect(ON_RESUME)
  drives the refresh. Dead `settingsFlow`/`rulesFlow` deleted.
- F6 Concurrent handle() calls raced. A Mutex guards handle(); span writes run in
  `db.withTransaction`.
- F7 ReadingDiff composed cluster reduction with r/l/th substitution, so grill->will and
  free->wee scored OK. The two variant sets are unioned, never chained.
- F8 Missing recognizer confidence (-1) is stored raw and shown as `conf=n/a`.
- F9 BootReceiver deleted. Background activity starts are blocked on Android 10+ and Inkling
  comes up on boot as the HOME app anyway; the setup banner is the boot check.
- F11a spikeCsv escapes embedded double quotes through a pure `csvField` helper.
- F11b com.onyx.android.sdk stays in the allowlist with a comment saying to capture the real
  Boox system UI packages with `adb shell dumpsys window | grep mCurrentFocus`.

Emulator evidence (boox7, emulator-5554, 2026-09-04):
- Cold start after a force-stop shows "Enter parent PIN", not the set flow (F1).
- Change PIN, first entry, mismatching confirm shows "Those don't match. Start over."; Back out and
  9999 is rejected while 1234 still unlocks (F2).
- Cap re-check: Settings at 8 of a 10-minute cap, opened 12:56:20, warning fired at once, bounced
  to dev.inkling/MainActivity at 12:58:23 (F4, F4b). Before the F4b fix the span closed itself at
  exactly 2.00 minutes and never bounced.
- Kid home showed the Settings tile greyed "Done for today" straight after the bounce, no relaunch (F5).
- Play Store launched 12:59:12, focus back on dev.inkling by 12:59:20.

Deviations from the review's instructions:
- F4's warning branch also re-arms the cap check. Without it the chain still ended at the first
  warning (default warningMinutes is 2), so the cap never fired mid-session.
- The app's cap stepper moves in steps of 5, so a 2-minute cap is not reachable from the UI. The
  emulator check used a 10-minute cap entered at 8 minutes of use, which exercises the same window
  (warning, then block one minute later).

## Review round 2 (2026-09-04)

A second review found 4 defects in the service's timing design. Fixed by taking timers out, not
by adding more. One commit each, each with a test or an emulator check that fails without it.

- R1 Every window event spawned its own delay(60s) cap-check coroutine, and each pass spawned
  another, so chains multiplied through a session. Each chain also read `lastPkg` outside the
  Mutex that guards evaluation, so a switch mid-delay could re-evaluate a stale package. One
  ticker started in onServiceConnected now loops `delay(30_000)` and re-evaluates `lastPkg` under
  that same Mutex; it is cancelled in onDestroy. Every read and write of `lastPkg` and `warnedFor`
  happens inside the lock. handle() split into onForeground() and evaluate(pkg, openSpan): the
  ticker passes openSpan=false, because the span it would open is already open.
  `ServiceState.onTick` is the pure decision, unit tested at cap and under cap.
- R2 `ignoreEvent` guessed with a clock: it dropped every event from our own package for 5 seconds
  after the warning overlay went up, so a real switch to Inkling inside that window was lost and a
  late overlay event was handled. Replaced by `isForegroundChange(pkg, className, self)`: only
  `dev.inkling.MainActivity` counts as us coming to the front, since the overlay reports its widget
  class. The round-1 F4b note above describes the deleted mechanism, not the current one. A test
  pins the constant to `MainActivity::class.java.name`.
- R3 `Budget.MAX_SPAN_MS` clamped closed spans too, so a real 3-hour session was billed as 2 hours.
  The clamp now applies only when `endedAt` is null, which is the case it exists for.
  `closeStaleSpans` still writes abandoned spans down to start+2h when it closes them, so the guess
  is made once, at close, not on every read.
- R4 A span left running at bedtime ran all night. The service now registers a receiver for
  ACTION_SCREEN_OFF (close the open span, clear lastPkg) and ACTION_USER_PRESENT (clear lastPkg, so
  the first event after unlocking opens a fresh span), unregistered in onDestroy.

Emulator evidence (boox7, emulator-5554, 2026-09-04):
- Screen off/on (R4): span opened 13:13:27, KEYCODE_SLEEP at 13:14:33 closed the row at 13:14:34;
  KEYCODE_WAKEUP at 13:14:48 opened a new span at 13:14:48. The 15 seconds asleep charge nobody.
- Cap (R1, R2): Settings at 8 of a 10-minute cap opened 13:18:14, "2 minutes left" overlay at once,
  bounced to dev.inkling/MainActivity at 13:20:31 without the child touching anything. The row ran
  13:18:14 to 13:20:31 unbroken, so the overlay no longer splits the span. Kid home showed the tile
  greyed "Done for today" (shots/r2_cap.png).
- PIN 1234 unlocks; the parent Today tab read "Screen time today 8 min" against an injected closed
  8-minute span, which is R3's arithmetic.
- Play Store launched 13:17:49.834, focus back on dev.inkling at 13:17:49.999 (165 ms).

Note for the next round: after `adb install -r`, toggle the accessibility service off and on. The
old binding survives the install pointing at a dead process, and the service silently does nothing.

Next:
- Run the speech spike with Cove on the Boox, then the device journey on real hardware.
- Capture the real Onyx system UI package names and extend SYSTEM_ALLOWLIST (F11b).

Home key (post round 1b): MainActivity.onNewIntent counts HOME intents; Nav pops to kid home, which
disposes the reader route and stops speech. Emulator: one TTS dispatch before Home, none after,
kid home in front. 118 tests green.

## Codex phase 1c handoff closure (2026-09-04)

Jon confirmed scope: finish the merged onboarding, profiles, and Next up integration and review.
Jon approved one shared parent PIN across children. No new product features or public release.

Verification: 161 tests, 0 failures, 0 errors; `cd android && ./gradlew
:app:testDebugUnitTest :app:assembleDebug` passed. `git diff --check` passed.
Independent read-only review found the progression and profile-PIN defects below; its precision
review caught the existing-v3 upgrade case. All three findings are closed after fixes and tests.

Review/fix matrix (paths below are relative to android/app/src):

| Finding | Status | Implementation | Regression |
|---|---|---|---|
| New child loses parent PIN | DONE | main/kotlin/dev/inkling/data/Repo.kt, data/Daos.kt | RepoTest.parentPinAndLockoutAreSharedWithoutChangingChildLimits |
| Existing v3 child can still have no PIN | DONE | main/kotlin/dev/inkling/data/Repo.kt | RepoTest.existingUnprotectedProfileInheritsEstablishedPinOnUpgrade |
| Placement above stage 0 prevents progress | DONE | main/kotlin/dev/inkling/ui/ReaderViewModel.kt | ReaderStateTest.masteringPlacementStageAdvancesWithoutEarlierBooks |
| Repeated Finish can create duplicate children | DONE | main/kotlin/dev/inkling/ui/onboarding/OnboardingViewModel.kt | OnboardingViewModelTest.repeatedFinishCreatesOneChildAndNavigatesOnce |
| Home exits first-run setup with no child | DONE | main/kotlin/dev/inkling/ui/Nav.kt | NavStateTest.homeRequiresAChild; emulator Home on fresh database remains on profile form |
| Same app can keep another child's usage span | DONE | main/kotlin/dev/inkling/data/Repo.kt | RepoTest.sameAppForAnotherChildStartsTheirOwnUsageSpan |
| Speech silently retries online | DONE | main/kotlin/dev/inkling/speech/Recognizer.kt | RecognizerTest missing-language and unavailable-engine checks |

Emulator walkthrough, emulator-5554 / boox7 / Android 14:
- Existing Cove profile retained through install. Set temporary QA PIN, add temporary second
  child with interests, skip all 6 placement words, see first-shelf summary, enter kid home.
- Finish The Cat and the Hat by turning past page 10. New child reads "1 book today";
  Parent Reading reads "finished×1". Next up shows the short-a Try it and short-e read-to-me cards.
- Second child offers "Enter parent PIN" and accepts the established PIN, never SET.
- Switch to Cove: all books still "not started", home reads "Hi, Cove." and "0 books today."
- Fresh database: profile form on launch and still profile form after hardware Home.
- With mic permission granted, the missing offline model shows the explicit setup notice on
  placement. It stays on cat; Skip remains available. No online retry.
- Original database restored after QA. All rows in Child, DeviceState, Settings, AppRule,
  UsageEvent, ReadEvent, TutorAttempt, and SpikeRow compare identical with the pre-QA snapshot.
  Microphone permission restored to denied. Final APK installed, original Cove home visible.

Proof screenshots are outside the repository:
`/Users/jonstenstrom/.codex/visualizations/2026/09/04/01a06e88-c72f-7452-99b2-d8b5dec0cf26/inkling-proof/`
(profile, first shelf, reader, Next up, switch back, Home during setup, offline notice, restored home).
No PR or public release was created. Changes remain local for Jon's review.

Residuals: no open findings from this scoped review. Overall device acceptance remains open:
actual Boox accessibility/Home/system packages, e-ink refresh, offline language-model availability,
TTS audibility, and Cove's 20-line false-positive measurement. Android below 12 has no supported
on-device recognizer through this wrapper; Vosk remains unimplemented. This emulator walkthrough
covers portrait tablet layout, not phone or landscape layouts.

Encoded test lesson in WORK.md: view-model tests install/reset a test Main dispatcher to avoid
Robolectric paused-looper deadlocks. Shared-core sync was blocked by filesystem permissions;
local core rules were read. No shared-core changes made.

## Pre-Boox speech closure (2026-09-04)

Astra reviewed the complete phase plan before implementation. The bounded work closed 3 software
defects and left device questions for the Boox run.

| Finding | Status | Implementation | Regression |
|---|---|---|---|
| Stale recognition changes a later page, placement word, retry, book, or child | DONE | `speech/CallbackSession.kt`, `speech/Recognizer.kt`, `ui/ReaderViewModel.kt`, `ui/onboarding/OnboardingViewModel.kt` | `RecognizerTest` gives each attempt a separate platform engine; view-model tests deliver result/error callbacks out of order and inspect stored attempts |
| Read aloud can use a network voice or log TTS after failure | DONE | `speech/Speaker.kt`, `ui/ReaderViewModel.kt` | `SpeakerTest` selects only offline English; `ReaderViewModelSpeechTest` proves async failure stores `look`, while a prior completed reading remains `tts` |
| Spike can double-save a verdict, reset on rotation, or wrap after line 20 | DONE | `spike/SpikeRun.kt`, `spike/SpikeScreen.kt` | `SpikeRunViewModelTest` suspends a Room save through UI disposal and proves one row/exact next line; `SpikeRunTest` covers 0/20, 1/20, 2/20 and 3/20 samples |

Verification: 183 tests, 0 failures, 0 errors; `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/
openjdk.jdk/Contents/Home ./gradlew :app:testDebugUnitTest :app:assembleDebug` passed. `git diff
--check` passed. The debug APK is at `android/app/build/outputs/apk/debug/app-debug.apk`.

The spike terminal reports false flags only among parent-confirmed correct reads. A run with fewer
than 20 correct reads is labeled an incomplete Gate 0 sample even when its observed rate is 0%.
Reopening the screen starts a separate run while preserving every prior row in the CSV.

Fresh Astra review found no remaining must-fix issue after checking the complete diff and the
recognizer, TTS, state, and duplicate-write boundaries.

Final emulator evidence (Android 14, emulator-5554):
- Reader and offline notice fit portrait and landscape. Page turn reaches page 2; Home returns
  to kid home and Back returns to the shelf.
- Final APK requests `en-us-x-iog-seanet-embedded` for read aloud. The missing recognition model
  shows the offline setup notice. Actual voice audibility remains unverified.
- Temporary QAChild completes all 6 Skip steps and the first-shelf summary. The existing QA PIN
  unlocks that child. Switching back to Cove preserves access to Reading and Next up.
- The speech-test offline notice and line number are identical before and after rotation on the
  final APK. Saved-result, in-flight-save, duplicate-save, and 20-row completion behavior use the
  Room/view-model regressions because this emulator has no offline recognition model.
- Restored the original database and reopened the final APK. All rows in all 10 tables compare
  equal to the original snapshot, including child identity and table cardinality. Microphone
  denial and its permission flags, plus portrait rotation settings, are restored.
- Screenshots are outside the repository at
  `/Users/jonstenstrom/.codex/visualizations/2026/09/05/01a06ee4-9219-7810-bf54-c7289a97d87f/inkling-proof/`.
- Final APK SHA-256: `6465c86208750cdd41f3c9b977c6a0c493ae9a73f21f6308cda480cb19f12f98`.

Hardware acceptance remains Boox launcher/accessibility/system packages and reboot, e-ink refresh, installed offline
recognition, audible offline TTS, and Cove's 20-correct-read spike. Vosk remains out of scope until
device evidence shows the built-in on-device recognizer cannot meet Gate 0. Warning/quiet-hour
editing, week summary, and vendor e-ink refresh hooks remain unimplemented plan discrepancies; the
current contract excludes them pending observed need.
