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

| Rule | Native input | Category | Pulse policy | Module action |
|---|---|---|---|---|
| `posted-pulse-candidate` | Notification posted / received and at least one usable AOD notification remains | `pulse-candidate` | `can-trigger-brief-display`, unless sensor / power policy blocks it | Observe OOS native pulse timing only. |
| `posted-filtered` | Notification posted / received but no usable AOD notification remains | `pulse-filtered` | `lockscreen-aod-filtered` | Do not pulse; keep the filter reason in notification logs. |
| `snapshot-pulse-candidate` | Listener or AOD view snapshot with at least one usable AOD notification | `pulse-candidate` | `observe-only` | Observe snapshot timing only. |
| `snapshot-filtered` | Snapshot contains raw notifications but none are AOD-usable | `pulse-filtered` | `lockscreen-aod-filtered` | Do not pulse. |
| `removed-or-cleared` | Notification removed or native view cleared | `pulse-clear` | `observe-only` | Observe native clear / removal timing only. |
| `ranking-update` | Ranking map changed | `diagnostic-only` | `observe-only` | Refresh pulse inputs and filtering evidence only. |
| `empty-snapshot` | Empty notification snapshot | `diagnostic-only` | `observe-only` | Observe empty state only. |

## Pulse Policy

The current adapter only classifies native notification pulse evidence. It does
not start a custom notification pulse.

| Pulse policy | Meaning |
|---|---|
| `can-trigger-brief-display` | A posted notification is usable by lockscreen / AOD filtering and is not blocked by currently known sensor or power policy. |
| `observe-only` | The event is useful diagnostic evidence but is not an explicit display trigger. |
| `lockscreen-aod-filtered` | The notification event did not leave any usable lockscreen / AOD notification after filtering. |
| `sensor-power-blocked` | The event would otherwise be a pulse candidate, but proximity / pocket / power policy says it should not wake AOD. |

## Logging Contract

Every notification-pulse observation log should include:

- `event`: normalized input class such as `notification-posted`.
- `rule`: exact mapping rule, for example `posted-pulse-candidate`.
- `category`: `pulse-candidate`, `pulse-filtered`, `pulse-clear`, or `diagnostic-only`.
- `futureAction`: what a later custom-pulse implementation might do.
- `pulsePolicy`: one of the pulse policy labels above.
- `pulsePolicyReason`: exact reason for that policy result.
- `pulsePolicyAction`: what a later custom-pulse implementation may do with this policy result.
- `pulsePolicyCanTriggerBrief`: whether the event is currently a future brief-display candidate.
- `pulsePolicyBlocked`: whether the event is blocked by lockscreen/AOD, sensor, or power policy.
- `raw`: raw notification count.
- `usable`: count after AOD / lockscreen visibility filtering.
- `media`: media candidate count.
- `rankings`: ranking snapshot count, or `-1` when not applicable.
- `packages`: package summary for usable AOD notifications.
- `trace`: current AOD lifecycle trace id.

This keeps notification pulse investigation separate from AOD icon rendering.
