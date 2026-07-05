package dev.codex.pixelaod;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;
import java.util.WeakHashMap;

final class StockAodVisibilityController {
    private static final Map<View, HiddenState> HIDDEN_STOCK_VIEWS = new WeakHashMap<>();
    private static final Map<View, AdjustedState> ADJUSTED_STATUS_VIEWS = new WeakHashMap<>();

    private StockAodVisibilityController() {
    }

    static void hideView(View view, String marker, boolean preserveSystemAodMediaView) {
        if (view == null) {
            return;
        }
        if (preserveSystemAodMediaView) {
            PixelAodLog.log("preserved system AOD media view " + marker
                    + " trace=" + PixelAodClockView.currentAodTraceId()
                    + " state={" + PixelAodClockView.describeAodState(view.getContext()) + "}");
            return;
        }
        boolean firstHide = false;
        synchronized (HIDDEN_STOCK_VIEWS) {
            if (!HIDDEN_STOCK_VIEWS.containsKey(view)) {
                HIDDEN_STOCK_VIEWS.put(view,
                        new HiddenState(view.getVisibility(), view.getAlpha()));
                firstHide = true;
            }
        }
        view.setAlpha(0f);
        view.setVisibility(View.GONE);
        if (firstHide) {
            PixelAodLog.log("hid stock AOD view " + marker);
        }
    }

    static void rememberAdjustedState(View view) {
        if (view == null) {
            return;
        }
        synchronized (ADJUSTED_STATUS_VIEWS) {
            if (!ADJUSTED_STATUS_VIEWS.containsKey(view)) {
                ADJUSTED_STATUS_VIEWS.put(view, new AdjustedState(
                        view.getTranslationX(),
                        view.getTranslationY(),
                        view.getTranslationZ(),
                        view.getAlpha(),
                        view.getLayerType()));
            }
        }
    }

    static void restoreHiddenStockViews() {
        synchronized (HIDDEN_STOCK_VIEWS) {
            for (Map.Entry<View, HiddenState> entry : HIDDEN_STOCK_VIEWS.entrySet()) {
                View view = entry.getKey();
                HiddenState state = entry.getValue();
                if (view != null && state != null) {
                    try {
                        view.setVisibility(state.visibility);
                        view.setAlpha(state.alpha);
                    } catch (Throwable t) {
                        PixelAodLog.log("restore hidden stock AOD view failed", t);
                    }
                }
            }
            HIDDEN_STOCK_VIEWS.clear();
        }
        PixelAodLog.log("restored hidden stock AOD views");
    }

    static void restoreAdjustedStatusViews() {
        synchronized (ADJUSTED_STATUS_VIEWS) {
            for (Map.Entry<View, AdjustedState> entry : ADJUSTED_STATUS_VIEWS.entrySet()) {
                View view = entry.getKey();
                AdjustedState state = entry.getValue();
                if (view != null && state != null) {
                    try {
                        view.setTranslationX(state.translationX);
                        view.setTranslationY(state.translationY);
                        view.setTranslationZ(state.translationZ);
                        view.setAlpha(state.alpha);
                        view.setLayerType(state.layerType, null);
                    } catch (Throwable t) {
                        PixelAodLog.log("restore adjusted AOD status view failed", t);
                    }
                }
            }
            ADJUSTED_STATUS_VIEWS.clear();
        }
        PixelAodLog.log("restored adjusted AOD status views");
    }

    static void scheduleStockSuppressionReapply(Handler main, ViewGroup host, String source,
            long delayMillis, String expectedTrace, SuppressionPass pass, HostSummary hostSummary) {
        if (main == null) {
            return;
        }
        main.postDelayed(() -> {
            try {
                Context context = host != null ? host.getContext() : null;
                String currentTrace = PixelAodClockView.peekAodTraceId();
                if (!OosAodLifecycleAdapter.matchesExpectedTrace(expectedTrace, currentTrace)) {
                    PixelAodLog.log("skipped delayed stock AOD suppression from " + source
                            + "+" + delayMillis
                            + " reason=trace-mismatch expectedTrace=" + expectedTrace
                            + " currentTrace=" + currentTrace
                            + " host=" + summarize(hostSummary, host)
                            + " state={" + PixelAodClockView.describeAodState(context) + "}");
                    return;
                }
                if (host == null || PixelAodClockView.isDeviceInteractive(context)) {
                    return;
                }
                if (pass != null) {
                    pass.apply(context, host);
                }
                PixelAodLog.log("reapplied stock AOD suppression from " + source
                        + "+" + delayMillis + " children=" + host.getChildCount()
                        + " trace=" + currentTrace
                        + " expectedTrace=" + expectedTrace
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
            } catch (Throwable t) {
                PixelAodLog.log("delayed stock AOD suppression failed", t);
            }
        }, delayMillis);
    }

    static void scheduleRestoreAfterTransition(Handler main, String source, String expectedTrace,
            long delayMillis, HostLookup hosts, HostSummary hostSummary) {
        if (main == null) {
            return;
        }
        main.postDelayed(() -> {
            ViewGroup pixelHost = hosts != null ? hosts.pixelHost() : null;
            ViewGroup stockHost = hosts != null ? hosts.stockHost() : null;
            String currentTrace = PixelAodClockView.peekAodTraceId();
            if (!OosAodLifecycleAdapter.matchesExpectedTrace(expectedTrace, currentTrace)) {
                Context context = pixelHost != null ? pixelHost.getContext()
                        : stockHost != null ? stockHost.getContext() : null;
                String state = context != null ? PixelAodClockView.describeAodState(context)
                        : "context=null";
                PixelAodLog.log("skipped restoring stock AOD/keyguard views after transition from "
                        + source + " reason=trace-mismatch expectedTrace=" + expectedTrace
                        + " currentTrace=" + currentTrace
                        + " stockHost=" + summarize(hostSummary, stockHost)
                        + " pixelHost=" + summarize(hostSummary, pixelHost)
                        + " state={" + state + "}");
                return;
            }
            Context context = null;
            if (pixelHost != null) {
                context = pixelHost.getContext();
            } else if (stockHost != null) {
                context = stockHost.getContext();
            }
            OosAodLifecycleAdapter.AodPolicyDecision decision =
                    PixelAodClockView.evaluateAodPolicy(context, source + "#restore-guard");
            if (PixelLockscreenClockView.shouldShowOnLockscreen(context)
                    || decision.shouldApplyModuleAod) {
                PixelAodLog.log("kept stock AOD/keyguard views hidden after transition from "
                        + source + " stockHost=" + summarize(hostSummary, stockHost)
                        + " pixelHost=" + summarize(hostSummary, pixelHost)
                        + " trace=" + currentTrace
                        + " expectedTrace=" + expectedTrace
                        + " shouldDrawPixelOverlay=" + decision.shouldDrawPixelOverlay
                        + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                        + " shouldSuppressStockAodViews=" + decision.shouldSuppressStockAodViews
                        + " shouldAllowNativeHideCallbacks="
                        + decision.shouldAllowNativeHideCallbacks
                        + " reasons={draw=" + decision.drawReason
                        + ",stock=" + decision.stockSuppressionReason
                        + ",nativeHide=" + decision.nativeHideCallbackReason + "}"
                        + " state={" + PixelAodClockView.describeAodState(context) + "}");
                return;
            }
            PixelAodLog.log("restoring stock AOD/keyguard views after transition from " + source
                    + " stockHost=" + summarize(hostSummary, stockHost)
                    + " pixelHost=" + summarize(hostSummary, pixelHost)
                    + " trace=" + currentTrace
                    + " expectedTrace=" + expectedTrace
                    + " state={" + PixelAodClockView.describeAodState(context) + "}");
            restoreHiddenStockViews();
        }, delayMillis);
    }

    private static String summarize(HostSummary hostSummary, ViewGroup host) {
        return hostSummary != null ? hostSummary.summary(host) : String.valueOf(host);
    }

    interface SuppressionPass {
        void apply(Context context, ViewGroup host);
    }

    interface HostLookup {
        ViewGroup stockHost();

        ViewGroup pixelHost();
    }

    interface HostSummary {
        String summary(ViewGroup host);
    }

    private static final class HiddenState {
        final int visibility;
        final float alpha;

        HiddenState(int visibility, float alpha) {
            this.visibility = visibility;
            this.alpha = alpha;
        }
    }

    private static final class AdjustedState {
        final float translationX;
        final float translationY;
        final float translationZ;
        final float alpha;
        final int layerType;

        AdjustedState(float translationX, float translationY, float translationZ,
                float alpha, int layerType) {
            this.translationX = translationX;
            this.translationY = translationY;
            this.translationZ = translationZ;
            this.alpha = alpha;
            this.layerType = layerType;
        }
    }
}
