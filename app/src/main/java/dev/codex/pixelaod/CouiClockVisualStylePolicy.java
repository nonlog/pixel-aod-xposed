package dev.codex.pixelaod;

/** Pure clock color and information-shadow contract owned by the COUI host. */
final class CouiClockVisualStylePolicy {
    static final int LOCKSCREEN_FALLBACK_CLOCK_COLOR = -1515784;
    static final int INFORMATION_SHADOW_COLOR = 1711276032;
    static final float INFORMATION_SHADOW_RADIUS_DP = 1.5f;
    static final float INFORMATION_SHADOW_DY_DP = 0.5f;

    private CouiClockVisualStylePolicy() {
    }

    static int clockColor(CouiClockPresentationModel.Scene scene, boolean dozing,
            int monetColor, int aodMonetColor) {
        if (dozing) {
            if (aodMonetColor != Integer.MIN_VALUE) {
                return aodMonetColor;
            }
            if (monetColor != Integer.MIN_VALUE) {
                return monetColor;
            }
            return LOCKSCREEN_FALLBACK_CLOCK_COLOR;
        }
        if (scene == CouiClockPresentationModel.Scene.IMMERSED) {
            return 0xffffffff;
        }
        return monetColor == Integer.MIN_VALUE
                ? LOCKSCREEN_FALLBACK_CLOCK_COLOR : monetColor;
    }

    static boolean shouldApplyInformationShadow(CouiClockPresentationModel.Scene visualScene,
            boolean dozing, boolean partialAod, boolean mediaContent) {
        return visualScene != CouiClockPresentationModel.Scene.IMMERSED
                && !(dozing && partialAod && mediaContent);
    }
}
