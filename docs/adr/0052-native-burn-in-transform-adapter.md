# ADR 0052: Prefer the native burn-in transform for Pixel ambient movement

Date: 2026-08-22
Status: Accepted

## Context

ADR 0050 makes burn-in movement mandatory, but it does not define which trajectory should drive the Pixel host. Android 17 SystemUI has a burn-in model that can provide translation and scale, while the current module has its own deterministic fallback trajectory. Applying both can double the visible movement; relying only on the module ignores a validated native transform that already tracks SystemUI configuration.

## Decision

Add a read-only **native burn-in transform adapter**.

1. Prefer stable OPlus/SystemUI burn-in X/Y translation and scale when they are available for the relevant ambient host.
2. If the vendor already transforms the exact host containing Pixel AOD, do not apply a second module transform.
3. If no reliable native transform exists, use the module's validated fallback trajectory so ADR 0050 remains satisfied.
4. Clamp whichever transform is active to the ADR 0045 safe/target region and ADR 0046-tested scene geometry.
5. Treat transform source changes as a deterministic handoff rather than summing two independent burn-in motions.

## Consequences

- Pixel content moves with the same ambient burn-in model as SystemUI when a stable seam exists.
- Double movement and cumulative translation are avoided.
- Unsupported vendor builds still retain mandatory burn-in protection through the module fallback.

## Rejected alternatives

- Always use the module trajectory: can fight or duplicate native movement.
- Depend exclusively on native transform discovery: a missing seam could leave persistent Pixel AOD static.
