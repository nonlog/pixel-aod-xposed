package dev.codex.pixelaod;

import android.content.Context;

/** Owns clock host, shade and stock-visibility hook registration. */
final class PixelAodSurfaceHookInstaller {
    private PixelAodSurfaceHookInstaller() {
    }

    static void installClockLayout(Context context, ClassLoader classLoader) {
        PixelAodHook.hookClockLayout(context, classLoader);
    }

    static void installGlobalStockSuppression(boolean enabled) {
        if (enabled) {
            PixelAodHook.hookStockClockVisibilityAndAlphaSuppression();
        } else {
            PixelAodLog.log("skipped global stock View visibility/alpha hooks");
        }
    }

    static void installShadeAndLockscreen(Context context, ClassLoader classLoader) {
        PixelAodHook.hookShadeWindowView(context, classLoader);
        PixelAodHook.hookLockscreenClockProbe(classLoader);
    }
}
