# ADR 0009: Capability-gated vendor dock adapter

Date: 2026-08-22
Status: Accepted

## Context

AOSP defines a docked AOD state, but the default dock manager is intentionally OEM-extensible. On an OPlus device, ordinary charging or orientation is not sufficient evidence that a true dock/stand lifecycle exists.

## Decision

Support docked AOD only through a **capability-gated vendor dock adapter**.

1. Enable docked presentation only when OPlus/SystemUI exposes a stable, semantically reliable dock or charging-stand state.
2. Treat devices without such a signal as not supporting docked AOD.
3. Do not infer dock state from charging, orientation, motion, or combinations of heuristics.
4. Do not implement a module-owned DockManager or panel lifecycle.
5. If a reliable signal exists, map it into presentation differences only; OPlus remains lifecycle owner.

## Consequences

- Docked AOD can be supported on capable ROMs without false positives on normal chargers.
- Feature availability is explicit and device/ROM capability-dependent.
- Absence of dock support is not considered a parity bug when the vendor exposes no dock lifecycle.

## Rejected alternatives

- Ignore docked AOD unconditionally: unnecessarily discards a safe capability if OPlus exposes one.
- Guess dock state from charger/orientation heuristics: creates incorrect UI and hidden lifecycle assumptions.
