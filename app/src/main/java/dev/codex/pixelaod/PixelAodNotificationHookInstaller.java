package dev.codex.pixelaod;

import android.content.Context;

/** Owns notification semantic/icon/policy hook registration. */
final class PixelAodNotificationHookInstaller {
    private PixelAodNotificationHookInstaller() {
    }

    static void installBaseViewHooks(ClassLoader classLoader) {
        PixelAodHook.hookNotificationView(classLoader);
    }

    static void installNotificationContent(Context context, ClassLoader classLoader,
            boolean notificationIcons) {
        if (!notificationIcons) {
            return;
        }
        PixelAodHook.hookNotificationListenerService();
        PixelAodHook.hookSystemUiNotificationListener(classLoader);
        PixelAodHook.hookStatusBarNotificationIconCapture(classLoader);
        PixelAodHook.hookOplusNotificationCapsuleIcons(classLoader);
        PixelAodHook.registerTorchStateCallback(context);
        PixelAodHook.registerTorchRefreshReceiver(context);
    }

    static void installLockscreenPolicy(ClassLoader classLoader, boolean lockscreenPolicy) {
        if (lockscreenPolicy) {
            PixelAodHook.hookLockscreenNotificationPolicy(classLoader);
        }
    }
}
