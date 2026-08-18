# COUI Port Implementation Status

Updated: 2026-08-18

## Current Status

- **Execution owner:** Web Sol direct implementation. The user explicitly disabled further Luna/Codex execution for this work.
- **Current accepted build:** `0.1.361`, version code `371`.
- **Branch:** `agent/coui-port`; M6 UI work from `agent/ui-refactor` has been merged and pushed.
- **Source/build gate:** PASS. The complete debug JVM suite passes **361 tests / 0 failures**; `git diff --check`, `:app:assembleDebug`, signing/metadata checks and final APK inspection pass.
- **Current device state:** 0.1.361 installed on CPH2573 / OP595DL1 (`4a851996`); device base.apk SHA-256 is `8B2C15B51EB2FD85AFAD5361161245ED2D027A68F732D0A93CD22838141B0E06`.
- **Current runtime state:** SystemUI PID `23485` is injected through the Modern entry from the current 0.1.361 base.apk; COUI_PORT clock and UDFPS owners start normally and the legacy primary clock path is blocked.
- **M5:** PASS, including package-clean reinstall, static scope recognition, Manager enable/disable, old `staticScope=false` upgrade and fresh injection evidence.
- **M6:** PASS. The 0.1.360 COUI settings UI was visually accepted by the user and merged into `agent/coui-port`.
- **Next gate:** Phase G comprehensive physical regression / frame-video evidence. No new runtime feature work should start until this matrix is complete.

## COUI Clock / AOD Runtime

- One persistent `CouiClockHostView` owns LS/AOD clock presentation; COUI_PORT is startup-exclusive and blocks the legacy primary clock owner.
- ClockPlugin load/render lifecycle, screen-off origin, partial/panoramic mapping, live AOD retarget and same-host LS↔AOD transitions use the accepted COUI-derived state model.
- The desktop-screen-off LARGE flash regression is fixed; the pre-Keyguard sleep-origin arm prevents stale unlocked renders from exposing a default LARGE host before AOD SMALL.
- AOD/lockscreen transition performance was stabilized by removing duplicate main-thread notification recomputation, coalescing UDFPS refreshes to animation frames, and preventing repeated identical 550 ms presentation restarts.
- Notification content rows snap to final AOD geometry and only fade, matching COUI behavior instead of visibly sliding from an old location.
- Notification overflow contract remains five visible glyphs plus `+x`. In 0.1.361 the overflow label now uses the same resolved Material/Monet accent as the notification glyphs; a physical AOD screenshot with five icons plus `+1` confirms the correction.

## COUI UDFPS Runtime

- COUI UDFPS hooks target the actual OPlus `OnScreenFingerprintUiMech`/pressed-icon ownership points while preserving vendor optical sensing/HBM behavior.
- Idle pressed-carrier ownership follows the physically stable module contract rather than blindly copying COUI's occasional persistent-highlight behavior.
- UDFPS visual refresh is frame-coalesced; HDR window setup is not redundantly resubmitted on every mutation.
- Previous physical acceptance covered real-finger recognition/unlock, press highlight, release cleanup and success ripple. Phase G will re-run the final 0.1.361-visible matrix before final cutover sign-off.

## M5 — LSPosed Static Scope — PASS

Final package contract:

- `META-INF/xposed/module.prop`: `staticScope=true`, `minApiVersion=101`, `targetApiVersion=101`.
- `META-INF/xposed/scope.list`: exactly `com.android.systemui`.
- `META-INF/xposed/java_init.list`: `dev.codex.pixelaod.PixelAodModernEntry`.
- No legacy `assets/xposed_init` and no bundled `io.github.libxposed.*` implementation classes.

Physical acceptance:

- **Package-clean reinstall:** `pm uninstall -k` removed the package body while preserving settings; reinstalling 0.1.361 caused LSPosed Manager to show module Enable off, while the declared static `System UI / com.android.systemui` scope remained automatically selected and marked Recommended. After enabling in Manager and reloading SystemUI, fresh logs loaded the current base.apk through the Modern entry.
- **Disable:** Manager switch off followed by SystemUI reload changed PID `11693 -> 16024`; the new PID contained no Pixel AOD injection lines.
- **Re-enable:** Manager switch on restored fresh Modern injection and COUI_PORT ownership on the next SystemUI instance.
- **Old dynamic-scope upgrade:** exact rollback commit `a1f7e8d` was built as 0.1.331 / 341 / `staticScope=false`. Old and new APK signing certificates match. The old build successfully injected into SystemUI PID `22350`; direct upgrade to 0.1.361 then loaded the new base.apk into PID `23485` with COUI_PORT clock/UDFPS startup.
- **LSPosed / Vector compatibility:** on-device LSPosed Manager is 2.1.1 with framework API 102 and recognizes Pixel AOD as API 101. In this project, Vector compatibility refers to the Modern packaging/API contract above; it does not require installing a second Xposed framework.

Evidence is stored under `.local/m5_static_scope_20260818/` and includes Manager screenshots/XML, clean-reinstall records, old-331 injection logs and 331→361 upgrade injection logs.

## M6 — Settings UI Redesign — PASS

- Three top-level destinations: Home / AOD / System UI; the redundant Settings tab/page was removed and Language lives on Home.
- AOD is a true hub with real child pages for Display & behavior, Clock Style/UDFPS, At a Glance and Lockscreen.
- `PixelAodDesignSystem` owns wallpaper-derived Material dynamic color, typography, shapes, spacing, segmented surfaces, COUI-style switch states, selection dialogs, time picker, disabled states and immersive bottom navigation.
- Home uses one SystemUI restart action instead of redundant navigation shortcuts.
- Existing setting keys, ContentProvider writes, permissions and persistence semantics remain unchanged.
- 0.1.360 passed 360/360 JVM tests and was visually accepted by the user before merge into `agent/coui-port`.

## Final Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Version: `0.1.361` / `371`
- Size: `19,764,215` bytes
- SHA-256: `8B2C15B51EB2FD85AFAD5361161245ED2D027A68F732D0A93CD22838141B0E06`
- Signing certificate SHA-256: `02DBFA7C632AB6F67112DA0C5C3096B4B1C3B622791ED23A9167443487BEDD4F`

## Next Stage — Phase G

Run one final physical acceptance matrix against the current 0.1.361 build, preserving fresh LSPosed logs and visual/video evidence for: empty/media/notifications/media+notifications, LS Large/Small/Immersed, LS↔AOD, live retarget, partial/panoramic, UDFPS touch/unlock/timeout, weather/forecast/alert/calendar, USB/hotspot/system-status icons, black-frame/power behavior and owner/crash regressions. Any visible regression blocks Slice 3 / media-capsule-immersed work.
