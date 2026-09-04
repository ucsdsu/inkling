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

Next:
- Plan task 1 (Gradle scaffold), then task 3 (speech spike) on the device.

Spike result: not run yet with Cove. Screen verified on the boox7 emulator (Android 14,
Play Store image) on 2026-09-04: SpikeScreen renders, the RECORD_AUDIO prompt appears, and
tapping Listen returns `recognizer error 13` (ERROR_LANGUAGE_NOT_SUPPORTED) because the
emulator ships no offline en-US model. Engine choice stays open until the Boox run.
