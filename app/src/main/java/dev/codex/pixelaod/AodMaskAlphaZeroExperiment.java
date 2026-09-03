package dev.codex.pixelaod;

import android.os.SystemClock;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/** Controlled black-frame experiment: keep only the vendor mAODMask transparent. */
final class AodMaskAlphaZeroExperiment {
    private static final String MASK_CONTROLLER =
            "com.oplus.systemui.aod.anim.OplusAODMaskAnimController";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static volatile WeakReference<View> trackedMask = new WeakReference<>(null);

    private AodMaskAlphaZeroExperiment() {}

    static boolean install(ClassLoader classLoader) {
        if (classLoader == null || !INSTALLED.compareAndSet(false, true)) {
            return INSTALLED.get();
        }
        try {
            Class<?> controller = ModernHookBridge.findClass(MASK_CONTROLLER, classLoader);
            for (Method method : controller.getDeclaredMethods()) {
                if (!"startAodMaskAnim".equals(method.getName())) {
                    continue;
                }
                method.setAccessible(true);
                ModernHookBridge.hookBefore(method, param -> rememberMask(param.thisObject));
            }
            ModernHookBridge.hookBefore(View.class, "setAlpha", param -> {
                View mask = trackedMask.get();
                if (mask == null || param.thisObject != mask || param.args == null
                        || param.args.length == 0 || !(param.args[0] instanceof Number)) {
                    return;
                }
                float requested = ((Number) param.args[0]).floatValue();
                if (requested == 0.0f) {
                    return;
                }
                param.args[0] = 0.0f;
                PixelAodLog.i("[BLACK-FRAME-EXP] MASK_ALPHA_ZERO requested=" + requested
                        + " forced=0 elapsedMs="
                        + (SystemClock.elapsedRealtimeNanos() / 1_000_000L)
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            }, float.class);
            PixelAodLog.i("[BLACK-FRAME-EXP] MASK_ALPHA_ZERO installed");
            return true;
        } catch (Throwable t) {
            PixelAodLog.e("[BLACK-FRAME-EXP] MASK_ALPHA_ZERO install failed", t);
            return false;
        }
    }

    private static void rememberMask(Object controller) {
        if (controller == null) {
            return;
        }
        try {
            Object value = ModernHookBridge.getObjectField(controller, "mAODMask");
            if (value instanceof View) {
                trackedMask = new WeakReference<>((View) value);
                PixelAodLog.i("[BLACK-FRAME-EXP] tracked mAODMask="
                        + Integer.toHexString(System.identityHashCode(value)));
            }
        } catch (Throwable t) {
            PixelAodLog.e("[BLACK-FRAME-EXP] mAODMask tracking failed", t);
        }
    }
}
