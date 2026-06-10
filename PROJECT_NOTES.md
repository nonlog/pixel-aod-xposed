# Pixel AOD Xposed Notes

Last updated: 2026-06-09

## TODO

- [x] Preserve the legacy LSPosed entry point through `assets/xposed_init`.
- [x] Replace the stock OxygenOS AOD clock with a Pixel-style two-line clock.
- [x] Switch to a compact, left-aligned clock when lockscreen-visible notifications exist.
- [x] Keep AOD notification icons monochrome instead of falling back to colored app icons.
- [x] Use bundled Google Sans Flex assets for the AOD clock and info lines.
- [x] Preserve the stock OxygenOS AOD media card while adding a Pixel-style media line.
- [x] Move battery percentage and charging indicator near the lower AOD area.
- [x] Display Breezy Weather data on the Pixel-style AOD date line.
- [x] Tint the AOD clock/info/icons with Material You accent resources.
- [x] Implement Pixel-style lockscreen clock replacement, separate from AOD state.
- [x] Display Breezy Weather data on the Pixel-style lockscreen date line.
- [x] Add AOD-to-lockscreen clock weight animation.
- [ ] Hide the stock OxygenOS lockscreen clock only after the custom lockscreen clock is visible.
- [ ] Verify all layout states on device: no notification, notification, media only, charging, Chinese locale, 12-hour time.
- [ ] Reduce debug log noise once layout and lifecycle behavior stabilize.

## Changelog

### 0.1.76

- Fixed weather disappearing after SystemUI restart by persisting Breezy/Gadgetbridge weather extras in the module app receiver and adding a SystemUI-side replay request path. The AOD/lockscreen render path now also requests cached weather when its in-memory weather snapshot is empty.
- Broadened direct weather extra parsing so ADB/Breezy-style numeric extras stored as float/string values can still produce a displayable temperature.
- Made the AOD-to-lockscreen clock weight animation visible instead of only logically triggered: the lockscreen clock now keeps the AOD weight for one frame, delays animation start by 140 ms, animates for 700 ms, and uses a stronger 280 -> 520 weight range.
- Built and installed on the connected OnePlus 12 as `versionCode 77` / `versionName 0.1.76`.
- Device verification: `verification/aod_0176_weather_cache.png` shows the AOD date line restored with weather (`30°` plus weather icon) after SystemUI restart. Logs show the lockscreen weight transition was scheduled from 280 to 520 and consumed from `AodRecord#onDreamingStopped` within 33 ms. No fatal AndroidRuntime crash was observed in the captured logs.

### 0.1.73

- Added a narrow SystemUI-side lockscreen notification policy override. When OxygenOS/Keyguard hides a notification that still passes this module's lockscreen eligibility rules, the module now allows it through `KeyguardNotificationVisibilityProvider`, with a `NotifFilter` fallback for keyguard/lockscreen-style filters.
- The override deliberately still excludes `VISIBILITY_SECRET`, system `android` / `com.android.systemui` records, media transport notifications, and silent or low-importance notifications, so USB/system and LSPosed secret notifications should remain unchanged.
- Reworked the AOD-to-lockscreen clock weight transition so `AodRecord#onDreamingStopped` explicitly marks the next lockscreen frame for animation, and the lockscreen clock consumes that marker within a short window before animating from AOD weight to lockscreen weight.
- Built and installed on the connected OnePlus 12 as `versionCode 74` / `versionName 0.1.73`.
- Device verification: `verification/lockscreen_0173_after_restart_policy.png` shows multiple normal third-party lockscreen cards after SystemUI restart. `verification/logs_0173_restart_full.txt` confirms the override allowed `com.tencent.mm`, `org.breezyweather`, `dev.codex.pixelaod`, `com.taobao.taobao`, and `com.google.android.apps.youtube.music` through `KeyguardNotificationVisibilityProvider`.
- Device verification: `verification/logs_0173_after_restart_policy.txt` confirms the AOD-to-lockscreen weight transition marker was prepared on `AodRecord#onDreamingStopped` and consumed on the next lockscreen update within 67 ms.
- Device verification: `verification/aod_0173_policy.png` was captured with `mWakefulness=Dozing`; it shows the module AOD still rendering normally with stock AOD battery/notification views hidden. No `AndroidRuntime` fatal crash was observed in the captured verification logs.

### 0.1.72

- Investigated the `notifications_*.png` state with `verification/notification_0171_dump.txt` and live `dumpsys notification --noredact`.
- `Xposed module update` notifications are posted as `pkg=android` on channel `lsposed_module_updated` with `vis=SECRET`; this explains why stock OxygenOS lockscreen hides them and why the module AOD filter also excludes them.
- USB charging/debugging notifications are `pkg=com.android.systemui`, channel `INS`, `ONGOING_EVENT|NO_CLEAR`, and `vis=PRIVATE`. OxygenOS still renders these system USB records on the lockscreen even when they are in the silent/low-priority path; the module continues to filter them from AOD as system/low-priority notifications.
- Normal user notifications in the dump (`com.tencent.mm`, `com.reddit.frontpage`, `com.taobao.taobao`, `com.google.android.apps.youtube.music`) are lockscreen-eligible by Android fields and are shown by the module on AOD, but OxygenOS native lockscreen cards still suppress them in the captured state. Treat this as an OxygenOS native lockscreen policy/state issue, not an AOD module filter issue.
- Fixed the lockscreen overlap seen in `screenshots/notifications_lockscreen.png` by adding an OxygenOS card-content fallback detector: large rounded lockscreen rows can now be recognized through geometry plus relative-time/text signals even when their class names do not expose the usual `notification`/`expandable` markers.
- Built and installed debug APK on the connected OnePlus 12 as `versionCode 73` / `versionName 0.1.72`. Verified legacy `assets/xposed_init` still points to `dev.codex.pixelaod.PixelAodXposedEntry`.
- Device verification: `verification/lockscreen_0172_wake.png` shows the USB aggregate card without the module's custom notification icon row overlapping the card. `dumpsys package dev.codex.pixelaod` reports `versionCode=73` / `versionName=0.1.72`. No `AndroidRuntime` fatal crash was observed in `verification/logs_0172_lockscreen_wake.txt`.

### 0.1.71

- Fixed AOD notification icons that rendered as a solid dot/block when an app notification small icon is just a filled mask. The icon loader now tries the app adaptive monochrome icon first, then falls back to the app's colored launcher icon instead of tinting an unusable mask.
- Changed OPlus AOD `NotificationView` notification arrays to merge into the cached SystemUI notification snapshot instead of replacing it. This avoids OxygenOS' AOD-specific subset hiding otherwise lockscreen-eligible notifications from the module.
- Relaxed the ranking filter so OxygenOS suppressed visual effect flags are logged but no longer treated as an absolute hide signal. Secret notifications and low-importance/silent notifications are still filtered.
- Added more diagnostic logging for kept/filtered notifications and icon fallback mode.
- Built and installed debug APK on the connected OnePlus 12 as `versionCode 72` / `versionName 0.1.71`. Verified legacy `assets/xposed_init` still points to `dev.codex.pixelaod.PixelAodXposedEntry`.
- Device verification: `verification/aod_0171_after_restart.png` was captured while `mWakefulness=Dozing` / `mDreamingLockscreen=true`. It shows four module AOD icons (`com.tencent.mm`, `com.reddit.frontpage`, `com.taobao.taobao`, `com.google.android.apps.youtube.music`) with no white block. Logs show `com.taobao.taobao` used `app-color-fallback filled=true tiny=false`, `input=4 emitted=4 loadFailures=0`, and active notification capture from SystemUI remained `count=17`. No `AndroidRuntime` fatal crash was observed.

### 0.1.70

- Replaced weather condition text (`Sunny`, `Cloudy`, etc.) with a small self-drawn monochrome weather icon. The AOD/lockscreen date line now displays `date · temperature` plus an icon.
- Added weather display to `PixelLockscreenClockView`, using the same Breezy/Gadgetbridge cached weather snapshot as AOD.
- Increased compact small-clock vertical rhythm: AOD compact date/notification/media rows moved down by 8dp; lockscreen compact date/notification rows moved down by 10dp.
- Added a lockscreen clock weight transition based on AOSP Pixel clock behavior: AOSP `AnimatableClockView` uses separate doze and lockscreen weights and animates between them during doze transitions. This module now animates the custom lockscreen clock from the AOD weight to the lockscreen weight over 300ms when the lockscreen clock first appears.
- Built and installed debug APK on the connected OnePlus 12 as `versionCode 71` / `versionName 0.1.70`.
- Device verification screenshots: `verification/aod_0170_weather_icon.png` shows large AOD with `27°` and a cloud icon, no `Cloudy` text; `verification/lockscreen_0170_weather_icon.png` shows the same weather treatment on lockscreen; `verification/aod_0170_compact_weather_icon.png` shows compact AOD spacing with test notification. Test notification was cleared after capture. No fatal AndroidRuntime crash was observed.

### 0.1.69

- Added `BreezyWeatherRelayReceiver` so Breezy Weather's Gadgetbridge data-sharing broadcast can target `dev.codex.pixelaod`; the receiver relays weather extras into the `com.android.systemui` process.
- Registered a SystemUI-side Breezy weather receiver and parsed Breezy/Gadgetbridge `WeatherJson` plus `WeatherGz` fallback. AOD date text now prefers fresh Breezy weather and displays `date · temperature condition`.
- Changed AOD clock, date/media/battery text, charging bolt, notification icons, and media icons to use Material You accent resources (`system_accent1_100` / `system_accent1_200`) with safe fallbacks.
- Added stock AOD weather/extra view draw/hide detection to reduce the leftover brief weather-symbol flash from OxygenOS/At-a-Glance-style nodes.
- Built and installed debug APK on the connected OnePlus 12 as `versionCode 70` / `versionName 0.1.69`. Verified legacy `assets/xposed_init` still points to `dev.codex.pixelaod.PixelAodXposedEntry`.
- Device verification: simulated Breezy broadcast with `WeatherJson` and confirmed logs `PixelAodBreezyRelay: relayed Breezy weather` and `PixelAodOPlus: updated Breezy weather from broadcast text=27° Cloudy`. Screenshot `verification/aod_0169_weather.png` shows `Tue, Jun 9 · 27° Cloudy` in Material You accent color on AOD. `dumpsys package dev.codex.pixelaod` shows `BreezyWeatherRelayReceiver` registered for `nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER` and `org.breezyweather.READ_PROVIDER: granted=true`.

### 0.1.68

- Hid the custom lockscreen notification icon row when real lockscreen notification cards are visible, avoiding duplicate/faint icons behind notification cards.
- Narrowed draw-time stock keyguard clock suppression to known stock clock containers and explicit digital-time text leaves instead of every descendant under `com.oplus.keyguard.clock.big.*`.
- Fresh baseline verification before this change showed `lockscreen_compact_0170.png` correctly entered compact mode with the module clock and stock clock suppression active. `lockscreen_large_0169.png` was not a true no-notification state because the USB debugging/charging card remained lockscreen-visible.
- Built, installed, and verified on device as `versionCode 69` / `versionName 0.1.68`. `verification/lockscreen_compact_0171.png` shows compact lockscreen without the duplicate custom icon row behind notification cards. `verification/aod_0172_after_unlock_sleep.png` shows unlock-to-sleep AOD without visible stock clock overlap.
- Verified unlocked notification shade after 0.1.68 with `verification/shade_0173.png`, `verification/shade_0173_before_pull_state.txt`, and `verification/shade_0173_after_pull_state.txt`. The capture was awake/unlocked (`mDreamingLockscreen=false`) and showed only the stock OxygenOS red/teal shade header, not module-owned AOD or lockscreen overlays. Logs show the interactive-shade hide path ran.

### 0.1.67

- Fixed the 0.1.66 draw-suppression false positive where module-owned `TextView` children under `PixelAodClockView` could be mistaken for stock AOD text.
- Added an ancestor guard so stock draw suppression never targets views inside `PixelAodClockView` or `PixelLockscreenClockView`.
- Installed and verified on device. `aod_screencap_0168_after_unlock_sleep.png` shows no stock AOD clock/module AOD overlap after unlock-then-sleep.
- Verified `shade_screencap_0168_state_checked.png` was captured while unlocked (`mDreamingLockscreen=false`, `mWakefulness=Awake`). The red/teal time at the top is the system notification shade header, not the module overlay; module clocks are single-color.

### 0.1.66

- Added a `View#draw(Canvas)` level stock AOD/keyguard clock suppression hook to skip drawing recognized stock AOD clock and stock OPlus keyguard clock views.
- Intended to reduce the short stock-system/module overlap during unlock-then-sleep transitions.
- Superseded by 0.1.67 because the first suppression matcher could also catch module-owned clock text.

### 0.1.65

- Tightened lockscreen visibility to `KeyguardManager.isKeyguardLocked()` only; stale notification-card state no longer keeps the custom lockscreen clock visible after unlock.
- Removed notification-card presence from `isLikelyLockscreenSurfaceVisible()` because the unlocked notification shade also contains notification cards.
- Added a draw-time guard in `PixelLockscreenClockView` so it does not render outside keyguard even if stale host state remains.

### 0.1.64

- Added draw-time AOD overlay suppression when the shared `NotificationShadeWindowView` root contains expanded notification shade content.
- Added detection for visible notification shade content so the AOD overlay can avoid painting into unlocked shade paths.

### 0.1.63

- Added `PixelAodClockView.hideAllAodOverlays()` and called it from screen-on, AOD stop, and interactive shade host paths.
- This addressed cases where `AodRecord#onDreamingStopped` or screen-on broadcasts were not observed before the notification shade reused the same root.

### 0.1.56

- Removed the remaining AOD clock `TextView#setTextScaleX(...)` calls so digit glyphs are never horizontally squeezed.
- Reduced large/compact negative clock letter spacing to `-0.02/-0.025`.
- Raised AOD clock weight from 200 to 280 and lockscreen clock weight from 420 to 450 to avoid the overly thin look after tightening spacing.
- Built and installed debug APK on the connected OnePlus 12; screenshots: `lockscreen_screencap_0157_clock_weight.png`, `aod_screencap_0157_clock_weight.png`.

### 0.1.54

- Added lockscreen clock attach probes for `CustomOplusKeyguardStyleClock` and `ClockViewRoot`.
- Added a filtered global `View#onAttachedToWindow` probe for those lockscreen clock class names.
- Device verification did not produce the expected probe logs; current reliable evidence is still the AOD parent tree where `ClockViewRoot` appears under `CustomOplusKeyguardStyleClock`.
- Next lockscreen approach should actively scan the `NotificationShadeWindowView` / window-root parent tree instead of relying on direct attach hooks.

### 0.1.52

- Added an early AOD host-ready path: when the AOD host attaches while the device is already non-interactive, the module marks AOD active before `AodRecord#onDreamingStarted`.
- Applies stock AOD hiding and status-view adjustment earlier to reduce the screen-off transition overlap window.
- Verified stable AOD screenshot has no stock AOD clock overlap.

### 0.1.51

- Reverted horizontal glyph scaling for clock digits.
- Kept tighter digit spacing through `letterSpacing` only, so the glyph shapes are not squeezed.
- Verified notification fallback icon is monochrome on AOD.

### 0.1.50

- Tightened large and compact clock digit spacing.
- Rejected colored app-icon fallback for AOD notification icons when the notification small icon looks mask-like.

### 0.1.49

- Tuned compact clock size and vertical rhythm toward Pixel small-clock mode.
- Added compact-clock proportional number feature setting.

### Earlier

- Added bundled Google Sans Flex 200/500/regular font assets.
- Added Pixel-style AOD media line using the current media notification small icon.
- Added bottom battery percentage and a custom vector charging bolt.
- Added filtering against lockscreen notification visibility settings.
- Hid stock OxygenOS AOD clock, stock AOD battery, and stock AOD notification views while preserving the stock media card.

## Known Bugs / Gaps

- Screen-off transition overlap has early-host and draw-level mitigations, but still needs video or repeated visual verification before closing completely.
- Lockscreen clock replacement is implemented, but normal lockscreen large/compact states still need fresh screenshots after the 0.1.65 visibility tightening.
- Direct lockscreen clock attach probes are less reliable than the global `NotificationShadeWindowView` probe; keep both until behavior stabilizes.
- Lockscreen-to-AOD font-weight transition is represented by separate weights, but no explicit animated interpolation exists yet.
- Compact clock currently uses one-line time with date and notification icons below; final Pixel accuracy still needs side-by-side comparison after more references.
- AOD At a Glance remains intentionally disabled.
- Notification icons depend on each app notification's small icon quality; some apps may provide unusable or blocky masks.
- The unlocked notification shade has its own system header clock/date. It should not be treated as AOD or lockscreen overlay unless it visually duplicates module content beyond the normal shade header.

## Reference Direction

- Pixel behavior to mimic: large two-line clock when there are no visible notifications; compact top-left clock when notifications are present.
- AOSP/SystemUI behavior to inspect further: `KeyguardClockSwitch`, clock-size switching, and doze/lockscreen clock state transitions.
- OxygenOS-specific implementation must be verified on-device because class names and view trees are ROM-build specific.
