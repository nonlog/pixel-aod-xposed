# ADR 0012: Native Android 17 MinMode handoff

Date: 2026-08-22
Status: Accepted

## Context

Android 17 introduces a native MinMode path in the Doze/AOD stack. Pixel AOD already overlays vendor AOD presentation, so a future OPlus MinMode implementation could otherwise create two simultaneous presentation owners.

ADR 0001 keeps vendor Doze lifecycle ownership with OPlus/SystemUI.

## Decision

Implement a **native MinMode handoff adapter** when, and only when, OPlus/SystemUI exposes a stable MinMode state.

1. While native MinMode is active, Pixel AOD yields its clock/content presentation surface.
2. When MinMode exits, Pixel AOD reevaluates the current real vendor lifecycle before resuming.
3. Pixel AOD does not host third-party MinMode activities or views.
4. Pixel AOD does not synthesize MinMode from unrelated activity, notification, or screen state.
5. Missing vendor MinMode support is treated as capability absence, not a reason to emulate it.

## Consequences

- Native Android 17/OPlus MinMode can coexist without double-rendered AOD ownership.
- MinMode support remains capability-gated and safe on older ROMs.
- The adapter is a presentation handoff, not a new lifecycle owner.

## Rejected alternatives

- Ignore MinMode and keep Pixel AOD visible: risks competing AOD surfaces.
- Implement a module-owned MinMode host: outside the presentation-parity architecture and duplicates system responsibilities.
