# ADR 0029: Preserve native Ambient Indication and Now Playing surfaces

Date: 2026-08-22
Status: Accepted

## Context

Android SystemUI maintains a dedicated ambient-indication surface with its own Doze tick and tap/lifecycle semantics. Pixel AOD already contains heuristics that avoid treating Now Playing/media-like surfaces as ordinary stock clock content, but there is no explicit ownership contract. Re-rendering native ambient indication would duplicate interaction and lifecycle state, while suppressing it would remove a platform capability.

## Decision

Use **native ambient indication pass-through**.

1. Identify stable OPlus/SystemUI Ambient Indication or Now Playing surfaces when they exist on a supported build.
2. Preserve those native surfaces instead of suppressing or cloning them.
3. Leave tick, tap, lifecycle, content validity, and interaction ownership with OPlus/SystemUI.
4. Use the contextual target arbiter to suppress equivalent module-owned contextual/media output when it would duplicate the preserved native indication.
5. Keep pass-through capability-gated and narrowly bound so preserving ambient indication cannot accidentally restore unrelated stock clock views.

## Consequences

- Native Now Playing/Ambient Indication behavior remains functional alongside the Pixel/COUI clock scene.
- The project gains an explicit contract for a surface that was previously handled only by heuristics.
- Tests must verify both preservation of the intended native surface and continued suppression of unwanted stock AOD content.

## Rejected alternatives

- Re-render native Now Playing data in Pixel AOD: duplicates vendor interaction and lifecycle semantics.
- Ignore or suppress the native surface: removes functionality Android/SystemUI already owns.
