# ADR 0027: Follow the selected Android user's locale for AOD formatting

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already honors the system 12/24-hour preference, but its date formatting currently distinguishes only Chinese from all other locales. Android/SystemUI presentation should follow the selected user's locale, localized digits, and locale-appropriate date ordering. The module application's own UI-language preference is a separate concern and should not create a second runtime locale for SystemUI surfaces.

## Decision

Adopt **system-locale AOD formatting**.

1. Resolve clock/date formatting from the currently selected Android user's SystemUI context.
2. Respect the system 12/24-hour setting, localized digits, locale date skeleton/order, and relevant calendar formatting exposed by the platform.
3. Keep the module app's Chinese/English preference scoped to the module UI only.
4. Refresh presentation on locale, time-zone, time-format, and user changes through existing lifecycle-safe refresh paths.
5. Avoid hand-maintained per-language date patterns when platform locale formatting can provide the answer.

## Consequences

- AOD/lockscreen date and time behave like native Android across supported locales.
- Module UI language can be changed without unexpectedly changing SystemUI AOD semantics.
- Locale/user-switch tests become part of the M9 presentation matrix.

## Rejected alternatives

- Keep Chinese-versus-other date patterns: visibly incorrect for many locales.
- Force the module app language onto AOD: creates a non-native locale boundary inside SystemUI.
