# Pixel AOD Xposed Session Changes For Agent

Date: 2026-06-09
Workspace: `D:\Downloads\Xposed_test\pixel-aod-xposed`
Device: OnePlus 12 / OxygenOS 16.0.6.702 via adb
Package: `dev.codex.pixelaod`
Target process: `com.android.systemui`
Loader constraint: legacy LSPosed only. Preserve `app/src/main/assets/xposed_init`.

## Current State Summary

- Project folder is not a Git worktree in this environment: `.git` is absent and `git status` fails.
- Installed device package is `versionCode=69`, `versionName=0.1.68`.
- Installed APK was built and verified before the latest unverified source tweak listed below.
- Current source also says `versionCode 69` / `versionName "0.1.68"`.
- Important mismatch: current source contains one post-install, unverified AOD icon-size tweak. The installed device build may not include that tweak.

## Verified Installed Change: 0.1.68

These changes were built, installed, SystemUI-restarted, and device-verified:

1. `app/src/main/java/dev/codex/pixelaod/PixelLockscreenClockView.java`
   - Changed lockscreen notification icon row behavior:
     - When real lockscreen notification cards are visible, the module passes an empty list to `rebuildNotificationIcons(...)`.
     - This prevents duplicate/faint custom module icons from showing behind real OxygenOS notification cards.
   - Current anchor:
     - `rebuildNotificationIcons(hasCards ? Collections.emptyList() : notifications);`

2. `app/src/main/java/dev/codex/pixelaod/PixelAodHook.java`
   - Narrowed draw-time stock keyguard clock suppression.
   - Before: keyguard draw suppression used broad `looksLikeOplusKeyguardBigClock(marker)` matching.
   - After: draw suppression uses `isStockKeyguardClockDrawCandidate(marker, view)`.
   - New helper split:
     - `isStockKeyguardClockDrawCandidate(...)`
     - `looksLikeOplusKeyguardClockContainer(...)`
     - `looksLikeOplusKeyguardClockText(...)`
   - Intent: keep blocking stock OPlus keyguard clock draw while avoiding non-clock descendants.

3. `app/build.gradle`
   - Bumped:
     - `versionCode 69`
     - `versionName "0.1.68"`

4. `PROJECT_NOTES.md`
   - Added `0.1.68` changelog.
   - Added verified screenshot/log evidence for lockscreen, AOD, and unlocked notification shade.

## Verification Evidence Created This Session

Pre-change baseline after handoff:

- `verification/lockscreen_large_0169.png`
  - Captured on awake keyguard.
  - Not a true no-notification large state because USB debugging/charging card was visible.
- `verification/lockscreen_compact_0170.png`
  - Baseline compact lockscreen with test notification.

Verified 0.1.68 after install:

- `verification/lockscreen_compact_0171.png`
  - Awake keyguard compact mode.
  - Test notification visible.
  - Duplicate module icon row behind real notification cards is no longer visible.
- `verification/lockscreen_compact_0171_state.txt`
  - `mDreamingLockscreen=true`
  - `mWakefulness=Awake`
- `verification/lockscreen_compact_0171_logs.txt`
  - Shows `Pixel lockscreen clock visible compact=true`.
  - No fatal crash evidence in final scan.

- `verification/aod_0172_after_unlock_sleep.png`
  - Unlock-to-sleep AOD capture.
  - Shows Pixel-style AOD with no obvious stock clock overlap.
- `verification/aod_0172_state.txt`
  - `mWakefulness=Dozing`
  - `mDreamingLockscreen=true`
- `verification/aod_0172_logs.txt`
  - Shows stock AOD battery/notification views hidden.
  - No fatal crash evidence in final scan.

Unlocked shade verification on 0.1.68:

- `verification/shade_0173.png`
  - Awake/unlocked notification shade.
  - Red/teal `16:15 Tue, 9 Jun` header is OxygenOS stock shade header, not module overlay.
- `verification/shade_0173_before_pull_state.txt`
  - Before pull: `mDreamingLockscreen=false`, `mWakefulness=Awake`.
- `verification/shade_0173_after_pull_state.txt`
  - After pull: `mCurrentFocus=NotificationShade`, `mDreamingLockscreen=false`, `mWakefulness=Awake`.
- `verification/shade_0173_logs.txt`
  - Shows interactive-shade hide path:
    - `hid Pixel AOD overlays from ... #interactive-shade count=0`
  - No module-owned AOD/lockscreen overlay evidence in unlocked shade.

Additional diagnostic:

- `verification/window_pixelaod_current.xml`
  - UIAutomator dump of current SystemUI hierarchy.
  - It exposes coarse nodes like `keyguard_message_area_container`, but not enough detail to safely target the centered OxygenOS charging/status area.

## Unverified Source Change After 0.1.68 Install

This source edit exists now but was not built, installed, or device-verified:

- `app/src/main/java/dev/codex/pixelaod/PixelAodClockView.java`
  - AOD notification icon constants changed from the prior values to:
    - `NOTIFICATION_ICON_SIZE_DP = 14`
    - `NOTIFICATION_ICON_SPACING_DP = 8`
  - Reason attempted: improve Pixel similarity by making AOD notification glyphs less dominant.
  - Important: this was made after the user questioned relying on older screenshots. Treat it as provisional.
  - Recommended next action:
    - Either revert it before continuing, or first capture fresh current `0.1.68` AOD compact/large screenshots and then decide whether to keep/build it.

## Investigations Without Final Code Change

- Verified current project and device state at session start:
  - Source and installed baseline matched handoff at `versionCode=68`, `versionName=0.1.67`.
  - Current SystemUI logs showed `dev.codex.pixelaod` loaded into `com.android.systemui`.
  - `assets/xposed_init` points to `dev.codex.pixelaod.PixelAodXposedEntry`.
- Checked LSPosed state read-only:
  - `/data/adb/lspd` exists.
  - Config strings indicate `dev.codex.pixelaod` scoped to `com.android.systemui`.
  - Did not modify LSPosed enable/scope state.
- Investigated log noise:
  - `PixelAodOPlus: updated AOD notification ranking lockscreen overrides count=...` logs can repeat frequently.
  - Located source in `PixelAodClockView.updateRankingMap(...)`.
  - No code change was made because the active goal was updated to prioritize Pixel visual similarity.
- Investigated hiding the centered OxygenOS lockscreen charging/status area:
  - UIAutomator dump did not expose precise enough children.
  - No hide rule was added to avoid broad/risky SystemUI suppression.

## Commands Used Successfully

Build:

```powershell
$java = (Get-Content D:\Downloads\Xposed_test\.tools\jdk17-java-path.txt -Raw).Trim()
$jdkHome = Split-Path -Parent (Split-Path -Parent $java)
$env:JAVA_HOME = $jdkHome
$env:PATH = (Join-Path $jdkHome 'bin') + ';' + $env:PATH
$env:ANDROID_HOME = 'D:\Android\sdk'
$env:ANDROID_SDK_ROOT = 'D:\Android\sdk'
.\gradlew.bat --no-daemon --console=plain --rerun-tasks :app:assembleDebug
```

Install and restart SystemUI:

```powershell
adb install -r D:\Downloads\Xposed_test\pixel-aod-xposed\app\build\outputs\apk\debug\app-debug.apk
adb shell pm grant dev.codex.pixelaod android.permission.POST_NOTIFICATIONS
adb logcat -c
$old = adb shell pidof com.android.systemui
adb shell su -c "kill $old"
Start-Sleep -Seconds 10
adb shell pidof com.android.systemui
```

APK identity check:

```powershell
& 'D:\Android\sdk\build-tools\36.1.0\aapt.exe' dump badging 'app\build\outputs\apk\debug\app-debug.apk'
```

## Important Cautions For Next Agent

- Do not treat older screenshots outside `verification/` as current evidence.
- Use fresh screenshots for current visual decisions.
- Do not modify LSPosed module enable/scope state unless user explicitly asks.
- Do not migrate this module to Vector `META-INF/xposed`; preserve legacy `assets/xposed_init`.
- The current source/install mismatch matters:
  - Installed device: verified 0.1.68 build.
  - Source tree: verified 0.1.68 plus one unverified AOD icon-size tweak.
- If continuing similarity work, first capture current AOD large and compact states from the installed build, then compare against the current source before building.
