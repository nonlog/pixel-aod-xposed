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
    private static final long SIZE_MORPH_MILLIS = 380L;
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
    private ClockPluginSceneMachine.Scene scene = ClockPluginSceneMachine.Scene.HIDDEN;
    private Runnable finishAodEntryRunnable;
    private long entryGeneration;
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

        addView(lockscreenLayer, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(aodLayer, new FrameLayout.LayoutParams(
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
                // Media-only / empty: never run weight-only animation on lockscreen SMALL
                // (native media card already gone → "small clock, no media"). Stage AOD LARGE.
                if (!PixelAodClockView.hasCompactClockNotificationContent()) {
                    armHandoffDiagnostics(source + "#early-aod-large");
                    presentAod(ClockPluginSceneMachine.Scene.AOD_LARGE, true,
                            source + "#early-aod-large");
                    scene = ClockPluginSceneMachine.Scene.AOD_LARGE;
                    preparingAodWeight = false;
                    notifyAfterPresentationFrame();
                    return;
                }
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
        lockscreenLayer.animate().cancel();
        aodLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        aodLayer.setAlpha(1f);
        lockscreenLayer.setClockPluginLayerVisible(false);
        aodLayer.setClockPluginLayerVisible(false);
        setVisibility(View.INVISIBLE);
        preparingAodWeight = false;
        scene = ClockPluginSceneMachine.Scene.HIDDEN;
        PixelAodLog.log("hid persistent ClockPlugin host source=" + source
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private void presentLockscreen(ClockPluginSceneMachine.Scene target, String source) {
        cancelAodEntry();
        preparingAodWeight = false;
        setVisibility(View.VISIBLE);
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
        lockscreenLayer.presentClockPluginLockscreen(
                target == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL,
                source + "#ClockPlugin-host",
                fromWeight,
                animateFromAod);
        lockscreenLayer.setClockPluginLayerVisible(true);
    }

    private void presentAod(ClockPluginSceneMachine.Scene target, boolean enteringAod,
            String source) {
        presentAod(target, enteringAod, source, scene == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL);
    }

    private void presentAod(ClockPluginSceneMachine.Scene target, boolean enteringAod,
            String source, boolean fromLockscreenSmall) {
        setVisibility(View.VISIBLE);
        boolean fromLockscreen = scene.isLockscreen()
                && lockscreenLayer.getVisibility() == View.VISIBLE;
        boolean crossfadeFromLockscreen = enteringAod && fromLockscreen;
        if (crossfadeFromLockscreen && !preparingAodWeight) {
            armHandoffDiagnostics(source + "#aod-scene");
        }
        int handoffWeight = lockscreenLayer.clockPluginWeight();
        preparingAodWeight = false;
        boolean aodCompact = target == ClockPluginSceneMachine.Scene.AOD_SMALL;
        // Media-only / empty must never stay on lockscreen SMALL after screen-off.
        boolean forceLargeAodSurface = !aodCompact
                && (crossfadeFromLockscreen || fromLockscreen || fromLockscreenSmall);

        aodLayer.animate().cancel();
        aodLayer.animate().setListener(null);
        // Atomic prepare: large/compact + media before any surface switch.
        // Do not touch platform black-frame / power path.
        boolean morphFromCompact = forceLargeAodSurface && fromLockscreenSmall;
        // Park at lockscreen weight only for a fresh handoff. If AOD weight already settled
        // (re-present from non-lockscreen-reveal), do not re-park at 340 (that is the bounce).
        int configuredLsWeight = PixelAodClockView.lockscreenClockWeight(getContext());
        int aodTargetWeight = PixelAodClockView.aodClockWeight(getContext());
        int weightStart = handoffWeight > 0 ? handoffWeight : configuredLsWeight;
        boolean fromLockscreenEntry = fromLockscreen || fromLockscreenSmall || crossfadeFromLockscreen;
        boolean weightBusy = aodLayer.isClockPluginWeightTransitionRunning();
        boolean alreadySettled = aodLayer.isAodWeightHandoffSettled();
        // AOD surface already showing settled weight: never clear latch / force park at 340.
        // presentLockscreen() is the only path that must clear settle for the next LS→AOD.
        boolean aodSurfaceActive = aodLayer.getVisibility() == View.VISIBLE
                && aodLayer.getAlpha() > 0.01f;
        if (fromLockscreenEntry && !weightBusy && alreadySettled && !aodSurfaceActive) {
            aodLayer.clearAodWeightHandoffSettled(source + "#from-lockscreen-entry");
            alreadySettled = false;
        } else if (fromLockscreenEntry && alreadySettled && aodSurfaceActive) {
            PixelAodLog.log("kept AOD weight settle on from-lockscreen re-present source=" + source
                    + " aodWeight=" + aodLayer.clockPluginWeight()
                    + " aodTarget=" + aodTargetWeight
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
        // Only rewrite start→LS weight for a genuine unparked handoff. Doing this while
        // settled/busy was a bounce source (weightStart forced 340 after finished 160).
        if (!alreadySettled && !weightBusy
                && Math.abs(weightStart - aodTargetWeight) <= 8) {
            weightStart = configuredLsWeight;
        }
        aodLayer.presentClockPluginAod(aodCompact, /*deferWeight*/ true, weightStart,
                source + "#ClockPlugin-host", morphFromCompact && !alreadySettled && !weightBusy);
        aodLayer.refreshClockPluginAodContent(source + "#ClockPlugin-host-media");
        boolean aodMediaReady = aodLayer.hasVisibleMediaLine();

        if (forceLargeAodSurface) {
            cancelAodEntry();
            long generation = ++entryGeneration;
            // Active surface = AOD LARGE (+ media). Hide lockscreen SMALL immediately so
            // pre-blank frames are not "small clock, no media". Always run weight morph
            // unless this is a re-present after weight already settled at AOD target.
            aodLayer.setAlpha(1f);
            aodLayer.setClockPluginLayerVisible(true);
            if (!aodLayer.isAodWeightHandoffSettled()
                    && !aodLayer.isClockPluginWeightTransitionRunning()) {
                aodLayer.startClockPluginAodWeightTransition(source + "#aod-large-weight");
                if (morphFromCompact) {
                    aodLayer.startCompactToLargeEntryMorph(CLOCK_WEIGHT_HANDOFF_MILLIS,
                            SIZE_MORPH_INTERPOLATOR, source + "#size-morph");
                }
            }
            aodLayer.refreshClockPluginAodContent(source + "#aod-large-surface-media");
            lockscreenLayer.animate().cancel();
            lockscreenLayer.setAlpha(1f);
            lockscreenLayer.setClockPluginLayerVisible(false);
            scheduleAodMediaRetries(generation, source + "#aod-large-surface");
            PixelAodLog.log("ClockPlugin AOD large surface active source=" + source
                    + " morphFromCompact=" + morphFromCompact
                    + " weightStart=" + weightStart
                    + " weightSettled=" + aodLayer.isAodWeightHandoffSettled()
                    + " weightRunning=" + aodLayer.isClockPluginWeightTransitionRunning()
                    + " aodMediaReady=" + aodLayer.hasVisibleMediaLine()
                    + " generation=" + generation
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            return;
        }

        aodLayer.setAlpha(1f);
        PixelAodLog.log("ClockPlugin layer snapshot source=" + source
                + " enteringAod=" + enteringAod
                + " crossfade=" + crossfadeFromLockscreen
                + " aodCompact=" + aodCompact
                + " aodMediaReady=" + aodMediaReady
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
            scheduleAodMediaRetries(generation, source + "#aod-direct");
            return;
        }

        // Compact AOD (has notifications): crossfade while AOD layer owns the weight morph.
        cancelAodEntry();
        long generation = ++entryGeneration;
        lockscreenLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        scheduleAodEntryCrossfade(generation, source, aodMediaReady, weightStart);
    }

    private void scheduleAodMediaRetries(long generation, String source) {
        // Earlier denser retries so media row is filled before/during first AOD frames.
        long[] delaysMs = new long[]{0L, 16L, 48L, 100L, 200L, 320L};
        for (long delayMs : delaysMs) {
            Runnable retry = () -> {
                if (generation != entryGeneration || !scene.isAod()) {
                    return;
                }
                aodLayer.refreshClockPluginAodContent(source + "#media-retry+" + delayMs);
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
     * Compact AOD entry: transfer weight morph to the AOD layer (visible after crossfade),
     * prepare media first, and never stable-snap AOD weight while morph should still run.
     */
    private void scheduleAodEntryCrossfade(long generation, String source,
            boolean aodMediaReady, int weightStartHint) {
        Runnable start = () -> {
            if (generation != entryGeneration || !scene.isAod()) {
                return;
            }
            aodLayer.animate().cancel();
            lockscreenLayer.animate().cancel();

            int aodTarget = PixelAodClockView.aodClockWeight(getContext());
            int lsWeight = PixelAodClockView.lockscreenClockWeight(getContext());
            // Prefer live lockscreen weight (may be mid early-aod-weight morph).
            int fromWeight = lockscreenLayer.clockPluginWeight();
            if (fromWeight <= 0) {
                fromWeight = weightStartHint > 0 ? weightStartHint : lsWeight;
            }
            // Transfer morph ownership to AOD layer so the user sees 340→target on the
            // layer that remains after crossfade (logs: LS finished 160, AOD still 340,
            // then applied stable 160 = snap).
            lockscreenLayer.cancelClockPluginWeightTransitionKeepCurrent(
                    source + "#transfer-weight-to-aod");
            boolean needAodWeightMorph = Math.abs(fromWeight - aodTarget) > 8
                    && !aodLayer.isAodWeightHandoffSettled();
            if (needAodWeightMorph) {
                aodLayer.clearAodWeightHandoffSettled(source + "#crossfade-weight");
                aodLayer.presentClockPluginAod(
                        scene == ClockPluginSceneMachine.Scene.AOD_SMALL,
                        /*deferWeight*/ true,
                        fromWeight,
                        source + "#crossfade-weight-park",
                        false);
                aodLayer.startClockPluginAodWeightTransition(source + "#crossfade-aod-weight");
            } else if (!aodLayer.isAodWeightHandoffSettled()) {
                aodLayer.applyClockPluginStableAodWeight(source + "#crossfade-already-aod");
            }

            // Fill media before revealing AOD layer.
            aodLayer.refreshClockPluginAodContent(source + "#handoff-media-pre");
            boolean mediaReadyNow = aodLayer.hasVisibleMediaLine() || aodMediaReady;
            scheduleAodMediaRetries(generation, source + "#handoff");

            // Crossfade immediately so weight morph is visible on the AOD layer.
            aodLayer.setClockPluginLayerVisible(true);
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
                    + " needAodWeightMorph=" + needAodWeightMorph
                    + " fromWeight=" + fromWeight
                    + " aodTarget=" + aodTarget
                    + " visibleLayer=crossfade"
                    + " aodLayerWeight=" + (needAodWeightMorph ? "animating" : "stable")
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
        PixelAodLog.log(framePrefix
                + " section=layers host=" + PixelAodClockView.describeViewForHandoff(this)
                + " lockscreen=" + lockscreenLayer.clockPluginDiagnosticState()
                + " aod=" + aodLayer.clockPluginDiagnosticState());
        PixelAodLog.log(framePrefix + " section=ancestors value=" + describeAncestorChain());
        PixelAodLog.log(framePrefix + " section=root value=" + describeClockPluginRoot());
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
