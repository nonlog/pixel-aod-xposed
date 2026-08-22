# ADR 0022: Treat DND ambient suppression as a hard AOD gate

Date: 2026-08-22
Status: Accepted

## Context

Android notification ranking exposes suppressed visual effects separately from lockscreen visibility. In particular, Ambient Display suppression expresses a system policy decision that a notification must not appear on ambient presentation. Pixel AOD already captures `Ranking.getSuppressedVisualEffects()` but does not currently make the ambient-suppression bit authoritative for every notification-derived AOD surface.

## Decision

Apply a **DND ambient suppression hard gate**.

1. Treat the system-equivalent `SUPPRESSED_EFFECT_AMBIENT` decision as authoritative for notification-derived AOD presentation.
2. Exclude ambient-suppressed notifications from Pixel AOD notification icons and notification pulse candidates.
3. Exclude notification-derived Live Update/contextual presentation when its source notification is ambient-suppressed.
4. Keep Zen/DND policy evaluation inside Android/SystemUI; Pixel AOD consumes the already-computed ranking result rather than reimplementing policy.
5. Keep lockscreen visibility and Ambient Display suppression as distinct inputs; passing one does not override failure of the other.

## Consequences

- Pixel AOD follows the user's system DND/Ambient Display policy consistently.
- Existing ranking snapshots gain a concrete presentation responsibility instead of being diagnostic-only for this field.
- Tests must cover icon, pulse, and Live Update behavior under ambient suppression.

## Rejected alternatives

- Block only notification pulses: still exposes notifications the system explicitly suppressed from Ambient Display.
- Ignore ambient suppression and follow only lockscreen visibility: conflates two separate system policy decisions.
