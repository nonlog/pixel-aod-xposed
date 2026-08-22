# ADR 0016: Consume vendor Doze transition progress when reliable

Date: 2026-08-22
Status: Accepted

## Context

Modern AOSP/SystemUI exposes continuous Doze transition progress to presentation consumers. Pixel AOD currently derives its LS-to-AOD and AOD-to-LS behavior mainly from discrete OPlus lifecycle observations plus module animations. A trustworthy vendor progress value could improve transition parity, but manufacturing an independent timer would create a second transition clock that can drift from the real display lifecycle.

## Decision

Add a **vendor Doze transition progress adapter** for M9-capable devices.

1. Prefer a stable OPlus/SystemUI continuous Doze progress signal when one can be identified and validated.
2. Use progress only for Pixel AOD presentation properties such as alpha, color, typography weight, and geometry handoff.
3. Keep OPlus authoritative for panel power, ambient brightness, Doze state, and wakefulness.
4. When no trustworthy continuous signal exists, degrade to real vendor lifecycle endpoints rather than synthesizing progress with a module timer.
5. Treat the adapter as capability-gated and device/SystemUI-version-sensitive.

## Consequences

- Supported devices can follow the vendor transition more precisely without taking over lifecycle ownership.
- Unsupported devices retain deterministic endpoint behavior instead of receiving guessed interpolation.
- Transition parity can be tested independently from panel-power behavior.

## Rejected alternatives

- Keep only discrete states everywhere: simpler, but leaves a visible parity gap where reliable vendor progress exists.
- Generate a module-owned 0-to-1 timer: creates a second transition authority and can visibly desynchronize from OPlus.
