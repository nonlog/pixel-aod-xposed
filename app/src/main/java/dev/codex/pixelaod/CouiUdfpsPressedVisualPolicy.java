package dev.codex.pixelaod;

/** Pure visual gating for the COUI UDFPS primary glyph and HDR illumination carrier. */
final class CouiUdfpsPressedVisualPolicy {
    private CouiUdfpsPressedVisualPolicy() {
    }

    static VisualState resolve(boolean liveTouchDown, boolean hdrEnabled,
            boolean vendorCarrierVisible) {
        return new VisualState(primaryDrawablePressed(liveTouchDown, hdrEnabled),
                illuminationAlpha(liveTouchDown, hdrEnabled), vendorCarrierVisible);
    }

    static boolean primaryDrawablePressed(boolean liveTouchDown, boolean hdrEnabled) {
        return liveTouchDown && hdrEnabled;
    }

    static int illuminationAlpha(boolean liveTouchDown, boolean hdrEnabled) {
        return primaryDrawablePressed(liveTouchDown, hdrEnabled) ? 255 : 0;
    }

    /**
     * Keep an attached HDR-capable pressed window at SDR headroom while idle. OPlus can attach
     * this window during ordinary wake/doze transitions even with no finger down, so pre-arming
     * max headroom creates a visible flash and can leave the surface bright if the later reset
     * races surface creation. Real touch raises the headroom through the normal pressed path.
     */
    static float desiredHdrHeadroom(boolean liveTouchDown, boolean hdrEnabled,
            float maxHeadroom) {
        if (!primaryDrawablePressed(liveTouchDown, hdrEnabled)) {
            return 1f;
        }
        return Math.max(1f, maxHeadroom);
    }

    /**
     * When module HDR is disabled, stable 0.1.331 leaves the vendor optical carrier itself
     * intact and gates only its View alpha. Vendor pressed-animation/HBM decisions must therefore
     * remain native in that mode. The COUI replacement carrier is allowed only for HDR mode.
     */
    static boolean useModulePressedCarrier(boolean hdrEnabled) {
        return hdrEnabled;
    }

    static boolean suppressVendorPressedAnimation(boolean hdrEnabled) {
        return useModulePressedCarrier(hdrEnabled);
    }

    /** Stable 0.1.331 contract: the vendor pressed carrier itself is invisible while idle. */
    static float pressedCarrierViewAlpha(boolean liveTouchDown, float originalAlpha) {
        if (!liveTouchDown) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, originalAlpha));
    }

    static final class VisualState {
        private final boolean primaryDrawablePressed;
        private final int moduleIlluminationAlpha;
        private final boolean preserveVendorCarrierVisibility;

        private VisualState(boolean primaryDrawablePressed, int moduleIlluminationAlpha,
                boolean preserveVendorCarrierVisibility) {
            this.primaryDrawablePressed = primaryDrawablePressed;
            this.moduleIlluminationAlpha = moduleIlluminationAlpha;
            this.preserveVendorCarrierVisibility = preserveVendorCarrierVisibility;
        }

        boolean primaryDrawablePressed() {
            return primaryDrawablePressed;
        }

        int moduleIlluminationAlpha() {
            return moduleIlluminationAlpha;
        }

        boolean preserveVendorCarrierVisibility() {
            return preserveVendorCarrierVisibility;
        }
    }
}
