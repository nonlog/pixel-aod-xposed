# ADR 0006: Low-power visual budget for AOD scenes

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already applies burn-in movement, but its richer scenes can simultaneously include clock, date/current weather, contextual content, media, notification icons and battery/status text. Burn-in movement alone does not bound lit-pixel density or idle rendering cost.

ADR 0001 keeps ambient panel brightness under OPlus ownership, so the module needs a presentation/test constraint rather than an independent brightness controller.

## Decision

Introduce a **low-power visual budget** for Pixel AOD scenes.

1. Define measurable lit-pixel/content-density budgets for representative Large, Small, Media, Smartspace/contextual and notification-overflow AOD scenes.
2. Enforce the budget primarily in design/tests, using deterministic screenshots or off-screen rendering where practical.
3. Keep OPlus authoritative for ambient panel brightness and low-power display state.
4. Treat clock, date/current weather and system-owned FOD as core presentation; optional contextual/media/status density must fit around that contract.
5. When a scene exceeds budget, fix the deterministic layout/presentation policy rather than randomly hiding content at runtime.
6. Keep burn-in movement as a separate requirement; passing one does not imply passing the other.

## Consequences

- AOD power/burn-in safety becomes a measurable release gate instead of a visual guess.
- Rich content changes need explicit budget evidence before release.
- CI can detect accidental increases in lit-pixel density without needing to own device brightness.
- Thresholds should be calibrated against AOSP/Pixel guidance and real-device screenshots rather than chosen arbitrarily.

## Rejected alternatives

- Rely only on burn-in movement: protects against static placement but does not bound pixel density or idle power.
- Add module-owned ambient brightness control: conflicts with ADR 0001 and duplicates vendor panel ownership.
- Dynamically hide random content whenever a pixel threshold is exceeded: unpredictable UX and hard to test.
