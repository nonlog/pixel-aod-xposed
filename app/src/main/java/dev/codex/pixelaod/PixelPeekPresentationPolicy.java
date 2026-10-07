package dev.codex.pixelaod;

final class PixelPeekPresentationPolicy {
    private static final String ALL_DAY = "all-day";
    private static final String SCHEDULED = "scheduled";

    private PixelPeekPresentationPolicy() {
    }

    static boolean shouldUsePixelPeek(boolean customEnabled, boolean nativePeekEnabled) {
        return customEnabled && nativePeekEnabled;
    }

    static boolean shouldSuppressNativeDraw(boolean customEnabled, boolean nativePeekEnabled,
            boolean hasSafeContent, boolean pixelOverlayAttached) {
        return shouldUsePixelPeek(customEnabled, nativePeekEnabled)
                && hasSafeContent && pixelOverlayAttached;
    }

    static boolean shouldPreserveContinuousAod(boolean nativeWindowAttached,
            boolean configuredEligible, String displayMode, boolean scheduleWindowEligible,
            boolean alwaysOnSuppressed) {
        if (!nativeWindowAttached || !configuredEligible || alwaysOnSuppressed) {
            return false;
        }
        if (ALL_DAY.equals(displayMode)) {
            return true;
        }
        return SCHEDULED.equals(displayMode) && scheduleWindowEligible;
    }
}
