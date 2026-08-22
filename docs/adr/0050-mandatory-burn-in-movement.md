# ADR 0050: Make burn-in movement mandatory in supported ambient presentation

Date: 2026-08-22
Status: Accepted

## Context

ADR 0006 treats burn-in movement as a separate required safety property from low-power pixel density. The current settings schema still exposes a normal `disable_burn_in_offset` preference, which allows a supported persistent AOD configuration to bypass that protection entirely. The implementation also already knows that vendor-managed movement can exist, so blindly stacking module movement on top is unnecessary.

## Decision

Adopt **mandatory burn-in movement** for supported Pixel AOD runtime behavior.

1. Remove the normal user-facing ability to permanently disable burn-in movement.
2. Whenever Pixel AOD owns persistent ambient presentation, some validated burn-in movement must remain active.
3. If OPlus reliably moves the same presentation host, disable only the module's duplicate offset so movement is not applied twice.
4. Clamp all movement to the ADR 0045 native target/safe region and keep it inside ADR 0046 OPR-tested scene bounds.
5. Permit a stationary override only as a debug-only, non-persistent development aid; it is not a supported release mode.
6. Treat missing movement on a persistent Pixel-owned AOD scene as a release-blocking safety defect.

## Consequences

- A normal settings toggle can no longer invalidate the project's burn-in safety contract.
- Vendor-managed and module-managed movement can coexist without double translation.
- Burn-in validation becomes an unconditional M9 release requirement for persistent module-owned ambient scenes.

## Rejected alternatives

- Keep the user switch with a warning: still allows a supported configuration to violate the safety contract.
- Allow permanent static AOD as an advanced option: makes burn-in risk an ordinary product feature rather than a debug exception.
