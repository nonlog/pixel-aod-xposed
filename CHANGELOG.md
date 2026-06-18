# Changelog

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
