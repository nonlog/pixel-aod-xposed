package dev.codex.pixelaod;

final class PixelFingerprintIconPolicy {
    private static final String COUI_DRAWABLE_PACKAGE = "one.dot.couiexpressive.";

    private PixelFingerprintIconPolicy() {
    }

    enum DispatchTarget {
        VIEW_HANDLER,
        MAIN_DISCOVERY
    }

    static DispatchTarget dispatchTarget(boolean hasViewAnchor) {
        return hasViewAnchor ? DispatchTarget.VIEW_HANDLER : DispatchTarget.MAIN_DISCOVERY;
    }

    static int opaqueColor(int color) {
        return (color & 0x00ffffff) | 0xff000000;
    }

    static boolean useAodStyle(boolean onDozeState, boolean onDreamingStart,
            boolean screenTurnedOff) {
        return onDozeState || onDreamingStart || screenTurnedOff;
    }

    static boolean useAodStyle(boolean interactive, boolean onDozeState,
            boolean onDreamingStart, boolean screenTurnedOff) {
        return !interactive || useAodStyle(onDozeState, onDreamingStart, screenTurnedOff);
    }

    static float lockscreenLayerAlpha(float aodProgress) {
        return 1f - Math.max(0f, Math.min(1f, aodProgress));
    }

    static float lockscreenBackgroundAlpha(boolean primaryCarrier, float aodProgress) {
        return primaryCarrier ? lockscreenLayerAlpha(aodProgress) : 0f;
    }

    static boolean shouldReplaceCarrier(boolean primaryCarrier) {
        return primaryCarrier;
    }

    static boolean isCompetingDrawableClass(String className) {
        return className != null && className.startsWith(COUI_DRAWABLE_PACKAGE);
    }

    static boolean shouldUsePixelIcon(boolean enabled, String currentDrawableClass) {
        return enabled;
    }
}
