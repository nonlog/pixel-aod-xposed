# ADR 0010: Read-only wallpaper ambient adapter

Date: 2026-08-22
Status: Accepted

## Context

AOSP Doze coordinates wallpaper ambient state as part of its lifecycle. Pixel AOD already owns detailed LS-to-AOD/AOD-to-LS clock and content presentation, but vendor wallpaper fade timing may not always line up with those transitions.

ADR 0001 keeps wallpaper ambient ownership with OPlus/SystemUI.

## Decision

Implement a **read-only wallpaper ambient adapter**.

1. Observe stable OPlus/SystemUI wallpaper/ambient transition state when available.
2. Use that state only to coordinate Pixel AOD clock/content fade and morph timing.
3. Do not call WallpaperManager or another system API to independently set wallpaper ambient mode.
4. Do not override or fight vendor wallpaper fade duration/state.
5. If a reliable signal is unavailable, retain current presentation timing rather than inventing wallpaper lifecycle state.

## Consequences

- LS/AOD presentation can align more closely with the vendor wallpaper transition without becoming its owner.
- Wallpaper integration remains optional and fail-safe per ROM.
- Diagnostics should distinguish vendor wallpaper transition state from Pixel AOD's own animation state.

## Rejected alternatives

- Ignore wallpaper timing forever: safe but leaves avoidable transition mismatch where a stable vendor signal exists.
- Actively control wallpaper ambient mode: duplicates system lifecycle ownership and risks transition conflicts.
