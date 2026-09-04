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
