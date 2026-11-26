package dev.codex.pixelaod;

/**
 * Pure policy for handling consecutive incoming notifications while an OPlus screen-off Peek
 * surface is already active. OPlus CurvedDisplayManager drops subsequent notifications while
 * mIsShowingCurvedDisplay is true; this policy governs when to refresh the active Peek card
 * and how long to defer the native dismiss callback without leaking the wake window.
 */
final class PixelPeekConsecutivePolicy {
    static final long DEFAULT_DISPLAY_DURATION_MS = 5_000L;
    static final long MIN_DISPLAY_DURATION_MS = 1_500L;
    static final long MAX_DISPLAY_DURATION_MS = 15_000L;
    static final long MIN_DEFER_THRESHOLD_MS = 150L;
    static final long MAX_TOTAL_SESSION_MS = 20_000L;

    private PixelPeekConsecutivePolicy() {
    }

    static boolean shouldRejectPackageOrId(String packageName, int notificationId) {
        if (packageName == null || packageName.isEmpty()) {
            return true;
        }
        if ("com.oplus.olc".equals(packageName)
                || "com.android.incallui".equals(packageName)
                || "com.google.android.dialer".equals(packageName)) {
            return true;
        }
        if ("com.tencent.mm".equals(packageName) && notificationId == 40) {
            return true;
        }
        return "com.tencent.mobileqq".equals(packageName)
                && (notificationId == 2130840368 || notificationId == 129);
    }

    static boolean isDuplicateNotification(String lastKey, long lastPostTime,
            String candidateKey, long candidatePostTime) {
        if (lastKey == null || candidateKey == null) {
            return false;
        }
        return lastKey.equals(candidateKey) && lastPostTime == candidatePostTime;
    }

    static long resolveDisplayDurationMs(int vendorDisplayTimeMs) {
        if (vendorDisplayTimeMs >= MIN_DISPLAY_DURATION_MS
                && vendorDisplayTimeMs <= MAX_DISPLAY_DURATION_MS) {
            return vendorDisplayTimeMs;
        }
        return DEFAULT_DISPLAY_DURATION_MS;
    }

    static long computeDeferMillis(long sessionStartMs, long lastUpdateMs, long nowMs,
            int vendorDisplayTimeMs, int replacementCount) {
        if (replacementCount <= 0 || sessionStartMs <= 0L || lastUpdateMs <= 0L) {
            return 0L;
        }
        long displayDuration = resolveDisplayDurationMs(vendorDisplayTimeMs);
        long targetEndMs = lastUpdateMs + displayDuration;
        long maxSessionEndMs = sessionStartMs + MAX_TOTAL_SESSION_MS;
        long effectiveEndMs = Math.min(targetEndMs, maxSessionEndMs);
        long remaining = effectiveEndMs - nowMs;
        return remaining > MIN_DEFER_THRESHOLD_MS ? remaining : 0L;
    }
}
