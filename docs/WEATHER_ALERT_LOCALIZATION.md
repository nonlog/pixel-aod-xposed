# Weather Alert Localization

## Current policy

Weather-alert presentation must remain deterministic and must never block the SystemUI/AOD render path.

The module keeps the original Breezy Weather alert headline unchanged for identity, deduplication, cooldown, persistence, and repeat-policy decisions. English localization happens only in the presentation layer.

For recognized Chinese alert headlines, `WeatherAlertDisplayFormatter` combines:

- Breezy's structured severity rank (`1=MINOR`, `2=MODERATE`, `3=SEVERE`, `4=EXTREME`), rendered as Blue / Yellow / Orange / Red;
- an ordered local dictionary of Chinese hazard terms;
- the fixed template `{Color} alert for {hazard}`.

Example:

- `中原发布暴雨蓝色预警` -> `Blue alert for rainstorms`
- `中原发布暴雨红色预警` -> `Red alert for rainstorms`

If the source headline is already non-Chinese, it is preserved. If a Chinese hazard is not recognized, the original source headline is preserved rather than guessed.

## Severity visuals

The contextual warning icon is also derived from the structured severity rank rather than reparsing the translated text:

- UNKNOWN / MINOR: outlined warning triangle;
- MODERATE: warning diamond;
- SEVERE: warning shield;
- EXTREME: warning octagon.

AOD keeps the existing monochrome Material tint; severity is communicated by silhouette/weight rather than introducing colored pixels into the AOD scene.

## Deferred fallback: ML Kit on-device translation

A future optional fallback may use Google ML Kit on-device translation only when the deterministic Chinese hazard dictionary does not recognize the warning type.

If implemented, the fallback must obey these constraints:

1. Never perform translation or model download synchronously on the SystemUI/AOD drawing path.
2. Run translation in the module/relay process and cache the result by normalized original headline.
3. Keep the original Breezy headline as the canonical identity and persistence key.
4. Treat model absence, download failure, timeout, or translation failure as non-fatal and display the original source headline.
5. Do not use an LLM or remote translation API for the normal alert path.
6. Do not allow ML output to change severity; severity always comes from Breezy's structured alert field.

This fallback is intentionally not included in the current implementation because deterministic local formatting covers common Chinese meteorological warning types with stable wording and zero network/model dependency.
