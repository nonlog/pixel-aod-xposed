# ADR 0020: Prefer vendor AOD time-tick authority

Date: 2026-08-22
Status: Accepted

## Context

AOSP `DozeUi` schedules minute-level AOD time ticks with low-power lifecycle awareness and wakelock handling. Pixel AOD currently receives normal Android time broadcasts and also hooks OPlus native AOD refresh callbacks. Scheduling a second module-owned exact minute alarm would duplicate wakeups and compete with the vendor low-power display lifecycle.

## Decision

Use **vendor AOD time-tick authority** while dozing.

1. Prefer a stable OPlus native AOD refresh/time-tick callback for minute clock, contextual deadline, and burn-in presentation refresh while the vendor AOD lifecycle is active.
2. Keep `ACTION_TIME_TICK`, time-change, and timezone-change handling for lockscreen/interactive behavior and as a capability fallback when no reliable vendor doze tick is available.
3. Do not add a module-owned exact per-minute `AlarmManager` schedule merely to reproduce AOSP `DozeUi`.
4. Deduplicate refreshes when vendor callbacks and broadcast fallbacks occur near the same boundary.
5. Keep all refresh paths presentation-only; no tick source grants Pixel AOD panel, brightness, or wakefulness ownership.

## Consequences

- Dozing refreshes can align with the vendor's actual low-power lifecycle without adding redundant device wakeups.
- Existing broadcast handling remains useful outside doze and on unsupported vendor builds.
- Missed/duplicate-tick diagnostics can be added without creating a second power-state machine.

## Rejected alternatives

- Always rely on `ACTION_TIME_TICK`: delivery is not the authoritative low-power AOD scheduler when a stable vendor callback exists.
- Schedule module exact alarms every minute: duplicates vendor work and increases idle-power risk.
