# Pixel AOD for OPlus

This context defines the user-visible concepts used by the module’s Pixel-like AOD and lock-screen information surfaces.

## Android 17 parity grill status

**Complete as of 2026-08-22.** The closure audit found 65/65 accepted ADRs, no missing decision IDs, and no remaining AOSP/Pixel-vs-OPlus product or architecture choice that requires another grill question. Q66+ must not be invented merely to continue numbering; new questions are justified only by new technical evidence that exposes a genuinely non-derivable user/product choice.

Runtime work after this point is M9 implementation and validation against these decisions. The stable Lockscreen↔AOD transition, clock weight interpolation, and large/small morphology remain protected by `docs/M9_ANIMATION_NON_REGRESSION.md`.

## Language

**Weather Forecast**:
A temporary At a Glance item containing tomorrow’s weather icon and tomorrow’s highest and lowest temperature.
_Avoid_: Hourly forecast, rain graph, multi-day forecast, current weather

**Weather Alert**:
A time-sensitive At a Glance item representing the highest-priority active warning supplied by the weather source, regardless of severity classification.
_Avoid_: Weather forecast, current weather, permanent warning

**Active Weather Location**:
The single Breezy Weather location currently selected as the source of current weather, forecasts, and alerts.
_Avoid_: All saved locations, merged locations, device position unless Breezy selected it

**AOSP/Pixel presentation parity**:
Match the user-visible AOD/lock-screen presentation and interaction semantics of modern AOSP/Pixel where they are relevant to this module, without replacing the vendor low-power display state machine.
_Avoid_: Full AOSP Doze reimplementation, second DozeMachine, vendor-independent panel power ownership

**Vendor-delegated Doze lifecycle**:
OPlus SystemUI remains authoritative for panel/doze power state, ambient brightness, low-power sensor registration, native pulse timing, wallpaper ambient state, and primary FOD/HBM lifecycle. Pixel AOD observes or normalizes these signals and owns its presentation on top.
_Avoid_: Parallel power state machine, independent ambient brightness controller, duplicate sensor registration

**Vendor pulse adapter**:
Pixel AOD decides whether a newly posted notification deserves Pixel-style AOD pulse semantics after privacy, DND/wake preference, pocket/proximity, and power-policy checks. If OPlus already pulses, Pixel AOD follows and deduplicates; otherwise it may request the existing OPlus AOD/pulse entry point without taking ownership of panel brightness or the Doze state machine.
_Avoid_: Custom panel wake state machine, duplicate pulse over an existing OPlus pulse, bypassing notification privacy or sensor/power policy

**Read-only Smartspace adapter**:
Pixel AOD consumes lock-screen/AOD contextual targets already produced by OPlus/SystemUI when a stable target surface is available, maps them into the module's `ContextualTarget` presentation model, and does not invoke target actions while the panel is in AOD. Existing module-owned Weather/Forecast/Alert/Calendar sources remain valid fallbacks when equivalent native targets are absent or unusable.
_Avoid_: Reimplementing every Smartspace provider, invoking arbitrary Smartspace actions directly from AOD, duplicating equivalent native and module-owned targets

**Vendor proximity pause adapter**:
Pixel AOD normalizes OPlus proximity/pocket state into presentation-level `PAUSING`, `PAUSED`, and resume semantics. A transient `NEAR` does not immediately hide the module surface; only sustained near beyond the configured dwell pauses presentation. `FAR` during the dwell cancels the pause, while `FAR` after a completed pause resumes presentation. OPlus remains authoritative for the sensor and panel/doze lifecycle.
_Avoid_: Registering a duplicate proximity sensor, immediate flicker-prone hide/show on every raw edge, using module-side pause state as a second panel power state machine

**Unified vendor suppressor adapter**:
Pixel AOD observes stable OPlus/SystemUI ambient-display suppression signals and maps them into module-owned `AmbientSuppressionReason` inputs for the typed capability policy from Q32. Base AOD, notification pulse, contextual content, wake gestures, and authentication pulse may therefore be gated independently, with Q34 preserving vendor-authorized authentication pulses when base AOD is suppressed. Suppression never forces panel state; when a vendor signal is unavailable or unreliable, Pixel AOD falls back to the vendor lifecycle rather than inventing its own system policy.
_Avoid_: Reimplementing `DozeSuppressor`, bypassing a vendor/system request to suppress ambient display, treating an unknown hook as a hard suppression reason

**Low-power visual budget**:
Every Pixel AOD scene has a measurable lit-pixel/content-density budget used as a design and release-test constraint. The budget protects OLED burn-in and idle power without taking over panel brightness. Exceeding the budget is fixed through deterministic presentation/layout policy rather than runtime-randomly hiding content.
_Avoid_: Module-owned ambient brightness control, using burn-in movement as the only power safeguard, nondeterministic content dropping to chase a pixel threshold

**Vendor wake trigger adapter**:
Pixel AOD consumes stable OPlus/SystemUI wake-trigger signals such as tap, double-tap, lift/pickup, and significant-motion equivalents, and normalizes them into a common transient-presentation trigger observation model. Q56 defines the lifetime: Trigger only follows the already-valid vendor transient scene and owns no independent fixed-duration brief-AOD timer. The module never registers a duplicate Doze sensor stack and never forces panel power independently of the vendor lifecycle.
_Avoid_: Duplicate low-power sensor registration, guessing unsupported gestures from unrelated events, treating a trigger observation as authority over panel state

**AOD presentation-only interaction policy**:
While the panel is in AOD, Pixel AOD content is read-only presentation. Except for vendor-owned biometric handling, a touch may only participate in the vendor wake/Keyguard transition; notification, media, and Smartspace actions are not executed directly from the module AOD surface.
_Avoid_: Direct AOD notification launch, direct media controls from doze, module-owned falsing/touch stack

**Capability-gated vendor dock adapter**:
Docked AOD presentation is enabled only when OPlus/SystemUI exposes a stable, semantically reliable dock or charging-stand state. Devices without such a signal are treated as not supporting docked AOD; charging, orientation, or motion are not combined to synthesize a fake dock state.
_Avoid_: Guessing dock state, module-owned DockManager replacement, enabling dock UI on ordinary charging

**Read-only wallpaper ambient adapter**:
Pixel AOD observes stable vendor wallpaper/ambient transition state only to coordinate clock/content LS-to-AOD and AOD-to-LS presentation timing. OPlus/SystemUI remains the sole owner that tells the wallpaper engine whether it is in ambient mode.
_Avoid_: Calling WallpaperManager to take over ambient state, fighting vendor wallpaper fades, using wallpaper state as a second doze lifecycle

**Native contextual pass-through boundary**:
Pixel AOD may render contextual targets already produced by OPlus/SystemUI/Google services, including targets whose backing data is Pixel-private or non-AOSP, but it does not reverse-engineer or recreate those private providers. Missing native targets remain missing unless covered by an explicit module-owned source such as Breezy Weather or Calendar.
_Avoid_: Reimplementing Now Playing/commute/delivery private backends, scraping private Google state, rejecting a useful target merely because its producer is not AOSP

**Native MinMode handoff adapter**:
If OPlus/SystemUI exposes a reliable Android 17 MinMode state, Pixel AOD yields its own clock/content presentation while native MinMode is active and resumes only from the current real vendor lifecycle after MinMode exits. Pixel AOD does not host third-party MinMode activities or implement a parallel MinMode lifecycle.
_Avoid_: Layering Pixel AOD on top of an active native MinMode surface, hosting app MinMode UI inside the module, inventing MinMode state when the vendor exposes none

**Read-only Live Update adapter**:
Pixel AOD may promote system-recognized ongoing/live-update notifications into a low-power read-only AOD contextual presentation using standard notification structures such as progress, metric, call, timer, fitness, and travel semantics when available. Live Updates remain subject to privacy, suppression, proximity, user scope, and the low-power visual budget, and never gain direct AOD actions.
_Avoid_: Rendering a full notification template on AOD, treating every ongoing notification as a Live Update, bypassing system promotion/importance semantics

**Unified Keyguard privacy and user scope**:
All personal AOD data is scoped to the currently selected Android user and current profile state. User switches invalidate prior-user caches immediately; locked or quiet work-profile content is withheld; sensitive text follows Keyguard privacy. Non-personal data such as weather can remain visible when allowed by the normal presentation gates.
_Avoid_: Cross-user cache reuse, module privacy overrides that reveal system-hidden content, stale work-profile content after profile lock/quiet changes

**Foldable scope boundary**:
The current product scope is validated straight-screen OPlus devices. Foldable/posture-aware behavior is intentionally deferred until real supported hardware and stable OPlus posture/display-role signals are available; current architecture should leave adapter seams but M9 must not ship guessed foldable behavior.
_Avoid_: Untested posture heuristics, module-owned fold state sensors, claiming foldable parity without physical validation

**Vendor Doze transition progress adapter**:
Pixel AOD may consume a reliable OPlus/SystemUI continuous Doze transition progress signal to coordinate presentation alpha, color, typography weight, and geometry during LS-to-AOD and AOD-to-LS handoff. When no trustworthy continuous signal exists, the adapter degrades to the real vendor lifecycle endpoints rather than synthesizing its own timer-driven progress.
_Avoid_: Driving panel power or brightness from the progress value, inventing a parallel transition clock, treating a guessed animation fraction as vendor state

**Selective biometric pulse adapter**:
When OPlus exposes reliable selective pulse semantics equivalent to Android 17 pulsing-without-UI, auth-UI pulse, full pulse, or bright pulse, Pixel AOD maps those states into presentation visibility only. A no-UI pulse suppresses module content, an auth-UI pulse yields ordinary clock/content to vendor biometric/auth presentation, and a full pulse may show normal Pixel AOD content; bright-pulse classification never grants the module brightness or HBM ownership.
_Avoid_: Showing full module AOD during vendor no-UI/auth-only pulses, module-owned biometric pulse state machine, using bright-pulse state to control HBM or panel brightness

**Contextual target arbiter**:
All contextual sources compete through one deterministic arbitration boundary before reaching the COUI scene owner. Native Smartspace-style targets, Live Updates, module Weather/Forecast/Alert/Calendar sources, media-adjacent contextual data, and future sources are deduplicated and ranked by urgency, validity/TTL, privacy/user scope, suppression state, and the low-power visual budget so equivalent or stale targets never stack independently.
_Avoid_: Letting each adapter decide visibility independently, duplicating native and module-owned equivalents, unbounded contextual rows that exceed the visual budget

**Single clock-face product scope**:
Android 17 clock-registry/theme-picker parity is outside the current Pixel AOD product goal. The module keeps one Pixel/COUI primary clock presentation owner and pursues AOD lifecycle, content, transition, privacy, and interaction parity rather than becoming a multi-clock theme engine.
_Avoid_: Module-owned ClockRegistry clone, exposing unvalidated multiple clock faces in M9, coupling runtime correctness to OPlus private clock-picker internals

**Vendor AOD time-tick authority**:
While dozing, reliable OPlus native AOD refresh/time-tick callbacks are the preferred authority for minute and burn-in presentation refresh. `ACTION_TIME_TICK` remains a lockscreen/interactive or capability-fallback input, and Pixel AOD must not schedule its own exact per-minute alarm merely to emulate AOSP `DozeUi`.
_Avoid_: Duplicate exact-alarm wakeups during doze, competing minute schedulers, assuming `ACTION_TIME_TICK` delivery is the primary low-power AOD clock while a stable vendor tick exists

**Native full-pulse notification presentation handoff**:
When OPlus exposes a stable native full-pulse notification or heads-up presentation layer, Pixel AOD yields that foreground notification presentation to SystemUI while retaining its background clock/AOD scene. Stock-suppression policy must exempt the vendor pulse layer without duplicating its interaction or content rendering.
_Avoid_: Reimplementing interactive pulse cards, suppressing the vendor full-pulse notification layer, treating every pulse as icon-only

**DND ambient suppression hard gate**:
`NotificationListenerService.Ranking.getSuppressedVisualEffects()` and the system-equivalent ambient-display suppression decision are authoritative for notification-derived AOD content. A notification suppressed for Ambient Display is excluded from Pixel AOD icons, notification pulses, and notification-derived Live Update presentation; Pixel AOD does not recreate Zen/DND policy.
_Avoid_: Ignoring `SUPPRESSED_EFFECT_AMBIENT`, implementing a parallel Zen policy engine, allowing static AOD icons for notifications the system explicitly suppresses from ambient display

**Vendor notification Doze visibility adapter**:
Notification/media/context rows have a presentation gate separate from generic clock Doze progress. Pixel AOD may consume reliable OPlus/SystemUI fully-dozing, pulsing, or notification-hidden state so screen-off animation cannot expose transient notification content; generic Q16 transition progress is not a substitute for this notification-specific visibility contract.
_Avoid_: Fading notification rows only from generic Doze progress, synthesizing notification visibility from timers, transient LS-to-AOD notification flashes

**Native AOD notification eligibility adapter**:
When OPlus/SystemUI exposes a stable final AOD notification/icon eligibility result, that result determines which notifications Pixel AOD may present; Pixel AOD remains responsible for Pixel/COUI icon-row rendering. The existing NotificationListenerService-based pipeline remains a fallback when no trustworthy native eligibility surface exists.
_Avoid_: Duplicating every evolving SystemUI filter rule in module policy, using native eligibility as a license to reuse vendor visuals, presenting notifications already dismissed/replied/filtered by SystemUI

**Vendor power indication adapter**:
Pixel AOD may consume stable OPlus/SystemUI charging semantics or power-indication output for its existing bottom status row, including states such as charged, fast/slow/restricted charging, charging source, and remaining time when the vendor already computes them. If unavailable, the module falls back to its current battery policy and does not create a hidden BatteryStats-based estimator.
_Avoid_: Reverse engineering private battery estimation backends, module-owned remaining-charge-time calculations that disagree with SystemUI, replacing vendor charging ownership

**Native dozing indication adapter**:
Pixel AOD may consume the vendor/SystemUI dozing indication priority result for transient biometric/help/error, transient system indications, alignment state, and similar temporary messages. A single indication lane may temporarily replace the ordinary battery/power row, while stable charging semantics remain owned by the vendor power indication adapter.
_Avoid_: Rebuilding biometric indication policy, stacking multiple competing indication rows, allowing stale transient messages to outlive vendor state

**System-locale AOD formatting**:
SystemUI AOD clock/date presentation follows the selected Android user's locale, 12/24-hour preference, localized digits, and locale-appropriate date skeleton. The module app's Chinese/English UI preference does not override runtime AOD locale semantics.
_Avoid_: Chinese-versus-other hard-coded date patterns, forcing module-app language onto SystemUI presentation, assuming Latin digits or LTR formatting

**Vendor-owned low-battery AOD suppression**:
The module does not impose a universal fixed battery-percentage threshold for hiding AOD. Low-battery suppression is accepted only from validated OPlus/SystemUI ambient/AOD power policy through the vendor suppressor boundary; absent such a vendor decision, a fixed module threshold must not shut off AOD.
_Avoid_: Hard-coded 15% AOD cutoff, user-configurable duplicate battery-saver policy, treating raw battery percentage as a lifecycle authority

**Native ambient indication pass-through**:
When OPlus/SystemUI provides a stable Ambient Indication or Now Playing surface, Pixel AOD preserves that native surface and its vendor-owned tick, tap, lifecycle, and interaction semantics. The contextual arbiter prevents equivalent module content from duplicating it but does not re-render the native surface.
_Avoid_: Suppressing a validated native ambient indication surface, cloning Now Playing interaction, showing duplicate module and native representations of the same ambient item

**RTL product support**:
M9 includes right-to-left validation for AOD and lockscreen presentation. Clock, date, weather, contextual, media, notification, and indication rows respect system layout direction, START/END semantics, bidi text, and mirrored alignment while preserving the intended COUI visual hierarchy.
_Avoid_: `supportsRtl=false` as a permanent product constraint, LTR-only geometry assumptions, mirroring content that should remain direction-neutral

**Vendor AOD enable authority**:
Continuous Pixel AOD is permitted only while the selected user's OPlus/SystemUI native always-on/AOD policy says continuous AOD is enabled and the vendor lifecycle is actually in an AOD-capable state. Module settings may further restrict presentation but never create a continuous AOD when the native setting is off; vendor-allowed transient pulse, wake-trigger, or authentication scenes remain independent capabilities.
_Avoid_: Treating the module continuous switch as an override of native AOD-off, writing vendor AOD settings from the module, keeping the panel in Doze solely because Pixel AOD is enabled

**Typed ambient suppression capabilities**:
Vendor ambient suppression is normalized by capability rather than collapsed into a single all-or-nothing boolean. The common policy can independently express whether base AOD, notification pulse, contextual content, wake gestures, and authentication pulse are permitted, while every capability remains derived from validated OPlus/SystemUI state.
_Avoid_: One suppression bit that accidentally disables valid auth/wake paths, letting adapters bypass vendor suppression, reconstructing system policy from module heuristics

**Pixel replacement schedule**:
The module schedule limits when Pixel AOD replaces the vendor AOD presentation; it does not own panel lifetime. Inside the schedule, Pixel presentation is eligible only when the vendor already permits AOD. Outside the schedule, Pixel stock suppression is released so OPlus native AOD may resume; users rely on OPlus system scheduling to actually turn ambient display on or off.
_Avoid_: Module schedule controlling `DreamService` screen state, keeping native Doze alive outside vendor policy, presenting a black/empty replacement instead of returning ownership to OPlus

**Authentication-pulse suppression exception**:
Base-AOD suppression does not automatically suppress a vendor-authorized UDFPS/authentication pulse. When OPlus explicitly allows an auth pulse while ordinary AOD is suppressed, Pixel AOD yields to the vendor biometric/auth surface and keeps ordinary clock, notification, media, and contextual presentation suppressed unless separately allowed by typed capability policy.
_Avoid_: Blocking valid vendor authentication because base AOD is off, using auth pulse as a route to restore full Pixel AOD, module-owned suppression-time biometric lifecycle

**Vendor Doze terminal gate**:
A vendor/SystemUI decision that Doze must terminate, including current-user provisioning/setup failure or pending-authentication terminal state, is authoritative. Pixel AOD immediately removes its presentation and cancels any reassert/keepalive attempt; presentation may return only after a new valid vendor Doze/AOD lifecycle begins.
_Avoid_: Rewriting vendor FINISH/OFF into DOZE, reasserting Pixel AOD during a terminal gate, treating pending authentication as an ordinary transient visibility state

**Native SystemUI media semantics adapter**:
Pixel AOD consumes the current-user/current-profile media eligibility and active-media selection already resolved by OPlus/SystemUI when a stable native media pipeline seam exists. The module renders that semantic result in its COUI media row; direct `MediaSessionManager` enumeration and module-owned paused/idle retention remain fallback behavior only.
_Avoid_: Treating all active media sessions as equally eligible, leaking old-user/work-profile media across switches, independently reproducing SystemUI media timeout/resumption policy, duplicating an equivalent native ambient indication

**Native clock-size preference ceiling**:
The selected user's native SMALL/DYNAMIC clock-size preference is respected without turning Pixel AOD into a multi-clock registry. Native SMALL is a hard ceiling that prevents the Pixel/COUI host from returning to the large face on lockscreen or AOD; DYNAMIC allows the existing content-aware large/compact policy to choose within that single clock face.
_Avoid_: Ignoring a native SMALL preference, creating a second module clock-size preference, conflating clock-size parity with multi-clock/theme-engine scope

**Native ambient foreground collision adapter**:
When a vendor-owned full-pulse, promoted notification, or equivalent ambient foreground surface occupies display space, Pixel AOD consumes reliable OPlus/SystemUI foreground/centering/bounds state and temporarily moves its background clock/information geometry out of collision. The native foreground remains interaction owner and Pixel geometry returns deterministically when it leaves.
_Avoid_: Allowing native pulse content to cover the Pixel clock, hiding the entire Pixel background when a safe avoidance layout exists, estimating collision from notification text or app identity

**Pixel/COUI charge animation**:
A vendor charging-semantic transition from not charging to charging may trigger one presentation-only charge animation on the currently visible Pixel/COUI clock. The animation does not own charging UI, panel state, brightness, HBM, or power semantics, and it obeys animator-scale and low-power visual-budget constraints.
_Avoid_: Replaying the animation on every battery percentage update, creating a module charging lifecycle, running high-cost ambient animation regardless of animation-scale/power constraints

**Configuration-responsive typography**:
M9 clock and information typography responds to SystemUI density, display-size, and font-scale configuration changes. Clock proportions remain Pixel/COUI-specific within validated bounds, while date, weather, contextual, media, notification/indication text and layout are recomputed rather than assuming fixed DIP geometry; constrained layouts resolve through compact/arbitration policy instead of ignoring the system configuration.
_Avoid_: `scaledClockTextDp()` permanently returning unscaled base DIP, stale geometry after density/font-scale changes, unconstrained scaling that breaks AOD visual budget or overlap rules

**Selectable AOD doze palette**:
Pixel AOD exposes a presentation preference between an AOSP-like neutral white Doze palette and the existing colored Monet/COUI ambient palette. The choice affects module-owned AOD clock, information text, notification/contextual glyph treatment and related presentation only; lockscreen theme behavior, source-owned multicolor artwork, and vendor biometric/native foreground surfaces remain outside this setting.
_Avoid_: Treating colored AOD as mandatory parity, recoloring vendor-owned surfaces, letting the palette setting become a lifecycle or brightness control

**Native clock theme adapter**:
For lockscreen presentation, Pixel AOD consumes a stable selected-user OPlus/SystemUI clock seed/color and reliable region-darkness/theme state when available, with current Monet behavior as fallback. Fully dozed AOD color remains governed by the selectable AOD doze palette rather than by the lockscreen seed.
_Avoid_: Creating a second module clock-color picker, assuming global wallpaper Monet equals the user's clock color, letting a lockscreen seed override AOD doze-palette policy

**Native Keyguard scene eligibility adapter**:
Pixel AOD prefers authoritative OPlus/SystemUI Keyguard scene/state for Lockscreen, AOD, Dozing, Occluded, Bouncer, Gone and related presentation eligibility. Existing `KeyguardManager`, screen-state and view-tree heuristics remain fallback diagnostics only; they do not override a reliable native scene decision.
_Avoid_: Inferring authoritative Keyguard state from visual child names, presenting Pixel AOD while SystemUI is Gone/Occluded in a disallowed scene, allowing fallback heuristics to fight a native scene transition

**Pulse doze-style override**:
When continuous native AOD is disabled but a vendor-authorized notification pulse enters a valid Dozing/pulse scene, Pixel background presentation immediately uses its fully-dozed palette, weight, alpha and geometry semantics for that pulse window. This is a transient presentation override only and never implies that continuous AOD has been enabled.
_Avoid_: Showing lockscreen styling behind an AOD-off pulse, synthesizing a fake continuous-AOD lifecycle, animating through an incorrect intermediate state when SystemUI already classifies the scene as fully dozed

**Native clock target-region adapter**:
A reliable OPlus/SystemUI clock target or safe region is the outer geometry boundary for Pixel/COUI clock placement. Pixel internal proportions, anchors, content layout and burn-in movement remain module-owned inside that region, while target-region updates participate in RTL, font-scale, collision and configuration recomputation.
_Avoid_: Assuming fixed full-screen DP bounds, moving burn-in content outside the native safe region, surrendering all Pixel geometry to a native container when only an outer target boundary is needed

**Five-percent module AOD OPR release gate**:
Representative fully-dozed Pixel-owned scenes must stay at or below a 5% on-pixel ratio as a hard release gate. Deterministic screenshot/off-screen tests cover large, compact, media, contextual, notification-overflow, and both selectable AOD palettes; vendor-owned biometric and native foreground surfaces are excluded from the module budget.
_Avoid_: Treating 5% as a logging-only hint, hiding random content at runtime to pass the metric, counting vendor-owned foreground pixels against module presentation

**System bold-text weight adjustment**:
Module AOD/lockscreen weight controls remain the base Pixel/COUI font-axis choice, then Android/SystemUI `fontWeightAdjustment` from accessibility configuration is applied and clamped to the validated typeface range. Runtime configuration changes refresh the visible host immediately.
_Avoid_: Ignoring system Bold text because a module slider exists, double-applying weight adjustment, allowing the resulting axis value outside validated font bounds

**Native notification icon capacity and overflow semantics**:
Notification eligibility remains owned by the native AOD eligibility adapter, while a separate adapter consumes reliable OPlus/SystemUI AOD/lockscreen icon capacity. Pixel rows use native/AOSP-style overflow-dot semantics rather than a hard-coded five-icon `+N`; when native capacity is unavailable, safe capacity is derived from the validated target region.
_Avoid_: Hard-coding five icons on every layout, conflating eligibility with capacity, using `+N` as permanent AOD overflow semantics

**Keyguard accessibility semantic adapter**:
When Pixel/COUI replaces the stock clock, the replacement host becomes the single accessibility semantic owner for its visible Keyguard content: localized time plus logically grouped date/weather/contextual/power information, with decorative digits and glyphs hidden from the accessibility tree. Hidden stock clock semantics must not remain as duplicates, and fully-dozed AOD exposes only semantics that native SystemUI accessibility eligibility permits.
_Avoid_: Marking the entire replacement host inaccessible, exposing each decorative clock glyph as a node, leaving both hidden stock and Pixel clock descriptions active

**Mandatory burn-in movement**:
Burn-in movement is a required safety property whenever Pixel AOD owns persistent ambient presentation and is not user-disableable in the normal settings UI. If OPlus reliably moves the same host, the module disables only its duplicate offset to prevent double movement; any stationary override is debug-only, non-persistent, and excluded from release behavior.
_Avoid_: A normal user setting that permanently disables burn-in movement, stacking vendor and module offsets, treating a static debug screenshot mode as supported runtime behavior

**Vendor AOD dim/scrim composition adapter**:
Pixel-owned ambient content participates in the OPlus/SystemUI AOD dim/scrim composition instead of treating panel brightness as the only low-light authority. Prefer placing the Pixel host under the native AOD dimming layer; if that is not possible, consume the vendor-computed dim amount as a read-only presentation multiplier. The module never derives ambient brightness or dimming from its own light-sensor policy.
_Avoid_: Pixel content remaining visually undimmed above a native AOD scrim, a second module ambient-light brightness algorithm, applying dimming that fights vendor biometric/native foreground surfaces

**Native burn-in transform adapter**:
Mandatory ambient movement prefers a stable OPlus/SystemUI burn-in transform, including X/Y translation and scale when available. If the vendor already moves the exact Pixel host, no duplicate transform is applied; only when no reliable native transform exists does the module use its validated fallback trajectory.
_Avoid_: Double-applying vendor and module offsets, requiring a module trajectory when SystemUI already supplies the transform, accepting a static persistent AOD when native transform discovery fails

**Vendor screen-off animation eligibility adapter**:
Doze transition progress animates Pixel presentation only when OPlus/SystemUI says the current screen-off/AOD transition may be animated. Display-blanking or equivalent snap-only paths go directly to their safe presentation endpoint instead of replaying a Pixel morph on top of a transition the platform intentionally does not animate.
_Avoid_: Treating every LS-to-AOD transition as animation-capable, inferring blanking capability from timing heuristics, using Q16 progress when native animation eligibility explicitly denies animation

**System animation-scale policy**:
All module-owned presentation animations obey the current Android/SystemUI animation-scale preference. With animations disabled, Pixel/COUI clock morphs, contextual/media transitions, charge effects, and optional module visual effects snap to their terminal state; non-zero scaling adjusts module timing consistently while vendor-owned biometric/pulse animation remains untouched.
_Avoid_: Respecting animator scale only for one effect, keeping decorative module motion active when Android animations are disabled, altering vendor-owned authentication animation timing

**Native Doze input pass-through**:
Pixel AOD is not a Doze input owner. Its ambient clock, content, and transition layers remain touch-transparent and do not register tap, double-tap, long-press, or wake gestures; OPlus/SystemUI continues to route wake gestures, pulse touch, UDFPS/device-entry input, and any dock-specific interaction.
_Avoid_: A full-screen Pixel overlay intercepting vendor gestures, module tap-to-wake handlers, copying SystemUI Doze touch-interception policy into the module

**Vendor transient presentation filter**:
The user-facing Trigger only mode is a presentation filter over already-valid OPlus/SystemUI transient ambient scenes, not a module-owned brief-AOD lifecycle. Pixel content appears only while the vendor pulse/pickup/tap/UDFPS or equivalent transient scene is valid and disappears when that native scene ends; the module owns no fixed-duration trigger timer, proximity sensor guard, or AOD-active lifetime for this mode.
_Avoid_: A hard-coded ten-second Pixel AOD session, extending a native pulse after SystemUI has ended it, treating Trigger only as permission to own sensor or panel lifecycle

**Native AOD notification icon order adapter**:
After native eligibility is resolved, Pixel AOD prefers the final OPlus/SystemUI ambient notification icon ordering rather than preserving arbitrary listener snapshot order or inventing a module ranking algorithm. Fallback ordering is deterministic and stable only when a reliable native ordered key list is unavailable.
_Avoid_: Assuming NotificationListenerService array order equals native AOD order, re-sorting by module-specific importance/post-time policy when SystemUI already supplies ordering, allowing identical inputs to reshuffle on rebuild

**Native AOD icon visual-metrics adapter**:
Pixel notification glyph presentation consumes stable OPlus/SystemUI Doze icon alpha, size, spacing, and overflow-dot geometry when available. Eligibility, ordering, capacity, color palette, and visual metrics remain separate responsibilities; AOSP Android 17 values are the fallback baseline when vendor metrics are unavailable.
_Avoid_: Full-opacity fixed-DP AOD icons on every device, coupling icon eligibility to visual dimensions, letting palette selection override vendor/AOSP Doze alpha semantics

**Vendor ambient session epoch**:
Each new valid OPlus/SystemUI ambient session receives a monotonically increasing module-side epoch used only to invalidate stale Pixel work. Native terminal/FINISH conditions, selected-user changes, or host teardown invalidate the epoch immediately; asynchronous adapter callbacks, delayed tasks, content refreshes, and presentation transitions from older epochs are discarded rather than allowed to re-show the surface.
_Avoid_: Independent stale-callback checks with no shared session boundary, old media/weather/animation work reasserting Pixel after vendor FINISH, using the epoch to drive or extend the vendor lifecycle

**Biometric transient session reset**:
Module-owned biometric presentation residue is scoped to one vendor ambient session. Starting a new ambient epoch or reaching a terminal gate clears optional success ripple, pressed/highlight residue, pending exit animation, and related Pixel callbacks; real OPlus/SystemUI authentication state is never cleared or rewritten by the module.
_Avoid_: Carrying success/pressed visuals into a later Doze session, relying only on old timers for cleanup, calling vendor authentication-state mutators as part of Pixel reset

**Native lockscreen notification visibility authority with narrow OOS compatibility correction**:
OPlus/SystemUI owns the general Keyguard notification visibility policy. Pixel AOD/Lockscreen normally observes and consumes the native result, but the existing user-facing OOS compatibility fix is retained for the confirmed unlock-then-screen-off regression where OOS incorrectly hides the entire set of otherwise-eligible lockscreen notifications. That exception may only correct `hidden=true` to visible through a deliberately narrow eligibility predicate; it never force-hides a native-visible notification and must not revive privacy-secret, ranking-secret, media/transport, SystemUI/Android, module-internal, or known low-importance notifications.
_Avoid_: Removing the confirmed OOS compatibility fix in the name of pure read-only authority, restoring broad module-owned low-importance/silent hiding, bypassing native privacy/profile rules, treating the compatibility exception as a second general lockscreen policy owner

**Native AOD availability gate**:
Continuous Pixel AOD requires a valid native AOD-capable device/configuration in addition to the selected user's native AOD-enabled preference and a valid vendor lifecycle. If native availability is false, Pixel presentation cannot manufacture continuous AOD; separately authorized transient pulse, wake, or authentication paths remain capability-gated.
_Avoid_: Treating enabled and available as the same signal, keeping Pixel continuous AOD alive on an unavailable native configuration, suppressing valid vendor transient authentication solely because base AOD is unavailable

**Native AOD display-option authority**:
The selected user's OPlus AOD mode is the only display-mode/schedule authority. All-day may pre-arm the existing Pixel screen-off presentation, scheduled mode may pre-arm only inside the native OPlus time window, and energy-saving mode waits for a real vendor transient/ambient scene. Pixel AOD exposes no second Continuous/Trigger-only selector or replacement schedule and never creates or extends Doze from these settings.
_Avoid_: Intersecting native AOD with a second module schedule, copying OPlus energy-saving mode into a fixed module timer, treating a configuration value as proof that a vendor ambient session is already active

**Native direct-wake-to-Gone handoff**:
When OPlus/SystemUI has already committed an AOD/Dozing wake transition directly to the unlocked/Gone scene, Pixel exits ambient presentation without attaching or briefly showing its lockscreen replacement. If the native direct-to-Gone transition is cancelled, presentation follows the next authoritative native scene instead of guessing.
_Avoid_: Flashing Pixel Lockscreen during face/trust/direct-unlock wake, forcing every AOD exit through a lockscreen scene, inferring Gone solely from screen-on timing

**Selected-user scoped presentation preferences**:
User-visible Pixel presentation preferences are resolved for the current Android selected user, including AOD palette, clock weights, non-lockscreen transition style, and contextual/weather/calendar display choices. AOD display mode and schedule are deliberately excluded because OPlus native AOD settings own those decisions. Device/module enablement, diagnostics, and release-safety policy may remain module-wide. Selected-user changes invalidate cached preference/content state and the current ambient session epoch.
_Avoid_: Reusing one user's private or visual preferences for another user, treating a profile switch as only a content refresh, making safety gates user-disableable through per-user preferences

**Primary-display-only M9 scope**:
M9 Pixel AOD/Lockscreen replacement targets the device's primary built-in/default display only. Secondary/external displays do not receive a copied Pixel host or primary-display geometry until real OPlus multi-display hardware and reliable vendor lifecycle/safe-region seams are available for validation.
_Avoid_: Mirroring the primary AOD overlay onto every display, assuming secondary displays share the same burn-in/safe-region/power lifecycle, expanding M9 scope without hardware evidence

**M9 animation non-regression baseline**:
The currently stable Lockscreen-to-AOD and AOD-to-Lockscreen transition animation, clock weight interpolation, and large/small clock morphology are protected product behavior. Parity adapters may replace lifecycle authority, eligibility, data inputs, configuration inputs, or stale-callback handling around those animations, but must not rewrite the proven interpolation/morph engine unless a separately reproduced defect requires it. On the normal supported path with Android animation scale at 1x, existing duration, continuity, glyph geometry, weight handoff, and visual rhythm are the regression baseline. A vendor-declared snap/blanking/direct-to-Gone path may intentionally skip the animation only for that native scene.
_Avoid_: Reimplementing stable morph math merely to look more AOSP-internal, accepting a parity change that introduces jumps/flicker/weight discontinuity, modifying the normal 1x animation path when only an eligibility or lifecycle adapter is needed
