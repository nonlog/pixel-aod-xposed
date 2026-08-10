package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;

/**
 * Persistent replacement clock rooted in OPlus' ClockPlugin view tree.
 *
 * <p>The host remains attached while its presentation moves from lockscreen to AOD. Its two
 * internal presentation layers are coordinated in one ViewRoot so a lockscreen layer is never
 * removed before the prepared AOD layer can be drawn.</p>
 */
final class PixelClockPluginHostView extends FrameLayout {
    private static final long ENTRY_CROSSFADE_MILLIS = 360L;
    private static final long CLOCK_WEIGHT_HANDOFF_MILLIS = 700L;
    /** COUI-like PathInterpolator(0.2, 0, 0, 1) for small→large entry morph. */
    private static final PathInterpolator SIZE_MORPH_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);
    /** COUI applies the clock and information target positions in one 550 ms transaction. */
    private static final long SIZE_MORPH_MILLIS = PixelAodVisualStyle.COUI_WEIGHT_MORPH_MILLIS;
    /** Compact text size / large text size — start scale for morph into LARGE. */
    private static final float COMPACT_TO_LARGE_START_SCALE =
            PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP / (float) PixelAodVisualStyle.LARGE_CLOCK_TEXT_DP;
    private static final int HANDOFF_DIAGNOSTIC_FRAME_LIMIT = 12;
    private static final long HANDOFF_DIAGNOSTIC_WINDOW_MILLIS = 2_500L;
    private static final int HANDOFF_DIAGNOSTIC_ANCESTOR_LIMIT = 8;
    private static final int HANDOFF_DIAGNOSTIC_CHILD_LIMIT = 12;
    private static final String HANDOFF_DIAGNOSTIC_PREFIX = "[DEBUG-WEIGHT-HANDOFF]";
    private static final int VIEW_CLOCK_TIME = 1;
    private static final int VIEW_DATE_MESSAGE = 11;

    private final PixelLockscreenClockView lockscreenLayer;
    private final PixelAodClockView aodLayer;
    private final CouiClockSizeTransitionLayer sizeTransitionLayer;
    private ClockPluginSceneMachine.Scene scene = ClockPluginSceneMachine.Scene.HIDDEN;
    private Runnable finishAodEntryRunnable;
    private long entryGeneration;
    private long sizeTransitionGeneration;
    private Runnable firstPresentationFrameCallback;
    private boolean preparingAodWeight;
    private int handoffDiagnosticFramesRemaining;
    private int handoffDiagnosticFrame;
    private long handoffDiagnosticStartedAt;
    private String handoffDiagnosticSource = "";
    private String handoffDiagnosticTrace = "";

    PixelClockPluginHostView(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(View.INVISIBLE);

        lockscreenLayer = new PixelLockscreenClockView(context);
        lockscreenLayer.setClockPluginManaged(true, "ClockPlugin-host-create");
        lockscreenLayer.setVisibility(View.INVISIBLE);

        aodLayer = new PixelAodClockView(context);
        aodLayer.setClockPluginManaged(true, "ClockPlugin-host-create");
        aodLayer.setVisibility(View.INVISIBLE);

        sizeTransitionLayer = new CouiClockSizeTransitionLayer(context);

        addView(lockscreenLayer, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(aodLayer, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(sizeTransitionLayer, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    ClockPluginSceneMachine.Scene scene() {
        return scene;
    }

    boolean hasUsableBounds() {
        return getWidth() > 0 && getHeight() > 0 && isAttachedToWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (scene != ClockPluginSceneMachine.Scene.HIDDEN) {
            notifyAfterPresentationFrame();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAodEntry();
        cancelSizeTransition("host-detached");
        handoffDiagnosticFramesRemaining = 0;
        super.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        logHandoffDiagnosticFrame();
    }

    void setFirstPresentationFrameCallback(Runnable callback) {
        firstPresentationFrameCallback = callback;
    }

    void present(ClockPluginSceneMachine.Decision decision, String source) {
        if (decision == null) {
            return;
        }
        ClockPluginSceneMachine.Scene target = decision.scene;
        if (target == ClockPluginSceneMachine.Scene.HIDDEN) {
            hide(source);
            return;
        }

        if (decision.changed && !isClockSizeChange(scene, target)) {
            cancelSizeTransition("scene-change-without-size-morph");
        }

        if (!decision.changed && scene == target) {
            refreshCurrentLayer(source);
            if (scene.isLockscreen() && decision.preparingAod) {
                Context ctx = getContext();
                boolean interactive = ctx != null && PixelAodClockView.isDeviceInteractive(ctx);
                // Logs 20:55: continuous LS wake/sleep cancelled aod-to-ls (160→340) because
                // early-aod-large ran while interactive and switched the host to AOD weight 160.
                // Never stage AOD while the user is still looking at the lockscreen.
                if (interactive) {
                    if (preparingAodWeight) {
                        preparingAodWeight = false;
                        lockscreenLayer.restoreClockPluginLockscreenWeight(
                                source + "#cancel-early-aod-interactive");
                    }
                    PixelAodLog.log("skipped early AOD staging while interactive source=" + source
                            + " hostScene=" + scene
                            + " preparingAod=" + decision.preparingAod
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                    return;
                }
                // Logs aod-c-6723d: unlock→app screen-off still sat on LOCKSCREEN_SMALL and ran
                // early-aod-weight (340→151 on the LS layer) during the black reveal delay.
                // Only morph for interactive-lockscreen origin (recent stamp || screen-off latch).
                boolean lockscreenToAodWeight = PixelAodClockView
                        .shouldAnimateLockscreenToAodWeight();
                if (!lockscreenToAodWeight) {
                    if (preparingAodWeight) {
                        preparingAodWeight = false;
                        lockscreenLayer.cancelClockPluginWeightTransitionKeepCurrent(
                                source + "#cancel-early-aod-non-ls");
                    }
                    ClockPluginSceneMachine.Scene earlyAod = PixelAodClockView
                            .hasCompactClockNotificationContent()
                            ? ClockPluginSceneMachine.Scene.AOD_SMALL
                            : ClockPluginSceneMachine.Scene.AOD_LARGE;
                    armHandoffDiagnostics(source + "#early-aod-direct-non-ls");
                    // enteringAod=false + non-LS origin → presentAod applies stable AOD weight.
                    presentAod(earlyAod, false, source + "#early-aod-direct-non-ls");
                    scene = earlyAod;
                    preparingAodWeight = false;
                    PixelAodLog.log("early AOD direct non-lockscreen source=" + source
                            + " hostSceneWas=LOCKSCREEN"
                            + " aodScene=" + earlyAod
                            + " lockscreenToAodWeight=false"
                            + " screenOffFromLs="
                            + PixelAodClockView.wasScreenOffFromInteractiveLockscreen()
                            + " trace=" + PixelAodClockView.currentAodTraceId());
                    notifyAfterPresentationFrame();
                    return;
                }
                // Media-only / empty: stage AOD LARGE immediately (no LS weight morph).
                if (!PixelAodClockView.hasCompactClockNotificationContent()) {
                    armHandoffDiagnostics(source + "#early-aod-large");
                    presentAod(ClockPluginSceneMachine.Scene.AOD_LARGE, true,
                            source + "#early-aod-large");
                    scene = ClockPluginSceneMachine.Scene.AOD_LARGE;
                    preparingAodWeight = false;
                    notifyAfterPresentationFrame();
                    return;
                }
                // Start weight morph on the still-visible lockscreen so 340→AOD is continuous
                // through blank/grace; AOD layer continues from the live intermediate weight.
                armHandoffDiagnostics(source + "#early-aod-weight");
                preparingAodWeight = true;
                lockscreenLayer.beginClockPluginAodWeightTransition(
                        source + "#early-aod-weight");
            } else if (scene.isLockscreen() && preparingAodWeight) {
                preparingAodWeight = false;
                lockscreenLayer.restoreClockPluginLockscreenWeight(
                        source + "#cancel-early-aod-weight");
            }
            return;
        }

        if (target.isLockscreen()) {
            // Hold AOD only while dozing. When interactive (wake), always take lockscreen —
            // logs: interactive=true + aodWeightRunning ignored LS present, leaving AOD weight.
            Context ctx = getContext();
            boolean interactive = ctx != null && PixelAodClockView.isDeviceInteractive(ctx);
            if (scene.isAod() && !interactive) {
                PixelAodLog.log("ignored lockscreen present during AOD handoff source=" + source
                        + " interactive=false"
                        + " aodWeightRunning=" + aodLayer.isClockPluginWeightTransitionRunning()
                        + " hostScene=" + scene
                        + " requested=" + target
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                aodLayer.refreshClockPluginAodContent(source + "#hold-aod-vs-lockscreen");
                notifyAfterPresentationFrame();
                return;
            }
            // Non-lockscreen screen-off can publish KEYGUARD while dozing; painting LS at 340
            // then early-aod-weight is what the user sees as "340 then scale". Skip to AOD.
            // LS→AOD keeps the screen-off latch / recent stamp so morph still runs.
            if (!interactive
                    && !PixelAodClockView.shouldAnimateLockscreenToAodWeight()) {
                ClockPluginSceneMachine.Scene aodFallback = PixelAodClockView
                        .hasCompactClockNotificationContent()
                        ? ClockPluginSceneMachine.Scene.AOD_SMALL
                        : ClockPluginSceneMachine.Scene.AOD_LARGE;
                PixelAodLog.log("skipped lockscreen present for non-lockscreen doze source="
                        + source
                        + " requested=" + target
                        + " aodFallback=" + aodFallback
                        + " hostScene=" + scene
                        + " screenOffFromLs="
                        + PixelAodClockView.wasScreenOffFromInteractiveLockscreen()
                        + " trace=" + PixelAodClockView.currentAodTraceId());
                presentAod(aodFallback, false, source + "#non-ls-doze-skip-lockscreen");
                scene = aodFallback;
                notifyAfterPresentationFrame();
                return;
            }
            presentLockscreen(target, source);
        } else if (target.isAod()) {
            // Never enter AOD_SMALL when module has no non-media notifications.
            ClockPluginSceneMachine.Scene aodTarget = target;
            if (target == ClockPluginSceneMachine.Scene.AOD_SMALL
                    && !PixelAodClockView.hasCompactClockNotificationContent()) {
                aodTarget = ClockPluginSceneMachine.Scene.AOD_LARGE;
                PixelAodLog.log("forced AOD_SMALL→AOD_LARGE before present source=" + source
                        + " reason=no-module-notifications"
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            }
            boolean fromLockscreenSmall = scene == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL;
            presentAod(aodTarget, decision.enteringAod || scene.isLockscreen(), source,
                    fromLockscreenSmall);
            target = aodTarget;
        }
        scene = target;
        notifyAfterPresentationFrame();
    }

    void refreshCurrentLayer(String source) {
        if (scene.isLockscreen()) {
            lockscreenLayer.refreshClockPluginLockscreenContent(source + "#ClockPlugin-host");
        } else if (scene.isAod()) {
            aodLayer.refreshClockPluginAodContent(source + "#ClockPlugin-host");
        }
    }

    void hide(String source) {
        cancelAodEntry();
        cancelSizeTransition("host-hide");
        lockscreenLayer.animate().cancel();
        aodLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        aodLayer.setAlpha(1f);
        lockscreenLayer.setClockPluginLayerVisible(false);
        aodLayer.setClockPluginLayerVisible(false);
        setVisibility(View.INVISIBLE);
        preparingAodWeight = false;
        scene = ClockPluginSceneMachine.Scene.HIDDEN;
        // Unlock / leave keyguard: drop LS→AOD morph arm so app screen-off stays direct.
        Context ctx = getContext();
        if (ctx != null
                && PixelAodClockView.isDeviceInteractive(ctx)
                && !PixelLockscreenClockView.isSystemKeyguardLocked(ctx)) {
            PixelAodClockView.clearLockscreenSessionForAodWeight(source + "#host-hide-unlock");
        }
        PixelAodLog.log("hid persistent ClockPlugin host source=" + source
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private void presentLockscreen(ClockPluginSceneMachine.Scene target, String source) {
        // A repeated request for the scene already selected by the first present() must not
        // cancel a prepared source frame and run applyClockMode(target) a second time. The
        // pending pre-draw will start the original transaction from its stable source snapshot.
        if (sizeTransitionLayer.hasActiveTransition() && target == scene) {
            PixelAodLog.log("coalesced equivalent lockscreen size transition source=" + source
                    + " target=" + target
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }
        SizeTransitionRequest sizeTransition = prepareSizeTransition(target, source);
        cancelAodEntry();
        preparingAodWeight = false;
        setVisibility(View.VISIBLE);
        Context ctx = getContext();
        if (ctx != null && PixelAodClockView.isDeviceInteractive(ctx)) {
            // Real interactive lockscreen presentation — arm LS→AOD weight morph.
            PixelAodClockView.noteLockscreenSessionForAodWeight(source + "#present-lockscreen");
        }
        // Capture AOD weight before hiding the layer so we can reverse-morph weight.
        boolean fromAodScene = scene.isAod() && aodLayer.getVisibility() == View.VISIBLE;
        int lsWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        int aodWeight = PixelAodClockView.aodClockWeight(getContext());
        int aodLayerWeight = aodLayer.clockPluginWeight();
        int lsLayerWeight = lockscreenLayer.clockPluginWeight();
        // early-aod-weight keeps host on LOCKSCREEN while parking the LS layer at AOD weight.
        // Wake must still reverse-morph 160→340 (logs: restore snap after cancel-early-aod).
        boolean lsLayerAtAodWeight = Math.abs(lsLayerWeight - aodWeight) <= 16
                && Math.abs(lsLayerWeight - lsWeight) > 8;
        int fromWeight;
        boolean animateFromAod;
        if (fromAodScene) {
            fromWeight = aodLayerWeight > 0 ? aodLayerWeight : aodWeight;
            animateFromAod = Math.abs(fromWeight - lsWeight) > 8;
        } else if (lsLayerAtAodWeight) {
            fromWeight = lsLayerWeight;
            animateFromAod = true;
        } else {
            fromWeight = lsWeight;
            animateFromAod = false;
        }
        PixelAodLog.log("presentLockscreen weight plan source=" + source
                + " fromAodScene=" + fromAodScene
                + " lsLayerAtAodWeight=" + lsLayerAtAodWeight
                + " animateFromAod=" + animateFromAod
                + " fromWeight=" + fromWeight
                + " lsWeight=" + lsWeight
                + " aodLayerWeight=" + aodLayerWeight
                + " lsLayerWeight=" + lsLayerWeight
                + " hostScene=" + scene
                + " trace=" + PixelAodClockView.currentAodTraceId());
        aodLayer.animate().cancel();
        aodLayer.setScaleX(1f);
        aodLayer.setScaleY(1f);
        aodLayer.setAlpha(1f);
        aodLayer.clearAodWeightHandoffSettled(source + "#present-lockscreen");
        aodLayer.setClockPluginLayerVisible(false);
        lockscreenLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        lockscreenLayer.setClockPluginGlyphTransitionActive(sizeTransition != null);
        try {
            lockscreenLayer.presentClockPluginLockscreen(
                    target == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL,
                    source + "#ClockPlugin-host",
                    fromWeight,
                    animateFromAod);
        } finally {
            lockscreenLayer.setClockPluginGlyphTransitionActive(false);
        }
        lockscreenLayer.setClockPluginLayerVisible(true);
        startSizeTransitionAfterLayout(sizeTransition);
    }

    private void presentAod(ClockPluginSceneMachine.Scene target, boolean enteringAod,
            String source) {
        presentAod(target, enteringAod, source, scene == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL);
    }

    private void presentAod(ClockPluginSceneMachine.Scene target, boolean enteringAod,
            String source, boolean fromLockscreenSmall) {
        setVisibility(View.VISIBLE);
        SizeTransitionRequest sizeTransition = prepareSizeTransition(target, source);
        boolean hostOnLockscreen = scene.isLockscreen()
                && lockscreenLayer.getVisibility() == View.VISIBLE;
        // Interactive-lockscreen origin only (recent stamp || noteScreenOff latch).
        // Unlock → launcher/app → screen-off latches false → stable AOD weight, no morph.
        boolean lockscreenToAodWeight = PixelAodClockView.shouldAnimateLockscreenToAodWeight();
        boolean fromLockscreen = hostOnLockscreen && lockscreenToAodWeight;
        // OOS 16.0.9 moves the ClockPlugin root as Doze commits. Blending the independent
        // lockscreen and AOD layouts therefore exposes a clock/date coordinate jump. Prepare
        // and show the AOD layer directly; the existing AOD weight animator remains intact.
        boolean crossfadeFromLockscreen = enteringAod && fromLockscreen
                && !OosAodHandoffProfile.usesStableSingleLayerAodHandoff(
                        android.os.Build.DISPLAY);
        if (crossfadeFromLockscreen && !preparingAodWeight) {
            armHandoffDiagnostics(source + "#aod-scene");
        }
        int handoffWeight = lockscreenLayer.clockPluginWeight();
        // Capture before clear: early-aod-weight may already be mid-morph on the LS layer.
        boolean wasPreparingAodWeight = preparingAodWeight;
        preparingAodWeight = false;
        boolean aodCompact = target == ClockPluginSceneMachine.Scene.AOD_SMALL;
        // Media-only / empty must never stay on lockscreen SMALL after screen-off.
        boolean forceLargeAodSurface = !aodCompact
                && (crossfadeFromLockscreen || fromLockscreen
                || (fromLockscreenSmall && lockscreenToAodWeight));

        aodLayer.animate().cancel();
        aodLayer.animate().setListener(null);
        // Atomic prepare: large/compact + media before any surface switch.
        // Do not touch platform black-frame / power path.
        boolean morphFromCompact = forceLargeAodSurface && fromLockscreenSmall;
        int configuredLsWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        int aodTargetWeight = PixelAodClockView.aodClockWeight(getContext());
        // Weight morph only for interactive-lockscreen origin. Host may briefly sit on
        // KEYGUARD/LOCKSCREEN during non-LS doze entry; that must not re-arm 340→AOD.
        boolean fromLockscreenEntry = lockscreenToAodWeight
                && (hostOnLockscreen || fromLockscreenSmall || wasPreparingAodWeight
                || crossfadeFromLockscreen
                || (enteringAod && scene.isLockscreen()));
        boolean weightBusy = aodLayer.isClockPluginWeightTransitionRunning();
        int weightStart;
        boolean needWeightMorph;
        if (fromLockscreenEntry) {
            // Single owner of LS→AOD weight morph: AOD layer. Park at live LS weight.
            weightStart = handoffWeight > 0 ? handoffWeight : configuredLsWeight;
            if (weightStart <= 0) {
                weightStart = configuredLsWeight;
            }
            int lsLive = lockscreenLayer.clockPluginWeight();
            if (lsLive > 0) {
                weightStart = lsLive;
            }
            aodLayer.clearAodWeightHandoffSettled(source + "#from-lockscreen-entry");
            needWeightMorph = Math.abs(weightStart - aodTargetWeight) > 8;
        } else {
            // Non-lockscreen screen-off: stable AOD weight only — never park at LS 340.
            weightStart = aodTargetWeight;
            needWeightMorph = false;
        }
        PixelAodLog.log("presentAod weightStart source=" + source
                + " weightStart=" + weightStart
                + " configuredLs=" + configuredLsWeight
                + " aodTarget=" + aodTargetWeight
                + " needWeightMorph=" + needWeightMorph
                + " weightBusy=" + weightBusy
                + " handoffWeight=" + handoffWeight
                + " fromLockscreenEntry=" + fromLockscreenEntry
                + " informationWeightTracksClock=true"
                + " lockscreenToAodWeight=" + lockscreenToAodWeight
                + " screenOffFromLs="
                + PixelAodClockView.wasScreenOffFromInteractiveLockscreen()
                + " hostOnLockscreen=" + hostOnLockscreen
                + " hostScene=" + scene
                + " trace=" + PixelAodClockView.currentAodTraceId());
        // Only the explicit compact-to-large clock switch needs a source geometry snapshot.
        // Normal lockscreen <-> AOD handoff must not translate independently laid-out glyphs.
        AodGeometryHandoff.Snapshot compactMorphGeometry = morphFromCompact
                ? lockscreenLayer.snapshotClockPluginTextCentersOnScreen()
                : AodGeometryHandoff.Snapshot.EMPTY;
        lockscreenLayer.cancelClockPluginWeightTransitionKeepCurrent(
                source + "#transfer-weight-to-aod");
        boolean fallbackWholeViewMorph = morphFromCompact && sizeTransition == null;
        aodLayer.setClockPluginGlyphTransitionActive(sizeTransition != null);
        try {
            if (fromLockscreenEntry) {
                aodLayer.presentClockPluginAod(aodCompact, /*deferWeight*/ true, weightStart,
                        source + "#ClockPlugin-host", fallbackWholeViewMorph);
            } else {
                // deferWeight=false → applyStableAodClockWeight (target AOD weight, settled).
                aodLayer.presentClockPluginAod(aodCompact, /*deferWeight*/ false, aodTargetWeight,
                        source + "#ClockPlugin-host-direct-aod", false);
            }
        } finally {
            aodLayer.setClockPluginGlyphTransitionActive(false);
        }
        boolean aodMediaReady = aodLayer.hasVisibleMediaLine();

        if (forceLargeAodSurface) {
            cancelAodEntry();
            long generation = ++entryGeneration;
            // Fully visible AOD + weight morph immediately (no alpha-0 hide of morph).
            aodLayer.setAlpha(1f);
            aodLayer.setClockPluginLayerVisible(true);
            if (fallbackWholeViewMorph) {
                // Fallback only when glyph geometry could not be captured.
                aodLayer.startCompactToLargeEntryMorph(compactMorphGeometry,
                        SIZE_MORPH_MILLIS, SIZE_MORPH_INTERPOLATOR,
                        source + "#coui-compact-to-large");
            }
            startAodWeightMorphIfNeeded(needWeightMorph, weightStart,
                    source + "#aod-large-weight");
            // Size morph steals the handoff visual — skip while weight morph runs.
            lockscreenLayer.animate().cancel();
            lockscreenLayer.setAlpha(1f);
            lockscreenLayer.setClockPluginLayerVisible(false);
            scheduleAodMediaRetries(generation, source + "#aod-large-surface");
            PixelAodLog.log("ClockPlugin AOD large surface active source=" + source
                    + " weightStart=" + weightStart
                    + " needWeightMorph=" + needWeightMorph
                    + " weightRunning=" + aodLayer.isClockPluginWeightTransitionRunning()
                    + " aodMediaReady=" + aodLayer.hasVisibleMediaLine()
                    + " generation=" + generation
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            startSizeTransitionAfterLayout(sizeTransition);
            return;
        }

        PixelAodLog.log("ClockPlugin layer snapshot source=" + source
                + " enteringAod=" + enteringAod
                + " crossfade=" + crossfadeFromLockscreen
                + " aodCompact=" + aodCompact
                + " aodMediaReady=" + aodMediaReady
                + " needWeightMorph=" + needWeightMorph
                + " weightStart=" + weightStart
                + " lockscreen={visibility=" + lockscreenLayer.getVisibility()
                + ",alpha=" + lockscreenLayer.getAlpha()
                + ",weight=" + lockscreenLayer.clockPluginWeight() + "}"
                + " aod={visibility=" + aodLayer.getVisibility()
                + ",alpha=" + aodLayer.getAlpha()
                + ",weight=" + aodLayer.clockPluginWeight() + "}"
                + " trace=" + PixelAodClockView.currentAodTraceId());

        if (!crossfadeFromLockscreen) {
            cancelAodEntry();
            long generation = ++entryGeneration;
            lockscreenLayer.animate().cancel();
            lockscreenLayer.setAlpha(1f);
            lockscreenLayer.setClockPluginLayerVisible(false);
            aodLayer.setAlpha(1f);
            aodLayer.setClockPluginLayerVisible(true);
            if (needWeightMorph) {
                startAodWeightMorphIfNeeded(true, weightStart,
                        source + "#aod-direct-weight");
            } else if (!fromLockscreenEntry) {
                // Ensure settled AOD weight even if present re-entered with a residual park.
                aodLayer.applyClockPluginStableAodWeight(source + "#non-lockscreen-direct");
            }
            scheduleAodMediaRetries(generation, source + "#aod-direct");
            startSizeTransitionAfterLayout(sizeTransition);
            return;
        }

        // Compact: AOD fully opaque + weight morph first; only fade out lockscreen.
        cancelAodEntry();
        long generation = ++entryGeneration;
        lockscreenLayer.animate().cancel();
        scheduleAodEntryCrossfade(generation, source, aodMediaReady, weightStart,
                needWeightMorph);
        startSizeTransitionAfterLayout(sizeTransition);
    }

    private SizeTransitionRequest prepareSizeTransition(ClockPluginSceneMachine.Scene target,
            String source) {
        boolean sceneRequestsSizeChange = isClockSizeChange(scene, target);
        if (!sceneRequestsSizeChange || getWidth() <= 0 || getHeight() <= 0) {
            return null;
        }
        if (sizeTransitionLayer.hasActiveTransition()) {
            cancelSizeTransition("superseded-size-transition");
        }
        long generation = ++sizeTransitionGeneration;
        CouiClockSizeTransitionLayer.SceneSnapshot snapshot = captureSceneSnapshot(scene);
        Boolean targetCompact = compactScene(target);
        if (snapshot == null || !snapshot.valid() || targetCompact == null) {
            return null;
        }
        if (!CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                sceneRequestsSizeChange, snapshot.compact, targetCompact)) {
            PixelAodLog.log("skipped COUI per-glyph size transaction actual-size no-op source="
                    + source + " fromCompact=" + snapshot.compact
                    + " toCompact=" + targetCompact
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return null;
        }
        if (!sizeTransitionLayer.prepare(snapshot, source + "#glyph-source")) {
            return null;
        }
        if (!sizeTransitionLayer.ownsPreparedSourceFrame()) {
            sizeTransitionLayer.cancelAndRestore("prepared-source-ownership-lost");
            return null;
        }
        return new SizeTransitionRequest(target, source + "#glyph-target", generation);
    }

    private void startSizeTransitionAfterLayout(SizeTransitionRequest request) {
        if (request == null) {
            return;
        }
        Runnable start = () -> {
            if (request.generation != sizeTransitionGeneration) {
                return;
            }
            if (scene != request.target) {
                cancelSizeTransition("stale-target-scene");
                return;
            }
            CouiClockSizeTransitionLayer.SceneSnapshot targetSnapshot =
                    captureSceneSnapshot(request.target);
            if (targetSnapshot == null || !targetSnapshot.valid()) {
                cancelSizeTransition("target-geometry-unavailable");
                return;
            }
            CouiClockSizeTransitionLayer.WeightProvider weightProvider =
                    weightProviderFor(request.target);
            sizeTransitionLayer.start(targetSnapshot, weightProvider, SIZE_MORPH_MILLIS,
                    SIZE_MORPH_INTERPOLATOR, request.source);
        };
        ViewTreeObserver observer = getViewTreeObserver();
        if (!observer.isAlive()) {
            postOnAnimation(start);
            return;
        }
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver current = getViewTreeObserver();
                if (current.isAlive()) {
                    current.removeOnPreDrawListener(this);
                }
                start.run();
                return true;
            }
        });
        requestLayout();
        invalidate();
    }

    private void cancelSizeTransition(String reason) {
        sizeTransitionGeneration++;
        sizeTransitionLayer.cancelAndRestore(reason);
    }

    private CouiClockSizeTransitionLayer.SceneSnapshot captureSceneSnapshot(
            ClockPluginSceneMachine.Scene sourceScene) {
        if (sourceScene != null && sourceScene.isLockscreen()) {
            return lockscreenLayer.captureClockPluginSizeTransition(sizeTransitionLayer, this);
        }
        if (sourceScene != null && sourceScene.isAod()) {
            return aodLayer.captureClockPluginSizeTransition(sizeTransitionLayer, this);
        }
        return CouiClockSizeTransitionLayer.SceneSnapshot.EMPTY;
    }

    private CouiClockSizeTransitionLayer.WeightProvider weightProviderFor(
            ClockPluginSceneMachine.Scene target) {
        if (target != null && target.isAod()) {
            return new CouiClockSizeTransitionLayer.WeightProvider() {
                @Override
                public int clockWeight() {
                    return aodLayer.clockPluginWeight();
                }

                @Override
                public int infoWeight() {
                    return aodLayer.clockPluginInfoWeight();
                }
            };
        }
        return new CouiClockSizeTransitionLayer.WeightProvider() {
            @Override
            public int clockWeight() {
                return lockscreenLayer.clockPluginWeight();
            }

            @Override
            public int infoWeight() {
                return lockscreenLayer.clockPluginInfoWeight();
            }
        };
    }

    private static boolean isClockSizeChange(ClockPluginSceneMachine.Scene from,
            ClockPluginSceneMachine.Scene to) {
        Boolean fromCompact = compactScene(from);
        Boolean toCompact = compactScene(to);
        // A lockscreen/AOD handoff changes coordinate and layer ownership; let the existing
        // handoff render that change instead of overlaying a glyph transaction across it.
        return fromCompact != null && toCompact != null
                && CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                from.isLockscreen(), to.isLockscreen(), fromCompact, toCompact);
    }

    private static Boolean compactScene(ClockPluginSceneMachine.Scene value) {
        if (value == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL
                || value == ClockPluginSceneMachine.Scene.AOD_SMALL) {
            return Boolean.TRUE;
        }
        if (value == ClockPluginSceneMachine.Scene.LOCKSCREEN_LARGE
                || value == ClockPluginSceneMachine.Scene.AOD_LARGE) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static final class SizeTransitionRequest {
        final ClockPluginSceneMachine.Scene target;
        final String source;
        final long generation;

        SizeTransitionRequest(ClockPluginSceneMachine.Scene target, String source,
                long generation) {
            this.target = target;
            this.source = source;
            this.generation = generation;
        }
    }

    private void startAodWeightMorphIfNeeded(boolean needWeightMorph, int fromWeight,
            String source) {
        if (!needWeightMorph) {
            return;
        }
        if (aodLayer.isClockPluginWeightTransitionRunning()) {
            return;
        }
        int aodTarget = PixelAodClockView.aodClockWeight(getContext());
        int now = aodLayer.clockPluginWeight();
        int from = fromWeight > 0 ? fromWeight : now;
        if (Math.abs(from - aodTarget) <= 8) {
            aodLayer.applyClockPluginStableAodWeight(source + "#already-at-target");
            return;
        }
        aodLayer.clearAodWeightHandoffSettled(source + "#ensure-unsettle");
        // Ensure parked at from before starting (continue mid early-aod, never jump to 340).
        if (Math.abs(now - from) > 8) {
            aodLayer.presentClockPluginAod(
                    aodLayer.isCompactClockMode(),
                    /*deferWeight*/ true,
                    from,
                    source + "#repark-from",
                    false);
        }
        aodLayer.startClockPluginAodWeightTransition(source);
    }

    private void scheduleAodMediaRetries(long generation, String source) {
        // The initial AOD presentation has already queried MediaSessionManager.  Keep the late
        // retries for players which publish a session after Doze begins, but do not run a second
        // full date/weather/notification refresh at frame zero.
        long[] delaysMs = new long[]{16L, 48L, 100L, 200L, 320L};
        for (long delayMs : delaysMs) {
            Runnable retry = () -> {
                if (generation != entryGeneration || !scene.isAod()) {
                    return;
                }
                if (aodLayer.hasVisibleMediaLine()) {
                    return;
                }
                aodLayer.refreshClockPluginAodMedia(source + "#media-retry+" + delayMs);
                if (aodLayer.hasVisibleMediaLine() && aodLayer.getAlpha() < 1f) {
                    aodLayer.setAlpha(1f);
                }
            };
            if (delayMs <= 0L) {
                post(retry);
            } else {
                postDelayed(retry, delayMs);
            }
        }
    }

    /**
     * Compact AOD entry: reveal AOD, then start weight morph so 340→target is fully on-screen.
     * Never re-park / applyStable here.
     */
    private void scheduleAodEntryCrossfade(long generation, String source,
            boolean aodMediaReady, int weightStartHint, boolean needWeightMorph) {
        Runnable start = () -> {
            if (generation != entryGeneration || !scene.isAod()) {
                return;
            }
            // Only cancel ViewPropertyAnimator alpha — not the weight ValueAnimator.
            aodLayer.animate().cancel();
            lockscreenLayer.animate().cancel();
            lockscreenLayer.cancelClockPluginWeightTransitionKeepCurrent(
                    source + "#crossfade-ls-weight-idle");

            aodLayer.refreshClockPluginAodContent(source + "#handoff-media-pre");
            boolean mediaReadyNow = aodLayer.hasVisibleMediaLine() || aodMediaReady;
            scheduleAodMediaRetries(generation, source + "#handoff");

            aodLayer.setClockPluginLayerVisible(true);
            // Start weight morph when AOD is shown so the full 700ms is visible (not during
            // grace while the layer was INVISIBLE / alpha 0 under the lockscreen).
            startAodWeightMorphIfNeeded(needWeightMorph, weightStartHint,
                    source + "#crossfade-weight");
            if (aodLayer.getAlpha() >= 0.99f) {
                aodLayer.setAlpha(0f);
            }
            aodLayer.animate()
                    .alpha(1f)
                    .setDuration(ENTRY_CROSSFADE_MILLIS)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .start();
            lockscreenLayer.animate()
                    .alpha(0f)
                    .setDuration(ENTRY_CROSSFADE_MILLIS)
                    .setInterpolator(new DecelerateInterpolator(1.4f))
                    .withEndAction(() -> {
                        if (generation == entryGeneration && scene.isAod()) {
                            lockscreenLayer.setAlpha(1f);
                            lockscreenLayer.setClockPluginLayerVisible(false);
                        }
                    })
                    .start();
            PixelAodLog.log("started persistent ClockPlugin AOD handoff source=" + source
                    + " generation=" + generation
                    + " weightWaitMs=0"
                    + " aodMediaReady=" + mediaReadyNow
                    + " needWeightMorph=" + needWeightMorph
                    + " informationWeightTracksClock=true"
                    + " weightStartHint=" + weightStartHint
                    + " aodWeightNow=" + aodLayer.clockPluginWeight()
                    + " aodTarget=" + PixelAodClockView.aodClockWeight(getContext())
                    + " weightRunning=" + aodLayer.isClockPluginWeightTransitionRunning()
                    + " mediaVisible=" + aodLayer.hasVisibleMediaLine()
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        };
        finishAodEntryRunnable = start;
        postOnAnimation(start);
    }

    private void cancelAodEntry() {
        entryGeneration++;
        if (finishAodEntryRunnable != null) {
            removeCallbacks(finishAodEntryRunnable);
            finishAodEntryRunnable = null;
        }
    }

    private void armHandoffDiagnostics(String source) {
        if (!PixelAodLog.isDebugEnabled()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        String trace = PixelAodClockView.currentAodTraceId();
        if (handoffDiagnosticFramesRemaining > 0
                && now - handoffDiagnosticStartedAt <= HANDOFF_DIAGNOSTIC_WINDOW_MILLIS) {
            return;
        }
        handoffDiagnosticFramesRemaining = HANDOFF_DIAGNOSTIC_FRAME_LIMIT;
        handoffDiagnosticFrame = 0;
        handoffDiagnosticStartedAt = now;
        handoffDiagnosticSource = source;
        handoffDiagnosticTrace = trace;
        PixelAodLog.log(HANDOFF_DIAGNOSTIC_PREFIX + " armed source=" + source
                + " scene=" + scene + " trace=" + trace);
        invalidate();
    }

    private void logHandoffDiagnosticFrame() {
        if (handoffDiagnosticFramesRemaining <= 0 || !PixelAodLog.isDebugEnabled()) {
            return;
        }
        int frame = ++handoffDiagnosticFrame;
        long elapsed = SystemClock.uptimeMillis() - handoffDiagnosticStartedAt;
        if (elapsed > HANDOFF_DIAGNOSTIC_WINDOW_MILLIS) {
            handoffDiagnosticFramesRemaining = 0;
            PixelAodLog.log(HANDOFF_DIAGNOSTIC_PREFIX + " expired frame=" + frame
                    + " elapsedMs=" + elapsed
                    + " source=" + handoffDiagnosticSource
                    + " trace=" + handoffDiagnosticTrace);
            return;
        }
        handoffDiagnosticFramesRemaining--;
        String framePrefix = HANDOFF_DIAGNOSTIC_PREFIX
                + " frame=" + frame + "/" + HANDOFF_DIAGNOSTIC_FRAME_LIMIT
                + " elapsedMs=" + elapsed
                + " source=" + handoffDiagnosticSource
                + " scene=" + scene
                + " trace=" + handoffDiagnosticTrace;
        PixelAodLog.log("handoff-frame-layers", () -> framePrefix
                + " section=layers host=" + PixelAodClockView.describeViewForHandoff(this)
                + " lockscreen=" + lockscreenLayer.clockPluginDiagnosticState()
                + " aod=" + aodLayer.clockPluginDiagnosticState());
        PixelAodLog.log("handoff-frame-ancestors",
                () -> framePrefix + " section=ancestors value=" + describeAncestorChain());
        PixelAodLog.log("handoff-frame-root",
                () -> framePrefix + " section=root value=" + describeClockPluginRoot());
    }

    private String describeAncestorChain() {
        StringBuilder result = new StringBuilder("[");
        View current = this;
        for (int depth = 0; depth < HANDOFF_DIAGNOSTIC_ANCESTOR_LIMIT
                && current != null; depth++) {
            if (depth > 0) {
                result.append(';');
            }
            result.append(depth).append(':')
                    .append(PixelAodClockView.describeViewForHandoff(current));
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return result.append(']').toString();
    }

    private String describeClockPluginRoot() {
        ViewParent parent = getParent();
        if (!(parent instanceof ViewGroup)) {
            return "none";
        }
        ViewGroup root = (ViewGroup) parent;
        StringBuilder result = new StringBuilder("{children=")
                .append(root.getChildCount())
                .append(",nativeTime=")
                .append(PixelAodClockView.describeViewForHandoff(
                        root.findViewById(VIEW_CLOCK_TIME)))
                .append(",nativeDate=")
                .append(PixelAodClockView.describeViewForHandoff(
                        root.findViewById(VIEW_DATE_MESSAGE)))
                .append(",direct=[");
        int childCount = Math.min(root.getChildCount(), HANDOFF_DIAGNOSTIC_CHILD_LIMIT);
        for (int index = 0; index < childCount; index++) {
            if (index > 0) {
                result.append(';');
            }
            result.append(index).append(':')
                    .append(PixelAodClockView.describeViewForHandoff(root.getChildAt(index)));
        }
        if (root.getChildCount() > childCount) {
            result.append(";truncated=").append(root.getChildCount() - childCount);
        }
        return result.append("]}").toString();
    }

    private void notifyAfterPresentationFrame() {
        if (firstPresentationFrameCallback == null || !isAttachedToWindow()) {
            return;
        }
        ViewTreeObserver observer = getViewTreeObserver();
        if (!observer.isAlive()) {
            return;
        }
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver current = getViewTreeObserver();
                if (current.isAlive()) {
                    current.removeOnPreDrawListener(this);
                }
                Runnable callback = firstPresentationFrameCallback;
                firstPresentationFrameCallback = null;
                if (callback != null) {
                    postOnAnimation(callback);
                }
                return true;
            }
        });
    }
}
