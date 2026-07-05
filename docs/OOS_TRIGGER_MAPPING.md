# OOS Trigger Mapping

Last updated: 2026-07-05

This document records how the module currently translates observed OOS AOD /
Doze trigger events into Pixel AOD behavior. OOS remains the lifecycle owner;
the module only maps native events into display decisions.

## Rules

| Rule | Native event | Category | Module action |
|---|---|---|---|
| `pickup-brief` | pickup / raise / lift keywords | `display-wake` | Candidate to start a short Pixel AOD window. |
| `tap-brief` | tap / touch / gesture keywords | `display-wake` | Candidate to start a short Pixel AOD window. |
| `pocket-hide` | pocket keyword | `sensor-guard` | Blocks or cancels brief and continuous AOD display. |
| `proximity-near-hide` | proximity near / `Boolean(true)` | `sensor-guard` | Hides or blocks AOD while covered. |
| `proximity-far-release` | proximity far / `Boolean(false)` | `sensor-guard` | Releases the sensor guard for future AOD display. |
| `proximity-observe` | proximity event without near/far result | `sensor-guard` | Diagnostic only until the result is known. |
| `sensor-diagnostic` | generic sensor event | `diagnostic-only` | Logs only; does not start display. |
| `unknown-diagnostic` | unclassified trigger | `diagnostic-only` | Logs only; does not start display. |

## Priority

1. Module master switch and power policy decide whether Pixel AOD can display at all.
2. Proximity / pocket guard can block display even when schedule or trigger mode allows it.
3. `Continuous + Trigger` displays continuously only inside the continuous schedule.
4. Outside the continuous schedule, `Continuous + Trigger` may still show briefly when a native display-wake rule is active.
5. `Trigger-only` never becomes a continuous session; it only displays during a short trigger window.
6. Generic OOS native `DOZE` short-wake can still act as an implicit trigger when logs do not expose a clean pickup / tap method name.

## Logging Contract

Every trigger mapping log should include:

- `event`: normalized event type, for example `trigger-proximity`.
- `rule`: exact mapping rule, for example `proximity-near-hide`.
- `category`: `display-wake`, `sensor-guard`, or `diagnostic-only`.
- `displayMode`: mapped display mode such as `trigger-only-brief-display`.
- `futureAction`: the intended module action.
- `behaviorApplied`: whether the module actually applied that action.
- `trace`: current AOD lifecycle trace id.

These fields are intentionally stable so logs from `logcat` and
`/data/adb/lspd/log/modules_*.log` can be compared across builds.
