# ADR 0038: Avoid collisions with vendor-owned ambient foreground surfaces

Date: 2026-08-22
Status: Accepted

## Context

ADR 0021 preserves native full-pulse notification/HUN presentation instead of cloning it. Android 17 also adjusts lockscreen/AOD clock centering when pulsing or promoted notification foreground content occupies one side of the display. Without a corresponding geometry contract, a preserved native foreground can overlap the Pixel background clock.

## Decision

Add a read-only **native ambient foreground collision adapter**.

1. Prefer reliable OPlus/SystemUI foreground visibility, centering, or occupied-bounds state for full-pulse, promoted, or equivalent ambient foreground surfaces.
2. Temporarily move the Pixel clock and dependent information geometry out of the occupied region while retaining the native foreground as interaction owner.
3. Restore the canonical Pixel/COUI geometry deterministically when the foreground leaves.
4. Do not infer collision from notification app identity, text, or guessed card dimensions when a native geometry signal is unavailable.
5. Apply ADR 0006 visual-budget and burn-in constraints to every temporary avoidance layout.

## Consequences

- Native pulse/promoted content can coexist with the Pixel background without visual overlap.
- The project keeps presentation ownership split cleanly: vendor foreground, Pixel background geometry.
- Collision handling becomes testable as a deterministic scene rather than an incidental overlay race.

## Rejected alternatives

- Leave the clock fixed: permits direct overlap with native foreground content.
- Hide all Pixel content during every pulse: unnecessarily loses ambient context when a safe avoidance layout is possible.
