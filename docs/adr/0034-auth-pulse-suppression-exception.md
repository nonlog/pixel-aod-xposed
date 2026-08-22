# ADR 0034: Allow vendor-authorized authentication pulses through base-AOD suppression

Date: 2026-08-22
Status: Accepted

## Context

Android 17 keeps selected screen-off fingerprint/authentication pulse behavior available even when ordinary always-on display is suppressed by power policy. ADR 0017 already makes biometric pulse presentation vendor-owned, while ADR 0032 distinguishes suppression by capability. A coarse base-AOD gate must therefore not suppress a vendor-authorized authentication path.

## Decision

Use an **authentication-pulse suppression exception**.

1. When OPlus/SystemUI explicitly authorizes an UDFPS/auth pulse, preserve the vendor biometric/authentication surface even if base AOD is currently suppressed.
2. Keep ordinary Pixel clock, notification, media, and contextual presentation suppressed unless their own typed capabilities are separately allowed.
3. Do not treat an auth pulse as permission to re-enter or keep alive continuous AOD.
4. Do not create a module-owned biometric pulse lifecycle for suppressed states.
5. Clear the exception when the vendor auth pulse ends or a terminal Doze gate is asserted.

## Consequences

- Screen-off authentication can remain functional under battery/AOD suppression.
- The exception is narrowly scoped and cannot restore unrelated Pixel AOD content.
- Q17 selective pulse behavior and Q32 typed suppression form one consistent policy.

## Rejected alternatives

- Suppress biometrics whenever base AOD is suppressed: can break valid vendor authentication behavior.
- Build a module-specific suppressed-state UDFPS pulse: duplicates vendor biometric and HBM ownership.
