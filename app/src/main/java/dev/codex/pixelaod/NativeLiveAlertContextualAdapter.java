package dev.codex.pixelaod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only normalization of vendor-classified OPlus Live Alerts into structured semantics.
 *
 * <p>S20.1 deliberately does not render generic vendor View text. OPlus remains the authority for
 * deciding whether an item is a Live Alert; Pixel AOD additionally requires a recognized semantic
 * shape (timer, hotspot, progress, or call) before it is eligible for the contextual row.</p>
 */
final class NativeLiveAlertContextualAdapter {
    static final long FALLBACK_TTL_MILLIS = 24L * 60L * 60L * 1000L;
    static final long DYNAMIC_BASE_DRIFT_TOLERANCE_MILLIS = 2500L;
    static final long TIMER_PROGRESS_MIN_AGE_MILLIS = 3000L;
    static final long TIMER_PROGRESS_MAX_TOTAL_MILLIS = 24L * 60L * 60L * 1000L;

    enum SemanticKind {
        NONE,
        TIMER,
        HOTSPOT,
        PROGRESS,
        CALL
    }

    private static final Map<String, Snapshot> aodSnapshots = new LinkedHashMap<>();
    private static final Map<String, Snapshot> vendorLiveSnapshots = new LinkedHashMap<>();
    private static Set<String> activeAodServiceIds = Collections.emptySet();
    private static boolean activeAodSetObserved;
    private static String currentSignature = "";

    private NativeLiveAlertContextualAdapter() {
    }

    static synchronized Update observeActiveSet(Map<?, ?> rawActive, long nowMillis,
            String source) {
        Set<String> nextActive = new LinkedHashSet<>();
        if (rawActive != null) {
            for (Object key : rawActive.keySet()) {
                String id = normalize(key);
                if (!id.isEmpty()) {
                    nextActive.add(id);
                }
            }
        }
        activeAodSetObserved = true;
        activeAodServiceIds = Collections.unmodifiableSet(nextActive);
        aodSnapshots.entrySet().removeIf(entry -> !nextActive.contains(entry.getKey()));
        for (String serviceId : nextActive) {
            Snapshot vendor = vendorLiveSnapshots.get(serviceId);
            if (vendor != null) {
                aodSnapshots.put(serviceId, vendor.withAuthority(Authority.AOD_FINAL));
            }
        }
        return update(nowMillis, source, "aod-active-set", "", false, false);
    }

    /**
     * Final OPlus AOD payloads only upgrade an already structured vendor model. Raw title/des are
     * not a semantic contract and are therefore never turned into a generic contextual string.
     */
    static synchronized Update observeAodData(Map<?, ?> rawData, long nowMillis, String source) {
        if (rawData == null) {
            return update(nowMillis, source, "aod-data", "", false, false);
        }
        String serviceId = normalize(rawData.get("service_id"));
        String packageName = normalize(rawData.get("package_name"));
        boolean shouldShow = booleanValue(rawData.get("should_show"), false);
        if (!serviceId.isEmpty()) {
            if (!shouldShow) {
                aodSnapshots.remove(serviceId);
            } else {
                Snapshot structured = vendorLiveSnapshots.get(serviceId);
                if (structured != null
                        && (!activeAodSetObserved || activeAodServiceIds.contains(serviceId))) {
                    aodSnapshots.put(serviceId, structured.withAuthority(Authority.AOD_FINAL));
                }
            }
        }
        return update(nowMillis, source, "aod-data", packageName, false, false);
    }

    /** Replaces one full, already-ranked OPlus Live Alert snapshot. */
    static synchronized Update observeVendorLiveSet(List<VendorLivePayload> payloads,
            long nowMillis, String source) {
        Map<String, Snapshot> previous = new LinkedHashMap<>(vendorLiveSnapshots);
        vendorLiveSnapshots.clear();
        String firstPackage = "";
        int structuredCount = 0;
        if (payloads != null) {
            for (VendorLivePayload payload : payloads) {
                if (payload == null || !payload.shouldShow || payload.semanticKind == SemanticKind.NONE) {
                    continue;
                }
                String serviceId = normalize(payload.serviceId);
                if (serviceId.isEmpty() || vendorLiveSnapshots.containsKey(serviceId)) {
                    continue;
                }
                Snapshot snapshot = Snapshot.fromPayload(payload, nowMillis, previous.get(serviceId));
                if (snapshot == null) {
                    continue;
                }
                if (firstPackage.isEmpty()) {
                    firstPackage = snapshot.packageName;
                }
                vendorLiveSnapshots.put(serviceId, snapshot);
                structuredCount++;
                if (activeAodSetObserved && activeAodServiceIds.contains(serviceId)) {
                    aodSnapshots.put(serviceId, snapshot.withAuthority(Authority.AOD_FINAL));
                }
            }
        }
        // A full vendor snapshot owns removal. Do not retain a final snapshot whose structured
        // source disappeared merely because OPlus missed one final AOD callback.
        aodSnapshots.entrySet().removeIf(entry -> !vendorLiveSnapshots.containsKey(entry.getKey())
                || (activeAodSetObserved && !activeAodServiceIds.contains(entry.getKey())));
        return update(nowMillis, source, "vendor-live-set", firstPackage,
                structuredCount > 0, false);
    }

    static synchronized List<ContextualTarget> currentTargets(boolean suppressionEligible,
            boolean sensitiveContentHidden, long nowMillis) {
        prune(nowMillis);
        if (aodSnapshots.isEmpty() && vendorLiveSnapshots.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContextualTarget> targets = new ArrayList<>(
                aodSnapshots.size() + vendorLiveSnapshots.size());
        for (Snapshot snapshot : aodSnapshots.values()) {
            ContextualTarget target = snapshot != null ? snapshot.toContextualTarget(
                    suppressionEligible, sensitiveContentHidden, nowMillis) : null;
            if (target != null) {
                targets.add(target);
            }
        }
        for (Snapshot snapshot : vendorLiveSnapshots.values()) {
            if (snapshot == null || aodSnapshots.containsKey(snapshot.serviceId)) {
                continue;
            }
            ContextualTarget target = snapshot.toContextualTarget(
                    suppressionEligible, sensitiveContentHidden, nowMillis);
            if (target != null) {
                targets.add(target);
            }
        }
        return targets;
    }

    static synchronized void clear(String source) {
        aodSnapshots.clear();
        vendorLiveSnapshots.clear();
        activeAodServiceIds = Collections.emptySet();
        activeAodSetObserved = false;
        currentSignature = "";
    }

    static long parseTimerRemainingMillis(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return -1L;
        }
        Pattern duration = Pattern.compile("(?<!\\d)(\\d{1,3}):(\\d{2})(?::(\\d{2}))?(?!\\d)");
        for (String raw : texts) {
            String text = raw != null ? raw.trim() : "";
            Matcher matcher = duration.matcher(text);
            if (matcher.find()) {
                long first = parseLongOrMinusOne(matcher.group(1));
                long second = parseLongOrMinusOne(matcher.group(2));
                String thirdRaw = matcher.group(3);
                if (first < 0L || second < 0L || second >= 60L) {
                    continue;
                }
                long seconds;
                if (thirdRaw != null) {
                    long third = parseLongOrMinusOne(thirdRaw);
                    if (third < 0L || third >= 60L) {
                        continue;
                    }
                    seconds = first * 3600L + second * 60L + third;
                } else {
                    seconds = first * 60L + second;
                }
                if (seconds > 0L) {
                    return seconds * 1000L;
                }
            }
        }
        for (int start = 0; start < texts.size(); start++) {
            StringBuilder candidate = new StringBuilder();
            for (int index = start; index < texts.size() && index < start + 5; index++) {
                String piece = texts.get(index) != null ? texts.get(index).trim() : "";
                if (!piece.matches("\\d{1,3}|:")) {
                    break;
                }
                candidate.append(piece);
                if (duration.matcher(candidate.toString()).matches()) {
                    return parseTimerRemainingMillis(Collections.singletonList(candidate.toString()));
                }
            }
        }
        return -1L;
    }

    static int timerRemainingPercent(long startedAtMillis, long nowMillis, long remainingMillis) {
        if (startedAtMillis <= 0L || nowMillis <= 0L || remainingMillis <= 0L) {
            return -1;
        }
        long elapsed = nowMillis - startedAtMillis;
        if (elapsed < TIMER_PROGRESS_MIN_AGE_MILLIS
                || elapsed > TIMER_PROGRESS_MAX_TOTAL_MILLIS) {
            return -1;
        }
        long total;
        try {
            total = Math.addExact(elapsed, remainingMillis);
        } catch (ArithmeticException overflow) {
            return -1;
        }
        if (total <= 0L || total > TIMER_PROGRESS_MAX_TOTAL_MILLIS) {
            return -1;
        }
        return Math.max(0, Math.min(100,
                Math.round((remainingMillis * 100f) / total)));
    }
    static int parseConnectedDeviceCount(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return -1;
        }
        Pattern deviceCount = Pattern.compile("(?i)(\\d{1,3})\\s*(?:device|devices|设备|台)");
        for (String raw : texts) {
            String text = raw != null ? raw.trim() : "";
            Matcher matcher = deviceCount.matcher(text);
            if (matcher.find()) {
                long value = parseLongOrMinusOne(matcher.group(1));
                if (value >= 0L && value <= 999L) {
                    return (int) value;
                }
            }
        }
        return -1;
    }

    static String progressSemanticLabel(String rawTitle) {
        String normalized = rawTitle != null ? rawTitle.trim().toLowerCase(java.util.Locale.US) : "";
        if (normalized.contains("install")) {
            return "Installing";
        }
        if (normalized.contains("download")) {
            return "Downloading";
        }
        if (normalized.contains("upload")) {
            return "Uploading";
        }
        return "Progress";
    }

    private static long parseLongOrMinusOne(String value) {
        try {
            return Long.parseLong(value);
        } catch (Throwable ignored) {
            return -1L;
        }
    }
    private static Update update(long nowMillis, String source, String event,
            String packageName, boolean titlePresent, boolean descriptionPresent) {
        prune(nowMillis);
        String signature = signature();
        boolean changed = !signature.equals(currentSignature);
        currentSignature = signature;
        return new Update(changed, activeAodSetObserved, activeAodServiceIds.size(),
                aodSnapshots.size(), vendorLiveSnapshots.size(), source, event, packageName,
                titlePresent, descriptionPresent);
    }

    private static void prune(long nowMillis) {
        long now = Math.max(0L, nowMillis);
        aodSnapshots.entrySet().removeIf(entry -> entry.getValue() == null
                || entry.getValue().expiresAtMillis() <= now
                || (activeAodSetObserved && !activeAodServiceIds.contains(entry.getKey())));
        vendorLiveSnapshots.entrySet().removeIf(entry -> entry.getValue() == null
                || entry.getValue().expiresAtMillis() <= now);
    }

    private static String signature() {
        StringBuilder out = new StringBuilder();
        out.append(activeAodSetObserved).append(':').append(activeAodServiceIds.size());
        appendSignature(out, "a", aodSnapshots);
        appendSignature(out, "v", vendorLiveSnapshots);
        return out.toString();
    }

    private static void appendSignature(StringBuilder out, String prefix,
            Map<String, Snapshot> values) {
        for (Snapshot snapshot : values.values()) {
            out.append('|').append(prefix).append(':').append(snapshot.serviceId).append(':')
                    .append(snapshot.packageName).append(':').append(snapshot.semanticKind).append(':')
                    .append(snapshot.label.hashCode()).append(':')
                    .append(snapshot.metricText.hashCode()).append(':')
                    .append(snapshot.progressPercent).append(':')
                    .append(snapshot.timeBaseElapsedRealtime).append(':')
                    .append(snapshot.countDown).append(':').append(snapshot.milestone);
        }
    }

    private static String normalize(Object value) {
        return value != null ? String.valueOf(value).trim() : "";
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    enum Authority {
        AOD_FINAL,
        VENDOR_LIVE
    }

    static final class VendorLivePayload {
        final String serviceId;
        final String packageName;
        final SemanticKind semanticKind;
        final String label;
        final String metricText;
        final int progressPercent;
        final long timeBaseElapsedRealtime;
        final boolean countDown;
        final boolean shouldShow;
        final boolean milestone;

        VendorLivePayload(String serviceId, String packageName, SemanticKind semanticKind,
                String label, String metricText, int progressPercent,
                long timeBaseElapsedRealtime, boolean countDown,
                boolean shouldShow, boolean milestone) {
            this.serviceId = serviceId != null ? serviceId : "";
            this.packageName = packageName != null ? packageName : "";
            this.semanticKind = semanticKind != null ? semanticKind : SemanticKind.NONE;
            this.label = label != null ? label : "";
            this.metricText = metricText != null ? metricText : "";
            this.progressPercent = progressPercent >= 0 ? Math.min(100, progressPercent) : -1;
            this.timeBaseElapsedRealtime = Math.max(0L, timeBaseElapsedRealtime);
            this.countDown = countDown;
            this.shouldShow = shouldShow;
            this.milestone = milestone;
        }
    }

    static final class Snapshot {
        final String serviceId;
        final String packageName;
        final SemanticKind semanticKind;
        final String label;
        final String metricText;
        final int progressPercent;
        final long timeBaseElapsedRealtime;
        final boolean countDown;
        final long receivedAtMillis;
        final boolean milestone;
        final Authority authority;

        Snapshot(String serviceId, String packageName, SemanticKind semanticKind, String label,
                String metricText, int progressPercent, long timeBaseElapsedRealtime,
                boolean countDown, long receivedAtMillis, boolean milestone, Authority authority) {
            this.serviceId = serviceId != null ? serviceId : "";
            this.packageName = packageName != null ? packageName : "";
            this.semanticKind = semanticKind != null ? semanticKind : SemanticKind.NONE;
            this.label = PixelAodRenderModel.normalizeAtAGlanceExtra(label != null ? label : "");
            this.metricText = PixelAodRenderModel.normalizeAtAGlanceExtra(
                    metricText != null ? metricText : "");
            this.progressPercent = progressPercent >= 0 ? Math.min(100, progressPercent) : -1;
            this.timeBaseElapsedRealtime = Math.max(0L, timeBaseElapsedRealtime);
            this.countDown = countDown;
            this.receivedAtMillis = Math.max(0L, receivedAtMillis);
            this.milestone = milestone;
            this.authority = authority != null ? authority : Authority.VENDOR_LIVE;
        }

        static Snapshot fromPayload(VendorLivePayload payload, long nowMillis, Snapshot previous) {
            String serviceId = normalize(payload.serviceId);
            String label = normalize(payload.label);
            if (serviceId.isEmpty() || label.isEmpty() || payload.semanticKind == SemanticKind.NONE) {
                return null;
            }
            boolean dynamic = payload.semanticKind == SemanticKind.TIMER
                    || (payload.semanticKind == SemanticKind.CALL
                    && payload.timeBaseElapsedRealtime > 0L);
            long timeBase = dynamic ? payload.timeBaseElapsedRealtime : 0L;
            if (payload.semanticKind == SemanticKind.TIMER && timeBase <= 0L) {
                return null;
            }
            if (dynamic && previous != null && previous.semanticKind == payload.semanticKind
                    && previous.countDown == payload.countDown
                    && previous.timeBaseElapsedRealtime > 0L
                    && Math.abs(previous.timeBaseElapsedRealtime - timeBase)
                    <= DYNAMIC_BASE_DRIFT_TOLERANCE_MILLIS) {
                timeBase = previous.timeBaseElapsedRealtime;
            }
            return new Snapshot(serviceId, normalize(payload.packageName), payload.semanticKind,
                    label, normalize(payload.metricText), payload.progressPercent, timeBase,
                    payload.countDown, nowMillis, payload.milestone, Authority.VENDOR_LIVE);
        }

        Snapshot withAuthority(Authority nextAuthority) {
            return new Snapshot(serviceId, packageName, semanticKind, label, metricText,
                    progressPercent, timeBaseElapsedRealtime, countDown, receivedAtMillis,
                    milestone, nextAuthority);
        }

        long expiresAtMillis() {
            return receivedAtMillis + FALLBACK_TTL_MILLIS;
        }

        ContextualTarget toContextualTarget(boolean suppressionEligible,
                boolean sensitiveContentHidden, long nowMillis) {
            if (serviceId.isEmpty() || label.isEmpty() || semanticKind == SemanticKind.NONE
                    || nowMillis >= expiresAtMillis()) {
                return null;
            }
            ContextualAtAGlanceCard.LiveUpdateKind cardKind;
            ContextualTarget.Urgency urgency;
            switch (semanticKind) {
                case TIMER:
                    cardKind = ContextualAtAGlanceCard.LiveUpdateKind.TIMER;
                    urgency = ContextualTarget.Urgency.HIGH;
                    break;
                case CALL:
                    cardKind = ContextualAtAGlanceCard.LiveUpdateKind.CALL;
                    urgency = ContextualTarget.Urgency.CRITICAL;
                    break;
                case PROGRESS:
                    cardKind = ContextualAtAGlanceCard.LiveUpdateKind.PROGRESS;
                    urgency = ContextualTarget.Urgency.HIGH;
                    break;
                case HOTSPOT:
                    cardKind = ContextualAtAGlanceCard.LiveUpdateKind.HOTSPOT;
                    urgency = ContextualTarget.Urgency.NORMAL;
                    break;
                default:
                    return null;
            }
            String semanticKey = "live-update:" + serviceId;
            ContextualAtAGlanceCard card = ContextualAtAGlanceCard.liveUpdate(
                    semanticKey, cardKind, label, metricText, progressPercent,
                    timeBaseElapsedRealtime, countDown, 1f);
            if (!card.isVisible()) {
                return null;
            }
            return new ContextualTarget(ContextualTarget.Source.LIVE_UPDATE, urgency,
                    semanticKey, card, 0L, expiresAtMillis(), true,
                    !sensitiveContentHidden, suppressionEligible, true,
                    ContextualTarget.CONTEXTUAL_ROW_COST);
        }
    }

    static final class Update {
        final boolean changed;
        final boolean activeSetObserved;
        final int activeCount;
        final int aodSnapshotCount;
        final int vendorSnapshotCount;
        final int snapshotCount;
        final String source;
        final String event;
        final String packageName;
        final boolean titlePresent;
        final boolean descriptionPresent;

        Update(boolean changed, boolean activeSetObserved, int activeCount, int aodSnapshotCount,
                int vendorSnapshotCount, String source, String event, String packageName,
                boolean titlePresent, boolean descriptionPresent) {
            this.changed = changed;
            this.activeSetObserved = activeSetObserved;
            this.activeCount = Math.max(0, activeCount);
            this.aodSnapshotCount = Math.max(0, aodSnapshotCount);
            this.vendorSnapshotCount = Math.max(0, vendorSnapshotCount);
            this.snapshotCount = this.aodSnapshotCount + this.vendorSnapshotCount;
            this.source = source != null ? source : "unknown";
            this.event = event != null ? event : "unknown";
            this.packageName = packageName != null ? packageName : "";
            this.titlePresent = titlePresent;
            this.descriptionPresent = descriptionPresent;
        }
    }
}

