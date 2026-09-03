package dev.codex.pixelaod;

import android.os.SystemClock;
import android.service.dreams.DreamService;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;

/** Controlled black-frame experiment: bypass only Display OFF during AOD mask entry. */
final class AodEntryOffBypassExperiment {
    private static final String AOD_DISPLAY_UTIL =
            "com.oplus.systemui.aod.display.AODDisplayUtil";
    private static final String MASK_CONTROLLER =
            "com.oplus.systemui.aod.anim.OplusAODMaskAnimController";
    private static final long ENTRY_WINDOW_MS = 1200L;
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static volatile long entryUntilElapsedMs;

    private AodEntryOffBypassExperiment() {}

    static boolean install(ClassLoader classLoader) {
        if (classLoader == null || !INSTALLED.compareAndSet(false, true)) {
            return INSTALLED.get();
        }
        try {
            Class<?> maskController = ModernHookBridge.findClass(MASK_CONTROLLER, classLoader);
            for (Method method : maskController.getDeclaredMethods()) {
                if (!"startAodMaskAnim".equals(method.getName())) {
                    continue;
                }
                method.setAccessible(true);
                ModernHookBridge.hookBefore(method, param -> {
                    entryUntilElapsedMs = SystemClock.elapsedRealtime() + ENTRY_WINDOW_MS;
                    PixelAodLog.i("[BLACK-FRAME-EXP] ENTRY_OFF_BYPASS armed until="
                            + entryUntilElapsedMs + " trace="
                            + PixelAodClockView.currentAodTraceId());
                });
            }

            ModernHookBridge.hookBefore(DreamService.class, "setDozeScreenState", param -> {
                if (!entryActive() || param.args == null || param.args.length == 0
                        || !(param.args[0] instanceof Number)
                        || ((Number) param.args[0]).intValue() != 1) {
                    return;
                }
                param.args[0] = 3;
                logBypass("DreamService#setDozeScreenState", null);
            }, int.class);

            Class<?> displayUtil = ModernHookBridge.findClass(AOD_DISPLAY_UTIL, classLoader);
            for (Method method : displayUtil.getDeclaredMethods()) {
                if (Modifier.isAbstract(method.getModifiers())
                        || !"requestScreenState".equals(method.getName())
                        || method.getParameterTypes().length != 3) {
                    continue;
                }
                method.setAccessible(true);
                ModernHookBridge.hookBefore(method, param -> {
                    if (!entryActive() || param.args == null || param.args.length < 3
                            || !(param.args[0] instanceof Number)
                            || ((Number) param.args[0]).intValue() != 1) {
                        return;
                    }
                    String reason = String.valueOf(param.args[2]);
                    param.args[0] = 3;
                    logBypass("AODDisplayUtil#requestScreenState", reason);
                });
            }
            PixelAodLog.i("[BLACK-FRAME-EXP] ENTRY_OFF_BYPASS installed");
            return true;
        } catch (Throwable t) {
            PixelAodLog.e("[BLACK-FRAME-EXP] ENTRY_OFF_BYPASS install failed", t);
            return false;
        }
    }

    private static boolean entryActive() {
        return SystemClock.elapsedRealtime() <= entryUntilElapsedMs;
    }

    private static void logBypass(String source, String reason) {
        PixelAodLog.i("[BLACK-FRAME-EXP] ENTRY_OFF_BYPASS source=" + source
                + " OFF->DOZE reason=" + reason
                + " elapsedMs=" + SystemClock.elapsedRealtime()
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }
}
