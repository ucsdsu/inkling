Project: Inkling (kids mode + reading tutor for Android e-ink tablets)
Owner chat / channel: Jon, Codex pre-Boox handoff from Fable
Last updated: 2026-09-04

GOAL:
  Close the remaining software defects before the first Boox device acceptance run.

MUST NOT BREAK:
  - Each child's reading history and app limits stay separate.
  - One shared parent PIN protects all profiles (approved by Jon, 2026-09-04).
  - Kid experience works with Wi-Fi off. No online speech fallback.
  - Stock Boox launcher remains reachable from parent screen.
  - No reset, Device Owner, rewards, or reading-gated games.

DONE WHEN:
  - Profile -> six-word placement -> first shelf -> finish book -> Reading / Next up
    -> add child -> switch back passes on the emulator.
  - Duplicate finish, placement progression, Home during setup, shared PIN upgrades,
    and child usage isolation have regression checks.
  - Speech callbacks cannot cross pages, placement words, retries, books, or children.
  - Read aloud selects an installed offline English voice and reports failure without
    recording the page as TTS.
  - The speech spike saves each verdict once, stops after 20 lines, and reports the
    false-flag numerator, correct-read denominator, and whether the 20-correct sample exists.

METRIC:
  - 183 unit tests pass; debug APK builds.
  - Emulator walkthrough preserves Cove's original records after switching.

VERIFY:
  - cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug
  - See docs/plans/progress.md for emulator evidence and the review matrix.
  - Robolectric view-model tests must install a test Main dispatcher and reset it in
    teardown; runBlocking plus the paused Android main looper can deadlock join().

STOP / ASK JON FIRST:
  - Factory reset, Device Owner, cloud API calls, account changes, publishing.

OUT OF SCOPE:
  - Generated books, images, PDF import, sync, and new feature/design work.

CURRENT PHASE:
  - Phase 1c and the bounded pre-Boox speech fixes are verified locally.
  - Overall hardware acceptance remains open: actual Boox launcher/blocking behavior,
    e-ink refresh, offline speech availability, and Cove's 20-line speech spike.

NEXT:
  - Independent Astra review is complete. Run docs/boox-acceptance.md on the Boox
    after Jon approves the device trial.
