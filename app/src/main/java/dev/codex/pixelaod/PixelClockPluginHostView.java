package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
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
            presentLockscreen(target, source);
        } else if (target.isAod()) {
            presentAod(target, decision.enteringAod, source);
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
        aodLayer.animate().cancel();
        aodLayer.setAlpha(1f);
        aodLayer.setClockPluginLayerVisible(false);
        lockscreenLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        lockscreenLayer.presentClockPluginLockscreen(
                target == ClockPluginSceneMachine.Scene.LOCKSCREEN_SMALL,
                source + "#ClockPlugin-host");
        lockscreenLayer.setClockPluginLayerVisible(true);
    }

    private void presentAod(ClockPluginSceneMachine.Scene target, boolean enteringAod,
            String source) {
        setVisibility(View.VISIBLE);
        boolean crossfadeFromLockscreen = enteringAod && scene.isLockscreen()
                && lockscreenLayer.getVisibility() == View.VISIBLE;
        if (crossfadeFromLockscreen && !preparingAodWeight) {
            armHandoffDiagnostics(source + "#aod-scene");
        }
        int handoffWeight = lockscreenLayer.clockPluginWeight();
        preparingAodWeight = false;

        aodLayer.animate().cancel();
        aodLayer.setAlpha(crossfadeFromLockscreen ? 0f : 1f);
        // Prepare the hidden AOD layer at the lockscreen weight. Starting its animator here
        // would consume the visible transition while alpha is still zero.
        aodLayer.presentClockPluginAod(target == ClockPluginSceneMachine.Scene.AOD_SMALL,
                crossfadeFromLockscreen, handoffWeight, source + "#ClockPlugin-host");
        PixelAodLog.log("ClockPlugin layer snapshot source=" + source
                + " enteringAod=" + enteringAod
                + " crossfade=" + crossfadeFromLockscreen
                + " lockscreen={visibility=" + lockscreenLayer.getVisibility()
                + ",alpha=" + lockscreenLayer.getAlpha()
                + ",weight=" + lockscreenLayer.clockPluginWeight() + "}"
                + " aod={visibility=" + aodLayer.getVisibility()
                + ",alpha=" + aodLayer.getAlpha()
                + ",weight=" + aodLayer.clockPluginWeight() + "}"
                + " trace=" + PixelAodClockView.currentAodTraceId());

        if (!crossfadeFromLockscreen) {
            cancelAodEntry();
            lockscreenLayer.animate().cancel();
            lockscreenLayer.setAlpha(1f);
            lockscreenLayer.setClockPluginLayerVisible(false);
            return;
        }

        cancelAodEntry();
        long generation = ++entryGeneration;
        lockscreenLayer.animate().cancel();
        lockscreenLayer.setAlpha(1f);
        scheduleAodEntryCrossfade(generation, source);
    }

    private void scheduleAodEntryCrossfade(long generation, String source) {
        Runnable start = () -> {
            if (generation != entryGeneration || !scene.isAod()) {
                return;
            }
            aodLayer.animate().cancel();
            lockscreenLayer.animate().cancel();
            if (!lockscreenLayer.isClockPluginWeightTransitionRunning()) {
                lockscreenLayer.beginClockPluginAodWeightTransition(
                        source + "#lockscreen-handoff");
            }
            aodLayer.applyClockPluginStableAodWeight(source + "#aod-handoff");
            Runnable finish = () -> {
                if (generation != entryGeneration || !scene.isAod()) {
                    return;
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
                PixelAodLog.log("started persistent ClockPlugin layer handoff after weights source="
                        + source + " generation=" + generation
                        + " weight=" + PixelAodClockView.aodClockWeight()
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            };
            finishAodEntryRunnable = finish;
            postDelayed(finish, CLOCK_WEIGHT_HANDOFF_MILLIS);
            PixelAodLog.log("started persistent ClockPlugin AOD handoff source=" + source
                    + " generation=" + generation
                    + " waitingForWeight=true visibleLayer=lockscreen"
                    + " aodLayerWeight=stable"
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
