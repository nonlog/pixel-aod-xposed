package dev.codex.pixelaod;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.github.libxposed.api.XposedInterface;

final class PixelAodLog {
    static final String TAG = "PixelAodOPlus";
    private static final long DEBUG_WINDOW_MILLIS = 1_000L;
    private static final int MAX_DEBUG_MESSAGES_PER_WINDOW = 48;
    private static final String[] HOT_PREFIXES = new String[]{
            "clock paint snapshot",
            "Pixel fingerprint carrier state",
            "[FP-PRESSED-A2] dispatch",
            "[FP-PRESSED-A2] applied",
            "AOD schedule cache hit",
            "OOS AOD power policy mapping",
            "AOD power policy allows overlay",
            "AOD policy decision",
            "filtered AOD notification",
            "kept AOD notification",
            "blocked lockscreen policy override",
            "forcing lockscreen silent-notification hide",
            "ClockPlugin lockscreen size",
            "ClockPlugin host sync",
            "[CLOCK-HANDOFF-FRAME]"
    };
    private static final String[] KEY_SEPARATORS =
            new String[]{" source=", " trace=", " state={"};
    private static final DebugLogGate DEBUG_GATE =
            new DebugLogGate(DEBUG_WINDOW_MILLIS, MAX_DEBUG_MESSAGES_PER_WINDOW);
    private static volatile XposedInterface framework;
    private static volatile boolean debugEnabled;

    private PixelAodLog() {
    }

    static void attach(XposedInterface xposed) {
        framework = xposed;
    }

    static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        if (!enabled) {
            DEBUG_GATE.reset();
        }
    }

    static boolean isDebugEnabled() {
        return debugEnabled;
    }

    static void i(String message) {
        emitInfo(message);
    }

    private static void emitInfo(String message) {
        XposedInterface local = framework;
        if (local != null) {
            try {
                local.log(Log.INFO, TAG, message);
                return;
            } catch (Throwable ignored) {
            }
        }
        Log.i(TAG, message);
    }

    static void log(String message) {
        if (!debugEnabled || message == null) {
            return;
        }
        String key = debugKey(message);
        emitDebug(key, minimumIntervalMillis(key), () -> message);
    }

    static void log(String key, Supplier<String> messageSupplier) {
        if (!debugEnabled || messageSupplier == null) {
            return;
        }
        String normalizedKey = key != null && !key.isEmpty() ? key : "lazy-debug";
        emitDebug(normalizedKey, minimumIntervalMillis(normalizedKey), messageSupplier);
    }

    static void log(Supplier<String> messageSupplier) {
        log("lazy-debug", messageSupplier);
    }

    private static void emitDebug(String key, long minimumIntervalMillis,
            Supplier<String> messageSupplier) {
        DebugLogGate.Decision decision = DEBUG_GATE.acquire(
                key, System.nanoTime() / 1_000_000L, minimumIntervalMillis);
        if (!decision.emit) {
            return;
        }
        if (decision.suppressedBefore > 0) {
            emitInfo("debug log pressure bounded suppressed=" + decision.suppressedBefore
                    + " previousWindowMs=" + DEBUG_WINDOW_MILLIS);
        }
        try {
            String message = messageSupplier.get();
            if (message != null) {
                emitInfo(message);
            }
        } catch (Throwable throwable) {
            e("failed to build debug log key=" + key, throwable);
        }
    }

    private static String debugKey(String message) {
        for (String prefix : HOT_PREFIXES) {
            if (message.startsWith(prefix)) {
                return prefix;
            }
        }
        int end = message.length();
        for (String separator : KEY_SEPARATORS) {
            int index = message.indexOf(separator);
            if (index >= 0 && index < end) {
                end = index;
            }
        }
        return message.substring(0, Math.min(end, 96));
    }

    private static long minimumIntervalMillis(String key) {
        if (key.startsWith("clock paint snapshot")
                || key.startsWith("Pixel fingerprint carrier state")
                || key.startsWith("[FP-PRESSED-A2]")) {
            return 200L;
        }
        if (key.startsWith("AOD schedule cache hit")
                || key.startsWith("filtered AOD notification")
                || key.startsWith("kept AOD notification")
                || key.startsWith("blocked lockscreen policy override")
                || key.startsWith("forcing lockscreen silent-notification hide")
                || key.startsWith("ClockPlugin lockscreen size")
                || key.startsWith("ClockPlugin host sync")
                || key.startsWith("handoff-frame-")
                || key.startsWith("[CLOCK-HANDOFF-FRAME]")) {
            return 100L;
        }
        if (key.startsWith("OOS AOD power policy mapping")
                || key.startsWith("AOD power policy allows overlay")
                || key.startsWith("AOD policy decision")) {
            return 50L;
        }
        return 0L;
    }

    static void e(String message, Throwable throwable) {
        XposedInterface local = framework;
        if (local != null) {
            try {
                local.log(Log.ERROR, TAG, message, throwable);
                return;
            } catch (Throwable ignored) {
            }
        }
        Log.e(TAG, message, throwable);
    }

    static void log(String message, Throwable throwable) {
        e(message, throwable);
    }
}

final class DebugLogGate {
    private final long windowMillis;
    private final int maxMessagesPerWindow;
    private final Map<String, Long> lastEmissionByKey = new HashMap<>();
    private long windowStartedAt = Long.MIN_VALUE;
    private int emittedInWindow;
    private int suppressedInWindow;

    DebugLogGate(long windowMillis, int maxMessagesPerWindow) {
        if (windowMillis <= 0L) {
            throw new IllegalArgumentException("windowMillis must be positive");
        }
        if (maxMessagesPerWindow <= 0) {
            throw new IllegalArgumentException("maxMessagesPerWindow must be positive");
        }
        this.windowMillis = windowMillis;
        this.maxMessagesPerWindow = maxMessagesPerWindow;
    }

    synchronized Decision acquire(String key, long nowMillis, long minimumIntervalMillis) {
        int suppressedBefore = 0;
        if (windowStartedAt == Long.MIN_VALUE
                || nowMillis < windowStartedAt
                || nowMillis - windowStartedAt >= windowMillis) {
            suppressedBefore = suppressedInWindow;
            windowStartedAt = nowMillis;
            emittedInWindow = 0;
            suppressedInWindow = 0;
            lastEmissionByKey.clear();
        }

        String normalizedKey = key != null ? key : "";
        Long lastEmission = lastEmissionByKey.get(normalizedKey);
        if (minimumIntervalMillis > 0L && lastEmission != null
                && nowMillis - lastEmission < minimumIntervalMillis) {
            suppressedInWindow++;
            return new Decision(false, suppressedBefore);
        }
        if (emittedInWindow >= maxMessagesPerWindow) {
            suppressedInWindow++;
            return new Decision(false, suppressedBefore);
        }

        emittedInWindow++;
        lastEmissionByKey.put(normalizedKey, nowMillis);
        return new Decision(true, suppressedBefore);
    }

    synchronized void reset() {
        windowStartedAt = Long.MIN_VALUE;
        emittedInWindow = 0;
        suppressedInWindow = 0;
        lastEmissionByKey.clear();
    }

    static final class Decision {
        final boolean emit;
        final int suppressedBefore;

        Decision(boolean emit, int suppressedBefore) {
            this.emit = emit;
            this.suppressedBefore = suppressedBefore;
        }
    }
}
