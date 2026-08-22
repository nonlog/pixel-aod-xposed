package dev.codex.pixelaod;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Fetches the module-owned calendar summary off the SystemUI main thread. */
final class CalendarAtAGlanceClient {
    private static final long REFRESH_INTERVAL_MILLIS = 60_000L;
    private static final long BOUNDARY_REFRESH_SLOP_MILLIS = 100L;
    private static final long MIN_BOUNDARY_REFRESH_DELAY_MILLIS = 250L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService QUERY_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "PixelAodCalendar");
        thread.setDaemon(true);
        return thread;
    });
    private static final Object LOCK = new Object();

    private static boolean queryInFlight;
    private static boolean immediateRefreshPending;
    private static long lastQueryAt;
    private static String lastPublishedExtra = "";
    private static boolean providerObserverRegistered;
    private static Context applicationContext;
    private static Runnable boundaryRefreshRunnable;
    private static long scheduledBoundaryAtMillis;
    private static final ContentObserver PROVIDER_OBSERVER = new ContentObserver(MAIN_HANDLER) {
        @Override
        public void onChange(boolean selfChange) {
            requestRefresh(applicationContext, "calendar-provider-change", true);
        }
    };

    private CalendarAtAGlanceClient() {
    }

    static void maybeRefresh(Context context, String source) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        applicationContext = appContext != null ? appContext : context;
        if (!PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_CALENDAR_EVENTS, false)) {
            cancelBoundaryRefresh();
            publish("", source + "#disabled");
            return;
        }
        ensureProviderObserver(applicationContext);
        requestRefresh(applicationContext, source, false);
    }

    private static void ensureProviderObserver(Context context) {
        if (context == null || providerObserverRegistered) {
            return;
        }
        synchronized (LOCK) {
            if (providerObserverRegistered) {
                return;
            }
            try {
                context.getContentResolver().registerContentObserver(
                        CalendarAtAGlanceProvider.URI, false, PROVIDER_OBSERVER);
                providerObserverRegistered = true;
            } catch (Throwable t) {
                PixelAodLog.log("Calendar At a Glance provider observer registration failed", t);
            }
        }
    }

    private static void requestRefresh(Context context, String source, boolean immediate) {
        if (context == null) {
            return;
        }
        if (!PixelAodSettings.getBoolean(context, PixelAodSettings.KEY_CALENDAR_EVENTS, false)) {
            cancelBoundaryRefresh();
            publish("", source + "#disabled");
            return;
        }
        long now = SystemClock.elapsedRealtime();
        synchronized (LOCK) {
            if (queryInFlight) {
                immediateRefreshPending |= immediate;
                return;
            }
            if (!immediate && now - lastQueryAt < REFRESH_INTERVAL_MILLIS) {
                return;
            }
            queryInFlight = true;
            lastQueryAt = now;
        }
        QUERY_EXECUTOR.execute(() -> {
            CalendarSnapshot snapshot = queryNextEvent(context);
            boolean runPendingRefresh;
            synchronized (LOCK) {
                queryInFlight = false;
                runPendingRefresh = immediateRefreshPending;
                immediateRefreshPending = false;
            }
            publish(snapshot, source);
            if (runPendingRefresh) {
                requestRefresh(context, source + "#coalesced", true);
            }
        });
    }

    private static CalendarSnapshot queryNextEvent(Context context) {
        try (Cursor cursor = context.getContentResolver().query(CalendarAtAGlanceProvider.URI,
                null, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                return CalendarSnapshot.EMPTY;
            }
            int textIndex = cursor.getColumnIndex(CalendarAtAGlanceProvider.COLUMN_DISPLAY_TEXT);
            int refreshIndex = cursor.getColumnIndex(
                    CalendarAtAGlanceProvider.COLUMN_NEXT_REFRESH_AT_MILLIS);
            String extra = textIndex >= 0 ? PixelAodRenderModel.normalizeAtAGlanceExtra(
                    cursor.getString(textIndex)) : "";
            long refreshAtMillis = refreshIndex >= 0 && !cursor.isNull(refreshIndex)
                    ? cursor.getLong(refreshIndex) : 0L;
            return new CalendarSnapshot(extra, refreshAtMillis);
        } catch (Throwable t) {
            PixelAodLog.log("Calendar At a Glance provider unavailable", t);
            return CalendarSnapshot.EMPTY;
        }
    }

    private static void publish(String extra, String source) {
        publish(new CalendarSnapshot(extra, 0L), source);
    }

    private static void publish(CalendarSnapshot snapshot, String source) {
        String normalized = PixelAodRenderModel.normalizeAtAGlanceExtra(snapshot.displayText);
        MAIN_HANDLER.post(() -> {
            boolean changed;
            synchronized (LOCK) {
                changed = !TextUtils.equals(lastPublishedExtra, normalized);
                lastPublishedExtra = normalized;
            }
            scheduleBoundaryRefresh(snapshot.nextRefreshAtMillis, source);
            if (changed) {
                PixelAodContentState.setCalendarAtAGlanceExtra(normalized, source);
            }
        });
    }

    private static void scheduleBoundaryRefresh(long refreshAtMillis, String source) {
        if (refreshAtMillis <= 0L || applicationContext == null) {
            cancelBoundaryRefresh();
            return;
        }
        long delayMillis = Math.max(MIN_BOUNDARY_REFRESH_DELAY_MILLIS,
                refreshAtMillis - System.currentTimeMillis() + BOUNDARY_REFRESH_SLOP_MILLIS);
        Runnable refresh = () -> requestRefresh(applicationContext,
                source + "#calendar-boundary", true);
        Runnable previous;
        synchronized (LOCK) {
            if (boundaryRefreshRunnable != null
                    && scheduledBoundaryAtMillis == refreshAtMillis) {
                return;
            }
            previous = boundaryRefreshRunnable;
            boundaryRefreshRunnable = refresh;
            scheduledBoundaryAtMillis = refreshAtMillis;
        }
        if (previous != null) {
            MAIN_HANDLER.removeCallbacks(previous);
        }
        MAIN_HANDLER.postDelayed(refresh, delayMillis);
        PixelAodLog.log("scheduled Calendar At a Glance boundary refresh source=" + source
                + " at=" + refreshAtMillis + " delayMs=" + delayMillis);
    }

    private static void cancelBoundaryRefresh() {
        Runnable previous;
        synchronized (LOCK) {
            previous = boundaryRefreshRunnable;
            boundaryRefreshRunnable = null;
            scheduledBoundaryAtMillis = 0L;
        }
        if (previous != null) {
            MAIN_HANDLER.removeCallbacks(previous);
        }
    }

    private static final class CalendarSnapshot {
        static final CalendarSnapshot EMPTY = new CalendarSnapshot("", 0L);

        final String displayText;
        final long nextRefreshAtMillis;

        CalendarSnapshot(String displayText, long nextRefreshAtMillis) {
            this.displayText = displayText;
            this.nextRefreshAtMillis = nextRefreshAtMillis;
        }
    }
}
