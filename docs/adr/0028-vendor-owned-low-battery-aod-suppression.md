# ADR 0028: Remove the module-owned fixed low-battery AOD cutoff

Date: 2026-08-22
Status: Accepted

## Context

The current runtime contains a module-defined 15 percent low-battery threshold that hides Pixel AOD while the device is not charging. Android/AOSP delegates always-on power-saving decisions to system battery/AOD policy rather than defining a universal fixed percentage cutoff. ADR 0001 and ADR 0005 already establish OPlus/SystemUI as the lifecycle and ambient-suppression authority.

## Decision

Use **vendor-owned low-battery AOD suppression**.

1. Remove the universal module-owned 15 percent hard gate from M9 runtime behavior.
2. Accept low-battery AOD suppression only from a validated OPlus/SystemUI ambient/AOD power-policy signal through the unified vendor suppressor adapter.
3. When no such vendor suppression is active, raw battery percentage alone must not hide Pixel AOD.
4. Charging-state presentation remains independent from suppression and continues through the power-indication path.
5. Do not replace the fixed threshold with a user-configurable duplicate battery-saver policy.

## Consequences

- Power-policy ownership becomes consistent with the vendor-delegated Doze architecture.
- Pixel AOD no longer disappears at an arbitrary module-specific percentage when the system still permits AOD.
- Regression tests must distinguish vendor low-power suppression from ordinary low battery percentage.

## Rejected alternatives

- Keep the fixed 15 percent cutoff: preserves a second power-policy owner that can disagree with SystemUI.
- Make the threshold configurable: increases policy divergence instead of removing it.
