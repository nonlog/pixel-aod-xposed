package dev.codex.pixelaod;

/**
 * Gates the persistent ClockPlugin's desktop/app-to-AOD pre-presentation.
 *
 * <p>OOS makes the persistent ClockPlugin drawable before it reports the AOD uiState.  Showing
 * the replacement immediately therefore puts the Pixel clock over the still-visible wallpaper
 * for a frame or two. The normal path is to wait for the display to report Doze. If that does
 * not happen promptly, the normal vendor host-ready path must take over instead of showing the
 * replacement over the wallpaper.</p>
 */
final class ClockPluginNonLockscreenEntryGate {
    static final long MAX_WAIT_FOR_NATIVE_AOD_MILLIS = 120L;
    static final long RETRY_INTERVAL_MILLIS = 16L;

    enum Decision {
        DEFER,
        PRESENT,
        CANCEL,
        ALREADY_PRESENTED
    }

    private String activeTrace = "";
    private long entryStartedAt;
    private boolean cancelled;
    private boolean presented;

    synchronized Decision evaluate(String trace, boolean interactive, boolean displayInAod,
            long now) {
        String safeTrace = trace != null ? trace : "";
        if (entryStartedAt <= 0L || !safeTrace.equals(activeTrace)) {
            activeTrace = safeTrace;
            entryStartedAt = now;
            cancelled = false;
            presented = false;
        }
        if (cancelled || interactive) {
            cancelled = true;
            return Decision.CANCEL;
        }
        if (presented) {
            return Decision.ALREADY_PRESENTED;
        }
        if (displayInAod) {
            presented = true;
            return Decision.PRESENT;
        }
        if (elapsedMillis(now) >= MAX_WAIT_FOR_NATIVE_AOD_MILLIS) {
            cancelled = true;
            return Decision.CANCEL;
        }
        return Decision.DEFER;
    }

    synchronized long retryDelayMillis(long now) {
        long remaining = Math.max(0L, MAX_WAIT_FOR_NATIVE_AOD_MILLIS - elapsedMillis(now));
        return Math.max(1L, Math.min(RETRY_INTERVAL_MILLIS, remaining));
    }

    private long elapsedMillis(long now) {
        return Math.max(0L, now - entryStartedAt);
    }
}
