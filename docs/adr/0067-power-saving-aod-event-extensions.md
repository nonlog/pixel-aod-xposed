# ADR 0067: Event-driven extensions for native Power Saving AOD

Date: 2026-09-11
Status: Accepted

## Context

OPlus Power Saving AOD keeps the native AOD stack configured but normally hides its clock after a vendor-owned interval. The product requirement is two narrow exceptions without restoring the retired module display-mode or schedule owner: show AOD briefly for a new notification, and keep AOD visible while external power is connected.

Current CPH2573/OOS evidence provides two suitable vendor seams. The incoming-notification lifetime and privacy decision are owned by OplusAodCurvedDisplayView under ADR 0066. Power Saving clock visibility is entered through AodClockLayout.showClock(int), while AodUpdateManager.setHideAlarm() and the existing energy-saving hide callbacks own the later hide.

## Decision

1. Activate these extensions only when NativeAodAvailabilityAdapter reports the selected user native display mode as energy-saving. Never create a second module AOD mode.
2. Notification-triggered AOD is allowed only when the module toggle and native Show new notifications on AOD preference are enabled. OPlus retains notification admission, privacy, and lifetime. An attached OplusAodCurvedDisplayView is authoritative evidence for this exact transient event, even when it arrives before Display.STATE_DOZE; the coarser mAodPowerSave pulse-suppression bit must not veto a notification OPlus has already admitted. Pixel asks the existing AodClockLayout to show and creates no module timeout.
3. Charging AOD observes the sticky platform battery plugged state. While plugged, non-interactive, unsuppressed, and outside the proximity/pocket gate, request AodClockLayout.showClock(0), but keep AodUpdateManager.setHideAlarm() intact. Its native deadline remains authoritative.
4. At the actual OplusOSAodManager.IAodDisplayStateChange#onEnergySavingNotifyHide() controller callback, a valid charging hold suppresses only the full-AOD hide. At that same native deadline, setVisibilityInAOD(1) is applied to the OPlus UDFPS mechanism so the fingerprint icon still times out. Do not call notifyHideAodIcon() here because the current OOS implementation also requests display state OFF.
5. CPH2573/OOS has a second Power Saving presentation terminal in `AodUpdateManager.isDisplayModeAllowUpdateClock()`: after the native update budget is exhausted, `WorkshopAodController.needUpdateClock()` calls `AodClockLayout.hideClock(12)`. Bypass only this energy-saving update-budget decision while either the charging hold is eligible or an admitted native Peek window is attached with the Pixel notification transient active. This keeps native clock updates alive for those two explicit windows and prevents the notification `showClock(0)` from being immediately undone.
6. CPH2573/OOS can issue a later DreamService#setDozeScreenState(OFF) terminal after the controller/update-budget paths. While the same explicit charging hold remains eligible, replace only that terminal request with DOZE_SUSPEND. This is a charging-only exception; proximity, base suppression, power policy, unplug, and every non-energy-saving mode leave the vendor request unchanged.
7. When the charging hold becomes invalid because of unplug, policy/suppression, or proximity, stop holding controller, update-budget, and Dream OFF terminals. If a native terminal was actually held, re-apply the remembered native setHideAlarm() so OPlus resumes its own Power Saving behavior. If no terminal has fired yet, leave the existing vendor deadline untouched. Native AOD disabled/provisioning failure and OPlus notification admission remain higher-priority gates.
8. Do not write OPlus Secure Settings, schedule exact alarms, register a duplicate proximity sensor, acquire wake locks, call PowerManager.wakeUp, or hook DisplayPowerController/panel/HBM ownership.

## Consequences

- The requested exceptions follow existing vendor lifecycle objects instead of creating another AOD state machine.
- Charging preserves the vendor hide timer, converts the controller timeout into a fingerprint-only timeout, keeps the native clock update budget open only for the charging window, and keeps the existing Dream in DOZE_SUSPEND when the later vendor OFF terminal arrives while the charging hold is still valid.
- A future ROM that changes these vendor controller seams still falls back outside the explicit charging-only gates; the extension does not take DisplayPowerController, panel, HBM, wake-lock, or schedule ownership.
- Physical validation remains required for notification-window duration, charging plug/unplug, pocket interaction, and Lockscreen/AOD transition regressions.
