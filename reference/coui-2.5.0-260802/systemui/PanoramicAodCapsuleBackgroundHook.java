package one.dot.couiexpressive.hooks.systemui;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.view.View;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import defpackage.aq;
import defpackage.dr;
import defpackage.l22;
import defpackage.l80;
import defpackage.nr;
import defpackage.ph0;
import defpackage.sb0;
import defpackage.sc1;
import defpackage.tc1;
import defpackage.us0;
import defpackage.vb0;
import defpackage.x30;
import defpackage.xm0;
import defpackage.zt;
import one.dot.couiexpressive.ConfigStore;

public final class PanoramicAodCapsuleBackgroundHook implements IXposedHookLoadPackage {
    public static final int $stable = 0;
    private static final String AOD_PROGRESS_FIELD = "coui_expressive_panoramic_aod_progress";
    private static final String CAPSULE_BACKGROUND_VIEW_CLASS = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundView";
    private static final String CAPSULE_EAR_VIEW_CLASS = "com.oplus.systemui.notification.lockscreen.capsule.CapsuleEarView";
    private static final Companion Companion = new Companion(null);
    private static final String SCRIM_CONTROLLER_EX_CLASS = "com.android.systemui.statusbar.phone.ScrimControllerEx";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "PanoramicAodCapsuleBg";

    /* JADX WARN: Type inference failed for: r0v0, types: [one.dot.couiexpressive.hooks.systemui.PanoramicAodCapsuleBackgroundHook$drawHook$1] */
    private final AnonymousClass1 drawHook(final Class<?> cls, final l80 l80Var) {
        return new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                Path path;
                int iQ;
                methodHookParam.getClass();
                PanoramicAodCapsuleBackgroundHook panoramicAodCapsuleBackgroundHook = PanoramicAodCapsuleBackgroundHook.this;
                Class<?> cls2 = cls;
                l80 l80Var2 = l80Var;
                try {
                    Object obj = methodHookParam.thisObject;
                    View view = obj instanceof View ? (View) obj : null;
                    if (view == null) {
                        return;
                    }
                    Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(view, PanoramicAodCapsuleBackgroundHook.AOD_PROGRESS_FIELD);
                    Float f = additionalInstanceField instanceof Float ? (Float) additionalInstanceField : null;
                    if (f != null) {
                        float fFloatValue = f.floatValue();
                        if (fFloatValue > 0.0f && panoramicAodCapsuleBackgroundHook.isThirdTheme(cls2)) {
                            Object[] objArr = methodHookParam.args;
                            objArr.getClass();
                            Object obj2 = objArr.length == 0 ? null : objArr[0];
                            Canvas canvas = obj2 instanceof Canvas ? (Canvas) obj2 : null;
                            if (canvas != null && (path = (Path) l80Var2.invoke(view)) != null && (iQ = nr.q(us0.G(fFloatValue * 255.0f), 0, 255)) != 0) {
                                int iSave = canvas.save();
                                try {
                                    canvas.clipPath(path);
                                    canvas.drawColor(Color.argb(iQ, 0, 0, 0));
                                    canvas.restoreToCount(iSave);
                                    objF = l22.a;
                                } catch (Throwable th) {
                                    canvas.restoreToCount(iSave);
                                    throw th;
                                }
                            }
                            return;
                        }
                        return;
                    }
                    return;
                } catch (Throwable th2) {
                    objF = dr.f(th2);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    sb0 sb0Var = vb0.a;
                    vb0.d(PanoramicAodCapsuleBackgroundHook.TAG, "draw-callback", "failed to apply panoramic AOD capsule background", thA);
                }
            }
        };
    }

    private final void hookCapsuleBackground(XC_LoadPackage.LoadPackageParam loadPackageParam, Class<?> cls) {
        Object objF;
        Class clsFindClassIfExists = XposedHelpers.findClassIfExists(CAPSULE_BACKGROUND_VIEW_CLASS, loadPackageParam.classLoader);
        if (clsFindClassIfExists == null) {
            sb0 sb0Var = vb0.a;
            vb0.j(TAG, "background-view-missing", "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundView not found", null, 0, 56);
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(clsFindClassIfExists, "setScrimAlpha", new Object[]{Float.TYPE, progressHook()});
            XposedBridge.hookAllMethods(clsFindClassIfExists, "onDraw", drawHook(cls, new xm0(21)));
            sb0 sb0Var2 = vb0.a;
            vb0.f(TAG, "background-hook", "com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundView#setScrimAlpha/onDraw hooked");
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            sb0 sb0Var3 = vb0.a;
            vb0.e(sb0.j, TAG, "background-hook", "failed to hook com.oplus.systemui.notification.lockscreen.capsule.CapsuleBackgroundView", thA);
        }
    }

    public static final Path hookCapsuleBackground$lambda$0$0(View view) {
        Object objectField;
        view.getClass();
        Object objCallMethod = XposedHelpers.callMethod(view, "getCapsuleHelper", new Object[0]);
        if (objCallMethod == null || (objectField = XposedHelpers.getObjectField(objCallMethod, "pathProvider")) == null) {
            return null;
        }
        Object objCallMethod2 = XposedHelpers.callMethod(objectField, "getClipPath", new Object[0]);
        if (objCallMethod2 instanceof Path) {
            return (Path) objCallMethod2;
        }
        return null;
    }

    private final void hookCapsuleEars(XC_LoadPackage.LoadPackageParam loadPackageParam, Class<?> cls) {
        Object objF;
        Class clsFindClassIfExists = XposedHelpers.findClassIfExists(CAPSULE_EAR_VIEW_CLASS, loadPackageParam.classLoader);
        if (clsFindClassIfExists == null) {
            sb0 sb0Var = vb0.a;
            vb0.j(TAG, "ear-view-missing", "com.oplus.systemui.notification.lockscreen.capsule.CapsuleEarView not found", null, 0, 56);
            return;
        }
        try {
            XposedHelpers.findAndHookMethod(clsFindClassIfExists, "setScrimAlpha", new Object[]{Float.TYPE, progressHook()});
            XposedBridge.hookAllMethods(clsFindClassIfExists, "onDraw", drawHook(cls, new xm0(22)));
            sb0 sb0Var2 = vb0.a;
            vb0.f(TAG, "ear-hook", "com.oplus.systemui.notification.lockscreen.capsule.CapsuleEarView#setScrimAlpha/onDraw hooked");
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            sb0 sb0Var3 = vb0.a;
            vb0.e(sb0.j, TAG, "ear-hook", "failed to hook com.oplus.systemui.notification.lockscreen.capsule.CapsuleEarView", thA);
        }
    }

    public static final Path hookCapsuleEars$lambda$0$0(View view) {
        view.getClass();
        Object objectField = XposedHelpers.getObjectField(view, "earPath");
        if (objectField instanceof Path) {
            return (Path) objectField;
        }
        return null;
    }

    public final boolean isThirdTheme(Class<?> cls) {
        Object objF;
        try {
            objF = Boolean.valueOf(XposedHelpers.getStaticBooleanField(cls, "isThirdTheme"));
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            sb0 sb0Var = vb0.a;
            vb0.d(TAG, "third-theme-state", "failed to read ScrimControllerEx.isThirdTheme", thA);
        }
        Boolean bool = Boolean.FALSE;
        if (objF instanceof sc1) {
            objF = bool;
        }
        return ((Boolean) objF).booleanValue();
    }

    /* JADX WARN: Type inference failed for: r0v1, types: [one.dot.couiexpressive.hooks.systemui.PanoramicAodCapsuleBackgroundHook$progressHook$1] */
    private final C00551 progressHook() {
        return new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                try {
                    Object obj = methodHookParam.thisObject;
                    View view = obj instanceof View ? (View) obj : null;
                    if (view == null) {
                        return;
                    }
                    Object[] objArr = methodHookParam.args;
                    objArr.getClass();
                    Object obj2 = objArr.length == 0 ? null : objArr[0];
                    Float f = obj2 instanceof Float ? (Float) obj2 : null;
                    if (f == null) {
                        return;
                    }
                    XposedHelpers.setAdditionalInstanceField(view, PanoramicAodCapsuleBackgroundHook.AOD_PROGRESS_FIELD, Float.valueOf(nr.p(f.floatValue(), 0.0f, 1.0f)));
                    view.invalidate();
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    sb0 sb0Var = vb0.a;
                    vb0.d(PanoramicAodCapsuleBackgroundHook.TAG, "progress-callback", "failed to retain panoramic AOD progress", thA);
                }
            }
        };
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        loadPackageParam.getClass();
        if (ph0.i(loadPackageParam.packageName, SYSTEM_UI_PACKAGE) && ph0.i(loadPackageParam.processName, SYSTEM_UI_PACKAGE)) {
            if (!((Boolean) ConfigStore.INSTANCE.load(x30.h(ConfigStore.MODULE_PACKAGE, ConfigStore.PREFS_NAME)).a(aq.L)).booleanValue()) {
                sb0 sb0Var = vb0.a;
                vb0.a(TAG, ConfigStore.PREFS_NAME, "disabled");
                return;
            }
            Class<?> clsFindClassIfExists = XposedHelpers.findClassIfExists(SCRIM_CONTROLLER_EX_CLASS, loadPackageParam.classLoader);
            if (clsFindClassIfExists == null) {
                sb0 sb0Var2 = vb0.a;
                vb0.j(TAG, "third-theme-state-missing", "com.android.systemui.statusbar.phone.ScrimControllerEx not found", null, 0, 56);
            } else {
                sb0 sb0Var3 = vb0.a;
                vb0.f(TAG, "install", "enabled; applying black background only for third-party themes during panoramic AOD");
                hookCapsuleBackground(loadPackageParam, clsFindClassIfExists);
                hookCapsuleEars(loadPackageParam, clsFindClassIfExists);
            }
        }
    }

    public static final class Companion {
        public Companion(zt ztVar) {
            this();
        }

        private Companion() {
        }
    }
}
