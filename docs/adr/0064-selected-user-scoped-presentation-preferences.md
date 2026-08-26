# ADR 0064: Scope user-visible Pixel presentation preferences to the selected user

Date: 2026-08-22
Status: Accepted

## Context

Android AOD and Keyguard behavior is evaluated for the currently selected user, and earlier decisions already require user/profile-aware privacy and content filtering. Pixel AOD settings are currently exposed through one module settings store without an explicit selected-user scope, which can cause a second Android user to inherit the first user's visual and contextual presentation choices.

## Decision

Adopt **selected-user scoped presentation preferences**.

1. Resolve user-visible Pixel presentation choices against the current Android selected user.
2. Per-user module preferences include AOD palette, AOD/Lockscreen base clock weights, non-lockscreen transition style, weather/contextual/calendar visibility, and comparable Pixel presentation choices. The native OPlus AOD display mode and schedule remain selected-user scoped system settings and are consumed directly rather than duplicated into module preferences.
3. Module installation/enablement, diagnostics/debug logging, safety release policy, and other administrator/device-wide controls may remain module-wide where appropriate.
4. A selected-user change invalidates cached preference/content state and the current vendor ambient session epoch before rendering the new user's presentation.
5. Work-profile privacy and quiet/locked-profile eligibility remain governed by the existing privacy/profile adapter rather than by copying the parent user's settings into the profile.

## Consequences

- Multiple Android users do not silently share personal Pixel presentation preferences.
- User switches produce a clean content/settings/session boundary.
- Device-wide safety and diagnostics can remain centralized instead of becoming user-overridable.

## Rejected alternatives

- Share all module settings device-wide: leaks presentation/context choices between Android users.
- Scope content only but keep all visual preferences global: still diverges from selected-user native behavior and can surprise secondary users.
