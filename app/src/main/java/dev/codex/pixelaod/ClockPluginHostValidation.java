package dev.codex.pixelaod;

final class ClockPluginHostValidation {
    private static final float MIN_DRAWABLE_ALPHA = 0.01f;

    private ClockPluginHostValidation() {
    }

    static boolean isDrawableNode(boolean attached, int visibility, float alpha) {
        return attached && visibility == 0 && alpha > MIN_DRAWABLE_ALPHA;
    }
}
