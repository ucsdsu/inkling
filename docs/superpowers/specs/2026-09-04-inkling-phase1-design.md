# Inkling Phase 1 Design

Date: 2026-09-04
Status: approved in chat by Jon, 2026-09-04
Prototype: `docs/prototype/inkling-sim.html` (click-through, the source of truth for screens and copy)

## What Inkling is

An Android app that turns a cheap e-ink tablet into a kid's reader with a
one-tap kids mode, an offline reading tutor, and a parent screen behind a
PIN. First device: Onyx BOOX Go Color 7 Gen II (Android 13, 1264x1680,
Kaleido 3 color e-ink, 4 GB RAM, mic, speaker, 2,300 mAh). First tester: a
4-year-old reading at the Dr. Seuss / early decodable level.

The kid's experience is fully offline. Wi-Fi is only needed when a parent
asks for new generated books (phase 2).

## Why not Linux

Onyx ships a locked bootloader and no kernel sources. No custom ROM or
mainline Linux exists for any BOOX device. Every cheap e-ink tablet runs
Android. One APK covers all of them. Omarchy and "Omarchy Kids" do not run
on this hardware.

## Decisions already made

| Decision | Choice | Why |
|---|---|---|
| Kiosk enforcement | Default-launcher + AccessibilityService blocker, no factory reset (option B) | Jon wants to demo before committing the device. Device Owner (option A) is the upgrade path before phase 3. |
| Language, UI | Kotlin, Jetpack Compose, animations off | Device Owner, lock task, accessibility, TTS, and speech APIs are native only. |
| Min SDK | 29 (Android 10) | Covers older cheap e-ink tablets. Boox Go Color 7 is 33. |
| Kid reader | Our own, not KOReader | At 4 the books are picture books and decodables. A tiny reader we own is fully instrumented and has no menus to get lost in. |
| Speech | On-device only. Try Android `SpeechRecognizer` offline first; Vosk if it fails the spike. | Offline, private, no per-utterance cost. |
| Rewards | No stars, no coins, no streaks. Games do not unlock behind reading. | Developmental psych review: visible prize structures erode reading for its own sake. Read tile first and biggest is the whole nudge. |
| Storage | SQLite (Room) on device. No server in phase 1. | Offline. Sync is optional later. |
| Profiles | Data model supports many children from day 1. Onboarding UI ships in phase 1b. | Jon has 3 kids. |
| Typeface | Andika for kid-facing text | Designed for early readers. Single-storey a and g, no confusable I/l. |
| License | MIT | Open source. Core Knowledge CKLA materials are CC BY-NC-SA and stay out of the repo. |

## Phase split

- **Phase 1a (this spec, first plan):** speech spike, launcher, blocker,
  usage log, PIN, parent screen (Kids mode toggle, Today, Apps, Rules).
  Done when Cove can't leave kids mode and Jon can, and the spike has a
  number.
- **Phase 1b (next plan):** reader with starter-pack decodables, TTS with
  word highlight, inline tutor, onboarding (profile, placement), Reading
  and Next up tabs.
- **Phase 2:** generated books via API with deterministic validator,
  images, Core Knowledge read-alouds in the starter pack, trade-book PDF
  import script for the Mac.
- **Phase 3:** Device Owner upgrade, second device brand, optional sync.

## Gate 0: the speech spike

Nothing in the tutor gets built until this has a number.

Procedure: a hidden screen in the app (behind the parent PIN) shows one
decodable line, records, runs on-device recognition, diffs, and appends a
row to `spike.csv` in app storage: expected text, transcript, confidence,
flagged words, reviewer verdict (parent taps correct / wrong / unclear).
Cove reads 20 lines in a normal room.

Pass: 2 or fewer of the 20 correct reads are flagged as a miss (false
positive rate at or under 10%). Fail: redesign the coach (confidence
gating, articulation allowlist, larger silence window) before phase 1b.

## Screens (phase 1a)

Copy and layout are in the prototype. Summary:

1. **Kid home.** Greeting with the child's name, "N books today." Read
   tile (phase 1a: opens a placeholder that says "Books come next"), then
   one tile per enabled app in a 2-column grid. Each game tile shows a
   budget bar (used / cap), no numbers, no clock. A small gear in the strip
   opens the PIN on long press (600 ms).
2. **Done for today.** Shown when a capped app's tile is tapped. Copy:
   "Chess is done for today. It comes back tomorrow. Want to read one more
   book?" Buttons: Pick a book, Back.
3. **PIN.** 4 digits. After 3 wrong tries, 30-second lockout doubling each
   further failure. Default PIN set on first parent open.
4. **Parent.** Kids mode switch at the top with hint text. Tabs: Today
   (screen time, by-app bars, week summary), Apps (every launchable app on
   the device, on/off switch, daily cap stepper in 5-minute steps), Rules
   (whole-device daily ceiling, warning minutes before a cap, quiet hours,
   change PIN, children list). Reading and Next up tabs arrive in 1b.
5. **Spike** (hidden, under Rules > "Speech test"). Described above.

## Behavior

### Kids mode on

- Inkling is the default HOME app. Home button and back-out-of-app land on
  Kid home.
- The AccessibilityService watches `TYPE_WINDOW_STATE_CHANGED`. When the
  foreground package is not allowed, it fires `GLOBAL_ACTION_HOME` and
  brings Kid home to front. Allowed: enabled apps, Inkling itself, system
  UI, the input method, and the Android permission dialog package.
- Foreground time per package accumulates in `UsageEvent` rows (package,
  start, end). Budget = sum of today's durations for that package.
- When an app reaches (cap minus warning minutes), a full-screen overlay
  from the service says "2 minutes left" for 3 seconds. At cap, the
  service sends the child home and the tile becomes Done for today.
- Whole-device ceiling applies to enabled non-Read apps summed. Read has
  no cap.
- Quiet hours: outside them, every tile except Read is Done for today.

### Kids mode off

- The service does nothing. Inkling is still the HOME app, so Kid home
  still appears on Home press, but the parent screen offers "Open Boox
  home" which launches the stock launcher activity by package. Jon reads
  his own books there.
- Turning kids mode back on from the parent screen returns to Kid home.

### Boot

- `BOOT_COMPLETED` receiver checks that Inkling is still the default HOME
  and the accessibility service is enabled. If not, the parent screen
  shows a red banner with a button to the right settings page.

## Data model (Room)

```
Child(id, name, ageYears, createdAt)
AppRule(id, childId, packageName, label, enabled, dailyCapMinutes)   // cap 0 = no cap
UsageEvent(id, childId, packageName, startedAt, endedAt)
Settings(childId, kidsModeOn, deviceCeilingMinutes, warningMinutes,
         quietStart, quietEnd, pinHash, pinFailures, lockoutUntil)
SpikeRow(id, expected, transcript, confidence, flagged, verdict, at)
```

Phase 1a has exactly one Child row. Everything is keyed by childId so 1b
adds a picker, not a migration.

## Pure logic (unit tested, no Android)

- `BudgetCalculator.remainingMinutes(events, rule, now)`.
- `BlockDecision.shouldBlock(packageName, rules, kidsModeOn, systemAllowlist)`.
- `QuietHours.isQuiet(now, start, end)` including ranges that cross midnight.
- `ReadingDiff.score(expected, transcript, confidence)` returns per-word
  OK / MISS / UNSURE and an overall `LOW_CONFIDENCE` flag. Normalizes case
  and punctuation, aligns by word-level edit distance, treats typical
  4-year-old articulation substitutions (r to w, l to w, th to f or d,
  initial-cluster reduction) as OK, and never returns MISS when confidence
  is under 0.5.
- `PinPolicy.nextLockout(failures)`.

## E-ink rules

- No animations. `LocalInspectionMode` off, all Compose transitions
  disabled.
- Full refresh only on screen change and page turn. Partial updates for
  toggles, PIN dots, tab switches.
- Text is black on paper. Color only for book covers and coach states.
- Onyx SDK refresh calls are wrapped in one `EinkRefresh` object with a
  no-op implementation on non-Onyx devices.

## Verification for phase 1a

- Unit tests pass: `./gradlew :app:testDebugUnitTest`.
- Debug APK builds: `./gradlew :app:assembleDebug`.
- On the Boox, with kids mode on: Cove's 5 apps open from tiles; Settings,
  Play Store, the Boox launcher, and the browser bounce back within 1
  second; Home button returns to Kid home; a 1-minute cap on Hangman turns
  the tile to Done for today and blocks it; PIN gets Jon out; "Open Boox
  home" lands on the stock launcher. Reboot keeps everything.
- Spike CSV has 20 rows with verdicts and a computed false-positive rate.

## Out of scope for 1a

Reader, TTS, tutor UI, onboarding, book generation, images, PDF import,
sync, second device, Device Owner.
