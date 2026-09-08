# 0.1.40 clock refresh investigation and bounded code audit

Date: 2026-09-08. Baseline: 0.1.39 / 9038. Candidate: 0.1.40 / 9039.

## Evidence and confidence

The user reported intermittent old AOD time which updates only after touching the display. Read-only root ADB sampling confirmed the installed baseline, but did not capture the failing occurrence. The following are verified source defects, not a claim that every possible physical display freeze has been reproduced or eliminated.

1. Native refresh hooks reached PixelAodClockView.refreshAllForNativeAodTick, which only iterated legacy INSTANCES. The actual primary owner is COUI. ActiveClockRendererController.onTimeTick existed but had no producer callers. Pure policy tests therefore passed despite a disconnected production path.
2. The 0.1.39 proximity resume/reset fixes also only refreshed legacy instances. A persistent COUI host could return without attach, a fresh clock-plugin transaction, or a timely TIME_TICK broadcast.
3. Calendar retained the timezone captured at construction. A reset minute key did not fix its timezone, and a minute-only key missed same-minute locale/12-hour format changes.
4. A weak-key HOSTS map strongly owned HostRecord, which strongly referenced the key/root. Separately, the HDR attach-listener map's values captured their ImageView keys. Both defeated the intended weak ownership.
5. The exported settings Provider accepted updates without caller authorization and accepted unknown keys. The diagnostic notification receiver also had no permission requirement.

## Repair boundaries

Native ticks and vendor recovery now reach the single active host. A callback already on the main thread stays in its native refresh window; other threads dispatch to main. Visibility and semantic refresh paths check the wall-clock formatting key before drawing. The same-minute native/broadcast duplicates are coalesced instead of invalidating the cache for every TIME_TICK.

The clock takes one wall-clock sample, resets Calendar's timezone, writes digits, then commits its cache. Ancillary date/weather refresh is isolated from primary digit success. A rare gap-recovered diagnostic records only source and missed-minute count; it is not itself evidence of a visible freeze, since pocket sleep can naturally skip minutes.

The host owns its record through a keyed tag; the process-global index has weak keys and values. Detach removes the index entry and record tag using a snapshot, avoiding map iteration across reentrant view callbacks. HDR attach listeners use their callback View instead of capturing the weak-map key.

Settings reads remain available to the SystemUI bridge. Writes accept only exact module, root, system, or shell UIDs; a matching app ID in a different Android user is not sufficient. Only schema-defined keys can be written. NaN, infinities, numeric overflow and malformed numbers fall back to the setting default. The test receiver requires android.permission.DUMP for external callers. This is a review of those boundaries, not a complete security certification of all exported integration endpoints.

## Android lint review

The first non-blocking audit found 31 errors and 95 warnings. Workflow success was not evidence that lint succeeded. The final workflow makes lint failure blocking and emits explicit test/lint summaries.

27 errors were calls/resources newer than minSdk 26. Add guards and fallbacks at the actual API boundaries (including notification visibility, HDR, icon diagnostics, process-name logging, and animation scale), rather than globally raising minSdk or disabling NewApi. Android 12 splash properties live in v31-qualified resources. One cutout-mode error is repaired with supported symbolic constants for API 28/30.

Three UnspecifiedRegisterReceiverFlag errors pointed to legacy branches below API 33, while the new-platform branches already specified export flags. Keep narrowly scoped documented annotations on only these registrations. The pre-33 private inactive-media receiver additionally requires DUMP. Do not add a blanket lint baseline, suppress NewApi, or disable lint errors.

Warnings remain available in the artifact for review. Some reflect necessary SystemUI private/resource lookup APIs, tooling constructors for programmatically constructed views, or sentinel objects with null View references. Others (for example legacy resources/dependencies and Peek layout allocation) are deferred rather than changed speculatively in a clock reliability patch. Field-name matching is locale-independent in both fingerprint controllers.

## Verification

GitHub Actions is the build authority. The candidate runs the full JVM suite, assembleDebug and lintDebug, uploads the APK with its actual SHA-256 file, uploads JUnit XML/HTML and lint reports, and prints UNIT_TEST_SUMMARY / LINT_SUMMARY. The SHA-256 of an artifact ZIP is not the APK SHA-256.

Focused tests cover missed/duplicate/rollback minute updates, same-minute timezone/offset/locale/hour-format changes, actual native/proximity-to-primary source wiring, pre-visible and ancestor visibility recovery, cache commit ordering, weak registry pruning/ownership, HDR listener ownership, write authorization and numeric inputs. Source-wiring tests supplement executable policy tests; they are not instrumented rendering tests.

Physical acceptance is still required for quiet AOD minute changes, pocket entry/exit across a minute boundary, ancestor visibility recovery, Direct Final/Animated entry, normal Lockscreen-AOD weight morph and large/small continuity. Do not use a successful build/install as proof of low-probability symptom closure.

No phone screenshot, foreground-app inspection, display wake/sleep, sensor injection, screen-timeout modification or animation-scale change was needed for this audit. The initial device sampling was read-only. Follow the safe installation/health-check workflow and record its result separately from visual acceptance.
