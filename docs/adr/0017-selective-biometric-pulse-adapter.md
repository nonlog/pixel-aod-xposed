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

## Consequences

- Biometric pulse behavior can match Android 17 semantics without duplicating authentication state.
- Restricted pulses no longer imply full module content visibility.
- Existing OPlus UDFPS ownership remains intact.

## Rejected alternatives

- Render full Pixel AOD for every pulse: ignores system intent and can expose unnecessary content.
- Build a module-owned biometric pulse state machine: duplicates vendor authentication and display lifecycle ownership.
