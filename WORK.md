Project: Inkling (kids mode + reading tutor for Android e-ink tablets)
Owner chat / channel: Claude Code, homeschool sessions
Last updated: 2026-09-04

GOAL:
  Phase 1a on the Boox Go Color 7: Cove cannot leave kids mode, Jon can with a PIN,
  usage is logged per app, and the speech spike has a measured false-positive rate.

MUST NOT BREAK:
  - Jon's own reading on the device (stock Boox launcher reachable from parent screen)
  - No factory reset, no Device Owner in phase 1
  - Kid experience works with Wi-Fi off
  - No rewards UI (stars, coins, streaks), no reading-gated games

DONE WHEN:
  - Spec section "Verification for phase 1a" passes on the device
  - docs/plans/progress.md records the spike false-positive rate

METRIC:
  - `cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug` → exit 0
  - Spike: flagged-correct-reads / 20 ≤ 0.10

VERIFY:
  - Unit tests + APK build (commands above)
  - Real device journey on the Boox, photographed, attached to the PR

STOP / ASK JON FIRST:
  - Factory reset or Device Owner setup
  - Any cloud API call from the app
  - Publishing to Play, F-Droid, or a public release
  - Anything that touches Jon's Google account on the device

OUT OF SCOPE:
  - Reader, TTS, tutor UI, onboarding (phase 1b)
  - Generated books, images, PDF import, sync (phase 2)

CURRENT PHASE:
  - 1a: speech spike, launcher, blocker, parent screen

NEXT:
  - Task 1 of docs/superpowers/plans/2026-09-04-phase1a-launcher-and-spike.md
