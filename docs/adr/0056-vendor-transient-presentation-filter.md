# ADR 0056: Redefine Trigger only as a vendor transient presentation filter

Date: 2026-08-22
Status: Accepted

## Context

The current Trigger only implementation owns a fixed-duration module brief-AOD session, including a ten-second timer, proximity/power guards, expiry callbacks, and changes to module AOD-active state. ADRs 0001, 0002, and 0007 instead place ambient lifecycle, pulse timing, and wake-trigger ownership with OPlus/SystemUI.

## Decision

Keep the user-facing Trigger only mode, but redefine it as a **vendor transient presentation filter**.

1. Pixel content may appear only while OPlus/SystemUI is already in a valid transient ambient scene such as a notification pulse, pickup/tap wake window, UDFPS/auth pulse, or equivalent vendor state.
2. The lifetime of the Pixel presentation exactly follows the vendor scene; the module does not start or extend a fixed-duration ambient session.
3. Remove module ownership of `TRIGGER_BRIEF_AOD_DURATION_MS`, trigger expiry, trigger-only proximity sensor guards, and trigger-only mutations of AOD-active lifetime during M9 implementation.
4. ADR 0002 may still request a supported vendor notification pulse entry point, but once accepted, timing and termination remain vendor-owned.
5. Existing Trigger only UI may remain as a presentation preference because it no longer changes panel or sensor lifecycle.

## Consequences

- Existing product behavior remains recognizable without violating vendor lifecycle ownership.
- Native pulse, wake, and authentication duration semantics are preserved.
- Trigger-only code becomes substantially simpler and loses a class of stale timer/reassert failures.

## Rejected alternatives

- Remove Trigger only entirely: unnecessary because a presentation-only version fits the architecture.
- Preserve the independent ten-second lifecycle: conflicts directly with accepted lifecycle and wake-trigger ownership.
