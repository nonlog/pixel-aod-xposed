package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ExternalStatusBarIconCacheTest {
    @Test
    public void tracksPutChangeDetectionEvictionAndRemoval() {
        ExternalStatusBarIconCache<String> cache = new ExternalStatusBarIconCache<>(2);
        assertTrue(cache.put("k1", "icon1"));
        assertFalse(cache.put("k1", "icon1"));
        assertTrue(cache.put("k1", "icon1b"));
        assertEquals("icon1b", cache.get("k1"));

        assertTrue(cache.put("k2", "icon2"));
        assertTrue(cache.put("k3", "icon3"));
        assertEquals(2, cache.size());
        assertNull(cache.get("k1"));
        assertEquals("icon2", cache.get("k2"));
        assertEquals("icon3", cache.get("k3"));

        assertTrue(cache.remove("k2"));
        assertFalse(cache.remove("k2"));
        assertNull(cache.get("k2"));

        cache.clear();
        assertEquals(0, cache.size());
    }

    @Test
    public void detectsExternalBitmapReplacementFromColorOsNotify() {
        Object rawResourceIcon = new Object();
        Object replacementBitmap = new Object();

        assertTrue(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                rawResourceIcon, replacementBitmap, true, false));
        assertFalse(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                rawResourceIcon, rawResourceIcon, true, true));
        assertFalse(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                rawResourceIcon, replacementBitmap, false, false));
        assertTrue(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                rawResourceIcon, replacementBitmap, true, true));
        assertTrue(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                null, replacementBitmap, true, false));
        assertFalse(ExternalStatusBarIconCache.isExternalBitmapReplacement(
                rawResourceIcon, null, true, false));
    }

    @Test
    public void acceptsMonochromeAlphaMasksAndRejectsColoredBitmaps() {
        int[] whiteGlyph = new int[] {
                0x00000000, 0xFFFFFFFF, 0x80E0E0E0, 0xFFCCCCCC, 0x00FF0000
        };
        assertTrue(ExternalStatusBarIconCache.isMonochromePixels(whiteGlyph));

        int[] coloredLogo = new int[] {
                0x00000000, 0xFF1A73E8, 0xFFEA4335, 0xFF34A853, 0xFFFBBC05
        };
        assertFalse(ExternalStatusBarIconCache.isMonochromePixels(coloredLogo));

        int[] fullyTransparent = new int[] {0x00000000, 0x10FFFFFF, 0x00000000};
        assertFalse(ExternalStatusBarIconCache.isMonochromePixels(fullyTransparent));
        assertFalse(ExternalStatusBarIconCache.isMonochromePixels(new int[0]));
        assertFalse(ExternalStatusBarIconCache.isMonochromePixels(null));
    }
}
