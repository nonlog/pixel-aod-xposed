package dev.codex.pixelaod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Application;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.SurfaceControl;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Independent COUI Expressive 2.5 UDFPS port.
 *
 * <p>This class deliberately owns only the replacement glyph and its COUI press/success visual
 * effects. OPlus still owns the carrier view's visibility, HBM/highlight, optical sensing and
 * power lifecycle. The legacy Pixel controller is never installed by this path.</p>
 */
final class CouiUdfpsController {
    private static final String UI_MECH_CLASS =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech";
    private static final String AUTH_CONTROLLER_CLASS =
            "com.oplus.systemui.biometrics.OplusBiometricAuthController";
    private static final String UPDATE_MONITOR_CALLBACK_CLASS =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateMonitorCallback$1";
    private static final String BIOMETRIC_UNLOCK_CLASS =
            "com.oplus.systemui.statusbar.phone.OplusBiometricUnlockControllerExImpl";
    private static final String AUTH_RIPPLE_CONTROLLER_CLASS =
            "com.android.systemui.biometrics.AuthRippleController";
    private static final String AUTH_RIPPLE_VIEW_CLASS =
            "com.android.systemui.biometrics.AuthRippleView";
    private static final String FINGERPRINT_UTILS_CLASS =
            "com.oplus.systemui.biometrics.finger.KeyguardFingerprintUtils";
    private static final String PRESSED_ICON_CLASS =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintPressedIcon";
    private static final String[] ASYNC_RUNNABLE_CLASSES = {
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$1",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$fpIconShow$2",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$restoreIconDrawable$1",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$touchEvent$2",
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateFpColor$1"
    };
    // Exact COUI Expressive 2.5 state-refresh surface for OnScreenFingerprintUiMech.
    private static final String[] VISUAL_METHODS = {
            "loadAnimDrawables",
            "restoreIconDrawable",
            "restoreIconDrawableDark",
            "updateFpIconColor",
            "updateFpColor",
            "updateFpIconState",
            "fpIconShow",
            "setVisibilityInAOD",
            "notifyShowAodIcon",
            "notifyHideAodIcon",
            "setOnDozeState",
            "setOnDreamingStart",
            "onDreamingStart",
            "onDreamingStopped",
            "onScreenTurnedOff",
            "onScreenTurnedOn",
            "startToAnimInDream",
            "onFpTouch",
            "setTouchDownNow",
            "stopOpticalAnimation",
            "stopPressedAnimation"
    };
    private static final String[] SHOW_METHODS = {
            "showUdfpsOverlay",
            "fpIconShow",
            "notifyShowAodIcon",
            "showFingerprintIconTemporarily",
            "setFpIconVisibilityInAOD",
            "setVisibilityInAOD",
            "showOrHideFingerprintIconTemporarily"
    };
    private static final String[] HIDE_METHODS = {
            "hideUdfpsOverlay",
            "fpIconHide",
            "notifyHideAodIcon",
            "hideFingerprintIcon",
            "hideFingerprintIconTemporarily",
            "setFpIconVisibilityInAOD",
            "setFingerprintIconShow",
            "setVisibilityInAOD",
            "showOrHideFingerprintIconTemporarily"
    };
    private static final long[] DISCOVERY_RETRY_DELAYS_MS = {
            0L, 16L, 48L, 120L, 250L, 500L, 1_000L
    };

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Map<ImageView, Drawable> ORIGINAL_DRAWABLES = new WeakHashMap<>();
    private static final Map<ImageView, Drawable> ORIGINAL_BACKGROUNDS = new WeakHashMap<>();
    private static final Map<ImageView, ColorStateList> ORIGINAL_TINTS = new WeakHashMap<>();
    private static final Map<ImageView, ColorFilter> ORIGINAL_FILTERS = new WeakHashMap<>();
    private static final Map<ImageView, ImageView.ScaleType> ORIGINAL_SCALE_TYPES =
            new WeakHashMap<>();
    private static final Map<ImageView, Float> ORIGINAL_SCALE_X = new WeakHashMap<>();
    private static final Map<ImageView, Float> ORIGINAL_SCALE_Y = new WeakHashMap<>();
    private static final Map<ImageView, CouiUdfpsDrawable> DRAWABLES = new WeakHashMap<>();
    private static final Map<ImageView, Drawable> ORIGINAL_PRESSED_DRAWABLES =
            new WeakHashMap<>();
    private static final Map<ImageView, Drawable> ORIGINAL_PRESSED_BACKGROUNDS =
            new WeakHashMap<>();
    private static final Map<ImageView, ColorStateList> ORIGINAL_PRESSED_TINTS =
            new WeakHashMap<>();
    private static final Map<ImageView, ColorFilter> ORIGINAL_PRESSED_FILTERS =
            new WeakHashMap<>();
    private static final Map<ImageView, Float> ORIGINAL_PRESSED_ALPHAS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> PRESSED_TOUCH_STATES = new WeakHashMap<>();
    private static final Map<Object, CouiUdfpsStateMachine> STATES = new WeakHashMap<>();
    private static final Map<Object, String> LAST_STATE_LOGS = new WeakHashMap<>();
    private static final Map<Object, PendingRefresh> PENDING_REFRESHES = new WeakHashMap<>();
    private static final Map<Object, Integer> DISCOVERY_ATTEMPTS = new WeakHashMap<>();
    private static final Map<Object, Runnable> DISCOVERY_RUNNABLES = new WeakHashMap<>();
    private static final Map<ImageView, ValueAnimator> EXIT_ANIMATORS = new WeakHashMap<>();
    private static final Map<ImageView, CouiUdfpsGlowOverlay> GLOW_OVERLAYS = new WeakHashMap<>();
    private static final Map<ImageView, View.OnAttachStateChangeListener> HDR_ATTACH_LISTENERS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> LAST_HDR_PRESS_STATES = new WeakHashMap<>();
    private static final Map<Object, String> LAST_PRESSED_CARRIER_LOGS = new WeakHashMap<>();
    private static volatile WeakReference<Object> lastUiMech = new WeakReference<>(null);

    private CouiUdfpsController() {
    }

    static void install(ClassLoader classLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        int hooked = 0;
        hooked += hookVisualClass(classLoader, UI_MECH_CLASS, true);
        hooked += hookPressedIconClass(classLoader);
        hooked += hookPressedIconMutations(classLoader);
        hooked += hookPressedAnimationDecisions(classLoader);
        hooked += hookAsyncClasses(classLoader);
        hooked += hookAuthenticationCallbacks(classLoader, UPDATE_MONITOR_CALLBACK_CLASS);
        hooked += hookAuthenticationCallbacks(classLoader, BIOMETRIC_UNLOCK_CLASS);
        hooked += hookStockRipple(classLoader, AUTH_RIPPLE_CONTROLLER_CLASS, "showUnlockedRipple");
        hooked += hookStockRipple(classLoader, AUTH_RIPPLE_VIEW_CLASS, "startDwellRipple");
        hooked += hookConfigurationChanges();
        PixelAodLog.i("installed independent COUI UDFPS hooks count=" + hooked
                + " renderer=" + PixelAodFeatureFlags.startupUdfpsRenderer());
    }

    private static int hookPressedIconClass(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(PRESSED_ICON_CLASS, classLoader);
            int count = 0;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                final Constructor<?> target = constructor;
                target.setAccessible(true);
                ModernHookBridge.hookAfter(target, param -> {
                    if (!(param.thisObject instanceof ImageView)) {
                        return;
                    }
                    ImageView pressedIcon = (ImageView) param.thisObject;
                    Context context = pressedIcon.getContext();
                    if (!isReplacementEnabled(context)) {
                        return;
                    }
                    dispatchViewOperation(pressedIcon,
                            "OnScreenFingerprintPressedIcon#constructor", () -> {
                                configurePressedIcon(pressedIcon,
                                        isHdrPressEffectEnabled(context));
                                PixelAodLog.log("COUI UDFPS pressed icon configured"
                                        + " source=OnScreenFingerprintPressedIcon#constructor"
                                        + " iconId=" + Integer.toHexString(
                                        System.identityHashCode(pressedIcon)));
                            });
                });
                count++;
            }
            PixelAodLog.log("hooked COUI UDFPS pressed icon class=" + PRESSED_ICON_CLASS
                    + " constructors=" + count);
            return count;
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("COUI UDFPS pressed icon class not found class="
                    + PRESSED_ICON_CLASS);
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook COUI UDFPS pressed icon class="
                    + PRESSED_ICON_CLASS, throwable);
        }
        return 0;
    }

    /**
     * Stable 0.1.331 behavior: vendor pressed-view mutations may re-expose the optical carrier
     * after our refresh, so re-assert the touch-owned alpha on that vendor class only. Do not hook
     * View.setAlpha globally and do not intercept optical/HBM methods.
     */
    private static int hookPressedIconMutations(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(PRESSED_ICON_CLASS, classLoader);
            int count = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isPressedCarrierMutation(method)) {
                    continue;
                }
                final Method target = method;
                target.setAccessible(true);
                ModernHookBridge.hookAfter(target, param -> {
                    if (param.thisObject instanceof ImageView) {
                        ImageView pressedIcon = (ImageView) param.thisObject;
                        reassertPressedCarrierVisibility(pressedIcon,
                                "OnScreenFingerprintPressedIcon#" + target.getName());
                    }
                });
                count++;
            }
            PixelAodLog.log("hooked stable UDFPS pressed carrier mutations class="
                    + PRESSED_ICON_CLASS + " methods=" + count);
            return count;
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook stable UDFPS pressed carrier mutations", throwable);
            return 0;
        }
    }

    private static boolean isPressedCarrierMutation(Method method) {
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

    /** COUI 2.5 hooks these decisions on OnScreenFingerprintUiMech, not pressedIcon. */
    private static int hookPressedAnimationDecisions(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(UI_MECH_CLASS, classLoader);
            int count = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                final Method target = method;
                if ("checkHasPressedAnimation".equals(target.getName())
                        && (target.getReturnType() == boolean.class
                        || target.getReturnType() == Boolean.class)) {
                    target.setAccessible(true);
                    ModernHookBridge.hookBefore(target, param -> {
                        Context context = contextFrom(null, param.thisObject);
                        boolean hdrEnabled = isHdrPressEffectEnabled(context);
                        if (isReplacementEnabled(context)
                                && CouiUdfpsPressedVisualPolicy
                                .suppressVendorPressedAnimation(hdrEnabled)) {
                            param.setResult(Boolean.FALSE);
                        }
                    });
                    count++;
                } else if ("getScalePressedAnim".equals(target.getName())
                        && (target.getReturnType() == float.class
                        || target.getReturnType() == Float.class)) {
                    target.setAccessible(true);
                    ModernHookBridge.hookBefore(target, param -> {
                        Context context = contextFrom(null, param.thisObject);
                        boolean hdrEnabled = isHdrPressEffectEnabled(context);
                        if (isReplacementEnabled(context)
                                && CouiUdfpsPressedVisualPolicy
                                .suppressVendorPressedAnimation(hdrEnabled)) {
                            param.setResult(Float.valueOf(1f));
                        }
                    });
                    count++;
                }
            }
            PixelAodLog.log("hooked COUI UDFPS press decisions class=" + UI_MECH_CLASS
                    + " methods=" + count);
            return count;
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook COUI UDFPS press decisions", throwable);
            return 0;
        }
    }

    static void refreshLast(Context context, String source) {
        Object uiMech = lastUiMech.get();
        if (uiMech != null) {
            requestVisualState(context, uiMech, source, false, true);
        }
    }

    static Object lastUiMech() {
        return lastUiMech.get();
    }

    private static int hookVisualClass(ClassLoader classLoader, String className,
            boolean uiMechClass) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            int count = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!contains(VISUAL_METHODS, method.getName())
                        || Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                final Method target = method;
                final String source = simpleClassName(className) + "#" + signature(method);
                target.setAccessible(true);
                ModernHookBridge.hookAfter(target, param -> {
                    Object uiMech = uiMechClass ? param.thisObject : lastUiMech.get();
                    if (uiMech == null) {
                        return;
                    }
                    rememberUiMech(uiMech);
                    handleVisualCallback(contextFrom(param, uiMech), uiMech,
                            target.getName(), param.args, source);
                });
                count++;
            }
            PixelAodLog.log("hooked COUI UDFPS visual class=" + className
                    + " methods=" + count);
            return count;
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("COUI UDFPS visual class not found class=" + className);
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook COUI UDFPS visual class=" + className, throwable);
        }
        return 0;
    }

    private static int hookConfigurationChanges() {
        try {
            ModernHookBridge.hookAfter(Application.class, "onConfigurationChanged",
                    param -> refreshLast(null, "Application#onConfigurationChanged"),
                    Configuration.class);
            PixelAodLog.log("hooked COUI UDFPS configuration refresh");
            return 1;
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook COUI UDFPS configuration refresh", throwable);
            return 0;
        }
    }

    private static int hookAsyncClasses(ClassLoader classLoader) {
        int count = 0;
        for (String className : ASYNC_RUNNABLE_CLASSES) {
            try {
                Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!"run".equals(method.getName()) || method.getParameterCount() != 0
                            || Modifier.isAbstract(method.getModifiers())) {
                        continue;
                    }
                    final Method target = method;
                    final String source = className + "#run()";
                    target.setAccessible(true);
                    ModernHookBridge.hookBefore(target, param -> {
                    Object uiMech = readObjectField(param.thisObject, "this$0");
                        if (uiMech == null) {
                            return;
                        }
                        rememberUiMech(uiMech);
                        if (isAodExitRunnable(className, param.thisObject)
                                && isAodExitAnimationEnabled(contextFrom(null, uiMech))
                                && isAodExitPending(uiMech)
                                && startCustomAodExit(uiMech)) {
                            param.setResult(defaultReturnValue(target.getReturnType()));
                            PixelAodLog.i("COUI UDFPS intercepted native AOD exit runnable source="
                                    + source);
                        }
                    });
                    ModernHookBridge.hookAfter(target, param -> {
                        Object uiMech = readObjectField(param.thisObject, "this$0");
                        if (uiMech != null) {
                            rememberUiMech(uiMech);
                            requestVisualState(contextFrom(null, uiMech), uiMech,
                                    source, false, true);
                        }
                    });
                    count++;
                }
            } catch (ClassNotFoundException ignored) {
                PixelAodLog.log("COUI UDFPS async class not found class=" + className);
            } catch (Throwable throwable) {
                PixelAodLog.log("failed to hook COUI UDFPS async class=" + className, throwable);
            }
        }
        return count;
    }

    private static int hookAuthenticationCallbacks(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            int count = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                String name = method.getName();
                if ("onBiometricAuthenticated".equals(name)) {
                    final Method target = method;
                    target.setAccessible(true);
                    ModernHookBridge.hookBefore(target, param -> {
                        if (isFingerprintSource(param.args)) {
                            Object callbackOuter = UPDATE_MONITOR_CALLBACK_CLASS.equals(className)
                                    ? readObjectField(param.thisObject, "this$0") : null;
                            Object uiMech = CouiUdfpsStateMachine.resolveAuthenticationUiMech(
                                    className, callbackOuter, lastUiMech.get());
                            showSuccess(uiMech, "onBiometricAuthenticated");
                        }
                    });
                    count++;
                } else if ("onBiometricAuthFailed".equals(name)
                        || "onBiometricError".equals(name)) {
                    final Method target = method;
                    target.setAccessible(true);
                    ModernHookBridge.hookAfter(target, param -> {
                        if (isFingerprintSource(param.args)) {
                            Object callbackOuter = UPDATE_MONITOR_CALLBACK_CLASS.equals(className)
                                    ? readObjectField(param.thisObject, "this$0") : null;
                            Object uiMech = CouiUdfpsStateMachine.resolveAuthenticationUiMech(
                                    className, callbackOuter, lastUiMech.get());
                            stopPress(uiMech, target.getName());
                        }
                    });
                    count++;
                }
            }
            PixelAodLog.log("hooked COUI UDFPS authentication class=" + className
                    + " methods=" + count);
            return count;
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("COUI UDFPS authentication class not found class=" + className);
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook COUI UDFPS authentication class=" + className,
                    throwable);
        }
        return 0;
    }

    private static int hookStockRipple(ClassLoader classLoader, String className,
            String methodName) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            int count = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!methodName.equals(method.getName()) || Modifier.isAbstract(method.getModifiers())) {
                    continue;
                }
                final Method target = method;
                target.setAccessible(true);
                ModernHookBridge.hookBefore(target, param -> {
                    Context context = contextFrom(param, lastUiMech.get());
                    if (isReplacementEnabled(context) && isSuccessRippleEnabled(context)) {
                        param.setResult(defaultReturnValue(target.getReturnType()));
                        PixelAodLog.log("suppressed stock UDFPS ripple method=" + methodName);
                    }
                });
                count++;
            }
            return count;
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("stock UDFPS ripple class not found class=" + className);
        } catch (Throwable throwable) {
            PixelAodLog.log("failed to hook stock UDFPS ripple class=" + className, throwable);
        }
        return 0;
    }

    /**
     * COUI 2.5 treats every UiMech callback as a request to re-read the live vendor fields. It
     * does not maintain a second SHOW/HIDE/TOUCH lifecycle beside OPlus.
     */
    private static void handleVisualCallback(Context context, Object uiMech, String methodName,
            Object[] args, String source) {
        if (!isReplacementEnabled(context)) {
            return;
        }
        if ("onFpTouch".equals(methodName) || "setTouchDownNow".equals(methodName)) {
            Boolean fingerDown = firstBoolean(args);
            if (fingerDown != null) {
                PixelAodLog.i("COUI UDFPS touch signal=" + methodName + " fingerDown=" + fingerDown
                        + " source=" + source);
                setPressedTouchState(uiMech, fingerDown,
                        source + "#stable-pressed-touch");
                if (fingerDown) {
                    PixelAodHook.clearFodNativeTimeoutHide("COUI UDFPS explicit touch");
                }
            }
        }
        boolean force = "loadAnimDrawables".equals(methodName)
                || "updateFpIconColor".equals(methodName)
                || "updateFpColor".equals(methodName);
        requestVisualState(context, uiMech, source, true, force);
    }

    private static boolean showInvocationIsVisible(String methodName, Object[] args) {
        if ("setFpIconVisibilityInAOD".equals(methodName)
                || "setFingerprintIconShow".equals(methodName)
                || "showOrHideFingerprintIconTemporarily".equals(methodName)
                || "setVisibilityInAOD".equals(methodName)) {
            Boolean value = visibilityArgument(args);
            return value == null || value;
        }
        return true;
    }

    private static boolean hideInvocationIsVisible(String methodName, Object[] args) {
        if ("showOrHideFingerprintIconTemporarily".equals(methodName)
                || "setFpIconVisibilityInAOD".equals(methodName)
                || "setFingerprintIconShow".equals(methodName)
                || "setVisibilityInAOD".equals(methodName)) {
            Boolean value = visibilityArgument(args);
            return value == null || !value;
        }
        return true;
    }

    private static boolean isShowMethod(String name) {
        return contains(SHOW_METHODS, name);
    }

    private static boolean isHideMethod(String name) {
        return contains(HIDE_METHODS, name);
    }

    private static boolean isAodExitRunnable(String className, Object runnable) {
        if (!className.endsWith("OnScreenFingerprintUiMech$1")) {
            return false;
        }
        Object classId = readObjectField(runnable, "$r8$classId");
        return classId instanceof Number && ((Number) classId).intValue() == 22;
    }

    private static boolean isAodExitPending(Object uiMech) {
        return uiMech != null && isAod(uiMech)
                && readObjectField(uiMech, "realHideRunnable") != null;
    }

    private static boolean isAod(Object uiMech) {
        return readBooleanField(uiMech, "onDozeState")
                || readBooleanField(uiMech, "onDreamingStart")
                || readBooleanField(uiMech, "screenTurnedOff");
    }

    private static void requestVisualState(Context context, Object uiMech, String source,
            boolean animate, boolean force) {
        if (uiMech == null) {
            return;
        }
        rememberUiMech(uiMech);
        PendingRefresh pending;
        boolean schedule;
        synchronized (PENDING_REFRESHES) {
            pending = PENDING_REFRESHES.get(uiMech);
            if (pending == null) {
                pending = new PendingRefresh(uiMech, context, source, animate, force);
                PENDING_REFRESHES.put(uiMech, pending);
                schedule = true;
            } else {
                if (context != null) {
                    pending.context = context;
                }
                pending.source = source;
                pending.animate |= animate;
                pending.force |= force;
                schedule = false;
            }
        }
        if (!schedule) {
            return;
        }
        final PendingRefresh request = pending;
        request.runnable = () -> {
            synchronized (PENDING_REFRESHES) {
                if (PENDING_REFRESHES.get(uiMech) == request) {
                    PENDING_REFRESHES.remove(uiMech);
                }
            }
            ImageView icon = findFingerprintIcon(uiMech);
            if (icon != null) {
                Runnable apply = () -> applyVisualState(request.context, uiMech,
                        request.source, request.animate, request.force, false);
                if (icon.getHandler() != null
                        && Looper.myLooper() == icon.getHandler().getLooper()) {
                    apply.run();
                } else {
                    icon.post(apply);
                }
                cancelDiscovery(uiMech);
            } else {
                scheduleDiscovery(request.context, uiMech, request.source,
                        request.animate, request.force);
            }
        };
        // Match COUI 2.5: once the fingerprint carrier exists, coalesce state refreshes at the
        // next frame boundary rather than draining multiple MAIN.post() callbacks in one frame.
        // This is especially important during optical wake/auth where several UiMech and async
        // callbacks arrive back-to-back before the success ripple starts.
        ImageView frameOwner = findFingerprintIcon(uiMech);
        if (frameOwner != null && frameOwner.isAttachedToWindow()) {
            frameOwner.postOnAnimation(request.runnable);
        } else {
            MAIN.post(request.runnable);
        }
    }

    private static void applyVisualState(Context context, Object uiMech, String source,
            boolean animate, boolean force, boolean allowAodExitPending) {
        ImageView icon = findFingerprintIcon(uiMech);
        if (icon == null) {
            PixelAodLog.log("COUI UDFPS visual refresh skipped source=" + source
                    + " reason=primary-icon-unavailable");
            return;
        }
        if (!allowAodExitPending
                && (isAodExitPending(uiMech) || isCustomAodExitRunning(icon))) {
            PixelAodLog.log("COUI UDFPS visual refresh skipped source=" + source
                    + " reason=aod-exit-pending");
            return;
        }
        Context resolved = context != null ? context : icon.getContext();
        if (!isReplacementEnabled(resolved)) {
            ImageView pressedIcon = findPressedIcon(uiMech);
            dispatchViewOperation(pressedIcon, source + "#disabled", () -> {
                updatePressedHdr(pressedIcon, false);
                restorePressed(pressedIcon, source + "#pressed-disabled");
            });
            restoreOriginal(icon, source + "#disabled");
            hidePress(uiMech, source + "#disabled");
            return;
        }
        boolean liveTouchDown = readBooleanField(uiMech, "isTouchDownNow");
        boolean aod = isAod(uiMech);
        normalizeIcon(icon, aod);
        CouiUdfpsDrawable drawable = couiDrawable(icon, resolved, aod);
        cancelCustomAodExit(icon);
        drawable.setAlpha(255);
        boolean transitionAnimate = animate && !(force && drawable.isOutlineOnly() == aod);
        drawable.transitionTo(aod, isDark(resolved), transitionAnimate, force);
        boolean hdrEnabled = isHdrPressEffectEnabled(resolved);
        drawable.setPressed(CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(
                liveTouchDown, hdrEnabled));
        if (icon.getDrawable() != drawable) {
            icon.setImageDrawable(drawable);
        }
        ImageView pressedIcon = findPressedIcon(uiMech);
        dispatchViewOperation(pressedIcon, source + "#pressed", () -> {
            configurePressedIcon(pressedIcon, hdrEnabled);
            synchronized (PRESSED_TOUCH_STATES) {
                PRESSED_TOUCH_STATES.put(pressedIcon, liveTouchDown);
            }
            applyPressedCarrierVisibility(pressedIcon, liveTouchDown,
                    source + "#stable-pressed");
            logPressedCarrierIfChanged(uiMech, pressedIcon, liveTouchDown, hdrEnabled, source);
        });
        updatePressEffect(uiMech, icon, liveTouchDown, hdrEnabled, source);
        logLiveStateIfChanged(uiMech, aod, liveTouchDown, source, icon);
    }

    /** Mirrors StockUdfpsIconHook.updatePressGlow: raw vendor touch is the only press source. */
    private static void updatePressEffect(Object uiMech, ImageView icon, boolean liveTouchDown,
            boolean hdrEnabled, String source) {
        if (icon == null) {
            return;
        }
        if (hdrEnabled) {
            // HDR illumination is carried by the vendor pressedIcon window; do not create a
            // second glow or visibility state.
            CouiUdfpsGlowOverlay overlay;
            synchronized (GLOW_OVERLAYS) {
                overlay = GLOW_OVERLAYS.get(icon);
            }
            if (overlay != null) {
                overlay.hidePress();
            }
            return;
        }
        // Stable 0.1.331 optical contract: when module HDR is disabled, the vendor pressed
        // carrier owns illumination/HBM. A module press overlay can alter the light reaching the
        // optical sensor, so keep it absent and only retain the post-auth success ripple path.
        dispatchViewOperation(icon, source + "#press-glow", () -> {
            CouiUdfpsGlowOverlay overlay;
            synchronized (GLOW_OVERLAYS) {
                overlay = GLOW_OVERLAYS.get(icon);
            }
            if (overlay != null) {
                overlay.hidePress();
            }
        });
    }

    private static CouiUdfpsDrawable couiDrawable(ImageView icon, Context context,
            boolean aod) {
        synchronized (DRAWABLES) {
            CouiUdfpsDrawable drawable = DRAWABLES.get(icon);
            if (drawable == null) {
                rememberOriginal(icon);
                drawable = new CouiUdfpsDrawable(context, aod, isDark(context));
                DRAWABLES.put(icon, drawable);
            }
            return drawable;
        }
    }

    private static void normalizeIcon(ImageView icon, boolean aod) {
        rememberOriginal(icon);
        try {
            if (icon.getAnimation() != null) {
                icon.clearAnimation();
            }
            icon.animate().cancel();
            if (icon.getScaleX() != 1f) {
                icon.setScaleX(1f);
            }
            if (icon.getScaleY() != 1f) {
                icon.setScaleY(1f);
            }
            if (icon.getBackground() != null) {
                icon.setBackground(null);
            }
            if (icon.getImageTintList() != null) {
                icon.setImageTintList(null);
            }
            if (icon.getColorFilter() != null) {
                icon.clearColorFilter();
            }
            if (icon.getScaleType() != ImageView.ScaleType.CENTER) {
                icon.setScaleType(ImageView.ScaleType.CENTER);
            }
        } catch (Throwable throwable) {
            PixelAodLog.log("COUI UDFPS icon normalization failed", throwable);
        }
        // Primary carrier alpha/brightness remains OPlus-owned. Only drawable presentation,
        // animation cancellation, and scale normalization above are module-owned.
    }

    private static void configurePressedIcon(ImageView pressedIcon, boolean hdrEnabled) {
        if (pressedIcon == null) {
            return;
        }
        rememberPressedOriginal(pressedIcon);
        if (!CouiUdfpsPressedVisualPolicy.useModulePressedCarrier(hdrEnabled)) {
            // Do not touch drawable/background/tint/animation in native optical mode. Stable
            // 0.1.331 only gates this vendor View's alpha while idle; OPlus retains the pressed
            // asset, HBM and optical animation needed for acquisition.
            if (pressedIcon.getBackground() instanceof CouiUdfpsPressedIlluminationDrawable) {
                restoreNativePressedCarrier(pressedIcon);
            }
            return;
        }
        try {
            if (pressedIcon.getAnimation() != null) {
                pressedIcon.clearAnimation();
            }
            pressedIcon.animate().cancel();
            if (pressedIcon.getScaleX() != 1f) {
                pressedIcon.setScaleX(1f);
            }
            if (pressedIcon.getScaleY() != 1f) {
                pressedIcon.setScaleY(1f);
            }
            if (pressedIcon.getImageTintList() != null) {
                pressedIcon.setImageTintList(null);
            }
            if (pressedIcon.getColorFilter() != null) {
                pressedIcon.clearColorFilter();
            }
            if (pressedIcon.getDrawable() != null) {
                pressedIcon.setImageDrawable(null);
            }
            if (!(pressedIcon.getBackground() instanceof CouiUdfpsPressedIlluminationDrawable)) {
                pressedIcon.setBackground(new CouiUdfpsPressedIlluminationDrawable(
                        pressedIcon.getContext()));
            }
            setPressedIlluminationAlpha(pressedIcon, isPressedTouchActive(pressedIcon));
            prepareHdrWindow(pressedIcon);
        } catch (Throwable throwable) {
            PixelAodLog.log("COUI UDFPS pressed icon configuration failed", throwable);
        }
    }

    private static void restoreNativePressedCarrier(ImageView pressedIcon) {
        Drawable drawable;
        Drawable background;
        ColorStateList tint;
        ColorFilter filter;
        synchronized (ORIGINAL_PRESSED_DRAWABLES) {
            if (!ORIGINAL_PRESSED_DRAWABLES.containsKey(pressedIcon)) {
                return;
            }
            drawable = ORIGINAL_PRESSED_DRAWABLES.get(pressedIcon);
            background = ORIGINAL_PRESSED_BACKGROUNDS.get(pressedIcon);
            tint = ORIGINAL_PRESSED_TINTS.get(pressedIcon);
            filter = ORIGINAL_PRESSED_FILTERS.get(pressedIcon);
        }
        pressedIcon.setImageDrawable(drawable);
        pressedIcon.setBackground(background);
        pressedIcon.setImageTintList(tint);
        if (filter != null) {
            pressedIcon.setColorFilter(filter);
        } else {
            pressedIcon.clearColorFilter();
        }
    }

    private static float maxHdrHeadroom(ImageView pressedIcon) {
        float ratio = 7f;
        try {
            if (pressedIcon != null && pressedIcon.getDisplay() != null) {
                float reported = pressedIcon.getDisplay().getHighestHdrSdrRatio();
                if (Float.isFinite(reported) && reported > 1f) {
                    ratio = reported;
                }
            }
        } catch (Throwable throwable) {
            PixelAodLog.log("COUI UDFPS HDR headroom fallback reason="
                    + throwable.getClass().getSimpleName());
        }
        return Math.min(7f, Math.max(1f, ratio));
    }

    private static void prepareHdrWindow(ImageView pressedIcon) {
        if (pressedIcon == null) {
            return;
        }
        synchronized (HDR_ATTACH_LISTENERS) {
            // Match COUI 2.5: HDR window preparation is a one-time per pressed carrier setup.
            // The previous port kept falling through here on every visual refresh and therefore
            // posted WindowManager.updateViewLayout() repeatedly during optical wake/auth.
            if (HDR_ATTACH_LISTENERS.containsKey(pressedIcon)) {
                return;
            }
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    configureHdrLayout(pressedIcon);
                }
                @Override
                public void onViewDetachedFromWindow(View view) {
                    // Reference leaves the detached surface alone; the next attach reconfigures
                    // the window before it can become a pressed carrier again.
                }
            };
            HDR_ATTACH_LISTENERS.put(pressedIcon, listener);
            pressedIcon.addOnAttachStateChangeListener(listener);
        }
        if (pressedIcon.isAttachedToWindow()) {
            configureHdrLayout(pressedIcon);
        }
    }

    private static void configureHdrLayout(ImageView pressedIcon) {
        if (pressedIcon == null) {
            return;
        }
        pressedIcon.post(() -> {
            try {
                if (!(pressedIcon.getLayoutParams() instanceof WindowManager.LayoutParams)) {
                    PixelAodLog.log("COUI UDFPS HDR window skipped reason=non-window-layout-params");
                    return;
                }
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) pressedIcon.getLayoutParams();
                boolean liveTouchDown = isPressedTouchActive(pressedIcon);
                boolean hdrEnabled = isHdrPressEffectEnabled(pressedIcon.getContext());
                float headroom = CouiUdfpsPressedVisualPolicy.desiredHdrHeadroom(
                        liveTouchDown, hdrEnabled, maxHdrHeadroom(pressedIcon));
                params.setColorMode(ActivityInfo.COLOR_MODE_HDR);
                params.setDesiredHdrHeadroom(headroom);
                WindowManager manager = pressedIcon.getContext().getSystemService(WindowManager.class);
                if (manager == null) {
                    PixelAodLog.log("COUI UDFPS HDR window skipped reason=no-window-manager");
                    return;
                }
                manager.updateViewLayout(pressedIcon, params);
                // SurfaceControl can be recreated after updateViewLayout(); re-read live touch
                // on the next frame so idle attach never inherits stale max HDR headroom.
                pressedIcon.postOnAnimation(() -> updatePressedHdr(
                        pressedIcon, isPressedTouchActive(pressedIcon)));
                PixelAodLog.i("COUI UDFPS HDR window prepared headroom=" + headroom
                        + " touchDown=" + liveTouchDown);
            } catch (Throwable throwable) {
                PixelAodLog.log("COUI UDFPS HDR window preparation failed", throwable);
            }
        });
    }

    private static void updatePressedHdr(ImageView pressedIcon, boolean pressed) {
        if (pressedIcon == null) {
            return;
        }
        boolean enabled = isHdrPressEffectEnabled(pressedIcon.getContext());
        if (!enabled) {
            // Native optical mode owns all brightness/HBM surface state. In particular, do not
            // force desired HDR headroom back to 1 here: that transaction races OPlus optical
            // illumination and was correlated with real-finger authentication failure.
            return;
        }
        boolean active = enabled && pressed;
        if (!pressedIcon.isAttachedToWindow()) {
            return;
        }
        float headroom = active ? maxHdrHeadroom(pressedIcon) : 1f;
        try {
            Object root = ModernHookBridge.callMethod(pressedIcon, "getViewRootImpl");
            Object value = root != null ? ModernHookBridge.callMethod(root, "getSurfaceControl") : null;
            if (!(value instanceof SurfaceControl) || !((SurfaceControl) value).isValid()) {
                PixelAodLog.log("COUI UDFPS HDR surface skipped reason=invalid-surface");
                return;
            }
            SurfaceControl surface = (SurfaceControl) value;
            try (SurfaceControl.Transaction transaction = new SurfaceControl.Transaction()) {
                transaction.setDesiredHdrHeadroom(surface, headroom);
                transaction.setExtendedRangeBrightness(surface, active ? 7f : 1f, headroom);
                transaction.apply();
            }
            boolean shouldLog;
            synchronized (LAST_HDR_PRESS_STATES) {
                Boolean previous = LAST_HDR_PRESS_STATES.put(pressedIcon, active);
                shouldLog = previous == null || previous.booleanValue() != active;
            }
            if (shouldLog) {
                PixelAodLog.i("COUI UDFPS HDR surface pressed=" + active + " headroom=" + headroom);
            }
        } catch (Throwable throwable) {
            PixelAodLog.log("COUI UDFPS HDR surface update failed", throwable);
        }
    }

    private static void setPressedIlluminationAlpha(ImageView pressedIcon, boolean active) {
        if (pressedIcon == null) {
            return;
        }
        Drawable background = pressedIcon.getBackground();
        if (background instanceof CouiUdfpsPressedIlluminationDrawable) {
            background.setAlpha(CouiUdfpsPressedVisualPolicy.illuminationAlpha(
                    active, true));
        }
    }

    private static void setPressedTouchState(Object uiMech, boolean touchDown, String source) {
        ImageView pressedIcon = findPressedIcon(uiMech);
        if (pressedIcon == null) {
            return;
        }
        rememberPressedOriginal(pressedIcon);
        synchronized (PRESSED_TOUCH_STATES) {
            PRESSED_TOUCH_STATES.put(pressedIcon, touchDown);
        }
        dispatchViewOperation(pressedIcon, source,
                () -> applyPressedCarrierVisibility(pressedIcon, touchDown, source));
    }

    private static boolean isPressedTouchActive(ImageView pressedIcon) {
        synchronized (PRESSED_TOUCH_STATES) {
            return Boolean.TRUE.equals(PRESSED_TOUCH_STATES.get(pressedIcon));
        }
    }

    private static float originalPressedAlpha(ImageView pressedIcon) {
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            Float original = ORIGINAL_PRESSED_ALPHAS.get(pressedIcon);
            return original != null ? original : 1f;
        }
    }

    private static void applyPressedCarrierVisibility(ImageView pressedIcon, boolean touchDown,
            String source) {
        if (pressedIcon == null) {
            return;
        }
        applyPressedCarrierVisualOnly(pressedIcon, touchDown);
        updatePressedHdr(pressedIcon, touchDown);
        PixelAodLog.log("coui-udfps-stable-pressed-carrier", () ->
                "COUI UDFPS stable pressed carrier source=" + source
                        + " touchDown=" + touchDown
                        + " viewAlpha=" + pressedIcon.getAlpha()
                        + " backgroundAlpha=" + (pressedIcon.getBackground() != null
                        ? pressedIcon.getBackground().getAlpha() : -1));
    }

    private static void applyPressedCarrierVisualOnly(ImageView pressedIcon, boolean touchDown) {
        float targetAlpha = CouiUdfpsPressedVisualPolicy.pressedCarrierViewAlpha(
                touchDown, originalPressedAlpha(pressedIcon));
        if (Float.compare(pressedIcon.getAlpha(), targetAlpha) != 0) {
            pressedIcon.setAlpha(targetAlpha);
        }
        if (CouiUdfpsPressedVisualPolicy.useModulePressedCarrier(
                isHdrPressEffectEnabled(pressedIcon.getContext()))) {
            setPressedIlluminationAlpha(pressedIcon, touchDown);
        }
    }

    private static void reassertPressedCarrierVisibility(ImageView pressedIcon, String source) {
        if (pressedIcon == null) {
            return;
        }
        boolean tracked;
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            tracked = ORIGINAL_PRESSED_ALPHAS.containsKey(pressedIcon);
        }
        if (!tracked) {
            return;
        }
        boolean touchDown = isPressedTouchActive(pressedIcon);
        dispatchViewOperation(pressedIcon, source,
                () -> applyPressedCarrierVisualOnly(pressedIcon, touchDown));
    }

    /** Runs a mutation on the view hierarchy's owning handler, including the vendor optical view. */
    private static void dispatchViewOperation(View view, String source, Runnable operation) {
        if (view == null || operation == null) {
            return;
        }
        Handler handler = view.getHandler();
        if (handler != null && Looper.myLooper() == handler.getLooper()) {
            operation.run();
            return;
        }
        if (handler != null) {
            handler.post(operation);
            return;
        }
        if (!view.post(operation)) {
            PixelAodLog.log("COUI UDFPS view mutation deferred source=" + source
                    + " reason=no-owner-handler");
        }
    }

    private static void restoreOriginal(ImageView icon, String source) {
        if (icon == null) {
            return;
        }
        cancelCustomAodExit(icon);
        Drawable original;
        Drawable originalBackground;
        ColorStateList originalTint;
        ColorFilter originalFilter;
        ImageView.ScaleType originalScaleType;
        Float originalScaleX;
        Float originalScaleY;
        synchronized (ORIGINAL_DRAWABLES) {
            original = ORIGINAL_DRAWABLES.remove(icon);
            originalBackground = ORIGINAL_BACKGROUNDS.remove(icon);
            originalTint = ORIGINAL_TINTS.remove(icon);
            originalFilter = ORIGINAL_FILTERS.remove(icon);
            originalScaleType = ORIGINAL_SCALE_TYPES.remove(icon);
            originalScaleX = ORIGINAL_SCALE_X.remove(icon);
            originalScaleY = ORIGINAL_SCALE_Y.remove(icon);
        }
        CouiUdfpsDrawable coui;
        synchronized (DRAWABLES) {
            coui = DRAWABLES.remove(icon);
        }
        if (coui != null && icon.getDrawable() == coui) {
            icon.setImageDrawable(original);
        }
        if (coui != null || originalBackground != null) {
            icon.setBackground(originalBackground);
        }
        if (originalTint != null) {
            icon.setImageTintList(originalTint);
        }
        if (originalFilter != null) {
            icon.setColorFilter(originalFilter);
        }
        if (originalScaleType != null) {
            icon.setScaleType(originalScaleType);
        }
        if (originalScaleX != null) {
            icon.setScaleX(originalScaleX);
        }
        if (originalScaleY != null) {
            icon.setScaleY(originalScaleY);
        }
        PixelAodLog.log("COUI UDFPS restored native icon source=" + source);
    }

    private static void restorePressed(ImageView pressedIcon, String source) {
        if (pressedIcon == null) {
            return;
        }
        Drawable drawable;
        Drawable background;
        ColorStateList tint;
        ColorFilter filter;
        float originalAlpha = originalPressedAlpha(pressedIcon);
        synchronized (ORIGINAL_PRESSED_DRAWABLES) {
            if (!ORIGINAL_PRESSED_DRAWABLES.containsKey(pressedIcon)) {
                return;
            }
            drawable = ORIGINAL_PRESSED_DRAWABLES.remove(pressedIcon);
            background = ORIGINAL_PRESSED_BACKGROUNDS.remove(pressedIcon);
            tint = ORIGINAL_PRESSED_TINTS.remove(pressedIcon);
            filter = ORIGINAL_PRESSED_FILTERS.remove(pressedIcon);
        }
        pressedIcon.setImageDrawable(drawable);
        pressedIcon.setBackground(background);
        if (tint != null) {
            pressedIcon.setImageTintList(tint);
        }
        if (filter != null) {
            pressedIcon.setColorFilter(filter);
        }
        pressedIcon.setAlpha(originalAlpha);
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            ORIGINAL_PRESSED_ALPHAS.remove(pressedIcon);
        }
        synchronized (PRESSED_TOUCH_STATES) {
            PRESSED_TOUCH_STATES.remove(pressedIcon);
        }
        PixelAodLog.log("COUI UDFPS restored native pressed icon source=" + source);
    }

    private static void rememberOriginal(ImageView icon) {
        synchronized (ORIGINAL_DRAWABLES) {
            if (ORIGINAL_DRAWABLES.containsKey(icon)) {
                return;
            }
            ORIGINAL_DRAWABLES.put(icon, icon.getDrawable());
            ORIGINAL_BACKGROUNDS.put(icon, icon.getBackground());
            ORIGINAL_TINTS.put(icon, icon.getImageTintList());
            ORIGINAL_FILTERS.put(icon, icon.getColorFilter());
            ORIGINAL_SCALE_TYPES.put(icon, icon.getScaleType());
            ORIGINAL_SCALE_X.put(icon, icon.getScaleX());
            ORIGINAL_SCALE_Y.put(icon, icon.getScaleY());
        }
    }

    private static void scheduleDiscovery(Context context, Object uiMech, String source,
            boolean animate, boolean force) {
        synchronized (DISCOVERY_ATTEMPTS) {
            if (DISCOVERY_RUNNABLES.containsKey(uiMech)) {
                return;
            }
            DISCOVERY_ATTEMPTS.put(uiMech, 0);
        }
        Runnable[] holder = new Runnable[1];
        Runnable runnable = () -> {
            int attempt;
            synchronized (DISCOVERY_ATTEMPTS) {
                attempt = DISCOVERY_ATTEMPTS.getOrDefault(uiMech, 0);
                DISCOVERY_ATTEMPTS.put(uiMech, attempt + 1);
            }
            if (findFingerprintIcon(uiMech) != null) {
                synchronized (DISCOVERY_RUNNABLES) {
                    DISCOVERY_RUNNABLES.remove(uiMech);
                }
                synchronized (DISCOVERY_ATTEMPTS) {
                    DISCOVERY_ATTEMPTS.remove(uiMech);
                }
                requestVisualState(context, uiMech, source + "#discovered", animate, force);
                return;
            }
            if (attempt + 1 >= DISCOVERY_RETRY_DELAYS_MS.length) {
                synchronized (DISCOVERY_RUNNABLES) {
                    DISCOVERY_RUNNABLES.remove(uiMech);
                }
                PixelAodLog.log("COUI UDFPS discovery exhausted source=" + source);
                return;
            }
            MAIN.postDelayed(holder[0], DISCOVERY_RETRY_DELAYS_MS[attempt + 1]);
        };
        holder[0] = runnable;
        synchronized (DISCOVERY_RUNNABLES) {
            DISCOVERY_RUNNABLES.put(uiMech, runnable);
        }
        MAIN.postDelayed(runnable, DISCOVERY_RETRY_DELAYS_MS[0]);
    }

    private static void cancelDiscovery(Object uiMech) {
        Runnable runnable;
        synchronized (DISCOVERY_RUNNABLES) {
            runnable = DISCOVERY_RUNNABLES.remove(uiMech);
        }
        if (runnable != null) {
            MAIN.removeCallbacks(runnable);
        }
        synchronized (DISCOVERY_ATTEMPTS) {
            DISCOVERY_ATTEMPTS.remove(uiMech);
        }
    }

    private static void rememberUiMech(Object uiMech) {
        lastUiMech = new WeakReference<>(uiMech);
        PixelAodHook.rememberFingerprintAodInstance("OnScreenFingerprintUiMech", uiMech);
    }

    private static CouiUdfpsStateMachine stateFor(Object uiMech) {
        synchronized (STATES) {
            CouiUdfpsStateMachine state = STATES.get(uiMech);
            if (state == null) {
                state = new CouiUdfpsStateMachine();
                STATES.put(uiMech, state);
            }
            return state;
        }
    }

    private static void dispatch(Object uiMech, CouiUdfpsStateMachine.Event event,
            String source) {
        if (uiMech == null) {
            return;
        }
        CouiUdfpsStateMachine.Snapshot snapshot = stateFor(uiMech).dispatch(event);
        String stateText = snapshot.toString();
        synchronized (LAST_STATE_LOGS) {
            if (stateText.equals(LAST_STATE_LOGS.get(uiMech))) {
                return;
            }
            LAST_STATE_LOGS.put(uiMech, stateText);
        }
        PixelAodLog.i("COUI UDFPS transition event=" + event
                + " source=" + source + " " + stateText);
    }

    private static void showPress(Object uiMech, String source) {
        ImageView icon = findFingerprintIcon(uiMech);
        if (icon == null || !isReplacementEnabled(icon.getContext())) {
            return;
        }
        if (isHdrPressEffectEnabled(icon.getContext())) {
            ImageView pressedIcon = findPressedIcon(uiMech);
            boolean hdrEnabled = isHdrPressEffectEnabled(icon.getContext());
            dispatchViewOperation(pressedIcon, source + "#show-pressed", () -> {
                configurePressedIcon(pressedIcon, hdrEnabled);
                updatePressedHdr(pressedIcon, true);
                logPressedCarrierIfChanged(uiMech, pressedIcon, true, hdrEnabled, source);
            });
            dispatchViewOperation(icon, source + "#show-primary", () -> {
                CouiUdfpsGlowOverlay existing;
                synchronized (GLOW_OVERLAYS) {
                    existing = GLOW_OVERLAYS.get(icon);
                }
                if (existing != null) {
                    existing.hidePress();
                }
            });
            PixelAodLog.i("COUI UDFPS HDR press=show source=" + source);
            return;
        }
        // HDR disabled means native optical pressed illumination, matching stable 0.1.331.
        // Do not add a module overlay before authentication.
        dispatchViewOperation(icon, source + "#show-primary-native", () -> {
            CouiUdfpsGlowOverlay overlay;
            synchronized (GLOW_OVERLAYS) {
                overlay = GLOW_OVERLAYS.get(icon);
            }
            if (overlay != null) {
                overlay.hidePress();
            }
        });
        PixelAodLog.i("COUI UDFPS press=native-optical source=" + source);
    }

    private static void hidePress(Object uiMech, String source) {
        ImageView icon = findFingerprintIcon(uiMech);
        if (icon == null) {
            return;
        }
        ImageView pressedIcon = findPressedIcon(uiMech);
        dispatchViewOperation(pressedIcon, source + "#hide-pressed", () -> {
            updatePressedHdr(pressedIcon, false);
            logPressedCarrierIfChanged(uiMech, pressedIcon, false,
                    isHdrPressEffectEnabled(icon.getContext()), source + "#hide");
        });
        dispatchViewOperation(icon, source + "#hide-primary", () -> {
            CouiUdfpsGlowOverlay overlay;
            synchronized (GLOW_OVERLAYS) {
                overlay = GLOW_OVERLAYS.get(icon);
            }
            if (overlay != null) {
                overlay.hidePress();
                PixelAodLog.i("COUI UDFPS press glow=hide source=" + source);
            }
        });
    }

    private static void stopPress(Object uiMech, String source) {
        if (uiMech == null) {
            return;
        }
        dispatch(uiMech, CouiUdfpsStateMachine.Event.FAILURE, source);
        hidePress(uiMech, source);
        requestVisualState(null, uiMech, source, false, true);
    }

    private static void showSuccess(Object uiMech, String source) {
        if (uiMech == null) {
            return;
        }
        ImageView icon = findFingerprintIcon(uiMech);
        if (icon == null || !isReplacementEnabled(icon.getContext())) {
            return;
        }
        dispatch(uiMech, CouiUdfpsStateMachine.Event.SUCCESS, source);
        if (!isSuccessRippleEnabled(icon.getContext())) {
            hidePress(uiMech, source + "#native-success");
            PixelAodLog.i("COUI UDFPS success ripple=native source=" + source);
            return;
        }
        CouiUdfpsGlowOverlay overlay = glowOverlay(icon);
        overlay.showSuccess(glowSpec(icon));
        PixelAodLog.i("COUI UDFPS success glow=show source=" + source);
    }

    private static CouiUdfpsGlowOverlay glowOverlay(ImageView icon) {
        synchronized (GLOW_OVERLAYS) {
            CouiUdfpsGlowOverlay overlay = GLOW_OVERLAYS.get(icon);
            if (overlay == null) {
                overlay = new CouiUdfpsGlowOverlay(icon.getContext());
                GLOW_OVERLAYS.put(icon, overlay);
            }
            return overlay;
        }
    }

    private static CouiUdfpsGlowOverlay.GlowSpec glowSpec(ImageView icon) {
        int[] location = new int[2];
        try {
            icon.getLocationOnScreen(location);
        } catch (Throwable ignored) {
        }
        float centerX = location[0] + icon.getWidth() / 2f;
        float centerY = location[1] + icon.getHeight() / 2f;
        float density = icon.getResources().getDisplayMetrics().density;
        boolean dark = isDark(icon.getContext());
        int introColor = dark ? Color.WHITE
                : resolveToneColor(icon.getContext(), "system_accent1_0", Color.WHITE);
        int successColor = dark ? resolveToneColor(
                icon.getContext(), "system_accent1_200", Color.WHITE)
                : resolveToneColor(icon.getContext(), "system_accent1_500",
                        Color.rgb(25, 90, 100));
        return new CouiUdfpsGlowOverlay.GlowSpec(centerX, centerY, density * 32f,
                introColor, successColor, dark ? 1f : 1.35f);
    }

    static boolean startCustomAodExit(Object uiMech) {
        if (uiMech == null) {
            return false;
        }
        ImageView icon = findFingerprintIcon(uiMech);
        if (icon == null || !isReplacementEnabled(icon.getContext())) {
            return false;
        }
        if (!isAodExitAnimationEnabled(icon.getContext())) {
            return false;
        }
        CouiUdfpsDrawable drawable;
        synchronized (DRAWABLES) {
            drawable = DRAWABLES.get(icon);
        }
        if (drawable == null) {
            if (icon.getHandler() == null
                    || Looper.myLooper() != icon.getHandler().getLooper()) {
                icon.post(() -> {
                    applyVisualState(icon.getContext(), uiMech,
                            "custom-aod-exit#prepare", false, true, true);
                    startCustomAodExit(uiMech);
                });
                return true;
            }
            applyVisualState(icon.getContext(), uiMech,
                    "custom-aod-exit#prepare", false, true, true);
            synchronized (DRAWABLES) {
                drawable = DRAWABLES.get(icon);
            }
        }
        if (drawable == null) {
            return false;
        }
        final CouiUdfpsDrawable targetDrawable = drawable;
        Runnable start = () -> {
            cancelCustomAodExit(icon);
            if (icon.getDrawable() != targetDrawable) {
                icon.setImageDrawable(targetDrawable);
            }
            ValueAnimator animator = ValueAnimator.ofInt(targetDrawable.getAlpha(), 0);
            animator.setDuration(resolveAodExitDuration(uiMech));
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(valueAnimator ->
                    targetDrawable.setAlpha((Integer) valueAnimator.getAnimatedValue()));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    synchronized (EXIT_ANIMATORS) {
                        if (EXIT_ANIMATORS.get(icon) == animation) {
                            EXIT_ANIMATORS.remove(icon);
                        }
                    }
                    PixelAodLog.log("COUI UDFPS custom AOD exit=end");
                }
            });
            synchronized (EXIT_ANIMATORS) {
                EXIT_ANIMATORS.put(icon, animator);
            }
            PixelAodLog.i("COUI UDFPS custom AOD exit=start duration="
                    + animator.getDuration());
            animator.start();
        };
        if (icon.getHandler() != null && Looper.myLooper() == icon.getHandler().getLooper()) {
            start.run();
        } else {
            icon.post(start);
        }
        return true;
    }

    static void cancelCustomAodExit(ImageView icon) {
        if (icon == null) {
            return;
        }
        ValueAnimator animator;
        synchronized (EXIT_ANIMATORS) {
            animator = EXIT_ANIMATORS.remove(icon);
        }
        if (animator != null) {
            animator.cancel();
            PixelAodLog.log("COUI UDFPS custom AOD exit=cancel");
        }
    }

    private static boolean isCustomAodExitRunning(ImageView icon) {
        synchronized (EXIT_ANIMATORS) {
            ValueAnimator animator = EXIT_ANIMATORS.get(icon);
            return animator != null && (animator.isStarted() || animator.isRunning());
        }
    }

    private static long resolveAodExitDuration(Object uiMech) {
        long duration = 500L;
        try {
            Class<?> clazz = Class.forName(FINGERPRINT_UTILS_CLASS, false,
                    uiMech.getClass().getClassLoader());
            Field instanceField = clazz.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            Object result = ModernHookBridge.callMethod(instance, "getFadeOutAnimTime");
            if (result instanceof Number) {
                duration = ((Number) result).longValue();
            }
        } catch (Throwable throwable) {
            PixelAodLog.log("COUI UDFPS AOD exit duration fallback reason="
                    + throwable.getClass().getSimpleName());
        }
        return CouiUdfpsStateMachine.clampAodExitDurationMillis(duration);
    }

    private static boolean isHdrPressEffectEnabled(Context context) {
        return context != null && PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_UDFPS_HDR_PRESS_EFFECT, true);
    }

    private static boolean isSuccessRippleEnabled(Context context) {
        return context != null && PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_UDFPS_SUCCESS_RIPPLE, true);
    }

    private static boolean isAodExitAnimationEnabled(Context context) {
        return context != null && PixelAodSettings.getBoolean(context,
                PixelAodSettings.KEY_UDFPS_AOD_EXIT_ANIMATION, true);
    }

    private static boolean isReplacementEnabled(Context context) {
        return PixelAodFeatureFlags.useCouiUdfps()
                && context != null
                && PixelAodSettings.getBoolean(
                        context, PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, false);
    }

    private static boolean isDark(Context context) {
        return context != null
                && (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isInteractive(Context context) {
        if (context == null) {
            return false;
        }
        PowerManager manager = context.getSystemService(PowerManager.class);
        return manager != null && manager.isInteractive();
    }

    private static int resolveToneColor(Context context, String name, int fallback) {
        if (context == null) {
            return fallback;
        }
        try {
            int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
            if (id == 0) {
                id = context.getResources().getIdentifier(name, "color", "com.android.systemui");
            }
            return id != 0 ? context.getColor(id) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static void logLiveStateIfChanged(Object uiMech, boolean aod,
            boolean liveTouchDown, String source, ImageView icon) {
        String state = "surface=" + (aod ? "AOD" : "LOCKSCREEN")
                + ",touchDown=" + liveTouchDown + ",drawable="
                + (icon.getDrawable() != null ? icon.getDrawable().getClass().getSimpleName() : "null")
                + ",visibility=" + icon.getVisibility();
        synchronized (LAST_STATE_LOGS) {
            if (state.equals(LAST_STATE_LOGS.get(uiMech))) {
                return;
            }
            LAST_STATE_LOGS.put(uiMech, state);
        }
        PixelAodLog.i("COUI UDFPS visual owner=COUI_PORT source=" + source + " " + state);
    }

    private static void logPressedCarrierIfChanged(Object uiMech, ImageView pressedIcon,
            boolean liveTouchDown, boolean hdrEnabled, String source) {
        if (uiMech == null || pressedIcon == null) {
            return;
        }
        Drawable background = pressedIcon.getBackground();
        Drawable drawable = pressedIcon.getDrawable();
        Drawable foreground = pressedIcon.getForeground();
        boolean surfaceValid = false;
        try {
            Object root = ModernHookBridge.callMethod(pressedIcon, "getViewRootImpl");
            Object value = root != null ? ModernHookBridge.callMethod(root, "getSurfaceControl")
                    : null;
            surfaceValid = value instanceof SurfaceControl
                    && ((SurfaceControl) value).isValid();
        } catch (Throwable ignored) {
        }
        String state = "touchDown=" + liveTouchDown
                + ",visibility=" + pressedIcon.getVisibility()
                + ",viewAlpha=" + pressedIcon.getAlpha()
                + ",imageAlpha=" + pressedIcon.getImageAlpha()
                + ",pressed=" + pressedIcon.isPressed()
                + ",drawable=" + (drawable == null ? "null" : drawable.getClass().getSimpleName())
                + ",drawableAlpha=" + (drawable == null ? -1 : drawable.getAlpha())
                + ",background=" + (background == null
                        ? "null" : background.getClass().getSimpleName())
                + ",backgroundAlpha=" + (background == null ? -1 : background.getAlpha())
                + ",foreground=" + (foreground == null
                        ? "null" : foreground.getClass().getSimpleName())
                + ",foregroundAlpha=" + (foreground == null ? -1 : foreground.getAlpha())
                + ",hdrActive=" + CouiUdfpsPressedVisualPolicy.primaryDrawablePressed(
                        liveTouchDown, hdrEnabled)
                + ",surfaceValid=" + surfaceValid
                + ",headroom=" + maxHdrHeadroom(pressedIcon)
                + ",thread=" + Thread.currentThread().getName();
        synchronized (LAST_PRESSED_CARRIER_LOGS) {
            if (state.equals(LAST_PRESSED_CARRIER_LOGS.get(uiMech))) {
                return;
            }
            LAST_PRESSED_CARRIER_LOGS.put(uiMech, state);
        }
        PixelAodLog.i("COUI UDFPS pressed carrier rendererMode=COUI_PORT source="
                + source + " " + state);
    }

    private static ImageView findFingerprintIcon(Object uiMech) {
        if (uiMech == null) {
            return null;
        }
        String[] names = {
                "fpIcon", "mFpIcon", "fingerprintIcon", "mFingerprintIcon",
                "fpIconView", "mFpIconView", "udfpsIcon", "mUdfpsIcon"
        };
        for (String name : names) {
            Object value = readObjectField(uiMech, name);
            if (value instanceof ImageView) {
                return (ImageView) value;
            }
        }
        try {
            Object value = ModernHookBridge.callMethod(uiMech, "getFingerprintIcon");
            if (value instanceof ImageView) {
                return (ImageView) value;
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

    private static ImageView findPressedIcon(Object uiMech) {
        if (uiMech == null) {
            return null;
        }
        for (String name : new String[] {
                "pressedIcon", "mPressedIcon", "fingerprintPressedIcon", "mFingerprintPressedIcon"
        }) {
            Object value = readObjectField(uiMech, name);
            if (value instanceof ImageView) {
                return (ImageView) value;
            }
        }
        try {
            Object value = ModernHookBridge.callMethod(uiMech, "getFingerprintPressedIcon");
            if (value instanceof ImageView) {
                return (ImageView) value;
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

    private static void rememberPressedOriginal(ImageView icon) {
        synchronized (ORIGINAL_PRESSED_DRAWABLES) {
            boolean moduleCarrier =
                    icon.getBackground() instanceof CouiUdfpsPressedIlluminationDrawable;
            if (!moduleCarrier) {
                // Refresh the native snapshot while OPlus still owns the carrier. Constructors
                // can initially expose null drawables and populate the real optical assets later.
                ORIGINAL_PRESSED_DRAWABLES.put(icon, icon.getDrawable());
                ORIGINAL_PRESSED_BACKGROUNDS.put(icon, icon.getBackground());
                ORIGINAL_PRESSED_TINTS.put(icon, icon.getImageTintList());
                ORIGINAL_PRESSED_FILTERS.put(icon, icon.getColorFilter());
            }
        }
        synchronized (ORIGINAL_PRESSED_ALPHAS) {
            if (!ORIGINAL_PRESSED_ALPHAS.containsKey(icon)) {
                ORIGINAL_PRESSED_ALPHAS.put(icon, icon.getAlpha());
            }
        }
    }

    private static Object readObjectField(Object receiver, String fieldName) {
        if (receiver == null) {
            return null;
        }
        try {
            return ModernHookBridge.getObjectField(receiver, fieldName);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean readBooleanField(Object receiver, String fieldName) {
        Object value = readObjectField(receiver, fieldName);
        return value instanceof Boolean && (Boolean) value;
    }

    private static Boolean firstBoolean(Object[] args) {
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

    private static Boolean visibilityArgument(Object[] args) {
        return CouiUdfpsStateMachine.visibilityArgument(args);
    }

    private static boolean isFingerprintSource(Object[] args) {
        if (args == null) {
            return false;
        }
        for (Object arg : args) {
            if (arg != null && "FINGERPRINT".equalsIgnoreCase(String.valueOf(arg))) {
                return true;
            }
        }
        return false;
    }

    private static Context contextFrom(ModernHookBridge.HookParam param, Object fallback) {
        if (param != null) {
            if (param.thisObject instanceof View) {
                return ((View) param.thisObject).getContext();
            }
            if (param.args != null) {
                for (Object arg : param.args) {
                    if (arg instanceof Context) {
                        return (Context) arg;
                    }
                    if (arg instanceof View) {
                        return ((View) arg).getContext();
                    }
                }
            }
        }
        Object directContext = readObjectField(fallback, "context");
        if (directContext instanceof Context) {
            return (Context) directContext;
        }
        ImageView icon = findFingerprintIcon(fallback);
        return icon != null ? icon.getContext() : null;
    }

    private static Method findCompatibleMethod(Class<?> clazz, String name,
            Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (!name.equals(method.getName())
                        || method.getParameterCount() != parameterTypes.length) {
                    continue;
                }
                Class<?>[] actual = method.getParameterTypes();
                boolean compatible = true;
                for (int i = 0; i < actual.length; i++) {
                    if (!wrap(actual[i]).isAssignableFrom(wrap(parameterTypes[i]))) {
                        compatible = false;
                        break;
                    }
                }
                if (compatible) {
                    method.setAccessible(true);
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private static Object defaultReturnValue(Class<?> type) {
        if (type == null || !type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return (char) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String signature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(parameters[i].getSimpleName());
        }
        return builder.append(')').toString();
    }

    private static String simpleClassName(String className) {
        int index = className.lastIndexOf('.');
        return index >= 0 ? className.substring(index + 1) : className;
    }

    private static final class PendingRefresh {
        final WeakReference<Object> target;
        Context context;
        String source;
        boolean animate;
        boolean force;
        Runnable runnable;

        PendingRefresh(Object target, Context context, String source,
                boolean animate, boolean force) {
            this.target = new WeakReference<>(target);
            this.context = context;
            this.source = source;
            this.animate = animate;
            this.force = force;
        }
    }
}
