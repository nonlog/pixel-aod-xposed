package dev.codex.pixelaod;

import android.os.SystemClock;
import android.service.dreams.DreamService;
import android.view.Display;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Observation-only tracing for the persistent lockscreen-to-AOD black frame. */
final class AodBlackFrameDiagnostics {
    private static final String AOD_DISPLAY_UTIL = "com.oplus.systemui.aod.display.AODDisplayUtil";
    private static final String AOD_MASK_CONTROLLER = "com.oplus.systemui.aod.anim.OplusAODMaskAnimController";
    private static final String AOD_BLACK_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodBlackLayout";
    private static final String WAKEFULNESS_LIFECYCLE = "com.android.systemui.keyguard.WakefulnessLifecycle";
    private static final String PREFIX = "[BLACK-FRAME-DIAG]";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static volatile WeakReference<Object> lastDisplayUtil = new WeakReference<>(null);
    private static volatile WeakReference<Object> lastMaskController = new WeakReference<>(null);

    private AodBlackFrameDiagnostics() {}

    static void install(ClassLoader classLoader) {
        if (classLoader == null || !INSTALLED.compareAndSet(false, true)) return;
        boolean display = hookDisplayUtil(classLoader);
        boolean mask = hookMaskController(classLoader);
        boolean blackLayout = hookBlackLayout(classLoader);
        boolean dream = hookDreamService();
        boolean wakefulness = hookWakefulness(classLoader);
        log("install", null, null, "display=" + display + " mask=" + mask
                + " blackLayout=" + blackLayout + " dream=" + dream
                + " wakefulness=" + wakefulness);
    }

    private static boolean hookDisplayUtil(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(AOD_DISPLAY_UTIL, classLoader);
            int hooked = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) || !isDisplayDiagnosticMethod(method.getName())) continue;
                method.setAccessible(true);
                final String source = "AODDisplayUtil#" + signature(method);
                ModernHookBridge.hookBefore(method, param -> {
                    if (param.thisObject != null) lastDisplayUtil = new WeakReference<>(param.thisObject);
                    log(source + ":before", param.thisObject, param.args, null);
                });
                ModernHookBridge.hookAfter(method, param -> {
                    if (param.thisObject != null) lastDisplayUtil = new WeakReference<>(param.thisObject);
                    log(source + ":after", param.thisObject, param.args,
                            "result=" + summarizeValue(param.getResult()));
                });
                hooked++;
            }
            PixelAodLog.i(PREFIX + " hooked AODDisplayUtil methods=" + hooked);
            return hooked > 0;
        } catch (Throwable t) {
            PixelAodLog.e(PREFIX + " failed AODDisplayUtil diagnostics", t);
            return false;
        }
    }

    private static boolean hookMaskController(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(AOD_MASK_CONTROLLER, classLoader);
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    if (param.thisObject != null) lastMaskController = new WeakReference<>(param.thisObject);
                    log("OplusAODMaskAnimController#constructor:after", param.thisObject, param.args, null);
                });
            }
            int hooked = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) || !isMaskDiagnosticMethod(method.getName())) continue;
                method.setAccessible(true);
                final String source = "OplusAODMaskAnimController#" + signature(method);
                ModernHookBridge.hookBefore(method, param -> {
                    if (param.thisObject != null) lastMaskController = new WeakReference<>(param.thisObject);
                    log(source + ":before", param.thisObject, param.args, null);
                });
                ModernHookBridge.hookAfter(method, param -> {
                    if (param.thisObject != null) lastMaskController = new WeakReference<>(param.thisObject);
                    String extra = "result=" + summarizeValue(param.getResult());
                    if (param.getResult() instanceof View) extra += " resultView={" + describeView((View) param.getResult()) + "}";
                    log(source + ":after", param.thisObject, param.args, extra);
                });
                hooked++;
            }
            PixelAodLog.i(PREFIX + " hooked mask-controller methods=" + hooked);
            return hooked > 0;
        } catch (Throwable t) {
            PixelAodLog.e(PREFIX + " failed mask-controller diagnostics", t);
            return false;
        }
    }

    private static boolean hookBlackLayout(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(AOD_BLACK_LAYOUT, classLoader);
            int hooks = 0;
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor, param -> {
                    View view = param.thisObject instanceof View ? (View) param.thisObject : null;
                    log("AodBlackLayout#constructor:after", param.thisObject, param.args,
                            view != null ? "view={" + describeView(view) + "}" : null);
                });
                hooks++;
            }
            for (Method method : clazz.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) || !isBlackLayoutDiagnosticMethod(method.getName())) continue;
                method.setAccessible(true);
                final String source = "AodBlackLayout#" + signature(method);
                ModernHookBridge.hookBefore(method, param -> log(source + ":before", param.thisObject, param.args,
                        param.thisObject instanceof View ? "view={" + describeView((View) param.thisObject) + "}" : null));
                ModernHookBridge.hookAfter(method, param -> log(source + ":after", param.thisObject, param.args,
                        param.thisObject instanceof View ? "view={" + describeView((View) param.thisObject) + "}" : null));
                hooks++;
            }
            PixelAodLog.i(PREFIX + " hooked AodBlackLayout members=" + hooks);
            return hooks > 0;
        } catch (Throwable t) {
            PixelAodLog.e(PREFIX + " failed AodBlackLayout diagnostics", t);
            return false;
        }
    }

    private static boolean hookDreamService() {
        try {
            ModernHookBridge.hookBefore(DreamService.class, "setDozeScreenState",
                    param -> log("DreamService#setDozeScreenState:before", param.thisObject, param.args, null), int.class);
            ModernHookBridge.hookAfter(DreamService.class, "setDozeScreenState",
                    param -> log("DreamService#setDozeScreenState:after", param.thisObject, param.args, null), int.class);
            return true;
        } catch (Throwable t) {
            PixelAodLog.e(PREFIX + " failed DreamService diagnostics", t);
            return false;
        }
    }

    private static boolean hookWakefulness(ClassLoader classLoader) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(WAKEFULNESS_LIFECYCLE, classLoader);
            int hooked = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                String name = method.getName();
                if (Modifier.isAbstract(method.getModifiers()) || !(name.contains("GoingToSleep") || name.contains("WakingUp"))) continue;
                method.setAccessible(true);
                final String source = "WakefulnessLifecycle#" + signature(method);
                ModernHookBridge.hookBefore(method, param -> log(source + ":before", param.thisObject, param.args, null));
                ModernHookBridge.hookAfter(method, param -> log(source + ":after", param.thisObject, param.args, null));
                hooked++;
            }
            PixelAodLog.i(PREFIX + " hooked WakefulnessLifecycle methods=" + hooked);
            return hooked > 0;
        } catch (Throwable t) {
            PixelAodLog.e(PREFIX + " failed WakefulnessLifecycle diagnostics", t);
            return false;
        }
    }

    private static boolean isDisplayDiagnosticMethod(String name) {
        return "requestScreenState".equals(name) || "setScreenState".equals(name)
                || "updateDisplayState".equals(name) || "requestSmoothTransitionScreenState".equals(name)
                || "smoothTransitionRequestScreenState".equals(name);
    }

    private static boolean isMaskDiagnosticMethod(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.US);
        if (!lower.contains("mask")) return false;
        return lower.startsWith("start") || lower.startsWith("show") || lower.startsWith("hide")
                || lower.startsWith("check") || lower.startsWith("reset") || lower.startsWith("update")
                || lower.startsWith("add") || lower.startsWith("prepare");
    }

    private static boolean isBlackLayoutDiagnosticMethod(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.US);
        return lower.contains("show") || lower.contains("hide") || lower.contains("anim")
                || lower.contains("attach") || lower.contains("detach") || lower.contains("visibility");
    }

    private static void log(String event, Object owner, Object[] args, String extra) {
        long seq = SEQUENCE.incrementAndGet();
        long elapsed = SystemClock.elapsedRealtimeNanos() / 1_000_000L;
        long uptime = SystemClock.uptimeMillis();
        Object displayUtil = AOD_DISPLAY_UTIL.equals(className(owner)) ? owner : lastDisplayUtil.get();
        Object maskController = AOD_MASK_CONTROLLER.equals(className(owner)) ? owner : lastMaskController.get();
        StringBuilder message = new StringBuilder(PREFIX)
                .append(" seq=").append(seq).append(" elapsedMs=").append(elapsed)
                .append(" uptimeMs=").append(uptime).append(" thread=").append(Thread.currentThread().getName())
                .append(" event=").append(event).append(" args={").append(summarizeArgs(args)).append('}')
                .append(" display={").append(describeDisplayUtil(displayUtil)).append('}')
                .append(" mask={").append(describeMaskController(maskController)).append('}')
                .append(" trace=").append(PixelAodClockView.currentAodTraceId());
        if (extra != null && !extra.isEmpty()) message.append(' ').append(extra);
        PixelAodLog.i(message.toString());
    }

    private static String describeDisplayUtil(Object target) {
        if (target == null) return "null";
        return "requested=" + readDisplayStateField(target, "mRequestedDisplayState")
                + ",device=" + readDisplayStateField(target, "mDeviceDisplayState")
                + ",frameDuration=" + readField(target, "mFrameDuration")
                + ",block=" + readField(target, "mBlockRequestState")
                + ",offWhileDreaming=" + callNoArg(target, "isOffWhileDreaming")
                + ",dreaming=" + callNoArg(target, "isDreaming");
    }

    private static String describeMaskController(Object target) {
        if (target == null) return "null";
        Object mask = readFieldObject(target, "mAODMask");
        Object black = readFieldObject(target, "aodBlackLayout");
        return "animState=" + readField(target, "mAnimState")
                + ",waking=" + readField(target, "isWakingUpAnimRunning")
                + ",blackFlag=" + readField(target, "iSAodBlackLayoutShow")
                + ",maskView=" + (mask instanceof View ? describeView((View) mask) : summarizeValue(mask))
                + ",blackView=" + (black instanceof View ? describeView((View) black) : summarizeValue(black));
    }

    private static String describeView(View view) {
        if (view == null) return "null";
        return view.getClass().getSimpleName() + ",vis=" + view.getVisibility() + ",alpha=" + view.getAlpha()
                + ",shown=" + view.isShown() + ",attached=" + view.isAttachedToWindow()
                + ",size=" + view.getWidth() + "x" + view.getHeight();
    }

    private static String readDisplayStateField(Object target, String fieldName) {
        Object value = readFieldObject(target, fieldName);
        if (!(value instanceof Number)) return summarizeValue(value);
        return displayState(((Number) value).intValue());
    }

    private static String readField(Object target, String fieldName) { return summarizeValue(readFieldObject(target, fieldName)); }

    private static Object readFieldObject(Object target, String fieldName) {
        if (target == null) return null;
        try { return ModernHookBridge.getObjectField(target, fieldName); } catch (Throwable ignored) { return null; }
    }

    private static String callNoArg(Object target, String name) {
        if (target == null) return "null";
        try { return summarizeValue(ModernHookBridge.callMethod(target, name)); } catch (Throwable ignored) { return "?"; }
    }

    private static String summarizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "none";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) builder.append(';');
            Object value = args[i];
            builder.append(i).append('=');
            if (value instanceof Integer && i == 0) builder.append(displayState((Integer) value));
            else builder.append(summarizeValue(value));
        }
        return builder.toString();
    }

    private static String summarizeValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String) {
            String text = ((String) value).replace('\n', ' ').replace('\r', ' ');
            return text.length() > 80 ? text.substring(0, 80) + "..." : text;
        }
        if (value instanceof View) return describeView((View) value);
        return String.valueOf(value);
    }

    private static String displayState(int state) {
        try { return Display.stateToString(state) + '(' + state + ')'; }
        catch (Throwable ignored) { return String.valueOf(state); }
    }

    private static String signature(Method method) {
        StringBuilder builder = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(parameterTypes[i].getSimpleName());
        }
        return builder.append(')').toString();
    }

    private static String className(Object value) { return value != null ? value.getClass().getName() : ""; }
}
