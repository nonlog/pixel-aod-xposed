# Changelog

## [0.1.224] - 2026-07-20
### Fixed
- Reassert stock AOD suppression after OOS's authoritative per-minute `AodClockLayout#performAodUpdate` callback, with a same-trace 56 ms follow-up pass. This prevents native clock, battery, notification, and media views from being restored over the module AOD on alternating minute refreshes.

### Diagnostics
- Log native minute-tick suppression scheduling, trace mismatches, and duplicate-callback debouncing to make future vendor refresh regressions attributable.

## [0.1.223] - 2026-07-20
### Fixed
- Keep the existing per-weight cache only for exact Google Sans Flex instances built from the module font file. OOS system-family derived Typeface fallbacks are no longer cached or applied during the lockscreen-to-AOD handoff.
- Do not re-submit a same-weight `300 -> 300` Typeface at the visible handoff boundary. The existing 300-to-AOD-weight animation now starts from the already rendered lockscreen Typeface.

## [0.1.222] - 2026-07-20
### Fixed
- Re-sync the persistent ClockPlugin host after the existing delayed lifecycle-ready visibility pass, preventing a large lockscreen clock from remaining on AOD until the next minute tick when notifications require the compact clock.

## [0.1.221] - 2026-07-20
### Diagnostics
- Capture the first 12 rendered ClockPlugin handoff frames with module layer, clock Typeface, ancestor transform, and native clock carrier state when debug logging is enabled, without changing AOD presentation behavior.

## [0.1.220] - 2026-07-20
### Fixed
- Re-submit the visible ClockPlugin clock's exact Google Sans Flex Typeface at the screen-off handoff boundary even when its logical lockscreen weight is unchanged, closing the unstyled frame before the first 300-to-160 animation update.

## [0.1.219] - 2026-07-20
### Fixed
- Preserve the committed ClockPlugin AOD scene when OPlus delivers a stale keyguard render while the device remains non-interactive and the display is still dozing, preventing that callback from restoring the visible clock to the lockscreen weight.

### Diagnostics
- Record interactivity, doze display state, and rejected stale-lockscreen decisions in ClockPlugin scene logs while continuing to accept real interactive wake transitions.

## [0.1.218] - 2026-07-19
### Fixed
- Dispatch Pixel fingerprint drawable mutations through the optical fingerprint view's own handler, preventing `CalledFromWrongThreadException` from crashing System UI and triggering LSPosed safe mode.
- Normalize the lockscreen fingerprint `colorSurface` circle to opaque RGB before applying the native drawable alpha.

### Diagnostics
- Record actual clock Typeface weight/style, fake-bold state, variation, alpha, visibility, and persistent host layer overlap during handoff.

## [0.1.217] - 2026-07-19
### Fixed
- Leave the OOS pressed fingerprint carrier, its animations, scaling, HBM, and authentication state entirely native while replacing only the primary fingerprint drawable.
- Keep exact cached Google Sans Flex `wght` instances without also applying `Typeface.Builder.setWeight()`, preventing an over-bold frame at the start of the lockscreen-to-AOD transition.

## [0.1.216] - 2026-07-19
### Fixed
- Build and cache each bundled Google Sans Flex clock weight from an exact `wght` variation instance instead of relying on Android's ineffective derived-weight wrapper.
- Keep every non-interactive fingerprint state on the AOD visual path and draw the lockscreen `colorSurface` circle only on the primary carrier, preventing the pressed carrier from appearing permanently highlighted.

## [0.1.215] - 2026-07-19
### Fixed
- Derive every clock weight from one cached bundled Google Sans Flex base typeface, preventing OOS from resolving lockscreen and AOD weights as different font families during handoff.
- Match the YAAP UDFPS palette with a dynamic `textColorPrimary` lockscreen foreground over a 64 dp `colorSurface` circle that fades out completely for the pure-white AOD fingerprint.

## [0.1.214] - 2026-07-19
### Fixed
- Keep the AOD notification overflow label `+X` aligned with the date's font, paint, spacing, color, and alpha styling.
- Apply the optional Pixel fingerprint drawable to both OOS fingerprint icon carriers and re-apply it after OOS asynchronous visual updates, while leaving native visibility and authentication behavior in control.

## [0.1.213] - 2026-07-19
### Added
- Add an opt-in Pixel fingerprint icon that replaces only the native OOS `fpIcon` drawable. Native positioning, visibility, fading, touch handling, HBM, and authentication remain owned by OOS, and the replacement yields when the COUI fingerprint drawable is active.
- Limit the module AOD notification row to five application icons and show the remaining drawable, deduplicated notifications as `+X`. The persistent lockscreen-to-AOD handoff row uses the same display plan.

### Changed
- Refresh the fingerprint visual immediately when its setting changes, without requiring a SystemUI restart. The setting remains disabled by default.

## [0.1.212] - 2026-07-19
### Fixed
- Restore only module-hidden ancestors of the persistent OPlus ClockPlugin host before presenting a lockscreen or AOD scene. A cold SystemUI start can no longer leave `CustomOplusKeyguardStyleClock` visible but fully transparent after the module host is attached.
- Require the complete persistent-host ancestor chain to be attached, visible, and non-transparent before removing the legacy clock overlays. The existing OOS panel blank behavior is unchanged.

### Diagnostics
- Log restored ClockPlugin ancestors and the exact node that defers persistent-host validation.

## [0.1.211] - 2026-07-19
### Fixed
- Start the passive FOD suppression window only on a real OOS proximity `near -> far` transition. Repeated `getProxNear() == false` polls no longer suppress legitimate fingerprint recovery after the proximity sensor is uncovered.

### Diagnostics
- Log the OOS proximity suppression edge that starts or clears the passive FOD window.

## [0.1.210] - 2026-07-19
### Fixed
- Keep each clock glyph on its lockscreen-weight advance during the visible lockscreen-to-AOD weight animation, preventing the digits and colon from shifting as Google Sans Flex changes weight.
- Mirror the already-filtered AOD notification icons on the persistent lockscreen handoff layer, so they are visible before the existing OOS panel blank and transfer with the AOD layer afterward.
- Drop Android synthetic autogroup summary carriers before icon deduplication, allowing the real hotspot notification to use its native system glyph.
- Preserve the original color of a notification-provided launcher resource smallIcon instead of tinting it into a solid shape, including the OPlus Weather notification.

## [0.1.209] - 2026-07-19
### Fixed
- Start the visible persistent lockscreen clock's 300-to-AOD weight animation as soon as OPlus reports its early AOD render state, while retaining the lockscreen scene until the module lifecycle is ready.
- Continue the hidden AOD layer from the visible layer's actual handoff weight and avoid restarting an in-flight transition, so the configured AOD weight is reached before the existing panel blank instead of jumping after it.
- Restore the lockscreen weight if an early AOD entry is cancelled. The OOS panel blank timing is unchanged.

## [0.1.208] - 2026-07-19
### Fixed
- Preserve the committed ClockPlugin scene while OPlus reports an early AOD render state before the module lifecycle is ready, including while the display is still interactive. This prevents the transient state from hiding both persistent clock layers.

## [0.1.207] - 2026-07-19
### Fixed
- Keep the lockscreen clock visible through the complete 300-to-AOD-weight animation while the AOD layer transitions invisibly to its final weight.
- Start the existing layer crossfade only after both clock layers reach the configured AOD weight, preventing an intermediate visible AOD 300-weight frame.

## [0.1.206] - 2026-07-19
### Fixed
- Start the AOD child and visible lockscreen child weight animations together with the persistent-host crossfade. The visible clock now transitions continuously from the configured lockscreen weight to the configured AOD weight instead of completing behind a transparent AOD layer.
- Keep compact-clock letter spacing constant through every font-weight frame, eliminating the weight-dependent spacing drift after the AOD handoff.

### Diagnostics
- Log the prepare, start, finish, and cancellation states of the persistent lockscreen-to-AOD weight handoff.

## [0.1.205] - 2026-07-19
### Fixed
- Prewarm the bundled Google Sans Flex file and both configured clock weights before the OPlus ClockPlugin creates any module clock views.
- Build both base and weighted clock Typefaces through the same file-backed `Typeface.Builder` path. The module no longer selects OOS `AndroidClock.ttf` as its clock fallback during AOD entry.

## [0.1.204] - 2026-07-19
### Fixed
- Keep the prebuilt weighted Google Sans Flex Typeface as the only clock weight source during the lockscreen-to-AOD transition. OOS no longer receives a second TextView font-variation mutation that can briefly replace the file font with a system fallback.
- Reapply the final AOD weight Typeface after the animator ends, including when its final frame already reached the target weight.

## [0.1.203] - 2026-07-19
### Fixed
- Do not alpha-suppress an OPlus ClockPlugin view when it is the persistent module host, contains that host, or is contained by it. This prevents an opaque vendor `getView(int)` slot from blacking out the complete module clock surface.

### Diagnostics
- Log the class, ID, alpha, and parent of each rejected unsafe ClockPlugin native-visual candidate.

## [0.1.202] - 2026-07-18
### Fixed
- Preserve a committed persistent AOD scene through OPlus's `lifecycle-not-ready` gap when the module display policy still allows AOD. This prevents a transient policy callback from collapsing the host to `HIDDEN`.
- Recreate the lockscreen scene when OPlus reports animated `uiState=1`, even if a previous transient AOD callback hid the persistent host.

## [0.1.201] - 2026-07-18
### Fixed
- Read OPlus ClockPlugin's `UiState.isAnim` flag. An animated transient `uiState=1` no longer hides an already-visible persistent lockscreen host; only a settled unlocked state may hide it. The value is included in ClockPlugin host-sync diagnostics for verification.

## [0.1.200] - 2026-07-18
### Fixed
- Revert the experimental persistent-host per-frame lockscreen timestamp update from 0.1.199. On this OOS build it could leave both persistent clock layers invisible after the early lockscreen-to-AOD handoff. The previous stable host visibility behavior is restored while retaining the Google Sans preparation fix.

## [0.1.199] - 2026-07-18
### Fixed
- Keep the persistent ClockPlugin lockscreen layer's interactive-visible timestamp fresh while it is drawn. Lockscreen screen-off is now classified as a lockscreen-to-AOD handoff instead of a delayed non-lockscreen reveal, so the AOD weight transition begins before the OOS panel blank rather than jumping after it.

## [0.1.198] - 2026-07-18
### Fixed
- Keep the persistent ClockPlugin AOD child on its prepared Google Sans weight transition during the lockscreen-layer crossfade. This prevents the transition from being skipped and avoids exposing a stale/default-font AOD frame before the final AOD weight is applied.

## [0.1.197] - 2026-07-18
### Fixed
- Do not apply the legacy stock-clock draw, alpha, visibility, or probe suppression to an OPlus `ClockViewRoot` that contains the persistent module host. The previous experimental build could make the host's visible child layers unrenderable by hiding their parent container.

## [0.1.196] - 2026-07-18
### Changed
- Move the module clock handoff onto one persistent host attached to OPlus `ClockPlugin#getView(0)`. The host keeps its root attached while internal lockscreen and AOD layers transition, instead of handing off between separate `NotificationShadeWindowView` overlays.

### Fixed
- Keep the already-rendered lockscreen scene in place when ClockPlugin reports AOD one frame before the module lifecycle policy is ready, preventing an intentional pre-AOD hide from creating a visible first-frame gap.
- After the persistent host has drawn and validated, block legacy overlay injection, delayed reapply, and panel-handoff visibility mutations from competing with the new host. Native OOS clock suppression remains active.

## [Unreleased]
### Deferred
- Silent notifications can still briefly flash during the OOS lockscreen-to-AOD transition when the affected silent channel also has lockscreen display permission enabled. This is not fixed yet; current workaround is to disable lockscreen display permission for those silent notification channels. The unfinished experimental row/card suppression code is parked in git stash `wip: defer silent notification flash experiment`.

## [0.1.186] - 2026-07-17
### Fixed
- Treat an OOS passive proximity-far callback as a short FOD suppression session, covering delayed fingerprint show callbacks rather than only the first 250ms after the sensor query.
- Cover OOS fingerprint visibility setters in addition to the direct show APIs, and request a FOD hide when a passive show is suppressed so an already-created fingerprint window cannot remain visible.

## [0.1.185] - 2026-07-17
### Fixed
- Use OOS's confirmed proximity state instead of a module-owned raw sensor listener, preventing noisy `0.0/5.0` samples from repeatedly hiding AOD and recreating the fingerprint icon while the device is idle.
- Suppress steady-AOD fingerprint re-show requests caused only by passive proximity-far callbacks, while preserving initial entry and recent tap/pickup-triggered shows.

### Changed
- Extend the unavoidable OOS panel handoff blank into one guarded 520 ms presentation blackout, then refresh and reveal the module AOD once on the next animation frame without changing brightness or the final Doze power state.
- Keep the lockscreen-to-AOD weight animation running behind the presentation gate instead of hiding the AOD view and cancelling its animator.

### Diagnostics
- Add trace- and generation-guarded panel handoff logs and unit tests for duplicate events, cancellation, stale callbacks, and single-reveal behavior.

## [0.1.183] - 2026-07-14
### Fixed
- Suppress the OOS stock AOD media subtree while the module AOD is active, instead of preserving the native media card alongside the module media row.
- Recognize the SystemUI Do Not Disturb notice as an AOD-visible system notification so AOD and lockscreen clock modes remain consistent.
- Force a fresh lockscreen notification-card scan on the first visible frame before choosing the compact or large clock layout, preventing the clock from jumping after AOD exit.

## [0.1.180] - 2026-07-13
### Fixed
- Hide the OOS AOD battery and notification status views immediately when the real plugin AOD host arrives in Trigger-only mode, instead of waiting for the 1800ms delayed suppression pass.

## [0.1.179] - 2026-07-13
### Fixed
- Apply stock AOD suppression immediately while Trigger-only AOD is briefly visible, instead of treating its `aodActive=false` state as permission to show OOS battery and notification icon views.

## [0.1.178] - 2026-07-07
### Fixed
- Treat non-lockscreen screen-off entry as a delayed Pixel AOD reveal using the last real interactive lockscreen visibility, not the stale OOS lockscreen host state observed after dreaming has already started.
- Keep native doze alive and stock AOD suppressed during the delayed reveal gate while preventing the module overlay from marking itself as already visible.

## [0.1.177] - 2026-07-07
### Changed
- Delay Pixel AOD overlay reveal for screen-off transitions that start outside the lockscreen surface, keeping stock AOD suppression active while waiting for the unavoidable black frame to pass before drawing the module AOD.

## [0.1.176] - 2026-07-07
### Fixed
- Restore the colorful app-icon fallback only for OPlus / Heytap push bitmap notification carriers whose small icon renders as a filled mask, so Taobao-style push notifications no longer become white blocks while normal resource small icons such as Bybit stay on the smallIcon / monochrome path.

## [0.1.175] - 2026-07-07
### Fixed
- Keep the AOD media idle timeout anchored to the first paused / idle state in the current non-playing cycle, so later player state updates such as `PAUSED` -> `NONE` do not restart the 10-minute grace window.

## [0.1.174] - 2026-07-06
### Changed
- Move the two-line large AOD clock down from `144dp` to `184dp` so it sits closer to the visual center between the date row and the fingerprint / bottom status area.

## [0.1.173] - 2026-07-06
### Fixed
- Treat SystemUI Torch / Flash Light `id=10011` as an OOS Live Alert even when the carrier extras are missing, keeping it on the Live Alert glyph and dedupe path instead of the generic SystemUI notification path.
- Request a guarded native OOS AOD frame kick after delayed Torch / Live Alert notification refreshes, so DOZE/DOZE_SUSPEND can repaint the icon row without waiting for the next tap, minute tick, or other native AOD event.

## [0.1.172] - 2026-07-06
### Fixed
- Refresh AOD notification icons from Android torch state changes, OOS flashlight action broadcasts, OOS black-screen gesture callbacks, and Torch notification cache changes, so the Flash Light Live Alert icon can show or disappear without waiting for a tap, minute tick, or other AOD refresh event.

## [0.1.171] - 2026-07-06
### Fixed
- Use the AOSP flashlight quick-settings vector as the fallback AOD icon for OOS Flash Light / Torch Live Alerts, replacing the rough hand-drawn fallback while keeping native SystemUI resources preferred when available.

## [0.1.170] - 2026-07-06
### Fixed
- Let OOS Live Alerts use distinct AOD notification icon dedupe keys, so SystemUI Flash Light / Torch and USB notifications do not suppress each other just because both come from `com.android.systemui`.
- Render OOS Timer and Flash Light Live Alert carriers with stable monochrome AOD glyph fallbacks instead of tinting filled notification masks into circular white blocks.

## [0.1.169] - 2026-07-06
### Fixed
- Allow OOS Live Alerts / Fluid Cloud carrier notifications, such as Timer, to contribute their notification `smallIcon` to the module AOD icon row even when their ranking importance is LOW, without relaxing the normal silent-notification filtering policy.

## [0.1.168] - 2026-07-06
### Fixed
- Keep third-party AOD notification icons on the notification `smallIcon` or app monochrome path; filled-mask detection no longer falls back to the colorful launcher icon when a usable `smallIcon` exists.

## [0.1.167] - 2026-07-06
### Fixed
- Refresh AOD clock, date, notification icons, and media content before making the module overlay visible again after proximity / pocket restore, preventing the first visible frame from showing the stale pre-hide time.

## [0.1.166] - 2026-07-06
### Fixed
- Request a guarded native OOS AOD refresh kick when the module media row text changes or clears, so DOZE/DOZE_SUSPEND does not wait for the next minute tick before showing updated media information.

## [0.1.165] - 2026-07-06
### Fixed
- Treat media metadata and media notification content changes as fresh media activity so AOD media text updates promptly after switching tracks, even when the player reports an idle or none playback state.
### Diagnostics
- Add hash-based AOD media line and media notification cache logs for future latency debugging without writing raw song titles to logs.

## [0.1.164] - 2026-07-05
### Diagnostics
- Add visual profile revision and runtime display metrics to AOD / lockscreen init logs so future visual parity changes can be compared across density and font-scale environments.

## [0.1.163] - 2026-07-05
### Internal
- Move date, media, battery, and charge-bolt sizing into the centralized visual style profile while preserving the current rendered dimensions.

## [0.1.162] - 2026-07-05
### Internal
- Centralize AOD / lockscreen clock, info, and media alpha values in the visual style profile without changing their rendered values.

## [0.1.161] - 2026-07-05
### Diagnostics
- Add runtime visual profile logging for AOD and lockscreen clock initialization, covering current typography, spacing, icon, burn-in, and weight values without changing display behavior.

## [0.1.160] - 2026-07-05
### UI
- Start Phase 6.0 by centralizing Pixel AOD / lockscreen visual style constants without changing lifecycle behavior.
- Make the dark-mode startup splash explicitly use the same adaptive launcher icon as light mode, so the splash icon can follow the system icon shape mask.

## [0.1.159] - 2026-07-05
### Diagnostics
- Reframe notification pulse policy diagnostics around OOS native pulse coexistence instead of future custom module pulse triggering.
- Strengthen `MODE=pulse` so native notification pulse samples report whether module brief display was incorrectly started during the audit window.
- Add `scripts/diagnose_aod_smoke_suite.sh` to run a compact AOD smoke suite covering screen-off entry and native notification pulse coexistence.

## [0.1.158] - 2026-07-05
### Internal
- Add a notification pulse policy adapter that classifies native pulse observations as native-compatible, observe-only, lockscreen/AOD-filtered, or sensor/power-blocked without changing AOD display behavior.
- Include notification pulse policy fields in AOD lifecycle state snapshots so OOS native pulse coexistence can be audited with explicit policy evidence.
- Extend the pulse diagnostic summary with notification pulse policy counters.

## [0.1.157] - 2026-07-05
### Diagnostics
- Add `MODE=pulse` to `scripts/diagnose_aod_trigger_loop.sh` so native notification-pulse behavior can be sampled by entering AOD, posting the module test notification, and correlating pulse observations with display state and AOD lifecycle phase logs.
- Extend the diagnostic summary with pulse post / clear markers, skipped test-notification count, `displayState=DOZE/OFF`, and `phase=aod-visible/entering-aod` counters.
- Document the notification pulse sampler while keeping runtime AOD behavior unchanged.

## [0.1.156] - 2026-07-05
### Internal
- Correlate notification pulse candidates with the current AOD trace by recording the latest pulse-candidate rule, source, trace, package summary, and age in AOD lifecycle state snapshots.
- Extend diagnostics to count `notificationPulseRecent=true` in the current run window.

## [0.1.155] - 2026-07-05
### Internal
- Start Phase 5.4 by recording notification snapshot, ranking, posted, removed, and cleared events as explicit native notification-pulse observations.
- Preserve existing notification filtering and AOD display behavior; this build only improves logs for deciding whether a future custom notification pulse is safe.
- Extend diagnostics to count notification-pulse candidate, filtered, clear, and ranking observation categories.

## [0.1.154] - 2026-07-05
### Internal
- Start Phase 5.3 by routing power-saver, low-battery, charging, and unknown battery state checks through an explicit OOS power-policy decision model.
- Add stable `OOS AOD power policy mapping` logs with `reason`, `category`, `futureAction`, battery state, and threshold fields.
- Extend trigger diagnostics to count power-policy mapping categories in the current run window.

## [0.1.153] - 2026-07-05
### Internal
- Add explicit OOS trigger / sensor mapping rules so pickup, tap, proximity, pocket, generic sensor, and unknown trigger events log stable `rule` and `category` fields.
- Document the current trigger mapping and priority model in `docs/OOS_TRIGGER_MAPPING.md`.
- Extend the trigger diagnostic script to count mapped trigger categories and key trigger rules from the current run window only, while keeping LSPosed module logs as auxiliary evidence.

## [0.1.152] - 2026-07-05
### Experimental Fixes
- Try a narrower native-timeout path for Continuous AOD: when OOS attempts to hide the whole native AOD for the fingerprint timeout callback, invoke the captured FOD-only hide method first and suppress the broader native AOD hide if that succeeds.
- Extend the black-frame diagnostic script to report FOD-only suppression and whether `AodData-->setAodIsInShow:false` still appears afterward.

## [0.1.151] - 2026-07-05
### Diagnostics
- Add targeted FOD / UDFPS AOD diagnostics around OOS fingerprint icon show/hide callbacks so the remaining AOD entry black-frame window can be correlated with native fingerprint timeout behavior.
- Add `scripts/diagnose_aod_black_frame.sh` to capture logcat plus LSPosed module logs and summarize native hide callbacks, `AodData` hide signals, SurfaceFlinger power-mode transitions, and Pixel overlay visibility decisions.

## [0.1.150] - 2026-07-05
### Bug Fixes
- Reassert the Pixel AOD overlay after OOS native fingerprint / timeout hide callbacks complete, so Continuous AOD does not disappear together with the fingerprint affordance.
- Keep native timeout reassertion trace-guarded and proximity-aware so Trigger-only, outside-schedule, interactive, and pocket/near-sensor paths are not accidentally kept alive.
- Rewrite OOS `DreamService#setDozeScreenState(OFF)` to `DOZE` only while Continuous AOD policy is actively keeping native Doze alive, reducing the entry / timeout black-frame path without blocking fingerprint fadeout callbacks.
- Extend the Continuous AOD diagnostic script to report native-timeout reassert coverage and OOS Doze screen-state OFF events.

## [0.1.149] - 2026-07-05
### Bug Fixes
- Let OOS `notifyHideCallback` run during Continuous AOD so native fingerprint / short-wake timeout callbacks can self-dismiss normally, while keeping module AOD lifecycle decisions active.
- Stop treating proximity-near expected hiding as a black-frame diagnostic failure in the Continuous AOD diagnostic script.

## [0.1.148] - 2026-07-05
### Bug Fixes
- Keep Continuous AOD active for the whole non-interactive AOD session inside schedule, instead of letting the lifecycle fall back to `lifecycle-not-ready` after the short entry/recent-overlay window.
- Suppress OOS energy-saving native hide callbacks whenever Continuous AOD is actively keeping native Doze alive, so the Pixel overlay is not hidden about one second after screen-off.

## [0.1.147] - 2026-07-05
### Internal
- Add `PixelAodRenderModel` and route AOD / lockscreen clock-date rendering through it, keeping the existing visibility, media, notification, and transition policies unchanged.

## [0.1.146] - 2026-07-05
### Removed
- Remove the ineffective `Skip AOD black frame` advanced option and its old doze screen-state rewrite hook, so stale enabled preferences no longer install that compatibility path.

## [0.1.145] - 2026-07-05
### Internal
- Add `PixelAodSettingsSchema` to centralize setting keys, defaults, always-enabled flags, and SystemUI restart metadata.
- Refactor `PixelAodSettingsProvider` and the settings UI to read defaults from the shared schema instead of duplicating fallback values.

## [0.1.144] - 2026-07-05
### Internal
- Add `AodNotificationPipeline` and move AOD notification visibility filtering, ranking snapshots, lockscreen visibility decisions, media candidate detection, notification signatures, and system notification classification out of `PixelAodClockView`.
- Reuse the shared notification pipeline silent-notification policy from `PixelAodHook` so lockscreen and AOD policy logic stay aligned.

## [0.1.143] - 2026-07-05
### Internal
- Move the final AOD policy decision output into `OosAodLifecycleAdapter`, keeping behavior the same while making Pixel overlay, native Doze keepalive, stock AOD suppression, and native hide callback decisions come from one lifecycle policy module.
- Add `StockAodVisibilityController` to own stock view hidden-state tracking, adjusted status-view restoration, delayed stock suppression reapply trace guards, and delayed transition restore trace guards.

## [0.1.142] - 2026-07-05
### Bug Fixes
- Prevent proximity / pocket / sensor diagnostics such as `getProxNear() result=false` from starting `Trigger-only` native short-wake AOD; those events now remain sensor guard release / diagnostic signals instead of briefly showing Pixel AOD.

## [0.1.141] - 2026-07-05
### Bug Fixes
- Stop treating plain screen-off / AOD host-ready as a trigger-only brief display source; outside-schedule and `Trigger-only` AOD now wait for real native short-wake triggers such as tap or pickup.
- Let native OOS hide callbacks run during trigger-only brief windows instead of keeping native Doze alive, so fingerprint affordances can time out normally.
- Re-apply stable AOD clock weight during visible brief refreshes to prevent lockscreen transition weight from sticking on the Pixel AOD overlay.

## [0.1.140] - 2026-07-05
### Bug Fixes
- Move `Continuous Display Schedule` directly under `AOD Behavior` so it is visually scoped to `Continuous + Trigger`, not `Lockscreen Policy`.
- Start the brief trigger window at screen-off when `Trigger-only` or outside-schedule `Continuous + Trigger` mode is active, avoiding the delayed native short-wake black gap.
- Keep native Doze and suppress OOS native hide callbacks only for the active brief trigger window, then release them when the window expires.
- Change native short-wake de-duplication from once per AOD trace to once per native trigger event so later tap/lift short-wake events can show AOD again in the same sleep session.

## [0.1.139] - 2026-07-05
### Bug Fixes
- Rename the AOD settings hierarchy to `AOD Behavior` plus `Continuous Display Schedule` so schedule only controls continuous display, not trigger behavior.
- Hide the continuous schedule controls while `Trigger-only` behavior is selected to remove the mode-vs-schedule priority ambiguity.
- Prevent trigger-only and outside-schedule brief displays from marking Pixel AOD as continuously active.
- Prevent native short-wake triggers from being recreated repeatedly in the same AOD trace after the brief window expires.
- Restrict Doze keepalive, screen-state rewrite, and OOS native hide suppression to continuous AOD only so trigger-only display can end naturally.
- Apply stable AOD clock weight for brief trigger windows instead of running the lockscreen-to-continuous-AOD weight transition.

## [0.1.138] - 2026-07-05
### Features
- Redo Phase 5.1 AOD display modes as `Continuous AOD` and `Trigger-only AOD`.
- Let `Continuous AOD` display continuously inside the schedule while still allowing native short-wake triggers outside the schedule.
- Let `Trigger-only AOD` skip continuous scheduled display and show only during native short-wake / pickup / tap style trigger windows.
- Replace the ineffective `Custom AOD` and `Lockscreen Clock` settings cards with a real module master switch; disabling it and restarting SystemUI prevents module hooks from being installed.

### Bug Fixes
- Treat OOS native `DOZE` short-wake entry as a trigger source instead of waiting only for explicit pickup/tap method names.
- Keep proximity/pocket policy active during brief trigger windows and cancel the brief window when proximity reports near.
- Suppress stock OOS AOD during module-managed brief trigger windows to avoid stock AOD flashing over the module AOD.

## [0.1.137] - 2026-07-05
### Features
- Implement Phase 5.1 trigger-only brief Pixel AOD display for native OOS pickup and tap triggers while keeping it separate from continuous scheduled AOD.
- Block or cancel trigger-only brief display when proximity-near or pocket triggers are reported, and keep battery saver / low-battery policy enforced.
- Add trigger brief window state to AOD snapshots so logs show whether a brief trigger is active, its source, age, and remaining time.

## [0.1.136] - 2026-07-05
### Diagnostics
- Classify native OOS pickup, tap, proximity, pocket, and sensor triggers into display modes for Phase 5 trigger work without changing AOD behavior.
- Record trigger-only brief display candidates separately from continuous scheduled AOD, and mark all new trigger mappings as observe-only.

## [0.1.135] - 2026-07-04
### Internal
- Centralize AOD lifecycle, module policy, stock AOD suppression, and native hide callback choices into a shared policy decision object with explicit logs for each output.
- Route AOD overlay drawing, Doze keepalive, OPlus energy-saving hide guard, display-state rewrite, host ready, delayed reapply, and restore guard checks through the shared decision layer without intentionally changing behavior.

## [0.1.134] - 2026-07-04
### Bug Fixes
- Keep suppressing stock OOS AOD views when the module AOD schedule blocks Pixel AOD display, avoiding a brief stock AOD flash on screen-off.
- Prevent `AodRecord#onDreamingStarted` from marking Pixel AOD active/recent-visible outside the module schedule or power policy, while still allowing native OOS hide callbacks to dismiss fingerprint affordances.

## [0.1.133] - 2026-07-04
### Bug Fixes
- Stop module AOD host/reapply paths from hiding stock AOD views or reasserting Pixel AOD while the module schedule or power policy blocks display.
- Allow OOS energy-saving AOD hide callbacks to run outside the module AOD policy window so native fingerprint / short-wake affordances can time out normally.

## [0.1.132] - 2026-07-04
### Bug Fixes
- Stop the module from keeping OOS Doze screen state alive or rewriting OFF requests while the module AOD schedule or power policy says the Pixel AOD overlay should not display.
- Fix the schedule-outside case where module AOD stayed hidden correctly but OOS short wake / fingerprint affordance could fail to disappear because OFF requests were still rewritten to DOZE.

## [0.1.131] - 2026-07-04
### Diagnostics
- Add native-style AOD trigger diagnostics for OPlus wake-up controller methods related to pickup, tap, proximity, pocket, and sensors without changing trigger behavior.
- Record the latest native trigger type, source, detail, and age in AOD lifecycle state snapshots so real OOS trigger events can be mapped before adding module-owned sensor logic.

## [0.1.130] - 2026-07-04
### Power
- Align module AOD visibility with native-style power policy by hiding the overlay while system battery saver is active.
- Hide module AOD when the device is on low battery and not charging, with explicit debug-log reasons for power-save and low-battery decisions.

## [0.1.129] - 2026-07-04
### UI
- Remove the Clock Scale slider from module settings and keep AOD clock text at the Pixel-style default 1.0 scale.

## [0.1.128] - 2026-07-04
### Visual
- Replace the custom burn-in drift periods with AOSP-style 83/521 minute Pixel-like offset periods while keeping the current low-power native-tick refresh path.
- Keep the 8-second AOD entry settle window so the clock group does not visibly jump during the OOS lockscreen-to-AOD transition.

## [0.1.127] - 2026-07-04
### Maintenance
- Route the core Pixel AOD draw, Doze keepalive, and lockscreen-to-AOD bridge decisions through the OOS lifecycle adapter while preserving the existing boolean behavior.

## [0.1.126] - 2026-07-04
### Maintenance
- Route existing AOD trace guard checks for known-host refresh, delayed stock AOD suppression, and delayed stock view restore through the OOS lifecycle adapter without changing visual behavior.

## [0.1.125] - 2026-07-04
### Diagnostics
- Add an observability-only OOS AOD lifecycle adapter that classifies hook sources such as dreaming start/stop, screen on/off, display-state requests, host readiness, and native ticks against the current `AodLifecycleState.phase()`.
- Record the 19:51 rapid lockscreen / AOD switching evidence from LSPosed persistent module logs in the lifecycle mapping document.
- Add a local `tools/extract_pixelaod_logs.ps1` helper that extracts Pixel AOD logs from both current `adb logcat` and `/data/adb/lspd/log/modules_*.log` for a requested time window.

## [0.1.124] - 2026-07-04
### Diagnostics
- Add lightweight AOD lifecycle phase-change logs that record source, previous/current phase, trace id, display state, active flag, and timing snapshot only when `AodLifecycleState.phase()` changes.
- Document the phase-change log format and the next required live captures for AOD entry and exit mapping.

## [0.1.123] - 2026-07-04
### Bug Fixes
- Add a centralized AOD lifecycle snapshot used by AOD visibility, Doze screen keepalive, and lockscreen-to-AOD bridge decisions, keeping the current behavior while making transition state easier to reason about from logs.
- Guard the delayed stock AOD suppression reapply task with the originating AOD trace so an old transition cannot hide stock views during a newer AOD session.

## [0.1.122] - 2026-07-02
### Bug Fixes
- Drive module AOD time updates from SystemUI's native OOS AOD refresh callbacks, with a short entry refresh and time-change broadcast fallback while the module AOD view is active.
- Guard OOS energy-saving AOD hide callbacks during the AOD entry window so the custom AOD overlay is reasserted instead of disappearing or causing SystemUI-like restart behavior.

## [0.1.121] - 2026-07-02
### Bug Fixes
- Drive module AOD clock refresh from SystemUI's system `ACTION_TIME_TICK` broadcast while AOD is running, so the clock keeps advancing even when the OOS `DozeUi` native tick hook is not available.
- Keep the custom `Handler.postDelayed` minute ticker removed; this update uses the platform broadcast already delivered to SystemUI instead of adding a module-owned self-loop.
- Remove the remaining global debug-log rate limit so `debug_logging=true` no longer drops transition or time-refresh evidence during dense AOD traces.

## [0.1.120] - 2026-07-02
### Bug Fixes
- Keep supported system notifications such as the USB and tether/network-status entries on module AOD even when OOS marks them as silent or `LOW`, by letting the module's system-notification allow-path run before the lockscreen-policy silent filter.
- Restore lockscreen-policy hiding for silent third-party notifications only on the real lockscreen path, so notifications like Link to Windows no longer stay visible on OOS lockscreen while the unlocked notification shade still keeps its normal silent section.

## [0.1.119] - 2026-07-01
### Bug Fixes
- Bind the AOSP-style silent-notification filtering to the `Lockscreen Policy` setting instead of forcing a global SystemUI hide. With the toggle enabled, silent or low-importance notifications are filtered only from the lockscreen/AOD path; with it disabled, the module no longer alters OOS silent-notification visibility.
- Stop forcing `shouldHideNotification` / `shouldFilterOut` to hide silent notifications globally. This restores FlyClash-style silent notification groups in the unlocked notification center while keeping the lockscreen-policy override that preserves lockscreen notifications across unlock/relock cycles.

## [0.1.118] - 2026-07-01
### Bug Fixes
- Make silent notifications follow AOSP semantics on both OOS lockscreen and module AOD: if a notification is marked `FLAG_SILENT` or its ranking importance is `LOW` or below, the module now force-hides it from the lockscreen visibility path and filters it from AOD as well.
- Remove the temporary third-party aggregate-summary special case and replace it with the general silent-notification rule, so grouped summaries like FlyClash's auto-group notification no longer leak onto AOD when the underlying notification channel is silent.

## [0.1.117] - 2026-07-01
### Bug Fixes
- Make the `Debug Logging` setting push changes through the settings provider and notify the hooked SystemUI process immediately, so toggling the switch refreshes module settings without waiting for the cache TTL or a later opportunistic reload.
- Unify AOD notification visibility with the lockscreen visibility decision path: AOD now consumes the same keyguard/provider filter results that decide whether a notification can appear on the lockscreen, instead of applying a separate low-importance/silent heuristic. This fixes FlyClash-style cases where a notification could leak onto AOD while still being hidden on the lockscreen.

## [0.1.116] - 2026-06-28
### Bug Fixes
- Hide the charging indicator icon on the AOD battery status once the battery is fully charged, even if the charger remains plugged in.
## [0.1.115] - 2026-06-28
### Bug Fixes
- Fix JSON parsing crash in BreezyWeatherRelayReceiver that skipped parsing if the root was a JSONObject.
- Fix AOD being stuck in night mode after sunset by converting timestamps to time-of-day before comparison, preventing expiration.
- Fix time unit mismatch in Breezy Weather intents by properly scaling second-based timestamps to milliseconds.
- Support system dark mode on module startup splash screen.

### UI
- Replace the buggy, manual Canvas-based `ClockDialPicker` with Google's official Material 3 `TimePicker`, fixing massive GC thrashing and frame drops.
## [0.1.114] - 2026-06-27
### UI
- Replace the stock Android `TimePickerDialog` for "Start Time" / "End Time" with a custom clock-dial (表盘) picker. The user drags on the circular face to pick hours (outer ring, 1–12) or minutes (inner ring, 0–59), with Hour/Minute and AM/PM toggle chips above the dial.

## [0.1.113] - 2026-06-27
### Bug Fixes
- Use Breezy Weather's actual sunrise/sunset times for day/night icon selection instead of a naive `hour < 6 || hour >= 18` check. The previous behavior incorrectly showed the night icon at 18:01 in summer (sunset ~19:30). The relay now extracts `sunRise`/`sunSet` (camelCase) and `sunrise`/`sunset` (lowercase) from the Breezy Weather JSON, stores them in SharedPreferences, and passes them through the relay broadcast as `sunrise_millis`/`sunset_millis` extras. Falls back to the hour check when Breezy Weather hasn't published sun-times.

## [0.1.112] - 2026-06-27
### Bug Fixes
- Make the status bar and navigation bar icons in the Settings screen adapt to the system theme: enable edge-to-edge layout and flip `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` based on the current `UI_MODE_NIGHT_MASK`. Previously the icons stayed light on top of `Theme.Material.Light`, making them invisible on a white surface.

## [0.1.111] - 2026-06-22
### UI
- Remove the Pocket Mode and Notification Icons toggles from the settings screen and treat both behaviors as built-in defaults instead of optional switches.

### Bug Fixes
- Force both Pocket Mode and monochrome notification icons to remain enabled at runtime, and automatically normalize old saved `false` values back to `true` so legacy preferences no longer disable those features.

## [0.1.110] - 2026-06-22
### Bug Fixes
- Force custom AOD views to refresh their frame after time, notification, weather, and media updates so stale minute text or delayed notification icons are less likely to remain until the next wake cycle.
- Keep the display in live doze instead of suspended doze while the custom AOD needs active frames, reducing missed redraws on OxygenOS AOD.
- Expire paused or idle media sessions after 10 minutes and clear them immediately when the media session is destroyed or playback stops, matching the expected AOD media timeout behavior.

## [0.1.109] - 2026-06-20
### UI
- Rework the launcher icon assets into a stable adaptive icon set: keep the rebuilt full icon as the adaptive foreground, switch the adaptive background to transparent to avoid a doubled card effect on OOS, and temporarily remove the broken monochrome layer export so the launcher icon renders correctly instead of collapsing into a washed-out bar.

### Bug Fixes
- Keep stock AOD suppression-miss logging debug-only and deduplicated, refresh known AOD hosts without reinjecting the whole tree, and narrow the media classifier so ordinary ongoing notifications stay on the normal icon path.
- Use the native AOSP tether Wi-Fi drawable for the system network fallback instead of the custom multi-name lookup.

## [0.1.108] - 2026-06-18
### Bug Fixes
- Guard the delayed stock AOD/keyguard restore with the originating AOD trace and refresh known host visibility after AOD activation, preventing an old transition from resurrecting stock AOD views into a new cycle.

## [0.1.107] - 2026-06-18
### Diagnostics
- Remove the 60-per-minute debug log throttle and keep full AOD / lockscreen decision traces, including trace ids, state snapshots, transition reasons, and notification rebuild decisions.

## [0.1.106] - 2026-06-18
### Bug Fixes
- Replace the self-drawn USB and tether/hotspot notification icons with native AOSP system drawables loaded from framework resources.
- Remove the self-drawn module update glyph and keep module-package notifications logged when they are filtered out of lockscreen/AOD visibility.

## [0.1.105] - 2026-06-18
### Bug Fixes
- Third-party AOD notifications without a monochrome icon now fall back to the application's launcher icon instead of tinting the raw small icon into a white block.

## [0.1.104] - 2026-06-18
### Bug Fixes
- Fallback third-party push notifications to the original tinted small icon on AOD when the app does not provide a monochrome adaptive icon, so blocky/tiny icons such as Taobao no longer disappear from AOD.

## [0.1.103] - 2026-06-18
### Diagnostics
- Added low-noise AOD suppression trace logs for entry-state snapshots, stock hide passes, and transition restore decisions to help diagnose intermittent system AOD overlap.

## [0.1.101] - 2026-06-17
### Bug Fixes
- **Fix AOD and Lockscreen Clock Overlap**: Addressed an issue where the stock Lockscreen/AOD clock could overlap with the custom module clock (e.g. at 07:35 or outside the AOD schedule). Added the correct Lockscreen container candidates to the stock clock draw suppression hook and removed the early `isDeviceInteractive` bailout that was incorrectly skipping the draw suppression hook on the Lockscreen.

## [0.1.100] - 2026-06-17
### Bug Fixes
- **Fix AOD media info disappearing on pause**: The media row used to hide the moment playback left the PLAYING state and never reset its dedupe cache, so resuming the same track was skipped as "unchanged" and the row stayed hidden until the player was swiped away and reopened. Media visibility is now driven by whether the session still has a displayable track (any state except STOPPED/ERROR), and the cache is reset when the row is cleared. Verified on-device across play → pause → resume (PixelPlay reports both PAUSED and NONE on pause; both are now kept).
### UI
- **Settings app redesign (Material 3 / Expressive)**: Grouped cards (Appearance / Clock / Behavior / Advanced), larger rounded corners, dynamic color.
- **Follow system light/dark theme** instead of a hardcoded light theme.
- **Language switch (Follow system / 中文 / English)**, defaulting to the system language, applied via `attachBaseContext`.
- Moved the "Restart SystemUI" action from a bottom button to a small restart icon in the top app bar.
### Build
- Bump Android Gradle Plugin to 8.6.0 and Compose BOM to 2026.05.01 (Material3 1.4.0), required by the Expressive components.

## [0.1.99f] - 2026-06-16
### Stability
- Disable global `View#setVisibility` / `View#setAlpha` stock-clock hooks by default to avoid SystemUI-wide hot-path interception.
- Move custom AOD visibility enforcement out of `dispatchDraw()` and cache schedule checks to reduce per-frame work.
- Guard proximity listener registration so pocket mode does not repeatedly register the same listener.
- Keep test notifications, but remove the experimental broadcast-based settings mutation path.
- Sync modern Xposed `module.prop` with the Gradle app version.

## [0.1.99e] - 2026-06-16
### Features
- **AOD Display Schedule Mode**: Added a custom scheduling option (Start Time ~ End Time) for the custom AOD. When enabled, the AOD clock and widgets will only render during the user-configured time range (supporting ranges spanning midnight). Outside this schedule, the custom AOD layout is hidden, and the stock clock remains suppressed to keep the screen completely black.
### Bug Fixes
- **Settings Synchronization Fix**: Expose missing keys (`pocket_mode`, `force_english_date`, `disable_burn_in_offset`, and AOD schedule keys) in `PixelAodSettingsProvider` to ensure settings successfully propagate from the Settings application to SystemUI.

## [0.1.99d] - 2026-06-16
### Bug Fixes
- **Fix Overlapping System AOD Clock:** Enhanced the stock clock draw suppression hook in `PixelAodHook.java`. Added standard and Oplus keyguard clock container classes (such as `KeyguardStatusView`, `KeyguardClockSwitch`, and `DateMessageView`) to the suppression class list and draw candidate checks. This ensures that the stock clock is completely blocked from rendering when the screen is in AOD mode, resolving the overlapping/double-clock issue.

## [0.1.99c] - 2026-06-16

## [0.1.99b] - 2026-06-16

## [0.1.99a] - 2026-06-15
### Bug Fixes
- **Fix Notification Shade Header Clock (Robust Exclusion)**: Implemented recursive ancestor tracking to protect all descendant views of Quick Settings, Status Bar, Bouncer, and Emergency layers from being hidden by the stock view suppression mechanism. This correctly preserves the system clock inside the notification shade header (e.g. `QSClock`) under all layouts.

## [0.1.99] - 2026-06-15
### Bug Fixes
- **Fix Notification Shade Header Clock**: Exclude status bar, quick settings, shade header, bouncer, emergency, and carrier views from the stock AOD view suppression filters. This ensures the system clock in the top-left corner of the notification shade/quick settings header remains visible and functional.

## [0.1.98] - 2026-06-15
### Bug Fixes
- **Fix AOD Media Info display**: Ensure the AOD media row is hidden immediately when the active media playback state changes to paused or stopped, or when the media session is swiped away.
- **Fix Lockscreen Clock Overlap**: Add active media session presence to the lockscreen clock's compact layout check. This ensures the clock switches to compact layout from the very first frame when waking up with a paused media card, completely preventing the visual overlap.

## [0.1.95] - 2026-06-13
### Bug Fixes
- **Fix Lockscreen Clock Instability/Flickering**: The lockscreen clock size evaluation (`PixelLockscreenClockView`) was tightly coupled to the Oplus layout tree and animations. When Fluid Cloud media text animated (e.g., marquee or equalizer), the layout state fluctuated at 60fps, causing the clock to infinitely toggle between large and compact mode. A 1000ms debounce has been implemented for `setVisibleLockscreenNotificationCards(false)` to completely stabilize the lockscreen layout against these transient UI animation states.

## [0.1.94] - 2026-06-13
### Bug Fixes
- **Fix AOD Clock Mode (Screenshot 17:08)**: The AOD clock view (`PixelAodClockView.java`) now properly shrinks to compact mode when media is playing via `MediaSessionManager`, even if there are no standard notification icons.
- **Fix Lockscreen Clock Flickering/Twitching**: Removed the physical size (`w > 0 && h > 10dp`) check from `isNonEmptySeedling` which caused an infinite layout loop. Now only relies on meaningful text content to detect Fluid Cloud.

## [0.1.93] - 2026-06-13
### Performance
- **Root cause fix for unlock animation frame drops.** `dispatchDraw()` (called ~60fps) was doing a full view tree traversal (`hasLiveLockscreenNotificationCards` → `PixelAodHook.hasVisibleLockscreenNotificationCardsIn`) and parsing all active notifications (`currentNotifications` with string operations) on every single frame. Now `dispatchDraw` only reads lightweight cached boolean flags that are updated event-driven by `applyLockscreenClockReplacement` and `setActiveNotifications`. This eliminates hundreds of tree traversals per second during unlock.

## [0.1.92] - 2026-06-13
### Performance
- **Root cause fix for unlock animation frame drops.** `dispatchDraw()` (called ~60fps) was doing a full view tree traversal (`hasLiveLockscreenNotificationCards` → `PixelAodHook.hasVisibleLockscreenNotificationCardsIn`) and parsing all active notifications (`currentNotifications` with string operations) on every single frame. Now `dispatchDraw` only reads lightweight cached boolean flags that are updated event-driven by `applyLockscreenClockReplacement` and `setActiveNotifications`. This eliminates hundreds of tree traversals per second during unlock.

## [0.1.91] - 2026-06-13
### Performance
- Fixed massive frame drops / CPU overload during unlock and UI layout animations. The recursive content verification (`traverse`) is now strictly limited to media and Seedling containers. Generic system containers are evaluated instantly, eliminating O(N²) layout thrashing.

## [0.1.90] - 2026-06-13
### Fixed
- Fixed a bug where Oplus (ColorOS/OxygenOS) "Fluid Cloud" (Seedling/MediaControlTip) empty containers falsely triggered the small clock layout even when no media was playing. The detector now recursively traverses the container to verify if there is any visible inner content before forcing the small clock.
- Fixed wake-up animation (AOD to Lockscreen) stuttering. The module now avoids hardcoded visibility thresholds (`getAlpha() < 0.1f` and `getHeight() <= 24dp`) on generic notification rows, which previously caused the clock size to abruptly jump mid-animation when the notifications faded in or expanded.

## [0.1.89] - Previous Versions
- Initial implementation of Pixel AOD and Lockscreen custom clocks.
- Add dynamic injection index support to place clocks below `NotificationPanelView` to avoid z-order overlap with notifications.
