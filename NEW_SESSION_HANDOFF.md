# Pixel AOD Xposed New Session Handoff

Date: 2026-06-09
Workspace: `D:\Downloads\Xposed_test`
Project: `D:\Downloads\Xposed_test\pixel-aod-xposed`
Package: `dev.codex.pixelaod`
Target process: `com.android.systemui`
Device: OnePlus 12 / OxygenOS 16.0.6.702, connected through adb
Loader: LSPosed legacy entry only. Preserve `app/src/main/assets/xposed_init`.

## New Session Goal

Use this as the new `/goal` text:

```text
Continue the Pixel AOD Xposed module in D:\Downloads\Xposed_test\pixel-aod-xposed.

Build an LSPosed/Xposed module for OnePlus 12 OxygenOS 16.0.6.702 that replaces the stock OxygenOS AOD and lockscreen clock style with a Google Pixel-like style:

1. AOD:
   - Show a Pixel-style clock.
   - Large mode when no lockscreen-visible notifications exist.
   - Compact left-aligned mode when lockscreen-visible notifications exist.
   - Notification icons should be monochrome native-style.
   - Preserve the stock OxygenOS AOD media card.
   - Also provide a Pixel-style media line with the current media notification small icon.
   - Move battery/charging info near the lower screen and use a vector charging indicator, not emoji.

2. Lockscreen:
   - Replace the stock OxygenOS lockscreen clock with a Pixel-like lockscreen clock.
   - Implement large and compact lockscreen clock states similar to Pixel.
   - Hide stock OxygenOS lockscreen clock only when the custom replacement is valid.
   - Avoid showing custom lockscreen/AOD clocks in the unlocked notification shade.

3. Transitions:
   - Avoid system AOD/lockscreen clock overlapping module clocks during lockscreen to AOD and unlock to AOD transitions.
   - AOD clock should appear visually thinner than lockscreen clock.
   - Continue improving transition behavior; explicit animated interpolation is not yet implemented.

4. Process:
   - Keep a TODO list, changelog, and known bugs in PROJECT_NOTES.md.
   - Validate changes on the connected device with screenshots/logcat.
   - Use one-shot adb/PowerShell command batches for unlock -> shade -> screenshot or unlock -> sleep -> AOD screenshot tests to avoid auto-sleep causing misleading captures.
   - Do not modify LSPosed module enable/scope state unless the user explicitly asks.
   - Preserve the legacy LSPosed entry in assets/xposed_init; do not migrate to Vector META-INF/xposed for this module.
```

## Current Source State

Latest installed/build version:

- `versionCode 68`
- `versionName 0.1.67`

Important files:

- `app/src/main/java/dev/codex/pixelaod/PixelAodClockView.java`
- `app/src/main/java/dev/codex/pixelaod/PixelAodHook.java`
- `app/src/main/java/dev/codex/pixelaod/PixelLockscreenClockView.java`
- `app/src/main/java/dev/codex/pixelaod/TestNotificationReceiver.java`
- `app/src/main/res/drawable/ic_stat_pixel_aod_test.xml`
- `app/src/main/assets/xposed_init`
- `PROJECT_NOTES.md`

Recent key changes:

- `0.1.63`: Added `PixelAodClockView.hideAllAodOverlays()` and called it from screen-on, AOD stop, and interactive shade host paths.
- `0.1.64`: Added AOD draw-time suppression when the shared `NotificationShadeWindowView` root contains expanded notification shade content.
- `0.1.65`: Tightened lockscreen custom clock visibility to `KeyguardManager.isKeyguardLocked()` only, and removed ordinary notification-card presence from lockscreen surface detection.
- `0.1.66`: Added `View#draw(Canvas)` stock AOD/keyguard clock suppression to skip stock clock drawing earlier than post-hide.
- `0.1.67`: Fixed 0.1.66 false positive by skipping stock draw suppression for any view under `PixelAodClockView` or `PixelLockscreenClockView`.

## Verified Evidence

Files in `D:\Downloads\Xposed_test`:

- `aod_screencap_0168_after_unlock_sleep.png`
  - Captured after unlock -> sleep -> AOD on 0.1.67.
  - Result: no obvious stock AOD clock/module AOD overlap in the screenshot.

- `shade_screencap_0168_state_checked.png`
  - Captured after unlock -> pull notification shade on 0.1.67.
  - State files prove it was unlocked:
    - `shade_state_0168_before_pull.txt`: launcher focused, `mDreamingLockscreen=false`, `mWakefulness=Awake`.
    - `shade_state_0168_after_pull.txt`: `NotificationShade` focused, `mDreamingLockscreen=false`, `mWakefulness=Awake`.
  - The red/teal `14:57 Tue, 9 Jun` at top is believed to be the system unlocked notification shade header, not module AOD/lockscreen overlay. Reason: module clocks use single-color `TextView`s and do not draw red-hour/teal-minute segmented text.

- `shade_logs_0168_state_checked.txt`
  - Shows module hide paths ran:
    - `hid Pixel AOD overlays from ... #interactive-shade count=0`
    - `Pixel lockscreen visible notification cards=true` after pull, but lockscreen clock should still not show because `isKeyguardLocked()` is false.

No `AndroidRuntime` or `FATAL` was seen in the final quick log check after 0.1.67 install.

## Important Interpretation

Earlier "AOD in notification shade" reports were partly caused by the module attaching custom views to `NotificationShadeWindowView`, which OxygenOS reuses across AOD, keyguard, and the unlocked notification shade.

However, after 0.1.67, the visible red/teal clock in the unlocked notification shade appears to be OxygenOS' own shade header, not module residue. Do not blindly hide all red/teal shade header clocks unless the user explicitly wants to redesign the unlocked notification shade too.

The user's latest question asks whether stock lockscreen/AOD styles can be completely blocked. Best answer:

- We can get closer by blocking stock AOD/keyguard clock at `draw(Canvas)` level.
- It is already implemented in 0.1.67 with guardrails.
- A theoretically perfect first-frame block may require hooking stock class initialization/attach/visibility/draw even earlier.
- Avoid affecting normal unlocked notification shade header unless requested.

## Build Commands

```powershell
$java = (Get-Content D:\Downloads\Xposed_test\.tools\jdk17-java-path.txt -Raw).Trim()
$jdkHome = Split-Path -Parent (Split-Path -Parent $java)
$env:JAVA_HOME = $jdkHome
$env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
$env:ANDROID_HOME = 'D:\Android\sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\sdk'
cd D:\Downloads\Xposed_test\pixel-aod-xposed
.\gradlew.bat --no-daemon --console=plain :app:assembleDebug
```

Install:

```powershell
adb install -r D:\Downloads\Xposed_test\pixel-aod-xposed\app\build\outputs\apk\debug\app-debug.apk
adb shell pm grant dev.codex.pixelaod android.permission.POST_NOTIFICATIONS
```

Restart SystemUI:

```powershell
adb logcat -c
$old = adb shell pidof com.android.systemui
adb shell su -c "kill $old"
Start-Sleep -Seconds 10
adb shell pidof com.android.systemui
```

## One-Shot Verification Commands

Device PIN is `0000`. Use one-shot command batches to avoid auto-sleep during reasoning.

Unlock -> pull notification shade -> screenshot:

```powershell
adb logcat -c
adb shell input keyevent 224
Start-Sleep -Milliseconds 700
adb shell input swipe 540 1850 540 550 220
Start-Sleep -Milliseconds 300
adb shell input text 0000
adb shell input keyevent 66
Start-Sleep -Milliseconds 1500
adb shell dumpsys window | Select-String -Pattern "mCurrentFocus|mDreamingLockscreen|mShowingDream|mKeyguard" > D:\Downloads\Xposed_test\shade_state_next_before_pull.txt
adb shell dumpsys power | Select-String -Pattern "mWakefulness|mInteractive" >> D:\Downloads\Xposed_test\shade_state_next_before_pull.txt
adb shell input swipe 540 0 540 1250 420
Start-Sleep -Milliseconds 700
adb shell dumpsys window | Select-String -Pattern "mCurrentFocus|mDreamingLockscreen|mShowingDream|mKeyguard" > D:\Downloads\Xposed_test\shade_state_next_after_pull.txt
adb shell dumpsys power | Select-String -Pattern "mWakefulness|mInteractive" >> D:\Downloads\Xposed_test\shade_state_next_after_pull.txt
adb exec-out screencap -p > D:\Downloads\Xposed_test\shade_screencap_next.png
adb logcat -d | Select-String -Pattern "Pixel lockscreen|suppressed Pixel lockscreen|suppressed Pixel AOD|stock clock draw|Pixel AOD overlay|AndroidRuntime|FATAL" > D:\Downloads\Xposed_test\shade_logs_next.txt
```

Unlock -> sleep -> AOD screenshot:

```powershell
adb logcat -c
adb shell input keyevent 224
Start-Sleep -Milliseconds 700
adb shell input swipe 540 1850 540 550 220
Start-Sleep -Milliseconds 300
adb shell input text 0000
adb shell input keyevent 66
Start-Sleep -Milliseconds 1500
adb shell input keyevent 223
Start-Sleep -Milliseconds 3500
adb exec-out screencap -p > D:\Downloads\Xposed_test\aod_screencap_next.png
adb shell dumpsys power | Select-String -Pattern "mWakefulness|mInteractive" > D:\Downloads\Xposed_test\aod_state_next.txt
adb shell dumpsys window | Select-String -Pattern "mCurrentFocus|mDreamingLockscreen|mShowingDream|mKeyguard" >> D:\Downloads\Xposed_test\aod_state_next.txt
adb logcat -d | Select-String -Pattern "suppressed stock clock draw|hid stock AOD view|Pixel AOD active|Pixel AOD overlay|AndroidRuntime|FATAL" > D:\Downloads\Xposed_test\aod_logs_next.txt
```

Test notification:

```powershell
adb shell am broadcast -a dev.codex.pixelaod.TEST_NOTIFICATION -n dev.codex.pixelaod/.TestNotificationReceiver --es title "Pixel AOD Test" --es text "Lockscreen notification"
```

Clear test notification:

```powershell
adb shell am broadcast -a dev.codex.pixelaod.CLEAR_TEST_NOTIFICATION -n dev.codex.pixelaod/.TestNotificationReceiver
```

## Next Work

1. Re-check lockscreen normal large mode and notification compact mode after 0.1.65+ visibility tightening.
2. If user still sees short overlap after unlock -> sleep, capture a short video or rapid screenshot sequence; single screenshots may miss a sub-second first-frame issue.
3. Improve stock AOD/keyguard draw suppression by narrowing matchers to avoid any non-clock view impact.
4. Decide whether the unlocked notification shade header should be left alone. Current recommendation: leave it alone unless user explicitly asks to customize shade header too.
5. Reduce debug logging once behavior stabilizes.
6. Continue Pixel style refinement against screenshots/source references.

## Communication Notes

- The user prefers direct factual answers and expects device-side validation.
- If running device UI tests, use one-shot PowerShell command batches instead of step-by-step commands with long pauses.
- Do not call the red/teal unlocked shade header a module AOD overlay unless evidence proves it is module-owned.
- If ambiguity remains, ask specifically whether they want to modify the unlocked notification shade header in addition to lockscreen/AOD.
