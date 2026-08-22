# ADR 0039: Add a presentation-only Pixel/COUI charge animation

Date: 2026-08-22
Status: Accepted

## Context

Android 17 clock presentation triggers a dedicated charge animation on the transition from not charging to charging. Pixel AOD already presents charging text/state but has no equivalent clock animation. Charging semantics and power ownership remain vendor/SystemUI responsibilities under ADR 0025 and ADR 0001.

## Decision

Add a **Pixel/COUI charge animation** as presentation-only behavior.

1. Trigger only on a validated charging-semantic edge from not charging to charging.
2. Animate only the currently visible Pixel/COUI clock face; do not replay on ordinary battery percentage updates.
3. Respect system animator-scale settings and skip or reduce the effect when animations are disabled.
4. Keep the effect within ADR 0006 low-power visual-budget constraints on AOD.
5. Do not control charging overlays, panel state, display brightness, HBM/local-HBM, or charging lifecycle.

## Consequences

- Plug-in transitions gain a visible Android/Pixel-equivalent clock response.
- Charging state remains sourced from the vendor power indication boundary.
- Automated tests can treat the false-to-true charging edge as a deterministic one-shot event.

## Rejected alternatives

- Text-only charging updates: leaves a visible clock-parity gap.
- Let the module own charging UI or power behavior: exceeds the presentation-only boundary.
