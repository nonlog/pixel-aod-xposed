# ADR 0001: Vendor-delegated Doze lifecycle

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD for OPlus is comparing its feature set with AOSP Android 17 Doze/AOD. AOSP SystemUI owns a full low-power state machine with pulse, ambient brightness, sensors, suppressors, dock and wallpaper integration. Reimplementing those responsibilities inside this Xposed module would create a second lifecycle owner beside OPlus SystemUI and would reintroduce the dual-owner risk that M8 deliberately removed from the clock architecture.

## Decision

Target **AOSP/Pixel presentation parity with vendor-delegated Doze lifecycle**.

Pixel AOD owns presentation and semantic normalization: clock scenes, LS↔AOD visual handoff, notification/media/contextual content, weather, burn-in geometry, and module-specific visual effects.

OPlus remains authoritative for panel/doze power state, ambient brightness, low-power sensor registration, native pulse timing, wallpaper ambient state, and primary FOD/HBM lifecycle. Pixel AOD may observe, classify and normalize those vendor signals, but should not run a parallel DozeMachine-equivalent state machine.

## Consequences

- AOSP Android 17 is used as a behavioral/presentation reference rather than a checklist to clone every `DozeMachine.Part`.
- Feature gaps must be classified as presentation gaps or vendor-lifecycle gaps before implementation.
- Prefer vendor adapters and normalization over independent brightness/sensor/power controllers.
- Notification pulse parity may adapt to vendor pulse signals, but panel wake/doze ownership remains with OPlus.
- Stable UDFPS ownership remains OPlus system primary glyph/HBM plus Pixel AOD's independent optional success ripple.

## Rejected alternative

Implement a full AOSP-style Doze lifecycle inside Pixel AOD, including its own pulse, brightness/scrim, proximity pausing, wake sensors, suppressors, docking and wallpaper state. This would duplicate vendor ownership and materially increase power, panel and SystemUI regression risk.
