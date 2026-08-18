package dev.codex.pixelaod;

import java.util.Locale;

/**
 * Immutable startup policy for the primary clock owner.
 */
public final class ClockRendererPolicy {
    public enum Mode {
        LEGACY,
        COUI_PORT
    }

    public static final String VALUE_LEGACY = "legacy";
    public static final String VALUE_COUI_PORT = "coui_port";

    private static final ClockRendererPolicy LEGACY_POLICY =
            new ClockRendererPolicy(Mode.LEGACY);
    private static final ClockRendererPolicy COUI_PORT_POLICY =
            new ClockRendererPolicy(Mode.COUI_PORT);

    private final Mode mode;

    private ClockRendererPolicy(Mode mode) {
        this.mode = mode;
    }

    public static ClockRendererPolicy parse(String configured) {
        // M4 cutover: COUI_PORT is the production owner. LEGACY remains an explicit startup-only
        // rollback value, but missing/invalid values must never silently recreate the old primary
        // renderer on a clean install or after a settings migration.
        if (configured == null) {
            return COUI_PORT_POLICY;
        }
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        if (VALUE_LEGACY.equals(normalized)) {
            return LEGACY_POLICY;
        }
        return COUI_PORT_POLICY;
    }

    public Mode mode() {
        return mode;
    }

    public boolean useLegacyOwner() {
        return mode == Mode.LEGACY;
    }

    public boolean useCouiOwner() {
        return mode == Mode.COUI_PORT;
    }

    public String storedValue() {
        return mode == Mode.COUI_PORT ? VALUE_COUI_PORT : VALUE_LEGACY;
    }
}
