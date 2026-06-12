# Pixel AOD Xposed Module - Antigravity Handoff

Last updated: 2026-06-12

This document summarizes the current local state of the project for handoff.
The current functional code baseline is commit `f1e9403 Fix lockscreen overlap and add AOD verification script`.

## Project Identity

- Workspace root: `D:\Downloads\Xposed_test`
- Project root: `D:\Downloads\Xposed_test\pixel-aod-xposed`
- Android package / Xposed module id: `dev.codex.pixelaod`
- Target process / scope: `com.android.systemui`
- Target device used during development: OnePlus 12, OxygenOS 16.0.6.702, LSPosed
- Latest module version in source: `0.1.89`
- Latest `versionCode`: `90`
- Latest built debug APK:
  `D:\Downloads\Xposed_test\pixel-aod-xposed\app\build\outputs\apk\debug\app-debug.apk`
- Latest build timestamp observed: 2026-06-10 21:27

## Loader / Xposed State

The project has been migrated to modern LSPosed/libxposed metadata:

- `app/src/main/resources/META-INF/xposed/module.prop`
- `app/src/main/resources/META-INF/xposed/java_init.list`
- `app/src/main/resources/META-INF/xposed/scope.list`

Current metadata:

```properties
id=dev.codex.pixelaod
name=Pixel AOD for OPlus
description=Pixel-style AOD and lockscreen clock for OPlus SystemUI
author=Codex
version=0.1.89
versionCode=90
minApiVersion=101
targetApiVersion=101
staticScope=false
```

`java_init.list` points to:

```text
dev.codex.pixelaod.PixelAodModernEntry
```

`scope.list` contains:

```text
com.android.systemui
```

Important implementation details:

- Main modern entry: `PixelAodModernEntry.java`
- It initializes EzXHelper with `EzXposed.initOnModuleLoaded`, `initOnPackageLoaded`, and `initOnPackageReady`.
- Actual business hooks are still routed through `PixelAodHook.install(context, classLoader)`.
- Hook plumbing is wrapped by `ModernHookBridge.java`.
- There is no current `assets/xposed_init` legacy entry in the file list.
- The Gradle dependency is `io.github.kyuubiran.ezxhelper:xposed-api-101:3.2.0-preview1`, excluding bundled libxposed API classes.
- The project also has local `libxposed-stubs` as `compileOnly`.

## Build Environment

Known working build command:

```powershell
$java=(Get-Content D:\Downloads\Xposed_test\.tools\jdk17-java-path.txt -Raw).Trim()
$jdkHome=Split-Path -Parent (Split-Path -Parent $java)
$env:JAVA_HOME=$jdkHome
$env:PATH=(Join-Path $jdkHome 'bin')+';'+$env:PATH
$env:ANDROID_HOME='D:\Android\sdk'
$env:ANDROID_SDK_ROOT='D:\Android\sdk'
cd D:\Downloads\Xposed_test\pixel-aod-xposed
.\gradlew.bat --no-daemon --console=plain :app:assembleDebug
```

The latest full build during development succeeded before the handoff document was created.

## Install / Verify Commands

ADB path used during development:

```powershell
D:\Android\sdk\platform-tools\adb.exe
```

Install latest debug APK:

```powershell
$adb='D:\Android\sdk\platform-tools\adb.exe'
cd D:\Downloads\Xposed_test\pixel-aod-xposed
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb logcat -c
& $adb shell su -c "pkill -f com.android.systemui"
```

Verification helper script added in commit `f1e9403`:

```powershell
cd D:\Downloads\Xposed_test\pixel-aod-xposed
.\tools\verify_aod.ps1 -All
.\tools\verify_aod.ps1 -Scrcpy
.\tools\verify_aod.ps1 -ClearNotification -Aod -Lockscreen -UnlockShade -Logs
.\tools\verify_aod.ps1 -PostNotification -Aod -Lockscreen -Logs
```

Script features:

- Installs the APK when `-Install` is passed.
- Restarts SystemUI when `-RestartSystemUi` is passed.
- Sends and clears the module's test notification.
- Captures AOD, lockscreen, and unlocked notification shade screenshots via `adb exec-out screencap`.
- Captures logcat tag `PixelAodOPlus`.
- Can launch visible scrcpy through `-Scrcpy`.

Device PIN used by previous automation: `0000`.

## Current Features

### AOD Clock

Implemented in `PixelAodClockView.java`.

Current layout constants:

- Large clock text size: `150dp`
- Large clock top: `118dp`
- Compact clock text size: `56dp`
- Compact clock top: `54dp`
- Left edge for compact/info rows: `34dp`
- AOD large date/info top: `72dp`
- AOD compact date/info top: `126dp`
- Max notification icons: `5`
- Battery row top: `720dp`

Behavior:

- Pixel-style two-line large clock when there are no kept AOD notifications.
- Pixel-style compact one-line clock when there are kept AOD notifications.
- Google Sans Flex assets are bundled:
  - `GoogleSansFlex-200.ttf`
  - `GoogleSansFlex-500.ttf`
  - `GoogleSansFlex-Regular.ttf`
- Fallback font is `/system/fonts/AndroidClock.ttf`, then fallback typeface.
- Font variation settings attempt to set `'opsz' 144, 'wght' ...`.
- AOD font default weight is `280`.
- Lockscreen font default weight is `520`.

### Burn-In Protection

Implemented in `PixelAodClockView.applyBurnInTranslation()`.

Current behavior:

- Moves the entire AOD overlay with a slow zigzag translation.
- X amplitude: `16dp`
- Y amplitude: `24dp`
- X period: `43` minutes
- Y period: `271` minutes
- Last AOD translation is cached for AOD-to-lockscreen transition positioning.

How to verify:

- Enable debug logging in the app.
- Capture `PixelAodOPlus` logs and look for `applied Pixel AOD burn-in offset x=... y=...`.
- Take AOD screenshots separated by enough time to observe position drift.

### AOD Notification Icons

Implemented in `PixelAodClockView.rebuildNotificationIcons()`.

Current behavior:

- Uses filtered active `StatusBarNotification` data.
- Deduplicates by package.
- Loads notification `smallIcon`.
- Attempts to reject blocky/bad monochrome icons.
- Falls back to app icon where appropriate.
- Tints output with current info color.
- Hidden/filtered notifications are not included.

Filtering logic:

- Drops module update notification except module test notification.
- Drops `android` and `com.android.systemui`.
- Drops media transport notifications from the notification icon row.
- Drops global lockscreen-disabled notifications.
- Drops `VISIBILITY_SECRET`.
- Drops ranking override/channel visibility secret.
- Drops likely silent / low-importance notifications unless it is the test notification.

Relevant code:

- `PixelAodClockView.sanitizeNotifications(...)`
- `PixelAodClockView.isLockscreenVisibleNotification(...)`
- `PixelAodClockView.RankingSnapshot`

### Media Information

Implemented in `PixelAodClockView`.

Current behavior:

- AOD media row listens to active media sessions / media notifications.
- Attempts to use the media notification's monochrome small icon rather than the app icon.
- Rejects blocky media monochrome candidates and falls back.
- Current latest visual verification did not focus on media playback.

Known historical issue:

- A previous version showed a monochrome media icon only after SystemUI restart; it disappeared after wake/sleep. That path has been worked on, but it should be re-tested with active playback.

### Weather

Implemented through:

- `BreezyWeatherRelayReceiver.java`
- `PixelAodClockView.handleBreezyWeatherIntent(...)`
- A receiver registered by `PixelAodClockView.ensureBreezyWeatherReceiver(...)`

Accepted actions:

- `nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER`
- `org.breezyweather.ACTION_UPDATE_NOTIFIER`
- `dev.codex.pixelaod.REQUEST_BREEZY_WEATHER_RELAY`

Current behavior:

- Caches weather payload in module shared preferences.
- Replays cached Breezy weather to SystemUI when requested.
- Displays temperature text and a weather icon.
- Weather is considered stale after 12 hours.
- `AT_A_GLANCE_EXTRA_ENABLED` is currently `false`, so the old stock At a Glance text experiment is disabled.

Latest screenshots showed weather on AOD and lockscreen.

### Battery / Charging

Implemented in `PixelAodClockView`.

Current behavior:

- AOD bottom battery row shows percent.
- Charging indicator uses a drawn/tinted symbol, not an emoji.
- Avoid reintroducing per-frame lockscreen charging animation: a previous attempt caused severe SystemUI / LSPosed instability.

### Lockscreen Clock

Implemented in `PixelLockscreenClockView.java`.

Current behavior:

- Shows only while device is interactive, keyguard is locked, and bouncer/PIN UI is not visible.
- Compact mode is triggered by active notifications or visible lockscreen notification cards.
- Large lockscreen mode now hides the module date/weather row, because placing it under the large clock caused ugly overlap.
- Compact lockscreen mode keeps date/weather visible below the compact time.
- Module's own lockscreen notification icon row is disabled; lockscreen notification cards are left to the system.

Latest fix:

- In `applyClockMode(false)`, `dateView` is explicitly set `GONE`.
- In compact mode, `dateView` is `VISIBLE`.

### AOD-to-Lockscreen Transition

Implemented in `PixelLockscreenClockView`.

Current behavior:

- On `AodRecord#onDreamingStopped`, the module records the last burn-in translation and prepares a lockscreen transition.
- Lockscreen clock starts from AOD weight and translation, then animates to lockscreen weight / zero translation.
- Transition duration currently `700ms`.

Important caveat:

- The transition exists in code, but the last visual verification did not conclusively prove the font weight animation is visible. Use video/scrcpy or screen recording for the next check.

### Stock OOS Clock / AOD Suppression

Implemented mostly in `PixelAodHook.java`.

Current behavior:

- Hooks stock AOD/keyguard clock draw suppression.
- Hides stock AOD views and stock keyguard clock candidates when the Pixel replacement is active.
- Preserves system AOD media subtree where possible.
- Avoids drawing Pixel AOD overlay inside expanded notification shade.
- Restores hidden stock views when leaving AOD/keyguard contexts.

Latest fix:

- When SystemUI's `NotificationShadeWindowView` is interactive and the device is not keyguard-locked, the hook now immediately:
  - hides Pixel AOD overlay,
  - marks lockscreen surface invisible,
  - refreshes Pixel lockscreen clock visibility,
  - restores adjusted status views,
  - restores hidden stock views,
  - returns before lockscreen replacement.

This was added to fix "after unlocking, expanded notification shade top-left clock sometimes disappears".

### Lockscreen Notification Policy Override

Implemented in `PixelAodHook.java`.

Current behavior:

- Hooks `KeyguardNotificationVisibilityProviderImpl.shouldHideNotification`.
- Attempts fallback hook on keyguard-like `NotifFilter.shouldFilterOut` if available and not abstract.
- Allows lockscreen-eligible non-silent notifications through OOS policy when OOS hides them incorrectly.
- Does not override:
  - system / android notifications,
  - module update notification,
  - media transport notification,
  - `VISIBILITY_SECRET`,
  - ranking/channel secret,
  - silent/low-importance notifications.

Known caveat:

- OOS notification behavior is inconsistent. The override is best-effort and should be retested with real apps after each SystemUI/ROM update.

### Settings UI

Implemented in `SettingsActivity.kt` with Jetpack Compose Material 3.

Current controls:

- Custom AOD toggle
- Lockscreen clock toggle
- Weather toggle
- Notification icons toggle
- Lockscreen notification policy toggle
- Debug logging toggle
- Clock scale slider, range `0.9..1.15`
- AOD weight slider, range `200..420`
- Lockscreen weight slider, range `420..650`

Settings are read from SystemUI through `PixelAodSettingsProvider`.
The hook-side cache TTL is 2 seconds.

## Color State

Current clock/info color implementation is not a hardcoded pure white and not a direct accent-color implementation.

It resolves SystemUI's `wallpaperTextColor` theme attribute when available, then falls back to:

- Clock: `Color.rgb(232, 234, 237)`
- Info: `Color.rgb(218, 220, 224)` with alpha `230`

This was chosen as a system/wallpaper-aware Material-style text color. If the next requirement is specifically "Material You accent color", change `PixelAodClockView.resolveMaterialClockColor(...)` and `resolveMaterialInfoColor(...)`.

## Latest Verification Evidence

Old screenshot clutter has been cleaned from:

```text
D:\Downloads\Xposed_test\screenshots
```

Deleted:

- 53 outdated screenshot/log items
- Approx. 96,755,775 bytes

Kept current evidence only:

```text
D:\Downloads\Xposed_test\screenshots\verify_20260610_213245
D:\Downloads\Xposed_test\screenshots\verify_20260610_213309
```

Contents:

```text
verify_20260610_213245\aod.png
verify_20260610_213245\lockscreen.png
verify_20260610_213245\shade_unlocked.png
verify_20260610_213309\aod.png
verify_20260610_213309\lockscreen.png
```

Relevant logs:

```text
D:\Downloads\Xposed_test\aod_logs_verify_20260610_213245.txt
D:\Downloads\Xposed_test\aod_logs_verify_20260610_213309.txt
```

Observed in latest screenshots:

- AOD custom clock was visible.
- AOD stock OOS giant clock was not visible.
- AOD weather was visible.
- AOD notification icons were visible.
- AOD battery/charging indicator was visible.
- Lockscreen compact mode showed system notification cards without module icon overlay.
- Unlocked expanded notification shade top-left system clock was visible.

Not conclusively verified in the last screenshot pass:

- Truly empty-notification large lockscreen mode. Real device notifications were still present, so lockscreen stayed compact.
- AOD-to-lockscreen font weight animation visibility.
- Media playback icon persistence after wake/sleep.
- Very transient stock AOD flash during screen-off/screen-on transitions.

## Current Screenshot Directory State

After cleanup, `D:\Downloads\Xposed_test\screenshots` contains only:

```text
verify_20260610_213245
verify_20260610_213309
```

No old loose PNGs remain there.

## Known Risk / Regression Areas

1. OOS view tree detection is heuristic.
   - Many suppress/restore decisions are based on class names, resource names, view size, and text signals.
   - Any OOS update can rename or restructure SystemUI views.

2. Stock clock suppression is aggressive.
   - There are safeguards for media, notifications, bouncer, charging, biometric, status bar, and unlocked shade, but this still needs visual checks after any hook change.

3. Notification filtering is policy-sensitive.
   - It tries to follow lockscreen visibility and silent notification rules.
   - OOS itself was observed to behave inconsistently.
   - Always test with several real apps plus USB/system notifications.

4. Weather depends on external broadcasts / cached relay.
   - If Breezy Weather changes broadcast action/extras, weather may disappear.
   - Cached relay helps, but only if the module receiver has already seen useful weather data.

5. AOD-to-lockscreen animation needs better validation.
   - The code path exists, but screenshots are insufficient for validating weight interpolation.
   - Use scrcpy or screen recording.

6. Avoid custom animated charging UI.
   - A previous lockscreen charging animation caused severe jank and SystemUI/LSPosed crashes.
   - Keep charging display static unless implementing a carefully throttled, hardware-tested animation.

## Recommended Next Steps for Antigravity

1. Start by reading:

```text
ANTIGRAVITY_HANDOFF.md
app/src/main/java/dev/codex/pixelaod/PixelAodModernEntry.java
app/src/main/java/dev/codex/pixelaod/PixelAodHook.java
app/src/main/java/dev/codex/pixelaod/PixelAodClockView.java
app/src/main/java/dev/codex/pixelaod/PixelLockscreenClockView.java
tools/verify_aod.ps1
```

2. Run a baseline verification on the connected phone:

```powershell
cd D:\Downloads\Xposed_test\pixel-aod-xposed
.\tools\verify_aod.ps1 -Scrcpy
.\tools\verify_aod.ps1 -ClearNotification -Aod -Lockscreen -UnlockShade -Logs
.\tools\verify_aod.ps1 -PostNotification -Aod -Lockscreen -Logs
```

3. Manually clear real lockscreen notifications and verify large lockscreen mode.

Expected:

- Large lockscreen should show only large time from this module.
- Module date/weather row should not appear below the large lockscreen clock.
- No OOS stock lockscreen clock should overlap it.

4. Verify AOD-to-lockscreen transition with video.

Expected:

- AOD clock starts thin.
- Lockscreen clock transitions toward thicker lockscreen weight.
- No transient stock OOS AOD/lockscreen clock should flash above it.

5. Test media playback.

Expected:

- AOD media info appears.
- Media icon uses the current media notification monochrome small icon when available.
- Icon survives wake/sleep cycles.

6. Test notification policy with real apps.

Expected:

- Non-silent, lockscreen-eligible notifications show on lockscreen.
- Silent / secret / system USB-style notifications do not incorrectly force compact AOD unless policy says they should.
- AOD icon row should not show blank white blocks.

7. If color is revisited, decide explicitly:

- Keep current wallpaper text color behavior.
- Or implement true Material You accent color.
- Or use Pixel-like mostly-white AOD/lockscreen color.

Do not assume the current implementation is pure accent color.

## Useful Commands

Test notification:

```powershell
$adb='D:\Android\sdk\platform-tools\adb.exe'
& $adb shell am broadcast -n dev.codex.pixelaod/.TestNotificationReceiver -a dev.codex.pixelaod.TEST_NOTIFICATION --es title PixelAOD --es text Lockcard
```

Clear test notification:

```powershell
$adb='D:\Android\sdk\platform-tools\adb.exe'
& $adb shell am broadcast -n dev.codex.pixelaod/.TestNotificationReceiver -a dev.codex.pixelaod.CLEAR_TEST_NOTIFICATION
```

Restart SystemUI:

```powershell
$adb='D:\Android\sdk\platform-tools\adb.exe'
& $adb logcat -c
& $adb shell su -c "pkill -f com.android.systemui"
```

Capture relevant logs:

```powershell
$adb='D:\Android\sdk\platform-tools\adb.exe'
& $adb logcat -d -v time -s PixelAodOPlus PixelAodModern
```

Inspect APK Xposed metadata:

```powershell
cd D:\Downloads\Xposed_test\pixel-aod-xposed
tar -xOf app\build\outputs\apk\debug\app-debug.apk META-INF/xposed/module.prop
tar -xOf app\build\outputs\apk\debug\app-debug.apk META-INF/xposed/java_init.list
tar -tf app\build\outputs\apk\debug\app-debug.apk | Select-String -Pattern 'META-INF/xposed|xposed_init|io/github/libxposed'
```

Expected:

- `module.prop`, `java_init.list`, `scope.list` exist.
- No packaged `io/github/libxposed` classes.
- No stale `assets/xposed_init` legacy entry unless intentionally reintroduced.

## Historical Context

Earlier versions had these problems, most of which were addressed by the current code:

- No hook effect because module was disabled or wrong loader/module state was active.
- Triple clocks: stock AOD plus two module clocks.
- System AOD/lockscreen style not fully hidden.
- Module AOD overlay appearing in notification shade.
- Module clock appearing on PIN/bouncer screen.
- AOD notification icons stuck after notification dismissal.
- Notification icon blank white blocks.
- Media monochrome icon disappearing after wake/sleep.
- Weather disappeared after moving between AOD/lockscreen.
- Lockscreen notification cards overlapped by module notification icons.
- USB/system notifications confusing lockscreen/AOD compact mode.
- Charging animation caused severe jank/crashes.

Current state is improved, but OOS SystemUI remains fragile and should be tested after each targeted change.
