package dev.codex.pixelaod;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

final class PowerSavingAodController {
    private static final String CLOCK_LAYOUT = "com.oplus.systemui.aod.aodclock.off.AodClockLayout";
    private static final String UPDATE_MANAGER = "com.oplus.systemui.aod.aodclock.off.AodUpdateManager";
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ThreadLocal<Boolean> NATIVE_HIDE_BYPASS = new ThreadLocal<>();
    private static volatile Context appContext;
    private static volatile boolean plugged;
    private static volatile boolean runtimeHoldAllowed;
    private static volatile WeakReference<Object> lastClockLayout = new WeakReference<>(null);
    private static volatile WeakReference<Object> lastUpdateManager = new WeakReference<>(null);
    private PowerSavingAodController() {}

    static void install(Context context, ClassLoader loader) {
        if (context == null || loader == null || !INSTALLED.compareAndSet(false, true)) return;
        appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        hookClockLayout(loader); hookUpdateManager(loader); registerBatteryReceiver(appContext);
        onPolicyChanged("install");
    }
    static boolean isPlugged() { return plugged; }
    static boolean isChargingHoldRequested(Context context) {
        Context ctx = context != null ? context : appContext;
        if (ctx == null) return false;
        NativeAodAvailabilityAdapter.Decision nativeAod = NativeAodAvailabilityAdapter.read(
                ctx, PixelAodClockView.isVendorAmbientSessionActive());
        return PowerSavingAodPolicy.isChargingHoldRequested(
                PixelAodSettings.getBoolean(ctx, PixelAodSettings.KEY_POWER_SAVING_CHARGING_AOD, true),
                nativeAod.displayMode, nativeAod.configuredEligible, plugged);
    }
    static void onPolicyChanged(String source) { runOnMain(() -> reconcile(source)); }

    static void requestNotificationShow(String source) {
        runOnMain(() -> {
            Context ctx = appContext;
            if (ctx == null || isChargingHoldRequested(ctx)) {
                return;
            }
            NativeAodAvailabilityAdapter.Decision nativeAod = NativeAodAvailabilityAdapter.read(
                    ctx, PixelAodClockView.isVendorAmbientSessionActive());
            if (!PowerSavingAodPolicy.isEnergySavingMode(nativeAod.displayMode)
                    || !nativeAod.configuredEligible
                    || PixelAodClockView.isDeviceInteractive(ctx)) {
                return;
            }
            showNative("notification#" + source);
        });
    }

    static boolean shouldSuppressNativeEnergySavingHide(Context context, String source) {
        Context ctx = context != null ? context : appContext;
        return ctx != null && PixelAodClockView.shouldKeepPowerSavingAodVisibleForCharging(
                ctx, "energy-saving-hide#" + source);
    }

    private static void hookClockLayout(ClassLoader loader) {
        try {
            Class<?> c=ModernHookBridge.findClass(CLOCK_LAYOUT, loader);
            ModernHookBridge.hookAfter(c,"onAttachedToWindow",p->{ lastClockLayout=new WeakReference<>(p.thisObject); reconcile("clock-layout-attach"); });
            ModernHookBridge.hookAfter(c,"showClock",p-> lastClockLayout=new WeakReference<>(p.thisObject),int.class);
        } catch(Throwable t){ PixelAodLog.log("failed Power Saving AOD clock hooks",t); }
    }
    private static void hookUpdateManager(ClassLoader loader) {
        try {
            Class<?> c=ModernHookBridge.findClass(UPDATE_MANAGER, loader);
            ModernHookBridge.hookBefore(c,"setHideAlarm",p->{
                lastUpdateManager=new WeakReference<>(p.thisObject);
                if (Boolean.TRUE.equals(NATIVE_HIDE_BYPASS.get())) return;
                Context ctx=appContext;
                if(ctx!=null && PixelAodClockView.shouldKeepPowerSavingAodVisibleForCharging(ctx,"setHideAlarm")){
                    runtimeHoldAllowed=true; p.setResult(null);
                    PixelAodLog.i("suppressed native Power Saving AOD hide alarm while charging");
                }
            });
        } catch(Throwable t){ PixelAodLog.log("failed Power Saving AOD hide hook",t); }
    }
    private static void registerBatteryReceiver(Context context) {
        BroadcastReceiver r=new BroadcastReceiver(){ @Override public void onReceive(Context c,Intent i){ handleBattery(i,"battery"); }};
        try {
            IntentFilter f=new IntentFilter(Intent.ACTION_BATTERY_CHANGED); Intent sticky;
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU) sticky=context.registerReceiver(r,f,Context.RECEIVER_NOT_EXPORTED);
            else sticky=context.registerReceiver(r,f);
            if(sticky!=null) handleBattery(sticky,"battery-sticky");
        } catch(Throwable t){ PixelAodLog.log("failed Power Saving AOD battery observer",t); }
    }
    private static void handleBattery(Intent intent,String source) {
        if(intent==null)return; boolean next=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0)!=0;
        if(plugged!=next) PixelAodLog.i("Power Saving AOD charging edge plugged="+next+" source="+source);
        plugged=next; reconcile(source);
    }
    private static void reconcile(String source) {
        Context ctx=appContext; if(ctx==null)return; boolean previous=runtimeHoldAllowed;
        boolean hold=PixelAodClockView.shouldKeepPowerSavingAodVisibleForCharging(ctx,"reconcile#"+source);
        runtimeHoldAllowed=hold;
        if(hold){ showNative(source); PixelAodClockView.refreshNativeAodEligibility("charging-hold#"+source); return; }
        NativeAodAvailabilityAdapter.Decision nativeAod=NativeAodAvailabilityAdapter.read(ctx,PixelAodClockView.isVendorAmbientSessionActive());
        if(PowerSavingAodPolicy.shouldReapplyNativeHide(previous,false,nativeAod.displayMode,nativeAod.configuredEligible,PixelAodClockView.isDeviceInteractive(ctx))) reapplyNativeHide(source);
    }
    private static void showNative(String source) {
        Object o=lastClockLayout.get(); if(o==null){ PixelAodLog.log("Power Saving charging AOD waiting for native clock layout"); return; }
        try { ModernHookBridge.callMethod(o,"showClock",0); PixelAodLog.i("requested native Power Saving AOD show source="+source); }
        catch(Throwable t){ PixelAodLog.log("failed native Power Saving AOD show",t); }
    }
    private static void reapplyNativeHide(String source) {
        Object o=lastUpdateManager.get(); if(o==null){ PixelAodLog.log("Power Saving native hide restore deferred"); return; }
        try { NATIVE_HIDE_BYPASS.set(Boolean.TRUE); ModernHookBridge.callMethod(o,"setHideAlarm"); PixelAodLog.i("restored native Power Saving AOD hide source="+source); }
        catch(Throwable t){ PixelAodLog.log("failed native Power Saving AOD hide restore",t); }
        finally { NATIVE_HIDE_BYPASS.remove(); }
    }
    private static void runOnMain(Runnable r){ if(Looper.myLooper()==Looper.getMainLooper())r.run(); else MAIN.post(r); }
}
