package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StockAodSuppressionScopePolicyTest {
    @Test
    public void shadeWindowIsRejectedAsStockAodSuppressionRoot() {
        assertFalse(StockAodSuppressionScopePolicy.isStockAodSuppressionRootClassName(
                "com.android.systemui.shade.NotificationShadeWindowView"));
        assertFalse(StockAodSuppressionScopePolicy.isStockAodSuppressionRootClassName(null));
        assertFalse(StockAodSuppressionScopePolicy.isStockAodSuppressionRootClassName(""));
    }

    @Test
    public void aodRootAndClockLayoutAreAcceptedAsStockAodSuppressionRoots() {
        assertTrue(StockAodSuppressionScopePolicy.isStockAodSuppressionRootClassName(
                "com.oplus.systemui.aod.aodclock.off.AodRootLayout"));
        assertTrue(StockAodSuppressionScopePolicy.isStockAodSuppressionRootClassName(
                "com.oplus.systemui.aod.aodclock.off.AodClockLayout"));
    }

    @Test
    public void sharedMediaTemplateAndLyricMarkersAreProtected() {
        assertTrue(StockAodSuppressionScopePolicy.isProtectedSharedPluginOrLyricMarker(
                "com.oplus.systemui.plugins.shared.template.component.media.view.OplusLottieAnimationView id=com.oplus.systemui.plugins:id/lottie_btn"));
        assertTrue(StockAodSuppressionScopePolicy.isProtectedSharedPluginOrLyricMarker(
                "com.oplus.systemui.media.LockscreenLyricView id=com.android.systemui:id/lyric_container"));
        assertFalse(StockAodSuppressionScopePolicy.isProtectedSharedPluginOrLyricMarker(
                "com.oplus.systemui.aod.aodclock.off.media.AodMediaLayout id=com.android.systemui:id/aod_media_container"));
        assertFalse(StockAodSuppressionScopePolicy.isProtectedSharedPluginOrLyricMarker(null));
    }
}
