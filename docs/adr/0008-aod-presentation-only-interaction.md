# ADR 0008: AOD presentation-only interaction policy

Date: 2026-08-22
Status: Accepted

## Context

AOSP/Pixel AOD shares Keyguard content and can integrate falsing/touch infrastructure. Pixel AOD's accepted architecture avoids duplicating the vendor Doze lifecycle, and ADR 0003 already makes Smartspace consumption read-only on AOD.

Allowing direct module-side actions from a dozing surface would require additional falsing, authentication, and launch policy ownership.

## Decision

Keep Pixel AOD **presentation-only while dozing**.

1. Notification, media, and Smartspace/contextual actions are not executed directly from the Pixel AOD surface.
2. A touch may participate only in the existing vendor wake/Keyguard transition.
3. Once the device reaches normal Keyguard, the system's existing interaction and authentication rules apply.
4. Vendor-owned UDFPS remains the explicit biometric exception; Pixel AOD does not intercept its primary touch/auth path.
5. Do not add a module-owned falsing manager solely for AOD content actions.

## Consequences

- AOD stays simple, safe, and consistent with vendor authentication ownership.
- Contextual content can be richer without creating a second interaction/security stack.
- Interactive features, if ever added, require a new architecture decision rather than incremental event handlers.

## Rejected alternatives

- Allow selected direct controls such as media pause: creates inconsistent action/security semantics.
- Build a fully interactive AOD with custom falsing: outside the presentation-parity ownership boundary.
