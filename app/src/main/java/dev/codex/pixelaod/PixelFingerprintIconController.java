package dev.codex.pixelaod;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

final class PixelFingerprintIconController {
    private static final Map<ImageView, Drawable> ORIGINAL_DRAWABLES = new WeakHashMap<>();
    private static final Map<ImageView, Drawable> ORIGINAL_BACKGROUNDS = new WeakHashMap<>();
    private static final Map<ImageView, PixelFingerprintDrawable> PIXEL_DRAWABLES =
            new WeakHashMap<>();
    private static final Map<ImageView, PixelFingerprintBackgroundDrawable> PIXEL_BACKGROUNDS =
            new WeakHashMap<>();
    private static final Map<ImageView, View> PIXEL_BACKGROUND_LAYERS = new WeakHashMap<>();
    private static final Map<ImageView, Float> ORIGINAL_PRESSED_ALPHAS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> TRACKED_PRESSED_VIEWS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> PRESSED_TOUCH_STATES = new WeakHashMap<>();
    private static final Map<ImageView, Runnable> PENDING_REFRESHES = new WeakHashMap<>();
    private static final Map<ImageView, Runnable> PENDING_RECLAIMS = new WeakHashMap<>();
    private static final Map<Object, Runnable> PENDING_DISCOVERY = new WeakHashMap<>();
    private static final Map<Object, PendingRequest> PENDING_REQUESTS = new WeakHashMap<>();
    private static final Map<Class<?>, Boolean> VENDOR_VIEW_HOOKS = new WeakHashMap<>();
    private static final Map<ImageView, WeakReference<Object>> VIEW_OWNERS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> TRACKED_VIEWS = new WeakHashMap<>();
    private static final Map<ImageView, String> LAST_LOGGED_STATES = new WeakHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long VENDOR_RECLAIM_DELAY_MS = 48L;
    private static final long VENDOR_RECLAIM_SECOND_PASS_DELAY_MS = 160L;
    private static final long[] DISCOVERY_RETRY_DELAYS_MS = {
            0L, 16L, 48L, 120L, 250L, 500L, 1000L, 1800L
    };
    private static volatile boolean imageViewHooksInstalled;
    private static final ThreadLocal<Integer> INTERNAL_MUTATION_DEPTH =
            new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
    private static volatile WeakReference<Object> lastUiMech = new WeakReference<>(null);

    private static final class PendingRequest {
        final WeakReference<Object> target;
        Context context;
        String source;
        boolean animate;
        Runnable runnable;

        PendingRequest(Context context, Object target, String source, boolean animate) {
            this.context = context;
            this.target = new WeakReference<>(target);
            this.source = source;
            this.animate = animate;
        }
    }

    private PixelFingerprintIconController() {
    }

    static void refresh(Context context, Object uiMech, String source, boolean animate) {
        if (uiMech == null) {
            return;
        }
        lastUiMech = new WeakReference<>(uiMech);
        requestVisualState(context, uiMech, source, animate);
    }

    private static void requestVisualState(Context context, Object uiMech,
            String source, boolean animate) {
        PendingRequest request;
        boolean schedule;
        synchronized (PENDING_REQUESTS) {
            request = PENDING_REQUESTS.get(uiMech);
            if (request == null) {
                request = new PendingRequest(context, uiMech, source, animate);
                PENDING_REQUESTS.put(uiMech, request);
                schedule = true;
            } else {
                if (context != null) {
                    request.context = context;
                }
                request.source = source;
                request.animate |= animate;
                schedule = false;
            }
        }
        if (!schedule) {
            return;
        }
        final PendingRequest pending = request;
        pending.runnable = () -> {
            synchronized (PENDING_REQUESTS) {
                if (PENDING_REQUESTS.get(uiMech) == pending) {
                    PENDING_REQUESTS.remove(uiMech);
                }
            }
            Object target = pending.target.get();
            if (target != null) {
                dispatchRefresh(pending.context, target, pending.source,
                        pending.animate, true);
            }
        };
        ImageView anchor = findFingerprintIcon(uiMech);
        if (anchor != null) {
            runOnViewHandler(anchor, pending.runnable);
        } else {
            MAIN.post(pending.runnable);
        }
    }

    private static void dispatchRefresh(Context context, Object uiMech,
            String source, boolean animate, boolean scheduleReclaim) {
        ImageView anchor = findFingerprintIcon(uiMech);
        if (anchor != null) {
            runOnViewHandler(anchor, () -> refreshOnViewThread(context, uiMech, source,
                    animate, scheduleReclaim));
            return;
        }
        scheduleDiscovery(context, uiMech, source, animate);
    }

    private static void refreshOnViewThread(Context context, Object uiMech,
            String source, boolean animate, boolean scheduleReclaim) {
        ImageView iconView = findFingerprintIcon(uiMech);
        ImageView pressedIcon = findFingerprintPressedIcon(uiMech);
        if (iconView == null) {
            restoreNativePressedVisual(pressedIcon, source + "#pressed");
            PixelAodLog.log("Pixel fingerprint icon refresh skipped source=" + source
                    + " reason=primary-fpIcon-unavailable");
            scheduleDiscovery(context, uiMech, source, animate);
            return;
        }
        logCarrierState("before-refresh", iconView, pressedIcon, source);
        cancelDiscovery(uiMech);
        Context resolvedContext = context != null
                ? context : iconView.getContext();
        boolean enabled = PixelAodUdfpsRuntimePolicy.replacementRequested(resolvedContext);
        if (!enabled) {
            restoreOriginalDrawable(iconView, source);
            restoreTrackedPressedIcons(source + "#pressed-disabled");
            return;
        }
        boolean nativeCarrierVisible = iconView.getVisibility() == View.VISIBLE;
        PixelFingerprintIconPolicy.RefreshMode refreshMode =
                PixelAodClockView.fingerprintIconRefreshMode(
                        resolvedContext, source, nativeCarrierVisible);
        if (refreshMode == PixelFingerprintIconPolicy.RefreshMode.SKIP) {
            cancelPendingReclaim(iconView);
            return;
        }
        boolean carrierRefreshAllowed =
                refreshMode == PixelFingerprintIconPolicy.RefreshMode.REFRESH_CARRIER;
        if (!carrierRefreshAllowed) {
            // Native FOD hide owns the carrier until an explicit interaction/new AOD cycle.
            // Cancel any first-pass reclaim queued before the timeout so it cannot race the hide.
            cancelPendingReclaim(iconView);
        }

        trackPressedIcon(pressedIcon);
        updatePressedVisual(pressedIcon, source + "#pressed-passive");

        boolean onDozeState = readBooleanField(uiMech, "onDozeState");
        boolean onDreamingStart = readBooleanField(uiMech, "onDreamingStart");
        boolean screenTurnedOff = readBooleanField(uiMech, "screenTurnedOff");
        boolean interactive = isInteractive(resolvedContext);
        boolean aodStyle = PixelFingerprintIconPolicy.useAodStyle(
                interactive, onDozeState, onDreamingStart, screenTurnedOff);
        boolean dark = isDarkMode(resolvedContext);

        if (carrierRefreshAllowed && PixelFingerprintIconPolicy.shouldReplaceCarrier(true)) {
            applyPixelDrawable(resolvedContext, uiMech, iconView, source, animate,
                    aodStyle, dark, onDozeState, onDreamingStart, screenTurnedOff, true);
        }
        if (scheduleReclaim && carrierRefreshAllowed
                && PixelFingerprintIconPolicy.shouldReplaceCarrier(true)
                && !isPixelOwnedDrawable(iconView.getDrawable())
                && !isVendorTemporaryAnimation(iconView.getDrawable())) {
            scheduleVendorReclaim(resolvedContext, uiMech, iconView, source);
        }
        logCarrierState("after-refresh", iconView, pressedIcon, source);
    }

    private static void applyPixelDrawable(Context context, Object uiMech, ImageView iconView,
            String source, boolean animate, boolean aodStyle, boolean dark,
            boolean onDozeState, boolean onDreamingStart, boolean screenTurnedOff,
            boolean primary) {
        Drawable current = iconView.getDrawable();
        Drawable currentBackground = iconView.getBackground();
        String drawableClass = current != null ? current.getClass().getName() : null;
        boolean alreadyCarrier = current instanceof PixelFingerprintAnimCarrier;
        boolean vendorTempAnim = isVendorTemporaryAnimation(current) || alreadyCarrier;
        boolean alreadyPixel = current instanceof PixelFingerprintDrawable;
        boolean created;
        PixelFingerprintDrawable pixelDrawable;
        synchronized (PIXEL_DRAWABLES) {
            pixelDrawable = PIXEL_DRAWABLES.get(iconView);
            created = pixelDrawable == null;
            if (created) {
                pixelDrawable = new PixelFingerprintDrawable(context, aodStyle, dark);
                PIXEL_DRAWABLES.put(iconView, pixelDrawable);
            }
        }
        PixelFingerprintBackgroundDrawable pixelBackground;
        synchronized (PIXEL_BACKGROUNDS) {
            pixelBackground = PIXEL_BACKGROUNDS.get(iconView);
            if (pixelBackground == null) {
                pixelBackground = new PixelFingerprintBackgroundDrawable(context, aodStyle, dark);
                PIXEL_BACKGROUNDS.put(iconView, pixelBackground);
            }
        }
        synchronized (TRACKED_VIEWS) {
            TRACKED_VIEWS.put(iconView, Boolean.TRUE);
            VIEW_OWNERS.put(iconView, new WeakReference<>(uiMech));
        }

        // OOS black-frame / tap temporary show uses OplusAnimationDrawable for optical/AOD
        // lifecycle. Wrap it in PixelFingerprintAnimCarrier (no per-frame Xposed draw hook).
        if (vendorTempAnim) {
            sanitizeTintAndBackground(iconView, currentBackground);
            pixelDrawable.transitionTo(aodStyle, dark, false);
            if (pixelDrawable.getAlpha() != 255) {
                pixelDrawable.setAlpha(255);
            }
            if (pixelBackground != null) {
                pixelBackground.transitionTo(aodStyle, dark, false);
            }
            ensurePixelBackgroundLayer(iconView, pixelBackground);
            Drawable vendor = alreadyCarrier
                    ? ((PixelFingerprintAnimCarrier) current).getVendor()
                    : current;
            if (!(current instanceof PixelFingerprintAnimCarrier)
                    || ((PixelFingerprintAnimCarrier) current).getVendor() != vendor
                    || ((PixelFingerprintAnimCarrier) current).getPixel() != pixelDrawable) {
                if (current != null && !(current instanceof PixelFingerprintAnimCarrier)
                        && !(current instanceof PixelFingerprintDrawable)) {
                    synchronized (ORIGINAL_DRAWABLES) {
                        ORIGINAL_DRAWABLES.put(iconView, current);
                    }
                }
                PixelFingerprintAnimCarrier carrier =
                        new PixelFingerprintAnimCarrier(vendor, pixelDrawable);
                setImageDrawableInternal(iconView, carrier);
                // Keep vendor anim ticking so OOS optical/AOD lifecycle still advances.
                if (vendor instanceof Animatable) {
                    try {
                        Animatable animatable = (Animatable) vendor;
                        if (!animatable.isRunning()) {
                            animatable.start();
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
            logStateIfChanged(iconView, "pixel-anim-carrier-"
                            + (aodStyle ? "aod" : "lockscreen"), source,
                    "vendorAnim=" + drawableClass
                            + " viewAlpha=" + iconView.getAlpha()
                            + " visibility=" + visibilityName(iconView.getVisibility())
                            + " fields={doze=" + onDozeState
                            + ",dreaming=" + onDreamingStart
                            + ",screenOff=" + screenTurnedOff + "}");
            return;
        }

        // Static stock / non-anim vendor glyph: swap to Pixel (no system look).
        if (!alreadyPixel) {
            normalizeIconViewForReclaim(iconView, currentBackground);
        } else {
            sanitizeTintAndBackground(iconView, currentBackground);
        }
        View backgroundLayer = ensurePixelBackgroundLayer(iconView, pixelBackground);
        if (current != pixelDrawable) {
            if (current != null && !(current instanceof PixelFingerprintDrawable)
                    && !(current instanceof PixelFingerprintAnimCarrier)) {
                synchronized (ORIGINAL_DRAWABLES) {
                    ORIGINAL_DRAWABLES.put(iconView, current);
                }
            }
            setImageDrawableInternal(iconView, pixelDrawable);
        }
        boolean styleAnimate = animate && !alreadyPixel && !created;
        pixelDrawable.transitionTo(aodStyle, dark, styleAnimate);
        if (pixelDrawable.getAlpha() != 255) {
            pixelDrawable.setAlpha(255);
        }
        if (backgroundLayer != null) {
            pixelBackground.transitionTo(aodStyle, dark, styleAnimate);
        }
        logStateIfChanged(iconView, "pixel-" + (aodStyle ? "aod" : "lockscreen"), source,
                "animate=" + styleAnimate
                        + " dark=" + dark
                        + " interactive=" + isInteractive(context)
                        + " primary=" + primary
                        + " alreadyPixel=" + alreadyPixel
                        + " viewAlpha=" + iconView.getAlpha()
                        + " reclaimedCompeting="
                        + PixelFingerprintIconPolicy.isCompetingDrawableClass(drawableClass)
                        + " fields={doze=" + onDozeState
                        + ",dreaming=" + onDreamingStart
                        + ",screenOff=" + screenTurnedOff + "}");
    }

    static void refreshLast(Context context, String source) {
        Object uiMech = lastUiMech.get();
        if (uiMech != null) {
            refresh(context, uiMech, source, false);
            return;
        }
        ImageView[] views;
        synchronized (TRACKED_VIEWS) {
            views = TRACKED_VIEWS.keySet().toArray(new ImageView[0]);
        }
        for (ImageView view : views) {
            refreshTrackedView(context, view, source);
        }
    }

    private static void restoreOriginalDrawable(ImageView iconView, String source) {
        if (iconView == null) {
            return;
        }
        cancelPendingReclaim(iconView);
        Drawable current = iconView.getDrawable();
        Drawable currentBackground = iconView.getBackground();
        if (current instanceof PixelFingerprintDrawable
                || current instanceof PixelFingerprintAnimCarrier) {
            Drawable original;
            synchronized (ORIGINAL_DRAWABLES) {
                original = ORIGINAL_DRAWABLES.get(iconView);
            }
            if (current instanceof PixelFingerprintAnimCarrier) {
                Drawable vendor = ((PixelFingerprintAnimCarrier) current).getVendor();
                if (vendor instanceof Animatable) {
                    try {
                        ((Animatable) vendor).stop();
                    } catch (Throwable ignored) {
                    }
                }
            }
            setImageDrawableInternal(iconView, original);
            logStateIfChanged(iconView, "vendor-restored", source,
                    "drawable=" + (original != null ? original.getClass().getName() : "null"));
        }
        removePixelBackgroundLayer(iconView);
        Drawable originalBackground;
        boolean hadOriginalBackground;
        synchronized (ORIGINAL_BACKGROUNDS) {
            hadOriginalBackground = ORIGINAL_BACKGROUNDS.containsKey(iconView);
            originalBackground = ORIGINAL_BACKGROUNDS.remove(iconView);
        }
        if (currentBackground instanceof PixelFingerprintBackgroundDrawable || hadOriginalBackground) {
            setBackgroundInternal(iconView, originalBackground);
        }
        synchronized (ORIGINAL_DRAWABLES) {
            ORIGINAL_DRAWABLES.remove(iconView);
        }
        synchronized (PIXEL_DRAWABLES) {
            PIXEL_DRAWABLES.remove(iconView);
        }
        synchronized (PIXEL_BACKGROUNDS) {
            PIXEL_BACKGROUNDS.remove(iconView);
        }
        synchronized (TRACKED_VIEWS) {
            TRACKED_VIEWS.remove(iconView);
            VIEW_OWNERS.remove(iconView);
        }
    }

    /**
     * Sanitize when swapping off a stock/vendor drawable. Intentionally does NOT:
     * - set View alpha / setBrightnessAlpha (causes permanent lockscreen HBM "highlight")
     * - animate().cancel() / clearAnimation() (kills OOS black-frame / tap temp-show fades)
     */
    private static void normalizeIconViewForReclaim(ImageView iconView,
            Drawable currentBackground) {
        beginInternalMutation();
        try {
            // Only un-collapse if OOS left the carrier at ~0 scale (would make Pixel invisible).
            if (iconView.getScaleX() < 0.05f) {
                iconView.setScaleX(1f);
            }
            if (iconView.getScaleY() < 0.05f) {
                iconView.setScaleY(1f);
            }
            sanitizeTintAndBackgroundLocked(iconView, currentBackground);
        } finally {
            endInternalMutation();
        }
    }

    private static void sanitizeTintAndBackground(ImageView iconView,
            Drawable currentBackground) {
        beginInternalMutation();
        try {
            sanitizeTintAndBackgroundLocked(iconView, currentBackground);
        } finally {
            endInternalMutation();
        }
    }

    private static void sanitizeTintAndBackgroundLocked(ImageView iconView,
            Drawable currentBackground) {
        if (currentBackground != null) {
            if (!(currentBackground instanceof PixelFingerprintBackgroundDrawable)) {
                synchronized (ORIGINAL_BACKGROUNDS) {
                    if (!ORIGINAL_BACKGROUNDS.containsKey(iconView)) {
                        ORIGINAL_BACKGROUNDS.put(iconView, currentBackground);
                    }
                }
            }
            iconView.setBackground(null);
        }
        if (iconView.getImageTintList() != null) {
            iconView.setImageTintList(null);
        }
        if (iconView.getColorFilter() != null) {
            iconView.clearColorFilter();
        }
        if (iconView.getScaleType() != ImageView.ScaleType.CENTER) {
            iconView.setScaleType(ImageView.ScaleType.CENTER);
        }
    }

    private static View ensurePixelBackgroundLayer(ImageView iconView,
            PixelFingerprintBackgroundDrawable pixelBackground) {
        if (!(iconView.getParent() instanceof FrameLayout)) {
            PixelAodLog.log("Pixel fingerprint background layer skipped"
                    + " reason=primary-parent-not-FrameLayout"
                    + " parent=" + (iconView.getParent() != null
                    ? iconView.getParent().getClass().getName() : "null"));
            return null;
        }
        FrameLayout parent = (FrameLayout) iconView.getParent();
        View backgroundLayer;
        synchronized (PIXEL_BACKGROUND_LAYERS) {
            backgroundLayer = PIXEL_BACKGROUND_LAYERS.get(iconView);
            if (backgroundLayer == null) {
                backgroundLayer = new View(iconView.getContext());
                backgroundLayer.setClickable(false);
                backgroundLayer.setLongClickable(false);
                backgroundLayer.setFocusable(false);
                backgroundLayer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                PIXEL_BACKGROUND_LAYERS.put(iconView, backgroundLayer);
            }
        }
        if (backgroundLayer.getBackground() != pixelBackground) {
            backgroundLayer.setBackground(pixelBackground);
        }
        backgroundLayer.setAlpha(1f);
        int iconIndex = parent.indexOfChild(iconView);
        if (backgroundLayer.getParent() != parent) {
            if (backgroundLayer.getParent() instanceof ViewGroup) {
                ((ViewGroup) backgroundLayer.getParent()).removeView(backgroundLayer);
            }
            parent.addView(backgroundLayer, Math.max(0, iconIndex),
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            int backgroundIndex = parent.indexOfChild(backgroundLayer);
            if (iconIndex >= 0 && backgroundIndex >= iconIndex) {
                parent.removeView(backgroundLayer);
                parent.addView(backgroundLayer, iconIndex,
                        new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }
        return backgroundLayer;
    }

    private static void removePixelBackgroundLayer(ImageView iconView) {
        View backgroundLayer;
        synchronized (PIXEL_BACKGROUND_LAYERS) {
            backgroundLayer = PIXEL_BACKGROUND_LAYERS.remove(iconView);
        }
        if (backgroundLayer != null && backgroundLayer.getParent() instanceof ViewGroup) {
            ((ViewGroup) backgroundLayer.getParent()).removeView(backgroundLayer);
        }
    }

    private static void setImageDrawableInternal(ImageView iconView, Drawable drawable) {
        beginInternalMutation();
        try {
            iconView.setImageDrawable(drawable);
        } finally {
            endInternalMutation();
        }
    }

    private static void setBackgroundInternal(ImageView iconView, Drawable drawable) {
        beginInternalMutation();
        try {
            iconView.setBackground(drawable);
        } finally {
            endInternalMutation();
        }
    }

    private static void runOnViewHandler(View view, Runnable runnable) {
        Handler handler = view != null ? view.getHandler() : null;
        if (handler == null) {
            if (view != null) {
                view.post(runnable);
            } else {
                MAIN.post(runnable);
            }
            return;
        }
        if (Looper.myLooper() == handler.getLooper()) {
            runnable.run();
        } else {
            handler.post(runnable);
        }
    }

    private static void scheduleDiscovery(Context context, Object uiMech,
            String source, boolean animate) {
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.containsKey(uiMech)) {
                return;
            }
        }
        WeakReference<Object> reference = new WeakReference<>(uiMech);
        int[] attempt = {0};
        Runnable[] holder = new Runnable[1];
        Runnable retry = () -> {
            Object target = reference.get();
            if (target == null) {
                cancelDiscovery(uiMech, holder[0]);
                return;
            }
            ImageView iconView = findFingerprintIcon(target);
            ImageView pressedIcon = findFingerprintPressedIcon(target);
            if (iconView != null || pressedIcon != null) {
                cancelDiscovery(target, holder[0]);
                refresh(context, target, source + "#discovery", animate);
                return;
            }
            int nextAttempt = ++attempt[0];
            if (nextAttempt >= DISCOVERY_RETRY_DELAYS_MS.length) {
                cancelDiscovery(target, holder[0]);
                PixelAodLog.log("Pixel fingerprint icon discovery exhausted source=" + source);
                return;
            }
            MAIN.postDelayed(holder[0], DISCOVERY_RETRY_DELAYS_MS[nextAttempt]);
        };
        holder[0] = retry;
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.containsKey(uiMech)) {
                return;
            }
            PENDING_DISCOVERY.put(uiMech, retry);
        }
        MAIN.post(retry);
    }

    private static void cancelDiscovery(Object uiMech) {
        Runnable pending;
        synchronized (PENDING_DISCOVERY) {
            pending = PENDING_DISCOVERY.remove(uiMech);
        }
        if (pending != null) {
            MAIN.removeCallbacks(pending);
        }
    }

    private static void cancelDiscovery(Object uiMech, Runnable expected) {
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.get(uiMech) != expected) {
                return;
            }
            PENDING_DISCOVERY.remove(uiMech);
        }
        MAIN.removeCallbacks(expected);
    }

    private static void refreshTrackedView(Context context, ImageView iconView, String source) {
        if (iconView == null || !isTracked(iconView)) {
            return;
        }
        WeakReference<Object> owner;
        synchronized (TRACKED_VIEWS) {
            owner = VIEW_OWNERS.get(iconView);
        }
        Object uiMech = owner != null ? owner.get() : lastUiMech.get();
        if (uiMech == null) {
            return;
        }
        requestVisualState(context, uiMech, source, false);
    }

    private static boolean isTracked(ImageView iconView) {
        synchronized (TRACKED_VIEWS) {
            return TRACKED_VIEWS.containsKey(iconView);
        }
    }

    private static void trackPressedIcon(ImageView pressedIcon) {
        if (pressedIcon == null) {
            return;
        }
        synchronized (TRACKED_PRESSED_VIEWS) {
            TRACKED_PRESSED_VIEWS.put(pressedIcon, Boolean.TRUE);
        }
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            if (!ORIGINAL_PRESSED_ALPHAS.containsKey(pressedIcon)) {
                ORIGINAL_PRESSED_ALPHAS.put(pressedIcon, pressedIcon.getAlpha());
            }
        }
    }

    private static boolean isTrackedPressed(ImageView pressedIcon) {
        synchronized (TRACKED_PRESSED_VIEWS) {
            return TRACKED_PRESSED_VIEWS.containsKey(pressedIcon);
        }
    }

    private static boolean isPressedTouchActive(ImageView pressedIcon) {
        synchronized (PRESSED_TOUCH_STATES) {
            return Boolean.TRUE.equals(PRESSED_TOUCH_STATES.get(pressedIcon));
        }
    }

    private static void updatePressedVisual(ImageView pressedIcon, String source) {
        if (pressedIcon == null || !isTrackedPressed(pressedIcon)) {
            return;
        }
        Runnable apply = () -> {
            boolean fingerDown = isPressedTouchActive(pressedIcon);
            float targetAlpha = PixelFingerprintIconPolicy.shouldShowNativePressedLayer(fingerDown)
                    ? originalPressedAlpha(pressedIcon) : 0f;
            float beforeAlpha = pressedIcon.getAlpha();
            if (Float.compare(beforeAlpha, targetAlpha) != 0) {
                setPressedAlphaInternal(pressedIcon, targetAlpha);
            }
            PixelAodLog.log("[FP-PRESSED-A2] applied", () ->
                    "[FP-PRESSED-A2] applied source=" + source
                    + " fingerDown=" + fingerDown
                    + " beforeAlpha=" + beforeAlpha
                    + " afterAlpha=" + pressedIcon.getAlpha()
                    + " attached=" + pressedIcon.isAttachedToWindow()
                    + " thread=" + Thread.currentThread().getName());
            logStateIfChanged(pressedIcon,
                    fingerDown ? "pressed-native-touch" : "pressed-native-suppressed",
                    source, "alpha=" + targetAlpha);
        };
        dispatchPressedVisual(pressedIcon, source, apply);
    }

    private static void dispatchPressedVisual(ImageView pressedIcon, String source, Runnable apply) {
        Handler handler = pressedIcon.getHandler();
        Looper currentLooper = Looper.myLooper();
        boolean sameLooper = handler != null && currentLooper == handler.getLooper();
        if (sameLooper) {
            PixelAodLog.log("[FP-PRESSED-A2] dispatch", () ->
                    "[FP-PRESSED-A2] dispatch source=" + source
                    + " route=inline handler=" + describeHandler(handler)
                    + " currentThread=" + Thread.currentThread().getName());
            apply.run();
            return;
        }
        boolean posted = handler != null ? handler.post(apply) : pressedIcon.post(apply);
        PixelAodLog.log("[FP-PRESSED-A2] dispatch", () ->
                "[FP-PRESSED-A2] dispatch source=" + source
                + " route=" + (handler != null ? "view-handler" : "view-post")
                + " posted=" + posted
                + " handler=" + describeHandler(handler)
                + " currentThread=" + Thread.currentThread().getName());
    }

    private static void setPressedAlphaInternal(ImageView pressedIcon, float alpha) {
        beginInternalMutation();
        try {
            pressedIcon.setAlpha(alpha);
        } finally {
            endInternalMutation();
        }
    }

    private static String describeHandler(Handler handler) {
        if (handler == null) {
            return "null";
        }
        Looper looper = handler.getLooper();
        Thread thread = looper != null ? looper.getThread() : null;
        if (looper == null) {
            return "null-looper";
        }
        return looper.getClass().getSimpleName()
                + "@" + Integer.toHexString(System.identityHashCode(looper))
                + "/" + (thread != null ? thread.getName() : "null-thread");
    }

    private static float originalPressedAlpha(ImageView pressedIcon) {
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            Float original = ORIGINAL_PRESSED_ALPHAS.get(pressedIcon);
            return original != null ? original : 1f;
        }
    }

    private static void restoreNativePressedVisual(ImageView pressedIcon, String source) {
        if (pressedIcon == null) {
            return;
        }
        float originalAlpha = originalPressedAlpha(pressedIcon);
        synchronized (TRACKED_PRESSED_VIEWS) {
            TRACKED_PRESSED_VIEWS.remove(pressedIcon);
        }
        synchronized (PRESSED_TOUCH_STATES) {
            PRESSED_TOUCH_STATES.remove(pressedIcon);
        }
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            ORIGINAL_PRESSED_ALPHAS.remove(pressedIcon);
        }
        runOnViewHandler(pressedIcon, () -> {
            if (Float.compare(pressedIcon.getAlpha(), originalAlpha) != 0) {
                pressedIcon.setAlpha(originalAlpha);
            }
            logStateIfChanged(pressedIcon, "pressed-native-restored", source,
                    "alpha=" + originalAlpha);
        });
    }

    private static void restoreTrackedPressedIcons(String source) {
        ImageView[] pressedIcons;
        synchronized (TRACKED_PRESSED_VIEWS) {
            pressedIcons = TRACKED_PRESSED_VIEWS.keySet().toArray(new ImageView[0]);
        }
        for (ImageView pressedIcon : pressedIcons) {
            restoreNativePressedVisual(pressedIcon, source);
        }
    }

    private static Boolean firstBooleanArgument(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Boolean) {
                return (Boolean) arg;
            }
        }
        return null;
    }

    static void installImageViewMutationHooks() {
        synchronized (PixelFingerprintIconController.class) {
            if (imageViewHooksInstalled) {
                return;
            }
            imageViewHooksInstalled = true;
        }
        String[] names = {
                "setImageDrawable", "setImageResource", "setImageIcon",
                "setImageTintList", "setColorFilter", "setScaleType"
        };
        int hooked = 0;
        for (Method method : ImageView.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 1 || !contains(names, method.getName())) {
                continue;
            }
            try {
                ModernHookBridge.hookAfter(method, param -> {
                    if (!(param.thisObject instanceof ImageView)
                            || INTERNAL_MUTATION_DEPTH.get() > 0) {
                        return;
                    }
                    ImageView view = (ImageView) param.thisObject;
                    if (isTracked(view)) {
                        logSingleCarrierState("image-mutation#" + method.getName(), view);
                        scheduleTrackedRefresh(view, "ImageView#" + method.getName());
                    }
                });
                hooked++;
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook fingerprint ImageView mutation "
                        + method.getName(), t);
            }
        }
        // Do NOT hook View.setAlpha globally — every SystemUI alpha animation paid Xposed
        // overhead (unlock/screen-off jank). Pressed layer is driven by onFingerprintTouch +
        // vendor mutation hooks on OnScreenFingerprintPressedIcon only.
        PixelAodLog.log("installed fingerprint ImageView mutation hooks count=" + hooked
                + " globalViewSetAlphaHook=false");
    }

    private static void scheduleTrackedRefresh(ImageView iconView, String source) {
        Drawable current = iconView.getDrawable();
        // Vendor temp anim or our anim carrier: restyle without stripping optical lifecycle.
        if (isVendorTemporaryAnimation(current)
                || current instanceof PixelFingerprintAnimCarrier) {
            runOnViewHandler(iconView,
                    () -> reclaimTrackedViewImmediate(iconView, source + "#vendor-anim"));
            return;
        }
        // Static stock glyph: reclaim to Pixel immediately (same frame).
        if (!(current instanceof PixelFingerprintDrawable)) {
            runOnViewHandler(iconView,
                    () -> reclaimTrackedViewImmediate(iconView, source + "#immediate"));
            return;
        }
        // Already Pixel: only re-evaluate aod/lockscreen style. Do not touch View alpha.
        Runnable previous;
        Runnable[] holder = new Runnable[1];
        Runnable refresh = () -> {
            synchronized (PENDING_REFRESHES) {
                if (PENDING_REFRESHES.get(iconView) == holder[0]) {
                    PENDING_REFRESHES.remove(iconView);
                }
            }
            refreshTrackedViewStyleOnly(iconView, source);
        };
        holder[0] = refresh;
        synchronized (PENDING_REFRESHES) {
            previous = PENDING_REFRESHES.put(iconView, refresh);
        }
        if (previous != null) {
            iconView.removeCallbacks(previous);
        }
        runOnViewHandler(iconView, refresh);
    }

    /**
     * Update dashed/solid style only when ImageView already hosts Pixel. Vendor anim hosts
     * go through full apply (attach hook + style) instead.
     */
    private static void refreshTrackedViewStyleOnly(ImageView iconView, String source) {
        if (iconView == null || !isTracked(iconView)) {
            return;
        }
        Drawable current = iconView.getDrawable();
        if (isVendorTemporaryAnimation(current)
                || current instanceof PixelFingerprintAnimCarrier
                || !(current instanceof PixelFingerprintDrawable)) {
            // Anim carrier / stock: full apply path (wrap or swap). Pure Pixel uses style-only.
            if (!(current instanceof PixelFingerprintDrawable)) {
                reclaimTrackedViewImmediate(iconView, source + "#style-only-reclaim");
                return;
            }
        }
        WeakReference<Object> owner;
        synchronized (TRACKED_VIEWS) {
            owner = VIEW_OWNERS.get(iconView);
        }
        Object uiMech = owner != null ? owner.get() : lastUiMech.get();
        if (uiMech == null) {
            return;
        }
        Context context = iconView.getContext();
        if (!PixelAodUdfpsRuntimePolicy.replacementRequested(context)) {
            return;
        }
        PixelFingerprintIconPolicy.RefreshMode refreshMode =
                PixelAodClockView.fingerprintIconRefreshMode(
                        context, source, iconView.getVisibility() == View.VISIBLE);
        if (refreshMode == PixelFingerprintIconPolicy.RefreshMode.SKIP) {
            return;
        }
        boolean onDozeState = readBooleanField(uiMech, "onDozeState");
        boolean onDreamingStart = readBooleanField(uiMech, "onDreamingStart");
        boolean screenTurnedOff = readBooleanField(uiMech, "screenTurnedOff");
        boolean interactive = isInteractive(context);
        boolean aodStyle = PixelFingerprintIconPolicy.useAodStyle(
                interactive, onDozeState, onDreamingStart, screenTurnedOff);
        boolean dark = isDarkMode(context);
        PixelFingerprintDrawable pixelDrawable;
        if (current instanceof PixelFingerprintDrawable) {
            pixelDrawable = (PixelFingerprintDrawable) current;
        } else {
            return;
        }
        pixelDrawable.transitionTo(aodStyle, dark, false);
        PixelFingerprintBackgroundDrawable pixelBackground;
        synchronized (PIXEL_BACKGROUNDS) {
            pixelBackground = PIXEL_BACKGROUNDS.get(iconView);
        }
        if (pixelBackground != null) {
            pixelBackground.transitionTo(aodStyle, dark, false);
        }
        logStateIfChanged(iconView, "pixel-" + (aodStyle ? "aod" : "lockscreen"),
                source + "#style-only",
                "viewAlpha=" + iconView.getAlpha()
                        + " visibility=" + visibilityName(iconView.getVisibility()));
    }

    /**
     * Apply Pixel policy immediately on the view thread (no PENDING_REQUESTS debounce).
     * For vendor temp anim: attach draw hook. For static stock: swap to Pixel drawable.
     */
    private static void reclaimTrackedViewImmediate(ImageView iconView, String source) {
        if (iconView == null || !isTracked(iconView)) {
            return;
        }
        Context context = iconView.getContext();
        if (!PixelAodUdfpsRuntimePolicy.replacementRequested(context)) {
            return;
        }
        WeakReference<Object> owner;
        synchronized (TRACKED_VIEWS) {
            owner = VIEW_OWNERS.get(iconView);
        }
        Object uiMech = owner != null ? owner.get() : lastUiMech.get();
        if (uiMech == null) {
            return;
        }
        refreshOnViewThread(context, uiMech, source, false, true);
    }

    static void onFingerprintTouch(Object uiMech, Object[] args, String source) {
        Boolean fingerDown = firstBooleanArgument(args);
        if (fingerDown == null) {
            PixelAodLog.log("Pixel fingerprint touch state ignored source=" + source
                    + " reason=boolean-argument-unavailable");
            return;
        }
        ImageView pressedIcon = findFingerprintPressedIcon(uiMech);
        trackPressedIcon(pressedIcon);
        if (pressedIcon == null) {
            return;
        }
        synchronized (PRESSED_TOUCH_STATES) {
            PRESSED_TOUCH_STATES.put(pressedIcon, fingerDown);
        }
        updatePressedVisual(pressedIcon, source + "#touch=" + fingerDown);
    }

    static void installVendorViewHooks(ClassLoader classLoader) {
        hookVendorViewClass(classLoader,
                "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon");
        hookVendorViewClass(classLoader,
                "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintPressedIcon");
    }

    private static void hookVendorViewClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            synchronized (VENDOR_VIEW_HOOKS) {
                if (VENDOR_VIEW_HOOKS.containsKey(clazz)) {
                    return;
                }
                VENDOR_VIEW_HOOKS.put(clazz, Boolean.TRUE);
            }
            int hooked = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isVendorVisualMutation(method)) {
                    continue;
                }
                final String source = className + "#" + method.getName();
                try {
                    method.setAccessible(true);
                    ModernHookBridge.hookAfter(method, param -> {
                        if (INTERNAL_MUTATION_DEPTH.get() == 0) {
                            onTrackedViewMutation(param.thisObject, source);
                        }
                    });
                    hooked++;
                } catch (Throwable t) {
                    PixelAodLog.log("failed to hook fingerprint vendor view mutation " + source, t);
                }
            }
            PixelAodLog.log("installed fingerprint vendor view hooks class=" + className
                    + " count=" + hooked);
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("fingerprint vendor view class not found class=" + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install fingerprint vendor view hooks class=" + className, t);
        }
    }

    private static boolean isVendorVisualMutation(Method method) {
        if (method == null || method.getParameterCount() > 2) {
            return false;
        }
        String name = method.getName();
        return "setVisibility".equals(name)
                || "setBrightnessAlpha".equals(name)
                || "setMaxBrightnessToAlpha".equals(name)
                || "stopSwitchAnim".equals(name)
                || "onVisibilityChanged".equals(name);
    }

    private static void onTrackedViewMutation(Object object, String source) {
        if (!(object instanceof ImageView)) {
            return;
        }
        ImageView view = (ImageView) object;
        if (isTrackedPressed(view)) {
            logSingleCarrierState("pressed-mutation#" + source, view);
            updatePressedVisual(view, source + "#pressed-mutation");
            return;
        }
        if (!isTracked(view)) {
            return;
        }
        logSingleCarrierState("vendor-mutation#" + source, view);
        // Brightness-only updates must not re-enter applyPixelDrawable (that used to force
        // highlight alpha). If drawable is already Pixel, ignore; OOS owns alpha.
        if (source.contains("setBrightnessAlpha") || source.contains("setMaxBrightnessToAlpha")) {
            if (!(view.getDrawable() instanceof PixelFingerprintDrawable)) {
                scheduleTrackedRefresh(view, source);
            }
            return;
        }
        scheduleTrackedRefresh(view, source);
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void beginInternalMutation() {
        INTERNAL_MUTATION_DEPTH.set(INTERNAL_MUTATION_DEPTH.get() + 1);
    }

    private static void endInternalMutation() {
        int depth = INTERNAL_MUTATION_DEPTH.get() - 1;
        if (depth <= 0) {
            INTERNAL_MUTATION_DEPTH.remove();
        } else {
            INTERNAL_MUTATION_DEPTH.set(depth);
        }
    }

    private static void scheduleVendorReclaim(Context context, Object uiMech,
            ImageView iconView, String source) {
        Runnable previous;
        Runnable[] reclaimRunnableHolder = new Runnable[1];
        Runnable reclaim = () -> {
            synchronized (PENDING_RECLAIMS) {
                if (PENDING_RECLAIMS.get(iconView) == reclaimRunnableHolder[0]) {
                    PENDING_RECLAIMS.remove(iconView);
                }
            }
            if (!isPixelOwnedDrawable(iconView.getDrawable())) {
                logSingleCarrierState("vendor-reclaim", iconView);
                refreshOnViewThread(context, uiMech, source + "#vendor-reclaim", false, false);
            }
        };
        reclaimRunnableHolder[0] = reclaim;
        synchronized (PENDING_RECLAIMS) {
            previous = PENDING_RECLAIMS.put(iconView, reclaim);
        }
        if (previous != null) {
            iconView.removeCallbacks(previous);
        }
        iconView.postDelayed(reclaim, VENDOR_RECLAIM_DELAY_MS);
        iconView.postDelayed(() -> {
            if (!isPixelOwnedDrawable(iconView.getDrawable())) {
                logSingleCarrierState("vendor-reclaim-second-pass", iconView);
                refreshOnViewThread(context, uiMech,
                        source + "#vendor-reclaim-second-pass", false, false);
            }
        }, VENDOR_RECLAIM_SECOND_PASS_DELAY_MS);
    }

    /** Pixel static ridge or anim carrier wrapping vendor temp-show. */
    private static boolean isPixelOwnedDrawable(Drawable drawable) {
        return drawable instanceof PixelFingerprintDrawable
                || drawable instanceof PixelFingerprintAnimCarrier;
    }

    /** True for raw OOS temporary-show animations (not our carrier wrapper). */
    private static boolean isVendorTemporaryAnimation(Drawable drawable) {
        if (drawable == null
                || drawable instanceof PixelFingerprintDrawable
                || drawable instanceof PixelFingerprintAnimCarrier) {
            return false;
        }
        String name = drawable.getClass().getName();
        return name.contains("AnimationDrawable")
                || name.contains("AnimatedImageDrawable")
                || name.contains("AnimatedVectorDrawable");
    }

    private static void cancelPendingReclaim(ImageView iconView) {
        Runnable pending;
        synchronized (PENDING_RECLAIMS) {
            pending = PENDING_RECLAIMS.remove(iconView);
        }
        if (pending != null) {
            iconView.removeCallbacks(pending);
        }
    }

    private static ImageView findFingerprintIcon(Object uiMech) {
        String[] fieldNames = {
                "fpIcon", "mFpIcon", "fingerprintIcon", "mFingerprintIcon",
                "fpIconView", "mFpIconView", "udfpsIcon", "mUdfpsIcon"
        };
        for (String fieldName : fieldNames) {
            try {
                Object field = ModernHookBridge.getObjectField(uiMech, fieldName);
                if (field instanceof ImageView) {
                    return (ImageView) field;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Object result = ModernHookBridge.callMethod(uiMech, "getFingerprintIcon");
            if (result instanceof ImageView) {
                return (ImageView) result;
            }
        } catch (Throwable ignored) {
        }
        Class<?> current = uiMech.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (!ImageView.class.isAssignableFrom(field.getType())
                        || (!name.contains("fp") && !name.contains("finger")
                        && !name.contains("udfps"))) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(uiMech);
                    if (value instanceof ImageView) {
                        return (ImageView) value;
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static ImageView findFingerprintPressedIcon(Object uiMech) {
        String[] fieldNames = {
                "pressedIcon", "mPressedIcon", "fingerprintPressedIcon", "mFingerprintPressedIcon"
        };
        for (String fieldName : fieldNames) {
            try {
                Object field = ModernHookBridge.getObjectField(uiMech, fieldName);
                if (field instanceof ImageView) {
                    return (ImageView) field;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Object result = ModernHookBridge.callMethod(uiMech, "getFingerprintPressedIcon");
            if (result instanceof ImageView) {
                return (ImageView) result;
            }
        } catch (Throwable ignored) {
        }
        Class<?> current = uiMech.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (!ImageView.class.isAssignableFrom(field.getType())
                        || (!name.contains("pressed") && !name.contains("press"))) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(uiMech);
                    if (value instanceof ImageView) {
                        return (ImageView) value;
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean readBooleanField(Object receiver, String fieldName) {
        try {
            Object value = ModernHookBridge.getObjectField(receiver, fieldName);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isDarkMode(Context context) {
        if (context == null) {
            return true;
        }
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isInteractive(Context context) {
        if (context == null) {
            return false;
        }
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isInteractive();
    }

    private static void logCarrierState(String stage, ImageView primary,
            ImageView pressed, String source) {
        if (!PixelAodLog.isDebugEnabled()) {
            return;
        }
        PixelAodLog.log("Pixel fingerprint carrier state", () ->
                "Pixel fingerprint carrier state"
                + " stage=" + stage
                + " source=" + source
                + " primary={" + describeCarrier(primary) + "}"
                + " pressed={" + describeCarrier(pressed) + "}");
    }

    private static void logSingleCarrierState(String stage, ImageView view) {
        if (!PixelAodLog.isDebugEnabled()) {
            return;
        }
        PixelAodLog.log("Pixel fingerprint carrier state", () ->
                "Pixel fingerprint carrier state"
                + " stage=" + stage
                + " view={" + describeCarrier(view) + "}");
    }

    private static String describeCarrier(ImageView view) {
        if (view == null) {
            return "null";
        }
        Drawable drawable = view.getDrawable();
        Drawable background = view.getBackground();
        Object parent = view.getParent();
        return "class=" + view.getClass().getName()
                + ",visibility=" + visibilityName(view.getVisibility())
                + ",alpha=" + view.getAlpha()
                + ",imageAlpha=" + view.getImageAlpha()
                + ",attached=" + view.isAttachedToWindow()
                + ",size=" + view.getWidth() + "x" + view.getHeight()
                + ",xy=" + view.getX() + "," + view.getY()
                + ",drawable=" + describeDrawable(drawable)
                + ",background=" + describeDrawable(background)
                + ",parent=" + (parent != null ? parent.getClass().getName() : "null");
    }

    private static String describeDrawable(Drawable drawable) {
        if (drawable == null) {
            return "null";
        }
        return drawable.getClass().getName() + "@alpha=" + drawable.getAlpha();
    }

    private static String visibilityName(int visibility) {
        if (visibility == 0) {
            return "VISIBLE";
        }
        if (visibility == 4) {
            return "INVISIBLE";
        }
        if (visibility == 8) {
            return "GONE";
        }
        return String.valueOf(visibility);
    }

    private static void logStateIfChanged(ImageView view, String state,
            String source, String detail) {
        if (!PixelAodLog.isDebugEnabled()) {
            return;
        }
        synchronized (LAST_LOGGED_STATES) {
            if (state.equals(LAST_LOGGED_STATES.get(view))) {
                return;
            }
            LAST_LOGGED_STATES.put(view, state);
        }
        PixelAodLog.log("Pixel fingerprint icon state=" + state
                + " source=" + source + " " + detail);
    }
}
