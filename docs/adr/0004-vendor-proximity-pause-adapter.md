# ADR 0004: Vendor proximity pause adapter

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already consumes OPlus proximity/pocket signals and blocks presentation while the device is covered. The current behavior is edge-oriented: a `NEAR` signal can hide presentation immediately and `FAR` can release it immediately. AOSP Doze distinguishes a pausing phase before a fully paused AOD state so brief sensor occlusions do not create visible flicker.

ADR 0001 keeps OPlus authoritative for sensor registration and panel/doze power state. The desired parity is therefore the user-visible pause semantics, not a duplicate `DozePauser` power owner.

## Decision

Implement a **vendor proximity pause adapter** at the Pixel AOD presentation layer.

1. Consume the existing OPlus proximity/pocket signal; do not register a second proximity sensor.
2. On `NEAR`, enter a presentation-level `PAUSING` state and start a dwell timer.
3. If `FAR` arrives before the dwell expires, cancel pausing without hiding the Pixel AOD surface.
4. If `NEAR` remains beyond the dwell, enter `PAUSED` and hide/suspend Pixel AOD presentation.
5. On `FAR` after `PAUSED`, resume presentation using the current vendor lifecycle state rather than forcing panel power state.
6. Reuse the same normalized proximity gate for notification pulse eligibility.

## Consequences

- Short hand/pocket sensor edges should no longer cause immediate AOD flicker.
- The adapter needs deterministic dwell timing, cancellation and lifecycle-reset behavior.
- A vendor transition that already turns the panel/doze surface off remains authoritative; the module must not fight that decision.
- Logging should distinguish raw OPlus proximity state from normalized `PAUSING` / `PAUSED` / resumed presentation state.

## Rejected alternatives

- Keep immediate hide/show for every raw edge: simpler but leaves an AOSP/Pixel presentation-semantic gap and can visibly flicker.
- Reimplement AOSP `DozePauser` including sensor/panel ownership: conflicts with ADR 0001 and duplicates vendor lifecycle responsibilities.
