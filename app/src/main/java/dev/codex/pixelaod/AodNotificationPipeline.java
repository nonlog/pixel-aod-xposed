package dev.codex.pixelaod;

import android.app.Notification;
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
        if ("com.android.systemui".equals(sbn.getPackageName()) && !systemNotification) {
            return false;
        }
        RankingSnapshot ranking = rankings != null ? rankings.get(sbn.getKey()) : null;
        LockscreenVisibilityDecision lockscreenDecision =
                lockscreenDecisions != null ? lockscreenDecisions.get(sbn.getKey()) : null;
        if (!systemNotification) {
            String silentHiddenReason = lockscreenPolicySilentHiddenReason(
                    lockscreenPolicyEnabled,
                    sbn,
                    ranking != null ? ranking.importance : NotificationManagerImportance.UNKNOWN);
            if (silentHiddenReason != null) {
                logFilteredNotification(sbn, silentHiddenReason + " ranking=" + ranking, trace);
                return false;
            }
        }
        String rankingHiddenReason = ranking != null ? ranking.hiddenReason() : null;
        if (!systemNotification && rankingHiddenReason != null) {
            logFilteredNotification(sbn, rankingHiddenReason + " ranking=" + ranking, trace);
            return false;
        }
        String lockscreenHiddenReason = lockscreenDecision != null
                ? lockscreenDecision.hiddenReason()
                : null;
        if (!systemNotification && !testNotification && lockscreenHiddenReason != null) {
            logFilteredNotification(sbn, lockscreenHiddenReason
                    + " decision=" + lockscreenDecision
                    + " ranking=" + ranking, trace);
            return false;
        }
        logKeptNotification(sbn, ranking, trace);
        return true;
    }

    static boolean isTestNotification(StatusBarNotification sbn) {
        return sbn != null
                && MODULE_PACKAGE.equals(sbn.getPackageName())
                && TestNotificationReceiver.TEST_TAG.equals(sbn.getTag());
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
        if ((notification.flags & NOTIFICATION_FLAG_SILENT) != 0) {
            return "lockscreen-policy-notification-flag-silent";
        }
        if (importance != NotificationManagerImportance.UNKNOWN
                && importance <= NotificationManagerImportance.LOW) {
            return "lockscreen-policy-ranking-importance-low-or-less importance=" + importance;
        }
        return null;
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
        String joined = systemNotificationText(sbn);
        return joined.contains("module update")
                || joined.contains("network status")
                || joined.contains("hotspot")
                || joined.contains("tether")
                || joined.contains("usb")
                || joined.contains("debugging enabled")
                || joined.contains("charging this device");
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
        ArrayList<String> entries = new ArrayList<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null) {
                continue;
            }
            Notification notification = sbn.getNotification();
            entries.add(sbn.getKey()
                    + '@' + sbn.getPostTime()
                    + ':' + (notification != null ? notification.visibility : 0)
                    + ':' + (notification != null ? notification.flags : 0));
        }
        Collections.sort(entries);
        return TextUtils.join("|", entries);
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
            entries.add(sbn.getKey() + '@' + sbn.getPostTime()
                    + '#' + mediaCandidateContentHash(sbn));
        }
        Collections.sort(entries);
        return TextUtils.join("|", entries);
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
        PixelAodLog.log("filtered AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
                + " reason=" + reason
                + " trace=" + trace);
    }

    private static void logKeptNotification(StatusBarNotification sbn, RankingSnapshot ranking, String trace) {
        PixelAodLog.log("kept AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
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
