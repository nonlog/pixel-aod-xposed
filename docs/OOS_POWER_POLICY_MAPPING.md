# OOS Power Policy Mapping

Last updated: 2026-07-05

This document records how the module maps native OOS power and battery state to
Pixel AOD display decisions. The goal is to keep power policy explicit and
observable without changing the current behavior.

## Rules

| Reason | Category | Module action |
|---|---|---|
| `power-save-mode` | `system-power-saver` | Hide or block Pixel AOD while Android power saver is active. |
| `low-battery` | `battery-low` | Hide or block Pixel AOD when battery is low and the device is not charging. |
| `low-battery-while-charging` | `battery-charging` | Allow Pixel AOD because charging overrides low-battery suppression. |
| `charging` | `battery-charging` | Allow Pixel AOD while charging. |
| `battery-unknown` | `diagnostic-only` | Allow Pixel AOD but keep the unknown battery state visible in logs. |
| `power-policy-allowed` | `power-normal` | Allow Pixel AOD when no power policy blocks display. |

## Logging Contract

Every power-policy mapping log should include:

- `reason`: exact decision reason, for example `low-battery`.
- `category`: broad class such as `system-power-saver` or `battery-low`.
- `allowsDisplay`: whether Pixel AOD may render.
- `futureAction`: expected module action.
- `powerSave`: current Android power saver state.
- `threshold`: low-battery suppression threshold.
- `level`: current battery percentage, or `-1` when unknown.
- `battery`: raw battery snapshot fields used by the decision.
- `trace`: current AOD lifecycle trace id.

These fields let diagnostics distinguish "OOS hid AOD because of power policy"
from unrelated native hide callbacks, FOD timeouts, or trigger-window expiry.
