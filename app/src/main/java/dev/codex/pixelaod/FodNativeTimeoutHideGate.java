package dev.codex.pixelaod;

/**
 * Trace-scoped ownership latch for the native AOD fingerprint timeout.
 *
 * <p>Once ColorOS has asked the fingerprint icon to hide for the current AOD trace, Pixel AOD
 * must keep the overall Doze/AOD surface alive without reclaiming the fingerprint carrier. The
 * latch intentionally has no wall-clock expiry: native FOD stays hidden until an explicit
 * fingerprint interaction, an interactive wake, or a new AOD trace takes ownership.</p>
 */
final class FodNativeTimeoutHideGate {
    private String hiddenTrace = "";
    private long hiddenAtMillis;

    synchronized boolean markHidden(String trace, long nowMillis) {
        if (isEmpty(trace)) {
            return false;
        }
        hiddenTrace = trace;
        hiddenAtMillis = nowMillis;
        return true;
    }

    synchronized boolean shouldPreserveNativeHide(String currentTrace, boolean interactive) {
        return !interactive
                && !isEmpty(hiddenTrace)
                && !isEmpty(currentTrace)
                && hiddenTrace.equals(currentTrace);
    }

    synchronized boolean clearIfTrace(String expectedTrace) {
        if (isEmpty(expectedTrace) || !hiddenTrace.equals(expectedTrace)) {
            return false;
        }
        clearLocked();
        return true;
    }

    synchronized boolean clear() {
        if (isEmpty(hiddenTrace)) {
            return false;
        }
        clearLocked();
        return true;
    }

    synchronized String hiddenTrace() {
        return hiddenTrace;
    }

    synchronized long hiddenAgeMillis(long nowMillis) {
        return hiddenAtMillis > 0L && nowMillis >= hiddenAtMillis
                ? nowMillis - hiddenAtMillis : -1L;
    }

    private void clearLocked() {
        hiddenTrace = "";
        hiddenAtMillis = 0L;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
