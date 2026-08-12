package dev.codex.pixelaod;

/**
 * Separates notification-cache updates from expensive view work.
 *
 * <p>Notification listener callbacks are allowed while SystemUI is interactive or a
 * presentation surface is hidden. In those states the cache still needs to stay current, but
 * rebuilding Pixel AOD or lockscreen content cannot be visible and only creates layout work.</p>
 */
final class NotificationPresentationGate {
    private static final float MIN_DRAWABLE_ALPHA = 0.01f;

    private NotificationPresentationGate() {
    }

    static boolean shouldRefreshAod(boolean attached, boolean visible, boolean shown,
            float alpha, boolean lifecycleAllowsDrawing) {
        return attached
                && visible
                && shown
                && alpha > MIN_DRAWABLE_ALPHA
                && lifecycleAllowsDrawing;
    }

    static boolean shouldRefreshLockscreen(boolean attached, boolean visible, boolean shown,
            float alpha) {
        return attached
                && visible
                && shown
                && alpha > MIN_DRAWABLE_ALPHA;
    }
}
