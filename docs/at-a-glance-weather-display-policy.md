# At a Glance Weather Display Policy

Status: Final — approved on 2026-08-05 as the implementation and acceptance contract. Implementation still requires an explicit user request.

## Evidence boundary

- Current weather is a confirmed Pixel At a Glance category.
- Severe weather warnings are a confirmed Pixel At a Glance category.
- Tomorrow's daily high and low are an observed Pixel-like behaviour, not a publicly specified At a Glance contract.
- Google's public material does not define exact weather-card display windows or expiry rules.

## Resolved decisions

### Weather Forecast

- Content: tomorrow's weather icon and tomorrow's highest and lowest temperature.
- Excluded: hourly forecasts, precipitation graphs, multi-day forecasts, and current weather.
- Default eligibility window: 21:00 through 23:30 in the device's local time.
- Users may adjust the local start and end times; the end boundary is exclusive, and a range may cross midnight.
- The forecast is low-priority contextual information and must yield to more important At a Glance information.

### Weather Alert

- Weather Alert is AOD-only. A newly observed active alert becomes pending immediately and receives a 10-minute window when it is first visibly selected on AOD.
- An unchanged minor or moderate alert is displayed only once.
- An unchanged severe or extreme alert enters a two-hour cooldown after its display window.
- After that cooldown, the severe or extreme alert becomes eligible for another 10-minute display on the next AOD entry. It does not wake or refresh the display solely to repeat itself.
- A changed headline or increased severity is treated as a new alert and receives a new immediate 10-minute display window.
- An alert becomes ineligible immediately when its source validity period ends or the weather source removes it.
- Refreshing an unchanged alert does not extend its current display window or cooldown.
- Lock-screen selection and rendering never show an alert, start its 10-minute window, or consume the AOD repeat-entry marker.

### At a Glance Card Selection

- At most one contextual At a Glance card is visible at a time.
- Card priority is: Weather Alert, then upcoming Calendar Event, then Weather Forecast.
- On AOD, Weather Alert has the stated highest priority. On the lock screen, Weather Alert is not eligible, so Calendar Event then Weather Forecast provide the contextual-card choices.
- Current weather remains part of the baseline date-and-weather surface and does not compete for the contextual card slot.
- Notification icons and media information are not At a Glance cards. They remain eligible and move below the selected contextual card without overlapping it.

### Lock-screen and AOD Presentation

- Weather Forecast and Calendar Event are eligible on both the lock screen and AOD and use identical card content, icon geometry, spacing, line count, and truncation on both surfaces.
- Weather Forecast is one line: weather icon followed by a localized equivalent of `Tomorrow 31° / 25°`.
- Weather Alert is AOD-only and, when selected there, is one line: warning icon followed by the alert headline. The lock screen never selects or renders it.
- Each contextual card is limited to one line and truncates at the end when necessary.
- Alert descriptions, validity periods, issuing organizations, hourly details, and multi-line expansions are excluded.
- A lock-screen/AOD transition may change color and font weight, but must not change text, icon size, spacing, wrapping, or measured character positions for the card eligible on the receiving surface. The transition must not cause lock-screen rendering to select an alert or consume its AOD history.

### Forecast Freshness

- Forecast data is eligible only when its forecast date equals the device-local calendar date plus one day.
- The weather source's most recent successful update must be no more than six hours old.
- The forecast requires a usable weather icon and both highest and lowest temperatures.
- Missing, incomplete, mismatched-date, or stale forecast data produces no card. The module does not show placeholders or fall back to an older forecast.

### Alert-source Freshness

- A successful source response that removes or ends an alert makes it ineligible immediately and clears its display state.
- A temporary source-query failure may use the last successfully confirmed active alert for at most 60 minutes.
- After 60 minutes without a successful source confirmation, the alert is hidden and cannot become eligible for a repeated severe/extreme display.
- When the source recovers and confirms the same still-active alert, its existing display and cooldown history is preserved. Source recovery alone does not make it a new alert.
- A substantively changed headline or increased severity remains a new presentation and receives a new immediate display window; a severity decrease follows the existing non-replay rule.

### Unknown Alert Severity

- An active alert with unknown or unrecognized severity receives one immediate 10-minute display window.
- Unknown severity does not qualify for repeated displays after the initial window.
- The module does not infer a severe or extreme classification from the headline alone.
- If the source later supplies a severe or extreme classification, the severity change makes the alert new and starts an immediate 10-minute display window.

### Feature Settings

- Weather Forecast has an independent user setting and is disabled by default.
- Disabling Weather Forecast does not disable the baseline current-weather surface.
- The setting exposes configurable local start and end times for the optional tomorrow high/low card; its default window is 21:00 through 23:30.
- Weather Alert remains controlled by its existing independent setting and Breezy Weather data-access permission.
- Weather Alert remains disabled by default, matching the existing settings schema.
- Rename the user-facing setting from `Severe Weather Alerts` / `恶劣天气提醒` to `Weather Alerts` / `天气预警`, because minor, moderate, and unknown-severity alerts are also eligible for one-time display.
- The setting description states that it displays currently active weather alerts from Breezy Weather.
- Severity controls repeat eligibility; it does not act as a minimum threshold for initial display.

### Alert Display Clock

- Reading or caching an alert does not start its 10-minute display window.
- The display window starts when the alert is first visibly presented on AOD. Lock-screen selection and rendering do not show the alert, start its window, or consume the AOD repeat-entry marker.
- An unseen pending alert is discarded if it ends, becomes source-stale, or otherwise becomes ineligible before its first presentation.
- A severe or extreme alert's two-hour cooldown starts when its visible 10-minute window ends.
- A changed alert receives a new pending presentation even if an older alert is currently hidden or cooling down.

### Card Transition and Layout Stability

- Weather Forecast and Calendar Event share one fixed-height, one-line contextual card slot on both surfaces; AOD Weather Alert uses that same slot when it is eligible.
- Replacing one selected card with another uses an approximately 250 ms crossfade without moving notification or media rows.
- Entering or leaving the no-card state uses an approximately 300 ms coordinated card fade and vertical movement of lower notification/media rows.
- Card transitions do not use per-character motion, scaling, or horizontal slides.
- A lock-screen/AOD transition preserves the current eligible card, transition progress, and layout geometry rather than replaying the card-entry animation. Lock-screen rendering does not select an AOD-only alert or consume its repeat-entry marker.

### Visual Emphasis

- Weather Forecast uses the same neutral white family as current weather, at reduced alpha to communicate its low priority.
- Calendar Event retains the module's existing light information accent.
- Weather Alert uses the deeper accent shared by the clock/media emphasis, at full card alpha.
- Alert severity does not introduce red, orange, or other severity-specific colors. Severity affects reminder policy, not palette.
- Weather Alert uses a module-owned simple monochrome weather-warning icon rather than Android's generic dialog-warning drawable.
- Weather Forecast uses the selected weather icon source for tomorrow's condition.

### Typography

- Weather Forecast, Calendar Event, and Weather Alert use the same card text size and font-weight policy.
- Card text size matches the date/current-weather information size for the active large or compact clock layout.
- Card weight follows the same lock-screen/AOD transition and compensation policy as date/current-weather text.
- Weather Alert does not become larger or heavier than the other contextual cards; its icon and deeper accent provide emphasis.
- Weight transitions must preserve measured character positions and must not change card-slot geometry.

### Dynamic Clock Size

- A contextual card does not independently force the dynamic clock into compact mode.
- Clock size continues to depend on notifications, media, total content height, and available safe space.
- The layout selects compact mode only when the complete visible content set cannot fit safely with the large clock.
- Lock screen and AOD use the same clock-size calculation for the content eligible on each surface; Weather Alert can affect that calculation only on AOD.
- Alert appearance or expiry on AOD must not cause a size transition unless the aggregate space calculation actually crosses the large/compact boundary.

### Alert-state Persistence

- Persist the logical alert identity, first visible-presentation time, display-window end, cooldown deadline, and last successful source-confirmation time.
- The same logical alert retains its display and cooldown history across SystemUI restarts, module updates, and device restarts.
- A restart does not grant an unchanged alert a new immediate 10-minute display window.
- After restart, Breezy Weather is queried again before persisted state is resumed. The alert must still be active and within the 60-minute source-freshness allowance.
- Persisted state is cleared when the source removes the alert or its validity ends.

### Logical Alert Identity

- Prefer a stable provider-supplied alert identifier when available.
- Alert identity is scoped to the Breezy Weather location that supplied it.
- Without a stable provider ID, identify an alert by stable location identity, normalized headline, and its original start time.
- Extending or shortening the end time updates validity but does not create a new alert or reset display/cooldown history.
- A severity increase immediately starts a new 10-minute display window.
- A severity decrease does not immediately replay the alert; future repeat eligibility follows the lower severity.
- A substantive headline change creates a new alert.
- Differences limited to case, repeated whitespace, or punctuation do not create a new alert.

### Multiple Active Alerts

- At most one Weather Alert is selected for the contextual card slot at a time.
- Select the alert with the highest severity first.
- Among alerts with the same severity, prefer the alert that ends sooner because it has the shorter remaining action window.
- If severity and end time are equal, use original start time and then stable logical identity as deterministic tie-breakers so source refreshes cannot make the card oscillate.
- When the selected alert ends or is removed, immediately evaluate the remaining active alerts.
- A newly selected alert that has never been visibly presented receives its own 10-minute display window.
- A previously presented alert resumes according to its own existing display or cooldown history; becoming selected again does not reset that history.
- Display-window and cooldown state is persisted separately for every logical alert, not only for the currently selected alert.

### Alert Preemption

- If a newly active alert ranks above the currently displayed alert under the established selection order, it replaces the current alert immediately using the standard approximately 250 ms card crossfade.
- The newly selected alert receives its own 10-minute display window if it has not already consumed that window under its own history.
- The displaced alert's display deadline continues to advance in wall-clock time while it is hidden. Preemption does not pause, extend, or reset its window.
- An alert that ranks below the currently displayed alert does not interrupt it. It remains independently pending or follows its existing display/cooldown history until selection is evaluated again.
- If the displaced alert later becomes selected again, it is shown only when its unchanged individual history still makes it eligible.

### Alerts Without an End Time

- An otherwise valid active alert remains eligible when the source does not provide a usable end time.
- Among alerts with the same severity, alerts with a known end time rank ahead of alerts without one; known shorter action windows remain more urgent.
- Alerts without an end time use original start time and stable logical identity as deterministic tie-breakers.
- A missing end time does not grant indefinite trust. The source must continue to confirm the alert under the same 60-minute source-freshness rule.
- The alert becomes ineligible immediately when a successful source response removes it, or after 60 minutes without successful source confirmation.
- Its one-time or severe/extreme repeat policy remains unchanged; a missing end time does not create extra display windows.

### Forecast Time-boundary Updates

- While the lock screen or AOD is already visible, forecast eligibility is reevaluated when local time reaches the configured start/end boundaries and at local midnight when a cross-midnight range is in use.
- At the configured start time, an eligible forecast enters the contextual slot using the standard approximately 300 ms no-card-to-card transition when no higher-priority card is selected.
- At the configured end time, the forecast becomes ineligible immediately and leaves using the corresponding coordinated transition.
- A boundary update may change an already visible lock-screen/AOD surface, but it must not wake the display solely to show or hide the forecast.
- If the display is hidden at the boundary, no presentation animation is run; the next lock-screen/AOD entry renders the state appropriate to the then-current local time.
- Manual clock, date, or time-zone changes trigger immediate reevaluation against the new device-local date and time.

### Forecast Temperature Format

- The tomorrow forecast uses the same temperature unit as the baseline current-weather surface.
- The current product format is rounded whole degrees Celsius with a degree symbol and no unit letter or decimal fraction.
- Display the daily high first and the daily low second, using a localized equivalent of `Tomorrow 31° / 25°`.
- Current weather and forecast must never mix temperature units. If unit selection is added later, both surfaces change together.

### Forecast Condition Icon

- Prefer the source's representative whole-day condition for tomorrow.
- If no whole-day condition exists and the source separates daytime and nighttime conditions, use tomorrow's daytime condition.
- The icon represents tomorrow's forecast and must not switch to a moon or nighttime variant merely because the card is viewed during the current evening.
- Do not alternate between tomorrow's daytime and nighttime icons.
- If neither a representative whole-day condition nor tomorrow's daytime condition is usable, the forecast is incomplete and the card is not shown; a nighttime-only condition is not used as a fallback.

### Forecast Data Refresh

- The module does not introduce high-frequency forecast polling. It consumes Breezy Weather update events and requests the existing cached relay when entering or recreating the lock-screen/AOD surface.
- If newly received data materially changes the currently selected visible forecast, replace the old card content with an approximately 250 ms crossfade while preserving slot geometry.
- If the forecast is not selected because a higher-priority card is visible, update only the cached forecast state and do not run a hidden presentation animation.
- A forecast update must not wake the display.
- If the displayed icon, date, high, and low are unchanged, do not invalidate or redraw the card solely because a new source payload arrived.

### Lock-screen and AOD Privacy

- Weather Alert follows the existing SystemUI sensitive-content policy on AOD.
- When sensitive content is hidden, AOD shows a localized generic label equivalent to `Weather alert` instead of the provider headline. The lock screen never selects or renders Weather Alert, so it neither shows the headline nor a redacted alert label.
- The generic privacy label retains the normal warning icon, deeper accent, selection priority, display window, and cooldown history.
- Redaction changes presentation only. It does not change logical alert identity or create a new display window.
- Tomorrow's high/low forecast is treated as non-sensitive weather information and remains visible on both the lock screen and AOD under the same privacy setting.

### Card Interaction

- Weather Forecast and Calendar Event cards are display-only on the lock screen and AOD; Weather Alert is display-only on AOD.
- They do not register tap, long-press, swipe, or other touch handlers and do not launch Breezy Weather.
- AOD touch input remains owned by the existing OOS wake, gesture, and fingerprint flows.
- Adding authenticated lock-screen navigation is outside the scope of this policy and requires a separate design decision.

### Weather Location Ownership

- Forecast and alert cards use only Breezy Weather's currently active primary location.
- Do not merge alerts or forecasts from other saved or favourite locations into the contextual slot.
- When the active location changes, immediately hide content belonging to the previous location and wait for usable data belonging to the new location.
- Each logical alert includes stable location identity so equivalent headlines and start times in different locations remain distinct alerts.
- Display and cooldown histories are stored per location. Switching away and back does not make an unchanged alert new or reset its history.

### Feature Disable and Re-enable

- Explicitly disabling Weather Alert immediately hides the card, cancels its presentation timers, and clears persisted pending, display-window, and cooldown history for that feature.
- Re-enabling Weather Alert queries Breezy Weather again. A currently active eligible alert is treated as newly eligible and can receive a fresh 10-minute display window.
- Explicitly disabling Weather Forecast immediately hides its card but requires no display-history reset because forecasts do not have one-time or cooldown state.
- Re-enabling Weather Forecast evaluates the current local time, tomorrow date, completeness, and six-hour freshness rules before showing anything.
- Feature toggles do not wake the display solely to show the resulting card-state change.
