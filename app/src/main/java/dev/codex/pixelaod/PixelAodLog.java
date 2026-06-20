package dev.codex.pixelaod;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;

final class PixelAodLog {
    static final String TAG = "PixelAodOPlus";
    private static final int DEBUG_LOGS_PER_WINDOW = 240;
    private static final long DEBUG_LOG_WINDOW_MS = 60_000L;
    private static volatile XposedInterface framework;
    private static volatile boolean debugEnabled;
    private static long debugLogWindowStart;
    private static int debugLogCount;
    private static boolean debugLogSuppressed;

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
        if (debugEnabled && shouldEmitDebugLog()) {
            i(message);
        }
    }

    private static boolean shouldEmitDebugLog() {
        long now = android.os.SystemClock.uptimeMillis();
        synchronized (PixelAodLog.class) {
            if (debugLogWindowStart <= 0L || now - debugLogWindowStart >= DEBUG_LOG_WINDOW_MS) {
                debugLogWindowStart = now;
                debugLogCount = 0;
                debugLogSuppressed = false;
            }
            if (debugLogCount < DEBUG_LOGS_PER_WINDOW) {
                debugLogCount++;
                return true;
            }
            if (!debugLogSuppressed) {
                debugLogSuppressed = true;
                i("debug log budget exhausted; suppressing further debug logs for this window");
            }
            return false;
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
