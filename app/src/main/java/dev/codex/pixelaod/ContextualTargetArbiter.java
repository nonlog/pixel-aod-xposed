package dev.codex.pixelaod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One deterministic owner for contextual eligibility, dedupe, ranking and row budget. */
final class ContextualTargetArbiter {
    static final int DEFAULT_VISUAL_BUDGET_UNITS = ContextualTarget.CONTEXTUAL_ROW_COST;

    private ContextualTargetArbiter() {
    }

    static Selection select(List<ContextualTarget> inputs, long nowMillis) {
        return select(inputs, nowMillis, DEFAULT_VISUAL_BUDGET_UNITS);
    }

    static Selection select(List<ContextualTarget> inputs, long nowMillis,
            int visualBudgetUnits) {
        int budget = Math.max(0, visualBudgetUnits);
        List<ContextualTarget> eligible = new ArrayList<>();
        long nextBoundary = 0L;
        if (inputs != null) {
            for (ContextualTarget target : inputs) {
                if (target == null) {
                    continue;
                }
                nextBoundary = earlierFuture(nextBoundary,
                        target.nextBoundaryAfter(nowMillis), nowMillis);
                if (target.isEligibleAt(nowMillis, budget)) {
                    eligible.add(target);
                }
            }
        }

        // Dedupe only after eligibility. This is what makes a module target a real fallback when
        // an equivalent native target is stale, privacy-blocked, suppressed or otherwise invalid.
        Map<String, ContextualTarget> bySemanticKey = new LinkedHashMap<>();
        for (ContextualTarget target : eligible) {
            ContextualTarget existing = bySemanticKey.get(target.semanticKey);
            if (existing == null || preferEquivalent(target, existing) < 0) {
                bySemanticKey.put(target.semanticKey, target);
            }
        }

        List<ContextualTarget> deduped = new ArrayList<>(bySemanticKey.values());
        deduped.sort(ContextualTargetArbiter::compareForPresentation);
        ContextualTarget selected = deduped.isEmpty() ? null : deduped.get(0);
        return new Selection(selected, eligible.size(), deduped.size(), budget, nextBoundary);
    }

    private static int preferEquivalent(ContextualTarget left, ContextualTarget right) {
        int authority = Integer.compare(right.source.equivalentAuthority,
                left.source.equivalentAuthority);
        if (authority != 0) {
            return authority;
        }
        return compareForPresentation(left, right);
    }

    private static int compareForPresentation(ContextualTarget left, ContextualTarget right) {
        int urgency = Integer.compare(right.urgency.rank, left.urgency.rank);
        if (urgency != 0) {
            return urgency;
        }
        long leftExpiry = left.expiresAtMillis > 0L ? left.expiresAtMillis : Long.MAX_VALUE;
        long rightExpiry = right.expiresAtMillis > 0L ? right.expiresAtMillis : Long.MAX_VALUE;
        int expiry = Long.compare(leftExpiry, rightExpiry);
        if (expiry != 0) {
            return expiry;
        }
        int authority = Integer.compare(right.source.equivalentAuthority,
                left.source.equivalentAuthority);
        if (authority != 0) {
            return authority;
        }
        int source = left.source.name().compareTo(right.source.name());
        if (source != 0) {
            return source;
        }
        return left.semanticKey.compareTo(right.semanticKey);
    }

    private static long earlierFuture(long current, long candidate, long nowMillis) {
        if (candidate <= nowMillis) {
            return current;
        }
        if (current <= nowMillis || candidate < current) {
            return candidate;
        }
        return current;
    }

    static final class Selection {
        final ContextualTarget target;
        final int eligibleCount;
        final int dedupedCount;
        final int visualBudgetUnits;
        final long nextDeadlineMillis;

        Selection(ContextualTarget target, int eligibleCount, int dedupedCount,
                int visualBudgetUnits, long nextDeadlineMillis) {
            this.target = target;
            this.eligibleCount = Math.max(0, eligibleCount);
            this.dedupedCount = Math.max(0, dedupedCount);
            this.visualBudgetUnits = Math.max(0, visualBudgetUnits);
            this.nextDeadlineMillis = Math.max(0L, nextDeadlineMillis);
        }

        ContextualAtAGlanceCard card() {
            return target != null ? target.card : ContextualAtAGlanceCard.none();
        }
    }
}
