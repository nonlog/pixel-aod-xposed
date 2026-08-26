# ADR 0066: Re-skin the vendor incoming-notification AOD surface as Pixel Peek

Date: 2026-08-26
Status: Accepted

## Context

ADR 0021 assumed that a reliable OPlus/SystemUI full-pulse notification or pulsing-HUN foreground layer would exist and therefore required Pixel AOD to preserve that native presentation. S25 device investigation on CPH2573/OOS 16.0.9 found a different architecture:

- the user-facing **Show new notifications on AOD** setting is owned by the OPlus AOD stack;
- the current ROM does not enter the AOSP `mPulsing=true` / pulsing-HUN path for this feature;
- OPlus instead creates `com.oplus.systemui.aod.surface.OplusAodCurvedDisplayView`, performs its own notification admission/privacy processing, attaches that surface for the transient notification window, and removes it when the vendor-owned presentation ends.

The product requirement is a Pixel-style Peek notification rather than OPlus curved/edge presentation. Rebuilding admission, privacy, Doze timing, notification interaction or wake ownership would still create an unsafe second notification lifecycle.

## Decision

Use the existing OPlus incoming-notification AOD surface as the **lifecycle and privacy authority**, while Pixel owns only the non-interactive visual card drawn during that exact vendor window.

1. Respect the selected user's native **Show new notifications on AOD** preference. If OPlus disables the feature, Pixel Peek is disabled.
2. Consume only the app/title/message content after OPlus has populated its privacy-processed incoming-notification paint state. Do not reconstruct lockscreen privacy or notification eligibility.
3. Keep `OplusAodCurvedDisplayView` attached and let its existing animator/end/removal callbacks own presentation lifetime. Do not start, extend or terminate a module timer/Doze pulse.
4. Suppress only the vendor surface's own curved/full-screen drawing after safe Pixel content has been captured; otherwise fail open to native presentation.
5. Draw a non-clickable/non-focusable Pixel Peek card on the existing primary Pixel presentation host. Do not implement click, expand, reply, dismissal or gesture ownership.
6. Use the notification's own monochrome `Notification.smallIcon`, not the application/launcher icon.
7. Resolve card placement from actual ambient foreground bounds and keep it below the clock/date/weather/contextual/media/notification cluster. Do not guess collision from app identity or notification text.
8. Use a dark wallpaper-derived Material You neutral surface with a restrained Primary tint and an AOD brightness cap so the card is visibly separated from black without becoming a high-OPR colored panel.

## Relationship to earlier ADRs

- This ADR **refines ADR 0021** for the current-ROM capability shape. ADR 0021 remains the preferred path if a future ROM exposes a trustworthy interactive native pulsing-HUN foreground.
- ADR 0038 collision avoidance still applies; on the current implementation the occupied bounds are the Pixel Peek card and existing Pixel ambient content rather than a native HUN card.
- ADR 0044 remains applicable when a vendor-authorized transient notification scene occurs while continuous AOD is otherwise unavailable: Pixel background presentation must remain fully-dozed and must not manufacture continuous AOD enablement.
- ADR 0056 remains authoritative for transient lifetime: vendor scene entry/exit owns the window.

## Consequences

- Pixel-style Peek can ship on the current OPlus notification-AOD architecture without importing AOSP's pulse state machine.
- OPlus retains notification admission, selected-user/privacy processing and presentation lifetime.
- Pixel owns only a deliberately non-interactive presentation layer, keeping the interaction boundary narrow and testable.
- A future ROM with a real stable native HUN/pulse foreground may choose the older ADR 0021 preservation path instead of this re-skin path.
