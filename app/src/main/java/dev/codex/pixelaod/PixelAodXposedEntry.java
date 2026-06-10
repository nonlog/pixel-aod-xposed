package dev.codex.pixelaod;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class PixelAodXposedEntry implements IXposedHookLoadPackage {
    static final String TAG = "PixelAodOPlus";
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final AtomicBoolean ATTACH_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean TOASTED = new AtomicBoolean(false);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SYSTEMUI.equals(lpparam.packageName)) {
            return;
        }

        log("loaded package=" + lpparam.packageName + " process=" + lpparam.processName);
        if (!ATTACH_HOOKED.compareAndSet(false, true)) {
            return;
        }

        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                Context context = (Context) param.args[0];
                ClassLoader classLoader = context.getClassLoader();
                log("Application.attach package=" + context.getPackageName()
                        + " process=" + Application.getProcessName()
                        + " loader=" + classLoader);
                resolveModulePath(context);
                PixelAodHook.install(context, classLoader);
                showToastOnce(context, "Pixel AOD module injected: " + context.getPackageName());
            }
        });
    }

    private static void resolveModulePath(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
            PixelAodClockView.setModulePath(info.sourceDir);
            log("modulePath=" + info.sourceDir);
        } catch (Throwable t) {
            log("failed to resolve module APK path", t);
        }
    }

    static void log(String message) {
        Log.i(TAG, message);
        XposedBridge.log(TAG + ": " + message);
    }

    static void log(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        XposedBridge.log(TAG + ": " + message + "\n" + Log.getStackTraceString(throwable));
    }

    private static void showToastOnce(Context context, String message) {
        if (!TOASTED.compareAndSet(false, true)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Context toastContext = appContext != null ? appContext : context;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(toastContext, message, Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                log("toast proof failed", t);
            }
        });
    }
}
