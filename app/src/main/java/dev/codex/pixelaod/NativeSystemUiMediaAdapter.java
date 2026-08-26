package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Read-only adapter over the current OOS SystemUI media pipeline.
 *
 * <p>The preferred seam is {@code LegacyMediaDataManagerImpl -> LegacyMediaDataFilterImpl ->
 * OplusMediaDataFilterEx}. SystemUI has already applied current-user/profile filtering before the
 * OPlus extension chooses its current media. Pixel only snapshots that semantic result and never
 * mutates media playback, resumption state, timeout state, or SystemUI ordering.</p>
 */
final class NativeSystemUiMediaAdapter {
    private static final String MANAGER_CLASS =
            "com.android.systemui.media.controls.domain.pipeline.LegacyMediaDataManagerImpl";
    private static final String LISTENER_CLASS =
            "com.android.systemui.media.controls.domain.pipeline.MediaDataManager$Listener";
    private static final String BASE_FILTER_EX_CLASS =
            "com.android.systemui.media.controls.pipeline.OplusMediaDataFilterEx";

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Object LOCK = new Object();
    private static final Map<Object, Object> LISTENER_BY_MANAGER =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Object> REGISTERING_MANAGERS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static Context applicationContext;
    private static Class<?> listenerClass;
    private static Runnable changeListener;
    private static boolean nativeAuthorityObserved;
    private static Snapshot current = Snapshot.unknown();

    private NativeSystemUiMediaAdapter() {
    }

    static void install(Context context, ClassLoader classLoader, Runnable listener) {
        if (context == null || classLoader == null) {
            return;
        }
        synchronized (LOCK) {
            applicationContext = context.getApplicationContext() != null
                    ? context.getApplicationContext() : context;
            if (listener != null) {
                changeListener = listener;
            }
        }
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> managerClass = ModernHookBridge.findClass(MANAGER_CLASS, classLoader);
            listenerClass = ModernHookBridge.findClass(LISTENER_CLASS, classLoader);
            int constructorHooks = 0;
            for (Constructor<?> constructor : managerClass.getDeclaredConstructors()) {
                ModernHookBridge.hookAfter(constructor,
                        param -> registerManager(param.thisObject, "constructor"));
                constructorHooks++;
            }
            ModernHookBridge.hookAfter(managerClass, "addListener",
                    param -> registerManager(param.thisObject, "add-listener"), listenerClass);
            PixelAodLog.i("installed native SystemUI media semantics adapter"
                    + " manager=" + MANAGER_CLASS
                    + " constructorHooks=" + constructorHooks
                    + " fallback=MediaSessionManager");
        } catch (Throwable t) {
            PixelAodLog.e("native SystemUI media semantics unavailable; keeping fallback", t);
        }
    }

    static Snapshot snapshot() {
        synchronized (LOCK) {
            return current;
        }
    }

    static void clearForSelectedUserChange(String source) {
        Runnable listener = null;
        boolean changed = false;
        synchronized (LOCK) {
            if (nativeAuthorityObserved) {
                Snapshot next = Snapshot.empty(true, "selected-user-reset");
                changed = !current.sameSemanticState(next);
                current = next;
                listener = changeListener;
            }
        }
        if (changed && listener != null) {
            listener.run();
        }
        PixelAodLog.log("native SystemUI media selected-user state cleared"
                + " authoritative=" + nativeAuthorityObserved
                + " source=" + (source != null ? source : "unknown"));
    }

    private static void registerManager(Object manager, String source) {
        if (manager == null || listenerClass == null) {
            return;
        }
        synchronized (LOCK) {
            if (LISTENER_BY_MANAGER.containsKey(manager)
                    || REGISTERING_MANAGERS.contains(manager)) {
                return;
            }
            REGISTERING_MANAGERS.add(manager);
        }
        Object proxy = null;
        boolean registered = false;
        try {
            proxy = Proxy.newProxyInstance(listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass}, new NativeListenerInvocationHandler(manager));
            ModernHookBridge.callMethod(manager, "addListener", proxy);
            registered = true;
            boolean bootstrapped = refreshFromOplusFilter(manager, "register-" + source);
            PixelAodLog.i("registered native SystemUI media listener"
                    + " manager=" + manager.getClass().getName()
                    + " bootstrapped=" + bootstrapped
                    + " source=" + source);
        } catch (Throwable t) {
            PixelAodLog.e("failed to register native SystemUI media listener source=" + source, t);
        } finally {
            synchronized (LOCK) {
                REGISTERING_MANAGERS.remove(manager);
                if (registered && proxy != null) {
                    LISTENER_BY_MANAGER.put(manager, proxy);
                }
            }
        }
    }

    private static boolean refreshFromOplusFilter(Object manager, String source) {
        try {
            Object filter = ModernHookBridge.getObjectField(manager, "mediaDataFilter");
            Object extension = ModernHookBridge.getObjectField(filter, "oplusMediaDataFilterEx");
            if (extension == null || BASE_FILTER_EX_CLASS.equals(extension.getClass().getName())) {
                return false;
            }
            Object mediaData = ModernHookBridge.callMethod(extension, "getCurData");
            Object keyValue = null;
            try {
                keyValue = ModernHookBridge.callMethod(extension, "getCurKeyHandling");
            } catch (Throwable ignored) {
            }
            observeAuthoritativeMedia(keyValue != null ? keyValue.toString() : "", mediaData,
                    "oplus-filter-" + source);
            PixelAodLog.log("native SystemUI media OPlus filter snapshot"
                    + " extension=" + extension.getClass().getName()
                    + " present=" + (mediaData != null)
                    + " source=" + source);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void observeAuthoritativeMedia(String key, Object mediaData, String source) {
        Context context;
        synchronized (LOCK) {
            context = applicationContext;
        }
        Snapshot next = snapshotFromMediaData(mediaData, context, true, source);
        Runnable listener;
        boolean changed;
        synchronized (LOCK) {
            nativeAuthorityObserved = true;
            changed = !current.sameSemanticState(next);
            current = next;
            listener = changeListener;
        }
        if (changed && listener != null) {
            listener.run();
        }
        PixelAodLog.log("native SystemUI current media changed"
                + " keyPresent=" + (key != null && !key.isEmpty())
                + " present=" + next.present
                + " active=" + next.active
                + " userId=" + next.userId
                + " package=" + next.packageName
                + " source=" + source);
    }

    static Snapshot snapshotFromMediaDataForTest(Object mediaData) {
        return snapshotFromMediaData(mediaData, null, true, "test");
    }

    private static Snapshot snapshotFromMediaData(Object mediaData, Context context,
            boolean authoritative, String source) {
        if (mediaData == null) {
            return Snapshot.empty(authoritative, source);
        }
        boolean active = booleanValue(invokeNoArg(mediaData, "getActive"), false);
        int userId = intValue(invokeNoArg(mediaData, "getUserId"), -1);
        String packageName = stringValue(invokeNoArg(mediaData, "getPackageName"));
        if (!active) {
            return new Snapshot(authoritative, false, false, userId, packageName,
                    "", "", null, source);
        }
        String title = stringValue(invokeNoArg(mediaData, "getSong"));
        String artist = stringValue(invokeNoArg(mediaData, "getArtist"));
        Drawable appIcon = loadAppIcon(context, packageName, invokeNoArg(mediaData, "getAppIcon"));
        if (context != null && !packageName.isEmpty()) {
            if (title.isEmpty()) {
                Object app = invokeNoArg(mediaData, "getApp");
                title = stringValue(app);
            }
            if (artist.isEmpty()) {
                try {
                    artist = context.getPackageManager().getApplicationLabel(
                            context.getPackageManager().getApplicationInfo(packageName, 0)).toString();
                } catch (Throwable ignored) {
                }
            }
        }
        if (title.isEmpty()) {
            title = packageName;
        }
        if (artist.isEmpty()) {
            artist = packageName;
        }
        return new Snapshot(authoritative, true, true, userId, packageName,
                title, artist, appIcon, source);
    }

    private static Drawable loadAppIcon(Context context, String packageName, Object rawIcon) {
        if (context == null) {
            return null;
        }
        if (rawIcon instanceof Icon) {
            try {
                Drawable drawable = ((Icon) rawIcon).loadDrawable(context);
                if (drawable != null) {
                    return drawable;
                }
            } catch (Throwable ignored) {
            }
        }
        if (rawIcon instanceof Drawable) {
            return (Drawable) rawIcon;
        }
        if (packageName != null && !packageName.isEmpty()) {
            try {
                return context.getPackageManager().getApplicationIcon(packageName);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Object invokeNoArg(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(name);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean booleanValue(Object value, boolean fallback) {
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static int intValue(Object value, int fallback) {
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString().trim() : "";
    }

    private static final class NativeListenerInvocationHandler implements InvocationHandler {
        private final Object manager;

        NativeListenerInvocationHandler(Object manager) {
            this.manager = manager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method != null ? method.getName() : "";
            if ("toString".equals(name)) {
                return "PixelAodNativeMediaListener";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return args != null && args.length == 1 && proxy == args[0];
            }
            if ("onCurrentActiveMediaChanged".equals(name)) {
                String key = args != null && args.length > 0 ? stringValue(args[0]) : "";
                Object data = args != null && args.length > 1 ? args[1] : null;
                observeAuthoritativeMedia(key, data, "listener-current-active");
                return null;
            }
            if ("onMediaDataLoaded".equals(name) || "onMediaDataRemoved".equals(name)) {
                refreshFromOplusFilter(manager, "listener-" + name);
                return null;
            }
            return null;
        }
    }

    static final class Snapshot {
        final boolean authoritative;
        final boolean present;
        final boolean active;
        final int userId;
        final String packageName;
        final String title;
        final String artist;
        final Drawable appIcon;
        final String source;

        Snapshot(boolean authoritative, boolean present, boolean active, int userId,
                String packageName, String title, String artist, Drawable appIcon, String source) {
            this.authoritative = authoritative;
            this.present = present;
            this.active = active;
            this.userId = userId;
            this.packageName = packageName != null ? packageName : "";
            this.title = title != null ? title : "";
            this.artist = artist != null ? artist : "";
            this.appIcon = appIcon;
            this.source = source != null ? source : "unknown";
        }

        static Snapshot unknown() {
            return new Snapshot(false, false, false, -1, "", "", "", null, "unknown");
        }

        static Snapshot empty(boolean authoritative, String source) {
            return new Snapshot(authoritative, false, false, -1, "", "", "", null, source);
        }

        boolean sameSemanticState(Snapshot other) {
            if (other == null) {
                return false;
            }
            return authoritative == other.authoritative
                    && present == other.present
                    && active == other.active
                    && userId == other.userId
                    && packageName.equals(other.packageName)
                    && title.equals(other.title)
                    && artist.equals(other.artist);
        }
    }
}
