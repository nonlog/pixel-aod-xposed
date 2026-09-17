# ADR 0075: Freeze Small AOD forecast anchor across async weather refresh

## Context

0.1.52 intentionally moved the compact AOD endpoint right/up and shifted the complete Small AOD upper stack 16 dp upward. That geometry is accepted and remains the target. The tomorrow forecast row, however, still derived its vertical anchor from the live visibility and height of the current-weather row. Current weather can arrive asynchronously after the AOD entry transaction, so the forecast could first be positioned as if no weather row existed and then be pushed to a second Y coordinate when weather text or artwork became visible.

0.1.53 through 0.1.55 tightened first-frame geometry and delayed visible forecast pixels during entry, but those timing guards cannot prevent a current-weather update that arrives after the entry animation has already finished.

## Decision

- A dozing Small scene reserves one stable current-weather row slot from its first geometry transaction, even when current-weather text and artwork are not yet visible.
- Both the normal information-target path and the contextual pre-layout prime path use the same reservation rule and stable weather-row height.
- Lockscreen Small, Large, and other scenes retain their existing visibility-dependent geometry.
- The accepted 0.1.52 compact clock endpoint, 32 dp leading-edge alignment, burn-in ownership, and right/up transition direction are unchanged.
- The 0.1.55 alpha-only deferred forecast reveal remains as entry-time protection.

## Consequences

A Small AOD with no current-weather payload still reserves the weather-line height above tomorrow forecast content. If weather arrives later, the row fills the reserved slot without changing the forecast Y coordinate or lower media/notification geometry. No weather fetching, AOD lifetime, power, proximity, or notification policy changes are introduced.
