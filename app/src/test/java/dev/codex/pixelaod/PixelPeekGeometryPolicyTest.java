package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class PixelPeekGeometryPolicyTest {
    @Test
    public void currentCph2573CompactProfileStartsBelowNotificationIcons() {
        // CPH2573 at 640 dpi: current compact notification row ends around y=920 px.
        assertEquals(992f, PixelPeekGeometryPolicy.resolveCardTopPx(
                4f, 3168, 920f), 0.001f);
    }

    @Test
    public void earlyVendorCallbackStillUsesSafeCompactFallback() {
        // If the vendor callback arrives before the icon row is laid out, the fallback itself
        // must already be below the canonical compact clock/icon cluster.
        assertEquals(992f, PixelPeekGeometryPolicy.resolveCardTopPx(
                4f, 3168, 0f), 0.001f);
    }

    @Test
    public void lowerContextualOrMediaContentPushesPeekFurtherDown() {
        assertEquals(1272f, PixelPeekGeometryPolicy.resolveCardTopPx(
                4f, 3168, 1200f), 0.001f);
    }

    @Test
    public void cardCannotConsumeBatterySafeArea() {
        assertEquals(2312f, PixelPeekGeometryPolicy.resolveCardTopPx(
                4f, 3168, 3000f), 0.001f);
    }
}
