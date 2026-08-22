# M8 Architecture Convergence

Status: **complete — 0.1.383 verified**  
Started: 2026-08-21  
Input behavior golden: `v0.1.380` / release commit `a67ea2e0e42927a037980515661da0e267bacaa6`
Stable output: `v0.1.383 / 393` on `master`; annotated tag `v0.1.383` is the post-M8 rollback point
Runtime convergence commit: `46adb50cb84ff8e680bf42f5fa8b43d26be6f137`
Implementation branch: `agent/m8-architecture` (integrated)

## Stable rollback points

- `v0.1.383`: current post-M8 stable baseline. Use this tag for normal rollback/rebuild of the converged architecture.
- `v0.1.380`: pre-M8 behavior golden. Use this when diagnosing whether a regression was introduced by architecture convergence itself.
- Source rollback is tag-based (`git switch --detach v0.1.383` or `v0.1.380`); the stable APK should be rebuilt from the selected tag and installed with the normal overwrite path rather than reverting individual M8 slices in-place.

## Contract

M8 is architecture-only. Released 0.1.380 remains the visual/runtime golden:

- COUI_PORT is the sole primary clock owner.
- LS↔AOD clock geometry, weight handoff, content anchors, weather, Forecast, media, notification and burn-in behavior must not change.
- OPlus owns the primary fingerprint glyph/pressed/HBM/local-HBM/AOD-FOD visuals when `pixel_fingerprint_icon=false`; Pixel AOD may independently own the configured success ripple.
- Modern Xposed remains API `101/101`, `staticScope=true`, with only `com.android.systemui` in scope and no legacy `assets/xposed_init`.
- Version/tag rollback replaces parallel runtime clock architectures.
- Existing unrelated dirty/untracked local files are excluded from all M8 commits.

## S1 — Collapse clock-owner startup selection — PASS

Candidate `0.1.381 / 391`:

- removed hidden `clock_renderer` startup selection, `ClockRendererPolicy` and `ClockRendererStartupRouter`;
- made `ActiveClockRendererController` permanently delegate to `CouiClockPluginHostController`;
- removed selector-specific tests and added a fixed-owner convergence contract.

Gate: full JVM **376/376 PASS**, clean build/diff/metadata PASS, device install/hash PASS, one intentional SystemUI reload `17052 -> 9591`, fresh log `COUI clock startup owner=COUI_PORT fixed=true`, 3/3 LS↔AOD PASS, no FATAL/ANR.

## S2 — Remove dead legacy primary clock owner — PASS

Candidate `0.1.382 / 392`:

- deleted `ClockPluginHostController.java` and `PixelClockPluginHostView.java`;
- deleted unreachable legacy primary-clock injection, fallback/reapply and old startup branches;
- retained `PixelAodClockView` / `PixelLockscreenClockView` only where their shared semantic/state implementation is still active.

Gate: full JVM **377/377 PASS**, clean APK 19,732,031 bytes, SHA-256 `2231197054F0F9DFFF6A1D62BAFC4E8F6EC9B50B1BF843422AEFAFED9FEDD1D6`, metadata PASS, install/hash PASS, one intentional reload `9591 -> 18063`, 3/3 LS↔AOD plus LS endpoint PASS, no FATAL/ANR.

## S3 — Presentation/shared-service boundary — PASS

M8 deliberately uses a facade-first extraction rather than moving the M7-proven state machine in one rewrite.

New narrow boundaries:

- `PixelAodTypography` — font/typeface, text styling/measurement and presentation colors;
- `PixelAodContentState` — current weather, contextual-card, calendar and notification semantic access;
- `PixelAodRuntimeState` — interaction/AOD trace/entry-transition and shared runtime-state access.

COUI presentation classes, contextual presentation, calendar client and the Modern entry no longer call `PixelAodClockView.*` utility methods directly. The proven implementation remains behind the facades for this release; `CouiClockHostView` still names the existing `WeatherSnapshot` data type, intentionally avoiding a risky data-model rewrite.

Gate: full JVM **377/377 PASS**, `git diff --check` PASS; no presentation behavior was changed.

## S4 — Domain hook installers — PASS

Registration ownership is now explicit without relocating mature reflection-hook implementations:

- `PixelAodLifecycleHookInstaller`
- `PixelAodNotificationHookInstaller`
- `PixelAodSurfaceHookInstaller`
- `PixelAodUdfpsHookInstaller`

`PixelAodHook.install()` preserves the accepted 0.1.380 registration order while delegating registration responsibility to those domain installers. Hook implementation methods stay in place, reducing refactor risk while creating stable extraction seams for later maintenance.

Gate: full JVM **379/379 PASS**, `git diff --check` PASS; architecture regression coverage confirms shared facades and domain installers are packaged.

## S5 — UDFPS ownership isolation — PASS

Added `PixelAodUdfpsRuntimePolicy` as the runtime ownership boundary:

- primary-glyph replacement request;
- system-primary-glyph release ownership;
- success-ripple enablement;
- COUI replacement ownership;
- COUI success-ripple ownership;
- COUI-vs-legacy renderer routing.

`CouiUdfpsController`, `PixelFingerprintIconController` and the UDFPS hook path no longer interpret the primary-glyph/success-ripple settings independently. Optional replacement behavior and the legacy renderer are retained; M8 does **not** remove product capability merely because stable settings use the system glyph.

Gate: full JVM **380/380 PASS**, `git diff --check` PASS. Outside schema/settings definitions, direct reads of `pixel_fingerprint_icon` / `udfps_success_ripple` are centralized in `PixelAodUdfpsRuntimePolicy`.

## S6 — Debt/docs/test cleanup — IMPLEMENTED

- final M8 candidate consolidated as `0.1.383 / 393`;
- architecture/convergence regression tests retained with the current owner model;
- roadmap and implementation-status documents point to this file as M8 authority;
- deprecated API/build warnings were audited. The existing `navigationBarColor` deprecation and manifest `extractNativeLibs` AGP warning are intentionally not rewritten inside M8 because the obvious replacements can alter edge-to-edge or native-library packaging behavior; they remain non-blocking cleanup candidates for a future UI/build-system-specific change;
- `tools/extract_pixelaod_logs.ps1` is a pre-existing local dirty file and is deliberately excluded rather than normalized by M8;
- no helper cleanup is allowed to absorb `%SystemDrive%/`, `%USERPROFILE%/`, local reference APKs, `Python/` or `recordings/`.

## Final exit gate — PASS

Automated final gate on JDK 17.0.19:

- clean `:app:testDebugUnitTest :app:assembleDebug` PASS;
- complete JVM suite **380/380 PASS**, 0 failures/errors/skips;
- `git diff --check` PASS;
- exact main-source audit finds no removed legacy clock-owner/router/injection symbols;
- COUI presentation has no direct `PixelAodClockView.*(...)` utility calls;
- final APK is **19,748,415 bytes**, SHA-256 `7C117B0398A8556F60390581383CE386AE9FAA51FE12F70412F7F80F681A0081`;
- Modern Xposed metadata remains API `101/101`, `staticScope=true`, scope exactly `com.android.systemui`, Java init `dev.codex.pixelaod.PixelAodModernEntry`, with no legacy `assets/xposed_init`.

Physical final gate on CPH2573 / OP595DL1 (`4a851996`):

- standard `adb install -r` PASS; device reports `0.1.383 / 393`; installed `base.apk` hash exactly matches the final artifact;
- settings persisted: `pixel_fingerprint_icon=false`, `udfps_success_ripple=true`, `debug_logging=false`, Forecast configuration unchanged;
- exactly one final-candidate SystemUI reload through the repository verification helper changed PID `11051 -> 2668`;
- fresh LSPosed logs load the new base.apk through the Modern entry, report `COUI clock startup owner=COUI_PORT fixed=true`, and install 34 COUI UDFPS hooks;
- 3/3 explicit LS↔AOD cycles keep PID `2668` and reach Awake/Dozing correctly;
- final Small/content evidence: `.local/m8_final_01383/aod.png` and `.local/m8_final_01383/ls.png` show accepted clock/date/weather/notification/Charging behavior;
- media lifecycle PASS: PixelPlay reaches PLAYING and `.local/m8_final_01383/media.png` shows title/artist + notification row, then MEDIA_STOP reaches `NONE`; `.local/m8_final_01383/after_media_aod.png` returns to notification-only with no stale media row;
- reversible notification snooze was used only to expose final zero-content endpoints. `.local/m8_final_01383/large_empty_aod.png` is genuine AOD Large/empty; after ending the PixelPlay test process and a normal sleep/wake, `.local/m8_final_01383/ls_large_candidate.png` is genuine LS Large. The Android `Snoozed notifications` queue is empty afterward; no notification was deleted by the module;
- UDFPS ownership remains system-primary: OPlus `OnScreenFingerprintIcon`, pressed and dim windows remain present. The independent `COUIExpressiveUdfpsGlow` idle record is `GONE`, `mHasSurface=false`, `NO_SURFACE`, `isOnScreen=false`, `isVisible=false`, so there is no success-overlay residue. A new enrolled-finger physical authentication was not synthesized during M8; the already accepted M7 real-auth contract is preserved and the M8 policy tests cover the unchanged ownership decision;
- post-reload health: PID remains `2668`; no current-PID FATAL/ANR/crash is present. The only relevant recent `am_proc_died` is PID `11051` at the intentional final reload.

M8 S1-S6 is therefore complete. Commit/push must still exclude the pre-existing dirty `tools/extract_pixelaod_logs.ps1` and all local reference/recording paths.
