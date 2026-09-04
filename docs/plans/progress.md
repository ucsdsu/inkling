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

Next:
- Run the speech spike with Cove on the Boox, then the device journey on real hardware.
- Capture the real Onyx system UI package names and extend SYSTEM_ALLOWLIST (F11b).
