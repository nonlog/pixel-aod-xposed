# ADR 0026: Consume native dozing indication priority

Date: 2026-08-22
Status: Accepted

## Context

AOSP Keyguard/AOD presentation includes a prioritized dozing indication lane for transient biometric help/error, temporary system indications, alignment state, charging information, and battery state. Pixel AOD currently has a battery/status row but does not consume a comparable vendor-resolved transient indication result. Rebuilding biometric and transient indication policy inside the module would duplicate SystemUI ownership.

## Decision

Add a read-only **native dozing indication adapter**.

1. Prefer a stable OPlus/SystemUI dozing indication result for transient biometric/help/error, transient system indications, alignment state, and equivalent temporary messages.
2. Present at most one high-priority indication in the module's indication lane at a time.
3. Allow the transient indication to temporarily replace the ordinary battery/power row while active.
4. Keep stable charging semantics under ADR 0025 and do not duplicate biometric policy, retry timing, or authentication ownership.
5. Expire or clear module presentation immediately when the vendor indication is no longer valid.
6. Fall back to the existing battery/status presentation when no trustworthy vendor indication is available.

## Consequences

- Pixel AOD can expose important native dozing feedback without creating a second biometric or transient-message state machine.
- Bottom-of-screen content has a single deterministic priority lane rather than stacked status rows.
- Validation must cover transient replacement, expiration, and restoration of normal battery/power content.

## Rejected alternatives

- Support only biometric errors: leaves other native dozing indications outside the presentation contract.
- Reimplement all Keyguard indication policy in the module: duplicates fast-changing SystemUI semantics and authentication ownership.
