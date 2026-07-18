# Panel Handoff Blackout Design

## Goal

Make the unavoidable OOS panel blank during screen-off look intentional by extending only its presentation blackout, without preventing or rewriting the final `DOZE_SUSPEND` power state.

## Required Sequence

1. Preserve the existing lockscreen-to-AOD clock weight transition.
2. Detect the active native panel-handoff event after the first AOD frame.
3. Keep the Pixel AOD presentation hidden for a short bounded interval that overlaps and extends the hardware blank.
4. Keep stock OOS AOD content suppressed throughout the interval.
5. Reveal the Pixel AOD once on an animation frame after the interval.
6. Leave OOS fingerprint timeout and final `DOZE_SUSPEND` behavior intact.

## Architecture

`PanelHandoffGate` is a pure state controller. It owns a generation, deadline, and cancellation rules but has no Android or power-state APIs. `PixelAodClockView` applies the resulting presentation gate to overlay visibility. `PixelAodHook` only observes confirmed OOS lifecycle/FOD callbacks and opens or cancels the gate; it must not call `setDozeScreenState`, `requestScreenState`, or otherwise force a display power state.

The first implementation is deliberately independent of a future single-renderer clock refactor. It must not add a wrapper around the existing AOD and lockscreen view trees.

## Safety Rules

- A new AOD trace invalidates all callbacks from an older trace.
- Screen-on, proximity-near, module disable, policy denial, or AOD exit cancels the gate immediately.
- Repeated matching handoff events may extend the current deadline but may not create repeated reveal cycles.
- Gate completion posts the reveal on the next animation frame.
- Stock AOD suppression remains active while the Pixel overlay is presentation-gated.
- No display power state or brightness rewrite is allowed.

## Verification

- Pure JVM tests cover generation replacement, duplicate coalescing, cancellation, and deadline completion.
- The diagnostic script records gate open/extend/cancel/reveal events alongside FOD, WindowManager, DisplayPowerController, and SurfaceFlinger events.
- Device QA confirms one initial clock transition, one intentional blackout, one final reveal, normal fingerprint fade, and a final `DOZE_SUSPEND` state.
