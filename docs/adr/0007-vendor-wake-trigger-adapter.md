# ADR 0007: Vendor wake trigger adapter

Date: 2026-08-22
Status: Accepted

## Context

AOSP Doze exposes wake-up gestures including tap, double tap, lift/pickup, and significant motion through its low-power sensor stack. Pixel AOD already observes some OPlus pickup/tap/doze events, but does not model all supported vendor wake signals through one presentation contract.

ADR 0001 keeps sensor registration and panel/doze ownership with OPlus/SystemUI.

## Decision

Implement a **vendor wake trigger adapter**.

1. Consume only stable OPlus/SystemUI wake-trigger signals available on the supported ROM.
2. Normalize tap, double-tap, lift/pickup, significant-motion equivalents, and future stable vendor triggers into one transient-presentation trigger observation model.
3. ADR 0056 defines the lifetime semantics: Trigger only follows the already-valid vendor transient scene and owns no independent fixed-duration brief-AOD timer.
4. Reuse common schedule, suppression, proximity, power, and privacy gates before presentation.
5. Never register a duplicate Doze sensor stack.
6. Never treat trigger observation as authority to force panel state independently of OPlus.

## Consequences

- Trigger-only behavior can approximate Pixel/AOSP wake semantics without duplicating low-power sensors.
- Unsupported trigger categories remain absent instead of being inferred from unrelated motion or screen events.
- Diagnostics need to retain both raw vendor source and normalized trigger reason.

## Rejected alternatives

- Keep separate ad-hoc pickup/tap handling forever: leaves semantics fragmented.
- Register module-owned wake sensors: conflicts with ADR 0001 and risks duplicate power use.
