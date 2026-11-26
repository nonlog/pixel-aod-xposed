package dev.codex.pixelaod;

import android.animation.Animator;
import android.app.Notification;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Re-skins OPlus' native screen-off incoming-notification surface while leaving its lifecycle,
 * notification admission, privacy processing, and removal timing untouched.
 */
final class PixelPeekNotificationController {
    private static final String OPLUS_PEEK_VIEW =
            "com.oplus.systemui.aod.surface.OplusAodCurvedDisplayView";
    private static final String CURVED_DISPLAY_MANAGER =
            "com.oplus.systemui.aod.common.CurvedDisplayManager";
    private static final String SHOW_CURVED_NOTIFICATION =
            "com.oplus.systemui.aod.common.CurvedDisplayManager$ShowCurvedNotification";
    private static final String AOD_MANAGER =
            "com.oplus.systemui.aod.common.AodManager";
    private static final Map<View, State> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Class<?>> HOOKED_ANIMATION_END_LISTENER_CLASSES =
            Collections.synchronizedSet(new HashSet<>());

    private PixelPeekNotificationController() {
    }

    static void install(ClassLoader classLoader) {
        try {
            Class<?> peekClass = ModernHookBridge.findClass(OPLUS_PEEK_VIEW, classLoader);
            ModernHookBridge.hookAfter(peekClass, "updateReceiveNotification", param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View nativeView = (View) param.thisObject;
                if (isSettingPreview(nativeView)) {
                    return;
                }
                StatusBarNotification sbn = param.args != null && param.args.length > 0
                        && param.args[0] instanceof StatusBarNotification
                        ? (StatusBarNotification) param.args[0] : null;
                if (sbn == null) {
                    clear(nativeView, "native-update-null");
                    return;
                }
                State state = state(nativeView);
                state.nativeAttached = nativeView.isAttachedToWindow();
                PixelPeekNotificationContent content = contentFromVendorPaint(nativeView, sbn);
                if (content == null || !content.hasRenderableText()) {
                    state.content = null;
                    removeOverlay(state.overlay.get());
                    nativeView.invalidate();
                    PixelAodLog.log("Pixel peek kept native presentation reason=no-safe-content"
                            + " pkg=" + sbn.getPackageName() + " key=" + sbn.getKey());
                    if (state.nativeAttached) {
                        onNativeWindowActive(nativeView, state, "native-update-no-safe-content");
                    }
                    return;
                }
                state.content = content;
                state.lastNotificationKey = sbn.getKey();
                state.lastNotificationPostTime = sbn.getPostTime();
                state.vendorDisplayTimeMs = readIntField(nativeView, "mNotificationDisplayTime", -1);
                PixelAodLog.log("Pixel peek captured vendor-safe notification"
                        + " pkg=" + content.packageName
                        + " key=" + content.notificationKey
                        + " attached=" + state.nativeAttached);
                if (state.nativeAttached) {
                    onNativeWindowActive(nativeView, state, "native-update");
                }
            }, StatusBarNotification.class);

            ModernHookBridge.hookAfter(peekClass, "onAttachedToWindow", param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View nativeView = (View) param.thisObject;
                if (isSettingPreview(nativeView)) {
                    return;
                }
                State state = state(nativeView);
                long now = SystemClock.uptimeMillis();
                state.nativeAttached = true;
                state.sessionStartMs = now;
                state.lastUpdateMs = now;
                state.replacementCount = 0;
                state.deferToken++;
                state.bypassDefer = false;
        state.deferDismissScheduled = false;
                state.deferDismissScheduled = false;
                state.vendorDisplayTimeMs = readIntField(nativeView, "mNotificationDisplayTime", -1);
                Object existingListener = readFieldOrNull(nativeView, "mAnimationEndListener");
                if (existingListener != null) {
                    state.animationEndListener = new WeakReference<>(existingListener);
                    ensureAnimationEndListenerHooked(existingListener.getClass());
                }
                PixelAodLog.log("Pixel peek native surface attached"
                        + " parent=" + parentChain(nativeView)
                        + " hasContent=" + (state.content != null));
                onNativeWindowActive(nativeView, state, "native-attach");
            });

            ModernHookBridge.hookAfter(peekClass, "onDetachedFromWindow", param -> {
                if (param.thisObject instanceof View) {
                    clear((View) param.thisObject, "native-detach");
                }
            });

            ModernHookBridge.hookBefore(peekClass, "onDraw", param -> {
                if (!(param.thisObject instanceof View)
                        || param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Canvas)) {
                    return;
                }
                View nativeView = (View) param.thisObject;
                if (isSettingPreview(nativeView)) {
                    return;
                }
                State state = STATES.get(nativeView);
                boolean customEnabled = PixelAodSettings.getBoolean(nativeView.getContext(),
                        PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, true);
                boolean nativeEnabled =
                        NativeOplusPeekSettingAdapter.isEnabled(nativeView.getContext());
                if (!PixelPeekPresentationPolicy.shouldSuppressNativeDraw(
                        customEnabled, nativeEnabled,
                        state != null && state.content != null,
                        state != null && state.overlay.get() != null)) {
                    return;
                }
                param.setResult(null);
            }, Canvas.class);

            ModernHookBridge.hookAfter(peekClass, "onDraw", param -> {
                if (!(param.thisObject instanceof View)
                        || param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Canvas)) {
                    return;
                }
                View nativeView = (View) param.thisObject;
                if (isSettingPreview(nativeView)) {
                    return;
                }
                State state = STATES.get(nativeView);
                if (state == null) {
                    return;
                }
                Context context = nativeView.getContext();
                boolean customEnabled = PixelAodSettings.getBoolean(context,
                        PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, true);
                boolean nativeEnabled = NativeOplusPeekSettingAdapter.isEnabled(context);
                boolean composite =
                        PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                                customEnabled, nativeEnabled, state.nativeAttached,
                                state.nativeAodCompositeRequested);
                if (!composite) {
                    state.nativeAodCompositeLogged = false;
                    return;
                }
                boolean drawn = ActiveClockRendererController.drawAodIntoNativePeekSurface(
                        (Canvas) param.args[0], nativeView, "PixelPeek#native-onDraw");
                if (drawn && !state.nativeAodCompositeLogged) {
                    state.nativeAodCompositeLogged = true;
                    PixelAodLog.log("composited module AOD over native OPlus Peek"
                            + " nativeView=" + nativeView.getClass().getName()
                            + " nativeSize=" + nativeView.getWidth() + "x"
                            + nativeView.getHeight());
                }
            }, Canvas.class);

            installAnimationEndListenerHook(peekClass);
            installConsecutiveNotificationHooks(classLoader);
            PixelAodLog.log("installed optional Pixel peek presentation class=" + OPLUS_PEEK_VIEW);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install optional Pixel peek presentation", t);
        }
    }

    private static State state(View nativeView) {
        synchronized (STATES) {
            State state = STATES.get(nativeView);
            if (state == null) {
                state = new State();
                STATES.put(nativeView, state);
            }
            return state;
        }
    }

    /** Native OPlus Peek attachment is the authority for the transient notification window. */
    static boolean hasActiveNativeNotificationWindow() {
        synchronized (STATES) {
            for (State state : STATES.values()) {
                if (state != null && state.nativeAttached) {
                    return true;
                }
            }
        }
        return false;
    }

    static void onSettingsChanged(Context context, String source) {
        synchronized (STATES) {
            for (Map.Entry<View, State> entry : STATES.entrySet()) {
                View nativeView = entry.getKey();
                State state = entry.getValue();
                if (nativeView == null || state == null) {
                    continue;
                }
                nativeView.post(() -> {
                    if (state.nativeAttached) {
                        onNativeWindowActive(nativeView, state, "settings#" + source);
                    } else {
                        removeOverlay(state.overlay.get());
                        nativeView.invalidate();
                    }
                });
            }
        }
        if (context != null && !NativeOplusPeekSettingAdapter.isEnabled(context)) {
            PixelAodClockView.endPowerSavingNotificationAod(
                    "PixelPeek#native-setting-disabled#" + source);
        }
        PixelAodClockView.onNativeNotificationWindowChanged("PixelPeek#settings#" + source);
    }

    static void onPocketGuardChanged(boolean blocked, String source) {
        synchronized (STATES) {
            for (Map.Entry<View, State> entry : STATES.entrySet()) {
                View nativeView = entry.getKey();
                State state = entry.getValue();
                if (nativeView == null || state == null) {
                    continue;
                }
                nativeView.post(() -> {
                    if (blocked) {
                        removeOverlay(state.overlay.get());
                        state.powerSavingAodStarted = false;
                        nativeView.invalidate();
                    } else if (state.nativeAttached) {
                        onNativeWindowActive(nativeView, state, "pocket-release");
                    }
                });
            }
        }
        if (blocked) {
            PixelAodClockView.endPowerSavingNotificationAod("PixelPeek#pocket-guard#" + source);
        }
        PixelAodClockView.onNativeNotificationWindowChanged("PixelPeek#pocket-guard#" + source);
        PixelAodLog.log("Pixel peek pocket guard changed blocked=" + blocked
                + " source=" + source);
    }

    private static void onNativeWindowActive(View nativeView, State state, String source) {
        if (nativeView == null || state == null || !state.nativeAttached) {
            return;
        }
        Context context = nativeView.getContext();
        if (!NativeOplusPeekSettingAdapter.isEnabled(context)) {
            removeOverlay(state.overlay.get());
            state.powerSavingAodStarted = false;
            state.nativeAodCompositeRequested = false;
            state.nativeAodCompositeLogged = false;
            nativeView.invalidate();
            PixelAodClockView.onNativeNotificationWindowChanged(
                    "PixelPeek#" + source + "#native-disabled");
            return;
        }
        if (PixelAodClockView.isPocketGuardActive()) {
            removeOverlay(state.overlay.get());
            state.powerSavingAodStarted = false;
            state.nativeAodCompositeRequested = false;
            state.nativeAodCompositeLogged = false;
            nativeView.invalidate();
            PixelAodLog.log("Pixel peek suppressed by pocket/proximity guard source=" + source);
            PixelAodClockView.onNativeNotificationWindowChanged(
                    "PixelPeek#" + source + "#pocket-blocked");
            return;
        }
        if (!state.powerSavingAodStarted) {
            String detail = state.content != null
                    ? "key=" + state.content.notificationKey + ",pkg=" + state.content.packageName
                    : "native-window-attached";
            state.powerSavingAodStarted = PixelAodClockView.startPowerSavingNotificationAod(
                    context, "PixelPeek#" + source, detail);
        }

        boolean customEnabled = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, true);
        boolean moduleAodWanted = PixelAodClockView.shouldApplyModuleAodNow(
                context, "PixelPeek#" + source + "#native-surface-composite");
        state.nativeAodCompositeRequested =
                PixelPeekPresentationPolicy.shouldCompositeModuleAodOverNativePeek(
                        customEnabled, true, true, moduleAodWanted);
        state.nativeAodCompositeLogged = false;
        if (PixelPeekPresentationPolicy.shouldUsePixelPeek(customEnabled, true)
                && state.content != null) {
            showPixelPeek(nativeView, state, source);
        } else {
            removeOverlay(state.overlay.get());
            nativeView.invalidate();
            PixelAodLog.log("Pixel peek preserving native presentation"
                    + " source=" + source
                    + " customEnabled=" + customEnabled
                    + " hasContent=" + (state.content != null));
        }
        PixelAodClockView.onNativeNotificationWindowChanged("PixelPeek#" + source);
    }

    private static void showPixelPeek(View nativeView, State state, String source) {
        if (nativeView == null || state == null || state.content == null) {
            return;
        }
        ViewGroup host = PixelAodHook.currentPixelPresentationHost();
        if (host == null || !PrimaryDisplayPolicy.isPrimary(host)) {
            PixelAodLog.log("Pixel peek delayed reason=no-primary-pixel-host source=" + source
                    + " nativeParent=" + parentChain(nativeView));
            return;
        }
        PixelPeekNotificationView overlay = state.overlay.get();
        if (overlay == null || overlay.getParent() != host) {
            removeOverlay(overlay);
            overlay = new PixelPeekNotificationView(host.getContext());
            overlay.setTag("dev.codex.pixelaod.PIXEL_PEEK_NOTIFICATION");
            host.setClipChildren(false);
            host.setClipToPadding(false);
            host.addView(overlay, host.getChildCount(), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            state.overlay = new WeakReference<>(overlay);
        }
        overlay.setContent(state.content);
        overlay.setVisibility(View.VISIBLE);
        overlay.bringToFront();
        overlay.setElevation(10_000f);
        if (overlay.getAlpha() < 1f) {
            overlay.animate().cancel();
        }
        overlay.setAlpha(1f);
        layoutOverlayToHost(host, overlay);
        positionOverlayBelowAmbientContent(host, overlay, source + "#initial");
        PixelPeekNotificationView positionedOverlay = overlay;
        host.post(() -> positionOverlayBelowAmbientContent(
                host, positionedOverlay, source + "#post-layout"));
        host.postDelayed(() -> positionOverlayBelowAmbientContent(
                host, positionedOverlay, source + "#settled-layout"), 120L);
        nativeView.invalidate();
        host.invalidate();
        PixelAodLog.log("Pixel peek presentation active source=" + source
                + " pkg=" + state.content.packageName
                + " key=" + state.content.notificationKey
                + " host=" + host.getClass().getName()
                + " hostSize=" + host.getWidth() + "x" + host.getHeight()
                + " overlaySize=" + overlay.getWidth() + "x" + overlay.getHeight()
                + " overlayShown=" + overlay.isShown()
                + " overlayAlpha=" + overlay.getAlpha());
    }

    private static void layoutOverlayToHost(ViewGroup host, View overlay) {
        int width = host != null ? host.getWidth() : 0;
        int height = host != null ? host.getHeight() : 0;
        if (overlay == null || width <= 0 || height <= 0) {
            return;
        }
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        overlay.measure(widthSpec, heightSpec);
        overlay.layout(0, 0, width, height);
    }

    private static void positionOverlayBelowAmbientContent(ViewGroup host,
            PixelPeekNotificationView overlay, String source) {
        if (host == null || overlay == null || overlay.getParent() != host) {
            return;
        }
        CouiClockHostView clockHost = findCouiClockHost(host);
        int[] hostLocation = new int[2];
        host.getLocationInWindow(hostLocation);
        int ambientBottomWindow = clockHost != null
                ? clockHost.peekForegroundBottomInWindow() : 0;
        float ambientBottomLocal = Math.max(0f, ambientBottomWindow - hostLocation[1]);
        float density = host.getResources().getDisplayMetrics().density;
        float top = PixelPeekGeometryPolicy.resolveCardTopPx(
                density, host.getHeight(), ambientBottomLocal);
        overlay.setCardTopPx(top);
        PixelAodLog.log("Pixel peek geometry source=" + source
                + " ambientBottomWindow=" + ambientBottomWindow
                + " hostWindowY=" + hostLocation[1]
                + " ambientBottomLocal=" + ambientBottomLocal
                + " cardTop=" + top
                + " density=" + density
                + " clockHost=" + (clockHost != null));
    }

    private static CouiClockHostView findCouiClockHost(View root) {
        if (root instanceof CouiClockHostView) {
            return (CouiClockHostView) root;
        }
        if (!(root instanceof ViewGroup)) {
            return null;
        }
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            CouiClockHostView found = findCouiClockHost(group.getChildAt(i));
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void clear(View nativeView, String source) {
        State state;
        synchronized (STATES) {
            state = STATES.remove(nativeView);
        }
        if (state == null) {
            return;
        }
        state.nativeAttached = false;
        state.nativeAodCompositeRequested = false;
        state.nativeAodCompositeLogged = false;
        state.deferToken++;
        state.replacementCount = 0;
        state.bypassDefer = false;
        PixelPeekNotificationView overlay = state.overlay.get();
        removeOverlay(overlay);
        state.powerSavingAodStarted = false;
        if (!hasActiveNativeNotificationWindow()) {
            PixelAodClockView.endPowerSavingNotificationAod("PixelPeek#" + source);
        }
        PixelAodClockView.onNativeNotificationWindowChanged("PixelPeek#" + source);
        PixelAodLog.log("Pixel peek presentation cleared source=" + source
                + " key=" + (state.content != null ? state.content.notificationKey : "none"));
    }

    private static void removeOverlay(View overlay) {
        if (overlay == null) {
            return;
        }
        ViewGroup parent = overlay.getParent() instanceof ViewGroup
                ? (ViewGroup) overlay.getParent() : null;
        if (parent != null) {
            parent.removeView(overlay);
        }
    }

    private static PixelPeekNotificationContent contentFromVendorPaint(View nativeView,
            StatusBarNotification sbn) {
        try {
            Object paint = readField(nativeView, "mIncomingNotiPaint");
            if (paint == null) {
                return null;
            }
            CharSequence appName = text(readField(paint, "mHeaderLayout"));
            CharSequence title = text(readField(paint, "mTitleLayout"));
            CharSequence message = text(readField(paint, "mMessageLayout"));
            // OPlus' mIncomingNotiPaint.mDrawable is deliberately the launcher/application icon.
            // Pixel ambient notifications use the notification's own monochrome smallIcon instead.
            Drawable icon = loadNotificationSmallIcon(nativeView, sbn);
            return new PixelPeekNotificationContent(sbn.getKey(), sbn.getPackageName(),
                    appName, title, message, icon);
        } catch (Throwable t) {
            PixelAodLog.log("failed to copy vendor-safe Pixel peek content pkg="
                    + sbn.getPackageName(), t);
            return null;
        }
    }

    private static CharSequence text(Object value) {
        return value instanceof Layout ? PixelPeekNotificationContent.textFromLayout(value) : "";
    }

    private static Drawable cloneDrawable(Object value, View nativeView) {
        if (!(value instanceof Drawable)) {
            return null;
        }
        Drawable drawable = (Drawable) value;
        try {
            Drawable.ConstantState constantState = drawable.getConstantState();
            if (constantState != null) {
                return constantState.newDrawable(nativeView.getResources()).mutate();
            }
        } catch (Throwable ignored) {
        }
        return drawable;
    }

    private static Drawable loadNotificationSmallIcon(View nativeView, StatusBarNotification sbn) {
        if (nativeView == null || sbn == null) {
            return null;
        }
        try {
            Notification notification = sbn.getNotification();
            Icon smallIcon = notification != null ? notification.getSmallIcon() : null;
            if (smallIcon == null) {
                PixelAodLog.log("Pixel peek notification smallIcon unavailable pkg="
                        + sbn.getPackageName() + " key=" + sbn.getKey());
                return null;
            }
            Drawable drawable = smallIcon.loadDrawable(nativeView.getContext());
            if (drawable == null) {
                PixelAodLog.log("Pixel peek notification smallIcon drawable missing pkg="
                        + sbn.getPackageName() + " key=" + sbn.getKey());
                return null;
            }
            Drawable result = cloneDrawable(drawable, nativeView);
            if (result == null) {
                return null;
            }
            // Keep the notification's native smallIcon artwork exactly as supplied. The Peek
            // card must not recolor it with Material/Monet accent, and must never substitute the
            // launcher/application icon from OPlus' mIncomingNotiPaint.mDrawable.
            result.setTintList(null);
            result.clearColorFilter();
            PixelAodLog.log("Pixel peek icon source=notification-smallIcon tint=none pkg="
                    + sbn.getPackageName() + " key=" + sbn.getKey()
                    + " iconType=" + (android.os.Build.VERSION.SDK_INT >= 28
                            ? smallIcon.getType() : -1));
            return result;
        } catch (Throwable t) {
            PixelAodLog.log("failed to load Pixel peek notification smallIcon pkg="
                    + sbn.getPackageName(), t);
            return null;
        }
    }

    private static boolean isSettingPreview(View view) {
        try {
            Object value = readField(view, "isSettingInterface");
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private static Object readField(Object owner, String name) throws ReflectiveOperationException {
        Class<?> current = owner.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static String parentChain(View view) {
        StringBuilder out = new StringBuilder();
        Object current = view;
        for (int i = 0; current != null && i < 6; i++) {
            if (i > 0) {
                out.append(" <- ");
            }
            out.append(current.getClass().getName());
            current = current instanceof View ? ((View) current).getParent() : null;
        }
        return out.toString();
    }

    private static void installAnimationEndListenerHook(Class<?> peekClass) {
        try {
            int hooked = 0;
            for (Method method : peekClass.getDeclaredMethods()) {
                if (!"setAnimationEndListener".equals(method.getName())
                        || method.getParameterTypes().length != 1) {
                    continue;
                }
                ModernHookBridge.hookAfter(method, param -> {
                    if (!(param.thisObject instanceof View)
                            || param.args == null || param.args.length == 0
                            || param.args[0] == null) {
                        return;
                    }
                    View nativeView = (View) param.thisObject;
                    if (isSettingPreview(nativeView)) {
                        return;
                    }
                    Object listener = param.args[0];
                    State state = state(nativeView);
                    state.animationEndListener = new WeakReference<>(listener);
                    ensureAnimationEndListenerHooked(listener.getClass());
                });
                hooked++;
            }
            if (hooked > 0) {
                PixelAodLog.log("installed Pixel peek animation-end listener hook count=" + hooked);
            }
        } catch (Throwable t) {
            PixelAodLog.log("failed to install Pixel peek animation-end listener hook", t);
        }
    }

    private static void ensureAnimationEndListenerHooked(Class<?> listenerClass) {
        if (listenerClass == null || !HOOKED_ANIMATION_END_LISTENER_CLASSES.add(listenerClass)) {
            return;
        }
        try {
            Method onAnimationEnd = listenerClass.getMethod("onAnimationEnd", Animator.class);
            ModernHookBridge.hookBefore(onAnimationEnd, param -> {
                Object listener = param.thisObject;
                if (listener == null) {
                    return;
                }
                View matchedView = null;
                State matchedState = null;
                synchronized (STATES) {
                    for (Map.Entry<View, State> entry : STATES.entrySet()) {
                        State candidate = entry.getValue();
                        if (candidate != null && candidate.nativeAttached
                                && candidate.animationEndListener.get() == listener) {
                            matchedView = entry.getKey();
                            matchedState = candidate;
                            break;
                        }
                    }
                }
                if (matchedView == null || matchedState == null || matchedState.bypassDefer) {
                    return;
                }
                long now = SystemClock.uptimeMillis();
                long deferMs = PixelPeekConsecutivePolicy.computeDeferMillis(
                        matchedState.sessionStartMs,
                        matchedState.lastUpdateMs,
                        now,
                        matchedState.vendorDisplayTimeMs,
                        matchedState.replacementCount);
                if (deferMs <= 0L) {
                    return;
                }
                param.setResult(null);
                final View viewRef = matchedView;
                final State stateRef = matchedState;
                final Animator animatorArg = param.args != null && param.args.length > 0
                        && param.args[0] instanceof Animator ? (Animator) param.args[0] : null;
                if (!stateRef.deferDismissScheduled) {
                    stateRef.deferDismissScheduled = true;
                    scheduleDeferredDismiss(viewRef, stateRef, onAnimationEnd, listener,
                            animatorArg, deferMs);
                }
                PixelAodLog.log("Pixel peek deferred native dismiss deferMs=" + deferMs
                        + " replacements=" + stateRef.replacementCount);
            });
            PixelAodLog.log("hooked Pixel peek animation-end listener class=" + listenerClass.getName());
        } catch (Throwable t) {
            PixelAodLog.log("failed to hook Pixel peek animation-end listener class="
                    + listenerClass.getName(), t);
        }
    }

    private static void scheduleDeferredDismiss(View viewRef, State stateRef,
            Method onAnimationEnd, Object listener, Animator animatorArg, long delayMs) {
        final int sessionToken = stateRef.deferToken;
        viewRef.postDelayed(() -> {
            if (!stateRef.nativeAttached || stateRef.deferToken != sessionToken) {
                stateRef.deferDismissScheduled = false;
                return;
            }
            long remainingMs = PixelPeekConsecutivePolicy.computeDeferMillis(
                    stateRef.sessionStartMs,
                    stateRef.lastUpdateMs,
                    SystemClock.uptimeMillis(),
                    stateRef.vendorDisplayTimeMs,
                    stateRef.replacementCount);
            if (remainingMs > 0L) {
                scheduleDeferredDismiss(viewRef, stateRef, onAnimationEnd, listener,
                        animatorArg, remainingMs);
                return;
            }
            stateRef.deferDismissScheduled = false;
            stateRef.bypassDefer = true;
            try {
                onAnimationEnd.invoke(listener, animatorArg);
            } catch (Throwable t) {
                PixelAodLog.log("deferred Pixel peek onAnimationEnd invoke failed", t);
            } finally {
                stateRef.bypassDefer = false;
            }
        }, delayMs);
    }

    private static void installConsecutiveNotificationHooks(ClassLoader classLoader) {
        try {
            Class<?> managerClass = ModernHookBridge.findClass(CURVED_DISPLAY_MANAGER, classLoader);
            ModernHookBridge.hookAfter(managerClass, "onReceiveNotification", param -> {
                Object manager = param.thisObject;
                if (manager == null || param.args == null || param.args.length < 3) {
                    return;
                }
                StatusBarNotification sbn = param.args[1] instanceof StatusBarNotification
                        ? (StatusBarNotification) param.args[1] : null;
                boolean isNew = param.args[2] instanceof Boolean && (Boolean) param.args[2];
                if (sbn == null || !isNew) {
                    return;
                }
                maybeForwardConsecutiveNotification(manager, sbn, "curved-manager-onReceive");
            }, StatusBarNotification[].class, StatusBarNotification.class, boolean.class);
            PixelAodLog.log("installed Pixel peek consecutive hook on " + CURVED_DISPLAY_MANAGER);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install Pixel peek CurvedDisplayManager hook", t);
        }

        try {
            Class<?> runnableClass = ModernHookBridge.findClass(SHOW_CURVED_NOTIFICATION, classLoader);
            ModernHookBridge.hookAfter(runnableClass, "run", param -> {
                Object runnable = param.thisObject;
                if (runnable == null) {
                    return;
                }
                Object manager = readFieldOrNull(runnable, "instance");
                Object notifObj = readFieldOrNull(runnable, "mNotification");
                if (manager == null || !(notifObj instanceof StatusBarNotification)) {
                    return;
                }
                maybeForwardConsecutiveNotification(manager, (StatusBarNotification) notifObj,
                        "curved-show-runnable");
            });
            PixelAodLog.log("installed Pixel peek consecutive hook on " + SHOW_CURVED_NOTIFICATION);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install Pixel peek ShowCurvedNotification hook", t);
        }
    }

    private static void maybeForwardConsecutiveNotification(Object manager,
            StatusBarNotification sbn, String source) {
        Object viewObj = readFieldOrNull(manager, "mCurvedDisplayView");
        if (!(viewObj instanceof View)) {
            return;
        }
        View nativeView = (View) viewObj;
        if (!nativeView.isAttachedToWindow() || isSettingPreview(nativeView)) {
            return;
        }
        State state = STATES.get(nativeView);
        if (state == null || !state.nativeAttached) {
            return;
        }
        Context context = nativeView.getContext();
        if (context == null) {
            return;
        }
        boolean customEnabled = PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_PIXEL_NOTIFICATION_PEEK, true);
        boolean nativeEnabled = NativeOplusPeekSettingAdapter.isEnabled(context);
        if (!PixelPeekPresentationPolicy.shouldUsePixelPeek(customEnabled, nativeEnabled)) {
            return;
        }
        if (PixelAodClockView.isPocketGuardActive()) {
            return;
        }
        Notification notification = sbn.getNotification();
        if (notification == null || (notification.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0 || sbn.isOngoing()) {
            return;
        }
        if (PixelPeekConsecutivePolicy.shouldRejectPackageOrId(sbn.getPackageName(), sbn.getId())) {
            return;
        }
        if (PixelPeekConsecutivePolicy.isDuplicateNotification(
                state.lastNotificationKey, state.lastNotificationPostTime,
                sbn.getKey(), sbn.getPostTime())) {
            return;
        }
        if (!passesVendorAdmissionChecks(manager, context, sbn)) {
            return;
        }

        nativeView.post(() -> {
            if (!nativeView.isAttachedToWindow() || !state.nativeAttached) {
                return;
            }
            if (PixelPeekConsecutivePolicy.isDuplicateNotification(
                    state.lastNotificationKey, state.lastNotificationPostTime,
                    sbn.getKey(), sbn.getPostTime())) {
                return;
            }
            long now = SystemClock.uptimeMillis();
            state.replacementCount++;
            state.lastUpdateMs = now;
            writeFieldQuietly(nativeView, "mTime", System.currentTimeMillis());
            acquireVendorWakeLockQuietly(context, nativeView.getClass().getClassLoader());
            try {
                Method updateMethod = nativeView.getClass().getMethod(
                        "updateReceiveNotification", StatusBarNotification.class);
                updateMethod.invoke(nativeView, sbn);
                if (state.powerSavingAodStarted) {
                    PixelAodClockView.startPowerSavingNotificationAod(
                            context,
                            "PixelPeek#" + source + "#consecutive",
                            "key=" + sbn.getKey() + ",pkg=" + sbn.getPackageName());
                }
                PixelAodLog.log("Pixel peek updated for consecutive notification"
                        + " source=" + source
                        + " pkg=" + sbn.getPackageName()
                        + " key=" + sbn.getKey()
                        + " replacements=" + state.replacementCount);
            } catch (Throwable t) {
                PixelAodLog.log("failed to forward consecutive Pixel peek notification", t);
            }
        });
    }

    private static boolean passesVendorAdmissionChecks(Object manager, Context context,
            StatusBarNotification sbn) {
        try {
            Method checkCurvedState = manager.getClass().getMethod("checkCurvedState");
            Object allowed = checkCurvedState.invoke(manager);
            if (allowed instanceof Boolean && !((Boolean) allowed)) {
                return false;
            }
        } catch (Throwable ignored) {
        }
        int zenMode = readIntField(manager, "mZenMode", 0);
        int zenNotif = readIntField(manager, "mZenModeNotification", 0);
        int zenCurve = readIntField(manager, "mZenModeCurveDisplay", 0);
        if (zenMode == 1 && (zenNotif != 1 || zenCurve != 1)) {
            return false;
        }
        Object keyguardHelper = readFieldOrNull(manager, "mNotificationKeyguardHelper");
        if (keyguardHelper != null) {
            try {
                Method shouldShow = keyguardHelper.getClass().getMethod(
                        "shouldShowOnKeyguard", StatusBarNotification.class);
                Object result = shouldShow.invoke(keyguardHelper, sbn);
                if (result instanceof Boolean && !((Boolean) result)) {
                    return false;
                }
            } catch (Throwable ignored) {
            }
        }
        Object packagesObj = readFieldOrNull(manager, "mPackages");
        if (packagesObj instanceof Collection<?>) {
            Collection<?> packages = (Collection<?>) packagesObj;
            if (!packages.isEmpty() && !packages.contains(sbn.getPackageName())) {
                return false;
            }
        }
        try {
            Class<?> aodManagerClass = ModernHookBridge.findClass(
                    AOD_MANAGER, manager.getClass().getClassLoader());
            Object aodManager = aodManagerClass.getMethod("getInstance", Context.class)
                    .invoke(null, context);
            if (aodManager != null) {
                Object inCall = aodManagerClass.getMethod("isInCall").invoke(aodManager);
                if (inCall instanceof Boolean && (Boolean) inCall) {
                    return false;
                }
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static void acquireVendorWakeLockQuietly(Context context, ClassLoader classLoader) {
        try {
            Class<?> aodManagerClass = ModernHookBridge.findClass(AOD_MANAGER, classLoader);
            Object aodManager = aodManagerClass.getMethod("getInstance", Context.class)
                    .invoke(null, context);
            if (aodManager != null) {
                aodManagerClass.getMethod("acquireWakeLock", long.class).invoke(aodManager, 6000L);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object readFieldOrNull(Object owner, String name) {
        if (owner == null) {
            return null;
        }
        try {
            return readField(owner, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int readIntField(Object owner, String name, int fallback) {
        Object value = readFieldOrNull(owner, name);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static void writeFieldQuietly(Object owner, String name, Object value) {
        if (owner == null) {
            return;
        }
        Class<?> current = owner.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                field.set(owner, value);
                return;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    private static final class State {
        PixelPeekNotificationContent content;
        WeakReference<PixelPeekNotificationView> overlay = new WeakReference<>(null);
        WeakReference<Object> animationEndListener = new WeakReference<>(null);
        boolean nativeAttached;
        boolean powerSavingAodStarted;
        boolean nativeAodCompositeRequested;
        boolean nativeAodCompositeLogged;
        long sessionStartMs;
        long lastUpdateMs;
        int vendorDisplayTimeMs = -1;
        String lastNotificationKey;
        long lastNotificationPostTime;
        int replacementCount;
        int deferToken;
        boolean bypassDefer;
        boolean deferDismissScheduled;
    }
}
