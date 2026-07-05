# Pixel AOD Roadmap

Last updated: 2026-07-05

## Goal

Build a stable Pixel-like AOD visual replacement for OPlus SystemUI.

The target is not a full ROM-level replacement of Android Doze. The module should keep Pixel-style visuals while aligning itself as tightly as practical with the native OOS AOD / Doze lifecycle.

## Open-Source Reference Choice

Use different references for different layers:

| Layer | Recommended reference | Reason |
|---|---|---|
| Visual target | Google Pixel AOD behavior | This is the desired user-facing look and feel. |
| Lifecycle model | AOSP SystemUI Doze | AOSP exposes the canonical Doze state machine and lifecycle concepts. |
| Practical open implementation | LineageOS / YAAP / crDroid SystemUI Doze | Custom ROMs provide readable, modified SystemUI implementations closer to real-world Pixel-style AOD customization. |
| Final runtime truth | OOS SystemUI logs and classes | The module runs inside OOS SystemUI, so OOS behavior decides what is actually safe and stable. |

Google Pixel AOD should not be treated as the primary source-code base. Pixel's complete AOD experience includes Google / Pixel-specific pieces that are not fully available as open-source implementation code.

## Baseline Recommendation

Use this priority order:

1. Pixel AOD as the visual and behavior target.
2. AOSP `SystemUI/src/com/android/systemui/doze` as the lifecycle and state-machine reference.
3. LineageOS / YAAP / crDroid SystemUI Doze code as practical implementation references.
4. OOS runtime logs, dumps, and hooked classes as the final source of truth.

The module should become a lifecycle-aligned Pixel visual layer:

```text
OOS Doze / AOD lifecycle
        ↓
Pixel AOD state adapter
        ↓
Pixel-style AOD rendering
```

It should not become an independent AOD engine that guesses screen state, owns long-running refresh loops, or fights OOS display power policy from the outside.

## Doze Lifecycle Integration Direction

Full ROM-level integration would require owning or replacing SystemUI Doze internals such as `DozeMachine`, display state, sensor triggers, wake locks, pulse handling, brightness, and low-power policy. That is not realistic or desirable for this Xposed module.

The feasible Xposed direction is lifecycle alignment:

| Area | Preferred behavior |
|---|---|
| AOD active state | Follow OOS native AOD / dreaming / display callbacks. |
| Clock refresh | Prefer native SystemUI / OOS refresh callbacks; avoid self-owned minute loops unless used as a narrowly scoped fallback. |
| Visibility | Render only while native lifecycle says AOD should be visible. |
| Sensors | Reuse or observe OOS handling for proximity, pocket, pickup, tap, and pulse where possible. |
| Power policy | Respect OOS energy-saving, low-battery, and display-off decisions instead of overriding them broadly. |
| Failure handling | Log state transitions and decision reasons so intermittent bugs can be traced by lifecycle session. |

This can reduce power usage and improve stability compared with a purely parasitic overlay, but it cannot become identical to Pixel ROM Doze without replacing or deeply modifying SystemUI itself.

## Roadmap

## Architecture Deepening Roadmap

These items are incremental deep-module refactors, not mutually exclusive
alternatives. They should be implemented one at a time so each change keeps
runtime behavior observable and reversible.

| Order | Module direction | Status | Purpose |
|---|---|---|---|
| 1 | Deepen the AOD lifecycle policy module | First migration implemented on 2026-07-05 | Put Pixel overlay, native Doze keepalive, stock AOD suppression, and native hide callback decisions behind one lifecycle policy interface. |
| 2 | Split SystemUI hook orchestration from stock visibility | First migration implemented on 2026-07-05 | Keep hooks as adapters and move stock view hide / restore / delayed reapply / trace guard behavior into one stock visibility module. |
| 3 | Extract the AOD notification pipeline | First migration implemented on 2026-07-05 | Make lockscreen and AOD consume the same notification display model, including silent filtering, media classification, and icon fallback reasons. |
| 4 | Create a settings schema module | First migration implemented on 2026-07-05 | Centralize setting keys, defaults, UI visibility, provider rows, and restart requirements. |
| 5 | Separate Pixel AOD rendering from policy and data collection | First migration implemented on 2026-07-05 | Let views apply render models instead of owning policy, data parsing, and drawing state in the same implementation. |

### Architecture Tracking

- 2026-07-05: Architecture review identified five deepening candidates. The
  recommended implementation order is lifecycle policy, stock visibility,
  notification pipeline, settings schema, then render model.
- 2026-07-05: Phase 2 continues with item 1. First implementation step should
  move the existing behavior-preserving AOD policy output into
  `OosAodLifecycleAdapter` without changing AOD / lockscreen behavior.
- 2026-07-05: Item 1 first migration implemented. `OosAodLifecycleAdapter`
  now owns the final AOD policy decision object and reason calculation, while
  `PixelAodClockView` still collects current state, module settings, schedule,
  power policy, and trigger-window facts. Debug APK build passed with JDK 17.
- 2026-07-05: Item 2 first migration implemented.
  `StockAodVisibilityController` now owns stock view hidden-state tracking,
  adjusted status-view restoration, delayed stock suppression reapply trace
  guards, and delayed transition restore trace guards. `PixelAodHook` still
  owns OOS view-tree marker heuristics and calls this module through a small
  interface.
- 2026-07-05: Item 3 first migration implemented.
  `AodNotificationPipeline` now owns AOD notification visibility filtering,
  ranking snapshots, lockscreen visibility decisions, media candidate
  detection, notification signatures, and system notification classification.
  `PixelAodClockView` still owns rendering and icon drawable loading.
- 2026-07-05: Item 4 first migration implemented.
  `PixelAodSettingsSchema` now owns setting keys, defaults, always-enabled
  flags, and SystemUI restart metadata. `PixelAodSettingsProvider` and
  `SettingsActivity` read schema defaults instead of duplicating fallback
  values.
- 2026-07-05: Item 5 first migration implemented.
  `PixelAodRenderModel` now owns clock text, date/weather text, At a Glance
  extra formatting, and battery row render data for the AOD clock path.
  `PixelAodClockView` and `PixelLockscreenClockView` apply that model while
  leaving visibility, media, notification, and transition policy unchanged.

### Phase 1: Stabilize Current Replacement Layer

- Keep the currently working AOD clock refresh path.
- Keep debug logs focused on lifecycle transitions, visibility decisions, notification decisions, and hide / restore paths.
- Continue treating the silent notification flash during OOS lockscreen-to-AOD transition as a deferred known issue.
- Avoid broad changes to lockscreen notification rows until the lifecycle model is cleaner.

### Phase 2: Define AOD State Adapter

- Build a small internal state model inspired by AOSP Doze states.
- Map OOS events into module states such as inactive, lockscreen, entering AOD, AOD visible, pulsing, paused, and exiting AOD.
- Make rendering decisions depend on this state adapter instead of scattered direct checks.
- Keep OOS as the lifecycle owner; the adapter only translates observed native state into module decisions.

### Phase 3: Pixel-Like Burn-In Behavior

- Replace ad-hoc movement with Pixel-like burn-in offset behavior.
- Keep offsets small, periodic, and deterministic enough to avoid visible jitter.
- Ensure clock, notification icons, media text, and secondary rows move as one visual group unless Pixel behavior says otherwise.

### Phase 4: Pixel Visual Parity

- Remove the user-facing clock scale option if no longer needed.
- Keep the default clock scale and spacing close to Pixel behavior.
- Review clock weight transition, AOD-to-lockscreen transition, and lockscreen-to-AOD transition for one-frame flashes.
- Keep existing notification icon handling for now unless a specific app regression requires a targeted fix.

### Phase 5: Native-Style Trigger Features

- Phase 5.2 starts by making OOS trigger / sensor mapping explicit and
  observable. The current rule table is recorded in
  `docs/OOS_TRIGGER_MAPPING.md`.
- Phase 5.3 starts by making power-saver, low-battery, charging, and unknown
  battery state checks explicit power-policy decisions inside
  `OosAodLifecycleAdapter`. This keeps behavior unchanged while making the
  reason for hiding or allowing Pixel AOD visible in logs. The current rule
  table is recorded in `docs/OOS_POWER_POLICY_MAPPING.md`.
- Phase 5.4 starts by observing native notification-pulse inputs without
  starting a custom pulse. Notification snapshot, ranking, posted, removed,
  and clear events now map to stable pulse-observation logs. The current rule
  table is recorded in `docs/OOS_NOTIFICATION_PULSE_MAPPING.md`.
- Phase 5.5 correlates notification pulse candidates with the active AOD
  trace by adding recent pulse-candidate rule, source, trace, package summary,
  and age fields to AOD lifecycle state snapshots. This still does not start a
  custom notification pulse.
- Treat AOD display as one of two explicit modes:
- `Continuous + Trigger`: continuously displays during the configured schedule while respecting power saver, low battery, proximity, and pocket policy; outside the schedule it can still display briefly when OOS provides a native short-wake / trigger window.
- `Trigger-only`: never keeps AOD visible continuously; it only displays during native short-wake / pickup / tap style trigger windows and expires automatically.
- Treat `Continuous Display Schedule` as a child setting of `Continuous + Trigger`; it controls only continuous display, never trigger display.
- Phase 5.1 treats OOS native `DOZE` short-wake entry as a trigger source, because real OOS logs do not always expose explicit pickup / tap method names.
- Native `DOZE` short-wake is de-duplicated per native trigger event, not per whole AOD trace, so later tap / lift / short-wake events in the same sleep session can show AOD again.
- Continuous display keeps native Doze alive for the scheduled window. Trigger brief display may also keep native Doze alive and suppress native hide callbacks only while its short window is active, then releases them when the window expires.
- Proximity / pocket guard trigger-only brief display by cancelling or blocking the brief trigger window when the device is covered.
- The module settings UI now has one real master switch. When disabled and SystemUI is restarted, module hooks are not installed. The old `Custom AOD` and `Lockscreen Clock` toggles were removed because they did not map cleanly to the actual hook boundary.
- Add or align pickup / lift-to-wake behavior.
- Add or align tap-to-show behavior.
- Add proximity and pocket-aware behavior.
- Add charging-state and low-battery handling.
- Defer custom notification pulse until the core lifecycle is stable; use OOS native pulse behavior where possible.

### Phase 6: Later Pixel Features

- At a Glance.
- Richer media line behavior.
- Possible media artwork / progress / controls only after confirming how Pixel / AOSP / target ROMs handle interaction on AOD.

## Deferred Known Issue

Silent notifications on OOS may briefly flash during the lockscreen-to-AOD transition when the channel is silent but still has lockscreen display permission enabled. The current user workaround is to disable lockscreen display permission for affected silent channels.

Do not resume this issue unless it becomes a priority again. It is likely tied to OOS lockscreen row timing and should be revisited after the AOD state adapter is clearer.

## Reference Links

- AOSP source setup: https://source.android.com/docs/setup/download
- Pixel open-source components note: https://support.google.com/pixelphone/answer/13202895
- AOSP DozeMachine: https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/doze/DozeMachine.java
- AOSP DozeHost: https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/doze/DozeHost.java
- AOSP DozeTriggers: https://android.googlesource.com/platform/frameworks/base/+/master/packages/SystemUI/src/com/android/systemui/doze/DozeTriggers.java
- LineageOS SystemUI Doze: https://github.com/LineageOS/android_frameworks_base/tree/lineage-23.2/packages/SystemUI/src/com/android/systemui/doze
- YAAP SystemUI Doze: https://github.com/yaap/frameworks_base/tree/sixteen/packages/SystemUI/src/com/android/systemui/doze
- crDroid SystemUI Doze: https://github.com/crdroidandroid/android_frameworks_base/tree/16.0/packages/SystemUI/src/com/android/systemui/doze
