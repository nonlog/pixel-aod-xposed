package dev.codex.pixelaod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only normalization of the filtered lockscreen Smartspace target stream owned by SystemUI.
 *
 * <p>The current OOS seam is {@code LockscreenSmartspaceController#addListener(...)}. Its plugin
 * listener receives the list only after SystemUI has applied selected-user, managed-profile and
 * sensitive-content filtering. This adapter never creates a Smartspace session and never invokes a
 * target action.</p>
 */
final class NativeSmartspaceContextualAdapter {
    static final int FEATURE_WEATHER = 1;
    static final int FEATURE_CALENDAR = 2;
    static final int FEATURE_WEATHER_ALERT = 10;
    static final long FALLBACK_TTL_MILLIS = 60L * 60L * 1000L;

    private static List<Snapshot> currentSnapshots = Collections.emptyList();
    private static String currentSignature = "";

    private NativeSmartspaceContextualAdapter() {
    }

    static synchronized Update observeFilteredTargets(List<?> rawTargets, long nowMillis,
            String source) {
        long receivedAt = Math.max(0L, nowMillis);
        int rawTargetCount = rawTargets != null ? rawTargets.size() : 0;
        Map<String, Snapshot> byKey = new LinkedHashMap<>();
        Set<Integer> featureTypes = new LinkedHashSet<>();
        if (rawTargets != null) {
            for (Object rawTarget : rawTargets) {
                Snapshot snapshot = snapshotFromFrameworkTarget(rawTarget, receivedAt);
                if (snapshot == null || !shouldIncludeFeature(snapshot.featureType)
                        || snapshot.text.isEmpty()) {
                    continue;
                }
                long effectiveExpiry = snapshot.effectiveExpiryMillis();
                if (effectiveExpiry > 0L && receivedAt >= effectiveExpiry) {
                    continue;
                }
                featureTypes.add(snapshot.featureType);
                byKey.put(snapshot.semanticKey(), snapshot);
            }
        }
        List<Snapshot> next = Collections.unmodifiableList(new ArrayList<>(byKey.values()));
        String signature = signature(next);
        boolean changed = !signature.equals(currentSignature);
        currentSnapshots = next;
        currentSignature = signature;
        return new Update(changed, rawTargetCount, next.size(), featureTypesLabel(featureTypes),
                source != null ? source : "unknown");
    }

    static synchronized List<ContextualTarget> currentTargets(boolean suppressionEligible,
            boolean sensitiveContentHidden) {
        if (currentSnapshots.isEmpty()) {
            return Collections.emptyList();
        }
        List<ContextualTarget> targets = new ArrayList<>(currentSnapshots.size());
        for (Snapshot snapshot : currentSnapshots) {
            ContextualTarget target = snapshot.toContextualTarget(
                    suppressionEligible, sensitiveContentHidden);
            if (target != null) {
                targets.add(target);
            }
        }
        return targets;
    }

    static synchronized void clear(String source) {
        boolean changed = !currentSnapshots.isEmpty() || !currentSignature.isEmpty();
        currentSnapshots = Collections.emptyList();
        currentSignature = "";
        if (changed) {
            PixelAodLog.i("cleared native Smartspace contextual targets source="
                    + (source != null ? source : "unknown"));
        }
    }

    static boolean shouldIncludeFeature(int featureType) {
        // Current weather already owns a dedicated date/weather row. Mirroring Smartspace weather
        // into the one-line contextual budget would duplicate the same information.
        return featureType != FEATURE_WEATHER;
    }

    static String composeDisplayText(String title, String subtitle) {
        String first = normalizeText(title);
        String second = normalizeText(subtitle);
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty() || first.equals(second)) {
            return first;
        }
        return first + " · " + second;
    }

    static String semanticKeyFor(int featureType, String targetId, String text) {
        String normalized = PixelAodRenderModel.normalizeAtAGlanceExtra(text);
        if (featureType == FEATURE_CALENDAR && !normalized.isEmpty()) {
            return ContextualAtAGlanceCard.calendar(normalized, 1f).identity;
        }
        String id = targetId != null ? targetId.trim() : "";
        if (!id.isEmpty()) {
            return "native-smartspace:" + id;
        }
        return "native-smartspace:" + normalized;
    }

    private static Snapshot snapshotFromFrameworkTarget(Object target, long receivedAtMillis) {
        if (target == null) {
            return null;
        }
        int featureType = intValue(invokeNoArg(target, "getFeatureType"), -1);
        String targetId = stringValue(invokeNoArg(target, "getSmartspaceTargetId"));
        long expiryTimeMillis = longValue(invokeNoArg(target, "getExpiryTimeMillis"), 0L);
        boolean sensitive = booleanValue(invokeNoArg(target, "isSensitive"), false);

        Object headerAction = invokeNoArg(target, "getHeaderAction");
        Object baseAction = invokeNoArg(target, "getBaseAction");
        String title = firstNonEmpty(actionText(headerAction, "getTitle"),
                actionText(baseAction, "getTitle"));
        String subtitle = firstNonEmpty(actionText(headerAction, "getSubtitle"),
                actionText(baseAction, "getSubtitle"));
        String text = composeDisplayText(title, subtitle);
        if (text.isEmpty()) {
            text = templateDisplayText(target);
        }
        if (text.isEmpty()) {
            return null;
        }
        return new Snapshot(targetId, featureType, text, receivedAtMillis,
                expiryTimeMillis, sensitive);
    }

    private static String templateDisplayText(Object target) {
        Object template = invokeNoArg(target, "getTemplateData");
        if (template == null) {
            return "";
        }
        String primary = templateItemText(template, "getPrimaryItem");
        String subtitle = firstNonEmpty(
                templateItemText(template, "getSubtitleItem"),
                templateItemText(template, "getSupplementalLineItem"));
        return composeDisplayText(primary, subtitle);
    }

    private static String templateItemText(Object template, String getter) {
        Object item = invokeNoArg(template, getter);
        if (item == null) {
            return "";
        }
        Object text = invokeNoArg(item, "getText");
        if (text == null) {
            return "";
        }
        if (text instanceof CharSequence) {
            return normalizeText(text.toString());
        }
        Object nested = invokeNoArg(text, "getText");
        return nested != null ? normalizeText(nested.toString()) : "";
    }

    private static String actionText(Object action, String getter) {
        Object value = invokeNoArg(action, getter);
        return value != null ? normalizeText(value.toString()) : "";
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String firstNonEmpty(String first, String second) {
        return first != null && !first.isEmpty() ? first : second != null ? second : "";
    }

    private static String normalizeText(String text) {
        return PixelAodRenderModel.normalizeAtAGlanceExtra(text != null ? text : "");
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long longValue(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : "";
    }

    private static String signature(List<Snapshot> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (Snapshot snapshot : snapshots) {
            if (out.length() > 0) {
                out.append('|');
            }
            out.append(snapshot.semanticKey()).append(':')
                    .append(snapshot.featureType).append(':')
                    .append(snapshot.effectiveExpiryMillis()).append(':')
                    .append(snapshot.sensitive).append(':')
                    .append(snapshot.text.hashCode());
        }
        return out.toString();
    }

    private static String featureTypesLabel(Set<Integer> featureTypes) {
        if (featureTypes == null || featureTypes.isEmpty()) {
            return "none";
        }
        StringBuilder out = new StringBuilder();
        for (Integer type : featureTypes) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(type != null ? type : -1);
        }
        return out.toString();
    }

    static final class Snapshot {
        final String targetId;
        final int featureType;
        final String text;
        final long receivedAtMillis;
        final long expiryTimeMillis;
        final boolean sensitive;

        Snapshot(String targetId, int featureType, String text, long receivedAtMillis,
                long expiryTimeMillis, boolean sensitive) {
            this.targetId = targetId != null ? targetId : "";
            this.featureType = featureType;
            this.text = normalizeText(text);
            this.receivedAtMillis = Math.max(0L, receivedAtMillis);
            this.expiryTimeMillis = Math.max(0L, expiryTimeMillis);
            this.sensitive = sensitive;
        }

        long effectiveExpiryMillis() {
            return expiryTimeMillis > 0L
                    ? expiryTimeMillis : receivedAtMillis + FALLBACK_TTL_MILLIS;
        }

        String semanticKey() {
            return semanticKeyFor(featureType, targetId, text);
        }

        ContextualTarget toContextualTarget(boolean suppressionEligible,
                boolean sensitiveContentHidden) {
            ContextualAtAGlanceCard card = ContextualAtAGlanceCard.nativeSmartspace(
                    semanticKey(), text, 1f);
            if (!card.isVisible()) {
                return null;
            }
            ContextualTarget.Urgency urgency = featureType == FEATURE_WEATHER_ALERT
                    ? ContextualTarget.Urgency.HIGH : ContextualTarget.Urgency.NORMAL;
            return new ContextualTarget(ContextualTarget.Source.NATIVE_SMARTSPACE, urgency,
                    semanticKey(), card, 0L, effectiveExpiryMillis(),
                    true, !sensitive || !sensitiveContentHidden, suppressionEligible,
                    true, ContextualTarget.CONTEXTUAL_ROW_COST);
        }
    }

    static final class Update {
        final boolean changed;
        final int rawTargetCount;
        final int targetCount;
        final String featureTypes;
        final String source;

        Update(boolean changed, int rawTargetCount, int targetCount, String featureTypes,
                String source) {
            this.changed = changed;
            this.rawTargetCount = Math.max(0, rawTargetCount);
            this.targetCount = Math.max(0, targetCount);
            this.featureTypes = featureTypes != null ? featureTypes : "none";
            this.source = source != null ? source : "unknown";
        }
    }
}
