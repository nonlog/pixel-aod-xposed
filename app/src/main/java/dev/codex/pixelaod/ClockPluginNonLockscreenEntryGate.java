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

    /**
     * A persistent ClockPlugin can keep its last lockscreen scene drawable for a few seconds
     * while OPlus is transitioning an app/desktop screen-off into native Doze.  During that
     * interval the vendor may still publish KEYGUARD/LARGE even though the module already knows
     * this AOD trace did not originate from the interactive lockscreen.  Keep the replacement
     * host parked until either the vendor publishes an AOD state or the display itself enters
     * Doze; otherwise that stale lockscreen scene becomes a user-visible first frame.
     */
    synchronized boolean shouldParkPersistentHost(String trace, boolean interactive,
            boolean displayInAod, boolean vendorReportsAod) {
        String safeTrace = trace != null ? trace : "";
        return entryStartedAt > 0L
                && safeTrace.equals(activeTrace)
                && !interactive
                && !displayInAod
                && !vendorReportsAod;
    }

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
