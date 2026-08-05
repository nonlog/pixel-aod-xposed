# Pixel AOD for OPlus

This context defines the user-visible concepts used by the module’s Pixel-like AOD and lock-screen information surfaces.

## Language

**Weather Forecast**:
A temporary At a Glance item containing tomorrow’s weather icon and tomorrow’s highest and lowest temperature.
_Avoid_: Hourly forecast, rain graph, multi-day forecast, current weather

**Weather Alert**:
A time-sensitive At a Glance item representing the highest-priority active warning supplied by the weather source, regardless of severity classification.
_Avoid_: Weather forecast, current weather, permanent warning

**Active Weather Location**:
The single Breezy Weather location currently selected as the source of current weather, forecasts, and alerts.
_Avoid_: All saved locations, merged locations, device position unless Breezy selected it
