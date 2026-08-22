# ADR 0015: Defer foldable and posture-aware AOD support

Date: 2026-08-22
Status: Accepted

## Context

AOSP Doze can vary sensor and ambient behavior by device posture, but this project is currently validated on straight-screen OPlus hardware. Implementing foldable behavior without a real supported device and reliable OPlus posture/display-role signals would make correctness impossible to verify.

## Decision

Keep foldable/posture-aware AOD **out of M9 scope**.

1. Treat straight-screen OPlus devices as the validated product scope for the Android 17 parity milestone.
2. Leave architecture seams so a future vendor posture/display-role adapter can be added without replacing core presentation policy.
3. Do not register module-owned posture sensors.
4. Do not infer inner/outer display role from geometry, orientation, hinge guesses, or other heuristics.
5. Reopen the feature only when real supported foldable hardware and stable OPlus runtime evidence are available.

## Consequences

- M9 stays physically testable and avoids speculative foldable debt.
- Future foldable support remains possible but must earn its own device-specific validation matrix.
- Lack of foldable parity is an explicit scope boundary, not an accidental omission.

## Rejected alternatives

- Build a vendor posture adapter now without hardware: untestable and likely to encode wrong assumptions.
- Implement a module-owned fold sensor stack: conflicts with vendor-delegated lifecycle ownership.
