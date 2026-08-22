# ADR 0021: Hand native full-pulse notification presentation to OPlus

Date: 2026-08-22
Status: Accepted

## Context

Android SystemUI treats a full notification pulse as more than an AOD icon update: a pulsing notification or heads-up presentation may become visible while the device remains in Doze. Pixel AOD already delegates wake, touch, Keyguard, and biometric ownership to OPlus. Rebuilding an interactive pulse card inside the module would create a competing notification-interaction owner.

## Decision

Use a **native full-pulse notification presentation handoff** when a reliable OPlus/SystemUI pulse layer exists.

1. Preserve the vendor full-pulse notification/HUN presentation layer instead of suppressing or cloning it.
2. Keep Pixel AOD's clock/AOD scene as background presentation while the native pulse foreground is visible.
3. Exempt the identified native pulse layer from stock-view suppression only through a narrow, validated binding.
4. Do not duplicate notification click, expansion, reply, dismissal, or gesture behavior in Pixel AOD.
5. Fall back to the existing icon/content behavior when no stable native full-pulse presentation surface can be identified.

## Consequences

- Full notification pulses can retain native interaction and lifecycle semantics.
- Pixel AOD avoids creating a second HUN/notification owner.
- Stock-suppression tests must verify that the native pulse surface is preserved without restoring unwanted stock clock content.

## Rejected alternatives

- Implement a Pixel pulse notification card: duplicates interactive SystemUI behavior and ownership.
- Keep every pulse icon-only: leaves a functional parity gap where the vendor already supports a full pulse foreground.
