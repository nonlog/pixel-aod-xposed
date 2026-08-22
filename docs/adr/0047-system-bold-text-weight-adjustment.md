# ADR 0047: Apply system Bold text as an adjustment to Pixel/COUI base weight

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD exposes separate AOD and lockscreen clock-weight controls. Android/SystemUI also exposes accessibility-driven font weight adjustment through configuration, including the system Bold text preference. Ignoring that native adjustment would make the replacement clock diverge from an explicit accessibility setting, while removing module base-weight controls would unnecessarily discard existing product customization.

## Decision

Use a **system bold-text weight adjustment** layered over the module base weight.

1. Treat module AOD/lockscreen weight settings as the base font-axis value.
2. Read the current selected-user/SystemUI `fontWeightAdjustment` or equivalent validated configuration input.
3. Apply that adjustment once, then clamp the final value to the validated Pixel/COUI typeface axis range.
4. Recompute the visible lockscreen/AOD host immediately when the configuration changes.
5. Keep transition interpolation, charge animation, and Doze weight morphs operating on the adjusted endpoints rather than bypassing the accessibility setting.

## Consequences

- Existing Pixel/COUI weight customization remains available.
- Android Bold text and related accessibility configuration are respected consistently.
- Weight tests need normal and adjusted configurations across lockscreen and AOD.

## Rejected alternatives

- Ignore Bold text whenever a module weight is selected: violates the system accessibility preference.
- Remove module weight settings and use only system adjustment: gives up reversible product customization without an architectural need.
