package dev.codex.pixelaod;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Process;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.LruCache;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Optional bridge to the ROM's SystemUI TextAnimator.
 *
 * <p>All SystemUI types are resolved through the supplied class loader. A missing or incompatible
 * ROM runtime returns {@code null}; the host then uses its deterministic four-TextView fallback.</p>
 */
final class CouiClockRomTextAnimatorRuntime {
    private static final String TEXT_ANIMATOR = "com.android.systemui.animation.TextAnimator";
    private static final String TEXT_ANIMATOR_LISTENER =
            "com.android.systemui.animation.TextAnimatorListener";
    private static final String TEXT_INTERPOLATOR =
            "com.android.systemui.animation.TextInterpolator";
    private static final String TYPEFACE_CACHE =
            "com.android.systemui.animation.TypefaceVariantCache";
    private static final String TYPEFACE_CACHE_IMPL =
            "com.android.systemui.animation.TypefaceVariantCacheImpl";
    private static final int FONT_CACHE_MAX_ENTRIES = 384;
    private static final long PREWARM_DELAY_MS = 2000L;
    private static final String PREWARM_TEXT = "8";
    private static final int PREWARM_LAYOUT_WIDTH_PX = 256;
    private static final float PREWARM_TEXT_SIZE_PX = 128f;

    private final Constructor<?> animatorConstructor;
    private final Object cache;
    private final Constructor<?> styleConstructor;
    private final Constructor<?> animationConstructor;
    private final Method drawMethod;
    private final Method updateLayoutMethod;
    private final Method setTextStyleMethod;
    private final Method setTextStyleInternalMethod;
    private final Method getTextInterpolatorMethod;
    private final Method setProgressMethod;
    private final Method setLinearProgressMethod;
    private final Method rebaseMethod;
    private final Class<?> listenerClass;
    private final Typeface baseTypeface;
    private final boolean fontCacheExpanded;
    private final AtomicBoolean prewarmStarted = new AtomicBoolean(false);

    private CouiClockRomTextAnimatorRuntime(Constructor<?> animatorConstructor, Object cache,
            Constructor<?> styleConstructor, Constructor<?> animationConstructor,
            Method drawMethod, Method updateLayoutMethod, Method setTextStyleMethod,
            Method setTextStyleInternalMethod, Method getTextInterpolatorMethod,
            Method setProgressMethod, Method setLinearProgressMethod, Method rebaseMethod,
            Class<?> listenerClass, Typeface baseTypeface, boolean fontCacheExpanded) {
        this.animatorConstructor = animatorConstructor;
        this.cache = cache;
        this.styleConstructor = styleConstructor;
        this.animationConstructor = animationConstructor;
        this.drawMethod = drawMethod;
        this.updateLayoutMethod = updateLayoutMethod;
        this.setTextStyleMethod = setTextStyleMethod;
        this.setTextStyleInternalMethod = setTextStyleInternalMethod;
        this.getTextInterpolatorMethod = getTextInterpolatorMethod;
        this.setProgressMethod = setProgressMethod;
        this.setLinearProgressMethod = setLinearProgressMethod;
        this.rebaseMethod = rebaseMethod;
        this.listenerClass = listenerClass;
        this.baseTypeface = baseTypeface;
        this.fontCacheExpanded = fontCacheExpanded;
    }

    static CouiClockRomTextAnimatorRuntime create(Context context, ClassLoader classLoader,
            Typeface baseTypeface) {
        if (context == null || baseTypeface == null) {
            return null;
        }
        ClassLoader loader = classLoader != null ? classLoader : context.getClassLoader();
        if (loader == null) {
            return null;
        }
        try {
            Class<?> animatorClass = Class.forName(TEXT_ANIMATOR, false, loader);
            Class<?> listenerClass = Class.forName(TEXT_ANIMATOR_LISTENER, false, loader);
            Class<?> cacheInterface = Class.forName(TYPEFACE_CACHE, false, loader);
            Class<?> cacheImpl = Class.forName(TYPEFACE_CACHE_IMPL, false, loader);
            Class<?> styleClass = Class.forName(TEXT_ANIMATOR + "$Style", false, loader);
            Class<?> animationClass = Class.forName(TEXT_ANIMATOR + "$Animation", false, loader);

            Constructor<?> cacheConstructor = cacheImpl.getConstructor(Typeface.class, int.class);
            Object cache = cacheConstructor.newInstance(baseTypeface, 30);
            boolean expanded = expandFontCache(cache);

            Constructor<?> animatorConstructor = animatorClass.getConstructor(
                    Layout.class, cacheInterface, listenerClass);
            Constructor<?> styleConstructor = styleClass.getConstructor(
                    String.class, Float.class, Integer.class, Float.class);
            Constructor<?> animationConstructor = animationClass.getConstructor(
                    boolean.class, long.class, long.class, TimeInterpolator.class, Runnable.class);
            Method drawMethod = animatorClass.getMethod("draw", Canvas.class);
            Method updateLayoutMethod = animatorClass.getMethod("updateLayout", Layout.class,
                    float.class);
            Method setTextStyleMethod = animatorClass.getMethod("setTextStyle", styleClass,
                    animationClass);
            Method setTextStyleInternalMethod = optionalMethod(animatorClass,
                    "setTextStyleInternal", styleClass, boolean.class, boolean.class);
            Method getTextInterpolatorMethod = optionalMethod(animatorClass,
                    "getTextInterpolator");

            Class<?> textInterpolatorClass = null;
            try {
                textInterpolatorClass = Class.forName(TEXT_INTERPOLATOR, false, loader);
            } catch (Throwable ignored) {
                // Prewarm is optional.
            }
            Method setProgressMethod = textInterpolatorClass == null ? null
                    : optionalMethod(textInterpolatorClass, "setProgress", float.class);
            Method setLinearProgressMethod = textInterpolatorClass == null ? null
                    : optionalMethod(textInterpolatorClass, "setLinearProgress", float.class);
            Method rebaseMethod = textInterpolatorClass == null ? null
                    : optionalMethod(textInterpolatorClass, "rebase");

            CouiClockRomTextAnimatorRuntime runtime = new CouiClockRomTextAnimatorRuntime(
                    animatorConstructor, cache,
                    styleConstructor, animationConstructor, drawMethod, updateLayoutMethod,
                    setTextStyleMethod, setTextStyleInternalMethod, getTextInterpolatorMethod,
                    setProgressMethod, setLinearProgressMethod, rebaseMethod, listenerClass,
                    baseTypeface, expanded);
            PixelAodLog.log("COUI ROM TextAnimator available morphRuntime=true"
                    + " fontCacheExpanded=" + expanded
                    + " textInterpolatorPrewarm=" + (setProgressMethod != null
                    && setLinearProgressMethod != null && rebaseMethod != null)
                    + " classLoader=" + loader.getClass().getName());
            return runtime;
        } catch (Throwable t) {
            PixelAodLog.log("COUI ROM TextAnimator unavailable; using four-set fallback", t);
            return null;
        }
    }

    private static Method optionalMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        try {
            return owner.getMethod(name, parameterTypes);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean expandFontCache(Object cache) {
        try {
            Method getFontCache = cache.getClass().getMethod("getFontCache");
            Object fontCache = getFontCache.invoke(cache);
            for (String fieldName : new String[]{"interpCache", "verFontCache"}) {
                Field field = fontCache.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(fontCache, new LruCache<>(FONT_CACHE_MAX_ENTRIES));
            }
            return true;
        } catch (Throwable t) {
            PixelAodLog.log("COUI ROM font cache expansion unavailable", t);
            return false;
        }
    }

    Bridge createBridge(Layout layout, View view) {
        try {
            Object animator = animatorConstructor.newInstance(layout, cache,
                    listenerProxy(view));
            return new Bridge(animator, styleConstructor, animationConstructor, drawMethod,
                    updateLayoutMethod, setTextStyleMethod);
        } catch (Throwable t) {
            PixelAodLog.log("COUI ROM TextAnimator bridge creation unavailable", t);
            return null;
        }
    }

    void prewarmAsync(final TimeInterpolator interpolator) {
        if (!fontCacheExpanded || interpolator == null || setTextStyleInternalMethod == null
                || getTextInterpolatorMethod == null || setProgressMethod == null
                || setLinearProgressMethod == null || rebaseMethod == null
                || !prewarmStarted.compareAndSet(false, true)) {
            return;
        }
        PixelAodLog.log("COUI ROM variable-font cache prewarm scheduled delayMs="
                + PREWARM_DELAY_MS);
        new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            long startedAt = android.os.SystemClock.uptimeMillis();
            try {
                Thread.sleep(PREWARM_DELAY_MS);
                PixelAodLog.log("COUI ROM variable-font cache prewarm started");
                prewarm(interpolator);
                PixelAodLog.log("COUI ROM variable-font cache prewarm complete durationMs="
                        + (android.os.SystemClock.uptimeMillis() - startedAt));
            } catch (Throwable t) {
                PixelAodLog.log("COUI ROM variable-font cache prewarm unavailable", t);
            }
        }, "COUIClockFontPrewarm").start();
    }

    private void prewarm(TimeInterpolator interpolator) throws Exception {
        TextPaint paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setTypeface(baseTypeface);
        paint.setTextSize(PREWARM_TEXT_SIZE_PX);
        paint.setFontFeatureSettings("'tnum'");
        StaticLayout layout = StaticLayout.Builder.obtain(PREWARM_TEXT, 0, 1, paint,
                PREWARM_LAYOUT_WIDTH_PX).build();
        Object animator = animatorConstructor.newInstance(layout, cache, listenerProxy(null));
        Object textInterpolator = getTextInterpolatorMethod.invoke(animator);
        Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmap);
        String[] path = {
                CouiClockFontPolicy.SMALL_VARIATION,
                CouiClockFontPolicy.LARGE_VARIATION,
                CouiClockFontPolicy.AOD_LARGE_VARIATION,
                CouiClockFontPolicy.AOD_SMALL_VARIATION,
                CouiClockFontPolicy.AOD_LARGE_VARIATION,
                CouiClockFontPolicy.LARGE_VARIATION,
                CouiClockFontPolicy.SMALL_VARIATION,
                CouiClockFontPolicy.AOD_SMALL_VARIATION,
                CouiClockFontPolicy.SMALL_VARIATION
        };
        try {
            for (String variation : path) {
                Object style = styleConstructor.newInstance(variation, null, null, null);
                setTextStyleInternalMethod.invoke(animator, style, true, true);
                for (int i = 0; i <= 30; i++) {
                    float linear = i / 30f;
                    setProgressMethod.invoke(textInterpolator,
                            interpolator.getInterpolation(linear));
                    setLinearProgressMethod.invoke(textInterpolator, linear);
                    drawMethod.invoke(animator, canvas);
                }
                rebaseMethod.invoke(textInterpolator);
            }
        } finally {
            bitmap.recycle();
        }
    }

    private Object listenerProxy(View view) {
        ClassLoader loader = listenerClass.getClassLoader();
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if ("onInvalidate".equals(name) || "onPaintModified".equals(name)) {
                if (view != null) {
                    view.invalidate();
                }
                return null;
            }
            if ("toString".equals(name)) {
                return "COUIClockTextAnimatorListener";
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(name)) {
                return proxy == (args == null || args.length == 0 ? null : args[0]);
            }
            return null;
        };
        return Proxy.newProxyInstance(loader, new Class<?>[]{listenerClass}, handler);
    }

    static final class Bridge {
        private final Object animator;
        private final Constructor<?> styleConstructor;
        private final Constructor<?> animationConstructor;
        private final Method drawMethod;
        private final Method updateLayoutMethod;
        private final Method setTextStyleMethod;

        Bridge(Object animator, Constructor<?> styleConstructor,
                Constructor<?> animationConstructor, Method drawMethod, Method updateLayoutMethod,
                Method setTextStyleMethod) {
            this.animator = animator;
            this.styleConstructor = styleConstructor;
            this.animationConstructor = animationConstructor;
            this.drawMethod = drawMethod;
            this.updateLayoutMethod = updateLayoutMethod;
            this.setTextStyleMethod = setTextStyleMethod;
        }

        void draw(Canvas canvas) throws Exception {
            drawMethod.invoke(animator, canvas);
        }

        void updateLayout(Layout layout) throws Exception {
            updateLayoutMethod.invoke(animator, layout, -1f);
        }

        void setStyle(CouiClockFontPolicy.MorphStyleSpec style, TimeInterpolator interpolator) {
            try {
                Object romStyle = styleConstructor.newInstance(style.variation(), null,
                        Integer.valueOf(style.color()), null);
                Object animation = animationConstructor.newInstance(style.animate(), 0L,
                        style.durationMillis(), interpolator, null);
                setTextStyleMethod.invoke(animator, romStyle, animation);
            } catch (Throwable t) {
                PixelAodLog.log("COUI ROM TextAnimator style update unavailable", t);
            }
        }
    }
}
