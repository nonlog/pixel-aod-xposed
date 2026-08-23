# ADR 0017: Respect vendor selective biometric pulse presentation

Date: 2026-08-22
Status: Accepted

## Context

Android 17 distinguishes full AOD pulses from pulsing-without-UI, biometric authentication UI pulses, and bright pulses. Pixel AOD already delegates primary UDFPS glyph, pressed carrier, HBM/local-HBM, and panel ownership to OPlus. Treating every vendor pulse as a full Pixel AOD scene would conflict with that ownership and can expose content when the system intentionally requested a restricted pulse.

## Decision

Add a capability-gated **selective biometric pulse adapter**.

1. Observe stable OPlus/SystemUI pulse classifications equivalent to no-UI, auth-UI, full, and bright pulse states when available.
2. Suppress module clock/content during no-UI pulses.
3. Yield ordinary module clock/content during auth-UI-only pulses so vendor biometric/auth presentation remains authoritative.
4. Allow normal Pixel AOD presentation for a full pulse when other privacy, suppression, proximity, and power gates pass.
5. Treat bright-pulse classification as presentation metadata only; brightness, HBM/local-HBM, and panel state remain vendor-owned.
6. Fall back to existing vendor lifecycle behavior when no reliable selective-pulse signal exists.

## Current OOS implementation note (S17)

The exact CPH2573/OOS runtime does not route its active AOD path through the AOSP `DozeMachine` instance even though those classes are present in SystemUI. S17 therefore uses the OPlus biometric authority that is both present in the exact binary and observed on the physical device: `OplusBiometricAuthController#showUdfpsOverlay(int)` plus its matching hide/auth-success boundaries.

Exact current-ROM semantics are:

- `showUdfpsOverlay(8)` is the hardware fingerprint **TouchDown** edge. OPlus immediately calls `OnScreenFingerprintUiMech.onFpTouch(true)` and `DreamPolicy.onFpTouchDown()`; physical runtime logs confirm `SensorOverlays ... reason=8`, `touchEvent isDown true`, fingerprint capture, and authentication on this device.
- `showUdfpsOverlay(9)` / `(10)` are native TouchUp edges and return Pixel presentation to the ordinary vendor lifecycle.
- `hideUdfpsOverlay()` and a successful `setFingerprintAuthenticated(true)` are terminal recovery boundaries so a rapid successful unlock cannot leave stale auth-only presentation state behind.
- ordinary icon-show reasons `0..6` are not auth pulses and clear any stale touch-only state at the start of a new fingerprint-display session.
- unsupported reasons neither invent a restricted pulse nor prematurely clear an already-active hardware TouchDown.

While the TouchDown authority is active, S17 treats the vendor surface as **auth-UI-only** and yields ordinary Pixel clock/content. The current ROM exposes no separately proven no-UI or bright selective-pulse classification through this active OPlus path, so S17 does not fabricate either; normal non-biometric AOD/pulse presentation continues through the existing privacy, proximity, notification, schedule, power, and lifecycle gates.

The adapter does not request authentication, register a sensor, set brightness/HBM, mutate the pressed carrier, or create an authentication timer. OPlus remains the sole owner of fingerprint sensing, optical illumination, HBM/local-HBM, wake locks, and panel state.

## Consequences

- Biometric pulse behavior can match Android 17 semantics without duplicating authentication state.
- Restricted pulses no longer imply full module content visibility.
- Existing OPlus UDFPS ownership remains intact.

## Rejected alternatives

- Render full Pixel AOD for every pulse: ignores system intent and can expose unnecessary content.
- Build a module-owned biometric pulse state machine: duplicates vendor authentication and display lifecycle ownership.
