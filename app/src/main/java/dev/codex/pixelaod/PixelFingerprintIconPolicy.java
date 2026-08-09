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

    static boolean shouldShowNativePressedLayer(boolean fingerDown) {
        return fingerDown;
    }

    static boolean isCompetingDrawableClass(String className) {
        return className != null && className.startsWith(COUI_DRAWABLE_PACKAGE);
    }

    static boolean shouldUsePixelIcon(boolean enabled, String currentDrawableClass) {
        return enabled;
    }

    enum RefreshMode {
        SKIP,
        STYLE_EXISTING_NATIVE,
        REFRESH_CARRIER
    }

    static RefreshMode refreshMode(boolean interactive, boolean modulePolicyAllowsDisplay,
            boolean powerPolicyDenied, boolean nativeCarrierVisible) {
        if (interactive || modulePolicyAllowsDisplay || !powerPolicyDenied) {
            return RefreshMode.REFRESH_CARRIER;
        }
        return nativeCarrierVisible ? RefreshMode.STYLE_EXISTING_NATIVE : RefreshMode.SKIP;
    }

    static boolean shouldRefreshAfterAodPolicy(boolean interactive,
            boolean modulePolicyAllowsDisplay, boolean powerPolicyDenied) {
        // While the screen is non-interactive, a power-policy denial hands steady carrier
        // visibility back to the vendor hide lifecycle. Do not reapply our carrier from a
        // delayed FOD callback, but keep interactive and normally allowed AOD refreshes.
        return refreshMode(interactive, modulePolicyAllowsDisplay, powerPolicyDenied, false)
                == RefreshMode.REFRESH_CARRIER;
    }
}
