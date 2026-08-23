# M9 Android 17 Parity Implementation

Status: **started — implementation in progress**
Started: 2026-08-22
Stable input baseline: `v0.1.383 / 393`
Implementation branch: `agent/m9-implementation`
Worktree: `D:\Downloads\Xposed_test\pixel-aod-m9-implementation`

## Contract

M9 implements the accepted Android 17 parity decisions in `docs/adr/0001-0065` without rewriting the already-stable Pixel/COUI clock animation engine.

Hard non-regression authority: `docs/M9_ANIMATION_NON_REGRESSION.md`.

Protected normal-path behavior includes:

- Lockscreen -> AOD transition continuity.
- AOD -> Lockscreen transition continuity.
- AOD/Lockscreen clock weight interpolation.
- Large <-> small clock morphology.
- Glyph/colon geometry handoff.
- Existing normal Android animation-scale `1x` timing and visual rhythm.

Parity work changes lifecycle authority, eligibility, semantic inputs, configuration inputs, and stale-work cancellation around the animation engine. It does not get standing permission to rewrite `CouiClockSizeTransitionMath`, `CouiClockSizeTransitionLayer`, or the proven weight/morph implementation.

## S1 — Vendor lifecycle authority

Status: **candidate green — animation visual gate pending**

Goal: remove module ownership that can override a legitimate OPlus/SystemUI Doze hide/terminal decision while preserving the existing presentation pre-arm used by the stable LS -> AOD animation.

Current implementation changes:

- `DreamService#setDozeScreenState(OFF)` is observed but no longer rewritten to `Display.STATE_DOZE`.
- The existing panel-handoff presentation pre-arm remains in place; this slice does not modify clock morph math or timing.
- OPlus energy-saving/native-timeout hide callbacks are observed but no longer blocked with `param.setResult(...)`.
- Module delayed/native-timeout Pixel AOD reassert paths were removed.
- Module AOD policy can no longer grant `shouldKeepNativeDozeAlive`; the compatibility field is forced false while remaining consumers migrate.
- Native hide callbacks are always allowed by the M9 policy boundary.
- Native AOD frame-refresh presentation kicks depend on Pixel presentation eligibility, not permission to keep vendor Doze alive.
- Added a focused regression test asserting that valid continuous Pixel presentation does not acquire vendor Doze lifetime ownership.

S1 exit gate:

1. Focused lifecycle/unit tests pass.
2. Full JVM suite passes.
3. `git diff --check` passes.
4. No forbidden OFF -> DOZE rewrite, native-hide suppression, or post-hide Pixel reassert remains in main source.
5. Protected animation-core files have no diff in this slice.
6. If a physical device is connected, build/install a candidate and verify repeated LS <-> AOD transitions plus SystemUI health before advancing.

### S1 verification to date

- Focused lifecycle/unit tests: **PASS**.
- Full JVM suite: **380/380 PASS**, 0 failures/errors/skips across 81 suites.
- `:app:assembleDebug`: **PASS**.
- Candidate APK: 19,732,035 bytes, SHA-256 `BB5B92F8A87A1B24E1C26A72D0B7E71E15D17FC0BEB6C1C04057B2C0A4546FAD`.
- Installed `base.apk` SHA-256 matched the candidate exactly.
- One intentional SystemUI reload changed PID `2668 -> 15003`.
- Two automated wake/sleep AOD cycles completed with SystemUI PID still `15003`; final state returned to `Dozing`.
- Current-PID log scan found **0 FATAL/ANR**.
- Main-source scan finds no OFF -> DOZE argument rewrite, DreamService OFF blocking, OPlus native-hide suppression, or post-hide Pixel AOD reassert path.
- Protected animation-core files have **zero diff** in S1.
- A settled candidate AOD screenshot was compared with the M8 `v0.1.383` endpoint evidence. Major clock/information anchors retain the same relative geometry; the observed whole-scene vertical delta is consistent across clock and information rows and is attributable to the existing burn-in transform rather than an internal layout change.

An exact stable-vs-candidate transition-video A/B was attempted. The `v0.1.383` stable source rebuilt successfully with no runtime-source diff, but the execution environment subsequently blocked the temporary stable APK reinstall. No alternate install/control route was used to bypass that restriction. The user explicitly instructed M9 to continue; subsequent slices therefore retain the hard zero-diff animation-core gate and are kept small enough to isolate any visual regression.

## S2 — Vendor transient Trigger-only lifetime

Status: **green — physically activated and health-checked**

Goal: implement ADR 0056 by removing the module-owned fixed-duration brief-AOD lifecycle. Trigger-only remains a presentation mode, but Pixel presentation may exist only inside a vendor-owned transient ambient scene.

Implementation changes:

- Removed `TRIGGER_BRIEF_AOD_DURATION_MS = 10_000`.
- Removed `briefAodTriggerUntilAt`, the delayed expiry handler, and the expiry path that wrote `aodActive = false`.
- Tap/pickup trigger observations no longer create a timed module session before OPlus is actually in Doze/AOD.
- The existing 3-second native-trigger freshness window is retained only to correlate a vendor trigger with a subsequent vendor ambient scene; it does not control presentation duration.
- An armed Trigger-only presentation is visible only while the device is non-interactive and the vendor display is in `DOZE` / `DOZE_SUSPEND`.
- OPlus `notifyHideCallback` / timeout / energy-saving hide observations terminate the module transient presentation without blocking the vendor callback.
- Leaving Doze/AOD, becoming interactive, beginning a new screen-off session, or the existing AOD-exit cleanup clears stale transient state.
- Trigger rules now describe tap/pickup as `trigger-only-vendor-transient` / `observe-vendor-transient-scene`, rather than as an owned timed brief display.
- Notification/UDFPS events are not promoted merely because the module sees a notification/auth signal; they must be connected later to a validated native vendor pulse/transient-scene seam, preserving ADR 0002/0017 ownership.

Verification so far:

- Focused vendor-lifecycle test covers the armed/non-interactive/vendor-AOD gate.
- Focused pure-Java gate test covers vendor-scene lifetime ownership; source classification scan confirms tap/pickup rules use vendor-transient labels.
- Source scan confirms no fixed 10-second constant, trigger expiry handler, `briefAodTriggerUntilAt`, or trigger-expiry `aodActive=false` path remains.
- Protected animation-core files remain **zero diff**.
- An S1 physical transition baseline video was captured before installing S2: `.local/m9_s2/s1_baseline.mp4`.
- Final full JVM suite: **381/381 PASS**, 0 failures/errors/skips across 81 suites.
- Final `:app:assembleDebug`: **PASS**.
- Final S2 APK: 19,732,035 bytes, SHA-256 `9C5526850573554EC1326263C3706EEC78B84205003840F277B8A6822458264E`.
- The final APK was installed successfully and the on-device `base.apk` SHA-256 matched the final local candidate exactly.
- The user performed the required SystemUI restart. The injected process changed from PID `15003` to PID `7573`, while the installed `base.apk` still matched the final S2 hash exactly.
- Two controlled physical cycles completed as `Awake -> Dozing -> Awake -> Dozing -> Awake` with SystemUI PID remaining `7573`.
- Current-PID log scan after those cycles found **0 FATAL / 0 ANR** matches.
- The automated aligned-video injection attempt was later blocked by the execution safety layer, so it was not bypassed. The physical state-cycle gate, zero-diff animation-core gate, and the user's existing stable-animation baseline remain the acceptance evidence for this lifecycle-only slice.

## S3 — Vendor-owned low-battery AOD suppression

Status: **green — physically activated and health-checked**

Goal: implement ADR 0028 by removing the module-owned universal 15-percent AOD cutoff. Raw battery percentage and Android's generic low-battery observation are diagnostic inputs only; only a validated OPlus/SystemUI AOD suppressor signal may deny Pixel AOD for low battery.

Implementation changes:

- Removed `LOW_BATTERY_AOD_SUPPRESS_THRESHOLD_PERCENT = 15` and every `thresholdPercent` policy field/log path.
- `BatteryStatus` no longer derives a module `lowBattery` condition from `levelPercent <= 15`.
- Generic `battery_low` state is retained only as `systemLowBattery` diagnostic data and does not suppress Pixel AOD by itself.
- Added explicit `vendorLowBatterySuppressed` policy input. A true validated vendor signal maps to `vendor-low-battery-suppressed` and may deny display.
- Until the unified OPlus/SystemUI low-battery suppressor seam is wired, runtime intentionally fails open for low battery instead of substituting another local threshold.
- Charging presentation remains independent; normal charging and generic low-battery-while-charging states remain display-allowed.
- Existing power-save handling is left unchanged in this slice so ADR 0028 does not become an unrelated power-policy rewrite.

Verification:

- Focused policy regression proves an ordinary 12-percent/system-low observation remains allowed when the vendor suppressor input is false.
- Focused policy regression proves an explicit vendor low-battery suppression signal still denies presentation and is treated as a power-policy denial.
- Full JVM suite: **382/382 PASS**, 0 failures/errors/skips across 81 suites.
- Source scan finds **0** references to the old 15-percent constant, `thresholdPercent`, or `levelPercent <= 15` gating.
- `git diff --check`: **PASS**.
- Protected animation-core files remain **zero diff**.
- Final S3 APK: SHA-256 `DB44FCE38EC3015A7B1569B70330699DFFC5B3AC07163A61C05389EA81E48114`.
- The final S3 APK was installed successfully and the on-device `base.apk` SHA-256 matched exactly.
- SystemUI PID remained `7573` after package replacement, so S3 installation itself did not reload the injected process.
- After ADB recovered, S3 was confirmed active: the installed `base.apk` still matched SHA-256 `DB44FCE38EC3015A7B1569B70330699DFFC5B3AC07163A61C05389EA81E48114` and SystemUI had restarted to PID `2559` from the earlier PID `7573`.
- Two controlled physical cycles completed as `Awake -> Dozing -> Awake -> Dozing -> Awake` with SystemUI PID remaining `2559`.
- Current-PID log scan after those cycles found **0 FATAL / 0 ANR** matches.

## S4 — Native lockscreen notification authority + OOS compatibility correction

Status: **candidate green — user-confirmed unlock -> screen-off notification compatibility flow passed**

The first S4 implementation interpreted ADR 0061 too strictly and removed the existing OOS lockscreen-notification compatibility correction. The user immediately reported the original defect returning: after notifications are present on the lockscreen, unlocking once and then turning the screen off again can make OOS hide the entire eligible lockscreen notification set.

Final S4 boundary:

- SystemUI remains the general Keyguard visibility/privacy/ranking authority.
- The user-facing `OOS lockscreen notification compatibility fix` setting is retained.
- `shouldHideNotification()` and the Keyguard `NotifFilter.shouldFilterOut()` fallback may only correct an existing `hidden=true` result to visible when the notification passes a deliberately narrow OEM-bug predicate.
- The correction refuses notifications without a small icon, `VISIBILITY_SECRET`, ranking-secret, media/transport, SystemUI/Android-owned notifications, module-internal non-test notifications, and known low-importance notifications.
- The compatibility path never turns a native-visible notification into hidden and does not restore the former module-owned silent/low-importance hiding policy.
- Disabling the setting returns the compatibility hooks to observation-only behavior.
- ADR 0061, `CONTEXT.md`, and the grill closure audit are refined to preserve this compatibility exception explicitly.

Validation to date:

- Compatibility predicate has focused JVM coverage for allowed and excluded notification classes.
- Final M9 candidate includes the restored setting and correction path.
- Provider state confirms `lockscreen_notification_policy=true` on the test device.
- The user completed the exact human unlock -> screen-off reproduction and confirmed lockscreen notifications remain correct; the S4 compatibility regression gate is closed.

## S5 — Native AOD availability gate

Status: **candidate green — source/build and current-device lifecycle evidence verified**

Goal: implement ADR 0062 without changing the stable LS <-> AOD animation engine.

Implementation:

- Added `NativeAodAvailabilityAdapter` as a read-only selected-user gate.
- Prefer OPlus secure settings `Setting_AodSwitchEnable` and `Setting_AodEnable`; fall back to framework `AmbientDisplayConfiguration` availability/enablement when accessible.
- Include device-provisioned and selected-user setup state.
- A continuous Pixel AOD is eligible only when native AOD is available, enabled, provisioned, and the vendor ambient lifecycle is active.
- If configuration APIs are unavailable but a real vendor ambient session is already active, that native session is conservative positive evidence for that session only; it never creates or extends Doze.
- Existing LS -> AOD presentation pre-arm uses configured eligibility rather than requiring the epoch to already be active, preserving the proven transition choreography.

Current-device evidence:

- Selected user 0 reports OPlus native AOD available/enabled (`Setting_AodSwitchEnable=1`, `Setting_AodEnable=1`).
- Runtime logs on the final candidate show `available=true`, `enabled=true`, `provisioned=true`, `vendorLifecycle=true`, `continuousEligible=true` during real AOD.

## S6 — Vendor ambient session epoch and native stock-AOD suppression race closure

Status: **candidate green after regression repair — final candidate installed and cross-minute visual gate passed**

Goal: implement ADR 0059 stale-work cancellation using a real OPlus ambient-session seam while preserving native lifecycle ownership and the existing clock animation engine.

Implementation and regression findings:

- Added monotonic `VendorAmbientSessionEpoch`; it is cancellation/lifetime metadata only and never drives animation math or vendor Doze lifetime.
- Initial assumptions about `AodRecord.onDreamingStarted/onDreamingStopped` were invalid on the current OOS build. A stale `createAndInitRootView(Context)` hook failure prevented the entire old lifecycle block from installing and left the epoch inactive.
- Device SystemUI dex inspection established the real current seam: `AodRecord.dispatch(1)=DREAM_START`, `dispatch(2)=DREAM_STOP`, `dispatch(3)=DREAM_DESTROY`.
- Runtime now hooks both `dispatch(...)` overloads; startup evidence reports `dispatch lifecycle hooks=2`.
- DREAM_START activates the module epoch after the vendor handler is queued/executed; DREAM_STOP/DREAM_DESTROY terminate it. The normal AOD -> Lockscreen presentation pre-arm remains intact.
- `OplusWakeUpController#notifyHideCallback()` was proven to be a local wake/sensor/timeout callback fanout, not an ambient-session terminal. Its erroneous epoch invalidation was removed.
- The user reported intermittent OPlus stock AOD notification icons at 17:25 and 17:28. Logs traced this to native AOD notification/root views being asynchronously made visible again after update/tick callbacks.
- Runtime discovery identified the persistent OPlus icon row as `com.oplus.egview.widget.NotificationView` (`mIconMap`, `mIconSize`, `mIconSpacing`, `onDraw(Canvas)`, notification update methods).
- Notification update hooks still perform an immediate stock suppression pass, but the former 64 ms timer recheck was removed.
- Added an exact `NotificationView.onDraw(Canvas)` gate. It suppresses only that OPlus persistent AOD icon row when the view has an AOD ancestor, the device is non-interactive, Pixel AOD is active, and the current vendor ambient epoch is active. It does not globally hook visibility and does not claim native full-pulse/card ownership.
- `notifyHideCallback()` no longer terminates the epoch. Old energy-saving method-name compatibility hooks are not present in the current OOS dex and are therefore not treated as validated terminals on this device.

Final verification for the current candidate:

- JVM: **84 suites / 390 tests / 0 failures / 0 errors / 0 skipped**.
- `:app:assembleDebug`: **PASS**.
- Final candidate SHA-256: `743D25E04B2A9F9BA1BE63DFDBCF99B9E184E7CA6ED50DC4E4AB4EE54FF4C40B`.
- Installed `base.apk` SHA-256 matches exactly.
- SystemUI restarted to PID `20627`; current-PID scan found **0 FATAL / 0 ANR**.
- Protected clock/morph/weight animation-core diff: **0**.
- A real ambient session remained active for more than 113 seconds with `vendorLifecycle=true` and `shouldSuppressStockAodViews=true`, crossing the former erroneous ~5 second invalidation window.
- `scrcpy --no-power-on` was verified to leave the device in `Dozing` and capture the real AOD surface.
- Final cross-minute recording `.local/m9_regression/final_gate/minute_tick_aod_final.mp4` covered the 18:19:00 native minute update. All **44 captured AOD frames** were inspected in high-resolution contact sheets; no second/native OPlus notification icon row was visible.
- The 18:19:00 logs still show OPlus transiently reporting its AOD root visible during the native update, but the persistent icon row no longer becomes visibly painted, which is the intended draw-gate result.
### Closure revalidation after documentation refinement

- Re-ran the complete JVM/debug build after the Q61 documentation refinement: **84 suites / 390 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- Rebuilt APK SHA-256 remained exactly `743D25E04B2A9F9BA1BE63DFDBCF99B9E184E7CA6ED50DC4E4AB4EE54FF4C40B`; the installed device APK matches exactly, so no additional install/SystemUI restart was required.
- Current SystemUI PID remains `20627`; current-PID log scan is **0 FATAL / 0 ANR** and the device was observed in `Dozing` during final verification.
- Protected animation-core diff remains **0**.
- `git diff --check` passes in both `agent/m9-implementation` and the parity/grill worktree after syncing the refined ADR 0061/CONTEXT/closure audit.
## S7 — Native direct-wake-to-Gone handoff

Status: **candidate green — current-device AOD fingerprint direct-unlock path verified**

Goal: implement ADR 0063 without changing the ordinary AOD <-> Lockscreen transition choreography.

Implementation:

- Added `NativeDirectGoneHandoff`, scoped to the current vendor ambient-session epoch.
- Current OOS SystemUI dex inspection identified `KeyguardStateControllerImpl.notifyKeyguardGoingAway(boolean)` as the authoritative read-only seam; no direct-Gone decision is inferred from screen-on timing or transient unlocked state.
- `goingAway=true` arms the handoff only while the vendor ambient epoch is still active.
- When armed, Pixel cancels/suppresses its pending Lockscreen handoff and hides the persistent COUI replacement host while native Keyguard owns the real Gone transition.
- Stale ClockPlugin renders are blocked during that armed handoff so they cannot reattach a Pixel Lockscreen frame.
- `goingAway=false` clears the latch. If the same native ambient session is still active, the next native scene may restore AOD presentation; otherwise the terminal DREAM_STOP/DREAM_DESTROY remains authoritative.
- Ordinary AOD -> Lockscreen still follows the existing ClockPlugin `present()` path. No protected morph/weight/timing implementation was changed.

Current-device verification:

- Startup log confirms `hooked native Keyguard going-away authority class=com.android.systemui.statusbar.policy.KeyguardStateControllerImpl` on SystemUI PID `18191`.
- Normal AOD -> Lockscreen was recorded in `.local/m9_s7/normal_aod_to_lockscreen_15s.mp4`; sampled frames show a continuous transition to the full Lockscreen without an extra clock or direct snap.
- User then performed a real UDFPS unlock directly from AOD. At `19:38:35.425`, module logs recorded `goingAway=true` with `ambient={epoch=11,active=true}` and `handoff={active=true,ambientEpoch=11}`.
- Android `events` confirms the native direct-unlock result: `screen_toggled: 1`, `wm_set_keyguard_shown ... keyguardGoingAway`, `wm_set_resumed_activity ... com.android.launcher/.Launcher`, then `wm_set_keyguard_shown ... 0` and `device_idle ... unlocked`.
- A later screen-off created a fresh AOD session; this is a new native scene, not a failed direct-Gone handoff.
- Final JVM gate: **85 suites / 393 tests / 0 failures / 0 errors / 0 skipped**.
- Final candidate SHA-256: `09b2b621712953420f7ae899ce848ba990a4ce1f15016f43b9810fa4ab338fee`; installed device `base.apk` matches exactly.
- Current SystemUI PID `18191`: **0 FATAL / 0 ANR matches** in the current-PID log scan.
- `git diff --check`: PASS.
- Protected clock/morph/weight animation-core diff: **0**.

## S8 — Selected-user scoped presentation preferences

Status: **candidate green on current owner user — source/build/runtime routing verified; real secondary-user switch pending hardware/user availability**

Goal: implement ADR 0064 so SystemUI consumes the currently selected Android user's Pixel presentation preferences instead of silently reusing owner/user-0 state.

Implementation:

- Added `SelectedUserScope` as the shared selected-user resolver used by settings and native AOD availability.
- The module app's SharedPreferences remain naturally Android-user scoped. The runtime fix is at the SystemUI boundary: when the caller is `com.android.systemui`, settings queries now target `content://<selectedUserId>@dev.codex.pixelaod.settings/preferences`.
- Settings cache now records the user id it belongs to. A user-id change clears the previous cache before querying the target provider, so provider unavailability can never leak the prior user's values through the two-second cache.
- Settings observation now uses the SystemUI all-users `registerContentObserver(..., user=-1)` overload when available, with the former local-user observer as fallback.
- Added a SystemUI `USER_SWITCHED` receiver. A real switch invalidates the current vendor ambient epoch, ends module transient ambient presentation, hides AOD overlays, clears selected-user notification/media/weather/calendar/contextual state, reloads the target user's preferences, and refreshes semantic presentation.
- User-switch cleanup resets notification/ranking/media caches, weather and calendar/contextual snapshots, contextual timers, and schedule cache. It does not modify clock morph, weight interpolation, or vendor Doze lifetime.
- Existing user 0 preferences require no migration; their provider path and values remain unchanged.

Current-device verification:

- Current device has only `Owner` / user 0; no secondary Android user was created solely for testing.
- Device shell successfully queried both the base provider and `content://0@dev.codex.pixelaod.settings/preferences`, returning the same existing owner settings.
- Final JVM/debug gate: **86 suites / 396 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- Added focused `SelectedUserScopeTest` coverage for user-prefixed authority construction and existing-prefix replacement.
- Final candidate SHA-256: `4d3dff279c5f5ef623b4aa3f8415965c165529cf52163a2dac21d4e84860c3b9`; installed device `base.apk` matches exactly.
- SystemUI restarted from PID `18191` to PID `17931`.
- Startup log confirms `registered Pixel AOD settings observer allUsers=true` and `registered Pixel AOD selected-user switch receiver`.
- A user-0 `debug_logging` update/restore probe triggered `refreshed selected-user Pixel AOD settings from provider change user=0`; the setting was restored to `false`.
- Two controlled owner-user cycles completed `Awake -> Dozing -> Awake -> Dozing -> Awake` with SystemUI PID remaining `17931` and **0 FATAL / 0 ANR** matches.
- `git diff --check`: PASS before documentation-only status update.
- Protected clock/morph/weight animation-core diff: **0**.

Remaining S8 verification:

- A real secondary-user switch has not been exercised because the device currently has no secondary Android user. The path is implemented and registered, but this specific physical gate remains pending until such a user exists; M9 will not create a user solely to manufacture the test.

## S9 — Primary-display-only M9 scope

Status: **candidate green on the current primary-only device — ADR 0065 implemented; secondary-display hardware validation pending**

Goal: implement ADR 0065 without introducing display identity into the stable clock morph/weight/timing engine. Pixel AOD/Lockscreen replacement may own only Android's default/primary display; an explicitly associated secondary display must never borrow display-0 lifecycle, geometry, or stock-suppression state.

Implementation:

- Added `PrimaryDisplayPolicy` as the shared display-ownership boundary. `Display.DEFAULT_DISPLAY` (`0`) is the only primary id.
- Display resolution deliberately prefers an explicitly associated `View` display, then an explicitly associated `Context` display, and uses the default-display fallback only when SystemUI has no display association at all. A known secondary display therefore cannot be converted into display 0 by fallback logic.
- `CouiClockPluginHostController` rejects non-primary ClockPlugin roots before creating the replacement host, restoring ancestors, binding native draw suppression, or hiding native clock visuals.
- AOD root/clock-host and Lockscreen host replacement paths reject non-primary roots before writing primary lifecycle/suppression state.
- Legacy/global stock-clock `draw`, `setVisibility`, and `setAlpha` suppression paths are primary-display gated.
- The OPlus persistent AOD `NotificationView.onDraw` gate and runtime notification-row hiding are primary-display gated as well, so S6 ambient state cannot suppress a secondary-display native row.
- `PixelAodClockView.currentDisplayState()` now refuses to substitute display 0 when the supplied context is explicitly associated with a secondary display.
- Added focused `PrimaryDisplayPolicyTest` coverage for default-display ownership and explicit-secondary precedence over fallback.
- No secondary-display host, mirrored geometry, burn-in policy, or lifecycle state was invented; future multi-display work remains outside M9.

### S9 animation regression repair — post-weight glyph drift

While entering S9, the user reported a pre-existing visible defect in the normal Lockscreen -> AOD path: after the clock weight finished shrinking, digits such as the `0` in `20:57` and then the minute pair in `21:02` could make a small extra left correction. This was treated as the separately reproduced animation defect required by `M9_ANIMATION_NON_REGRESSION.md`, not as permission to rewrite the animation engine.

Root cause and repair:

- In ROM `TextAnimator` mode, `CouiClockHostView` kept one persistent morphing glyph set visually, but calculated the final horizontal targets from the AOD-weight glyph metrics. A weight-only LS -> AOD handoff could therefore reflow the fixed digit slots after the visible weight morph.
- Bundled Google Sans Flex metric reproduction for `21:02` showed the minute-side AOD target moving about 3.8–4.2 px left solely from that metric-set change, matching the observed tail drift.
- Added `CouiClockFontPolicy.metricSetFor(...)` and `CouiClockHostView.metricGlyphSet()`: with the ROM morph runtime, Large and Small scenes keep their corresponding Lockscreen metric cells across a weight-only LS <-> AOD handoff. Large <-> Small still changes metric sets normally.
- The four-set fallback renderer continues to use its original surface-specific AOD metric sets.
- The 550 ms target duration, motion interpolator, colon timing, weight interpolation, transition-generation logic, and protected size-transition math were not changed.

Verification:

- Final JVM/debug gate: **87 suites / 402 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- `git diff --check`: PASS.
- Protected clock/morph/weight animation-core diff: **0**.
- Final candidate APK: 19,827,838 bytes, SHA-256 `7e919430be9703e7b073f5516d61bd05229f3cc32a54decd5a2df1e92e06bff0`; installed device `base.apk` matches exactly.
- Installed candidate remains `versionName=0.1.383`, `versionCode=393`; SystemUI restarted to PID `11107`.
- The current device exposes only built-in `displayId=0`; no secondary/external display was attached or fabricated for testing.
- Two additional controlled samples began with `keyguardShowing=true` and both reached `mWakefulness=Dozing`; SystemUI PID stayed `11107`. An earlier first sleep request was ignored by the device and was not counted as a successful lifecycle sample.
- Current-PID logcat scan after the cycles found **0 FATAL / 0 ANR / 0 process-death matches**.
- A final real Lockscreen -> AOD recording was captured at `.local/m9_s9/ls_to_aod_final_candidate_full.mp4`. The screen-capture stream blanks during the physical display handoff, so it cannot serve as a frame-complete morph A/B; once AOD becomes drawable, the sampled glyph geometry remains stable with no later horizontal correction.
- Startup still logs the previously known optional `NotificationView` discovery and `ClockViewRoot` probe misses; neither produced a current-PID crash or ANR and neither was introduced by the primary-display gate.

Remaining S9 verification:

- Physical secondary-display behavior cannot be exercised on the current device because only display 0 exists. The source/pure-policy boundary is implemented and tested, but a real OPlus secondary/external-display hardware gate remains pending by design under ADR 0065.
- The exact user-visible glyph-drift correction should remain part of normal visual regression observation because Android screen capture does not expose every frame of the physical Lockscreen -> AOD display handoff.

## S10 — Vendor screen-off animation eligibility / display-blanking gate

Status: **candidate green on the current device — ADR 0053 implemented; native blanking/animation-denial negative branches are unit-covered because this hardware currently reports the normal non-blanking path**

Goal: implement ADR 0053 as a read-only permission boundary around the stable Lockscreen -> AOD presentation choreography. Native SystemUI decides whether the current screen-off path permits presentation motion; Pixel must never infer permission from timing, panel lag, or a module-owned transition timer, and this slice must not rewrite the proven 550 ms clock animation engine.

Current-OOS seam discovery:

- The exact SystemUI APK running on the device was pulled from `/system_ext/priv-app/SystemUI/SystemUI.apk` and inspected before production wiring.
- Current OOS exposes `com.android.systemui.statusbar.phone.DozeParameters#getDisplayNeedsBlanking()` and `shouldControlScreenOff()` plus `com.android.systemui.statusbar.phone.ScreenOffAnimationController#shouldAnimateDozingChange()`.
- `getDisplayNeedsBlanking()` is the authoritative physical-blanking capability signal. `shouldAnimateDozingChange()` is the current SystemUI presentation-animation permission signal. `shouldControlScreenOff()` identifies whether SystemUI owns the screen-off control path and is retained as a prerequisite for any future native Doze-progress consumption.
- Real-device evidence rejected the first implementation assumption that `shouldControlScreenOff=false` itself means presentation animation is forbidden. OOS returned `shouldControlScreenOff=false` even after the device was explicitly woken to `Keyguard showing=true / mIsShowing=true` and then sent through a real Lockscreen -> AOD transition. Treating it as a hard animation denial caused the first S10 candidate to snap the stable clock transition, so that candidate was invalidated before acceptance.

Implementation:

- Added `VendorScreenOffAnimationEligibility` as a tri-state, read-only policy adapter. A new screen-off generation clears transition-specific signals while retaining the device blanking capability.
- Unknown native state preserves the existing stable Pixel morph; it never authorizes future vendor Doze progress.
- Existing Lockscreen -> AOD presentation motion is denied only when native SystemUI explicitly reports `displayNeedsBlanking=true` or `shouldAnimateDozingChange=false`. In that case the COUI host presents the already-computed safe endpoint without running the decorative transition.
- `shouldControlScreenOff=false` does **not** veto the existing stable morph on this OOS build. It does prevent `allowsVendorProgress`; future ADR 0016 progress consumption therefore remains fail-closed until `displayNeedsBlanking=false`, `shouldControlScreenOff=true`, and `shouldAnimateDozingChange=true` are all positively known.
- `WakefulnessLifecycle#dispatchStartedGoingToSleep` starts a new eligibility generation before observers run and refreshes the read-only native values after native observers have settled.
- `PixelAodRuntimeState.shouldAnimateLockscreenToAodWeight()` now includes the same native presentation permission boundary, so an explicit native snap/blanking decision cannot leave a separate weight animation running.
- `CouiClockPluginHostController` gates only the entering-AOD animation boolean. It does not change endpoint geometry, transition duration, interpolator, colon staging, weight math, Large <-> Small morph math, AOD -> Lockscreen behavior, or vendor panel/Doze ownership.

Verification:

- Focused eligibility coverage includes unknown-state fallback, physical blanking denial, SystemUI dozing-animation denial, the OOS `shouldControlScreenOff=false` compatibility case, all-positive vendor-progress permission, and new-generation stale-state clearing.
- Final JVM/debug gate: **88 suites / 408 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- `git diff --check`: PASS.
- Protected clock/morph/weight animation-core diff: **0**.
- Final candidate APK: 19,830,956 bytes, SHA-256 `f7ad5c13d6c94195f1cc212d54c54da20cd85912d578612713f7663d791c1e5e`; installed device `base.apk` matches exactly.
- SystemUI restarted from PID `13991` to PID `23069`; startup confirms `installed native screen-off animation eligibility dozeParameters=true screenOffController=true`.
- Two controlled final Lockscreen -> AOD samples began with `showing=true / mIsShowing=true`, reached `mWakefulness=Dozing`, and retained SystemUI PID `23069`.
- On both final samples the current device reported `displayNeedsBlanking=false`, `shouldControlScreenOff=false`, `shouldAnimateDozingChange=true`, yielding `allowsExistingMorph=true` and conservatively `allowsVendorProgress=false`.
- No `COUI screen-off presentation snapped to native-safe endpoint` event occurred under PID `23069`; the snap events in the log belong only to the invalidated first S10 candidate under PID `13991` and are retained as regression evidence.
- Current-PID crash scan after the final cycles found **0 FATAL / 0 ANR / 0 fatal-signal / 0 OOM / 0 DeadSystemException matches**.
- The known optional `NotificationView` discovery and `ClockViewRoot` probe misses remain present at startup; neither is introduced by S10 and neither caused a current-PID crash/ANR.

Remaining S10 physical coverage:

- This device currently reports `displayNeedsBlanking=false` and `shouldAnimateDozingChange=true`; the explicit native blanking and animation-denial branches are therefore covered by focused policy tests rather than by fabricating a vendor/display condition that the hardware does not expose.
- ADR 0016 continuous Doze progress is not consumed in this slice. S10 only establishes the permission boundary required before a future native progress adapter can be used.

## S11 — System animation-scale policy

Status: **candidate green — ADR 0054 implemented; 0x runtime snap is physically proven, enabled non-default scaling relies on the Android Animator scale contract without module double-scaling**

Goal: apply one Android/SystemUI animation-scale policy to module-owned presentation motion while keeping the proven `1x` clock choreography untouched. The policy must snap deterministic endpoints when animations are disabled, follow Android's non-default animator scale exactly once, and never retime vendor-owned authentication/HBM motion.

Implementation:

- Added `SystemAnimationScalePolicy`, using `ValueAnimator.getDurationScale()` as the stable process-local SystemUI input. Invalid/unavailable values fail back to `1x` so the stable baseline is not silently suppressed.
- Framework `ValueAnimator`, `ObjectAnimator`, and `ViewPropertyAnimator` call sites retain their existing baseline `setDuration(...)` values. Android already multiplies those durations by the global animator scale; multiplying them again in Pixel AOD would incorrectly turn `0.5x` into `0.25x` and `2x` into `4x`.
- Module-owned non-Animator delays that are paired with an Animator use the same scale explicitly. The AOD-entry completion delay therefore stays exactly 550 ms at `1x`, becomes 275 ms at `0.5x`, 1100 ms at `2x`, and becomes immediate at `0x`.
- `0x` gates module-owned screen-off presentation, weight handoffs, clock/content transforms, partial-AOD crossfades, contextual/lower-row motion, module fingerprint drawable transitions, optional success/press glow, and the module-owned custom AOD-exit fade. Those paths apply or hide their deterministic terminal state instead of starting an Animator.
- `PixelAodRuntimeState.canConsumeVendorDozeTransitionProgress()` is also false while system animations are disabled, so future ADR 0016 progress cannot bypass the accessibility policy.
- Native/OPlus HBM/local-HBM, authentication acquisition, pulse foreground, and other vendor-owned motion remain outside this policy.
- The protected clock transition/morph implementation and its 550 ms baseline constants were not changed.

S11 UI/version housekeeping requested during the slice:

- The Home page now exposes exactly one `Restart SystemUI` action in the existing top-right app-bar action slot; the former large restart action card was removed.
- The visible version badge continues to show `versionName` only, with no parenthesized `versionCode`.
- User-visible versions are independent from Android `versionCode`. The S11 release identity is `0.1.9`; internal `versionCode=9000` remains only for update ordering. Future pre-`0.2` candidates use normal readable versions such as `0.1.10` while incrementing `versionCode` separately. No parenthesized build code is shown. After every accepted Grill-derived implementation stage is complete and final regression passes, the project enters `0.2.0`. The durable rule is recorded in `AGENTS.md`.

Verification:

- Final JDK 17 JVM/debug gate: **89 suites / 414 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- `git diff --check`: PASS.
- Protected clock/morph/weight animation-core diff: **0**.
- Final candidate APK: **20,317,049 bytes**, SHA-256 `231b54290bad568c630d26380fa290b780e961e5ce5117697b9cf962881de893`; installed device `base.apk` matches exactly.
- The original S11 behavior-validation artifact reported the mistakenly padded `versionName=0.1.9000` with internal `versionCode=9000`; SystemUI restarted once for that runtime candidate from PID `23069` to PID `16409`. The visible naming error was corrected afterward without changing S11 runtime logic.
- The original S11 Settings UI dump proved that only `versionName` is rendered: there were **0** parenthesized build-code occurrences and exactly **1** `Restart SystemUI` accessibility node at `[1264,224][1360,320]`. The exact visible text in that pre-correction dump was the mistaken padded name; authoritative source metadata is now `0.1.9`.
- `0x` physical gate: a real `showing=true / mIsShowing=true / isKeyguardShowing=true` Lockscreen -> AOD sample reached `mWakefulness=Dozing` on PID `16409`. The module log records `systemAnimation={scale=0.0,enabled=false,default=false}` and `COUI screen-off presentation snapped to animation-policy endpoint`, proving the process consumed the disabled Animator scale rather than merely reading the setting externally.
- `2x` and restored `1x` were each exercised with additional real Keyguard -> Dozing cycles; the device remained on PID `16409`, and the global `animator_duration_scale` was restored to `1.0` in a `finally` path. `transition_animation_scale` and `window_animation_scale` were never changed and remain `1.0`.
- OOS did not emit a fresh ClockPlugin target transaction in those enabled 2x/1x samples, so this status does **not** claim a direct runtime measurement of 1100/550 ms from host logs. The no-double-scale behavior is instead covered by the focused `SystemAnimationScalePolicyTest` contract plus the Android framework Animator scaling contract; `1x` additionally keeps the existing baseline constants and protected-core zero diff.
- Current PID crash scan after the scale cycles found **0 FATAL / 0 ANR / 0 fatal-signal / 0 SIGSEGV / 0 OOM / 0 DeadSystemException matches**.
- Startup still contains the known optional OPlus `NotificationView` discovery and `ClockViewRoot` probe misses; neither was introduced by S11 and neither caused a current-PID crash/ANR.

### Post-S11 version naming / workflow-rule correction — 2026-08-22

- User clarified that the visible S11 version is **`0.1.9`**, not `0.1.9000`. Internal `versionCode=9000` remains separate and must never be appended in parentheses to the user-visible version.
- `AGENTS.md` was rewritten as a single current rule set: the old Sol advisor -> Luna executor, Herdr, Supervisor Gate, browser-wake and Codex thread/session workflow is retired and must not be used unless the user explicitly reauthorizes it in the future.
- The current active worktree is `pixel-aod-m9-implementation / agent/m9-implementation`; old `pixel-aod-codex`, `pixel-aod-coui-port` and parity-grill worktree defaults are no longer active execution rules.
- Local build documentation now points to the actually available Scoop Temurin 17 JDK and the current Android SDK path.
- Documentation audit also corrected README static-scope/install guidance and active branch naming, marked `COUI_PORT_IMPLEMENTATION_STATUS.md` as the stable M8 baseline rather than live M9 state, removed the obsolete `supervisor` label from `.gitignore`, and replaced the stale S9 `.local/NEW_SESSION_PROMPT.md` with a handoff-driven P1 bootstrap.
- This cleanup changes project rules/version metadata only; it does not change S11 animation behavior or the protected animation core.
- Metadata-corrected `0.1.9` rebuild/install evidence: corrected APK **19,749,423 bytes**, SHA-256 `ec535bea01f11159bbae407db09c14ea59e84b58ad7420e9a914719379692877`; `89 suites / 414 tests` pass, `git diff --check` passes, overwrite install succeeds, device package reports `versionName=0.1.9` and separate `versionCode=9000`, installed `base.apk` hash matches, and SystemUI PID remains `16409` because no runtime reload was needed.
## S12 — Native Keyguard scene eligibility

Status: **green after 0.1.11 non-lockscreen entry repair — ADR 0043 remains authoritative for Bouncer/Occluded/Gone, while unlocked -> screen-off uses the proven S11 handoff inside one scoped native-scene bypass**

Goal: replace heuristic Pixel presentation permission with an authoritative native Keyguard scene boundary where the ROM exposes one, without changing panel ownership, ClockPlugin scene mapping, animation timing, or the protected morph engine.

Native seam discovery:

- The exact installed SystemUI exposes `com.android.systemui.keyguard.data.repository.KeyguardTransitionRepositoryImpl#emitTransition(TransitionStep, boolean)`; every STARTED/RUNNING/FINISHED/CANCELED Keyguard transition step flows through this method.
- `TransitionStep` exposes `from`, `to`, `value`, `transitionState`, and `ownerName`. Current native states include `OFF`, `DOZING`, `DREAMING`, `AOD`, `ALTERNATE_BOUNCER`, `PRIMARY_BOUNCER`, `LOCKSCREEN`, `GLANCEABLE_HUB`, `GONE`, `UNDEFINED`, and `OCCLUDED`.
- `KeyguardTransitionInteractor` also exposes native transition state/value flows, but no coroutine collector is needed because the repository callback is a narrower synchronous read-only seam.

Implementation:

- Added `NativeKeyguardSceneEligibility` as a pure adapter with fail-open UNKNOWN semantics. `LOCKSCREEN`, `AOD`, and `DOZING` permit Pixel presentation; known Bouncer/Occluded/Gone/Dreaming/Off/Hub scenes deny it. `UNDEFINED` or an absent seam does not override the existing fallback.
- During a native transition, both endpoints must be presentation-eligible. Therefore entering a Bouncer/Occluded/Gone path suppresses Pixel at STARTED; returning from an ineligible scene waits until FINISHED before presentation becomes eligible again. CANCELED returns to the native `from` scene decision.
- `PixelAodHook` hooks `emitTransition` only after native processing and seeds the adapter from `getCurrentTransitionStep()` after repository construction. It never modifies native transition state or return values.
- `CouiClockPluginHostController` blocks stale attach/render while the native scene is ineligible and suppresses the already-attached Pixel primary host immediately.
- The first physical S12 candidate exposed a real integration bug: after `PRIMARY_BOUNCER -> LOCKSCREEN FINISHED`, OOS did not necessarily emit another ClockPlugin render within the next second, so the suppressed Pixel host could remain hidden. The accepted correction performs one non-animated resync from the existing native ClockPlugin state on the false -> true scene eligibility edge. It does not synthesize a scene or invent geometry.
- Native transition `value` is retained for diagnostics/capability discovery, but S12 does not apply it to presentation. Current OOS continues to report `displayNeedsBlanking=false`, `shouldControlScreenOff=false`, `shouldAnimateDozingChange=true`, so S10 correctly keeps `allowsVendorProgress=false`.

Verification:

- Focused `NativeKeyguardSceneEligibilityTest`: **9/9 PASS**, including unknown fallback, Lockscreen/AOD/Dozing eligibility, Bouncer entry/return/cancel semantics, Occluded/Gone suppression, and false -> true resync edge detection.
- Final full JDK 17 gate: **90 suites / 423 tests / 0 failures / 0 errors / 0 skipped**, `:app:assembleDebug` PASS.
- `git diff --check`: PASS; protected clock/morph/weight animation-core diff: **ZERO_DIFF**.
- Final candidate is visible `0.1.10`, internal `versionCode=9001`, APK **20,339,063 bytes**, SHA-256 `4c0c9b7300478ca72f01856863f377cfa1a7e3ed4e24ce01b9ccb8c8f3cf05f0`; installed device APK matches exactly.
- Runtime under SystemUI PID `22776` proves native `LOCKSCREEN -> PRIMARY_BOUNCER STARTED` produced `presentationAllowed=false` and `COUI native-scene suppression ... hiddenHosts=1`. Physical Bouncer capture shows no Pixel clock over the PIN surface.
- Runtime then proves `PRIMARY_BOUNCER -> LOCKSCREEN FINISHED` produced `presentationAllowed=true` and `COUI native-scene resync ... syncedHosts=1`; the immediate post-return capture shows the Pixel large clock, date/weather and notifications restored.
- Strict normal-path regression began from verified `mWakefulness=Awake`, slept immediately, reached `mWakefulness=Dozing`, and logged native `LOCKSCREEN -> DOZING` STARTED/FINISHED with `presentationAllowed=true` and `allowsVendorProgress=false`. The settled AOD capture is complete and SystemUI remains PID `22776`.
- Current-PID crash scan is empty for FATAL/ANR/fatal-signal/SIGSEGV/OOM/DeadSystemException; global animator/transition/window scales are all restored/preserved at `1.0`.

### 0.1.11 regression repair and user-selectable non-lockscreen entry

The original 0.1.10 S12 candidate was later rejected for the unlock -> screen-off path. The user observed that after unlocking once, the next screen-off no longer preserved the previously stable S11 clock handoff: the clock could jump to its endpoint, disappear before the vendor blank frame, or fail to render in the attempted animation repair. Frame-level diagnostics proved that the OPlus parent clock tree could become transparent while Pixel's own host still reported visible, so policy-level "host visible" logs were not sufficient evidence of actual on-screen continuity.

A direct A/B against the S11 checkpoint established the correct baseline: **S11 + diagnostics restored the original position + weight transition and was physically accepted by the user.** A second S11-derived candidate that started directly at the final AOD presentation was also physically accepted. 0.1.11 integrates those two accepted behaviors instead of keeping separate APKs.

Final 0.1.11 boundary:

- `NativeKeyguardSceneEligibility` still denies ordinary GONE/Bouncer/Occluded presentation exactly as S12 intended.
- A `prepareNonLockscreenAodEntry()` call creates one explicit session capability. Only that pre-armed session may pass the native-scene gate while the native transition is the finished `GONE` state or `GONE -> DOZING/AOD`; Bouncer and Occluded never qualify.
- When that native transition becomes presentation-eligible, the controller releases the scoped bypass and **does not run the generic non-animated ClockPlugin resync**. Normal false -> true recovery such as `PRIMARY_BOUNCER -> LOCKSCREEN` still performs the S12 resync.
- A new setting, `non_lockscreen_aod_transition`, is exposed under the Clock page with `animated` as the default/recommended mode and `direct_final` as the alternative. The setting is live and is snapshotted once per new screen-off session.
- `animated` uses the unchanged S11 non-dozing pre-arm followed by the proven AOD entry morph. `direct_final` uses a separate pre-arm seam whose first drawable presentation is already the normalized Doze/AOD endpoint, and the first real AOD render for that session is kept non-animated.
- Normal Lockscreen -> AOD and AOD -> Lockscreen transitions do not read this preference and retain their existing choreography.

0.1.11 verification:

- User physically accepted both isolated S11-based modes before integration.
- Final full JDK 17 gate: **91 suites / 429 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected animation core remains **ZERO_DIFF**.
- Installed candidate is visible `0.1.11`, internal `versionCode=9002`, APK **19,768,335 bytes**, SHA-256 `c93f1ce8e92d964002fa49b34631146725da158524bb311071ee60c6cfbccd1d`; installed `base.apk` matches.
- Merged animated runtime reaches native `GONE -> DOZING FINISHED` with `releasedNonLockscreenBypasses=1` and `syncedHosts=0`, proving the problematic terminal resync is skipped for the pre-armed session.
- Merged direct-final runtime logs `COUI non-lockscreen AOD direct-final render kept animation disabled`, then releases the same bypass with `syncedHosts=0`.
- Bouncer regression remains green: entry logs `hiddenHosts=1`; `PRIMARY_BOUNCER -> LOCKSCREEN` returns with `syncedHosts=1` and `releasedNonLockscreenBypasses=0`.

## S13 — Native Doze transition progress capability adapter

Status: **green — native continuous progress normalized and capability-gated; current OOS remains observe-only because S10 denies consumption**

Goal: implement ADR 0016 without introducing a second animation clock or changing the protected 550 ms presentation engine.

Implementation:

- Added `NativeDozeTransitionProgressAdapter` over the same validated `KeyguardTransitionRepositoryImpl#emitTransition(TransitionStep, boolean)` seam used by S12 scene eligibility. No new SystemUI hook or module-owned timer is introduced.
- Only ordinary `LOCKSCREEN -> DOZING/AOD` and `DOZING/AOD -> LOCKSCREEN` handoffs are recognized as presentation-progress directions. `GONE -> DOZING`, Bouncer, Occluded and ambient-internal transitions are excluded.
- `transitionProgress` preserves the native TransitionStep 0-to-1 direction. `ambientFraction` normalizes both directions so `0.0 = Lockscreen` and `1.0 = ambient`; leaving ambient therefore maps to `1 - transitionProgress`.
- A finite STARTED/RUNNING/FINISHED endpoint is a valid native sample, but continuous capability is not claimed until the exact transition has produced at least one real RUNNING sample (`continuousObserved=true`). Endpoint-only transitions therefore fall back to lifecycle endpoints instead of becoming synthetic continuous motion.
- `PixelAodRuntimeState` exposes a future consumable ambient-fraction seam, but it returns no value unless all gates are true: reliable native sample, continuous RUNNING evidence, S10 `allowsVendorProgress=true`, and Android animations enabled. No S13 presentation code consumes the fraction.
- Current OOS keeps `displayNeedsBlanking=false`, `shouldControlScreenOff=false`, `shouldAnimateDozingChange=true`; therefore `allowsExistingMorph=true` but `allowsVendorProgress=false`. The existing Pixel morph remains authoritative on this device.

Verification:

- Final full JDK 17 gate: **92 suites / 439 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected animation core **ZERO_DIFF**.
- Final visible candidate is `0.1.12`, internal `versionCode=9003`, APK **20,348,979 bytes**, SHA-256 `78495023df574e43610cc2f20c6e676878558b3e960f839368b947e3fd45355a`; installed device `base.apk` matches exactly.
- Strict physical `Awake + showing=true` Lockscreen -> Dozing produced native `LOCKSCREEN -> DOZING` progress with `reliable=true`, `continuousObserved=true`, normalized `ambientFraction=1.0` at FINISHED, `vendorProgressAllowed=false`, and `canConsume=false`.
- Physical Dozing -> Lockscreen reports normalized `ambientFraction=1.0` at STARTED and `0.0` at FINISHED. The final snapshot also records whether real RUNNING samples occurred for that exact transition.
- SystemUI PID `10838` remains healthy with no current crash/ANR/fatal-signal/SIGSEGV/OOM/DeadSystemException match; Android animator/transition/window scales remain `1.0 / 1.0 / 1.0`.

Remaining P1 progress work:

- Doze transition progress and scene eligibility are represented by read-only adapters. S14 below adds the first unified/typed vendor suppressor capabilities without changing the S3 raw-low-battery fail-open rule.

## S14 — Unified vendor ambient suppression capabilities

Status: **candidate green — exact current-OOS suppressor seams, typed consumer boundaries, full source/build gate, reversible Battery Saver runtime evidence, and normal AOD smoke verified**

Goal: implement ADR 0005 + ADR 0032 by consuming only suppression decisions that the current OPlus/SystemUI binary actually owns, keeping capability semantics independent instead of turning every native suppressor into one broad module boolean.

Native seam discovery on the exact installed SystemUI (`SystemUI.apk` SHA-256 `18ac7d6b40081fdd913d656e9f436bf583d559829661c1f14383aef80d9134a6`):

- `CentralSurfacesCommandQueueCallbacks.suppressAmbientDisplay(boolean)` forwards the boolean unchanged to `DozeServiceHost#setAlwaysOnSuppressed(boolean)`. `DozeServiceHost` stores `mAlwaysOnSuppressed` and notifies `DozeHost.Callback`; `DozeSuppressor` responds by requesting `DOZE` while suppressed and `DOZE_AOD` when otherwise eligible. This is authoritative **base-AOD** suppression, but the binary does not prove it should also suppress wake gestures, authentication pulses, or notification pulses.
- `BatteryControllerImpl#setPowerSave(boolean)` refreshes `mAodPowerSave` from `PowerManager.getPowerSaveState(14).batterySaverEnabled`. Current `DozeParameters#getAlwaysOn()` and `DozeSuppressor` consume that field for AOD, while `PulseBatterySaverSuppressor#shouldSuppress()` returns the same field and notification pulse policy checks it. Therefore `mAodPowerSave=true` authoritatively denies **base AOD + notification pulse**.
- Clearing one suppressor is not treated as proof that every other suppressor is clear. Contextual presentation, wake gestures, and authentication pulse remain `UNKNOWN`; notification pulse returns to `UNKNOWN` when AOD power save clears. Missing or partial native observations therefore fail open to the existing vendor lifecycle instead of manufacturing an allow decision.

Implementation:

- Added pure `VendorAmbientSuppressionCapabilities` with typed `ALLOW / DENY / UNKNOWN` decisions and reason diagnostics for base AOD, notification pulse, contextual presentation, wake gestures, and authentication pulse.
- Hooked `DozeServiceHost#setAlwaysOnSuppressed`, `BatteryControllerImpl#setPowerSave`, and constructor/`DozeSuppressor` seed paths. The later `DozeSuppressor` seed recovers live host/battery state even when the singleton host/controller existed before module hook registration.
- Base continuous AOD policy reads only `baseAod`; trigger-only vendor transient presentation is not incorrectly denied by a base-AOD-only suppressor. Suppression clear only refreshes current policy consumers and still requires real native AOD availability/lifecycle before anything can render.
- Notification-posted pulse candidates read only `notificationPulse`. Native AOD power-save suppression produces `vendor-suppression-blocked / vendor-aod-power-save`; `UNKNOWN` leaves the established pulse policy untouched. Snapshot/ranking observations remain observe-only.
- Raw battery percentage remains diagnostic/fail-open exactly as S3/ADR 0028 require; S14 does not invent a low-battery threshold or claim a low-battery suppressor seam that was not found.
- Replaced three local `TextUtils.isEmpty` string-helper calls in `OosAodLifecycleAdapter` with equivalent pure-Java null/empty checks so the new typed pulse policy can be JVM-tested without Android stub behavior; runtime semantics are unchanged.

Verification:

- Final full JDK 17 gate: **94 suites / 447 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected animation core **ZERO_DIFF**.
- Final visible candidate is `0.1.13`, internal `versionCode=9004`, APK **20,352,894 bytes**, SHA-256 `cfc6c6e1b88b5749f44dbad1bf998f7ef11a57f10c8ea57bad1b1e46991e4383`; installed device `base.apk` matches exactly.
- Runtime hook installation reports `host=true battery=true suppressorSeed=true`; the live seed reaches `alwaysOnSuppressed=false`, `aodPowerSave=false`, `baseAod=ALLOW`, while unproven capabilities remain `UNKNOWN`.
- Reversible real Battery Saver validation began and ended with Android `low_power=0`. Enabling Battery Saver produced `aodPowerSave=true`, `baseAod=DENY`, `notificationPulse=DENY`; restoring it produced `aodPowerSave=false`, `baseAod=ALLOW`, `notificationPulse=UNKNOWN`. Contextual/wake/auth remained `UNKNOWN` throughout.
- The Android statusbar shell does not expose a `suppressAmbientDisplay` command, so no synthetic mutation of that system-level state was attempted. Its exact command-queue -> host -> DozeSuppressor binary path is retained as source evidence rather than bypassing SystemUI ownership for a test.
- After restoration, a normal Awake -> Dozing -> Awake smoke kept SystemUI PID `17877` stable. Current-PID crash/ANR/fatal-signal/SIGSEGV/OOM/DeadSystemException scan is empty; Android animator/transition/window scales remain `1.0 / 1.0 / 1.0`.
- Evidence is retained under `.local/m9_s14_0.1.13/`; exact decompiled SystemUI classes remain under `D:\Downloads\Xposed_test\pixel-aod-shared\systemui-analysis\s14-classes`.

## S15 — Vendor proximity pause capability

Status: **candidate green — exact current-OOS vendor dwell seam, full source/build gate, final hook installation, real FAR/reset lifecycle evidence, and normal AOD smoke verified; sustained physical NEAR remains a manual hardware-interaction check**

Goal: implement ADR 0004 without creating a second proximity sensor, timer, or panel/doze power owner. Pixel AOD should retain the current presentation during a brief obstruction, hide only after the vendor proximity dwell commits, and reuse the same normalized gate for notification-pulse eligibility.

Native seam discovery on the exact current OOS SystemUI:

- `OplusWakeUpController` maintains both raw `proximityNearEvent` and committed `proximityNear` state. Its sensor callback removes the previous pending `ProximityTask`, calls `ProximityTask#setNear(...)`, and lets OPlus own the dwell.
- Raw `NEAR` is posted to the vendor handler after **1000 ms when LCD-AOD mode is supported or 1500 ms otherwise**. Raw `FAR` removes the pending task and executes it immediately, so a short obstruction is canceled before committed near state is published.
- `ProximityTask#run()` writes committed `proximityNear` and calls `notifyProxCallback()`. `getProxNear()` exposes this committed state; `getProxNearForLuxAod()` exposes the raw event state. `unregisterProximitySensor()` cancels pending work and clears the vendor state.
- Because the current ROM already owns the required dwell/cancel behavior, adding the ADR's own second 1.5-second timer after `getProxNear()` would approximately double the OLED path. S15 therefore observes the vendor timer instead of duplicating it.

Implementation:

- Added pure `VendorProximityPauseAdapter` with `ACTIVE`, `PAUSING`, and `PAUSED` presentation phases. It owns no sensor, handler, timer, wake lock, or display power transition.
- Hooked `ProximityTask#setNear(boolean)` as the raw request edge. Raw `NEAR` enters `PAUSING`; raw `FAR` cancels `PAUSING` unless a previously committed near state is still authoritative.
- Hooked `ProximityTask#run()` **after** the OPlus method completes, so the normalized commit follows rather than precedes vendor state ownership. A committed `NEAR` enters `PAUSED`; committed `FAR` returns to `ACTIVE`.
- Hooked `OplusWakeUpController#unregisterProximitySensor()` to reset normalized state fail-open. Existing committed `getProxNear()` observation remains as the fallback path if the dedicated task seam is absent on another ROM.
- Current Pixel AOD presentation still reads the committed proximity authority, so `PAUSING` keeps already-visible AOD pixels on screen and only `PAUSED` hides them. Notification-posted pulse candidates use the stricter normalized gate and are blocked during both `PAUSING` and `PAUSED`.
- Wake-trigger and authentication behavior are deliberately not redefined by S15. Those consumers keep their existing committed/native behavior for the later ADR 0007 wake-trigger slice.

Verification:

- Final full JDK 17 gate: **95 suites / 453 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected animation core **ZERO_DIFF**.
- Final visible candidate is `0.1.14`, internal `versionCode=9005`, APK **20,355,847 bytes**, SHA-256 `a36892ad048b6907cf85a21ae93f0c9575c171354dfd3b06a7323ac5c4aeaae2`; installed device `base.apk` matches exactly.
- Final SystemUI PID `8366` emits the S15 proximity unregister/reset lifecycle path with no matching task/reset hook failure and no current crash/ANR/fatal match.
- A controlled Awake -> Dozing -> Awake cycle kept PID `8366` unchanged. Real uncovered-device runtime captured OPlus proximity FAR/unregister/reset activity; no synthetic NEAR state was injected.
- No synthetic NEAR state was injected. The exact binary proves the sustained-NEAR delay/commit path and unit tests cover NEAR -> PAUSING, early FAR cancellation, delayed PAUSED commit, FAR resume, stale-raw protection, and lifecycle reset; physical hand-cover NEAR remains a non-blocking manual check.
- Current-PID crash/ANR/fatal scan is empty. Android `low_power=0`, animator/transition/window scales remain `1.0 / 1.0 / 1.0`, and the existing `non_lockscreen_aod_transition=direct_final` preference remains unchanged.
- Evidence is retained under `.local/m9_s15_0.1.14/`; exact decompiled proximity classes remain under `D:\Downloads\Xposed_test\pixel-aod-shared\systemui-analysis\s15-classes`.

## S16 — Vendor wake-trigger adapter + compact weather alignment

Status: **green candidate — exact OPlus post-classification wake authority integrated, duplicate lower-level wake actions suppressed, 0.1.15 installed/hash-verified, and requested weather alignment physically captured**

Goal: implement ADR 0007 without registering a duplicate low-power sensor stack or creating an independent wake window, while making the user-requested compact current-weather row sit slightly higher without touching stable clock animation math.

Native seam discovery on the exact current OOS SystemUI:

- `OplusWakeUpController#notifyWakeUpCallback(int)` is the narrow synchronous fanout after OPlus has already classified its black-screen gesture / tilt / AMD motion inputs. OPlus continues to own gesture registration, motion sensors, proximity state, wake locks and hide alarms.
- Current type `0` originates from the single black-screen click callback; type `1` originates from the tilt/lift path; type `2` originates from the AMD/motion path. The current `onDoubleClick()` callback is empty, so S16 does not invent a double-tap capability.
- The prior broad OPlus callback/`PowerManager#wakeUp` hooks remain diagnostics/fallback only when the exact authority hook is installed, preventing one physical vendor event from starting two Pixel transient observations.

Implementation:

- Added pure `VendorWakeTriggerAdapter`: `0 -> SINGLE_TAP/tap`, `1 -> TILT_PICKUP/pickup`, `2 -> MOTION/motion`; unknown raw values are observe-only.
- Added explicit `TRIGGER_MOTION` / `motion-vendor-transient` classification to the existing vendor-transient presentation policy. ADR 0056 still owns lifetime: Pixel follows the vendor scene and does not start a fixed timer or force panel state.
- Wake-trigger presentation reuses the module replacement schedule, committed S15 proximity authority, existing power gate, current lockscreen/privacy content handling, and the typed `wakeGestures` suppression slot. The latter remains `UNKNOWN` on this ROM and therefore does not fabricate a denial.
- Subordinate OPlus callback methods and `PowerManager#wakeUp(...)` are diagnostic-only while `notifyWakeUpCallback(int)` is available. If the exact seam is absent on another ROM, the previous fallback diagnostics remain available rather than black-screening presentation.
- Compact weather layout was raised by exactly **2 dp** in both active and legacy paths: `CouiClockGeometryPolicy.DATE_WEATHER_GAP_DP 3 -> 1` for the active COUI small scene, and `COUI_COMPACT_DATE_TO_WEATHER_TOP_OFFSET_DP 27 -> 25` for the shared fallback. Type sizes, date anchor, notification/context minimum anchors and clock position are unchanged.

Verification:

- Final JDK 17 gate: **96 suites / 458 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected clock/morph/weight animation core **ZERO_DIFF**.
- Final visible candidate `0.1.15`, internal `versionCode=9006`, APK **20,374,389 bytes**, SHA-256 `9d9462d4cf906958e3396e10a46c7fa241b4a1c40d84ee514c46d1be74c64a29`; installed device `base.apk` matches exactly.
- Final SystemUI PID `21570` reports `installed OPlus vendor wake-trigger authority hooked=true ... seam=notifyWakeUpCallback(int)` with no corresponding hook failure. Controlled Awake -> Dozing -> Awake retained the same PID.
- Runtime on the same S16 wake implementation captured an actual OPlus type-0 observation as `rawType=0,kind=SINGLE_TAP,normalizedTrigger=tap`; type 1/2 are covered by exact decompiled origin plus pure unit tests rather than internal callback injection.
- `.local/m9_s16_0.1.15/keyguard.png` physically confirms the current-weather row moved upward and is visually bottom-aligned with the clock while the date/notification/card geometry stayed stable.
- Current PID crash/ANR/fatal scan is empty; `low_power=0`; animator/transition/window scales are `1.0 / 1.0 / 1.0`; `non_lockscreen_aod_transition=direct_final` and `debug_logging=true` remain unchanged.

## S17 — Selective biometric/auth presentation adapter

Status: **green candidate — exact current-OOS UDFPS TouchDown authority integrated without taking biometric/HBM/panel ownership; 0.1.16 installed and hash-verified**

Goal: implement ADR 0017 using one reliable current-ROM biometric presentation signal, while keeping S15 proximity, S16 wake triggers, notification pulses, continuous AOD, UDFPS optical/HBM, and display power independent.

Native seam discovery and correction:

- AOSP `DozeSensors`/`DozeMachine` classes exist in the exact SystemUI and expose Android 17 pulse-reason semantics, but physical Awake -> Dozing transitions on this OPlus build did not traverse the hooked `DozeMachine#transitionTo(...)`. S17 rejected that initially plausible seam instead of treating class presence as runtime authority.
- The active OPlus path is `OplusBiometricAuthController#showUdfpsOverlay(int)`. Exact decompilation maps reason `8` to `OnScreenFingerprintUiMech.onFpTouch(true)` plus `DreamPolicy.onFpTouchDown()`, and reasons `9/10` to matching TouchUp callbacks.
- Physical device runtime independently captured `fingerprint trigger Down -> SensorOverlays showUdfpsOverlay : 1, 8 -> OnScreenFingerprintUiMech showUdfpsOverlay reason=8 -> touchEvent isDown true -> fingerprint capture/authentication`, proving that reason 8 is not a fabricated semantic mapping.

Implementation:

- Added pure `SelectiveBiometricPulseAdapter`. Native reason `8` enters `AUTH_UI_ONLY`; reasons `9/10`, `hideUdfpsOverlay()`, successful `setFingerprintAuthenticated(true)`, or a new ordinary icon-show reason `0..6` restore `IDLE`.
- Unsupported reasons never invent a restricted presentation and also do not prematurely clear an already-active hardware TouchDown.
- While `AUTH_UI_ONLY` is active, the normal Pixel AOD module policy and trigger-only entry path yield ordinary Pixel clock/content. Stock/native biometric presentation can therefore remain authoritative for that vendor-owned touch lifetime.
- S17 does not call `showUdfpsOverlay`, request authentication, register a sensor, mutate `OnScreenFingerprintPressedIcon`, retime authentication, create a timer, or touch optical illumination/HBM/local-HBM/panel state.
- The current active OPlus path exposes no separately proven no-UI or bright pulse classification, so those ADR capabilities remain absent rather than inferred from unrelated AOSP classes.

Verification:

- Final JDK 17 gate: **97 suites / 464 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected seven-file clock/morph/weight animation core **ZERO_DIFF**.
- Final visible candidate `0.1.16`, internal `versionCode=9007`, APK **19,784,719 bytes**, SHA-256 `bd3ba65cee8656fbcbac448605765427097a16c8fd4524a4fd2549111b5b1e8a`; installed device `base.apk` matches exactly.
- Final SystemUI PID `9781` reports S17 `show=true hide=true authState=true`; the real startup fingerprint icon session emits `showUdfpsOverlay(4)` and remains `IDLE` as intended.
- A controlled Awake -> Dozing -> Awake cycle kept PID `9781` unchanged and `.local/m9_s17_0.1.16/aod.png` shows a complete normal Pixel AOD scene. Current-PID crash/ANR/fatal scan is empty; the broader logcat SIGABRT hit is historical at 15:02 under old PID `18968`.
- ADB touchscreen injection at the fingerprint location did not reach the fingerprint HAL and was not counted as biometric validation. No internal `requestPulse`, biometric callback, or UDFPS method was invoked to manufacture a passing auth edge.
- `low_power=0`; animator/transition/window scales remain `1.0 / 1.0 / 1.0`; `non_lockscreen_aod_transition=direct_final` remains unchanged.
## S18 — Contextual target arbiter

Status: **green candidate — existing module contextual sources centralized behind one deterministic ADR 0018 arbitration boundary; 0.1.17 installed/hash-verified and real SystemUI AOD execution proven**

Goal: implement the common contextual owner before adding any new native Smartspace or Live Update adapter. S18 must preserve current Weather Alert / Calendar / Forecast behavior while creating one place for future native inputs to compete by validity, privacy, suppression, deduplication and low-power budget.

Implementation:

- Added immutable `ContextualTarget` inputs with source, urgency, semantic identity, validity/TTL, selected-user/privacy eligibility, typed contextual suppression eligibility, presentation eligibility and visual-budget cost.
- Added one pure `ContextualTargetArbiter`. It filters invalid/ineligible inputs first, then deduplicates semantic equivalents, ranks deterministically, and commits at most the current one-row contextual budget to the existing `ContextualAtAGlanceCard` scene owner.
- Existing module Weather Alert, Calendar and Weather Forecast now produce candidates instead of independently short-circuiting the selector. Their established visible order remains Weather Alert > Calendar > Forecast.
- Equivalent native-vs-module deduplication prefers the valid native representation, but deduplication runs **after** eligibility. A stale, privacy-blocked, suppressed or otherwise ineligible future native target therefore cannot suppress the explicit module fallback.
- S14's typed `contextualPresentation` capability now has a single consumer. Only explicit `DENY` blocks contextual candidates; the current ROM still reports `UNKNOWN`, which remains fail-open to the established vendor lifecycle and does not borrow base-AOD or notification-pulse suppression semantics.
- Weather Alert durable `markVisible(...)` moved after arbitration. Only a Weather Alert that actually wins and is on a visible surface consumes its display/repeat window, preventing a future native equivalent from burning the module fallback before the fallback is ever rendered.
- `NATIVE_SMARTSPACE`, `LIVE_UPDATE`, and `NATIVE_AMBIENT_INDICATION` exist only as normalized source classes for later adapters. S18 does **not** fabricate a SystemUI Smartspace target seam, classify ordinary notifications as Live Updates, or clone a native Ambient Indication surface without current-ROM evidence.
- Added one low-frequency diagnostic containing only selected source/kind/counts and typed suppression state, never contextual body text or semantic identity. This gives runtime proof of arbiter execution without exposing calendar/weather content.

Verification:

- Final JDK 17 gate: **98 suites / 470 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` PASS; `git diff --check` PASS; protected seven-file clock/morph/weight animation core **ZERO_DIFF**.
- Focused arbiter coverage verifies validity/TTL, privacy/suppression/presentation filtering, native-equivalent preference with module fallback, deterministic urgency/expiry ordering, the one-row visual budget, and the rule that a suppressed Weather Alert does not consume its visible window.
- Final visible candidate is `0.1.17`, internal `versionCode=9008`, APK **20,381,430 bytes**, SHA-256 `c47174e8726f678becd33f97a96114fa278768d3462809eb52d5d18765cd4928`; installed device `base.apk` matches exactly.
- Final SystemUI PID `31776` emits real `contextual arbitration ... selectedSource=NONE selectedKind=NONE eligible=0 deduped=0 suppression=UNKNOWN` from `ClockPlugin#render#presentation` while the device is genuinely Dozing. No contextual candidate existed in that sample, so the correct output is an empty contextual row rather than fabricated content.
- Controlled final `Awake + isKeyguardShowing=true -> Dozing` kept PID `31776` unchanged. `.local/m9_s18_0.1.17/aod.png` shows a complete normal AOD with clock, date/weather, notification icons, fingerprint and battery; current-PID crash/ANR/fatal scan is empty.
- `low_power=0`; animator/transition/window scales remain `1.0 / 1.0 / 1.0`; `non_lockscreen_aod_transition=direct_final` and `debug_logging=true` remain unchanged.

## Next implementation checkpoint

S18 establishes the common contextual owner without inventing new native content. The next recommended small P1 slice is **S19 — current-OOS native contextual/Smartspace source adapter / ADR 0003 + ADR 0011**: first identify a stable target surface in the exact installed SystemUI, then map only proven read-only target identity/text/TTL/privacy metadata into `ContextualTarget`. If the current ROM exposes no reliable Smartspace target seam, leave it absent rather than recreating a private provider and move to the next evidence-backed contextual source such as ADR 0013 Live Updates.
Validated independent stages are checkpointed and pushed to the current development branch. Merge/push to `master`, history rewriting, force-push, formal tags/releases, and other stable-history operations still require explicit user authorization.