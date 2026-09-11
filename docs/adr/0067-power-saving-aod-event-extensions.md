# ADR 0067: Event-driven extensions for native Power Saving AOD

Date: 2026-09-11
Status: Accepted

## Context

OPlus Power Saving AOD keeps the native AOD stack configured but normally hides its clock after a vendor-owned interval. The product requirement is two narrow exceptions without restoring the retired module display-mode or schedule owner: show AOD briefly for a new notification, and keep AOD visible while external power is connected.

Current CPH2573/OOS evidence provides two suitable vendor seams. The incoming-notification lifetime and privacy decision are owned by OplusAodCurvedDisplayView under ADR 0066. Power Saving clock visibility is entered through AodClockLayout.showClock(int), while AodUpdateManager.setHideAlarm() and the existing energy-saving hide callbacks own the later hide.

## Decision

1. Activate these extensions only when NativeAodAvailabilityAdapter reports the selected user native display mode as energy-saving. Never create a second module AOD mode.
2. Notification-triggered AOD is allowed only when the module toggle and native Show new notifications on AOD preference are enabled. OPlus retains notification admission, privacy, and lifetime. Pixel starts its full ambient presentation for that exact native window and asks the existing AodClockLayout to show; no module timeout is created.
3. Charging AOD observes the sticky platform battery plugged state. While plugged, non-interactive, unsuppressed, and outside the proximity/pocket gate, request AodClockLayout.showClock(0) and suppress only native Power Saving hide scheduling/execution. Do not wake an interactive device or control panel state directly.
4. When the charging hold becomes invalid because of unplug, policy/suppression, or proximity, stop suppressing native hides. If still in native Power Saving mode, re-apply the remembered native setHideAlarm() so OPlus resumes its own behavior.
5. Native AOD disabled/provisioning failure, Android/OPlus base-AOD suppression, notification-pulse suppression, power policy, and proximity remain higher-priority gates.
6. Do not write OPlus Secure Settings, schedule exact alarms, register a duplicate proximity sensor, acquire wake locks, call PowerManager.wakeUp, or hook DisplayPowerController/panel/HBM ownership.

## Consequences

- The requested exceptions follow existing vendor lifecycle objects instead of creating another AOD state machine.
- Charging covers both newly scheduled and already-pending Power Saving hides by guarding the native hide execution while the hold is valid.
- A future ROM that renames or removes these vendor methods fails open to stock Power Saving behavior rather than forcing the display on.
- Physical validation remains required for notification-window duration, charging plug/unplug, pocket interaction, and Lockscreen/AOD transition regressions.
