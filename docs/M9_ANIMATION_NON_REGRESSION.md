# M9 Animation Non-Regression Baseline

Date: 2026-08-22
Status: Required M9 release constraint

## Goal

M9 parity work must not regress the currently stable Pixel/COUI clock animation system. The protected baseline is the existing implementation of:

- Lockscreen -> AOD transition animation.
- AOD -> Lockscreen transition animation.
- AOD/Lockscreen clock weight interpolation.
- Large <-> small clock size/geometry morphing.
- Colon/glyph continuity and existing geometry handoff during those transitions.

These behaviors are product requirements, not implementation debt to rewrite for internal AOSP resemblance.

## Implementation rule

Prefer changing inputs and ownership around the proven animation engine instead of replacing the engine itself. In particular:

- ADR 0016 may replace the source of Doze progress, but does not authorize new morph math.
- ADR 0040 may recompute typography/geometry after configuration changes, but normal transition interpolation remains stable.
- ADR 0047 adds Android bold-text adjustment to endpoint weights; it must not change the normal weight interpolation contract.
- ADR 0053 may suppress animation only when the vendor declares a snap/display-blanking path.
- ADR 0054 preserves the current 1x timing/visual rhythm and only changes behavior when Android animation scale requires it.
- ADR 0059 invalidates stale callbacks; it does not create a second transition timeline.
- ADR 0063 skips Pixel Lockscreen animation only for an authoritative native direct-to-Gone wake. Ordinary AOD -> Lockscreen keeps the stable baseline.

## Regression scenarios

Before M9 runtime convergence begins, capture the current stable build as the comparison baseline. Every animation-adjacent implementation phase must verify at least:

1. Lockscreen -> continuous AOD with no notifications/media.
2. AOD -> Lockscreen with no notifications/media.
3. Large -> small transition when notification/context occupancy changes.
4. Small -> large restoration after occupancy clears.
5. AOD/Lockscreen weight handoff in both directions.
6. Notification appearance/disappearance during an otherwise valid AOD session.
7. Media appearance/disappearance where the current product intentionally changes geometry.
8. Repeated screen-off/screen-on cycles to catch accumulated transform or stale-callback errors.
9. Android animation scale = 1x, which must preserve the current stable timing and motion.
10. Android animations disabled, where ADR 0054 intentionally snaps to terminal state instead of running decorative module motion.

## Failure policy

If an AOSP/OPlus parity adapter causes new clock jumps, one-frame flashes, weight discontinuities, glyph misalignment, doubled motion, or changed normal 1x timing, treat that as a regression in the adapter integration first. Fix or roll back the parity integration before considering changes to the proven clock animation/morph implementation.

A change to the protected animation engine itself requires a separately reproduced animation defect and dedicated validation; parity cleanup alone is not sufficient justification.

## S9 defect evidence — fixed metric ownership during LS <-> AOD weight morph

The S9 entry investigation reproduced a user-visible post-weight horizontal correction: after a normal Lockscreen -> AOD weight shrink, a digit/pair could move slightly left after the apparent weight animation had finished. Font-metric reproduction showed that the ROM morph path was recalculating horizontal targets from the AOD-weight glyph advance even though the persistent morphing view is supposed to keep a stable per-digit slot. For `21:02`, that mismatch accounted for roughly a 3.8–4.2 px minute-side correction.

The repair changes metric ownership only: ROM morph mode keeps the corresponding Lockscreen Large/Small metric cells for the same scene across a weight-only doze handoff. It does not change the 550 ms duration, the motion curve, colon staging, weight interpolation, Large <-> Small transition math, or transition-generation behavior. The four-set fallback path retains surface-specific metrics.

Post-repair verification on 2026-08-22: full JVM suite **87/402 PASS**, debug APK build PASS, `git diff --check` PASS, and the protected core files listed by the M9 gate remain **zero diff**. The physical capture stream blanks during part of the Lockscreen -> AOD handoff, so that recording is not treated as a complete frame-by-frame animation oracle; final drawable AOD frames remain geometrically stable.

## S10 gate evidence — native screen-off permission without animation-engine rewrite

S10 implements ADR 0053 outside the protected animation engine. The current OOS SystemUI exposes `DozeParameters#getDisplayNeedsBlanking()` and `ScreenOffAnimationController#shouldAnimateDozingChange()` as read-only native inputs. Pixel keeps the existing Lockscreen -> AOD morph when those signals do not explicitly deny motion, and snaps only the already-computed presentation endpoint when physical display blanking is required or SystemUI explicitly rejects the dozing-state animation.

A real-device semantic correction was required before acceptance. The first S10 candidate treated `DozeParameters#shouldControlScreenOff=false` as an animation denial and therefore snapped the clock. Controlled testing then woke the device to a real Keyguard (`showing=true / mIsShowing=true`) before Lockscreen -> AOD and proved that this OOS build still reports `shouldControlScreenOff=false` while `displayNeedsBlanking=false` and `shouldAnimateDozingChange=true`. That candidate was rejected. The final policy uses `shouldControlScreenOff` only as a positive prerequisite for future vendor Doze-progress consumption; it is not allowed to veto the already-stable Pixel morph.

Final-device evidence under SystemUI PID `23069` shows `allowsExistingMorph=true` on two controlled Keyguard -> Dozing samples, with no S10 snap event for that PID. The final full gate is **88 suites / 408 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and protected animation-core diff **zero**. No 550 ms duration, motion interpolator, colon staging, weight interpolation, Large <-> Small math, or AOD -> Lockscreen choreography was changed. Android screen capture still cannot expose every frame of the physical display handoff, so normal human visual regression observation remains part of the release gate.

## S11 gate evidence — one system animation-scale policy without double-scaling

S11 implements ADR 0054 around the stable animation engine. Android's framework Animator classes already apply `ValueAnimator.getDurationScale()` to the duration passed to `setDuration()`. Pixel AOD therefore deliberately keeps its existing 550 ms (and other feature-specific) baseline durations unchanged whenever animations are enabled. A second manual multiplication would be a regression: `0.5x` would become `0.25x`, while `2x` would become `4x`. Only module-owned non-Animator timing that must remain synchronized with an Animator is explicitly scaled.

When the scale is `0x`, module-owned motion snaps deterministic terminal state instead of relying on a zero-duration Animator side effect. This includes clock/weight presentation, contextual/media transitions, module-owned fingerprint drawable/effect transitions, and the optional custom AOD-exit fade. Vendor authentication, HBM/local-HBM, and other native system motion are not retimed.

The final code gate is **89 suites / 414 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and all seven protected clock/morph/weight core files remain **zero diff**. The final APK SHA-256 is `231b54290bad568c630d26380fa290b780e961e5ce5117697b9cf962881de893`, matching the installed device APK.

Physical validation under SystemUI PID `16409` proved the disabled branch from a genuine Keyguard -> Dozing transition: runtime log `systemAnimation={scale=0.0,enabled=false,default=false}` was followed by the intentional animation-policy endpoint snap. Additional `2x` and restored `1x` Keyguard -> Dozing cycles retained the same PID and no crash/ANR. OOS did not issue a new ClockPlugin target transaction in those enabled samples, so no direct host-log duration measurement is claimed for them; the enabled-scale contract is the unchanged framework Animator baseline plus focused policy tests, while the normal `1x` 550 ms choreography remains the protected regression baseline.
## S12 gate evidence — native Keyguard scene eligibility outside the animation engine

S12 implements ADR 0043 around the stable presentation engine. The current OOS `KeyguardTransitionRepositoryImpl#emitTransition(TransitionStep, boolean)` seam provides authoritative native `from`, `to`, phase, owner and transition value. Pixel uses only the scene semantics in this slice: Lockscreen/AOD/Dozing permit the primary host, while known Bouncer/Occluded/Gone-style scenes suppress it. Unknown/unsupported state preserves the prior fallback rather than becoming a new black-screen condition.

The first physical candidate caught a non-animation integration regression: entering `PRIMARY_BOUNCER` correctly hid the Pixel host, but returning to `LOCKSCREEN` could leave it hidden because OOS did not guarantee an immediate ClockPlugin render. The correction performs exactly one non-animated resync from the current native ClockPlugin state on an authoritative false -> true eligibility edge. This is not a second animation timeline and does not invent a scene, endpoint, duration or geometry.

Final physical evidence shows `LOCKSCREEN -> PRIMARY_BOUNCER` suppression with `hiddenHosts=1`, then `PRIMARY_BOUNCER -> LOCKSCREEN` recovery with `syncedHosts=1`; screenshots confirm no Pixel clock on the Bouncer and correct clock/date/weather/notification restoration afterward. A separate verified Awake -> Dozing sample logs `LOCKSCREEN -> DOZING` as presentation-eligible throughout and ends at the normal settled AOD.

Final S12 gate: **90 suites / 423 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and all seven protected animation-core files remain **zero diff**. Current OOS still reports `allowsVendorProgress=false` because `shouldControlScreenOff=false`, so the observed native transition value is not consumed by the 550 ms clock morph in S12.

### 0.1.11 post-S12 regression repair — non-lockscreen AOD entry

The original S12 acceptance missed one critical scenario: **unlock -> screen-off -> AOD**. User observation later proved the new native scene gate could interrupt the established S11 non-lockscreen pre-arm while SystemUI still reported `GONE`, producing endpoint jumps, a visible disappearance before the vendor blank frame, or a missing clock in attempted repairs. Frame-level visibility logging further showed that OPlus could set the parent `ClockViewRoot` / `CustomOplusKeyguardStyleClock` chain to alpha 0 while the Pixel child host still reported `VISIBLE`; policy-level host logs were therefore not sufficient proof of visible continuity.

A direct A/B against the S11 checkpoint restored the previous position + variable-weight handoff and was physically accepted by the user. A second S11-derived candidate that starts at the final AOD endpoint with no morph was also physically accepted. 0.1.11 integrates both as one live preference instead of keeping divergent builds.

The repair changes only integration ownership around the protected engine. A pre-armed non-lockscreen AOD session receives a narrow scene-gate capability only for finished `GONE` and `GONE -> DOZING/AOD`; Bouncer/Occluded remain denied. When native eligibility becomes true, that session releases the bypass without the generic non-animated ClockPlugin resync. `Animated` retains the original S11 first frame and 550 ms morph; `Direct final` uses a separate pre-arm endpoint and keeps that session's first real AOD render non-animated. Normal Lockscreen <-> AOD does not read the new preference.

0.1.11 gate: **91 suites / 429 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and the seven protected animation-core files remain **ZERO_DIFF**. Runtime proves merged animated/direct-final sessions release their non-lockscreen bypass with `syncedHosts=0`, while `PRIMARY_BOUNCER -> LOCKSCREEN` still performs the S12 recovery resync with `syncedHosts=1` and no non-lockscreen bypass.

## S13 gate evidence — native Doze progress remains outside the animation engine

S13 implements ADR 0016 as a read-only capability adapter over the same native `TransitionStep` seam already proven by S12. It does not wire native progress into `CouiClockSizeTransitionMath`, `CouiClockSizeTransitionLayer`, weight interpolation, glyph/colon staging, or any other protected presentation consumer. No module-owned timer or secondary transition clock is introduced.

The adapter accepts only ordinary Lockscreen <-> DOZING/AOD handoffs, normalizes native 0-to-1 progress into an `ambientFraction` where `0=Lockscreen` and `1=ambient`, and requires an actual native RUNNING sample before calling the signal continuous. `GONE -> DOZING`, Bouncer, Occluded and ambient-internal transitions cannot become progress sources. Even a reliable continuous sample is consumable only if S10 positively reports `allowsVendorProgress=true` and Android animations are enabled.

Current OOS physical evidence remains intentionally observe-only: a real `LOCKSCREEN -> DOZING` transition reaches `continuousObserved=true`, but `shouldControlScreenOff=false` keeps `allowsVendorProgress=false` and `canConsume=false`. The reverse `DOZING -> LOCKSCREEN` path normalizes ambient fraction from 1.0 to 0.0. The existing stable Pixel animation therefore remains the only presentation timeline on this hardware.

Final S13 gate: **92 suites / 439 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and all seven protected clock/morph/weight files remain **ZERO_DIFF**. Installed 0.1.12 APK SHA-256 is `78495023df574e43610cc2f20c6e676878558b3e960f839368b947e3fd45355a`; SystemUI PID `10838` remains healthy and all three Android animation scales remain 1.0.
## S14 gate evidence — typed native suppression outside clock/morph ownership

S14 implements ADR 0005 + ADR 0032 around the existing presentation lifecycle, not inside the clock animation engine. The adapter consumes exact SystemUI suppression state for base AOD and notification pulse while leaving unproven contextual/wake/auth capabilities UNKNOWN. It does not change the 550 ms timing, geometry/weight interpolation, Large <-> Small morph, glyph/colon staging, system animation-scale policy, or the accepted unlocked-screen-off animated/direct-final setting.

Native base-AOD suppression only removes eligibility for continuous Pixel AOD; clearing suppression merely reevaluates the real native availability/lifecycle and never synthesizes a panel wake or transition. Native AOD power-save pulse suppression is applied only to explicit notification-posted pulse candidates. Trigger-only vendor transient presentation and unrelated authentication/wake semantics are not broadened by a base-AOD suppressor.

A reversible physical Battery Saver A/B produced the expected typed `baseAod=DENY / notificationPulse=DENY` while enabled and restored `baseAod=ALLOW / notificationPulse=UNKNOWN` afterward. Battery Saver was returned to its original off state, a normal Awake -> Dozing -> Awake smoke passed on the same SystemUI PID, and the protected seven-file animation core remains **ZERO_DIFF**.

Final S14 gate: **94 suites / 447 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS. Installed 0.1.13 APK SHA-256 is `cfc6c6e1b88b5749f44dbad1bf998f7ef11a57f10c8ea57bad1b1e46991e4383`; SystemUI PID `17877` is healthy and Android animator/transition/window scales remain `1.0 / 1.0 / 1.0`.

## S15 gate evidence — vendor proximity pause remains outside animation ownership

S15 implements ADR 0004 around OPlus's existing `OplusWakeUpController.ProximityTask` dwell and does not touch the clock/morph/weight animation engine. The module registers no second proximity sensor and creates no second timer: raw vendor `NEAR` becomes presentation `PAUSING`, while only the completed vendor task can commit `PAUSED`. Raw `FAR` retains OPlus's own pending-task cancellation and immediate resume semantics.

`PAUSING` intentionally leaves the already-visible Pixel AOD surface untouched, so a brief sensor obstruction cannot create a new clock hide/show animation. Notification pulse eligibility may block during `PAUSING`, but that policy does not retime the 550 ms clock transition, rewrite geometry/weight interpolation, change glyph/colon staging, or modify the accepted `direct_final` non-lockscreen entry preference. Wake-trigger/authentication semantics remain outside S15.

Final S15 gate: **95 suites / 453 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and the protected animation core remains **ZERO_DIFF**. Installed 0.1.14 APK SHA-256 is `a36892ad048b6907cf85a21ae93f0c9575c171354dfd3b06a7323ac5c4aeaae2`; a controlled Awake -> Dozing -> Awake smoke kept SystemUI PID `8366` unchanged, with current-PID crash/ANR/fatal scan empty and Android animator/transition/window scales at `1.0 / 1.0 / 1.0`.

## S16 gate evidence — wake authority and weather alignment outside animation ownership

S16 implements ADR 0007 by consuming OPlus's already-classified `notifyWakeUpCallback(int)` fanout and does not register a gesture/motion sensor, own a wake-window timer, or force display state. Type 0/1/2 are normalized to tap/pickup/motion only after the vendor authority fires; lower callback/PowerManager paths become diagnostic-only when that seam is present. Existing S15 proximity, power, schedule, privacy/content and vendor-scene lifetime gates remain the consumers.

The requested weather adjustment changes only compact information geometry: the active date-to-weather gap and legacy weather anchor are each raised by 2 dp. No clock target, type size, contextual/notification minimum anchor, 550 ms timing, weight interpolation, glyph/colon staging, Large <-> Small morph math, or non-lockscreen transition policy changes.

Final S16 gate: **96 suites / 458 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and the protected animation core remains **ZERO_DIFF**. Installed 0.1.15 APK SHA-256 is `9d9462d4cf906958e3396e10a46c7fa241b4a1c40d84ee514c46d1be74c64a29`; controlled Awake -> Dozing -> Awake kept SystemUI PID `21570` unchanged, current-PID crash/ANR/fatal scan is empty, and Android animator/transition/window scales remain `1.0 / 1.0 / 1.0`.

## S17 gate evidence — biometric presentation is a policy edge, not an animation owner

S17 observes the current OPlus UDFPS authority only. Hardware `showUdfpsOverlay(8)` becomes an auth-UI-only presentation edge; TouchUp/hide/auth-success returns to the existing lifecycle. The adapter never changes the clock target, transition origin, animation duration, weight interpolation, glyph/colon staging, pressed-carrier animation, HBM/local-HBM, or panel power state.

The initially plausible AOSP `DozeMachine` seam was rejected after physical Awake -> Dozing testing showed that the active OPlus AOD path does not traverse it. This correction stayed entirely outside the protected animation engine. A normal final Awake -> Dozing -> Awake cycle renders the complete Pixel AOD scene and retains the same SystemUI PID.

Final S17 gate: **97 suites / 464 tests / 0 failures / 0 errors / 0 skipped**, debug build PASS, `git diff --check` PASS, and all seven protected animation-core files remain **ZERO_DIFF**. Installed 0.1.16 APK SHA-256 is `bd3ba65cee8656fbcbac448605765427097a16c8fd4524a4fd2549111b5b1e8a`; controlled Awake -> Dozing -> Awake kept SystemUI PID `9781` unchanged, current-PID crash/ANR/fatal scan is empty, and Android animator/transition/window scales remain `1.0 / 1.0 / 1.0`.
