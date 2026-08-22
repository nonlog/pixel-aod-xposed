# Android 17 Parity Grill Closure Audit

Date: 2026-08-22
Status: **GRILL COMPLETE**
Scope: Pixel AOD for OPlus, AOSP/Pixel presentation parity with vendor-delegated Doze lifecycle

## Verdict

The Android 17 parity grill is complete at **65 accepted decisions**. The closure audit found no missing ADR ID, no unresolved choice marker, and no remaining AOSP/Pixel-vs-OPlus product or architecture question that requires Q66.

Further work is **M9 implementation and validation**, not continued grilling. A future grill question is justified only if new technical evidence reveals two or more materially different user-visible/product behaviors that cannot be derived from the accepted decisions.

## Baselines audited

### AOSP

The audit rechecked the exact AOSP Android 17 release tag:

- `android-17.0.0_r1`
- tag object: `23149ba144e850fe494a91ffa13f8019455c0804`
- release commit: `94b4c163b7dfe5ce3607f7bb8456f9573f7de57d`

Exact-tag source was re-read for the principal Doze/Keyguard surfaces used during the grill, including `DozeModule`, `DozeMachine`, `DozeUi`, `DozeTriggers`, `DozeSuppressor`, `DozeAuthRemover`, `BurnInInteractor`, `AodDimInteractor`, `DozeTouchInteractor`, `KeyguardClockInteractor`, and `KeyguardWakeDirectlyToGoneInteractor`.

### Pixel AOD runtime

The stable product baseline remains `v0.1.383 / versionCode 393`.

Repository topology at closure:

- `v0.1.383` points to `d29254b` (`docs: mark 0.1.383 stable`).
- Runtime convergence commit is its parent, `46adb50` (`refactor: converge Pixel AOD runtime architecture`).
- The tag commit changes documentation only, so the runtime source at `46adb50` is the runtime source validated for `v0.1.383`.
- The active `agent/m8-architecture` worktree is still at `46adb50`; `origin/master` / the parity worktree include `d29254b`.

This explains why plain `git describe` from the active runtime branch can report `v0.1.380-1-g46adb50` even though `v0.1.383` is the verified stable rollback tag.

## Decision-ledger integrity

Closure checks:

- ADR files present: **65**.
- Unique IDs in `0001` through `0065`: **65/65**.
- Missing IDs: **none**.
- Duplicate IDs: **none**.
- IDs above 65: **none**.
- Non-`Accepted` ADRs: **none**.
- `git diff --check`: **PASS**.

The grill is therefore structurally complete.

## AOSP surface coverage

The accepted ADRs cover the Android 17 surfaces relevant to this product without requiring a second Doze implementation.

### Doze lifecycle and power ownership

Covered by ADRs 0001, 0031, 0035, and 0062. OPlus/SystemUI owns panel state, Doze lifetime, AOD enablement/availability, and terminal shutdown.

### Doze pausing, sensors, falsing, and touch

- proximity/pocket pause: ADR 0004
- wake triggers: ADR 0007
- presentation-only input boundary: ADR 0008
- native Doze touch pass-through: ADR 0055

No module Doze sensor or falsing stack is required.

### Pulse and authentication

- vendor pulse entry/observation: ADR 0002
- selective biometric pulse presentation: ADR 0017
- full-pulse foreground handoff: ADR 0021
- typed suppression and auth exception: ADRs 0032 and 0034
- fully-dozed pulse styling: ADR 0044
- Trigger-only lifetime follows vendor transient scene: ADR 0056
- biometric transient cleanup: ADR 0060

### Suppression and terminal state

- vendor suppression observation: ADR 0005
- DND ambient hard gate: ADR 0022
- vendor low-battery authority: ADR 0028
- typed suppression capabilities: ADR 0032
- terminal/FINISH gate: ADR 0035
- session-wide stale-work invalidation: ADR 0059

### Time, brightness, wallpaper, dimming, and burn-in

- vendor AOD time tick: ADR 0020
- vendor wallpaper ambient state: ADR 0010
- low-power/OPR budget: ADRs 0006 and 0046
- mandatory burn-in: ADR 0050
- native burn-in transform preference: ADR 0052
- native AOD dim/scrim composition: ADR 0051
- screen-off animation eligibility/display blanking: ADR 0053

### Keyguard/clock behavior

- single clock-face scope: ADR 0019
- native clock-size ceiling: ADR 0037
- foreground collision: ADR 0038
- configuration-responsive typography: ADR 0040
- selectable AOD palette: ADR 0041
- native lockscreen theme/seed: ADR 0042
- authoritative Keyguard scene eligibility: ADR 0043
- native clock target/safe region: ADR 0045
- system bold-text weight adjustment: ADR 0047
- direct AOD/Dozing-to-Gone handoff: ADR 0063

### Notifications, media, and contextual content

- Smartspace/native contextual input: ADRs 0003 and 0011
- Live Updates: ADR 0013
- contextual arbiter: ADR 0018
- notification Doze visibility and eligibility: ADRs 0023 and 0024
- ambient indication/Now Playing pass-through: ADR 0029
- native media semantics: ADR 0036
- notification capacity/overflow: ADR 0048
- notification order: ADR 0057
- notification visual metrics: ADR 0058
- final lockscreen notification visibility authority: ADR 0061

### Locale, accessibility, users, and scope

- selected-user/profile privacy: ADR 0014
- system locale/12-24 hour/date formatting: ADR 0027
- RTL: ADR 0030
- accessibility semantics: ADR 0049
- selected-user presentation preferences: ADR 0064
- foldables deferred: ADR 0015
- dock capability-gated: ADR 0009
- MinMode native handoff: ADR 0012
- primary-display-only M9: ADR 0065

No relevant AOSP surface uncovered by this audit creates a new product decision.

## Refinements that are not conflicts

Several later ADRs intentionally refine earlier broad decisions. They are compatible and should be implemented as the later, more specific contract:

1. **ADR 0005 -> 0032 -> 0034**: vendor suppression reasons feed typed capabilities; authentication can remain independently allowed.
2. **ADR 0007 -> 0056**: wake-trigger observation does not create a module-owned timed brief-AOD session.
3. **ADR 0008 -> 0055**: presentation-only behavior is enforced at the actual Doze input-routing boundary.
4. **ADR 0016 -> 0053/0054**: native Doze progress is an animation input only when animation is eligible and while system animation scale allows it.
5. **ADR 0024 -> 0057 -> 0048 -> 0058**: notification eligibility, ordering, capacity/overflow, and visual metrics are distinct responsibilities.
6. **ADR 0031 -> 0062**: native AOD enabled and native AOD available are separate requirements for continuous Pixel AOD.
7. **ADR 0035 -> 0059 -> 0060**: terminal state invalidates the whole ambient session and its module-owned biometric transients.
8. **ADR 0050 -> 0052**: movement is mandatory; native transform is preferred when reliable, with module fallback if unavailable.
9. **ADR 0014 vs 0064**: privacy/content scope and user presentation-preference scope are separate but use the same selected-user boundary.
10. **ADR 0041 vs 0042**: fully-dozed AOD palette and lockscreen theme/seed are intentionally separate color authorities.

The closure audit updated ADR 0005 and ADR 0007 wording to make the first two refinements explicit; this is clarification only, not a new decision.

## Current runtime debt: direct implementation consequences

The runtime still contains substantial pre-M9 behavior that conflicts with the accepted architecture. These are implementation tasks with already-decided outcomes, not new grill questions.

### P0 — lifecycle safety and animation-preserving convergence

These should be addressed first and in small independently testable slices:

1. Remove the `DreamService#setDozeScreenState` OFF-block/rewrite path.
2. Remove `shouldKeepNativeDozeAlive` as authority to keep vendor Doze alive.
3. Remove native-timeout Pixel AOD reassert/keepalive behavior after OPlus has legitimately hidden/finished the ambient scene.
4. Replace the module-owned `TRIGGER_BRIEF_AOD_DURATION_MS = 10_000` lifecycle and `briefAodTriggerUntilAt` expiry with ADR 0056 vendor transient-scene lifetime.
5. Remove the fixed `LOW_BATTERY_AOD_SUPPRESS_THRESHOLD_PERCENT = 15` policy; consume native suppression instead.
6. Keep SystemUI as the general Keyguard visibility authority while preserving only the confirmed OOS unlock-then-screen-off compatibility correction: an otherwise-eligible notification may be corrected from `hidden=true` to visible, but the module must not restore broad low-importance/silent policy ownership or bypass native privacy/profile/media/system exclusions.
7. Add native AOD availability, terminal-state, direct-to-Gone, and shared ambient-session epoch gates.
8. Add screen-off animation eligibility/display-blanking input before using Doze transition progress.
9. Add one system animation-scale policy for module-owned motion.

The P0 rule is **adapter/lifecycle surgery around the existing clock animation engine**, not an animation rewrite.

### P1 — native semantic adapters

Implement capability-gated read-only adapters for the accepted native surfaces where stable OPlus/SystemUI seams exist:

- Doze transition progress and scene eligibility
- suppressor capabilities
- native AOD enablement/availability
- proximity and wake triggers
- pulse classification/foreground collision
- notification eligibility/order/capacity/metrics
- SystemUI media semantics
- power/dozing indication
- wallpaper ambient state and dim/scrim composition
- target/safe region
- native burn-in transform
- ambient indication / MinMode / dock only when a reliable seam is actually present

Missing vendor seams are engineering discovery/fallback cases, not reasons to invent new product policy.

### P2 — presentation parity cleanup

Direct consequences already determined by the ADRs include:

- remove runtime `force_english_date` semantics from SystemUI presentation
- enable and validate RTL instead of manifest `supportsRtl=false`
- react to density/font-scale and `fontWeightAdjustment`
- implement accessibility semantic ownership instead of marking the whole replacement host inaccessible
- remove the normal user-facing `disable_burn_in_offset` path
- replace fixed notification icon limits and `+N` overflow with accepted native/AOSP semantics
- replace direct `MediaSessionManager` enumeration with the native semantic adapter when available
- enforce the 5% OPR release gate
- scope user-facing presentation preferences to selected Android user

### P3 — explicitly deferred or capability-gated

Do not expand M9 scope merely for parity completeness:

- foldable/posture support remains deferred
- secondary/external display replacement remains deferred
- multi-clock registry/theme-engine work remains out of scope
- Pixel-private contextual backends are not reverse engineered
- dock and MinMode are used only if reliable native capability is exposed

## Stable UDFPS release contract

The pre-Grill M7/M8 release decision remains valid and does not require a new question:

- stable release configuration uses the **OPlus native primary fingerprint glyph/pressed/HBM/local-HBM/AOD-FOD lifecycle**
- Pixel AOD may independently provide the configured success ripple
- `pixel_fingerprint_icon=false` is the stable release baseline

The source still contains the optional primary-glyph replacement/legacy renderer path retained by M8. That legacy capability is not required by M9 parity and must not become a dependency of the M9 lifecycle architecture. Its later removal or continued quarantine can be handled as cleanup without reopening the Android 17 parity grill.

## Animation non-regression gate

`docs/M9_ANIMATION_NON_REGRESSION.md` is a hard release constraint.

Protected behavior:

- Lockscreen -> AOD transition
- AOD -> Lockscreen transition
- AOD/Lockscreen weight interpolation
- Large <-> small clock morphology
- glyph/colon continuity and geometry handoff
- normal Android animation-scale `1x` timing and visual rhythm

The main proven animation implementation currently lives around:

- `CouiClockHostView`
- `CouiClockSizeTransitionLayer`
- `CouiClockSizeTransitionMath`
- `CouiClockPresentationModel`
- `CouiClockColonAnimationPolicy`
- `AodInfoWeightHandoff`
- `ClockGlyphMetrics`
- the existing font/typeface handoff helpers

Current `CouiClockPresentationModel.TARGET_TRANSITION_MS` is **550 ms**. M9 parity work has no standing authorization to change that normal-path timing or replace the interpolation/morph math.

Before the first runtime M9 slice, capture a physical `v0.1.383` animation baseline. After every animation-adjacent slice, compare normal 1x LS->AOD, AOD->LS, Large<->Small, notification/media occupancy changes, and weight handoff against that baseline. If a parity adapter causes a jump, flash, doubled motion, weight discontinuity, or changed normal-path rhythm, fix/rollback the adapter integration first.

## Automated verification performed during closure

The active runtime worktree was not modified.

The first Gradle invocation could not resolve the Android SDK because the AgentDock process did not inherit `ANDROID_HOME`; it did not reach compilation/tests. The audit located the existing SDK at `D:\Android\Sdk` and reran Gradle by injecting `ANDROID_HOME`/`ANDROID_SDK_ROOT` for that command only.

Final result:

- `:app:testDebugUnitTest`: **BUILD SUCCESSFUL**
- suites: **80**
- tests: **380**
- failures: **0**
- errors: **0**
- skipped: **0**

No project `local.properties`, source file, build configuration, or runtime behavior was changed to obtain the test result.

## Closure rule

**Do not create Q66 simply because implementation discovers a class name, hook name, field name, or ROM-specific adapter difficulty.** Those are engineering tasks.

Reopen the grill only if new evidence reveals a true undecided behavior, for example two plausible user-visible outcomes that both satisfy the current architecture and cannot be selected from existing ADRs.

Absent such evidence, the next project phase is M9 implementation against ADR 0001-0065 with the animation non-regression gate.
