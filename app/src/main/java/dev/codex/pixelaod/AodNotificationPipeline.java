package dev.codex.pixelaod;

import android.app.Notification;
import android.graphics.drawable.Icon;
import android.app.NotificationChannel;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class AodNotificationPipeline {
    static final int NOTIFICATION_FLAG_SILENT = 0x00020000;
    private static final int NOTIFICATION_FLAG_GROUP_SUMMARY = 0x00000200;
    private static final int NOTIFICATION_FLAG_AUTOGROUP_SUMMARY = 0x00000400;
    static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    static final String LOCK_SCREEN_SHOW_NOTIFICATIONS = "lock_screen_show_notifications";
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];

    private AodNotificationPipeline() {
    }

    static StatusBarNotification[] sanitizeNotifications(Context context,
            StatusBarNotification[] notifications,
            Map<String, RankingSnapshot> rankings,
            Map<String, LockscreenVisibilityDecision> lockscreenDecisions,
            boolean lockscreenPolicyEnabled,
            String trace) {
        if (notifications == null || notifications.length == 0) {
            return EMPTY_NOTIFICATIONS;
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>(notifications.length);
        for (StatusBarNotification sbn : notifications) {
            if (isLockscreenVisibleNotification(context, sbn, rankings, lockscreenDecisions,
                    lockscreenPolicyEnabled, trace)) {
                list.add(sbn);
            }
        }
        return list.toArray(new StatusBarNotification[0]);
    }

    static boolean isLockscreenVisibleNotification(Context context, StatusBarNotification sbn,
            Map<String, RankingSnapshot> rankings,
            Map<String, LockscreenVisibilityDecision> lockscreenDecisions,
            boolean lockscreenPolicyEnabled,
            String trace) {
        if (sbn == null || sbn.getNotification() == null || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        boolean testNotification = isTestNotification(sbn);
        if (MODULE_PACKAGE.equals(sbn.getPackageName()) && !testNotification) {
            logFilteredNotification(sbn, "module-package-not-test-notification", trace);
            return false;
        }
        if (isSyntheticAutogroupSummaryFlags(notification.flags)) {
            logFilteredNotification(sbn, "synthetic-autogroup-summary", trace);
            return false;
        }
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)
                || hasMediaSessionExtra(notification)
                || isMediaIconCandidate(sbn)) {
            return false;
        }
        if (!lockscreenNotificationsEnabled(context)) {
            logFilteredNotification(sbn, "global-lockscreen-notifications-disabled", trace);
            return false;
        }
        if (notification.visibility == Notification.VISIBILITY_SECRET) {
            logFilteredNotification(sbn, "notification-visibility-secret", trace);
            return false;
        }
        boolean systemNotification = isSystemNotificationCandidate(sbn);
        boolean oosLiveAlert = isOosLiveAlertNotification(sbn);
        if ("com.android.systemui".equals(sbn.getPackageName())
                && !systemNotification
                && !oosLiveAlert) {
            return false;
        }
        RankingSnapshot ranking = rankings != null ? rankings.get(sbn.getKey()) : null;
        LockscreenVisibilityDecision lockscreenDecision =
                lockscreenDecisions != null ? lockscreenDecisions.get(sbn.getKey()) : null;
        // Parity: anything still shown on the lockscreen must stay eligible for AOD icons.
        // Do not apply a stricter silent/low-importance gate than the lockscreen path.
        boolean lockscreenExplicitlyVisible = isExplicitlyVisibleOnLockscreen(lockscreenDecision);
        boolean systemPackage = isSystemUiOrAndroidPackage(sbn.getPackageName());
        if (!systemNotification && !oosLiveAlert && !lockscreenExplicitlyVisible) {
            // Mirror PixelAodHook lockscreen force-hide: never importance-filter android/SystemUI
            // status rows (hotspot NETWORK_STATUS is importance=2 but still on lockscreen).
            if (!systemPackage) {
                String silentHiddenReason = lockscreenPolicySilentHiddenReason(
                        lockscreenPolicyEnabled,
                        sbn,
                        ranking != null ? ranking.importance
                                : NotificationManagerImportance.UNKNOWN);
                if (silentHiddenReason != null) {
                    logFilteredNotification(sbn, silentHiddenReason + " ranking=" + ranking, trace);
                    return false;
                }
            }
        }
        String rankingHiddenReason = ranking != null ? ranking.hiddenReason() : null;
        if (!systemNotification && !lockscreenExplicitlyVisible
                && rankingHiddenReason != null) {
            logFilteredNotification(sbn, rankingHiddenReason + " ranking=" + ranking, trace);
            return false;
        }
        String lockscreenHiddenReason = lockscreenDecision != null
                ? lockscreenDecision.hiddenReason()
                : null;
        if (!systemNotification && !testNotification && !oosLiveAlert
                && lockscreenHiddenReason != null) {
            logFilteredNotification(sbn, lockscreenHiddenReason
                    + " decision=" + lockscreenDecision
                    + " ranking=" + ranking, trace);
            return false;
        }
        logKeptNotification(sbn, ranking, trace,
                oosLiveAlert ? "oos-live-alert"
                        : (systemNotification ? "system-status"
                                : (lockscreenExplicitlyVisible
                                        ? "lockscreen-explicitly-visible"
                                        : "lockscreen-visible")));
        return true;
    }

    /** True when OOS Keyguard visibility hooks reported the row as not hidden. */
    static boolean isExplicitlyVisibleOnLockscreen(LockscreenVisibilityDecision decision) {
        if (decision == null) {
            return false;
        }
        if (Boolean.TRUE.equals(decision.providerHidden)
                || Boolean.TRUE.equals(decision.filterHidden)) {
            return false;
        }
        return Boolean.FALSE.equals(decision.providerHidden)
                || Boolean.FALSE.equals(decision.filterHidden);
    }

    static boolean isSystemUiOrAndroidPackage(String packageName) {
        return "android".equals(packageName) || "com.android.systemui".equals(packageName);
    }

    static boolean isTestNotification(StatusBarNotification sbn) {
        return sbn != null
                && MODULE_PACKAGE.equals(sbn.getPackageName())
                && TestNotificationReceiver.TEST_TAG.equals(sbn.getTag());
    }

    static boolean isSyntheticAutogroupSummaryFlags(int flags) {
        return (flags & NOTIFICATION_FLAG_GROUP_SUMMARY) != 0
                && (flags & NOTIFICATION_FLAG_AUTOGROUP_SUMMARY) != 0;
    }

    static boolean isLauncherStyleSmallIconResourceName(String resourceName) {
        if (resourceName == null || resourceName.isEmpty()) {
            return false;
        }
        String normalized = resourceName.toLowerCase(Locale.US);
        int slash = normalized.lastIndexOf('/');
        String entryName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return entryName.equals("ic_launcher")
                || entryName.startsWith("ic_launcher_")
                || entryName.equals("launcher_icon")
                || entryName.startsWith("launcher_icon_");
    }

    static String lockscreenPolicySilentHiddenReason(boolean lockscreenPolicyEnabled,
            StatusBarNotification sbn, int importance) {
        if (!lockscreenPolicyEnabled) {
            return null;
        }
        if (sbn == null || sbn.getNotification() == null) {
            return null;
        }
        Notification notification = sbn.getNotification();
        if (shouldHideForLockscreenImportance(notification.flags, importance)) {
            return "lockscreen-policy-ranking-importance-low-or-less importance=" + importance;
        }
        return null;
    }

    /** FLAG_SILENT is intentionally not a visibility policy input; only importance is. */
    static boolean shouldHideForLockscreenImportance(int notificationFlags, int importance) {
        return isLowImportanceForLockscreenPolicy(importance);
    }

    static boolean isLowImportanceForLockscreenPolicy(int importance) {
        return importance != NotificationManagerImportance.UNKNOWN
                && importance <= NotificationManagerImportance.LOW;
    }

    static boolean isExcludedFromLockscreenPolicyOverride(String packageName, String category,
            int visibility, boolean rankingSecret) {
        return isExcludedFromLockscreenPolicyOverride(packageName, category, visibility,
                rankingSecret, false);
    }

    static boolean isExcludedFromLockscreenPolicyOverride(String packageName, String category,
            int visibility, boolean rankingSecret, boolean mediaSessionOrIcon) {
        return isSystemUiOrAndroidPackage(packageName)
                || Notification.CATEGORY_TRANSPORT.equals(category)
                || visibility == Notification.VISIBILITY_SECRET
                || rankingSecret
                || mediaSessionOrIcon;
    }

    static boolean isExcludedFromLockscreenPolicyOverride(StatusBarNotification sbn,
            boolean rankingSecret) {
        if (sbn == null || sbn.getNotification() == null) {
            return true;
        }
        Notification notification = sbn.getNotification();
        return isExcludedFromLockscreenPolicyOverride(sbn.getPackageName(), notification.category,
                notification.visibility, rankingSecret,
                hasMediaSessionExtra(notification) || isMediaIconCandidate(sbn));
    }

    static boolean isOosLiveAlertCarrier(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        try {
            Bundle extras = sbn.getNotification().extras;
            if (extras == null || extras.isEmpty()) {
                return false;
            }
            for (String key : extras.keySet()) {
                String normalized = normalizeOosExtraKey(key);
                if (normalized.contains("opfluid")
                        || normalized.contains("oplusfluid")
                        || normalized.contains("fluidservice")
                        || normalized.contains("livealert")
                        || normalized.contains("seedling")
                        || normalized.contains("capsule")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    static boolean isOosLiveAlertNotification(StatusBarNotification sbn) {
        return isOosLiveAlertCarrier(sbn) || isOosFlashlightLiveAlert(sbn);
    }

    static boolean isOosFlashlightLiveAlert(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || !"com.android.systemui".equals(sbn.getPackageName())) {
            return false;
        }
        if (sbn.getId() == 10011) {
            return true;
        }
        Notification notification = sbn.getNotification();
        String group = notification.getGroup();
        if (group != null) {
            String normalizedGroup = group.toLowerCase(Locale.US);
            if (normalizedGroup.contains("torch") || normalizedGroup.contains("flashlight")) {
                return true;
            }
        }
        String joined = systemNotificationText(sbn);
        if (joined.contains("torch") || joined.contains("flashlight")) {
            return true;
        }
        return extrasContain(sbn, "torch") || extrasContain(sbn, "flashlight");
    }

    static boolean isOosDeskClockLiveAlert(StatusBarNotification sbn) {
        return sbn != null
                && "com.oneplus.deskclock".equals(sbn.getPackageName())
                && isOosLiveAlertCarrier(sbn);
    }

    static String notificationIconDedupeKey(StatusBarNotification sbn) {
        if (sbn == null) {
            return "";
        }
        String packageName = sbn.getPackageName();
        if (isOosLiveAlertNotification(sbn)) {
            return packageName + "|live-alert|" + liveAlertSubtype(sbn) + "|"
                    + stableNotificationIdentity(sbn);
        }
        if (isSystemUiUsbNotification(sbn)) {
            return packageName + "|usb";
        }
        return packageName != null ? packageName : "";
    }

    private static String liveAlertSubtype(StatusBarNotification sbn) {
        if (isOosFlashlightLiveAlert(sbn)) {
            return "flashlight";
        }
        if (isOosDeskClockLiveAlert(sbn)) {
            return "deskclock";
        }
        try {
            Notification notification = sbn.getNotification();
            Bundle extras = notification != null ? notification.extras : null;
            if (extras != null) {
                for (String key : extras.keySet()) {
                    String normalizedKey = normalizeOosExtraKey(key);
                    if (normalizedKey.contains("seedlingevent")) {
                        Object value = extras.get(key);
                        if (value != null) {
                            return normalizeOosExtraKey(String.valueOf(value));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "generic";
    }

    private static String stableNotificationIdentity(StatusBarNotification sbn) {
        String key = sbn.getKey();
        if (!TextUtils.isEmpty(key)) {
            return key;
        }
        return sbn.getId() + "|" + String.valueOf(sbn.getTag());
    }

    private static boolean extrasContain(StatusBarNotification sbn, String needle) {
        try {
            Notification notification = sbn != null ? sbn.getNotification() : null;
            Bundle extras = notification != null ? notification.extras : null;
            if (extras == null || TextUtils.isEmpty(needle)) {
                return false;
            }
            String normalizedNeedle = needle.toLowerCase(Locale.US);
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                String normalizedKey = key != null ? key.toLowerCase(Locale.US) : "";
                String normalizedValue = value != null ? String.valueOf(value).toLowerCase(Locale.US) : "";
                if (normalizedKey.contains(normalizedNeedle)
                        || normalizedValue.contains(normalizedNeedle)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static String normalizeOosExtraKey(String key) {
        if (key == null) {
            return "";
        }
        return key.toLowerCase(Locale.US)
                .replace("_", "")
                .replace("-", "")
                .replace(".", "");
    }

    static boolean isSystemUiUsbNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || !"com.android.systemui".equals(sbn.getPackageName())) {
            return false;
        }
        int id = sbn.getId();
        if (id == 10004 || id == 10005) {
            return true;
        }
        Notification notification = sbn.getNotification();
        String group = notification.getGroup();
        if (group != null && group.toLowerCase(Locale.US).contains("usb")) {
            return true;
        }
        try {
            Bundle extras = notification.extras;
            CharSequence title = extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
            CharSequence text = extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
            String joined = ((title != null ? title : "") + " " + (text != null ? text : ""))
                    .toLowerCase(Locale.US);
            return joined.contains("usb")
                    || joined.contains("adb")
                    || joined.contains("debugging enabled")
                    || joined.contains("charging this device");
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isSystemUiUsbDebugNotification(StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            Bundle extras = notification != null ? notification.extras : null;
            if (extras == null) {
                return false;
            }
            String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, ""));
            String text = String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT, ""));
            String combined = (title + " " + text).toLowerCase(Locale.US);
            return combined.contains("debug") || combined.contains("adb");
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean isSystemNotificationCandidate(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return false;
        }
        String packageName = sbn.getPackageName();
        if (!"android".equals(packageName) && !"com.android.systemui".equals(packageName)) {
            return false;
        }
        if (isSystemUiUsbNotification(sbn)) {
            return true;
        }
        if (isSystemUiDndNotification(sbn)) {
            return true;
        }
        if (isSystemNetworkStatusNotification(sbn)) {
            return true;
        }
        String joined = systemNotificationText(sbn);
        return joined.contains("module update")
                || joined.contains("network status")
                || joined.contains("hotspot")
                || joined.contains("tether")
                || joined.contains("wi-fi sharing")
                || joined.contains("wifi sharing")
                || joined.contains("device is connected")
                || joined.contains("usb")
                || joined.contains("debugging enabled")
                || joined.contains("charging this device");
    }

    /**
     * OOS hotspot / tethering status (channel NETWORK_STATUS, group Tethering). Title is often
     * "1 device is connected via Wi-Fi sharing" without the words hotspot/tether.
     */
    static boolean isSystemNetworkStatusNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || !"android".equals(sbn.getPackageName())) {
            return false;
        }
        Notification notification = sbn.getNotification();
        String channelId = notification.getChannelId();
        if ("NETWORK_STATUS".equals(channelId)) {
            return true;
        }
        String group = notification.getGroup();
        if (group != null) {
            String normalized = group.toLowerCase(Locale.US);
            if (normalized.contains("tether") || normalized.contains("hotspot")) {
                return true;
            }
        }
        String joined = systemNotificationText(sbn);
        return joined.contains("wi-fi sharing")
                || joined.contains("wifi sharing")
                || joined.contains("hotspot")
                || joined.contains("tether");
    }

    private static boolean isSystemUiDndNotification(StatusBarNotification sbn) {
        if (sbn == null || !"com.android.systemui".equals(sbn.getPackageName())) {
            return false;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return false;
        }
        String channelId = notification.getChannelId();
        if ("channel_dnd_notice".equals(channelId)) {
            return true;
        }
        String joined = systemNotificationText(sbn);
        return sbn.getId() == 10001
                && (joined.contains("do not disturb") || joined.contains("勿扰"));
    }

    static boolean isMediaIconCandidate(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        return Notification.CATEGORY_TRANSPORT.equals(notification.category)
                || hasMediaSessionExtra(notification);
    }

    static boolean hasMediaSessionExtra(Notification notification) {
        try {
            Bundle extras = notification != null ? notification.extras : null;
            return extras != null && extras.containsKey(Notification.EXTRA_MEDIA_SESSION);
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean matchesMediaSession(Notification notification, android.media.session.MediaController controller) {
        try {
            Bundle extras = notification.extras;
            if (extras == null || !extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
                return false;
            }
            Object token = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            return token != null && token.equals(controller.getSessionToken());
        } catch (Throwable ignored) {
            return false;
        }
    }

    static boolean lockscreenNotificationsEnabled(Context context) {
        if (context != null) {
            try {
                return Settings.Secure.getInt(context.getContentResolver(),
                        LOCK_SCREEN_SHOW_NOTIFICATIONS, 1) != 0;
            } catch (Throwable ignored) {
                return true;
            }
        }
        return true;
    }

    static String systemNotificationText(StatusBarNotification sbn) {
        try {
            Notification notification = sbn != null ? sbn.getNotification() : null;
            Bundle extras = notification != null ? notification.extras : null;
            CharSequence title = extras != null ? extras.getCharSequence(Notification.EXTRA_TITLE) : null;
            CharSequence text = extras != null ? extras.getCharSequence(Notification.EXTRA_TEXT) : null;
            CharSequence subText = extras != null ? extras.getCharSequence(Notification.EXTRA_SUB_TEXT) : null;
            return ((title != null ? title : "") + " "
                    + (text != null ? text : "") + " "
                    + (subText != null ? subText : "") + " "
                    + (notification != null && notification.tickerText != null
                    ? notification.tickerText : "")).toLowerCase(Locale.US);
        } catch (Throwable ignored) {
            return "";
        }
    }

    static String describePackages(StatusBarNotification[] notifications) {
        if (notifications == null || notifications.length == 0) {
            return "";
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>(notifications.length);
        Collections.addAll(list, notifications);
        return describePackages(list);
    }

    static String describePackages(List<StatusBarNotification> notifications) {
        StringBuilder builder = new StringBuilder();
        HashSet<String> seen = new HashSet<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null || !seen.add(sbn.getPackageName())) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(sbn.getPackageName());
            if (seen.size() >= 8) {
                if (notifications.size() > seen.size()) {
                    builder.append(",...");
                }
                break;
            }
        }
        return builder.toString();
    }

    static String notificationSignature(StatusBarNotification[] notifications) {
        if (notifications == null || notifications.length == 0) {
            return "";
        }
        // The icon row is rendered in snapshot order. Keeping this signature ordered makes
        // an order-only change refresh both the AOD layer and the lockscreen handoff together.
        StringBuilder builder = new StringBuilder();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null) {
                continue;
            }
            Notification notification = sbn.getNotification();
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(notificationPresentationSignature(
                    sbn.getKey(),
                    sbn.getPostTime(),
                    notification != null ? notification.visibility : 0,
                    notification != null ? notification.flags : 0,
                    notification != null ? notification.category : "",
                    smallIconIdentity(notification)));
        }
        return builder.toString();
    }

    /**
     * Returns only fields that can change the notification presentation. A listener callback
     * often supplies a new postTime for an otherwise identical notification; postTime by itself
     * must not rebuild the AOD or lockscreen icon rows.
     */
    static String notificationPresentationSignature(String key, long ignoredPostTime,
            int visibility, int flags, String category, String smallIconIdentity) {
        return String.valueOf(key)
                + ':' + visibility
                + ':' + flags
                + ':' + String.valueOf(category)
                + ':' + String.valueOf(smallIconIdentity);
    }

    static String mediaCandidatesSignature(Iterable<StatusBarNotification> notifications) {
        if (notifications == null) {
            return "";
        }
        ArrayList<String> entries = new ArrayList<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null) {
                continue;
            }
            entries.add(sbn.getKey() + '#' + mediaCandidateContentHash(sbn));
        }
        Collections.sort(entries);
        return TextUtils.join("|", entries);
    }

    private static String smallIconIdentity(Notification notification) {
        try {
            Icon icon = notification != null ? notification.getSmallIcon() : null;
            return icon != null ? icon.toString() : "";
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static int mediaCandidateContentHash(StatusBarNotification sbn) {
        try {
            Notification notification = sbn != null ? sbn.getNotification() : null;
            Bundle extras = notification != null ? notification.extras : null;
            if (extras == null) {
                return 0;
            }
            String title = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE, ""));
            String titleBig = String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE_BIG, ""));
            String text = String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT, ""));
            String subText = String.valueOf(extras.getCharSequence(Notification.EXTRA_SUB_TEXT, ""));
            String summary = String.valueOf(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT, ""));
            return (title + '\u0001' + titleBig + '\u0001' + text + '\u0001'
                    + subText + '\u0001' + summary).hashCode();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static String rankingSignature(Map<String, RankingSnapshot> snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            return "";
        }
        ArrayList<String> keys = new ArrayList<>(snapshot.keySet());
        Collections.sort(keys);
        StringBuilder builder = new StringBuilder();
        for (String key : keys) {
            RankingSnapshot ranking = snapshot.get(key);
            if (ranking == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(key).append('=').append(ranking.toString());
        }
        return builder.toString();
    }

    private static void logFilteredNotification(StatusBarNotification sbn, String reason, String trace) {
        PixelAodLog.log("filtered AOD notification", () ->
                "filtered AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
                + " reason=" + reason
                + " trace=" + trace);
    }

    private static void logKeptNotification(StatusBarNotification sbn, RankingSnapshot ranking,
            String trace, String reason) {
        PixelAodLog.log("kept AOD notification", () ->
                "kept AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
                + " reason=" + reason
                + " ranking=" + ranking
                + " trace=" + trace);
    }

    static final class RankingSnapshot {
        final int overrideVisibility;
        final int channelVisibility;
        final int importance;
        final int suppressedVisualEffects;

        RankingSnapshot(int overrideVisibility, int channelVisibility, int importance,
                int suppressedVisualEffects) {
            this.overrideVisibility = overrideVisibility;
            this.channelVisibility = channelVisibility;
            this.importance = importance;
            this.suppressedVisualEffects = suppressedVisualEffects;
        }

        static RankingSnapshot from(NotificationListenerService.Ranking ranking) {
            int channelVisibility = NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE;
            try {
                NotificationChannel channel = ranking.getChannel();
                if (channel != null) {
                    channelVisibility = channel.getLockscreenVisibility();
                }
            } catch (Throwable ignored) {
            }
            int importance = NotificationManagerImportance.UNKNOWN;
            try {
                importance = ranking.getImportance();
            } catch (Throwable ignored) {
            }
            int suppressedEffects = 0;
            try {
                suppressedEffects = ranking.getSuppressedVisualEffects();
            } catch (Throwable ignored) {
            }
            return new RankingSnapshot(
                    ranking.getLockscreenVisibilityOverride(),
                    channelVisibility,
                    importance,
                    suppressedEffects);
        }

        String hiddenReason() {
            if (importance == NotificationManagerImportance.NONE) {
                return "ranking-importance-none";
            }
            if (overrideVisibility == Notification.VISIBILITY_SECRET) {
                return "ranking-override-secret";
            }
            if (channelVisibility == Notification.VISIBILITY_SECRET) {
                return "ranking-channel-secret";
            }
            return null;
        }

        @Override
        public String toString() {
            return "override=" + overrideVisibility
                    + ",channel=" + channelVisibility
                    + ",importance=" + importance
                    + ",suppressed=" + suppressedVisualEffects;
        }
    }

    static final class LockscreenVisibilityDecision {
        private Boolean providerHidden;
        private String providerSource = "";
        private Boolean filterHidden;
        private String filterSource = "";

        LockscreenVisibilityDecision() {
        }

        LockscreenVisibilityDecision(LockscreenVisibilityDecision other) {
            if (other == null) {
                return;
            }
            providerHidden = other.providerHidden;
            providerSource = other.providerSource;
            filterHidden = other.filterHidden;
            filterSource = other.filterSource;
        }

        boolean setProviderHidden(boolean hidden, String source) {
            if (providerHidden != null
                    && providerHidden == hidden
                    && TextUtils.equals(providerSource, source)) {
                return false;
            }
            providerHidden = hidden;
            providerSource = source != null ? source : "";
            return true;
        }

        boolean setFilterHidden(boolean hidden, String source) {
            if (filterHidden != null
                    && filterHidden == hidden
                    && TextUtils.equals(filterSource, source)) {
                return false;
            }
            filterHidden = hidden;
            filterSource = source != null ? source : "";
            return true;
        }

        String hiddenReason() {
            if (Boolean.TRUE.equals(providerHidden)) {
                return "lockscreen-provider-hidden source=" + providerSource;
            }
            if (Boolean.TRUE.equals(filterHidden)) {
                return "lockscreen-filter-hidden source=" + filterSource;
            }
            return null;
        }

        @Override
        public String toString() {
            return "providerHidden=" + providerHidden
                    + "@"
                    + providerSource
                    + ",filterHidden="
                    + filterHidden
                    + "@"
                    + filterSource;
        }
    }

    static final class NotificationManagerImportance {
        static final int UNKNOWN = Integer.MIN_VALUE;
        static final int NONE = 0;
        static final int LOW = 2;
        static final int DEFAULT = 3;
    }
}
