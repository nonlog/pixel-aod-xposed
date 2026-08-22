# ADR 0042: Consume native clock theme input for lockscreen presentation

Date: 2026-08-22
Status: Accepted

## Context

Android 17 clock presentation can use a clock-specific seed/color and region-darkness/theme state instead of treating the global wallpaper Monet palette as the only color source. Pixel AOD currently resolves colors mainly from system accent resources and wallpaper/theme data.

## Decision

Add a read-only **native clock theme adapter**.

1. Prefer a stable selected-user OPlus/SystemUI clock seed/color when one is available.
2. Consume reliable native region-darkness/theme information for lockscreen contrast and theme selection when available.
3. Use the current Monet-based behavior as fallback when the native clock-theme seam is unavailable.
4. Apply these inputs to lockscreen Pixel/COUI presentation only; fully dozed AOD color follows ADR 0041.
5. Do not create a second module clock-color picker or write native clock/theme settings.

## Consequences

- The Pixel/COUI lockscreen can follow the user's system clock-theme choice more faithfully.
- AOD palette choice remains independent and deterministic.
- Theme fallbacks remain available on OPlus builds without a stable native clock-theme API.

## Rejected alternatives

- Treat global wallpaper Monet as the only clock color source: can diverge from an explicit native clock choice.
- Add a module-owned clock color setting: creates competing customization policy.
