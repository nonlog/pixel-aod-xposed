# ADR 0060: Reset module-owned biometric transients at ambient session boundaries

Date: 2026-08-22
Status: Accepted

## Context

Primary fingerprint/authentication lifecycle remains OPlus/SystemUI-owned under ADRs 0001, 0017, and 0034, but Pixel AOD can still own optional presentation state such as success ripple, pressed/highlight residue, and exit-animation callbacks. Those transients must not survive into a later Doze/AOD session after the vendor has reset or changed its authentication state.

## Decision

Add a **biometric transient session reset** tied to ADR 0059.

1. On every new vendor ambient session epoch, clear all module-owned biometric presentation residue before accepting new biometric events.
2. On ADR 0035 terminal/FINISH teardown, immediately clear the same transient state and cancel its pending callbacks/animations.
3. Scope success ripple, pressed/highlight state, optional exit animation, and related presentation callbacks to the epoch in which they were created.
4. New biometric presentation may appear only from a fresh vendor event associated with the current valid session.
5. Never call OPlus/SystemUI authentication mutators, clear vendor recognized state, or otherwise make the module an authentication-state owner.

## Consequences

- Stale fingerprint visuals cannot leak across ambient sessions.
- Biometric presentation becomes compatible with the shared ambient-session cancellation boundary.
- Vendor authentication state and HBM/FOD lifecycle remain untouched.

## Rejected alternatives

- Let module transients expire only by their existing timers: stale visuals can outlive the vendor session.
- Clear vendor fingerprint-recognized/authentication state as part of reset: violates the vendor-owned biometric boundary.
