package dev.codex.pixelaod;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Visually synchronizes the system-owned UDFPS glyph with the vendor AOD reveal.
 *
 * <p>OPlus renders the fingerprint glyph in an independent window. During the black-mask/AOD
 * setup transition that window can commit before the AOD surface becomes visible. This adapter
 * changes only the ImageView alpha while the native AOD reveal is in progress. It never changes
 * fingerprint visibility state, sensing, touch routing, HBM/local-HBM, wake locks, or panel
 * requests. A real fingerprint touch immediately releases the visual synchronization.</p>
 */
final class AodFingerprintRevealSyncController {
    private static final String AOD_CLOCK_LAYOUT =
            "com.oplus.systemui.aod.aodclock.off.AodClockLayout";
    private static final String FINGERPRINT_ICON =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon";
    private static final String FINGERPRINT_UI_MECH =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech";
    private static final String AOD_MASK_ID_NAME = "workshop_aod_anim_mock";
    private static final String SYSTEMUI_PACKAGE = "com.android.systemui";

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Map<ImageView, SyncState> ACTIVE =
            Collections.synchronizedMap(new WeakHashMap<ImageView, SyncState>());

    private static volatile WeakReference<View> lastAodClockLayout = new WeakReference<>(null);
    private static volatile WeakReference<ImageView> lastFingerprintIcon = new WeakReference<>(null);

    private AodFingerprintRevealSyncController() {
    }

    static void install(ClassLoader classLoader) {
        if (classLoader == null || !INSTALLED.compareAndSet(false, true)) {
            return;
        }
        int hooks = 0;
        hooks += hookAodClockLayout(classLoader);
        hooks += hookFingerprintIcon(classLoader);
        hooks += hookFingerprintUiMech(classLoader);
        PixelAodLog.i("installed AOD/system-UDFPS reveal synchronization hooks=" + hooks);
    }

    private static int hookAodClockLayout(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(AOD_CLOCK_LAYOUT, classLoader);
            int hooks = 0;
            ModernHookBridge.hookAfter(clazz, "onAttachedToWindow", param -> {
                rememberAodClockLayout(param.thisObject);
            });
            hooks++;
            ModernHookBridge.hookAfter(clazz, "showClock", param -> {
                rememberAodClockLayout(param.thisObject);
                synchronizeLastIcon("AodClockLayout#showClock");
            }, int.class);
            hooks++;
            ModernHookBridge.hookAfter(clazz, "setVisibleWithSetupAnimate", param -> {
                rememberAodClockLayout(param.thisObject);
                synchronizeLastIcon("AodClockLayout#setVisibleWithSetupAnimate");
            });
            hooks++;
            return hooks;
        } catch (Throwable t) {
            PixelAodLog.log("failed AOD reveal synchronization clock hooks", t);
            return 0;
        }
    }

    private static int hookFingerprintIcon(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(FINGERPRINT_ICON, classLoader);
            int hooks = 0;
            ModernHookBridge.hookAfter(clazz, "setVisibility", param -> {
                if (!(param.thisObject instanceof ImageView)) {
                    return;
                }
                ImageView icon = (ImageView) param.thisObject;
                rememberFingerprintIcon(icon);
                int visibility = intArg(param.args, 0, icon.getVisibility());
                if (visibility == View.VISIBLE) {
                    synchronize(icon, "OnScreenFingerprintIcon#setVisibility");
                } else {
                    cancel(icon, false, "OnScreenFingerprintIcon#setVisibility-hidden");
                }
            }, int.class);
            hooks++;
            ModernHookBridge.hookAfter(clazz, "setBrightnessAlpha", param -> {
                if (param.thisObject instanceof ImageView) {
                    ImageView icon = (ImageView) param.thisObject;
                    rememberFingerprintIcon(icon);
                    synchronize(icon, "OnScreenFingerprintIcon#setBrightnessAlpha");
                }
            }, float.class);
            hooks++;
            ModernHookBridge.hookAfter(clazz, "setMaxBrightnessToAlpha", param -> {
                if (param.thisObject instanceof ImageView) {
                    ImageView icon = (ImageView) param.thisObject;
                    rememberFingerprintIcon(icon);
                    synchronize(icon, "OnScreenFingerprintIcon#setMaxBrightnessToAlpha");
                }
            }, float.class);
            hooks++;
            return hooks;
        } catch (Throwable t) {
            PixelAodLog.log("failed AOD reveal synchronization fingerprint-icon hooks", t);
            return 0;
        }
    }

    private static int hookFingerprintUiMech(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(FINGERPRINT_UI_MECH, classLoader);
            int hooks = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                String name = method.getName();
                if ("onFpTouch".equals(name) && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    ModernHookBridge.hookBefore(method, param -> {
                        if (booleanArg(param.args, 0)) {
                            releaseForInteraction(param.thisObject, "OnScreenFingerprintUiMech#onFpTouch");
                        }
                    });
                    hooks++;
                } else if ("setTouchDownNow".equals(name) && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    ModernHookBridge.hookBefore(method, param -> {
                        if (booleanArg(param.args, 0)) {
                            releaseForInteraction(param.thisObject,
                                    "OnScreenFingerprintUiMech#setTouchDownNow");
                        }
                    });
                    hooks++;
                } else if ("updateFpIconAlpha".equals(name)) {
                    method.setAccessible(true);
                    ModernHookBridge.hookAfter(method, param -> {
                        ImageView icon = fingerprintIconFromUiMech(param.thisObject);
                        if (icon != null) {
                            rememberFingerprintIcon(icon);
                            synchronize(icon, "OnScreenFingerprintUiMech#updateFpIconAlpha");
                        }
                    });
                    hooks++;
                } else if ("onDreamingStopped".equals(name) && method.getParameterCount() == 0) {
                    method.setAccessible(true);
                    ModernHookBridge.hookAfter(method, param -> {
                        ImageView icon = fingerprintIconFromUiMech(param.thisObject);
                        if (icon != null) {
                            cancel(icon, true, "OnScreenFingerprintUiMech#onDreamingStopped");
                        }
                    });
                    hooks++;
                }
            }
            return hooks;
        } catch (Throwable t) {
            PixelAodLog.log("failed AOD reveal synchronization UiMech hooks", t);
            return 0;
        }
    }

    private static void rememberAodClockLayout(Object candidate) {
        if (candidate instanceof View) {
            lastAodClockLayout = new WeakReference<>((View) candidate);
        }
    }

    private static void rememberFingerprintIcon(ImageView icon) {
        if (icon != null) {
            lastFingerprintIcon = new WeakReference<>(icon);
        }
    }

    private static void synchronizeLastIcon(String source) {
        ImageView icon = lastFingerprintIcon.get();
        if (icon != null && icon.getVisibility() == View.VISIBLE) {
            synchronize(icon, source);
        }
    }

    private static void synchronize(ImageView icon, String source) {
        if (icon == null || icon.getVisibility() != View.VISIBLE) {
            return;
        }
        View aodLayout = lastAodClockLayout.get();
        Context context = icon.getContext();
        boolean moduleEnabled = context != null && PixelAodSettings.getBoolean(
                context, PixelAodSettings.KEY_MODULE_ENABLED, true);
        boolean customAodEnabled = context != null && PixelAodSettings.getBoolean(
                context, PixelAodSettings.KEY_CUSTOM_AOD, true);
        boolean replacementRequested = context != null
                && PixelAodUdfpsRuntimePolicy.replacementRequested(context);
        boolean interactive = context == null || PixelAodClockView.isDeviceInteractive(context);
        boolean nativeAodShowing = isVendorAodShowing(aodLayout);
        boolean attached = aodLayout != null && aodLayout.isAttachedToWindow();
        if (!AodFingerprintRevealSyncPolicy.shouldSynchronize(moduleEnabled, customAodEnabled,
                replacementRequested, interactive, nativeAodShowing, attached)) {
            cancel(icon, true, source + "#ineligible");
            return;
        }

        float progress = revealProgress(aodLayout);
        if (AodFingerprintRevealSyncPolicy.isComplete(progress)) {
            complete(icon, source, progress);
            return;
        }

        SyncState state;
        boolean started = false;
        synchronized (ACTIVE) {
            state = ACTIVE.get(icon);
            if (state == null) {
                state = new SyncState(icon, source);
                ACTIVE.put(icon, state);
                started = true;
            }
        }
        applySynchronizedAlpha(icon, progress);
        if (started) {
            PixelAodLog.i("system UDFPS AOD reveal synchronization started source=" + source
                    + " progress=" + progress
                    + " aodAlpha=" + safeAlpha(aodLayout)
                    + " mask=" + describeMask(aodLayout));
            icon.postOnAnimation(state.runnable);
        }
    }

    private static void runFrame(ImageView icon) {
        if (icon == null) {
            return;
        }
        SyncState state;
        synchronized (ACTIVE) {
            state = ACTIVE.get(icon);
        }
        if (state == null) {
            return;
        }
        if (icon.getVisibility() != View.VISIBLE) {
            cancel(icon, false, state.source + "#icon-hidden");
            return;
        }
        synchronize(icon, state.source + "#frame");
        synchronized (ACTIVE) {
            if (ACTIVE.get(icon) == state) {
                icon.postOnAnimation(state.runnable);
            }
        }
    }

    private static void complete(ImageView icon, String source, float progress) {
        SyncState removed;
        synchronized (ACTIVE) {
            removed = ACTIVE.remove(icon);
        }
        if (removed == null) {
            return;
        }
        icon.removeCallbacks(removed.runnable);
        restoreNativeAlpha(icon);
        PixelAodLog.i("system UDFPS AOD reveal synchronization completed source=" + source
                + " progress=" + progress
                + " durationMs=" + (android.os.SystemClock.uptimeMillis() - removed.startedAt));
    }

    private static void cancel(ImageView icon, boolean restore, String source) {
        if (icon == null) {
            return;
        }
        SyncState removed;
        synchronized (ACTIVE) {
            removed = ACTIVE.remove(icon);
        }
        if (removed == null) {
            return;
        }
        icon.removeCallbacks(removed.runnable);
        if (restore && icon.getVisibility() == View.VISIBLE) {
            restoreNativeAlpha(icon);
        }
        PixelAodLog.i("system UDFPS AOD reveal synchronization cancelled source=" + source
                + " durationMs=" + (android.os.SystemClock.uptimeMillis() - removed.startedAt));
    }

    private static void releaseForInteraction(Object uiMech, String source) {
        ImageView icon = fingerprintIconFromUiMech(uiMech);
        if (icon == null) {
            icon = lastFingerprintIcon.get();
        }
        cancel(icon, true, source + "#finger-touch");
    }

    private static void applySynchronizedAlpha(ImageView icon, float progress) {
        float nativeAlpha = nativeBrightnessAlpha(icon);
        float target = AodFingerprintRevealSyncPolicy.synchronizedIconAlpha(nativeAlpha, progress);
        if (Math.abs(icon.getAlpha() - target) > 0.002f) {
            icon.setAlpha(target);
        }
    }

    private static void restoreNativeAlpha(ImageView icon) {
        float target = nativeBrightnessAlpha(icon);
        if (Math.abs(icon.getAlpha() - target) > 0.002f) {
            icon.setAlpha(target);
        }
    }

    private static float revealProgress(View aodLayout) {
        if (aodLayout == null) {
            return 1f;
        }
        View mask = findAodMask(aodLayout);
        boolean maskShown = mask != null && mask.isShown() && mask.getVisibility() == View.VISIBLE;
        boolean layoutShown = aodLayout.isShown() && aodLayout.getVisibility() == View.VISIBLE;
        return AodFingerprintRevealSyncPolicy.revealProgress(layoutShown, safeAlpha(aodLayout),
                maskShown, safeAlpha(mask));
    }

    private static View findAodMask(View aodLayout) {
        if (aodLayout == null) {
            return null;
        }
        try {
            Context context = aodLayout.getContext();
            int id = context.getResources().getIdentifier(
                    AOD_MASK_ID_NAME, "id", SYSTEMUI_PACKAGE);
            if (id == 0) {
                return null;
            }
            View root = aodLayout.getRootView();
            return root != null ? root.findViewById(id) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isVendorAodShowing(View aodLayout) {
        if (aodLayout == null) {
            return false;
        }
        try {
            Object data = ModernHookBridge.getObjectField(aodLayout, "mAodData");
            Object value = ModernHookBridge.getObjectField(data, "mAodIsInShow");
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static float nativeBrightnessAlpha(ImageView icon) {
        try {
            Object value = ModernHookBridge.callMethod(icon, "getBrightnessAlpha");
            if (value instanceof Number) {
                return Math.max(0f, Math.min(1f, ((Number) value).floatValue()));
            }
        } catch (Throwable ignored) {
        }
        return Math.max(0f, Math.min(1f, icon.getAlpha()));
    }

    private static ImageView fingerprintIconFromUiMech(Object uiMech) {
        try {
            Object icon = ModernHookBridge.getObjectField(uiMech, "fpIcon");
            return icon instanceof ImageView ? (ImageView) icon : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean booleanArg(Object[] args, int index) {
        return args != null && index >= 0 && index < args.length
                && args[index] instanceof Boolean && (Boolean) args[index];
    }

    private static int intArg(Object[] args, int index, int fallback) {
        return args != null && index >= 0 && index < args.length && args[index] instanceof Number
                ? ((Number) args[index]).intValue() : fallback;
    }

    private static float safeAlpha(View view) {
        return view != null ? view.getAlpha() : 0f;
    }

    private static String describeMask(View aodLayout) {
        View mask = findAodMask(aodLayout);
        if (mask == null) {
            return "none";
        }
        return "shown=" + mask.isShown()
                + ",visibility=" + mask.getVisibility()
                + ",alpha=" + mask.getAlpha();
    }

    private static final class SyncState {
        final long startedAt = android.os.SystemClock.uptimeMillis();
        final String source;
        final Runnable runnable;

        SyncState(ImageView icon, String source) {
            this.source = source != null ? source : "unknown";
            WeakReference<ImageView> weakIcon = new WeakReference<>(icon);
            this.runnable = () -> runFrame(weakIcon.get());
        }
    }
}
