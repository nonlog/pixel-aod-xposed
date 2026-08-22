# ADR 0024: Prefer native AOD notification eligibility when available

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD currently reconstructs AOD notification eligibility from active notifications, ranking, and OPlus lockscreen decisions, then renders its own icon row. Android/SystemUI maintains additional evolving notification filters for ambient presentation such as dismissal, reply, media, pulse, and policy state. Duplicating every internal rule permanently would be brittle, while reusing the vendor's final visuals would give up Pixel/COUI presentation ownership.

## Decision

Add a read-only **native AOD notification eligibility adapter**.

1. When OPlus/SystemUI exposes a stable final AOD notification/icon eligibility result, use it as the authoritative answer for which notifications may be presented.
2. Keep Pixel AOD responsible for icon-row layout, artwork treatment, overflow, animation, and COUI visual integration.
3. Keep the existing NotificationListenerService-based eligibility pipeline as a fallback when no trustworthy native result exists.
4. Preserve Q22 DND ambient suppression and Q14 user/privacy boundaries even when native eligibility is consumed.
5. Treat the adapter as version-sensitive and fail closed to the tested fallback rather than guessing private internals.

## Consequences

- SystemUI decides eligibility while Pixel AOD owns presentation.
- The module no longer needs to mirror every changing internal ambient-notification filter on supported builds.
- Eligibility and rendering can be tested independently.

## Rejected alternatives

- Copy every AOSP/OPlus notification filter into module code: high maintenance cost and easy semantic drift.
- Reuse the full native AOD icon container: gives up Pixel/COUI presentation ownership and couples layout to vendor internals.
