# ADR 0049: Make the Pixel replacement host the Keyguard accessibility semantic owner

Date: 2026-08-22
Status: Accepted

## Context

The current Pixel/COUI host and many child views are explicitly marked `IMPORTANT_FOR_ACCESSIBILITY_NO`, so replacing the stock clock can also remove useful Keyguard semantics. Android/SystemUI clocks expose localized time descriptions and accessibility-aware semantics. Keeping both hidden stock semantics and replacement semantics active would instead create duplicate announcements.

## Decision

Add a **Keyguard accessibility semantic adapter** for the replacement surface.

1. When Pixel/COUI replaces the stock clock, expose one localized semantic time node owned by the replacement host.
2. Group date, weather, contextual content, and power/charging indication into logical semantic units rather than exposing every decorative glyph or digit.
3. Keep purely decorative clock digits, notification glyph decoration, burn-in transforms, and visual transition layers out of the accessibility tree.
4. Ensure hidden/suppressed stock clock semantics are not simultaneously exposed as duplicate content.
5. Respect native SystemUI accessibility eligibility in fully-dozed AOD; do not invent click actions or interactive semantics that ADR 0008 forbids.
6. Refresh localized descriptions on time, locale, selected-user, content, and configuration changes.

## Consequences

- Replacing the native clock no longer silently removes basic Keyguard accessibility information.
- Decorative visual implementation details remain separated from semantic content.
- Accessibility tests need duplicate-node, locale, RTL, and lockscreen/AOD eligibility coverage.

## Rejected alternatives

- Expose only time and ignore all other visible replacement information: leaves meaningful module-owned content semantically absent.
- Keep the entire replacement surface inaccessible: preserves current behavior but breaks parity with native Keyguard semantics.
