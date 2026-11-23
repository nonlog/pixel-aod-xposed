package dev.codex.pixelaod;

import java.util.Locale;

/**
 * Pure geometry and view-matching rules to prevent the large lockscreen clock and date row
 * from colliding with ColorOS / OPlus lockscreen widgets placed below the clock.
 */
final class CouiClockWidgetAvoidancePolicy {
    static final float MIN_WIDGET_TOP_RATIO = 0.30f;
    static final float MAX_WIDGET_TOP_RATIO = 0.80f;
    static final float MIN_CLOCK_TOP_DP = 64f;
    static final float WIDGET_CLEARANCE_DP = 14f;

    private CouiClockWidgetAvoidancePolicy() {
    }

    static boolean isLockscreenWidgetCandidate(String className, String idName) {
        String cls = className == null ? "" : className.toLowerCase(Locale.US);
        String id = idName == null ? "" : idName.toLowerCase(Locale.US);
        String combined = cls + " " + id;
        if (combined.trim().isEmpty()) {
            return false;
        }
        if (combined.contains("pixelaod")
                || combined.contains("security")
                || combined.contains("bouncer")
                || combined.contains("keyboard")
                || combined.contains("pin")
                || combined.contains("password")
                || combined.contains("editinput")
                || combined.contains("affordance")
                || combined.contains("fingerprint")
                || combined.contains("udfps")) {
            return false;
        }
        return combined.contains("widgetplugin")
                || combined.contains("stylewidget")
                || combined.contains("keyguard_widget")
                || combined.contains("lockscreen_widget")
                || combined.contains("clock_widget")
                || combined.contains("oplus_widget")
                || combined.contains("cardwidget")
                || combined.contains("widget_container")
                || (cls.contains("oplus") && cls.contains("widget"));
    }

    static float resolveVerticalShiftUpPx(
            float hostHeightPx,
            float clockTopPx,
            float dateBottomPx,
            float widgetTopPx,
            float minClockTopPx,
            float clearancePx) {
        if (hostHeightPx <= 0f || Float.isNaN(widgetTopPx) || widgetTopPx <= 0f) {
            return 0f;
        }
        float minBand = hostHeightPx * MIN_WIDGET_TOP_RATIO;
        float maxBand = hostHeightPx * MAX_WIDGET_TOP_RATIO;
        if (widgetTopPx < minBand || widgetTopPx > maxBand) {
            return 0f;
        }
        float overlap = (dateBottomPx + Math.max(0f, clearancePx)) - widgetTopPx;
        if (overlap <= 0f) {
            return 0f;
        }
        float maxShift = Math.max(0f, clockTopPx - Math.max(0f, minClockTopPx));
        return Math.min(overlap, maxShift);
    }
}
