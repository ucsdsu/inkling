# Boox acceptance

Status: hardware acceptance is open. Emulator checks do not prove e-ink refresh,
offline speech quality, or Boox accessibility behavior.

Target: Onyx BOOX Go Color 7 Gen II. No factory reset or Device Owner setup.

## Before installation

1. Keep the current APK if Inkling is already installed.
2. Back up existing Inkling data before upgrading a pre-release database.
3. Record the current Home app and accessibility settings.
4. Confirm that the parent can reach Android Settings and the stock launcher.
5. Install only after Jon approves the device trial.

## Launcher and parent controls

1. Complete the child profile and placement, or use Skip.
2. Set the parent PIN.
3. Choose Inkling as the Home app.
4. Enable the Inkling accessibility service.
5. Enable the intended child apps in the parent Apps tab.
6. Turn Kids mode on.
7. Open each allowed app from its tile. Press Home. Confirm that kid home appears.
8. Try an app that is not allowed. Confirm that Inkling returns within 1 second.
9. Set an allowed app's cap to 5 minutes. Use it through the cap. Confirm that the
   warning appears, Inkling returns, and the tile says Done for today.
10. Unlock the parent screen. Turn Kids mode off. Tap Open Boox home. Confirm that
    the stock launcher opens.
11. Restart the tablet. Confirm that Inkling opens as Home and blocking still works.

After an APK update, toggle the accessibility service off and on before testing.
Force-stopping Inkling can unbind the service. Check it again after any force-stop.

## Reading and profile isolation

1. Turn Wi-Fi off.
2. Open a book. Tap Read to me. Confirm that the voice is audible and highlights
   follow the words.
3. Turn a page during speech. Confirm that the old page stops speaking.
4. Start I'll read. Turn a page. Confirm that the old result does not coach the new page.
5. Press Back and Home during speech. Confirm that speech stops.
6. Finish a book. Check Reading and Next up in the parent screen.
7. Add a second child. Confirm that the same parent PIN protects that profile.
8. Switch back. Confirm that the first child's reading records and app limits are unchanged.
9. Check portrait and landscape. Confirm that controls remain reachable and page
   changes leave readable text without persistent ghosting.

If offline recognition or an offline English voice is unavailable, record the
notice. Keep reading and Skip usable. Do not enable an online fallback to pass.

## Cove's speech measurement

1. Keep Wi-Fi off. Use a normal room.
2. Open the parent Rules tab. Tap Speech test.
3. Have Cove read each of the 20 lines.
4. Label each result from what Cove actually said: Read it right, Missed a word,
   or Unclear.
5. Save the completed summary and export the CSV from Rules.
6. Report the number of parent-correct reads that were flagged, divided by the
   number of parent-correct reads. Exclude wrong and unclear reads from that denominator.
7. Require 20 parent-correct observations before closing the original 20-correct-read
   gate. Pass requires no more than 2 false flags out of 20. A smaller sample remains incomplete.

Do not combine earlier QA rows with the hardware run. Keep the run's exported data
and its boundaries together. Missing recognition is an unavailable test, not a pass.

## Return to the original setup

1. Unlock the parent screen.
2. Turn Kids mode off.
3. Change the Android Home app back to the recorded stock launcher.
4. Disable the Inkling accessibility service.
5. Preserve the test data and APK until the result is reviewed.

Do not clear app data or uninstall to recover the launcher. Those actions remove
local reading records. If the parent route fails, use Android Settings to change
the Home app and disable the service.
