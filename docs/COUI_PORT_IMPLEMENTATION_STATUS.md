# COUI Port Implementation Status

Updated: 2026-08-22

## Current Status

> This document is the stable M8/COUI-port baseline, not the live uncommitted M9 checkpoint. For current M9 implementation state and candidate evidence, use `docs/M9_IMPLEMENTATION_STATUS.md`.

- **Stable release:** `v0.1.383` / versionCode `393` on `master`; annotated tag `v0.1.383` is the primary rollback point.
- **Runtime source baseline:** `46adb50cb84ff8e680bf42f5fa8b43d26be6f137` / `0.1.383`; the release marker after it is documentation-only.
- **Previous stable rollback:** `v0.1.380` remains the pre-M8 behavior golden and can be used for architecture-regression bisect/rollback.
- **M7:** completed and released. Modern Xposed API `101/101`, `staticScope=true`, sole scope `com.android.systemui`; M7 clock/content/weather/system-icon-UDFPS + success-ripple matrix accepted with documented conditional-scene exceptions.
- **M8:** S1-S6 completed on `agent/m8-architecture`, fully verified, then fast-forward integrated into `master` without altering the accepted 0.1.380 visual/runtime contract.
- **M8 architecture result:** COUI_PORT is the sole primary clock owner; presentation-facing typography/content/runtime facades are explicit; hook registration is split by lifecycle/notification/surface/UDFPS domain; UDFPS runtime ownership is centralized while optional replacement capability is retained.
- **M8 verification:** S1 **376/376**, S2/S3 **377/377**, S4 **379/379**, S5/final **380/380**, all zero failures/errors/skips with `git diff --check` PASS. Final APK 19,748,415 bytes, SHA-256 `7C117B0398A8556F60390581383CE386AE9FAA51FE12F70412F7F80F681A0081`; install/hash/settings, one final reload `11051 -> 2668`, 3/3 LS↔AOD, Small/content, media PLAYING→NONE, AOD Large/empty, LS Large, UDFPS idle convergence and current-PID FATAL/ANR health all PASS.
- **Behavior invariant:** no visual/layout/content/UDFPS behavior was intentionally changed by M8; `v0.1.380` is the comparison golden and `v0.1.383` is the post-convergence stable release.

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