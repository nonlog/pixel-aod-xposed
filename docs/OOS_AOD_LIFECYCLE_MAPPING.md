# OOS AOD Lifecycle Mapping

Last updated: 2026-07-04

## Purpose

Map real OOS / SystemUI AOD events into the module's `AodLifecycleState.phase`.

This document is a working reference for making Pixel AOD behavior lifecycle-aligned instead of relying on scattered `aodActive`, display-state, and delayed-task checks.

## Evidence Snapshot

Live device sample:

| Item | Value |
|---|---|
| Device | `CPH2573` |
| Module version | `0.1.123` / `versionCode=130` |
| SystemUI PID | `29418` |
| Command | `adb -s 192.168.1.6:42481 logcat -d -v time -t 50000 PixelAodOPlus:I *:S` |
| Retained sample coverage | Stable AOD only |
| Observed phases | `aod-visible` |
| Missing from retained buffer | `entering-aod`, `aod-grace`, `aod-active-waiting-display`, `interactive`, `inactive` |

Important limitation: the retained logcat sample did not include the first few hundred milliseconds of a fresh screen-off entry. Rows marked "expected" below come from current code paths, not from the retained live sample.

## Phase Definitions

Current implementation: `PixelAodClockView.AodLifecycleState.phase()`.

| Phase | Current condition | Meaning |
|---|---|---|
| `interactive` | `PowerManager.isInteractive() == true` | Screen is awake / user-facing. Pixel AOD should not draw. |
| `aod-visible` | non-interactive, display is `DOZE` or `DOZE_SUSPEND`, and Pixel AOD draw condition is true | Native display is already in AOD. Module AOD may render. |
| `entering-aod` | non-interactive and inside the short entry-delay window | Screen just turned off or AOD was just activated; module may draw before display state fully reports doze. |
| `aod-grace` | non-interactive, `aodActive=true`, inside entry grace window, but not already classified above | Short compatibility window after AOD activation. |
| `aod-active-waiting-display` | non-interactive, `aodActive=true`, but not display-AOD and not in grace window | OOS says AOD active, but display state is not aligned. This is suspicious if long-lived. |
| `inactive` | none of the above | Pixel AOD should not draw. |

## Live Observed Event Mapping

These rows were observed in the retained live logcat sample.

| OOS / module event source | Observed phase | Observed role | Notes |
|---|---|---|---|
| `AodClockLayout#performAodUpdate(boolean)` | `aod-visible` | Native OOS AOD update tick | Drives minute/time refresh and AOD overlay visibility while display is `DOZE_SUSPEND`. |
| `DreamService#setDozeScreenState` | `aod-visible` | Native doze screen-state request | Observed as `requestedState=4` with `reason=outside-entry-window`; module correctly did not rewrite outside the entry window. |
| `AODDisplayUtil#requestScreenState(int,int,String)` | `aod-visible` | Native display-state request | Observed as `requestedState=1` with `reason=outside-entry-window`; module correctly did not rewrite outside the entry window. |
| `broadcast` | `aod-visible` | System broadcast reaching lockscreen clock view | Observed lockscreen decision was `visible=false reason=noninteractive-outside-aod-window`, which is correct during stable AOD. |
| `updateTime` | `aod-visible` | Internal frame refresh after time text update | Observed after minute tick; requests invalidate / frame refresh while AOD remains shown. |
| `screen-off` as `traceSource` | `aod-visible` | Trace origin for current AOD session | Retained logs show the stable AOD trace was seeded by screen-off. |

## Expected Event Mapping From Code

These rows are expected from the current code paths but were not all present in the retained logcat window.

| Event source | Expected phase sequence | Current behavior | Risk / next check |
|---|---|---|---|
| `screen-off` | `entering-aod` -> `aod-visible` | `noteScreenOff()` seeds trace and entry-delay state; delayed visibility update follows. | Need a live capture immediately after screen-off to confirm the short `entering-aod` window is logged. |
| `AodRecord#onDreamingStarted` | `entering-aod` or `aod-visible` | Calls `setAodActive(true)`, refreshes notifications, ticks instances. | Should become the canonical native-AOD-active signal if present reliably on OOS. |
| `AodRecord#createAndInitRootView` | usually `entering-aod` or `aod-visible` | Calls `handleOuterRootLayout()` and begins stock hide / host handling. | Host arrival timing should be compared against `onDreamingStarted`. |
| `AodClockLayout#...#host-ready` | `entering-aod` -> `aod-visible` | If screen is already off, seeds screen-off if needed and may set AOD active. | This is a fallback host-readiness path; should not become the primary state owner if `AodRecord` is reliable. |
| `AodRecord#onDreamingStopped` | `interactive` or `inactive` after transition | Hides Pixel AOD overlays, prepares AOD-to-lockscreen transition, and schedules guarded stock restore. | Retained logs should show whether restore executes, skips by trace mismatch, or keeps stock hidden due lockscreen/AOD. |
| `screen-on` | `interactive` | Prepares AOD-to-lockscreen transition, hides Pixel AOD, suppresses system AOD during transition. | Should not start a new AOD trace that can confuse delayed restore unless needed for transition logging. |
| `AodRecord#onEnergySavingNotifyHide()` | `aod-visible` during suppressed hide | Suppressed inside entry/live AOD path, then `setAodActive(true)` and refresh host visibility. | Already observed indirectly through `traceSource=...#suppressed-hide` after install. |
| `OplusWakeUpController#notifyHideCallback()` | `aod-visible` during suppressed hide | Same reassertion pattern as energy-saving hide callbacks. | Confirm this does not create redundant refresh churn in long AOD sessions. |
| `DreamService#setDozeScreenState` inside entry window | `entering-aod` | May rewrite `STATE_OFF` / `STATE_DOZE_SUSPEND` to `STATE_DOZE`. | Need live entry logs to prove rewrite happens only during the intended entry window. |
| `AODDisplayUtil#requestScreenState...` inside entry window | `entering-aod` | Same rewrite guard as DreamService path. | Same live-entry evidence needed. |
| `restoreHiddenStockViewsAfterTransition()` delayed task | `interactive` or `inactive`, unless trace mismatch or lockscreen/AOD still active | Restores stock views only if expected trace still matches and neither lockscreen replacement nor Pixel AOD should remain active. | This path already has trace guard; future delayed restore paths should follow the same pattern. |
| `scheduleStockSuppressionReapply()` delayed task | same trace as scheduling event | Reapplies stock hide only if expected trace still matches. | Added in `0.1.123`; watch for `reason=trace-mismatch` to confirm old hide tasks are skipped. |

## Decision Rules

Use this interpretation when reading logs:

| Log pattern | Interpretation |
|---|---|
| `phase=entering-aod entryDelay=true` | Early AOD entry. Display may not yet report `DOZE`; stock hide and Pixel overlay can be prepared. |
| `phase=aod-visible displayAod=true customizeNow=true` | Normal active module AOD. Pixel AOD can draw; stock AOD should remain suppressed. |
| `phase=aod-visible recentOverlayVisible=false` | Still active AOD, but last overlay-visible mark is older than the recent-overlay window. Not automatically bad. |
| `phase=aod-active-waiting-display` | Suspicious if persistent. OOS says active but display is not doze and grace window expired. |
| `reason=outside-entry-window` on screen-state rewrite | Expected during long-running AOD; do not rewrite mature OOS screen-state requests. |
| `reason=trace-mismatch` on delayed restore / hide | Desired guard behavior. Old delayed tasks did not apply to the current AOD session. |
| `lockscreen visibility decision ... bridge=false ... phase=aod-visible` | Correct during stable AOD: lockscreen replacement should stay hidden while AOD owns the visual surface. |

## Gaps To Close Next

1. Add a lightweight "phase changed" log emitted only when `AodLifecycleState.phase()` changes.
2. Capture a fresh screen-off sequence and verify this chain:
   `screen-off` -> `entering-aod` -> `AodRecord#onDreamingStarted` -> `AodClockLayout#host-ready` -> `aod-visible`.
3. Capture screen-on / AOD exit and verify this chain:
   `AodRecord#onDreamingStopped` -> `screen-on` -> Pixel AOD hidden -> lockscreen replacement visible or stock restore skipped/restored for the right reason.
4. Treat any long-lived `aod-active-waiting-display` as a bug candidate: it means module state and native display state disagree.
5. Keep every delayed hide/restore task tied to the trace that scheduled it.

## Current Recommendation

Next implementation should not add more visual behavior yet. It should first make phase transitions observable:

```text
previous phase != current phase
        -> log source, previous phase, current phase, trace, display state, active flag, screenOffAge, aodAge
```

That will let future bugs be diagnosed by state transition order instead of by isolated visibility logs.
