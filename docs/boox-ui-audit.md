# BOOX child UI and tutor audit

Date: 2026-09-06. Target: 4-year-old, BOOX Go Color 7 Gen II.

Verdict: a calm, usable reading prototype. Effective tutoring and real e-ink usability remain unproven. Confidence is high for the observed navigation defects, moderate for product recommendations, and unknown for child learning outcomes.

## Evidence boundary

This audit inspected the Android implementation and all 6 bundled books. The live walkthrough covered home, shelf, reader, simulated success/coaching, page navigation, and completion on the Android 14 boox7 emulator. It used 1264×1680 portrait, 1680×1264 landscape, and 720×1440 narrow rendering at 320 dpi. The emulator cannot reproduce e-ink ghosting or the BOOX Android 13 speech installation. Parent/onboarding findings below are source findings, not a new end-to-end visual audit of those flows.

## Flow findings and fixes

| Step | Observed health | Change / remaining issue |
|---|---|---|
| 1. Home | Clear Read action; parent target was only 28dp | Parent target is now 60dp with an accessibility description. Long-press and PIN behavior remain intact. App tiles can grow with wrapped labels. |
| 2. Shelf | Opens books reliably; text-heavy for a beginning reader | Rows can grow instead of clipping. Letter covers, phonics jargon, and 6 equal-weight choices remain candidates for a cohesive design pass. |
| 3. Read | 32sp Andika is clear; navigation was hidden | Removed edge overlays that covered 76% of the page width. Visible Back/Next/Finish controls now work in every tutor state. Touching a word cannot turn a page. |
| 4. Tutor feedback | Short, specific correction; original coach forced heading and chunks onto one row | Chunks wrap below the heading. Missed words have an underline as well as color. The microphone says Stop listening while active. Successful reading uses one feedback card and the same Next control. |
| 5. Finish | Existing last-page handler records completion | Visible Finish uses that handler. No extra completion state or parallel navigation path was introduced. |

Changed paths are under `android/app/src/main/kotlin/dev/inkling/`:

- `ui/ReaderScreen.kt`: navigation, feedback, wrapping, semantic button roles.
- `ui/Nav.kt`: removed redundant `onNext` callback.
- `ui/KidHome.kt`, `ui/ShelfScreen.kt`, `ui/DoneScreen.kt`: target sizing and flexible height.
- `ui/ReaderViewModel.kt`: removed an orphaned comment describing a deleted mechanism.
- `speech/Recognizer.kt`: explicit SDK guard at API creation. Existing availability logic already rejects Android below 12; this makes the invariant visible to lint without adding online speech.

## Design constraints

The friend's constraint-system advice is useful. Its exact numbers are a starting point for a specific design, not universal requirements.

- Keep Andika for beginning-reader text. Regular and bold are sufficient. Inter plus serif has no demonstrated advantage here.
- Keep page text at 32sp and child targets at least 60dp. Android uses dp/sp, not CSS pixels.
- Use an 8dp spacing rhythm for a future cohesive pass, with 24dp outer padding where it fits. This patch preserves the existing layout rather than mechanically replacing every spacing value.
- Keep a small radius vocabulary. Existing 8–12dp outlines suit this compact screen; changing everything to 24dp has no proven usability benefit.
- Use dark text and strong outlines. Color must reinforce a label or shape. BOOX specifies 300ppi monochrome and 150ppi color, so fine colored detail is a poor place to carry meaning. [BOOX specifications](https://shop.boox.com/products/go7)
- Keep transitions disabled. Test word-by-word bold highlighting on the actual panel before adding more changing pixels. An emulator cannot select the correct refresh policy.
- Use recognizable, carefully chosen book illustrations in a future shelf pass. Test whether the child can choose a familiar book without adult reading. Do not fill the page with decoration.

## Highest-value tutoring work

These are ranked proposals, not features implemented or learning gains demonstrated by this audit.

| Priority | Proposal and current evidence | Acceptance evidence |
|---|---|---|
| 1, Strong | Verified offline phoneme audio and blending. `Speaker.speakChunks()` sends strings such as `h. a. t.` to ordinary TTS. That does not establish correct /h/ /a/ /t/ sounds; `Chunker` also splits exceptional words such as “the” without an exception model. | A qualified early-reading teacher checks every shipped sound and blend. Replay on the actual BOOX offline. Compare letter names, added schwas, and irregular-word handling. |
| 2, Strong | Validate recognition before trusting correction. The current device/child speech gate is still open. A false correction is a tutoring failure even when its UI looks polished. | Complete the existing 20-parent-correct-read gate with at most 2 false flags. Keep this a device pilot, not evidence of population-wide accuracy. |
| 3, Strong | More coherent, controlled decodables and explicit prerequisites. There are 6 books, each 10 pages, one per stage. Short-a already includes “the,” “is,” and “on”; the content schema has no taught-word exceptions. The hen story contains “The hen fed the ten” without a clear referent. | Define taught patterns and exception words per book. Validate every token against that inventory. Have a teacher check story coherence and stage ordering. |
| 4, Strong | Mastery across different words/pages and later attempts. `Repo.history()` pools the latest attempts; it doesn't require diverse pages or delayed retention. Repeating one line can be mistaken for broader mastery. | Repeated success on one page cannot establish stage mastery. New examples and a later reread must demonstrate transfer without blocking book access. |
| 5, Worth exploring | One obvious “Continue reading” or recommended book on the child's shelf. Current recommendations live with the parent; reading starts at page 0 after reopening. | The child reaches the intended book/page without an adult choosing from technical shelf labels. Preserve per-child identity and refresh/reopen behavior. |
| 6, Worth exploring | One short oral retell/comprehension prompt after a story, supported by parent conversation. Current scoring checks transcription alignment only. | The child explains an event or answers a concrete question about meaning. Don't grade an open response with the current word-diff scorer. |

The IES guide recommends linking speech sounds to letters, decoding/word analysis, and daily connected-text reading. It also addresses narrative language and vocabulary. Those are foundations for the proposals above, not proof that these exact app features work for a 4-year-old. Individual primary trials were not opened in this audit. [IES guide and evidence ratings](https://ies.ed.gov/ncee/wwc/PracticeGuide/21/Published)

## Other open findings

- Parent recommendation copy claims a second pass “usually pushes it past 90.” `NextUp.kt` supplies no evidence for that promise. Replace it with a description of the observed performance when revising recommendation logic.
- `listen()` sets `usedSelf` before a result, so cancelled/unavailable listening can appear as self-reading. Treat that as a measurement-definition issue before interpreting parent reports as demonstrated independent reading.
- First-shelf onboarding promises personalized books in a later update. It collects interests that don't currently personalize the bundled reading experience.
- The child-facing error notices remain relatively long and require adult help. A later audio/onboarding pass should make failures understandable without blaming the child.
- No hardware page-key mapping was found in `MainActivity`. Verify actual BOOX key events before implementing a mapping; avoid assuming they are standard PageUp/PageDown.
- Android lint has 38 warnings, including outdated dependencies, skipped dependency lint checks, and missing application icon. Dependency upgrades and release preparation were not part of this UI patch.
- No new child usability study, TalkBack journey, large-font sweep, real speech accuracy measurement, or learning outcome trial was performed.

## Verification and rollback

1. Run `cd android`.
2. Run `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`.
3. Run `git diff --check` from the repository root.
4. Follow the reader checks in `docs/boox-acceptance.md` on an isolated emulator profile or backed-up test device.

Result: 183 unit tests passed with no failures, errors, or skipped tests. The debug APK builds. Lint has 0 errors and 38 warnings. Robolectric emitted temporary-directory cleanup diagnostics during the passing run. Independent source review found no confirmed must-fix in the final diff.

The emulator walkthrough explicitly checked pointing at text, next/back without speech, simulated success and correction, and coach visibility at tablet and narrow sizes. Simulated transcripts prove UI behavior only. Screen-size emulation restarted the launcher; the narrow reader was reopened before testing.

At the September 6 audit, the changes were local and uncommitted. The UI audit performed no deployment, merge, database migration, curriculum change, or speech model installation. Reverting the UI changes restores the previous layout without changing the database schema. The combined finalization also retains the separately tested version 3-to-4 migration for existing installations.


The final APK also passed page 10 → Finish → shelf → home with the expected 1-book count. QA changed only ReadEvent, TutorAttempt, and their sequence counters. The original snapshot passed SQLite integrity checking. Restoration compared every row in all 11 SQLite tables, including metadata and counters; all matched. Profile identities, app limits, existing reading history, microphone permission, and original display settings were preserved.

Screenshots and the state-comparison receipt are outside the product repository:
[Captured flow evidence](/Users/jonstenstrom/.codex/visualizations/2026/09/06/01a07925-649a-7ec2-b6f3-f5a3dbc028c5/inkling-audit/evidence.md).
