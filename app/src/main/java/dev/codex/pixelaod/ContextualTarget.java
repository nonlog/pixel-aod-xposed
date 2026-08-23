package dev.codex.pixelaod;

/**
 * Normalized contextual arbitration input.
 *
 * <p>Source adapters own data extraction only. They hand one of these immutable targets to the
 * arbiter, which applies common validity, privacy, suppression, presentation and visual-budget
 * policy before the COUI scene owner sees a card.</p>
 */
final class ContextualTarget {
    enum Source {
        NATIVE_AMBIENT_INDICATION(600),
        NATIVE_SMARTSPACE(500),
        LIVE_UPDATE(450),
        MODULE_WEATHER_ALERT(300),
        MODULE_CALENDAR(200),
        MODULE_WEATHER_FORECAST(100);

        final int equivalentAuthority;

        Source(int equivalentAuthority) {
            this.equivalentAuthority = equivalentAuthority;
        }
    }

    enum Urgency {
        LOW(100),
        NORMAL(200),
        HIGH(300),
        CRITICAL(400);

        final int rank;

        Urgency(int rank) {
            this.rank = rank;
        }
    }

    static final int CONTEXTUAL_ROW_COST = 1;

    final Source source;
    final Urgency urgency;
    final String semanticKey;
    final ContextualAtAGlanceCard card;
    final long validFromMillis;
    final long expiresAtMillis;
    final boolean selectedUserEligible;
    final boolean privacyEligible;
    final boolean suppressionEligible;
    final boolean presentationEligible;
    final int visualBudgetUnits;

    ContextualTarget(Source source, Urgency urgency, String semanticKey,
            ContextualAtAGlanceCard card, long validFromMillis, long expiresAtMillis,
            boolean selectedUserEligible, boolean privacyEligible, boolean suppressionEligible,
            boolean presentationEligible, int visualBudgetUnits) {
        this.source = source != null ? source : Source.MODULE_WEATHER_FORECAST;
        this.urgency = urgency != null ? urgency : Urgency.LOW;
        this.semanticKey = normalizeKey(semanticKey, card);
        this.card = card != null ? card : ContextualAtAGlanceCard.none();
        this.validFromMillis = Math.max(0L, validFromMillis);
        this.expiresAtMillis = Math.max(0L, expiresAtMillis);
        this.selectedUserEligible = selectedUserEligible;
        this.privacyEligible = privacyEligible;
        this.suppressionEligible = suppressionEligible;
        this.presentationEligible = presentationEligible;
        this.visualBudgetUnits = Math.max(1, visualBudgetUnits);
    }

    static ContextualTarget moduleWeatherAlert(ContextualAtAGlanceCard card,
            BreezyWeatherAlert alert, long expiresAtMillis, boolean suppressionEligible) {
        int severity = alert != null ? alert.severity : 0;
        Urgency urgency = severity >= 3 ? Urgency.CRITICAL : Urgency.HIGH;
        return new ContextualTarget(Source.MODULE_WEATHER_ALERT, urgency,
                card != null ? card.identity : "", card, 0L, expiresAtMillis,
                true, true, suppressionEligible, true, CONTEXTUAL_ROW_COST);
    }

    static ContextualTarget moduleCalendar(ContextualAtAGlanceCard card,
            boolean suppressionEligible) {
        return new ContextualTarget(Source.MODULE_CALENDAR, Urgency.NORMAL,
                card != null ? card.identity : "", card, 0L, 0L,
                true, true, suppressionEligible, true, CONTEXTUAL_ROW_COST);
    }

    static ContextualTarget moduleForecast(ContextualAtAGlanceCard card, long expiresAtMillis,
            boolean suppressionEligible) {
        return new ContextualTarget(Source.MODULE_WEATHER_FORECAST, Urgency.LOW,
                card != null ? card.identity : "", card, 0L, expiresAtMillis,
                true, true, suppressionEligible, true, CONTEXTUAL_ROW_COST);
    }

    boolean isEligibleAt(long nowMillis, int availableBudgetUnits) {
        if (!selectedUserEligible || !privacyEligible || !suppressionEligible
                || !presentationEligible || !card.isVisible()) {
            return false;
        }
        if (validFromMillis > 0L && nowMillis < validFromMillis) {
            return false;
        }
        if (expiresAtMillis > 0L && nowMillis >= expiresAtMillis) {
            return false;
        }
        return visualBudgetUnits <= Math.max(0, availableBudgetUnits);
    }

    long nextBoundaryAfter(long nowMillis) {
        long next = 0L;
        if (validFromMillis > nowMillis) {
            next = validFromMillis;
        }
        if (expiresAtMillis > nowMillis && (next <= 0L || expiresAtMillis < next)) {
            next = expiresAtMillis;
        }
        return next;
    }

    private static String normalizeKey(String key, ContextualAtAGlanceCard card) {
        String normalized = key == null ? "" : key.trim();
        if (!normalized.isEmpty()) {
            return normalized;
        }
        if (card != null && card.identity != null && !card.identity.trim().isEmpty()) {
            return card.identity.trim();
        }
        return "anonymous";
    }
}
