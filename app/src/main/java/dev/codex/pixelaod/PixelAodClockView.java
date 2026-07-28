package dev.codex.pixelaod;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.hardware.display.DisplayManager;
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
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;
import android.view.Display;
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
    private static final int CLOCK_COLOR = Color.rgb(PixelAodVisualStyle.CLOCK_COLOR_RED,
            PixelAodVisualStyle.CLOCK_COLOR_GREEN, PixelAodVisualStyle.CLOCK_COLOR_BLUE);
    private static final int INFO_COLOR = Color.rgb(PixelAodVisualStyle.INFO_COLOR_RED,
            PixelAodVisualStyle.INFO_COLOR_GREEN, PixelAodVisualStyle.INFO_COLOR_BLUE);
    private static final int LARGE_CLOCK_TEXT_DP = PixelAodVisualStyle.LARGE_CLOCK_TEXT_DP;
    private static final int LARGE_CLOCK_TOP_DP = PixelAodVisualStyle.LARGE_CLOCK_TOP_DP;
    private static final int SMALL_CLOCK_TEXT_DP = PixelAodVisualStyle.SMALL_CLOCK_TEXT_DP;
    private static final int SMALL_CLOCK_TOP_DP = PixelAodVisualStyle.SMALL_CLOCK_TOP_DP;
    private static final int INFO_EDGE_DP = PixelAodVisualStyle.EDGE_DP;
    private static final int COMPACT_CLOCK_VISUAL_START_OFFSET_DP =
            PixelAodVisualStyle.COMPACT_CLOCK_VISUAL_START_OFFSET_DP;
    private static final int LARGE_INFO_TOP_DP = PixelAodVisualStyle.LARGE_INFO_TOP_DP;
    private static final int LARGE_INFO_TEXT_DP = PixelAodVisualStyle.LARGE_INFO_TEXT_DP;
    private static final int LARGE_NOTIFICATION_LINE_TOP_DP =
            PixelAodVisualStyle.NOTIFICATION_LINE_TOP_DP;
    private static final int LARGE_MEDIA_TOP_DP = PixelAodVisualStyle.Aod.LARGE_MEDIA_TOP_DP;
    private static final int LARGE_MEDIA_WITH_NOTIFICATIONS_TOP_DP =
            PixelAodVisualStyle.Aod.LARGE_MEDIA_WITH_NOTIFICATIONS_TOP_DP;
    private static final int SMALL_INFO_TOP_DP = PixelAodVisualStyle.SMALL_INFO_TOP_DP;
    private static final int COMPACT_INFO_TEXT_DP = PixelAodVisualStyle.COMPACT_INFO_TEXT_DP;
    private static final int SMALL_NOTIFICATION_LINE_TOP_DP =
            PixelAodVisualStyle.NOTIFICATION_LINE_TOP_DP;
    private static final int SMALL_MEDIA_TOP_DP = PixelAodVisualStyle.Aod.SMALL_MEDIA_TOP_DP;
    private static final int NOTIFICATION_ICON_SIZE_DP =
            PixelAodVisualStyle.Aod.NOTIFICATION_ICON_SIZE_DP;
    private static final int NOTIFICATION_ICON_SPACING_DP =
            PixelAodVisualStyle.Aod.NOTIFICATION_ICON_SPACING_DP;
    private static final int MEDIA_ICON_SIZE_DP = PixelAodVisualStyle.Aod.MEDIA_ICON_SIZE_DP;
    private static final int MEDIA_ICON_SPACING_DP =
            PixelAodVisualStyle.Aod.MEDIA_ICON_SPACING_DP;
    private static final int MEDIA_TEXT_DP = PixelAodVisualStyle.Aod.MEDIA_TEXT_DP;
    private static final int MAX_NOTIFICATION_ICONS = 5;
    private static final int ICON_MASK_SAMPLE_SIZE = 48;
    private static final int BATTERY_TOP_DP = PixelAodVisualStyle.Aod.BATTERY_TOP_DP;
    private static final int BATTERY_TEXT_DP = PixelAodVisualStyle.Aod.BATTERY_TEXT_DP;
    private static final int CHARGE_BOLT_WIDTH_DP = PixelAodVisualStyle.Aod.CHARGE_BOLT_WIDTH_DP;
    private static final int CHARGE_BOLT_HEIGHT_DP = PixelAodVisualStyle.Aod.CHARGE_BOLT_HEIGHT_DP;
    private static final float CLOCK_LINE_SPACING = PixelAodVisualStyle.CLOCK_LINE_SPACING;
    private static final float LARGE_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.LARGE_CLOCK_LETTER_SPACING;
    private static final float COMPACT_CLOCK_LETTER_SPACING =
            PixelAodVisualStyle.COMPACT_CLOCK_LETTER_SPACING;
    private static final float INFO_LETTER_SPACING = PixelAodVisualStyle.INFO_LETTER_SPACING;
    private static final float AOD_CLOCK_ALPHA = PixelAodVisualStyle.AOD_CLOCK_ALPHA;
    private static final float INFO_ALPHA = PixelAodVisualStyle.INFO_ALPHA;
    private static final float MEDIA_ALPHA = PixelAodVisualStyle.MEDIA_ALPHA;
    private static final int CLOCK_AOD_WEIGHT = PixelAodVisualStyle.Aod.CLOCK_WEIGHT;
    private static final int INFO_AOD_WEIGHT = PixelAodVisualStyle.Aod.INFO_WEIGHT;
    private static final int WEATHER_ICON_SIZE_DP = PixelAodVisualStyle.Aod.WEATHER_ICON_SIZE_DP;
    private static final int WEATHER_ICON_PADDING_DP =
            PixelAodVisualStyle.Aod.WEATHER_ICON_PADDING_DP;
    private static final int BURN_IN_OFFSET_X_DP = PixelAodVisualStyle.Aod.BURN_IN_OFFSET_X_DP;
    private static final int BURN_IN_OFFSET_Y_DP = PixelAodVisualStyle.Aod.BURN_IN_OFFSET_Y_DP;
    private static final int LOW_BATTERY_AOD_SUPPRESS_THRESHOLD_PERCENT = 15;
    private static final long AOD_ENTRY_SECOND_REFRESH_DELAY_MS = 500L;
    private static final float BURN_IN_PREVENTION_PERIOD_X_MINUTES = 83f;
    private static final float BURN_IN_PREVENTION_PERIOD_Y_MINUTES = 521f;
    private static final String GOOGLE_SANS_FLEX_VARIABLE_ASSET = "assets/fonts/GoogleSansFlex-Variable.ttf";
    private static final String GOOGLE_SANS_FLEX_VARIABLE_CACHE = "GoogleSansFlex-Variable.ttf";
    private static final String GOOGLE_SANS_FLEX_VARIABLE_CACHE_PREFIX = "GoogleSansFlex-Variable-";
    private static final int GOOGLE_SANS_FLEX_BASE_WEIGHT = 400;
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final String BREEZY_PACKAGE = "org.breezyweather";
    private static final String THEME_CUSTOMIZATION_OVERLAY_PACKAGES =
            "theme_customization_overlay_packages";
    private static final String ACTION_GADGETBRIDGE_WEATHER =
            "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER";
    private static final String ACTION_BREEZY_UPDATE_NOTIFIER =
            "org.breezyweather.ACTION_UPDATE_NOTIFIER";
    private static final boolean AT_A_GLANCE_EXTRA_ENABLED = false;
    private static final boolean ENABLE_AOD_SHADE_TREE_GUARD = false;
    private static final long AOD_ENTRY_GRACE_MILLIS = 1800L;
    private static final long AOD_ENTRY_DELAY_MILLIS = 650L;
    private static final long NON_LOCKSCREEN_AOD_REVEAL_DELAY_MS =
            AOD_ENTRY_DELAY_MILLIS + 160L;
    private static final long PANEL_HANDOFF_PRESENTATION_HOLD_MS = 520L;
    private static final long BURN_IN_SETTLE_MILLIS = 8000L;
    private static final long AOD_FORCE_DOZE_RECENT_OVERLAY_MILLIS = 15_000L;
    private static final long TRIGGER_BRIEF_AOD_DURATION_MS = 10_000L;
    private static final long NATIVE_SHORT_WAKE_TRIGGER_FRESHNESS_MS = 3_000L;
    private static final long IMPLICIT_DISPLAY_WAKE_MIN_SCREEN_OFF_AGE_MS = 5_000L;
    private static final long NOTIFICATION_PULSE_RECENT_MILLIS = 30_000L;
    private static final long WEATHER_STALE_MILLIS = 12L * 60L * 60L * 1000L;
    private static final long SCHEDULE_CACHE_MILLIS = 30_000L;
    private static final long PAUSED_MEDIA_TIMEOUT_MILLIS = 10L * 60L * 1000L;
    private static final StatusBarNotification[] EMPTY_NOTIFICATIONS = new StatusBarNotification[0];
    private static final LinkedHashMap<String, StatusBarNotification> mediaNotificationCache =
            new LinkedHashMap<>();
    private static final Set<String> expiredInactiveMediaPackages = new HashSet<>();
    private static final Set<String> loggedNativeSystemDrawableNames = new HashSet<>();
    private static StatusBarNotification[] rawNotifications = EMPTY_NOTIFICATIONS;
    private static StatusBarNotification[] activeNotifications = EMPTY_NOTIFICATIONS;
    private static Typeface cachedClockTypeface;
    private static boolean cachedClockTypefaceFromBundledFont;
    private static Typeface cachedInfoTypeface;
    private static final Map<Integer, Typeface> cachedClockTypefaceByWeight = new HashMap<>();
    private static String modulePath;
    private static Context appContext;
    private static boolean aodActive;
    private static long aodTraceSequence;
    private static String lastAodTraceId = "";
    private static String lastAodTraceSource = "";
    private static long lastAodTraceAt;
    private static String lastLoggedAodPhase = "";
    private static String lastLoggedAodPhaseTrace = "";
    private static long lastLoggedAodPhaseAt;
    private static boolean lastAodCompactClock;
    private static int lastAodClockWeight = -1;
    private static String lastNativeTriggerType = "none";
    private static String lastNativeTriggerSource = "none";
    private static String lastNativeTriggerDetail = "";
    private static long lastNativeTriggerAt;
    private static long lastExplicitWakeTriggerAt;
    private static String briefAodTriggerType = "none";
    private static String briefAodTriggerSource = "none";
    private static String briefAodTriggerDetail = "";
    private static long briefAodTriggerStartedAt;
    private static long briefAodTriggerUntilAt;
    private static String lastNotificationPulseRule = "none";
    private static String lastNotificationPulseCategory = "none";
    private static String lastNotificationPulseSource = "none";
    private static String lastNotificationPulseTrace = "none";
    private static String lastNotificationPulsePackages = "none";
    private static String lastNotificationPulsePolicy = "none";
    private static String lastNotificationPulsePolicyReason = "none";
    private static String lastNotificationPulsePolicyAction = "none";
    private static boolean lastNotificationPulsePolicyNativeCompatible;
    private static boolean lastNotificationPulsePolicyBlocked;
    private static int lastNotificationPulseRawCount = -1;
    private static int lastNotificationPulseUsableCount = -1;
    private static int lastNotificationPulseMediaCount = -1;
    private static long lastNotificationPulseAt;
    private static String lastNativeShortWakeTriggerKey = "";
    private static long lastNativeShortWakeTriggerAt;
    private static int lastObservedDisplayState = Integer.MIN_VALUE;
    private static long lastDisplayStateChangedAt;
    private static int previousObservedDisplayState = Integer.MIN_VALUE;
    private static float lastBurnInTranslationX;
    private static float lastBurnInTranslationY;
    private static long lastAodOverlayVisibleAt;
    private static long lastScreenOffAt;
    private static long lastAodActivatedAt;
    private static long nonLockscreenAodRevealBlockedUntilAt;
    /**
     * Latched at {@link #noteScreenOff}: true when this AOD session started from the
     * interactive lockscreen. Survives surface-hide clearing timing races so LS→AOD weight
     * morph is not dropped; cleared on screen-on / AOD exit.
     */
    private static boolean lastScreenOffFromInteractiveLockscreen;
    /**
     * Host showed lockscreen for a real interactive/keyguard session (not KEYGUARD flash
     * after unlock). Logs aod-2f-3df142c: aod-to-ls finished then early-aod ran with
     * screenOffAgeMs=-1 / lockscreenToAodWeight=false because marks + noteScreenOff were
     * not ready yet — this stamp keeps LS→AOD morph armed.
     */
    private static long lockscreenSessionForAodWeightAt;
    private static final PanelHandoffGate PANEL_HANDOFF_GATE =
            new PanelHandoffGate(PANEL_HANDOFF_PRESENTATION_HOLD_MS);
    private static Runnable panelHandoffCompletionRunnable;
    private static long lastCachedWeatherRequestAt;
    private static String lastNotificationSnapshotSignature = "";
    private static String lastRankingSignature = "";
    private static String lastMediaCandidatesSignature = "";
    private static String atAGlanceExtra = "";
    private static WeatherSnapshot breezyWeather = WeatherSnapshot.empty();
    private static boolean breezyWeatherReceiverRegistered;
    private static long cachedScheduleCheckedAt;
    private static boolean cachedScheduleResult = true;
    private static String cachedScheduleKey = "";
    private static final Map<String, AodNotificationPipeline.RankingSnapshot> notificationRankings =
            new HashMap<>();
    private static final Map<String, AodNotificationPipeline.LockscreenVisibilityDecision> lockscreenVisibilityDecisions =
            new HashMap<>();
    private static final ProximityAuthorityGate PROXIMITY_AUTHORITY_GATE =
            new ProximityAuthorityGate();

    static void updateProximityFromOos(boolean near, String source, String detail) {
        String normalizedSource = TextUtils.isEmpty(source) ? "unknown" : source;
        String normalizedDetail = TextUtils.isEmpty(detail) ? "" : detail;
        boolean pocketModeEnabled = appContext != null
                && PixelAodSettings.getBoolean(
                appContext, PixelAodSettings.KEY_POCKET_MODE, true);
        boolean changed = pocketModeEnabled
                ? PROXIMITY_AUTHORITY_GATE.update(
                ProximityAuthorityGate.Source.OOS_NATIVE, near)
                : PROXIMITY_AUTHORITY_GATE.reset();
        noteNativeTrigger(near ? "proximity-near" : "proximity-far",
                normalizedSource, normalizedDetail);
        if (!changed) {
            return;
        }
        if (near) {
            cancelPanelHandoffPresentation("oos-proximity-near", false);
        }
        PixelAodLog.i("OOS proximity state changed: near=" + near
                + " appliedNear=" + isProximityNear()
                + " pocketModeEnabled=" + pocketModeEnabled
                + " source=" + normalizedSource
                + " detail={" + normalizedDetail + "}");
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateAodVisibility("oos-proximity");
                }
            }
        });
    }

    private static void clearProximityState() {
        PROXIMITY_AUTHORITY_GATE.reset();
    }

    static boolean isProximityNear() {
        return PROXIMITY_AUTHORITY_GATE.isNear();
    }

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
    private TextView notificationOverflowView;
    private final LinearLayout mediaRow;
    private final ImageView mediaIconView;
    private android.animation.ValueAnimator clockWeightAnimator;
    private int currentClockWeight;
    private String currentMediaNotificationKey;
    private String lastMediaLineText = "";
    private String lastMediaLineKey = "";
    private String lastMediaIconSignature = "";
    private String lastNotificationIconSignature = "";
    private final List<MediaController> mediaControllers = new ArrayList<>();
    private final Map<MediaController, MediaController.Callback> mediaCallbacks = new HashMap<>();
    private final Map<String, Long> inactiveMediaStartedAt = new HashMap<>();
    private final Map<String, Runnable> inactiveMediaTimeoutRunnables = new HashMap<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener activeSessionsChangedListener =
            this::updateMediaControllers;
    private final BroadcastReceiver screenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : "";
            PixelAodLog.log("Pixel AOD screen-state receiver action=" + action
                    + " visibility=" + getVisibility()
                    + " interactive=" + isDeviceInteractive(context));
            logAodPhaseIfChanged(context, "screen-state#" + action);
            if (intent != null && Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                if (getVisibility() == View.VISIBLE
                        || shouldApplyModuleAodNow(context, "screen-on#transition")) {
                    PixelLockscreenClockView.prepareAodToLockscreenTransition("screen-on");
                }
                hideAllAodOverlays("screen-on");
                PixelAodHook.suppressSystemAodDuringLockscreenTransition("screen-on");
            } else if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                boolean fromLockscreen =
                        PixelLockscreenClockView.wasRecentlyInteractiveLockscreenVisibleForAodEntry();
                noteScreenOff("screen-off", fromLockscreen);
                scheduleAodVisibilityUpdate("screen-off", 160L);
                if (!fromLockscreen) {
                    scheduleAodVisibilityUpdate("screen-off-non-lockscreen-reveal",
                            NON_LOCKSCREEN_AOD_REVEAL_DELAY_MS + 40L);
                }
            }
            updateAodVisibility("screen-state");
            if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                PixelAodHook.reassertStockAodSuppressionAfterScreenOff("screen-off");
            }
        }
    };
    private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent != null ? intent.getAction() : "";
            PixelAodLog.log("Pixel AOD time receiver action=" + action
                    + " trace=" + currentAodTraceId()
                    + " state={" + describeAodState(context) + "}");
            refreshForNativeAodTick("time-broadcast");
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
    /**
     * Once LS→AOD weight morph has finished at AOD weight, re-presents must not
     * {@code prepare...fromWeight=lockscreen} (that snaps 160→340). Cleared when leaving AOD.
     */
    private boolean aodWeightHandoffSettled;
    private boolean screenStateReceiverRegistered;
    private boolean notificationSettingsObserverRegistered;
    private boolean timeReceiverRegistered;
    private boolean clockPluginManaged;

    public PixelAodClockView(Context context) {
        super(context);
        Context applicationContext = context.getApplicationContext();
        appContext = applicationContext != null ? applicationContext : context;
        ensureBreezyWeatherReceiver(appContext);
        setClipChildren(false);
        setClipToPadding(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(View.GONE);
        setAccessibilityDelegate(new AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setVisibleToUser(false);
            }
        });

        Typeface infoTypeface = resolveInfoTypeface(context);

        dateView = makeInfoLine(context, infoTypeface, INFO_AOD_WEIGHT, LARGE_INFO_TEXT_DP,
                Gravity.START);
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
        notificationIconRow.setAlpha(INFO_ALPHA);
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
        mediaRow.setAlpha(MEDIA_ALPHA);

        mediaIconView = new ImageView(context);
        mediaIconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mediaIconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams mediaGlyphParams = new LinearLayout.LayoutParams(
                dp(MEDIA_ICON_SIZE_DP), dp(MEDIA_ICON_SIZE_DP));
        mediaRow.addView(mediaIconView, mediaGlyphParams);

        mediaView = makeInfoLine(context, infoTypeface, INFO_AOD_WEIGHT, MEDIA_TEXT_DP,
                Gravity.START);
        mediaView.setAlpha(MEDIA_ALPHA);
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

        clockView = makeClock(context);
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
        batteryRow.setAlpha(INFO_ALPHA);

        batteryView = makeInfoLine(context, infoTypeface, INFO_AOD_WEIGHT, BATTERY_TEXT_DP,
                Gravity.CENTER);
        batteryRow.addView(batteryView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        chargeBoltView = new ChargeBoltView(context);
        LinearLayout.LayoutParams chargeParams = new LinearLayout.LayoutParams(
                dp(CHARGE_BOLT_WIDTH_DP), dp(CHARGE_BOLT_HEIGHT_DP));
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
            cachedClockTypefaceFromBundledFont = false;
            cachedInfoTypeface = null;
            cachedClockTypefaceByWeight.clear();
        }
    }

    /**
     * Resolves the file-backed font before a ClockPlugin view is constructed.  OOS can draw a
     * newly attached TextView before a later style pass reaches it, so the initial Typeface must
     * already be the same Google Sans Flex family used by the weighted animation frames.
     */
    static void prewarmGoogleSansFlex(Context context) {
        if (context == null) {
            return;
        }
        File fontFile = googleSansFlexFontFile(context);
        if (fontFile == null) {
            PixelAodLog.i("Google Sans Flex prewarm unavailable; no bundled font file resolved");
            return;
        }
        int lockscreenWeight = lockscreenClockWeight(context);
        int aodWeight = aodClockWeight(context);
        Typeface baseTypeface = resolveClockTypeface(context);
        Typeface lockscreenTypeface = resolveClockTypeface(context, lockscreenWeight);
        Typeface aodTypeface = resolveClockTypeface(context, aodWeight);
        PixelAodLog.i("prepared Google Sans Flex before clock host creation path="
                + fontFile.getAbsolutePath()
                + " bytes=" + fontFile.length()
                + " base=" + (baseTypeface != null)
                + " bundledBase=" + cachedClockTypefaceFromBundledFont
                + " strategy=" + ClockTypefaceResolutionPolicy.strategyName(
                        cachedClockTypefaceFromBundledFont)
                + " lockscreenWeight=" + lockscreenWeight
                + " lockscreen=" + (lockscreenTypeface != null)
                + " aodWeight=" + aodWeight
                + " aod=" + (aodTypeface != null));
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
        setActiveNotifications(notifications, "setActiveNotifications");
    }

    static void setActiveNotifications(StatusBarNotification[] notifications, String source) {
        int rawCount = notifications == null ? 0 : notifications.length;
        int usableCount;
        int mediaCandidateCount;
        String packageSummary;
        String signature;
        synchronized (PixelAodClockView.class) {
            rawNotifications = notifications != null ? notifications.clone() : EMPTY_NOTIFICATIONS;
            retainLockscreenVisibilityDecisionsLocked(rawNotifications);
            activeNotifications = AodNotificationPipeline.sanitizeNotifications(
                    appContext,
                    notifications,
                    notificationRankings,
                    lockscreenVisibilityDecisions,
                    isLockscreenPolicyEnabled(),
                    currentAodTraceId());
            mediaCandidateCount = replaceMediaNotificationCandidatesLocked(rawNotifications);
            signature = AodNotificationPipeline.notificationSignature(rawNotifications) + "|"
                    + AodNotificationPipeline.notificationSignature(activeNotifications) + "|media="
                    + mediaCandidatesSignatureLocked();
            if (TextUtils.equals(lastNotificationSnapshotSignature, signature)) {
                return;
            }
            lastNotificationSnapshotSignature = signature;
            usableCount = activeNotifications.length;
            packageSummary = AodNotificationPipeline.describePackages(activeNotifications);
            String trace = currentAodTraceId();
            AodLifecycleState pulseState = currentAodLifecycleState(appContext);
            OosAodLifecycleAdapter.ModulePolicy pulseModulePolicy =
                    evaluateModuleAodPolicy(appContext,
                            source + "#notification-pulse-policy", trace, pulseState);
            OosAodLifecycleAdapter.NotificationPulseObservation pulseObservation =
                    OosAodLifecycleAdapter.evaluateNotificationPulseObservation(
                            source, rawCount, usableCount, -1,
                            pulseModulePolicy, isProximityNear());
            if (pulseObservation.isPulseCandidate()) {
                markNotificationPulseCandidateLocked(pulseObservation, source, trace,
                        packageSummary, rawCount, usableCount, mediaCandidateCount);
            }
            String state = describeAodState(appContext);
            PixelAodLog.log("updated native AOD notification snapshot raw="
                    + rawCount
                    + " usable=" + usableCount
                    + " media=" + mediaCandidateCount
                    + " packages=" + packageSummary
                    + " trace=" + trace
                    + " state={" + state + "}");
            PixelAodLog.log("AOD notification snapshot direct raw="
                    + rawCount
                    + " usable=" + usableCount
                    + " trace=" + trace
                    + " state={" + state + "}");
            OosAodLifecycleAdapter.recordNotificationPulseObservation(
                    source,
                    rawCount,
                    usableCount,
                    mediaCandidateCount,
                    -1,
                    packageSummary,
                    trace,
                    state,
                    pulseModulePolicy,
                    isProximityNear());
        }
        refreshInstancesFromNotificationSnapshot(source);
        PixelLockscreenClockView.setActiveNotifications(activeNotifications);
    }

    private static void markNotificationPulseCandidateLocked(
            OosAodLifecycleAdapter.NotificationPulseObservation observation,
            String source, String trace, String packageSummary, int rawCount,
            int usableCount, int mediaCandidateCount) {
        lastNotificationPulseRule = observation.ruleLabel;
        lastNotificationPulseCategory = observation.categoryLabel;
        lastNotificationPulseSource = TextUtils.isEmpty(source) ? "unknown" : source;
        lastNotificationPulseTrace = TextUtils.isEmpty(trace) ? "none" : trace;
        lastNotificationPulsePackages = TextUtils.isEmpty(packageSummary)
                ? "none" : packageSummary;
        lastNotificationPulsePolicy = TextUtils.isEmpty(observation.policyLabel)
                ? "none" : observation.policyLabel;
        lastNotificationPulsePolicyReason = TextUtils.isEmpty(observation.policyReason)
                ? "none" : observation.policyReason;
        lastNotificationPulsePolicyAction = TextUtils.isEmpty(observation.policyAction)
                ? "none" : observation.policyAction;
        lastNotificationPulsePolicyNativeCompatible = observation.policyNativePulseCompatible;
        lastNotificationPulsePolicyBlocked = observation.policyBlocked;
        lastNotificationPulseRawCount = rawCount;
        lastNotificationPulseUsableCount = usableCount;
        lastNotificationPulseMediaCount = mediaCandidateCount;
        lastNotificationPulseAt = SystemClock.uptimeMillis();
    }

    static void refreshNotificationFiltering(String source) {
        StatusBarNotification[] rawSnapshot;
        synchronized (PixelAodClockView.class) {
            rawSnapshot = rawNotifications;
        }
        PixelAodLog.log("refreshing AOD notification filtering trace=" + currentAodTraceId()
                + " source=" + source + " state={" + describeAodState(appContext) + "}");
        setActiveNotifications(rawSnapshot, source);
    }

    static void updateLockscreenVisibilityFromProvider(StatusBarNotification sbn,
            boolean hidden, String source) {
        updateLockscreenVisibilityDecision(sbn, hidden, false, source);
    }

    static void updateLockscreenVisibilityFromFilter(StatusBarNotification sbn,
            boolean hidden, String source) {
        updateLockscreenVisibilityDecision(sbn, hidden, true, source);
    }

    private static void updateLockscreenVisibilityDecision(StatusBarNotification sbn,
            boolean hidden, boolean fromFilter, String source) {
        if (sbn == null || TextUtils.isEmpty(sbn.getKey())) {
            return;
        }
        AodNotificationPipeline.LockscreenVisibilityDecision next;
        boolean changed;
        synchronized (PixelAodClockView.class) {
            AodNotificationPipeline.LockscreenVisibilityDecision current =
                    lockscreenVisibilityDecisions.get(sbn.getKey());
            next = current != null
                    ? new AodNotificationPipeline.LockscreenVisibilityDecision(current)
                    : new AodNotificationPipeline.LockscreenVisibilityDecision();
            changed = fromFilter
                    ? next.setFilterHidden(hidden, source)
                    : next.setProviderHidden(hidden, source);
            if (!changed) {
                return;
            }
            lockscreenVisibilityDecisions.put(sbn.getKey(), next);
        }
        PixelAodLog.log("updated lockscreen visibility decision pkg=" + sbn.getPackageName()
                + " key=" + sbn.getKey()
                + " source=" + source
                + " stage=" + (fromFilter ? "filter" : "provider")
                + " hidden=" + hidden
                + " decision=" + next
                + " trace=" + currentAodTraceId());
        refreshNotificationFiltering("lockscreen-visibility-" + (fromFilter ? "filter" : "provider"));
    }

    private static void retainLockscreenVisibilityDecisionsLocked(
            StatusBarNotification[] notifications) {
        if (lockscreenVisibilityDecisions.isEmpty()) {
            return;
        }
        HashSet<String> activeKeys = new HashSet<>();
        if (notifications != null) {
            for (StatusBarNotification sbn : notifications) {
                if (sbn != null && !TextUtils.isEmpty(sbn.getKey())) {
                    activeKeys.add(sbn.getKey());
                }
            }
        }
        lockscreenVisibilityDecisions.keySet().retainAll(activeKeys);
    }

    static void setAodActive(boolean active, String source) {
        boolean requestedActive = active;
        boolean continuousAodPolicyAllows = !requestedActive
                || isContinuousAodPolicyAllowingDisplay(appContext, source + "#set-active");
        boolean changed;
        boolean delayedNonLockscreenReveal = false;
        long revealBlockedUntil = 0L;
        long now = android.os.SystemClock.uptimeMillis();
        boolean fromInteractiveLockscreen =
                PixelLockscreenClockView.wasRecentlyInteractiveLockscreenVisibleForAodEntry();
        synchronized (PixelAodClockView.class) {
            if (requestedActive && !continuousAodPolicyAllows) {
                active = false;
                lastAodOverlayVisibleAt = 0L;
            }
            changed = aodActive != active;
            aodActive = active;
            if (active) {
                if (changed) {
                    lastAodActivatedAt = now;
                    if (!fromInteractiveLockscreen) {
                        if (lastScreenOffAt <= 0L
                                || !isAllowedAodEntryAge(now, lastScreenOffAt)) {
                            lastScreenOffAt = now;
                        }
                        revealBlockedUntil = now + NON_LOCKSCREEN_AOD_REVEAL_DELAY_MS;
                        nonLockscreenAodRevealBlockedUntilAt = revealBlockedUntil;
                        lastAodOverlayVisibleAt = 0L;
                        delayedNonLockscreenReveal = true;
                    } else {
                        nonLockscreenAodRevealBlockedUntilAt = 0L;
                    }
                }
            } else {
                lastAodActivatedAt = 0L;
                nonLockscreenAodRevealBlockedUntilAt = 0L;
                if (!requestedActive) {
                    clearBriefAodTriggerLocked();
                }
            }
        }
        if (changed) {
            startAodTrace(source);
        }
        if (active && changed && fromInteractiveLockscreen) {
            ClockPluginHostController.noteLockscreenToAodHandoff(source);
        }
        if (active) {
            if (delayedNonLockscreenReveal) {
                PixelAodLog.log("delayed non-lockscreen Pixel AOD reveal trace="
                        + currentAodTraceId()
                        + " source=" + source
                        + " revealDelayMs=" + Math.max(0L, revealBlockedUntil - now)
                        + " recentLockscreen={"
                        + PixelLockscreenClockView.describeRecentInteractiveLockscreenForAodEntry()
                        + "}"
                        + " state={" + describeAodState(appContext) + "}");
            } else {
                markRecentAodOverlayVisible(source + "#active");
            }
        } else {
            if (changed) {
                clearProximityState();
            }
        }
        if (!changed && active) {
            return;
        }
        final boolean finalActive = active;
        final boolean finalDelayedNonLockscreenReveal = delayedNonLockscreenReveal;
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateAodVisibility(source);
                    if (finalActive) {
                        view.refreshActiveMediaControllers();
                        if (!finalDelayedNonLockscreenReveal) {
                            view.requestAodFrameRefresh(source + "#active");
                        }
                    }
                }
            }
            if (finalActive) {
                PixelAodHook.refreshKnownAodHostVisibility(source + "#active");
            }
            ClockPluginHostController.refreshAll(source + "#set-aod-active");
        });
        if (delayedNonLockscreenReveal) {
            long delayMs = Math.max(0L,
                    revealBlockedUntil - android.os.SystemClock.uptimeMillis()) + 40L;
            scheduleAodVisibilityUpdateForAll(source + "#non-lockscreen-reveal", delayMs);
        }
        PixelAodLog.log("Pixel AOD active=" + active + " changed=" + changed
                + " source=" + source
                + " requestedActive=" + requestedActive
                + " continuousAodPolicyAllows=" + continuousAodPolicyAllows
                + " delayedNonLockscreenReveal=" + delayedNonLockscreenReveal
                + " fromInteractiveLockscreen=" + fromInteractiveLockscreen
                + " recentLockscreen={"
                + PixelLockscreenClockView.describeRecentInteractiveLockscreenForAodEntry()
                + "}"
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
        logAodPhaseIfChanged(appContext, source + "#setAodActive");
    }

    static boolean isAodActive() {
        synchronized (PixelAodClockView.class) {
            return aodActive;
        }
    }

    static String describeAodState(Context context) {
        return describeAodState(context, lastAodCompactClock, lastAodClockWeight);
    }

    static String describeAodState(Context context, boolean compact, int weight) {
        return currentAodLifecycleState(context).describe(compact, weight);
    }

    static void logAodPhaseIfChanged(Context context, String source) {
        Context ctx = context != null ? context : appContext;
        if (ctx == null) {
            return;
        }
        AodLifecycleState state = currentAodLifecycleState(ctx);
        String phase = state.phase();
        String previousPhase;
        String previousTrace;
        long previousAt;
        boolean compact;
        int weight;
        synchronized (PixelAodClockView.class) {
            if (TextUtils.equals(lastLoggedAodPhase, phase)) {
                return;
            }
            previousPhase = lastLoggedAodPhase;
            previousTrace = lastLoggedAodPhaseTrace;
            previousAt = lastLoggedAodPhaseAt;
            lastLoggedAodPhase = phase;
            lastLoggedAodPhaseTrace = state.traceId;
            lastLoggedAodPhaseAt = state.now;
            compact = lastAodCompactClock;
            weight = lastAodClockWeight;
        }
        PixelAodLog.log("AOD lifecycle phase changed source=" + source
                + " from=" + (TextUtils.isEmpty(previousPhase) ? "none" : previousPhase)
                + " to=" + phase
                + " previousTrace=" + (TextUtils.isEmpty(previousTrace) ? "none" : previousTrace)
                + " trace=" + state.traceId
                + " sinceLastMs=" + ageSince(state.now, previousAt)
                + " state={" + state.describe(compact, weight) + "}");
        OosAodLifecycleAdapter.recordPhaseChange(source, previousPhase, phase,
                state.traceId, state.describe(compact, weight));
    }

    static String currentAodTraceId() {
        synchronized (PixelAodClockView.class) {
            if (TextUtils.isEmpty(lastAodTraceId)) {
                startAodTraceLocked("auto");
            }
            return lastAodTraceId;
        }
    }

    static String peekAodTraceId() {
        synchronized (PixelAodClockView.class) {
            return lastAodTraceId;
        }
    }

    static String currentAodTraceSource() {
        synchronized (PixelAodClockView.class) {
            if (TextUtils.isEmpty(lastAodTraceId)) {
                startAodTraceLocked("auto");
            }
            return lastAodTraceSource;
        }
    }

    private static String startAodTrace(String source) {
        cancelPanelHandoffPresentation(source + "#trace-replaced");
        synchronized (PixelAodClockView.class) {
            return startAodTraceLocked(source);
        }
    }

    private static String ensureAodTrace(String source) {
        synchronized (PixelAodClockView.class) {
            if (TextUtils.isEmpty(lastAodTraceId)) {
                return startAodTraceLocked(source);
            }
            return lastAodTraceId;
        }
    }

    private static String startAodTraceLocked(String source) {
        long now = SystemClock.uptimeMillis();
        aodTraceSequence++;
        lastAodTraceId = "aod-" + Long.toHexString(aodTraceSequence)
                + "-" + Long.toHexString(now);
        lastAodTraceSource = source != null ? source : "";
        lastAodTraceAt = now;
        lastLoggedAodPhase = "";
        lastLoggedAodPhaseTrace = "";
        lastLoggedAodPhaseAt = 0L;
        return lastAodTraceId;
    }

    private static void setAodPresentationState(boolean compact, int weight) {
        synchronized (PixelAodClockView.class) {
            lastAodCompactClock = compact;
            lastAodClockWeight = weight;
        }
    }

    static void noteNativeTrigger(String type, String source, String detail) {
        long now = SystemClock.uptimeMillis();
        String normalizedType = TextUtils.isEmpty(type) ? "unknown" : type;
        String normalizedSource = TextUtils.isEmpty(source) ? "unknown" : source;
        String normalizedDetail = TextUtils.isEmpty(detail) ? "" : detail;
        OosAodLifecycleAdapter.TriggerBehavior behavior =
                OosAodLifecycleAdapter.behaviorForTrigger(
                        normalizedType, normalizedSource, normalizedDetail);
        String trace;
        String stateDescription;
        synchronized (PixelAodClockView.class) {
            if (TextUtils.isEmpty(lastAodTraceId)) {
                startAodTraceLocked(normalizedSource);
            }
            lastNativeTriggerType = normalizedType;
            lastNativeTriggerSource = normalizedSource;
            lastNativeTriggerDetail = normalizedDetail;
            lastNativeTriggerAt = now;
            if (behavior != null && behavior.startsBriefDisplay) {
                lastExplicitWakeTriggerAt = now;
            }
            trace = lastAodTraceId;
        }
        boolean behaviorApplied = applyNativeTriggerBehavior(behavior,
                normalizedType, normalizedSource, normalizedDetail, now);
        stateDescription = describeAodState(appContext);
        PixelAodLog.log("AOD native trigger type=" + normalizedType
                + " source=" + normalizedSource
                + " detail={" + normalizedDetail + "}"
                + " rule=" + behavior.ruleLabel
                + " category=" + behavior.categoryLabel
                + " displayMode=" + behavior.displayModeLabel
                + " futureAction=" + behavior.futureAction
                + " behaviorApplied=" + behaviorApplied
                + " trace=" + trace
                + " state={" + stateDescription + "}");
        OosAodLifecycleAdapter.recordTriggerEvent(normalizedType, normalizedSource,
                normalizedDetail, behaviorApplied, trace, stateDescription);
    }

    static long currentAodTraceAgeMillis() {
        long now = SystemClock.uptimeMillis();
        synchronized (PixelAodClockView.class) {
            return lastAodTraceAt > 0L && now >= lastAodTraceAt
                    ? now - lastAodTraceAt : -1L;
        }
    }

    static long recentExplicitWakeTriggerAgeMillis() {
        long now = SystemClock.uptimeMillis();
        synchronized (PixelAodClockView.class) {
            return lastExplicitWakeTriggerAt > 0L && now >= lastExplicitWakeTriggerAt
                    ? now - lastExplicitWakeTriggerAt : -1L;
        }
    }

    private static boolean applyNativeTriggerBehavior(
            OosAodLifecycleAdapter.TriggerBehavior behavior, String type,
            String source, String detail, long now) {
        if (behavior == null) {
            return false;
        }
        if (behavior.startsBriefDisplay) {
            return startBriefAodTrigger(type, source, detail, now);
        }
        if (behavior.blocksDisplay) {
            return cancelBriefAodTrigger(source, behavior.displayModeLabel);
        }
        if (behavior.releasesDisplayGuard) {
            PixelAodLog.log("released trigger-only Pixel AOD sensor guard"
                    + " source=" + source
                    + " trace=" + currentAodTraceId()
                    + " state={" + describeAodState(appContext) + "}");
        }
        return false;
    }

    private static boolean startBriefAodTrigger(
            String type, String source, String detail, long now) {
        Context context = appContext;
        String trace = currentAodTraceId();
        if (context == null) {
            PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                    + " source=" + source
                    + " reason=no-context"
                    + " trace=" + trace
                    + " state={" + describeAodState(null) + "}");
            return false;
        }
        String displayMode = aodDisplayMode(context);
        if (!isModuleEnabled(context) || !isTriggerBriefAllowedByMode(displayMode)) {
            PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                    + " source=" + source
                    + " reason=display-mode"
                    + " displayMode=" + displayMode
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return false;
        }
        if (isDeviceInteractive(context)) {
            PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                    + " source=" + source
                    + " reason=interactive"
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return false;
        }
        if (isGenericOplusWakeCallback(source)
                && isInitialScreenOffWindow(now)) {
            PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                    + " source=" + source
                    + " reason=initial-screen-off-generic-wake"
                    + " displayMode=" + displayMode
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return false;
        }
        if (isProximityNear()) {
            PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                    + " source=" + source
                    + " reason=proximity-near"
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return false;
        }
        if (!isPowerPolicyAllowingAod(context, source + "#trigger-brief", trace, false)) {
            return false;
        }
        long until = now + TRIGGER_BRIEF_AOD_DURATION_MS;
        synchronized (PixelAodClockView.class) {
            briefAodTriggerType = type;
            briefAodTriggerSource = source;
            briefAodTriggerDetail = detail;
            briefAodTriggerStartedAt = now;
            briefAodTriggerUntilAt = until;
        }
        PixelAodLog.log("started trigger-only Pixel AOD brief display"
                + " source=" + source
                + " type=" + type
                + " detail={" + detail + "}"
                + " displayMode=" + displayMode
                + " durationMs=" + TRIGGER_BRIEF_AOD_DURATION_MS
                + " untilAgeMs=" + (until - now)
                + " trace=" + trace
                + " state={" + describeAodState(context) + "}");
        refreshAodPolicyConsumers(source + "#trigger-brief-start");
        scheduleBriefAodTriggerExpiry(source, until);
        return true;
    }

    private static boolean isGenericOplusWakeCallback(String source) {
        return !TextUtils.isEmpty(source) && source.contains("notifyWakeUpCallback");
    }

    private static boolean isInitialScreenOffWindow(long now) {
        long screenOffAt;
        synchronized (PixelAodClockView.class) {
            screenOffAt = lastScreenOffAt;
        }
        return isRecentUptime(now, screenOffAt, IMPLICIT_DISPLAY_WAKE_MIN_SCREEN_OFF_AGE_MS);
    }

    private static boolean cancelBriefAodTrigger(String source, String reason) {
        boolean hadBriefTrigger;
        synchronized (PixelAodClockView.class) {
            hadBriefTrigger = clearBriefAodTriggerLocked();
        }
        PixelAodLog.log("blocked trigger-only Pixel AOD brief display"
                + " source=" + source
                + " reason=" + reason
                + " hadBriefTrigger=" + hadBriefTrigger
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
        if (hadBriefTrigger) {
            if (!isAodActive()) {
                clearProximityState();
            }
            refreshAodPolicyConsumers(source + "#trigger-brief-blocked");
        }
        return hadBriefTrigger;
    }

    private static void scheduleBriefAodTriggerExpiry(String source, long expectedUntilAt) {
        long delayMillis = Math.max(0L,
                expectedUntilAt - SystemClock.uptimeMillis() + 50L);
        mainHandler().postDelayed(() -> expireBriefAodTrigger(source, expectedUntilAt),
                delayMillis);
    }

    private static void expireBriefAodTrigger(String source, long expectedUntilAt) {
        boolean expired = false;
        long now = SystemClock.uptimeMillis();
        boolean keepContinuousAfterBrief = appContext != null
                && isContinuousAodAllowedByMode(aodDisplayMode(appContext))
                && isWithinAodSchedule(appContext);
        synchronized (PixelAodClockView.class) {
            if (briefAodTriggerUntilAt == expectedUntilAt
                    && briefAodTriggerUntilAt > 0L
                    && now >= briefAodTriggerUntilAt) {
                expired = clearBriefAodTriggerLocked();
                if (!keepContinuousAfterBrief) {
                    aodActive = false;
                    lastAodActivatedAt = 0L;
                }
            }
        }
        if (!expired) {
            return;
        }
        PixelAodLog.log("expired trigger-only Pixel AOD brief display"
                + " source=" + source
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
        if (!isAodActive()) {
            clearProximityState();
        }
        refreshAodPolicyConsumers(source + "#trigger-brief-expired");
    }

    private static boolean clearBriefAodTriggerLocked() {
        boolean hadBriefTrigger = briefAodTriggerStartedAt > 0L
                || briefAodTriggerUntilAt > 0L
                || !"none".equals(briefAodTriggerType);
        briefAodTriggerType = "none";
        briefAodTriggerSource = "none";
        briefAodTriggerDetail = "";
        briefAodTriggerStartedAt = 0L;
        briefAodTriggerUntilAt = 0L;
        return hadBriefTrigger;
    }

    private static void refreshAodPolicyConsumers(String source) {
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateAodVisibility(source);
                    view.requestAodFrameRefresh(source);
                }
            }
            PixelAodHook.refreshKnownAodHostVisibility(source);
            ClockPluginHostController.refreshAll(source + "#policy");
        });
    }

    static void refreshAodPolicyFromSettings(String source) {
        Context context = appContext;
        if (context != null && !isModuleEnabled(context)) {
            hideAllAodOverlays(source + "#module-disabled");
            return;
        }
        refreshAodPolicyConsumers(source);
    }

    static void hideAllAodOverlays(String source) {
        cancelPanelHandoffPresentation(source + "#aod-exit");
        synchronized (PixelAodClockView.class) {
            aodActive = false;
            lastScreenOffAt = 0L;
            lastAodActivatedAt = 0L;
            nonLockscreenAodRevealBlockedUntilAt = 0L;
            lastScreenOffFromInteractiveLockscreen = false;
            // Keep lockscreenSessionForAodWeightAt across brief AOD hide only when waking to
            // lockscreen; unlock path clears it explicitly via clearLockscreenSessionForAodWeight.
            clearBriefAodTriggerLocked();
        }
        startAodTrace(source);
        logAodPhaseIfChanged(appContext, source + "#hideAllAodOverlays");
        Runnable task = () -> {
            int hidden = 0;
            int managed = 0;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    if (view.isClockPluginManaged()) {
                        managed++;
                        continue;
                    }
                    if (view.getVisibility() != View.GONE) {
                        view.setVisibility(View.GONE);
                        hidden++;
                    }
                    view.setAlpha(1f);
                    view.resetBurnInTranslation();
                    view.stop();
                }
            }
            PixelAodLog.log("hid Pixel AOD overlays trace=" + currentAodTraceId()
                    + " source=" + source + " count=" + hidden
                    + " managedSkipped=" + managed
                    + " state={" + describeAodState(appContext) + "}");
            ClockPluginHostController.refreshAll(source + "#hide-legacy-overlays");
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().postAtFrontOfQueue(task);
        }
    }

    static void noteScreenOff(String source) {
        noteScreenOff(source,
                PixelLockscreenClockView.wasRecentlyInteractiveLockscreenVisibleForAodEntry());
    }

    static void noteScreenOff(String source, boolean fromLockscreenSurface) {
        long now = android.os.SystemClock.uptimeMillis();
        long revealBlockedUntil = fromLockscreenSurface
                ? 0L
                : now + NON_LOCKSCREEN_AOD_REVEAL_DELAY_MS;
        synchronized (PixelAodClockView.class) {
            lastScreenOffAt = now;
            lastScreenOffFromInteractiveLockscreen = fromLockscreenSurface;
            nonLockscreenAodRevealBlockedUntilAt = revealBlockedUntil;
            if (!fromLockscreenSurface) {
                lastAodOverlayVisibleAt = 0L;
            }
        }
        startAodTrace(source);
        PixelAodLog.log("noted Pixel AOD screen-off trace=" + currentAodTraceId()
                + " source=" + source
                + " fromInteractiveLockscreen=" + fromLockscreenSurface
                + " revealDelayMs=" + (revealBlockedUntil > now ? revealBlockedUntil - now : 0L)
                + " recentLockscreen={"
                + PixelLockscreenClockView.describeRecentInteractiveLockscreenForAodEntry()
                + "}"
                + " state={" + describeAodState(appContext) + "}");
        logAodPhaseIfChanged(appContext, source + "#noteScreenOff");
    }

    static void noteScreenOffIfUnset(String source) {
        boolean noted = false;
        boolean fromLockscreenSurface =
                PixelLockscreenClockView.wasRecentlyInteractiveLockscreenVisibleForAodEntry();
        long now = android.os.SystemClock.uptimeMillis();
        long revealBlockedUntil = fromLockscreenSurface
                ? 0L
                : now + NON_LOCKSCREEN_AOD_REVEAL_DELAY_MS;
        synchronized (PixelAodClockView.class) {
            if (lastScreenOffAt <= 0L) {
                lastScreenOffAt = now;
                lastScreenOffFromInteractiveLockscreen = fromLockscreenSurface;
                nonLockscreenAodRevealBlockedUntilAt = revealBlockedUntil;
                if (!fromLockscreenSurface) {
                    lastAodOverlayVisibleAt = 0L;
                }
                noted = true;
            }
        }
        if (noted) {
            startAodTrace(source);
            PixelAodLog.log("seeded Pixel AOD screen-off trace=" + currentAodTraceId()
                    + " source=" + source
                    + " fromInteractiveLockscreen=" + fromLockscreenSurface
                    + " revealDelayMs=" + (revealBlockedUntil > now ? revealBlockedUntil - now : 0L)
                    + " recentLockscreen={"
                    + PixelLockscreenClockView.describeRecentInteractiveLockscreenForAodEntry()
                    + "}"
                    + " state={" + describeAodState(appContext) + "}");
            logAodPhaseIfChanged(appContext, source + "#noteScreenOffIfUnset");
        }
    }

    /** Whether this AOD session's screen-off was from the interactive lockscreen. */
    static boolean wasScreenOffFromInteractiveLockscreen() {
        synchronized (PixelAodClockView.class) {
            return lastScreenOffFromInteractiveLockscreen && lastScreenOffAt > 0L;
        }
    }

    /**
     * Mark that the user is in a real lockscreen clock session (interactive keyguard or
     * finished aod-to-ls restore). Arms LS→AOD weight morph even before noteScreenOff.
     */
    static void noteLockscreenSessionForAodWeight(String source) {
        long now = android.os.SystemClock.uptimeMillis();
        boolean log;
        synchronized (PixelAodClockView.class) {
            log = lockscreenSessionForAodWeightAt <= 0L
                    || now - lockscreenSessionForAodWeightAt > 700L;
            lockscreenSessionForAodWeightAt = now;
        }
        if (log) {
            PixelAodLog.log("noted lockscreen session for AOD weight source=" + source
                    + " trace=" + currentAodTraceId());
        }
    }

    /** Clear on unlock / leave keyguard so non-LS screen-off does not morph. */
    static void clearLockscreenSessionForAodWeight(String source) {
        boolean had;
        synchronized (PixelAodClockView.class) {
            had = lockscreenSessionForAodWeightAt > 0L;
            lockscreenSessionForAodWeightAt = 0L;
        }
        if (had) {
            PixelAodLog.log("cleared lockscreen session for AOD weight source=" + source
                    + " trace=" + currentAodTraceId());
        }
    }

    /**
     * LS→AOD weight morph / early-aod-weight gate.
     * <ul>
     *   <li>False when noteScreenOff already latched a non-lockscreen origin (unlock→app).</li>
     *   <li>True for recent interactive LS marks, LS screen-off latch, or an armed
     *       lockscreen session (aod-to-ls / interactive presentLockscreen).</li>
     * </ul>
     */
    static boolean shouldAnimateLockscreenToAodWeight() {
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (PixelAodClockView.class) {
            // Positive non-LS evidence from noteScreenOff wins (keeps 0.1.256 fix).
            if (lastScreenOffAt > 0L && !lastScreenOffFromInteractiveLockscreen) {
                long offAge = now - lastScreenOffAt;
                if (offAge >= 0L && offAge <= 5_000L) {
                    return false;
                }
            }
            if (lastScreenOffFromInteractiveLockscreen && lastScreenOffAt > 0L) {
                long offAge = now - lastScreenOffAt;
                if (offAge >= 0L && offAge <= 5_000L) {
                    return true;
                }
            }
            if (lockscreenSessionForAodWeightAt > 0L) {
                long sessionAge = now - lockscreenSessionForAodWeightAt;
                // Cover quick re-sleep after aod-to-ls (logs: ~640ms) and longer LS stare.
                if (sessionAge >= 0L && sessionAge <= 30_000L) {
                    return true;
                }
            }
        }
        return PixelLockscreenClockView.wasRecentlyInteractiveLockscreenVisibleForAodEntry();
    }

    static BurnInOffset currentBurnInOffset() {
        synchronized (PixelAodClockView.class) {
            return new BurnInOffset(lastBurnInTranslationX, lastBurnInTranslationY);
        }
    }

    static BurnInOffset consumeRecentBurnInOffset(long windowMillis) {
        synchronized (PixelAodClockView.class) {
            long age = android.os.SystemClock.uptimeMillis() - lastAodOverlayVisibleAt;
            if (lastAodOverlayVisibleAt <= 0L || age < 0L || age > windowMillis) {
                return null;
            }
            lastAodOverlayVisibleAt = 0L;
            return new BurnInOffset(lastBurnInTranslationX, lastBurnInTranslationY);
        }
    }

    static void markRecentAodOverlayVisible(String source) {
        float x;
        float y;
        synchronized (PixelAodClockView.class) {
            lastAodOverlayVisibleAt = android.os.SystemClock.uptimeMillis();
            x = lastBurnInTranslationX;
            y = lastBurnInTranslationY;
        }
        ensureAodTrace(source);
        PixelAodLog.log("marked recent Pixel AOD visible trace=" + currentAodTraceId()
                + " source=" + source
                + " x=" + Math.round(x) + " y=" + Math.round(y)
                + " state={" + describeAodState(appContext) + "}");
    }

    static boolean shouldCustomizeAodNow(Context context) {
        return evaluateAodPolicy(context, "shouldCustomizeAodNow").lifecycleWantsPixelOverlay;
    }

    static boolean shouldApplyModuleAodNow(Context context, String source) {
        return evaluateAodPolicy(context, source).shouldApplyModuleAod;
    }

    static boolean isBriefAodDisplayActive(Context context) {
        if (context == null) {
            return false;
        }
        AodLifecycleState state = currentAodLifecycleState(context);
        return state != null
                && !state.interactive
                && state.triggerBriefActive
                && isModuleEnabled(context)
                && isTriggerBriefAllowedByMode(aodDisplayMode(context));
    }

    static boolean shouldKeepDozeScreenActive(Context context) {
        if (context == null) {
            return false;
        }
        return evaluateAodPolicy(context, "doze-keepalive").shouldKeepNativeDozeAlive;
    }

    static boolean isModuleAodPolicyAllowingDisplay(Context context, String source) {
        return evaluateAodPolicy(context, source).modulePolicyAllowsDisplay;
    }

    static boolean isContinuousAodPolicyAllowingDisplay(Context context, String source) {
        if (context == null) {
            return false;
        }
        String trace = ensureAodTrace(source);
        String displayMode = aodDisplayMode(context);
        boolean allowed = isModuleEnabled(context)
                && isContinuousAodAllowedByMode(displayMode)
                && isWithinAodSchedule(context)
                && isPowerPolicyAllowingAod(context, source, trace, false);
        PixelAodLog.log("AOD continuous policy source=" + source
                + " allowed=" + allowed
                + " displayMode=" + displayMode
                + " trace=" + trace
                + " state={" + describeAodState(context) + "}");
        return allowed;
    }

    static OosAodLifecycleAdapter.AodPolicyDecision evaluateAodPolicy(
            Context context, String source) {
        return evaluateAodPolicy(context, source, false, false);
    }

    private static OosAodLifecycleAdapter.AodPolicyDecision evaluateAodPolicy(
            Context context, String source, boolean proximityBlocked,
            boolean expandedShadeBlocked) {
        String normalizedSource = TextUtils.isEmpty(source) ? "unknown" : source;
        String trace = ensureAodTrace(normalizedSource);
        Context ctx = context != null ? context : appContext;
        AodLifecycleState state = currentAodLifecycleState(ctx);
        if (maybeStartNativeShortWakeTrigger(ctx, normalizedSource, trace, state)) {
            state = currentAodLifecycleState(ctx);
        }
        OosAodLifecycleAdapter.ModulePolicy modulePolicy =
                evaluateModuleAodPolicy(ctx, normalizedSource, trace, state);
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                OosAodLifecycleAdapter.evaluatePolicy(
                normalizedSource,
                trace,
                state,
                modulePolicy,
                proximityBlocked,
                expandedShadeBlocked);
        logAodPolicyDecision(decision);
        return decision;
    }

    private static OosAodLifecycleAdapter.ModulePolicy evaluateModuleAodPolicy(
            Context context, String source, String trace, AodLifecycleState state) {
        if (context == null) {
            PixelAodLog.log("AOD module policy blocked source=" + source
                    + " reason=no-context"
                    + " trace=" + trace
                    + " state={" + describeAodState(null) + "}");
            return new OosAodLifecycleAdapter.ModulePolicy(false, false, false, false,
                    "no-context", "unknown", false, false);
        }
        boolean moduleEnabled = isModuleEnabled(context);
        String displayMode = aodDisplayMode(context);
        boolean withinSchedule = isWithinAodSchedule(context);
        boolean triggerBriefActive = state != null && state.triggerBriefActive;
        if (!moduleEnabled) {
            PixelAodLog.log("AOD module policy blocked source=" + source
                    + " reason=module-disabled"
                    + " displayMode=" + displayMode
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return new OosAodLifecycleAdapter.ModulePolicy(false, false, false, false,
                    "module-disabled", displayMode, withinSchedule, triggerBriefActive);
        }
        OosAodLifecycleAdapter.PowerPolicyDecision powerPolicy =
                evaluateAndLogPowerPolicy(context, source, trace, false);
        if (!powerPolicy.allowsDisplay) {
            return new OosAodLifecycleAdapter.ModulePolicy(false, moduleEnabled, false, false,
                    powerPolicy.reason, displayMode, withinSchedule, triggerBriefActive);
        }
        boolean continuousAllowed = isContinuousAodAllowedByMode(displayMode)
                && withinSchedule;
        boolean triggerBriefAllowed = isTriggerBriefAllowedByMode(displayMode)
                && triggerBriefActive;
        if (triggerBriefAllowed) {
            PixelAodLog.log("AOD module policy allowed source=" + source
                    + " reason=trigger-brief-display"
                    + " displayMode=" + displayMode
                    + " withinSchedule=" + withinSchedule
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return new OosAodLifecycleAdapter.ModulePolicy(true, moduleEnabled, continuousAllowed,
                    true, "trigger-brief-display", displayMode, withinSchedule,
                    true);
        }
        if (continuousAllowed) {
            return new OosAodLifecycleAdapter.ModulePolicy(true, moduleEnabled, true, false,
                    "continuous-schedule", displayMode, withinSchedule, false);
        }
        if (isTriggerOnlyAodMode(displayMode)) {
            PixelAodLog.log("AOD module policy blocked source=" + source
                    + " reason=trigger-only-waiting-trigger"
                    + " displayMode=" + displayMode
                    + " withinSchedule=" + withinSchedule
                    + " trace=" + trace
                    + " state={" + describeAodState(context) + "}");
            return new OosAodLifecycleAdapter.ModulePolicy(false, moduleEnabled, false, false,
                    "trigger-only-waiting-trigger", displayMode, withinSchedule, false);
        }
        PixelAodLog.log("AOD module policy blocked source=" + source
                + " reason=outside-schedule"
                + " displayMode=" + displayMode
                + " withinSchedule=" + withinSchedule
                + " trace=" + trace
                + " state={" + describeAodState(context) + "}");
        return new OosAodLifecycleAdapter.ModulePolicy(false, moduleEnabled, false, false,
                "outside-schedule", displayMode, withinSchedule, false);
    }

    private static boolean maybeStartNativeShortWakeTrigger(Context context, String source,
            String trace, AodLifecycleState state) {
        if (context == null || state == null || state.interactive || state.triggerBriefActive) {
            return false;
        }
        String displayMode = aodDisplayMode(context);
        if (!isModuleEnabled(context) || !isTriggerBriefAllowedByMode(displayMode)) {
            return false;
        }
        if (!state.displayAod) {
            return false;
        }
        if (isProximityNear()) {
            PixelAodLog.log("blocked native short-wake Pixel AOD trigger"
                    + " source=" + source
                    + " reason=proximity-near"
                    + " displayMode=" + displayMode
                    + " trace=" + trace
                    + " state={" + state.describe(lastAodCompactClock, lastAodClockWeight) + "}");
            return false;
        }
        boolean withinSchedule = isWithinAodSchedule(context);
        if (isContinuousAodAllowedByMode(displayMode) && withinSchedule) {
            return false;
        }
        OosAodLifecycleAdapter.TriggerBehavior behavior =
                OosAodLifecycleAdapter.behaviorForTrigger(
                        state.nativeTriggerType,
                        state.nativeTriggerSource,
                        state.nativeTriggerDetail);
        boolean implicitDisplayWake =
                isImplicitDisplayWakeTriggerCandidate(state, behavior);
        if (behavior == null || (!behavior.startsBriefDisplay && !implicitDisplayWake)) {
            PixelAodLog.log("skipped native short-wake Pixel AOD trigger"
                    + " source=" + source
                    + " reason=non-display-trigger"
                    + " displayMode=" + displayMode
                    + " withinSchedule=" + withinSchedule
                    + " triggerEvent=" + (behavior != null ? behavior.eventLabel : "none")
                    + " triggerRule=" + (behavior != null ? behavior.ruleLabel : "none")
                    + " triggerCategory="
                    + (behavior != null ? behavior.categoryLabel : "none")
                    + " triggerDisplayMode="
                    + (behavior != null ? behavior.displayModeLabel : "none")
                    + " futureAction=" + (behavior != null ? behavior.futureAction : "none")
                    + " trace=" + trace
                    + " nativeTriggerAgeMs=" + ageSince(state.now, state.nativeTriggerAt)
                    + " state={" + state.describe(lastAodCompactClock, lastAodClockWeight)
                    + "}");
            return false;
        }
        if (!isRecentUptime(state.now, state.nativeTriggerAt,
                NATIVE_SHORT_WAKE_TRIGGER_FRESHNESS_MS)) {
            PixelAodLog.log("skipped native short-wake Pixel AOD trigger"
                    + " source=" + source
                    + " reason=stale-native-trigger"
                    + " displayMode=" + displayMode
                    + " withinSchedule=" + withinSchedule
                    + " trace=" + trace
                    + " nativeTriggerAgeMs=" + ageSince(state.now, state.nativeTriggerAt)
                    + " state={" + state.describe(lastAodCompactClock, lastAodClockWeight) + "}");
            return false;
        }
        String triggerKey = nativeShortWakeTriggerKey(state);
        synchronized (PixelAodClockView.class) {
            if (TextUtils.equals(lastNativeShortWakeTriggerKey, triggerKey)) {
                PixelAodLog.log("skipped native short-wake Pixel AOD trigger"
                        + " source=" + source
                        + " reason=already-consumed-native-trigger"
                        + " displayMode=" + displayMode
                        + " withinSchedule=" + withinSchedule
                        + " trace=" + trace
                        + " key=" + triggerKey
                        + " consumedAgeMs=" + ageSince(state.now, lastNativeShortWakeTriggerAt)
                        + " state={" + state.describe(lastAodCompactClock, lastAodClockWeight) + "}");
                return false;
            }
        }
        String triggerType = implicitDisplayWake ? "native-display-wake" : "native-short-wake";
        boolean started = startBriefAodTrigger(triggerType, source,
                "displayState=" + displayStateLabel(state.displayState)
                        + " withinSchedule=" + withinSchedule
                        + " mode=" + displayMode
                        + " triggerEvent=" + (behavior != null ? behavior.eventLabel : "none")
                        + " triggerRule=" + (behavior != null ? behavior.ruleLabel : "none")
                        + " triggerCategory="
                        + (behavior != null ? behavior.categoryLabel : "none")
                        + " triggerDisplayMode="
                        + (behavior != null ? behavior.displayModeLabel : "none")
                        + " implicitDisplayWake=" + implicitDisplayWake,
                state.now);
        if (started) {
            synchronized (PixelAodClockView.class) {
                lastNativeShortWakeTriggerKey = triggerKey;
                lastNativeShortWakeTriggerAt = state.now;
            }
        }
        PixelAodLog.log("native short-wake trigger candidate source=" + source
                + " started=" + started
                + " displayMode=" + displayMode
                + " withinSchedule=" + withinSchedule
                + " implicitDisplayWake=" + implicitDisplayWake
                + " triggerRule=" + (behavior != null ? behavior.ruleLabel : "none")
                + " triggerCategory=" + (behavior != null ? behavior.categoryLabel : "none")
                + " trace=" + trace
                + " state={" + describeAodState(context) + "}");
        return started;
    }

    private static boolean isImplicitDisplayWakeTriggerCandidate(AodLifecycleState state,
            OosAodLifecycleAdapter.TriggerBehavior behavior) {
        if (state == null || !state.displayAod || state.triggerBriefActive) {
            return false;
        }
        long screenOffAge = ageSince(state.now, state.screenOffAt);
        if (screenOffAge < IMPLICIT_DISPLAY_WAKE_MIN_SCREEN_OFF_AGE_MS) {
            return false;
        }
        if (behavior != null && behavior.releasesDisplayGuard) {
            return true;
        }
        return state.previousDisplayState == Display.STATE_OFF
                && isRecentUptime(state.now, state.displayStateChangedAt,
                NATIVE_SHORT_WAKE_TRIGGER_FRESHNESS_MS);
    }

    private static String nativeShortWakeTriggerKey(AodLifecycleState state) {
        if (state == null) {
            return "";
        }
        return state.traceId
                + "|" + state.nativeTriggerType
                + "|" + state.nativeTriggerSource
                + "|" + state.nativeTriggerDetail
                + "|" + state.nativeTriggerAt;
    }

    private static boolean isModuleEnabled(Context context) {
        return PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_MODULE_ENABLED, true);
    }

    private static String aodDisplayMode(Context context) {
        String mode = PixelAodSettings.getString(context, PixelAodSettings.KEY_AOD_DISPLAY_MODE,
                PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS);
        if (PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY.equals(mode)) {
            return PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY;
        }
        return PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS;
    }

    private static boolean isContinuousAodAllowedByMode(String displayMode) {
        return PixelAodSettings.AOD_DISPLAY_MODE_CONTINUOUS.equals(displayMode);
    }

    private static boolean isTriggerOnlyAodMode(String displayMode) {
        return PixelAodSettings.AOD_DISPLAY_MODE_TRIGGER_ONLY.equals(displayMode);
    }

    private static boolean isTriggerBriefAllowedByMode(String displayMode) {
        return isContinuousAodAllowedByMode(displayMode) || isTriggerOnlyAodMode(displayMode);
    }

    private static void logAodPolicyDecision(OosAodLifecycleAdapter.AodPolicyDecision decision) {
        PixelAodLog.log("AOD policy decision source=" + decision.source
                + " trace=" + decision.trace
                + " shouldDrawPixelOverlay=" + decision.shouldDrawPixelOverlay
                + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                + " shouldSuppressStockAodViews=" + decision.shouldSuppressStockAodViews
                + " shouldAllowNativeHideCallbacks=" + decision.shouldAllowNativeHideCallbacks
                + " lifecycleWantsPixelOverlay=" + decision.lifecycleWantsPixelOverlay
                + " modulePolicyAllowsDisplay=" + decision.modulePolicyAllowsDisplay
                + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                + " proximityBlocked=" + decision.proximityBlocked
                + " expandedShadeBlocked=" + decision.expandedShadeBlocked
                + " reasons={draw=" + decision.drawReason
                + ",keepDoze=" + decision.keepNativeDozeReason
                + ",stock=" + decision.stockSuppressionReason
                + ",nativeHide=" + decision.nativeHideCallbackReason
                + ",module=" + decision.modulePolicyReason + "}"
                + " displayMode=" + decision.stateDisplayMode()
                + " withinSchedule=" + decision.stateWithinSchedule()
                + " state={" + describeAodState(appContext) + "}");
    }

    static boolean isInAodEntryTransitionWindow(long windowMillis) {
        return currentAodLifecycleState(null).isInEntryTransitionWindow(windowMillis);
    }

    static boolean shouldBridgeLockscreenDuringAodEntry(Context context, long windowMillis) {
        AodLifecycleState state = currentAodLifecycleState(context);
        boolean inEntryWindow =
                OosAodLifecycleAdapter.shouldBridgeLockscreenDuringAodEntry(state, windowMillis);
        if (!inEntryWindow) {
            return false;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                evaluateAodPolicy(context, "lockscreen-bridge");
        boolean bridge = decision.shouldApplyModuleAod;
        PixelAodLog.log("AOD lockscreen bridge decision trace=" + decision.trace
                + " bridge=" + bridge
                + " inEntryWindow=" + inEntryWindow
                + " shouldApplyModuleAod=" + decision.shouldApplyModuleAod
                + " reason=" + decision.drawReason
                + " state={" + describeAodState(context) + "}");
        return bridge;
    }

    private static boolean isAllowedAodEntryDelay(long now, long then) {
        if (then <= 0L) {
            return false;
        }
        long age = now - then;
        return age >= 0L && age < AOD_ENTRY_DELAY_MILLIS;
    }

    private static boolean isAllowedAodEntryAge(long now, long then) {
        if (then <= 0L) {
            return false;
        }
        long age = now - then;
        return age >= 0L && age <= AOD_ENTRY_GRACE_MILLIS;
    }

    private static boolean isRecentUptime(long now, long then, long windowMillis) {
        if (then <= 0L) {
            return false;
        }
        long age = now - then;
        return age >= 0L && age <= windowMillis;
    }

    static boolean isDisplayInAodState(Context context) {
        int state = currentDisplayState(context);
        return state == Display.STATE_DOZE || state == Display.STATE_DOZE_SUSPEND;
    }

    private static int currentDisplayState(Context context) {
        try {
            if (context == null) {
                return -1;
            }
            Display display = null;
            if (Build.VERSION.SDK_INT >= 30) {
                display = context.getDisplay();
            }
            if (display == null) {
                DisplayManager displayManager =
                        (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                if (displayManager != null) {
                    display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
                }
            }
            if (display == null) {
                return -1;
            }
            return display.getState();
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static String displayStateLabel(int state) {
        switch (state) {
            case Display.STATE_OFF:
                return "OFF(" + state + ")";
            case Display.STATE_ON:
                return "ON(" + state + ")";
            case Display.STATE_DOZE:
                return "DOZE(" + state + ")";
            case Display.STATE_DOZE_SUSPEND:
                return "DOZE_SUSPEND(" + state + ")";
            default:
                return "STATE_" + state;
        }
    }

    private static AodLifecycleState currentAodLifecycleState(Context context) {
        long now = SystemClock.uptimeMillis();
        boolean active;
        long screenOffAt;
        long aodActivatedAt;
        long overlayVisibleAt;
        String traceId;
        String traceSource;
        long traceAt;
        String triggerType;
        String triggerSource;
        String triggerDetail;
        long triggerAt;
        String briefTriggerType;
        String briefTriggerSource;
        String briefTriggerDetail;
        long briefTriggerStartedAt;
        long briefTriggerUntilAt;
        String notificationPulseRule;
        String notificationPulseCategory;
        String notificationPulseSource;
        String notificationPulseTrace;
        String notificationPulsePackages;
        String notificationPulsePolicy;
        String notificationPulsePolicyReason;
        String notificationPulsePolicyAction;
        boolean notificationPulsePolicyNativeCompatible;
        boolean notificationPulsePolicyBlocked;
        int notificationPulseRawCount;
        int notificationPulseUsableCount;
        int notificationPulseMediaCount;
        long notificationPulseAt;
        long revealBlockedUntilAt;
        synchronized (PixelAodClockView.class) {
            if (TextUtils.isEmpty(lastAodTraceId)) {
                startAodTraceLocked("auto");
            }
            active = aodActive;
            screenOffAt = lastScreenOffAt;
            aodActivatedAt = lastAodActivatedAt;
            overlayVisibleAt = lastAodOverlayVisibleAt;
            revealBlockedUntilAt = nonLockscreenAodRevealBlockedUntilAt;
            traceId = lastAodTraceId;
            traceSource = lastAodTraceSource;
            traceAt = lastAodTraceAt;
            triggerType = lastNativeTriggerType;
            triggerSource = lastNativeTriggerSource;
            triggerDetail = lastNativeTriggerDetail;
            triggerAt = lastNativeTriggerAt;
            briefTriggerType = briefAodTriggerType;
            briefTriggerSource = briefAodTriggerSource;
            briefTriggerDetail = briefAodTriggerDetail;
            briefTriggerStartedAt = briefAodTriggerStartedAt;
            briefTriggerUntilAt = briefAodTriggerUntilAt;
            notificationPulseRule = lastNotificationPulseRule;
            notificationPulseCategory = lastNotificationPulseCategory;
            notificationPulseSource = lastNotificationPulseSource;
            notificationPulseTrace = lastNotificationPulseTrace;
            notificationPulsePackages = lastNotificationPulsePackages;
            notificationPulsePolicy = lastNotificationPulsePolicy;
            notificationPulsePolicyReason = lastNotificationPulsePolicyReason;
            notificationPulsePolicyAction = lastNotificationPulsePolicyAction;
            notificationPulsePolicyNativeCompatible =
                    lastNotificationPulsePolicyNativeCompatible;
            notificationPulsePolicyBlocked = lastNotificationPulsePolicyBlocked;
            notificationPulseRawCount = lastNotificationPulseRawCount;
            notificationPulseUsableCount = lastNotificationPulseUsableCount;
            notificationPulseMediaCount = lastNotificationPulseMediaCount;
            notificationPulseAt = lastNotificationPulseAt;
        }
        int displayState = currentDisplayState(context);
        int previousDisplayState;
        long displayStateChangedAt;
        boolean displayStateChanged;
        synchronized (PixelAodClockView.class) {
            previousDisplayState = previousObservedDisplayState;
            displayStateChangedAt = lastDisplayStateChangedAt;
            displayStateChanged = false;
            if (lastObservedDisplayState != displayState) {
                previousDisplayState = lastObservedDisplayState;
                previousObservedDisplayState = lastObservedDisplayState;
                lastObservedDisplayState = displayState;
                lastDisplayStateChangedAt = now;
                displayStateChangedAt = now;
                displayStateChanged = true;
            }
        }
        boolean interactive = isDeviceInteractive(context);
        boolean displayAod = displayState == Display.STATE_DOZE
                || displayState == Display.STATE_DOZE_SUSPEND;
        boolean triggerBriefActive = briefTriggerStartedAt > 0L
                && briefTriggerUntilAt > 0L
                && now >= briefTriggerStartedAt
                && now <= briefTriggerUntilAt;
        boolean entryDelay = isAllowedAodEntryDelay(now, screenOffAt)
                || isAllowedAodEntryDelay(now, aodActivatedAt);
        boolean graceWindow = isAllowedAodEntryAge(now, screenOffAt)
                || isAllowedAodEntryAge(now, aodActivatedAt);
        boolean recentOverlayVisible =
                isRecentUptime(now, overlayVisibleAt, AOD_FORCE_DOZE_RECENT_OVERLAY_MILLIS);
        boolean rawShouldDrawPixelAod = context != null
                && !interactive
                && (displayAod || entryDelay || triggerBriefActive || (active && graceWindow));
        boolean revealBlocked = rawShouldDrawPixelAod
                && revealBlockedUntilAt > 0L
                && now >= screenOffAt
                && now < revealBlockedUntilAt;
        boolean shouldDrawPixelAod = rawShouldDrawPixelAod && !revealBlocked;
        return new AodLifecycleState(now, active, screenOffAt, aodActivatedAt,
                overlayVisibleAt, revealBlockedUntilAt, revealBlocked, traceId,
                traceSource, traceAt, displayState, previousDisplayState,
                displayStateChangedAt, displayStateChanged, interactive, displayAod,
                entryDelay, graceWindow, recentOverlayVisible, shouldDrawPixelAod,
                triggerType, triggerSource, triggerDetail, triggerAt, triggerBriefActive,
                briefTriggerType, briefTriggerSource, briefTriggerDetail,
                briefTriggerStartedAt, briefTriggerUntilAt, notificationPulseRule,
                notificationPulseCategory, notificationPulseSource, notificationPulseTrace,
                notificationPulsePackages, notificationPulsePolicy,
                notificationPulsePolicyReason, notificationPulsePolicyAction,
                notificationPulsePolicyNativeCompatible, notificationPulsePolicyBlocked,
                notificationPulseRawCount,
                notificationPulseUsableCount, notificationPulseMediaCount,
                notificationPulseAt);
    }

    static final class AodLifecycleState {
        final long now;
        final boolean active;
        final long screenOffAt;
        final long aodActivatedAt;
        final long overlayVisibleAt;
        final long revealBlockedUntilAt;
        final boolean revealBlocked;
        final String traceId;
        final String traceSource;
        final long traceAt;
        final int displayState;
        final int previousDisplayState;
        final long displayStateChangedAt;
        final boolean displayStateChanged;
        final boolean interactive;
        final boolean displayAod;
        final boolean entryDelay;
        final boolean graceWindow;
        final boolean recentOverlayVisible;
        final boolean shouldDrawPixelAod;
        final String nativeTriggerType;
        final String nativeTriggerSource;
        final String nativeTriggerDetail;
        final long nativeTriggerAt;
        final boolean triggerBriefActive;
        final String triggerBriefType;
        final String triggerBriefSource;
        final String triggerBriefDetail;
        final long triggerBriefStartedAt;
        final long triggerBriefUntilAt;
        final String notificationPulseRule;
        final String notificationPulseCategory;
        final String notificationPulseSource;
        final String notificationPulseTrace;
        final String notificationPulsePackages;
        final String notificationPulsePolicy;
        final String notificationPulsePolicyReason;
        final String notificationPulsePolicyAction;
        final boolean notificationPulsePolicyNativeCompatible;
        final boolean notificationPulsePolicyBlocked;
        final int notificationPulseRawCount;
        final int notificationPulseUsableCount;
        final int notificationPulseMediaCount;
        final long notificationPulseAt;

        AodLifecycleState(long now, boolean active, long screenOffAt, long aodActivatedAt,
                long overlayVisibleAt, long revealBlockedUntilAt, boolean revealBlocked,
                String traceId, String traceSource, long traceAt, int displayState,
                int previousDisplayState, long displayStateChangedAt, boolean displayStateChanged,
                boolean interactive, boolean displayAod, boolean entryDelay,
                boolean graceWindow, boolean recentOverlayVisible, boolean shouldDrawPixelAod,
                String nativeTriggerType,
                String nativeTriggerSource, String nativeTriggerDetail, long nativeTriggerAt,
                boolean triggerBriefActive, String triggerBriefType,
                String triggerBriefSource, String triggerBriefDetail,
                long triggerBriefStartedAt, long triggerBriefUntilAt,
                String notificationPulseRule, String notificationPulseCategory,
                String notificationPulseSource, String notificationPulseTrace,
                String notificationPulsePackages, String notificationPulsePolicy,
                String notificationPulsePolicyReason, String notificationPulsePolicyAction,
                boolean notificationPulsePolicyNativeCompatible,
                boolean notificationPulsePolicyBlocked, int notificationPulseRawCount,
                int notificationPulseUsableCount, int notificationPulseMediaCount,
                long notificationPulseAt) {
            this.now = now;
            this.active = active;
            this.screenOffAt = screenOffAt;
            this.aodActivatedAt = aodActivatedAt;
            this.overlayVisibleAt = overlayVisibleAt;
            this.revealBlockedUntilAt = revealBlockedUntilAt;
            this.revealBlocked = revealBlocked;
            this.traceId = traceId;
            this.traceSource = traceSource;
            this.traceAt = traceAt;
            this.displayState = displayState;
            this.previousDisplayState = previousDisplayState;
            this.displayStateChangedAt = displayStateChangedAt;
            this.displayStateChanged = displayStateChanged;
            this.interactive = interactive;
            this.displayAod = displayAod;
            this.entryDelay = entryDelay;
            this.graceWindow = graceWindow;
            this.recentOverlayVisible = recentOverlayVisible;
            this.shouldDrawPixelAod = shouldDrawPixelAod;
            this.nativeTriggerType = nativeTriggerType;
            this.nativeTriggerSource = nativeTriggerSource;
            this.nativeTriggerDetail = nativeTriggerDetail;
            this.nativeTriggerAt = nativeTriggerAt;
            this.triggerBriefActive = triggerBriefActive;
            this.triggerBriefType = triggerBriefType;
            this.triggerBriefSource = triggerBriefSource;
            this.triggerBriefDetail = triggerBriefDetail;
            this.triggerBriefStartedAt = triggerBriefStartedAt;
            this.triggerBriefUntilAt = triggerBriefUntilAt;
            this.notificationPulseRule = notificationPulseRule;
            this.notificationPulseCategory = notificationPulseCategory;
            this.notificationPulseSource = notificationPulseSource;
            this.notificationPulseTrace = notificationPulseTrace;
            this.notificationPulsePackages = notificationPulsePackages;
            this.notificationPulsePolicy = notificationPulsePolicy;
            this.notificationPulsePolicyReason = notificationPulsePolicyReason;
            this.notificationPulsePolicyAction = notificationPulsePolicyAction;
            this.notificationPulsePolicyNativeCompatible = notificationPulsePolicyNativeCompatible;
            this.notificationPulsePolicyBlocked = notificationPulsePolicyBlocked;
            this.notificationPulseRawCount = notificationPulseRawCount;
            this.notificationPulseUsableCount = notificationPulseUsableCount;
            this.notificationPulseMediaCount = notificationPulseMediaCount;
            this.notificationPulseAt = notificationPulseAt;
        }

        boolean shouldDrawPixelAod() {
            return shouldDrawPixelAod;
        }

        boolean isInEntryTransitionWindow(long windowMillis) {
            return isRecentUptime(now, screenOffAt, windowMillis)
                    || isRecentUptime(now, aodActivatedAt, windowMillis);
        }

        String phase() {
            if (interactive) {
                return "interactive";
            }
            if (displayAod && shouldDrawPixelAod) {
                return "aod-visible";
            }
            if (revealBlocked) {
                return "entering-aod-reveal-blocked";
            }
            if (entryDelay) {
                return "entering-aod";
            }
            if (active && graceWindow) {
                return "aod-grace";
            }
            if (active) {
                return "aod-active-waiting-display";
            }
            return "inactive";
        }

        String describe(boolean compact, int weight) {
            return "phase=" + phase()
                    + " active=" + active
                    + " customizeNow=" + shouldDrawPixelAod
                    + " interactive=" + interactive
                    + " displayState=" + displayStateLabel(displayState)
                    + " previousDisplayState=" + displayStateLabel(previousDisplayState)
                    + " displayStateAgeMs=" + ageSince(now, displayStateChangedAt)
                    + " displayStateChanged=" + displayStateChanged
                    + " displayAod=" + displayAod
                    + " screenOffAgeMs=" + ageSince(now, screenOffAt)
                    + " aodAgeMs=" + ageSince(now, aodActivatedAt)
                    + " overlayAgeMs=" + ageSince(now, overlayVisibleAt)
                    + " recentOverlayVisible=" + recentOverlayVisible
                    + " revealBlocked=" + revealBlocked
                    + " revealRemainingMs=" + Math.max(0L, revealBlockedUntilAt - now)
                    + " trace=" + traceId
                    + " traceSource=" + traceSource
                    + " traceAgeMs=" + ageSince(now, traceAt)
                    + " nativeTrigger=" + nativeTriggerType
                    + " nativeTriggerSource=" + nativeTriggerSource
                    + " nativeTriggerAgeMs=" + ageSince(now, nativeTriggerAt)
                    + " nativeTriggerDetail={" + nativeTriggerDetail + "}"
                    + " triggerBriefActive=" + triggerBriefActive
                    + " triggerBriefType=" + triggerBriefType
                    + " triggerBriefSource=" + triggerBriefSource
                    + " triggerBriefAgeMs=" + ageSince(now, triggerBriefStartedAt)
                    + " triggerBriefRemainingMs=" + remainingUntil(now, triggerBriefUntilAt)
                    + " triggerBriefDetail={" + triggerBriefDetail + "}"
                    + " notificationPulseRule=" + notificationPulseRule
                    + " notificationPulseCategory=" + notificationPulseCategory
                    + " notificationPulseSource=" + notificationPulseSource
                    + " notificationPulseTrace=" + notificationPulseTrace
                    + " notificationPulseAgeMs=" + ageSince(now, notificationPulseAt)
                    + " notificationPulseRecent=" + isRecentUptime(now, notificationPulseAt,
                    NOTIFICATION_PULSE_RECENT_MILLIS)
                    + " notificationPulsePolicy=" + notificationPulsePolicy
                    + " notificationPulsePolicyReason=" + notificationPulsePolicyReason
                    + " notificationPulsePolicyAction=" + notificationPulsePolicyAction
                    + " notificationPulsePolicyNativeCompatible="
                    + notificationPulsePolicyNativeCompatible
                    + " notificationPulsePolicyBlocked=" + notificationPulsePolicyBlocked
                    + " notificationPulseRaw=" + notificationPulseRawCount
                    + " notificationPulseUsable=" + notificationPulseUsableCount
                    + " notificationPulseMedia=" + notificationPulseMediaCount
                    + " notificationPulsePackages=" + notificationPulsePackages
                    + " compact=" + compact
                    + " weight=" + weight
                    + " entryDelay=" + entryDelay
                    + " graceWindow=" + graceWindow;
        }
    }

    private static long ageSince(long now, long then) {
        if (then <= 0L) {
            return -1L;
        }
        long age = now - then;
        return age >= 0L ? age : -1L;
    }

    private static long remainingUntil(long now, long then) {
        if (then <= 0L) {
            return -1L;
        }
        long remaining = then - now;
        return remaining >= 0L ? remaining : -1L;
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
            HashMap<String, AodNotificationPipeline.RankingSnapshot> snapshot = new HashMap<>();
            NotificationListenerService.Ranking ranking = new NotificationListenerService.Ranking();
            for (String key : keys) {
                if (key != null && rankingMap.getRanking(key, ranking)) {
                    snapshot.put(key, AodNotificationPipeline.RankingSnapshot.from(ranking));
                }
            }
            String signature = AodNotificationPipeline.rankingSignature(snapshot);
            synchronized (PixelAodClockView.class) {
                if (TextUtils.equals(lastRankingSignature, signature)) {
                    return;
                }
                lastRankingSignature = signature;
                notificationRankings.clear();
                notificationRankings.putAll(snapshot);
            }
            PixelAodLog.log("updated AOD notification ranking lockscreen overrides count="
                    + snapshot.size());
            OosAodLifecycleAdapter.recordNotificationPulseObservation(
                    "ranking-map",
                    -1,
                    -1,
                    -1,
                    snapshot.size(),
                    "rankings",
                    currentAodTraceId(),
                    describeAodState(appContext));
            StatusBarNotification[] rawSnapshot;
            synchronized (PixelAodClockView.class) {
                rawSnapshot = rawNotifications;
            }
            setActiveNotifications(rawSnapshot, "ranking-map");
        } catch (Throwable t) {
            PixelAodLog.log("failed to update AOD notification ranking map", t);
        }
    }

    static void clearActiveNotifications() {
        setActiveNotifications(null, "clearActiveNotifications");
    }

    static void setMediaNotificationCandidates(StatusBarNotification[] notifications, String source) {
        int count;
        boolean changed;
        ArrayList<String> mediaPackages;
        synchronized (PixelAodClockView.class) {
            count = replaceMediaNotificationCandidatesLocked(notifications);
            String signature = mediaCandidatesSignatureLocked();
            changed = !TextUtils.equals(lastMediaCandidatesSignature, signature);
            lastMediaCandidatesSignature = signature;
            mediaPackages = mediaNotificationPackagesLocked();
        }
        if (!changed) {
            return;
        }
        logMediaNotificationCache("replaced", source, count);
        refreshMediaLinesForMediaActivity(mediaPackages, source + "#media-candidates");
    }

    private static int replaceMediaNotificationCandidatesLocked(StatusBarNotification[] notifications) {
        mediaNotificationCache.clear();
        int count = 0;
        if (notifications != null) {
            for (StatusBarNotification sbn : notifications) {
                if (AodNotificationPipeline.isMediaIconCandidate(sbn)) {
                    mediaNotificationCache.put(sbn.getKey(), sbn);
                    count++;
                }
            }
        }
        return count;
    }

    static void cacheMediaNotificationCandidate(StatusBarNotification sbn, String source) {
        if (!AodNotificationPipeline.isMediaIconCandidate(sbn)) {
            return;
        }
        int count;
        boolean contentChanged;
        String oldText;
        String newText = formatMediaNotificationText(sbn);
        synchronized (PixelAodClockView.class) {
            oldText = formatMediaNotificationText(mediaNotificationCache.get(sbn.getKey()));
            contentChanged = !TextUtils.equals(oldText, newText);
            mediaNotificationCache.put(sbn.getKey(), sbn);
            count = mediaNotificationCache.size();
            lastMediaCandidatesSignature = mediaCandidatesSignatureLocked();
        }
        logMediaNotificationCache("cached", source, count, sbn.getPackageName(), contentChanged, newText);
        if (contentChanged) {
            refreshMediaLinesForMediaActivity(Collections.singletonList(sbn.getPackageName()),
                    source + "#media-notification-content");
        } else {
            refreshMediaLines();
        }
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
            if (removed) {
                lastMediaCandidatesSignature = mediaCandidatesSignatureLocked();
            }
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
                        view.requestAodFrameRefresh("weather");
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
        long[] sunTimes = readSunTimesFromIntent(intent);
        WeatherSnapshot direct = WeatherSnapshot.from(
                formatTemperature(temperature),
                code,
                condition,
                System.currentTimeMillis(),
                sunTimes[0],
                sunTimes[1]);
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
            long[] sunTimes = readSunTimesFromJson(object);
            WeatherSnapshot snapshot = WeatherSnapshot.from(
                    formatTemperature(temperature), code, condition, timestampMillis,
                    sunTimes[0], sunTimes[1]);
            return snapshot.hasDisplayableWeather() ? snapshot : null;
        } catch (Throwable t) {
            PixelAodLog.log("failed to parse Breezy WeatherJson", t);
            return null;
        }
    }

    private static long[] readSunTimesFromJson(JSONObject object) {
        long[] result = new long[]{0L, 0L};
        if (object == null) {
            return result;
        }
        long sunriseSec = optLongSeconds(object, "sunRise", "sunrise");
        long sunsetSec = optLongSeconds(object, "sunSet", "sunset");
        if ((sunriseSec > 0L || sunsetSec > 0L)) {
            result[0] = sunriseSec * 1000L;
            result[1] = sunsetSec * 1000L;
            return result;
        }
        JSONObject[] candidates = collectSunTimeCandidates(object);
        for (JSONObject candidate : candidates) {
            if (candidate == null) continue;
            long s = optLongSeconds(candidate, "sunRise", "sunrise");
            long e = optLongSeconds(candidate, "sunSet", "sunset");
            if (s > 0L) result[0] = s * 1000L;
            if (e > 0L) result[1] = e * 1000L;
            if (result[0] > 0L && result[1] > 0L) return result;
        }
        return result;
    }

    private static JSONObject[] collectSunTimeCandidates(JSONObject object) {
        java.util.ArrayList<JSONObject> list = new java.util.ArrayList<>();
        JSONArray daily = object.optJSONArray("dailyForecast");
        if (daily != null) {
            for (int i = 0; i < daily.length(); i++) {
                JSONObject day = daily.optJSONObject(i);
                if (day != null) list.add(day);
            }
        }
        JSONArray hourly = object.optJSONArray("hourlyForecast");
        if (hourly != null && hourly.length() > 0) {
            list.add(hourly.optJSONObject(0));
        }
        JSONObject hourlyObj = object.optJSONObject("hourlyForecast");
        if (hourlyObj != null) {
            list.add(hourlyObj);
        }
        return list.toArray(new JSONObject[0]);
    }

    private static long optLongSeconds(JSONObject object, String camelKey, String lowerKey) {
        if (object == null) {
            return 0L;
        }
        if (object.has(camelKey)) {
            return object.optLong(camelKey, 0L);
        }
        if (object.has(lowerKey)) {
            return object.optLong(lowerKey, 0L);
        }
        return 0L;
    }

    private static long[] readSunTimesFromIntent(Intent intent) {
        long[] result = new long[]{0L, 0L};
        if (intent == null) {
            return result;
        }
        long s = firstLongExtra(intent,
                BreezyWeatherRelayReceiver.EXTRA_SUNRISE, "sunrise", "sunRise");
        long e = firstLongExtra(intent,
                BreezyWeatherRelayReceiver.EXTRA_SUNSET, "sunset", "sunSet");
        if (s > 0L) result[0] = s;
        if (e > 0L) result[1] = e;
        return result;
    }

    private static long firstLongExtra(Intent intent, String... keys) {
        for (String key : keys) {
            if (intent.hasExtra(key)) {
                long value = intent.getLongExtra(key, 0L);
                if (value > 0L) {
                    return value;
                }
            }
        }
        return 0L;
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
        PixelAodLog.log(message + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
    }

    static void setAtAGlanceExtra(String extra) {
        if (!AT_A_GLANCE_EXTRA_ENABLED) {
            synchronized (PixelAodClockView.class) {
                atAGlanceExtra = "";
            }
            return;
        }
        String normalized = PixelAodRenderModel.normalizeAtAGlanceExtra(extra);
        boolean changed;
        synchronized (PixelAodClockView.class) {
            changed = !TextUtils.equals(atAGlanceExtra, normalized);
            atAGlanceExtra = normalized;
        }
        if (!changed) {
            return;
        }
        PixelAodLog.log("updated Pixel AOD At a Glance extra=" + normalized
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateTime();
                    view.requestAodFrameRefresh("at-a-glance");
                }
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        INSTANCES.add(this);
        registerNotificationSettingsObserver();
        if (!clockPluginManaged) {
            registerScreenStateReceiver();
            updateAodVisibility("attach");
        }
        start();
        startMediaListening();
        if (clockPluginManaged) {
            refreshClockPluginAodContent("attach");
        } else {
            refreshPresentation();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterNotificationSettingsObserver();
        unregisterScreenStateReceiver();
        stopMediaListening();
        if (clockPluginManaged) {
            running = false;
            unregisterTimeReceiver();
            resetBurnInTranslation();
        } else {
            stop();
        }
        INSTANCES.remove(this);
        super.onDetachedFromWindow();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (getVisibility() != View.VISIBLE) {
            resetBurnInTranslation();
            return;
        }
        super.dispatchDraw(canvas);
    }

    public void start() {
        if (running) {
            if (clockPluginManaged) {
                refreshClockPluginAodContent("start-existing");
            } else {
                updateAodVisibility("start-existing");
                refreshPresentation();
            }
            return;
        }
        running = true;
        registerTimeReceiver();
        if (clockPluginManaged) {
            refreshClockPluginAodContent("start");
        } else {
            updateAodVisibility("start");
            refreshPresentation();
            scheduleEntrySecondRefresh();
        }
    }

    public void stop() {
        if (clockPluginManaged) {
            return;
        }
        running = false;
        unregisterTimeReceiver();
        resetBurnInTranslation();
    }

    void setClockPluginManaged(boolean managed, String source) {
        if (clockPluginManaged == managed) {
            return;
        }
        clockPluginManaged = managed;
        if (managed) {
            unregisterScreenStateReceiver();
            if (clockWeightAnimator != null) {
                clockWeightAnimator.cancel();
                clockWeightAnimator = null;
            }
            setVisibility(View.INVISIBLE);
            resetBurnInTranslation();
            if (isAttachedToWindow()) {
                start();
                refreshClockPluginAodContent(source + "#managed");
            }
        } else if (isAttachedToWindow()) {
            registerScreenStateReceiver();
            updateAodVisibility(source + "#legacy");
        }
        PixelAodLog.log("configured ClockPlugin AOD layer managed=" + managed
                + " source=" + source + " trace=" + currentAodTraceId());
    }

    boolean isClockPluginManaged() {
        return clockPluginManaged;
    }

    void presentClockPluginAod(boolean compact, boolean deferLockscreenWeightTransition,
            int handoffStartWeight, String source) {
        presentClockPluginAod(compact, deferLockscreenWeightTransition, handoffStartWeight,
                source, false);
    }

    /**
     * @param morphFromCompact when true and {@code compact} is false, layout is committed
     *                         as large immediately but the view starts scaled like compact
     *                         so {@link #startCompactToLargeEntryMorph} can expand it.
     */
    void presentClockPluginAod(boolean compact, boolean deferLockscreenWeightTransition,
            int handoffStartWeight, String source, boolean morphFromCompact) {
        if (!clockPluginManaged) {
            return;
        }
        if (!running) {
            start();
        }
        // Re-query media sessions before the first AOD paint so a lockscreen→AOD handoff
        // does not draw large/compact clock without the media row for a frame.
        refreshActiveMediaControllers();
        applyMaterialColors();
        boolean wasVisible = getVisibility() == View.VISIBLE && getAlpha() > 0.01f;
        boolean sizeChanged = compact != compactClock;
        // Glyph content + textSize before mode switch (MATCH_PARENT box is wrong for morph).
        final float[] fromClock = sizeChanged && wasVisible
                ? snapshotTextContentMorph(clockView)
                : null;
        if (clockView != null) {
            clockView.animate().cancel();
            clockView.setScaleX(1f);
            clockView.setScaleY(1f);
            clockView.setTranslationX(0f);
            clockView.setTranslationY(0f);
        }
        setScaleX(1f);
        setScaleY(1f);

        // Park weight BEFORE applyClockMode / icon rebuild so the first painted frame uses
        // LS weight (340), not residual AOD weight (160) from the previous session.
        boolean weightRunning = clockWeightAnimator != null && clockWeightAnimator.isRunning();
        int aodTargetWeight = aodClockWeight(getContext());
        int startWeight = normalizeClockWeight(handoffStartWeight);
        if (weightRunning) {
            PixelAodLog.log("kept running AOD weight transition on re-present source=" + source
                    + " currentWeight=" + currentClockWeight
                    + " settled=" + aodWeightHandoffSettled
                    + " trace=" + currentAodTraceId());
        } else if (aodWeightHandoffSettled) {
            if (Math.abs(currentClockWeight - aodTargetWeight) > 12) {
                applyStableAodClockWeight(source + "#ClockPlugin-reassert-settled");
            }
            PixelAodLog.log("kept settled AOD weight on re-present source=" + source
                    + " weight=" + currentClockWeight
                    + " aodTarget=" + aodTargetWeight
                    + " settled=true"
                    + " trace=" + currentAodTraceId());
        } else if (deferLockscreenWeightTransition
                && Math.abs(startWeight - aodTargetWeight) > 8) {
            aodWeightHandoffSettled = false;
            prepareClockPluginAodWeightForHandoff(startWeight, source + "#ClockPlugin");
        } else if (deferLockscreenWeightTransition) {
            applyStableAodClockWeight(source + "#ClockPlugin-already-aod");
        } else {
            applyStableAodClockWeight(source + "#ClockPlugin");
        }

        applyClockMode(compact);
        rebuildNotificationIcons(source + "#ClockPlugin");
        updateMediaLine(source + "#ClockPlugin");
        // Avoid exposing the TextView before its weighted Google Sans Typeface is ready.
        setVisibility(View.VISIBLE);
        applyBurnInTranslation();
        if (morphFromCompact && !compact) {
            // Entry morph is driven by startCompactToLargeEntryMorph on the host after present.
            if (mediaRow != null) {
                mediaRow.setAlpha(0f);
            }
        } else if (sizeChanged && wasVisible && !weightRunning
                && !deferLockscreenWeightTransition
                && fromClock != null && fromClock[4] > 1f) {
            // Size content morph only when not doing LS→AOD weight handoff (that path uses
            // startCompactToLargeEntryMorph after weight morph is visible).
            startAodTextContentMorph(fromClock, source + "#aod-size-morph");
            if (mediaRow != null) {
                mediaRow.setAlpha(MEDIA_ALPHA);
            }
        } else if (mediaRow != null && !(morphFromCompact && !compact)) {
            mediaRow.setAlpha(MEDIA_ALPHA);
        }
        requestAodFrameRefresh(source + "#ClockPlugin");
    }

    /**
     * @return float[]{centerX, centerY, contentW, contentH, textSize, pivotX, pivotY}
     *         zeros if unmeasured. Content box only — not MATCH_PARENT width.
     */
    private float[] snapshotTextContentMorph(TextView tv) {
        float[] out = new float[7];
        if (tv == null) {
            return out;
        }
        float localLeft;
        float localTop;
        float contentW;
        float contentH;
        android.text.Layout layout = tv.getLayout();
        if (layout != null && layout.getLineCount() > 0) {
            float left = Float.MAX_VALUE;
            float right = Float.MIN_VALUE;
            for (int i = 0; i < layout.getLineCount(); i++) {
                left = Math.min(left, layout.getLineLeft(i));
                right = Math.max(right, layout.getLineRight(i));
            }
            contentW = Math.max(1f, right - left);
            contentH = Math.max(1f, (float) layout.getHeight());
            localLeft = tv.getTotalPaddingLeft() + left;
            localTop = tv.getTotalPaddingTop();
        } else if (tv.getWidth() > 0 && tv.getHeight() > 0) {
            CharSequence cs = tv.getText();
            String text = cs != null ? cs.toString() : "";
            boolean multi = text.indexOf('\n') >= 0;
            float maxLineW = 1f;
            if (multi) {
                for (String line : text.split("\n")) {
                    maxLineW = Math.max(maxLineW, tv.getPaint().measureText(line));
                }
            } else {
                maxLineW = Math.max(1f, tv.getPaint().measureText(text));
            }
            contentW = maxLineW;
            contentH = Math.max(1f, tv.getTextSize() * (multi ? 2.15f : 1.2f));
            localLeft = tv.getWidth() > contentW * 1.4f
                    ? (tv.getWidth() - contentW) / 2f
                    : tv.getTotalPaddingLeft();
            localTop = tv.getTotalPaddingTop();
        } else {
            return out;
        }
        out[5] = localLeft + contentW / 2f;
        out[6] = localTop + contentH / 2f;
        out[0] = tv.getLeft() + out[5] + tv.getTranslationX();
        out[1] = tv.getTop() + out[6] + tv.getTranslationY();
        out[2] = contentW;
        out[3] = contentH;
        out[4] = tv.getTextSize();
        return out;
    }

    private void startAodTextContentMorph(float[] from, String source) {
        if (clockView == null || from == null || from[4] <= 1f) {
            return;
        }
        final float fromCx = from[0];
        final float fromCy = from[1];
        final float fromTextSize = from[4];
        final float fromW = from[2];
        final float fromH = from[3];
        android.view.ViewTreeObserver observer = clockView.getViewTreeObserver();
        if (!observer.isAlive()) {
            clockView.post(() -> runAodTextContentMorph(
                    fromCx, fromCy, fromW, fromH, fromTextSize, source));
            return;
        }
        observer.addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                android.view.ViewTreeObserver obs = clockView.getViewTreeObserver();
                if (obs.isAlive()) {
                    obs.removeOnPreDrawListener(this);
                }
                runAodTextContentMorph(fromCx, fromCy, fromW, fromH, fromTextSize, source);
                return true;
            }
        });
        clockView.invalidate();
    }

    private void runAodTextContentMorph(float fromCx, float fromCy, float fromW, float fromH,
            float fromTextSize, String source) {
        float[] to = snapshotTextContentMorph(clockView);
        if (to[4] <= 1f) {
            clockView.setScaleX(1f);
            clockView.setScaleY(1f);
            clockView.setTranslationX(0f);
            clockView.setTranslationY(0f);
            return;
        }
        float scale = fromTextSize / to[4];
        if (scale < 0.25f) {
            scale = 0.25f;
        } else if (scale > 4.5f) {
            scale = 4.5f;
        }
        float startTx = fromCx - to[0];
        float startTy = fromCy - to[1];
        clockView.animate().cancel();
        clockView.setPivotX(to[5]);
        clockView.setPivotY(to[6]);
        clockView.setScaleX(scale);
        clockView.setScaleY(scale);
        clockView.setTranslationX(startTx);
        clockView.setTranslationY(startTy);
        clockView.animate()
                .scaleX(1f)
                .scaleY(1f)
                .translationX(0f)
                .translationY(0f)
                .setDuration(550L)
                .setInterpolator(new android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f))
                .withEndAction(() -> {
                    clockView.setScaleX(1f);
                    clockView.setScaleY(1f);
                    clockView.setTranslationX(0f);
                    clockView.setTranslationY(0f);
                    PixelAodLog.log("finished AOD size morph source=" + source
                            + " toCx=" + to[0] + " toCy=" + to[1]
                            + " toW=" + to[2] + " toH=" + to[3]
                            + " toTextSize=" + to[4]
                            + " trace=" + currentAodTraceId());
                })
                .start();
        PixelAodLog.log("started AOD size morph source=" + source
                + " fromCx=" + fromCx + " fromCy=" + fromCy
                + " fromW=" + fromW + " fromH=" + fromH
                + " fromTextSize=" + fromTextSize
                + " toCx=" + to[0] + " toCy=" + to[1]
                + " toW=" + to[2] + " toH=" + to[3]
                + " toTextSize=" + to[4]
                + " startScale=" + scale
                + " startTx=" + startTx + " startTy=" + startTy
                + " durationMs=550"
                + " trace=" + currentAodTraceId());
    }

    /**
     * COUI-inspired compact→large entry: large layout is already applied; morph the clock
     * TextView from the compact visual center/size to the large target. Media fades in.
     * Does not scale the whole layer (wrong pivot flew off-screen).
     */
    void startCompactToLargeEntryMorph(long durationMs,
            android.view.animation.Interpolator interpolator, String source) {
        if (!clockPluginManaged) {
            return;
        }
        animate().cancel();
        setScaleX(1f);
        setScaleY(1f);
        if (mediaRow != null && mediaRow.getVisibility() == View.VISIBLE) {
            mediaRow.setAlpha(0f);
            mediaRow.animate().cancel();
            mediaRow.animate()
                    .alpha(MEDIA_ALPHA)
                    .setDuration(durationMs)
                    .setInterpolator(interpolator)
                    .start();
        }
        if (clockView == null) {
            return;
        }
        // Estimated compact clock bounds → current large layout bounds.
        float parentW = getWidth() > 0
                ? getWidth()
                : getResources().getDisplayMetrics().widthPixels;
        float smallTextPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP),
                getResources().getDisplayMetrics());
        float fromW = smallTextPx * 3.4f;
        float fromH = smallTextPx * 1.2f;
        float fromCx = dp(PixelAodVisualStyle.EDGE_DP
                - PixelAodVisualStyle.COMPACT_CLOCK_VISUAL_START_OFFSET_DP) + fromW / 2f;
        float fromCy = dp(SMALL_CLOCK_TOP_DP) + fromH / 2f;
        float[] from = new float[]{fromCx, fromCy, fromW, fromH};
        // Reuse rect morph; duration may differ from default 550.
        final long morphMs = durationMs > 0L ? durationMs : 550L;
        final android.view.animation.Interpolator morphInterp = interpolator != null
                ? interpolator
                : new android.view.animation.PathInterpolator(0.2f, 0f, 0f, 1f);
        clockView.post(() -> {
            if (clockView.getWidth() <= 0 || clockView.getHeight() <= 0) {
                return;
            }
            float toCx = clockView.getLeft() + clockView.getWidth() / 2f;
            float toCy = clockView.getTop() + clockView.getHeight() / 2f;
            float toW = clockView.getWidth();
            float toH = clockView.getHeight();
            float scale = ((from[2] / toW) + (from[3] / toH)) * 0.5f;
            if (scale < 0.2f) {
                scale = 0.2f;
            } else if (scale > 5f) {
                scale = 5f;
            }
            float startTx = from[0] - toCx;
            float startTy = from[1] - toCy;
            clockView.animate().cancel();
            clockView.setPivotX(clockView.getWidth() / 2f);
            clockView.setPivotY(clockView.getHeight() / 2f);
            clockView.setScaleX(scale);
            clockView.setScaleY(scale);
            clockView.setTranslationX(startTx);
            clockView.setTranslationY(startTy);
            clockView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .setDuration(morphMs)
                    .setInterpolator(morphInterp)
                    .withEndAction(() -> {
                        clockView.setScaleX(1f);
                        clockView.setScaleY(1f);
                        clockView.setTranslationX(0f);
                        clockView.setTranslationY(0f);
                        if (mediaRow != null && mediaRow.getVisibility() == View.VISIBLE) {
                            mediaRow.setAlpha(MEDIA_ALPHA);
                        }
                        PixelAodLog.log("finished compact→large AOD entry morph source=" + source
                                + " mediaVisible=" + hasVisibleMediaLine()
                                + " trace=" + currentAodTraceId());
                    })
                    .start();
            PixelAodLog.log("started compact→large AOD entry morph source=" + source
                    + " startScale=" + scale
                    + " startTx=" + startTx
                    + " startTy=" + startTy
                    + " fromCx=" + from[0]
                    + " toCx=" + toCx
                    + " parentW=" + parentW
                    + " durationMs=" + morphMs
                    + " mediaVisible=" + hasVisibleMediaLine()
                    + " trace=" + currentAodTraceId());
        });
    }

    void startClockPluginAodWeightTransition(String source) {
        startClockPluginAodWeightTransition(source, null);
    }

    void startClockPluginAodWeightTransition(String source, Runnable onFinished) {
        if (!clockPluginManaged) {
            return;
        }
        if (clockWeightAnimator != null && clockWeightAnimator.isRunning()) {
            PixelAodLog.log("kept AOD weight transition already running source=" + source
                    + " currentWeight=" + currentClockWeight
                    + " trace=" + currentAodTraceId());
            return;
        }
        int aodTarget = aodClockWeight(getContext());
        if (aodWeightHandoffSettled && Math.abs(currentClockWeight - aodTarget) <= 12) {
            PixelAodLog.log("skipped AOD weight transition source=" + source
                    + " reason=already-settled weight=" + currentClockWeight
                    + " trace=" + currentAodTraceId());
            if (onFinished != null) {
                postOnAnimation(onFinished);
            }
            return;
        }
        aodWeightHandoffSettled = false;
        // Prefer current layer weight (just parked at lockscreen handoff start).
        int from = currentClockWeight > 0
                ? currentClockWeight
                : lockscreenClockWeight(getContext());
        beginLockscreenToAodWeightTransition(source + "#ClockPlugin", onFinished, from);
    }

    boolean isClockPluginWeightTransitionRunning() {
        return clockWeightAnimator != null && clockWeightAnimator.isRunning();
    }

    boolean isAodWeightHandoffSettled() {
        return aodWeightHandoffSettled
                && Math.abs(currentClockWeight - aodClockWeight(getContext())) <= 12;
    }

    /** Call when starting a fresh lockscreen→AOD entry so stale AOD weight is not kept. */
    void clearAodWeightHandoffSettled(String source) {
        if (aodWeightHandoffSettled) {
            PixelAodLog.log("cleared AOD weight handoff settled source=" + source
                    + " previousWeight=" + currentClockWeight
                    + " trace=" + currentAodTraceId());
        }
        aodWeightHandoffSettled = false;
    }

    void applyClockPluginStableAodWeight(String source) {
        if (!clockPluginManaged) {
            return;
        }
        applyStableAodClockWeight(source + "#ClockPlugin");
        aodWeightHandoffSettled = true;
    }

    private void prepareClockPluginAodWeightForHandoff(int handoffStartWeight, String source) {
        int aodTarget = aodClockWeight(getContext());
        int fromWeight = normalizeClockWeight(handoffStartWeight);
        int previousWeight = currentClockWeight;
        // Never cancel a running LS→AOD morph just to re-park at lockscreen weight.
        if (clockWeightAnimator != null && clockWeightAnimator.isRunning()) {
            PixelAodLog.log("skipped prepare AOD weight handoff source=" + source
                    + " reason=weight-anim-running currentWeight=" + previousWeight
                    + " requestedFrom=" + fromWeight
                    + " trace=" + currentAodTraceId());
            return;
        }
        // Settle latch is authoritative. After finished→160, re-present must not park 340.
        // When latch is clear (fresh LS→AOD after presentLockscreen), parking is required
        // even if the AOD layer still holds residual 160 from the previous session.
        if (aodWeightHandoffSettled) {
            PixelAodLog.log("skipped prepare AOD weight handoff source=" + source
                    + " reason=settled weight=" + previousWeight
                    + " requestedFrom=" + fromWeight
                    + " aodTarget=" + aodTarget
                    + " trace=" + currentAodTraceId());
            return;
        }
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        aodWeightHandoffSettled = false;
        setClockWeight(fromWeight, true);
        PixelAodLog.log("prepared ClockPlugin AOD weight handoff source=" + source
                + " fromWeight=" + fromWeight
                + " toWeight=" + aodTarget
                + " previousWeight=" + previousWeight
                + " alpha=" + getAlpha()
                + " visibility=" + getVisibility()
                + " trace=" + currentAodTraceId());
    }

    void refreshClockPluginAodContent(String source) {
        if (!clockPluginManaged) {
            return;
        }
        applyMaterialColors();
        updateTime();
        // Icon rebuild calls applyClockMode; safe during weight morph (uses currentClockWeight).
        // Never call applyStable / prepare weight here.
        rebuildNotificationIcons(source + "#ClockPlugin");
        updateMediaLine(source + "#ClockPlugin");
        requestAodFrameRefresh(source + "#ClockPlugin");
    }

    void setClockPluginLayerVisible(boolean visible) {
        if (!clockPluginManaged) {
            return;
        }
        if (!visible) {
            // Leaving AOD surface (unlock / hide). Cancel morph; settle cleared so the next
            // LS→AOD entry always re-parks and animates.
            if (clockWeightAnimator != null) {
                clockWeightAnimator.cancel();
                clockWeightAnimator = null;
            }
            aodWeightHandoffSettled = false;
        }
        setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        if (!visible) {
            resetBurnInTranslation();
        }
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

    static void refreshAllForNativeAodTick(String source) {
        for (PixelAodClockView view : INSTANCES) {
            if (view != null) {
                view.refreshForNativeAodTick(source);
            }
        }
    }

    private void refreshPresentation() {
        applyMaterialColors();
        updateTime();
        rebuildNotificationIcons("refreshPresentation");
        updateMediaLine("refreshPresentation");
        requestAodFrameRefresh("refreshPresentation");
    }

    private void registerTimeReceiver() {
        if (timeReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        try {
            getContext().registerReceiver(timeChangedReceiver, filter);
            timeReceiverRegistered = true;
            PixelAodLog.log("registered Pixel AOD time-change receiver trace=" + currentAodTraceId()
                    + " state={" + describeAodState(getContext()) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to register Pixel AOD time receiver", t);
        }
    }

    private void unregisterTimeReceiver() {
        if (!timeReceiverRegistered) {
            return;
        }
        try {
            getContext().unregisterReceiver(timeChangedReceiver);
        } catch (Throwable t) {
            PixelAodLog.log("failed to unregister Pixel AOD time receiver", t);
        }
        timeReceiverRegistered = false;
    }

    private void scheduleEntrySecondRefresh() {
        mainHandler().postDelayed(() -> {
            if (!running) {
                return;
            }
            refreshForNativeAodTick("aod-entry-delayed");
        }, AOD_ENTRY_SECOND_REFRESH_DELAY_MS);
    }

    private void refreshForNativeAodTick(String source) {
        logAodPhaseIfChanged(getContext(), source + "#nativeTick");
        if (!running) {
            PixelAodLog.log("ignored native AOD refresh while stopped trace="
                    + currentAodTraceId() + " source=" + source
                    + " state={" + describeAodState(getContext()) + "}");
            return;
        }
        if (clockPluginManaged) {
            refreshClockPluginAodContent(source);
            return;
        }
        updateTime();
        updateMediaLine(source);
        updateAodVisibility(source);
        requestAodFrameRefresh(source);
        PixelAodLog.log("handled native AOD refresh trace=" + currentAodTraceId()
                + " source=" + source
                + " state={" + describeAodState(getContext()) + "}");
    }

    private static void refreshInstancesFromNotificationSnapshot(String source) {
        Runnable task = () -> {
            int count = 0;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    count++;
                    view.updateTime();
                    view.rebuildNotificationIcons("snapshot-" + source);
                    view.updateMediaLine("snapshot-" + source);
                    view.updateAodVisibility("snapshot-" + source);
                    view.requestAodFrameRefresh("snapshot-" + source);
                }
            }
            PixelAodLog.log("refreshed Pixel AOD instances trace=" + currentAodTraceId()
                    + " source=" + source + " count=" + count
                    + " state={" + describeAodState(appContext) + "}");
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().post(task);
        }
    }

    static void forceRefreshNotificationIcons(String source) {
        Runnable task = () -> {
            int count = 0;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    count++;
                    view.lastNotificationIconSignature = "";
                    view.rebuildNotificationIcons("force-" + source);
                    view.updateAodVisibility("force-" + source);
                    view.requestAodFrameRefresh("force-" + source);
                }
            }
            PixelAodLog.log("force refreshed Pixel AOD notification icons trace="
                    + currentAodTraceId()
                    + " source=" + source
                    + " count=" + count
                    + " state={" + describeAodState(appContext) + "}");
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().post(task);
        }
    }

    private TextView makeClock(Context context) {
        int weight = aodClockWeight(context);
        TextView textView = new TextView(context);
        currentClockWeight = weight;
        textView.setTextColor(resolveMaterialClockColor(context));
        applySharedClockTextStyle(textView, context, weight,
                scaledClockTextDp(context, LARGE_CLOCK_TEXT_DP), false);
        PixelAodLog.log("applied Pixel AOD clock style source=init weight=" + weight
                + " variation=" + sharedClockFontVariationSettings(weight)
                + " typeface=builder"
                + " visualProfile={" + PixelAodVisualStyle.aodProfile(context, weight) + "}");
        textView.setAlpha(AOD_CLOCK_ALPHA);
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
                    Settings.Secure.getUriFor(AodNotificationPipeline.LOCK_SCREEN_SHOW_NOTIFICATIONS),
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
        if (clockPluginManaged) {
            return;
        }
        logAodPhaseIfChanged(getContext(), source + "#updateAodVisibility");
        boolean visible = shouldDrawAodOverlay(source);
        String trace = currentAodTraceId();
        if (!visible && PANEL_HANDOFF_GATE.shouldBlockPresentation(trace)) {
            cancelPanelHandoffPresentation(source + "#policy-denied");
        }
        boolean panelHandoffBlocked = visible
                && PANEL_HANDOFF_GATE.shouldBlockPresentation(trace);
        boolean briefDisplay = visible && isBriefAodDisplay(getContext());
        int desiredVisibility = visible ? View.VISIBLE : View.GONE;
        boolean visibilityChanged = getVisibility() != desiredVisibility;
        PixelAodLog.log("AOD overlay visibility decision trace=" + currentAodTraceId()
                + " source=" + source
                + " visible=" + visible
                + " panelHandoffBlocked=" + panelHandoffBlocked
                + " desiredVisibility=" + desiredVisibility
                + " currentVisibility=" + getVisibility()
                + " state={" + describeAodState(getContext()) + "}");
        if (visibilityChanged) {
            if (visible) {
                refreshAodContentBeforeVisible(source);
            }
            setVisibility(desiredVisibility);
            if (visible) {
                if (briefDisplay) {
                    applyStableAodClockWeight(source + "#brief");
                } else if (PixelLockscreenClockView.shouldAnimateLockscreenToAodTransition()) {
                    beginLockscreenToAodWeightTransition(source);
                } else {
                    applyStableAodClockWeight(source);
                }
            } else {
                if (clockWeightAnimator != null) {
                    clockWeightAnimator.cancel();
                    clockWeightAnimator = null;
                }
            }
            PixelAodLog.log("Pixel AOD overlay visibility="
                    + (visible ? "visible" : "hidden")
                    + " source=" + source
                    + " trace=" + currentAodTraceId()
                    + " state={" + describeAodState(getContext()) + "}");
        }
        if (visible) {
            setAlpha(panelHandoffBlocked ? 0f : 1f);
            if (!visibilityChanged && briefDisplay) {
                applyStableAodClockWeight(source + "#brief-stable");
            }
            if (!panelHandoffBlocked) {
                markRecentAodOverlayVisible(source);
            }
            applyBurnInTranslation();
        } else {
            setAlpha(1f);
            resetBurnInTranslation();
        }
    }

    static void beginPanelHandoffPresentation(Context context, String source) {
        Context requestedContext = context != null ? context : appContext;
        long requestedAt = SystemClock.uptimeMillis();
        Runnable task = () -> beginPanelHandoffPresentationOnMain(
                requestedContext, source, requestedAt);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().postAtFrontOfQueue(task);
        }
    }

    private static void beginPanelHandoffPresentationOnMain(
            Context context, String source, long requestedAt) {
        String trace = currentAodTraceId();
        String normalizedSource = TextUtils.isEmpty(source) ? "unknown" : source;
        if (context == null) {
            logPanelHandoffSkip(normalizedSource, trace, "no-context");
            return;
        }
        if (isDeviceInteractive(context)) {
            logPanelHandoffSkip(normalizedSource, trace, "interactive");
            return;
        }
        if (isProximityNear()) {
            cancelPanelHandoffPresentation(normalizedSource + "#proximity-near");
            logPanelHandoffSkip(normalizedSource, trace, "proximity-near");
            return;
        }
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                evaluateAodPolicy(context, normalizedSource + "#panel-handoff");
        if (!decision.shouldDrawPixelOverlay) {
            cancelPanelHandoffPresentation(normalizedSource + "#policy-denied");
            logPanelHandoffSkip(normalizedSource, trace,
                    "policy-" + decision.drawReason);
            return;
        }
        boolean alreadyBlocked = PANEL_HANDOFF_GATE.shouldBlockPresentation(trace);
        boolean hasVisibleOverlay = false;
        for (PixelAodClockView view : INSTANCES) {
            if (view != null
                    && !view.isClockPluginManaged()
                    && view.getVisibility() == View.VISIBLE) {
                hasVisibleOverlay = true;
                break;
            }
        }
        if (!alreadyBlocked && !hasVisibleOverlay) {
            logPanelHandoffSkip(normalizedSource, trace, "no-visible-overlay");
            return;
        }

        PanelHandoffGate.OpenResult opened =
                PANEL_HANDOFF_GATE.openOrExtend(trace, requestedAt);
        if (!opened.accepted) {
            logPanelHandoffSkip(normalizedSource, trace, "trace-already-finished");
            return;
        }
        int blockedViews = 0;
        for (PixelAodClockView view : INSTANCES) {
            if (view != null
                    && !view.isClockPluginManaged()
                    && view.getVisibility() == View.VISIBLE) {
                view.setAlpha(0f);
                blockedViews++;
            }
        }
        long now = SystemClock.uptimeMillis();
        long remainingMillis = Math.max(0L, opened.deadlineMillis - now);
        String action = opened.extended ? "extended"
                : opened.replaced ? "replaced" : "opened";
        PixelAodLog.log("PanelHandoffGate " + action
                + " source=" + normalizedSource
                + " trace=" + opened.traceId
                + " generation=" + opened.generation
                + " openedAt=" + opened.openedAtMillis
                + " deadline=" + opened.deadlineMillis
                + " requestedAt=" + requestedAt
                + " remainingMs=" + remainingMillis
                + " blockedViews=" + blockedViews
                + " state={" + describeAodState(context) + "}");
        PixelAodHook.refreshKnownAodHostVisibility(
                normalizedSource + "#panel-handoff-stock-suppression");
        Runnable previousCompletion = panelHandoffCompletionRunnable;
        if (previousCompletion != null) {
            mainHandler().removeCallbacks(previousCompletion);
        }
        Runnable completion = new Runnable() {
            @Override
            public void run() {
                if (panelHandoffCompletionRunnable != this) {
                    return;
                }
                panelHandoffCompletionRunnable = null;
                preparePanelHandoffReveal(opened, normalizedSource);
            }
        };
        panelHandoffCompletionRunnable = completion;
        mainHandler().postDelayed(completion, remainingMillis);
    }

    private static void preparePanelHandoffReveal(
            PanelHandoffGate.OpenResult opened, String source) {
        String currentTrace = currentAodTraceId();
        if (!TextUtils.equals(opened.traceId, currentTrace)) {
            boolean cancelled = PANEL_HANDOFF_GATE.cancelIfCurrent(
                    opened.traceId, opened.generation);
            if (cancelled) {
                restorePanelHandoffPresentationAlpha();
            }
            PixelAodLog.log("PanelHandoffGate stale-generation skip"
                    + " source=" + source
                    + " expectedTrace=" + opened.traceId
                    + " currentTrace=" + currentTrace
                    + " generation=" + opened.generation
                    + " cancelled=" + cancelled
                    + " state={" + describeAodState(appContext) + "}");
            return;
        }
        if (!PANEL_HANDOFF_GATE.shouldBlockPresentation(currentTrace)) {
            PixelAodLog.log("PanelHandoffGate stale-generation skip"
                    + " source=" + source
                    + " trace=" + currentTrace
                    + " generation=" + opened.generation
                    + " reason=inactive"
                    + " state={" + describeAodState(appContext) + "}");
            return;
        }

        PixelAodClockView animationAnchor = null;
        for (PixelAodClockView view : INSTANCES) {
            if (view != null
                    && !view.isClockPluginManaged()
                    && view.getVisibility() == View.VISIBLE
                    && view.isAttachedToWindow()) {
                animationAnchor = view;
                break;
            }
        }
        if (animationAnchor == null) {
            boolean cancelled = PANEL_HANDOFF_GATE.cancelIfCurrent(
                    opened.traceId, opened.generation);
            if (cancelled) {
                restorePanelHandoffPresentationAlpha();
            }
            PixelAodLog.log("PanelHandoffGate cancelled"
                    + " source=" + source
                    + " trace=" + currentTrace
                    + " generation=" + opened.generation
                    + " reason=no-animation-anchor"
                    + " state={" + describeAodState(appContext) + "}");
            return;
        }

        int refreshedViews = 0;
        for (PixelAodClockView view : INSTANCES) {
            if (view == null
                    || view.isClockPluginManaged()
                    || view.getVisibility() != View.VISIBLE) {
                continue;
            }
            view.refreshAodContentBeforeVisible(source + "#panel-handoff");
            refreshedViews++;
        }
        final int finalRefreshedViews = refreshedViews;
        Runnable reveal = () -> completePanelHandoffReveal(
                opened, source, finalRefreshedViews);
        animationAnchor.postOnAnimation(reveal);
    }

    private static void completePanelHandoffReveal(
            PanelHandoffGate.OpenResult opened, String source, int refreshedViews) {
        long now = SystemClock.uptimeMillis();
        String currentTrace = currentAodTraceId();
        if (!TextUtils.equals(opened.traceId, currentTrace)
                || !PANEL_HANDOFF_GATE.completeIfCurrent(
                        opened.traceId, opened.generation, now)) {
            PixelAodLog.log("PanelHandoffGate stale-generation skip"
                    + " source=" + source
                    + " expectedTrace=" + opened.traceId
                    + " currentTrace=" + currentTrace
                    + " generation=" + opened.generation
                    + " reason=reveal-not-current"
                    + " state={" + describeAodState(appContext) + "}");
            return;
        }

        int revealedViews = 0;
        for (PixelAodClockView view : INSTANCES) {
            if (view != null && !view.isClockPluginManaged()) {
                view.updateAodVisibility(source + "#panel-handoff-complete");
                view.requestAodFrameRefresh(source + "#panel-handoff-complete");
                if (view.getVisibility() == View.VISIBLE && view.getAlpha() > 0f) {
                    revealedViews++;
                }
            }
        }
        PixelAodHook.refreshKnownAodHostVisibility(
                source + "#panel-handoff-complete");
        PixelAodLog.log("PanelHandoffGate completed"
                + " source=" + source
                + " trace=" + opened.traceId
                + " generation=" + opened.generation
                + " openToRevealMs=" + Math.max(0L, now - opened.openedAtMillis)
                + " refreshedViews=" + refreshedViews
                + " revealedViews=" + revealedViews
                + " state={" + describeAodState(appContext) + "}");
    }

    private static void cancelPanelHandoffPresentation(String source) {
        cancelPanelHandoffPresentation(source, true);
    }

    private static void cancelPanelHandoffPresentation(String source, boolean restoreAlpha) {
        if (!PANEL_HANDOFF_GATE.cancel()) {
            return;
        }
        Runnable pendingCompletion = panelHandoffCompletionRunnable;
        panelHandoffCompletionRunnable = null;
        if (pendingCompletion != null) {
            mainHandler().removeCallbacks(pendingCompletion);
        }
        if (restoreAlpha) {
            restorePanelHandoffPresentationAlpha();
        }
        PixelAodLog.log("PanelHandoffGate cancelled"
                + " source=" + source
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(appContext) + "}");
    }

    private static void restorePanelHandoffPresentationAlpha() {
        Runnable task = () -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null && !view.isClockPluginManaged()) {
                    view.setAlpha(1f);
                }
            }
        };
        if (Looper.myLooper() == Looper.getMainLooper()) {
            task.run();
        } else {
            mainHandler().postAtFrontOfQueue(task);
        }
    }

    private static void logPanelHandoffSkip(String source, String trace, String reason) {
        PixelAodLog.log("PanelHandoffGate skipped"
                + " source=" + source
                + " trace=" + trace
                + " reason=" + reason
                + " state={" + describeAodState(appContext) + "}");
    }

    private void refreshAodContentBeforeVisible(String source) {
        updateTime();
        rebuildNotificationIcons(source + "#before-visible");
        updateMediaLine(source + "#before-visible");
        requestAodFrameRefresh(source + "#before-visible");
        PixelAodLog.log("refreshed AOD content before visible frame trace=" + currentAodTraceId()
                + " source=" + source
                + " clock=" + String.valueOf(clockView.getText()).replace('\n', '/')
                + " visibility=" + getVisibility()
                + " shown=" + isShown()
                + " state={" + describeAodState(getContext()) + "}");
    }

    private void scheduleAodVisibilityUpdate(String source, long delayMillis) {
        mainHandler().postDelayed(() -> updateAodVisibility(source + "+" + delayMillis),
                delayMillis);
    }

    private static void scheduleAodVisibilityUpdateForAll(String source, long delayMillis) {
        mainHandler().postDelayed(() -> {
            String delayedSource = source + "+" + delayMillis;
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateAodVisibility(delayedSource);
                    view.requestAodFrameRefresh(delayedSource);
                }
            }
            PixelAodHook.refreshKnownAodHostVisibility(delayedSource);
            ClockPluginHostController.refreshAll(delayedSource + "#policy");
        }, delayMillis);
    }

    private boolean shouldDrawAodOverlay(String source) {
        String trace = ensureAodTrace(source);
        boolean expandedShadeBlocked = isInsideExpandedSystemShade();
        OosAodLifecycleAdapter.AodPolicyDecision decision =
                evaluateAodPolicy(getContext(), source,
                        isProximityNear(), expandedShadeBlocked);
        if (expandedShadeBlocked && !decision.shouldDrawPixelOverlay) {
            PixelAodLog.log("suppressed Pixel AOD overlay in expanded shade trace=" + trace
                    + " source=" + source
                    + " state={" + describeAodState(getContext()) + "}");
        }
        PixelAodLog.log("AOD overlay decision trace=" + trace
                + " source=" + source
                + " visible=" + decision.shouldDrawPixelOverlay
                + " reason=" + decision.drawReason
                + " shouldKeepNativeDozeAlive=" + decision.shouldKeepNativeDozeAlive
                + " shouldSuppressStockAodViews=" + decision.shouldSuppressStockAodViews
                + " shouldAllowNativeHideCallbacks=" + decision.shouldAllowNativeHideCallbacks
                + " state={" + describeAodState(getContext()) + "}");
        return decision.shouldDrawPixelOverlay;
    }

    private static boolean isBriefAodDisplay(Context context) {
        if (context == null) {
            return false;
        }
        AodLifecycleState state = currentAodLifecycleState(context);
        if (!state.triggerBriefActive) {
            return false;
        }
        String displayMode = aodDisplayMode(context);
        return isTriggerOnlyAodMode(displayMode)
                || (isContinuousAodAllowedByMode(displayMode) && !isWithinAodSchedule(context));
    }

    private boolean isPowerPolicyAllowingAod(String source, String trace) {
        return isPowerPolicyAllowingAod(getContext(), source, trace, true);
    }

    private static boolean isPowerPolicyAllowingAod(
            Context context, String source, String trace, boolean logAllowed) {
        OosAodLifecycleAdapter.PowerPolicyDecision decision =
                evaluateAndLogPowerPolicy(context, source, trace, logAllowed);
        BatteryStatus batteryStatus = readBatteryStatus(context);
        if (!decision.allowsDisplay) {
            PixelAodLog.log("AOD overlay decision trace=" + trace
                    + " source=" + source
                    + " visible=false reason=" + decision.reason
                    + " powerCategory=" + decision.categoryLabel
                    + " threshold=" + decision.thresholdPercent
                    + " battery={" + batteryStatus.describeForLog() + "}"
                    + " state={" + describeAodState(context) + "}");
            return false;
        }
        if (logAllowed) {
            PixelAodLog.log("AOD power policy allows overlay trace=" + trace
                    + " source=" + source
                    + " reason=" + decision.reason
                    + " powerCategory=" + decision.categoryLabel
                    + " powerSave=" + isPowerSaveMode(context)
                    + " battery={" + batteryStatus.describeForLog() + "}"
                    + " state={" + describeAodState(context) + "}");
        }
        return true;
    }

    private static OosAodLifecycleAdapter.PowerPolicyDecision evaluateAndLogPowerPolicy(
            Context context, String source, String trace, boolean logAllowed) {
        boolean powerSaveMode = isPowerSaveMode(context);
        BatteryStatus batteryStatus = readBatteryStatus(context);
        OosAodLifecycleAdapter.PowerPolicyDecision decision =
                OosAodLifecycleAdapter.evaluatePowerPolicy(
                        powerSaveMode,
                        batteryStatus.valid,
                        batteryStatus.lowBattery,
                        batteryStatus.charging,
                        batteryStatus.levelPercent,
                        LOW_BATTERY_AOD_SUPPRESS_THRESHOLD_PERCENT);
        OosAodLifecycleAdapter.recordPowerPolicyDecision(
                decision,
                source,
                powerSaveMode,
                batteryStatus.describeForLog(),
                trace,
                describeAodState(context));
        return decision;
    }

    private boolean isWithinAodSchedule() {
        return isWithinAodSchedule(getContext());
    }

    private static boolean isWithinAodSchedule(Context context) {
        Context contextApp = context != null ? context.getApplicationContext() : null;
        Context ctx = contextApp != null ? contextApp : context;
        if (ctx == null) {
            return true;
        }
        boolean enabled = PixelAodSettings.getBoolean(ctx, PixelAodSettings.KEY_AOD_SCHEDULE_ENABLED, false);
        if (!enabled) {
            synchronized (PixelAodClockView.class) {
                cachedScheduleCheckedAt = 0L;
                cachedScheduleKey = "";
                cachedScheduleResult = true;
            }
            PixelAodLog.log("AOD schedule check disabled result=true state={"
                    + describeAodState(ctx) + "}");
            return true;
        }
        String startTimeStr = PixelAodSettings.getString(ctx, PixelAodSettings.KEY_AOD_SCHEDULE_START_TIME, "22:00");
        String endTimeStr = PixelAodSettings.getString(ctx, PixelAodSettings.KEY_AOD_SCHEDULE_END_TIME, "07:00");
        long now = SystemClock.uptimeMillis();
        String key = startTimeStr + "|" + endTimeStr;
        synchronized (PixelAodClockView.class) {
            if (key.equals(cachedScheduleKey)
                    && now - cachedScheduleCheckedAt < SCHEDULE_CACHE_MILLIS) {
                PixelAodLog.log("AOD schedule cache hit key=" + key
                        + " result=" + cachedScheduleResult
                        + " ageMs=" + (now - cachedScheduleCheckedAt)
                        + " state={" + describeAodState(ctx) + "}");
                return cachedScheduleResult;
            }
        }

        try {
            String[] startParts = startTimeStr.split(":");
            String[] endParts = endTimeStr.split(":");
            if (startParts.length < 2 || endParts.length < 2) {
                return true;
            }
            int startMins = Integer.parseInt(startParts[0].trim()) * 60 + Integer.parseInt(startParts[1].trim());
            int endMins = Integer.parseInt(endParts[0].trim()) * 60 + Integer.parseInt(endParts[1].trim());

            Calendar calendar = Calendar.getInstance();
            int curMins = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);

            boolean result;
            if (startMins == endMins) {
                result = true;
            } else if (startMins < endMins) {
                result = curMins >= startMins && curMins < endMins;
            } else {
                result = curMins >= startMins || curMins < endMins;
            }
            synchronized (PixelAodClockView.class) {
                cachedScheduleCheckedAt = now;
                cachedScheduleKey = key;
                cachedScheduleResult = result;
            }
            PixelAodLog.log("AOD schedule check: start=" + startTimeStr + " (" + startMins
                    + "m), end=" + endTimeStr + " (" + endMins + "m), current="
                    + curMins + "m, result=" + result
                    + " key=" + key
                    + " state={" + describeAodState(ctx) + "}");
            return result;
        } catch (Throwable t) {
            PixelAodLog.e("failed to parse AOD schedule times: start=" + startTimeStr + " end=" + endTimeStr, t);
            return true;
        }
    }

    private boolean isInsideExpandedSystemShade() {
        if (!ENABLE_AOD_SHADE_TREE_GUARD) {
            return false;
        }
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

    private static boolean isPowerSaveMode(Context context) {
        if (context == null) {
            return false;
        }
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isPowerSaveMode();
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
                updateMediaLine("media-manager-null");
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
                        notePlaybackState(controller, state);
                        updateMediaLine("media-playback");
                    }

                    @Override
                    public void onMetadataChanged(MediaMetadata metadata) {
                        noteMediaContentActivity(controller.getPackageName(), "media-metadata");
                        PixelAodLog.log("AOD media metadata callback pkg=" + controller.getPackageName()
                                + " text=" + describeMediaTextForLog(formatMediaText(metadata))
                                + " state=" + playbackStateName(safePlaybackState(controller))
                                + " trace=" + currentAodTraceId());
                        updateMediaLine("media-metadata");
                    }

                    @Override
                    public void onSessionDestroyed() {
                        clearInactiveMediaTracking(controller, "media-destroyed");
                        refreshActiveMediaControllers();
                        updateMediaLine("media-destroyed");
                    }
                };
                try {
                    notePlaybackState(controller, controller.getPlaybackState());
                    controller.registerCallback(callback, mainHandler());
                    mediaCallbacks.put(controller, callback);
                } catch (Throwable t) {
                    PixelAodLog.log("failed to watch active media controller for AOD", t);
                }
            }
        }
        updateMediaLine("media-controllers");
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
        updateMediaLine("direct");
    }

    private void updateMediaLine(String source) {
        MediaController controller = chooseVisibleMediaController();
        if (controller == null) {
            clearMediaLine(source);
            return;
        }
        StatusBarNotification mediaNotification = findMediaNotification(controller);
        String notificationText = formatMediaNotificationText(mediaNotification);
        String mediaText = !TextUtils.isEmpty(notificationText)
                ? notificationText
                : formatMediaText(controller.getMetadata());
        String mediaKey = mediaNotification != null ? mediaNotification.getKey() : "";
        String iconSignature = mediaKey + "|" + controller.getPackageName();
        if (TextUtils.equals(lastMediaLineText, mediaText)
                && TextUtils.equals(lastMediaLineKey, mediaKey)
                && TextUtils.equals(lastMediaIconSignature, iconSignature)) {
            // Content is unchanged, but the row may have been hidden by a prior
            // pause/stop. Re-assert visibility so resuming the same track shows again.
            if (!TextUtils.isEmpty(mediaText) && mediaRow.getVisibility() != View.VISIBLE) {
                mediaRow.setVisibility(View.VISIBLE);
                requestAodFrameRefresh(source + "#media-visible");
                PixelAodHook.requestNativeAodFrameRefreshKick(source + "#media-visible");
            }
            return;
        }
        PixelAodLog.log("AOD media line update source=" + source
                + " pkg=" + controller.getPackageName()
                + " state=" + playbackStateName(safePlaybackState(controller))
                + " notificationKey=" + mediaKey
                + " oldText=" + describeMediaTextForLog(lastMediaLineText)
                + " newText=" + describeMediaTextForLog(mediaText)
                + " expired=" + isExpiredInactiveMediaPackage(controller.getPackageName())
                + " trace=" + currentAodTraceId());
        lastMediaLineText = mediaText;
        lastMediaLineKey = mediaKey;
        lastMediaIconSignature = iconSignature;
        if (TextUtils.isEmpty(mediaText)) {
            clearMediaLine(source);
            return;
        }
        currentMediaNotificationKey = mediaNotification != null ? mediaNotification.getKey() : null;
        updateMediaIcon(controller, mediaNotification);
        mediaView.setText(mediaText);
        mediaRow.setVisibility(View.VISIBLE);
        rebuildNotificationIcons(source);
        requestAodFrameRefresh(source + "#media");
        PixelAodHook.requestNativeAodFrameRefreshKick(source + "#media");
    }

    private void updateMediaIcon(MediaController controller, StatusBarNotification notification) {
        Drawable drawable = loadMediaNotificationIcon(getContext(), controller, notification);
        mediaIconView.setImageDrawable(drawable);
        mediaIconView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) mediaView.getLayoutParams();
        int leftMargin = drawable != null ? dp(MEDIA_ICON_SPACING_DP) : 0;
        if (params.leftMargin != leftMargin) {
            params.leftMargin = leftMargin;
            mediaView.setLayoutParams(params);
        }
    }

    private void clearMediaLine(String source) {
        boolean hadMedia = !TextUtils.isEmpty(lastMediaLineText)
                || mediaRow.getVisibility() == View.VISIBLE
                || currentMediaNotificationKey != null;
        if (hadMedia) {
            PixelAodLog.log("AOD media line cleared source=" + source
                    + " oldText=" + describeMediaTextForLog(lastMediaLineText)
                    + " notificationKey=" + currentMediaNotificationKey
                    + " expiredMedia=" + expiredInactiveMediaPackageSignature()
                    + " trace=" + currentAodTraceId());
        }
        currentMediaNotificationKey = null;
        // Reset the dedupe signature so that resuming the SAME track later is not
        // mistaken for an unchanged state and silently skipped (which previously
        // left the media row hidden until the player was swiped away and reopened).
        lastMediaLineText = "";
        lastMediaLineKey = "";
        lastMediaIconSignature = "";
        mediaView.setText("");
        mediaIconView.setImageDrawable(null);
        mediaRow.setVisibility(View.GONE);
        rebuildNotificationIcons(source);
        requestAodFrameRefresh(source + "#media-cleared");
        if (hadMedia) {
            PixelAodHook.requestNativeAodFrameRefreshKick(source + "#media-cleared");
        }
    }

    private static boolean isStoppedPlaybackState(PlaybackState state) {
        if (state == null) {
            return false;
        }
        int st = state.getState();
        return st == PlaybackState.STATE_STOPPED || st == PlaybackState.STATE_ERROR;
    }

    private static boolean isPlayingPlaybackState(PlaybackState state) {
        if (state == null) {
            return false;
        }
        int st = state.getState();
        return st == PlaybackState.STATE_PLAYING
                || st == PlaybackState.STATE_BUFFERING
                || st == PlaybackState.STATE_CONNECTING
                || st == PlaybackState.STATE_FAST_FORWARDING
                || st == PlaybackState.STATE_REWINDING
                || st == PlaybackState.STATE_SKIPPING_TO_NEXT
                || st == PlaybackState.STATE_SKIPPING_TO_PREVIOUS
                || st == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM;
    }

    private static boolean isPausedPlaybackState(PlaybackState state) {
        if (state == null) {
            return false;
        }
        return state.getState() == PlaybackState.STATE_PAUSED;
    }

    private static boolean isNonePlaybackState(PlaybackState state) {
        if (state == null) {
            return false;
        }
        return state.getState() == PlaybackState.STATE_NONE;
    }

    private static PlaybackState safePlaybackState(MediaController controller) {
        if (controller == null) {
            return null;
        }
        try {
            return controller.getPlaybackState();
        } catch (Throwable ignored) {
            return null;
        }
    }

    // True when the controller can produce a non-empty media line (either from a
    // matching media notification or from its own metadata).
    private boolean hasDisplayableMedia(MediaController controller) {
        if (controller == null) {
            return false;
        }
        StatusBarNotification notification = findMediaNotification(controller);
        if (!TextUtils.isEmpty(formatMediaNotificationText(notification))) {
            return true;
        }
        try {
            return !TextUtils.isEmpty(formatMediaText(controller.getMetadata()));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean shouldKeepInactiveMediaController(MediaController controller, PlaybackState state) {
        if (controller == null || isStoppedPlaybackState(state) || !hasDisplayableMedia(controller)) {
            if (isStoppedPlaybackState(state)) {
                clearInactiveMediaTracking(controller, "media-stopped");
            }
            return false;
        }
        if (isExpiredInactiveMediaPackage(controller.getPackageName())) {
            PixelAodLog.log("AOD media inactive suppressed pkg=" + controller.getPackageName()
                    + " key=" + mediaControllerKey(controller)
                    + " reason=package-timeout"
                    + " state=" + playbackStateName(state)
                    + " trace=" + currentAodTraceId());
            return false;
        }
        if (isPausedPlaybackState(state)) {
            return isInactiveMediaWithinTimeout(controller, state, "paused");
        }
        if (isNonePlaybackState(state) || state == null) {
            return findMediaNotification(controller) != null
                    && isInactiveMediaWithinTimeout(controller, state, "idle");
        }
        return false;
    }

    private MediaController chooseVisibleMediaController() {
        // Prefer an actively playing session that also has displayable content.
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
            if (isPlayingPlaybackState(state)) {
                clearExpiredInactiveMediaPackage(controller.getPackageName(), "media-playing");
                if (hasDisplayableMedia(controller)) {
                    return controller;
                }
            }
        }
        // Otherwise keep a paused session, or an idle/null-state session only while
        // it still has a matching notification. This avoids stale metadata lingering
        // after the player has already torn down its media notification.
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
            if (shouldKeepInactiveMediaController(controller, state)) {
                return controller;
            }
        }
        return null;
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

    private static String describeMediaTextForLog(String text) {
        if (TextUtils.isEmpty(text)) {
            return "empty";
        }
        return "len=" + text.length() + ",hash=" + text.hashCode();
    }

    private static String formatMediaNotificationText(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return "";
        }
        try {
            Bundle extras = sbn.getNotification().extras;
            if (extras == null) {
                return "";
            }
            CharSequence title = firstNonEmpty(
                    extras.getCharSequence(Notification.EXTRA_TITLE),
                    extras.getCharSequence(Notification.EXTRA_TITLE_BIG),
                    extras.getCharSequence(Notification.EXTRA_TEXT));
            CharSequence artist = firstNonEmpty(
                    extras.getCharSequence(Notification.EXTRA_TEXT),
                    extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                    extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
            if (TextUtils.isEmpty(title)) {
                return "";
            }
            if (TextUtils.equals(title, artist) || TextUtils.isEmpty(artist)) {
                return title.toString();
            }
            return title + " - " + artist;
        } catch (Throwable ignored) {
            return "";
        }
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
        rebuildNotificationIcons("direct");
    }

    private void rebuildNotificationIcons(String source) {
        List<StatusBarNotification> notifications = currentNotifications();
        String signature = AodNotificationPipeline.notificationSignature(
                notifications.toArray(new StatusBarNotification[0]))
                + "|media=" + currentMediaNotificationKey
                + "|expiredMedia=" + expiredInactiveMediaPackageSignature();
        if (TextUtils.equals(lastNotificationIconSignature, signature)) {
            PixelAodLog.log("skipped native AOD notification icon rebuild trace=" + currentAodTraceId()
                    + " source=" + source
                    + " reason=unchanged-signature"
                    + " signature=" + signature
                    + " state={" + describeAodState(getContext()) + "}");
            return;
        }
        lastNotificationIconSignature = signature;
        notificationIconRow.removeAllViews();
        notificationOverflowView = null;
        if (notifications.isEmpty()) {
            notificationIconRow.setVisibility(View.GONE);
            applyClockMode(false);
            PixelAodLog.log("rebuilt native AOD notification icons trace=" + currentAodTraceId()
                    + " source=" + source
                    + " input=0 emitted=0"
                    + " state={" + describeAodState(getContext()) + "}");
            return;
        }
        int loadFailures = 0;
        int skippedMedia = 0;
        HashSet<String> seenIconKeys = new HashSet<>();
        ArrayList<String> loadedIconKeys = new ArrayList<>();
        ArrayList<Drawable> loadedIcons = new ArrayList<>();
        for (StatusBarNotification sbn : notifications) {
            if (sbn == null) {
                continue;
            }
            if (isNotificationForCurrentMedia(sbn)) {
                skippedMedia++;
                PixelAodLog.log("skipped AOD notification icon trace=" + currentAodTraceId()
                        + " source=" + source
                        + " reason=current-media pkg=" + sbn.getPackageName()
                        + " key=" + sbn.getKey()
                        + " state={" + describeAodState(getContext()) + "}");
                continue;
            }
            String dedupeKey = AodNotificationPipeline.notificationIconDedupeKey(sbn);
            if (!seenIconKeys.add(dedupeKey)) {
                PixelAodLog.log("skipped AOD notification icon trace=" + currentAodTraceId()
                        + " source=" + source
                        + " reason=duplicate-icon-key pkg=" + sbn.getPackageName()
                        + " dedupeKey=" + dedupeKey
                        + " key=" + sbn.getKey()
                        + " state={" + describeAodState(getContext()) + "}");
                continue;
            }
            Drawable drawable = loadSmallIconDrawable(getContext(), sbn);
            if (drawable == null) {
                loadFailures++;
                PixelAodLog.log("failed AOD notification icon load trace=" + currentAodTraceId()
                        + " source=" + source
                        + " reason=drawable-null pkg=" + sbn.getPackageName()
                        + " key=" + sbn.getKey()
                        + " state={" + describeAodState(getContext()) + "}");
                continue;
            }
            loadedIconKeys.add(dedupeKey);
            loadedIcons.add(drawable);
        }
        NotificationIconDisplayPlan displayPlan =
                NotificationIconDisplayPlan.fromEligibleIconKeys(
                        loadedIconKeys, MAX_NOTIFICATION_ICONS);
        int emitted = displayPlan.visibleIconCount();
        for (int index = 0; index < emitted; index++) {
            ImageView iconView = new ImageView(getContext());
            iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            iconView.setImageDrawable(loadedIcons.get(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(NOTIFICATION_ICON_SIZE_DP),
                    dp(NOTIFICATION_ICON_SIZE_DP));
            if (index > 0) {
                params.leftMargin = dp(NOTIFICATION_ICON_SPACING_DP);
            }
            notificationIconRow.addView(iconView, params);
        }
        notificationOverflowView = addNotificationOverflowText(notificationIconRow,
                displayPlan.overflowCount(), emitted > 0,
                sharedInfoTypeface(getContext(), INFO_AOD_WEIGHT), INFO_AOD_WEIGHT,
                compactClock ? COMPACT_INFO_TEXT_DP : LARGE_INFO_TEXT_DP);
        notificationIconRow.setVisibility(
                displayPlan.totalIconCount() > 0 ? View.VISIBLE : View.GONE);
        applyClockMode(displayPlan.totalIconCount() > 0);
        PixelAodLog.log("rebuilt native AOD notification icons trace=" + currentAodTraceId()
                + " source=" + source
                + " input=" + notifications.size()
                + " emitted=" + emitted
                + " eligible=" + displayPlan.totalIconCount()
                + " overflow=" + displayPlan.overflowCount()
                + " skippedMedia=" + skippedMedia
                + " loadFailures=" + loadFailures
                + " packages=" + AodNotificationPipeline.describePackages(notifications)
                + " state={" + describeAodState(getContext()) + "}");
    }

    static TextView addNotificationOverflowText(LinearLayout row, int overflowCount,
            boolean hasVisibleIcons, Typeface infoTypeface, int infoWeight, int textSizeDp) {
        if (row == null || overflowCount <= 0) {
            return null;
        }
        Context context = row.getContext();
        TextView overflowView = makeInfoLine(context, infoTypeface, infoWeight, textSizeDp,
                Gravity.CENTER);
        overflowView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        overflowView.setText("+" + overflowCount);
        overflowView.setAlpha(1f);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(context, NOTIFICATION_ICON_SIZE_DP));
        if (hasVisibleIcons) {
            params.leftMargin = dp(context, NOTIFICATION_ICON_SPACING_DP + 1);
        }
        row.addView(overflowView, params);
        return overflowView;
    }

    static void syncNotificationOverflowStyle(TextView overflowView, TextView styleSource,
            ViewGroup parent) {
        if (overflowView == null || styleSource == null) {
            return;
        }
        overflowView.getPaint().set(styleSource.getPaint());
        overflowView.setTextColor(styleSource.getTextColors());
        overflowView.setTextSize(TypedValue.COMPLEX_UNIT_PX, styleSource.getTextSize());
        overflowView.setTypeface(styleSource.getTypeface());
        overflowView.setIncludeFontPadding(styleSource.getIncludeFontPadding());
        overflowView.setTextScaleX(styleSource.getTextScaleX());
        overflowView.setLetterSpacing(styleSource.getLetterSpacing());
        overflowView.setFontFeatureSettings(styleSource.getFontFeatureSettings());
        overflowView.setGravity(styleSource.getGravity());
        overflowView.setTextAlignment(styleSource.getTextAlignment());
        overflowView.setTextDirection(styleSource.getTextDirection());
        overflowView.setPadding(styleSource.getPaddingLeft(), styleSource.getPaddingTop(),
                styleSource.getPaddingRight(), styleSource.getPaddingBottom());
        overflowView.setLineSpacing(styleSource.getLineSpacingExtra(),
                styleSource.getLineSpacingMultiplier());
        overflowView.setEllipsize(styleSource.getEllipsize());
        overflowView.setMaxLines(styleSource.getMaxLines());
        overflowView.setMinLines(styleSource.getMinLines());
        overflowView.setMinHeight(styleSource.getMinHeight());
        overflowView.setMinWidth(styleSource.getMinWidth());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overflowView.setBreakStrategy(styleSource.getBreakStrategy());
            overflowView.setHyphenationFrequency(styleSource.getHyphenationFrequency());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            overflowView.setElegantTextHeight(styleSource.isElegantTextHeight());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overflowView.setFontVariationSettings(styleSource.getFontVariationSettings());
        }
        float parentAlpha = parent != null ? parent.getAlpha() : 1f;
        float sourceAlpha = styleSource.getAlpha();
        overflowView.setAlpha(parentAlpha > 0.01f
                ? Math.min(1f, sourceAlpha / parentAlpha) : sourceAlpha);
    }

    private boolean isNotificationForCurrentMedia(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return false;
        }
        if (!TextUtils.isEmpty(currentMediaNotificationKey)
                && TextUtils.equals(currentMediaNotificationKey, sbn.getKey())) {
            return true;
        }
        Notification notification = sbn.getNotification();
        if (Notification.CATEGORY_TRANSPORT.equals(notification.category)
                || AodNotificationPipeline.hasMediaSessionExtra(notification)) {
            return true;
        }
        String packageName = sbn.getPackageName();
        for (MediaController controller : mediaControllers) {
            if (controller != null && TextUtils.equals(packageName, controller.getPackageName())) {
                return true;
            }
        }
        return false;
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

    /**
     * Non-media notifications that should force the compact clock on AOD.
     * Media alone keeps the large AOD clock (module media row).
     */
    static boolean hasCompactClockNotificationContent() {
        synchronized (PixelAodClockView.class) {
            if (activeNotifications != null) {
                for (StatusBarNotification sbn : activeNotifications) {
                    if (sbn != null && sbn.getNotification() != null) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Active media that should force compact on the lockscreen (native media card),
     * but not on AOD (module media row under the large clock).
     */
    static boolean hasActiveDisplayableMedia() {
        for (PixelAodClockView view : INSTANCES) {
            if (view == null || !view.running) {
                continue;
            }
            try {
                MediaController controller = view.chooseVisibleMediaController();
                if (controller != null && view.hasDisplayableMedia(controller)) {
                    return true;
                }
            } catch (Throwable ignored) {
            }
        }
        return PixelLockscreenClockView.hasPlayingMediaOnAnyInstance();
    }

    boolean hasVisibleMediaLine() {
        return mediaRow != null
                && mediaRow.getVisibility() == View.VISIBLE
                && !TextUtils.isEmpty(lastMediaLineText);
    }

    static boolean isLockscreenPolicyEnabled() {
        Context context = appContext;
        return context == null
                || PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_LOCKSCREEN_NOTIFICATION_POLICY, true);
    }

    static String lockscreenPolicySilentHiddenReason(StatusBarNotification sbn, int importance) {
        return AodNotificationPipeline.lockscreenPolicySilentHiddenReason(
                isLockscreenPolicyEnabled(), sbn, importance);
    }

    private static Drawable loadMediaNotificationIcon(Context context, MediaController controller,
            StatusBarNotification notification) {
        if (context == null) {
            return null;
        }
        if (notification != null) {
            Drawable smallIcon = loadMonochromeNotificationIcon(context, notification);
            if (smallIcon != null) {
                logMediaIconChoice(notification.getPackageName(), "notification-smallIcon");
                return smallIcon;
            }
            PixelAodLog.log("AOD media icon fallback from notification pkg="
                    + notification.getPackageName()
                    + " reason=notification-smallIcon-unavailable"
                    + " trace=" + currentAodTraceId());
        }
        if (controller == null) {
            PixelAodLog.log("AOD media icon skipped reason=no-controller trace=" + currentAodTraceId());
            return null;
        }
        Drawable monochrome = loadApplicationMonochromeIcon(context, controller.getPackageName());
        if (monochrome != null) {
            logMediaIconChoice(controller.getPackageName(), "app-monochrome-fallback");
            return monochrome;
        }
        PixelAodLog.log("AOD media icon fallback pkg=" + controller.getPackageName()
                + " reason=app-monochrome-unavailable"
                + " trace=" + currentAodTraceId());
        return null;
    }

    private static StatusBarNotification findMediaNotification(MediaController controller) {
        String packageName = controller.getPackageName();
        if (isExpiredInactiveMediaPackageForAnyInstance(packageName)) {
            PixelAodLog.log("ignored AOD media notification lookup pkg=" + packageName
                    + " reason=package-timeout trace=" + currentAodTraceId());
            return null;
        }
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
            if (AodNotificationPipeline.matchesMediaSession(notification, controller)) {
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

    private static Drawable loadMonochromeNotificationIcon(Context context, StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null || notification.getSmallIcon() == null) {
                PixelAodLog.log("AOD media notification smallIcon unavailable pkg="
                        + sbn.getPackageName()
                        + " trace=" + currentAodTraceId());
                return null;
            }
            Drawable drawable = loadIconDrawable(context, sbn.getPackageName(), notification.getSmallIcon());
            if (drawable == null) {
                PixelAodLog.log("AOD media notification smallIcon drawable missing pkg="
                        + sbn.getPackageName()
                        + " trace=" + currentAodTraceId());
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
                logRejectedMediaIcon(packageName, "adaptive-icon-no-monochrome");
                return null;
            }
            Drawable result = monochrome.mutate();
            result.setTint(resolveMaterialInfoColor(context));
            result.setTintMode(PorterDuff.Mode.SRC_IN);
            if (looksLikeFilledMonochromeMask(result) || looksLikeTinyForeground(result)) {
                logRejectedMediaIcon(packageName,
                        looksLikeFilledMonochromeMask(result) ? "filled-mask" : "tiny-foreground");
                return null;
            }
            return result;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load AOD media monochrome icon pkg=" + packageName, t);
            return null;
        }
    }

    private static Drawable loadApplicationIcon(PackageManager packageManager, String packageName)
            throws PackageManager.NameNotFoundException {
        ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
        Drawable drawable = info.loadIcon(packageManager);
        return drawable != null ? drawable : packageManager.getApplicationIcon(packageName);
    }

    private static Drawable loadApplicationColorIcon(Context context, String packageName) {
        try {
            Drawable drawable = loadApplicationIcon(context.getPackageManager(), packageName);
            return drawable != null ? drawable.mutate() : null;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load AOD notification app color icon pkg=" + packageName, t);
            return null;
        }
    }

    static Drawable loadSmallIconDrawable(Context context, StatusBarNotification sbn) {
        try {
            Notification notification = sbn.getNotification();
            if (notification == null) {
                PixelAodLog.log("AOD notification icon skipped pkg=" + sbn.getPackageName()
                        + " reason=no-notification"
                        + " trace=" + currentAodTraceId());
                return null;
            }
            Icon icon = notification.getSmallIcon();
            if (icon == null) {
                PixelAodLog.log("AOD notification icon skipped pkg=" + sbn.getPackageName()
                        + " reason=no-small-icon"
                        + " trace=" + currentAodTraceId());
                return null;
            }
            Drawable drawable = loadIconDrawable(context, sbn.getPackageName(), icon);
            if (drawable == null) {
                PixelAodLog.log("AOD notification icon skipped pkg=" + sbn.getPackageName()
                        + " reason=icon-drawable-null"
                        + " trace=" + currentAodTraceId());
                return null;
            }
            boolean filledMask = looksLikeFilledNotificationMask(drawable);
            boolean tinyForeground = looksLikeTinyForeground(drawable);
            if (AodNotificationPipeline.isOosLiveAlertNotification(sbn)) {
                Drawable liveAlertIcon = loadOosLiveAlertIcon(context, sbn);
                if (liveAlertIcon != null) {
                    logNotificationIconChoice(sbn.getPackageName(), "oos-live-alert-glyph");
                    return liveAlertIcon;
                }
                if (filledMask || tinyForeground) {
                    logNotificationIconChoice(sbn.getPackageName(),
                            "dropped-blocky-live-alert-smallIcon filled=" + filledMask
                                    + " tiny=" + tinyForeground);
                    return null;
                }
            }
            if (AodNotificationPipeline.isSystemNotificationCandidate(sbn)) {
                Drawable glyph = loadSystemNotificationIcon(context, sbn);
                if (glyph != null) {
                    logNotificationIconChoice(sbn.getPackageName(), "system-native-icon");
                    return glyph;
                }
                if (filledMask || tinyForeground) {
                    logNotificationIconChoice(sbn.getPackageName(),
                            "dropped-blocky-system-smallIcon filled=" + filledMask
                                    + " tiny=" + tinyForeground);
                    return null;
                }
                Drawable mutated = drawable.mutate();
                mutated.setTint(resolveMaterialInfoColor(context));
                mutated.setTintMode(PorterDuff.Mode.SRC_IN);
                logNotificationIconChoice(sbn.getPackageName(), "system-smallIcon");
                return mutated;
            }
            String resourceName = notificationSmallIconResourceName(context, sbn, icon);
            if (AodNotificationPipeline.isLauncherStyleSmallIconResourceName(resourceName)) {
                logNotificationIconChoice(sbn.getPackageName(),
                        "notification-launcher-resource-original-color resource=" + resourceName);
                return drawable.mutate();
            }
            if (filledMask || tinyForeground) {
                Drawable monochrome = loadApplicationMonochromeIcon(context, sbn.getPackageName());
                if (monochrome != null) {
                    logNotificationIconChoice(sbn.getPackageName(), "app-monochrome-fallback");
                    return monochrome;
                }
                if (isOplusPushBitmapSmallIcon(sbn, icon)) {
                    Drawable appIcon = loadApplicationColorIcon(context, sbn.getPackageName());
                    if (appIcon != null) {
                        logNotificationIconChoice(sbn.getPackageName(),
                                "oplus-push-bitmap-app-color-fallback filled=" + filledMask
                                        + " tiny=" + tinyForeground
                                        + " iconType=" + icon.getType());
                        return appIcon;
                    }
                }
                Drawable tinted = drawable.mutate();
                tinted.setTint(resolveMaterialInfoColor(context));
                tinted.setTintMode(PorterDuff.Mode.SRC_IN);
                logNotificationIconChoice(sbn.getPackageName(),
                        "notification-smallIcon-filled-mask-tint filled=" + filledMask
                                + " tiny=" + tinyForeground);
                return tinted;
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

    private static String notificationSmallIconResourceName(Context context,
            StatusBarNotification sbn, Icon icon) {
        if (context == null || sbn == null || icon == null || icon.getType() != Icon.TYPE_RESOURCE) {
            return "";
        }
        try {
            String resourcePackage = icon.getResPackage();
            if (TextUtils.isEmpty(resourcePackage)) {
                resourcePackage = sbn.getPackageName();
            }
            Resources resources = context.getPackageManager()
                    .getResourcesForApplication(resourcePackage);
            return resources.getResourceName(icon.getResId());
        } catch (Throwable t) {
            PixelAodLog.log("failed to resolve AOD notification smallIcon resource pkg="
                    + sbn.getPackageName() + " iconType=" + icon.getType(), t);
            return "";
        }
    }

    private static boolean isOplusPushBitmapSmallIcon(StatusBarNotification sbn, Icon icon) {
        if (sbn == null || icon == null || !isBitmapIcon(icon)) {
            return false;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return false;
        }
        Bundle extras = notification.extras;
        if (extras != null) {
            if (extras.getBoolean("EXTRA_IS_MCS", false)) {
                return true;
            }
            if (extras.containsKey("oplus_smallicon_use_app_icon")) {
                return true;
            }
            String appPackage = extras.getString("appPackage");
            if (TextUtils.equals(appPackage, sbn.getPackageName())) {
                return true;
            }
        }
        String channelId = notification.getChannelId();
        return TextUtils.equals("Heytap PUSH", channelId)
                || TextUtils.equals("Notify PUSH", channelId)
                || TextUtils.equals("Silent PUSH", channelId);
    }

    private static boolean isBitmapIcon(Icon icon) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false;
        }
        int type = icon.getType();
        return type == Icon.TYPE_BITMAP || type == Icon.TYPE_ADAPTIVE_BITMAP;
    }

    private static Drawable loadSystemNotificationIcon(Context context, StatusBarNotification sbn) {
        int color = resolveMaterialInfoColor(context);
        if (AodNotificationPipeline.isSystemUiUsbNotification(sbn)) {
            Drawable nativeIcon = loadTintedSystemDrawable(context, color,
                    "stat_sys_data_usb",
                    "stat_notify_sdcard_usb");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            return loadSystemNotificationGlyph(context, sbn);
        }
        String text = AodNotificationPipeline.systemNotificationText(sbn);
        if (AodNotificationPipeline.isSystemNetworkStatusNotification(sbn)
                || text.contains("network status")
                || text.contains("hotspot")
                || text.contains("tether")
                || text.contains("wi-fi sharing")
                || text.contains("wifi sharing")) {
            Drawable nativeIcon = loadTintedSystemDrawable(context, color, "stat_sys_tether_wifi");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            return loadSystemNotificationGlyph(context, sbn);
        }
        if (text.contains("module update")) {
            return loadSystemNotificationGlyph(context, sbn);
        }
        return null;
    }

    private static Drawable loadOosLiveAlertIcon(Context context, StatusBarNotification sbn) {
        int color = resolveMaterialInfoColor(context);
        if (AodNotificationPipeline.isOosFlashlightLiveAlert(sbn)) {
            Drawable nativeIcon = loadTintedPackageDrawable(context, color, "com.android.systemui",
                    "stat_sys_flashlight",
                    "ic_qs_flashlight",
                    "ic_qs_flashlight_enabled",
                    "ic_flashlight",
                    "ic_flashlight_on",
                    "op_ic_flashlight",
                    "op_ic_qs_flashlight");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            nativeIcon = loadTintedPackageDrawable(context, color, MODULE_PACKAGE,
                    "ic_aosp_qs_flashlight");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            return new SystemNotificationGlyphDrawable(color,
                    SystemNotificationGlyphDrawable.TYPE_FLASHLIGHT);
        }
        if (AodNotificationPipeline.isOosDeskClockLiveAlert(sbn)) {
            Drawable nativeIcon = loadTintedSystemDrawable(context, color,
                    "stat_notify_alarm",
                    "stat_sys_alarm",
                    "ic_lock_idle_alarm",
                    "ic_dialog_time");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            nativeIcon = loadTintedPackageDrawable(context, color, "com.oneplus.deskclock",
                    "stat_notify_alarm",
                    "stat_notify_timer",
                    "ic_stat_timer",
                    "ic_stat_alarm",
                    "ic_timer",
                    "ic_stopwatch",
                    "ic_alarm",
                    "ic_clock");
            if (nativeIcon != null) {
                return nativeIcon;
            }
            return new SystemNotificationGlyphDrawable(color,
                    SystemNotificationGlyphDrawable.TYPE_TIMER);
        }
        return new SystemNotificationGlyphDrawable(color,
                SystemNotificationGlyphDrawable.TYPE_LIVE_ALERT);
    }

    private void noteMediaContentActivity(String packageName, String source) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        clearExpiredInactiveMediaPackage(packageName, source);
        for (MediaController controller : mediaControllers) {
            if (controller == null || !TextUtils.equals(packageName, controller.getPackageName())) {
                continue;
            }
            PlaybackState state = safePlaybackState(controller);
            if (isPlayingPlaybackState(state)) {
                clearInactiveMediaTracking(controller, source + "#playing");
            } else {
                restartInactiveMediaTracking(controller, state, source);
            }
        }
    }

    private void restartInactiveMediaTracking(MediaController controller, PlaybackState state, String source) {
        String key = mediaControllerKey(controller);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        clearInactiveMediaTimeout(key);
        Long existing = inactiveMediaStartedAt.get(key);
        long startedAt = existing != null && existing > 0L
                ? existing
                : SystemClock.elapsedRealtime();
        Long previous = inactiveMediaStartedAt.put(key, startedAt);
        PixelAodLog.log("AOD media content activity pkg=" + controller.getPackageName()
                + " key=" + key
                + " source=" + source
                + " state=" + playbackStateName(state)
                + " previousInactiveAt=" + previous
                + " startedAt=" + startedAt
                + " preserved=" + (existing != null && existing > 0L)
                + " trace=" + currentAodTraceId());
        scheduleInactiveMediaTimeoutCheck(controller, key);
    }

    private void notePlaybackState(MediaController controller, PlaybackState state) {
        if (controller == null) {
            return;
        }
        if (isPlayingPlaybackState(state)) {
            clearExpiredInactiveMediaPackage(controller.getPackageName(), "media-playing");
            clearInactiveMediaTracking(controller, "media-playing");
            return;
        }
        if (isPausedPlaybackState(state) || isNonePlaybackState(state) || state == null) {
            noteInactiveMediaState(controller, state,
                    isPausedPlaybackState(state) ? "paused" : "idle");
            return;
        }
        if (isStoppedPlaybackState(state)) {
            clearInactiveMediaTracking(controller, "media-stopped");
        }
    }

    private void noteInactiveMediaState(MediaController controller, PlaybackState state, String reason) {
        String key = mediaControllerKey(controller);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        Long existing = inactiveMediaStartedAt.get(key);
        long stateStartedAt = inactiveMediaStartFromState(state);
        long startedAt = existing != null && existing > 0L ? existing : stateStartedAt;
        if (startedAt <= 0L) {
            startedAt = existing != null ? existing : SystemClock.elapsedRealtime();
        }
        Long previous = inactiveMediaStartedAt.put(key, startedAt);
        long age = SystemClock.elapsedRealtime() - startedAt;
        PixelAodLog.log("AOD media inactive state pkg=" + controller.getPackageName()
                + " key=" + key
                + " reason=" + reason
                + " state=" + playbackStateName(state)
                + " stateStartedAt=" + stateStartedAt
                + " startedAt=" + startedAt
                + " previous=" + previous
                + " preserved=" + (existing != null && existing > 0L)
                + " ageMs=" + age
                + " trace=" + currentAodTraceId());
        scheduleInactiveMediaTimeoutCheck(controller, key);
    }

    private boolean isInactiveMediaWithinTimeout(MediaController controller, PlaybackState state,
            String reason) {
        if (controller == null) {
            return false;
        }
        String key = mediaControllerKey(controller);
        if (TextUtils.isEmpty(key)) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        Long startedAt = inactiveMediaStartedAt.get(key);
        if (startedAt == null) {
            long stateStart = inactiveMediaStartFromState(state);
            startedAt = stateStart > 0L ? stateStart : now;
            inactiveMediaStartedAt.put(key, startedAt);
            scheduleInactiveMediaTimeoutCheck(controller, key);
        }
        long age = now - startedAt;
        boolean within = age >= 0L && age < PAUSED_MEDIA_TIMEOUT_MILLIS;
        PixelAodLog.log("AOD media inactive keep pkg=" + controller.getPackageName()
                + " key=" + key
                + " reason=" + reason
                + " state=" + playbackStateName(state)
                + " within=" + within
                + " ageMs=" + age
                + " timeoutMs=" + PAUSED_MEDIA_TIMEOUT_MILLIS
                + " trace=" + currentAodTraceId());
        if (!within) {
            clearInactiveMediaTimeout(key);
            markExpiredInactiveMediaPackage(controller.getPackageName(), "timeout");
            PixelAodLog.log("AOD media inactive expired pkg=" + controller.getPackageName()
                    + " key=" + key
                    + " reason=" + reason
                    + " state=" + playbackStateName(state)
                    + " ageMs=" + age
                    + " timeoutMs=" + PAUSED_MEDIA_TIMEOUT_MILLIS
                    + " trace=" + currentAodTraceId());
        }
        return within;
    }

    private long inactiveMediaStartFromState(PlaybackState state) {
        if (state == null) {
            return 0L;
        }
        try {
            long updatedAt = state.getLastPositionUpdateTime();
            if (updatedAt > 0L) {
                return updatedAt;
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    private void scheduleInactiveMediaTimeoutCheck(MediaController controller, String key) {
        if (controller == null || TextUtils.isEmpty(key)) {
            return;
        }
        Long startedAt = inactiveMediaStartedAt.get(key);
        if (startedAt == null) {
            return;
        }
        Runnable previous = inactiveMediaTimeoutRunnables.remove(key);
        if (previous != null) {
            mainHandler().removeCallbacks(previous);
        }
        long delay = (startedAt + PAUSED_MEDIA_TIMEOUT_MILLIS) - SystemClock.elapsedRealtime();
        if (delay <= 0L) {
            mainHandler().post(() -> expireInactiveMedia(controller, key, "media-inactive-timeout"));
            return;
        }
        Runnable timeoutRunnable = () -> expireInactiveMedia(controller, key, "media-inactive-timeout");
        inactiveMediaTimeoutRunnables.put(key, timeoutRunnable);
        mainHandler().postDelayed(timeoutRunnable, delay + 50L);
        PixelAodLog.log("scheduled AOD media inactive timeout pkg=" + controller.getPackageName()
                + " key=" + key
                + " delayMs=" + delay
                + " trace=" + currentAodTraceId());
    }

    private void expireInactiveMedia(MediaController controller, String key, String source) {
        inactiveMediaTimeoutRunnables.remove(key);
        PlaybackState state = null;
        try {
            state = controller.getPlaybackState();
        } catch (Throwable ignored) {
        }
        if (isPlayingPlaybackState(state)) {
            clearInactiveMediaTracking(controller, source + "#playing");
            return;
        }
        if (controller != null) {
            markExpiredInactiveMediaPackage(controller.getPackageName(), source);
        }
        PixelAodLog.log("expired AOD inactive media pkg="
                + (controller != null ? controller.getPackageName() : "")
                + " key=" + key
                + " source=" + source
                + " state=" + playbackStateName(state)
                + " trace=" + currentAodTraceId());
        updateMediaLine(source);
        requestAodFrameRefresh(source);
    }

    private boolean isExpiredInactiveMediaPackage(String packageName) {
        return isExpiredInactiveMediaPackageForAnyInstance(packageName);
    }

    private static boolean isExpiredInactiveMediaPackageForAnyInstance(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        synchronized (PixelAodClockView.class) {
            return expiredInactiveMediaPackages.contains(packageName);
        }
    }

    private void markExpiredInactiveMediaPackage(String packageName, String source) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        boolean changed;
        synchronized (PixelAodClockView.class) {
            changed = expiredInactiveMediaPackages.add(packageName);
        }
        if (changed) {
            PixelAodLog.log("marked AOD inactive media package expired pkg=" + packageName
                    + " source=" + source
                    + " trace=" + currentAodTraceId());
        }
    }

    private static void clearExpiredInactiveMediaPackage(String packageName, String source) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        boolean changed;
        synchronized (PixelAodClockView.class) {
            changed = expiredInactiveMediaPackages.remove(packageName);
        }
        if (changed) {
            PixelAodLog.log("cleared AOD inactive media package expiry pkg=" + packageName
                    + " source=" + source
                    + " trace=" + currentAodTraceId());
        }
    }

    private void clearInactiveMediaTracking(MediaController controller, String source) {
        String key = mediaControllerKey(controller);
        if (TextUtils.isEmpty(key)) {
            return;
        }
        clearInactiveMediaTimeout(key);
        inactiveMediaStartedAt.remove(key);
        PixelAodLog.log("cleared AOD media inactive tracking pkg=" + controller.getPackageName()
                + " key=" + key
                + " source=" + source
                + " trace=" + currentAodTraceId());
    }

    private void clearInactiveMediaTimeout(String key) {
        Runnable pendingTimeout = inactiveMediaTimeoutRunnables.remove(key);
        if (pendingTimeout != null) {
            mainHandler().removeCallbacks(pendingTimeout);
        }
    }

    private static String mediaControllerKey(MediaController controller) {
        if (controller == null) {
            return "";
        }
        try {
            return controller.getPackageName() + "|" + controller.getSessionToken();
        } catch (Throwable ignored) {
            return controller.getPackageName();
        }
    }

    private static String playbackStateName(PlaybackState state) {
        if (state == null) {
            return "null";
        }
        return state.getState() + "@" + state.getLastPositionUpdateTime();
    }

    private static Drawable loadSystemNotificationGlyph(Context context, StatusBarNotification sbn) {
        int color = resolveMaterialInfoColor(context);
        if (AodNotificationPipeline.isSystemUiUsbNotification(sbn)) {
            return new UsbNotificationDrawable(color,
                    AodNotificationPipeline.isSystemUiUsbDebugNotification(sbn));
        }
        String text = AodNotificationPipeline.systemNotificationText(sbn);
        if (text.contains("module update")) {
            return new SystemNotificationGlyphDrawable(color, SystemNotificationGlyphDrawable.TYPE_CHECK);
        }
        if (AodNotificationPipeline.isSystemNetworkStatusNotification(sbn)
                || text.contains("network status")
                || text.contains("hotspot")
                || text.contains("tether")
                || text.contains("wi-fi sharing")
                || text.contains("wifi sharing")) {
            return new SystemNotificationGlyphDrawable(color, SystemNotificationGlyphDrawable.TYPE_NETWORK);
        }
        return null;
    }

    private static Drawable loadTintedSystemDrawable(Context context, int color, String... names) {
        Drawable drawable = loadSystemDrawableByName(context, names);
        if (drawable == null) {
            return null;
        }
        Drawable result = drawable.mutate();
        result.setTint(color);
        result.setTintMode(PorterDuff.Mode.SRC_IN);
        return result;
    }

    private static Drawable loadTintedPackageDrawable(Context context, int color,
            String resourcePackage, String... names) {
        Drawable drawable = loadPackageDrawableByName(context, resourcePackage, names);
        if (drawable == null) {
            return null;
        }
        Drawable result = drawable.mutate();
        result.setTint(color);
        result.setTintMode(PorterDuff.Mode.SRC_IN);
        return result;
    }

    private static Drawable loadPackageDrawableByName(Context context, String resourcePackage,
            String... names) {
        if (context == null || TextUtils.isEmpty(resourcePackage)
                || names == null || names.length == 0) {
            return null;
        }
        try {
            Context packageContext = context.createPackageContext(
                    resourcePackage, Context.CONTEXT_IGNORE_SECURITY);
            Resources resources = packageContext.getResources();
            for (String name : names) {
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                try {
                    int resId = resources.getIdentifier(name, "drawable", resourcePackage);
                    if (resId == 0) {
                        continue;
                    }
                    Drawable drawable = resources.getDrawable(resId, packageContext.getTheme());
                    if (drawable != null) {
                        String logKey = resourcePackage + ":" + name;
                        synchronized (loggedNativeSystemDrawableNames) {
                            if (loggedNativeSystemDrawableNames.add(logKey)) {
                                PixelAodLog.log("loaded native package drawable package="
                                        + resourcePackage + " name=" + name
                                        + " resId=" + resId);
                            }
                        }
                        return drawable;
                    }
                } catch (Throwable t) {
                    String logKey = "fail:" + resourcePackage + ":" + name;
                    synchronized (loggedNativeSystemDrawableNames) {
                        if (loggedNativeSystemDrawableNames.add(logKey)) {
                            PixelAodLog.log("failed to load native package drawable package="
                                    + resourcePackage + " name=" + name, t);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            String logKey = "fail:" + resourcePackage;
            synchronized (loggedNativeSystemDrawableNames) {
                if (loggedNativeSystemDrawableNames.add(logKey)) {
                    PixelAodLog.log("failed to load native package drawable package="
                            + resourcePackage, t);
                }
            }
        }
        return null;
    }

    private static Drawable loadSystemDrawableByName(Context context, String... names) {
        if (context == null || names == null || names.length == 0) {
            return null;
        }
        Resources resources = Resources.getSystem();
        for (String name : names) {
            if (TextUtils.isEmpty(name)) {
                continue;
            }
            try {
                int resId = resources.getIdentifier(name, "drawable", "android");
                if (resId == 0) {
                    continue;
                }
                Drawable drawable = resources.getDrawable(resId, context.getTheme());
                if (drawable != null) {
                    synchronized (loggedNativeSystemDrawableNames) {
                        if (loggedNativeSystemDrawableNames.add(name)) {
                            PixelAodLog.log("loaded native AOSP drawable name=" + name
                                    + " resId=" + resId);
                        }
                    }
                    return drawable;
                }
            } catch (Throwable t) {
                synchronized (loggedNativeSystemDrawableNames) {
                    if (loggedNativeSystemDrawableNames.add("fail:" + name)) {
                        PixelAodLog.log("failed to load native AOSP drawable name=" + name, t);
                    }
                }
            }
        }
        return null;
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

    private static String mediaCandidatesSignatureLocked() {
        return AodNotificationPipeline.mediaCandidatesSignature(mediaNotificationCache.values());
    }

    private static ArrayList<String> mediaNotificationPackagesLocked() {
        ArrayList<String> packages = new ArrayList<>();
        for (StatusBarNotification sbn : mediaNotificationCache.values()) {
            if (sbn == null || TextUtils.isEmpty(sbn.getPackageName())
                    || packages.contains(sbn.getPackageName())) {
                continue;
            }
            packages.add(sbn.getPackageName());
        }
        return packages;
    }

    private static String expiredInactiveMediaPackageSignature() {
        synchronized (PixelAodClockView.class) {
            if (expiredInactiveMediaPackages.isEmpty()) {
                return "";
            }
            ArrayList<String> packages = new ArrayList<>(expiredInactiveMediaPackages);
            Collections.sort(packages);
            return TextUtils.join("|", packages);
        }
    }

    private static void logNotificationIconChoice(String packageName, String mode) {
        PixelAodLog.log("AOD notification icon choice pkg=" + packageName
                + " mode=" + mode
                + " trace=" + currentAodTraceId());
    }

    private static void logMediaIconChoice(String packageName, String mode) {
        PixelAodLog.log("AOD media icon choice pkg=" + packageName
                + " mode=" + mode
                + " trace=" + currentAodTraceId());
    }

    private static void logRejectedMediaIcon(String packageName, String reason) {
        PixelAodLog.log("rejected AOD media monochrome icon pkg=" + packageName
                + " reason=" + reason
                + " trace=" + currentAodTraceId());
    }

    private static void logMediaNotificationCache(String action, String source, int count) {
        PixelAodLog.log("AOD media notification cache " + action
                + " source=" + source + " count=" + count
                + " trace=" + currentAodTraceId());
    }

    private static void logMediaNotificationCache(String action, String source, int count,
            String packageName, boolean contentChanged, String mediaText) {
        PixelAodLog.log("AOD media notification cache " + action
                + " source=" + source
                + " count=" + count
                + " pkg=" + packageName
                + " contentChanged=" + contentChanged
                + " text=" + describeMediaTextForLog(mediaText)
                + " trace=" + currentAodTraceId());
    }

    private static void refreshMediaLines() {
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view != null) {
                    view.updateMediaLine("refreshMediaLines");
                }
            }
        });
    }

    private static void refreshMediaLinesForMediaActivity(List<String> packages, String source) {
        mainHandler().post(() -> {
            for (PixelAodClockView view : INSTANCES) {
                if (view == null) {
                    continue;
                }
                if (packages != null) {
                    for (String packageName : packages) {
                        view.noteMediaContentActivity(packageName, source);
                    }
                }
                view.updateMediaLine(source);
            }
        });
    }

    private void applyMaterialColors() {
        int clockColor = resolveMaterialClockColor(getContext());
        int infoColor = resolveMaterialInfoColor(getContext());
        clockView.setTextColor(clockColor);
        dateView.setTextColor(infoColor);
        syncNotificationOverflowStyle(notificationOverflowView, dateView, notificationIconRow);
        mediaView.setTextColor(infoColor);
        batteryView.setTextColor(infoColor);
        chargeBoltView.setTint(infoColor);
        applyWeatherIcon(dateView, currentFreshWeather(getContext()), infoColor);
    }

    static int resolveMaterialClockColor(Context context) {
        int fallback = resolveWallpaperTextColor(context, "wallpaperTextColor", CLOCK_COLOR);
        int themedAccent = resolveWallpaperTextColor(context, "wallpaperTextColorAccent", fallback);
        return resolveDynamicPaletteLightAccent(context, themedAccent);
    }

    static int resolveMaterialInfoColor(Context context) {
        int secondary = resolveWallpaperTextColor(context, "wallpaperTextColorSecondary", INFO_COLOR);
        int color = resolveDynamicPaletteLightAccent(context, secondary);
        return withAlpha(color, 230);
    }

    private static int resolveDynamicPaletteLightAccent(Context context, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            String overlay = Settings.Secure.getString(context.getContentResolver(),
                    THEME_CUSTOMIZATION_OVERLAY_PACKAGES);
            if (TextUtils.isEmpty(overlay)) {
                return fallback;
            }
            JSONObject object = new JSONObject(overlay);
            int sourceColor = parseThemeColor(object.optString(
                    "android.theme.customization.system_palette", null));
            if (sourceColor == 0) {
                sourceColor = parseThemeColor(object.optString(
                        "android.theme.customization.accent_color", null));
            }
            if (sourceColor == 0) {
                return fallback;
            }
            float[] hsv = new float[3];
            Color.colorToHSV(sourceColor, hsv);
            hsv[1] = Math.max(0.08f, Math.min(0.16f, hsv[1] * 0.23f));
            hsv[2] = Math.max(0.90f, Math.min(0.96f, hsv[2] + 0.21f));
            return Color.HSVToColor(hsv);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean mayBeColoredSystemUiUsbIcon(Drawable drawable) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, ICON_MASK_SAMPLE_SIZE, ICON_MASK_SAMPLE_SIZE);
            drawable.draw(canvas);
            int firstColor = 0;
            int distinctColors = 0;
            for (int y = 0; y < ICON_MASK_SAMPLE_SIZE; y++) {
                for (int x = 0; x < ICON_MASK_SAMPLE_SIZE; x++) {
                    int pixel = bitmap.getPixel(x, y);
                    if (Color.alpha(pixel) <= 48) {
                        continue;
                    }
                    int rgb = pixel & 0x00ffffff;
                    if (firstColor == 0) {
                        firstColor = rgb;
                    } else if (Math.abs(Color.red(rgb) - Color.red(firstColor)) > 10
                            || Math.abs(Color.green(rgb) - Color.green(firstColor)) > 10
                            || Math.abs(Color.blue(rgb) - Color.blue(firstColor)) > 10) {
                        distinctColors++;
                        if (distinctColors > 4) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (bitmap != null) {
                bitmap.recycle();
            }
        }
    }

    private static int parseThemeColor(String value) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        try {
            String hex = value.trim();
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            if (hex.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(hex, 16));
            }
            if (hex.length() == 8) {
                return (int) Long.parseLong(hex, 16);
            }
        } catch (Throwable ignored) {
            // Invalid overlay value.
        }
        return 0;
    }

    private static int resolveWallpaperTextColor(Context context, String attrName, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            int attrId = context.getResources().getIdentifier(
                    attrName, "attr", "com.android.systemui");
            if (attrId == 0) {
                attrId = context.getResources().getIdentifier(
                        attrName, "attr", context.getPackageName());
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

    static TextView makeInfoLine(Context context, Typeface typeface, int weight,
            int textSizeDp, int gravity) {
        TextView textView = new TextView(context);
        textView.setTextColor(resolveMaterialInfoColor(context));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        textView.setTypeface(typeface);
        applySharedFontVariation(textView, weight);
        textView.setIncludeFontPadding(false);
        textView.setGravity(gravity);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        textView.setSingleLine(true);
        textView.setLetterSpacing(INFO_LETTER_SPACING);
        textView.setAlpha(INFO_ALPHA);
        return textView;
    }

    private static Drawable getExternalWeatherIconDrawable(Context context, WeatherSnapshot weather) {
        int weatherCode = weather.weatherCode;
        String packageName = PixelAodSettings.getString(context, PixelAodSettings.KEY_WEATHER_ICON_PACK, "");
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        try {
            PackageManager pm = context.getPackageManager();
            android.content.res.Resources res = pm.getResourcesForApplication(packageName);

            boolean isNight = resolveIsNight(weather, System.currentTimeMillis());
            String breezyName = getBreezyWeatherIconName(weatherCode, isNight);

            int xmlId = 0;
            try {
                android.content.pm.ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                if (appInfo.metaData != null && appInfo.metaData.containsKey("org.breezyweather.DRAWABLE_FILTER")) {
                    xmlId = appInfo.metaData.getInt("org.breezyweather.DRAWABLE_FILTER", 0);
                }
            } catch (Exception e) {
                // Ignore
            }
            if (xmlId == 0) {
                xmlId = res.getIdentifier("drawable_filter", "xml", packageName);
            }

            if (xmlId != 0) {
                try (android.content.res.XmlResourceParser parser = res.getXml(xmlId)) {
                    int eventType = parser.getEventType();
                    String mappedName = null;
                    while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                        if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                            String name = parser.getAttributeValue(null, "name");
                            if (breezyName.equals(name)) {
                                mappedName = parser.getAttributeValue(null, "value");
                                break;
                            }
                        }
                        eventType = parser.next();
                    }
                    if (mappedName != null) {
                        int resId = res.getIdentifier(mappedName, "drawable", packageName);
                        if (resId != 0) {
                            return res.getDrawable(resId, null);
                        }
                    }
                } catch (Exception e) {
                    PixelAodLog.e("Failed to parse drawable_filter.xml", e);
                }
            }

            int directId = res.getIdentifier(breezyName, "drawable", packageName);
            if (directId != 0) {
                return res.getDrawable(directId, null);
            }

            int chronusCode = getChronusWeatherCode(weatherCode, isNight);
            String[] prefixes = {"weather_", "ic_weather_", "condition_", "outline_", "color_"};
            for (String prefix : prefixes) {
                int resId = res.getIdentifier(prefix + chronusCode, "drawable", packageName);
                if (resId != 0) {
                    return res.getDrawable(resId, null);
                }
            }

            int fallbackId = res.getIdentifier("weather_na", "drawable", packageName);
            if (fallbackId == 0) fallbackId = res.getIdentifier("weather_0", "drawable", packageName);
            if (fallbackId != 0) return res.getDrawable(fallbackId, null);

        } catch (Exception e) {
            PixelAodLog.e("Failed to load external weather icon from " + packageName, e);
        }
        return null;
    }

    /**
     * Determine day/night for a weather snapshot. Prefers Breezy Weather's
     * own sunrise/sunset boundaries when available; falls back to a simple
     * local hour check (6:00-18:00 = day) when the snapshot doesn't carry
     * sun-time data.
     */
    static boolean resolveIsNight(WeatherSnapshot weather, long nowMillis) {
        if (weather != null && weather.hasSunTimes()) {
            return weather.isNightAt(nowMillis);
        }
        java.util.Calendar c = java.util.Calendar.getInstance();
        c.setTimeInMillis(nowMillis);
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        return hour < 6 || hour >= 18;
    }

    private static String getBreezyWeatherIconName(int code, boolean isNight) {
        String suffix = isNight ? "_night" : "_day";
        if (code >= 200 && code < 300) return "weather_thunderstorm" + suffix;
        if (code >= 300 && code < 400) return "weather_rain" + suffix;
        if (code >= 500 && code < 600) {
            if (code == 511 || code == 512) return "weather_sleet" + suffix;
            return "weather_rain" + suffix;
        }
        if (code >= 600 && code < 700) {
            if (code == 611 || code == 612 || code == 615 || code == 616) return "weather_sleet" + suffix;
            return "weather_snow" + suffix;
        }
        if (code == 701 || code == 741) return "weather_fog" + suffix;
        if (code == 711 || code == 721 || code == 731 || code == 751 || code == 761 || code == 762) return "weather_haze" + suffix;
        if (code == 771 || code == 781) return "weather_wind" + suffix;
        if (code == 800) return "weather_clear" + suffix;
        if (code == 801 || code == 802) return "weather_partly_cloudy" + suffix;
        if (code == 803 || code == 804) return "weather_cloudy" + suffix;
        return "weather_cloudy" + suffix;
    }

    private static int getChronusWeatherCode(int code, boolean isNight) {
        if (code >= 200 && code < 300) return 4;
        if (code >= 300 && code < 400) return 9;
        if (code >= 500 && code < 600) {
            if (code == 511) return 10;
            return 11;
        }
        if (code >= 600 && code < 700) {
            if (code == 611 || code == 612 || code == 615 || code == 616) return 18;
            return 16;
        }
        if (code == 701 || code == 741) return 20;
        if (code == 711) return 22;
        if (code == 721) return 21;
        if (code == 731 || code == 751 || code == 761 || code == 762) return 19;
        if (code == 771 || code == 781) return 24;
        if (code == 800) return isNight ? 31 : 32;
        if (code == 801 || code == 802) return isNight ? 29 : 30;
        if (code == 803 || code == 804) return 26;
        return 3200;
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
        Drawable drawable = getExternalWeatherIconDrawable(textView.getContext(), weather);
        if (drawable == null) {
            drawable = new WeatherIconDrawable(weather.weatherCode, color);
        } else {
            // Unlink original to avoid mutating shared state across views
            drawable = drawable.mutate();
        }
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
        return PixelAodRenderModel.formatDateWithWeather(calendar, currentFreshWeather(appContext));
    }

    private void updateTime() {
        WeatherSnapshot weather = currentFreshWeather(getContext());
        BatteryStatus batteryStatus = readBatteryStatus();
        PixelAodRenderModel model = PixelAodRenderModel.forAod(getContext(), compactClock,
                weather, currentAtAGlanceExtra(), batteryStatus.percentText,
                batteryStatus.charging);
        CharSequence previousClockText = clockView.getText();
        applySharedClockText(clockView, getContext(), model.clockText, compactClock);
        int infoColor = resolveMaterialInfoColor(getContext());
        dateView.setText(model.dateText);
        applyWeatherIcon(dateView, model.weather, infoColor);
        batteryView.setText(model.batteryText);
        chargeBoltView.setVisibility(model.batteryCharging ? View.VISIBLE : View.GONE);
        batteryRow.setVisibility(TextUtils.isEmpty(model.batteryText) ? View.GONE : View.VISIBLE);
        applyBurnInTranslation();
        PixelAodLog.log("AOD time update trace=" + currentAodTraceId()
                + " text=" + model.clockText.replace('\n', '/')
                + " previous=" + String.valueOf(previousClockText).replace('\n', '/')
                + " changed=" + !TextUtils.equals(previousClockText, model.clockText)
                + " visibility=" + getVisibility()
                + " shown=" + isShown()
                + " state={" + describeAodState(getContext()) + "}");
        requestAodFrameRefresh("updateTime");
    }

    private void requestAodFrameRefresh(String source) {
        try {
            requestLayout();
            invalidate();
            postInvalidateOnAnimation();
            invalidateView(clockView);
            invalidateView(dateView);
            invalidateView(mediaRow);
            invalidateView(mediaIconView);
            invalidateView(mediaView);
            invalidateView(notificationIconRow);
            invalidateView(batteryRow);
            View root = getRootView();
            if (root != null && root != this) {
                root.invalidate();
                root.postInvalidateOnAnimation();
            }
            PixelAodLog.log("requested AOD frame refresh trace=" + currentAodTraceId()
                    + " source=" + source
                    + " visibility=" + getVisibility()
                    + " shown=" + isShown()
                    + " parent=" + (getParent() != null ? getParent().getClass().getName() : "null")
                    + " root=" + (root != null ? root.getClass().getName() : "null")
                    + " clock=" + String.valueOf(clockView.getText()).replace('\n', '/')
                    + " state={" + describeAodState(getContext()) + "}");
        } catch (Throwable t) {
            PixelAodLog.log("failed to request AOD frame refresh source=" + source, t);
        }
    }

    private static void invalidateView(View view) {
        if (view == null) {
            return;
        }
        view.invalidate();
        view.postInvalidateOnAnimation();
    }

    private void applyBurnInTranslation() {
        if (getVisibility() != View.VISIBLE
                || !shouldApplyModuleAodNow(getContext(), "burn-in")) {
            resetBurnInTranslation();
            return;
        }
        long now = android.os.SystemClock.uptimeMillis();
        if (PixelAodSettings.getBoolean(getContext(),
                PixelAodSettings.KEY_DISABLE_BURN_IN_OFFSET, false)) {
            setTranslationX(0f);
            setTranslationY(0f);
            rememberBurnInTranslation(0f, 0f);
            return;
        }
        if (isWithinBurnInSettleWindow(now)) {
            setTranslationX(0f);
            setTranslationY(0f);
            rememberBurnInTranslation(0f, 0f);
            return;
        }
        BurnInOffset offset = computePixelLikeBurnInOffset();
        setTranslationX(offset.translationX);
        setTranslationY(offset.translationY);
        rememberBurnInTranslation(offset.translationX, offset.translationY);
        PixelAodLog.log("applied Pixel AOD burn-in offset trace=" + currentAodTraceId()
                + " x=" + Math.round(offset.translationX)
                + " y=" + Math.round(offset.translationY)
                + " periodX=" + Math.round(BURN_IN_PREVENTION_PERIOD_X_MINUTES)
                + " periodY=" + Math.round(BURN_IN_PREVENTION_PERIOD_Y_MINUTES)
                + " state={" + describeAodState(getContext()) + "}");
    }

    private BurnInOffset computePixelLikeBurnInOffset() {
        long wallTimeMillis = System.currentTimeMillis();
        int maxX = dp(BURN_IN_OFFSET_X_DP);
        int maxY = dp(BURN_IN_OFFSET_Y_DP);
        float x = burnInOffset(wallTimeMillis, maxX, true) - maxX / 2f;
        float y = burnInOffset(wallTimeMillis, maxY, false) - maxY / 2f;
        return new BurnInOffset(x, y);
    }

    private static int burnInOffset(long wallTimeMillis, int maxOffset, boolean xAxis) {
        if (maxOffset <= 0) {
            return 0;
        }
        float period = xAxis
                ? BURN_IN_PREVENTION_PERIOD_X_MINUTES
                : BURN_IN_PREVENTION_PERIOD_Y_MINUTES;
        return Math.round(zigzag(wallTimeMillis / 60000f, maxOffset, period));
    }

    private static void rememberBurnInTranslation(float x, float y) {
        synchronized (PixelAodClockView.class) {
            lastBurnInTranslationX = x;
            lastBurnInTranslationY = y;
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
        setAodPresentationState(compact, currentClockWeight);
        FrameLayout.LayoutParams clockParams = (FrameLayout.LayoutParams) clockView.getLayoutParams();
        if (compact) {
            applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    scaledClockTextDp(getContext(), SMALL_CLOCK_TEXT_DP), true);
            clockParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            clockParams.gravity = Gravity.TOP | Gravity.START;
            clockParams.leftMargin = dp(INFO_EDGE_DP - COMPACT_CLOCK_VISUAL_START_OFFSET_DP);
            clockParams.topMargin = dp(SMALL_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, COMPACT_INFO_TEXT_DP);
        } else {
            applySharedClockTextStyle(clockView, getContext(), currentClockWeight,
                    scaledClockTextDp(getContext(), LARGE_CLOCK_TEXT_DP), false);
            clockParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clockParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            clockParams.leftMargin = 0;
            clockParams.topMargin = dp(LARGE_CLOCK_TOP_DP);
            dateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, LARGE_INFO_TEXT_DP);
        }
        clockView.setLayoutParams(clockParams);
        syncNotificationOverflowStyle(notificationOverflowView, dateView, notificationIconRow);
        updateInfoStackLayout();
        updateTime();
        PixelAodLog.log("applied Pixel AOD clock mode trace=" + currentAodTraceId()
                + " compact=" + compact
                + " changed=" + changed
                + " weight=" + currentClockWeight
                + " clockTop=" + clockParams.topMargin
                + " state={" + describeAodState(getContext(), compactClock, currentClockWeight) + "}");
        if (changed) {
            PixelAodLog.log("applied Pixel AOD clock style source=mode-change compact=" + compact
                    + " weight=" + currentClockWeight
                    + " variation=" + sharedClockFontVariationSettings(currentClockWeight)
                    + " typeface=builder"
                    + " trace=" + currentAodTraceId());
        }
    }

    private void applyStableAodClockWeight(String source) {
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
            clockWeightAnimator = null;
        }
        int targetWeight = aodClockWeight(getContext());
        setClockWeight(targetWeight, true);
        // Stable AOD weight means handoff is done — re-present must not park at LS weight.
        aodWeightHandoffSettled = true;
        PixelAodLog.log("applied stable AOD clock weight source=" + source
                + " weight=" + targetWeight
                + " settled=true"
                + " variation=" + sharedClockFontVariationSettings(targetWeight)
                + " typeface=builder"
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(getContext(), compactClock, targetWeight) + "}");
    }

    private void beginLockscreenToAodWeightTransition(String source) {
        beginLockscreenToAodWeightTransition(source, null);
    }

    private void beginLockscreenToAodWeightTransition(String source, Runnable onFinished) {
        beginLockscreenToAodWeightTransition(source, onFinished,
                lockscreenClockWeight(getContext()));
    }

    private void beginLockscreenToAodWeightTransition(String source, Runnable onFinished,
            int requestedFromWeight) {
        if (clockWeightAnimator != null) {
            clockWeightAnimator.cancel();
        }
        int fromWeight = normalizeClockWeight(requestedFromWeight);
        int toWeight = aodClockWeight(getContext());
        if (fromWeight == toWeight) {
            setClockWeight(toWeight, true);
            PixelAodLog.log("skipped AOD clock weight transition source=" + source
                    + " reason=equal-weight weight=" + toWeight
                    + " trace=" + currentAodTraceId()
                    + " state={" + describeAodState(getContext(), compactClock, toWeight) + "}");
            if (onFinished != null) {
                postOnAnimation(onFinished);
            }
            return;
        }
        setClockWeight(fromWeight, true);
        clockWeightAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f);
        clockWeightAnimator.setDuration(700L);
        clockWeightAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.4f));
        final boolean[] cancelled = {false};
        clockWeightAnimator.addUpdateListener(animation -> {
            float progress = (Float) animation.getAnimatedValue();
            int weight = Math.round(fromWeight + ((toWeight - fromWeight) * progress));
            setClockWeight(weight);
        });
        clockWeightAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                cancelled[0] = true;
                clockWeightAnimator = null;
                aodWeightHandoffSettled = false;
                PixelAodLog.log("cancelled AOD clock weight transition source=" + source
                        + " fromWeight=" + fromWeight
                        + " toWeight=" + toWeight
                        + " settled=false"
                        + " trace=" + currentAodTraceId()
                        + " state={" + describeAodState(getContext(), compactClock, currentClockWeight) + "}");
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (cancelled[0]) {
                    return;
                }
                // The final animator update can already carry toWeight. Reapply the
                // prebuilt Typeface so OOS cannot retain a transient system fallback.
                setClockWeight(toWeight, true);
                clockWeightAnimator = null;
                aodWeightHandoffSettled = true;
                PixelAodLog.log("finished AOD clock weight transition source=" + source
                        + " toWeight=" + toWeight
                        + " settled=true"
                        + " trace=" + currentAodTraceId()
                        + " state={" + describeAodState(getContext(), compactClock, currentClockWeight) + "}");
                if (onFinished != null) {
                    postOnAnimation(onFinished);
                }
            }
        });
        aodWeightHandoffSettled = false;
        clockWeightAnimator.start();
        PixelAodLog.log("started AOD clock weight transition source=" + source
                + " fromWeight=" + fromWeight
                + " fromVariation=" + sharedClockFontVariationSettings(fromWeight)
                + " toWeight=" + toWeight
                + " toVariation=" + sharedClockFontVariationSettings(toWeight)
                + " typeface=builder"
                + " settled=false"
                + " trace=" + currentAodTraceId()
                + " state={" + describeAodState(getContext(), compactClock, fromWeight) + "}");
    }

    private void setClockWeight(int weight) {
        setClockWeight(weight, false);
    }

    int clockPluginWeight() {
        return currentClockWeight;
    }

    boolean isCompactClockMode() {
        return compactClock;
    }

    String clockPluginDiagnosticState() {
        return "{compact=" + compactClock
                + ",weight=" + currentClockWeight
                + ",layer=" + describeViewForHandoff(this)
                + ",clock=" + describeClockTextView(clockView)
                + ',' + describeViewForHandoff(clockView)
                + '}';
    }

    private void setClockWeight(int weight, boolean forceTypeface) {
        weight = normalizeClockWeight(weight);
        if (!forceTypeface && currentClockWeight == weight) {
            return;
        }
        currentClockWeight = weight;
        setAodPresentationState(compactClock, weight);
        applySharedClockTypeface(clockView, getContext(), weight);
        applySharedClockLetterSpacing(clockView, compactClock);
        if (PixelAodLog.isDebugEnabled()) {
            PixelAodLog.log("clock paint snapshot layer=aod requestedWeight=" + weight
                    + " state=" + describeClockTextView(clockView));
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
                    ? LARGE_MEDIA_WITH_NOTIFICATIONS_TOP_DP : LARGE_MEDIA_TOP_DP);
        }
        dateView.setLayoutParams(dateParams);
        notificationIconRow.setLayoutParams(notificationParams);
        mediaRow.setLayoutParams(mediaParams);
    }

    private static String currentAtAGlanceExtra() {
        synchronized (PixelAodClockView.class) {
            return atAGlanceExtra;
        }
    }

    private BatteryStatus readBatteryStatus() {
        return readBatteryStatus(getContext());
    }

    private static BatteryStatus readBatteryStatus(Context context) {
        if (context == null) {
            return BatteryStatus.empty();
        }
        try {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
            boolean charging = (status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0)
                    && status != BatteryManager.BATTERY_STATUS_FULL;
            int levelPercent = Math.round(level * 100f / scale);
            boolean systemLow = intent.getBooleanExtra("battery_low", false);
            boolean lowBattery = systemLow
                    || levelPercent <= LOW_BATTERY_AOD_SUPPRESS_THRESHOLD_PERCENT;
            return new BatteryStatus(percent, charging, true, levelPercent, lowBattery, systemLow, plugged, status);
        } catch (Throwable t) {
            return BatteryStatus.empty();
        }
    }

    static Typeface sharedClockTypeface(Context context, int weight) {
        Typeface weightedTypeface = resolveClockTypeface(context, normalizeClockWeight(weight));
        return weightedTypeface != null ? weightedTypeface : resolveClockTypeface(context);
    }

    static Typeface sharedInfoTypeface(Context context, int weight) {
        return resolveInfoTypeface(context);
    }

    static int aodClockWeight() {
        return aodClockWeight(appContext);
    }

    static int aodClockWeight(Context context) {
        // Must match SettingsActivity slider range (100f..500f). Old min=160 silently
        // ignored user AOD weight=100 and logged 160.
        return PixelAodSettings.getIntFromFloat(context, PixelAodSettings.KEY_AOD_WEIGHT,
                PixelAodSettings.DEFAULT_AOD_WEIGHT, 100, 500);
    }

    static int lockscreenClockWeight(Context context) {
        // Must match SettingsActivity slider range (100f..500f).
        return PixelAodSettings.getIntFromFloat(context, PixelAodSettings.KEY_LOCKSCREEN_WEIGHT,
                PixelAodSettings.DEFAULT_LOCKSCREEN_WEIGHT, 100, 500);
    }

    static int scaledClockTextDp(Context context, int baseDp) {
        return baseDp;
    }

    static void applySharedFontVariation(TextView textView, int weight) {
        applyFontVariation(textView, weight);
    }

    static void applySharedClockTypeface(TextView textView, Context context, int weight) {
        if (textView == null) {
            return;
        }
        boolean appliedWeightedTypeface = false;
        try {
            Typeface typeface = resolveClockTypeface(context, normalizeClockWeight(weight));
            if (typeface != null) {
                // Typeface.Builder already owns these axes. Reapplying them through TextView
                // makes OOS re-resolve the file font through its system fallback mid-animation.
                clearTextViewFontVariation(textView);
                textView.setTypeface(typeface);
                textView.getPaint().setFakeBoldText(false);
                appliedWeightedTypeface = true;
            } else {
                textView.setTypeface(resolveClockTypeface(context));
            }
        } catch (Throwable ignored) {
            // Typeface selection is best-effort across OEM font stacks.
        }
        if (!appliedWeightedTypeface) {
            applySharedFontVariation(textView, weight);
            textView.getPaint().setFakeBoldText(false);
        }
    }

    static String describeClockTextView(TextView textView) {
        if (textView == null) {
            return "null";
        }
        Typeface typeface = textView.getTypeface();
        int actualWeight = typeface != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? typeface.getWeight() : -1;
        return "actualWeight=" + actualWeight
                + " style=" + (typeface != null ? typeface.getStyle() : -1)
                + " typeface=" + (typeface != null
                        ? typeface.getClass().getName() + '@'
                                + Integer.toHexString(System.identityHashCode(typeface))
                        : "null")
                + " fakeBold=" + textView.getPaint().isFakeBoldText()
                + " variation=" + textView.getFontVariationSettings()
                + " textScaleX=" + textView.getTextScaleX()
                + " alpha=" + textView.getAlpha()
                + " visibility=" + textView.getVisibility();
    }

    static String describeViewForHandoff(View view) {
        if (view == null) {
            return "null";
        }
        return view.getClass().getSimpleName()
                + '@' + Integer.toHexString(System.identityHashCode(view))
                + "{id=" + view.getId()
                + ",visibility=" + view.getVisibility()
                + ",alpha=" + view.getAlpha()
                + ",shown=" + view.isShown()
                + ",attached=" + view.isAttachedToWindow()
                + ",size=" + view.getWidth() + 'x' + view.getHeight()
                + ",position=" + view.getX() + ',' + view.getY()
                + ",translation=" + view.getTranslationX() + ',' + view.getTranslationY()
                + ",scale=" + view.getScaleX() + ',' + view.getScaleY()
                + ",rotation=" + view.getRotation()
                + ",pivot=" + view.getPivotX() + ',' + view.getPivotY()
                + ",matrix=" + view.getMatrix().toShortString()
                + ",layerType=" + view.getLayerType()
                + ",hardware=" + view.isHardwareAccelerated()
                + '}';
    }

    static void applySharedClockTextStyle(TextView textView, Context context, int weight,
            int textSizeDp, boolean compact) {
        if (textView == null) {
            return;
        }
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeDp);
        applySharedClockTypeface(textView, context, weight);
        textView.setIncludeFontPadding(false);
        textView.setElegantTextHeight(false);
        textView.setAllCaps(false);
        textView.setPadding(0, 0, 0, 0);
        textView.setTextScaleX(1f);
        textView.getPaint().setSubpixelText(true);
        textView.setGravity(compact ? Gravity.START : Gravity.CENTER_HORIZONTAL);
        textView.setTextAlignment(compact ? View.TEXT_ALIGNMENT_TEXT_START
                : View.TEXT_ALIGNMENT_CENTER);
        textView.setSingleLine(compact);
        textView.setLines(compact ? 1 : 2);
        textView.setLineSpacing(0f, compact ? 1f : CLOCK_LINE_SPACING);
        applySharedClockLetterSpacing(textView, compact);
        textView.setFontFeatureSettings("tnum");
    }

    static void applySharedClockLetterSpacing(TextView textView, boolean compact) {
        if (textView == null) {
            return;
        }
        // Clock text uses fixed-advance spans so variable-font weight frames cannot reflow.
        textView.setLetterSpacing(0f);
    }

    static void applySharedClockText(TextView textView, Context context, CharSequence text,
            boolean compact) {
        if (textView == null) {
            return;
        }
        CharSequence normalizedText = text != null ? text : "";
        if (normalizedText.length() == 0) {
            textView.setText(normalizedText);
            return;
        }

        Paint referencePaint = new Paint(textView.getPaint());
        Typeface referenceTypeface = resolveClockTypeface(context, lockscreenClockWeight(context));
        if (referenceTypeface != null) {
            referencePaint.setTypeface(referenceTypeface);
        }
        referencePaint.setTextScaleX(1f);
        referencePaint.setLetterSpacing(0f);
        float letterSpacingPixels = referencePaint.getTextSize()
                * (compact ? COMPACT_CLOCK_LETTER_SPACING : LARGE_CLOCK_LETTER_SPACING);
        SpannableString fixedText = new SpannableString(normalizedText);
        for (int index = 0; index < normalizedText.length(); index++) {
            char glyph = normalizedText.charAt(index);
            if (glyph == '\n') {
                continue;
            }
            boolean lineEnd = index + 1 >= normalizedText.length()
                    || normalizedText.charAt(index + 1) == '\n';
            float referenceAdvance = referencePaint.measureText(normalizedText, index, index + 1);
            fixedText.setSpan(new FixedClockAdvanceSpan(referenceAdvance, letterSpacingPixels,
                            lineEnd),
                    index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView.setLetterSpacing(0f);
        textView.setText(fixedText, TextView.BufferType.SPANNABLE);
    }

    private static final class FixedClockAdvanceSpan extends ReplacementSpan {
        private final float referenceGlyphAdvance;
        private final float cellAdvance;

        FixedClockAdvanceSpan(float referenceGlyphAdvance, float letterSpacingPixels,
                boolean lineEnd) {
            this.referenceGlyphAdvance = referenceGlyphAdvance;
            cellAdvance = ClockGlyphMetrics.cellAdvance(
                    referenceGlyphAdvance, letterSpacingPixels, lineEnd);
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end,
                Paint.FontMetricsInt fontMetrics) {
            if (fontMetrics != null) {
                Paint.FontMetricsInt source = paint.getFontMetricsInt();
                fontMetrics.top = source.top;
                fontMetrics.ascent = source.ascent;
                fontMetrics.descent = source.descent;
                fontMetrics.bottom = source.bottom;
                fontMetrics.leading = source.leading;
            }
            return Math.max(1, Math.round(cellAdvance));
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x,
                int top, int y, int bottom, Paint paint) {
            float animatedAdvance = paint.measureText(text, start, end);
            float offset = ClockGlyphMetrics.centerOffset(referenceGlyphAdvance, animatedAdvance);
            canvas.drawText(text, start, end, x + offset, y, paint);
        }
    }

    private static Typeface resolveClockTypeface(Context context) {
        synchronized (PixelAodClockView.class) {
            if (cachedClockTypeface != null) {
                return cachedClockTypeface;
            }
            cachedClockTypeface = loadGoogleSansFlex(context);
            if (cachedClockTypeface != null) {
                cachedClockTypefaceFromBundledFont = true;
                PixelAodLog.i("loaded bundled Google Sans Flex Variable base Typeface for clock");
                return cachedClockTypeface;
            }
            cachedClockTypefaceFromBundledFont = false;
            cachedClockTypeface = firstUsableTypeface(
                    "google-sans-flex",
                    "roboto-flex",
                    "google-sans-text",
                    "google-sans",
                    "sans-serif");
            PixelAodLog.i("bundled Google Sans Flex unavailable; using system sans fallback for clock");
            return cachedClockTypeface;
        }
    }

    private static Typeface resolveClockTypeface(Context context, int weight) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null;
        }
        weight = normalizeClockWeight(weight);
        synchronized (PixelAodClockView.class) {
            Typeface cached = cachedClockTypefaceByWeight.get(weight);
            if (cached != null) {
                return cached;
            }
        }
        File fontFile = googleSansFlexFontFile(context);
        if (fontFile == null) {
            return null;
        }
        Typeface typeface = buildGoogleSansFlexTypeface(fontFile, weight, "weight-" + weight);
        if (typeface != null) {
            synchronized (PixelAodClockView.class) {
                cachedClockTypefaceByWeight.put(weight, typeface);
            }
        }
        return typeface;
    }

    private static Typeface resolveInfoTypeface(Context context) {
        synchronized (PixelAodClockView.class) {
            if (cachedInfoTypeface != null) {
                return cachedInfoTypeface;
            }
            cachedInfoTypeface = loadGoogleSansFlex(context);
            if (cachedInfoTypeface != null) {
                PixelAodLog.i("loaded bundled Google Sans Flex Variable base Typeface for info");
                return cachedInfoTypeface;
            }
            cachedInfoTypeface = firstUsableTypeface(
                    "google-sans-flex",
                    "google-sans-text",
                    "google-sans",
                    "sans-serif-medium");
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

    private static Typeface loadGoogleSansFlex(Context context) {
        File fontFile = googleSansFlexFontFile(context);
        if (fontFile == null) {
            return null;
        }
        return buildGoogleSansFlexTypeface(fontFile, GOOGLE_SANS_FLEX_BASE_WEIGHT, "base");
    }

    private static Typeface buildGoogleSansFlexTypeface(File fontFile, int weight, String source) {
        if (fontFile == null) {
            return null;
        }
        try {
            Typeface typeface = new Typeface.Builder(fontFile)
                    .setFontVariationSettings(sharedClockFontVariationSettings(weight))
                    .build();
            if (typeface == null) {
                PixelAodLog.i("Google Sans Flex Typeface.Builder returned null source=" + source
                        + " path=" + fontFile.getAbsolutePath());
            }
            return typeface;
        } catch (Throwable t) {
            PixelAodLog.e("failed to build Google Sans Flex Typeface source=" + source
                    + " path=" + fontFile.getAbsolutePath(), t);
            return null;
        }
    }

    private static File googleSansFlexFontFile(Context context) {
        if (context == null) {
            return null;
        }
        String apkPath = resolveModuleApkPath(context);
        if (TextUtils.isEmpty(apkPath)) {
            return fallbackGoogleSansFlexFontFile(context);
        }
        File fontFile = new File(context.getCacheDir(), googleSansFlexCacheFileName(apkPath));
        if (isUsableFontFile(fontFile)) {
            return fontFile;
        }
        if (ensureExtractedAsset(apkPath, GOOGLE_SANS_FLEX_VARIABLE_ASSET, fontFile)) {
            return fontFile;
        }
        return fallbackGoogleSansFlexFontFile(context);
    }

    private static String resolveModuleApkPath(Context context) {
        String cachedPath;
        synchronized (PixelAodClockView.class) {
            cachedPath = modulePath;
        }
        if (!TextUtils.isEmpty(cachedPath) && new File(cachedPath).isFile()) {
            return cachedPath;
        }
        return refreshModuleApkPath(context, cachedPath);
    }

    private static String refreshModuleApkPath(Context context, String previousPath) {
        if (context == null) {
            return null;
        }
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
            String resolvedPath = info != null ? info.sourceDir : null;
            if (TextUtils.isEmpty(resolvedPath)) {
                return null;
            }
            boolean changed = !TextUtils.equals(previousPath, resolvedPath);
            synchronized (PixelAodClockView.class) {
                modulePath = resolvedPath;
                if (changed) {
                    cachedClockTypeface = null;
                    cachedClockTypefaceFromBundledFont = false;
                    cachedInfoTypeface = null;
                    cachedClockTypefaceByWeight.clear();
                }
            }
            if (changed) {
                PixelAodLog.log("refreshed module APK path old=" + previousPath + " new=" + resolvedPath);
            }
            return resolvedPath;
        } catch (Throwable t) {
            PixelAodLog.log("failed to refresh module APK path", t);
            return null;
        }
    }

    private static String googleSansFlexCacheFileName(String apkPath) {
        return GOOGLE_SANS_FLEX_VARIABLE_CACHE_PREFIX
                + Integer.toHexString(apkPath.hashCode())
                + ".ttf";
    }

    private static File fallbackGoogleSansFlexFontFile(Context context) {
        File legacyFontFile = new File(context.getCacheDir(), GOOGLE_SANS_FLEX_VARIABLE_CACHE);
        if (isUsableFontFile(legacyFontFile)) {
            return legacyFontFile;
        }
        File[] files = context.getCacheDir().listFiles();
        if (files == null) {
            return null;
        }
        File newestMatch = null;
        long newestModified = Long.MIN_VALUE;
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith(GOOGLE_SANS_FLEX_VARIABLE_CACHE_PREFIX) || !name.endsWith(".ttf")) {
                continue;
            }
            if (!isUsableFontFile(file)) {
                continue;
            }
            long modified = file.lastModified();
            if (newestMatch == null || modified > newestModified) {
                newestMatch = file;
                newestModified = modified;
            }
        }
        return newestMatch;
    }

    private static boolean isUsableFontFile(File file) {
        return file != null && file.isFile() && file.length() > 0L;
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
            PixelAodLog.log("failed to extract bundled Google Sans Flex apkPath=" + apkPath
                    + " entry=" + entryName, t);
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

    private static void applyFontVariation(TextView textView, int weight) {
        if (textView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            textView.setFontVariationSettings(sharedClockFontVariationSettings(weight));
        } catch (Throwable ignored) {
            // Some fonts ignore variation axes; the base typeface remains usable.
        }
    }

    private static void clearTextViewFontVariation(TextView textView) {
        if (textView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        try {
            textView.setFontVariationSettings(null);
        } catch (Throwable ignored) {
            // The Typeface itself already carries the desired variation.
        }
    }

    static String sharedClockFontVariationSettings(int weight) {
        return "'opsz' 144, 'wght' " + normalizeClockWeight(weight)
                + ", 'GRAD' 0, 'ROND' 0, 'wdth' 100";
    }

    private static int normalizeClockWeight(int weight) {
        // Align with Settings sliders (100–500). Previous floor 160 made AOD weight=100
        // still render as wght 160 in typeface/variation.
        return Math.max(100, Math.min(500, weight));
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
        final boolean valid;
        final int levelPercent;
        final boolean lowBattery;
        final boolean systemLowBattery;
        final int plugged;
        final int status;

        BatteryStatus(String percentText, boolean charging, boolean valid, int levelPercent,
                boolean lowBattery, boolean systemLowBattery, int plugged, int status) {
            this.percentText = percentText;
            this.charging = charging;
            this.valid = valid;
            this.levelPercent = levelPercent;
            this.lowBattery = lowBattery;
            this.systemLowBattery = systemLowBattery;
            this.plugged = plugged;
            this.status = status;
        }

        static BatteryStatus empty() {
            return new BatteryStatus("", false, false, -1, false, false, 0,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
        }

        boolean shouldSuppressAodForLowBattery() {
            return valid && lowBattery && !charging;
        }

        String describeForLog() {
            return "valid=" + valid
                    + ",level=" + levelPercent
                    + ",charging=" + charging
                    + ",low=" + lowBattery
                    + ",systemLow=" + systemLowBattery
                    + ",plugged=" + plugged
                    + ",status=" + status;
        }
    }

    static final class WeatherSnapshot {
        final String temperatureText;
        final int weatherCode;
        final String conditionText;
        final long timestampMillis;
        final long sunriseMillis;
        final long sunsetMillis;

        WeatherSnapshot(String temperatureText, int weatherCode, String conditionText, long timestampMillis) {
            this(temperatureText, weatherCode, conditionText, timestampMillis, 0L, 0L);
        }

        WeatherSnapshot(String temperatureText, int weatherCode, String conditionText, long timestampMillis,
                long sunriseMillis, long sunsetMillis) {
            this.temperatureText = temperatureText;
            this.weatherCode = weatherCode;
            this.conditionText = normalizeWeatherCondition(conditionText);
            this.timestampMillis = timestampMillis;
            this.sunriseMillis = sunriseMillis;
            this.sunsetMillis = sunsetMillis;
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

        boolean hasSunTimes() {
            return sunriseMillis > 0L && sunsetMillis > 0L;
        }

        /**
         * True if Breezy Weather's day/night boundaries are known. When true,
         * callers should prefer this over local hour-based checks so the icon
         * matches the host app's own day/night logic.
         */
        boolean isNightAt(long nowMillis) {
            if (!hasSunTimes()) {
                return false;
            }
            java.util.Calendar now = java.util.Calendar.getInstance();
            now.setTimeInMillis(nowMillis);
            int nowMins = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 + now.get(java.util.Calendar.MINUTE);
            
            java.util.Calendar sunrise = java.util.Calendar.getInstance();
            sunrise.setTimeInMillis(sunriseMillis);
            int sunRiseMins = sunrise.get(java.util.Calendar.HOUR_OF_DAY) * 60 + sunrise.get(java.util.Calendar.MINUTE);
            
            java.util.Calendar sunset = java.util.Calendar.getInstance();
            sunset.setTimeInMillis(sunsetMillis);
            int sunSetMins = sunset.get(java.util.Calendar.HOUR_OF_DAY) * 60 + sunset.get(java.util.Calendar.MINUTE);
            
            return nowMins < sunRiseMins || nowMins >= sunSetMins;
        }

        boolean sameDisplay(WeatherSnapshot other) {
            return other != null
                    && TextUtils.equals(temperatureText, other.temperatureText)
                    && weatherCode == other.weatherCode
                    && TextUtils.equals(conditionText, other.conditionText)
                    && sunriseMillis == other.sunriseMillis
                    && sunsetMillis == other.sunsetMillis;
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
            return from(temperatureText, weatherCode, conditionText, timestampMillis, 0L, 0L);
        }

        static WeatherSnapshot from(String temperatureText, int weatherCode,
                String conditionText, long timestampMillis, long sunriseMillis, long sunsetMillis) {
            if (weatherCode == Integer.MIN_VALUE) {
                weatherCode = inferWeatherCode(conditionText);
            }
            return new WeatherSnapshot(temperatureText, weatherCode, conditionText, timestampMillis,
                    sunriseMillis, sunsetMillis);
        }

        static WeatherSnapshot empty() {
            return new WeatherSnapshot("", Integer.MIN_VALUE, "", 0L, 0L, 0L);
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

    private boolean isWithinBurnInSettleWindow(long now) {
        synchronized (PixelAodClockView.class) {
            return isRecentUptime(now, lastScreenOffAt, BURN_IN_SETTLE_MILLIS)
                    || isRecentUptime(now, lastAodActivatedAt, BURN_IN_SETTLE_MILLIS);
        }
    }

    private static final class SystemNotificationGlyphDrawable extends Drawable {
        static final int TYPE_CHECK = 1;
        static final int TYPE_NETWORK = 2;
        static final int TYPE_FLASHLIGHT = 3;
        static final int TYPE_TIMER = 4;
        static final int TYPE_LIVE_ALERT = 5;

        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int type;
        private int alpha = 255;

        SystemNotificationGlyphDrawable(int color, int type) {
            this.type = type;
            stroke.setColor(color);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            fill.setColor(color);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            if (size <= 0f) {
                return;
            }
            int save = canvas.save();
            canvas.translate(bounds.left + (bounds.width() - size) / 2f,
                    bounds.top + (bounds.height() - size) / 2f);
            stroke.setAlpha(alpha);
            fill.setAlpha(alpha);
            stroke.setStrokeWidth(Math.max(2.2f, size * 0.085f));
            if (type == TYPE_NETWORK) {
                drawNetwork(canvas, size);
            } else if (type == TYPE_FLASHLIGHT) {
                drawFlashlight(canvas, size);
            } else if (type == TYPE_TIMER) {
                drawTimer(canvas, size);
            } else if (type == TYPE_LIVE_ALERT) {
                drawLiveAlert(canvas, size);
            } else {
                drawCheck(canvas, size);
            }
            canvas.restoreToCount(save);
        }

        private void drawCheck(Canvas canvas, float size) {
            canvas.drawCircle(size * 0.50f, size * 0.50f, size * 0.34f, stroke);
            Path check = new Path();
            check.moveTo(size * 0.34f, size * 0.51f);
            check.lineTo(size * 0.46f, size * 0.63f);
            check.lineTo(size * 0.68f, size * 0.38f);
            canvas.drawPath(check, stroke);
        }

        private void drawNetwork(Canvas canvas, float size) {
            RectF large = new RectF(size * 0.18f, size * 0.20f, size * 0.82f, size * 0.84f);
            RectF middle = new RectF(size * 0.30f, size * 0.36f, size * 0.70f, size * 0.76f);
            RectF small = new RectF(size * 0.42f, size * 0.52f, size * 0.58f, size * 0.68f);
            canvas.drawArc(large, 220f, 100f, false, stroke);
            canvas.drawArc(middle, 220f, 100f, false, stroke);
            canvas.drawArc(small, 220f, 100f, false, stroke);
            canvas.drawPoint(size * 0.50f, size * 0.78f, stroke);
        }

        private void drawFlashlight(Canvas canvas, float size) {
            int save = canvas.save();
            canvas.rotate(-22f, size * 0.50f, size * 0.52f);
            RectF head = new RectF(size * 0.35f, size * 0.14f, size * 0.65f, size * 0.30f);
            RectF body = new RectF(size * 0.40f, size * 0.30f, size * 0.60f, size * 0.78f);
            canvas.drawRoundRect(head, size * 0.04f, size * 0.04f, stroke);
            canvas.drawRoundRect(body, size * 0.08f, size * 0.08f, stroke);
            canvas.drawCircle(size * 0.50f, size * 0.66f, size * 0.035f, fill);
            canvas.restoreToCount(save);

            canvas.drawLine(size * 0.66f, size * 0.16f, size * 0.82f, size * 0.06f, stroke);
            canvas.drawLine(size * 0.72f, size * 0.32f, size * 0.90f, size * 0.34f, stroke);
        }

        private void drawTimer(Canvas canvas, float size) {
            canvas.drawCircle(size * 0.50f, size * 0.58f, size * 0.30f, stroke);
            canvas.drawLine(size * 0.42f, size * 0.18f, size * 0.58f, size * 0.18f, stroke);
            canvas.drawLine(size * 0.50f, size * 0.18f, size * 0.50f, size * 0.27f, stroke);
            canvas.drawLine(size * 0.70f, size * 0.30f, size * 0.78f, size * 0.23f, stroke);
            canvas.drawLine(size * 0.50f, size * 0.58f, size * 0.50f, size * 0.42f, stroke);
            canvas.drawLine(size * 0.50f, size * 0.58f, size * 0.63f, size * 0.66f, stroke);
        }

        private void drawLiveAlert(Canvas canvas, float size) {
            RectF capsule = new RectF(size * 0.22f, size * 0.32f, size * 0.78f, size * 0.68f);
            canvas.drawRoundRect(capsule, size * 0.18f, size * 0.18f, stroke);
            canvas.drawCircle(size * 0.40f, size * 0.50f, size * 0.045f, fill);
            canvas.drawCircle(size * 0.60f, size * 0.50f, size * 0.045f, fill);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            stroke.setColorFilter(colorFilter);
            fill.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return 64;
        }

        @Override
        public int getIntrinsicHeight() {
            return 64;
        }
    }

    private static final class UsbNotificationDrawable extends Drawable {
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path triangle = new Path();
        private final boolean debug;
        private int alpha = 255;

        UsbNotificationDrawable(int color, boolean debug) {
            this.debug = debug;
            stroke.setColor(color);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            fill.setColor(color);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            float size = Math.min(bounds.width(), bounds.height());
            if (size <= 0f) {
                return;
            }
            int save = canvas.save();
            canvas.translate(bounds.left + (bounds.width() - size) / 2f,
                    bounds.top + (bounds.height() - size) / 2f);
            stroke.setAlpha(alpha);
            fill.setAlpha(alpha);
            stroke.setStrokeWidth(Math.max(2f, size * 0.075f));

            float cx = size * 0.50f;
            canvas.drawLine(cx, size * 0.18f, cx, size * 0.78f, stroke);
            canvas.drawLine(cx, size * 0.42f, size * 0.28f, size * 0.42f, stroke);
            canvas.drawLine(size * 0.28f, size * 0.42f, size * 0.28f, size * 0.58f, stroke);
            canvas.drawLine(cx, size * 0.50f, size * 0.72f, size * 0.50f, stroke);
            canvas.drawLine(size * 0.72f, size * 0.50f, size * 0.72f, size * 0.30f, stroke);

            triangle.reset();
            triangle.moveTo(cx, size * 0.08f);
            triangle.lineTo(size * 0.42f, size * 0.24f);
            triangle.lineTo(size * 0.58f, size * 0.24f);
            triangle.close();
            canvas.drawPath(triangle, fill);
            canvas.drawCircle(size * 0.28f, size * 0.64f, size * 0.06f, fill);
            canvas.drawRect(size * 0.66f, size * 0.20f, size * 0.78f, size * 0.32f, fill);

            if (debug) {
                canvas.drawCircle(cx, size * 0.84f, size * 0.085f, fill);
            } else {
                canvas.drawRect(cx - size * 0.075f, size * 0.76f,
                        cx + size * 0.075f, size * 0.92f, fill);
            }
            canvas.restoreToCount(save);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            stroke.setColorFilter(colorFilter);
            fill.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return 64;
        }

        @Override
        public int getIntrinsicHeight() {
            return 64;
        }
    }

}
