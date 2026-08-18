# COUI Port Implementation Status

Updated: 2026-08-18

## Current Status

- **Execution owner:** Web Sol direct implementation. The user explicitly disabled further Luna/Codex execution for this work; historical executor notes do not authorize resuming it.
- **Current build:** `0.1.347`, version code `357`.
- **Source/build gate:** PASS. The complete debug JVM suite passes **332 tests**; `git diff --check`, Kotlin/Java compilation, `:app:assembleDebug --rerun-tasks`, and final APK/Xposed metadata inspection pass.
- **Physical gate:** OPEN. The final direct build has not been installed/accepted because the last verified ADB endpoint `127.0.0.1:15556` became `offline`. No reconnect loop, ADB-server restart, or phone reboot was performed.
- **User-visible acceptance still required:** AOD→lockscreen black-frame behavior; Small AOD→Small lockscreen position + weight transition; lockscreen UDFPS idle/touch/release/auth highlight; LSPosed static-scope runtime injection; settings light/dark visual review.

## COUI Clock / AOD Runtime

The production path now follows the checked COUI Expressive 2.5 ClockPlugin ownership model instead of module-invented transient visibility states:

- `ClockPlugin#loadPluginReal` / real `render` callbacks are the presentation owners; unrelated notification/weather/context callbacks only refresh semantic data.
- UI state `0` holds without changing presentation; non-zero states with a known clock scene continue using the same persistent `CouiClockHostView` rather than hiding/recreating it.
- `AOD_SMALL -> LS_SMALL` exits through one `present(...)` transaction so X/Y/burn-in removal and variable-font weight morph are scheduled together on the same 550 ms transition.
- Partial AOD keeps COUI's requested-LARGE/content-derived-visual-SMALL model; panoramic AOD keeps last-lockscreen-scene fallback behavior.
- `beginAodEntry(...)` is restricted to the screen-off-from-unlocked normalization path; ordinary lockscreen→AOD goes through the direct presentation transition.
- COUI_PORT is now the missing/invalid configuration default; explicit `legacy` remains a startup-only rollback selector. Only one primary clock owner is installed for a startup.
- Notification overflow remains the user's explicit contract: at most five visible icons plus `+x` for the remainder.

## COUI UDFPS Runtime

The UDFPS hook targets were corrected against COUI Expressive 2.5 and the target OPlus SystemUI classes:

- `updateFpIconAlpha`, `checkHasPressedAnimation`, and `getScalePressedAnim` are hooked on `OnScreenFingerprintUiMech`, where the vendor actually owns these decisions.
- The vendor pressed-icon constructor only configures the carrier; it no longer reads a stale previous `lastUiMech` touch state or activates HDR during construction.
- Normal visual refresh reads live OPlus `isTouchDownNow` and AOD flags instead of maintaining a parallel module SHOW/HIDE/TOUCH visibility lifecycle.
- HDR/non-HDR press visuals use the live vendor touch state while vendor pressed-icon visibility/HBM ownership remains intact.
- Previous M1 real-finger optical recognition/unlock evidence remains historical proof that the sensing path can work, but it is **not** reused as acceptance for the new `0.1.347` code.

## M5 — LSPosed Static Scope

Implemented and package-validated:

- `META-INF/xposed/module.prop`: `staticScope=true`.
- `META-INF/xposed/scope.list`: exactly `com.android.systemui`.
- `META-INF/xposed/java_init.list`: `dev.codex.pixelaod.PixelAodModernEntry`.
- Android application metadata exposes `android:description`; SettingsActivity exposes the LSPosed module-settings category.
- Xposed API target remains 101 intentionally; the build keeps the existing compile-only Modern API boundary.
- Final APK contains no legacy `assets/xposed_init` and no bundled `io.github.libxposed.*` implementation classes.
- Runtime clean-install/upgrade injection confirmation remains pending until the final APK is installed.

## M6 — Settings UI Redesign

Implemented:

- New shared `PixelAodDesignSystem.kt` owns dynamic Material 3 color, typography, shapes, spacing, surfaces, motion, page scaffold, sections/groups, hero/toggle/choice/slider rows, and shared selection dialogs.
- `SettingsActivity.kt` has migrated the complete settings surface to the design system; the former page-private `Coui*` component block was removed.
- Language, AOD display mode, calendar-icon app, and weather-icon-pack dialogs use one shared selection component; time selection remains Material 3 under the same theme.
- Existing setting keys, ContentProvider writes, permission flows, schedule values, language behavior, and persistence semantics are preserved.
- Real-device light/dark screenshot parity remains a visual acceptance follow-up, not a source/build blocker.

## Additional Final-Test Repair

Full-suite validation exposed and fixed an older weather deadline-composition defect: a disabled forecast's `0` deadline could overwrite a positive active-alert deadline. `AtAGlanceWeatherPolicy.earliest(...)` now ignores non-positive candidates when a valid current deadline exists. The associated timezone forecast test was also corrected to compare the same instant under UTC and Singapore local-date semantics. After these corrections, all 332 debug JVM tests pass.

## Final Artifact

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Version: `0.1.347` / `357`
- Size: `19,718,663` bytes
- SHA-256: `EB057DEAE2B268DCFA844594A58FD3BD8F80338FED46C76B4A683CBB2BA2ADF2`

No commit, push, reset, clean, revert, phone reboot, or ADB-server restart was performed.
