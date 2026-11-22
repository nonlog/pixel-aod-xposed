package dev.codex.pixelaod;

/**
 * Prevents an unchanged OPlus ClockPlugin render from re-presenting the complete replacement
 * hierarchy.  The vendor invokes {@code render()} far more often than the visible scene changes.
 */
final class ClockPluginPresentationGate {
    private String lastPresentedFingerprint = "";

    synchronized boolean shouldPresent(ClockPluginSceneMachine.Decision decision, boolean force) {
        String safeFingerprint = decision == null ? "none"
                : decision.scene + "|entering=" + decision.enteringAod
                + "|preparing=" + decision.preparingAod;
        if (force || !safeFingerprint.equals(lastPresentedFingerprint)) {
            lastPresentedFingerprint = safeFingerprint;
            return true;
        }
        return false;
    }
}
