# ADR 0031: Treat native AOD enablement as the authority for continuous Pixel AOD

Date: 2026-08-22
Status: Accepted

## Context

Android 17 resolves ordinary Doze versus always-on Doze from the selected user's always-on configuration and current SystemUI lifecycle. Pixel AOD currently exposes its own continuous/trigger presentation modes. Allowing that module setting to override a native AOD-off state would make the module a second panel/Doze owner, contrary to ADR 0001.

## Decision

Use **vendor AOD enable authority**.

1. Continuous Pixel AOD requires both native OPlus/SystemUI AOD enablement for the selected user and a vendor lifecycle state that permits AOD presentation.
2. Module settings may further restrict Pixel presentation but cannot turn a native AOD-off state into continuous AOD.
3. Do not write or synchronize the vendor AOD setting from the module.
4. Vendor-authorized transient notification, wake-trigger, and biometric/auth pulse paths remain capability-gated independently from continuous AOD.
5. Remove or neutralize runtime keepalive behavior that exists only to defeat a legitimate native AOD-off transition.

## Consequences

- The system setting remains the user's authoritative control over continuous ambient display.
- Pixel AOD becomes a replacement presentation, not an alternate AOD service.
- Trigger/auth behavior can still work when vendor policy explicitly allows it without restoring full continuous AOD.

## Rejected alternatives

- Let the module continuous switch override native AOD-off: creates competing lifecycle ownership.
- Have the module modify the OPlus AOD setting: couples the project to private settings and violates the presentation-only boundary.
