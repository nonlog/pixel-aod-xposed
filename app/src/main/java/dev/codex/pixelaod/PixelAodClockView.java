package dev.codex.pixelaod;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONArray;
import org.json.JSONObject;

public final class PixelAodClockView extends FrameLayout {
    private static final Set<PixelAodClockView> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private static final int CLOCK_COLOR = Color.rgb(232, 234, 237);
    private static final int INFO_COLOR = Color.rgb(218, 220, 224);
    private static final int LARGE_CLOCK_TEXT_DP = 150;
    private static final int LARGE_CLOCK_TOP_DP = 118;
    private static final int SMALL_CLOCK_TEXT_DP = 56;
    private static final int SMALL_CLOCK_TOP_DP = 28;
    private static final int INFO_EDGE_DP = 34;
    private static final int LARGE_INFO_TOP_DP = 36;
    private static final int LARGE_NOTIFICATION_LINE_TOP_DP = 76;
    private static final int LARGE_MEDIA_TOP_DP = 104;
    private static final int LARGE_MEDIA_NO_NOTIFICATIONS_TOP_DP = 72;
    private static final int SMALL_INFO_TOP_DP = 98;
    private static final int SMALL_NOTIFICATION_LINE_TOP_DP = 150;
    private static final int SMALL_MEDIA_TOP_DP = 186;
    private static final int NOTIFICATION_ICON_SIZE_DP = 14;
    private static final int NOTIFICATION_ICON_SPACING_DP = 8;
    private static final int MEDIA_ICON_SIZE_DP = 13;
    private static final int MEDIA_ICON_SPACING_DP = 8;
    private static final int MAX_NOTIFICATION_ICONS = 5;
    private static final int ICON_MASK_SAMPLE_SIZE = 48;
    private static final int BATTERY_TOP_DP = 720;
    private static final float CLOCK_LINE_SPACING = 0.70f;
    private static final float LARGE_CLOCK_LETTER_SPACING = -0.02f;
    private static final float COMPACT_CLOCK_LETTER_SPACING = -0.025f;
    private static final int CLOCK_AOD_WEIGHT = 280;
    private static final int INFO_AOD_WEIGHT = 500;
    private static final int WEATHER_ICON_SIZE_DP = 15;
    private static final int WEATHER_ICON_PADDING_DP = 6;
    private static final int BURN_IN_OFFSET_X_DP = 16;
    private static final int BURN_IN_OFFSET_Y_DP = 24;
    private static final float BURN_IN_PERIOD_X_MINUTES = 43f;
    private static final float BURN_IN_PERIOD_Y_MINUTES = 271f;
    private static final int NOTIFICATION_FLAG_SILENT = 0x00020000;
    private static final String GOOGLE_SANS_FLEX_CLOCK_ASSET = "assets/fonts/GoogleSansFlex-200.ttf";
    private static final String GOOGLE_SANS_FLEX_INFO_ASSET = "assets/fonts/GoogleSansFlex-500.ttf";
    private static final String GOOGLE_SANS_FLEX_REGULAR_ASSET = "assets/fonts/GoogleSansFlex-Regular.ttf";
    private static final String GOOGLE_SANS_FLEX_CLOCK_CACHE = "pixelaod_google_sans_flex_200.ttf";
    private static final String GOOGLE_SANS_FLEX_INFO_CACHE = "pixelaod_google_sans_flex_500.ttf";
    private static final String GOOGLE_SANS_FLEX_REGULAR_CACHE = "pixelaod_google_sans_flex_regular.ttf";
    private static final String ANDROID_CLOCK_FONT = "/system/fonts/AndroidClock.ttf";
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final String BREEZY_PACKAGE = "org.breezyweather";
    private static final String ACTION_GADGETBRIDGE_WEATHER =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    private static final String ACTION_BREEZY_UPDATE_NOTIFIER =
            "org.breezyweather.ACTION_UPDATE_NOTIFIER";
    private static final String LOCK_SCREEN_SHOW_NOTIFICATIONS = "lock_screen_show_notifications";
    private static final boolean AT_A_GLANCE_EXTRA_ENABLED = false;
    private static final long WEATHER_STALE_MILLIS = 12L * 60L * 60L * 1000L;
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];
    private static final LinkedHashMap<String, StatusBarNotification> mediaNotificationCache =
            new LinkedHashMap<>();
    private static StatusBarNotification[] rawNotifications = EMPTY_NOTIFICATIONS;
    private static StatusBarNotification[] activeNotifications = EMPTY_NOTIFICATIONS;
    private static Typeface cachedClockTypeface;
    private static Typeface cachedInfoTypeface;
    private static String modulePath;
    private static Context appContext;
    private static boolean aodActive;
    private static int notificationUpdateLogCount;
    private static int notificationRebuildLogCount;
    private static int mediaIconLogCount;
    private static int mediaIconRejectLogCount;
    private static int mediaNotificationLogCount;
    private static int notificationIconLogCount;
    private static int notificationFilterLogCount;
    private static int notificationKeepLogCount;
    private static int atAGlanceLogCount;
    private static int breezyWeatherLogCount;
    private static int activeSnapshotDirectLogCount;
    private static int instanceRefreshLogCount;
    private static int shadeSuppressionLogCount;
    private static int burnInLogCount;
    private static float lastBurnInTranslationX;
    private static float lastBurnInTranslationY;
    private static long lastCachedWeatherRequestAt;
    private static String atAGlanceExtra = "";
    private static WeatherSnapshot breezyWeather = WeatherSnapshot.empty();
    private static boolean breezyWeatherReceiverRegistered;
    private static final Map<String, RankingSnapshot> notificationRankings = new HashMap<>();
    private static final BroadcastReceiver BREEZY_WEATHER_RECEIVER = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBreezyWeatherIntent(intent, "broadcast");
        }
    };
    private final TextView clockView;
    private final TextView dateView;
    private final TextView mediaView;
    private final LinearLayout batteryRow;
    private final TextView batteryView;
    private final ChargeBoltView chargeBoltView;
    private final LinearLayout notificationIconRow;
    private final LinearLayout mediaRow;
    private final ImageView mediaIconView;
    private final List<MediaController> mediaControllers = new ArrayList<>();
    private final Map<MediaController, MediaController.Callback> mediaCallbacks = new HashMap<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener activeSessionsChangedListener =
            this::updateMediaControllers;
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateTime();
            mainHandler().postDelayed(this, millisUntilNextMinute());
        }
    };
    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (getVisibility() == View.VISIBLE || shouldCustomizeAodNow(context)) {
                    PixelLockscreenClockView.prepareAodToLockscreenTransition("screen-on");
                }
                hideAllAodOverlays("screen-on");
                PixelAodHook.restoreSystemViewsForLockscreen("screen-on");
            }
            updateAodVisibility("screen-state");
        }
    };
    private final ContentObserver notificationSettingsObserver = new ContentObserver(mainHandler()) {
        @Override
        public void onChange(boolean selfChange) {
            refreshNotificationFiltering("lockscreen-notification-setting");
        }
    };
    private MediaSessionManager mediaSessionManager;
    private boolean running;
    private boolean mediaListening;
    private boolean compactClock;
    private boolean screenStateReceiverRegistered;
    private boolean notificationSettingsObserverRegistered;

    public PixelAodClockView(Context context) {
        super(context);
        Context applicationContext = context.getApplicationContext();
        appContext = applicationContext != null ? applicationContext : context;
        ensureBreezyWeatherReceiver(appContext);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setVisibleToUser(false);
            }
        });

        Typeface clockTypeface = resolveClockTypeface(context);
        Typeface infoTypeface = weighted(resolveInfoTypeface(context), INFO_AOD_WEIGHT);

        dateView = makeInfoLine(context, infoTypeface, 16, Gravity.START);
        FrameLayout.LayoutParams dateParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        dateParams.leftMargin = dp(INFO_EDGE_DP);
        dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
        addView(dateView, dateParams);

        notificationIconRow = new LinearLayout(context);
        notificationIconRow.setOrientation(LinearLayout.HORIZONTAL);
        notificationIconRow.setGravity(Gravity.CENTER_VERTICAL);
        notificationIconRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        notificationIconRow.setAlpha(0.94f);
        FrameLayout.LayoutParams notificationParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(NOTIFICATION_ICON_SIZE_DP),
                Gravity.TOP | Gravity.START);
        notificationParams.leftMargin = dp(INFO_EDGE_DP);
        notificationParams.topMargin = dp(LARGE_NOTIFICATION_LINE_TOP_DP);
        addView(notificationIconRow, notificationParams);

        mediaRow = new LinearLayout(context);
        mediaRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaRow.setGravity(Gravity.CENTER_VERTICAL);
        mediaRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mediaRow.setVisibility(View.GONE);
        mediaRow.setAlpha(0.82f);

        mediaIconView = new ImageView(context);
        mediaIconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mediaIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams mediaGlyphParams = new LinearLayout.LayoutParams(
                dp(MEDIA_ICON_SIZE_DP), dp(MEDIA_ICON_SIZE_DP));
        mediaRow.addView(mediaIconView, mediaGlyphParams);

        mediaView = makeInfoLine(context, infoTypeface, 14, Gravity.START);
        mediaView.setAlpha(0.82f);
        mediaView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams mediaTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        mediaTextParams.leftMargin = dp(MEDIA_ICON_SPACING_DP);
        mediaRow.addView(mediaView, mediaTextParams);

        FrameLayout.LayoutParams mediaParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        mediaParams.leftMargin = dp(INFO_EDGE_DP);
        mediaParams.rightMargin = dp(INFO_EDGE_DP);
        mediaParams.topMargin = dp(LARGE_MEDIA_TOP_DP);
        addView(mediaRow, mediaParams);

        clockView = makeClock(context, clockTypeface);
        FrameLayout.LayoutParams clockParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
        addView(clockView, clockParams);

        batteryRow = new LinearLayout(context);
        batteryRow.setOrientation(LinearLayout.HORIZONTAL);
        batteryRow.setGravity(Gravity.CENTER);
        batteryRow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        batteryRow.setAlpha(0.94f);

        batteryView = makeInfoLine(context, infoTypeface, 13, Gravity.CENTER);
        batteryRow.addView(batteryView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        chargeBoltView = new ChargeBoltView(context);
        LinearLayout.LayoutParams chargeParams = new LinearLayout.LayoutParams(dp(9), dp(13));
        chargeParams.leftMargin = dp(7);
        batteryRow.addView(chargeBoltView, chargeParams);

        FrameLayout.LayoutParams batteryParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        batteryParams.topMargin = dp(BATTERY_TOP_DP);
        addView(batteryRow, batteryParams);

        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons();
    }

    static void setModulePath(String path) {
        synchronized (PixelAodClockView.class) {
            modulePath = path;
            cachedClockTypeface = null;
            cachedInfoTypeface = null;
        }
    }

    static void ensureBreezyWeatherReceiver(Context context) {
        if (context == null) {
            return;
        }
        Context receiverContext = context.getApplicationContext();
        if (receiverContext == null) {
            receiverContext = context;
        }
        synchronized (PixelAodClockView.class) {
            if (breezyWeatherReceiverRegistered) {
                return;
            }
            appContext = receiverContext;
        }
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BreezyWeatherRelayReceiver.ACTION_RELAY);
            filter.addAction(ACTION_GADGETBRIDGE_WEATHER);
            filter.addAction(ACTION_BREEZY_UPDATE_NOTIFIER);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                receiverContext.registerReceiver(
                        BREEZY_WEATHER_RECEIVER, filter, Context.RECEIVER_EXPORTED);
            } else {
                receiverContext.registerReceiver(BREEZY_WEATHER_RECEIVER, filter);
            }
            synchronized (PixelAodClockView.class) {
                breezyWeatherReceiverRegistered = true;
            }
            PixelAodLog.log("registered Pixel AOD Breezy weather receiver");
            requestCachedBreezyWeather(receiverContext);
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD Breezy weather receiver", t);
        }
    }

    private static void requestCachedBreezyWeather(Context context) {
        try {
            Intent request = new Intent(BreezyWeatherRelayReceiver.ACTION_REQUEST_RELAY)
                    .setPackage(MODULE_PACKAGE)
                    .setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(request);
            PixelAodLog.log("requested cached Breezy weather replay");
        } catch (Throwable t) {
            PixelAodLog.log("failed to request cached Breezy weather replay", t);
        }
    }

    static int notificationLineTopDp() {
        return LARGE_NOTIFICATION_LINE_TOP_DP;
    }

    static void setActiveNotifications(StatusBarNotification[] notifications) {
        int rawCount = notifications == null ? 0 : notifications.length;
        int usableCount;
        String packageSummary;
        synchronized (PixelAodClockView.class) {
            rawNotifications = notifications != null ? notifications.clone() : EMPTY_NOTIFICATIONS;
            activeNotifications = sanitizeNotifications(notifications);
            usableCount = activeNotifications.length;
            packageSummary = describeNotificationPackages(activeNotifications);
            if (notificationUpdateLogCount < 10) {
                notificationUpdateLogCount++;
                PixelAodLog.log("updated native AOD notification snapshot raw="
                        + rawCount
                        + " usable=" + usableCount
                        + " packages=" + packageSummary);
            }
            if (activeSnapshotDirectLogCount < 16) {
                activeSnapshotDirectLogCount++;
                PixelAodLog.log("AOD notification snapshot direct raw="
                        + rawCount
                        + " usable=" + usableCount);
            }
        }
        refreshInstancesFromNotificationSnapshot("setActiveNotifications");
        PixelLockscreenClockView.setActiveNotifications(activeNotifications);
    }

    static void refreshNotificationFiltering(String source) {
        StatusBarNotification[] rawSnapshot;
        synchronized (PixelAodClockView.class) {
            rawSnapshot = rawNotifications;
        }
        PixelAodLog.log("refreshing AOD notification filtering from " + source);
        setActiveNotifications(rawSnapshot);
    }

    static void setAodActive(boolean active, String source) {
        boolean changed;
        synchronized (PixelAodClockView.class) {
            changed = aodActive != active;
            aodActive = active;
        }
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateAodVisibility(source);
                    if (active) {
                        view.refreshActiveMediaControllers();
                    }
                }
            }
        });
        if (changed) {
            PixelAodLog.log("Pixel AOD active=" + active + " source=" + source);
        }
    }

    static void hideAllAodOverlays(String source) {
        synchronized (PixelAodClockView.class) {
            aodActive = false;
        }
        mainHandler().post(() -> {
            int hidden = 0;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    if (view.getVisibility() != View.GONE) {
                        view.setVisibility(View.GONE);
                        view.resetBurnInTranslation();
                        hidden++;
                    }
                    view.stop();
                }
            }
            PixelAodLog.log("hid Pixel AOD overlays from " + source + " count=" + hidden);
        });
    }

    static BurnInOffset currentBurnInOffset() {
        synchronized (PixelAodClockView.class) {
            return new BurnInOffset(lastBurnInTranslationX, lastBurnInTranslationY);
        }
    }

    static boolean shouldCustomizeAodNow(Context context) {
        if (context == null) {
            return false;
        }
        synchronized (PixelAodClockView.class) {
            if (!aodActive) {
                return false;
            }
        }
        return !isDeviceInteractive(context);
    }

    static void updateRankingMap(NotificationListenerService.RankingMap rankingMap) {
        if (rankingMap == null) {
            return;
        }
        try {
            String[] keys = rankingMap.getOrderedKeys();
            if (keys == null) {
                return;
            }
            HashMap<String, RankingSnapshot> snapshot = new HashMap<>();
            NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
            for (String key : keys) {
                if (key != null && rankingMap.getRanking(key, ranking)) {
                    snapshot.put(key, RankingSnapshot.from(ranking));
                }
            }
            synchronized (PixelAodClockView.class) {
                notificationRankings.clear();
                notificationRankings.putAll(snapshot);
            }
            PixelAodLog.log("updated AOD notification ranking lockscreen overrides count="
                    + snapshot.size());
            StatusBarNotification[] rawSnapshot;
            synchronized (PixelAodClockView.class) {
                rawSnapshot = rawNotifications;
            }
            setActiveNotifications(rawSnapshot);
        } catch (Throwable t) {
            PixelAodLog.log("failed to update AOD notification ranking map", t);
        }
    }

    static void clearActiveNotifications() {
        setActiveNotifications(null);
    }

    static void setMediaNotificationCandidates(StatusBarNotification[] notifications, String source) {
        int count = 0;
        synchronized (PixelAodClockView.class) {
            mediaNotificationCache.clear();
            if (notifications != null) {
                for (StatusBarNotification sbn : notifications) {
                    if (isMediaIconCandidate(sbn)) {
                        mediaNotificationCache.put(sbn.getKey(), sbn);
                        count++;
                    }
                }
            }
        }
        logMediaNotificationCache("replaced", source, count);
        refreshMediaLines();
    }

    static void cacheMediaNotificationCandidate(StatusBarNotification sbn, String source) {
        if (!isMediaIconCandidate(sbn)) {
            return;
        }
        int count;
        synchronized (PixelAodClockView.class) {
            mediaNotificationCache.put(sbn.getKey(), sbn);
            count = mediaNotificationCache.size();
        }
        logMediaNotificationCache("cached", source, count);
        refreshMediaLines();
    }

    static void removeMediaNotificationCandidate(StatusBarNotification sbn, String source) {
        if (sbn == null) {
            return;
        }
        boolean removed;
        int count;
        synchronized (PixelAodClockView.class) {
            removed = mediaNotificationCache.remove(sbn.getKey()) != null;
            count = mediaNotificationCache.size();
        }
        if (removed) {
            logMediaNotificationCache("removed", source, count);
            refreshMediaLines();
        }
    }

    private static void handleBreezyWeatherIntent(Intent intent, String source) {
        if (intent == null) {
            return;
        }
        try {
            WeatherSnapshot snapshot = parseBreezyWeatherIntent(intent);
            if (snapshot == null || !snapshot.hasDisplayableWeather()) {
                if (ACTION_BREEZY_UPDATE_NOTIFIER.equals(intent.getAction())) {
                    logBreezyWeather("received Breezy update notifier without weather payload");
                }
                return;
            }
            boolean changed;
            synchronized (PixelAodClockView.class) {
                changed = !breezyWeather.sameDisplay(snapshot);
                breezyWeather = snapshot;
            }
            if (changed) {
                logBreezyWeather("updated Breezy weather from " + source
                        + " text=" + snapshot.logText());
            }
            mainHandler().post(() -> {
                for (PixelAodClockView view : INSTANCES) {
                    if (view != null) {
                        view.updateTime();
                    }
                }
                PixelLockscreenClockView.refreshAll("weather");
            });
        } catch (Throwable t) {
            PixelAodLog.log("failed to handle Breezy weather payload", t);
        }
    }

    private static WeatherSnapshot parseBreezyWeatherIntent(Intent intent) {
        String json = intent.getStringExtra("WeatherJson");
        if (TextUtils.isEmpty(json)) {
            json = firstWeatherJsonFromGzip(intent.getByteArrayExtra("WeatherGz"));
        }
        if (!TextUtils.isEmpty(json)) {
            return parseBreezyWeatherJson(json,
                    intent.getLongExtra(BreezyWeatherRelayReceiver.EXTRA_RECEIVED_AT,
                            System.currentTimeMillis()));
        }

        String condition = firstNonEmptyString(
                intent.getStringExtra("currentCondition"),
                intent.getStringExtra("condition"),
                intent.getStringExtra("weatherText"));
        double temperature = firstNonNaN(
                numericExtra(intent, "currentTemp"),
                numericExtra(intent, "temperature"),
                numericExtra(intent, "temp"));
        int code = firstWeatherCode(
                intent.getIntExtra("currentConditionCode", Integer.MIN_VALUE),
                intent.getIntExtra("weatherCode", Integer.MIN_VALUE),
                intent.getIntExtra("conditionCode", Integer.MIN_VALUE));
        WeatherSnapshot direct = WeatherSnapshot.from(
                formatTemperature(temperature),
                code,
                condition,
                System.currentTimeMillis());
        if (!direct.hasDisplayableWeather()) {
            return null;
        }
        return direct;
    }

    private static String firstWeatherJsonFromGzip(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return null;
        }
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
             InputStreamReader reader = new InputStreamReader(gzip, StandardCharsets.UTF_8)) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, read);
            }
            JSONArray array = new JSONArray(builder.toString());
            if (array.length() == 0) {
                return null;
            }
            return array.optJSONObject(0).toString();
        } catch (Throwable t) {
            PixelAodLog.log("failed to parse Breezy WeatherGz payload", t);
            return null;
        }
    }

    private static WeatherSnapshot parseBreezyWeatherJson(String json, long receivedAtMillis) {
        try {
            JSONObject object = new JSONObject(json);
            String condition = firstNonEmptyString(
                    object.optString("currentCondition", null),
                    object.optString("weatherText", null),
                    object.optString("condition", null));
            double temperature = Double.NaN;
            if (object.has("currentTemp")) {
                temperature = object.optDouble("currentTemp", Double.NaN);
            } else if (object.has("temperature")) {
                temperature = object.optDouble("temperature", Double.NaN);
            }
            int code = firstWeatherCode(
                    object.has("currentConditionCode")
                            ? object.optInt("currentConditionCode", Integer.MIN_VALUE)
                            : Integer.MIN_VALUE,
                    object.has("weatherCode")
                            ? object.optInt("weatherCode", Integer.MIN_VALUE)
                            : Integer.MIN_VALUE,
                    object.has("conditionCode")
                            ? object.optInt("conditionCode", Integer.MIN_VALUE)
                            : Integer.MIN_VALUE);
            long timestampSeconds = object.optLong("timestamp", 0L);
            long timestampMillis = timestampSeconds > 0L
                    ? timestampSeconds * 1000L
                    : Math.max(receivedAtMillis, System.currentTimeMillis());
            WeatherSnapshot snapshot = WeatherSnapshot.from(
                    formatTemperature(temperature), code, condition, timestampMillis);
            return snapshot.hasDisplayableWeather() ? snapshot : null;
        } catch (Throwable t) {
            PixelAodLog.log("failed to parse Breezy WeatherJson", t);
            return null;
        }
    }

    private static String formatTemperature(double rawTemperature) {
        if (Double.isNaN(rawTemperature) || Double.isInfinite(rawTemperature)) {
            return "";
        }
        double celsius = rawTemperature > 170d ? rawTemperature - 273.15d : rawTemperature;
        if (celsius < -80d || celsius > 80d) {
            return "";
        }
        return Math.round(celsius) + "\u00b0";
    }

    private static String normalizeWeatherCondition(String condition) {
        if (condition == null) {
            return "";
        }
        String normalized = condition.replace('\n', ' ').replace('\r', ' ').trim();
        normalized = normalized.replaceAll("\\s+", " ");
        if (normalized.length() > 18) {
            normalized = normalized.substring(0, 18).trim();
        }
        return normalized;
    }

    private static int firstWeatherCode(int first, int second, int third) {
        if (first != Integer.MIN_VALUE) {
            return first;
        }
        return second != Integer.MIN_VALUE ? second : third;
    }

    private static String firstNonEmptyString(String first, String second, String third) {
        if (!TextUtils.isEmpty(first)) {
            return first;
        }
        if (!TextUtils.isEmpty(second)) {
            return second;
        }
        return TextUtils.isEmpty(third) ? "" : third;
    }

    private static double firstNonNaN(double first, double second, double third) {
        if (!Double.isNaN(first)) {
            return first;
        }
        return !Double.isNaN(second) ? second : third;
    }

    private static double numericExtra(Intent intent, String key) {
        try {
            Bundle extras = intent.getExtras();
            if (extras == null || !extras.containsKey(key)) {
                return Double.NaN;
            }
            Object value = extras.get(key);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            if (value instanceof CharSequence) {
                return Double.parseDouble(value.toString().trim());
            }
        } catch (Throwable ignored) {
            // Ignore malformed weather extras.
        }
        return Double.NaN;
    }

    private static void logBreezyWeather(String message) {
        if (breezyWeatherLogCount >= 16) {
            return;
        }
        breezyWeatherLogCount++;
        PixelAodLog.log(message);
    }

    static void setAtAGlanceExtra(String extra) {
        if (!AT_A_GLANCE_EXTRA_ENABLED) {
            synchronized (PixelAodClockView.class) {
                atAGlanceExtra = "";
            }
            return;
        }
        String normalized = normalizeAtAGlanceExtra(extra);
        boolean changed;
        synchronized (PixelAodClockView.class) {
            changed = !TextUtils.equals(atAGlanceExtra, normalized);
            atAGlanceExtra = normalized;
        }
        if (!changed) {
            return;
        }
        if (atAGlanceLogCount < 12) {
            atAGlanceLogCount++;
            PixelAodLog.log("updated Pixel AOD At a Glance extra=" + normalized);
        }
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateTime();
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        INSTANCES.add(this);
        registerScreenStateReceiver();
        registerNotificationSettingsObserver();
        updateAodVisibility("attach");
        start();
        startMediaListening();
        refreshPresentation();
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterNotificationSettingsObserver();
        unregisterScreenStateReceiver();
        stopMediaListening();
        stop();
        INSTANCES.remove(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!shouldDrawAodOverlay("dispatchDraw")) {
            return;
        }
        super.dispatchDraw(canvas);
    }

    public void start() {
        if (running) {
            updateAodVisibility("start-existing");
            refreshPresentation();
            return;
        }
        running = true;
        updateAodVisibility("start");
        refreshPresentation();
        mainHandler().removeCallbacks(ticker);
        mainHandler().postDelayed(ticker, millisUntilNextMinute());
    }

    public void stop() {
        running = false;
        mainHandler().removeCallbacks(ticker);
        resetBurnInTranslation();
    }

    static void tickAllInstances() {
        for (PixelAodClockView view : INSTANCES) {
            if (view != null) {
                view.start();
            }
        }
    }

    static void stopAllInstances() {
        for (PixelAodClockView view : INSTANCES) {
            if (view != null) {
                view.stop();
            }
        }
    }

    private void refreshPresentation() {
        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons();
        updateMediaLine();
    }

    private static void refreshInstancesFromNotificationSnapshot(String source) {
        Runnable task = () -> {
            int count = 0;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    count++;
                    view.rebuildNotificationIcons();
                    view.updateMediaLine();
                }
            }
            if (instanceRefreshLogCount < 16) {
                instanceRefreshLogCount++;
                PixelAodLog.log("refreshed Pixel AOD instances from "
                        + source + " count=" + count);
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().post(task);
        }
    }

    private TextView makeClock(Context context, Typeface typeface) {
        int weight = aodClockWeight(context);
        TextView textView = new TextView(context);
        textView.setTextColor(resolveMaterialClockColor(context));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                scaledClockTextDp(context, LARGE_CLOCK_TEXT_DP));
        textView.setTypeface(weighted(typeface, weight));
        applyFontVariation(textView, weight);
        textView.setIncludeFontPadding(false);
        textView.setElegantTextHeight(false);
        textView.setGravity(Gravity.CENTER_HORIZONTAL);
        textView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
        textView.setSingleLine(false);
        textView.setLines(2);
        textView.setLineSpacing(0f, CLOCK_LINE_SPACING);
        textView.setLetterSpacing(LARGE_CLOCK_LETTER_SPACING);
        textView.setFontFeatureSettings("tnum");
        textView.getPaint().setSubpixelText(true);
        textView.setAlpha(0.96f);
        return textView;
    }

    private void startMediaListening() {
        if (mediaListening) {
            refreshActiveMediaControllers();
            return;
        }
        try {
            mediaSessionManager = (MediaSessionManager) getContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) {
                return;
            }
            mediaListening = true;
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    activeSessionsChangedListener, null, mainHandler());
            refreshActiveMediaControllers();
            PixelAodLog.log("started Pixel-style AOD media listener");
        } catch (Throwable t) {
            mediaListening = false;
            PixelAodLog.log("failed to start Pixel-style AOD media listener", t);
            mediaRow.setVisibility(View.GONE);
        }
    }

    private void registerScreenStateReceiver() {
        if (screenStateReceiverRegistered) {
            return;
        }
        try {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            getContext().registerReceiver(screenStateReceiver, filter);
            screenStateReceiverRegistered = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD screen-state receiver", t);
        }
    }

    private void unregisterScreenStateReceiver() {
        if (!screenStateReceiverRegistered) {
            return;
        }
        screenStateReceiverRegistered = false;
        try {
            getContext().unregisterReceiver(screenStateReceiver);
        } catch (Throwable t) {
            PixelAodLog.log("failed to unregister Pixel AOD screen-state receiver", t);
        }
    }

    private void registerNotificationSettingsObserver() {
        if (notificationSettingsObserverRegistered) {
            return;
        }
        try {
            getContext().getContentResolver().registerContentObserver(
                    Settings.Secure.getUriFor(LOCK_SCREEN_SHOW_NOTIFICATIONS),
                    false,
                    notificationSettingsObserver);
            notificationSettingsObserverRegistered = true;
        } catch (Throwable t) {
            PixelAodLog.log("failed to watch lockscreen notification setting", t);
        }
    }

    private void unregisterNotificationSettingsObserver() {
        if (!notificationSettingsObserverRegistered) {
            return;
        }
        notificationSettingsObserverRegistered = false;
        try {
            getContext().getContentResolver().unregisterContentObserver(notificationSettingsObserver);
        } catch (Throwable t) {
            PixelAodLog.log("failed to unwatch lockscreen notification setting", t);
        }
    }

    private void updateAodVisibility(String source) {
        boolean visible = shouldDrawAodOverlay(source);
        int desiredVisibility = visible ? View.VISIBLE : View.GONE;
        if (getVisibility() != desiredVisibility) {
            setVisibility(desiredVisibility);
            PixelAodLog.log("Pixel AOD overlay visibility="
                    + (visible ? "visible" : "hidden")
                    + " source=" + source);
        }
        if (visible) {
            applyBurnInTranslation();
        } else {
            resetBurnInTranslation();
        }
    }

    private boolean shouldDrawAodOverlay(String source) {
        if (!shouldCustomizeAodNow(getContext())) {
            return false;
        }
        if (isInsideExpandedSystemShade()) {
            if (getVisibility() != View.GONE) {
                setVisibility(View.GONE);
            }
            if (shadeSuppressionLogCount < 12) {
                shadeSuppressionLogCount++;
                PixelAodLog.log("suppressed Pixel AOD overlay in expanded shade source="
                        + source);
            }
            return false;
        }
        return true;
    }

    private boolean isInsideExpandedSystemShade() {
        View root = getRootView();
        if (!(root instanceof ViewGroup)) {
            return false;
        }
        return PixelAodHook.hasExpandedSystemNotificationShadeContent((ViewGroup) root);
    }

    private boolean isDeviceInteractive() {
        return isDeviceInteractive(getContext());
    }

    static boolean isDeviceInteractive(Context context) {
        if (context == null) {
            return true;
        }
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isInteractive();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void stopMediaListening() {
        if (!mediaListening) {
            return;
        }
        mediaListening = false;
        try {
            if (mediaSessionManager != null) {
                mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener);
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to remove Pixel-style AOD media listener", t);
        }
        unregisterMediaCallbacks();
    }

    private void refreshActiveMediaControllers() {
        try {
            if (mediaSessionManager == null) {
                updateMediaLine();
                return;
            }
            updateMediaControllers(mediaSessionManager.getActiveSessions(null));
        } catch (Throwable t) {
            PixelAodLog.log("failed to query active media sessions for AOD", t);
            updateMediaControllers(Collections.emptyList());
        }
    }

    private void updateMediaControllers(List<MediaController> controllers) {
        unregisterMediaCallbacks();
        if (controllers != null) {
            mediaControllers.addAll(controllers);
            for (MediaController controller : mediaControllers) {
                if (controller == null) {
                    continue;
                }
                MediaController.Callback callback = new MediaController.Callback() {
                    @Override
                    public void onPlaybackStateChanged(PlaybackState state) {
                        updateMediaLine();
                    }

                    @Override
                    public void onMetadataChanged(MediaMetadata metadata) {
                        updateMediaLine();
                    }

                    @Override
                    public void onSessionDestroyed() {
                        refreshActiveMediaControllers();
                    }
                };
                try {
                    controller.registerCallback(callback, mainHandler());
                    mediaCallbacks.put(controller, callback);
                } catch (Throwable t) {
                    PixelAodLog.log("failed to watch active media controller for AOD", t);
                }
            }
        }
        updateMediaLine();
    }

    private void unregisterMediaCallbacks() {
        for (Map.Entry<MediaController, MediaController.Callback> entry : mediaCallbacks.entrySet()) {
            try {
                entry.getKey().unregisterCallback(entry.getValue());
            } catch (Throwable t) {
                PixelAodLog.log("failed to unregister AOD media callback", t);
            }
        }
        mediaCallbacks.clear();
        mediaControllers.clear();
    }

    private void updateMediaLine() {
        MediaController controller = chooseVisibleMediaController();
        String mediaText = controller != null ? formatMediaText(controller.getMetadata()) : "";
        if (TextUtils.isEmpty(mediaText)) {
            mediaView.setText("");
            mediaIconView.setImageDrawable(null);
            mediaRow.setVisibility(View.GONE);
            return;
        }
        updateMediaIcon(controller);
        mediaView.setText(mediaText);
        mediaRow.setVisibility(View.VISIBLE);
    }

    private void updateMediaIcon(MediaController controller) {
        Drawable drawable = controller != null ? loadMediaNotificationIcon(getContext(), controller) : null;
        mediaIconView.setImageDrawable(drawable);
        mediaIconView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mediaView.getLayoutParams();
        int leftMargin = drawable != null ? dp(MEDIA_ICON_SPACING_DP) : 0;
        if (params.leftMargin != leftMargin) {
            params.leftMargin = leftMargin;
            mediaView.setLayoutParams(params);
        }
    }

    private MediaController chooseVisibleMediaController() {
        MediaController pausedCandidate = null;
        for (MediaController controller : mediaControllers) {
            if (controller == null) {
                continue;
            }
            PlaybackState state = null;
            try {
                state = controller.getPlaybackState();
            } catch (Throwable ignored) {
                // Try the next controller.
            }
            if (state == null) {
                continue;
            }
            int playbackState = state.getState();
            if (playbackState == PlaybackState.STATE_PLAYING
                    || playbackState == PlaybackState.STATE_BUFFERING
                    || playbackState == PlaybackState.STATE_CONNECTING
                    || playbackState == PlaybackState.STATE_FAST_FORWARDING
                    || playbackState == PlaybackState.STATE_REWINDING) {
                return controller;
            }
            if (pausedCandidate == null && playbackState == PlaybackState.STATE_PAUSED) {
                pausedCandidate = controller;
            }
        }
        return pausedCandidate;
    }

    private static String formatMediaText(MediaMetadata metadata) {
        if (metadata == null) {
            return "";
        }
        CharSequence title = firstNonEmpty(
                metadata.getText(MediaMetadata.METADATA_KEY_TITLE),
                metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
        CharSequence artist = firstNonEmpty(
                metadata.getText(MediaMetadata.METADATA_KEY_ARTIST),
                metadata.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
                metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
        MediaDescription description = metadata.getDescription();
        if (TextUtils.isEmpty(title) && description != null) {
            title = description.getTitle();
        }
        if (TextUtils.isEmpty(artist) && description != null) {
            artist = description.getSubtitle();
        }
        if (TextUtils.isEmpty(title)) {
            return "";
        }
        if (TextUtils.isEmpty(artist)) {
            return title.toString();
        }
        return title + " - " + artist;
    }

    private static CharSequence firstNonEmpty(CharSequence first, CharSequence second) {
        return !TextUtils.isEmpty(first) ? first : second;
    }

    private static CharSequence firstNonEmpty(CharSequence first, CharSequence second, CharSequence third) {
        if (!TextUtils.isEmpty(first)) {
            return first;
        }
        return !TextUtils.isEmpty(second) ? second : third;
    }

    private void rebuildNotificationIcons() {
        notificationIconRow.removeAllViews();
        List<StatusBarNotification> notifications = currentNotifications();
        if (notifications.isEmpty()) {
            notificationIconRow.setVisibility(View.GONE);
            applyClockMode(false);
            if (notificationRebuildLogCount < 16) {
                notificationRebuildLogCount++;
                PixelAodLog.log("rebuilt native AOD notification icons input=0 emitted=0");
            }
            return;
        }
        applyClockMode(true);
        notificationIconRow.setVisibility(View.VISIBLE);
        int emitted = 0;
        int loadFailures = 0;
        HashSet<String> seenPackages = new HashSet<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null || !seenPackages.add(sbn.getPackageName())) {
                continue;
            }
            Drawable drawable = loadSmallIconDrawable(getContext(), sbn);
            if (drawable == null) {
                loadFailures++;
                continue;
            }
            ImageView iconView = new ImageView(getContext());
            iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setImageDrawable(drawable);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(NOTIFICATION_ICON_SIZE_DP),
                    dp(NOTIFICATION_ICON_SIZE_DP));
            if (emitted > 0) {
                params.leftMargin = dp(NOTIFICATION_ICON_SPACING_DP);
            }
            notificationIconRow.addView(iconView, params);
            emitted++;
            if (emitted >= MAX_NOTIFICATION_ICONS) {
                break;
            }
        }
        notificationIconRow.setVisibility(emitted > 0 ? View.VISIBLE : View.GONE);
        applyClockMode(emitted > 0);
        if (notificationRebuildLogCount < 12) {
            notificationRebuildLogCount++;
            PixelAodLog.log("rebuilt native AOD notification icons input="
                    + notifications.size() + " emitted=" + emitted + " loadFailures=" + loadFailures
                    + " packages=" + describeNotificationPackages(notifications));
        }
    }

    private static List<StatusBarNotification> currentNotifications() {
        StatusBarNotification[] snapshot;
        synchronized (PixelAodClockView.class) {
            snapshot = activeNotifications;
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>();
        if (snapshot == null) {
            return list;
        }
        for (StatusBarNotification sbn : snapshot) {
            if (sbn != null && sbn.getNotification() != null) {
                list.add(sbn);
            }
        }
        return list;
    }

    private static StatusBarNotification[] sanitizeNotifications(StatusBarNotification[] notifications) {
        if (notifications == null || notifications.length == 0) {
            return EMPTY_NOTIFICATIONS;
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>(notifications.length);
        for (StatusBarNotification sbn : notifications) {
            if (isLockscreenVisibleNotification(sbn)) {
                list.add(sbn);
            }
        }
        return list.toArray(new StatusBarNotification[0]);
    }

    private static boolean isLockscreenVisibleNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        boolean testNotification = isTestNotification(sbn);
        if (MODULE_PACKAGE.equals(sbn.getPackageName()) && !testNotification) {
            return false;
        }
        if ("android".equals(sbn.getPackageName())
                || "com.android.systemui".equals(sbn.getPackageName())) {
            return false;
        }
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            return false;
        }
        if (!lockscreenNotificationsEnabled()) {
            logFilteredNotification(sbn, "global-lockscreen-notifications-disabled");
            return false;
        }
        if (notification.visibility == Notification.VISIBILITY_SECRET) {
            logFilteredNotification(sbn, "notification-visibility-secret");
            return false;
        }
        RankingSnapshot ranking;
        synchronized (PixelAodClockView.class) {
            ranking = notificationRankings.get(sbn.getKey());
        }
        String rankingHiddenReason = ranking != null ? ranking.hiddenReason() : null;
        if (rankingHiddenReason != null) {
            logFilteredNotification(sbn, rankingHiddenReason + " ranking=" + ranking);
            return false;
        }
        if (!testNotification && isLikelySilentNotification(notification, ranking)) {
            logFilteredNotification(sbn, "silent-or-low-importance ranking=" + ranking);
            return false;
        }
        logKeptNotification(sbn, ranking);
        return true;
    }

    private static boolean isTestNotification(StatusBarNotification sbn) {
        return sbn != null
                && MODULE_PACKAGE.equals(sbn.getPackageName())
                && TestNotificationReceiver.TEST_TAG.equals(sbn.getTag());
    }

    private static boolean isLikelySilentNotification(Notification notification, RankingSnapshot ranking) {
        if (ranking != null && ranking.importance != NotificationManagerImportance.UNKNOWN
                && ranking.importance < NotificationManagerImportance.DEFAULT) {
            return true;
        }
        return (notification.flags & NOTIFICATION_FLAG_SILENT) != 0;
    }

    private static boolean isMediaIconCandidate(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null
                || sbn.getNotification().getSmallIcon() == null) {
            return false;
        }
        Notification notification = sbn.getNotification();
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
            return true;
        }
        try {
            Bundle extras = notification.extras;
            return extras != null && extras.containsKey(Notification.EXTRA_MEDIA_SESSION);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean lockscreenNotificationsEnabled() {
        Context context = appContext;
        if (context != null) {
            try {
                return Settings.Secure.getInt(context.getContentResolver(),
                        "lock_screen_show_notifications", 1) != 0;
            } catch (Throwable ignored) {
                return true;
            }
        }
        return true;
    }

    private static Drawable loadMediaNotificationIcon(Context context, MediaController controller) {
        if (context == null || controller == null) {
            return null;
        }
        StatusBarNotification notification = findMediaNotification(controller);
        if (notification != null) {
            Drawable smallIcon = loadMonochromeNotificationIcon(context, notification);
            if (smallIcon != null) {
                logMediaIconChoice(notification.getPackageName(), "notification-smallIcon");
                return smallIcon;
            }
        }
        Drawable monochrome = loadApplicationMonochromeIcon(context, controller.getPackageName());
        if (monochrome != null) {
            logMediaIconChoice(controller.getPackageName(), "app-monochrome-fallback");
            return monochrome;
        }
        return null;
    }

    private static StatusBarNotification findMediaNotification(MediaController controller) {
        String packageName = controller.getPackageName();
        ArrayList<StatusBarNotification> snapshot;
        synchronized (PixelAodClockView.class) {
            snapshot = new ArrayList<>(mediaNotificationCache.values());
        }
        if (snapshot.isEmpty()) {
            synchronized (PixelAodClockView.class) {
                snapshot = new ArrayList<>();
                if (rawNotifications != null) {
                    Collections.addAll(snapshot, rawNotifications);
                }
            }
        }
        StatusBarNotification categoryCandidate = null;
        StatusBarNotification packageCandidate = null;
        for (StatusBarNotification sbn : snapshot) {
            if (sbn == null || sbn.getNotification() == null
                    || !TextUtils.equals(packageName, sbn.getPackageName())) {
                continue;
            }
            Notification notification = sbn.getNotification();
            if (matchesMediaSession(notification, controller)) {
                return sbn;
            }
            if (categoryCandidate == null && Notification.CATEGORY_TRANSPORT.equals(notification.category)) {
                categoryCandidate = sbn;
            }
            if (packageCandidate == null && notification.getSmallIcon() != null) {
                packageCandidate = sbn;
            }
        }
        return categoryCandidate != null ? categoryCandidate : packageCandidate;
    }

    private static boolean matchesMediaSession(Notification notification, MediaController controller) {
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

    private static Drawable loadMonochromeNotificationIcon(Context context, StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null || notification.getSmallIcon() == null) {
                return null;
            }
            Drawable drawable = loadIconDrawable(context, sbn.getPackageName(), notification.getSmallIcon());
            if (drawable == null) {
                return null;
            }
            Drawable result = drawable.mutate();
            result.setTint(resolveMaterialInfoColor(context));
            result.setTintMode(PorterDuff.Mode.SRC_IN);
            return result;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load media notification smallIcon pkg=" + sbn.getPackageName(), t);
            return null;
        }
    }

    private static Drawable loadApplicationMonochromeIcon(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName) || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            Drawable drawable = loadApplicationIcon(packageManager, packageName);
            if (!(drawable instanceof AdaptiveIconDrawable)) {
                return null;
            }
            Drawable monochrome = ((AdaptiveIconDrawable) drawable).getMonochrome();
            if (monochrome == null) {
                return null;
            }
            Drawable result = monochrome.mutate();
            result.setTint(resolveMaterialInfoColor(context));
            result.setTintMode(PorterDuff.Mode.SRC_IN);
            if (looksLikeFilledMonochromeMask(result) || looksLikeTinyForeground(result)) {
                logRejectedMediaIcon(packageName);
                return null;
            }
            return result;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load AOD media monochrome icon pkg=" + packageName, t);
            return null;
        }
    }

    private static Drawable loadApplicationIconDrawable(Context context, String packageName, boolean monochrome) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            Drawable drawable = loadApplicationIcon(packageManager, packageName);
            if (drawable == null) {
                return null;
            }
            Drawable result = drawable.mutate();
            if (monochrome) {
                result.setTint(resolveMaterialInfoColor(context));
                result.setTintMode(PorterDuff.Mode.SRC_IN);
            }
            return result;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load AOD application icon pkg=" + packageName, t);
            return null;
        }
    }

    private static Drawable loadApplicationIcon(PackageManager packageManager, String packageName)
            throws PackageManager.NameNotFoundException {
        ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
        Drawable drawable = info.loadIcon(packageManager);
        return drawable != null ? drawable : packageManager.getApplicationIcon(packageName);
    }

    static Drawable loadSmallIconDrawable(Context context, StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null) {
                return null;
            }
            Icon icon = notification.getSmallIcon();
            if (icon == null) {
                return null;
            }
            Drawable drawable = loadIconDrawable(context, sbn.getPackageName(), icon);
            if (drawable == null) {
                return null;
            }
            boolean filledMask = looksLikeFilledNotificationMask(drawable);
            boolean tinyForeground = looksLikeTinyForeground(drawable);
            if (filledMask || tinyForeground) {
                Drawable monochrome = loadApplicationMonochromeIcon(context, sbn.getPackageName());
                if (monochrome != null) {
                    logNotificationIconChoice(sbn.getPackageName(), "app-monochrome-fallback");
                    return monochrome;
                }
                Drawable applicationIcon = loadApplicationIconDrawable(context, sbn.getPackageName(), false);
                if (applicationIcon != null) {
                    logNotificationIconChoice(sbn.getPackageName(),
                            "app-color-fallback filled=" + filledMask + " tiny=" + tinyForeground);
                    return applicationIcon;
                }
            }
            Drawable mutated = drawable.mutate();
            mutated.setTint(resolveMaterialInfoColor(context));
            mutated.setTintMode(PorterDuff.Mode.SRC_IN);
            logNotificationIconChoice(sbn.getPackageName(), "notification-smallIcon");
            return mutated;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load native notification smallIcon", t);
            return null;
        }
    }

    private static Drawable loadIconDrawable(Context context, String packageName, Icon icon) {
        Drawable drawable = null;
        try {
            Context packageContext = context.createPackageContext(
                    packageName, Context.CONTEXT_IGNORE_SECURITY);
            drawable = icon.loadDrawable(packageContext);
        } catch (Throwable ignored) {
            // Some icons already carry their resource package; load them through SystemUI.
        }
        if (drawable == null) {
            try {
                drawable = icon.loadDrawable(context);
            } catch (Throwable ignored) {
                return null;
            }
        }
        return drawable;
    }

    private static boolean looksLikeTinyForeground(Drawable drawable) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE);
            drawable.draw(canvas);
            int opaque = 0;
            int size = ICON_MASK_SAMPLE_SIZE;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (Color.alpha(bitmap.getPixel(x, y)) > 32) {
                        opaque++;
                    }
                }
            }
            return opaque / (float) (size * size) < 0.16f;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static boolean looksLikeFilledNotificationMask(Drawable drawable) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE);
            drawable.draw(canvas);
            int opaque = 0;
            int edgeOpaque = 0;
            int cornerOpaque = 0;
            int size = ICON_MASK_SAMPLE_SIZE;
            int corner = Math.max(4, size / 6);
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    int alpha = Color.alpha(bitmap.getPixel(x, y));
                    if (alpha <= 32) {
                        continue;
                    }
                    opaque++;
                    if (x == 0 || y == 0 || x == size - 1 || y == size - 1) {
                        edgeOpaque++;
                    }
                    boolean inHorizontalCorner = x < corner || x >= size - corner;
                    boolean inVerticalCorner = y < corner || y >= size - corner;
                    if (inHorizontalCorner && inVerticalCorner) {
                        cornerOpaque++;
                    }
                }
            }
            float area = opaque / (float) (size * size);
            float edge = edgeOpaque / (float) (size * 4 - 4);
            float cornerArea = cornerOpaque / (float) (corner * corner * 4);
            return area > 0.68f || edge > 0.55f || cornerArea > 0.72f;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static boolean looksLikeFilledMonochromeMask(Drawable drawable) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE);
            drawable.draw(canvas);
            int opaque = 0;
            int edgeOpaque = 0;
            int cornerOpaque = 0;
            int transitions = 0;
            int size = ICON_MASK_SAMPLE_SIZE;
            int corner = Math.max(4, size / 6);
            boolean[][] solid = new boolean[size][size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean alpha = Color.alpha(bitmap.getPixel(x, y)) > 32;
                    solid[y][x] = alpha;
                    if (!alpha) {
                        continue;
                    }
                    opaque++;
                    if (x == 0 || y == 0 || x == size - 1 || y == size - 1) {
                        edgeOpaque++;
                    }
                    boolean inHorizontalCorner = x < corner || x >= size - corner;
                    boolean inVerticalCorner = y < corner || y >= size - corner;
                    if (inHorizontalCorner && inVerticalCorner) {
                        cornerOpaque++;
                    }
                }
            }
            for (int y = 0; y < size; y++) {
                for (int x = 1; x < size; x++) {
                    if (solid[y][x] != solid[y][x - 1]) {
                        transitions++;
                    }
                }
            }
            for (int y = 1; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    if (solid[y][x] != solid[y - 1][x]) {
                        transitions++;
                    }
                }
            }
            float area = opaque / (float) (size * size);
            float edge = edgeOpaque / (float) (size * 4 - 4);
            float cornerArea = cornerOpaque / (float) (corner * corner * 4);
            float transitionRate = transitions / (float) (size * size);
            return (area > 0.72f && transitionRate < 0.09f)
                    || edge > 0.58f
                    || cornerArea > 0.76f;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static String describeNotificationPackages(StatusBarNotification[] notifications) {
        if (notifications == null || notifications.length == 0) {
            return "";
        }
        ArrayList<StatusBarNotification> list = new ArrayList<>(notifications.length);
        Collections.addAll(list, notifications);
        return describeNotificationPackages(list);
    }

    private static String describeNotificationPackages(List<StatusBarNotification> notifications) {
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

    private static void logFilteredNotification(StatusBarNotification sbn, String reason) {
        if (notificationFilterLogCount >= 40) {
            return;
        }
        notificationFilterLogCount++;
        PixelAodLog.log("filtered AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
                + " reason=" + reason);
    }

    private static void logKeptNotification(StatusBarNotification sbn, RankingSnapshot ranking) {
        if (notificationKeepLogCount >= 30) {
            return;
        }
        notificationKeepLogCount++;
        PixelAodLog.log("kept AOD notification pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " category=" + sbn.getNotification().category
                + " visibility=" + sbn.getNotification().visibility
                + " ranking=" + ranking);
    }

    private static void logNotificationIconChoice(String packageName, String mode) {
        if (notificationIconLogCount >= 30) {
            return;
        }
        notificationIconLogCount++;
        PixelAodLog.log("AOD notification icon mode=" + mode + " pkg=" + packageName);
    }

    private static void logMediaIconChoice(String packageName, String mode) {
        if (mediaIconLogCount >= 20) {
            return;
        }
        mediaIconLogCount++;
        PixelAodLog.log("AOD media icon mode=" + mode + " pkg=" + packageName);
    }

    private static void logRejectedMediaIcon(String packageName) {
        if (mediaIconRejectLogCount >= 8) {
            return;
        }
        mediaIconRejectLogCount++;
        PixelAodLog.log("ignored blocky AOD media monochrome icon pkg=" + packageName);
    }

    private static void logMediaNotificationCache(String action, String source, int count) {
        if (mediaNotificationLogCount >= 16) {
            return;
        }
        mediaNotificationLogCount++;
        PixelAodLog.log("AOD media notification cache " + action
                + " source=" + source + " count=" + count);
    }

    private static void refreshMediaLines() {
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateMediaLine();
                }
            }
        });
    }

    private void applyMaterialColors() {
        int clockColor = resolveMaterialClockColor(getContext());
        int infoColor = resolveMaterialInfoColor(getContext());
        clockView.setTextColor(clockColor);
        dateView.setTextColor(infoColor);
        mediaView.setTextColor(infoColor);
        batteryView.setTextColor(infoColor);
        chargeBoltView.setTint(infoColor);
        applyWeatherIcon(dateView, currentFreshWeather(getContext()), infoColor);
    }

    static int resolveMaterialClockColor(Context context) {
        return resolveWallpaperTextColor(context, CLOCK_COLOR);
    }

    static int resolveMaterialInfoColor(Context context) {
        return withAlpha(resolveWallpaperTextColor(context, INFO_COLOR), 230);
    }

    private static int resolveWallpaperTextColor(Context context, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            int attrId = context.getResources().getIdentifier(
                    "wallpaperTextColor", "attr", "com.android.systemui");
            if (attrId == 0) {
                attrId = context.getResources().getIdentifier(
                        "wallpaperTextColor", "attr", context.getPackageName());
            }
            if (attrId != 0) {
                TypedValue value = new TypedValue();
                if (context.getTheme() != null
                        && context.getTheme().resolveAttribute(attrId, value, true)) {
                    if (value.resourceId != 0) {
                        return context.getColor(value.resourceId);
                    }
                    if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                            && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        return value.data;
                    }
                }
            }
        } catch (Throwable ignored) {
            // Wallpaper text colors are provided by SystemUI and vary by OEM theme.
        }
        return fallback;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private TextView makeInfoLine(Context context, Typeface typeface, int textSizeDp, int gravity) {
        TextView textView = new TextView(context);
        textView.setTextColor(resolveMaterialInfoColor(context));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        textView.setTypeface(typeface);
        textView.setIncludeFontPadding(false);
        textView.setGravity(gravity);
        textView.setSingleLine(true);
        textView.setLetterSpacing(0f);
        textView.setAlpha(0.94f);
        return textView;
    }

    static void applyWeatherIcon(TextView textView, WeatherSnapshot weather, int color) {
        if (textView == null) {
            return;
        }
        if (weather == null || !weather.hasIcon()) {
            textView.setCompoundDrawablesRelative(null, null, null, null);
            textView.setCompoundDrawablePadding(0);
            return;
        }
        int size = dp(textView.getContext(), WEATHER_ICON_SIZE_DP);
        WeatherIconDrawable drawable = new WeatherIconDrawable(weather.weatherCode, color);
        drawable.setBounds(0, 0, size, size);
        textView.setCompoundDrawablePadding(dp(textView.getContext(), WEATHER_ICON_PADDING_DP));
        textView.setCompoundDrawablesRelative(null, null, drawable, null);
    }

    static WeatherSnapshot currentFreshWeather() {
        return currentFreshWeather(appContext);
    }

    static WeatherSnapshot currentFreshWeather(Context context) {
        if (!PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_WEATHER, true)) {
            return WeatherSnapshot.empty();
        }
        WeatherSnapshot snapshot;
        synchronized (PixelAodClockView.class) {
            snapshot = breezyWeather.isFresh(System.currentTimeMillis())
                    ? breezyWeather
                    : WeatherSnapshot.empty();
        }
        if (!snapshot.hasDisplayableWeather()) {
            maybeRequestCachedBreezyWeather();
        }
        return snapshot;
    }

    private static void maybeRequestCachedBreezyWeather() {
        Context context = appContext;
        if (context == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (PixelAodClockView.class) {
            if (now - lastCachedWeatherRequestAt < 30_000L) {
                return;
            }
            lastCachedWeatherRequestAt = now;
        }
        requestCachedBreezyWeather(context);
    }

    static String formatDateWithWeather(Calendar calendar) {
        Locale locale = Locale.getDefault();
        String pattern = locale.getLanguage().equals(Locale.CHINESE.getLanguage())
                ? "M\u6708d\u65e5 EEEE" : "EEE, MMM d";
        String date = new SimpleDateFormat(pattern, locale).format(calendar.getTime());
        WeatherSnapshot weather = currentFreshWeather(appContext);
        if (!TextUtils.isEmpty(weather.temperatureText)) {
            return date + " \u00b7 " + weather.temperatureText;
        }
        return date;
    }

    private void updateTime() {
        Calendar calendar = Calendar.getInstance();
        boolean is24Hour = DateFormat.is24HourFormat(getContext());
        int hour = calendar.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) {
            hour = 12;
        }
        int minute = calendar.get(Calendar.MINUTE);
        if (compactClock) {
            clockView.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
        } else {
            clockView.setText(String.format(Locale.getDefault(), "%02d\n%02d", hour, minute));
        }
        int infoColor = resolveMaterialInfoColor(getContext());
        dateView.setText(formatAtAGlanceLine(calendar));
        applyWeatherIcon(dateView, currentFreshWeather(getContext()), infoColor);
        BatteryStatus batteryStatus = readBatteryStatus();
        batteryView.setText(batteryStatus.percentText);
        chargeBoltView.setVisibility(batteryStatus.charging ? View.VISIBLE : View.GONE);
        batteryRow.setVisibility(TextUtils.isEmpty(batteryStatus.percentText) ? View.GONE : View.VISIBLE);
        applyBurnInTranslation();
    }

    private void applyBurnInTranslation() {
        if (getVisibility() != View.VISIBLE || !shouldCustomizeAodNow(getContext())) {
            resetBurnInTranslation();
            return;
        }
        float minutes = System.currentTimeMillis() / 60000f;
        int maxX = dp(BURN_IN_OFFSET_X_DP);
        int maxY = dp(BURN_IN_OFFSET_Y_DP);
        float x = zigzag(minutes, maxX, BURN_IN_PERIOD_X_MINUTES) - maxX / 2f;
        float y = zigzag(minutes, maxY, BURN_IN_PERIOD_Y_MINUTES) - maxY / 2f;
        setTranslationX(x);
        setTranslationY(y);
        synchronized (PixelAodClockView.class) {
            lastBurnInTranslationX = x;
            lastBurnInTranslationY = y;
        }
        if (burnInLogCount < 8) {
            burnInLogCount++;
            PixelAodLog.log("applied Pixel AOD burn-in offset x=" + Math.round(x)
                    + " y=" + Math.round(y));
        }
    }

    private void resetBurnInTranslation() {
        if (getTranslationX() != 0f) {
            setTranslationX(0f);
        }
        if (getTranslationY() != 0f) {
            setTranslationY(0f);
        }
    }

    private static float zigzag(float value, float amplitude, float period) {
        float x = value % period / (period / 2f);
        float interpolation = x <= 1f ? x : 2f - x;
        return amplitude * interpolation;
    }

    private void applyClockMode(boolean compact) {
        boolean changed = compactClock != compact;
        compactClock = compact;
        FrameLayout.LayoutParams clockParams = (FrameLayout.LayoutParams) clockView.getLayoutParams();
        if (changed && compact) {
            clockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                    scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP));
            clockView.setGravity(Gravity.START);
            clockView.setTextAlignment(TEXT_ALIGNMENT_TEXT_START);
            clockView.setSingleLine(true);
            clockView.setLines(1);
            clockView.setLineSpacing(0f, 1f);
            clockView.setLetterSpacing(COMPACT_CLOCK_LETTER_SPACING);
            clockView.setFontFeatureSettings("pnum");
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.gravity = Gravity.TOP | Gravity.START;
            clockParams.leftMargin = dp(INFO_EDGE_DP);
            clockParams.topMargin = dp(SMALL_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        } else if (changed) {
            clockView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,
                    scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP));
            clockView.setGravity(Gravity.CENTER_HORIZONTAL);
            clockView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            clockView.setSingleLine(false);
            clockView.setLines(2);
            clockView.setLineSpacing(0f, CLOCK_LINE_SPACING);
            clockView.setLetterSpacing(LARGE_CLOCK_LETTER_SPACING);
            clockView.setFontFeatureSettings("tnum");
            clockParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            clockParams.leftMargin = 0;
            clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        }
        if (changed) {
            clockView.setLayoutParams(clockParams);
        }
        updateInfoStackLayout();
        updateTime();
        if (changed) {
            PixelAodLog.log("switched Pixel AOD clock mode compact=" + compact);
        }
    }

    private void updateInfoStackLayout() {
        FrameLayout.LayoutParams dateParams = (FrameLayout.LayoutParams) dateView.getLayoutParams();
        FrameLayout.LayoutParams notificationParams = (FrameLayout.LayoutParams) notificationIconRow.getLayoutParams();
        FrameLayout.LayoutParams mediaParams = (FrameLayout.LayoutParams) mediaRow.getLayoutParams();
        if (compactClock) {
            dateParams.topMargin = dp(SMALL_INFO_TOP_DP);
            notificationParams.topMargin = dp(SMALL_NOTIFICATION_LINE_TOP_DP);
            mediaParams.topMargin = dp(SMALL_MEDIA_TOP_DP);
        } else {
            dateParams.topMargin = dp(LARGE_INFO_TOP_DP);
            notificationParams.topMargin = dp(LARGE_NOTIFICATION_LINE_TOP_DP);
            mediaParams.topMargin = dp(notificationIconRow.getVisibility() == View.VISIBLE
                    ? LARGE_MEDIA_TOP_DP : LARGE_MEDIA_NO_NOTIFICATIONS_TOP_DP);
        }
        dateView.setLayoutParams(dateParams);
        notificationIconRow.setLayoutParams(notificationParams);
        mediaRow.setLayoutParams(mediaParams);
    }

    private String formatAtAGlanceLine(Calendar calendar) {
        String date = formatDateWithWeather(calendar);
        String extra;
        synchronized (PixelAodClockView.class) {
            extra = atAGlanceExtra;
        }
        if (TextUtils.isEmpty(extra)) {
            return date;
        }
        return date + " · " + extra;
    }

    private static String normalizeAtAGlanceExtra(String extra) {
        if (extra == null) {
            return "";
        }
        String normalized = extra.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() > 24) {
            normalized = normalized.substring(0, 24).trim();
        }
        return normalized;
    }

    private BatteryStatus readBatteryStatus() {
        try {
            Intent intent = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (intent == null) {
                return BatteryStatus.empty();
            }
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level < 0 || scale <= 0) {
                return BatteryStatus.empty();
            }
            String percent = Math.round(level * 100f / scale) + "%";
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL
                    || plugged != 0;
            return new BatteryStatus(percent, charging);
        } catch (Throwable t) {
            return BatteryStatus.empty();
        }
    }

    static Typeface sharedClockTypeface(Context context, int weight) {
        return weighted(resolveClockTypeface(context), weight);
    }

    static Typeface sharedInfoTypeface(Context context, int weight) {
        return weighted(resolveInfoTypeface(context), weight);
    }

    static int aodClockWeight() {
        return aodClockWeight(appContext);
    }

    static int aodClockWeight(Context context) {
        return PixelAodSettings.getIntFromFloat(context, PixelAodSettings.KEY_AOD_WEIGHT,
                PixelAodSettings.DEFAULT_AOD_WEIGHT, 160, 700);
    }

    static int lockscreenClockWeight(Context context) {
        return PixelAodSettings.getIntFromFloat(context, PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT, 250, 800);
    }

    static int scaledClockTextDp(Context context, int baseDp) {
        float scale = PixelAodSettings.getFloat(context, PixelAodSettings.KEY_CLOCK_SCALE,
                PixelAodSettings.DEFAULT_CLOCK_SCALE);
        scale = Math.max(0.75f, Math.min(1.35f, scale));
        return Math.round(baseDp * scale);
    }

    static void applySharedFontVariation(TextView textView, int weight) {
        applyFontVariation(textView, weight);
    }

    private static Typeface resolveClockTypeface(Context context) {
        synchronized (PixelAodClockView.class) {
            if (cachedClockTypeface != null) {
                return cachedClockTypeface;
            }
            cachedClockTypeface = loadGoogleSansFlex(context,
                    GOOGLE_SANS_FLEX_CLOCK_ASSET,
                    GOOGLE_SANS_FLEX_CLOCK_CACHE);
            if (cachedClockTypeface != null) {
                PixelAodLog.log("loaded bundled Google Sans Flex 200 for AOD clock");
                return cachedClockTypeface;
            }
            cachedClockTypeface = loadGoogleSansFlex(context,
                    GOOGLE_SANS_FLEX_REGULAR_ASSET,
                    GOOGLE_SANS_FLEX_REGULAR_CACHE);
            if (cachedClockTypeface != null) {
                PixelAodLog.log("loaded bundled Google Sans Flex regular fallback for AOD clock");
                return cachedClockTypeface;
            }
            File androidClock = new File(ANDROID_CLOCK_FONT);
            if (androidClock.isFile() && androidClock.canRead()) {
                try {
                    cachedClockTypeface = Typeface.createFromFile(androidClock);
                    PixelAodLog.log("loaded Android clock font " + ANDROID_CLOCK_FONT);
                    return cachedClockTypeface;
                } catch (Throwable t) {
                    PixelAodLog.log("failed to load " + ANDROID_CLOCK_FONT, t);
                }
            }
            try {
                cachedClockTypeface = Typeface.MONOSPACE;
                PixelAodLog.log("using fallback AOSP Pixel clock typeface semantics: monospace weight 200");
                return cachedClockTypeface;
            } catch (Throwable ignored) {
                // Try named families below.
            }
            cachedClockTypeface = firstUsableTypeface(
                    "google-sans-flex",
                    "roboto-flex",
                    "osans-solid-digits",
                    "google-sans",
                    "sans-serif-light",
                    "sans-serif");
            return cachedClockTypeface;
        }
    }

    private static Typeface resolveInfoTypeface(Context context) {
        synchronized (PixelAodClockView.class) {
            if (cachedInfoTypeface != null) {
                return cachedInfoTypeface;
            }
            cachedInfoTypeface = loadGoogleSansFlex(context,
                    GOOGLE_SANS_FLEX_INFO_ASSET,
                    GOOGLE_SANS_FLEX_INFO_CACHE);
            if (cachedInfoTypeface != null) {
                PixelAodLog.log("loaded bundled Google Sans Flex 500 for AOD info");
                return cachedInfoTypeface;
            }
            cachedInfoTypeface = loadGoogleSansFlex(context,
                    GOOGLE_SANS_FLEX_REGULAR_ASSET,
                    GOOGLE_SANS_FLEX_REGULAR_CACHE);
            if (cachedInfoTypeface != null) {
                PixelAodLog.log("loaded bundled Google Sans Flex regular fallback for AOD info");
                return cachedInfoTypeface;
            }
            cachedInfoTypeface = firstUsableTypeface(
                    "google-sans-flex",
                    "google-sans-text",
                    "google-sans",
                    "roboto-flex",
                    "sys-sans-en",
                    "op-sans-en",
                    "sans-serif");
            return cachedInfoTypeface;
        }
    }

    private static Typeface firstUsableTypeface(String... families) {
        for (String family : families) {
            try {
                Typeface typeface = Typeface.create(family, Typeface.NORMAL);
                if (typeface != null) {
                    return typeface;
                }
            } catch (Throwable ignored) {
                // Try the next system family.
            }
        }
        return Typeface.DEFAULT;
    }

    private static Typeface loadGoogleSansFlex(Context context, String assetName, String cacheName) {
        if (context == null) {
            return null;
        }
        String apkPath;
        synchronized (PixelAodClockView.class) {
            apkPath = modulePath;
        }
        if (TextUtils.isEmpty(apkPath)) {
            return null;
        }
        File fontFile = new File(context.getCacheDir(), cacheName);
        if (ensureExtractedAsset(apkPath, assetName, fontFile)) {
            try {
                return Typeface.createFromFile(fontFile);
            } catch (Throwable t) {
                PixelAodLog.log("failed to load extracted Google Sans Flex", t);
            }
        }
        return null;
    }

    private static boolean ensureExtractedAsset(String apkPath, String entryName, File outFile) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(apkPath);
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null || entry.getSize() <= 0) {
                return false;
            }
            if (outFile.isFile() && outFile.length() == entry.getSize()) {
                return true;
            }
            File parent = outFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                return false;
            }
            InputStream input = null;
            FileOutputStream output = null;
            try {
                input = zipFile.getInputStream(entry);
                output = new FileOutputStream(outFile, false);
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return true;
            } finally {
                closeQuietly(input);
                closeQuietly(output);
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to extract bundled Google Sans Flex", t);
            return false;
        } finally {
            closeQuietly(zipFile);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable ignored) {
            // Best-effort cleanup.
        }
    }

    private static Typeface weighted(Typeface typeface, int weight) {
        if (typeface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return Typeface.create(typeface, weight, false);
            } catch (Throwable ignored) {
                return typeface;
            }
        }
        return typeface;
    }

    private static void applyFontVariation(TextView textView, int weight) {
        if (textView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            textView.setFontVariationSettings("'opsz' 144, 'wght' " + weight
                    + ", 'GRAD' 0, 'ROND' 0, 'wdth' 100");
        } catch (Throwable ignored) {
            // Static Google Fonts files ignore variation axes; explicit weights above still apply.
        }
    }

    private long millisUntilNextMinute() {
        Calendar calendar = Calendar.getInstance();
        long elapsed = calendar.get(Calendar.SECOND) * 1000L + calendar.get(Calendar.MILLISECOND);
        return Math.max(1000L, 60_000L - elapsed + 50L);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static Handler mainHandler() {
        return MainHandlerHolder.MAIN;
    }

    private static final class MainHandlerHolder {
        static final Handler MAIN = new Handler(Looper.getMainLooper());
    }

    static final class BurnInOffset {
        final float translationX;
        final float translationY;

        BurnInOffset(float translationX, float translationY) {
            this.translationX = translationX;
            this.translationY = translationY;
        }
    }

    private static final class BatteryStatus {
        final String percentText;
        final boolean charging;

        BatteryStatus(String percentText, boolean charging) {
            this.percentText = percentText;
            this.charging = charging;
        }

        static BatteryStatus empty() {
            return new BatteryStatus("", false);
        }
    }

    static final class WeatherSnapshot {
        final String temperatureText;
        final int weatherCode;
        final String conditionText;
        final long timestampMillis;

        WeatherSnapshot(String temperatureText, int weatherCode, String conditionText, long timestampMillis) {
            this.temperatureText = temperatureText;
            this.weatherCode = weatherCode;
            this.conditionText = normalizeWeatherCondition(conditionText);
            this.timestampMillis = timestampMillis;
        }

        boolean isFresh(long nowMillis) {
            return hasDisplayableWeather()
                    && timestampMillis > 0L
                    && Math.abs(nowMillis - timestampMillis) <= WEATHER_STALE_MILLIS;
        }

        boolean hasDisplayableWeather() {
            return !TextUtils.isEmpty(temperatureText) || hasIcon();
        }

        boolean hasIcon() {
            return weatherCode != Integer.MIN_VALUE
                    || !TextUtils.isEmpty(conditionText);
        }

        boolean sameDisplay(WeatherSnapshot other) {
            return other != null
                    && TextUtils.equals(temperatureText, other.temperatureText)
                    && weatherCode == other.weatherCode
                    && TextUtils.equals(conditionText, other.conditionText);
        }

        String logText() {
            if (TextUtils.isEmpty(temperatureText)) {
                return conditionText;
            }
            if (TextUtils.isEmpty(conditionText)) {
                return temperatureText;
            }
            return temperatureText + " " + conditionText;
        }

        static WeatherSnapshot from(String temperatureText, int weatherCode,
                String conditionText, long timestampMillis) {
            if (weatherCode == Integer.MIN_VALUE) {
                weatherCode = inferWeatherCode(conditionText);
            }
            return new WeatherSnapshot(temperatureText, weatherCode, conditionText, timestampMillis);
        }

        static WeatherSnapshot empty() {
            return new WeatherSnapshot("", Integer.MIN_VALUE, "", 0L);
        }
    }

    private static int inferWeatherCode(String conditionText) {
        if (conditionText == null) {
            return Integer.MIN_VALUE;
        }
        String lower = conditionText.toLowerCase(Locale.US);
        if (lower.contains("thunder") || lower.contains("storm")) {
            return 211;
        }
        if (lower.contains("rain") || lower.contains("drizzle") || lower.contains("shower")) {
            return 500;
        }
        if (lower.contains("snow")) {
            return 600;
        }
        if (lower.contains("sleet") || lower.contains("hail") || lower.contains("ice")) {
            return 611;
        }
        if (lower.contains("fog") || lower.contains("mist")) {
            return 741;
        }
        if (lower.contains("haze") || lower.contains("smoke") || lower.contains("dust")) {
            return 721;
        }
        if (lower.contains("wind")) {
            return 771;
        }
        if (lower.contains("clear") || lower.contains("sun")) {
            return 800;
        }
        if (lower.contains("cloud") || lower.contains("overcast")) {
            return 804;
        }
        return Integer.MIN_VALUE;
    }

    private static final class WeatherIconDrawable extends Drawable {
        private final int weatherCode;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF rect = new RectF();

        WeatherIconDrawable(int weatherCode, int color) {
            this.weatherCode = weatherCode;
            stroke.setColor(color);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            fill.setColor(color);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            float w = getBounds().width();
            float h = getBounds().height();
            float left = getBounds().left;
            float top = getBounds().top;
            if (w <= 0f || h <= 0f) {
                return;
            }
            float unit = Math.min(w, h);
            stroke.setStrokeWidth(Math.max(1.2f, unit * 0.105f));
            canvas.save();
            canvas.translate(left, top);
            int family = weatherFamily(weatherCode);
            if (family == 1) {
                drawSun(canvas, unit);
            } else if (family == 2) {
                drawCloud(canvas, unit, 0f, 0f);
            } else if (family == 3) {
                drawCloud(canvas, unit, 0f, -unit * 0.08f);
                drawRain(canvas, unit);
            } else if (family == 4) {
                drawCloud(canvas, unit, 0f, -unit * 0.08f);
                drawSnow(canvas, unit);
            } else if (family == 5) {
                drawCloud(canvas, unit, 0f, -unit * 0.08f);
                drawBolt(canvas, unit);
            } else if (family == 6) {
                drawFog(canvas, unit);
            } else if (family == 7) {
                drawWind(canvas, unit);
            } else {
                drawCloud(canvas, unit, 0f, 0f);
            }
            canvas.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            stroke.setAlpha(alpha);
            fill.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            stroke.setColorFilter(colorFilter);
            fill.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        private static int weatherFamily(int code) {
            if (code == Integer.MIN_VALUE) {
                return 2;
            }
            if ((code >= 200 && code < 300) || code == 781) {
                return 5;
            }
            if ((code >= 300 && code < 600) || code == 511) {
                return 3;
            }
            if (code >= 600 && code < 700) {
                return 4;
            }
            if (code == 701 || code == 711 || code == 721 || code == 731
                    || code == 741 || code == 751 || code == 761 || code == 762) {
                return 6;
            }
            if (code == 771 || code == 905 || (code >= 952 && code <= 962)) {
                return 7;
            }
            if (code == 800) {
                return 1;
            }
            return 2;
        }

        private void drawSun(Canvas canvas, float u) {
            float cx = u * 0.5f;
            float cy = u * 0.5f;
            canvas.drawCircle(cx, cy, u * 0.18f, stroke);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2d * i / 8d;
                float sx = cx + (float) Math.cos(angle) * u * 0.30f;
                float sy = cy + (float) Math.sin(angle) * u * 0.30f;
                float ex = cx + (float) Math.cos(angle) * u * 0.42f;
                float ey = cy + (float) Math.sin(angle) * u * 0.42f;
                canvas.drawLine(sx, sy, ex, ey, stroke);
            }
        }

        private void drawCloud(Canvas canvas, float u, float dx, float dy) {
            path.reset();
            path.moveTo(u * 0.20f + dx, u * 0.68f + dy);
            path.cubicTo(u * 0.10f + dx, u * 0.68f + dy, u * 0.10f + dx, u * 0.47f + dy,
                    u * 0.29f + dx, u * 0.48f + dy);
            path.cubicTo(u * 0.32f + dx, u * 0.31f + dy, u * 0.56f + dx, u * 0.30f + dy,
                    u * 0.63f + dx, u * 0.48f + dy);
            path.cubicTo(u * 0.80f + dx, u * 0.47f + dy, u * 0.88f + dx, u * 0.68f + dy,
                    u * 0.70f + dx, u * 0.68f + dy);
            path.close();
            canvas.drawPath(path, stroke);
        }

        private void drawRain(Canvas canvas, float u) {
            canvas.drawLine(u * 0.34f, u * 0.76f, u * 0.28f, u * 0.92f, stroke);
            canvas.drawLine(u * 0.52f, u * 0.76f, u * 0.46f, u * 0.92f, stroke);
            canvas.drawLine(u * 0.70f, u * 0.76f, u * 0.64f, u * 0.92f, stroke);
        }

        private void drawSnow(Canvas canvas, float u) {
            float[] xs = {u * 0.34f, u * 0.52f, u * 0.70f};
            for (float x : xs) {
                float y = u * 0.84f;
                canvas.drawLine(x - u * 0.045f, y, x + u * 0.045f, y, stroke);
                canvas.drawLine(x, y - u * 0.045f, x, y + u * 0.045f, stroke);
            }
        }

        private void drawBolt(Canvas canvas, float u) {
            path.reset();
            path.moveTo(u * 0.52f, u * 0.70f);
            path.lineTo(u * 0.40f, u * 0.98f);
            path.lineTo(u * 0.62f, u * 0.82f);
            path.lineTo(u * 0.52f, u * 0.82f);
            path.close();
            canvas.drawPath(path, fill);
        }

        private void drawFog(Canvas canvas, float u) {
            rect.set(u * 0.18f, u * 0.27f, u * 0.78f, u * 0.68f);
            canvas.drawArc(rect, 200f, 145f, false, stroke);
            canvas.drawLine(u * 0.14f, u * 0.62f, u * 0.86f, u * 0.62f, stroke);
            canvas.drawLine(u * 0.22f, u * 0.78f, u * 0.78f, u * 0.78f, stroke);
            canvas.drawLine(u * 0.30f, u * 0.92f, u * 0.70f, u * 0.92f, stroke);
        }

        private void drawWind(Canvas canvas, float u) {
            canvas.drawLine(u * 0.14f, u * 0.36f, u * 0.68f, u * 0.36f, stroke);
            rect.set(u * 0.58f, u * 0.20f, u * 0.92f, u * 0.52f);
            canvas.drawArc(rect, -90f, 180f, false, stroke);
            canvas.drawLine(u * 0.22f, u * 0.62f, u * 0.82f, u * 0.62f, stroke);
            rect.set(u * 0.68f, u * 0.48f, u * 0.98f, u * 0.78f);
            canvas.drawArc(rect, -90f, 180f, false, stroke);
        }
    }

    private static final class ChargeBoltView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        ChargeBoltView(Context context) {
            super(context);
            paint.setColor(resolveMaterialInfoColor(context));
            paint.setStyle(Paint.Style.FILL);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        void setTint(int color) {
            paint.setColor(color);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            path.reset();
            path.moveTo(w * 0.62f, 0f);
            path.lineTo(w * 0.16f, h * 0.55f);
            path.lineTo(w * 0.48f, h * 0.55f);
            path.lineTo(w * 0.34f, h);
            path.lineTo(w * 0.84f, h * 0.40f);
            path.lineTo(w * 0.52f, h * 0.40f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private static final class RankingSnapshot {
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
                // Channel details are best-effort; fall back to the ranking override.
            }
            int importance = NotificationManagerImportance.UNKNOWN;
            try {
                importance = ranking.getImportance();
            } catch (Throwable ignored) {
                // Older framework variants may not expose all ranking fields.
            }
            int suppressedEffects = 0;
            try {
                suppressedEffects = ranking.getSuppressedVisualEffects();
            } catch (Throwable ignored) {
                // Best-effort; older framework variants may not expose all ranking fields.
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

    private static final class NotificationManagerImportance {
        static final int UNKNOWN = Integer.MIN_VALUE;
        static final int NONE = 0;
        static final int DEFAULT = 3;
    }

}
