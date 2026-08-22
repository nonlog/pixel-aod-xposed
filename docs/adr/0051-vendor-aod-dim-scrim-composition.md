# ADR 0051: Compose Pixel ambient content with the vendor AOD dim/scrim

Date: 2026-08-22
Status: Accepted

## Context

ADR 0001 keeps panel brightness and ambient display power state under OPlus/SystemUI ownership. That boundary is incomplete if a module-owned overlay can render above the native AOD dimming scrim and therefore appear brighter than equivalent native ambient content. Android/SystemUI computes ambient dim/scrim state separately from the raw panel-brightness value.

## Decision

Add a read-only **vendor AOD dim/scrim composition adapter**.

1. Prefer placing the Pixel ambient host in the same composition path as native AOD content so the vendor dim/scrim naturally affects it.
2. If host placement cannot inherit the native scrim, consume a stable OPlus/SystemUI-computed AOD dim amount and apply it as a presentation-only multiplier.
3. Never derive a second brightness/dim curve from module light-sensor readings.
4. Do not recolor or independently dim vendor-owned UDFPS, biometric, HBM/local-HBM, pulse, or other native foreground surfaces.
5. Reevaluate the composition input when the native Doze scene, dim amount, or terminal gate changes.

## Consequences

- Pixel-owned ambient content no longer bypasses native low-light dimming simply because it is an overlay.
- Brightness and ambient-light policy remain entirely vendor-owned.
- Validation must compare native and Pixel-owned ambient luminance behavior across representative dim/scrim states.

## Rejected alternatives

- Respect only panel brightness: an overlay above the scrim can still be visually too bright.
- Add a module light-sensor dimmer: duplicates power/display policy and conflicts with ADR 0001.
