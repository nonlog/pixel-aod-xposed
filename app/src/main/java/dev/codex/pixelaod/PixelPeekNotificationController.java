package dev.codex.pixelaod;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Re-skins OPlus' native screen-off incoming-notification surface while leaving its lifecycle,
 * notification admission, privacy processing, and removal timing untouched.
 */
final class PixelPeekNotificationController {
    private static final String OPLUS_PEEK_VIEW =
            "com.oplus.systemui.aod.surface.OplusAodCurvedDisplayView";
    private static final Map<View, State> STATES =
            Collections.synchronizedMap(new WeakHashMap<>());

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
                PixelPeekNotificationContent content = contentFromVendorPaint(nativeView, sbn);
                if (content == null || !content.hasRenderableText()) {
                    PixelAodLog.log("Pixel peek kept native presentation reason=no-safe-content"
                            + " pkg=" + sbn.getPackageName() + " key=" + sbn.getKey());
                    return;
                }
                State state = state(nativeView);
                state.content = content;
                PixelAodLog.log("Pixel peek captured vendor-safe notification"
                        + " pkg=" + content.packageName
                        + " key=" + content.notificationKey
                        + " attached=" + nativeView.isAttachedToWindow());
                if (nativeView.isAttachedToWindow()) {
                    show(nativeView, state, "native-update");
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
                PixelAodLog.log("Pixel peek native surface attached"
                        + " parent=" + parentChain(nativeView)
                        + " hasContent=" + (state.content != null));
                show(nativeView, state, "native-attach");
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
                if (isSettingPreview(nativeView)
                        || !NativeOplusPeekSettingAdapter.isEnabled(nativeView.getContext())) {
                    return;
                }
                State state = STATES.get(nativeView);
                if (state == null || state.content == null || state.overlay.get() == null) {
                    return;
                }
                // Keep this vendor View attached so its animator/end listener still owns the
                // notification window. Suppress only its OPlus full-screen/curved drawing.
                param.setResult(null);
            }, Canvas.class);

            PixelAodLog.log("installed Pixel peek presentation takeover class=" + OPLUS_PEEK_VIEW);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install Pixel peek presentation takeover", t);
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

    private static void show(View nativeView, State state, String source) {
        if (nativeView == null || state == null || state.content == null
                || !NativeOplusPeekSettingAdapter.isEnabled(nativeView.getContext())) {
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
        PixelPeekNotificationView overlay = state.overlay.get();
        removeOverlay(overlay);
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
                    + " iconType=" + smallIcon.getType());
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

    private static final class State {
        PixelPeekNotificationContent content;
        WeakReference<PixelPeekNotificationView> overlay = new WeakReference<>(null);
    }
}
