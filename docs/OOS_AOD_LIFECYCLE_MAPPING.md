# OOS AOD Lifecycle Mapping

Last updated: 2026-07-04

## Purpose

Map real OOS / SystemUI AOD events into the module's `AodLifecycleState.phase`.

This document is a working reference for making Pixel AOD behavior lifecycle-aligned instead of relying on scattered `aodActive`, display-state, and delayed-task checks.

## Evidence Snapshot

Stable AOD sample:

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

Fast lock/unlock sample:

| Item | Value |
|---|---|
| Device | `CPH2573` |
| Module version | `0.1.124` / `versionCode=131` |
| SystemUI PID | `19549` |
| Source | `/data/adb/lspd/log/modules_2026-07-04T19:44:27.56369.log` |
| User test window | `2026-07-04T19:51` |
| Retained sample coverage | Rapid lockscreen / AOD / screen-on switching |
| Observed phases | `interactive`, `entering-aod`, `aod-grace` |
| Error scan | No `FATAL`, `AndroidRuntime`, `Failed to instantiate`, `Class does not extend`, `NoClassDefFoundError`, or `UnsatisfiedLinkError` matches in the `19:51` module-log window |

Important logging rule: use `adb logcat` for live capture, but always check LSPosed persistent module logs under `/data/adb/lspd/log/modules_*.log` for user-reported time windows. The current logcat ring buffer can roll past dense AOD traces within minutes.

Adapter validation sample:

| Item | Value |
|---|---|
| Device | `CPH2573` |
| Module version | `0.1.125` / `versionCode=132` |
| SystemUI PID | `19041` |
| Extracted log | `logs/pixelaod_20260704_2025.txt` |
| Source | `/data/adb/lspd/log/modules_2026-07-04T20:21:26.512919.log` |
| User test window | `2026-07-04T20:24:30` to `2026-07-04T20:26:30` |
| Retained sample coverage | Rapid lockscreen / AOD / screen-on switching after adapter install |
| Pixel AOD matches | `172` from current `adb logcat`, `1868` from LSPosed persistent module log |
| Adapter events | `27` |
| Event counts | `dreaming-started=6`, `dreaming-stopped=6`, `screen-on=6`, `screen-off=5`, `display-state-request=1`, `native-tick=1`, `notification-snapshot=1`, `visibility-decision=1` |
| Observed final entry | `screen-off` trace `aod-2a-bac3d6` reached `aod-visible` at `20:25:56.357` with `displayState=DOZE(3)` and `displayAod=true` |
| Error scan | No `FATAL`, `AndroidRuntime`, `Failed to instantiate`, `Class does not extend`, `NoClassDefFoundError`, `UnsatisfiedLinkError`, `Exception`, `ANR`, or `crash` matches in the extracted Pixel AOD window |

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

These rows were observed in the `19:51` LSPosed persistent module-log sample.

| OOS / module event source | Observed phase | Observed role | Notes |
|---|---|---|---|
| `AodRecord#onDreamingStopped#hideAllAodOverlays` | `interactive` | AOD exit / screen-on transition | Repeated during rapid tests. Pixel AOD overlay was hidden and stock keyguard hide pass ran with the same trace. |
| `screen-on#hideAllAodOverlays` | `interactive` | Broadcast-driven screen-on cleanup | Observed immediately after `ACTION_SCREEN_ON`; starts a screen-on trace and keeps Pixel AOD hidden. |
| `AodRecord#onDreamingStarted#setAodActive` | `entering-aod` | Native AOD start | Observed at `19:51:02.997`, `19:51:05.115`, `19:51:16.400`, and `19:51:17.850` during rapid switching. |
| `screen-off#noteScreenOff` | `entering-aod` | Broadcast-driven AOD entry trace | Observed shortly after native AOD start; seeds the screen-off trace used by later visibility decisions. |
| `snapshot-setActiveNotifications#updateAodVisibility` | `aod-grace` | Entry grace after notification snapshot refresh | Observed while display was still `ON(2)` and non-interactive, before stable `DOZE`. |
| `aod-entry-delayed#nativeTick` | `interactive` during interrupted entry | Rapid test interruption | Observed when the device was switched back on before the short entry window finished; followed by `AodRecord#onDreamingStopped`. |
| `AODDisplayUtil#requestScreenState(View,int,boolean)#off-request#noteScreenOffIfUnset` | `entering-aod` | Native display-state request seeded entry trace | Confirms display-state hooks can become the trace source when OOS requests screen off before the broadcast path. |
| `AodRecord#createAndInitRootView+1800` delayed suppression | current trace phase, skip by `trace-mismatch` | Old delayed task guard | Observed skip when expected trace belonged to an older cycle and current trace was already newer. This is desired. |

These rows were observed in the `20:25` adapter validation sample after installing `0.1.125`.

| Adapter event | Example source | Observed phase | Notes |
|---|---|---|---|
| `dreaming-stopped` | `AodRecord#onDreamingStopped#hideAllAodOverlays` | `interactive` | Repeated during AOD exit. Pixel AOD became inactive and stock/keyguard hide passes ran on the same trace. |
| `screen-on` | `screen-on#hideAllAodOverlays` | `interactive` | Broadcast cleanup followed `dreaming-stopped` and opened a fresh screen-on trace. |
| `dreaming-started` | `AodRecord#onDreamingStarted#setAodActive` | `entering-aod` | Native AOD start appeared before or near screen-off trace creation. |
| `display-state-request` | `AODDisplayUtil#requestScreenState(View,int,boolean)#off-request#noteScreenOffIfUnset` | `entering-aod` | Confirmed the adapter classifies OOS display-state requests separately from broadcasts. |
| `screen-off` | `screen-off#noteScreenOff` | `entering-aod` | Broadcast path seeded a dedicated screen-off trace during entry. |
| `native-tick` | `aod-entry-delayed#nativeTick` | `interactive` during interrupted entry | Rapid screen-on interrupted one entry before stable doze. This is expected in fast switching. |
| `notification-snapshot` | `snapshot-setActiveNotifications#updateAodVisibility` | `inactive` after screen-on cleanup | Snapshot refresh after rapid screen-on did not incorrectly keep AOD visible. |
| `visibility-decision` | `start-existing#updateAodVisibility` | `aod-visible` | Final uninterrupted entry reached `DOZE(3)`, `displayAod=true`, and `customizeNow=true` after about `1888ms`. |

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

## Phase Transition Logging

Implemented in `PixelAodClockView.logAodPhaseIfChanged(...)`.

The helper is intentionally separate from `describeAodState()` so high-frequency diagnostic state descriptions do not emit extra logs. Each new AOD trace resets the remembered phase, then logs the first observed phase for that trace and later logs only when `AodLifecycleState.phase()` changes.

Expected format:

```text
AOD lifecycle phase changed source=<source> from=<previous> to=<current> previousTrace=<previousTrace> trace=<trace> sinceLastMs=<age> state={phase=<current> ...}
```

Current call sites:

| Call site | Why it logs phase changes |
|---|---|
| `screen-state#<ACTION>` | Records the first SystemUI broadcast-visible state for screen on/off. |
| `noteScreenOff(...)` / `noteScreenOffIfUnset(...)` | Records trace creation and early screen-off entry state. |
| `setAodActive(...)` | Records native AOD active/inactive transitions from hooked OOS paths. |
| `hideAllAodOverlays(...)` | Records AOD exit / lockscreen transition state. |
| `refreshForNativeAodTick(...)` | Records native AOD tick driven state if it crosses a phase boundary. |
| `updateAodVisibility(...)` | Records the phase at the exact point the overlay visibility decision is made. |

## OOS Lifecycle Adapter

Implemented in `OosAodLifecycleAdapter`.

The adapter is the behavior-preserving lifecycle decision gateway. It classifies raw hook sources into stable event names, logs which `AodLifecycleState.phase()` each event reached, and owns selected boolean decisions by forwarding the current state-derived result unchanged.

Current event names:

| Event | Example source |
|---|---|
| `dreaming-started` | `AodRecord#onDreamingStarted#setAodActive` |
| `dreaming-stopped` | `AodRecord#onDreamingStopped#hideAllAodOverlays` |
| `screen-off` | `screen-off#noteScreenOff` |
| `screen-on` | `screen-on#hideAllAodOverlays` |
| `display-state-request` | `AODDisplayUtil#requestScreenState...` / `DreamService#setDozeScreenState` |
| `aod-host` | `AodClockLayout#...#host-ready` / `AodRecord#createAndInitRootView` |
| `energy-saving-hide` | `AodRecord#onEnergySavingNotifyHide()` / `OplusWakeUpController#notifyHideCallback()` |
| `native-tick` | `aod-entry-delayed#nativeTick` / native AOD refresh hooks |
| `notification-snapshot` | `snapshot-setActiveNotifications#updateAodVisibility` |
| `visibility-decision` | `updateAodVisibility` sources |
| `module-event` | Any unclassified module source |

Expected format:

```text
OOS AOD lifecycle mapping event=<event> source=<source> from=<previous> to=<phase> trace=<trace> state={phase=<phase> ...}
```

The next refactor step is to move selected decisions onto this adapter once the event ordering is stable across slow entry, fast entry, AOD exit, and interrupted entry.
Current adapter-backed decisions:

| Decision | Current behavior |
|---|---|
| `shouldDrawPixelAod(...)` | Returns the existing `AodLifecycleState.shouldDrawPixelAod()` result. |
| `shouldKeepDozeScreenActive(...)` | Preserves the existing non-interactive plus recent-overlay-or-drawable rule. |
| `shouldBridgeLockscreenDuringAodEntry(...)` | Preserves the existing non-interactive, inactive, entry-window bridge rule. |

The next refactor step is to move one concrete policy at a time into these adapter-backed decisions only after logs prove the event ordering is stable across slow entry, fast entry, AOD exit, and interrupted entry.

## Log Extraction

Use `tools/extract_pixelaod_logs.ps1` when analyzing user-reported time windows. It reads both live `adb logcat` and LSPosed persistent module logs.

Example:

```powershell
powershell -ExecutionPolicy Bypass -File tools/extract_pixelaod_logs.ps1 -Serial 192.168.1.6:35001 -Start "2026-07-04 19:51:00" -End "2026-07-04 19:52:00"
```

## Gaps To Close Next

1. Capture a slow, non-rapid screen-off sequence and verify whether host readiness always lands between native AOD start and the final `aod-visible` decision:
   `AodRecord#onDreamingStarted` -> `screen-off` / display-state request -> optional host-ready -> `aod-visible`.
2. Capture a slow AOD exit and verify whether stock restore is skipped/restored for the right trace after Pixel AOD is hidden:
   `AodRecord#onDreamingStopped` -> `screen-on` -> Pixel AOD hidden -> lockscreen replacement visible or stock restore skipped/restored.
3. Treat any long-lived `aod-active-waiting-display` as a bug candidate: it means module state and native display state disagree.
4. Keep every delayed hide/restore task tied to the trace that scheduled it.
5. Keep new lifecycle-policy changes behind adapter methods so behavior changes are isolated and reviewable.

## Current Recommendation

The adapter is now producing useful logs for rapid AOD entry/exit and owns the first behavior-preserving decision gateway methods. Next development should move one small policy at a time into the adapter-backed lifecycle model only after the relevant slow-entry, long-session, or exit logs are clean. Future bugs should be diagnosed by state transition order first, then by isolated visibility logs.
