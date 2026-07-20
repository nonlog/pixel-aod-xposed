package dev.codex.pixelaod;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.widget.ImageView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

final class PixelFingerprintIconController {
    private static final Map<ImageView, Drawable> ORIGINAL_DRAWABLES = new WeakHashMap<>();
    private static final Map<ImageView, PixelFingerprintDrawable> PIXEL_DRAWABLES =
            new WeakHashMap<>();
    private static final Map<ImageView, Runnable> PENDING_REFRESHES = new WeakHashMap<>();
    private static final Map<ImageView, Runnable> PENDING_RECLAIMS = new WeakHashMap<>();
    private static final Map<Object, Runnable> PENDING_DISCOVERY = new WeakHashMap<>();
    private static final Map<Object, PendingRequest> PENDING_REQUESTS = new WeakHashMap<>();
    private static final Map<Class<?>, Boolean> VENDOR_VIEW_HOOKS = new WeakHashMap<>();
    private static final Map<ImageView, WeakReference<Object>> VIEW_OWNERS = new WeakHashMap<>();
    private static final Map<ImageView, Boolean> TRACKED_VIEWS = new WeakHashMap<>();
    private static final Map<ImageView, String> LAST_LOGGED_STATES = new WeakHashMap<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long VENDOR_RECLAIM_DELAY_MS = 48L;
    private static final long VENDOR_RECLAIM_SECOND_PASS_DELAY_MS = 160L;
    private static final long[] DISCOVERY_RETRY_DELAYS_MS = {
            0L, 16L, 48L, 120L, 250L, 500L, 1000L, 1800L
    };
    private static volatile boolean imageViewHooksInstalled;
    private static final ThreadLocal<Integer> INTERNAL_MUTATION_DEPTH =
            new ThreadLocal<Integer>() {
                @Override
                protected Integer initialValue() {
                    return 0;
                }
            };
    private static volatile WeakReference<Object> lastUiMech = new WeakReference<>(null);

    private static final class PendingRequest {
        final WeakReference<Object> target;
        Context context;
        String source;
        boolean animate;
        Runnable runnable;

        PendingRequest(Context context, Object target, String source, boolean animate) {
            this.context = context;
            this.target = new WeakReference<>(target);
            this.source = source;
            this.animate = animate;
        }
    }

    private PixelFingerprintIconController() {
    }

    static void refresh(Context context, Object uiMech, String source, boolean animate) {
        if (uiMech == null) {
            return;
        }
        lastUiMech = new WeakReference<>(uiMech);
        requestVisualState(context, uiMech, source, animate);
    }

    private static void requestVisualState(Context context, Object uiMech,
            String source, boolean animate) {
        PendingRequest request;
        boolean schedule;
        synchronized (PENDING_REQUESTS) {
            request = PENDING_REQUESTS.get(uiMech);
            if (request == null) {
                request = new PendingRequest(context, uiMech, source, animate);
                PENDING_REQUESTS.put(uiMech, request);
                schedule = true;
            } else {
                if (context != null) {
                    request.context = context;
                }
                request.source = source;
                request.animate |= animate;
                schedule = false;
            }
        }
        if (!schedule) {
            return;
        }
        final PendingRequest pending = request;
        pending.runnable = () -> {
            synchronized (PENDING_REQUESTS) {
                if (PENDING_REQUESTS.get(uiMech) == pending) {
                    PENDING_REQUESTS.remove(uiMech);
                }
            }
            Object target = pending.target.get();
            if (target != null) {
                dispatchRefresh(pending.context, target, pending.source,
                        pending.animate, true);
            }
        };
        ImageView anchor = findFingerprintIcon(uiMech);
        if (PixelFingerprintIconPolicy.dispatchTarget(anchor != null)
                == PixelFingerprintIconPolicy.DispatchTarget.VIEW_HANDLER) {
            anchor.postOnAnimation(pending.runnable);
        } else {
            MAIN.post(pending.runnable);
        }
    }

    private static void dispatchRefresh(Context context, Object uiMech,
            String source, boolean animate, boolean scheduleReclaim) {
        ImageView anchor = findFingerprintIcon(uiMech);
        if (anchor != null) {
            anchor.postOnAnimation(() -> refreshOnViewThread(context, uiMech, source,
                    animate, scheduleReclaim));
            return;
        }
        scheduleDiscovery(context, uiMech, source, animate);
    }

    private static void refreshOnViewThread(Context context, Object uiMech,
            String source, boolean animate, boolean scheduleReclaim) {
        ImageView iconView = findFingerprintIcon(uiMech);
        ImageView pressedIcon = findFingerprintPressedIcon(uiMech);
        if (iconView == null) {
            restoreOriginalDrawable(pressedIcon, source + "#pressed");
            PixelAodLog.log("Pixel fingerprint icon refresh skipped source=" + source
                    + " reason=primary-fpIcon-unavailable");
            scheduleDiscovery(context, uiMech, source, animate);
            return;
        }
        cancelDiscovery(uiMech);
        Context resolvedContext = context != null
                ? context : iconView.getContext();
        boolean enabled = resolvedContext != null && PixelAodSettings.getBoolean(
                resolvedContext, PixelAodSettings.KEY_PIXEL_FINGERPRINT_ICON, false);
        if (!enabled) {
            restoreOriginalDrawable(iconView, source);
            if (pressedIcon != null && pressedIcon != iconView) {
                restoreOriginalDrawable(pressedIcon, source + "#pressed");
            }
            return;
        }

        boolean onDozeState = readBooleanField(uiMech, "onDozeState");
        boolean onDreamingStart = readBooleanField(uiMech, "onDreamingStart");
        boolean screenTurnedOff = readBooleanField(uiMech, "screenTurnedOff");
        boolean interactive = isInteractive(resolvedContext);
        boolean aodStyle = PixelFingerprintIconPolicy.useAodStyle(
                interactive, onDozeState, onDreamingStart, screenTurnedOff);
        boolean dark = isDarkMode(resolvedContext);

        if (pressedIcon != null && pressedIcon != iconView) {
            restoreOriginalDrawable(pressedIcon, source + "#pressed-native");
        }
        if (PixelFingerprintIconPolicy.shouldReplaceCarrier(true)) {
            applyPixelDrawable(resolvedContext, uiMech, iconView, source, animate,
                    aodStyle, dark, onDozeState, onDreamingStart, screenTurnedOff, true);
        }
        if (scheduleReclaim && PixelFingerprintIconPolicy.shouldReplaceCarrier(true)) {
            scheduleVendorReclaim(resolvedContext, uiMech, iconView, source);
        }
    }

    private static void applyPixelDrawable(Context context, Object uiMech, ImageView iconView,
            String source, boolean animate, boolean aodStyle, boolean dark,
            boolean onDozeState, boolean onDreamingStart, boolean screenTurnedOff,
            boolean primary) {
        Drawable current = iconView.getDrawable();
        String drawableClass = current != null ? current.getClass().getName() : null;
        PixelFingerprintDrawable pixelDrawable;
        boolean created = false;
        synchronized (PIXEL_DRAWABLES) {
            pixelDrawable = PIXEL_DRAWABLES.get(iconView);
            if (pixelDrawable == null) {
                pixelDrawable = new PixelFingerprintDrawable(
                        context, aodStyle, dark, primary);
                PIXEL_DRAWABLES.put(iconView, pixelDrawable);
                created = true;
            }
        }
        synchronized (TRACKED_VIEWS) {
            TRACKED_VIEWS.put(iconView, Boolean.TRUE);
            VIEW_OWNERS.put(iconView, new WeakReference<>(uiMech));
        }
        normalizeIconView(iconView);
        if (current != pixelDrawable) {
            if (current != null && !(current instanceof PixelFingerprintDrawable)) {
                synchronized (ORIGINAL_DRAWABLES) {
                    ORIGINAL_DRAWABLES.put(iconView, current);
                }
            }
            setImageDrawableInternal(iconView, pixelDrawable);
        }
        pixelDrawable.transitionTo(aodStyle, dark, animate && !created);
        logStateIfChanged(iconView, "pixel-" + (aodStyle ? "aod" : "lockscreen"), source,
                "animate=" + (animate && !created)
                        + " dark=" + dark
                        + " interactive=" + isInteractive(context)
                        + " primary=" + primary
                        + " reclaimedCompeting="
                        + PixelFingerprintIconPolicy.isCompetingDrawableClass(drawableClass)
                        + " fields={doze=" + onDozeState
                        + ",dreaming=" + onDreamingStart
                        + ",screenOff=" + screenTurnedOff + "}");
    }

    static void refreshLast(Context context, String source) {
        Object uiMech = lastUiMech.get();
        if (uiMech != null) {
            refresh(context, uiMech, source, false);
            return;
        }
        ImageView[] views;
        synchronized (TRACKED_VIEWS) {
            views = TRACKED_VIEWS.keySet().toArray(new ImageView[0]);
        }
        for (ImageView view : views) {
            refreshTrackedView(context, view, source);
        }
    }

    private static void restoreOriginalDrawable(ImageView iconView, String source) {
        if (iconView == null) {
            return;
        }
        cancelPendingReclaim(iconView);
        Drawable current = iconView.getDrawable();
        if (current instanceof PixelFingerprintDrawable) {
            Drawable original;
            synchronized (ORIGINAL_DRAWABLES) {
                original = ORIGINAL_DRAWABLES.get(iconView);
            }
            setImageDrawableInternal(iconView, original);
            logStateIfChanged(iconView, "vendor-restored", source,
                    "drawable=" + (original != null ? original.getClass().getName() : "null"));
        }
        synchronized (ORIGINAL_DRAWABLES) {
            ORIGINAL_DRAWABLES.remove(iconView);
        }
        synchronized (PIXEL_DRAWABLES) {
            PIXEL_DRAWABLES.remove(iconView);
        }
        synchronized (TRACKED_VIEWS) {
            TRACKED_VIEWS.remove(iconView);
            VIEW_OWNERS.remove(iconView);
        }
    }

    private static void normalizeIconView(ImageView iconView) {
        beginInternalMutation();
        try {
            if (iconView.getBackground() != null) {
                iconView.setBackground(null);
            }
            if (iconView.getImageTintList() != null) {
                iconView.setImageTintList(null);
            }
            if (iconView.getColorFilter() != null) {
                iconView.clearColorFilter();
            }
            if (iconView.getScaleType() != ImageView.ScaleType.CENTER) {
                iconView.setScaleType(ImageView.ScaleType.CENTER);
            }
        } finally {
            endInternalMutation();
        }
    }

    private static void setImageDrawableInternal(ImageView iconView, Drawable drawable) {
        beginInternalMutation();
        try {
            iconView.setImageDrawable(drawable);
        } finally {
            endInternalMutation();
        }
    }

    private static void scheduleDiscovery(Context context, Object uiMech,
            String source, boolean animate) {
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.containsKey(uiMech)) {
                return;
            }
        }
        WeakReference<Object> reference = new WeakReference<>(uiMech);
        int[] attempt = {0};
        Runnable[] holder = new Runnable[1];
        Runnable retry = () -> {
            Object target = reference.get();
            if (target == null) {
                cancelDiscovery(uiMech, holder[0]);
                return;
            }
            ImageView iconView = findFingerprintIcon(target);
            ImageView pressedIcon = findFingerprintPressedIcon(target);
            if (iconView != null || pressedIcon != null) {
                cancelDiscovery(target, holder[0]);
                refresh(context, target, source + "#discovery", animate);
                return;
            }
            int nextAttempt = ++attempt[0];
            if (nextAttempt >= DISCOVERY_RETRY_DELAYS_MS.length) {
                cancelDiscovery(target, holder[0]);
                PixelAodLog.log("Pixel fingerprint icon discovery exhausted source=" + source);
                return;
            }
            MAIN.postDelayed(holder[0], DISCOVERY_RETRY_DELAYS_MS[nextAttempt]);
        };
        holder[0] = retry;
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.containsKey(uiMech)) {
                return;
            }
            PENDING_DISCOVERY.put(uiMech, retry);
        }
        MAIN.post(retry);
    }

    private static void cancelDiscovery(Object uiMech) {
        Runnable pending;
        synchronized (PENDING_DISCOVERY) {
            pending = PENDING_DISCOVERY.remove(uiMech);
        }
        if (pending != null) {
            MAIN.removeCallbacks(pending);
        }
    }

    private static void cancelDiscovery(Object uiMech, Runnable expected) {
        synchronized (PENDING_DISCOVERY) {
            if (PENDING_DISCOVERY.get(uiMech) != expected) {
                return;
            }
            PENDING_DISCOVERY.remove(uiMech);
        }
        MAIN.removeCallbacks(expected);
    }

    private static void refreshTrackedView(Context context, ImageView iconView, String source) {
        if (iconView == null || !isTracked(iconView)) {
            return;
        }
        WeakReference<Object> owner;
        synchronized (TRACKED_VIEWS) {
            owner = VIEW_OWNERS.get(iconView);
        }
        Object uiMech = owner != null ? owner.get() : lastUiMech.get();
        if (uiMech == null) {
            return;
        }
        requestVisualState(context, uiMech, source, false);
    }

    private static boolean isTracked(ImageView iconView) {
        synchronized (TRACKED_VIEWS) {
            return TRACKED_VIEWS.containsKey(iconView);
        }
    }

    static void installImageViewMutationHooks() {
        synchronized (PixelFingerprintIconController.class) {
            if (imageViewHooksInstalled) {
                return;
            }
            imageViewHooksInstalled = true;
        }
        String[] names = {
                "setImageDrawable", "setImageResource", "setImageIcon",
                "setImageTintList", "setColorFilter", "setScaleType"
        };
        int hooked = 0;
        for (Method method : ImageView.class.getDeclaredMethods()) {
            if (method.getParameterCount() != 1 || !contains(names, method.getName())) {
                continue;
            }
            try {
                ModernHookBridge.hookAfter(method, param -> {
                    if (!(param.thisObject instanceof ImageView)
                            || INTERNAL_MUTATION_DEPTH.get() > 0) {
                        return;
                    }
                    ImageView view = (ImageView) param.thisObject;
                    if (isTracked(view)) {
                        scheduleTrackedRefresh(view, "ImageView#" + method.getName());
                    }
                });
                hooked++;
            } catch (Throwable t) {
                PixelAodLog.log("failed to hook fingerprint ImageView mutation "
                        + method.getName(), t);
            }
        }
        PixelAodLog.log("installed fingerprint ImageView mutation hooks count=" + hooked);
    }

    private static void scheduleTrackedRefresh(ImageView iconView, String source) {
        Runnable previous;
        Runnable[] holder = new Runnable[1];
        Runnable refresh = () -> {
            synchronized (PENDING_REFRESHES) {
                if (PENDING_REFRESHES.get(iconView) == holder[0]) {
                    PENDING_REFRESHES.remove(iconView);
                }
            }
            refreshTrackedView(null, iconView, source);
        };
        holder[0] = refresh;
        synchronized (PENDING_REFRESHES) {
            previous = PENDING_REFRESHES.put(iconView, refresh);
        }
        if (previous != null) {
            iconView.removeCallbacks(previous);
        }
        iconView.postOnAnimation(refresh);
    }

    static void installVendorViewHooks(ClassLoader classLoader) {
        hookVendorViewClass(classLoader,
                "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintIcon");
        hookVendorViewClass(classLoader,
                "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintPressedIcon");
    }

    private static void hookVendorViewClass(ClassLoader classLoader, String className) {
        try {
            Class<?> clazz = ModernHookBridge.findClass(className, classLoader);
            synchronized (VENDOR_VIEW_HOOKS) {
                if (VENDOR_VIEW_HOOKS.containsKey(clazz)) {
                    return;
                }
                VENDOR_VIEW_HOOKS.put(clazz, Boolean.TRUE);
            }
            int hooked = 0;
            for (Method method : clazz.getDeclaredMethods()) {
                if (!isVendorVisualMutation(method)) {
                    continue;
                }
                final String source = className + "#" + method.getName();
                try {
                    method.setAccessible(true);
                    ModernHookBridge.hookAfter(method, param -> {
                        if (INTERNAL_MUTATION_DEPTH.get() == 0) {
                            onTrackedViewMutation(param.thisObject, source);
                        }
                    });
                    hooked++;
                } catch (Throwable t) {
                    PixelAodLog.log("failed to hook fingerprint vendor view mutation " + source, t);
                }
            }
            PixelAodLog.log("installed fingerprint vendor view hooks class=" + className
                    + " count=" + hooked);
        } catch (ClassNotFoundException ignored) {
            PixelAodLog.log("fingerprint vendor view class not found class=" + className);
        } catch (Throwable t) {
            PixelAodLog.log("failed to install fingerprint vendor view hooks class=" + className, t);
        }
    }

    private static boolean isVendorVisualMutation(Method method) {
        if (method == null || method.getParameterCount() > 2) {
            return false;
        }
        String name = method.getName();
        return "setVisibility".equals(name)
                || "setBrightnessAlpha".equals(name)
                || "setMaxBrightnessToAlpha".equals(name)
                || "stopSwitchAnim".equals(name)
                || "onVisibilityChanged".equals(name);
    }

    private static void onTrackedViewMutation(Object object, String source) {
        if (!(object instanceof ImageView)) {
            return;
        }
        ImageView view = (ImageView) object;
        if (isTracked(view)) {
            scheduleTrackedRefresh(view, source);
        }
    }

    private static boolean contains(String[] values, String candidate) {
        for (String value : values) {
            if (value.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void beginInternalMutation() {
        INTERNAL_MUTATION_DEPTH.set(INTERNAL_MUTATION_DEPTH.get() + 1);
    }

    private static void endInternalMutation() {
        int depth = INTERNAL_MUTATION_DEPTH.get() - 1;
        if (depth <= 0) {
            INTERNAL_MUTATION_DEPTH.remove();
        } else {
            INTERNAL_MUTATION_DEPTH.set(depth);
        }
    }

    private static void scheduleVendorReclaim(Context context, Object uiMech,
            ImageView iconView, String source) {
        Runnable previous;
        Runnable[] reclaimRunnableHolder = new Runnable[1];
        Runnable reclaim = () -> {
            synchronized (PENDING_RECLAIMS) {
                if (PENDING_RECLAIMS.get(iconView) == reclaimRunnableHolder[0]) {
                    PENDING_RECLAIMS.remove(iconView);
                }
            }
            if (!(iconView.getDrawable() instanceof PixelFingerprintDrawable)) {
                requestVisualState(context, uiMech, source + "#vendor-reclaim", false);
            }
        };
        reclaimRunnableHolder[0] = reclaim;
        synchronized (PENDING_RECLAIMS) {
            previous = PENDING_RECLAIMS.put(iconView, reclaim);
        }
        if (previous != null) {
            iconView.removeCallbacks(previous);
        }
        iconView.postDelayed(reclaim, VENDOR_RECLAIM_DELAY_MS);
        iconView.postDelayed(() -> {
            if (!(iconView.getDrawable() instanceof PixelFingerprintDrawable)) {
                requestVisualState(context, uiMech,
                        source + "#vendor-reclaim-second-pass", false);
            }
        }, VENDOR_RECLAIM_SECOND_PASS_DELAY_MS);
    }

    private static void cancelPendingReclaim(ImageView iconView) {
        Runnable pending;
        synchronized (PENDING_RECLAIMS) {
            pending = PENDING_RECLAIMS.remove(iconView);
        }
        if (pending != null) {
            iconView.removeCallbacks(pending);
        }
    }

    private static ImageView findFingerprintIcon(Object uiMech) {
        String[] fieldNames = {
                "fpIcon", "mFpIcon", "fingerprintIcon", "mFingerprintIcon",
                "fpIconView", "mFpIconView", "udfpsIcon", "mUdfpsIcon"
        };
        for (String fieldName : fieldNames) {
            try {
                Object field = ModernHookBridge.getObjectField(uiMech, fieldName);
                if (field instanceof ImageView) {
                    return (ImageView) field;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Object result = ModernHookBridge.callMethod(uiMech, "getFingerprintIcon");
            if (result instanceof ImageView) {
                return (ImageView) result;
            }
        } catch (Throwable ignored) {
        }
        Class<?> current = uiMech.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (!ImageView.class.isAssignableFrom(field.getType())
                        || (!name.contains("fp") && !name.contains("finger")
                        && !name.contains("udfps"))) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(uiMech);
                    if (value instanceof ImageView) {
                        return (ImageView) value;
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static ImageView findFingerprintPressedIcon(Object uiMech) {
        String[] fieldNames = {
                "pressedIcon", "mPressedIcon", "fingerprintPressedIcon", "mFingerprintPressedIcon"
        };
        for (String fieldName : fieldNames) {
            try {
                Object field = ModernHookBridge.getObjectField(uiMech, fieldName);
                if (field instanceof ImageView) {
                    return (ImageView) field;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Object result = ModernHookBridge.callMethod(uiMech, "getFingerprintPressedIcon");
            if (result instanceof ImageView) {
                return (ImageView) result;
            }
        } catch (Throwable ignored) {
        }
        Class<?> current = uiMech.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                String name = field.getName().toLowerCase();
                if (!ImageView.class.isAssignableFrom(field.getType())
                        || (!name.contains("pressed") && !name.contains("press"))) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(uiMech);
                    if (value instanceof ImageView) {
                        return (ImageView) value;
                    }
                } catch (Throwable ignored) {
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static boolean readBooleanField(Object receiver, String fieldName) {
        try {
            Object value = ModernHookBridge.getObjectField(receiver, fieldName);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isDarkMode(Context context) {
        if (context == null) {
            return true;
        }
        int nightMode = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static boolean isInteractive(Context context) {
        if (context == null) {
            return false;
        }
        PowerManager powerManager = context.getSystemService(PowerManager.class);
        return powerManager != null && powerManager.isInteractive();
    }

    private static void logStateIfChanged(ImageView view, String state,
            String source, String detail) {
        synchronized (LAST_LOGGED_STATES) {
            if (state.equals(LAST_LOGGED_STATES.get(view))) {
                return;
            }
            LAST_LOGGED_STATES.put(view, state);
        }
        PixelAodLog.i("Pixel fingerprint icon state=" + state
                + " source=" + source + " " + detail);
    }
}
