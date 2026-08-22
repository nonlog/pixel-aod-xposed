# ADR 0065: Limit M9 Pixel AOD replacement to the primary display

Date: 2026-08-22
Status: Accepted

## Context

Android's clock API supports secondary-display updates, but the current Pixel AOD runtime is designed around the default display and has no validated secondary-display lifecycle, safe-region, burn-in, notification, or geometry authority for OPlus hardware. Expanding the replacement to external/secondary displays without real hardware evidence would multiply lifecycle risk for a feature that is not required for the current phone target.

## Decision

Adopt **primary-display-only M9 scope**.

1. M9 Pixel AOD/Lockscreen replacement attaches only to the device's primary built-in/default display.
2. Do not mirror or instantiate the Pixel ambient host on secondary or external displays.
3. Ignore secondary-display clock parity until real OPlus hardware and reliable vendor lifecycle, safe-region, burn-in, and scene signals are available for testing.
4. A secondary display must not cause primary-display geometry, scene, or ambient-session state to be reused blindly across displays.
5. Future multi-display support requires its own validated design rather than extending this ADR implicitly.

## Consequences

- M9 remains focused on the tested phone display and avoids unvalidated lifecycle expansion.
- Existing primary-display animation and geometry behavior is not destabilized by a new display dimension.
- Secondary-display parity remains an explicit future scope item.

## Rejected alternatives

- Implement full secondary-display support in M9: lacks hardware and vendor-seam validation.
- Reuse the primary-display host/layout on every display: assumes incompatible geometry and power/lifecycle semantics.
