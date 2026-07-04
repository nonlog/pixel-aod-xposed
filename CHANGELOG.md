# Changelog

## [Unreleased]
### Deferred
- Silent notifications can still briefly flash during the OOS lockscreen-to-AOD transition when the affected silent channel also has lockscreen display permission enabled. This is not fixed yet; current workaround is to disable lockscreen display permission for those silent notification channels. The unfinished experimental row/card suppression code is parked in git stash `wip: defer silent notification flash experiment`.

## [0.1.135] - 2026-07-04
### Internal
- Centralize AOD lifecycle, module policy, stock AOD suppression, and native hide callback choices into a shared policy decision object with explicit logs for each output.
- Route AOD overlay drawing, Doze keepalive, OPlus energy-saving hide guard, display-state rewrite, host ready, delayed reapply, and restore guard checks through the shared decision layer without intentionally changing behavior.

## [0.1.134] - 2026-07-04
### Bug Fixes
- Keep suppressing stock OOS AOD views when the module AOD schedule blocks Pixel AOD display, avoiding a brief stock AOD flash on screen-off.
- Prevent `AodRecord#onDreamingStarted` from marking Pixel AOD active/recent-visible outside the module schedule or power policy, while still allowing native OOS hide callbacks to dismiss fingerprint affordances.

## [0.1.133] - 2026-07-04
### Bug Fixes
- Stop module AOD host/reapply paths from hiding stock AOD views or reasserting Pixel AOD while the module schedule or power policy blocks display.
- Allow OOS energy-saving AOD hide callbacks to run outside the module AOD policy window so native fingerprint / short-wake affordances can time out normally.

## [0.1.132] - 2026-07-04
### Bug Fixes
- Stop the module from keeping OOS Doze screen state alive or rewriting OFF requests while the module AOD schedule or power policy says the Pixel AOD overlay should not display.
- Fix the schedule-outside case where module AOD stayed hidden correctly but OOS short wake / fingerprint affordance could fail to disappear because OFF requests were still rewritten to DOZE.

## [0.1.131] - 2026-07-04
### Diagnostics
- Add native-style AOD trigger diagnostics for OPlus wake-up controller methods related to pickup, tap, proximity, pocket, and sensors without changing trigger behavior.
- Record the latest native trigger type, source, detail, and age in AOD lifecycle state snapshots so real OOS trigger events can be mapped before adding module-owned sensor logic.

## [0.1.130] - 2026-07-04
### Power
- Align module AOD visibility with native-style power policy by hiding the overlay while system battery saver is active.
- Hide module AOD when the device is on low battery and not charging, with explicit debug-log reasons for power-save and low-battery decisions.

## [0.1.129] - 2026-07-04
### UI
- Remove the Clock Scale slider from module settings and keep AOD clock text at the Pixel-style default 1.0 scale.

## [0.1.128] - 2026-07-04
### Visual
- Replace the custom burn-in drift periods with AOSP-style 83/521 minute Pixel-like offset periods while keeping the current low-power native-tick refresh path.
- Keep the 8-second AOD entry settle window so the clock group does not visibly jump during the OOS lockscreen-to-AOD transition.

## [0.1.127] - 2026-07-04
### Maintenance
- Route the core Pixel AOD draw, Doze keepalive, and lockscreen-to-AOD bridge decisions through the OOS lifecycle adapter while preserving the existing boolean behavior.

## [0.1.126] - 2026-07-04
### Maintenance
- Route existing AOD trace guard checks for known-host refresh, delayed stock AOD suppression, and delayed stock view restore through the OOS lifecycle adapter without changing visual behavior.

## [0.1.125] - 2026-07-04
### Diagnostics
- Add an observability-only OOS AOD lifecycle adapter that classifies hook sources such as dreaming start/stop, screen on/off, display-state requests, host readiness, and native ticks against the current `AodLifecycleState.phase()`.
- Record the 19:51 rapid lockscreen / AOD switching evidence from LSPosed persistent module logs in the lifecycle mapping document.
- Add a local `tools/extract_pixelaod_logs.ps1` helper that extracts Pixel AOD logs from both current `adb logcat` and `/data/adb/lspd/log/modules_*.log` for a requested time window.

## [0.1.124] - 2026-07-04
### Diagnostics
- Add lightweight AOD lifecycle phase-change logs that record source, previous/current phase, trace id, display state, active flag, and timing snapshot only when `AodLifecycleState.phase()` changes.
- Document the phase-change log format and the next required live captures for AOD entry and exit mapping.

## [0.1.123] - 2026-07-04
### Bug Fixes
- Add a centralized AOD lifecycle snapshot used by AOD visibility, Doze screen keepalive, and lockscreen-to-AOD bridge decisions, keeping the current behavior while making transition state easier to reason about from logs.
- Guard the delayed stock AOD suppression reapply task with the originating AOD trace so an old transition cannot hide stock views during a newer AOD session.

## [0.1.122] - 2026-07-02
### Bug Fixes
- Drive module AOD time updates from SystemUI's native OOS AOD refresh callbacks, with a short entry refresh and time-change broadcast fallback while the module AOD view is active.
- Guard OOS energy-saving AOD hide callbacks during the AOD entry window so the custom AOD overlay is reasserted instead of disappearing or causing SystemUI-like restart behavior.

## [0.1.121] - 2026-07-02
### Bug Fixes
- Drive module AOD clock refresh from SystemUI's system `ACTION_TIME_TICK` broadcast while AOD is running, so the clock keeps advancing even when the OOS `DozeUi` native tick hook is not available.
- Keep the custom `Handler.postDelayed` minute ticker removed; this update uses the platform broadcast already delivered to SystemUI instead of adding a module-owned self-loop.
- Remove the remaining global debug-log rate limit so `debug_logging=true` no longer drops transition or time-refresh evidence during dense AOD traces.

## [0.1.120] - 2026-07-02
### Bug Fixes
- Keep supported system notifications such as the USB and tether/network-status entries on module AOD even when OOS marks them as silent or `LOW`, by letting the module's system-notification allow-path run before the lockscreen-policy silent filter.
- Restore lockscreen-policy hiding for silent third-party notifications only on the real lockscreen path, so notifications like Link to Windows no longer stay visible on OOS lockscreen while the unlocked notification shade still keeps its normal silent section.

## [0.1.119] - 2026-07-01
### Bug Fixes
- Bind the AOSP-style silent-notification filtering to the `Lockscreen Policy` setting instead of forcing a global SystemUI hide. With the toggle enabled, silent or low-importance notifications are filtered only from the lockscreen/AOD path; with it disabled, the module no longer alters OOS silent-notification visibility.
- Stop forcing `shouldHideNotification` / `shouldFilterOut` to hide silent notifications globally. This restores FlyClash-style silent notification groups in the unlocked notification center while keeping the lockscreen-policy override that preserves lockscreen notifications across unlock/relock cycles.

## [0.1.118] - 2026-07-01
### Bug Fixes
- Make silent notifications follow AOSP semantics on both OOS lockscreen and module AOD: if a notification is marked `FLAG_SILENT` or its ranking importance is `LOW` or below, the module now force-hides it from the lockscreen visibility path and filters it from AOD as well.
- Remove the temporary third-party aggregate-summary special case and replace it with the general silent-notification rule, so grouped summaries like FlyClash's auto-group notification no longer leak onto AOD when the underlying notification channel is silent.

## [0.1.117] - 2026-07-01
### Bug Fixes
- Make the `Debug Logging` setting push changes through the settings provider and notify the hooked SystemUI process immediately, so toggling the switch refreshes module settings without waiting for the cache TTL or a later opportunistic reload.
- Unify AOD notification visibility with the lockscreen visibility decision path: AOD now consumes the same keyguard/provider filter results that decide whether a notification can appear on the lockscreen, instead of applying a separate low-importance/silent heuristic. This fixes FlyClash-style cases where a notification could leak onto AOD while still being hidden on the lockscreen.

## [0.1.116] - 2026-06-28
### Bug Fixes
- Hide the charging indicator icon on the AOD battery status once the battery is fully charged, even if the charger remains plugged in.
## [0.1.115] - 2026-06-28
### Bug Fixes
- Fix JSON parsing crash in BreezyWeatherRelayReceiver that skipped parsing if the root was a JSONObject.
- Fix AOD being stuck in night mode after sunset by converting timestamps to time-of-day before comparison, preventing expiration.
- Fix time unit mismatch in Breezy Weather intents by properly scaling second-based timestamps to milliseconds.
- Support system dark mode on module startup splash screen.

### UI
- Replace the buggy, manual Canvas-based `ClockDialPicker` with Google's official Material 3 `TimePicker`, fixing massive GC thrashing and frame drops.
## [0.1.114] - 2026-06-27
### UI
- Replace the stock Android `TimePickerDialog` for "Start Time" / "End Time" with a custom clock-dial (表盘) picker. The user drags on the circular face to pick hours (outer ring, 1–12) or minutes (inner ring, 0–59), with Hour/Minute and AM/PM toggle chips above the dial.

## [0.1.113] - 2026-06-27
### Bug Fixes
- Use Breezy Weather's actual sunrise/sunset times for day/night icon selection instead of a naive `hour < 6 || hour >= 18` check. The previous behavior incorrectly showed the night icon at 18:01 in summer (sunset ~19:30). The relay now extracts `sunRise`/`sunSet` (camelCase) and `sunrise`/`sunset` (lowercase) from the Breezy Weather JSON, stores them in SharedPreferences, and passes them through the relay broadcast as `sunrise_millis`/`sunset_millis` extras. Falls back to the hour check when Breezy Weather hasn't published sun-times.

## [0.1.112] - 2026-06-27
### Bug Fixes
- Make the status bar and navigation bar icons in the Settings screen adapt to the system theme: enable edge-to-edge layout and flip `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` based on the current `UI_MODE_NIGHT_MASK`. Previously the icons stayed light on top of `Theme.Material.Light`, making them invisible on a white surface.

## [0.1.111] - 2026-06-22
### UI
- Remove the Pocket Mode and Notification Icons toggles from the settings screen and treat both behaviors as built-in defaults instead of optional switches.

### Bug Fixes
- Force both Pocket Mode and monochrome notification icons to remain enabled at runtime, and automatically normalize old saved `false` values back to `true` so legacy preferences no longer disable those features.

## [0.1.110] - 2026-06-22
### Bug Fixes
- Force custom AOD views to refresh their frame after time, notification, weather, and media updates so stale minute text or delayed notification icons are less likely to remain until the next wake cycle.
- Keep the display in live doze instead of suspended doze while the custom AOD needs active frames, reducing missed redraws on OxygenOS AOD.
- Expire paused or idle media sessions after 10 minutes and clear them immediately when the media session is destroyed or playback stops, matching the expected AOD media timeout behavior.

## [0.1.109] - 2026-06-20
### UI
- Rework the launcher icon assets into a stable adaptive icon set: keep the rebuilt full icon as the adaptive foreground, switch the adaptive background to transparent to avoid a doubled card effect on OOS, and temporarily remove the broken monochrome layer export so the launcher icon renders correctly instead of collapsing into a washed-out bar.

### Bug Fixes
- Keep stock AOD suppression-miss logging debug-only and deduplicated, refresh known AOD hosts without reinjecting the whole tree, and narrow the media classifier so ordinary ongoing notifications stay on the normal icon path.
- Use the native AOSP tether Wi-Fi drawable for the system network fallback instead of the custom multi-name lookup.

## [0.1.108] - 2026-06-18
### Bug Fixes
- Guard the delayed stock AOD/keyguard restore with the originating AOD trace and refresh known host visibility after AOD activation, preventing an old transition from resurrecting stock AOD views into a new cycle.

## [0.1.107] - 2026-06-18
### Diagnostics
- Remove the 60-per-minute debug log throttle and keep full AOD / lockscreen decision traces, including trace ids, state snapshots, transition reasons, and notification rebuild decisions.

## [0.1.106] - 2026-06-18
### Bug Fixes
- Replace the self-drawn USB and tether/hotspot notification icons with native AOSP system drawables loaded from framework resources.
- Remove the self-drawn module update glyph and keep module-package notifications logged when they are filtered out of lockscreen/AOD visibility.

## [0.1.105] - 2026-06-18
### Bug Fixes
- Third-party AOD notifications without a monochrome icon now fall back to the application's launcher icon instead of tinting the raw small icon into a white block.

## [0.1.104] - 2026-06-18
### Bug Fixes
- Fallback third-party push notifications to the original tinted small icon on AOD when the app does not provide a monochrome adaptive icon, so blocky/tiny icons such as Taobao no longer disappear from AOD.

## [0.1.103] - 2026-06-18
### Diagnostics
- Added low-noise AOD suppression trace logs for entry-state snapshots, stock hide passes, and transition restore decisions to help diagnose intermittent system AOD overlap.

## [0.1.101] - 2026-06-17
### Bug Fixes
- **Fix AOD and Lockscreen Clock Overlap**: Addressed an issue where the stock Lockscreen/AOD clock could overlap with the custom module clock (e.g. at 07:35 or outside the AOD schedule). Added the correct Lockscreen container candidates to the stock clock draw suppression hook and removed the early `isDeviceInteractive` bailout that was incorrectly skipping the draw suppression hook on the Lockscreen.

## [0.1.100] - 2026-06-17
### Bug Fixes
- **Fix AOD media info disappearing on pause**: The media row used to hide the moment playback left the PLAYING state and never reset its dedupe cache, so resuming the same track was skipped as "unchanged" and the row stayed hidden until the player was swiped away and reopened. Media visibility is now driven by whether the session still has a displayable track (any state except STOPPED/ERROR), and the cache is reset when the row is cleared. Verified on-device across play → pause → resume (PixelPlay reports both PAUSED and NONE on pause; both are now kept).
### UI
- **Settings app redesign (Material 3 / Expressive)**: Grouped cards (Appearance / Clock / Behavior / Advanced), larger rounded corners, dynamic color.
- **Follow system light/dark theme** instead of a hardcoded light theme.
- **Language switch (Follow system / 中文 / English)**, defaulting to the system language, applied via `attachBaseContext`.
- Moved the "Restart SystemUI" action from a bottom button to a small restart icon in the top app bar.
### Build
- Bump Android Gradle Plugin to 8.6.0 and Compose BOM to 2026.05.01 (Material3 1.4.0), required by the Expressive components.

## [0.1.99f] - 2026-06-16
### Stability
- Disable global `View#setVisibility` / `View#setAlpha` stock-clock hooks by default to avoid SystemUI-wide hot-path interception.
- Move custom AOD visibility enforcement out of `dispatchDraw()` and cache schedule checks to reduce per-frame work.
- Guard proximity listener registration so pocket mode does not repeatedly register the same listener.
- Keep test notifications, but remove the experimental broadcast-based settings mutation path.
- Sync modern Xposed `module.prop` with the Gradle app version.

## [0.1.99e] - 2026-06-16
### Features
- **AOD Display Schedule Mode**: Added a custom scheduling option (Start Time ~ End Time) for the custom AOD. When enabled, the AOD clock and widgets will only render during the user-configured time range (supporting ranges spanning midnight). Outside this schedule, the custom AOD layout is hidden, and the stock clock remains suppressed to keep the screen completely black.
### Bug Fixes
- **Settings Synchronization Fix**: Expose missing keys (`pocket_mode`, `force_english_date`, `disable_burn_in_offset`, and AOD schedule keys) in `PixelAodSettingsProvider` to ensure settings successfully propagate from the Settings application to SystemUI.

## [0.1.99d] - 2026-06-16
### Bug Fixes
- **Fix Overlapping System AOD Clock:** Enhanced the stock clock draw suppression hook in `PixelAodHook.java`. Added standard and Oplus keyguard clock container classes (such as `KeyguardStatusView`, `KeyguardClockSwitch`, and `DateMessageView`) to the suppression class list and draw candidate checks. This ensures that the stock clock is completely blocked from rendering when the screen is in AOD mode, resolving the overlapping/double-clock issue.

## [0.1.99c] - 2026-06-16

## [0.1.99b] - 2026-06-16

## [0.1.99a] - 2026-06-15
### Bug Fixes
- **Fix Notification Shade Header Clock (Robust Exclusion)**: Implemented recursive ancestor tracking to protect all descendant views of Quick Settings, Status Bar, Bouncer, and Emergency layers from being hidden by the stock view suppression mechanism. This correctly preserves the system clock inside the notification shade header (e.g. `QSClock`) under all layouts.

## [0.1.99] - 2026-06-15
### Bug Fixes
- **Fix Notification Shade Header Clock**: Exclude status bar, quick settings, shade header, bouncer, emergency, and carrier views from the stock AOD view suppression filters. This ensures the system clock in the top-left corner of the notification shade/quick settings header remains visible and functional.

## [0.1.98] - 2026-06-15
### Bug Fixes
- **Fix AOD Media Info display**: Ensure the AOD media row is hidden immediately when the active media playback state changes to paused or stopped, or when the media session is swiped away.
- **Fix Lockscreen Clock Overlap**: Add active media session presence to the lockscreen clock's compact layout check. This ensures the clock switches to compact layout from the very first frame when waking up with a paused media card, completely preventing the visual overlap.

## [0.1.95] - 2026-06-13
### Bug Fixes
- **Fix Lockscreen Clock Instability/Flickering**: The lockscreen clock size evaluation (`PixelLockscreenClockView`) was tightly coupled to the Oplus layout tree and animations. When Fluid Cloud media text animated (e.g., marquee or equalizer), the layout state fluctuated at 60fps, causing the clock to infinitely toggle between large and compact mode. A 1000ms debounce has been implemented for `setVisibleLockscreenNotificationCards(false)` to completely stabilize the lockscreen layout against these transient UI animation states.

## [0.1.94] - 2026-06-13
### Bug Fixes
- **Fix AOD Clock Mode (Screenshot 17:08)**: The AOD clock view (`PixelAodClockView.java`) now properly shrinks to compact mode when media is playing via `MediaSessionManager`, even if there are no standard notification icons.
- **Fix Lockscreen Clock Flickering/Twitching**: Removed the physical size (`w > 0 && h > 10dp`) check from `isNonEmptySeedling` which caused an infinite layout loop. Now only relies on meaningful text content to detect Fluid Cloud.

## [0.1.93] - 2026-06-13
### Performance
- **Root cause fix for unlock animation frame drops.** `dispatchDraw()` (called ~60fps) was doing a full view tree traversal (`hasLiveLockscreenNotificationCards` → `PixelAodHook.hasVisibleLockscreenNotificationCardsIn`) and parsing all active notifications (`currentNotifications` with string operations) on every single frame. Now `dispatchDraw` only reads lightweight cached boolean flags that are updated event-driven by `applyLockscreenClockReplacement` and `setActiveNotifications`. This eliminates hundreds of tree traversals per second during unlock.

## [0.1.92] - 2026-06-13
### Performance
- **Root cause fix for unlock animation frame drops.** `dispatchDraw()` (called ~60fps) was doing a full view tree traversal (`hasLiveLockscreenNotificationCards` → `PixelAodHook.hasVisibleLockscreenNotificationCardsIn`) and parsing all active notifications (`currentNotifications` with string operations) on every single frame. Now `dispatchDraw` only reads lightweight cached boolean flags that are updated event-driven by `applyLockscreenClockReplacement` and `setActiveNotifications`. This eliminates hundreds of tree traversals per second during unlock.

## [0.1.91] - 2026-06-13
### Performance
- Fixed massive frame drops / CPU overload during unlock and UI layout animations. The recursive content verification (`traverse`) is now strictly limited to media and Seedling containers. Generic system containers are evaluated instantly, eliminating O(N²) layout thrashing.

## [0.1.90] - 2026-06-13
### Fixed
- Fixed a bug where Oplus (ColorOS/OxygenOS) "Fluid Cloud" (Seedling/MediaControlTip) empty containers falsely triggered the small clock layout even when no media was playing. The detector now recursively traverses the container to verify if there is any visible inner content before forcing the small clock.
- Fixed wake-up animation (AOD to Lockscreen) stuttering. The module now avoids hardcoded visibility thresholds (`getAlpha() < 0.1f` and `getHeight() <= 24dp`) on generic notification rows, which previously caused the clock size to abruptly jump mid-animation when the notifications faded in or expanded.

## [0.1.89] - Previous Versions
- Initial implementation of Pixel AOD and Lockscreen custom clocks.
- Add dynamic injection index support to place clocks below `NotificationPanelView` to avoid z-order overlap with notifications.
