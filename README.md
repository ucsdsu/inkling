# Inkling

Kids mode and a reading tutor for cheap Android e-ink tablets.

One tap puts the tablet in kids mode: a home screen with a few allowed
apps, a reader with books at the child's exact phonics stage, and a tutor
that listens to the child read and sounds out the words they miss. A PIN
gets the parent out and shows what the child actually read.

Everything the child touches works offline.

First device: Onyx BOOX Go Color 7 Gen II. First tester: a 4-year-old.

## Status

Phase 1c is implemented and emulator-tested. Real Boox and child-speech verification remain open. See [WORK.md](WORK.md) for the current contract and
[docs/plans/progress.md](docs/plans/progress.md) for what has shipped.

Use [the Boox acceptance checklist](docs/boox-acceptance.md) for the device trial
and the steps to return to the stock launcher.

## Design

- Click-through prototype: [docs/prototype/inkling-sim.html](docs/prototype/inkling-sim.html)
- Phase 1 spec: [docs/superpowers/specs/2026-09-04-inkling-phase1-design.md](docs/superpowers/specs/2026-09-04-inkling-phase1-design.md)
- Phase 1a plan: [docs/superpowers/plans/2026-09-04-phase1a-launcher-and-spike.md](docs/superpowers/plans/2026-09-04-phase1a-launcher-and-spike.md)

## Build

Requirements: JDK 17, Android SDK with platform 34 and build-tools 34.

1. Install the SDK packages.

   ```bash
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```

2. Build the debug APK and run the unit tests.

   ```bash
   cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug
   ```

3. Install on a device over USB.

   Existing database version 3 profiles are retained. Earlier pre-release database versions
   use destructive migration; back up their app data before upgrading.

   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

## Set up the device

1. Open Inkling. Enter a child name, age, and interests. Complete or skip the 6-word placement.
2. Open Settings > Apps > Default apps > Home app. Choose Inkling.
3. Open Settings > Accessibility. Enable the Inkling service.
4. Open Inkling. Long-press the gear. Set a PIN.
5. In the Apps tab, switch on the apps the child may use.
6. Switch Kids mode on.

The parent PIN and lockout apply to every child. Reading records and app limits remain
separate. For older profiles with different PINs, the oldest established PIN is retained.

Speech uses Android's on-device recognizer (Android 12 or newer with an installed offline
language model). If unavailable, the app shows a setup notice and never retries online.
Reading, read-aloud, and placement Skip remain available. Boox speech support still needs
testing; the Vosk fallback has not been implemented.

## License

MIT. See [LICENSE](LICENSE).
