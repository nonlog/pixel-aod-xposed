package dev.codex.pixelaod;

import java.util.Locale;

/**
 * Guards stock AOD view suppression so shade/lockscreen window roots (such as
 * NotificationShadeWindowView under ColorOS 17 Panoramic/Seamless AOD) and shared
 * SystemUIPlugin media/lyric components are never hidden by the legacy AOD hide pass.
 */
final class StockAodSuppressionScopePolicy {
    private static final String SHARED_TEMPLATE_COMPONENT_PREFIX =
            "com.oplus.systemui.plugins.shared.template.component.";

    private StockAodSuppressionScopePolicy() {
    }

    static boolean isStockAodSuppressionRootClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        String lower = className.toLowerCase(Locale.US);
        return !lower.contains("notificationshadewindowview");
    }

    static boolean isProtectedSharedPluginOrLyricMarker(String marker) {
        if (marker == null || marker.isEmpty()) {
            return false;
        }
        String lower = marker.toLowerCase(Locale.US);
        return lower.contains(SHARED_TEMPLATE_COMPONENT_PREFIX)
                || lower.contains("lyric");
    }
}
