package dev.codex.pixelaod;

/**
 * Read-only normalization of OPlus's screen-off UDFPS touch presentation.
 *
 * <p>The current supported OOS build receives the real hardware pointer transition through
 * {@code OplusBiometricAuthController#showUdfpsOverlay(int)}. Reason 8 is the native fingerprint
 * touch-down carrier; reasons 9/10 and overlay hide terminate it. Pixel AOD observes those vendor
 * edges only and never owns fingerprint sensing, HBM/local-HBM, pressed illumination, wake locks,
 * or panel state.</p>
 */
final class SelectiveBiometricPulseAdapter {
    static final int REASON_TOUCH_DOWN = 8;
    static final int REASON_TOUCH_UP = 9;
    static final int REASON_TOUCH_UP_ALT = 10;

    enum Presentation {
        IDLE,
        AUTH_UI_ONLY
    }

    private long generation;
    private boolean authUiActive;
    private int lastReason = -1;
    private String source = "none";

    synchronized Snapshot observeOverlayReason(int reason, String eventSource) {
        boolean nextActive = authUiActive;
        if (reason == REASON_TOUCH_DOWN) {
            nextActive = true;
        } else if (reason == REASON_TOUCH_UP || reason == REASON_TOUCH_UP_ALT
                || isIconShowReason(reason)) {
            nextActive = false;
        }
        if (authUiActive != nextActive || lastReason != reason) {
            generation++;
            authUiActive = nextActive;
            lastReason = reason;
            source = normalize(eventSource);
        }
        return snapshotLocked();
    }

    synchronized Snapshot observeOverlayHidden(String eventSource) {
        if (authUiActive || lastReason != -1) {
            generation++;
            authUiActive = false;
            lastReason = -1;
            source = normalize(eventSource);
        }
        return snapshotLocked();
    }

    synchronized Snapshot snapshot() {
        return snapshotLocked();
    }

    private static boolean isIconShowReason(int reason) {
        return reason >= 0 && reason <= 6;
    }

    private Snapshot snapshotLocked() {
        return new Snapshot(generation, authUiActive, lastReason, source);
    }

    private static String normalize(String value) {
        return value == null || value.trim().isEmpty() ? "unknown" : value.trim();
    }

    static final class Snapshot {
        final long generation;
        final boolean authUiActive;
        final int lastReason;
        final Presentation presentation;
        final String source;

        Snapshot(long generation, boolean authUiActive, int lastReason, String source) {
            this.generation = generation;
            this.authUiActive = authUiActive;
            this.lastReason = lastReason;
            this.presentation = authUiActive ? Presentation.AUTH_UI_ONLY : Presentation.IDLE;
            this.source = normalize(source);
        }

        boolean blocksPixelContent() {
            return authUiActive;
        }

        String blockReason() {
            return authUiActive
                    ? "native-auth-ui-only-pulse"
                    : "native-selective-pulse-allows-content";
        }

        String describe() {
            return "generation=" + generation
                    + ",authUiActive=" + authUiActive
                    + ",lastReason=" + lastReason
                    + ",presentation=" + presentation
                    + ",blocksPixelContent=" + blocksPixelContent()
                    + ",source=" + source;
        }
    }
}
