package dev.codex.pixelaod;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import io.github.kyuubiran.ezxhelper.xposed.EzXposed;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public final class PixelAodModernEntry extends XposedModule {
    private static final String TAG = "PixelAodModern";
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final AtomicBoolean ATTACH_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean TOASTED = new AtomicBoolean(false);

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        try {
            ModernHookBridge.attach(this);
            EzXposed.initOnModuleLoaded(this, param);
            logInfo("modern module loaded process=" + param.getProcessName()
                    + " api=" + getApiVersion()
                    + " framework=" + getFrameworkName() + "/" + getFrameworkVersion());
        } catch (Throwable t) {
            logError("failed to initialize EzXHelper onModuleLoaded", t);
        }
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!SYSTEMUI.equals(param.getPackageName())) {
            return;
        }
        try {
            EzXposed.initOnPackageLoaded(param);
            logInfo("modern package loaded package=" + param.getPackageName()
                    + " loader=" + param.getDefaultClassLoader());
            hookApplicationAttach();
        } catch (Throwable t) {
            logError("failed during modern package load", t);
        }
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        if (!SYSTEMUI.equals(param.getPackageName())) {
            return;
        }
        try {
            EzXposed.initOnPackageReady(param);
            logInfo("modern package ready package=" + param.getPackageName()
                    + " loader=" + param.getClassLoader());
        } catch (Throwable t) {
            logError("failed during modern package ready", t);
        }
    }

    private void hookApplicationAttach() throws NoSuchMethodException {
        if (!ATTACH_HOOKED.compareAndSet(false, true)) {
            return;
        }
        Method attach = Application.class.getDeclaredMethod("attach", Context.class);
        hook(attach)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(chain -> {
                    Object result = chain.proceed();
                    Context context = (Context) chain.getArg(0);
                    if (context == null || !SYSTEMUI.equals(context.getPackageName())) {
                        return result;
                    }
                    ClassLoader classLoader = context.getClassLoader();
                    logInfo("modern Application.attach package=" + context.getPackageName()
                            + " process=" + Application.getProcessName()
                            + " loader=" + classLoader);
                    resolveModulePath(context);
                    PixelAodClockView.prewarmGoogleSansFlex(context);
                    PixelAodHook.install(context, classLoader);
                    showToastOnce(context, "Pixel AOD modern injected: " + context.getPackageName());
                    return result;
                });
        logInfo("hooked Application.attach through modern libxposed API");
    }

    private void resolveModulePath(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
            PixelAodClockView.setModulePath(info.sourceDir);
            logInfo("modulePath=" + info.sourceDir);
        } catch (Throwable t) {
            logError("failed to resolve module APK path", t);
        }
    }

    private void showToastOnce(Context context, String message) {
        if (!TOASTED.compareAndSet(false, true)) {
            return;
        }
        Context appContext = context.getApplicationContext();
        Context toastContext = appContext != null ? appContext : context;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(toastContext, message, Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                logError("toast proof failed", t);
            }
        });
    }

    private void logInfo(String message) {
        Log.i(TAG, message);
        try {
            log(Log.INFO, TAG, message);
        } catch (Throwable ignored) {
        }
    }

    private void logError(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        try {
            log(Log.ERROR, TAG, message, throwable);
        } catch (Throwable ignored) {
        }
    }
}
