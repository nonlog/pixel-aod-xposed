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
