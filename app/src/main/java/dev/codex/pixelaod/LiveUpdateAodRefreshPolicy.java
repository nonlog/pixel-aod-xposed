package dev.codex.pixelaod;

/** Safety gate for second-level Live Update repaint requests while the device is dozing. */
final class LiveUpdateAodRefreshPolicy {
    private LiveUpdateAodRefreshPolicy() {
    }

    static boolean canUseRamless(Boolean ramlessSupported, Boolean aodInstalled,
            boolean pluginPresent) {
        return Boolean.TRUE.equals(ramlessSupported)
                && Boolean.TRUE.equals(aodInstalled)
                && pluginPresent;
    }

    /**
     * OPlus performAodUpdate() advances minute-level vendor accounting; it is never a legal
     * fallback for a one-second metric tick.
     */
    static boolean canUseMinuteSemanticKickForSecondTick() {
        return false;
    }
}