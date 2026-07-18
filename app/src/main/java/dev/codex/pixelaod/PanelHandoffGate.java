package dev.codex.pixelaod;

import java.util.HashSet;
import java.util.Set;

final class PanelHandoffGate {
    private final long holdMillis;
    private long generation;
    private String traceId = "";
    private long openedAtMillis;
    private long deadlineMillis;
    private boolean active;
    private final Set<String> terminalTraceIds = new HashSet<>();

    PanelHandoffGate(long holdMillis) {
        if (holdMillis < 0L) {
            throw new IllegalArgumentException("holdMillis must not be negative");
        }
        this.holdMillis = holdMillis;
    }

    synchronized OpenResult openOrExtend(String requestedTraceId, long nowMillis) {
        String normalizedTraceId = requestedTraceId != null ? requestedTraceId : "";
        boolean sameGeneration = active && traceId.equals(normalizedTraceId);
        if (!active && terminalTraceIds.contains(normalizedTraceId)) {
            return new OpenResult(traceId, generation, openedAtMillis, deadlineMillis,
                    false, false, false);
        }
        boolean replaced = active && !sameGeneration;
        boolean extended = false;
        if (!sameGeneration) {
            generation++;
            traceId = normalizedTraceId;
            openedAtMillis = nowMillis;
            deadlineMillis = nowMillis + holdMillis;
            active = true;
        } else {
            long nextDeadline = nowMillis + holdMillis;
            if (nextDeadline > deadlineMillis) {
                deadlineMillis = nextDeadline;
                extended = true;
            }
        }
        return new OpenResult(traceId, generation, openedAtMillis, deadlineMillis,
                true, replaced, extended);
    }

    synchronized boolean shouldBlockPresentation(String currentTraceId) {
        String normalizedTraceId = currentTraceId != null ? currentTraceId : "";
        return active && traceId.equals(normalizedTraceId);
    }

    synchronized boolean completeIfCurrent(String callbackTraceId, long callbackGeneration,
            long nowMillis) {
        String normalizedTraceId = callbackTraceId != null ? callbackTraceId : "";
        if (!active
                || callbackGeneration != generation
                || !traceId.equals(normalizedTraceId)
                || nowMillis < deadlineMillis) {
            return false;
        }
        active = false;
        terminalTraceIds.add(traceId);
        return true;
    }

    synchronized boolean cancel() {
        boolean cancelled = active;
        active = false;
        if (cancelled) {
            terminalTraceIds.add(traceId);
        }
        return cancelled;
    }

    synchronized boolean cancelIfCurrent(String expectedTraceId, long expectedGeneration) {
        String normalizedTraceId = expectedTraceId != null ? expectedTraceId : "";
        if (!active
                || generation != expectedGeneration
                || !traceId.equals(normalizedTraceId)) {
            return false;
        }
        active = false;
        terminalTraceIds.add(traceId);
        return true;
    }

    static final class OpenResult {
        final String traceId;
        final long generation;
        final long openedAtMillis;
        final long deadlineMillis;
        final boolean accepted;
        final boolean replaced;
        final boolean extended;

        OpenResult(String traceId, long generation, long openedAtMillis, long deadlineMillis,
                boolean accepted, boolean replaced, boolean extended) {
            this.traceId = traceId;
            this.generation = generation;
            this.openedAtMillis = openedAtMillis;
            this.deadlineMillis = deadlineMillis;
            this.accepted = accepted;
            this.replaced = replaced;
            this.extended = extended;
        }
    }
}
