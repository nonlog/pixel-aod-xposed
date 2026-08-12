package dev.codex.pixelaod;

/** Pure severity-to-visual mapping for contextual weather alerts. */
final class WeatherAlertVisuals {
    enum IconLevel {
        UNKNOWN,
        MINOR,
        MODERATE,
        SEVERE,
        EXTREME
    }

    private WeatherAlertVisuals() {
    }

    static IconLevel iconLevel(int severity) {
        if (severity >= 4) return IconLevel.EXTREME;
        if (severity == 3) return IconLevel.SEVERE;
        if (severity == 2) return IconLevel.MODERATE;
        if (severity == 1) return IconLevel.MINOR;
        return IconLevel.UNKNOWN;
    }
}
