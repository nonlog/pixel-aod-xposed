# Changelog

## [0.1.99b] - 2026-06-16
### Bug Fixes
- **Fix Lockscreen Clock stuck in compact mode**: Removed background media session check (`hasMediaSession`) from layout updates so that the clock correctly returns to the large layout when notifications are dismissed.
- **Eliminate Notification Expand/Collapse Transition Delay**: Reduced layout verification interval to 80ms and replaced slow `isShown()` calls with instant visibility state checks during animations.
- **Fix Pocket Mode / Proximity Sensor State Bypass**: Inspected state transition reasons to bypass AOD state overrides when pocket mode or proximity sensors request screen off.
- **Support AOD Schedule Settings**: Added system settings schedule verification to respect OnePlus/OOS system AOD scheduling and modes (Always On, Scheduled, Power Saving).

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
