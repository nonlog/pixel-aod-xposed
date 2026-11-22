# ADR 0073: Pocket guard priority over Power Saving AOD extensions

## Context

Physical CPH2573 testing of 0.1.50 showed that covering the proximity area while charging did not reliably remove AOD. The module already stopped a charging hold when its committed OPlus proximity authority became NEAR, but the failing trace did not emit the corresponding ProximityTask setNear/run commit. SensorService simultaneously showed OPlus SystemUI's existing OplusWakeUpController$amdSensorListener$1 receiving gesture_prox events (type 33171066), with 0.0 as NEAR and 5.0 as FAR.

A new incoming notification could therefore attach its native Peek window while the module still considered committed proximity FAR, allowing the notification brief or charging exception to keep or re-show AOD after the physical pocket authority had already reported NEAR.

## Decision

Observe the vendor-owned gesture_prox listener without registering another sensor. Feed its events only into the existing raw proximity pause adapter. A raw NEAR creates a hard module guard with this priority:

1. pocket/proximity guard
2. incoming-notification temporary AOD
3. charging continuous AOD exception

While the guard is active, Pixel AOD cancels its notification brief, hides its Peek overlay, refuses notification re-show, stops extending the Power Saving update budget, and does not convert a vendor terminal AOD decision back into a charging keep-alive.

## Ownership

OPlus remains authoritative for sensor registration, debounce/dwell, committed proximity state, native notification window, Dream/doze/panel lifecycle, fingerprint optical state, and final screen OFF. The module neither registers a sensor nor directly powers the panel off.
