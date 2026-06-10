package dev.codex.pixelaod;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class TestNotificationReceiver extends BroadcastReceiver {
    public static final String ACTION_POST = "dev.codex.pixelaod.TEST_NOTIFICATION";
    public static final String ACTION_CLEAR = "dev.codex.pixelaod.CLEAR_TEST_NOTIFICATION";
    static final String TEST_TAG = "pixel_aod_test";
    private static final String CHANNEL_ID = "pixel_aod_test";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        String action = intent.getAction();
        if (ACTION_CLEAR.equals(action)) {
            notificationManager(context).cancel(TEST_TAG, NOTIFICATION_ID);
            return;
        }
        if (!ACTION_POST.equals(action)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            PixelAodLog.log("test notification skipped; POST_NOTIFICATIONS is not granted");
            return;
        }
        NotificationManager manager = notificationManager(context);
        ensureChannel(manager);
        String title = intent.getStringExtra("title");
        String text = intent.getStringExtra("text");
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        Notification notification = builder
                .setSmallIcon(R.drawable.ic_stat_pixel_aod_test)
                .setContentTitle(title != null ? title : "Pixel AOD test")
                .setContentText(text != null ? text : "Lockscreen notification")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setShowWhen(true)
                .setOngoing(false)
                .setAutoCancel(false)
                .build();
        manager.notify(TEST_TAG, NOTIFICATION_ID, notification);
    }

    private static NotificationManager notificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private static void ensureChannel(NotificationManager manager) {
        if (Build.VERSION.SDK_INT < 26 || manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pixel AOD test",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }
}
