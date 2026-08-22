# ADR 0041: Make the AOD Doze palette selectable

Date: 2026-08-22
Status: Accepted

## Context

Android 17 default clocks use a neutral white Doze color while the current Pixel/COUI host keeps an ambient Monet-derived accent. The project wants Android/AOSP parity available without permanently discarding the existing colored COUI presentation.

## Decision

Expose a **selectable AOD Doze palette**.

1. Provide an AOSP-like neutral white Doze palette and the existing colored Monet/COUI ambient palette as presentation choices.
2. Apply the selected palette only to module-owned AOD clock, information text, notification/contextual glyph treatment, and related module presentation.
3. Keep lockscreen color/theme behavior independent from the AOD palette choice.
4. Preserve source-owned multicolor artwork and vendor-owned biometric/native foreground surfaces without recoloring them through this setting.
5. Palette selection must not affect panel state, brightness, HBM, lifecycle, suppression, or any other power policy.
6. Exact UX default/migration behavior is a reversible settings detail and is not part of this architecture boundary.

## Consequences

- Users can choose strict AOSP-like neutral Doze visuals or retain the existing colored COUI identity.
- Q42 native lockscreen theme input and AOD color policy remain cleanly separated.
- Visual-budget tests must cover both palettes.

## Rejected alternatives

- Force neutral white for every user: removes the existing colored ambient product option.
- Force Monet color permanently: leaves no strict AOSP-like Doze presentation mode.
