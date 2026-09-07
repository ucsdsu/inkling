Project: Inkling (kids mode + reading tutor for Android e-ink tablets)
Owner chat / channel: Jon, Codex pre-Boox handoff from Fable
Last updated: 2026-09-07

GOAL:
  Make parent-approved apps easy to add to the child home screen, open, and remove.
  Keep the home screen focused on Read and approved apps. Discover is deferred.

MUST NOT BREAK:
  - Each child's reading history and app limits stay separate.
  - One shared parent PIN protects all profiles (approved by Jon, 2026-09-04).
  - Kid experience works with Wi-Fi off. No online speech fallback.
  - Stock Boox launcher remains reachable from parent screen.
  - No reset, Device Owner, rewards, or reading-gated games.

DONE WHEN:
  - Parent opens directly to Manage apps, with icons and explicit approval switches.
  - Simulator add -> home tile -> launch -> Home -> remove -> blocked launch passes.
  - No Discover card or route remains in this version.
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
  - Removing Discover preserves schema version 4 and all existing tables.
  - Populated v3-to-v4 migration, duplicate finish, next-day/backward-date and speech isolation tests pass.
  - Reader navigation works without speech; pointing at text never turns a page.
  - Reader controls and coaching fit tablet portrait, landscape, and narrow Android width.
  - 185 unit tests pass; debug APK builds; lint has 0 errors (38 existing warnings).
  - Emulator walkthrough preserves Cove's original records after switching.

VERIFY:
  - App picker and child home fit portrait, landscape, and narrow Android widths.
  - Compare original database state after reversing the temporary approval; preserve
    reading records, profiles, PIN, and existing limits. Usage from the trial is expected.
  - cd android && ./gradlew :app:testDebugUnitTest :app:assembleDebug
  - See docs/plans/progress.md for emulator evidence and the review matrix.
  - Robolectric view-model tests must install a test Main dispatcher and reset it in
    teardown; runBlocking plus the paused Android main looper can deadlock join().

STOP / ASK JON FIRST:
  - Factory reset, Device Owner, cloud API calls, account changes, publishing.

OUT OF SCOPE:
  - Cloud sync, generated curriculum, rewards, automatic comprehension grading, new speech engines.
  - Discover is deferred; preserve its stored data for compatibility.

CURRENT PHASE:
  - Discover is deferred; its UI, lesson code, artwork, and active queries are removed.
  - 185 tests pass; debug APK and lint pass. Simulator flow and data preservation verified.
  - docs/discovery.md records the deferral and migration boundaries.
  - docs/boox-ui-audit.md records reader evidence and remaining tutoring questions.
  - Final APK is installed; original simulator records are restored.

NEXT:
  - Run the actual BOOX and child trial. Teaching effectiveness is not established by UI tests.

LAUNCHER PASS (2026-09-06):
  Constraints: 24dp outer padding, 8dp spacing, large labeled controls, no new permissions.
  Compare compact app rows with two-line app cards off-device; choose cards for readable
  approval labels and limits. No new store, DNS service, enforcement policy, or schema.
  Verified: 191 unit tests, debug APK, lint, diff check, and independent code review.
  Simulator Clock approval -> tile -> launch -> Home -> revoke -> direct-launch block passed.
  Parent and home visually checked at 1264x1680, 1680x1264, and 720x1440.
  Original tables, rows, IDs and counters restored and compared after QA.
  Actual BOOX enforcement and ad blocking remain hardware work.

DISCOVER REMOVAL:
  No data migration or reset. Preserve current simulator onboarding/profile state on install.
  Verified: 185 tests, APK build, lint, portrait/landscape home, and unchanged database rows.
  Independent review: removed lesson reference in migration fixture (DONE, test passes);
  stale app-position acceptance text (DONE, docs/boox-acceptance.md). No open removal findings.
