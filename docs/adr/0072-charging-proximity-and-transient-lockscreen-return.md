# ADR 0072: Preserve proximity authority and preload transient lockscreen returns

## Status

Accepted for the 0.1.50 candidate. Physical acceptance on CPH2573 is still required.

## Context

Two historical lifecycle defects remained after the Power Saving AOD extension and persistent
COUI ClockPlugin host work.

First, the charging exception is intentionally allowed to keep OPlus Power Saving AOD in
`DOZE_SUSPEND` after the vendor energy-saving deadline. `OosAodLifecycleAdapter` already encoded
the required ownership rule that proximity/pocket suppression must override this exception, but
the default `PixelAodClockView.evaluateAodPolicy(context, source)` caller path supplied
`proximityBlocked=false`. `DreamService#setDozeScreenState(OFF)` uses that default path, so a real
vendor pocket-mode OFF request could be converted back to `DOZE_SUSPEND` while charging.

Second, the persistent COUI clock host is deliberately kept as a child of the native ClockViewRoot
through lockscreen <-> `OCCLUDED`/bouncer transitions. Native SystemUI owns the root visibility.
When an alarm/call entered from an ambient state, the hidden child could still retain an AOD
presentation and thin variable-font weight. On `OCCLUDED -> LOCKSCREEN`, the native root begins
revealing before the later ClockPlugin keyguard render reaches the module. That allowed the stale
thin child to become visible before the normal lockscreen presentation changed it to the thicker
lockscreen weight. A pure `forcedLockscreenEntry()` mapping already existed but had no runtime
caller.

## Decision

1. Default AOD policy evaluation passes the current OPlus-derived proximity state into
   `OosAodLifecycleAdapter`. Explicit overlay evaluation keeps its existing proximity argument.
   Therefore charging can keep native doze alive only while proximity is clear.
2. Treat `OCCLUDED`/primary-bouncer/alternate-bouncer `-> LOCKSCREEN STARTED` as an authoritative
   opportunity to prepare the module child, not as authority to change native visibility.
3. While the native root still owns visibility, read the current ClockPlugin clock-size tracker and
   apply `forcedLockscreenEntry()` with animation disabled. If that tracker is transiently
   unavailable, use only the last real non-AOD ClockPlugin scene already recorded by the host.
4. Apply the lockscreen presentation synchronously on the hidden `CouiClockHostView`: cancel stale
   module transitions, update variable-font style/geometry, and run `applyTargets(false, 0)` before
   the native root can expose the child.

## Ownership boundaries

This change does not register a proximity sensor, create a dwell timer, call `PowerManager.wakeUp`,
hook DisplayPowerController, directly request panel/HBM state, or change OPlus pocket decisions.
It also does not set native ClockViewRoot visibility or synthesize an alarm/call transition. OPlus
remains authoritative for both lifecycles.

## Verification

- `OosAodVendorLifecycleOwnershipTest` already proves a charging hold is rejected when proximity is
  blocked.
- `PowerSavingAodWiringTest` now verifies the default lifecycle path carries live proximity into the
  `DreamService` charging decision.
- `CouiBouncerHostVisibilityPolicyTest` verifies only the authoritative transient-to-lockscreen
  STARTED edge preloads the child.
- `CouiClockTransientReturnWiringTest` verifies the native transition hook reaches the forced
  lockscreen mapping and that the hidden child is restyled synchronously without changing
  visibility.
- Local 0.1.50 unit-test run: 599 tests, 0 failures, 0 errors, 0 skipped.
