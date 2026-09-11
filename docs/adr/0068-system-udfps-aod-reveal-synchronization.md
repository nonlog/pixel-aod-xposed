# ADR 0068: System UDFPS visual synchronization with AOD reveal

Date: 2026-09-11
Status: Accepted

## Context

OPlus renders `OnScreenFingerprintIcon` in a window independent from the AOD clock surface. On CPH2573 the fingerprint window can commit while `AodClockLayout` is still revealing from the black AOD mask. The result is a visible fingerprint glyph several hundred milliseconds before the Pixel AOD content, even though biometric and AOD lifecycle ordering is otherwise correct.

The current stable configuration keeps the primary fingerprint glyph system-owned. Changing sensing, HBM, touch, panel state, or the vendor visibility state would cross the biometric ownership boundary.

## Decision

1. Keep OPlus as the sole owner of fingerprint visibility state, sensing, authentication, touch routing, HBM/local-HBM, wake locks, and panel requests.
2. When the module and Pixel AOD are enabled, the system glyph is selected, the device is non-interactive, and OPlus reports a real AOD show, synchronize only the fingerprint ImageView alpha with the native AOD reveal.
3. Derive reveal progress from the slower visible surface: `AodClockLayout` alpha and, when shown, the inverse alpha of `workshop_aod_anim_mock`. Never use a fixed millisecond delay.
4. Keep the OPlus brightness alpha as the maximum fingerprint alpha. Restore it when the AOD reveal completes.
5. A real fingerprint touch cancels the visual synchronization immediately so authentication feedback is never delayed.
6. Do not apply this adapter when the optional Pixel replacement fingerprint glyph is enabled.
7. The existing Power Saving fingerprint timeout remains authoritative; this adapter does not call `setVisibilityInAOD`, show/hide the window, or alter timeout scheduling.

## Consequences

- The native glyph no longer visually leads the AOD black-mask/setup reveal while the system glyph remains the primary fingerprint presentation.
- The exception narrows ADR 0051 only for transient reveal alpha composition. It does not independently choose a brightness level or change biometric/panel ownership.
- ROM changes to the mask resource or AOD layout fail open: without a valid native AOD reveal surface, the system glyph retains OPlus timing and brightness.
