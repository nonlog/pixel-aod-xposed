# ADR 0055: Keep Pixel AOD touch-transparent and vendor-routed during Doze

Date: 2026-08-22
Status: Accepted

## Context

ADR 0008 already says Pixel AOD is presentation-only and must not directly own AOD actions. Android 17 SystemUI additionally has explicit Doze touch-routing policy for pulsing, docked, UDFPS, and other device-entry states. A full-screen replacement overlay can still become an accidental input owner even if its visible controls are not intentionally interactive.

## Decision

Adopt **native Doze input pass-through**.

1. Pixel AOD clock, information, transition, and decorative overlay layers remain touch-transparent in ambient scenes.
2. Do not register module tap, double-tap, long-press, swipe, or wake-gesture handlers on the AOD replacement surface.
3. Preserve OPlus/SystemUI routing for tap-to-wake, notification pulse interaction, UDFPS/device-entry input, dock behavior, and any vendor gesture arbitration.
4. Module accessibility semantics from ADR 0049 do not imply click/touch ownership unless native SystemUI exposes an equivalent safe action outside Doze.
5. Treat any module overlay intercepting vendor Doze input as a correctness defect.

## Consequences

- Full-screen Pixel presentation cannot silently block native wake or biometric input.
- The module does not need to clone Android `DozeTouchInteractor` policy.
- Input validation can assert pass-through across AOD, pulse, UDFPS, and dock scenes.

## Rejected alternatives

- Let Pixel handle tap-to-wake: duplicates vendor wake-trigger ownership from ADR 0007.
- Reimplement SystemUI Doze touch interception: creates a second device-entry input policy and conflicts with presentation-only scope.
