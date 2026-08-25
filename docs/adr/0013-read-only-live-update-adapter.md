# ADR 0013: Read-only Android Live Update adapter

Date: 2026-08-22
Status: Accepted; S20.2 refinement 2026-08-24

## Context

Android promoted ongoing notifications and Android 17 `MetricStyle` expand the set of long-running activities that SystemUI can surface as stable, glanceable state. Current OxygenOS also has its own OPlus Live Alert / Seedling classification pipeline. Pixel AOD must not infer Live Update status from ordinary `ONGOING_EVENT` flags, clone the vendor card engine, or reduce every vendor Live Alert to a copied text sentence.

Real-device S20/S20.1 testing proved two important constraints on the current ROM:

- a real OnePlus Clock timer is an OPlus `type=LIVE` / `SOURCE_SEEDLING` item but is not placed in the stock `ENTRY_AOD` set;
- copying vendor-rendered text produces incorrect AOD presentation (`Nap 时间`, long Hotspot sentences), and a normal `TextView` callback alone does not guarantee a new hardware AOD frame while Dozing.

ADR 0008 keeps the dozing surface presentation-only; ADR 0006 constrains the low-power visual budget; ADR 0018 remains the sole cross-source contextual arbiter.

## Decision

Implement a **read-only, structured Live Update adapter and dedicated presentation surface**.

1. Accept only activities already classified live/promoted by Android/OPlus or exposed by a proven native final-AOD seam. Never upgrade arbitrary ongoing notifications.
2. Prefer final OPlus `ENTRY_AOD` data when present. On the current ROM, allow an `ENTRY_STATUS_BAR` fallback only after OPlus has already classified and ranked the model as Live Alert.
3. Normalize only reliable structured semantics. Initial kinds are Timer, Call, Hotspot, and determinate Progress. Unknown/generic vendor Live Alerts are fail-closed for contextual body presentation and may remain represented by the existing notification icon.
4. Treat vendor rendered Views only as a narrow extractor for known semantics when no structured field exists (for example strict Timer `M:SS/H:MM:SS` or Hotspot device count). Never display the scraped sentence itself and never scan arbitrary application Views.
5. Keep S18 as the only selector. After a `LIVE_UPDATE` wins, render it with a dedicated low-power surface rather than an ordinary contextual sentence:
   - Timer/Call: semantic glyph + small static label above a visually primary time metric;
   - Hotspot: semantic glyph + small static label above a visually primary numeric connected-device metric;
   - Progress: semantic glyph + small static label + primary percentage metric + thin determinate progress indicator.
6. Same-identity metric/progress changes update in place. They do not re-enter S18, cross-fade the whole contextual row, or move lower rows.
7. Dynamic time uses one monotonic `elapsedRealtime()` base. Vendor per-second samples may establish/repair that base, but small scheduler jitter must not replace it every second.
8. A second-level dozing repaint may use only a proven vendor region-refresh capability. On the current CPH2573 runtime `mIsSupportRamLessAod=false`, so deep AOD must not pretend that a `M:SS` value can repaint every second. Interactive/lockscreen surfaces keep chronometer seconds; deep AOD uses an adaptive low-power metric derived from the same `elapsedRealtime()` anchor.
9. Deep-AOD adaptive metrics refresh on system/native minute events that already exist for the clock. The primary current-ROM seam is the COUI host's existing `ACTION_TIME_TICK` receiver; OPlus `UPDATE_TIME` / `AodClockLayout#performAodUpdate` is observed as a secondary seam. Do not create a module Timer alarm merely to update the metric.
10. **Never** call current-OOS `AodClockLayout#performAodUpdate()` as a Timer tick source: exact code advances vendor `mCurrentDisplayTime` by one minute and increments update counters by 60 on every call. We observe vendor callbacks only.
11. Apply existing selected-user/privacy, contextual suppression, proximity, schedule/power, expiry/staleness, and one-row visual-budget gates. AOD exposes no direct actions; interaction stays with normal Keyguard/SystemUI.

## Consequences

- Pixel AOD can approximate Android 17 MetricStyle semantics instead of merely copying a changing notification string.
- Time metrics can change independently from label/layout state and avoid whole-row flashing.
- Current-ROM OPlus classification remains authoritative while Pixel owns only the low-power presentation of an already accepted state.
- Second-level AOD refresh remains device-capability dependent. On the current non-ramless device, deep AOD intentionally degrades to an adaptive minute metric and follows the already-running system/native clock cadence, while interactive surfaces keep second-level precision.
- Support for additional MetricStyle-like domains can be added incrementally without loosening generic notification classification.

## Rejected alternatives

- Treat every ongoing notification as Live Update: too broad and contradicts Android/OPlus classification ownership.
- Display vendor capsule/card text directly: produced wrong labels, truncation, and sentence-shaped AOD UI in real tests.
- Render full notification/Seedling templates on AOD: too dense and expands interaction/security scope.
- Call `performAodUpdate()` every second: unsafe on this exact ROM because it advances minute/update accounting on every invocation.

## S20.3 runtime evidence

- CPH2573 reports `mIsSupportRamLessAod=false`, `mIsAodInstalled=true`, and an AOD plugin object. Therefore `updateRamlessArea()` cannot be used as a supported second-level repaint path.
- Android `AlarmManager` reports `time_tick_allowed_while_idle=true` and consecutive `TIME_TICK` history while the device is Dozing.
- With S20.3 loaded in SystemUI PID `29315`, `CouiClockHostView` received `ACTION_TIME_TICK` at `2026-08-25 13:56:00.076` while Dozing. The same minute OPlus `AodClockLayout#performAodUpdate(boolean)` was observed at `13:56:00.120`. No module Timer was created or manipulated for this proof.
- This validates the refresh infrastructure independently of any Live Update payload. A physical user-created Timer remains the required final visual acceptance gate.

### S20.4 presentation refinement
- Center the dedicated Live Update block within the contextual lane rather than inheriting generic leading-edge composition; keep generic contextual geometry unchanged.
- Use a compact metric hierarchy (18dp glyph, 13dp/500 label, 30dp/500 primary metric) with explicit separation from the clock/weather cluster and lower notification row.
- A thin remaining-time bar is Timer-specific. Determinate installer/download-style Progress does not automatically receive a bar; its percentage remains the primary metric unless a future domain-specific design explicitly requires more.
- Timer bar progress must be deadline-derived and tick with the host. A vendor timestamp may bootstrap total duration only when sane; no reliable total means no bar.
