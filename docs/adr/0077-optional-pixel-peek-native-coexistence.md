# ADR 0077: Optional Pixel Peek and native notification coexistence

Date: 2026-10-07
Status: Accepted

## Context

ADR 0066 made the OPlus incoming-notification AOD surface the lifecycle/privacy authority and
always replaced its drawing with a Pixel-style card when safe content was available.

Two requirements now need separate ownership:

1. Users can keep the stock OPlus Peek visual while retaining Show AOD for new notifications.
2. In an already-continuous AOD mode, native Peek must not make the module AOD disappear.

## Decision

1. Add a live Use Pixel notification Peek preference. It defaults on.
2. Disabling it keeps the OPlus native card and never suppresses native Peek drawing.
3. Show AOD for new notifications remains an independent Power Saving enhancement.
4. Native notification-window attachment is tracked independently from custom-card content.
5. In all-day AOD, or scheduled AOD while inside schedule, attached native Peek preserves the
   module AOD background when OPlus temporarily swaps its ordinary AOD lifecycle surface.
6. Power Saving AOD is never promoted to continuous AOD by this coexistence path.
7. Explicit always-on suppression, module power policy, proximity/pocket, biometric suppression,
   user AOD enablement, and schedule boundaries remain authoritative.
8. The native OPlus Peek view and ancestor containers that own it are excluded from generic
   stock-AOD suppression while the notification surface is attached.

## Consequences

- Pixel Peek on/off is independent from Power Saving full-AOD-on-notification on/off.
- Stock Peek can render over module continuous AOD instead of replacing ambient presentation.
- Missing safe notification content or a missing Pixel host fails open to the native Peek.
- OPlus continues to own notification admission, privacy, timing, attach/detach, and interaction.
