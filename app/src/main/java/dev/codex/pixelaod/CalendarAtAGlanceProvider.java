package dev.codex.pixelaod;

import android.Manifest;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.text.TextUtils;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;

/**
 * Runs in the module app process so the SystemUI hook never needs calendar permission.
 * The exported surface exposes an already-filtered display string and the next time it changes.
 */
public final class CalendarAtAGlanceProvider extends ContentProvider {
    static final String AUTHORITY = "dev.codex.pixelaod.calendar";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/next_event");
    static final String COLUMN_DISPLAY_TEXT = "display_text";
    static final String COLUMN_NEXT_REFRESH_AT_MILLIS = "next_refresh_at_millis";

    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final long LOOK_AHEAD_MILLIS = 24L * 60L * 60L * 1000L;
    private static final String[] PROJECTION = {
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.START_DAY,
            CalendarContract.Instances.END_DAY,
            CalendarContract.Instances.STATUS
    };

    private final Object observerLock = new Object();
    private ContentObserver calendarObserver;
    private boolean calendarObserverRegistered;

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        MatrixCursor cursor = new MatrixCursor(new String[]{
                COLUMN_DISPLAY_TEXT,
                COLUMN_NEXT_REFRESH_AT_MILLIS
        });
        Context context = getContext();
        if (context == null || !isTrustedCaller(context) || !isCalendarEnabled(context)
                || !hasCalendarPermission(context)) {
            return cursor;
        }
        ensureCalendarObserver(context);
        CalendarSnapshot nextEvent = findNextEvent(context, System.currentTimeMillis());
        if (!TextUtils.isEmpty(nextEvent.displayText)) {
            cursor.addRow(new Object[]{nextEvent.displayText, nextEvent.nextRefreshAtMillis});
        }
        return cursor;
    }

    @Override
    public void shutdown() {
        Context context = getContext();
        synchronized (observerLock) {
            if (calendarObserverRegistered && context != null) {
                context.getContentResolver().unregisterContentObserver(calendarObserver);
            }
            calendarObserverRegistered = false;
            calendarObserver = null;
        }
        super.shutdown();
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.dev.codex.pixelaod.calendar_event";
    }

    @Override
    public Uri insert(Uri uri, android.content.ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, android.content.ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }

    private static boolean isCalendarEnabled(Context context) {
        return PixelAodSettings.getSharedPreferences(context).getBoolean(
                PixelAodSettings.KEY_CALENDAR_EVENTS,
                PixelAodSettings.defaultBoolean(PixelAodSettings.KEY_CALENDAR_EVENTS, false));
    }

    private boolean isTrustedCaller(Context context) {
        String callingPackage = getCallingPackage();
        return SYSTEM_UI_PACKAGE.equals(callingPackage)
                || context.getPackageName().equals(callingPackage);
    }

    private static boolean hasCalendarPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureCalendarObserver(Context context) {
        synchronized (observerLock) {
            if (calendarObserverRegistered || !hasCalendarPermission(context)) {
                return;
            }
            calendarObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, Uri changedUri) {
                    PixelAodLog.log("Calendar At a Glance source changed uri=" + changedUri);
                    Context providerContext = getContext();
                    if (providerContext != null) {
                        providerContext.getContentResolver().notifyChange(URI, null);
                    }
                }
            };
            try {
                context.getContentResolver().registerContentObserver(
                        CalendarContract.Events.CONTENT_URI, true, calendarObserver);
                context.getContentResolver().registerContentObserver(
                        CalendarContract.Calendars.CONTENT_URI, true, calendarObserver);
                calendarObserverRegistered = true;
            } catch (Throwable t) {
                ContentObserver failedObserver = calendarObserver;
                calendarObserver = null;
                if (failedObserver != null) {
                    try {
                        context.getContentResolver().unregisterContentObserver(failedObserver);
                    } catch (Throwable ignored) {
                        // Registration can fail before either URI accepts the observer.
                    }
                }
                PixelAodLog.log("Calendar At a Glance observer registration failed", t);
            }
        }
    }

    private static CalendarSnapshot findNextEvent(Context context, long nowMillis) {
        long lookAheadMillis = nowMillis + LOOK_AHEAD_MILLIS;
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, nowMillis);
        ContentUris.appendId(builder, lookAheadMillis);

        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(nowMillis);
        int todayJulianDay = julianDay(today);
        String allDayEvent = "";
        try (Cursor cursor = context.getContentResolver().query(builder.build(), PROJECTION,
                null, null, CalendarContract.Instances.BEGIN + " ASC")) {
            if (cursor == null) {
                return new CalendarSnapshot("", 0L);
            }
            int titleIndex = cursor.getColumnIndex(CalendarContract.Instances.TITLE);
            int beginIndex = cursor.getColumnIndex(CalendarContract.Instances.BEGIN);
            int allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY);
            int startDayIndex = cursor.getColumnIndex(CalendarContract.Instances.START_DAY);
            int endDayIndex = cursor.getColumnIndex(CalendarContract.Instances.END_DAY);
            int statusIndex = cursor.getColumnIndex(CalendarContract.Instances.STATUS);
            while (cursor.moveToNext()) {
                if (statusIndex >= 0 && cursor.getInt(statusIndex)
                        == CalendarContract.Events.STATUS_CANCELED) {
                    continue;
                }
                String title = normalizeTitle(titleIndex >= 0 ? cursor.getString(titleIndex) : null);
                if (TextUtils.isEmpty(title) || beginIndex < 0) {
                    continue;
                }
                boolean allDay = allDayIndex >= 0 && cursor.getInt(allDayIndex) != 0;
                if (allDay) {
                    boolean isToday = startDayIndex >= 0 && endDayIndex >= 0
                            && cursor.getInt(startDayIndex) <= todayJulianDay
                            && cursor.getInt(endDayIndex) >= todayJulianDay;
                    if (isToday && TextUtils.isEmpty(allDayEvent)) {
                        allDayEvent = formatAllDayEvent(context, title);
                    }
                    continue;
                }
                long beginMillis = cursor.getLong(beginIndex);
                if (beginMillis >= nowMillis && beginMillis < lookAheadMillis) {
                    return new CalendarSnapshot(formatTimedEvent(context, beginMillis, title),
                            beginMillis);
                }
            }
        } catch (SecurityException e) {
            PixelAodLog.log("Calendar At a Glance query denied", e);
        } catch (Throwable t) {
            PixelAodLog.log("Calendar At a Glance query failed", t);
        }
        return new CalendarSnapshot(allDayEvent,
                TextUtils.isEmpty(allDayEvent) ? 0L : nextLocalMidnightMillis(today));
    }

    private static long nextLocalMidnightMillis(Calendar now) {
        Calendar nextMidnight = (Calendar) now.clone();
        nextMidnight.add(Calendar.DAY_OF_YEAR, 1);
        nextMidnight.set(Calendar.HOUR_OF_DAY, 0);
        nextMidnight.set(Calendar.MINUTE, 0);
        nextMidnight.set(Calendar.SECOND, 0);
        nextMidnight.set(Calendar.MILLISECOND, 0);
        return nextMidnight.getTimeInMillis();
    }

    private static int julianDay(Calendar calendar) {
        long millis = calendar.getTimeInMillis();
        int offsetMillis = calendar.getTimeZone().getOffset(millis);
        return (int) ((millis + offsetMillis) / 86_400_000L) + 2_440_588;
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String formatTimedEvent(Context context, long beginMillis, String title) {
        String time = DateFormat.getTimeFormat(context).format(new Date(beginMillis));
        return PixelAodRenderModel.normalizeAtAGlanceExtra(time + " " + title);
    }

    private static String formatAllDayEvent(Context context, String title) {
        return PixelAodRenderModel.normalizeAtAGlanceExtra(
                context.getString(R.string.calendar_all_day) + " " + title);
    }

    private static final class CalendarSnapshot {
        final String displayText;
        final long nextRefreshAtMillis;

        CalendarSnapshot(String displayText, long nextRefreshAtMillis) {
            this.displayText = displayText;
            this.nextRefreshAtMillis = nextRefreshAtMillis;
        }
    }
}
