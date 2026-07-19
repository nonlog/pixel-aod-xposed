package dev.codex.pixelaod;

import android.content.Context;
import android.view.View;
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

    private final PixelLockscreenClockView lockscreenLayer;
    private final PixelAodClockView aodLayer;
    private ClockPluginSceneMachine.Scene scene = ClockPluginSceneMachine.Scene.HIDDEN;
    private Runnable finishAodEntryRunnable;
    private long entryGeneration;
    private Runnable firstPresentationFrameCallback;
    private boolean preparingAodWeight;

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
        super.onDetachedFromWindow();
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
        int handoffWeight = lockscreenLayer.clockPluginWeight();
        preparingAodWeight = false;

        aodLayer.animate().cancel();
        aodLayer.setAlpha(crossfadeFromLockscreen ? 0f : 1f);
        // Prepare the hidden AOD layer at the lockscreen weight. Starting its animator here
        // would consume the visible transition while alpha is still zero.
        aodLayer.presentClockPluginAod(target == ClockPluginSceneMachine.Scene.AOD_SMALL,
                crossfadeFromLockscreen, handoffWeight, source + "#ClockPlugin-host");

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
            // Both layers advance on the same frame, so the visible lockscreen glyphs remain
            // continuous until the prepared AOD layer takes over.
            lockscreenLayer.beginClockPluginAodWeightTransition(source + "#lockscreen-handoff");
            aodLayer.startClockPluginAodWeightTransition(source + "#aod-handoff", () -> {
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
            });
            PixelAodLog.log("started persistent ClockPlugin AOD handoff source=" + source
                    + " generation=" + generation
                    + " waitingForWeight=true"
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
