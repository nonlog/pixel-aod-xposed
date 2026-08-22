# ADR 0037: Respect the selected user's native clock-size preference as a ceiling

Date: 2026-08-22
Status: Accepted

## Context

Android 17 distinguishes a user-selected SMALL clock-size setting from DYNAMIC sizing. SMALL prevents the large clock from appearing, while DYNAMIC allows SystemUI to choose based on current content and state. Pixel AOD already has one Pixel/COUI clock face with large and compact presentations, and ADR 0019 intentionally keeps multi-clock registry/theme-engine scope out of M9.

## Decision

Use the **native clock-size preference ceiling**.

1. Read the selected user's stable OPlus/SystemUI SMALL/DYNAMIC clock-size preference when available.
2. Native SMALL is a hard ceiling: Pixel/COUI lockscreen and AOD presentation must not return to the large face while it is selected.
3. Native DYNAMIC permits the existing content-aware large/compact policy to choose presentation size.
4. Do not add a second module clock-size preference or write the native setting.
5. User changes invalidate cached presentation size immediately under the selected-user scope from ADR 0014.

## Consequences

- Pixel AOD respects an explicit user clock-size preference without becoming a clock registry.
- Existing notification/media/contextual sizing logic remains useful in DYNAMIC mode.
- Size handoff tests need SMALL and DYNAMIC coverage across LS and AOD.

## Rejected alternatives

- Ignore native clock-size preference: contradicts an explicit SystemUI user choice.
- Add an independent module Large/Small preference: creates conflicting settings and unnecessary policy ownership.
