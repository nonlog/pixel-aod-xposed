# OOS Notification Pulse Mapping

Last updated: 2026-07-05

This document records how the module currently observes OOS notification events
that may later feed Pixel-style notification pulse behavior. This phase does
not start a custom pulse or change notification visibility.

## Rules

| Rule | Native input | Category | Module action |
|---|---|---|---|
| `posted-pulse-candidate` | Notification posted / received and at least one usable AOD notification remains | `pulse-candidate` | Observe OOS native pulse timing only. |
| `posted-filtered` | Notification posted / received but no usable AOD notification remains | `pulse-filtered` | Do not pulse; keep the filter reason in notification logs. |
| `snapshot-pulse-candidate` | Listener or AOD view snapshot with at least one usable AOD notification | `pulse-candidate` | Observe snapshot timing only. |
| `snapshot-filtered` | Snapshot contains raw notifications but none are AOD-usable | `pulse-filtered` | Do not pulse. |
| `removed-or-cleared` | Notification removed or native view cleared | `pulse-clear` | Observe native clear / removal timing only. |
| `ranking-update` | Ranking map changed | `diagnostic-only` | Refresh pulse inputs and filtering evidence only. |
| `empty-snapshot` | Empty notification snapshot | `diagnostic-only` | Observe empty state only. |

## Logging Contract

Every notification-pulse observation log should include:

- `event`: normalized input class such as `notification-posted`.
- `rule`: exact mapping rule, for example `posted-pulse-candidate`.
- `category`: `pulse-candidate`, `pulse-filtered`, `pulse-clear`, or `diagnostic-only`.
- `futureAction`: what a later custom-pulse implementation might do.
- `raw`: raw notification count.
- `usable`: count after AOD / lockscreen visibility filtering.
- `media`: media candidate count.
- `rankings`: ranking snapshot count, or `-1` when not applicable.
- `packages`: package summary for usable AOD notifications.
- `trace`: current AOD lifecycle trace id.

This keeps notification pulse investigation separate from AOD icon rendering.
