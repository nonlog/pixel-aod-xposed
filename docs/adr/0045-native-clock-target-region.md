# ADR 0045: Use the native clock target/safe region as the outer geometry boundary

Date: 2026-08-22
Status: Accepted

## Context

Android 17 clock hosts provide clocks with a target region rather than assuming that the entire display is a safe layout surface. Pixel AOD currently relies heavily on fixed DP/device-profile geometry plus its own burn-in movement. That can conflict with cutouts, SystemUI-reserved space, large-font layout, RTL, and pulse/promoted foreground collision.

## Decision

Add a read-only **native clock target-region adapter**.

1. Prefer a reliable OPlus/SystemUI clock target region or equivalent safe-region geometry as the outer placement boundary.
2. Keep Pixel/COUI internal proportions, anchors, compact/large layout, content stacking, and burn-in policy module-owned inside that boundary.
3. Recompute layout when the native target region changes due to configuration, scene, orientation/layout direction, or SystemUI foreground conditions.
4. Combine the outer boundary with ADR 0038 collision avoidance, ADR 0040 configuration-responsive typography, and ADR 0030 RTL support.
5. Clamp burn-in movement and transient geometry so no module-owned content is moved outside the validated safe region.
6. Fall back to validated device-profile geometry only when no stable native target region exists.

## Consequences

- Pixel geometry remains visually distinct while respecting SystemUI-reserved space.
- Fixed full-screen assumptions can be reduced without surrendering layout ownership to the native clock container.
- Geometry tests gain an explicit outer constraint for cutouts, font scale, RTL, and pulse foregrounds.

## Rejected alternatives

- Keep fixed full-screen DP bounds as the only geometry source: fragile across SystemUI/configuration changes.
- Fully delegate layout to the native clock container: unnecessarily gives up Pixel/COUI internal geometry ownership.
