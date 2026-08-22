# ADR 0030: Include RTL presentation support in M9

Date: 2026-08-22
Status: Accepted

## Context

The module manifest currently declares `android:supportsRtl="false"`, even though some geometry helpers already understand Android layout direction. Android/SystemUI presentation is expected to respect right-to-left locales across clock-adjacent text, notification/context rows, and START/END alignment. Unlike foldable support, RTL validation does not require device-specific hardware and is a practical M9 parity target.

## Decision

Include **RTL product support** in M9.

1. Enable RTL support at the application/product level when runtime implementation is ready.
2. Validate AOD and lockscreen clock, date, weather, contextual, media, notification, battery/indication, and overflow presentation under RTL layout direction.
3. Use START/END and bidi-aware text semantics rather than hard-coded LEFT/RIGHT assumptions where direction should follow the locale.
4. Preserve direction-neutral visual elements and COUI hierarchy instead of mechanically mirroring every drawable or geometry value.
5. Treat mixed-direction content and localized digits as explicit test cases.

## Consequences

- RTL is an M9 quality requirement rather than an undeclared limitation.
- Existing direction-aware geometry can become part of a consistent end-to-end contract.
- Visual regression coverage must include at least one representative RTL locale.

## Rejected alternatives

- Support bidi text but keep LTR layout: produces partially broken native behavior.
- Defer RTL indefinitely: unnecessary given the lack of hardware dependency and existing direction-aware code seams.
