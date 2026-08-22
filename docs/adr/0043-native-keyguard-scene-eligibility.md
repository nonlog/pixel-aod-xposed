# ADR 0043: Use native Keyguard scene state as the presentation eligibility authority

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD currently combines screen/interactivity state, `KeyguardManager`, host visibility, bouncer checks, and view-tree heuristics to infer whether its lockscreen/AOD presentation belongs on screen. Android 17 SystemUI has explicit Keyguard scene/state transitions for Lockscreen, AOD, Dozing, Occluded, Bouncer, Gone and related states. A reliable native scene signal is a stronger authority than visual-tree inference.

## Decision

Add a read-only **native Keyguard scene eligibility adapter**.

1. Prefer stable OPlus/SystemUI Keyguard scene/state for Lockscreen, AOD, Dozing, Occluded, Bouncer, Gone and equivalent presentation states.
2. Permit Pixel lockscreen/AOD presentation only when the native scene and the already accepted vendor Doze policy allow it.
3. Treat existing `KeyguardManager`, screen-state, host-visibility, bouncer, and view-tree heuristics as fallback/diagnostic inputs only.
4. A fallback heuristic must never override a reliable native scene decision.
5. Scene changes immediately invalidate stale presentation and cached geometry/content eligibility.

## Consequences

- Large classes of fragile view-name/state inference can be retired from primary policy during M9.
- Occlusion, bouncer, unlock, and AOD transitions gain a single authoritative presentation boundary.
- Existing fallback paths remain useful on unsupported builds without becoming equal authorities.

## Rejected alternatives

- Keep view-tree heuristics as primary: couples behavior to OEM layout implementation details.
- Reduce eligibility to screen-off/interactivity alone: cannot distinguish important Keyguard scenes safely.
