# ADR 0074: Compact AOD leading-edge and vertical endpoint

## Status
Accepted — 2026-09-17

## Context
The lockscreen compact clock is already visually aligned with the native lockscreen notification card, but the compact AOD endpoint used a center-derived glyph target whose lighter AOD font metrics could move the painted clock edge left of the AOD contextual/media/notification column. The compact AOD information/content stack also retained the lockscreen vertical baseline, producing a visible left/down handoff.

## Decision
- Keep the lockscreen compact geometry unchanged.
- In compact AOD only, align the clock's painted leading edge to the existing 32 dp COUI content column. The host computes an optical alignment shift after the stable clock target is built, mirrors the rule for RTL, and applies the same burn-in X to clock and AOD content rows.
- Move the compact AOD clock, date/weather/contextual information, media, and notification rows 16 dp upward as one upper-stack endpoint. Battery placement remains independently owned.
- Preserve the existing target animation: changing the endpoint makes the lockscreen-to-AOD motion resolve toward the corrected right/up geometry without inventing a separate inverse animation.

## Consequences
The AOD clock and lower AOD content share one visual leading edge, and the upper ambient stack keeps its internal spacing while moving upward. Lockscreen layout, native notification ownership, panel/doze ownership, proximity behavior, and UDFPS behavior are unchanged.
