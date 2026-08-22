# ADR 0025: Consume vendor power indication semantics when available

Date: 2026-08-22
Status: Accepted

## Context

Android lockscreen/AOD power indication can represent more than a binary charging state: charged, charging speed, charging restrictions, charging source, battery percentage, and estimated remaining time may all be derived by SystemUI from system battery services. Pixel AOD currently owns only a simpler presentation policy and should not create a competing hidden battery-estimation stack.

## Decision

Add a read-only **vendor power indication adapter**.

1. Prefer stable OPlus/SystemUI charging semantics or power-indication output when available.
2. Map supported vendor semantics into Pixel AOD's existing bottom battery/status row rather than rendering the vendor row directly.
3. Preserve distinctions such as charged, fast/slow/restricted charging, source type, and remaining time only when the vendor already provides trustworthy data.
4. Fall back to the current `CouiBatteryStatusPolicy` when richer vendor semantics are unavailable.
5. Do not reverse engineer private BatteryStats estimators or maintain a module-owned charging-time prediction model.

## Consequences

- Pixel AOD can match richer native charging information without taking over battery-state computation.
- Unsupported builds retain the tested simple Charging/Charged behavior.
- Presentation tests can validate semantic mapping separately from vendor data acquisition.

## Rejected alternatives

- Build module-owned remaining-time and charging-speed estimation: duplicates platform computation and can disagree with SystemUI.
- Keep only Charging/Charged forever: leaves visible parity improvements unused even when stable vendor semantics are available.
