# ADR 0032: Normalize ambient suppression by capability

Date: 2026-08-22
Status: Accepted

## Context

Android 17 suppression semantics are not uniformly all-or-nothing. Always-on display and notification pulses can be suppressed while wake gestures or specific biometric authentication paths remain permitted. ADR 0005 established one common vendor suppressor boundary, but a single boolean would lose these distinctions.

## Decision

Refine the suppressor boundary into **typed ambient suppression capabilities**.

1. Normalize validated vendor/SystemUI policy into independent capability decisions for at least base AOD, notification pulse, contextual presentation, wake gestures, and authentication pulse.
2. Keep all capability values vendor-derived; the module does not infer permission merely because another capability is allowed.
3. Adapters consume only the capability relevant to their surface or lifecycle path.
4. Preserve a common diagnostic model so the originating vendor reason remains traceable.
5. Unknown capability state falls back conservatively to the already validated vendor lifecycle behavior rather than broad module heuristics.

## Consequences

- Legitimate auth and wake paths are not accidentally disabled by a coarse base-AOD suppression bit.
- Notification/contextual content cannot use unrelated allowed capabilities to bypass suppression.
- ADR 0005 remains the common suppression architecture but gains sufficient semantic precision.

## Rejected alternatives

- One global suppression boolean: loses native distinctions and can over-block valid behavior.
- Let each feature infer its own policy independently: fragments the single policy boundary and risks inconsistent behavior.
