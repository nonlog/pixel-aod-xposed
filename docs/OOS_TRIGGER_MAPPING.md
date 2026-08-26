# OOS Trigger Mapping

Last updated: 2026-08-26

This document records how the module currently translates observed OOS AOD /
Doze trigger events into Pixel AOD behavior. OOS remains the lifecycle owner;
the module only maps native events into display decisions.

## Rules

| Rule | Native event | Category | Module action |
|---|---|---|---|
| `pickup-brief` | pickup / raise / lift keywords | `display-wake` | Correlates Pixel transient presentation with the vendor ambient scene; it does not create a module-owned timed window. |
| `tap-brief` | tap / touch / gesture keywords | `display-wake` | Correlates Pixel transient presentation with the vendor ambient scene; it does not create a module-owned timed window. |
| `pocket-hide` | pocket keyword | `sensor-guard` | Blocks Pixel AOD presentation while pocket policy is active. |
| `proximity-near-hide` | proximity near / `Boolean(true)` | `sensor-guard` | Hides or blocks AOD while covered. |
| `proximity-far-release` | proximity far / `Boolean(false)` | `sensor-guard` | Releases the sensor guard for future AOD display. |
| `proximity-observe` | proximity event without near/far result | `sensor-guard` | Diagnostic only until the result is known. |
| `sensor-diagnostic` | generic sensor event | `diagnostic-only` | Logs only; does not start display. |
| `unknown-diagnostic` | unclassified trigger | `diagnostic-only` | Logs only; does not start display. |

## Priority

1. Module master switch, native AOD availability/enablement, typed suppression, and power policy decide whether Pixel AOD can display at all.
2. OPlus all-day / scheduled / energy-saving settings are the only display-option authority. The module has no second display-mode selector or replacement schedule.
3. All-day permits the existing screen-off pre-arm; scheduled permits it only inside the native OPlus window; energy-saving waits for a real vendor transient/ambient scene.
4. Proximity / pocket guard can block presentation even while a vendor scene exists.
5. Pickup/tap/motion observations never own lifetime. Pixel transient presentation may exist only inside the real vendor ambient scene and ends with that scene.
6. Generic OOS native `DOZE` can still serve as lifecycle evidence when logs do not expose a clean pickup / tap method name.

## Logging Contract

Every trigger mapping log should include:

- `event`: normalized event type, for example `trigger-proximity`.
- `rule`: exact mapping rule, for example `proximity-near-hide`.
- `category`: `display-wake`, `sensor-guard`, or `diagnostic-only`.
- `displayMode`: current native OPlus option classification such as `all-day`, `scheduled`, or `energy-saving`.
- `futureAction`: the intended module action.
- `behaviorApplied`: whether the module actually applied that action.
- `trace`: current AOD lifecycle trace id.

These fields are intentionally stable so logs from `logcat` and
`/data/adb/lspd/log/modules_*.log` can be compared across builds.
