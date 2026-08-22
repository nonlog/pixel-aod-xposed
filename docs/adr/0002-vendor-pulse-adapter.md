# ADR 0002: Vendor pulse adapter for notification wake semantics

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already classifies posted notifications into pulse candidates after lockscreen/AOD filtering and records proximity/pocket and power-policy blockers. In 0.1.383 the implementation remains observe-only: it follows OPlus native notification pulse behavior but does not guarantee a brief Pixel-style AOD appearance when OPlus does not pulse.

The accepted architecture in ADR 0001 keeps OPlus authoritative for the Doze/panel lifecycle, so a full custom pulse state machine is out of scope.

## Decision

Implement a **vendor pulse adapter** rather than a custom Doze pulse owner.

For a newly posted notification that survives privacy, notification wake/DND policy, pocket/proximity and power checks:

1. Observe whether OPlus already starts the native pulse/AOD wake path.
2. If it does, follow it and deduplicate module action.
3. If it does not, Pixel AOD may request an existing OPlus AOD/pulse entry point so the presentation receives one brief Pixel-style pulse window.
4. OPlus continues to own panel brightness, sensor registration, native Doze state transitions and pulse termination.

## Consequences

- Trigger-only mode can achieve Pixel-like notification wake semantics without a parallel `DozeMachine`.
- Pulse requests must be idempotent and correlate to the notification/lifecycle trace to avoid duplicate wakeups.
- Notification privacy, ranking/wake preference, DND, proximity/pocket and power policy are mandatory gates.
- If no safe/stable OPlus pulse request entry point exists on a supported build, the adapter must fall back to observe-only rather than synthesize panel power state.

## Rejected alternatives

- Remain permanently observe-only: lowest risk, but leaves a visible Pixel/AOSP parity gap for notification wake behavior.
- Implement custom pulse/panel-wake lifecycle: conflicts with the vendor-delegated Doze ownership decision and increases power/panel regression risk.
