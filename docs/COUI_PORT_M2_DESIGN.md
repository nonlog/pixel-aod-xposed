# COUI Port M2 Design

Status: M1 physical acceptance passed; M2 Slice 1 contracts are complete. M2 Slice 2
host/ClockPlugin bridge/startup-exclusive code exists and is under supervisor correction/source
review. Slice 2 has no physical, device, or visual acceptance.

Slice 1 implements the pure presentation/geometry contracts and startup-only clock owner policy.
Slice 2 adds the unified `CouiClockHostView`, its optional ROM morph bridge, the
`CouiClockPluginHostController`, native visual suppression, and the one-owner startup router.
Retained business adapters remain data-only seams deferred to Slice 3.

## Inputs and Gate

Behavioral ground truth is the checked-in COUI 2.5.0 reference:

- `reference/coui-2.5.0-260802/systemui/PixelClockHostView.java`
- `reference/coui-2.5.0-260802/systemui/PixelLockscreenClockHook.java`

The legacy boundary reference is commit `a1f7e8dcee77db73b08f785319567b50f634ecd2`, version `0.1.331`. Legacy source is used only to locate data and lifecycle adapters. Its geometry and renderer ownership must not be copied into M2.

M1 physical acceptance is complete: the user confirmed a real enrolled-finger `TOUCH_DOWN` and
successful optical recognition/unlock, with fresh `pressed=true`, authentication-success, and
vendor HBM/highlight evidence. M2 implementation proceeds from this gate while preserving the
behavior contract below.

## Primary Ownership

There is exactly one primary clock owner per SystemUI startup:

- In `COUI_PORT`, one COUI clock host owns every primary clock glyph, date, weather, clock content, AOD content row, and clock animation.
- In `LEGACY`, the existing legacy owner remains intact and owns its existing views and updates.
- The two owners are mutually exclusive. They must never coexist by draw order, repeated refresh, or a hidden second host.
- Business adapters provide data and semantic state only. They never calculate primary clock coordinates, create a second clock, or decide which renderer is active.

The host is the only component allowed to turn scene and content state into geometry. A ClockPlugin bridge may observe lifecycle and state, but it does not become a second visual owner.

## Scene Model

The reference host has exactly three visual scenes:

| Scene | Meaning | LockScreen use |
| --- | --- | --- |
| `LARGE` | Full two-line clock composition | LS Large |
| `SMALL` | Compact horizontal clock composition | LS Small and partial AOD |
| `IMMERSED` | Compact clock with immersed information placement | LS Immersed |

AOD is a mode, not a fourth scene. The reference uses `dozing` and `partialAod` together with the scene:

| Surface | Reference state | Exact host interpretation |
| --- | --- | --- |
| LS Large | `dozing=false`, scene `LARGE` | Large lockscreen geometry and lockscreen font weight |
| LS Small | `dozing=false`, scene `SMALL` | Compact lockscreen geometry and lockscreen font weight |
| LS Immersed | `dozing=false`, scene `IMMERSED` | Compact immersed geometry and lockscreen font weight |
| AOD Large | `dozing=true`, `partialAod=false` | Panoramic AOD; large AOD glyph set |
| AOD Small | `dozing=true`, `partialAod=true` | Partial AOD; content normally selects the small visual scene |

The reference visual-scene rule is: a non-partial surface keeps the requested scene; a partial AOD with non-`None` content uses `SMALL`; a partial AOD with `None` keeps the requested scene. M2 must preserve this rule rather than inventing a sixth scene.

## Scene and Content Matrix

`AodContent` has exactly `None`, `Notifications`, and `Media`. `Media` carries title, artist, app icon state, and a notification-icon list. The following is the required COUI behavior:

| Content | LS Large | LS Small | LS Immersed | AOD Large | AOD Small |
| --- | --- | --- | --- | --- | --- |
| `None` | Large clock and information; no AOD content rows | Small clock and information; no AOD content rows | Immersed clock and information; no AOD content rows | Panoramic large AOD clock; no partial content rows | No content rows; visual scene follows the reference `None` exception |
| `Notifications` with icons | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | No partial content rows | Notification icon row only |
| `Media` with an empty notification list | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | No partial content rows | Media row only; this is the exact COUI media-only case |
| `Media` with notification icons | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | Clock and information; AOD content is not shown on LS | No partial content rows | Media row plus notification icon row |

Media-only must remain a `Media` value with an empty notification list. It must not be promoted to a custom Large layout and must not be rendered by a second media-specific owner. AOD notification and media rows are partial-AOD content rows; `AOD Large` may retain input state for transitions but does not display those partial rows.

Weather/date/week are `Information`, not a second content scene. They remain host-owned information views and are updated independently of the AOD content variant.

## Ownership Map

| Responsibility | M2 owner | Adapter or external input |
| --- | --- | --- |
| Scene selection and normalization | COUI clock host | ClockPlugin render state and OPlus AOD state |
| Four clock glyphs and colon | COUI clock host | Current time and locale |
| Date, week, and weather placement | COUI clock host | `Information` data |
| Media title, artist, app icon | COUI clock host | `AodContent.Media` data |
| Notification icon row | COUI clock host | `AodContent.Notifications` or media notification icons |
| Battery text and burn-in translation | COUI clock host | Battery broadcasts and OPlus burn-in input |
| Font variation and weight animation | COUI clock host | Scene/mode transition event |
| ClockPlugin load/render/unload | ClockPlugin bridge | OPlus lifecycle callbacks |
| Weather/forecast/warning/calendar selection | Retained data adapters | Breezy and calendar providers |
| Notification sanitize/filter/rank/dedupe | Retained notification adapter | System notification state |
| USB and hotspot/tethering classification | Retained notification adapter | OPlus system-status notifications |
| Legacy rendering | Legacy owner only in `LEGACY` | Existing 0.1.331 boundary |

## Geometry and Weight Contract

All values below come from the reference host. `W` and `H` are the host width and height; `dp(x)` uses the host density. Burn-in offsets are added only in dozing mode.

### Clock targets

| Surface | Scale and anchor | Font variation and weight |
| --- | --- | --- |
| LS Large | Base clock width is `W * 0.47`; first line starts at `H * 0.215 - 10dp`; second line follows the computed large-line height | `LARGE_VARIATION`: `wght 450`, `wdth 100`, `ROND 100`, `GRAD 0`, `opsz 144`, `slnt 0`; tracking `-0.07 * clock width` |
| AOD Large | Large clock scale `0.9`; first line starts from `H * 0.215 - 24dp` plus burn-in and compressed-size offset | `AOD_LARGE_VARIATION`: `wght 100`, `wdth 100`, `ROND 100`, `GRAD 0`, `opsz 144`, `slnt 0`; tracking `-0.06 * clock width` |
| LS Small | Compact scale `0.36170214`; center around `W * 0.25 + 8dp`; top at `H * 0.105 + 25dp` | `COMPACT_VARIATION`: `wght 500`, `wdth 100`, `ROND 100`, `GRAD 0`, `opsz 96`, `slnt 0`; tracking `-0.09 * clock width` |
| AOD Small | Compact scale `0.36170214`; center around `W * 0.25 + 10dp`; top at `H * 0.105 + 25dp` plus burn-in | `AOD_COMPACT_VARIATION`: `wght 180`, `wdth 100`, `ROND 100`, `GRAD 0`, `opsz 96`, `slnt 0`; tracking `-0.09 * clock width` |
| LS Immersed | Compact scale `0.32978722`; center around `W * 0.25 + 8dp`; top at `H * 0.072 + 30dp` | Uses the compact lockscreen variation and immersed trims; text ratio `0.155`, info Y ratio `0.09` |

The reference applies digit-specific corrections for `0` and `1`, including the small-scene side expansion and left/right trim ratios. M2 must keep those corrections in the host's glyph-target calculation; adapters must not approximate them.

### Information and content targets

- Large information is a right-side group. Its X position is derived from the date/weather measured widths, a minimum `16dp` separation, and a `10dp` inter-group gap; it is not independently centered by an adapter.
- Small information uses the `0.75 * W` center ratio, with `-36dp` lockscreen and `-34dp` AOD X offsets, `H * 0.118 + 33dp` Y placement, and a `3dp` date-to-weather gap.
- Immersed information uses the immersed Y ratio and offset above; it does not reuse Large placement.
- Partial-AOD content starts at `H * 0.255`, with `32dp + burnInX` X placement and `burnInY` added to Y.
- A media row followed by notification icons uses a `28dp` vertical gap.
- Notification icons are `18dp`, have `15dp` gaps, and are capped at seven icons.
- The media group width is constrained to `W - 64dp`; the artist row reserves `24dp` for its app icon relationship.
- AOD burn-in uses the reference periods of 83 minutes on X and 521 minutes on Y. Battery burn-in scales are `0.75` on X and `0.5` on Y, with a `64dp` bottom margin.

### Animation ownership

The host alone applies target animations:

- Default target transition duration: `550ms`.
- Target interpolator: `PathInterpolator(0.2, 0.0, 0.0, 1.0)`.
- Glyph translation, scale, alpha, information, content, and battery targets share the same target application path.
- The colon alpha animation starts after `52%` of the transition and runs for `22%` of the transition.
- Live AOD retargeting uses a `150ms` host fade-out and `200ms` fade-in, with generation checks so stale content cannot finish a newer transition.
- AOD entry defers content and final mode application across the entry frame; cancellation invalidates the generation and cancels pending animations.
- `setLiveAodContent` may use a live crossfade when the content changes the partial scene; otherwise it updates data and applies targets on the next pre-draw.

No retained adapter may create a clock animation, a layout animator, or a second transition timeline.

## ClockPlugin Lifecycle Contract

The bridge must preserve the reference lifecycle and keep the host as the only visual owner.

| Event | Required host action | Required legacy/rollback behavior |
| --- | --- | --- |
| `loadPluginReal("com.oplus.keyguard.clock.big")` succeeds | Attach one host to the plugin view root, initialize data monitors, then sync state | In `LEGACY`, do not attach the COUI host |
| First `render` | Ensure the host exists if load did not attach it, read rendered params, and synchronize state | Legacy renderer remains the only owner in `LEGACY` |
| Rendered UI-state update | Read `getUiState`, animation state, and `getClockSizeState`; map them to LS/AOD scene and call one host presentation method | Never update both host and legacy primary views |
| Screen-off AOD entry | Use the authoritative screen-off origin and defer panoramic final state until OPlus state is authoritative; call host AOD-entry transition once | Roll back to legacy entry path without half-attached host |
| Partial-AOD notification update | Refresh the retained OPlus notification source, resolve `AodContent`, and call `setAodContent` or `setLiveAodContent` | Do not position notification rows outside the host |
| Media metadata/playback update | Resolve `AodContent.Media` or `None`; use the host live-content path when partial AOD accepts content | Do not turn media-only into Large |
| Time tick, battery, color, or information update | Call host update methods; host coalesces layout and target application | Legacy path receives updates only when selected |
| LS <-> AOD exit or re-entry | Cancel pending live/entry transitions, set mode/scene/content together, and apply the target state | Keep rollback path independently startable |
| `unloadPluginReal` | Remove the host and stored host state before the plugin is unloaded; cancel host animations and observers | Restore the legacy owner when `LEGACY` is selected |
| Host view detach | Cancel scheduled target work and live crossfades; unregister host-owned battery receiver | Must not leave a persistent second owner |

ClockPlugin lifecycle observation may remain installed for diagnostics in either mode, but only the selected renderer may mutate primary clock visuals.

## Retained Adapters: Data Only

The following 0.1.331 boundaries are retained as inputs, not renderer owners:

- Weather: `BreezyWeatherSnapshot`, `BreezyWeatherForecast`, `BreezyWeatherAlert`, and their provider/relay classes expose current conditions, forecast eligibility, and warnings. They feed one host `Information` value or one contextual selection.
- Contextual slot: `ContextualAtAGlanceSelector` chooses among forecast, warning, and calendar content. It must produce one semantic result; it must not add forecast, warning, and calendar rows independently.
- Calendar: `CalendarAtAGlanceClient` and `CalendarAtAGlanceProvider` supply calendar text/icon data to the selector. They do not choose a host coordinate.
- Notifications: `AodNotificationPipeline` sanitizes, ranks, filters, deduplicates, and classifies notification entries. The host receives normalized icon data only.
- USB: the notification adapter retains USB and USB-debugging classification and emits the normalized system-status input. The host decides only whether that input belongs in the COUI content model.
- Hotspot/tethering: the notification adapter retains OPlus network-status classification, including tethering text that may not contain the word hotspot. It emits data/semantic state only.
- Media: the legacy media-session boundary resolves the active controller, title, artist, and app icon state. M2 converts this to `AodContent.Media`, preserving an empty notification list for media-only.
- Settings and logging: settings select behavior and logging records owner, scene, content, and lifecycle. Neither is allowed to become a visual fallback renderer.

The host adapter boundary is therefore: `source state -> normalized data -> host presentation`. There is no adapter-to-coordinate path.

## Startup-Exclusive Feature Flag

M2 will use one startup-only clock renderer selector, planned as:

- key: `clock_renderer`;
- values: `COUI_PORT` and `LEGACY`;
- default during the migration: `LEGACY` until the supervisor authorizes COUI clock testing;
- read once during SystemUI module startup, before installing renderer hooks;
- immutable for the lifetime of that SystemUI process.

The renderer mode is captured once during startup and selects the module's primary owner path for the lifetime of that SystemUI process. `LEGACY` installs only the existing legacy module primary owner path. `COUI_PORT` installs only `CouiClockPluginHostController` / `CouiClockHostView`; module legacy primary views are never instantiated as a fallback in this mode. Native OPlus ClockPlugin visual children may be suppressed in the same UI-thread transaction that attaches the COUI host, but ownership is never transferred through a delayed double-owner validation handoff. A setting change cannot hot-switch owners inside one startup instance; it takes effect only after the documented SystemUI restart/reload path.

Slice 1 adds the `clock_renderer` schema key, its `LEGACY` default, and the immutable startup
policy capture. Slice 2 installs exactly one selected owner through the startup router and keeps
the legacy primary creation gates closed in `COUI_PORT`.

## Incremental Production Sequence After M1

1. Record and review the physical M1 acceptance evidence. This gate is passed; M1 UDFPS behavior remains unchanged.
2. Add pure scene/content model tests and the literal geometry/startup-policy contracts for the five surfaces and the four content cases, including media-only. Slice 1 complete; no hooks or visual suppression are part of this step.
3. Add the COUI host view and host-owned geometry/target code from the reference. Verify glyph targets, font variations, information placement, burn-in, and animation constants against the reference before connecting adapters.
4. Add the ClockPlugin host routing and a runtime ownership test proving that `COUI_PORT` and `LEGACY` cannot both install a primary owner. **Slice 2 source implementation exists; supervisor review is pending.**
5. Add the ClockPlugin `loadPluginReal`/`render`/`unloadPluginReal` bridge. Attach one host and suppress native visual output in the same UI-thread transaction. **Slice 2 source implementation exists; device proof is pending.**
6. Add LS Large/Small/Immersed and AOD Large/Small transitions, including screen-off origin, panoramic final-state deferral, AOD entry cancellation, and LS/AOD return. **Slice 2 source implementation exists; visual acceptance is pending.**
7. Add retained weather, forecast, warning, calendar, notification, USB, hotspot/tethering, and media adapters as normalized data inputs. **Deferred to Slice 3; current host setter seams are explicit data-only inputs.**
8. Run focused unit tests, debug build, and APK metadata checks. Then, only after authorization, perform device validation for every roadmap scene/content and lifecycle case with fresh LSPosed logs plus frame/video comparison.
9. Keep `LEGACY` as the startup rollback until the supervisor signs off on one-owner behavior, COUI golden parity, lifecycle coverage, and power/black-frame behavior. Only then consider M4 cleanup.

## Non-Goals

- No Slice 2 physical/device/visual acceptance is claimed in this document.
- Retained business adapters are not claimed as wired; they remain deferred to Slice 3.
- No legacy renderer is removed or changed by this design.
- No YAAP geometry or speculative power/Doze rewrite is part of M2.
- No adapter may become a second clock owner or a second AOD lifecycle owner.
