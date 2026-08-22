# COUI Port Implementation Status

Updated: 2026-08-22

## Current Status

- **Stable release:** `v0.1.380`, release commit `a67ea2e0e42927a037980515661da0e267bacaa6` on `master`.
- **Runtime source baseline:** `e7374956927f7ad8f89a870059b6218f66c1777e` / `0.1.380` / versionCode `390`.
- **M7:** completed and released. Modern Xposed API `101/101`, `staticScope=true`, sole scope `com.android.systemui`; M7 clock/content/weather/system-icon-UDFPS + success-ripple matrix accepted with documented conditional-scene exceptions.
- **M8 branch:** `agent/m8-architecture`, created from the stable release commit. No M8 work is merged to `master` yet.
- **Current M8 candidate:** `0.1.383 / 393`. S1 fixed COUI_PORT as the only startup clock owner; S2 removed the dead legacy ClockPlugin owner; S3 added presentation-facing typography/content/runtime facades; S4 separated hook registration by lifecycle/notification/surface/UDFPS domain; S5 centralized UDFPS runtime ownership; S6 consolidated docs/tests and audited non-blocking build/UI warnings.
- **M8 S1-S6:** complete and physically verified on candidate `0.1.383 / 393`. Incremental gates: S1 **376/376**, S2/S3 **377/377**, S4 **379/379**, S5/final **380/380**, all with zero failures/errors/skips and `git diff --check` PASS.
- **Final M8 artifact/device gate:** clean APK 19,748,415 bytes, SHA-256 `7C117B0398A8556F60390581383CE386AE9FAA51FE12F70412F7F80F681A0081`; install/hash/settings PASS; exactly one final reload `11051 -> 2668`; 3/3 LS↔AOD, Small/content, media PLAYING→NONE, AOD Large/empty, LS Large, UDFPS idle convergence and current-PID FATAL/ANR health all PASS. M8 is ready for commit/push on `agent/m8-architecture`; it is not yet merged to `master`.
- **Behavior invariant:** no visual/layout/content/UDFPS changes are intended. Released 0.1.380 is the golden comparison.

## Stable Runtime Ownership

### Clock / AOD

- One persistent `CouiClockHostView` is the accepted LS/AOD presentation owner.
- `CouiClockPluginHostController` owns the active ClockPlugin bridge and LS↔AOD scene handoff.
- Notification/media/contextual content uses the COUI semantic/presentation model and accepted 32dp partial-content anchor.
- Current weather uses provider-native color artwork at 18dp visible size in the fixed 22dp slot.

### UDFPS

- Release setting keeps `pixel_fingerprint_icon=false`: OPlus owns primary fingerprint glyph, pressed carrier, alpha/scale/animation, HBM/local-HBM and AOD fingerprint lifecycle.
- Pixel AOD observes authentication geometry and renders the independent success ripple when enabled.
- M8 must not accidentally restore custom primary UDFPS ownership while doing architecture cleanup.

### Packaging

- Modern entry: `dev.codex.pixelaod.PixelAodModernEntry`.
- Xposed API: `101/101`.
- `staticScope=true`.
- Scope: exactly `com.android.systemui`.
- No legacy `assets/xposed_init`.

## Architecture Debt Baseline

Largest source files at M8 start:

- `PixelAodClockView.java`: ~8.5k lines; contains both legacy View behavior and shared semantic/policy utilities still consumed by COUI.
- `PixelAodHook.java`: ~5.6k lines; centralizes multiple reflection-hook domains.
- `PixelLockscreenClockView.java`: ~2.0k lines.
- `CouiUdfpsController.java`: ~1.8k lines.
- `CouiClockHostView.java`: ~1.8k lines.
- `CouiClockSizeTransitionLayer.java`: ~1.7k lines.
- `PixelFingerprintIconController.java`: ~1.2k lines.
- `ClockPluginHostController.java` + `PixelClockPluginHostView.java`: removed in M8-S2 after S1 proved startup convergence; their dead fallback/injection surface in `PixelAodHook` was removed with them.

## M8 Authority

Use `docs/M8_ARCHITECTURE_CONVERGENCE.md` for current slice status and gates. Historical M5/M6/M7 details remain in `docs/COUI_PORT_ROADMAP.md`, `docs/M7_RELEASE_HARDENING.md`, CHANGELOG and Git history; this file is only the current implementation snapshot.