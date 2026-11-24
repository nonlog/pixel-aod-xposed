package dev.codex.pixelaod;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded per-notification cache for external status-bar monochrome icon replacements
 * (for example, ANIP monochrome bitmaps injected into StatusBarIcon by com.fankes.coloros.notify).
 */
final class ExternalStatusBarIconCache<T> {
    private static final int MIN_VISIBLE_ALPHA = 32;
    private static final int MAX_MONOCHROME_CHROMA = 24;
    private static final float MAX_COLORFUL_PIXEL_RATIO = 0.10f;

    private final int maxEntries;
    private final LinkedHashMap<String, T> entries;

    ExternalStatusBarIconCache(int maxEntries) {
        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.maxEntries = maxEntries;
        this.entries = new LinkedHashMap<String, T>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, T> eldest) {
                return size() > ExternalStatusBarIconCache.this.maxEntries;
            }
        };
    }

    /**
     * Stores an external icon token for the notification key.
     *
     * @return {@code true} if the cached token was added or changed.
     */
    synchronized boolean put(String notificationKey, T iconToken) {
        if (notificationKey == null || notificationKey.isEmpty() || iconToken == null) {
            return false;
        }
        T previous = entries.put(notificationKey, iconToken);
        return previous != iconToken && !Objects.equals(previous, iconToken);
    }

    synchronized T get(String notificationKey) {
        if (notificationKey == null || notificationKey.isEmpty()) {
            return null;
        }
        return entries.get(notificationKey);
    }

    synchronized boolean remove(String notificationKey) {
        if (notificationKey == null || notificationKey.isEmpty()) {
            return false;
        }
        return entries.remove(notificationKey) != null;
    }

    synchronized void clear() {
        entries.clear();
    }

    synchronized int size() {
        return entries.size();
    }

    /**
     * Returns true when a StatusBarIcon.icon appears to be an external bitmap replacement
     * (such as com.fankes.coloros.notify's Icon.createWithBitmap) rather than the raw smallIcon.
     */
    static boolean isExternalBitmapReplacement(Object rawSmallIcon, Object statusBarIcon,
            boolean statusBarIconIsBitmap, boolean rawSmallIconIsBitmap) {
        if (statusBarIcon == null || !statusBarIconIsBitmap) {
            return false;
        }
        if (rawSmallIcon == null) {
            return true;
        }
        if (statusBarIcon == rawSmallIcon) {
            return false;
        }
        if (!rawSmallIconIsBitmap) {
            return true;
        }
        return !Objects.equals(rawSmallIcon, statusBarIcon);
    }

    /**
     * Returns true when the sampled ARGB pixels represent an achromatic (white/gray) monochrome
     * glyph rather than a colored app icon bitmap. Guards against tinting native ColorOS colored
     * bitmaps into solid silhouettes if com.fankes.coloros.notify is uninstalled.
     */
    static boolean isMonochromePixels(int[] argbPixels) {
        if (argbPixels == null || argbPixels.length == 0) {
            return false;
        }
        int visible = 0;
        int colorful = 0;
        for (int pixel : argbPixels) {
            int alpha = (pixel >>> 24) & 0xFF;
            if (alpha < MIN_VISIBLE_ALPHA) {
                continue;
            }
            visible++;
            int r = (pixel >>> 16) & 0xFF;
            int g = (pixel >>> 8) & 0xFF;
            int b = pixel & 0xFF;
            int max = Math.max(r, Math.max(g, b));
            int min = Math.min(r, Math.min(g, b));
            if ((max - min) > MAX_MONOCHROME_CHROMA) {
                colorful++;
            }
        }
        if (visible == 0) {
            return false;
        }
        return ((float) colorful / (float) visible) <= MAX_COLORFUL_PIXEL_RATIO;
    }
}
