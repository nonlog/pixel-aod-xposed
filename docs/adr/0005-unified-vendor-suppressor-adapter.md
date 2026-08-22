# ADR 0005: Unified vendor ambient-display suppressor adapter

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already gates presentation for module enablement, schedule, power saver, low battery and proximity/pocket state. AOSP Doze also honors broader ambient-display suppression state owned by SystemUI/system policy. Relying only on eventual vendor panel state can leave a timing or overlay gap where Pixel AOD still presents content after the system has decided ambient display should be suppressed.

ADR 0001 keeps system policy ownership with OPlus/SystemUI, so Pixel AOD should consume suppression state rather than duplicate AOSP `DozeSuppressor` policy.

## Decision

Implement a **unified vendor suppressor adapter**.

1. Observe only stable OPlus/SystemUI ambient-display suppression signals available on the current ROM.
2. Normalize them into a module-owned `AmbientSuppressionReason` model.
3. Feed the normalized suppression reasons into the typed capability policy from ADR 0032 for base AOD, notification pulse, contextual content, wake gestures, and authentication pulse; this ADR does not imply one all-or-nothing suppression boolean.
4. ADR 0034 remains the explicit exception that allows a vendor-authorized authentication pulse when base AOD is suppressed.
5. When suppression clears, reevaluate the current real vendor lifecycle state; do not force a panel wake or create an independent Doze transition.
6. If a suppression hook is missing or its semantics are uncertain on a supported ROM, fail open to the vendor lifecycle and record diagnostics rather than inventing a hard suppression policy.

## Consequences

- A system request to suppress ambient display cannot be bypassed by a lingering module overlay.
- Pulse, Smartspace and base AOD presentation share one suppression interpretation instead of drifting into separate policy implementations.
- Suppression reasons should be traceable in diagnostics so vendor-policy hides can be distinguished from schedule, power, proximity and privacy gates.
- New ROM-specific suppressors can be added as adapters without changing presentation code.

## Rejected alternatives

- Keep only the existing module-specific gates: lower complexity, but leaves system/vendor suppression implicit and can produce timing mismatches.
- Reimplement AOSP `DozeSuppressor` and system policy decisions: conflicts with ADR 0001 and duplicates vendor ownership.
