package dev.codex.pixelaod;

/** One fixed-height, one-line contextual slot shared by lockscreen and AOD. */
final class ContextualAtAGlanceCard {
    static final long REPLACEMENT_CROSSFADE_MILLIS = 250L;
    static final long ENTER_LEAVE_FADE_MILLIS = 300L;

    enum Kind {
        NONE,
        NATIVE_SMARTSPACE,
        LIVE_UPDATE,
        WEATHER_ALERT,
        CALENDAR_EVENT,
        WEATHER_FORECAST
    }

    enum IconKind {
        NONE,
        WEATHER_ALERT,
        CALENDAR,
        WEATHER_FORECAST,
        LIVE_TIMER,
        LIVE_HOTSPOT,
        LIVE_PROGRESS,
        LIVE_CALL
    }

    enum LiveUpdateKind {
        NONE,
        TIMER,
        HOTSPOT,
        PROGRESS,
        CALL
    }

    final Kind kind;
    final IconKind iconKind;
    final String identity;
    final String text;
    final int weatherCode;
    final int alertSeverity;
    final float alpha;
    final boolean privacyRedacted;
    final LiveUpdateKind liveUpdateKind;
    final String liveUpdateMetricText;
    final int liveUpdateProgressPercent;
    final long liveUpdateTimeBaseElapsedRealtime;
    final boolean liveUpdateCountDown;

    private ContextualAtAGlanceCard(Kind kind, IconKind iconKind, String identity, String text,
            int weatherCode, int alertSeverity, float alpha, boolean privacyRedacted) {
        this(kind, iconKind, identity, text, weatherCode, alertSeverity, alpha, privacyRedacted,
                LiveUpdateKind.NONE, "", -1, 0L, false);
    }

    private ContextualAtAGlanceCard(Kind kind, IconKind iconKind, String identity, String text,
            int weatherCode, int alertSeverity, float alpha, boolean privacyRedacted,
            LiveUpdateKind liveUpdateKind, String liveUpdateMetricText,
            int liveUpdateProgressPercent, long liveUpdateTimeBaseElapsedRealtime,
            boolean liveUpdateCountDown) {
        this.kind = kind;
        this.iconKind = iconKind;
        this.identity = identity != null ? identity : "";
        this.text = text != null ? text : "";
        this.weatherCode = weatherCode;
        this.alertSeverity = Math.max(0, alertSeverity);
        this.alpha = alpha;
        this.privacyRedacted = privacyRedacted;
        this.liveUpdateKind = liveUpdateKind != null ? liveUpdateKind : LiveUpdateKind.NONE;
        this.liveUpdateMetricText = liveUpdateMetricText != null ? liveUpdateMetricText : "";
        this.liveUpdateProgressPercent = liveUpdateProgressPercent >= 0
                ? Math.min(100, liveUpdateProgressPercent) : -1;
        this.liveUpdateTimeBaseElapsedRealtime = Math.max(0L, liveUpdateTimeBaseElapsedRealtime);
        this.liveUpdateCountDown = liveUpdateCountDown;
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

    static ContextualAtAGlanceCard nativeSmartspace(String identity, String text, float alpha) {
        String normalized = PixelAodRenderModel.normalizeAtAGlanceExtra(text);
        if (normalized.isEmpty()) {
            return none();
        }
        String key = identity != null && !identity.trim().isEmpty()
                ? identity.trim() : "native-smartspace:" + normalized;
        return new ContextualAtAGlanceCard(Kind.NATIVE_SMARTSPACE, IconKind.NONE,
                key, normalized, BreezyWeatherForecast.UNKNOWN_WEATHER_CODE,
                0, alpha, false);
    }

    static ContextualAtAGlanceCard liveUpdate(String identity, String text, float alpha) {
        return liveUpdate(identity, LiveUpdateKind.NONE, text, "", -1, 0L, false, alpha);
    }

    static ContextualAtAGlanceCard liveUpdate(String identity, LiveUpdateKind liveUpdateKind,
            String label, String metricText, int progressPercent, long timeBaseElapsedRealtime,
            boolean countDown, float alpha) {
        String normalizedLabel = PixelAodRenderModel.normalizeAtAGlanceExtra(label);
        String normalizedMetric = PixelAodRenderModel.normalizeAtAGlanceExtra(metricText);
        if (normalizedLabel.isEmpty()) {
            return none();
        }
        String key = identity != null && !identity.trim().isEmpty()
                ? identity.trim() : "live-update:" + normalizedLabel;
        return new ContextualAtAGlanceCard(Kind.LIVE_UPDATE, liveUpdateIconKind(liveUpdateKind),
                key, normalizedLabel, BreezyWeatherForecast.UNKNOWN_WEATHER_CODE,
                0, alpha, false, liveUpdateKind, normalizedMetric, progressPercent,
                timeBaseElapsedRealtime, countDown);
    }

    private static IconKind liveUpdateIconKind(LiveUpdateKind kind) {
        if (kind == null) {
            return IconKind.NONE;
        }
        switch (kind) {
            case TIMER:
                return IconKind.LIVE_TIMER;
            case HOTSPOT:
                return IconKind.LIVE_HOTSPOT;
            case PROGRESS:
                return IconKind.LIVE_PROGRESS;
            case CALL:
                return IconKind.LIVE_CALL;
            default:
                return IconKind.NONE;
        }
    }

    boolean isDynamicLiveUpdate() {
        return kind == Kind.LIVE_UPDATE
                && (liveUpdateKind == LiveUpdateKind.TIMER || liveUpdateKind == LiveUpdateKind.CALL)
                && liveUpdateTimeBaseElapsedRealtime > 0L;
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
                && privacyRedacted == other.privacyRedacted
                && liveUpdateKind == other.liveUpdateKind
                && liveUpdateMetricText.equals(other.liveUpdateMetricText)
                && liveUpdateProgressPercent == other.liveUpdateProgressPercent
                && liveUpdateTimeBaseElapsedRealtime == other.liveUpdateTimeBaseElapsedRealtime
                && liveUpdateCountDown == other.liveUpdateCountDown;
    }

    boolean isReplacementOf(ContextualAtAGlanceCard other) {
        return other != null && isVisible() && other.isVisible() && !sameContent(other);
    }
}
