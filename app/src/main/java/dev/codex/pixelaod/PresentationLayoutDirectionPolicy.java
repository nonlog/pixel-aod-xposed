package dev.codex.pixelaod;

/** Pure START/END geometry helpers for the active COUI host. */
final class PresentationLayoutDirectionPolicy {
    private PresentationLayoutDirectionPolicy() {
    }

    static float mirrorCenter(float containerWidth, float ltrCenter, boolean rtl) {
        return rtl ? containerWidth - ltrCenter : ltrCenter;
    }

    static float startAlignedX(float containerWidth, float childWidth, float startInset,
            boolean rtl) {
        if (!rtl) {
            return startInset;
        }
        return Math.max(0f, containerWidth - startInset - Math.max(0f, childWidth));
    }

    static float compactInformationStart(float centeredStart, float informationWidth,
            float clockStart, float clockEnd, float minimumGap, float minimumStart,
            float maximumStart, boolean rtl) {
        float gap = Math.max(0f, minimumGap);
        if (!rtl) {
            float requiredStart = Math.max(centeredStart, clockEnd + gap);
            float safeMaximumStart = Math.max(centeredStart, maximumStart);
            return Math.min(requiredStart, safeMaximumStart);
        }
        float requiredStart = Math.min(centeredStart,
                clockStart - gap - Math.max(0f, informationWidth));
        float safeMinimumStart = Math.min(centeredStart, minimumStart);
        return Math.max(requiredStart, safeMinimumStart);
    }
}
