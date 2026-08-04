# Pixel AOD migration record (OOS 16.0.9)

Updated: 2026-08-04

## Scope

This module started from the OnePlus 12 OxygenOS 16.0.5 implementation.  Its
current visual target is the AOD and lockscreen behavior of the installed COUI
Expressive module on OxygenOS 16.0.9.

## Accepted baseline

- Compact clock, date, weather, notification icons, media line, alert line,
  and battery row were adjusted against COUI screenshots and recordings.
- The lockscreen-to-AOD handoff intentionally does **not** translate the
  custom content.  It retains the clock's weight transition and the large /
  compact clock switch, because a custom translation caused visible jitter.
- Clock digits use fixed glyph cells.  Date and weather use the same approach:
  each character is measured with the 500-weight Google Sans Flex reference
  face, then the animated glyph is centred in that fixed cell.  This preserves
  letter positions while its weight changes.
- Date and weather map the live clock-weight progress on every animation frame
  into a compensated 400--500 range.  They use the same Google Sans Flex
  weighted Typeface as the clock; there is no independent information-weight
  animator that can drift behind it.
- The AOD battery text is 16 dp (previously 13 dp), based on the COUI visual
  reference.

## Relevant implementation points

- `PixelAodClockView.applySharedClockText(...)` builds fixed cells for the
  clock.
- `PixelAodClockView.applySharedInfoText(...)` does the equivalent work for
  date and weather; both the AOD and lockscreen views call it.
- `FixedAdvanceSpan` supplies the stable layout width and centres the glyph
  drawn at the current variable-font weight.
- `AodInfoWeightHandoff.synchronizedInfoWeight(...)` maps the full live clock
  transition into the compensated date/weather range.
- `ClockGlyphMetrics` owns tracking and fixed-cell calculations.
- `PixelAodVisualStyle.Aod.BATTERY_TEXT_DP` is the battery size source of
  truth.

## Deliberately out of scope

Do not alter Doze timing, the OOS panel handoff, or the black-frame behavior
in an attempt to suppress the stock AOD.  Those are separate platform-facing
issues and were explicitly excluded from this visual migration.

## Verification status

`./gradlew.bat :app:testDebugUnitTest :app:assembleDebug` passed.  The Debug
APK was overwrite-installed and SystemUI restarted successfully.  The fixed
date/weather grid and battery-size baseline received visual approval.

The synchronized date/weather weight change, including its optical-size
compensation, still needs device-side visual verification before it is
committed.

## Next step

Install the current build and visually test lockscreen <-> AOD.  The date and
weather should change weight in exactly the same frames as the clock, without
letter-spacing movement.  A successful build alone does not prove that the
animation is correct on device.

## Suggested skills

- `diagnosing-bugs` if a new transition has visible jitter: capture the
  animation state, view position, text bounds, and active font variation before
  changing behavior.
- No special skill is required for the routine Android build and overwrite
  installation.
