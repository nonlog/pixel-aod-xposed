# ADR 0023: Add a vendor notification Doze visibility adapter

Date: 2026-08-22
Status: Accepted

## Context

Android SystemUI coordinates notification visibility during screen-off and Doze separately from the generic clock transition. Notification-specific state prevents rows from flashing during screen-off animation and distinguishes fully-dozing, pulsing, and hidden presentation. Pixel AOD has strong clock/content stale-frame gates but no dedicated equivalent notification-Doze visibility input.

## Decision

Add a capability-gated **vendor notification Doze visibility adapter**.

1. Prefer a stable OPlus/SystemUI signal for notification fully-dozing, pulsing, or hidden state when one can be identified and validated.
2. Use that state to gate notification, media, and notification-derived contextual rows independently from the primary clock transition.
3. Keep Q16 generic Doze progress as a separate presentation input; it must not substitute for notification-specific visibility semantics.
4. Do not synthesize notification visibility with independent timers.
5. Fall back to the existing scene/lifecycle gates when no trustworthy vendor signal exists.

## Consequences

- LS-to-AOD and AOD-to-LS transitions can avoid transient notification flashes.
- Notification visibility remains aligned with the vendor's own transition lifecycle.
- Tests need dedicated notification-row transition cases in addition to clock geometry cases.

## Rejected alternatives

- Fade all rows directly from generic Doze progress: misses notification-specific fully-dozing behavior.
- Keep only existing stale-frame gates: preserves a known parity gap during transition-specific notification visibility.
