# Inkling

Kids mode and a reading tutor for cheap Android e-ink tablets.

One tap puts the tablet in kids mode: a home screen with a few allowed
apps, a reader with books at the child's exact phonics stage, and a tutor
that listens to the child read and sounds out the words they miss. A PIN
gets the parent out and shows what the child actually read.

Everything the child touches works offline.

First device: Onyx BOOX Go Color 7 Gen II. First tester: a 4-year-old.

## Status

Phase 1a in progress. See [WORK.md](WORK.md) for the current contract and
[docs/plans/progress.md](docs/plans/progress.md) for what has shipped.

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

   ```bash
   adb install -r android/app/build/outputs/apk/debug/app-debug.apk
   ```

## Set up the device

1. Open Settings > Apps > Default apps > Home app. Choose Inkling.
2. Open Settings > Accessibility. Enable the Inkling service.
3. Open Inkling. Long-press the gear. Set a PIN.
4. In the Apps tab, switch on the apps the child may use.
5. Switch Kids mode on.

## License

MIT. See [LICENSE](LICENSE).
