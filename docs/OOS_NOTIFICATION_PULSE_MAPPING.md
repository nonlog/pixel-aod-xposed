# OOS Notification Pulse Mapping

Last updated: 2026-07-05

This document records how the module currently observes OOS notification events
that may later feed Pixel-style notification pulse behavior. This phase does
not start a custom pulse or change notification visibility.

## Diagnostic Sampler

Use this command to sample native notification-pulse behavior with the module's
built-in public test notification:

```bash
MODE=pulse PULSE_ENTER_AOD_SEC=5 PULSE_WAIT_SEC=20 ./scripts/diagnose_aod_trigger_loop.sh
```

The sampler clears the previous module test notification by default, sends the
device to sleep, waits for AOD to settle, posts
`dev.codex.pixelaod.TEST_NOTIFICATION`, and then captures logcat plus
`/data/adb/lspd/log/modules_*.log`. It reports whether the pulse-post marker,
`pulse-candidate` observation, and `notificationPulseRecent=true` lifecycle
state appeared in the same run window.

Optional environment variables:

- `PULSE_CLEAR_BEFORE=0`: keep any existing module test notification before the sample.
- `PULSE_CLEAR_AFTER=0`: leave the module test notification posted after the sample.
- `PULSE_TITLE="..."` and `PULSE_TEXT="..."`: override the posted notification text.

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
