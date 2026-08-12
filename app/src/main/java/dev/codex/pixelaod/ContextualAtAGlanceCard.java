package dev.codex.pixelaod;

/** One fixed-height, one-line contextual slot shared by lockscreen and AOD. */
final class ContextualAtAGlanceCard {
    static final long REPLACEMENT_CROSSFADE_MILLIS = 250L;
    static final long ENTER_LEAVE_FADE_MILLIS = 300L;

    enum Kind {
        NONE,
        WEATHER_ALERT,
        CALENDAR_EVENT,
        WEATHER_FORECAST
    }

    enum IconKind {
        NONE,
        WEATHER_ALERT,
        CALENDAR,
        WEATHER_FORECAST
    }

    final Kind kind;
    final IconKind iconKind;
    final String identity;
    final String text;
    final int weatherCode;
    final int alertSeverity;
    final float alpha;
    final boolean privacyRedacted;

    private ContextualAtAGlanceCard(Kind kind, IconKind iconKind, String identity, String text,
            int weatherCode, int alertSeverity, float alpha, boolean privacyRedacted) {
        this.kind = kind;
        this.iconKind = iconKind;
        this.identity = identity != null ? identity : "";
        this.text = text != null ? text : "";
        this.weatherCode = weatherCode;
        this.alertSeverity = Math.max(0, alertSeverity);
        this.alpha = alpha;
        this.privacyRedacted = privacyRedacted;
    }

    static ContextualAtAGlanceCard none() {
        return new ContextualAtAGlanceCard(Kind.NONE, IconKind.NONE, "", "",
                BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, 0, 0f, false);
    }

    static ContextualAtAGlanceCard alert(BreezyWeatherAlert alert, String text,
            boolean privacyRedacted, float alpha) {
        if (alert == null || alert.isEmpty()) {
            return none();
        }
        return new ContextualAtAGlanceCard(Kind.WEATHER_ALERT, IconKind.WEATHER_ALERT,
                alert.presentationKey, text, BreezyWeatherForecast.UNKNOWN_WEATHER_CODE,
                alert.severity, alpha, privacyRedacted);
    }

    static ContextualAtAGlanceCard calendar(String text, float alpha) {
        String normalized = PixelAodRenderModel.normalizeAtAGlanceExtra(text);
        if (normalized.isEmpty()) {
            return none();
        }
        return new ContextualAtAGlanceCard(Kind.CALENDAR_EVENT, IconKind.CALENDAR,
                "calendar:" + normalized, normalized,
                BreezyWeatherForecast.UNKNOWN_WEATHER_CODE, 0, alpha, false);
    }

    static ContextualAtAGlanceCard forecast(BreezyWeatherForecast forecast, String text,
            float alpha) {
        if (forecast == null || !forecast.isComplete()) {
            return none();
        }
        return new ContextualAtAGlanceCard(Kind.WEATHER_FORECAST,
                IconKind.WEATHER_FORECAST, forecast.displayKey(), text, forecast.weatherCode,
                0, alpha, false);
    }

    boolean isVisible() {
        return kind != Kind.NONE && !text.isEmpty();
    }

    boolean sameContent(ContextualAtAGlanceCard other) {
        return other != null && kind == other.kind && iconKind == other.iconKind
                && identity.equals(other.identity) && text.equals(other.text)
                && weatherCode == other.weatherCode
                && alertSeverity == other.alertSeverity
                && Float.compare(alpha, other.alpha) == 0
                && privacyRedacted == other.privacyRedacted;
    }

    boolean isReplacementOf(ContextualAtAGlanceCard other) {
        return other != null && isVisible() && other.isVisible() && !sameContent(other);
    }
}
