# ADR 0059: Scope all module ambient work to a vendor ambient session epoch

Date: 2026-08-22
Status: Accepted

## Context

The project already has several local generation/trace guards for clock transitions and panel handoff, but asynchronous weather, media, notification, contextual, animation, and delayed callback paths do not share one authoritative lifetime. ADR 0035 requires vendor terminal conditions to tear Pixel presentation down immediately and forbids later reassertion against a finished Doze session.

## Decision

Introduce a module-side **vendor ambient session epoch**.

1. Begin a new monotonically increasing epoch when a new valid OPlus/SystemUI ambient session becomes eligible for Pixel presentation.
2. Immediately invalidate the current epoch on native FINISH/terminal state, selected-user switch, authoritative SystemUI host teardown, or equivalent vendor session destruction.
3. Tag asynchronous adapter callbacks, delayed tasks, content refreshes, animation completion callbacks, and presentation work with the epoch that created them.
4. Discard any work whose epoch is no longer current; stale work must never show, attach, or reassert the Pixel surface.
5. Existing local transition generations may remain for finer cancellation, but they are subordinate to the ambient-session epoch.
6. The epoch is an invalidation mechanism only and never starts, extends, or terminates the vendor Doze lifecycle.

## Consequences

- M9 gains one consistent stale-callback boundary across adapters and renderers.
- Terminal vendor state becomes robust against delayed media/weather/animation work.
- Existing trace IDs can remain diagnostic without being treated as lifecycle authority.

## Rejected alternatives

- Keep independent component generations only: leaves cross-component stale reassertion possible.
- Re-query screen state only when a callback runs: cannot reliably distinguish a new session from stale work targeting a previous one.
