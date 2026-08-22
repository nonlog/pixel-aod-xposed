# ADR 0013: Read-only Android 17 Live Update adapter

Date: 2026-08-22
Status: Accepted

## Context

Android promoted ongoing notifications and Android 17 MetricStyle expand the set of long-running activities that can be surfaced prominently by SystemUI. Pixel AOD currently reduces most of these to ordinary notification/icon presentation.

ADR 0008 makes the dozing surface presentation-only, and ADR 0006 imposes a low-power visual budget.

## Decision

Implement a **read-only Live Update adapter**.

1. Recognize only notifications that the system/vendor already classifies as promoted/live ongoing or exposes through a stable equivalent signal.
2. Read standard structured notification semantics when available, including progress, metric, call, timer/stopwatch, fitness, and travel-like data.
3. Map supported Live Updates into a compact low-power AOD contextual presentation rather than a full notification template.
4. Apply unified privacy/user scope, suppression, proximity, schedule/power gates, expiry/staleness handling, and the low-power visual budget.
5. Do not expose direct actions from AOD; interaction remains deferred to normal Keyguard.
6. Unsupported or unrecognized promoted styles fall back to the ordinary notification representation instead of disappearing.

## Consequences

- Android 17 ongoing activity semantics become visible on AOD without importing the complete notification renderer.
- The feature reuses existing contextual presentation and gate infrastructure.
- Style support can grow incrementally while preserving a safe fallback.

## Rejected alternatives

- Keep all promoted ongoing notifications as ordinary icons: leaves a material Android 17 presentation gap.
- Render full system notification templates on AOD: too dense for the low-power surface and expands interaction/security scope.
