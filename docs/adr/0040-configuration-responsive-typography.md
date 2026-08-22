# ADR 0040: Make Pixel/COUI typography respond to density and font-scale configuration

Date: 2026-08-22
Status: Accepted

## Context

Android 17 clock controllers receive density/font-scale configuration changes and recompute clock font sizes. Pixel AOD currently records display configuration but its core `scaledClockTextDp()` returns the unscaled base DIP, and multiple information rows rely on fixed DIP geometry. That can leave stale or inaccessible presentation after display-size/font-scale changes.

## Decision

Adopt **configuration-responsive typography** for M9.

1. Observe reliable SystemUI configuration changes affecting density, display size, and font scale.
2. Recompute clock, date, weather, contextual, media, notification/indication typography and dependent layout geometry when those inputs change.
3. Preserve Pixel/COUI clock proportions with validated minimum/maximum bounds instead of unrestricted scaling.
4. Resolve constrained space through compact-layout and contextual-arbitration policy rather than ignoring the user's configuration.
5. Include RTL, locale, large-font, display-size, and low-power visual-budget combinations in deterministic validation.

## Consequences

- Runtime presentation follows accessibility/display configuration changes without process restart.
- Layout and typography become one coherent configuration-dependent model.
- Existing fixed-DIP assumptions must be audited during M9 implementation.

## Rejected alternatives

- Scale only auxiliary text: leaves the primary clock disconnected from the system configuration.
- Keep all Pixel sizes fixed: preserves screenshots at the cost of Android configuration/accessibility parity.
