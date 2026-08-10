# Changelog

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
