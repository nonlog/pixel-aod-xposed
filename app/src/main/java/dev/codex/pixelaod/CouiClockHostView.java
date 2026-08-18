package dev.codex.pixelaod;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The single COUI_PORT primary clock owner.
 *
 * <p>This host is intentionally independent from the legacy Pixel clock classes. It owns the
 * glyphs, information slots, AOD content, battery slot, presentation state, burn-in input, and
 * every target animation for one ClockPlugin root.</p>
 */
final class CouiClockHostView extends FrameLayout {
    static final String LARGE_VARIATION = CouiClockFontPolicy.LARGE_VARIATION;
    static final String SMALL_VARIATION = CouiClockFontPolicy.SMALL_VARIATION;
    static final String AOD_LARGE_VARIATION = CouiClockFontPolicy.AOD_LARGE_VARIATION;
    static final String AOD_SMALL_VARIATION = CouiClockFontPolicy.AOD_SMALL_VARIATION;
    static final String TABULAR_NUMBERS = CouiClockFontPolicy.TABULAR_NUMBERS;
    static final String COLON = ":";
    static final long TARGET_TRANSITION_MS = CouiClockPresentationModel.TARGET_TRANSITION_MS;
    static final long LIVE_FADE_OUT_MS = CouiClockPresentationModel.LIVE_FADE_OUT_MS;
    static final long LIVE_FADE_IN_MS = CouiClockPresentationModel.LIVE_FADE_IN_MS;
    static final float COLON_START_FRACTION = CouiClockPresentationModel.COLON_START_FRACTION;
    static final float COLON_DURATION_FRACTION = CouiClockPresentationModel.COLON_DURATION_FRACTION;
    static final int MAX_NOTIFICATION_ICONS =
            CouiClockNotificationOverflowPolicy.MAX_VISIBLE_ICONS;

    private static final int INFORMATION_COLOR = Color.WHITE;
    private static final float LARGE_COLON_SCALE = 0.44f;
    private static final float COMPACT_COLON_TRACKING_SCALE = 0.55f;
    private static final float LARGE_LINE_WIDTH_RATIO = 0.82f;
    private static final float LARGE_INFO_WIDTH_MULTIPLIER = 1.9f;
    private static final float LARGE_INFO_SIDE_GAP_DP = 10f;
    private static final float BATTERY_Y_OFFSET_DP = 16f;

    private final PathInterpolator motionInterpolator =
            new PathInterpolator(CouiClockPresentationModel.MOTION_X1,
                    CouiClockPresentationModel.MOTION_Y1,
                    CouiClockPresentationModel.MOTION_X2,
                    CouiClockPresentationModel.MOTION_Y2);
    private final Calendar calendar = Calendar.getInstance();
    private final CouiClockTransitionGeneration transitionGeneration =
            new CouiClockTransitionGeneration();
    private final CouiClockTransitionGeneration liveAodTransitionGeneration =
            new CouiClockTransitionGeneration();
    private final CouiClockRomTextAnimatorRuntime morphRuntime;
    private final GlyphSet largeSet;
    private final GlyphSet compactSet;
    private final GlyphSet aodLargeSet;
    private final GlyphSet aodCompactSet;
    private final GlyphSet[] glyphSets;
    private final LinearLayout dateGroup;
    private final LinearLayout weatherGroup;
    private final TextView dateView;
    private final TextView weekView;
    private final ImageView weatherIconView;
    private final TextView weatherView;
    private final LinearLayout notificationIconRow;
    private final TextView notificationOverflowView;
    private final LinearLayout mediaGroup;
    private final TextView mediaTitleView;
    private final LinearLayout mediaSubtitleRow;
    private final ImageView mediaAppIconView;
    private final TextView mediaArtistView;
    private final TextView batteryView;
    private final BroadcastReceiver batteryReceiver;
    private final BroadcastReceiver timeReceiver;
    private final Map<View, CouiClockAppliedTargetPolicy.Glyph> appliedGlyphTargets =
            new IdentityHashMap<>();
    private final Map<View, CouiClockAppliedTargetPolicy.Information> appliedInformationTargets =
            new IdentityHashMap<>();

    private CouiClockPresentationModel presentation = new CouiClockPresentationModel(
            CouiClockPresentationModel.Scene.LARGE, false, false,
            CouiClockPresentationModel.AodContent.none());
    private AodData aodData = AodData.empty();
    private long lastMinute = Long.MIN_VALUE;
    private long pendingEntryToken;
    private boolean aodEntryInProgress;
    private boolean liveCrossfadeInProgress;
    private AodData deferredAodData;
    private CouiClockPresentationModel.AodContent deferredAodContent;
    private CouiClockPresentationModel.AodContent deferredLiveAodContent;
    private Runnable beginAodFrameRunnable;
    private Runnable finishAodEntryRunnable;
    private Runnable finishLiveCrossfadeRunnable;
    private ViewTreeObserver.OnPreDrawListener pendingLivePreDrawListener;
    private boolean liveAodRetargetPending;
    private Runnable pendingTargetApplyRunnable;
    private boolean pendingTargetApply;
    private boolean pendingTargetApplyAnimated;
    private ObjectAnimator colonAlphaAnimator;
    private float burnInX;
    private float burnInY;
    private boolean manualBurnIn;
    private boolean batteryReceiverRegistered;
    private boolean timeReceiverRegistered;
    private int lastBatteryLevel = -1;
    private Boolean lastBatteryCharging;
    private int clockBaseWidth;
    private String timeText = "0000";
    private int monetColor = Integer.MIN_VALUE;
    private int aodMonetColor = Integer.MIN_VALUE;
    private Boolean informationShadowApplied;
    private String diagnosticSource = "constructor";
    private String lastTargetDiagnosticSignature;

    CouiClockHostView(Context context) {
        this(context, context != null ? context.getClassLoader() : null);
    }

    CouiClockHostView(Context context, ClassLoader classLoader) {
        super(context);
        ClassLoader effectiveClassLoader = classLoader != null
                ? classLoader : context.getClassLoader();
        Typeface largeTypeface = CouiClockFontLoader.buildCustomFont(context, LARGE_VARIATION);
        morphRuntime = CouiClockRomTextAnimatorRuntime.create(context, effectiveClassLoader,
                largeTypeface);
        PixelAodLog.log("COUI host runtime decision rendererMode=COUI_PORT"
                + " morphRuntimeAvailable=" + (morphRuntime != null)
                + " glyphMode=" + CouiClockFontPolicy.glyphMode(
                CouiClockPresentationModel.Scene.LARGE, false, morphRuntime != null)
                + " fontVariation=" + LARGE_VARIATION);
        largeSet = createGlyphSet(LARGE_VARIATION, morphRuntime != null);
        compactSet = createGlyphSet(SMALL_VARIATION, false);
        aodLargeSet = createGlyphSet(AOD_LARGE_VARIATION, false);
        aodCompactSet = createGlyphSet(AOD_SMALL_VARIATION, false);
        glyphSets = new GlyphSet[]{largeSet, compactSet, aodLargeSet, aodCompactSet};

        dateGroup = new LinearLayout(context);
        dateGroup.setOrientation(LinearLayout.HORIZONTAL);
        dateGroup.setGravity(android.view.Gravity.CENTER_VERTICAL);
        dateGroup.setClipChildren(false);
        weekView = informationText(18f, 500);
        dateView = informationText(18f, 500);
        dateGroup.addView(dateView);
        dateGroup.addView(weekView);

        weatherGroup = new LinearLayout(context);
        weatherGroup.setOrientation(LinearLayout.HORIZONTAL);
        weatherGroup.setGravity(android.view.Gravity.CENTER_VERTICAL);
        weatherGroup.setClipChildren(false);
        weatherIconView = new ImageView(context);
        weatherIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        weatherIconView.setVisibility(INVISIBLE);
        LinearLayout.LayoutParams weatherIconParams = new LinearLayout.LayoutParams(
                dp(22), dp(22));
        weatherIconParams.setMarginEnd(dp(4));
        weatherGroup.addView(weatherIconView, weatherIconParams);
        weatherView = informationText(18f, 500);
        weatherGroup.addView(weatherView);

        notificationIconRow = new LinearLayout(context);
        notificationIconRow.setOrientation(LinearLayout.HORIZONTAL);
        notificationIconRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        notificationIconRow.setClipChildren(false);
        notificationIconRow.setAlpha(0f);
        notificationOverflowView = informationText(16f, 500);
        notificationOverflowView.setGravity(android.view.Gravity.CENTER);
        notificationOverflowView.setVisibility(GONE);
        LinearLayout.LayoutParams overflowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        overflowParams.setMarginStart(dp(16));
        notificationIconRow.addView(notificationOverflowView, overflowParams);

        mediaGroup = new LinearLayout(context);
        mediaGroup.setOrientation(LinearLayout.VERTICAL);
        mediaGroup.setGravity(android.view.Gravity.START);
        mediaGroup.setClipChildren(false);
        mediaGroup.setAlpha(0f);
        mediaTitleView = informationText(18f, 500);
        mediaTitleView.setGravity(android.view.Gravity.START);
        mediaTitleView.setMaxLines(1);
        mediaTitleView.setEllipsize(TextUtils.TruncateAt.END);
        mediaGroup.addView(mediaTitleView);
        mediaSubtitleRow = new LinearLayout(context);
        mediaSubtitleRow.setOrientation(LinearLayout.HORIZONTAL);
        mediaSubtitleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(4);
        mediaGroup.addView(mediaSubtitleRow, subtitleParams);
        mediaAppIconView = new ImageView(context);
        mediaAppIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams mediaIconParams = new LinearLayout.LayoutParams(dp(18), dp(18));
        mediaIconParams.setMarginEnd(dp(6));
        mediaSubtitleRow.addView(mediaAppIconView, mediaIconParams);
        mediaArtistView = informationText(15f, 450);
        mediaArtistView.setMaxLines(1);
        mediaArtistView.setEllipsize(TextUtils.TruncateAt.END);
        mediaSubtitleRow.addView(mediaArtistView);

        batteryView = informationText(16f, 500);
        batteryView.setAlpha(0f);

        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        GlyphSet[] attachedGlyphSets = morphRuntime != null
                ? new GlyphSet[]{largeSet} : glyphSets;
        for (GlyphSet glyphSet : attachedGlyphSets) {
            for (TextView digit : glyphSet.digits) {
                addView(digit, glyphLayoutParams());
            }
            addView(glyphSet.colon, glyphLayoutParams());
        }
        addView(dateGroup, glyphLayoutParams());
        addView(weatherGroup, glyphLayoutParams());
        addView(notificationIconRow, glyphLayoutParams());
        addView(mediaGroup, glyphLayoutParams());
        addView(batteryView, glyphLayoutParams());

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                updateBattery(intent);
            }
        };
        timeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                String action = intent == null ? null : intent.getAction();
                if (CouiClockTimeTickPolicy.acceptsAction(action)) {
                    lastMinute = Long.MIN_VALUE;
                    onTimeTick();
                }
            }
        };
        onTimeTick();
        setMediaData("", "", null);
        updateMonetColor();
        scheduleApplyTargets(false);
        if (morphRuntime != null) {
            morphRuntime.prewarmAsync(motionInterpolator);
        }
    }

    /** Applies the canonical scene/content state through the host's sole target path. */
    void present(CouiClockPresentationModel next, boolean animate, String source) {
        if (next == null) {
            return;
        }
        diagnosticSource = source == null ? "present" : source;
        if (aodEntryInProgress && next.dozing()) {
            deferAodContent(next.content());
            return;
        }
        if (liveCrossfadeInProgress && next.dozing() && next.partialAod()) {
            deferredLiveAodContent = normalizeContent(next.content());
            return;
        }
        cancelAodEntryTransaction();
        if (!next.dozing() || !next.partialAod()) {
            cancelLiveAodCrossfade();
        }
        presentation = next;
        applyDataForContent(next.content());
        updateBurnInForPresentation();
        applyClockColors();
        scheduleApplyTargets(animate);
    }

    /**
     * Synchronously parks the persistent host on the first frame that a screen-off-from-unlocked
     * partial AOD is allowed to expose. This deliberately uses the non-dozing geometry for the
     * normalized entry scene, matching beginAodEntry()'s first transaction, but it does so during
     * WakefulnessLifecycle#dispatchStartedGoingToSleep instead of waiting for OPlus's later
     * ClockPlugin render. Any queued LARGE/UNLOCKED target animation is cancelled before the snap.
     */
    void prearmNonLockscreenAodEntry(CouiClockPresentationModel next, String source) {
        if (next == null || !next.dozing()) {
            return;
        }
        diagnosticSource = source == null ? "prearm-non-lockscreen-aod" : source;
        cancelAodEntryTransaction();
        cancelPendingLiveAodRetarget(true);
        cancelLiveAodCrossfade();
        cancelScheduledTargetApply();
        cancelRunningPropertyAnimations();
        CouiClockPresentationModel.AodContent entryContent = normalizeContent(next.content());
        presentation = new CouiClockPresentationModel(next.requestedScene(), false, false,
                entryContent);
        applyDataForContent(entryContent);
        updateBurnInForPresentation();
        applyClockColors();
        applyTargets(false, 0L);
    }

    /** Begins an AOD entry with one deferred finalization frame, as in the reference host. */
    void beginAodEntry(CouiClockPresentationModel next, boolean animate, String source) {
        if (next == null || !next.dozing()) {
            return;
        }
        diagnosticSource = source == null ? "begin-aod" : source;
        cancelAodEntryTransaction();
        cancelPendingLiveAodRetarget(true);
        cancelLiveAodCrossfade();
        cancelRunningPropertyAnimations();
        pendingEntryToken = transitionGeneration.begin();
        aodEntryInProgress = true;
        deferredAodContent = null;
        deferredAodData = null;
        CouiClockPresentationModel.AodContent entryContent = normalizeContent(next.content());
        presentation = new CouiClockPresentationModel(next.requestedScene(), false, false,
                entryContent);
        aodData = dataForContent(entryContent);
        updateBurnInForPresentation();
        applyClockColors();
        applyTargets(false, 0L);
        final long token = pendingEntryToken;
        final boolean finalAnimate = animate;
        beginAodFrameRunnable = () -> {
            if (!aodEntryInProgress || !transitionGeneration.isCurrent(token)) {
                return;
            }
            beginAodFrameRunnable = null;
            presentation = new CouiClockPresentationModel(next.requestedScene(), true,
                    next.partialAod(), entryContent);
            aodData = dataForContent(entryContent);
            updateBurnInForPresentation();
            applyClockColors();
            applyTargets(finalAnimate, finalAnimate ? TARGET_TRANSITION_MS : 0L);
            finishAodEntryRunnable = () -> finishAodEntry(token);
            postDelayed(finishAodEntryRunnable,
                    finalAnimate ? CouiClockAodTransitionPolicy.ENTRY_ANIMATION_DURATION_MS : 0L);
        };
        postOnAnimation(beginAodFrameRunnable);
    }

    /** Package seam retained for controllers that already have the pieces of the state. */
    void beginAodEntry(CouiClockPresentationModel.Scene scene, boolean partialAod,
            CouiClockPresentationModel.AodContent content, boolean animate, String source) {
        beginAodEntry(new CouiClockPresentationModel(scene, true, partialAod, content), animate,
                source);
    }

    void setAodContent(CouiClockPresentationModel.AodContent content, boolean animate) {
        diagnosticSource = "set-aod-content";
        CouiClockPresentationModel.AodContent normalized = normalizeContent(content);
        if (aodEntryInProgress) {
            deferAodContent(normalized);
            return;
        }
        if (liveCrossfadeInProgress) {
            deferredLiveAodContent = normalized;
            return;
        }
        if (!presentation.dozing()) {
            aodData = dataForContent(normalized);
            return;
        }
        // COUI 2.5's setAodContent returns immediately for equal semantic content. The visual
        // payload (icons/media text) is already refreshed through the data-only adapter, so
        // replaying the same target here would only restart the 550 ms clock morph.
        if (CouiClockAodTransitionPolicy.sameContent(presentation.content(), normalized)) {
            aodData = dataForContent(normalized);
            return;
        }
        if (!presentation.partialAod()) {
            presentation = new CouiClockPresentationModel(presentation.requestedScene(), true,
                    false, normalized);
            aodData = dataForContent(normalized);
            applyClockColors();
            scheduleApplyTargets(animate);
            return;
        }
        setLiveAodContent(normalized, animate, "set-aod-content");
    }

    /** Updates partial-AOD content with generation-guarded 150ms out / 200ms in retargeting. */
    void setLiveAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        diagnosticSource = source == null ? "live-aod-content" : source;
        CouiClockPresentationModel.AodContent normalized = normalizeContent(content);
        if (aodEntryInProgress) {
            deferAodContent(normalized);
            return;
        }
        if (liveCrossfadeInProgress) {
            deferredLiveAodContent = normalized;
            return;
        }
        if (!presentation.dozing() || !presentation.partialAod()) {
            aodData = dataForContent(normalized);
            return;
        }
        boolean visualSceneChanges = presentation.visualScene()
                != partialSceneFor(normalized);
        if (animate && visualSceneChanges) {
            startLiveAodCrossfade(normalized);
            return;
        }
        scheduleLiveAodRetarget(normalized, animate);
    }

    private void deferAodContent(CouiClockPresentationModel.AodContent content) {
        deferredAodContent = normalizeContent(content);
        deferredAodData = dataForContent(deferredAodContent);
    }

    private void finishAodEntry(long token) {
        if (!aodEntryInProgress || !transitionGeneration.isCurrent(token)) {
            return;
        }
        aodEntryInProgress = false;
        pendingEntryToken = 0L;
        finishAodEntryRunnable = null;
        CouiClockPresentationModel.AodContent deferred = deferredAodContent;
        AodData deferredData = deferredAodData;
        deferredAodContent = null;
        deferredAodData = null;
        if (deferred == null
                || CouiClockAodTransitionPolicy.sameContent(deferred, presentation.content())) {
            return;
        }
        boolean sceneChanges = presentation.partialAod()
                && partialSceneFor(presentation.content()) != partialSceneFor(deferred);
        if (sceneChanges) {
            startLiveAodCrossfade(deferred);
            return;
        }
        presentation = new CouiClockPresentationModel(presentation.requestedScene(), true,
                presentation.partialAod(), deferred);
        aodData = deferredData != null ? deferredData : dataForContent(deferred);
        updateBurnInForPresentation();
        applyClockColors();
        applyTargets(false, 0L);
    }

    private void scheduleLiveAodRetarget(final CouiClockPresentationModel.AodContent content,
            final boolean animate) {
        cancelPendingLiveAodRetarget(true);
        // Reference PixelClockHostView#setLiveAodContent cancels the current property target set
        // before staging a new pre-draw transaction. Without this, semantic refreshes can leave a
        // previous 550 ms target animator alive while the next target set is installed.
        cancelRunningPropertyAnimations();
        final long token = liveAodTransitionGeneration.begin();
        liveAodRetargetPending = true;
        presentation = new CouiClockPresentationModel(CouiClockPresentationModel.Scene.LARGE,
                true, true, content);
        aodData = dataForContent(content);
        updateBurnInForPresentation();
        applyClockColors();
        final ViewTreeObserver.OnPreDrawListener listener =
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (getViewTreeObserver().isAlive()) {
                            getViewTreeObserver().removeOnPreDrawListener(this);
                        }
                        if (CouiClockHostView.this.pendingLivePreDrawListener == this) {
                            CouiClockHostView.this.pendingLivePreDrawListener = null;
                        }
                        liveAodRetargetPending = false;
                        if (!CouiClockAodTransitionPolicy.acceptsLivePreDraw(token,
                                liveAodTransitionGeneration.current(),
                                presentation.dozing() && presentation.partialAod())) {
                            return true;
                        }
                        applyTargets(animate, animate ? TARGET_TRANSITION_MS : 0L);
                        return true;
                    }
                };
        pendingLivePreDrawListener = listener;
        if (getViewTreeObserver().isAlive()) {
            getViewTreeObserver().addOnPreDrawListener(listener);
            requestLayout();
            invalidate();
        } else {
            liveAodRetargetPending = false;
            applyTargets(animate, animate ? TARGET_TRANSITION_MS : 0L);
        }
    }

    private void startLiveAodCrossfade(CouiClockPresentationModel.AodContent content) {
        cancelPendingLiveAodRetarget(false);
        cancelRunningPropertyAnimations();
        long token = liveAodTransitionGeneration.begin();
        liveCrossfadeInProgress = true;
        deferredLiveAodContent = normalizeContent(content);
        animate().cancel();
        finishLiveCrossfadeRunnable = () -> finishLiveAodFadeOut(token);
        animate().alpha(0f).setDuration(LIVE_FADE_OUT_MS)
                .setInterpolator(motionInterpolator)
                .withEndAction(finishLiveCrossfadeRunnable).start();
    }

    private void finishLiveAodFadeOut(long token) {
        if (!liveCrossfadeInProgress || !liveAodTransitionGeneration.isCurrent(token)) {
            return;
        }
        CouiClockPresentationModel.AodContent next = deferredLiveAodContent;
        deferredLiveAodContent = null;
        if (next == null) {
            next = CouiClockPresentationModel.AodContent.none();
        }
        presentation = new CouiClockPresentationModel(CouiClockPresentationModel.Scene.LARGE,
                true, true, next);
        aodData = dataForContent(next);
        updateBurnInForPresentation();
        applyClockColors();
        requestLayout();
        applyTargets(false, 0L);
        setAlpha(0f);
        finishLiveCrossfadeRunnable = () -> finishLiveAodFadeIn(token);
        animate().alpha(1f).setDuration(LIVE_FADE_IN_MS)
                .setInterpolator(motionInterpolator)
                .withEndAction(finishLiveCrossfadeRunnable).start();
    }

    private void finishLiveAodFadeIn(long token) {
        if (!liveCrossfadeInProgress || !liveAodTransitionGeneration.isCurrent(token)) {
            return;
        }
        liveCrossfadeInProgress = false;
        finishLiveCrossfadeRunnable = null;
        CouiClockPresentationModel.AodContent deferred = deferredLiveAodContent;
        deferredLiveAodContent = null;
        if (deferred != null
                && !CouiClockAodTransitionPolicy.sameContent(deferred, presentation.content())) {
            scheduleLiveAodRetarget(deferred, false);
        }
    }

    void onTimeTick() {
        long minute = System.currentTimeMillis() / 60000L;
        if (!CouiClockTimeTickPolicy.shouldRefresh(lastMinute, minute)) {
            return;
        }
        lastMinute = minute;
        calendar.setTimeInMillis(System.currentTimeMillis());
        boolean is24Hour = DateFormat.is24HourFormat(getContext());
        int hour = calendar.get(is24Hour ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (!is24Hour && hour == 0) {
            hour = 12;
        }
        int minuteOfHour = calendar.get(Calendar.MINUTE);
        timeText = String.format(Locale.US, "%02d%02d", hour, minuteOfHour);
        for (GlyphSet glyphSet : glyphSets) {
            for (int i = 0; i < glyphSet.digits.length; i++) {
                glyphSet.digits[i].setText(String.valueOf(timeText.charAt(i)));
            }
        }
        refreshInformationFromExistingAdapters("time-tick");
        if (presentation.dozing() && !manualBurnIn) {
            updateBurnInForPresentation();
        }
        scheduleApplyTargets(false);
    }

    void setInformation(CharSequence date, CharSequence week, CharSequence weather,
            Drawable weatherIcon) {
        dateView.setText(date == null ? "" : date);
        weekView.setText(week == null ? "" : week);
        weatherView.setText(weather == null ? "" : weather);
        weatherIconView.setImageDrawable(weatherIcon);
        weatherIconView.setVisibility(weatherIcon == null ? INVISIBLE : VISIBLE);
        dateView.setVisibility(TextUtils.isEmpty(dateView.getText()) ? GONE : VISIBLE);
        weekView.setVisibility(TextUtils.isEmpty(weekView.getText()) ? GONE : VISIBLE);
        weatherView.setVisibility(TextUtils.isEmpty(weatherView.getText()) ? GONE : VISIBLE);
        dateGroup.requestLayout();
        weatherGroup.requestLayout();
        scheduleApplyTargets(false);
    }

    /** Refreshes only the existing semantic data; this host remains the sole visual owner. */
    void refreshInformationFromExistingAdapters(String source) {
        PixelAodClockView.WeatherSnapshot weather =
                PixelAodClockView.currentFreshWeather(getContext());
        boolean compact = presentation.visualScene() == CouiClockPresentationModel.Scene.SMALL;
        PixelAodRenderModel renderModel = presentation.dozing()
                ? PixelAodRenderModel.forAod(getContext(), compact, weather, "", false)
                : PixelAodRenderModel.forLockscreen(getContext(), compact, weather);
        CouiClockInformationPolicy.Data data = CouiClockInformationPolicy.from(renderModel);
        setInformation(data.dateText, "", data.weatherText, null);
        PixelAodClockView.applyWeatherLeadingIcon(weatherView, weather, Color.WHITE);
        PixelAodLog.log("COUI host information data rendererMode=COUI_PORT"
                + " datePresent=" + !data.dateText.isEmpty()
                + " weatherPresent=" + !data.weatherText.isEmpty()
                + " source=" + (source == null ? "information-refresh" : source));
    }

    void setNotificationIcons(List<? extends Drawable> icons) {
        int totalCount = icons == null ? 0 : icons.size();
        CouiClockNotificationOverflowPolicy.Plan plan =
                CouiClockNotificationOverflowPolicy.forCount(totalCount);
        int count = plan.visibleCount();
        while (notificationIconCount() < count) {
            ImageView iconView = new ImageView(getContext());
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(18), dp(18));
            if (notificationIconCount() > 0) {
                params.setMarginStart(dp(15));
            }
            int overflowIndex = notificationIconRow.indexOfChild(notificationOverflowView);
            notificationIconRow.addView(iconView, overflowIndex < 0
                    ? notificationIconRow.getChildCount() : overflowIndex, params);
        }
        int iconIndex = 0;
        for (int childIndex = 0; childIndex < notificationIconRow.getChildCount(); childIndex++) {
            View child = notificationIconRow.getChildAt(childIndex);
            if (child == notificationOverflowView) {
                continue;
            }
            ImageView iconView = (ImageView) child;
            Drawable icon = icons != null && iconIndex < count ? icons.get(iconIndex) : null;
            iconView.setImageDrawable(icon);
            iconView.setVisibility(icon == null ? GONE : VISIBLE);
            iconIndex++;
        }
        notificationOverflowView.setText(plan.overflowText());
        notificationOverflowView.setVisibility(plan.hasOverflow() ? VISIBLE : GONE);
        notificationIconRow.requestLayout();
        scheduleApplyTargets(false);
    }

    private int notificationIconCount() {
        int count = 0;
        for (int i = 0; i < notificationIconRow.getChildCount(); i++) {
            if (notificationIconRow.getChildAt(i) != notificationOverflowView) {
                count++;
            }
        }
        return count;
    }

    void setMediaData(CharSequence title, CharSequence artist, Drawable appIcon) {
        mediaTitleView.setText(title == null ? "" : title);
        mediaArtistView.setText(artist == null ? "" : artist);
        mediaAppIconView.setImageDrawable(normalizeMediaAppIcon(appIcon));
        mediaAppIconView.setVisibility(appIcon == null ? GONE : VISIBLE);
        mediaGroup.requestLayout();
        scheduleApplyTargets(false);
    }

    /** Mirrors COUI 2.5 media-icon extraction: monochrome/foreground first, then alpha trim. */
    private Drawable normalizeMediaAppIcon(Drawable appIcon) {
        if (appIcon == null) {
            return null;
        }
        Drawable drawable = cloneDrawable(appIcon);
        if (drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptive = (AdaptiveIconDrawable) drawable;
            Drawable selected = null;
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    selected = adaptive.getMonochrome();
                } catch (Throwable ignored) {
                }
            }
            if (selected == null) {
                selected = adaptive.getForeground();
            }
            drawable = cloneDrawable(selected != null ? selected : drawable);
        }
        return trimMediaIcon(drawable != null ? drawable.mutate() : appIcon.mutate());
    }

    private Drawable cloneDrawable(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        Drawable.ConstantState state = drawable.getConstantState();
        Drawable clone = state != null ? state.newDrawable(getResources()) : drawable;
        return clone != null ? clone.mutate() : null;
    }

    private Drawable trimMediaIcon(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth() > 0
                ? Math.max(1, Math.min(drawable.getIntrinsicWidth(), 96)) : 96;
        int height = drawable.getIntrinsicHeight() > 0
                ? Math.max(1, Math.min(drawable.getIntrinsicHeight(), 96)) : 96;
        Bitmap source = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(source);
        Rect oldBounds = new Rect(drawable.getBounds());
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        drawable.setBounds(oldBounds);

        int[] pixels = new int[width * height];
        source.getPixels(pixels, 0, width, 0, 0, width, height);
        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;
        for (int index = 0; index < pixels.length; index++) {
            if (Color.alpha(pixels[index]) <= 8) {
                continue;
            }
            int x = index % width;
            int y = index / width;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        if (maxX < minX || maxY < minY) {
            return drawable;
        }

        Bitmap output = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        float contentWidth = maxX - minX + 1f;
        float contentHeight = maxY - minY + 1f;
        float scale = Math.min(96f / contentWidth, 96f / contentHeight) * 0.98f;
        float targetWidth = contentWidth * scale;
        float targetHeight = contentHeight * scale;
        Rect sourceRect = new Rect(minX, minY, maxX + 1, maxY + 1);
        RectF targetRect = new RectF((96f - targetWidth) / 2f, (96f - targetHeight) / 2f,
                (96f + targetWidth) / 2f, (96f + targetHeight) / 2f);
        new Canvas(output).drawBitmap(source, sourceRect, targetRect,
                new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        return new BitmapDrawable(getResources(), output);
    }

    void setBurnInTranslation(float x, float y, long durationMillis) {
        manualBurnIn = true;
        if (burnInX == x && burnInY == y) {
            return;
        }
        burnInX = x;
        burnInY = y;
        applyTargets(durationMillis > 0L, Math.max(0L, Math.min(durationMillis, 2000L)));
    }

    void setBurnInTranslation(int x, int y, int durationMillis) {
        setBurnInTranslation((float) x, (float) y, durationMillis);
    }

    void setBatteryState(int level, boolean charging) {
        lastBatteryLevel = Math.max(0, Math.min(level, 100));
        lastBatteryCharging = charging;
        String suffix = charging ? " · Charging" : "";
        batteryView.setText(lastBatteryLevel + "%" + suffix);
        batteryView.requestLayout();
        scheduleApplyTargets(false);
    }

    void setBatteryEnabled(boolean enabled) {
        if (batteryEnabled == enabled) {
            return;
        }
        batteryEnabled = enabled;
        scheduleApplyTargets(false);
    }

    /**
     * Controls whether this host is the visible primary owner for its ClockPlugin root.
     *
     * <p>The bridge uses this seam for transient vendor states without constructing or
     * revealing a legacy module clock.</p>
     */
    void setPrimaryVisible(boolean visible, String source) {
        if (visible) {
            setVisibility(VISIBLE);
        } else {
            cancelTransitions();
            setVisibility(INVISIBLE);
        }
    }

    void cancelTransitions() {
        cancelAodEntryTransaction();
        cancelPendingLiveAodRetarget(true);
        cancelLiveAodCrossfade();
        transitionGeneration.invalidate();
        liveAodTransitionGeneration.invalidate();
        cancelRunningPropertyAnimations();
        cancelScheduledTargetApply();
    }

    void detachLifecycle() {
        cancelTransitions();
        unregisterBatteryReceiver();
        unregisterTimeReceiver();
    }

    CouiClockPresentationModel presentation() {
        return presentation;
    }

    boolean isTransitionActive() {
        return aodEntryInProgress || liveCrossfadeInProgress;
    }

    private boolean batteryEnabled = true;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        registerBatteryReceiver();
        registerTimeReceiver();
        lastMinute = Long.MIN_VALUE;
        onTimeTick();
        updateBattery(null);
        requestLayout();
        scheduleApplyTargets(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        detachLifecycle();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width > 0 && width != clockBaseWidth) {
            clockBaseWidth = width;
            setClockBaseSize(width * CouiClockGeometryPolicy.LS_LARGE.baseWidthRatio);
            int mediaWidth = Math.max(0, width - dp(64));
            mediaGroup.getLayoutParams().width = mediaWidth;
            mediaTitleView.setMaxWidth(mediaWidth);
            mediaArtistView.setMaxWidth(Math.max(0, mediaWidth - dp(24)));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && getWidth() > 0 && getHeight() > 0 && !isTransitionActive()) {
            scheduleApplyTargets(false);
        }
    }

    private void setClockBaseSize(float sizePx) {
        for (GlyphSet glyphSet : glyphSets) {
            for (TextView digit : glyphSet.digits) {
                digit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, sizePx);
            }
            glyphSet.colon.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, sizePx);
            int colonWidth = Math.max(dp(12), Math.round(
                    glyphSet.colon.getPaint().measureText(COLON) + dp(4)));
            int colonHeight = Math.max(Math.round(sizePx), Math.round(
                    glyphSet.colon.getPaint().getFontMetrics().descent
                            - glyphSet.colon.getPaint().getFontMetrics().ascent));
            glyphSet.colon.setLayoutParams(new FrameLayout.LayoutParams(colonWidth, colonHeight));
        }
    }

    private void registerBatteryReceiver() {
        if (batteryReceiverRegistered) {
            return;
        }
        try {
            getContext().registerReceiver(batteryReceiver,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            batteryReceiverRegistered = true;
        } catch (Throwable t) {
            PixelAodLog.log("COUI host battery receiver registration failed", t);
        }
    }

    private void unregisterBatteryReceiver() {
        if (!batteryReceiverRegistered) {
            return;
        }
        try {
            getContext().unregisterReceiver(batteryReceiver);
        } catch (Throwable t) {
            PixelAodLog.log("COUI host battery receiver unregister failed", t);
        } finally {
            batteryReceiverRegistered = false;
        }
    }

    private void registerTimeReceiver() {
        if (timeReceiverRegistered) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        try {
            getContext().registerReceiver(timeReceiver, filter);
            timeReceiverRegistered = true;
        } catch (Throwable t) {
            PixelAodLog.log("COUI host time receiver registration failed", t);
        }
    }

    private void unregisterTimeReceiver() {
        if (!timeReceiverRegistered) {
            return;
        }
        try {
            getContext().unregisterReceiver(timeReceiver);
        } catch (Throwable t) {
            PixelAodLog.log("COUI host time receiver unregister failed", t);
        } finally {
            timeReceiverRegistered = false;
        }
    }

    private void updateBattery(Intent intent) {
        try {
            Intent batteryIntent = intent;
            if (batteryIntent == null) {
                batteryIntent = getContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            }
            BatteryManager manager = (BatteryManager) getContext()
                    .getSystemService(Context.BATTERY_SERVICE);
            int level = batteryIntent != null
                    ? batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryIntent != null
                    ? batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100) : 100;
            if (level < 0 && manager != null) {
                level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
            if (level < 0) {
                return;
            }
            if (scale > 0 && batteryIntent != null) {
                level = Math.round(level * 100f / scale);
            }
            int status = batteryIntent != null
                    ? batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN)
                    : BatteryManager.BATTERY_STATUS_UNKNOWN;
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                    || status == BatteryManager.BATTERY_STATUS_FULL;
            if (level != lastBatteryLevel || lastBatteryCharging == null
                    || charging != lastBatteryCharging) {
                setBatteryState(level, charging);
            }
        } catch (Throwable t) {
            PixelAodLog.log("COUI host battery update failed", t);
        }
    }

    private void scheduleApplyTargets(boolean animate) {
        pendingTargetApplyAnimated |= animate;
        if (pendingTargetApply) {
            return;
        }
        pendingTargetApply = true;
        pendingTargetApplyRunnable = () -> {
            pendingTargetApply = false;
            boolean applyAnimated = pendingTargetApplyAnimated;
            pendingTargetApplyAnimated = false;
            applyTargets(applyAnimated, applyAnimated ? TARGET_TRANSITION_MS : 0L);
        };
        post(pendingTargetApplyRunnable);
    }

    private void cancelScheduledTargetApply() {
        if (pendingTargetApplyRunnable != null) {
            removeCallbacks(pendingTargetApplyRunnable);
            pendingTargetApplyRunnable = null;
        }
        pendingTargetApply = false;
        pendingTargetApplyAnimated = false;
    }

    private void cancelAodEntryTransaction() {
        if (beginAodFrameRunnable != null) {
            removeCallbacks(beginAodFrameRunnable);
            beginAodFrameRunnable = null;
        }
        if (finishAodEntryRunnable != null) {
            removeCallbacks(finishAodEntryRunnable);
            finishAodEntryRunnable = null;
        }
        if (pendingEntryToken != 0L) {
            transitionGeneration.invalidate();
        }
        pendingEntryToken = 0L;
        aodEntryInProgress = false;
        deferredAodContent = null;
        deferredAodData = null;
    }

    private void cancelPendingLiveAodRetarget(boolean invalidateGeneration) {
        if (pendingLivePreDrawListener != null && getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnPreDrawListener(pendingLivePreDrawListener);
        }
        pendingLivePreDrawListener = null;
        liveAodRetargetPending = false;
        if (invalidateGeneration) {
            liveAodTransitionGeneration.invalidate();
        }
    }

    private void cancelLiveAodCrossfade() {
        if (finishLiveCrossfadeRunnable != null) {
            finishLiveCrossfadeRunnable = null;
        }
        if (liveCrossfadeInProgress) {
            liveAodTransitionGeneration.invalidate();
        }
        liveCrossfadeInProgress = false;
        deferredLiveAodContent = null;
        animate().cancel();
        setAlpha(1f);
    }

    private void cancelRunningPropertyAnimations() {
        for (GlyphSet glyphSet : glyphSets) {
            for (TextView digit : glyphSet.digits) {
                digit.animate().cancel();
            }
            glyphSet.colon.animate().cancel();
        }
        if (colonAlphaAnimator != null) {
            colonAlphaAnimator.cancel();
            colonAlphaAnimator = null;
        }
        dateGroup.animate().cancel();
        weatherGroup.animate().cancel();
        notificationIconRow.animate().cancel();
        mediaGroup.animate().cancel();
        batteryView.animate().cancel();
    }

    private void applyTargets(boolean animate, long duration) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        // Exact COUI 2.5 ordering: an explicit target transaction owns this frame and cancels any
        // queued scheduleApplyTargets runnable before calculating/applying the new targets.
        cancelScheduledTargetApply();
        GlyphSet activeSet = activeGlyphSet();
        CouiClockGeometryPolicy.SurfaceTarget surface = currentSurfaceTarget();
        GlyphTarget[] targets = calculateGlyphTargets(activeSet, surface);
        String variation = CouiClockFontPolicy.variationFor(
                presentation.visualScene(), presentation.dozing());
        String diagnosticSignature = presentation.visualScene() + "|"
                + presentation.dozing() + "|" + presentation.partialAod() + "|"
                + currentSurfaceName() + "|" + variation + "|"
                + (morphRuntime != null ? "MORPHING_LARGE" : "FOUR_SET_CROSSFADE") + "|"
                + animate + "|" + duration + "|" + diagnosticSource;
        if (!diagnosticSignature.equals(lastTargetDiagnosticSignature)) {
            lastTargetDiagnosticSignature = diagnosticSignature;
            PixelAodLog.log("COUI host targets rendererMode=COUI_PORT"
                    + " source=" + diagnosticSource
                    + " visualScene=" + presentation.visualScene()
                    + " dozing=" + presentation.dozing()
                    + " partialAod=" + presentation.partialAod()
                    + " surface=" + currentSurfaceName()
                    + " glyphMode=" + (morphRuntime != null
                    ? "MORPHING_LARGE" : "FOUR_SET_CROSSFADE")
                    + " variation=" + variation
                    + " animate=" + animate
                    + " durationMs=" + duration
                    + " transitionGeneration=" + transitionGeneration.current()
                    + " liveGeneration=" + liveAodTransitionGeneration.current()
                    + " contentKind=" + presentation.content().kind()
                    + " iconCount=" + presentation.content().notificationIconCount()
                    + " burnInOffset=" + burnInX + "," + burnInY
                    + " geometry={baseWidthRatio=" + surface.baseWidthRatio
                    + ",topRatio=" + surface.topRatio
                    + ",topDp=" + surface.topDp
                    + ",scale=" + surface.scale
                    + ",centerRatio=" + surface.centerRatio
                    + ",centerDp=" + surface.centerDp
                    + ",weight=" + surface.weight
                    + ",opsz=" + surface.opsz
                    + ",trackingFactor=" + surface.trackingFactor
                    + ",textRatio=" + surface.textRatio
                    + ",infoYRatio=" + surface.infoYRatio
                    + ",burnInEnabled=" + surface.burnInEnabled + "}");
        }
        updateInformationShadow();
        if (morphRuntime != null) {
            applyMorphStyle(animate, duration);
            for (TextView digit : largeSet.digits) {
                int index = indexOfDigit(largeSet, digit);
                applyGlyphTarget(digit, targets[index], 1f, animate, duration);
            }
            applyMorphColonTarget(largeSet.colon, targets[4], animate, duration);
        } else {
            for (GlyphSet glyphSet : glyphSets) {
                float alpha = glyphSet == activeSet ? 1f : 0f;
                applyGlyphSet(glyphSet, targets, alpha, animate, duration);
            }
        }
        applyInformationTargets(animate, duration, surface);
        applyContentTargets(animate, duration);
        applyBatteryTarget(animate, duration);
    }

    private void applyGlyphSet(GlyphSet glyphSet, GlyphTarget[] targets, float alpha,
            boolean animate, long duration) {
        for (int i = 0; i < glyphSet.digits.length; i++) {
            applyGlyphTarget(glyphSet.digits[i], targets[i], alpha, animate, duration);
        }
        applyGlyphTarget(glyphSet.colon, targets[4], alpha, animate, duration);
    }

    private void applyGlyphTarget(View view, GlyphTarget target, float groupAlpha,
            boolean animate, long duration) {
        float targetAlpha = target.alpha * groupAlpha;
        CouiClockAppliedTargetPolicy.Glyph applied = CouiClockAppliedTargetPolicy.glyph(
                target.x, target.y, target.scale, targetAlpha);
        if (applied.equals(appliedGlyphTargets.get(view))) {
            return;
        }
        appliedGlyphTargets.put(view, applied);
        view.animate().cancel();
        if (!animate || duration <= 0L) {
            view.setTranslationX(target.x);
            view.setTranslationY(target.y);
            view.setScaleX(target.scale);
            view.setScaleY(target.scale);
            view.setAlpha(targetAlpha);
            return;
        }
        view.animate().translationX(target.x).translationY(target.y)
                .scaleX(target.scale).scaleY(target.scale)
                .alpha(targetAlpha).setDuration(duration)
                .setInterpolator(motionInterpolator).start();
    }

    private void applyMorphColonTarget(View view, GlyphTarget target, boolean animate,
            long duration) {
        float targetAlpha = target.alpha;
        CouiClockAppliedTargetPolicy.Glyph applied = CouiClockAppliedTargetPolicy.glyph(
                target.x, target.y, target.scale, targetAlpha);
        if (applied.equals(appliedGlyphTargets.get(view))) {
            return;
        }
        appliedGlyphTargets.put(view, applied);
        view.animate().cancel();
        if (colonAlphaAnimator != null) {
            colonAlphaAnimator.cancel();
            colonAlphaAnimator = null;
        }
        if (!animate || duration <= 0L) {
            view.setTranslationX(target.x);
            view.setTranslationY(target.y);
            view.setScaleX(target.scale);
            view.setScaleY(target.scale);
            view.setAlpha(targetAlpha);
            return;
        }
        view.animate().translationX(target.x).translationY(target.y)
                .scaleX(target.scale).scaleY(target.scale)
                .setDuration(duration).setInterpolator(motionInterpolator).start();
        ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(view, View.ALPHA,
                view.getAlpha(), targetAlpha);
        alphaAnimator.setStartDelay(CouiClockColonAnimationPolicy.alphaStartDelay(
                targetAlpha, view.getAlpha(), duration));
        alphaAnimator.setDuration(CouiClockColonAnimationPolicy.alphaDuration(duration));
        alphaAnimator.setInterpolator(motionInterpolator);
        alphaAnimator.start();
        colonAlphaAnimator = alphaAnimator;
    }

    private static int indexOfDigit(GlyphSet glyphSet, TextView digit) {
        for (int i = 0; i < glyphSet.digits.length; i++) {
            if (glyphSet.digits[i] == digit) {
                return i;
            }
        }
        return 0;
    }

    private void applyInformationTargets(boolean animate, long duration,
            CouiClockGeometryPolicy.SurfaceTarget surface) {
        int dateWidth = dateGroup.getMeasuredWidth();
        int weatherWidth = weatherGroup.getMeasuredWidth();
        int maximumWidth = Math.max(dateWidth, weatherWidth);
        float dateX;
        float dateY;
        float weatherX;
        float weatherY;
        if (presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE) {
            float dozingScale = presentation.dozing() ? surface.scale : 1f;
            float centeredGap = ((1f - dozingScale)
                    * (getWidth() * surface.baseWidthRatio * 1.82f) / 2f)
                    + getHeight() * surface.topRatio;
            dateX = Math.max(dp(16), (getWidth()
                    - (dateWidth + dp(LARGE_INFO_SIDE_GAP_DP) + weatherWidth)) / 2f);
            dateY = getWidth() * surface.baseWidthRatio * LARGE_INFO_WIDTH_MULTIPLIER
                    * dozingScale + centeredGap;
            weatherX = dateX + dateWidth + dp(LARGE_INFO_SIDE_GAP_DP);
            weatherY = dateY;
        } else {
            float centerX = getWidth() * CouiClockGeometryPolicy.INFO_CENTER_RATIO
                    + dp(presentation.dozing()
                    ? CouiClockGeometryPolicy.AOD_INFO_X_DP
                    : CouiClockGeometryPolicy.LOCKSCREEN_INFO_X_DP);
            boolean immersed = presentation.visualScene()
                    == CouiClockPresentationModel.Scene.IMMERSED;
            float top = getHeight() * (immersed && !presentation.dozing()
                    ? CouiClockGeometryPolicy.LS_IMMERSED.infoYRatio
                    : CouiClockGeometryPolicy.INFO_Y_RATIO)
                    + dp(immersed && !presentation.dozing()
                    ? 30f : CouiClockGeometryPolicy.INFO_Y_OFFSET_DP);
            dateX = centerX - maximumWidth / 2f;
            dateY = top;
            weatherX = dateX;
            weatherY = top + dateGroup.getMeasuredHeight()
                    + dp(CouiClockGeometryPolicy.DATE_WEATHER_GAP_DP);
        }
        float xOffset = presentation.dozing() ? burnInX : 0f;
        float yOffset = presentation.dozing() ? burnInY : 0f;
        applyInformationTarget(dateGroup, dateX + xOffset, dateY + yOffset, animate, duration);
        applyInformationTarget(weatherGroup, weatherX + xOffset, weatherY + yOffset, animate,
                duration);
    }

    private void applyInformationTarget(View view, float x, float y, boolean animate,
            long duration) {
        CouiClockAppliedTargetPolicy.Information applied =
                CouiClockAppliedTargetPolicy.information(x, y, 1f);
        if (applied.equals(appliedInformationTargets.get(view))) {
            return;
        }
        appliedInformationTargets.put(view, applied);
        view.animate().cancel();
        if (!animate || duration <= 0L) {
            view.setTranslationX(x);
            view.setTranslationY(y);
            view.setAlpha(1f);
            return;
        }
        view.animate().translationX(x).translationY(y).alpha(1f)
                .setDuration(duration).setInterpolator(motionInterpolator).start();
    }

    private void applyContentTargets(boolean animate, long duration) {
        CouiClockPresentationModel.AodContent content = presentation.content();
        boolean partialAodActive = presentation.dozing() && presentation.partialAod();
        boolean mediaVisible = partialAodActive
                && content.kind() == CouiClockPresentationModel.AodContent.Kind.MEDIA;
        boolean notificationsVisible = partialAodActive
                && (content.kind() == CouiClockPresentationModel.AodContent.Kind.NOTIFICATIONS
                || (content.kind() == CouiClockPresentationModel.AodContent.Kind.MEDIA
                && content.notificationIconCount() > 0));
        float x = dp(CouiClockGeometryPolicy.PARTIAL_CONTENT_X_DP) + burnInX;
        float baseY = getHeight() * CouiClockGeometryPolicy.PARTIAL_CONTENT_TOP_RATIO + burnInY;
        float mediaY = baseY;
        float notificationY = mediaVisible
                ? baseY + mediaGroup.getMeasuredHeight()
                + dp(CouiClockGeometryPolicy.MEDIA_TO_NOTIFICATION_GAP_DP) : baseY;
        boolean preserveCurrentPosition =
                CouiClockAodContentMotionPolicy.preserveCurrentPosition(partialAodActive, animate);
        float notificationX = preserveCurrentPosition
                ? notificationIconRow.getTranslationX() : x;
        float resolvedNotificationY = preserveCurrentPosition
                ? notificationIconRow.getTranslationY() : notificationY;
        float mediaX = preserveCurrentPosition ? mediaGroup.getTranslationX() : x;
        float resolvedMediaY = preserveCurrentPosition ? mediaGroup.getTranslationY() : mediaY;
        applyContentViewTarget(notificationIconRow, notificationX, resolvedNotificationY,
                notificationsVisible ? 1f : 0f, animate, duration);
        applyContentViewTarget(mediaGroup, mediaX, resolvedMediaY,
                mediaVisible ? 1f : 0f, animate, duration);
    }

    /**
     * Exact COUI 2.5 content-row behavior: geometry is committed before the frame's fade starts.
     * Clock glyphs still animate their X/Y/scale over the normal target duration, but AOD
     * notification/media rows only animate alpha. This prevents a newly visible notification row
     * from exposing the pre-AOD anchor while burn-in translation is being applied.
     */
    private void applyContentViewTarget(View view, float x, float y, float alpha,
            boolean animate, long duration) {
        view.animate().cancel();
        view.setTranslationX(x);
        view.setTranslationY(y);
        if (!animate || duration <= 0L) {
            view.setAlpha(alpha);
            return;
        }
        if (CouiClockAodContentMotionPolicy.animateTranslation()) {
            view.animate().translationX(x).translationY(y).alpha(alpha)
                    .setDuration(duration).setInterpolator(motionInterpolator).start();
            return;
        }
        view.animate().alpha(alpha).setDuration(duration)
                .setInterpolator(motionInterpolator).start();
    }

    private void applyBatteryTarget(boolean animate, long duration) {
        boolean visible = presentation.dozing() && batteryEnabled;
        float x = Math.max(0f, (getWidth() - batteryView.getMeasuredWidth()) / 2f)
                + (presentation.dozing() ? burnInX * CouiClockGeometryPolicy.BATTERY_BURN_IN_X_SCALE
                : 0f);
        float y = getHeight() - dp(CouiClockGeometryPolicy.BATTERY_BOTTOM_MARGIN_DP)
                - batteryView.getMeasuredHeight() + dp(BATTERY_Y_OFFSET_DP)
                + (presentation.dozing()
                ? -Math.abs(burnInY) * CouiClockGeometryPolicy.BATTERY_BURN_IN_Y_SCALE : 0f);
        applyViewTarget(batteryView, x, y, visible ? 1f : 0f, animate, duration);
    }

    private void applyViewTarget(View view, float x, float y, float alpha, boolean animate,
            long duration) {
        view.animate().cancel();
        if (!animate || duration <= 0L) {
            view.setTranslationX(x);
            view.setTranslationY(y);
            view.setAlpha(alpha);
            return;
        }
        view.animate().translationX(x).translationY(y).alpha(alpha)
                .setDuration(duration).setInterpolator(motionInterpolator).start();
    }

    private GlyphTarget[] calculateGlyphTargets(GlyphSet glyphSet,
            CouiClockGeometryPolicy.SurfaceTarget surface) {
        if (presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE) {
            return calculateLargeTargets(glyphSet, surface);
        }
        return calculateLineTargets(glyphSet, surface);
    }

    private GlyphTarget[] calculateLargeTargets(GlyphSet glyphSet,
            CouiClockGeometryPolicy.SurfaceTarget surface) {
        float width = getWidth();
        float height = getHeight();
        float baseWidth = width * surface.baseWidthRatio;
        float scale = surface.scale;
        float top = ((1f - scale) * (1.82f * baseWidth) / 2f)
                + height * surface.topRatio + dp(surface.topDp);
        float lineWidth = baseWidth * LARGE_LINE_WIDTH_RATIO * scale;
        float centerX = width / 2f;
        float tracking = baseWidth * scale * surface.trackingFactor;
        float firstWidth = glyphWidth(glyphSet, 0) * scale;
        float secondWidth = glyphWidth(glyphSet, 1) * scale;
        float thirdWidth = glyphWidth(glyphSet, 2) * scale;
        float fourthWidth = glyphWidth(glyphSet, 3) * scale;
        float leftPairStart = centerX - (firstWidth + tracking + secondWidth) / 2f;
        float rightPairStart = centerX - (thirdWidth + tracking + fourthWidth) / 2f;
        float burnX = presentation.dozing() ? burnInX : 0f;
        float burnY = presentation.dozing() ? burnInY : 0f;
        return new GlyphTarget[]{
                new GlyphTarget(leftPairStart + burnX, top + burnY, scale, 1f),
                new GlyphTarget(leftPairStart + firstWidth + tracking + burnX,
                        top + burnY, scale, 1f),
                new GlyphTarget(rightPairStart + burnX, top + lineWidth + burnY, scale, 1f),
                new GlyphTarget(rightPairStart + thirdWidth + tracking + burnX,
                        top + lineWidth + burnY, scale, 1f),
                new GlyphTarget(centerX + burnX, top + lineWidth / 2f + burnY,
                        LARGE_COLON_SCALE, 0f)
        };
    }

    private GlyphTarget[] calculateLineTargets(GlyphSet glyphSet,
            CouiClockGeometryPolicy.SurfaceTarget surface) {
        float width = getWidth();
        float lineWidth = width * CouiClockGeometryPolicy.LS_LARGE.baseWidthRatio
                * surface.scale;
        float tracking = -0.09f * lineWidth;
        float colonTracking = tracking * COMPACT_COLON_TRACKING_SCALE;
        float xCenter = width * surface.centerRatio + dp(surface.centerDp);
        float y = getHeight() * surface.topRatio + dp(surface.topDp);
        float[] advances = new float[4];
        float[] leftCorrections = new float[4];
        float[] rightCorrections = new float[4];
        float measured = 0f;
        for (int i = 0; i < 4; i++) {
            advances[i] = glyphWidth(glyphSet, i) * surface.scale;
            char digit = timeText.charAt(i);
            leftCorrections[i] = CouiClockGlyphCorrection.leftTrimOffset(digit, lineWidth);
            rightCorrections[i] = CouiClockGlyphCorrection.rightSideExpansion(digit, lineWidth);
            measured += advances[i] - leftCorrections[i] - rightCorrections[i];
        }
        float colonWidth = glyphSet.colon.getPaint().measureText(COLON) * surface.scale;
        float start = xCenter - ((colonWidth + colonTracking * 2f + tracking * 2f + measured)
                / 2f);
        float x0 = start - leftCorrections[0];
        float after0 = start + advances[0] - leftCorrections[0] - rightCorrections[0] + tracking;
        float x1 = after0 - leftCorrections[1];
        float colonX = after0 + advances[1] - leftCorrections[1] - rightCorrections[1]
                + colonTracking;
        float x2 = colonX + colonWidth - leftCorrections[2];
        float after2 = colonX + colonWidth + advances[2] - leftCorrections[2]
                - rightCorrections[2] + tracking;
        float x3 = after2 - leftCorrections[3];
        float burnX = presentation.dozing() ? burnInX : 0f;
        float burnY = presentation.dozing() ? burnInY : 0f;
        return new GlyphTarget[]{
                new GlyphTarget(x0 + burnX, y + burnY, surface.scale, 1f),
                new GlyphTarget(x1 + burnX, y + burnY, surface.scale, 1f),
                new GlyphTarget(x2 + burnX, y + burnY, surface.scale, 1f),
                new GlyphTarget(x3 + burnX, y + burnY, surface.scale, 1f),
                new GlyphTarget(colonX + burnX, y + burnY, surface.scale, 1f)
        };
    }

    private float glyphWidth(GlyphSet glyphSet, int index) {
        return glyphSet.digits[index].getPaint().measureText(
                String.valueOf(timeText.charAt(index)));
    }

    private GlyphSet activeGlyphSet() {
        if (!presentation.dozing()) {
            return presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE
                    ? largeSet : compactSet;
        }
        return presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE
                ? aodLargeSet : aodCompactSet;
    }

    private CouiClockGeometryPolicy.SurfaceTarget currentSurfaceTarget() {
        if (presentation.dozing()) {
            return presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE
                    ? CouiClockGeometryPolicy.AOD_LARGE : CouiClockGeometryPolicy.AOD_SMALL;
        }
        switch (presentation.visualScene()) {
            case LARGE:
                return CouiClockGeometryPolicy.LS_LARGE;
            case IMMERSED:
                return CouiClockGeometryPolicy.LS_IMMERSED;
            case SMALL:
            default:
                return CouiClockGeometryPolicy.LS_SMALL;
        }
    }

    private String currentSurfaceName() {
        if (presentation.dozing()) {
            return presentation.visualScene() == CouiClockPresentationModel.Scene.LARGE
                    ? "AOD_LARGE" : "AOD_SMALL";
        }
        switch (presentation.visualScene()) {
            case LARGE:
                return "LS_LARGE";
            case IMMERSED:
                return "LS_IMMERSED";
            case SMALL:
            default:
                return "LS_SMALL";
        }
    }

    private void applyClockColors() {
        int lockscreenColor = CouiClockVisualStylePolicy.clockColor(
                presentation.visualScene(), false, monetColor, aodMonetColor);
        int aodColor = CouiClockVisualStylePolicy.clockColor(
                presentation.visualScene(), true, monetColor, aodMonetColor);
        for (TextView digit : largeSet.digits) {
            digit.setTextColor(lockscreenColor);
        }
        largeSet.colon.setTextColor(lockscreenColor);
        for (TextView digit : compactSet.digits) {
            digit.setTextColor(lockscreenColor);
        }
        compactSet.colon.setTextColor(lockscreenColor);
        for (TextView digit : aodLargeSet.digits) {
            digit.setTextColor(aodColor);
        }
        aodLargeSet.colon.setTextColor(aodColor);
        for (TextView digit : aodCompactSet.digits) {
            digit.setTextColor(aodColor);
        }
        aodCompactSet.colon.setTextColor(aodColor);
        dateView.setTextColor(Color.WHITE);
        weekView.setTextColor(Color.WHITE);
        weatherView.setTextColor(Color.WHITE);
        weatherIconView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        notificationOverflowView.setTextColor(CouiClockVisualStylePolicy.notificationOverflowColor(
                PixelAodClockView.resolveMaterialInfoColor(getContext())));
        mediaTitleView.setTextColor(Color.WHITE);
        mediaArtistView.setTextColor(Color.WHITE);
        mediaAppIconView.setImageTintList(ColorStateList.valueOf(Color.WHITE));
    }

    private void applyMorphStyle(boolean animate, long duration) {
        String variation = CouiClockFontPolicy.variationFor(presentation.visualScene(),
                presentation.dozing());
        int color = CouiClockVisualStylePolicy.clockColor(presentation.visualScene(),
                presentation.dozing(), monetColor, aodMonetColor);
        for (TextView digit : largeSet.digits) {
            if (digit instanceof CouiClockMorphingGlyphView) {
                ((CouiClockMorphingGlyphView) digit).setMorphStyle(variation, color, animate,
                        duration, motionInterpolator);
            }
        }
        if (largeSet.colon instanceof CouiClockMorphingGlyphView) {
            ((CouiClockMorphingGlyphView) largeSet.colon).setMorphStyle(variation, color,
                    animate, duration, motionInterpolator);
        }
    }

    private void updateInformationShadow() {
        boolean apply = CouiClockVisualStylePolicy.shouldApplyInformationShadow(
                presentation.visualScene(), presentation.dozing(), presentation.partialAod(),
                presentation.content().kind() == CouiClockPresentationModel.AodContent.Kind.MEDIA);
        if (informationShadowApplied != null && informationShadowApplied == apply) {
            return;
        }
        informationShadowApplied = apply;
        float radius = apply ? dp(CouiClockVisualStylePolicy.INFORMATION_SHADOW_RADIUS_DP) : 0f;
        float dy = apply ? dp(CouiClockVisualStylePolicy.INFORMATION_SHADOW_DY_DP) : 0f;
        int color = apply ? CouiClockVisualStylePolicy.INFORMATION_SHADOW_COLOR : 0;
        dateView.setShadowLayer(radius, 0f, dy, color);
        weekView.setShadowLayer(radius, 0f, dy, color);
        weatherView.setShadowLayer(radius, 0f, dy, color);
    }

    void setMonetColors(int monetColor, int aodMonetColor) {
        this.monetColor = monetColor;
        this.aodMonetColor = aodMonetColor;
        applyClockColors();
        scheduleApplyTargets(false);
    }

    void updateMonetColor() {
        int fallback = CouiClockVisualStylePolicy.LOCKSCREEN_FALLBACK_CLOCK_COLOR;
        int lockscreen = resolveSystemColor("system_accent1_100", fallback);
        int accentLight = resolveSystemColor("system_accent1_10", Color.WHITE);
        int accentDark = resolveSystemColor("system_accent1_50", lockscreen);
        setMonetColors(lockscreen, blendArgb(accentLight, accentDark, 0.5f));
    }

    private int resolveSystemColor(String name, int fallback) {
        try {
            int id = getResources().getIdentifier(name, "color", "android");
            return id == 0 ? fallback : getResources().getColor(id, getContext().getTheme());
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int blendArgb(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        int a = Math.round(((from >>> 24) * inverse) + ((to >>> 24) * ratio));
        int r = Math.round((((from >> 16) & 0xff) * inverse)
                + (((to >> 16) & 0xff) * ratio));
        int g = Math.round((((from >> 8) & 0xff) * inverse)
                + (((to >> 8) & 0xff) * ratio));
        int b = Math.round(((from & 0xff) * inverse) + ((to & 0xff) * ratio));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void updateBurnInForPresentation() {
        if (manualBurnIn) {
            return;
        }
        if (!presentation.dozing()) {
            burnInX = 0f;
            burnInY = 0f;
            return;
        }
        long minute = System.currentTimeMillis() / 60000L;
        float xAmplitude = dp(presentation.partialAod() ? 8f : 5f);
        float yAmplitude = dp(presentation.partialAod() ? 10f : 4f);
        burnInX = centeredBurnInOffset(minute, xAmplitude,
                CouiClockGeometryPolicy.BURN_IN_X_PERIOD_MINUTES);
        burnInY = centeredBurnInOffset(minute, yAmplitude,
                CouiClockGeometryPolicy.BURN_IN_Y_PERIOD_MINUTES);
    }

    private static float centeredBurnInOffset(long value, float amplitude, long period) {
        float phase = (value % period) / (period / 2f);
        if (phase > 1f) {
            phase = 2f - phase;
        }
        return ((phase * 2f) - 1f) * amplitude;
    }

    private void applyDataForContent(CouiClockPresentationModel.AodContent content) {
        aodData = dataForContent(content);
    }

    private AodData dataForContent(CouiClockPresentationModel.AodContent content) {
        CouiClockPresentationModel.AodContent normalized = normalizeContent(content);
        if (normalized.kind() == CouiClockPresentationModel.AodContent.Kind.MEDIA) {
            return new AodData(normalized, mediaTitleView.getText(), mediaArtistView.getText(),
                    mediaAppIconView.getDrawable());
        }
        return new AodData(normalized, "", "", null);
    }

    private static CouiClockPresentationModel.AodContent normalizeContent(
            CouiClockPresentationModel.AodContent content) {
        return content == null ? CouiClockPresentationModel.AodContent.none() : content;
    }

    private static CouiClockPresentationModel.Scene partialSceneFor(
            CouiClockPresentationModel.AodContent content) {
        return normalizeContent(content).kind()
                == CouiClockPresentationModel.AodContent.Kind.NONE
                ? CouiClockPresentationModel.Scene.LARGE
                : CouiClockPresentationModel.Scene.SMALL;
    }

    private TextView informationText(float sizeSp, int weight) {
        TextView view = new TextView(getContext());
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(INFORMATION_COLOR);
        view.setGravity(android.view.Gravity.CENTER_VERTICAL);
        view.setIncludeFontPadding(false);
        view.setMaxLines(1);
        String variation = "'wght' " + weight
                + ", 'wdth' 100, 'ROND' 0, 'GRAD' 0, 'opsz' 18";
        Typeface typeface = CouiClockFontLoader.buildCustomFont(getContext(), variation);
        view.setTypeface(typeface != null ? typeface : Typeface.DEFAULT);
        view.setFontVariationSettings(variation);
        view.setClickable(false);
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private GlyphSet createGlyphSet(String variation, boolean useMorphingGlyphs) {
        Typeface typeface = CouiClockFontLoader.buildCustomFont(getContext(), variation);
        if (typeface == null) {
            typeface = Typeface.DEFAULT;
        }
        TextView[] digits = new TextView[4];
        for (int i = 0; i < digits.length; i++) {
            digits[i] = createClockGlyph(typeface, variation, false, useMorphingGlyphs);
            digits[i].setText(String.valueOf(timeText.charAt(i)));
        }
        TextView colon = createClockGlyph(typeface, variation, true, useMorphingGlyphs);
        colon.setText(COLON);
        return new GlyphSet(digits, colon);
    }

    private TextView createClockGlyph(Typeface typeface, String variation, boolean colon,
            boolean useMorphingGlyphs) {
        TextView view = useMorphingGlyphs && morphRuntime != null
                ? new CouiClockMorphingGlyphView(getContext(), morphRuntime)
                : new TextView(getContext());
        view.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        view.setIncludeFontPadding(false);
        view.setMaxLines(1);
        view.setTextColor(CouiClockVisualStylePolicy.LOCKSCREEN_FALLBACK_CLOCK_COLOR);
        view.setTypeface(typeface);
        view.setFontVariationSettings(variation);
        view.setFontFeatureSettings(TABULAR_NUMBERS);
        view.setPivotX(0f);
        view.setPivotY(0f);
        view.setClickable(false);
        view.setFocusable(false);
        view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        if (!colon) {
            view.setMinWidth(dp(12));
        }
        return view;
    }

    private FrameLayout.LayoutParams glyphLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class GlyphSet {
        final TextView[] digits;
        final TextView colon;

        GlyphSet(TextView[] digits, TextView colon) {
            this.digits = digits;
            this.colon = colon;
        }
    }

    private static final class GlyphTarget {
        final float x;
        final float y;
        final float scale;
        final float alpha;

        GlyphTarget(float x, float y, float scale, float alpha) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.alpha = alpha;
        }
    }

    private static final class AodData {
        final CouiClockPresentationModel.AodContent content;
        final CharSequence title;
        final CharSequence artist;
        final Drawable appIcon;

        AodData(CouiClockPresentationModel.AodContent content, CharSequence title,
                CharSequence artist, Drawable appIcon) {
            this.content = content;
            this.title = title;
            this.artist = artist;
            this.appIcon = appIcon;
        }

        static AodData empty() {
            return new AodData(CouiClockPresentationModel.AodContent.none(), "", "", null);
        }
    }
}
