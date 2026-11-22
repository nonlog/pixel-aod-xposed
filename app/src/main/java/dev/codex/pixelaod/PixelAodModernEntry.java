package dev.codex.pixelaod;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

public final class PixelAodModernEntry extends XposedModule {
    private static final String TAG = "PixelAodModern";
    private static final String SYSTEMUI = "com.android.systemui";
    private static final String MODULE_PACKAGE = "dev.codex.pixelaod";
    private static final AtomicBoolean ATTACH_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean ON_CREATE_HOOKED = new AtomicBoolean(false);
    private static final AtomicBoolean SYSTEMUI_INSTALLED = new AtomicBoolean(false);
    private static final AtomicBoolean TOASTED = new AtomicBoolean(false);

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        try {
            ModernHookBridge.attach(this);
            logInfo("modern module loaded process=" + param.getProcessName()
                    + " sdk=" + Build.VERSION.SDK_INT
                    + " release=" + Build.VERSION.RELEASE
                    + " display=" + Build.DISPLAY
                    + " device=" + Build.DEVICE
                    + " api=" + getApiVersion()
                    + " framework=" + getFrameworkName() + "/" + getFrameworkVersion());
        } catch (Throwable t) {
            logError("failed to initialize PixelAodModernEntry onModuleLoaded", t);
        }
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!SYSTEMUI.equals(param.getPackageName())) {
            return;
        }
        try {
            logInfo("modern package loaded package=" + param.getPackageName()
                    + " loader=" + param.getDefaultClassLoader());
            hookSystemUiBootstrap(param.getDefaultClassLoader(), "onPackageLoaded");
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
            logInfo("modern package ready package=" + param.getPackageName()
                    + " loader=" + param.getClassLoader());
            hookSystemUiBootstrap(param.getClassLoader(), "onPackageReady");
        } catch (Throwable t) {
            logError("failed during modern package ready", t);
        }
    }

    private void hookSystemUiBootstrap(ClassLoader fallbackLoader, String stage) {
        hookApplicationAttach(fallbackLoader, stage);
        hookApplicationOnCreate(fallbackLoader, stage);
    }

    private void hookApplicationAttach(ClassLoader fallbackLoader, String stage) {
        if (!ATTACH_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Context context = (Context) chain.getArg(0);
                        maybeInstallForSystemUi(context, fallbackLoader, "Application.attach");
                        return result;
                    });
            logInfo("hooked Application.attach through modern libxposed API stage=" + stage);
        } catch (Throwable t) {
            ATTACH_HOOKED.set(false);
            logError("failed to hook Application.attach stage=" + stage, t);
        }
    }

    private void hookApplicationOnCreate(ClassLoader fallbackLoader, String stage) {
        if (!ON_CREATE_HOOKED.compareAndSet(false, true)) {
            return;
        }
        try {
            Method onCreate = Application.class.getDeclaredMethod("onCreate");
            hook(onCreate)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .intercept(chain -> {
                        Object result = chain.proceed();
                        Object receiver = chain.getThisObject();
                        if (receiver instanceof Context) {
                            maybeInstallForSystemUi((Context) receiver, fallbackLoader,
                                    "Application.onCreate");
                        }
                        return result;
                    });
            logInfo("hooked Application.onCreate fallback through modern libxposed API stage=" + stage);
        } catch (Throwable t) {
            ON_CREATE_HOOKED.set(false);
            logError("failed to hook Application.onCreate stage=" + stage, t);
        }
    }

    private void maybeInstallForSystemUi(Context context, ClassLoader fallbackLoader, String trigger) {
        if (context == null || !SYSTEMUI.equals(context.getPackageName())) {
            return;
        }
        if (!SYSTEMUI_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        ClassLoader classLoader = context.getClassLoader() != null
                ? context.getClassLoader()
                : fallbackLoader;
        logInfo("modern " + trigger + " package=" + context.getPackageName()
                + " process=" + (Build.VERSION.SDK_INT >= 28
                        ? Application.getProcessName() : context.getApplicationInfo().processName)
                + " sdk=" + Build.VERSION.SDK_INT
                + " display=" + Build.DISPLAY
                + " loader=" + classLoader);
        resolveModulePath(context);
        PixelAodTypography.prewarmGoogleSansFlex(context);
        PixelAodHook.install(context, classLoader);
        showToastOnce(context, "Pixel AOD modern injected: " + context.getPackageName());
    }

    private void resolveModulePath(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(MODULE_PACKAGE, 0);
            PixelAodTypography.setModulePath(info.sourceDir);
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
