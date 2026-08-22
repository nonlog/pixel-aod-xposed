package dev.codex.pixelaod;

/**
 * Pure policy for suppressing the native OPlus AOD notification icon row while Pixel owns the
 * persistent ambient presentation. Keeping this decision separate makes the hot onDraw hook cheap
 * and prevents it from becoming another lifecycle authority.
 */
final class NativeAodNotificationDrawPolicy {
    private NativeAodNotificationDrawPolicy() {
    }

    static boolean shouldSuppress(boolean exactOplusAodNotificationView,
            boolean hasAodAncestor, boolean interactive, boolean pixelAodActive,
            boolean vendorAmbientSessionActive) {
        return exactOplusAodNotificationView
                && hasAodAncestor
                && !interactive
                && pixelAodActive
                && vendorAmbientSessionActive;
    }
}
