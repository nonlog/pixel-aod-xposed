# Changelog

## [0.1.9] - 2026-08-22
### Changed
- Modification model: **GPT-5.6 Sol**.
- Complete M9 S11 / ADR 0054 by adding one Android system animation-scale policy for module-owned presentation motion. `0x` snaps deterministic terminal state, while enabled framework Animators retain their existing baseline durations so Android applies `animator_duration_scale` exactly once rather than being double-scaled by the module.
- Apply the disabled-animation gate to clock/weight presentation, partial-AOD/content/contextual motion, module-owned fingerprint drawable/effect motion and custom AOD-exit presentation while leaving vendor authentication/HBM/local-HBM timing untouched.
- Move the Home-page `Restart SystemUI` action into the top-right app-bar action and remove the old large restart card, leaving one restart control.
- Correct the pre-`0.2` visible version identity to `0.1.9`. Android `versionCode=9000` remains an internal monotonic update field and is not part of the user-visible version string; future pre-`0.2` candidates use normal human-readable versions such as `0.1.10` rather than `0.1.9001`. After all accepted Grill-derived implementation stages and final regression are complete, enter `0.2.0`.

### Evidence / Status
- JDK 17 full JVM regression: **89 suites / 414 tests / 0 failures / 0 errors / 0 skipped**; `:app:assembleDebug` and `git diff --check` PASS; protected clock/morph/weight animation-core diff is **zero**.
- S11 runtime behavior was validated on the 20,317,049-byte artifact, SHA-256 `231b54290bad568c630d26380fa290b780e961e5ce5117697b9cf962881de893`. That artifact mistakenly exposed the padded visible name `0.1.9000`; the user subsequently corrected the release identity to `0.1.9` while keeping internal `versionCode=9000`. A metadata-corrected rebuild is recorded below after verification.
- The S11 Settings UI dump already proved the display contract: only `versionName` is rendered, no parenthesized build code is shown, and exactly one `Restart SystemUI` node is present at top-right bounds `[1264,224][1360,320]`. The visible text itself is corrected from the mistaken padded name to `0.1.9` in the metadata-only follow-up.
- Real Keyguard -> Dozing validation proves `0x` is consumed in SystemUI (`scale=0.0, enabled=false`) and snaps the screen-off presentation endpoint. Additional `2x` and restored `1x` cycles remain stable on PID `16409`; the original `1.0` animator scale is restored, transition/window scales remain `1.0`, and current-PID fatal/ANR scan is empty.
- OOS did not emit a new ClockPlugin target transaction in the enabled 2x/1x samples; no direct 1100/550 ms host-log measurement is claimed. Enabled non-default scaling relies on Android's framework Animator contract, with focused tests ensuring Pixel AOD does not multiply the scale a second time.
- **Post-S11 version-rule correction:** source metadata now uses visible `versionName=0.1.9` with internal `versionCode=9000`; corrected rebuild/install evidence: corrected APK **19,749,423 bytes**, SHA-256 `ec535bea01f11159bbae407db09c14ea59e84b58ad7420e9a914719379692877`; `89 suites / 414 tests` pass, `git diff --check` passes, overwrite install succeeds, device package reports `versionName=0.1.9` and separate `versionCode=9000`, installed `base.apk` hash matches, and SystemUI PID remains `16409` because no runtime reload was needed. No runtime animation code changed in this correction.

## [0.1.383] - 2026-08-22
### Changed
- Complete M8 architecture convergence without changing the released 0.1.380 visual contract: COUI_PORT remains the sole clock owner and the stable UDFPS mode remains OPlus system primary glyph + independent Pixel AOD success ripple.
- Add `PixelAodTypography`, `PixelAodContentState`, and `PixelAodRuntimeState` facades so COUI/contextual/Modern presentation code no longer calls `PixelAodClockView.*` utility methods directly; keep the proven implementation behind those facades instead of moving the lifecycle state machine in a risky big-bang rewrite.
- Split hook-registration ownership into lifecycle, notification, surface/stock and UDFPS domain installers while preserving the accepted registration order and leaving mature reflection-hook implementations in place.
- Add `PixelAodUdfpsRuntimePolicy` to centralize replacement/system-primary-glyph/success-ripple ownership. Optional legacy renderer and replacement capabilities remain available; M8 does not remove them simply because the stable configuration uses the system glyph.
- Consolidate M8 docs/tests and audit remaining deprecation/build warnings. Edge-to-edge and native-library-packaging warnings are deliberately deferred rather than changed inside an architecture-only release.

### Evidence / Status
- Incremental gates pass: S3 **377/377**, S4 **379/379**, S5 **380/380**; the final JDK 17 clean gate also passes **380/380** with 0 failures/errors/skips, clean assemble and `git diff --check` PASS.
- Removed legacy clock-owner classes/selectors remain absent. COUI presentation has no direct `PixelAodClockView.*(...)` utility calls, and direct primary-glyph/success-ripple runtime setting reads are centralized in `PixelAodUdfpsRuntimePolicy`.
- Final APK is **19,748,415 bytes**, SHA-256 `7C117B0398A8556F60390581383CE386AE9FAA51FE12F70412F7F80F681A0081`; Modern Xposed metadata remains API 101/101, `staticScope=true`, SystemUI-only scope, Modern Java init only, with no legacy `assets/xposed_init`.
- Raw-USB overwrite install on CPH2573 succeeds; device reports `0.1.383 / 393`, installed `base.apk` hash matches exactly, and release UDFPS/debug/Forecast settings persist. Exactly one final-candidate SystemUI reload changes PID `11051 -> 2668`; fresh logs report fixed COUI clock ownership and 34 COUI UDFPS hooks.
- Physical final smoke passes 3/3 LS↔AOD cycles, Small notification/weather/Charging presentation, PixelPlay PLAYING -> NONE with clean media-row removal, genuine AOD Large/empty and LS Large endpoints, and stable PID `2668`. Evidence is under `.local/m8_final_01383/`.
- UDFPS remains system-primary: native main/pressed/dim windows are present, while idle `COUIExpressiveUdfpsGlow` is `GONE` with `mHasSurface=false` / `NO_SURFACE` / not visible. No new enrolled-finger authentication was synthesized during M8; the already accepted M7 real-auth contract and the unchanged policy tests cover this physical-only action.
- Post-reload current PID has no FATAL/ANR/crash; the only relevant recent SystemUI death is PID `11051` from the intentional reload. M8 S1-S6 is complete and ready for branch commit/push.

## [0.1.382] - 2026-08-22
### Changed
- Complete M8-S2 clock-owner convergence by deleting the now-unreachable `ClockPluginHostController` and `PixelClockPluginHostView` legacy primary-owner implementation.
- Remove constant-dead legacy AOD/lockscreen overlay injection, delayed reapply/fallback branches, and the obsolete runtime selector surface while preserving the persistent COUI ClockPlugin host behavior already accepted in 0.1.381.
- Keep `PixelAodClockView` / `PixelLockscreenClockView` shared static semantic/state utilities intact for staged extraction in M8-S3; no clock geometry, content, weather, animation or UDFPS product behavior is intentionally changed.

### Evidence / Status
- Source audit reports zero references under `app/src` to the removed legacy owner classes, startup router/policy, hidden `clock_renderer` selector, or retired injection helpers.
- Full JVM regression is **377/377 with 0 failures/errors/skips**; clean assemble and `git diff --check` pass.
- Candidate APK is 19,732,031 bytes, SHA-256 `2231197054F0F9DFFF6A1D62BAFC4E8F6EC9B50B1BF843422AEFAFED9FEDD1D6`; Modern Xposed metadata remains API 101/101, `staticScope=true`, SystemUI-only scope, no legacy `assets/xposed_init`.
- Overwrite install/hash on CPH2573 succeeds and settings persist. Exactly one S2 SystemUI reload changes PID `9591 -> 18063`; fresh logs load the new base.apk and report `COUI clock startup owner=COUI_PORT fixed=true`. Three explicit LS↔AOD cycles plus an LS endpoint capture keep PID `18063`; current PID has no FATAL/ANR.
- Physical evidence: `.local/m8_s2_01382/aod.png` and `.local/m8_s2_01382/ls.png`.

## [0.1.381] - 2026-08-21
### Changed
- Start M8 architecture convergence by making the validated COUI ClockPlugin host the only primary clock owner installed in SystemUI.
- Remove the hidden `clock_renderer` runtime rollback selector, `ClockRendererPolicy`, `ClockRendererStartupRouter`, and their legacy-vs-COUI selection tests. Version-level rollback remains available through the stable release/tag history instead of keeping two primary clock architectures live in one process.
- Keep `ActiveClockRendererController` as a behavior-neutral facade for semantic/lifecycle producers, but route every operation directly to `CouiClockPluginHostController`. Legacy owner implementation classes are intentionally retained for one slice so S1 can be validated independently before dead-code deletion in M8-S2.

### Evidence / Status
- No visual/layout/content/UDFPS behavior change is intended. The 0.1.380 M7 behavior is the golden contract.
- Retired clock-selector references under `app/src` are zero after S1.
- Focused ownership/feature-flag tests and full JVM regression pass **376/376 with 0 failures/errors/skips**; clean build and `git diff --check` pass.
- 0.1.381 APK is 19,764,803 bytes, SHA-256 `6D981493BCDB198EE58ED41A8002A8CA605EE7EF31D3AA20F73C1950876DD21C`; overwrite install/hash and settings persistence pass. Exactly one S1 SystemUI reload changes PID `17052 -> 9591`; fresh logs report fixed COUI owner, 3/3 LS↔AOD cycles remain on PID `9591`, and the current PID has no FATAL/ANR.

## [0.1.380] - 2026-08-19
### Changed
- Increase the visible current-weather artwork from about 17dp to 18dp at user request by reducing the internal inset in the existing 22dp weather slot from 2.5dp to 2dp.
- Keep the 22dp slot, 4dp icon-to-temperature gap, provider-native colors, and all weather-row/alignment geometry unchanged.

### Evidence / Status
- 0.1.379 / 17dp was installed and visually much closer to the adjacent temperature glyphs; user requested one final increase to 18dp and authorized commit after validation.
- Full JVM regression is **382/382 with 0 failures/errors/skips**; `git diff --check` and `:app:assembleDebug` pass. Candidate APK is 19,764,803 bytes, SHA-256 `AEE59B94C319FAC89B90AB8CB559566E00F51DB786DBBF963A5C9651D21143D1`.
- USB overwrite install succeeded on CPH2573; device reports `0.1.380 / 390`, installed `base.apk` hash matches exactly, and settings remain Google weather provider + `pixel_fingerprint_icon=false` + `udfps_success_ripple=true`. Exactly one SystemUI reload changed PID `27123 -> 21323`; bounded post-reload FATAL/ANR scan is empty.
- Stable AOD evidence is `.local\m7_weather_01380_20260819\aod_weather_18dp.png`. User explicitly requested the final 18dp size and authorized commit/push, so 0.1.380 becomes the next M7 production baseline and resets soak.

### Release Status
- M7 release integration completed on 2026-08-21 with explicitly documented conditional/timed coverage exceptions; production code remains frozen at 0.1.380.
- Forecast configuration-boundary exit, Forecast/media/notification burn-in anchoring, 3/3 LS↔AOD final smoke, Modern Xposed metadata, and Window/Surface convergence pass with SystemUI PID stable at 22604.
- Release artifact remains the previously fully tested 19,764,803-byte APK, SHA-256 AEE59B94C319FAC89B90AB8CB559566E00F51DB786DBBF963A5C9651D21143D1, exactly matching the installed device APK. A release-time repeat build was blocked only by an external Gradle 8.7 distribution download stall; no production code changed after the frozen artifact was built.

## [0.1.379] - 2026-08-19
### Changed
- Increase the visible current-weather artwork from about 15dp to about 17dp after physical comparison against the OPlus system AOD weather row showed the module icon was still smaller than the adjacent temperature glyphs.
- Reduce the internal inset in the existing 22dp weather slot from 3.5dp to 2.5dp while preserving the 22dp slot, 4dp icon-to-temperature gap, provider-native colors, and all row/alignment geometry.

### Evidence / Status
- 0.1.378 physical FAIL: user comparison against the native OPlus AOD weather row showed the module weather icon remained visibly smaller than the temperature digits.
- Full JVM regression is **382/382 with 0 failures/errors/skips**; `git diff --check` and `:app:assembleDebug` pass. Candidate APK is 19,764,807 bytes, SHA-256 `31C76360CC3F602BFB4AF95E7B76A372BE562E375C13AF41F0A113999B093028`.
- USB overwrite install succeeded on CPH2573; device reports `0.1.379 / 389`, installed `base.apk` hash matches exactly, and the system-icon UDFPS settings remain `pixel_fingerprint_icon=false`, `udfps_success_ripple=true`. Exactly one SystemUI reload changed PID `7286 -> 27123`.
- Stable AOD evidence: `.local\m7_weather_01379_20260819\aod_weather_17dp.png`. Pending user physical visual acceptance; keep 0.1.379 uncommitted/unpushed until accepted.

## [0.1.378] - 2026-08-19
### Changed
- Increase the visible current-weather artwork slightly from about 14dp to about 15dp by reducing the internal inset in the existing 22dp icon slot from 4dp to 3.5dp.
- Keep the 22dp slot, 4dp icon-to-temperature gap, provider-native colors, and all row/alignment geometry unchanged.

### Evidence / Status
- Full JVM regression remains **382/382 with 0 failures/errors/skips**; `git diff --check` and `:app:assembleDebug` pass.
- Candidate APK is 19,764,799 bytes, SHA-256 `4C6B0F574DA424033D0808E370B85B4CCEFAEDF2AB0452599734EF8F8E13C4C6`.
- Pending physical visual acceptance; keep this weather-size change uncommitted/unpushed until accepted.

## [0.1.377] - 2026-08-19
### Changed
- Add an M7 UDFPS ownership mode for the accepted fallback strategy: with `pixel_fingerprint_icon=false`, OPlus keeps complete ownership of the primary fingerprint icon, pressed carrier, alpha/scale/animation, HDR/local-HBM, and AOD fingerprint lifecycle; Pixel AOD only observes authentication and may render the independent success ripple.
- Decouple `udfps_success_ripple` from fingerprint-icon replacement. The settings page now allows Success ripple while the replacement icon is disabled; HDR press effect and custom AOD fingerprint-exit animation remain replacement-only controls.
- In system-icon mode, preserve the native dwell/press ripple. Suppress the native unlock ripple only when the custom success overlay has a valid OPlus fingerprint View geometry target, preventing double success ripples without changing press/HBM behavior.
- Make replacement-disabled visual refreshes strict no-ops unless a replacement from the same live SystemUI process is still tracked and needs one-time restoration after a settings toggle.

### Evidence / Status
- New `CouiUdfpsOwnershipPolicyTest` covers system-icon visual ownership, independent success-ripple ownership, one-time live-toggle restoration, native dwell preservation, unlock-ripple suppression, and missing-target fallback.
- Full JVM regression is **382/382 with 0 failures/errors/skips**; `git diff --check` and `:app:assembleDebug` pass. Candidate APK is 19,764,799 bytes, SHA-256 `14EC33D5FFEA45A2ADB1BA83C47103FF693889D8FB98D9BC6035AEA0732B6569`.
- USB overwrite install succeeded on CPH2573; device reports `0.1.377 / 387` and installed `base.apk` SHA-256 exactly matches the candidate. Settings are `pixel_fingerprint_icon=false`, `udfps_success_ripple=true`; exactly one SystemUI reload changed PID `10699 -> 22212`.
- Six no-touch wake/sleep cycles kept SystemUI PID `22212` unchanged. In the new PID, module mutation logs were all zero for HDR-window preparation, HDR surface writes, stable pressed-carrier writes, pressed-icon configuration, native-icon restore, native-pressed restore, and module touch handling; FATAL=0. This is the runtime proof that the module is not participating in vendor FOD/HBM visuals when replacement is off.
- **Physical PASS:** user confirmed the OPlus system fingerprint icon/press behavior remains native and the Pixel AOD success ripple appears after enrolled-finger authentication. 0.1.377 is accepted for commit/push; the next weather-icon size adjustment will be a separate version.

## [0.1.376] - 2026-08-19
### Added
- Distinguish a completed wired/wireless charge from active charging in the COUI AOD battery line. A connected battery with Android `BATTERY_STATUS_CHARGING` keeps `Charging`; connected `BATTERY_STATUS_FULL` now shows `Charged`.
- Add an OPlus edge fallback for a connected 100% battery reported as `BATTERY_STATUS_NOT_CHARGING`, which is also treated as `Charged`. Unplugged/full, unknown, and discharging states do not claim a charge-state suffix.

### Evidence / Status
- New `CouiBatteryStatusPolicy` is covered by five focused cases: charging, full, 100% connected/not-charging fallback, unplugged-full, and connected unknown/discharging.
- Full JVM regression after the M7 feature exception is **377/377 with 0 failures/errors/skips**; `git diff --check` and `:app:assembleDebug` pass.
- Candidate APK is `0.1.376 / 386`, size `19,764,695` bytes, SHA-256 `521BFC474EB1BE48E3786703C4889923FD71ED5691E2F2C185925B8F0F74B73C`.
- USB install on CPH2573 succeeded; installed `base.apk` hash exactly matches the candidate. Exactly one SystemUI reload changed PID `7149 -> 18609`.
- Real device state `48% / USB powered / status=CHARGING` renders `48% · Charging` on AOD. A reversible battery-service simulation with `level=100`, `USB powered`, `status=FULL` renders `100% · Charged`; screenshots are saved under `.local/m7_battery_01376_20260819/`. `dumpsys battery reset` restored the real `48% / CHARGING` state afterward, with SystemUI PID still `18609`.
- This user-authorized M7 feature exception advances the release-hardening baseline from 0.1.375 to 0.1.376 and resets the soak clock.
## [0.1.375] - 2026-08-19
### Fixed
- Restore OPlus ownership of OnScreenFingerprintUiMech.updateFpIconAlpha: COUI_PORT no longer short-circuits the vendor alpha lifecycle. Stable 0.1.331 observes this callback after execution and does not suppress it; blocking it is now isolated as the strongest remaining cause of panel-only local-HBM highlight.
- Keep the complete 0.1.374/0.1.370 COUI visual and success path unchanged: 80dp intrinsic glyph, 64dp lockscreen background circle, original fingerprint path/colors, animation/scale normalization, success ripple, and touch-gated HDR pressed carrier.
- Keep primary View.alpha, imageAlpha, and setBrightnessAlpha writes absent, and retain the no-touch pressed-window 1.0x HDR-headroom fix.

### Evidence / Status
- **0.1.374 physical result:** enrolled-finger recognition and success ripple pass and COUI size/background are restored, but the unwanted highlight returns; the highlight is visible on the physical panel but absent from screenshots. This proves the remaining defect is below normal SystemUI composition (panel/FOD brightness path), not the drawable pixels.
- A/B isolation: 0.1.373 had no highlight with vendor updateFpIconAlpha allowed; 0.1.374 restored the BEFORE suppression and the panel highlight returned. Stable 0.1.331 also lets updateFpIconAlpha execute normally (diagnostic hook is AFTER-only).
- **Automated/build gate:** full JVM regression is **372/372 with 0 failures/errors/skips**; `git diff --check` passes; incremental full-source test recompilation passes and `:app:assembleDebug` passes. APK size is `19,764,691` bytes, SHA-256 `E00C35ABAE749DAB4F28F14826C7AF48689FFEE88C7C27A88CB6A03AAF322D26`.
- **Deployment history:** the first automated transfer attempt was blocked by ADB transport loss, so the candidate was provided for manual install. A later fresh ADB check confirmed the exact tested 0.1.375 artifact is installed on the CPH2573.
- **Physical PASS (2026-08-19):** user confirmed 0.1.375 removes the panel-only idle/wake/sleep fingerprint highlight while preserving the accepted COUI fingerprint size/background/style, enrolled-finger recognition, and success ripple.
- **Installed artifact verification:** device reports `0.1.375 / 385`; installed `base.apk` SHA-256 is `E00C35ABAE749DAB4F28F14826C7AF48689FFEE88C7C27A88CB6A03AAF322D26`, exactly matching the tested local APK. This closes the UDFPS blocker and authorizes entry into M7 Release Hardening.
## [0.1.374] - 2026-08-19
### Fixed
- Revert the 0.1.373 visual over-correction: restore the exact accepted COUI_PORT lockscreen fingerprint drawable from 0.1.370/0.1.371, including its 80dp intrinsic canvas, 64dp filled lockscreen background, fingerprint path, colors, and 420ms lockscreen-to-AOD transition.
- Restore the 0.1.371 primary glyph animation/scale normalization and success-ripple lifecycle that had already passed enrolled-finger physical validation. This avoids the size/style and missing-ripple regressions introduced while trying to copy the broader 0.1.331 carrier boundary.
- Keep only the safe part of the 0.1.331 brightness lesson: COUI_PORT still suppresses OPlus `updateFpIconAlpha` so the vendor alpha spring cannot fight the replacement glyph, but it no longer writes primary `View.alpha`, `imageAlpha`, or `setBrightnessAlpha(1f)` at any point. Those carrier brightness writes are the remaining HBM-risk seam.
- Carry forward the 0.1.371 pressed HDR attach race fix: no-touch attach starts at `1.0x`; only live touch raises the pressed surface to max HDR headroom.

### Evidence / Status
- **0.1.373 physical FAIL:** user confirmed the unwanted highlight was gone, but the fingerprint size/background/style changed and the success ripple disappeared. 0.1.373 remains uncommitted/unpushed and is superseded by this narrower candidate.
- 0.1.374 intentionally preserves the normal COUI white-circle visual. The acceptance criterion is therefore panel behavior: the circle must retain the accepted COUI appearance without the extra local-HBM brightening/stickiness seen before; success ripple must again be visible.
- **Automated/build gate:** full JVM regression is **372/372 with 0 failures/errors/skips**; `git diff --check` passes; clean `--no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug` passes. APK size is `19,764,691` bytes, SHA-256 `B4CFCBFA5C419EA7A2398B7AB45F0D80EBFB45915B04A6BA132EB973ABFC9C7A`.
- **Install/runtime gate:** verified LAN CPH2573 install returned `Success`; device reports 0.1.374 / 384 and installed `base.apk` hash matches local. Preferences are preserved. Exactly one SystemUI reload changed PID `13504 -> 7460`; fresh COUI_PORT hooks count=35 is present.
- **No-touch gate:** pressed window is `desiredHdrHeadroom=1.0`; actual `SurfaceControl.setExtendedRangeBrightness` is `desiredRatio=1.0` with `touchDown=false`. The observed `HBM_EN ... hbm_en 8` occurs only after OPlus `realHide`, primary FOD surface destruction, and matches historical FOD teardown logs, so it is not treated as an idle HBM enable.
- **Visual gate:** fresh 0.1.374 lockscreen screenshot restores the same COUI filled-circle/fingerprint structure as the pre-0.1.373 implementation; the 0.1.373 no-fill visual is gone.
- Physical validation required before commit/push: confirm no extra panel/local-HBM brightening beyond the normal COUI circle, and confirm enrolled-finger recognition plus success ripple.
## [0.1.373] - 2026-08-19
### Fixed
- Remove the filled **idle lockscreen fingerprint background circle** from the COUI_PORT glyph while retaining the fingerprint strokes, 420ms solid-to-dashed lockscreen↔AOD transition, touch-gated HDR illumination, authentication, and success ripple.
- Make the no-fill rule an explicit `CouiUdfpsIdleVisualPolicy` contract with a focused regression test. This intentionally follows the physically accepted 0.1.331 idle visual on this OPlus device rather than COUI Expressive's filled `StockFingerprintDrawable` background.
- Carry forward 0.1.372's restoration of stable 0.1.331 primary-carrier ownership: COUI_PORT no longer intercepts `updateFpIconAlpha`, writes primary `setBrightnessAlpha`/View alpha, cancels OPlus primary-carrier animations, or forces primary carrier scale.

### Evidence / Status
- **Pixel-level root-cause proof:** using the same wallpaper and exact screenshot coordinate `(720,2370)`, the unlocked no-FOD frame is RGB `93,142,142`, the physically accepted 0.1.331 lockscreen is `94,143,143`, while 0.1.372 is `164,190,190`. Pixels outside the FOD region are identical between the 0.1.331 and current captures. Therefore the persistent visible "highlight" is directly captured as COUI_PORT's filled idle background, not only a panel/HBM effect.
- 0.1.372 no-touch runtime still showed pressed-window `desiredHdrHeadroom=1.0`, `touchDown=false`, and no FATAL/ANR, but its screenshot retained the large filled circle; it is therefore a diagnostic intermediate rather than an accepted fix.
- **Automated/build gate:** full JVM regression is **373/373 with 0 failures/errors/skips**; `git diff --check` passes; `:app:assembleDebug` passes. APK size is `19,764,695` bytes, SHA-256 `46351D06B62788E3D69D9C5D09B9B5022BCB2B66E47CD09CB91C576EB46E997F`.
- **Install/runtime gate:** verified LAN CPH2573 install returned `Success`; device reports 0.1.373 / 383 and installed `base.apk` hash matches local. Exactly one SystemUI reload changed PID `2536 -> 13504`; fresh COUI_PORT startup is present with no new FATAL/ANR. No-touch pressed-surface updates remain `desiredRatio=1.0`.
- **Post-fix screenshot proof:** on unobstructed FOD scanline `y=2450`, sampled x=`600,640,680,720,760,800,840` are pixel-identical between stable 0.1.331 and 0.1.373 (`delta=0` at every sample). At the same points 0.1.372 differed by about `142–155` summed RGB levels because of the filled circle. The large idle fill is therefore removed in the rendered frame.
- **Pending physical gate:** user must confirm on the actual panel that idle/transition highlight is gone and one enrolled-finger unlock still has normal press illumination, recognition, and success ripple. Keep 0.1.373 uncommitted/unpushed until that acceptance.

## [0.1.372] - 2026-08-19
### Fixed
- Restore the **stable 0.1.331 OPlus fingerprint carrier ownership boundary** for COUI_PORT. 0.1.331 is the physically verified no-highlight baseline: it replaces the primary fingerprint glyph but does not intercept `updateFpIconAlpha`, does not write the primary carrier `View.alpha` / `imageAlpha` / `setBrightnessAlpha`, and does not cancel the carrier's OPlus animation lifecycle.
- Remove COUI_PORT's primary `updateFpIconAlpha` BEFORE hook and all primary-carrier alpha/brightness normalization. The custom COUI drawable now sanitizes only drawable presentation state (`background`, tint, color filter, scale type), matching the safe 0.1.331 replacement boundary.
- Stop cancelling primary fingerprint carrier animations or forcing its scale to `1`. OPlus again owns temporary-show/fade/optical lifecycle state; COUI_PORT continues to own only the replacement glyph, touch-gated pressed carrier, real-touch HDR effect, and success ripple.
- Keep the 0.1.371 pressed-window attach fix: an idle pressed HDR window starts at `1.0x` headroom and only a real fingerprint touch raises it. Device evidence showed that race was real, but user physical testing proved it was not the remaining persistent-highlight root cause.

### Evidence / Status
- **0.1.371 physical FAIL:** enrolled-finger recognition and success ripple passed, but the unwanted lockscreen highlight remained. No commit/push was made.
- **Reference correction:** stable 0.1.331 source was re-audited and confirmed to never call primary `setBrightnessAlpha` or suppress `updateFpIconAlpha`; it gates only the pressed carrier alpha while idle. This source contract now takes precedence over anti-COUI alpha-normalization behavior on this OPlus device.
- Physical validation of 0.1.372 is required before acceptance or commit/push.

## [0.1.371] - 2026-08-19
### Fixed
- Gate the COUI HDR pressed-window headroom by the **live fingerprint touch state at attach time**. OPlus can attach `OnScreenFingerprintPressedIcon` during ordinary wake/doze transitions while `isTouchDownNow=false`; 0.1.370 pre-armed that window to maximum HDR headroom and only reset the SurfaceControl later, producing a deterministic screen-on/off brightness flash and leaving a race where a newly created surface could retain stale HDR headroom.
- Idle/released HDR carriers now initialize at `1.0x` headroom and reassert the current live-touch state on the next frame after `WindowManager.updateViewLayout()`. A real touch still raises the existing HDR carrier to the device maximum and keeps the native optical/authentication path unchanged.
- Preserve the COUI main fingerprint drawable, its normal lockscreen/AOD style transition, vendor carrier visibility, HDR color mode, and the existing `checkHasPressedAnimation` / `getScalePressedAnim` suppression contract.

### Evidence / Status
- **Root-cause evidence on 0.1.370:** no-touch wake logs showed `touchDown=false`, pressed View alpha `0`, illumination background alpha `0`, yet the newly created `OnScreenFingerprintPressedIcon` surface was first configured at HDR ratio `5.0` and only about 14 ms later reset to `1.0`. Eight additional no-touch power transitions produced no false `touchDown=true`, ruling out a touch-state latch as the observed wake/doze highlight source.
- **Automated gates:** focused UDFPS tests and Java compilation pass; full JVM regression is **372/372 with 0 failures/errors/skips**; `git diff --check` passes; clean `--no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug` passes. APK is 0.1.371 / 381, size `19,764,695` bytes, SHA-256 `A7B29CE717A0B08A7CBB7C19ACB9FE1BAEA30F9A3F0F0298EB312EBE8E1BE7F7`.
- **Install/runtime gate:** verified LAN CPH2573 transport overwrite install returned `Success`; device reports 0.1.371 / 381 and installed `base.apk` hash matches local exactly. All fingerprint/renderer/debug preferences were preserved. Exactly one SystemUI reload changed PID `23102 -> 11459`; fresh Modern/COUI_PORT startup is present.
- **No-touch HDR race regression:** repeated AOD/lockscreen transitions recreated `OnScreenFingerprintPressedIcon` surfaces with `touchDown=false`. 0.1.371 produced only `desiredRatio=1.0` on the pressed surface (`RATIO_5=0`), including a real `NO_SURFACE -> DRAW_PENDING -> HAS_DRAWN` recreate. An additional eight-transition no-finger stress run again produced `RATIO_5=0`, `RATIO_1=6`, and no module/vendor touch-down event.
- **Pending physical gate:** user must confirm the real panel no longer flashes/sticks in HDR at wake/sleep, then perform one enrolled-finger unlock to confirm press illumination and recognition/ripple remain intact. Keep 0.1.371 uncommitted/unpushed until that physical acceptance.
## [0.1.370] - 2026-08-19
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Final current-weather icon-size calibration after physical 0.1.369 measurement. Preserve the 22dp COUI slot, 4dp text gap, provider colors, Forecast/media/notification geometry, UDFPS behavior, and lockscreen↔AOD clock transition contract.

### Fixed
- Increase the dedicated current-weather icon inset to `4dp` per side inside the unchanged `22dp` slot, producing about `14dp` visible provider artwork while keeping the temperature text and whole information column geometry unchanged.
- Keep external weather artwork untinted so the configured Google provider remains multicolor.

### Geometry audit
- The compact `HH:mm` line intentionally retains COUI's optical whole-line recentering on minute changes. anti-COUI `PixelClockHostView.lineTargets()` remeasures all four glyph advances and the `0`/`1` optical corrections on every time update, then recomputes the centered start X. Therefore transitions such as `09:20 -> 09:21 -> 09:22` can produce a small X adjustment for the entire clock line even though only the last digit's text changes. COUI_PORT's `calculateLineTargets()` matches that reference behavior; no fixed-column deviation was introduced.

### Evidence / Status
- **Success (source/build):** full JVM regression is **371/371 with 0 failures/errors/skips**; `git diff --check` passes; `:app:testDebugUnitTest :app:assembleDebug` passes. APK is 0.1.370 / 380, size `19,764,695` bytes, SHA-256 `8D3FB07304E27194438F5CCB2C908432623B1677B1D4BA8A40FD7266FA4CB166`.
- **Success (install/runtime):** standard LAN overwrite install returned `Success`; device reports 0.1.370 / 380 and `base.apk` hash matches local exactly. Settings remain preserved. Exactly one successful SystemUI reload for this build changed PID `18781 -> 23102`; fresh Modern/COUI_PORT startup is present and bounded logcat has no FATAL/ANR.
- **Success (physical AOD calibration):** after waiting for the restarted AOD host to settle, a 1440x3168 root screenshot measures the current-weather provider artwork at `54x54 px`; the adjacent `25°` painted glyph height is also `54 px`. This meets the requested visual-size target while retaining provider color. The first immediate post-reload screencap hit an AOD host reconstruction intermediate frame and was discarded; the same PID's stable capture five seconds later is normal.
- **Accepted (physical visual gate):** user confirmed the current-weather icon size passes physical panel testing. This closes the 0.1.370 weather-icon calibration gate and allows the accumulated COUI alignment/weather batch to be committed and pushed.

## [0.1.369] - 2026-08-19
### Meta
- **Scope:** Intermediate current-weather icon-size probe only; superseded by 0.1.370 before acceptance.

### Evidence / Status
- 3dp-per-side inset produced about 16dp artwork. Stable 1440x3168 AOD measurement showed the provider icon at roughly `62px` high versus about `53px` for the adjacent temperature glyph, proving it was still slightly oversized and motivating the 0.1.370 4dp inset.
## [0.1.368] - 2026-08-19
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Follow-up to the physically observed 0.1.367 current-weather icon regression. Preserve the accepted Forecast/media/notification 32dp content anchor and all clock/UDFPS transition behavior.

### Fixed
- Stop applying a blanket white `ImageView` tint to the dedicated COUI current-weather icon. External icon packs now keep their source artwork colors; the built-in fallback remains correctly color-resolved when it is constructed, so it does not require an `ImageView` tint.
- Increase the current-weather icon inset from 1dp to 2dp per side while keeping the COUI 22dp slot and 4dp text gap unchanged. Visible artwork is therefore 18dp instead of 20dp, making the reduction measurable without moving the current-weather row or temperature text.
- Leave the Forecast/contextual icon color contract unchanged: that AOD-only row still follows the accepted host accent, while only the right-side current-weather provider artwork preserves multicolor source pixels.

### Evidence / Status
- **Success (pre-fix physical evidence):** root AOD screenshot on 0.1.367 with PixelPlay actively PLAYING and Forecast visible showed Forecast, media and notification rows sharing the intended left anchor, but the right current-weather icon remained white and visually too large.
- **Success (source/build):** focused `CouiClockGeometryPolicyTest` + `CouiCompactLayoutTest` pass; full JVM regression is **371/371 with 0 failures/errors/skips**; `git diff --check` passes; clean `--no-daemon --rerun-tasks :app:testDebugUnitTest :app:assembleDebug` passes. Final APK is 0.1.368 / 378, size `19,764,695` bytes, SHA-256 `DF58955DAECB73B1D53E257C8E3559572CB89EF52FC4C08F52B984392AA0DE8A`.
- **Success (install/runtime):** standard overwrite install on verified LAN transport `192.168.137.195:5555` returned `Success`; device reports 0.1.368 / 378 and device `base.apk` hash matches local exactly. Existing settings, including Google weather icon pack and Forecast, were preserved. Exactly one SystemUI reload changed PID `12603 -> 21823`; fresh Modern/COUI_PORT startup logs are present and bounded logcat has no new FATAL/ANR.
- **Success (automated AOD screenshot):** root `screencap` after the 0.1.368 reload captured active Forecast + media + notifications. The right current-weather icon now visibly retains provider color (blue precipitation artwork instead of forced white), while Forecast/media/notification left alignment remains intact. On the same 1440x3168 crop/threshold measurement, the current-weather artwork bounding box changed from about `77x77 px` on 0.1.367 to `70x70 px` on 0.1.368, consistent with the 20dp -> 18dp content-size change.
- **Pending (user physical acceptance):** confirm the 18dp current-weather icon is now the desired size/color on the panel and that the Forecast row remains stationary through the lockscreen -> AOD transition. Keep the batch uncommitted until accepted.
## [0.1.367] - 2026-08-19
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Follow-up COUI AOD geometry correction for the Forecast/contextual row reported to move after screen-off, plus a small reduction of the current-weather icon artwork. Preserve the accepted lockscreen↔AOD clock glyph animation and the existing notification/media snap-geometry + alpha-only transition contract.

### Fixed
- Make the AOD Forecast/contextual row use the same COUI partial-content X anchor as media and notification rows: `32dp + burnInX`. It no longer derives X from the live SMALL clock glyph target, so the row cannot expose a separate clock-driven horizontal correction during AOD entry.
- Keep media and notification rows on the same `32dp + burnInX` reference anchor and retain their existing final-X/Y snap before alpha fade. Forecast clearance remains vertical-only and still prevents overlap with lower content.
- Keep the COUI current-weather layout slot at `22dp` with the reference `4dp` text gap, but inset the drawable by `1dp` on each side so the visible weather artwork is approximately `20dp` without changing row geometry.

### Evidence / Status
- **Success (source/build):** focused `CouiCompactLayoutTest` / `CouiClockGeometryPolicyTest` pass; full JVM regression is **371/371 with 0 failures/errors/skips**; `git diff --check` passes. A clean `--no-daemon --rerun-tasks :app:assembleDebug` succeeds after one reused Gradle daemon disappeared during packaging.
- **Success (artifact):** final 0.1.367 / 377 APK is `19,764,695` bytes, SHA-256 `0BD6ABF50057FDC149ECF0891DEF68D41A2008D144FD4086E0A492CAAEFD4C8C`. Archive contains Modern `META-INF/xposed/{java_init.list,module.prop,scope.list}` only; metadata remains API 101/101, `staticScope=true`, scope exactly `com.android.systemui`.
- **Success (install/runtime):** connection recovery exposed LAN `192.168.137.195:5555` and FRP `127.0.0.1:15556`; both resolve to the same physical CPH2573 / `ro.serialno=4a851996`, so the higher-priority LAN transport was fixed for deployment. Standard overwrite `adb install -r` returned Android PackageManager `Success`; device reports 0.1.367 / 377 and its `base.apk` SHA-256 exactly matches `0BD6ABF50057FDC149ECF0891DEF68D41A2008D144FD4086E0A492CAAEFD4C8C`. Existing settings were preserved. Exactly one SystemUI reload changed PID `877 -> 12603`; fresh LSPosed persistent-module logs show the Modern entry loading the new base.apk with COUI_PORT clock/UDFPS owners active.
- **Pending (physical visual acceptance):** AOD hardware-plane capture through `screencap` is black on this device, so automated screenshots cannot establish the final Forecast/media/notification alignment or screen-off motion. User visual confirmation is still required for the shared 32dp content X anchor, no Forecast correction after screen-off, the smaller current-weather icon, and preserved LS↔AOD clock animation.


## [0.1.366] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Physical screenshot + anti-COUI geometry correction for the compact current-weather row and partial-AOD content X anchor. Preserve the accepted clock/date geometry, Forecast AOD-only behavior, UDFPS behavior, and lockscreen↔AOD clock animation contract.

### Fixed
- Restore the COUI current-weather composition to one dedicated `22dp` icon slot plus `4dp` gap before the weather text. COUI_PORT previously left that dedicated slot `INVISIBLE` while also adding a second leading compound drawable to the weather `TextView`, which shifted the painted weather icon roughly 43–45 px to the right in the supplied 698 px screenshots even though the row target X itself matched COUI. The dedicated icon is now `GONE` when absent and exclusively owns current-weather artwork.
- Revert the earlier SMALL notification clock-edge override. Anti-COUI `PixelClockHostView.applyAodContentTarget()` anchors both notification and media content at independent `32dp + burnInX`; it does not derive either row from the clock's painted leading edge. COUI_PORT now follows that exact content anchor again while retaining the existing final-geometry snap + alpha-only content motion contract.
- Keep the module-added AOD Forecast/contextual row on the live compact clock painted edge. Forecast remains an intentional Pixel AOD extension and still pushes lower notification/media content vertically when needed to avoid overlap.

### Geometry audit
- **Clock:** no change. Compact LS/AOD center X (`0.25W + 8/10dp`), Y (`0.105H + 25dp`), scale and per-digit 0/1 optical corrections match the anti-COUI implementation and the supplied physical screenshots.
- **Date/current-info column:** no target change. Both hosts use the 75% screen-width center with LS `-36dp` / AOD `-34dp`, `max(dateWidth, weatherWidth) / 2`, and `0.118H + 33dp`; the current-weather defect was internal row composition, not the target X.
- **Media:** no geometry/type change. COUI_PORT already matches anti-COUI's independent `32dp + burnInX` X anchor, 18dp/500 title, 15dp/450 artist, 18dp app icon + 6dp gap, 4dp subtitle gap, and 28dp media-to-notification spacing. Only Forecast clearance can intentionally move it downward.
- **Notification icons:** horizontal parity restored to the anti-COUI independent 32dp content anchor. The module intentionally retains its previously accepted five-visible-icons + `+x` overflow policy rather than anti-COUI's raw seven-icon cap.

### Evidence / Status
- **Success (focused source gate):** `:app:compileDebugJavaWithJavac`, focused `CouiCompactLayoutTest` + `CouiClockGeometryPolicyTest`, and `git diff --check` pass. Clock target calculation and the 550ms LS↔AOD glyph transition path are unchanged.
- **Success (source/build):** full JVM regression is **371/371 with 0 failures/errors/skips**, `git diff --check` passes, and `assembleDebug` passes. Final APK is 0.1.366 / 376, size `19,764,695`, SHA-256 `1541086B5A2F9C8F3164EE89DEF204916B450160EB96060E706B7A81DFAFBDCC`; packaged Xposed metadata remains Modern API 101, `staticScope=true`, scope exactly `com.android.systemui`, and has no legacy `assets/xposed_init`.
- **Blocked (device transport only):** fresh enumeration exposed only FRP ADB `127.0.0.1:15556`. One controlled standard streamed `adb install -r` attempt failed after `Performing Streamed Install` without any Android PackageManager result; no retry loop, ADB server restart, alternate install path, reboot, or SystemUI reload was attempted. Physical validation remains pending until manual overwrite install or raw USB is available: current-weather X, independent notification/media 32dp X, Forecast clearance, and LS↔AOD transition.

## [0.1.365] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Dark-theme COUI card-surface correction on top of the 0.1.364 information-architecture pass. Settings presentation only; navigation structure, setting keys, providers, and SystemUI runtime hooks are unchanged.

### Fixed
- Replace ordinary settings-card use of dynamic `surfaceContainerLowest` with `surfaceContainerHighest`. On the user's current dark Monet palette the former resolves to absolute black, while COUI Expressive uses a visibly raised, wallpaper-tinted dark container above the page background.
- Apply the same neutral-container correction to unselected choice-dialog rows and the time-picker inner surface so the settings UI does not reintroduce isolated pure-black cards in secondary surfaces.
- Keep dynamic wallpaper color intact; no fixed gray/teal RGB values are introduced.

### Evidence / Status
- **Success (source/build):** full JVM regression is **371/371 with 0 failures/errors/skips**, `git diff --check` passes, runtime Java diff from accepted `9e31130` remains zero, and `assembleDebug` passes. Final APK is 0.1.365 / 375, size `19,764,695`, SHA-256 `6FEF618FA56B6B3DE3934293BB2A832113B07A5483D92C727F50FDD02F09CFF2`; packaged Xposed metadata remains `staticScope=true` / API 101.
- **Success (install):** standard overwrite install on verified raw USB `4a851996` returned Android PackageManager `Success`; device reports 0.1.365 / 375 and installed base.apk SHA-256 exactly matches local. SystemUI PID remained `20884` before and after install, confirming no runtime reload occurred.
- **Success (physical UI, user-observed):** user compared 0.1.365 against the supplied COUI Expressive dark-theme reference and reports the card-surface correction passes visually. The separate AOD Hub / Clock / Fingerprint structure also remains accepted. This closes the M6 UI information-architecture and dark-card presentation wrap-up.

## [0.1.364] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Final M6 information-architecture cleanup after the physically accepted 0.1.363 Phase-G corrections. Settings UI only; runtime hooks and setting/provider contracts are unchanged.

### Changed
- Split the previous combined `Clock Style / UDFPS` AOD child destination into two real pages: `Clock` and `Fingerprint`.
- Keep `Clock` focused on AOD and lockscreen clock-weight controls, and move the existing Pixel fingerprint icon, HDR press highlight, success ripple, and AOD exit-animation controls intact into the dedicated `Fingerprint` page.
- Add a separate Fingerprint row to the AOD Hub, use a palette icon for Clock and a fingerprint icon for Fingerprint, and keep both child pages under the AOD bottom-tab/back-navigation hierarchy.
- Rename the visible `Clock Style` label to `Clock` and update Chinese/English page descriptions so the hierarchy reflects user-facing concepts rather than implementation grouping.

### Evidence / Status
- **Success (source compile):** `:app:compileDebugKotlin` passes after the page split. Static inspection confirms the four UDFPS setting keys still have one state read and one existing provider write each; no runtime Java source is changed by this UI-only pass.
- **Success (source/build):** full JVM regression is **371/371 with 0 failures/errors/skips**, `git diff --check` passes, runtime Java diff from accepted `9e31130` is zero, and `assembleDebug` passes. Final APK is 0.1.364 / 374, size `19,764,695`, SHA-256 `75A6B935FC6EB99469768FF369AEFA90D41A164D1CA1CECDBB7089351A193F1B`; packaged Xposed metadata remains `staticScope=true` / API 101.
- **Success (install):** standard USB overwrite install on verified CPH2573 / serial `4a851996` returned `Success`; installed 0.1.364 base.apk SHA-256 matches local exactly. SystemUI PID remained `20884` before and after install, confirming no runtime reload occurred. SettingsActivity launch produced no fresh app FATAL; automated hierarchy inspection was blocked only because the device was on the secure lockscreen.
- **Pending (physical UI only):** user visual review of the AOD Hub and separate Clock / Fingerprint child pages. Keep this UI batch uncommitted until accepted.

## [0.1.363] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation during Phase G; no Luna/Codex executor used.
- **Scope:** Restore real-finger UDFPS recognition with the Pixel visual enabled, make the contextual row AOD-only, and unify COUI small-AOD painted-edge/color presentation without changing the accepted LS↔AOD clock glyph transition contract.

### Fixed
- When `udfps_hdr_press_effect=false`, return ownership of `OnScreenFingerprintPressedIcon` illumination to OPlus: preserve/restore the vendor drawable/background, leave vendor pressed-animation decisions native, stop issuing module HDR SurfaceControl transactions, and remove the module pre-auth press glow. Pixel AOD now only gates the vendor pressed View alpha (idle hidden, live touch original alpha), matching the physically stable 0.1.331 optical carrier contract. Custom success ripple remains a post-auth effect.
- Make Forecast / weather-alert / calendar contextual presentation COUI AOD-only. Lockscreen presentation now resolves to no contextual row, eliminating overlap with OPlus lockscreen notification cards and avoiding a separate lockscreen contextual Y.
- Derive one live SMALL painted-leading-edge from the existing clock target and use it for the contextual and notification rows. The clock target calculation and glyph animation path are untouched; contextual/notification rows continue using the accepted final-geometry snap + alpha-only content motion, including AOD burn-in exactly once.
- Make the COUI contextual glyph/text use the same active host AOD clock accent at full child alpha. Row alpha still owns contextual enter/leave fades, so Forecast no longer appears as a weaker/different color.

### Evidence / Status
- **Success (source/build):** UDFPS pressed-visual policy, compact painted-edge, contextual AOD-only/layout, COUI visual-style, selector, and AOD content-motion focused tests compile and pass. Full JVM regression is **371/371 with 0 failures/errors/skips**, `git diff --check` passes, and `assembleDebug` passes. Final APK is 0.1.363 / 373, size `19,764,215`, SHA-256 `473044784ABB6F3FDA7BD0BCEA364C1C3926990B5100A93197D0C9F282DDDF4D`; final archive retains Modern API 101, `staticScope=true`, scope exactly `com.android.systemui`, and no legacy `assets/xposed_init`.
- **Success (install/runtime):** the verified raw USB device `4a851996` accepted the standard overwrite install and Android PackageManager returned `Success`. Device `base.apk` SHA-256 matches the final 0.1.363 APK, existing `coui_port` / `udfps_hdr_press_effect=false` / `debug_logging=false` settings were preserved, and the single SystemUI reload changed PID `31284` → `16538`. Fresh LSPosed startup confirms the current 0.1.363 base.apk loading through the Modern entry with COUI_PORT clock/UDFPS owners active.
- **Success (physical, user-observed):** user visually accepted the full 0.1.363 correction set on the physical CPH2573: contextual Forecast is AOD-only, Forecast uses the same accent as the clock/notification glyph row, SMALL clock/Forecast/notification painted leading edges are aligned, the accepted lockscreen↔AOD clock transition remains smooth/intact, and real enrolled-finger UDFPS recognition works again. This closes the 0.1.362 UDFPS regression and the contextual geometry/color follow-up.

## [0.1.362] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation during Phase G; no Luna/Codex executor used.
- **Scope:** Close the Phase-G contextual At a Glance runtime gap in COUI_PORT without changing selector policy, setting keys, or the accepted notification/media first-frame motion contract.

### Fixed
- Add a host-owned contextual row to `CouiClockHostView` so the existing Calendar / Weather alert / Tomorrow forecast selector is actually rendered by the COUI_PORT primary owner. Before this change those policies and M6 settings existed, but their presentation wiring terminated in legacy `PixelAodClockView` / `PixelLockscreenClockView` instances that are absent under startup-exclusive COUI_PORT.
- Reuse the existing `ContextualAtAGlanceSelector`, privacy/state-store policy, weather/calendar icon resolution, text formatting, and presentation semantics rather than creating a second business-policy stack. Weather alerts remain AOD-only; calendar and forecast keep the existing priority/fallback rules.
- Add `CouiClockContextualLayoutPolicy` so the contextual row is placed below the actual date/current-weather group and only pushes notification/media content when its measured or fallback bottom requires additional clearance. Notification/media geometry still snaps to the final target before alpha animation, preserving the physically accepted 0.1.354 no-Y-correction invariant.
- Add an explicit non-animated first-frame overload to `ContextualAtAGlancePresentation`; unlocked pre-arm and AOD-entry preparation can populate the row atomically instead of exposing a stale/empty contextual fade on the first visible frame.
- Route calendar-data changes and contextual display deadlines through `ActiveClockRendererController.refreshInformationFromExistingAdapters(...)` as well as the retained legacy refresh calls, so COUI_PORT reevaluates cards when the source changes or a forecast/alert display boundary expires.

### Evidence / Status
- **Success (source/build):** Java compilation plus focused contextual/layout/content-motion tests pass; full JVM regression is **366/366 with 0 failures**, `git diff --check` passes, and `assembleDebug` passes. Final APK is 0.1.362 / 372, size `19,764,215`, SHA-256 `5B38D72EC5BFC10D00E04A8407573C67A47D9B518EF3DD436C48438DEAF31A96`; static SystemUI scope/Modern entry remain package-clean.
- **Success (install/runtime):** raw USB serial `4a851996` was used; overwrite install returned Success and device base.apk hash matches local. Exactly one SystemUI reload changed PID `23485` → `31284`; fresh LSPosed startup shows COUI_PORT clock/UDFPS owners with no bounded startup FATAL/ANR.
- **Success (physical contextual card):** a live AOD screenshot now shows the expected Breezy tomorrow fallback `Tmr 28° / 24°` with its weather glyph above the notification row. A 14-frame high-frequency lockscreen→AOD capture keeps the Forecast and notification rows at the same visible relative Y through transition/stable frames, with no old-position notification flash or downward correction. The Phase-G contextual runtime gap is physically closed.

## [0.1.361] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on merged `agent/coui-port`; no Luna/Codex executor used.
- **Scope:** Small COUI notification-overflow color correction before M5 static-scope physical acceptance.

### Fixed
- Make the COUI host `+x` notification overflow label use the exact same resolved Material/Monet accent source as monochrome notification glyphs instead of hard-coded white. This keeps the five-icons-plus-overflow row visually consistent when the clock and notification glyphs are wallpaper-colored.
- Add an explicit `CouiClockVisualStylePolicy.notificationOverflowColor(...)` contract and unit coverage so later clock/information color refreshes cannot silently return the overflow label to white.

### Evidence / Status
- **Success (source/build):** full JVM regression is 361/361 with 0 failures, `git diff --check` passes, and `assembleDebug` passes. Final APK is 0.1.361 / 371 with SHA-256 `8B2C15B51EB2FD85AFAD5361161245ED2D027A68F732D0A93CD22838141B0E06`.
- **Success (install/runtime):** overwrite install from 0.1.360 succeeded on the verified LAN transport, device base.apk hash matches, and the single SystemUI reload changed PID `5430` → `31680`. Fresh LSPosed logs show the Modern entry loading the 0.1.361 base.apk into `com.android.systemui` and COUI_PORT clock/UDFPS owners starting normally.
- **Success (physical overflow color):** fresh AOD screenshot with more than five eligible notifications shows five icons plus `+1`; the `+1` now uses the same wallpaper-derived cyan accent as the monochrome notification glyphs/clock instead of white.
- **Success (M5 static-scope physical acceptance):** package-clean reinstall confirms LSPosed Manager resets module Enable off while automatically retaining the declared/recommended `System UI / com.android.systemui` static scope; enabling then reloading SystemUI restores fresh Modern injection from the current base.apk. Manager disable followed by SystemUI reload produces a fresh PID with no Pixel AOD injection, and re-enable restores the Modern entry/COUI_PORT owners. Exact rollback baseline `a1f7e8d` was built as 0.1.331 / 341 / `staticScope=false`, injected successfully, then directly upgraded to 0.1.361; fresh logs on PID `23485` load the new base.apk and COUI_PORT owners. LSPosed Manager 2.1.1 / framework API 102 recognizes the module as API 101 and explicitly reports `The module declared static scope`.

## [0.1.360] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; no Luna/Codex executor used.
- **Scope:** M6 second-stage COUI settings refinement across the AOD child pages. Runtime hooks, setting keys, provider contracts, and persistence semantics remain unchanged.

### Changed
- Promote `Display & behavior` into the reference child page for shared controls: AOD behavior, continuous schedule, start/end time rows, disabled states, selection dialogs, and the 24-hour time picker now share the COUI dynamic-surface language. Schedule/time rows remain visible and become disabled when their prerequisite mode is unavailable instead of changing page height by disappearing.
- Refine the custom COUI switch with a true disabled visual state while preserving the existing animated check/close thumb and wallpaper-derived colors.
- Replace the old radio-button selection dialog with segmented COUI-style option surfaces and a selected check carrier; all existing language/AOD/icon-source dialogs inherit the same component.
- Add a shared `PixelAodTimePickerDialog` so AOD and forecast time selection use the same rounded dynamic container, TimePicker role mapping, and localized OK/Cancel actions.
- Reorganize `Clock Style` into Appearance, Fingerprint, and Animation effects. UDFPS effect controls remain visible but disabled when the Pixel fingerprint visual is off, avoiding abrupt layout collapse.
- Reorganize `At a Glance` into Weather, Forecast, and Contextual information. Weather icon source, forecast window, and calendar icon source remain visible with dependency-aware disabled states instead of conditional row removal.
- Rename the Lockscreen child section to Notifications and reduce System UI to the actual Diagnostics surface; language remains a single Home-level setting and is no longer duplicated in System UI.

### Evidence / Status
- **Success (source/build):** full JVM regression is 360/360 with 0 failures, `git diff --check` passes, `assembleDebug` passes, and the tracked runtime Java hook diff is 0. Final APK is 0.1.360 / 370 with SHA-256 `7002CAF95560870DF2CF2077AF56345CF38B18D34F16A1A7354DB547A35B8A36`.
- **Success (install):** the final 0.1.360 APK was overwrite-installed through the verified LAN transport `192.168.137.195:5555`; the physical device remains serial `4a851996` / CPH2573 and SystemUI PID stayed `5430`, so this UI-only pass did not reload runtime hooks.
- **Success (physical UI review):** user visually accepted the 0.1.360 child-page/navigation/dialog/disabled-state pass on the physical device. M6 second-stage UI is approved for commit and merge; no runtime regression was reported during this UI-only pass.

## [0.1.359] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; no Luna/Codex executor used.
- **Scope:** Fix the non-immersive bottom navigation observed after the 0.1.358 three-tab shell.

### Changed
- Remove the extra `navigationBarsPadding()` applied to the Material3 `NavigationBar`; Material3 already consumes its own bottom system inset, so the duplicate outer padding had exposed the page background below the bar.
- Make the Activity navigation bar transparent and disable the Android navigation-bar contrast scrim on API 29+, allowing the wallpaper-derived bottom-bar surface to extend continuously behind the gesture area.
- Preserve the three-tab Home / AOD / System UI shell, dynamic wallpaper color roles, and all runtime behavior unchanged.

### Evidence / Status
- **Success (source/build):** full JVM regression is 360/360 with 0 failures, `git diff --check` passes, and `assembleDebug` passes. Final APK is 0.1.359 / 369 with SHA-256 `D88E27E4B1282CB1EFBFC4DDD582E53D2F0ED959E13777A2FB0DA5C196F12573`.
- **Success (physical, user-observed):** 0.1.359 was manually installed and visually verified on the physical CPH2573. The three-tab shell remained stable and the bottom navigation now extends continuously behind the gesture area with no separate non-immersive navigation strip.

## [0.1.358] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; no Luna/Codex executor used.
- **Scope:** Remove the redundant Settings top-level destination after the 0.1.357 AOD-hub restructure.

### Changed
- Remove `SettingsPage.SETTINGS` and the Settings item from the persistent bottom navigation. The primary shell now has exactly three destinations: Home, AOD, and System UI.
- Move the existing Language choice, including its original setting key/dialog/persistence/recreate behavior, into a General section on Home.
- Preserve the AOD hub and all real AOD child pages from 0.1.357. No Clock/AOD/UDFPS runtime source or setting-provider contract is changed.

### Evidence / Status
- **Success (source/build):** full JVM regression is 360/360 with 0 failures, `git diff --check` passes, `assembleDebug` passes, no `SettingsPage.SETTINGS`/`nav_settings` routing remains in `SettingsActivity`, and runtime hook sources have 0 diff lines in this UI-only pass.
- **Blocked (physical install):** the first controlled FRP `adb install -r` of 0.1.358 again lost the streamed APK transfer before Android PackageManager returned a result. No ADB-server restart, reconnect loop, SystemUI reload, or runtime mutation was performed. Physical three-tab launch/navigation validation remains pending a reliable install path.

## [0.1.357] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; no Luna/Codex executor used.
- **Scope:** Apply the user's second COUI UI review: remove redundant Home actions, make AOD a real hub with child pages, replace the fixed light palette with wallpaper-derived dynamic roles, refine the COUI switch, and replace the unattractive enlarged launcher art with a dedicated dynamic hero mark.

### Changed
- Remove the three-segment Home shortcut bar. Home now exposes exactly one operational action, `Restart SystemUI`, plus the existing module master toggle.
- Replace the Home raster/adaptive launcher artwork with `PixelAodHeroMark`, a dedicated programmatic P/clock mark rendered from `primary`, `secondaryContainer`, `tertiary`, and related dynamic roles. It scales cleanly and cannot hit the Compose adaptive-icon painter crash from the first 0.1.356 preview.
- Stop overriding dynamic Material You neutral roles with fixed `#EDF1F0` / `#F9FDFC` / teal-adjacent colors. Android 12+ now uses the full `dynamicLightColorScheme` / `dynamicDarkColorScheme` from the current wallpaper; COUI hierarchy is expressed through surface/container role selection instead of fixed RGB values.
- Refine the custom switch to the physical COUI reference: 58×36 outlined off track with left grey thumb/close glyph, and a filled dynamic-primary on track with a light dynamic-primary-container thumb/check glyph. All hues come from the current color scheme.
- Implement AOD navigation option **B** from the user's review. The bottom AOD tab is now an AOD feature hub with real child pages: `Display & behavior`, `Clock Style`, `At a Glance`, and `Lockscreen`. Those child pages keep AOD selected in the bottom bar and return to the AOD hub.
- Remove the redundant Always-on-display row from Settings that only switched to the AOD bottom tab. Settings is now reserved for app-level/general options; language remains there, while System UI keeps diagnostics.
- Remove duplicated top-right SystemUI restart actions from Settings/AOD/child/SystemUI pages. The one Home restart action is the single explicit SystemUI restart control.
- Keep all setting keys, ContentProvider writes, permissions, dialogs, and persistence semantics unchanged. No Clock/AOD/UDFPS runtime hook file is modified.

### Evidence / Status
- **Success (compile):** `:app:compileDebugKotlin` passes after the dynamic palette, switch, Home, and AOD-hub restructuring.
- **Pending final gate:** full JVM regression, `git diff --check`, final `assembleDebug`, overwrite install, no-crash launch check, and physical navigation/color/switch review.

## [0.1.356] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; no Luna/Codex executor used.
- **Scope:** Rework the first UI preview against four user-supplied COUI Expressive 2.5 physical screenshots instead of merely applying generic Material 3 navigation.

### Changed
- Match the COUI Expressive light neutral hierarchy from the supplied screenshots while preserving the device Monet accent palette: light page background `#EDF1F0`, segmented-card surface `#F9FDFC`, bottom navigation surface `#E7F0EF`, and dark foreground `#283233`.
- Replace the 0.1.355 home/category shell with a COUI-like four-tab bottom navigation: Home, Settings, AOD, and System UI. Nested Clock/Fingerprint, At a Glance, and Lockscreen pages remain reachable from the Settings tab and keep Settings selected in the bottom bar.
- Rebuild Home around the COUI reference structure: large `Home` title, centered app icon/name/version/description, large three-segment quick-action pill, and a separate highlighted master-module card. The top-right SystemUI restart action remains available.
- Rebuild Settings category rows as COUI-style segmented white cards with narrow gaps, uncluttered teal line icons, larger two-line text, optional current-value summary, and chevron. Remove the 0.1.355 colored icon-container treatment.
- Replace the stock Material 3 switch visual with a COUI-style outlined 56x32 pill and animated circular thumb containing explicit check/close state glyphs.
- Increase the large-title top breathing room and use normal-weight 32sp page titles; keep section labels in the Monet primary color. AOD becomes a top-level bottom-navigation page with `Always-on display` / `Display` hierarchy and no ordinary subpage back affordance.
- Keep all existing setting keys, ContentProvider writes, permissions, dialogs, and persistence semantics unchanged. No Clock/AOD/UDFPS runtime source file is modified.

### Evidence / Status
- **Reference:** four user-provided physical COUI Expressive screenshots were inspected for page/card/bottom-bar colors, title spacing, segmented row structure, icon treatment, switch geometry, and persistent bottom navigation.
- **Success (compile):** `:app:compileDebugKotlin` passes after the second-pass component/navigation rewrite.
- **Crash found and corrected before acceptance:** the first 0.1.356 preview used `painterResource(R.mipmap.ic_launcher)` on an adaptive-icon XML and crashed on launch because Compose accepts only vector or raster painter resources. Home now uses the existing raster `ic_launcher_foreground` asset instead; fresh launch verification is required below before UI review continues.
- **Pending:** full JVM suite, `git diff --check`, final debug build, overwrite install, and fresh physical screenshots before this UI batch is committed.

## [0.1.355] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation on `agent/ui-refactor`; the accepted 0.1.354 runtime remains the stable baseline.
- **Scope:** First post-stability settings UI refinement batch. Presentation/navigation only; no Clock/AOD/UDFPS hook behavior, setting key, provider contract, or persistence semantics are changed.

### Changed
- Split the former single long settings column into a persistent category home and dedicated subpages for AOD, Clock/Fingerprint, At a Glance, Lockscreen, and System. The first visual-spec batch is the home + AOD page; the other category pages preserve the existing controls while moving them into the new navigation shell.
- Extend `PixelAodDesignSystem` with a COUI-style category navigation row: dynamic-color icon container, two-line label hierarchy, optional current-value summary, chevron affordance, and grouped separators.
- Extend `PixelAodPage` with an optional back affordance while keeping the restart action available. Android back now returns from a subpage to the settings home instead of closing the activity.
- Keep the module hero toggle on the settings home, show the current AOD display mode directly on the AOD category row, and show the current language on the System category row.
- Add localized page descriptions and navigation labels for Chinese and English.

### Evidence / Status
- **Baseline captured:** light and dark screenshots of the accepted 0.1.354 settings UI were saved under the gitignored `.local/ui_refactor_baseline_20260818/`, with the device UI mode restored to `auto` afterward.
- **Reference checked:** the freshly decompiled anti-obfuscation COUI Expressive 2.5 `SettingsActivity` confirms Compose content, edge-to-edge window handling, localized configuration, and system/dynamic-theme behavior remain valid foundations for this refactor.
- **Success (compile):** `:app:compileDebugKotlin` passes after the navigation-shell implementation.
- **Pending:** full JVM regression, diff/build/package checks, overwrite install, and user-visible home/AOD screenshot review before this first UI batch is committed.
## [0.1.354] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation; Luna/Codex executor remains disabled by the user's direct-execution override.
- **Scope:** Remove the intermittent partial-AOD notification-row position jump observed after the 0.1.353 performance correction.

### Changed
- Match COUI 2.5 `PixelClockHostView.applyContentViewTarget()` exactly for AOD content motion: notification and media rows now commit their final `translationX/Y` before an animated transition and animate alpha only. Clock glyph geometry/weight motion remains unchanged.
- Preserve the current notification/media row position while an animated transition is leaving partial AOD, matching the reference fade-out behavior and preventing a hidden content row from jumping back to the default anchor.
- Add `CouiClockAodContentMotionPolicy` as a pure regression seam proving partial-AOD entry never animates content-row translation and animated exit preserves the existing position.

### Evidence / Status
- **Root cause confirmed against COUI 2.5:** the reference passes `animateTranslation=false` for both `notificationIconRow` and `mediaGroup`; the port had used a generic `applyViewTarget()` that animated X/Y and alpha together. With pre-arm at the non-dozing anchor and burn-in applied on the next AOD frame, this exposed exactly the user-observed “wrong position → downward movement → correct position” sequence.
- **Success (focused regression):** AOD content-motion, AOD transition, unlocked-entry normalization, non-lockscreen prearm, notification overflow, and geometry tests pass.
- **Success (physical, user-observed):** 0.1.354 was overwrite-installed and hash-verified on the physical CPH2573, SystemUI was reloaded once, and repeated visual testing confirmed notification icons now appear directly at their final AOD position without the previous visible downward correction.

## [0.1.353] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation under the user-authorized direct-execution override; Luna/Codex executor was not used.
- **Scope:** Remove shared SystemUI hot-path work that made both unlocked-desktop → AOD entry and screen-off fingerprint unlock/success ripple visibly delayed and janky.

### Changed
- Replace per-notification synchronous AOD snapshot rebuilding from `KeyguardNotificationVisibilityProvider` / notification filter callbacks with an O(1) visibility-map update plus one coalesced AOD refresh. Interactive/waking keyguard traversal only marks the aggregate dirty; the next real AOD activation consumes it once. This removes the previous O(N²) main-thread notification-policy workload from the wake/ripple window while preserving the synchronous hide/show decision returned to SystemUI.
- In COUI UDFPS mode, stop installing PixelAodHook's legacy broad FOD after-diagnostic and duplicate async hooks. `CouiUdfpsController` remains the single UiMech/async observer, so optical wake/auth no longer executes two hook stacks or eagerly builds `describeAodState`/argument dumps for every vendor callback.
- Match COUI 2.5's UDFPS refresh scheduling: once the fingerprint icon is attached, coalesced visual-state application runs through `postOnAnimation()` instead of unrestricted `MAIN.post()`, bounding visual/HDR work to the frame boundary.
- Match COUI 2.5's HDR carrier setup: `prepareHdrWindow()` is now one-shot per pressed icon and no longer posts `WindowManager.updateViewLayout()` on every visual refresh. The extra pressed-icon mutation reassert path now restores only alpha/illumination and no longer submits another SurfaceControl HDR transaction; authoritative UiMech visual-state application still owns HDR state changes.
- Make screen-off-from-unlocked COUI first-frame pre-arm idempotent. The earlier `KeyguardService#onStartedGoingToSleep` pre-arm owns the cycle; `WakefulnessLifecycle#dispatchStartedGoingToSleep` is fallback only and no longer cancels/reapplies the same SMALL/500 first frame a second time.
- Make hot notification filtering/visibility diagnostic messages lazy and avoid building full AOD state descriptions for notification snapshot diagnostics while debug logging is disabled.

### Evidence / Status
- **Success (source/reference):** the UDFPS frame scheduling and one-shot HDR-window semantics now match the anti-obfuscated COUI 2.5 implementation; the old port's repeated HDR window setup and pressed-mutation HDR transaction were confirmed divergences.
- **Success (focused regression):** notification refresh-gate, non-lockscreen pre-arm, AOD transition/overflow, UDFPS pressed visual/animation, and UDFPS state-machine tests pass.
- **Success (physical, user-observed):** unlocked-desktop → AOD and screen-off fingerprint unlock/success-ripple jank were reported as largely fixed after the 0.1.353 install.
- **Failed / carried forward:** partial-AOD notification icons can intermittently become visible at the pre-AOD anchor and then move downward to the burn-in-adjusted final position. This is corrected in 0.1.354 by porting the reference content-row alpha-only motion contract.

## [0.1.352] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation; Luna/Codex remains disabled.
- **Scope:** Remove the remaining black-frame/jank sources during the unlocked-desktop → partial-AOD SMALL weight morph without deleting COUI's intended 500→180 variable-font transition.

### Changed
- Match COUI 2.5 `PixelClockHostView.applyTargets()` ordering: every explicit target transaction now cancels any queued `scheduleApplyTargets()` runnable before calculating/applying its frame. A stale queued lockscreen/AOD target can no longer fire after the authoritative transaction and restart the clock animation.
- Match COUI 2.5 `setLiveAodContent()` ordering: pending live retargets cancel currently running property animations before staging their pre-draw target. This prevents overlapping 550 ms target sets during notification/media updates.
- Restore COUI's equal-content short circuit for regular AOD content updates and suppress controller-level live retargets when the semantic AOD kind + notification count is unchanged. Icon/media payload data still refreshes through the data-only adapter, but an unrelated notification/weather/policy callback no longer restarts the clock weight/geometry morph.
- Add one-shot ROM TextAnimator prewarm scheduled/start/complete diagnostics so the physical gate can prove whether the variable-font cache is ready before the tested transition.

### Evidence / Status
- **Focused regression:** AOD transition, applied-target, unlocked-entry normalization, non-lockscreen prearm, and ClockPlugin mapping tests pass, including new coverage that identical semantic snapshots do not request a live target replay while real count/kind changes still do.
- **Failed (physical):** user visual testing confirmed the original unlocked-desktop → screen-off AOD delay/jank remained. Screen-off fingerprint unlock and the success ripple were also visibly janky, proving the dominant problem was not duplicate 550 ms clock retargeting alone and motivating the shared hot-path correction in 0.1.353.

## [0.1.351] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation; Luna/Codex remains disabled for this stage.
- **Scope:** Close the remaining pre-Wakefulness race found during the installed 0.1.350 desktop-screen-off gate.

### Changed
- Move the non-lockscreen AOD first-frame pre-arm to the already-verified `KeyguardService$3#onStartedGoingToSleep` before-hook when it authoritatively observes `mKeyguardViewMediator.mShowing=false`. The binder event arrives before OPlus's first post-screen-off ClockPlugin render.
- Because that hook runs off the UI thread, enqueue the one-shot COUI host pre-arm at the front of the SystemUI main queue. This prevents an already queued `UI_STATE_UNLOCKED` render from starting the old 550 ms LARGE→SMALL target animation before `WakefulnessLifecycle#dispatchStartedGoingToSleep` runs.
- Keep the Wakefulness pre-arm as an idempotent fallback, keep stale UNLOCKED/KEYGUARD renders held until real AOD consumes the pre-arm, and keep wake-before-AOD cancellation unchanged.

### Evidence / Status
- **Failed 0.1.350 gate that motivated this correction:** on the real device the authoritative binder latch appeared at `10:00:04.674`, while OPlus produced `UI_STATE_UNLOCKED` renders at `10:00:04.742` and `.770`; the Wakefulness-only pre-arm was therefore too late to guarantee that no animation was ever created, even though later stale renders were successfully held and no LARGE target appeared after pre-arm.
- **Pending 0.1.351 gate:** focused/full tests, build/package inspection, overwrite install, one SystemUI reload, then the exact PIN-unlock → HOME → screen-off trace must show the binder-requested pre-arm logged before the first post-screen-off ClockPlugin render and no LARGE target / LARGE→SMALL transition.

## [0.1.350] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation; Luna/Codex remains disabled for this stage.
- **Scope:** Eliminate the remaining unlocked-desktop screen-off LARGE→SMALL intermediate clock animation.

### Changed
- Treat a confirmed screen-off-from-unlocked event as an explicit COUI host pre-arm. During `WakefulnessLifecycle#dispatchStartedGoingToSleep`, the persistent host now synchronously cancels queued/running clock animations and parks on the normalized first-frame scene (`SMALL` for notification/media content, `LARGE` for no content) before OPlus can expose the keyguard/AOD root.
- While that pre-arm is active, stale OPlus `UI_STATE_UNLOCKED` / `UI_STATE_KEYGUARD` ClockPlugin renders are semantic-data-only and cannot retarget the host. The first real AOD/panoramic-AOD render consumes the pre-arm and resumes the normal ClockPlugin presentation path. A wake before AOD clears the pre-arm.
- Keep the 0.1.349 pre-Keyguard sleep-origin latch and first-AOD normalization unchanged. The new fix addresses the earlier stage that was still visibly animating the cached `LS_LARGE` host to `LS_SMALL` for 550 ms before the real AOD render arrived.

### Evidence / Status
- **Success (focused source regression):** tests cover pre-armed UNLOCKED/KEYGUARD holds, AOD consumption, normal routing without pre-arm, existing unlocked-entry normalization, sleep-origin resolution, mapper behavior, and applied-target deduplication.
- **Pending physical gate:** full JVM suite/build/package inspection, overwrite install, one SystemUI reload, and a recorded PIN-unlock → HOME → screen-off trace are required before this fix is accepted.

## [0.1.349] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation; Luna/Codex remained disabled by explicit user instruction.
- **Scope:** Final anti-COUI media-callback parity correction discovered during the installed 0.1.348 physical gate.

### Changed
- Match the anti-obfuscation COUI 2.5 `AodMediaMonitor.refreshMediaState()` behavior instead of treating every `MediaController.Callback#onPlaybackStateChanged` as a semantic change. OPlus/PixelPlay emits playback callbacks roughly every three seconds as position advances even while package/title/artist and `PLAYING` state are unchanged; those callbacks no longer trigger a host/AOD content refresh.
- Cache the current semantic media identity (`present + package + title + artist`) and notify the single COUI host only when that identity actually changes. Session destruction re-queries active sessions, matching the anti build's controller lifecycle. `snapshot()` now consumes the cached active-media record rather than reconstructing a new app icon and media record on every unrelated notification refresh.
- Port the anti build's pre-Keyguard sleep-origin latch. `KeyguardService` binder `onStartedGoingToSleep` records `mKeyguardViewMediator.mShowing` before OPlus changes the keyguard state, so an unlocked launcher/desktop screen-off remains classified as non-lockscreen even if `KeyguardManager.isKeyguardLocked()` is already true by `WakefulnessLifecycle#dispatchStartedGoingToSleep`.
- Normalize the special screen-off-from-unlocked partial-AOD entry exactly like the anti build: notification/media content enters `beginAodEntry()` with requested scene `SMALL` on its first frame, while content `NONE` keeps `LARGE`. The raw OPlus `LARGE` partial-AOD request is no longer allowed to become a visible intermediate frame before `visualScene()` resolves to `SMALL`.
- Preserve all 0.1.348 clock target-dedup, stable-0.1.331 UDFPS idle-alpha ownership, adaptive media-icon extraction, M5 static scope, and M6 design-system changes unchanged.

### Evidence / Status
- **Success (redrive from installed 0.1.348 evidence):** fresh physical logs showed repeated `media-playback` callbacks every ~3 s with identical `contentKind=MEDIA`, which the readable anti-COUI `ActiveMedia` equality would suppress. New media identity tests and the existing semantic/clock-target regressions pass after the correction.
- **Success (focused regression):** the desktop-screen-off regression now has explicit tests proving that an authoritative `unlocked=true` pre-Keyguard latch overrides a later `keyguardLocked=true` signal, and that content-bearing partial AOD is normalized to `SMALL` before `beginAodEntry()`.
- **Pending final package/device gate:** full JVM suite, final build/package inspection, overwrite install, one SystemUI reload for 0.1.349, and fresh AOD/lockscreen/media/UDFPS runtime evidence are required below before this version is considered the final test build.

## [0.1.348] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation. Luna/Codex remained disabled by explicit user instruction.
- **Scope:** Re-derive clock/AOD/media behavior from the freshly decompiled anti-obfuscation `COUI Expressive_2.5.0.260802_anti.apk`, while replacing COUI's unreliable idle UDFPS pressed-carrier ownership with the physically stable `0.1.331` module behavior.

### Changed
- Re-decompile the supplied anti-obfuscation COUI 2.5 APK and use its readable `PixelLockscreenClockHook`, `PixelClockHostView`, and `StockUdfpsIconHook` as the runtime reference rather than relying on the older obfuscated extraction.
- Restore COUI's per-view applied-target deduplication for clock glyphs and date/weather information. Repeated time/weather/media/notification refreshes now return before `ViewPropertyAnimator.cancel()` when their target is unchanged, so a 550 ms AOD/lockscreen displacement is no longer snapped to its endpoint while the variable-font morph continues.
- Keep the COUI fingerprint glyph/HDR window path, but use the stable `0.1.331` pressed-layer ownership contract: capture the vendor pressed view's original alpha, force the pressed carrier and module illumination fully transparent whenever the live touch state is false, restore the original alpha only for a real touch, and reassert that state after the vendor's pressed-view visibility/brightness mutations. The illumination drawable now starts transparent to remove the construction-frame highlight.
- Match the anti COUI media contract: only `PlaybackState.STATE_PLAYING` owns AOD media; title/artist fall back to package/app label; adaptive app icons are reduced to their monochrome or foreground layer, alpha-trimmed into a 96×96 canvas with the reference 0.98 fill factor, and only then tinted white. This prevents the entire adaptive icon background from becoming a white rounded square.
- Revalidate M5/M6 after the runtime corrections. `staticScope=true` remains scoped only to `com.android.systemui`, the Modern entry/settings category remain present, and `PixelAodDesignSystem` still owns the complete Compose settings UI. Correct the stale `AGENTS.md` line that still described `staticScope=false` as a current build invariant.

### Validation / Status
- **Success (focused source tests):** clock applied-target/presentation/AOD/geometry tests, UDFPS pressed-animation/pressed-visual/state-machine/settings tests, and media semantic/content tests pass after their respective changes.
- **Pending (final gate):** the full JVM suite, final `git diff --check`, `assembleDebug`, packaged-Xposed metadata inspection, installation, SystemUI reload, and fresh physical AOD/lockscreen/UDFPS/media acceptance are performed after this entry and must remain evidence-backed below; no physical fix is claimed from source changes alone.

## [0.1.347] - 2026-08-18
### Meta
- **Owner / Model:** GPT-5.6 Sol direct implementation. Luna/Codex execution was explicitly disabled by the user for this stage.
- **Scope:** COUI 2.5 clock/AOD and UDFPS runtime parity correction, final COUI_PORT cutover, LSPosed Modern static scope, and settings UI design-system migration.

### Changed
- Replace the module's transient ClockPlugin HIDE/continuity heuristics with the COUI Expressive 2.5 `syncHost(render)` ownership model: only real plugin load/render changes presentation, state `0` holds, every non-zero state with a known clock scene keeps the same persistent host, and animation is driven by the real render pass plus OPlus `isAnim`.
- Make AOD Small → lockscreen Small one `CouiClockHostView.present(...)` transaction so burn-in removal / X-Y displacement and variable-font weight morph start together on the same 550 ms transition instead of jumping position before the morph.
- Re-align UDFPS hooking with COUI 2.5: intercept the alpha spring and pressed-animation decisions on `OnScreenFingerprintUiMech`, configure the vendor pressed carrier at construction without reading stale prior touch state, and drive press/HDR visuals from live `isTouchDownNow` / OPlus AOD fields rather than a parallel module visibility state machine.
- Finalize the M3/M4 primary-owner cutover: `coui_port` is now the clean-install and invalid/missing-value default; `legacy` remains an explicit startup-only rollback value. Notification/weather/contextual adapters remain data providers to the single COUI host. The explicit AOD notification contract remains five visible icons plus `+x` overflow.
- Enable LSPosed Modern static scope with `staticScope=true` and a single `com.android.systemui` `scope.list`; keep `java_init.list` as the Modern API entry and expose the settings activity through the module-settings category. The APK does not package legacy `assets/xposed_init` or libxposed implementation classes.
- Add `PixelAodDesignSystem` and migrate the complete Compose settings surface to it: dynamic wallpaper-derived Material 3 color, shared typography/shapes/spacing/motion, edge-to-edge page scaffold, grouped settings, hero/toggle/choice/slider rows, and shared selection dialogs. Existing setting keys, provider writes, permission flows, scheduling values, and language behavior are preserved.

### Validation / Status
- **Success (source/build):** the complete debug JVM suite passes (332 tests), including clock/AOD, UDFPS, renderer-routing, settings-default, semantic-adapter, notification, weather and contextual contracts; Kotlin compilation, `git diff --check`, debug assembly and packaged Xposed metadata validation pass. Final validation also corrected an old deadline-composition bug where a disabled forecast's `0` deadline could erase an active weather-alert deadline, and fixed the corresponding cross-time-zone test expectation.
- **Success (packaging):** packaged metadata is `0.1.347` / `357`, `staticScope=true`, scope is only `com.android.systemui`, Modern `java_init.list` is present, and no legacy Xposed entry or bundled libxposed implementation is present.
- **Pending (physical acceptance):** this direct-build source has not yet been accepted on-device after installation. AOD→lockscreen black-frame removal, synchronized Small-clock displacement/weight motion, and UDFPS idle/touch/release/auth visuals remain user-visible acceptance gates and are not claimed fixed until the new APK is tested.

## [0.1.346] - 2026-08-17
### Meta
- **Owner / Model:** GPT-5.6 Luna max implementation, with GPT-5.6 Sol as decision-maker/reviewer.
- **Scope:** M1 COUI UDFPS optical-press recovery and user-selectable fingerprint visual effects; no clock/AOD primary-renderer migration in this build.

### Changed
- Port the COUI 2.5 HDR optical-press carrier: 64 dp extended-sRGB illumination drawable, HDR window headroom, and SurfaceControl desired-HDR/extended-range brightness updates driven by the real OPlus touch state.
- Stop forcing OPlus `checkHasPressedAnimation` / `getScalePressedAnim` return values, observe both `onFpTouch` and `setTouchDownNow`, and leave vendor optical sensing / HBM / highlight methods un-intercepted.
- Add three default-on fingerprint effect controls: HDR highlight on press, successful-unlock Monet ripple, and AOD fingerprint exit animation. Disabling success ripple or AOD exit falls back to the native OPlus path; disabling HDR restores the remembered native pressed carrier before using the legacy SDR press effect.
- Replace the settings schema's Android-only `TextUtils.isEmpty` key check with equivalent pure-Java null/empty validation so settings defaults are JVM-testable.

### Evidence / Status
- **Success (focused tests / compile):** `CouiUdfpsStateMachineTest`, `PixelAodFeatureFlagsTest`, and `PixelAodUdfpsSettingsDefaultsTest` pass (9 tests); Debug Java/Kotlin compilation and `git diff --check` pass.
- **Deferred (manual biometric gate):** APK assembly/install, fresh HDR runtime logs, and one real enrolled-finger unlock attempt are still required before M1 can be accepted. Fingerprint recognition is not claimed fixed until that physical touch succeeds.
- **Failed:** None in the completed source/test stage; runtime recognition remains intentionally unclaimed.

## [0.1.345] - 2026-08-17
### Meta
- **Owner / Model:** Codex / GPT-5.
- **Scope:** M1 COUI UDFPS physical-event diagnostics; no state, geometry, HBM, or clock ownership changes.

### Changed
- Promote COUI `onFpTouch` and press-glow show/hide diagnostics to INFO-level LSPosed records so physical validation does not depend on Debug Logging.

### Evidence / Status
- **Success:** Source-level logging path now records finger-down/up and press-glow transitions through the normal module log path.
- **Deferred:** Real enrolled-finger press, authentication success/failure, touch/unlock, and vendor HBM/highlight still require device video plus synchronized LSPosed logs.
- **Failed:** None in this scoped diagnostic change.

## [0.1.344] - 2026-08-17
### Meta
- **Owner / Model:** GPT-5.6 Luna implementation, with GPT-5.6 Sol as decision-maker/reviewer.
- **Scope:** M1 independent COUI Expressive 2.5 UDFPS port; clock/AOD primary renderer remains on the 0.1.331 legacy path.

### Changed
- Add a startup-only `udfps_renderer` selector. `coui_port` is the default; `legacy` is the explicit rollback value. The two UDFPS replacement paths are never installed together in one SystemUI process.
- Port COUI UDFPS solid-to-dashed glyph geometry, 420 ms transition, native fade-duration clamp, press glow, success glow, live-field state refresh and asynchronous AOD-exit interception into project-native classes.
- Reconcile refreshes from live `isTouchDownNow`/AOD fields, interpret OPlus boolean and integer visibility callbacks, keep refreshes from canceling an active custom AOD exit, and resolve update-monitor authentication callbacks to their owning UI-mech instance.
- Keep passive-FOD suppression and structured diagnostics shared under COUI while excluding legacy Pixel drawable mutation and vendor-reclaim hooks from the COUI startup path.
- Keep the existing `pixel_fingerprint_icon` preference as the replacement gate and leave OPlus carrier visibility, HBM/highlight and native timeout ownership intact.

### Evidence / Success
- **Success (focused JVM tests):** COUI UDFPS state/timing, live refresh, visibility-argument, callback-owner and startup renderer-selection tests pass.
- **Success (full relevant suite/build):** 238 JVM tests ran; the only 3 failures are the pre-existing time-zone/deadline cases in `BreezyWeatherForecastTest` and `ContextualAtAGlanceSelectorTest`; `git diff --check` and `:app:assembleDebug` pass.
- **Success (APK/install):** version `0.1.344` / code `354` packages valid Xposed metadata without bundled libxposed classes; LAN CPH2573 install SHA-256 matches local `66C11C9A84EFAD589ED93FB43E0FE0EFB4C53F7110898113616A533CFAE4B3B3`; SystemUI restarted from PID `10024` to `21645`.
- **Success (fresh runtime):** persistent LSPosed log records `COUI_PORT` startup, 43 COUI hooks, shared FOD diagnostics, AOD show/hide, native timeout, custom exit start/intercept/end, and integer visibility HIDE; no exit cancellation or SystemUI crash-loop marker observed.
- **Deferred (manual-only):** safe unattended biometric interaction was not performed; press glow under touch, authentication success glow, and touch/unlock remain for manual device confirmation.
- **Failed (unrelated baseline suite):** the three time-zone/deadline failures above remain outside M1; no BreezyWeather or ContextualAtAGlance source/test was changed by M1.

## [0.1.331] - 2026-08-14
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Prevent OPlus stock AOD notification icons from becoming drawable for a frame while UDFPS unlock flips Android to interactive before the native AOD APK root has actually retired.

### Changed
- Treat `interactive=true` and "stock AOD surface is gone" as separate lifecycle facts. A new `StockAodExitRestoreGate` defers stock-view restoration only while the current trace's concrete AOD root is still attached or parented, so normal post-AOD restoration is not blocked by stale hosts from older traces.
- Observe the actual `mAodViewFromApk`/`AodRootLayout` host with a weak attach-state registration and bind it to the active AOD trace. Detach becomes the semantic handoff point that arms the existing trace-aware transition restore instead of guessing from PowerManager state alone.
- Funnel interactive/unlocked restoration paths through one guarded `restoreStockViews(...)` path. If the current AOD root is still retiring, re-assert native clock/keyguard/`NotificationView` suppression rather than restoring their saved alpha/visibility and exposing stock notification glyphs.
- Put the same retiring-host guard in the 900 ms transition restore. If OPlus still owns the AOD root at the delayed checkpoint, stock views remain hidden and the concrete host-detach callback becomes the next restoration opportunity.
- Restore adjusted status-view state together with hidden stock-view state once the transition guard finally permits restoration, keeping the two pieces of saved native state atomic.
- Preserve the 0.1.330 non-lockscreen pre-Doze ClockPlugin parking/Capsule null fast-path and the 0.1.320 clock-size transition implementation unchanged.

### Evidence / Success
- **Reproduced log:** on trace `aod-10-9898b34`, UDFPS touch occurs at `16:47:59.818`; by `16:48:00.501` Android reports `interactive=true` while `com.oplus.aodimpl.AodRootLayout` still has parent `AodClockLayout`. At `16:48:00.502` policy changes to `shouldSuppressStockAodViews=false`, immediately followed by `restored hidden stock AOD views`; OPlus calls `AodClockLayout#onAttachedToWindow` again four milliseconds later and a second restore happens at `16:48:00.507`.
- **Targeted JVM tests:** `StockAodExitRestoreGateTest`, `ClockPluginNonLockscreenEntryGateTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` pass under JDK 17. The new gate covers current-trace attached-host deferral, detach release, stale-trace release, and unchanged non-interactive AOD ownership.
- **Full JVM suite caveat:** `:app:testDebugUnitTest` ran 230 tests with 3 failures in untouched time-boundary tests (`BreezyWeatherForecastTest.usesDeviceLocalTomorrowAndReevaluatesAcrossTimeZones` and two `ContextualAtAGlanceSelectorTest` deadline cases). No BreezyWeather/At-a-Glance source or test file is modified by this change; the AOD-exit targeted suites remain green.
- **Debug build:** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK badging and packaged Xposed metadata both report `0.1.331` / versionCode `341`; SHA-256 is `C71C8E8FF25F6711E3EE5CBD86B6F1DCED8EE33D49F9A02C468879B102C275BB`.
- **Installation:** exactly one CPH2573 was online; `adb install -r` succeeded without uninstalling, SystemUI restarted and returned with PID `18195`, and `dumpsys package dev.codex.pixelaod` reports `0.1.331` / `341`.

### Device verification
- **Success:** user confirmed on-device that the stock AOD notification-icon flash no longer appears during the affected unlock/exit path.
- **Separate follow-up:** app/desktop-to-screen-off still has a perceptible delay after the OnePlus lock haptic; this is not considered resolved by the stock-AOD-exit fix and will be optimized separately.

## [0.1.330] - 2026-08-14
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Remove the stale Large-clock first frame and reduce SystemUI main-thread exception pressure during app/desktop-to-AOD screen-off when notifications require Small.

### Changed
- Keep `ClockPluginNonLockscreenEntryGate` as the owner of app/desktop screen-off traces even after its 120 ms optional pre-presentation deadline expires. While that owned trace is non-interactive but the display/vendor state has not reached AOD, park the persistent ClockPlugin host `INVISIBLE` instead of allowing its last lockscreen scene to become drawable.
- Park the host immediately from the screen-off pre-present path and re-assert the park from `syncHost()` when OPlus publishes stale `KEYGUARD` state. This closes the observed `stable-scene-skip` hole where `LOCKSCREEN_LARGE` was already committed and therefore skipped `PixelClockPluginHostView`'s existing non-lockscreen direct-AOD fallback.
- Preserve the committed scene while parked. Once native DOZE/AOD arrives, host invisibility forces one real presentation and the existing AOD policy selects `AOD_SMALL` directly when module notification content is present; no new same-surface size animation is introduced and the 0.1.320 transition layer remains unchanged.
- Make `PixelClockPluginHostView.parkForNonLockscreenAod(...)` idempotent so repeated vendor renders during the multi-second OPlus screen-off transition do not repeatedly cancel animations or emit extra debug work.
- Treat transient OPlus Capsule `iconData == null` / `entry == null` as an ordinary cache miss. The previous reflection chain threw `NullPointerException` on the SystemUI main thread and wrote a full LSPosed stack trace; the captured log contained 343 occurrences, including one during the reproduced screen-off.

### Evidence / Success
- **Reproduced log:** trace `aod-50-9649738` starts at `16:07:37.029` with `fromInteractiveLockscreen=false` and `compact=true`; at `16:07:37.039` OPlus still publishes `uiState=KEYGUARD`, `clockSizeState=LARGE`, and the controller logs `presentation=stable-scene-skip scene=LOCKSCREEN_LARGE`. Native DOZE is not reported until roughly 2.6 s later, at which point the final scene is `AOD_SMALL`.
- **Targeted JVM tests:** `ClockPluginNonLockscreenEntryGateTest`, `ClockPluginSceneMachineTest`, `ClockPluginPresentationGateTest`, `CouiClockSizeTransitionLayerTest`, `CouiClockSizeTransitionMathTest`, and `NotificationCapsuleIconPolicyTest` pass under JDK 17. New gate coverage verifies trace ownership, parking beyond the 120 ms pre-presentation deadline, and release for different traces, interactive wake, display-Doze, or vendor AOD state.
- **Debug build:** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK badging and packaged Xposed metadata both report `0.1.330` / versionCode `340`; SHA-256 is `4B625998C109CC87F74976F023AFF2A0FE8BB77F778E7CB4CD670DD329E3E7BC`.
- **Installation:** exactly one CPH2573 was online; `adb install -r` succeeded without uninstalling, SystemUI restarted and returned with PID `17844`, and `dumpsys package dev.codex.pixelaod` reports `0.1.330` / `340`.

### Deferred
- **Device verification:** a fresh app/desktop screen-off with visible notifications is still required before claiming the Large-clock flash or perceived screen-off stutter fixed on-device.

## [0.1.329] - 2026-08-13
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Rebalance Small AOD vertical stacking when media and contextual warning content are present, using the user's three 930×2048 device screenshots as the calibration reference.

### Changed
- Preserve the notification-only Small baseline unchanged: the screenshot with only notification glyphs places their visible top at about y=452 px and remains the reference lower-content anchor.
- Move the Small media default anchor from the legacy fixed 218 dp position to the same 171 dp lower-content baseline used by the notification-only row (`SMALL_INFO_TOP_DP + COMPACT_DATE_TO_NOTIFICATION_WITHOUT_EVENT_TOP_OFFSET_DP`). On the supplied device screenshots this removes roughly 47 dp / 124 px of empty space before the media title.
- Keep contextual warnings/calendar rows authoritative when they actually occupy the baseline: `mediaTopAfterInfo(...)` still pushes media below the measured contextual bottom plus the existing compact information gap, preventing overlap while avoiding the old unconditional 218 dp floor.
- Replace the hard-coded 28 dp media-to-notification gap with a named 12 dp compact gap. Notifications therefore move down only by the media row's real height plus a modest optical separation, rather than adding another large spacer after media.
- Do not change the clock/date/weather anchors, warning-row position, horizontal painted-edge calibration, battery row, Large layout, Dynamic clock policy, FOD timeout ownership, or the 0.1.320 clock-size transition implementation.
- Bump AOD visual profile revision to 7.3 and include the Small media anchor and media-to-notification gap in profile diagnostics.

### Success
- **Success (screenshot analysis):** supplied 930×2048 frames measure notification-only glyphs at y≈452..483; media frames at y≈575..675 with notification glyphs at y≈756..795; warning+media similarly keeps media at y≈575. These measurements isolate the regression to the old 218 dp media floor plus the 28 dp post-media gap rather than the standalone notification anchor.
- **Success (targeted JVM tests):** 52 tests across `CouiCompactLayoutTest`, `AodInfoStackLayoutTest`, `PixelAodVisualStyleTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` pass under JDK 17 after updating the Small media baseline invariant and adding compact media→notification coverage.
- **Success (diff hygiene):** `git diff --check` passes; only the expected CRLF conversion warnings are reported by Git on Windows.
- **Success (debug build):** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK badging and packaged Xposed metadata both report `0.1.329` / versionCode `339`; SHA-256 is `F95D6BFC22AED5EE6EBB163A2FAF1CF570DA14B49AEACAB3C0A1C5AC2EA28AD7`.
- **Success (installation):** exactly one CPH2573 was online; `adb install -r` succeeded, SystemUI was restarted without uninstalling the module, returned with PID `7301`, and `dumpsys package dev.codex.pixelaod` reports `0.1.329` / `339`.

### Success (device verification)
- **Success (device visual verification):** user confirms the 0.1.329 Small AOD media/warning/notification spacing is visually correct on-device after installation; this layout is now device-validated.

## [0.1.328] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Fix the AOD fingerprint icon being reclaimed after ColorOS has already issued its native timeout hide, while keeping continuous Pixel AOD/Doze alive.

### Changed
- Add a trace-scoped `FodNativeTimeoutHideGate`. The latch is armed **before** invoking `OnScreenFingerprintUiMech#notifyHideAodIcon()` so the module's own hook-after and any synchronous `ImageView` mutations inside that OPlus method cannot race ahead and immediately reclaim the carrier. Failed reflective dispatch rolls the latch back.
- While the latch is active for the current non-interactive AOD trace, `PixelFingerprintIconPolicy` now returns `SKIP` for carrier refresh even when the broader module AOD policy still allows display. This separates "keep Pixel AOD/Doze alive" from "keep FOD visible" and prevents `stopSwitchAnim`, `setImageDrawable`, vendor view mutations, style-only refreshes, and delayed reclaim work from re-wrapping the timed-out FOD carrier.
- Release the latch on explicit `onFpTouch` **before** refreshing the Pixel carrier, on an interactive wake, or when a different AOD trace takes ownership. This keeps legitimate fingerprint interaction/new lock cycles able to show the Pixel icon again without imposing an arbitrary wall-clock suppression timeout.
- Cancel an already queued first-pass vendor reclaim when refresh is blocked, reducing post-timeout races from work scheduled before the native hide.
- Remove the `markRecentAodOverlayVisible(...)` renewal from successful FOD-only timeout dispatch. That call was semantically backwards for a hide event and extended the generic AOD-overlay visibility state that contributed to the observed reclaim chain. The outer Pixel AOD/Doze keepalive remains independent.
- Preserve the 0.1.327 lockscreen/AOD Small contextual-row optical alignment changes in the same build.

### Success
- **Success (targeted JVM tests):** `FodNativeTimeoutHideGateTest`, `PixelFingerprintIconPolicyTest`, `PassiveFodShowGateTest`, `OosAodLifecycleAdapterTest`, `CouiCompactLayoutTest`, `WeatherAlertDisplayFormatterTest`, `CouiClockSizeTransitionLayerTest`, `CouiClockSizeTransitionMathTest`, and `PixelDynamicClockPolicyTest` pass. New cases verify trace scoping/rollback/clear semantics and that a native FOD timeout outranks continuous-AOD carrier refresh while interactive refresh remains allowed.
- **Success (diff hygiene):** `git diff --check` passes before the final build.
- **Success (debug build):** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK badging and packaged Xposed metadata both report `0.1.328` / versionCode `338`; SHA-256 is `0D8BAD2C856261FD7090D36DC46BFA5E2B652EAE71E2FD4FFA1E5BEFF34A58DA`.

### Deferred/Failed
- **Deferred (device verification):** `adb devices -l` is empty after the build, so 0.1.328 was not installed and SystemUI was not restarted. This build cannot be claimed fixed until an on-device timeout reproducer confirms that native FOD disappears and stays hidden while the rest of Pixel AOD remains visible, then reappears normally on explicit fingerprint interaction or the next lock cycle.

## [0.1.327] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Synchronize Small lockscreen/AOD optical leading-edge geometry and investigate the 22:11 AOD fingerprint timeout-stuck report without changing fingerprint behavior.

### Changed
- Make Small lockscreen contextual/forecast rows use the same `CouiCompactLayout.contextualLayoutLeft(...)` painted-edge helper as AOD. The lockscreen previously reapplied the legacy 34 dp `EDGE_DP` while AOD used the new 32 dp optical target, so the same forecast row visibly shifted between lockscreen and AOD.
- Re-assert the compact contextual left margin inside `updateInfoGroupLayout()` as well as `updateTime()`, so a Small/Large mode switch cannot leave the lockscreen row at a stale large-mode X coordinate. Calendar application-icon leading compensation is routed through the same shared helper on both surfaces.
- Align ClockPlugin lockscreen→AOD notification-icon handoff rows with `CouiCompactLayout.notificationLayoutLeft(...)` while Small; normal lockscreen/Large geometry remains on the existing edge. Vertical spacing, Dynamic policy, alert localization/severity, and the 0.1.320 size-transition ownership fix are unchanged.
- **No fingerprint behavior change in this release.** The fingerprint finding below is diagnostic only, per user request.

### Fingerprint investigation
- In `modules_2026-08-12T22:11:27.359664.log`, the timeout path is present at `22:12:01.408`: `OnScreenFingerprintUiMech#notifyHideAodIcon()` is invoked successfully. The module then records `FOD-only native-timeout hide invoked` and deliberately suppresses the enclosing `OplusWakeUpController#notifyHideCallback()` so continuous Pixel AOD/Doze remains alive.
- The hide does not establish durable fingerprint ownership. Roughly one second later `OnScreenFingerprintIcon#stopSwitchAnim` reports the carrier View still `VISIBLE` with alpha `0.8` and a `PixelFingerprintAnimCarrier`; the AOD policy at `22:12:02.459` still returns `REFRESH_CARRIER` semantics because module display policy is allowed. `setVisibilityInAOD(int)` then appears with argument `1` at `22:12:02.464`.
- Current strongest hypothesis is a fingerprint-specific hide/reclaim race inside the module's continuous-AOD ownership model: the FOD-only hide is issued, but a later vendor animation mutation is eligible for module carrier refresh while the underlying View remains visible. `dispatchFodOnlyNativeTimeoutHide()` also marks the Pixel overlay recently visible after invoking the hide, extending the state used by native-hide/Doze decisions. This is sufficient to explain why a full interactive wake→lock cycle clears the stale carrier, but the exact OPlus semantics of `setVisibilityInAOD(1)` are not proven from the available source dump/log alone.

### Success
- **Success (log investigation):** The 22:11/22:12 LSPosed trace rules out "no hide callback" and narrows the failure to post-hide fingerprint carrier/state ownership. No speculative fingerprint patch is included.

### Success (continued)
- **Success (targeted JVM tests):** `CouiCompactLayoutTest`, `WeatherAlertDisplayFormatterTest`, `CouiClockSizeTransitionLayerTest`, `CouiClockSizeTransitionMathTest`, and `PixelDynamicClockPolicyTest` pass under JDK 17. The shared compact contextual helper is now exercised for both normal and calendar-application-icon leading offsets.
- **Success (debug build):** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK and packaged Xposed metadata both report `0.1.327` / versionCode `337`; SHA-256 is `D8E456F87D6B0C6743EA103A7E6BC5100F3B2C29F5B1BFAC32EC53E80DA47F85`.
- **Success (diff hygiene):** `git diff --check` passes; the build contains no fingerprint-code modification.

### Deferred/Failed
- **Deferred (fingerprint):** Do not change the FOD timeout/reclaim policy until `setVisibilityInAOD(int)` semantics and the intended OPlus `stopSwitchAnim`/drawable-clear ordering are verified from a usable vendor decompile or a more focused trace.
- **Deferred (installation/device verification):** `adb devices -l` is empty after the build. Per project rules, 0.1.327 was not installed and SystemUI was not restarted; final lockscreen/AOD optical alignment requires user device verification.

## [0.1.326] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Correct the 0.1.325 Small-AOD horizontal alignment regression by aligning actual painted content instead of forcing unrelated View layout boxes onto one X coordinate.

### Changed
- Replace the 0.1.325 shared-layout-edge rule with an explicit painted-edge optical model. Device screenshots showed the Small clock painted edge around x=83..85 while the forced-layout version moved the forecast icon to about x=71 and notification glyphs to about x=72, even though their View margins were mathematically aligned.
- Define a 32 dp Small painted target edge and model the measured per-element leading inset separately: the Google Sans Small clock keeps its 27 dp layout origin plus an approximately 5 dp glyph side-bearing; contextual forecast/warning icons use a 32 dp layout origin; notification glyphs use a 33 dp layout origin, then retain the existing -2 dp row translation plus approximately 1 dp glyph inset. The three resulting painted edges resolve to the same 32 dp optical line without moving the clock itself.
- Keep Large geometry, vertical spacing, Breezy localization/severity visuals, Dynamic clock policy, and the device-validated size-transition animation unchanged.
- Extend the AOD profile log with the painted target edge plus contextual and notification layout origins so future alignment work can distinguish layout coordinates from visible ink coordinates.

### Success
- **Success (screenshot calibration):** Compared the approved pre-alignment AOD screenshot with the rejected 0.1.325 alignment screenshot. Before the regression the forecast/notification painted edges were only about 5 px / 2 px to the right of the clock; the shared-layout-edge change moved them roughly 19 px / 15 px left. 0.1.326 applies only the smaller optical corrections implied by those measured deltas.
- **Success (targeted JVM tests):** `CouiCompactLayoutTest`, `WeatherAlertDisplayFormatterTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` pass under JDK 17, including a new painted-edge equality assertion after clock/contextual/notification-specific insets and translations are applied.

### Success (continued)
- **Success (debug build):** JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK metadata reports `0.1.326` / versionCode `336`; SHA-256 is `30D86138239170655B1347852F04551C74B24BE3D1A7F70BF905AB041B617C82`.
- **Success (installation):** Installed with `adb install -r` on the connected CPH2573, restarted `com.android.systemui`, and confirmed SystemUI returned with PID `19948`; `dumpsys package dev.codex.pixelaod` reports `0.1.326` / `336`.

### Deferred/Failed
- **Deferred (device visual verification):** Final painted-edge equality still requires the user to inspect a fresh AOD screenshot before this optical calibration should be committed.

## [0.1.325] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Add deterministic offline English presentation for Breezy Weather warnings and expose Breezy's structured alert severity through progressively stronger contextual warning icons.

### Changed
- Add `WeatherAlertDisplayFormatter`, a presentation-only Chinese weather-warning formatter. It keeps Breezy's original headline untouched for identity/dedup/cooldown, uses structured severity (`1..4`) for Blue/Yellow/Orange/Red, recognizes common Chinese meteorological hazard terms with a longest-match dictionary, and emits the fixed English template `{Color} alert for {hazard}`. Known examples now render `中原发布暴雨蓝色预警` as `Blue alert for rainstorms` and `中原发布暴雨红色预警` as `Red alert for rainstorms`.
- Preserve already-English/non-Chinese warning headlines verbatim. Unknown Chinese warning types also remain in their original source language rather than being guessed or sent to a network service.
- Carry `alert.severity` into `ContextualAtAGlanceCard` and include it in `sameContent()`, so a provider severity escalation refreshes the contextual presentation even if the logical warning identity is otherwise unchanged.
- Map alert severity to distinct monochrome AOD/lockscreen silhouettes: UNKNOWN/MINOR uses the existing warning triangle, MODERATE uses a warning diamond, SEVERE uses a warning shield, and EXTREME uses a warning octagon. Existing Material tinting remains unchanged; severity is conveyed by icon geometry rather than introducing colored AOD pixels.
- Add `docs/WEATHER_ALERT_LOCALIZATION.md`, including the deferred optional ML Kit on-device translation fallback for dictionary misses. Any future ML Kit path must run outside SystemUI drawing, cache by normalized source headline, preserve Breezy severity/identity, and fall back to the original headline on model/download/translation failure.
- Align the Small AOD contextual row and notification-glyph row to the same leading edge as the Small clock. The contextual row now uses `CouiCompactLayout.leadingEdge()` instead of the older 34 dp information edge, while the notification row offsets its layout margin by the existing 2 dp optical translation so its final painted edge resolves to that same clock baseline. Large-mode geometry is unchanged.

### Success
- **Success (targeted JVM tests):** `WeatherAlertDisplayFormatterTest`, the alert privacy/severity-replacement cases in `ContextualAtAGlanceSelectorTest`, `BreezyWeatherAlertTest`, `PixelDynamicClockPolicyTest`, `CouiCompactLayoutTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` pass under JDK 17.
- **Success (debug build):** After one Gradle daemon disappearance with no compile error, the required JDK 17 `:app:assembleDebug --rerun-tasks --no-daemon` retry completes with `BUILD SUCCESSFUL` and 39/39 tasks executed. APK and packaged Xposed metadata both report `0.1.325` / versionCode `335`; final SHA-256 is `5A3BD61716498532451A728DB0A0928BD65127C953C51F94B55E56A112E5FDD2`.
- **Success (installation):** Installed the final alignment build with `adb install -r` on the connected CPH2573; SystemUI is running again (PID `29403`) and `dumpsys package dev.codex.pixelaod` reports `0.1.325` / `335`.
- **Success (device wording/severity verification):** User AOD testing confirms `中原发布暴雨红色预警` renders as `Red alert for rainstorms` and the EXTREME warning uses the intended octagonal monochrome icon. The same screenshot exposed a remaining horizontal grid mismatch, addressed by the shared Small leading-edge refinement above.

### Deferred/Failed
- **Deferred (ML Kit):** ML Kit on-device translation is documented as a future optional fallback only and is not bundled in 0.1.325; deterministic dictionary formatting remains the sole active translation path.
- **Deferred (known pre-existing tests):** The broader targeted run still reports the two existing `ContextualAtAGlanceSelectorTest` deadline-policy failures (`schedulesAlertEndBeforeTheTenMinuteDisplayDeadline` and `schedulesSourceFreshnessExpiryBeforeTheTenMinuteDisplayDeadline`); all other 58 tests in that run pass, including the new localization/severity/leading-edge coverage.
- **Deferred (final edge visual verification):** The final Small leading-edge refinement is built after the approved screenshot but still requires one device screenshot to confirm the optical result; do not treat the alignment as user-verified yet.

## [0.1.324] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Restore a clear vertical hierarchy between the Small clock, contextual warning row, and AOD notification glyphs after 0.1.323 device feedback.

### Changed
- Make the runtime Small layout actually use `CouiCompactLayout.weatherAlertTop()` as the contextual-row minimum. The helper already encoded clock clearance, but 0.1.323 only exercised it in tests while the live AOD/lockscreen path still used `infoTop + COMPACT_DATE_TO_EVENT_TOP_OFFSET_DP`, allowing the alert to crowd the clock.
- Increase the Small clock-to-contextual minimum gap from 6 dp to 12 dp. On the 4x-density OnePlus 12 canvas this reserves 48 px between the clock layout box and the contextual row.
- Add a dedicated 12 dp contextual-to-notification gap instead of reusing the 4 dp date/weather stack gap. The notification glyph row now stays visibly separated from an alert/calendar row without unnecessarily expanding the date-to-weather spacing.
- Align the compact AOD handoff notification anchor with the new contextual stack (`COMPACT_DATE_TO_NOTIFICATION_TOP_OFFSET_DP` 57→85 dp) so lockscreen→AOD handoff does not momentarily place glyphs at the old compressed coordinate.
- Keep the 0.1.323 global Small positions (`clockTop=90 dp`, `infoTop=99 dp`, `smallMediaTop=218 dp`), the 0.1.322 vendor-authoritative Dynamic policy, and the 0.1.320 transition ownership fix unchanged.

### Success
- **Success (device-feedback localization):** The 0.1.323 screenshot shows the global Small scene at the intended height, but the clock, weather-alert row, and notification glyphs form one visually compressed block. This revision changes only the two internal vertical gaps responsible for that result.

### Success (continued)
- **Success (targeted JVM tests):** `PixelDynamicClockPolicyTest`, `NotificationCapsuleClockModeTest`, `CouiCompactLayoutTest`, `PixelAodVisualStyleTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` all pass under JDK 17, including a new assertion that the contextual row and notification glyphs retain a 12 dp separation on the 4x-density reference canvas.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` completes with 39/39 tasks executed. APK and packaged Xposed metadata both report `0.1.324` / versionCode `334`; SHA-256 is `88860B3B5527873F9E27662B1EB773F2C873FBE3B6EAE0DD518518AEFD8B4C43`.
- **Success (installation):** Installed with `adb install -r` on the connected CPH2573, restarted `com.android.systemui`, confirmed SystemUI returned with PID `28960`, and `dumpsys package dev.codex.pixelaod` reports `0.1.324` / `334`.

### Success (device verification)
- **Success:** User testing confirmed the 0.1.324 Small AOD spacing is substantially improved and approved for commit; the clock, warning row, and notification glyphs now read as distinct vertical layers.

## [0.1.323] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Rebalance the Pixel-style Small scene after device feedback without changing the recovered Dynamic state machine or the device-validated size-transition transaction.

### Changed
- Move the Small clock and its date/weather information group down by 16 dp (`74→90 dp` clock top, `83→99 dp` info top) so the scene no longer crowds the face-unlock/charging area while preserving their measured optical center relationship.
- Raise the Small AOD media baseline by 16 dp (`234→218 dp`) so title/artist content sits closer to the clock information group instead of drifting too far down the AOD canvas.
- Reduce Small notification offsets by the same 16 dp so moving the clock/info group down does not push notification glyphs lower when media is absent; when media is visible, the icon row still follows the raised media stack and therefore moves upward with it.
- Keep the 0.1.322 vendor-authoritative Dynamic policy, the 0.1.321 edge-anchored Small horizontal layout, and the 0.1.320 per-glyph transition ownership fix unchanged.

### Success
- **Success (device feedback basis):** User testing confirmed 0.1.322 restored automatic Small/Large switching; screenshots then showed the new Small clock too close to the face-unlock icon and the AOD media/notification rows too low, which this revision addresses directly.

### Success (continued)
- **Success (targeted JVM tests):** `PixelDynamicClockPolicyTest`, `NotificationCapsuleClockModeTest`, `CouiCompactLayoutTest`, `PixelAodVisualStyleTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` all pass under the repository JDK 17 environment.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` completes with 39/39 tasks executed. APK and packaged Xposed metadata both report `0.1.323` / versionCode `333`; SHA-256 is `C3EE692A9E4EFC0F184BD8E020F08B9B73209F664D0CE8D5C709F9B1C98ECC34`.

### Deferred/Failed
- **Deferred (installation):** The device disconnected before installation (`adb devices` returned no online device), so 0.1.323 was not installed or SystemUI-restarted in this pass.
- **Failed (visual verification):** After the user installed 0.1.323, the overall Small scene height was improved but the clock, contextual weather-warning row, and notification glyphs were still packed too tightly vertically. The live runtime was not applying the existing `weatherAlertTop()` safety anchor and still reused a 4 dp stack gap below contextual content. Superseded by 0.1.324.

## [0.1.322] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Restore automatic Small/Large switching after the 0.1.321 Dynamic-clock regression while retaining the new Pixel-style Small layout and unified AOD media profile.

### Fixed
- Remove the 0.1.321 whole-SystemUI `isLargeClockSpaceBlockedIn()` classifier. Persistent OPlus capsule/background containers are not notification-card geometry and must never override the vendor clock-size state.
- Make a valid OPlus `ClockPlugin.clockSizeState` authoritative on the interactive lockscreen. Only when that vendor state is missing/invalid does the module inspect actual visible lockscreen card content; raw notification/MediaSession presence remains the final bootstrap fallback.
- Make the standalone lockscreen fallback follow actual visible card presence rather than raw active notification/media state, so dismissed/collapsed content cannot pin Small.
- Keep the 0.1.321 Pixel-style edge-anchored Small geometry, AOD media constant unification, and the device-verified 0.1.320 per-glyph transition ownership fix unchanged.

### Success
- **Success (device-log root cause):** The 0.1.321 LSPosed log shows OPlus correctly publishing `oosClockSize=1` (LARGE) while the module forced `effectiveClockSize=0` (SMALL) because `largeClockBlocked=true`. False positives included full-screen `OplusImmersiveBgContainer` (`top=0 bottom=3168`) and `capsule_fake_container` (`top=0 bottom=160`).
- **Success (targeted JVM tests):** `PixelDynamicClockPolicyTest`, `NotificationCapsuleClockModeTest`, `CouiCompactLayoutTest`, `PixelAodVisualStyleTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` pass under JDK 17. The Dynamic policy regression test now requires vendor LARGE/SMALL to win even when fallback signals disagree.

### Success (continued)
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` completes under the repository JDK 17 environment with 39/39 tasks executed. APK and packaged Xposed metadata both report `0.1.322` / versionCode `332`; SHA-256 is `B861F637663E9E25AB7C98D5BCE05BEE67E15D20EC7B73D01022730E0593EF6D`.
- **Success (installation):** `adb install -r` succeeds on the connected CPH2573, `dumpsys package dev.codex.pixelaod` reports `0.1.322` / `332`, and SystemUI returns after the root restart with PID `16635`.

### Success (device verification)
- **Success:** User testing confirmed 0.1.322 restored automatic Small/Large switching. The remaining feedback was visual spacing only: the Small scene sat too high and the Small-AOD media/notification rows sat too low, addressed separately in 0.1.323.

## [0.1.321] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol.
- **Scope:** Align the Small/Dynamic lockscreen scene more closely with Pixel behavior and remove stale duplicate AOD media geometry constants.

### Changed
- Replace the fixed COUI 25%/75% compact-scene anchors with a Pixel-style Small layout: the clock is edge-anchored while the date/weather/contextual information uses a width-aware right column. The small scene now uses stable dp anchors instead of viewport-fraction offsets inherited from the OOS 16 COUI reference.
- Add `PixelDynamicClockPolicy` and make visible lockscreen content pressure the strongest module-owned Small/Large signal. Native OOS `clockSizeState` remains the next fallback, while raw notification or MediaSession presence is only used when neither measured geometry nor a valid vendor state is available.
- Detect whether visible notification/media card content actually intersects the reserved large-clock region before forcing Small, so a notification that has collapsed away from the clock area no longer needs to keep the module compact solely because the notification/session is still active.
- Keep the existing COUI per-glyph size-transition implementation and the 0.1.320 ownership fixes unchanged; this update changes target Small geometry and size policy, not the transition transaction model.

### Fixed
- Eliminate the AOD media-profile mismatch: title size/weight, artist size, icon size/spacing, and subtitle gap now come from `PixelAodVisualStyle.Aod` and are the same values used by `PixelAodClockView` at render time. The visual profile log therefore reflects the actual media row instead of the previous stale 13dp/14dp constants.

### Success
- **Success (targeted JVM tests):** `PixelDynamicClockPolicyTest`, `NotificationCapsuleClockModeTest`, `CouiCompactLayoutTest`, `PixelAodVisualStyleTest`, `CouiClockSizeTransitionLayerTest`, and `CouiClockSizeTransitionMathTest` all pass under the repository JDK 17 environment.

### Success (continued)
- **Success (debug build):** With the repository JDK 17 environment, `:app:assembleDebug --rerun-tasks` completes successfully with 39/39 tasks executed. The APK reports `versionName=0.1.321` / `versionCode=331`, and packaged `META-INF/xposed/module.prop` matches.
- **Success (installation):** Installed `0.1.321` with `adb install -r`, restarted `com.android.systemui` through root, confirmed SystemUI returned with PID 1739, and `dumpsys package dev.codex.pixelaod` reports `0.1.321` / `331`.

### Deferred/Failed
- **Failed (device verification):** Automatic Small/Large switching regressed and the clock remained Small. Persistent LSPosed logs showed the new whole-tree geometry classifier falsely treating OPlus capsule/background containers as large-clock blockers and overriding a correct vendor LARGE state. Superseded by 0.1.322.

## [0.1.320] - 2026-08-12
### Meta
- **Owner / Model:** ChatGPT Web / GPT-5.6 Sol — took over the remaining clock-size transition bug from the prior Codex investigation and completed the final device-validated fix.
- **Scope:** Remove the pre-animation whole-digit jump exposed by the 0.1.319 high-frame-rate recording by freezing the synthetic COUI digit-slot geometry after the prepared source becomes drawable.

### Fixed
- Stop resizing the four visible digit clones and colon inside `start()`. `prepare()` now establishes their physical overlay boxes once, while the layer is still invisible; after source-frame ownership is acquired, those boxes remain immutable for the entire size transaction and only position, scale, alpha, and variable-font weight are animated.
- Keep date/weather/contextual text and icon tracks independently configurable. The new recording proves those tracks do not participate in the approximately 270 px bad-frame displacement, so their existing geometry/alpha behavior is intentionally unchanged.
- Retain the 0.1.319 host-local `RootSpaceMapper` as a correct coordinate-space cleanup, but no longer treat an OPlus ancestor/root transform as the cause of this bug.
- Mirror the relevant COUI ownership model more closely: the decompiled SystemUI `ClockPlugin` loads `com.oplus.keyguard.personality.clocks` and forwards `clockSizeState` through `onClockSizeChanged`; prior runtime hierarchy evidence shows distinct `DigitalTimeView` children for the large clock. The module's transition clones now likewise keep each digit child as a stable geometry owner instead of changing its bounds after it becomes visible.

### Success
- **Success (frame-level failure localization):** In `screenshots/lock-to-aod.mp4`, frames 195-199 show the stable small clock; frames 200-203 preserve the same roughly 162-166 px digit height but move the digit group left by about 270 px; frame 204 returns to the correct small-clock positions; frame 205 onward begins the actual small-to-large scale/motion. Date/weather move only about 0-2 px in the same bad-frame window. This isolates the failure to digit-clone setup before animation progress starts.
- **Success (code-path correlation):** The only start-time operation that mutates digit geometry before frame zero is `configureOverlayGeometry(from, to) -> configureBox() -> setLayoutParams()/measure()/layout()`. That second visible-slot configuration is now gated out after the source prepare pass.
- **Success (COUI host-source audit):** Saved OOS SystemUI decompile confirms non-legacy clocks are loaded from `com.oplus.keyguard.personality.clocks`, and a size-state change is dispatched as `onClockSizeChanged` rather than by rebuilding the SystemUI host view.
- **Success (targeted JVM tests):** `CouiClockSizeTransitionLayerTest` passes 12/12 and `CouiClockSizeTransitionMathTest` passes 21/21, including a new regression policy that prepared digit-slot geometry cannot be configured a second time until transaction reset.
- **Success (debug build):** With the project-standard JDK 17, `:app:assembleDebug` completes successfully and produces `0.1.320` / versionCode `330` with matching packaged Xposed metadata.
- **Success (device verification):** After installing 0.1.320, repeated notification swipe tests confirmed the small-clock → large-clock transition no longer produces the pre-animation whole-digit jump. The user explicitly confirmed the fix is successful on device.

### Deferred/Failed
- **Failed (0.1.319 root-transform hypothesis):** The new recording disproves the whole-host/ancestor-coordinate explanation: date/weather remain stationary while only digit clones jump, and the bad frames occur before the actual size interpolation begins.
- **Failed (first full-build environment attempt):** A rerun launched against the machine's Temurin 21 daemon and that daemon disappeared after Java compilation. Rebuilding under the repository's documented JDK 17 environment succeeds; no source failure was involved.

## [0.1.319] - 2026-08-11
### Meta
- **Model:** GPT-5.6 Sol
- **Scope:** Correct the whole-clock leftward size-transition failure at the overlay coordinate-system boundary instead of applying another glyph-shape compensation.

### Fixed
- Stop capturing transition geometry as `descendant.getLocationOnScreen() - host.getLocationOnScreen()` and then treating that screen-derived delta as host-local X/Y. The per-glyph overlay now maps clock/date/weather/contextual points through the descendant-to-host view hierarchy only, including child layout/scroll/matrix transforms while deliberately excluding the host and its OPlus ancestors. Those outer transforms are therefore applied exactly once when the host/overlay is drawn instead of being baked into the snapshot and applied again.
- Use the same host-local coordinate mapper for clock digits, information text corridors, compound weather icons, and contextual forecast/alert icons so all tracks share one geometry owner throughout the 550 ms transaction.
- Add a low-volume persistent diagnostic, `corrected COUI transition coordinate ownership`, which records the legacy screen delta versus host-local origin only when they differ materially. A subsequent device log can therefore prove whether an OPlus ancestor transform was active at the exact transition capture without logging every animation frame.
- Keep the 0.1.317 redirected easing/target-layout readiness behavior and the confirmed 0.1.316 forecast-alpha fix. The 0.1.318 fixed-digit-cell change remains as COUI-style slot geometry, but is no longer treated as the explanation for the reported whole-clock drift.

### Success
- **Success (latest LSPosed export):** `LSPosed_20260811_230515.zip` shows the residual issue occurring on the COUI per-glyph path and contains no legacy `size-morph-clock`/whole-TextView fallback starts in the relevant run, ruling out that fallback as the observed whole-group displacement path.
- **Success (code-path audit):** Transition capture no longer uses `getLocationOnScreen()` for animation geometry; the only remaining calls are isolated in the new comparison diagnostic.
- **Success (targeted JVM tests):** `CouiClockSizeTransitionLayerTest` and `CouiClockSizeTransitionMathTest` pass after the coordinate-owner change; 24 tasks execute successfully.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` succeeds with all 39 build tasks executed for `0.1.319` / versionCode `329`.

### Deferred/Failed
- **Failed (0.1.318 hypothesis):** Device testing confirmed that changing each digit from painted-ink X ownership to a fixed advance-cell X owner did not remove the probabilistic whole-clock leftward drift; that theory must not be reused as the root cause.
- **Deferred (device verification):** The host-local coordinate fix still requires repeated small-to-large testing on the phone. A successful build proves compilation only; the user-visible drift is not considered fixed until device validation confirms it.

## [0.1.318] - 2026-08-11
### Meta
- **Model:** GPT-5.6 Sol
- **Scope:** Replace the residual painted-ink clock positioning with COUI-style stable per-digit slot geometry after the 0.1.317 device test.

### Fixed
- Mirror the stock OPlus clock's horizontal geometry ownership instead of continuing to correct individual malformed frames. Runtime LSPosed hierarchy shows the native big clock as `BigClockDigitalTimeView` with separate hour/minute `DigitalTimeView` children, while SystemUI's `ClockPlugin` delegates rendering to `com.oplus.keyguard.personality.clocks`. The transition overlay now treats each digit's fixed advance cell as that stable slot.
- Stop recomputing each moving digit's X pivot and capture centre from variable-font `getTextBounds()` ink on every weight frame. The module's live clock already renders every character through `FixedAdvanceSpan`, which centres changing font advance inside a stable reference cell; source/target capture and overlay placement now use the same cell centre. Narrow glyphs such as `1` can change ink shape without dragging the interpolated digit path left.
- Keep the proven 0.1.317 redirected easing and target-layout readiness gate, and keep the 0.1.316 contextual forecast composed-alpha fix unchanged.

### Success
- **Success (new device LSPosed export):** `LSPosed_20260811_230515.zip` shows the residual drift occurring on direct compact-to-large transactions even when the 0.1.317 geometry-readiness gate never defers, excluding stale compact measured width as the remaining cause.
- **Success (COUI reference):** The saved OOS SystemUI dex confirms `ClockPlugin.render()` delegates to the separate personality-clocks renderer; runtime LSPosed view dumps expose stable per-digit `DigitalTimeView` children rather than one clock whose animation origin is repeatedly derived from changing painted bounds.
- **Success (targeted JVM tests):** `CouiClockSizeTransitionMathTest` and `CouiClockSizeTransitionLayerTest` pass, including a new regression proving two different variable-font ink bounds cannot move the fixed digit-cell owner.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` succeeds with all 39 build tasks executed. APK badging and packaged Xposed metadata both report `0.1.318` / versionCode `328`.

### Deferred/Failed
- **Deferred (device verification):** Repeated compact-to-large transitions on the phone are still required to prove the left-drift artifact is eliminated in rendered SystemUI frames.

## [0.1.317] - 2026-08-11
### Meta
- **Model:** GPT-5.6 Sol
- **Scope:** Remove the residual compact-to-large left-side clock drift and normalize redirected large/small transition speed after the 0.1.316 device test.

### Fixed
- Do not capture a size-transition target merely because a pre-draw callback fired. A ClockPlugin render can mutate the large clock to two-line/MATCH_PARENT while Android still retains the previous compact measured width for that traversal; that mixed snapshot places otherwise-correct large glyphs around the old left-side compact box. The prepared source overlay now keeps ownership while target geometry is checked, defers across animation frames when layout is still requested or physically inconsistent, and starts the 550 ms transaction only after the requested compact/large line mode and measured width have settled.
- Re-ease every in-flight size redirection from its exact current visual frame toward the newly requested endpoint. The 0.1.316 implementation reversed the linear driver but evaluated the original COUI ease-out curve backwards, so a reversal could begin in the curve's near-flat tail and look abnormally slow. Redirected legs now get a fresh ease-out segment without cancelling or replacing the active overlay.
- Retain the 0.1.316 composed-alpha handling for Weather Forecast text/icons; the device report confirms that brightness fix is effective.

### Success
- **Success (device LSPosed export):** The supplied `LSPosed_20260811_224340.zip` contains repeated 0.1.316 redirected transitions around 22:41 where a running large/small transaction reverses in place, matching the reported slow-but-stable path, alongside direct approximately-550 ms compact-to-large transactions matching the path that can still expose the left-side malformed frame.
- **Success (targeted JVM tests):** `CouiClockSizeTransitionLayerTest` and `CouiClockSizeTransitionMathTest` pass with new coverage rejecting a two-line large clock that still owns compact physical width, waiting for requested layout completion, and preserving current-frame continuity while applying a fresh easing segment on reversal.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` succeeds with all 39 build tasks executed. The APK and packaged Xposed metadata both report `0.1.317` / versionCode `327`.

### Deferred/Failed
- **Deferred (device verification):** The new target-geometry gate and redirected easing still require repeated rapid small↔large testing on the phone; compilation and JVM tests cannot prove rendered SystemUI frames.

## [0.1.316] - 2026-08-11
### Meta
- **Model:** GPT-5.6 Sol
- **Scope:** Stabilize rapid lockscreen large/small clock reversals and preserve contextual weather-forecast opacity during the COUI size transition.

### Fixed
- Keep the existing per-glyph overlay as the visual owner when OOS reverses the requested clock size before the 550 ms transaction finishes. Reverse the active `ValueAnimator` in place instead of cancelling it, exposing a fully-mutated live endpoint, recapturing that endpoint, and starting a second animation.
- Preserve the contextual forecast row's real composed opacity in transition snapshots. Forecast text/icon children are intentionally rendered at `0.72` alpha while their parent row remains at `1.0`; the transition now captures parent × child alpha instead of temporarily rendering the clone at full opacity.

### Success
- **Success (recording + persistent LSPosed log):** `screenshots/lock-to-aod.mp4` aligns with repeated large→small transactions that are cancelled mid-flight and immediately restarted small→large; the 21:34:58.564→21:34:58.926→21:34:58.953 sequence reproduces the problematic ownership break exactly.
- **Success (targeted JVM tests):** `CouiClockSizeTransitionLayerTest` and `CouiClockSizeTransitionMathTest` pass, including new coverage for active-path reversal policy and composed contextual alpha.
- **Success (debug build):** `:app:assembleDebug --rerun-tasks` succeeds; the APK reports version `0.1.316` / versionCode `326`, and the packaged Xposed metadata matches.

### Deferred/Failed
- **Deferred (device verification):** The phone disconnected before the rebuilt APK could be installed. Repeated rapid small↔large switching is still required before the visual drift and forecast-brightness issues can be considered fixed.
- **Deferred (pre-existing selector tests):** Adding `ContextualAtAGlanceSelectorTest` to the targeted run still exposes two existing deadline-policy failures (`schedulesAlertEndBeforeTheTenMinuteDisplayDeadline` and `schedulesSourceFreshnessExpiryBeforeTheTenMinuteDisplayDeadline`); the transition-only test set passes and this change does not touch that deadline policy.

## [0.1.315] - 2026-08-11
### Meta
- **Model:** Codex
- **Scope:** Correct the failed non-lockscreen AOD flash mitigation on OOS 16.0.9.

### Fixed
- Do not convert the persistent ClockPlugin's 120 ms native-Doze deadline into a visible AOD
  pre-presentation. If native Doze has not arrived, abandon only that early path and let the
  normal AOD host-ready flow present the module.
- Prevent a desktop/app-originated screen-off entry from using the lockscreen-only entry/grace
  draw window while the display is still `ON`; preserve the existing lockscreen-to-AOD handoff
  and explicit brief-trigger behavior.

### Success
- **Success (persistent LSPosed log):** The failed 0.1.314 trace showed both the early
  `shouldDrawPixelOverlay=true` decision and `ClockPlugin-pre-present` while
  `displayState=ON`; this release guards both proven paths.
- **Success (targeted JVM tests):** The new timeout regression and non-lockscreen visibility
  policy tests pass (6 cases total).

### Deferred/Failed
- **Failed (0.1.314 device QA):** The previous 120 ms fallback still presented the module while
  Android reported `displayState=ON`; the user correctly reported the flash as unchanged.
- **Deferred (device verification):** Repeated desktop/app screen-off testing is still required;
  a successful build cannot prove the visual flash is gone or that no unrelated AOD behavior
  regressed.

## [0.1.314] - 2026-08-11
### Meta
- **Model:** Codex
- **Scope:** Prevent the persistent ClockPlugin AOD layer from flashing over wallpaper during a
  desktop/app-to-AOD transition on OOS 16.0.9.

### Fixed
- Gate only the non-lockscreen ClockPlugin pre-presentation until the native display reports
  Doze; use a 120 ms safety fallback when OOS does not surface that state promptly.
- Cancel a pending pre-presentation if the user wakes before the display reaches AOD, and
  coalesce duplicate retries for the same AOD trace.
- Preserve the existing lockscreen-to-AOD animation path and avoid restoring the former 810 ms
  replacement delay.

### Success
- **Success (recording/log evidence):** The supplied 100.9 fps recording and persistent LSPosed
  log identify the immediate `ClockPlugin-pre-present` call as the wallpaper-overlaid Pixel AOD
  frame on non-lockscreen entry.
- **Success (targeted JVM tests):** 20 targeted tests passed: the new entry gate (3),
  ClockPlugin scene machine (11), and clock-typeface/handoff profile (6).
- **Success (debug build):** `:app:assembleDebug` completed and produced an APK reporting
  version `0.1.314` / versionCode `324`.

### Deferred/Failed
- **Deferred (device verification):** The new gate must be checked on the phone with repeated
  desktop/app screen-off transitions; build success cannot prove the one-frame flash is gone.

## [0.1.313] - 2026-08-11
### Meta
- **Model:** Codex
- **Scope:** Reduce SystemUI work caused by high-frequency notification callbacks while the
  Pixel AOD or Pixel lockscreen surface cannot actually draw.

### Fixed
- Cache posted and removed notifications immediately, but coalesce their presentation snapshot
  on the SystemUI main thread with a 500 ms cooldown.
- Skip AOD icon/media/layout rebuilding unless the AOD view is attached, visible, shown,
  drawable, and its lifecycle currently permits Pixel AOD rendering.
- Skip lockscreen presentation and host reapplication unless an attached lockscreen surface is
  actually visible.
- Exclude pure postTime updates from icon/media snapshot signatures while retaining keys,
  visibility, flags, category, small-icon identity, and media content changes.

### Success
- **Success (targeted JVM tests):** New tests pass for visibility gates, callback coalescing, and
  postTime-only signature stability.

### Deferred/Failed
- **Deferred (device verification):** Actual lock-from-home/app latency and idle drain still need
  verification on the connected phone; a build cannot prove them.
- **Deferred (unrelated full-suite failures):** The full Debug JVM suite currently reports three
  weather/alert-policy failures in BreezyWeatherForecastTest and
  ContextualAtAGlanceSelectorTest, outside this notification-path change.

## [0.1.312] - 2026-08-11
### Meta
- **Model:** Codex
- **Scope:** Pin At a Glance date, current-weather, and forecast glyph origins during the
  lockscreen-to-AOD variable-font weight handoff.

### Fixed
- Information rows retain their existing fixed advance cells, but no longer centre each changing
  glyph inside its cell. The clock keeps its established centred-cell behavior; only date/weather
  and forecast text use the fixed origin.

### Success
- **Success (recording evidence):** All five lockscreen-to-AOD segments in the supplied 112 fps
  recording show the previous date row settling roughly 0.72–0.76 px right of its lockscreen
  origin, with larger short-lived local-glyph deviations. The fix removes that centring offset at
  the text-drawing seam.
- **Success (test/build evidence):** All Debug JVM tests passed, including the new fixed-origin
  regression case; `:app:assembleDebug` completed successfully through the process-local system
  proxy. The APK reports `0.1.312` / versionCode `322`.

### Deferred/Failed
- **Deferred (device test):** The new glyph-origin behavior must be verified on device; a build
  cannot prove that the perceived wobble is gone or that the intended weight transition remains
  visually smooth.

## [0.1.311] - 2026-08-10
### Meta
- **Model:** Codex
- **Scope:** Make the optional Weather Forecast card's local display window configurable, with
  the approved 21:00–23:30 default, while auditing the reported lockscreen-to-AOD text motion.

### Changed
- Add separate Weather Forecast start/end settings below the forecast toggle. The clock picker
  writes `HH:mm`, refreshes the Breezy relay, and accepts cross-midnight ranges.
- Move forecast time eligibility into a pure `ForecastDisplayWindow`: start is inclusive and end
  is exclusive. Malformed or equal start/end values fall back atomically to `21:00–23:30`.
- Schedule an already visible lockscreen/AOD surface to re-evaluate at the nearest configured
  boundary and at local midnight for cross-midnight windows; this applies even while a
  higher-priority alert or calendar card owns the slot.
- Update the approved At a Glance policy and Chinese/English settings text for the configurable
  default window.

### Success
- **Success (source evidence):** The supplied `lock-to-aod.mp4` was sampled at its native
  approximately 112 fps. Enlarged `Mon, Aug 10` and `Tmr 26° / 22°` crops show no reproducible
  per-character spacing change; only a sub-pixel whole-row difference occurs during the wallpaper
  fade. No clock-animation change was made from this inconclusive evidence.
- **Success (JVM/build evidence):** `:app:testDebugUnitTest` passed with the new forecast-window
  boundary/eligibility coverage, and `:app:assembleDebug` completed through the system proxy.
  The resulting debug APK reports `0.1.311` / versionCode `321`.

### Deferred/Failed
- **Deferred (device test):** The new user-selected forecast window and the unchanged
  lockscreen-to-AOD typography still require device observation. No APK was installed.

## [0.1.310] - 2026-08-09
### Meta
- **Model:** Terra; Codex (OOS 16 capsule lifecycle correction)
- **Scope:** Correct the device-proven compact-to-large handoff failure while retaining the
  enhanced SystemUI notification drawable and DEFAULT-silent notification policy changes.

### Fixed
- The 0.1.309 source-only-overlay ordering was incorrect: frame 705 of the 15:44:53 recording
  proved that `applyClockMode(target)` could draw one malformed target-sized live clock in compact
  coordinates before `OnPreDraw`. A primed source snapshot now becomes drawable and hides the
  live source inside `prepare()`, before any target mutation. The source overlay remains visible
  while the hidden target lays out and through frame zero of the existing 550 ms animation.
- Coalesce an equivalent lockscreen target while a prepared/running size transaction owns the
  frame, avoiding cancellation/re-entry that could restore a live intermediate layout.
- The previously targeted `com.oplus.systemui.keyguard.notificationcapsule.*` classes do not exist
  in the tested OOS 16.0.9 `SystemUI.apk`. The active path is now the device-proven
  `notification.lockscreen.notification.CapsuleNotificationCardView.bind(...)` card binding,
  which updates its `CachingIconView` directly.
- Capture an isolated final `StatusBarIconView` drawable at its next pre-draw boundary and verify
  the current `getNotification().getKey()` again before caching. A same-key capture generation,
  recycled-view generation, and removal generation reject stale callbacks.
- OPlus capsule icons now use a key-matched clone on the currently bound `CachingIconView`.
  A late cache hit coalesces direct, data-generation-checked live icon updates; it never clears or
  replays OOS's notification list. Binding ownership is indexed by both notification key and weak
  live view identity, so a recycled icon view cannot receive another key's queued update. A failed
  clone preserves the vendor drawable.
- Deferred StatusBar icon capture tokens now hold their view weakly, preventing the weak capture
  index from retaining recycled SystemUI views. The renamed low-importance lockscreen-hide debug
  prefix remains in the existing 100 ms hot-log throttle.
- Treat `FLAG_SILENT` independently from importance. DEFAULT (3) private notifications remain
  eligible for lockscreen/AOD policy override, while LOW (2) and lower remain blocked; secret,
  transport, android/SystemUI, module non-test, and missing-icon exclusions remain in force.
- Final enhanced icon capture is generation-bound to its `StatusBarIconView` and invalidated on
  removal. A late capture after one capsule cache miss coalesces one ordered UI-thread replay;
  the final ImageView is rasterized so instance-specific drawable state is retained. Production
  lockscreen override exclusion now also shares the tested media-session/media-icon policy.

### Success
- **Success (code/unit/build evidence):** JVM tests cover source-frame ownership, two-view
  same-key reverse capture completion, removal invalidation, stale capsule data generations,
  coalesced direct late updates, and media exclusion parity; the debug build completed.

### Deferred/Failed
- **Deferred (device test):** Compact-to-large clock transitions can still occasionally expose
  one malformed intermediate frame with the large clock rendered at an incorrect left-side
  coordinate before the normal motion begins. The supplied device screenshot confirms this is
  not resolved; it needs a separate frame-by-frame diagnosis and must not be presented as fixed.
- **Failed (0.1.309 device test):** The first source-only-overlay attempt left the source live
  during `applyClockMode(target)`. Recording frame 705 visibly clipped the target-sized clock at
  compact coordinates before the intended animation began.
- **Deferred:** Runtime proof for this corrected frame ordering, the enhanced capsule icon, and
  Weekly scrobble visibility requires user testing after a SystemUI restart. JVM tests and a build
  cannot prove rendered device behavior.
- **Failed (previous capsule implementation):** The inferred legacy
  `keyguard.notificationcapsule.*` hook path is absent from the tested device's SystemUI, so it
  could not alter the live OOS 16 notification capsule.

## [0.1.307] - 2026-08-09
### Meta
- **Model:** Codex
- **Scope:** Fix the remaining current-weather hand-off drift and one-frame weather flash,
  respect OOS notification-capsule clock sizing, and reduce SystemUI work during launcher/app
  screen-off without changing AOD power or fingerprint policy.

### Fixed
- Mirror `TextView`'s vertical-offset clamp in the size-transition temperature clone. A compact
  clone whose font line is taller than its box is now top-pinned instead of incorrectly centred,
  removing the measured 9–11 px live-view hand-off correction.
- Configure, exactly measure, and lay out every transition clone before exposing the overlay.
  This prevents `FrameLayout`'s temporary `MATCH_PARENT` child size from stretching the weather
  icon across the right side for one frame.
- Treat OOS `clockSizeState` as authoritative on the lock screen. Active notification state is
  now only a fallback when OOS has not supplied a size, so a card collapsed into the bottom
  capsule can return to the large clock.
- Bound debug-log throughput, build the largest AOD/FOD policy diagnostics only after their log
  gate admits them, and avoid duplicate Android/Xposed writes when the framework logger is
  attached. Existing controller-level stable-scene presentation gating remains the recovery
  owner; no host visibility refresh is skipped by a second policy.
- Keep late AOD media retries media-only instead of repeatedly refreshing date, weather, and
  notification surfaces. No-media retries skip UI work but still clear media dedupe signatures,
  preserving same-session metadata and resume recovery.

### Success
- The supplied recording is a deterministic red-capable fixture: all 8 clock-size transitions
  showed an approximately 11 px temperature offset, and 4 transitions contained a one-frame
  weather icon roughly 22.5 times its normal area. Both defects map directly to the corrected
  baseline and first-frame measurement paths.
- LSPosed logs measured 539 module lines / 596.1 KiB in the 2.1-second screen-off window, with
  453 lines on the SystemUI main thread; the new keyed lazy gate covers the dominant policy,
  schedule, FOD carrier, notification-filter, and clock-paint categories.
- The forced full JVM suite passed **167 tests / 34 suites / 0 failures**, including transition
  math/layer structure, notification-capsule sizing, debug-log gating, ClockPlugin
  presentation/validation, weather policy, fingerprint policy, and media policies.

### Deferred/Failed
- **Deferred:** Device-frame proof for zero weather drift/flash and user-perceived launcher/app
  screen-off latency requires a newly recorded run of this build. Compilation and old-log/video
  analysis cannot prove the new SystemUI runtime result.
- **Deferred:** Late MediaSession publication remains device-tested behavior; the unit suite
  verifies related policies but cannot emulate OOS MediaSession callback timing.

## [0.1.306] - 2026-08-08
### Meta
- **Model:** Codex
- **Scope:** Stabilize the residual current-weather temperature transition and remove the
  redundant AOD-entry work identified from the connected OnePlus 12 LSPosed logs.

### Fixed
- Keep the current-weather `FixedAdvanceSpan` corridor at its source size through a size
  transition, then scale that text-only track around its painted centre. This prevents the
  `3` / `1` / degree symbol from being re-rounded into different cells on intermediate frames;
  the weather icon remains on its independent native-size track.
- Do not submit unchanged date, weather, contextual, notification, or media layout parameters.
- Do not clear an already-empty media row during late MediaSession polling. Retry only media
  discovery after AOD entry, rather than repeatedly refreshing the complete clock/info surface.

### Success
- Added JVM coverage for the stable fixed-cell scale path.
- LSPosed evidence showed the old path submitting the same AOD info-stack geometry 3–4 times
  within 6 ms around a clock transition; this build removes those no-op layout commits.

### Deferred/Failed
- Visual proof for the right-side flash and temperature glyph stability remains Deferred until
  device verification. Build and unit tests cannot prove a recorded SystemUI animation is clean.

## [0.1.305] - 2026-08-08
### Meta
- **Model:** Codex
- **Scope:** Fix the current-weather temperature-glyph drift introduced while separating the
  0.1.304 weather icon and text tracks.

### Fixed
- Keep the source `FixedAdvanceSpan` cells for the current-weather text through the whole
  COUI-size animation.  The `3`, `1`, and degree symbol can no longer change internal position
  when the clone is prepared with target-layout cells.
- Keep the 0.1.304 independent weather-icon track unchanged; date, forecast, and icon geometry
  are intentionally outside this focused correction.

### Success
- Added a JVM regression test that rejects target-cell replacement for an unchanged temperature
  such as `31°` during a size transaction.

### Deferred/Failed
- Device-frame verification remains Deferred pending user testing.  Unit tests and deployment
  cannot prove visual stability of the temperature glyphs.

## [0.1.304] - 2026-08-08
### Meta
- **Model:** Codex
- **Scope:** Separate text and weather-icon geometry in the COUI-style clock-size transition,
  after 0.1.303 fixed icon drift but reintroduced date/current-weather text drift.

### Fixed
- Render date, current-weather, and contextual forecast text through centred text-only tracks.
  Their screen positions now derive only from their fixed character corridors, never from a
  compound drawable or the spare width of a transition box.
- Render current-weather and contextual forecast icons through independent `ImageView` tracks
  captured at their real screen centres.  Icon geometry no longer changes the text origin, while
  the icon continues to interpolate directly to its live target.
- Set compact date and current-weather text to the requested **16 dp**.  Increase the compact
  date-to-weather anchor by 3 dp so its original 43 dp vertical envelope remains intact.

### Success
- Added JVM regression coverage requiring dedicated icon transition tracks rather than a single
  compound-drawable information clone.

### Deferred/Failed
- Device-frame verification is Deferred pending user testing.  Build, installation, and unit
  tests cannot prove that the rendered final-frame hand-off is visually stable.

## [0.1.303] - 2026-08-08
### Meta
- **Model:** Codex
- **Scope:** Fix the final-frame current-weather and Weather Forecast icon hand-off during the
  COUI-style large/small clock transition; slightly refine compact information typography.

### Fixed
- Preserve the real host information row's horizontal gravity and text alignment in the
  transition clone. A widened, centred clone placed the leading weather/forecast icon left of
  its real row while leaving the text in place, so the icon visibly jumped on restoration.
- Calculate the clone's painted union from that preserved gravity, so date, current weather, and
  contextual Weather Forecast land at the same geometry as their live targets.
- Reduce compact date/current-weather type from 20 dp to 19 dp. Increase the interline anchor by
  1 dp so the date top, weather bottom, contextual-row anchor, and the small clock alignment stay
  stable.

### Success
- The recorded small-to-large transition is red-capable: frame analysis measured the previous
  weather-icon-only 26 px final-frame hand-off while the adjacent temperature text stayed fixed.
- Added a JVM regression test for START/CENTER/END information-clone gravity offsets.

### Deferred/Failed
- Device-frame verification is Deferred pending user testing. JVM tests, a debug build, and
  installation prove compilation and deployment only; they cannot prove the rendered animation.

## [0.1.302] - 2026-08-07
### Meta
- **Model:** Codex
- **Scope:** Fix residual current-weather and Weather Forecast drift during the COUI-style
  lock-screen large/small clock transition, and preserve icon-pack artwork.

### Fixed
- Render the contextual Weather Forecast through the isolated transition clone for the entire
  transaction, even when both endpoints use its fixed auxiliary text size. This gives its icon,
  text, and vertical position one geometry owner rather than allowing a later host layout pass
  to move the live row upward.
- Keep the current-weather information clone on the same fixed-size-drawable transition path,
  and retarget its fixed character cells to the receiving layout before animation. The leading
  icon and weather text therefore land together without a final horizontal rounding correction.
- Remove module tinting from external current-weather icon packs; their default multicolour
  artwork is now retained while date/current-weather text keeps the requested emphasis colour.

### Success
- Added a regression test that rejects the former live-contextual-row fast path even when its
  source and target text sizes match.
- Full JVM suite passed: 156 tests, 0 failures; debug APK assembled as `0.1.302` /
  versionCode `312`.

### Deferred/Failed
- Device-frame verification of the two clock-size directions is Deferred pending user testing;
  a unit test, build, installation, and SystemUI restart cannot prove the rendered animation.

## [0.1.301] - 2026-08-07
### Meta
- **Model:** Codex
- **Scope:** Stabilize Weather Forecast and current-weather icon geometry during the COUI-style
  large/small clock transition, and refine AOD information emphasis.

### Fixed
- Keep a Weather Forecast's real `ImageView + TextView` row alive when both transition endpoints
  use the same auxiliary text size; translate its visible centre as one unit rather than replacing
  it with a synthetic compound drawable.
- Do not interpolate equal-size current-weather drawable bounds during a text-size transition,
  preventing a fixed 15 dp icon from changing its internal origin.
- Add AOD-only forecast weight compensation and apply the clock emphasis colour to date/current
  weather text and its icon.

### Success
- Added a JVM regression test covering the stable-geometry guard for native forecast subviews.
- Full JVM suite passed: 156 tests, 0 failures; debug APK assembled and its package metadata
  confirms `0.1.301` / versionCode `311`.

### Deferred/Failed
- Device-frame verification of the forecast row, forecast icon, and current-weather icon is
  Deferred pending installation and user observation; passing JVM tests and a debug build cannot
  prove rendered SystemUI animation frames.

## [0.1.300] - 2026-08-06
### Meta
- **Model:** Codex
- **Scope:** Fix span-aware geometry for the COUI-style large/small transition information rows.

### Fixed
- Scale each fixed date/weather/forecast text cell with the current animated text size, preventing
  letters from sliding inside a cell and then changing spacing when the real target view takes over.
- Measure date, current-weather, and forecast text using their active replacement-span advances,
  instead of raw `Paint.measureText()` widths that ignore the fixed cells.
- Capture the forecast row from its actual layout-line geometry plus its separate leading icon, so
  the temporary compound-drawable clone follows the same visible group centre.

### Success
- Added JVM coverage for fixed cell scaling during text-size animation.

### Deferred/Failed
- Device-frame verification of date/current-weather/forecast icon continuity is Deferred pending
  installation and user observation; a successful build cannot prove rendered SystemUI frames.

## [0.1.299] - 2026-08-06
### Meta
- **Model:** Codex
- **Scope:** COUI-style large/small clock transition, weather forecast geometry, and English forecast label.

### Fixed
- Include the contextual At a Glance row in the same temporary transition surface as the clock,
  date, and current weather, so it no longer jumps into its target position and overlaps them at
  the beginning of a large/small clock switch.
- Animate date/current-weather text by text metrics and drawable bounds at scale `1`; this keeps
  the fixed-size weather icon from shrinking during the transition and rebounding at the endpoint.
- Position temporary information clones by their painted text-and-icon centre.
- Keep Weather Forecast at its compact auxiliary geometry in both clock sizes to prevent the
  `Tomorrow`/`Tmr` text from reflowing or changing apparent letter spacing mid-transition.
- Tighten the compact date-to-weather anchor and contextual gap to move the forecast row up from
  the system notification card; English `Tomorrow` is now the shorter `Tmr`.

### Success
- Added focused JVM regression coverage for direct information-metric interpolation and
  fixed-size weather drawable bounds; the complete JVM suite passed (154 tests, 0 failures).
- Debug APK assembled successfully as `0.1.299` / versionCode `309`.

### Deferred/Failed
- Device-frame verification of all large-to-small and small-to-large paths is Deferred pending
  installation and user observation; a successful build cannot prove SystemUI rendering.

## [0.1.298] - 2026-08-06
### Meta
- **Model:** Codex
- **Scope:** COUI large/small clock transition endpoints and shared lockscreen/AOD information-group layout.

### Fixed
- Position each temporary clock clone from its painted glyph center after its current variable-font
  weight has been applied; asymmetric digits such as `1` no longer use the oversized clone box as
  their animation centre.
- Hold the last transition overlay frame until the real target clock/date/weather views have
  applied final weight and passed one pre-draw, preventing the endpoint snap of the first digit
  and weather compound icon.
- Add `ClockInfoGroupLayout` for both surfaces: in large mode current weather attaches after the
  date with a 6 dp gap and aligned visual centre; contextual rows begin after the actual group
  bottom while retaining the COUI minimum anchor. Compact date/weather anchors remain unchanged.

### Success
- Focused JVM regression tests passed for painted-content centre math, weather-leading content,
  end-position calculations, and identical lockscreen/AOD information-group results.
- `0.1.298` / versionCode `308` was cover-installed on `192.168.137.28:5555`; System UI
  restarted and the persistent LSPosed log confirms the Pixel AOD host is actively rendering.

### Deferred/Failed
- Device visual verification of continuous large-to-small and small-to-large frames remains
  Deferred until the built APK is installed and observed on the target phone; JVM and build
  checks do not establish rendered-frame behavior.

## [0.1.297] - 2026-08-06
### Meta
- **Model:** Codex / Luna
- **Scope:** Restore visible same-surface LARGE ↔ SMALL COUI clock/date/weather transitions.

### Fixed
- Reverted the 0.1.296 custom `GlyphTextView`/`InfoRowView` overlay path to the last
  device-visible ordinary `TextView` and compound-drawable clones.
- Kept only the narrow painted-ink glyph-center correction for the original upper-row trajectory;
  weather remains one compound `TextView` clone.
- Added a compiled-renderer structure regression test locking the transition layer to platform
  `TextView`/compound-drawable clones and excluding the 0.1.296 custom nested renderers.

### Success
- Focused `CouiClockSizeTransitionMathTest` passed after the correction; reflection verifies the
  compiled transition layer uses platform `TextView` clone types and has no custom nested renderer.
- Full `:app:testDebugUnitTest --rerun-tasks` passed: 148 tests, 0 failures, 0 errors, 0 skipped.
- `:app:assembleDebug --rerun-tasks` passed and produced `app-debug.apk`.
- APK metadata inspection passed: versionCode 307, version 0.1.297, Vector 101/101,
  `staticScope=false`, all `META-INF/xposed` entries present, and no packaged
  `io/github/libxposed` implementation classes.

### Deferred/Failed
- **Failed:** 0.1.296's custom overlay produced a blank intermediate transaction for approximately
  the full 550 ms before the persistent target snapped in.
- Device lockscreen pixel continuity and persistent-alpha restoration remain Deferred; JVM/build
  proof does not claim actual rendered frames or device runtime behavior.

## [0.1.296] - 2026-08-06
### Meta
- **Model:** Codex / Luna
- **Scope:** Lockscreen-only LARGE ↔ SMALL COUI size-transition geometry.

### Fixed
- Capture all four clock digits from their actual painted ink bounds, including the two-line
  large clock, instead of Layout line-box/reference-cell centers.
- Move the weather leading drawable and text as one explicitly centered visual group during the
  existing 550 ms transaction.
- Recenter the cloned information row on its actual painted text/drawable union after layout and
  immediately before drawing, including the FixedAdvanceSpan weight offset, so source and target
  endpoints equal their captured visual centers.

### Success
- Focused `CouiClockSizeTransitionMathTest` passed: 16 tests with exact painted-center,
  endpoint-union, per-glyph path, ReplacementSpan-offset, and rigid-weather-group regression cases.
- Full `:app:testDebugUnitTest --rerun-tasks` passed: 149 tests, 0 failures, 0 errors, 0 skipped.
- `:app:assembleDebug --rerun-tasks` passed and produced `app-debug.apk`.
- APK metadata inspection passed: versionCode 306, version 0.1.296, Vector 101/101,
  `staticScope=false`, and no packaged `io/github/libxposed` implementation classes.

### Deferred/Failed
- Device lockscreen visual verification is Deferred; no runtime behavior is claimed from JVM or
  build proof alone.

## [0.1.295] - 2026-08-05
### Meta
- **Model:** Codex / Luna
- **Scope:** Low-battery Pixel FOD styling and Breezy At a Glance runtime corrections.

### Fixed
- Separate fingerprint drawable replacement from native FOD carrier ownership: under an
  independent low-battery denial, an already-visible OOS carrier can use the configured Pixel
  visual without scheduling reclaim, showing a hidden carrier, reasserting AOD, or extending the
  native timeout.
- Make Weather Alert AOD-only. Lockscreen selection no longer shows the alert, starts its
  ten-minute window, or consumes the shared AOD repeat-entry marker.
- Parse Breezy alert validity from epoch seconds, epoch milliseconds, and compatible ISO-8601
  aliases while preserving an omitted end time instead of inventing one.
- Parse Breezy's actual provider schema (`refreshTime`, `daily[].day/night`, nested temperature
  values), use the daytime condition with day/night high-low values, and reject night-only or
  incomplete forecasts.
- Replace the filled warning triangle/fallback with a module-owned outlined triangle containing
  an exclamation mark.
- Document process-local Android SDK variables for every Gradle invocation in isolated Luna
  worktrees so they do not first fail due to a missing `local.properties`.

### Success
- **Device validation Success:** the earlier low-battery permanent fingerprint-icon regression
  remains fixed; device logs at 13% with `low_power=0` confirm the independent `low-battery`
  policy and native hide ownership.
- `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` passed: 144 tests, 0 failures, 0 errors.
- `.\gradlew.bat :app:assembleDebug --rerun-tasks` passed and produced the Debug APK.
- APK metadata inspection passed: versionCode 305, version 0.1.295, min/target API 101,
  staticScope=false, and no packaged `io/github/libxposed` implementation classes.

### Deferred/Failed
- The new low-battery Pixel-style native FOD replacement, AOD-only alert presentation, outlined
  icon, alert validity handling, and forecast card require device runtime/visual verification;
  these items are Deferred until user testing and are not claimed as runtime Success.

## [0.1.294] - 2026-08-05
### Meta
- **Model:** Codex / Luna
- **Scope:** Final At a Glance weather policy corrections for Breezy permission, relay failure
  preservation, alert deadlines, forecast icons, and shared contextual-card presentation.

### Changed
- Add the independent `weather_forecast` setting, Breezy current-position forecast relay data,
  deterministic alert history/selection, privacy redaction, and durable SystemUI-side state.
- Share Breezy permission acquisition between Weather Alerts and Weather Forecast without
  enabling either setting before grant; preserve SystemUI data on failed/malformed relay caches.
- Schedule alert end/source-freshness boundaries, reject unknown condition-text placeholders, and
  use the selected calendar icon resolver and geometry on both lockscreen and AOD.
- Route alert, calendar, and forecast presentation through one fixed one-line lockscreen/AOD slot
  with the module-owned weather warning resource and policy-boundary refreshes.
- Preserve the existing current-weather setting and backward-compatible relay/cache extras.

### Success
- Focused weather/alert/selector Debug JVM tests passed during implementation.
- `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` passed: 140 tests, 0 failures, 0 errors.
- `.\gradlew.bat :app:assembleDebug --rerun-tasks` passed and produced the Debug APK.
- APK metadata inspection passed: versionCode 304, version 0.1.294, min/target API 101,
  staticScope=false, and no packaged `io/github/libxposed` implementation classes.

### Deferred/Failed
- Device installation and runtime visual/policy verification are Deferred until primary/user
  testing; no device runtime success is claimed.

## [0.1.293] - 2026-08-05
### Meta
- **Model:** Codex / Luna
- **Scope:** False COUI large/compact transaction during unlock → launcher/app → screen-off AOD entry

### Fixed
- Reject a COUI per-glyph transaction when the captured rendered source already has the
  intended target compact state, preventing the observed false → false animation and its
  late weather/upper-clock-row displacement.
- Keep a defensive actual-source/actual-target equality check at transaction start while
  preserving real same-surface large ↔ compact animation and lockscreen ↔ AOD weight behavior.

### Success
- Persistent LSPosed evidence identified the false → false transaction as the cause;
- `CouiClockSizeTransitionMathTest` covers the scene-requested/actual-size mismatch and
  actual large ↔ compact transitions.
- `.\gradlew.bat :app:testDebugUnitTest --rerun-tasks` passed, and
  `.\gradlew.bat :app:assembleDebug` passed with `app-debug.apk` produced.
- **Device validation Success:** repeated unlock → launcher/app → screen-off entries now
  present the weather row and upper large-clock row directly at their final target positions;
  lockscreen ↔ AOD and genuine large ↔ compact transitions remain normal.

### Deferred/Failed
- The 0.1.292 visual-fix generation/reset patch was device-tested and Failed to remove this
  defect; it remains defense-in-depth, not the root-cause fix.

## [0.1.292] - 2026-08-05
### Meta
- **Model:** Codex / Luna
- **Scope:** Repeated large-clock AOD entry geometry after unlock/home → screen-off

### Fixed
- Invalidate stale AOD text morph callbacks when the persistent AOD surface is hidden or
  presented again, and reset the clock, date, and weather transforms before the new geometry is
  drawn.
- Preserve lockscreen↔AOD weight behavior and keep the COUI per-glyph transaction restricted to
  same-surface large↔compact changes; cross-surface handoffs retain their existing path.

### Success
- Full 872-frame video evidence identified the reproducible split at frames 803–805
  (`8.613989`–`8.630767` s) and the snap back at frame 806 (`8.638456` s); the stable baseline
  is frame 92 (`0.778656` s).
- Focused and full Debug unit tests pass, and the Debug APK build passes with synchronized
  package/version and Vector metadata.

### Deferred/Failed
- Device visual verification is Deferred until the user tests the built APK; the recording is
  pre-fix evidence and does not prove post-fix runtime behavior.

## [0.1.291] - 2026-08-05
### Meta
- **Model:** Codex / Luna
- **Scope:** OPlus power-policy and vendor FOD hide lifecycle

### Fixed
- Prevent non-interactive automatic low-battery or power-saver denial from reclaiming or
  refreshing the Pixel fingerprint carrier after OOS/native FOD hide callbacks.
- Recheck queued energy-saving AOD reassert passes at execution time so a policy transition
  cannot re-arm the AOD overlay after the native hide lifecycle has been allowed to run.
- Preserve allowed-AOD recent-overlay refreshes needed for proximity-far recovery, touch handling,
  native timeout ownership, and manual power-saver hide behavior.

### Success
- Focused and full Debug unit-test evidence passed for power denial, manual saver, native hide,
  allowed recent-overlay/proximity recovery, and queued reassert policy decisions.
- Debug APK build evidence passed with synchronized version metadata and Vector module metadata.

### Deferred/Failed
- Automatic low-battery device reproduction is Deferred because the connected device remains above
  the 20% threshold; device-runtime confirmation is also Deferred because this APK was not installed.

## [0.1.290] - 2026-08-04
### Meta
- **Model:** Codex / Luna
- **Scope:** COUI-style large/compact clock glyph transition

### Fixed
- Replace the whole-TextView large/compact scale with a temporary per-glyph transaction in the
  persistent ClockPlugin host. The four digits now move and scale toward their own target cells
  instead of growing or shrinking as one rectangular text block.
- Match COUI's 550 ms `PathInterpolator(0.2, 0, 0, 1)` motion and colon timing: the colon fades
  out at the start of compact-to-large and waits until 52% before entering large-to-compact.
- Move date and weather in the same transaction and read the live lockscreen/AOD clock and
  information weights while the glyph overlay is active.
- Scope the 550 ms glyph transaction to same-surface large↔compact changes. The prior
  compactness-only predicate incorrectly started it across `LOCKSCREEN_*↔AOD_*` handoffs, while
  the background and layer ownership were already changing.
- Preserve the existing lockscreen/AOD handoff and whole-view fallback behavior for cross-surface
  transitions; only the misplaced COUI overlay is suppressed there.

### Success
- Frame analysis ties the split to current frame 381 at `3.333978s` (still present at frame 389,
  `3.400267s`) while the COUI target remains a single composition at frames 385/389
  (`3.303s`/`3.336s`).
- Pure transition tests cover independent glyph targets, both colon timelines, same-surface
  large/compact changes, and cross-surface rejection; the focused debug unit-test task passes.

### Deferred/Failed
- Device visual confirmation is pending after installation. Build and unit-test success do not
  prove that OOS composition and screen fading make the transition visually identical to COUI.
- This animation revision remains uncommitted until the user approves the installed build.

## [0.1.289] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** Deduplicate the COUI-style native ClockPlugin draw interceptor

### Fixed
- Install `MyCustomizedFrameLayout#dispatchDraw` suppression once per actual container class and
  class loader. Reflection returns distinct `Method` objects on repeated lookup, so method-object
  identity cannot safely guard this per-frame hook.

### Success
- The 0.1.288 startup trace proved the intended OPlus targets are correct: both `ClockTimeView`
  and `DateMessageView` resolve to `MyCustomizedFrameLayout` parents and receive bindings.
- `:app:testDebugUnitTest` and `:app:assembleDebug` pass with class-level hook deduplication.

### Deferred/Failed
- Device runtime must show one hook installation and both visual bindings after the 0.1.289
  SystemUI restart. User visual confirmation of stock-AOD suppression and `+x` remains pending.

## [0.1.288] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** COUI-style native ClockPlugin draw suppression and notification overflow styling

### Fixed
- Match COUI's persistent-host replacement at the actual drawing boundary: bind OPlus time/date
  visual containers obtained from `ClockPlugin#getView(1/11)` and intercept
  `MyCustomizedFrameLayout#dispatchDraw` while the Pixel host owns the scene. Vendor alpha or
  visibility resets can no longer expose a native clock frame between lifecycle callbacks.
- Keep the draw interceptor policy-aware. It releases native rendering when the Pixel scene is
  hidden and removes bindings when the ClockPlugin host is unloaded.
- Let the AOD notification row and `+x` overflow text measure their real font height instead of
  clipping the text into the former 14 dp icon-height box.
- Use the exact same resolved AOD accent for notification icons and `+x`; date/weather remain
  neutral white as requested.
- Extend the lock-to-AOD analyzer to pair screen-off pre-presentation events and require runtime
  evidence that native draw suppression was both installed and bound.

### Success
- The 17:20-17:23 failure trace proves Pixel pre-presentation was already fast (13/13 events at
  1-10 ms) while the new draw-suppression invariant was absent (`hooks=0`, `bindings=0`).
- `:app:testDebugUnitTest` passes after the host-controller and overflow-layout changes.

### Deferred/Failed
- **Failed intermediate build:** device startup logs showed the draw hook being installed more
  than once because the first guard used reflection `Method` identity. This was caught before
  handoff and corrected in 0.1.289 to avoid per-frame interceptor overhead.
- Device visual confirmation is pending after installation. Build/tests and hook-binding logs do
  not by themselves prove that the stock-AOD flash or subjective lock delay is fixed.
- The existing remembered-`AodRootLayout` suppression remains only as a fallback; the persistent
  ClockPlugin draw interceptor is now the primary replacement path.

## [0.1.287] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 desktop/application screen-off stock-AOD flash and delayed Pixel AOD presentation

### Fixed
- At `WakefulnessLifecycle#dispatchStartedGoingToSleep`, immediately suppress the remembered
  native AOD host for a non-lockscreen screen-off, before Dream can expose the stock clock.
- Pre-present the final compact/large Pixel AOD scene from the persistent ClockPlugin host at the
  same screen-off boundary. The module no longer waits for OPlus to publish its delayed
  `ClockPlugin uiState=AOD`; that later callback resolves to the already committed scene.
- Keep the existing interactive-lockscreen handoff unchanged, including its clock-weight
  transition and OOS 16.0.9 stable single-layer behavior.

### Success
- The user-reproduced 17:02-17:03 logs produced 9/9 analyzer failures before this change:
  average `startToAnimInDream -> presentAod` latency was 601 ms and P95 was 682 ms. This is the
  red baseline for the exact desktop double-tap-lock symptom.
- Scene-machine regression tests cover both a hidden desktop host and OPlus' stale lockscreen
  scene, and confirm that the later vendor AOD callback does not re-commit the scene.

### Deferred/Failed
- **Failed:** user testing at 17:20-17:23 still showed the stock AOD from desktop screen-off.
  Pixel pre-presentation completed in 1-10 ms, but the module had no reliable binding to the
  native ClockPlugin drawing container; remembered `AodRootLayout` instances were absent or empty.
- Notification/provider refresh volume remains a separately measured performance concern; it is
  intentionally not changed in this version so the pre-presentation behavior can be isolated.

## [0.1.286] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 non-lockscreen AOD handoff latency and stock-AOD flash

### Fixed
- Match COUI's early screen-off origin tracking by hooking
  `WakefulnessLifecycle#dispatchStartedGoingToSleep`. Each sleep now starts a fresh AOD trace
  before Dream begins instead of reusing stale `screenOffAgeMs` and trace state.
- Remove the module's additional 810 ms non-lockscreen reveal block on OOS 16.0.9. Older OOS
  builds retain the existing delay; 16.0.9 enters the stable AOD presentation immediately and
  does not run the lockscreen weight morph for a desktop/application-origin sleep.
- Gate repeated OPlus `ClockPlugin#render()` callbacks by the final visible scene rather than
  transient vendor lifecycle fields. Explicit notification, weather, media, and policy refreshes
  remain forced.
- Add a reusable LSPosed log analyzer for `startToAnimInDream -> presentAod` latency and render
  volume.

### Success
- Existing 16:29-16:30 logs fail the new 150 ms analyzer on all 9 matched transitions
  (average 583 ms, P95 718 ms), proving that it detects the reported delay.
- Debug logging A/B measured 2138 ms disabled versus 2159 ms enabled from native Dream start to
  OOS AOD visibility; logging is noisy but is not the primary delay source.
- COUI reference logs measured about 2048 ms on the same native boundary, so this build does not
  alter panel, Doze OFF, or `requestScreenState` timing.
- Targeted presentation-gate and OOS handoff-profile unit tests pass.

### Deferred/Failed
- **Failed:** user testing at 17:02-17:03 still showed the stock AOD and obvious delay on repeated
  desktop double-tap locks. All 9 measured transitions failed the 150 ms analyzer threshold with
  601 ms average and 682 ms P95 latency. The render gate reduced work in the critical interval,
  but the persistent host still waited for OPlus' delayed `uiState=AOD` callback.

## [0.1.285] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 persistent ClockPlugin host lock-entry performance

### Fixed
- Stop an unchanged OPlus `ClockPlugin#render()` callback from re-presenting the entire
  replacement hierarchy. Actual scene, lifecycle, lock state, and AOD-entry changes still force
  an immediate presentation.
- Consequently, the AOD media-retry series is armed only for a real AOD entry instead of each
  redundant vendor render callback.

### Success
- The 15:53:57-15:54:07 persistent LSPosed trace recorded 361 `ClockPlugin#render` callbacks,
  212 information-stack layouts, and 90% janky SystemUI frames after a counter reset; this
  identifies the repeated unchanged presentation path targeted here.
- Added unit coverage for unchanged, changed, and forced ClockPlugin presentations.

### Deferred/Failed
- **Failed:** user testing at 16:29-16:30 found the same visual delay. The gate included changing
  OOS transient lifecycle fields, so 71 gate hits still allowed 858 renders and 542 information
  stack layouts in the captured window.

## [0.1.284] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 app-to-AOD stock-visual suppression and lock-entry performance

### Fixed
- Break the `ClockPlugin#render` refresh feedback loop: replacement content now uses one
  coalesced local redraw rather than repeatedly relaying `requestLayout()` and full-root
  invalidations back into OOS `performAodUpdate()`.
- OOS 16.0.9 does not dispatch `ACTION_SCREEN_OFF` to SystemUI during the affected path.
  Start the existing `0/160/620ms` stock-AOD suppression passes from native AOD-host readiness
  and `onDreamingStarted`, after the current AOD trace exists.

### Success
- Persistent LSPosed logs at 15:23 and 15:27 show no `SCREEN_OFF` module event and show
  249-301 explicit frame refreshes per short session, identifying both corrected paths.
- Added unit coverage for coalescing nested AOD frame-refresh requests.

### Deferred/Failed
- Device visual/performance confirmation is pending; do not treat build success as proof that
  the system AOD no longer flashes.

## [0.1.283] - 2026-08-04
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 proximity-return UDFPS recovery and FOD auto-hide regression

### Fixed
- Allow OOS's `showUdfpsOverlay()` callback after a proximity-near to proximity-far transition.
  This restores direct AOD fingerprint unlock instead of leaving authentication active with the
  optical FOD session hidden.
- Restore the native FOD-only timeout path so the fingerprint icon automatically hides again
  after its normal OOS timeout.

### Success
- LSPosed logs at 14:27 captured the former module suppression and confirmed the exact
  proximity-return callback that must be allowed.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed for this corrected build.

### Deferred/Failed
- The 0.1.281/0.1.282 attempt to preserve FOD during the native timeout caused the icon to
  remain visible indefinitely; that change has been removed.
- Final device visual verification remains pending after this corrected build is installed.

## [0.1.279] - 2026-08-03
### Changed
- Align the compact clock scene with the measured COUI Expressive anchors on OnePlus 12:
  clock centre at `25%` width plus `10dp`, date/weather centre at `75%` width minus `34dp`,
  and media at `32dp` from the leading edge and `25.5%` of the display height.
- Use the same compact clock and date geometry in the lockscreen layer, and calculate the
  handoff notification coordinate from the same date anchor, preserving the OOS 16.0.9
  lockscreen-to-AOD alignment fix.
- Keep media fixed at the COUI target; when media is visible, place notification icons below it
  and move an overlapping Calendar row below the media line.

### Verification
- Added deterministic OnePlus 12 canvas coverage for the COUI compact anchors.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed before the version bump.

## [0.1.278] - 2026-08-03
### Fixed
- On OOS 16.0.9 and later 16.0.x builds, leave ClockPlugin burn-in translation to SystemUI.
  The module's AOD layer now remains at the same coordinates as its lockscreen layer, avoiding
  the stale `(+x,+y)` module offset that caused visible jumps in both directions.
- Keep the OOS 16.0.9 direct single-layer AOD handoff and the rounded Google Sans Flex axis.

### Verification
- Added coverage for the OOS-specific burn-in ownership policy.
- Device logs on CPH2573_16.0.9.400 identified the old AOD offset as `(+4,+14 px)` while the
  lockscreen layer remained at `(0,0)`.

## [0.1.277] - 2026-08-03
### Meta
- **Model:** Codex
- **Scope:** OOS 16.0.9 lockscreen-to-AOD coordinate handoff and Google Sans Flex rounding

### Fixed
- On OOS 16.0.9 and later 16.0.x builds, commit the prepared AOD layer directly instead of
  crossfading independent lockscreen and AOD coordinate systems. The AOD weight transition is
  retained, while the clock and date no longer visibly travel between the two layouts.
- Match COUI Expressive's Google Sans Flex rounded terminal axis with `'ROND' 100`.

### Verification
- Added regression coverage for the OOS 16.0.9 build profile and the rounded font variation.
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed.
- Device visual confirmation remains required because this ROM rejects ADB screen recording.

## [0.1.276] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** Align notification row horizontal handoff offset

### Fixed
- Apply the AOD notification row's `-2dp` leading offset to the lockscreen handoff row only.
- Reset the offset when returning to the normal lockscreen row.
- Log the handoff row `translationX` for future diagnosis.

### Success
- Code and unit tests passed; the change is limited to notification row horizontal alignment.

### Deferred
- Device visual confirmation of the remaining screen-off icon movement is pending user testing.

## [0.1.275] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** Keep AOD and lockscreen notification icon order consistent during handoff

### Fixed
- Make notification snapshot signatures preserve input order, so order-only changes rebuild both layers together.
- Add final emitted icon-order diagnostics to AOD and lockscreen handoff rebuild logs.

### Success
- Static review identified the mismatch: the previous sorted signature let AOD keep the old order while the lockscreen handoff rendered the new order.
- The change is limited to notification snapshot invalidation and diagnostics; Doze, black-frame, clock weight, media, and fingerprint paths were not changed.

### Deferred
- Device visual confirmation of repeated screen-off transitions remains pending user testing.

## [0.1.274] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** Align notification icons during lockscreen-to-AOD handoff

### Fixed
- Use the AOD information-stack notification coordinate while the lockscreen layer temporarily renders AOD handoff icons.
- Remove the ineffective notification-row alpha/initial-visibility workaround from `0.1.273`.

### Success
- LSPosed logs confirmed the previous mismatch: lockscreen `198dp` versus compact AOD `188dp`.
- Doze, black-frame, clock weight, media, and fingerprint paths were not changed.

### Deferred
- Final visual confirmation of repeated screen-off transitions remains pending.

## [0.1.273] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** Prevent AOD notification icons from visibly jumping during layout handoff

### Fixed
- Keep the AOD notification row hidden until its first complete icon and information-stack layout pass.
- Suppress only the notification row while committing a new AOD stack position, so an old `topMargin` is not drawn before the final position.
- Add diagnostic logging for the committed notification and media row positions.

### Success
- `:app:testDebugUnitTest` passed with 24 actionable tasks.
- Doze, lockscreen/AOD handoff, black-frame, weight animation, media policy, and fingerprint code paths were not changed.

### Deferred
- Device visual confirmation of the screen-off notification-position jump is pending user testing.

## [0.1.272] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** Match COUI system dynamic color surfaces

### Fixed
- Remove the custom light-mode background blend that introduced an extra cyan tint.
- Use the unmodified Material 3 system `background` and `surfaceContainerLow` colors for the page and cards, matching COUI's dynamic color source.

### Success
- Settings UI remains behavior-only unchanged; no AOD, Doze, lockscreen, or fingerprint code was modified.

### Deferred
- Device visual comparison with COUI remains pending after installation.

## [0.1.271] - 2026-07-31
### Meta
- **Model:** Codex
- **Scope:** COUI-like module settings UI hierarchy

### Changed
- Move the module master switch into a dedicated, prominent card below the page header.
- Split settings into AOD Behavior, Clock Style, At a Glance, Lockscreen, and System & Diagnostics groups.
- Move schedule controls under AOD Behavior, calendar/weather controls under At a Glance, and language/debug controls under System & Diagnostics.
- Reduce the oversized page title so the primary control and first settings group remain visible sooner.
- Keep all existing preference keys, permissions, dialogs, and AOD runtime behavior unchanged.

### Success
- Settings UI source and localized section labels updated without changing AOD/Doze/lockscreen code paths.

### Deferred
- Device visual validation of the new settings page is pending user confirmation.

## [0.1.270] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Breezy Weather temporary severe-weather At a Glance row

### Changed
- Add an opt-in `Severe Weather Alerts` setting that requests Breezy Weather's `READ_PROVIDER` permission.
- Query Breezy's current-location provider for active alerts, retain only the highest-severity active alert, and relay its minimal headline/timing snapshot to System UI.
- Render the alert as a monochrome one-line At a Glance row between Date/Weather and the next calendar event; remove it automatically when it expires or is disabled.
- Derive compact media-row placement from the final notification position so Date, notification icons, media text, calendar events, and alert rows keep a continuous vertical rhythm.
- Recover a missing media `smallIcon` once for its current notification key without reopening the adaptive launcher-icon fallback.
- Align the media row with the notification-icon optical grid and remove its duplicate alpha layers so media text renders with the same visible weight as Date/Weather.

### Success
- Confirmed the installed Breezy Weather 6.2.1 provider exposes alert data through `withAlerts=true`; the existing Gadgetbridge payload does not include alert fields.
- User-confirmed the large-clock calendar/event/media stack no longer overlaps or jumps back during refreshes; notification, media, and At a Glance rows now retain a consistent vertical rhythm.
- Added unit coverage for AOD information-stack placement, Breezy alert selection, bounded missing-media-icon recovery, and the OPlus OTA icon policy.
- Map `com.oplus.ota` notifications to the bundled AOSP system-update glyph instead of rendering the OEM adaptive-icon white block.

### Deferred
- Device visual validation requires a live Breezy Weather alert. No artificial alert will be left enabled after installation.
- Pixel does not publicly document exact alert-card placement or TTL; this is a conservative Pixel-like module policy, not a claim of pixel-exact private behaviour.
- The OPlus OTA mapping will be visually rechecked when the next system-update notification arrives.

## [0.1.269] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar At a Glance vertical rhythm correction

### Fixed
- Replace the incorrect line-height-derived compact layout with screenshot-calibrated coordinates.
- Align the visual whitespace of Clock-to-Date, Date-to-Notifications, Date-to-Event, and Event-to-Notifications.

### Success
- Device screenshots identified the prior `0.1.268` discrepancy: `93px` Clock-to-Date versus `32px` Date-to-Notifications.
- User visually confirmed the corrected no-event AOD layout.

### Deferred
- User visual validation of the event-present four-row layout remains required.

## [0.1.268] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar At a Glance vertical rhythm

### Fixed
- Make the compact AOD notification-icon gap equal the existing small-clock-to-date gap.
- Use that same measured gap for Date/Weather, Calendar Event, and Notification Icons while an event is visible.

### Success
- Source layout now accounts for the selected calendar icon's rendered scale when placing the following notification row.

### Deferred
- User visual validation is required for both the no-event and calendar-event layouts.

## [0.1.267] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar At a Glance notification layout

### Fixed
- Restore the original date/weather-to-notification icon spacing whenever no calendar event row is visible.
- Keep the tighter calendar-present layout only while an event is displayed.

### Success
- Source layout restores the pre-calendar notification top position for both clock modes.

### Deferred
- User validation of the live event-expiry transition remains pending.

## [0.1.266] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar expiry notification layout

### Fixed
- Collapse the notification icon row into the vacated calendar-event line as soon as the event row hides, instead of leaving the previous date-to-notification gap.
- Log the calendar visibility transition together with the applied notification-row top position for device-side diagnosis.

### Success
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed.
- Debug APK `0.1.266` was overlay-installed and SystemUI restarted; agent screenshot verified that the notification row now sits directly below Date/Weather when no calendar event is visible.

### Deferred
- User validation of the live event-expiry transition remains pending.

## [0.1.265] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar At a Glance freshness

### Fixed
- Observe calendar and calendar-list changes in the module app process, then notify the SystemUI calendar client immediately.
- Return the visible event's next boundary from the provider and schedule one exact refresh for its start time; all-day events refresh at the next local midnight.
- Coalesce concurrent calendar changes and keep only one pending boundary refresh, without adding a repeating background timer.

### Success
- `:app:testDebugUnitTest` and `:app:assembleDebug` passed.
- Debug APK `0.1.265` was overlay-installed, SystemUI restarted, and an AOD screenshot confirmed that the calendar event row, icon, and notification row still render normally.
- Persistent LSPosed logs confirmed one boundary task scheduled for the active `20:30 Test` event, with a 545-second delay to its exact start.

### Deferred
- Full Smartspace targets, event click actions, multiple-card ranking, and ongoing-event presentation remain intentionally out of scope.
- User verification of editing or deleting a calendar event while AOD is visible remains pending; it should refresh without waiting for the next minute tick.

## [0.1.264] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar / notification optical grid

### Fixed
- Tune Calendar leading compensation from 8dp to 6dp, move the notification row 2dp left, and move the event row 3dp down using screenshot pixel measurements.

### Success
- Agent screenshot verified that 0.1.263 no longer clipped the calendar icon.

### Deferred
- Final user visual validation of the shared leading edge and equal Date-to-Event / Event-to-Notification spacing remains pending.

## [0.1.263] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar event-row optical leading edge

### Fixed
- Move the entire selected-calendar event row left by its adaptive-icon safe-zone compensation instead of translating the child icon outside the host bounds.
- Remove the notification-row translation so Date/Weather, Calendar, and Notifications retain one shared layout baseline.

### Success
- Agent screenshot verified that the 0.1.262 child-translation path was clipped by the host boundary; this revision replaces that path.

### Deferred
- Final device visual validation of the unclipped icon and row alignment remains pending.

## [0.1.262] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Calendar event-row visual alignment

### Fixed
- Allow the selected calendar application's monochrome icon to extend beyond its row bounds, preventing its leading edge from being clipped after optical alignment with Date/Weather.
- Split date-to-event and date-to-notification offsets so the three AOD information rows can be adjusted independently.

### Success
- Agent screenshot reproduced the clipped calendar icon and confirmed the parent-clipping root cause before this fix.

### Deferred
- Final device visual validation of leading-edge alignment and equal row spacing remains pending.

## [0.1.261] - 2026-07-30
### Meta
- **Model:** Codex
- **Scope:** Local calendar At a Glance

### Added
- Add an opt-in Calendar Events toggle that requests `READ_CALENDAR` only when enabled.
- Show the next timed event within 24 hours as `start time + title`; show one all-day event only on its day.
- Query Calendar in the module app process and return only the filtered display text to SystemUI, avoiding Calendar permission in the hook process and avoiding main-thread queries.
- Render Calendar as an independent At a Glance event row below Date/Weather, with consistent event-to-notification spacing; the weather icon remains attached to the Date/Weather line.
- Add an opt-in Calendar App Icon selector: all event rows can use the selected app's original Launcher icon, with a monochrome calendar fallback when no app is selected or available.

### Success
- Build and device validation pending.

### Deferred
- Multiple-event rotation, locations, notes, attendees, and network-backed At a Glance cards remain intentionally out of scope.

## [0.1.260] - 2026-07-28
### Meta
- **Model:** Grok (xAI)
- **Scope:** Date/info line weight 400 → 450

### Fixed
- User: 400 too thin on device. `INFO_WEIGHT` now **450** (between Regular and Medium). Clock digits unchanged. **Pending user visual check.**

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.259] - 2026-07-28
### Meta
- **Model:** Grok (xAI)
- **Scope:** Date/info line weight 500 → 400 (Keyguard.Secondary Regular)

### Fixed
- `INFO_WEIGHT=500` → **400**. **Failed** (user: too thin) → 0.1.260.

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.258] - 2026-07-28
### Meta
- **Model:** Grok (xAI)
- **Scope:** Fix LS→AOD instant weight using log-proven gate failure

### Fixed
- **Logs aod-2f-3df142c (0.1.257 failed):** After `aod-to-ls` finished at weight 301, `early-aod-direct-non-ls` ran with `lockscreenToAodWeight=false screenOffFromLs=false screenOffAgeMs=-1` and `applied stable 151` — no morph. Root: morph gate only trusted recent marks / noteScreenOff latch, but preparingAod ran before noteScreenOff and without fresh marks. Now arm a **lockscreen session stamp** on interactive presentLockscreen, markInteractive, and aod-to-ls end; clear on unlock hide. `shouldAnimate` = session stamp || LS screen-off latch || recent marks; still **false** when noteScreenOff latched non-LS (keeps unlock→app direct path). **Pending user visual check.**

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.257] - 2026-07-27
### Meta
- **Model:** Grok (xAI)
- **Scope:** Restore LS→AOD weight morph without reintroducing non-LS morph

### Fixed
- Surface-hide stamp wipe + noteScreenOff latch. **Failed** — logs still showed early-aod-direct-non-ls on real LS→AOD → 0.1.258.

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.256] - 2026-07-27
### Meta
- **Model:** Grok (xAI)
- **Scope:** Block early-aod-weight morph on non-lockscreen doze (real root cause)

### Fixed
- **Root cause (logs aod-c-6723d):** 0.1.255 only skipped morph in `presentAod`, but non-lockscreen screen-off still kept `hostScene=LOCKSCREEN_SMALL` and ran `early-aod-weight` → lockscreen-layer `ls-to-aod` 340→151 during the black reveal delay. Now: (1) `preparingAod` without recent interactive LS jumps straight to stable AOD; (2) non-interactive KEYGUARD present without recent LS skips lockscreen paint; (3) `beginClockPluginAodWeightTransition` refuses when not recent interactive LS. **Pending user visual check.**

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.255] - 2026-07-27
### Meta
- **Model:** Grok (xAI)
- **Scope:** Skip LS→AOD weight morph on non-lockscreen screen-off

### Fixed
- Unlock → launcher/app → screen-off was parking AOD at lockscreen weight (≈340) then playing the weight scale animation after the black reveal delay. Non-lockscreen entry now applies stable AOD weight immediately (no morph). **Failed** for user — morph still ran via `early-aod-weight` on LOCKSCREEN_SMALL → 0.1.256.

### Deferred
- SMALL/LARGE clock size morph still imperfect during weight handoff.

## [0.1.254] - 2026-07-26
### Meta
- **Model:** Codex
- **Scope:** Synchronize Claude's local AOD/UDFPS work and repair the LS-to-AOD weight-handoff race.

### Fixed
- **Success (user confirmed):** An entering `ls-to-aod` request can now replace a still-running `aod-to-ls` restore on the lockscreen layer. The weight morph starts while the lockscreen clock is still visible and hands off at its live intermediate weight, instead of being dropped and restarted only on the AOD layer after the reveal delay.
- Retain the bundled weighted `Typeface.Builder` path and prevent automatic size morph from taking over an in-progress LS-to-AOD weight handoff.

### Deferred / Failed
- **Failed:** 0.1.253's `setFontVariationSettings()` experiment did not solve the timing race and still left the visible morph on the AOD layer. It has been removed in favor of the previously working weighted-typeface path.
- The synchronized Pixel fingerprint animation carrier and OOS temporary-show handling are included but were not revalidated during this handoff test.
- SMALL/LARGE clock size morph remains intentionally deferred during a weight handoff.

## [0.1.253] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Make LS→AOD weight morph actually visible (wght axis + no size morph steal)

### Fixed
- User still saw AOD freeze at 340 then only scale: (1) weight updates now drive bundled variable font via `setFontVariationSettings` instead of swapping Typeface.Builder each step (invisible/thrashy on OOS); (2) skip compact→large **size** morph when weight handoff runs so scale no longer steals the transition. **Pending user visual check.**

### Deferred
- SMALL↔LARGE size morph still imperfect (explicitly deferred during weight handoff).

## [0.1.252] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Weight morph must run on-screen (not off-screen during grace)

### Fixed
- Start weight morph when AOD shown. **Failed** for user (still 340 then scale) → 0.1.253.

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.251] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** LS→AOD weight morph reliability + unlock→AOD twitch

### Fixed
- Dual early-aod morph races. **Partial** — settle/skip improved but morph still ran off-screen → 0.1.252.

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.250] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Unlock / screen-off jank after weight morph always runs

### Fixed
- Unlock/screen-off jank: weight quantize/prewarm, AnimCarrier, no global setAlpha. **Partial** — smoother but weight skip/twitch remained → 0.1.251.

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.249] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** LS→AOD weight morph probabilistic skip (340 hold then snap 160)

### Fixed
- Intermittent missing LS→AOD weight animation (340 hold then snap). **Success (user: weight OK)** but introduced unlock/screen-off jank → 0.1.250.

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.248] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Pixel ridge over OOS temp-show animation (black-frame / tap)

### Fixed
- Black-frame / tap temp-show: keep `OplusAnimationDrawable` carrier, draw-hook Pixel ridge. **Success (user: fingerprint OK).**

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.247] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Fix lockscreen HBM highlight + restore Pixel temp-show after 0.1.246 over-normalize

### Fixed
- Lockscreen fingerprint stuck fully highlighted: removed `normalizeLockscreenIconAlpha` / `setBrightnessAlpha(1)` / forced View alpha. OOS owns brightness alpha again. **Success (user: no longer highlighted).**
- Black-frame / tap: stopped canceling View animations. **Failed** — temp-show still missing when static Pixel replaced vendor anim (doze optical path). Superseded by 0.1.248.

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.246] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Pixel fingerprint only — no stock OOS glyph flash / temp-show frames

### Fixed
- Stock fingerprint flash on screen on/off and temp-show frames: always reclaim to Pixel. **Partial Success** — no stock glyph (user confirmed), but over-normalize caused permanent highlight + lost temp-show → fixed in 0.1.247.

### Deferred
- SMALL↔LARGE size morph still imperfect.
- Temporary show no longer plays stock frame fade-out; fade is whatever OOS does to View alpha after Pixel reclaim.

## [0.1.245] - 2026-07-26
### Meta
- **Model:** Grok (xAI)
- **Scope:** Pixel fingerprint size restore + temporary re-show after black frame / tap

### Fixed
- Fingerprint ridge size felt too small after 0.1.244 AOSP metrics: restore user-preferred `FOREGROUND_SCALE` **0.58** and lockscreen stroke **2.6×pathScale** (AOD stroke/dash unchanged). **Success (user: size OK).**
- Temporary FOD re-show after black-frame / tap restored by preserving `OplusAnimationDrawable`. **Partial Success** — visibility OK, but pulse was stock OOS style → replaced by 0.1.246 Pixel reclaim.

### Deferred
- SMALL↔LARGE size morph still imperfect.
- (Superseded by 0.1.246) Temporary re-show used native OOS animation frames.

## [0.1.244] - 2026-07-25
### Meta
- **Model:** Grok (xAI)
- **Scope:** Align UDFPS ridge metrics with AOSP/Pixel defaults

### Fixed
- Fingerprint icon geometry closer to AOSP `config_udfpsIcon` / COUI defaults: `FOREGROUND_SCALE` 0.58→**0.5**, lockscreen stroke **3×pathScale** (was 2.6dp×scale), AOD stroke 2 and dash 4/4.5 unchanged. Path data already matched AOSP. **User: a bit small → reverted size in 0.1.245.**

### Deferred
- SMALL↔LARGE size morph still imperfect.

## [0.1.243] - 2026-07-25
### Meta
- **Model:** Grok (xAI)
- **Scope:** Pixel lockscreen UDFPS color/style (screenshot 23:05)

### Fixed
- Lockscreen fingerprint looked like a dark charcoal filled disc on colorful wallpaper (user screenshot 23:05). Pixel/AOSP style is a light ridge glyph without a solid surface disc. Lockscreen foreground forced near-white; background disc opacity forced to 0 on lockscreen/AOD; slightly larger ridge scale. **Pending user visual check.**

### Deferred
- SMALL↔LARGE size morph still imperfect (carried from 0.1.242).

## [0.1.242] - 2026-07-25
### Meta
- **Model:** Grok (xAI)
- **Scope:** weight handoff, settings clamp, AOD notification parity, size morph (partial), media timing

### Fixed (verified / user-confirmed OK except size morph)
- Settings AOD weight 100 applied as 160: both `aodClockWeight()` min clamp and `normalizeClockWeight()` floor were 160 while the settings slider allows 100–500. Both now use 100–500 so typeface `wght` matches the setting. **Success.**
- LS→AOD weight morph invisible/snap with notifications: compact path ran 340→target on the lockscreen layer then `applyStableAodWeight` on the AOD layer at reveal. Weight morph now transfers to the AOD layer (park at current LS weight, animate on AOD) and crossfades immediately. **Success (user: other issues OK).**
- Media row lag on AOD entry: denser earlier media retries (0/16/48/100/200/320ms) and media refresh before crossfade. **Success.**
- AOD→LS weight snap after notifications/hotspot: animate restore when LS layer already at AOD weight; `restoreClockPluginLockscreenWeight` no longer hard-snaps. **Success.**
- Hotspot / lockscreen-visible system status missing on AOD: NETWORK_STATUS / Tethering / Wi-Fi sharing treated as system status; no importance filter for android/SystemUI. **Success.**
- Interactive LS wake canceling aod-to-ls / early-aod staging: skip early AOD while interactive; animate restore. **Success.**

### Deferred / still imperfect
- **Lockscreen/AOD SMALL↔LARGE size morph:** content-bounds + OnPreDraw improved geometry vs whole-layer scale, but user still reports visual flaws (start/end not fully matching true large/small rest positions). **Not fixed; left for later.** No full COUI multi-glyph path port.

## [0.1.241] - 2026-07-25
### Fixed
- AOD→lockscreen weight snap after hotspot/notifications enabled: with compact notifications the host uses early-aod-weight on the lockscreen layer (stays LOCKSCREEN at wght~160). Wake called `restoreClockPluginLockscreenWeight` / present without `fromAod`, snapping to 340. Animate restore when layer weight is already near AOD target; treat lockscreen-layer AOD weight as reverse-morph source even if host scene is still lockscreen.

## [0.1.240] - 2026-07-25
### Fixed
- AOD notification icons missing lockscreen-visible status rows (e.g. hotspot "1 device is connected via Wi-Fi sharing"): OOS shows channel `NETWORK_STATUS` / group `Tethering` at importance=2 on lockscreen, but AOD filtered them as `lockscreen-policy-ranking-importance-low-or-less` and the system-status whitelist only matched English "hotspot/tether". Treat NETWORK_STATUS/Tethering/Wi-Fi sharing as system status; do not importance-filter `android`/`SystemUI` (same exemption as lockscreen policy); keep rows explicitly marked visible by keyguard visibility hooks.

## [0.1.239] - 2026-07-25
### Fixed
- Size morph still wrong in user video v2: large clock uses MATCH_PARENT so view-center/view-size morph used full screen width and wrong pivot (digits oversized and off-target mid-anim). Now morph uses glyph content bounds (`Layout` line box) + `textSize` ratio, pivot at content center, and `OnPreDraw` so the first drawn frame already has the start transform (no post-frame flash).

## [0.1.238] - 2026-07-25
### Fixed
- Lockscreen/AOD size morph geometry (user video): no longer scale the whole host layer with a wrong pivot (clock flew off-screen mid-anim). Capture pre-change clock/date center+size, apply target layout, then animate only the TextView(s) with scale+translation from previous center to laid-out center (`PathInterpolator(0.2,0,0,1)`, 550ms). Compact→large AOD entry morph uses the same approach.

## [0.1.237] - 2026-07-25
### Fixed
- Lockscreen weight reverts to AOD weight after aod-to-ls anim (logs 20:55 continuous wake/sleep):
  - Never stage `early-aod-large` / early AOD weight while device is interactive.
  - When interactive, always accept lockscreen present (do not ignore because AOD weight is running).
  - If `aod-to-ls` weight anim is cancelled while still interactive on lockscreen, snap to lockscreen weight (340) instead of leaving AOD weight.
### Added
- COUI-like SMALL↔LARGE size morph on lockscreen and AOD (`PathInterpolator(0.2,0,0,1)`, 550ms) when clock size changes on an already-visible layer.

## [0.1.236] - 2026-07-24
### Fixed
- LS→AOD weight bounce hardening (log-verified path):
  - `applyStableAodClockWeight` now sets `aodWeightHandoffSettled=true` so stable 160 cannot be followed by `prepared fromWeight=340`.
  - Do not clear the settle latch / force `weightStart=340` while the AOD surface is already active and settled (only `presentLockscreen` clears for the next handoff).
  - `prepare` refuses to cancel a running weight morph just to re-park at lockscreen weight.
  - Expected LS→AOD log sequence: `prepared/started 340→160` → `finished toWeight=160 settled=true` → `kept settled ... weight=160` (no later `prepared fromWeight=340` until leave AOD).

## [0.1.235] - 2026-07-24
### Fixed
- LS→AOD weight bounce after finish (logs: `finished toWeight=160` then `prepared fromWeight=340` from `non-lockscreen-reveal+849`): latch `aodWeightHandoffSettled` when weight morph completes; re-present / reveal must not re-park at lockscreen weight; only a fresh handoff (or leaving AOD) clears the latch.

## [0.1.234] - 2026-07-24
### Fixed
- Lockscreen→AOD weight was snapping while AOD→lockscreen still animated: AOD layer kept a stale ~160 from the previous session, so re-present treated it as “already at AOD weight” and called `applyStableAodWeight` instead of parking at ~340 and animating down. Always park at lockscreen handoff start when it differs from AOD target; always start the 700ms LS→AOD weight transition; size morph no longer owns a second weight animator.

## [0.1.233] - 2026-07-24
### Fixed
- AOD weight 340→100 then snap back to 340: logs showed mid-entry OOS KEYGUARD frames calling `presentClockPluginLockscreen` (`aod-to-ls` 337→340) and cancelling the AOD weight animator, plus re-present resetting `fromWeight=337`. Ignore lockscreen presents while host is already AOD and non-interactive (or AOD weight anim running); do not restart/reset AOD weight handoff on re-present/media refresh.

## [0.1.232] - 2026-07-24
### Fixed
- Restore lockscreen↔AOD **font-weight handoff animation** broken by the large-AOD surface switch: AOD entry again starts at lockscreen weight and animates to AOD weight (~700ms) instead of snapping via `applyStableAodWeight`; AOD→lockscreen animates weight back from the AOD layer weight. Compact→large scale morph still runs in parallel when leaving lockscreen SMALL.

## [0.1.231] - 2026-07-24
### Fixed
- Lockscreen stuck compact after dismissing a paused media card: logs showed OOS `clockSizeState=1` (LARGE) while the module forced `LOCKSCREEN_SMALL` because `mediaActive` stayed true on a **paused** MediaSession. ClockPlugin lockscreen size now follows OOS `clockSizeState` (still force SMALL for real module notifications); `hasPlayingMediaLocally` no longer treats PAUSED as compact.

## [0.1.230] - 2026-07-24
### Fixed
- Media-only lockscreen→AOD handoff (COUI-inspired, no black-frame changes):
  - Stop early weight-only animation on lockscreen SMALL when there are no notifications; stage AOD LARGE instead.
  - Promote dozing KEYGUARD uiState to AOD for size/scene so the first decision is AOD_LARGE (not a later snap).
  - Active surface switches to AOD LARGE + media immediately; compact→large entry uses scale morph (~380ms, PathInterpolator 0.2/0/0/1) with media fade-in and weight morph in parallel.
  - Force AOD_SMALL→AOD_LARGE before present when module has no non-media notifications.

## [0.1.229] - 2026-07-24
### Fixed
- Pre-blank AOD frame with media-only content: when entering AOD as LARGE (no notifs), immediately switch the visible ClockPlugin surface from the lockscreen SMALL layer (native media already gone) to the prepared AOD layer with large clock + media row, and retry media fill at 0/48/120/280ms. Does not change the platform black-frame path.

## [0.1.228] - 2026-07-24
### Fixed
- **Lockscreen:** restore compact clock when a native OOS media card is present (large clock was covered by the media card). Media cards / playing-or-paused sessions force SMALL; real notification cards still do.
- **AOD:** keep LARGE when only media is active (module media row under the large clock); ClockPlugin size policy is split lockscreen-vs-AOD.
- **AOD media timing:** prepare media on the AOD layer at present; if media is ready, start the handoff crossfade without the 700ms weight-wait hold so the media row is not stuck invisible under an opaque lockscreen layer. Does not change the platform black-frame / power path.

## [0.1.227] - 2026-07-24
### Fixed
- Earlier incomplete media/compact experiment (superseded by 0.1.228).

## [0.1.226] - 2026-07-21
### Fixed
- Reassert the native OOS pressed fingerprint layer only while a real fingerprint touch is active, so its inherited View alpha updates cannot leave the optional Pixel lockscreen icon permanently highlighted.

### Diagnostics
- Record the pressed-layer dispatch route, handler, alpha before/after, and touch state under the `FP-PRESSED-A2` debug marker.

## [0.1.225] - 2026-07-20
### Fixed
- Split the Pixel lockscreen fingerprint background from the foreground icon, matching YAAP's independent surface layer so OOS image-alpha updates no longer turn the background translucent.
- Use a compact 56dp opaque dark/light surface fallback for OOS themes that resolve the private `colorSurface` attribute to the wrong contrast.

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
