# ADR 0062: Require native AOD availability for continuous Pixel AOD

Date: 2026-08-22
Status: Accepted

## Context

A native AOD preference being enabled does not by itself prove that the current device/configuration can provide continuous always-on display. Android distinguishes user enablement from AOD availability/capability. Earlier decisions already require native enablement and a valid vendor lifecycle, but the module does not yet have a separate availability gate.

## Decision

Add a read-only **native AOD availability gate**.

1. Continuous Pixel AOD requires all three conditions: native AOD availability, the selected user's native AOD-enabled preference, and a valid OPlus/SystemUI ambient lifecycle.
2. When native availability is false, the module must not create or keep continuous AOD alive through overlay, screen-state rewrite, timer, or suppression hacks.
3. Vendor-authorized transient notification pulse, wake gesture, UDFPS/authentication, and similar capability-specific paths remain governed by the typed suppression/capability model rather than inheriting the base AOD denial automatically.
4. Prefer a stable OPlus/SystemUI availability signal; use conservative capability detection only as fallback.
5. Availability changes invalidate presentation eligibility without making the module a display-power owner.

## Consequences

- Continuous Pixel AOD cannot appear on a configuration where native SystemUI considers AOD unavailable.
- Native enabled/available/lifecycle signals have distinct responsibilities.
- Authentication and transient vendor paths remain available when the vendor explicitly supports them independently.

## Rejected alternatives

- Treat native enabled as equivalent to available: misses device/configuration capability constraints.
- Let the module decide availability from its ability to draw an overlay: recreates the display lifecycle the architecture intentionally delegates to OPlus.
