# ADR 0036: Prefer native SystemUI media semantics over raw session enumeration

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD currently enumerates `MediaSessionManager.getActiveSessions()` and applies its own active/paused/idle retention rules. Android 17 SystemUI already has a media pipeline that filters by the selected user and available profiles, maintains active/inactive/resumption semantics, and selects media state for lockscreen/SystemUI consumers. Reimplementing those rules creates privacy and lifecycle divergence.

## Decision

Add a read-only **native SystemUI media semantics adapter**.

1. Prefer a stable OPlus/SystemUI media-pipeline seam for current-media eligibility, current-user/profile filtering, and active media selection.
2. Keep Pixel/COUI ownership of the AOD media row presentation only.
3. Use direct `MediaSessionManager` enumeration only as a fallback when the native semantic seam is unavailable.
4. Apply ADR 0014 user/profile privacy scope before rendering any fallback media.
5. Deduplicate equivalent native Ambient Indication/Now Playing content under ADR 0029 rather than rendering the same item twice.

## Consequences

- Media selection follows the same user/profile semantics as SystemUI.
- Module-specific paused/idle timeout logic stops being the primary source of truth.
- The visual Pixel media row remains available without taking over the media lifecycle.

## Rejected alternatives

- Keep raw session enumeration as primary: duplicates SystemUI policy and can diverge across users/profiles.
- Remove the Pixel media row and always expose native media UI: gives up the intended Pixel/COUI presentation unnecessarily.

## M9 S23 implementation note — 2026-08-26

The exact current OOS SystemUI exposes a stronger seam than raw Android media sessions:

1. `LegacyMediaDataManagerImpl` feeds `LegacyMediaDataFilterImpl`.
2. `LegacyMediaDataFilterImpl` accepts a media entry only when its `MediaData.userId` is the current Android user or `NotificationLockscreenUserManager.isCurrentProfile(userId)` is true.
3. The already-filtered entry is then passed to `OplusMediaDataFilterEx`, whose current-device implementation is `com.oplus.systemui.media.OplusMediaDataFilterExImpl`.
4. That OPlus extension exposes its chosen current semantic item through `getCurData()` / `loadCurrentMediaData(listener)`, and this ROM extends `MediaDataManager.Listener` with `onCurrentActiveMediaChanged(String, MediaData)`.

S23 therefore installs a read-only `NativeSystemUiMediaAdapter` on the existing manager/listener boundary. It snapshots only the native current `MediaData` fields required by the existing Pixel row (active state, selected user id, package, title, artist and app icon). It does not invoke playback commands or mutate the SystemUI pipeline. `MediaSessionManager` remains a compatibility fallback only until a real OPlus filter implementation has been observed and bootstrapped.

Physical runtime evidence confirms the native path is active: a PixelPlay session was reported as `present=true / active=true / userId=0`; pausing playback kept the row because OPlus still considered it current media. Removing the raw session can likewise leave OPlus current-media/resumption state intact. That retention is deliberately inherited rather than replaced with a module timer or PLAYING-only rule.
