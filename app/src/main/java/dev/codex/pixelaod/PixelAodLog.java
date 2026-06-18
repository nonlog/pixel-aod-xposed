package dev.codex.pixelaod;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

final class PixelAodLog {
    static final String TAG = "PixelAodOPlus";
    private static volatile XposedInterface framework;
    private static volatile boolean debugEnabled;

    private PixelAodLog() {
    }

    static void attach(XposedInterface xposed) {
        framework = xposed;
    }

    static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    static boolean isDebugEnabled() {
        return debugEnabled;
    }

    static void i(String message) {
        Log.i(TAG, message);
        XposedInterface local = framework;
        if (local != null) {
            try {
                local.log(Log.INFO, TAG, message);
            } catch (Throwable ignored) {
            }
        }
    }

    static void log(String message) {
        if (debugEnabled) {
            i(message);
        }
    }

    static void e(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        XposedInterface local = framework;
        if (local != null) {
            try {
                local.log(Log.ERROR, TAG, message, throwable);
            } catch (Throwable ignored) {
            }
        }
    }

    static void log(String message, Throwable throwable) {
        e(message, throwable);
    }
}
