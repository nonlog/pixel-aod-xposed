# ADR 0053: Respect vendor screen-off animation eligibility

Date: 2026-08-22
Status: Accepted

## Context

ADR 0016 accepts native Doze transition progress as the source for Pixel presentation morphing, but Android/SystemUI does not animate every screen-off transition. Display blanking and other device/scene conditions can require snapping to a safe endpoint. Progress alone is therefore insufficient to decide that a visible Pixel animation should run.

## Decision

Add a read-only **vendor screen-off animation eligibility adapter**.

1. Consume a stable OPlus/SystemUI signal indicating whether the current screen-off/AOD transition may be animated.
2. Use ADR 0016 Doze progress for Pixel morphing only while native animation eligibility is true.
3. When display blanking or an equivalent native condition requires a snap, move Pixel presentation directly to the corresponding safe endpoint.
4. Do not infer animation eligibility from timing, panel state lag, or the existence of a progress callback alone.
5. Keep panel blanking, power mode, wallpaper transition, and wake lock behavior entirely vendor-owned.

## Consequences

- Pixel LS-to-AOD morphs no longer run on transitions where SystemUI intentionally avoids screen-off animation.
- Q16 remains the progress source without becoming an animation-permission signal.
- Device-specific display-blanking behavior can be validated without module lifecycle ownership.

## Rejected alternatives

- Animate every transition that exposes progress: can conflict with display blanking and OEM safety constraints.
- Remove LS/AOD morphing entirely: discards valid presentation parity on devices and scenes that support it.
