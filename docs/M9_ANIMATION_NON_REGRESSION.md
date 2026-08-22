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
