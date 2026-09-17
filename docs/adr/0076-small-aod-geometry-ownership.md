# ADR 0076: Single geometry owner during Small AOD entry

## Context

After 0.1.52 moved the accepted compact AOD endpoint right/up, the tomorrow-forecast row could still appear to drift horizontally on one entry and vertically on another. 0.1.53-0.1.56 progressively stabilized the row's own geometry, but physical runtime logs on 0.1.56 exposed two remaining ownership violations.

First, the normal Lockscreen -> AOD path does not use beginAodEntry(); ClockPlugin drives it through present(). The 0.1.55 deferred contextual reveal therefore protected only the screen-off-from-unlocked normalization path. On the normal path, contextual pixels could become visible before the 550 ms AOD target transaction settled.

Second, the COUI host still generated its own two-axis burn-in offset on this OOS 16.0.9-class ROM even though the established OPlus handoff profile already declares native ClockPlugin/SystemUI as the burn-in owner. Runtime evidence showed a non-zero child offset during the affected Small AOD transition.

## Decision

- Any animated non-dozing -> Small AOD present() transaction keeps contextual pixels transparent for the same target window while allowing the row to reserve its final layout slot. At completion the row is re-primed at the settled endpoint and revealed by alpha only.
- Burn-in ownership is resolved before contextual geometry priming. On builds where OosAodHandoffProfile.usesSystemManagedBurnIn(...) is true, the COUI child applies zero X/Y burn-in, including through the external burn-in seam.
- 0.1.52's 32 dp painted leading edge and -16 dp compact AOD upper-stack endpoint remain unchanged.
- 0.1.56's stable current-weather slot reservation remains unchanged.

## Consequences

The Small AOD clock still performs the requested right/up transition, but the forecast row is never exposed while two geometry transactions can disagree. OPlus remains the sole burn-in owner on OOS 16.0.9+; older builds retain the module fallback. No AOD power, notification lifetime, pocket/proximity, UDFPS, or weather-fetch policy changes are introduced.
