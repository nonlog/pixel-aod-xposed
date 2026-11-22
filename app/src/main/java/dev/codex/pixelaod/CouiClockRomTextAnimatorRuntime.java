package dev.codex.pixelaod;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.os.Build;
import android.os.Process;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.LruCache;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge to the ROM's SystemUI TextAnimator with a self-contained variable-font fallback.
 *
 * <p>On ROMs that expose {@code com.android.systemui.animation.TextAnimator} (e.g. ColorOS 15/16),
 * all SystemUI types are resolved through the supplied class loader. When the ROM omits or renames
 * {@code TextAnimator} (e.g. Android 17 / ColorOS 17), a self-contained variable-font morph runtime
 * backed by the bundled Google Sans Flex variable font and a bounded Typeface cache drives the
 * same {@link Bridge} contract.</p>
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
    private static final String MODULE_PACKAGE_NAME = "dev.codex.pixelaod";
    // One 550 ms morph at 120-144 Hz needs roughly 66-80 variants. Keep one transition
    // resident without retaining hundreds of native Typeface variants in SystemUI.
    private static final int FONT_CACHE_MAX_ENTRIES = 96;
    private static final int SELF_CONTAINED_WGHT_STEP = 5;
    private static final int SELF_CONTAINED_OPSZ_STEP = 4;
    private static final long PREWARM_DELAY_MS = 2000L;
    private static final String PREWARM_TEXT = "8";
    private static final int PREWARM_LAYOUT_WIDTH_PX = 256;
    private static final float PREWARM_TEXT_SIZE_PX = 128f;

    private final boolean selfContained;
    private final Context appContext;
    private final ByteBuffer fontBuffer;
    private final LruCache<String, Typeface> selfContainedCache;

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
        this.selfContained = false;
        this.appContext = null;
        this.fontBuffer = null;
        this.selfContainedCache = null;
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

    private CouiClockRomTextAnimatorRuntime(Context appContext, Typeface baseTypeface,
            ByteBuffer fontBuffer) {
        this.selfContained = true;
        this.appContext = appContext;
        this.fontBuffer = fontBuffer;
        this.selfContainedCache = new LruCache<>(FONT_CACHE_MAX_ENTRIES);
        this.animatorConstructor = null;
        this.cache = null;
        this.styleConstructor = null;
        this.animationConstructor = null;
        this.drawMethod = null;
        this.updateLayoutMethod = null;
        this.setTextStyleMethod = null;
        this.setTextStyleInternalMethod = null;
        this.getTextInterpolatorMethod = null;
        this.setProgressMethod = null;
        this.setLinearProgressMethod = null;
        this.rebaseMethod = null;
        this.listenerClass = null;
        this.baseTypeface = baseTypeface;
        this.fontCacheExpanded = true;
    }

    static CouiClockRomTextAnimatorRuntime create(Context context, ClassLoader classLoader,
            Typeface baseTypeface) {
        if (context == null || baseTypeface == null) {
            return null;
        }
        ClassLoader loader = classLoader != null ? classLoader : context.getClassLoader();
        if (loader != null) {
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
                PixelAodLog.log("COUI ROM TextAnimator unavailable; attempting self-contained"
                        + " variable-font morph runtime", t);
            }
        }
        return createSelfContained(context, baseTypeface);
    }

    private static CouiClockRomTextAnimatorRuntime createSelfContained(Context context,
            Typeface baseTypeface) {
        try {
            Context appContext = context.getApplicationContext() != null
                    ? context.getApplicationContext() : context;
            ByteBuffer buffer = loadVariableFontBuffer(appContext);
            CouiClockRomTextAnimatorRuntime runtime = new CouiClockRomTextAnimatorRuntime(
                    appContext, baseTypeface, buffer);
            PixelAodLog.i("COUI self-contained variable-font morph runtime active"
                    + " morphRuntime=true fontBufferLoaded=" + (buffer != null)
                    + " cacheCapacity=" + FONT_CACHE_MAX_ENTRIES);
            return runtime;
        } catch (Throwable t) {
            PixelAodLog.log("COUI self-contained variable-font morph runtime unavailable;"
                    + " using four-set fallback", t);
            return null;
        }
    }

    private static ByteBuffer loadVariableFontBuffer(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null;
        }
        try {
            Context assetContext = resolveAssetContext(context);
            try (InputStream in = assetContext.getAssets().open(
                    CouiClockFontPolicy.FONT_ASSET_PATH)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(64 * 1024);
                byte[] chunk = new byte[16 * 1024];
                int read;
                while ((read = in.read(chunk)) != -1) {
                    out.write(chunk, 0, read);
                }
                byte[] bytes = out.toByteArray();
                ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length);
                direct.put(bytes);
                direct.flip();
                return direct;
            }
        } catch (Throwable t) {
            PixelAodLog.log("COUI self-contained variable-font buffer load failed;"
                    + " falling back to AssetManager builder", t);
            return null;
        }
    }

    private static Context resolveAssetContext(Context context) {
        try {
            return context.createPackageContext(MODULE_PACKAGE_NAME,
                    Context.CONTEXT_IGNORE_SECURITY);
        } catch (Throwable ignored) {
            return context;
        }
    }

    private Typeface resolveVariantTypeface(String variation) {
        if (variation == null || selfContainedCache == null) {
            return baseTypeface;
        }
        Typeface cached = selfContainedCache.get(variation);
        if (cached != null) {
            return cached;
        }
        Typeface created = buildVariantTypeface(variation);
        if (created != null) {
            selfContainedCache.put(variation, created);
            return created;
        }
        return baseTypeface;
    }

    private Typeface buildVariantTypeface(String variation) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && fontBuffer != null) {
            try {
                Font font = new Font.Builder(fontBuffer.duplicate())
                        .setFontVariationSettings(variation)
                        .build();
                FontFamily family = new FontFamily.Builder(font).build();
                return new Typeface.CustomFallbackBuilder(family).build();
            } catch (Throwable ignored) {
                // Fall back to AssetManager Typeface.Builder below.
            }
        }
        if (appContext != null) {
            try {
                Context assetContext = resolveAssetContext(appContext);
                return new Typeface.Builder(assetContext.getAssets(),
                        CouiClockFontPolicy.FONT_ASSET_PATH)
                        .setFontVariationSettings(variation)
                        .build();
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
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
        if (selfContained) {
            return Bridge.selfContained(this, layout, view);
        }
        try {
            Object animator = animatorConstructor.newInstance(layout, cache,
                    listenerProxy(view));
            return Bridge.rom(animator, styleConstructor, animationConstructor, drawMethod,
                    updateLayoutMethod, setTextStyleMethod);
        } catch (Throwable t) {
            PixelAodLog.log("COUI ROM TextAnimator bridge creation unavailable", t);
            return null;
        }
    }

    void prewarmAsync(final TimeInterpolator interpolator) {
        if (selfContained || !fontCacheExpanded || interpolator == null
                || setTextStyleInternalMethod == null || getTextInterpolatorMethod == null
                || setProgressMethod == null || setLinearProgressMethod == null
                || rebaseMethod == null || !prewarmStarted.compareAndSet(false, true)) {
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

    static float parseAxis(String variation, String axisTag, float defaultValue) {
        if (variation == null || axisTag == null) {
            return defaultValue;
        }
        String quoted = "'" + axisTag + "'";
        int idx = variation.indexOf(quoted);
        if (idx < 0) {
            quoted = "\"" + axisTag + "\"";
            idx = variation.indexOf(quoted);
        }
        if (idx < 0) {
            return defaultValue;
        }
        int pos = idx + quoted.length();
        int len = variation.length();
        while (pos < len && (variation.charAt(pos) == ' ' || variation.charAt(pos) == '\t')) {
            pos++;
        }
        int end = pos;
        while (end < len) {
            char c = variation.charAt(end);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == '+') {
                end++;
            } else {
                break;
            }
        }
        if (end <= pos) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(variation.substring(pos, end));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    static String quantizeVariation(float wght, float opsz) {
        return quantizeVariation(wght, opsz, 100f, 100f);
    }

    static String quantizeVariation(float wght, float opsz, float wdth, float rond) {
        int qWght = Math.max(100, Math.min(900,
                Math.round(wght / SELF_CONTAINED_WGHT_STEP) * SELF_CONTAINED_WGHT_STEP));
        int qOpsz = Math.max(12, Math.min(144,
                Math.round(opsz / SELF_CONTAINED_OPSZ_STEP) * SELF_CONTAINED_OPSZ_STEP));
        int qWdth = Math.max(75, Math.min(125, Math.round(wdth)));
        int qRond = Math.max(0, Math.min(100, Math.round(rond)));
        return String.format(Locale.US,
                "'wght' %d, 'wdth' %d, 'ROND' %d, 'GRAD' 0, 'opsz' %d, 'slnt' 0",
                qWght, qWdth, qRond, qOpsz);
    }

    static int interpolateArgb(int startColor, int endColor, float fraction) {
        float t = Math.max(0f, Math.min(1f, fraction));
        int sA = (startColor >>> 24) & 0xff;
        int sR = (startColor >>> 16) & 0xff;
        int sG = (startColor >>> 8) & 0xff;
        int sB = startColor & 0xff;
        int eA = (endColor >>> 24) & 0xff;
        int eR = (endColor >>> 16) & 0xff;
        int eG = (endColor >>> 8) & 0xff;
        int eB = endColor & 0xff;
        int a = Math.round(sA + (eA - sA) * t);
        int r = Math.round(sR + (eR - sR) * t);
        int g = Math.round(sG + (eG - sG) * t);
        int b = Math.round(sB + (eB - sB) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static final class Bridge {
        private final Object animator;
        private final Constructor<?> styleConstructor;
        private final Constructor<?> animationConstructor;
        private final Method drawMethod;
        private final Method updateLayoutMethod;
        private final Method setTextStyleMethod;

        private final CouiClockRomTextAnimatorRuntime selfRuntime;
        private final View hostView;
        private final TextPaint drawPaint;
        private Layout layout;
        private ValueAnimator valueAnimator;
        private boolean initialized;
        private float currentWght = 450f;
        private float currentOpsz = 144f;
        private float currentWdth = 100f;
        private float currentRond = 100f;
        private int currentColor = Color.WHITE;
        private Typeface currentTypeface;
        private String currentQuantizedKey;

        private Bridge(Object animator, Constructor<?> styleConstructor,
                Constructor<?> animationConstructor, Method drawMethod, Method updateLayoutMethod,
                Method setTextStyleMethod) {
            this.animator = animator;
            this.styleConstructor = styleConstructor;
            this.animationConstructor = animationConstructor;
            this.drawMethod = drawMethod;
            this.updateLayoutMethod = updateLayoutMethod;
            this.setTextStyleMethod = setTextStyleMethod;
            this.selfRuntime = null;
            this.hostView = null;
            this.drawPaint = null;
        }

        private Bridge(CouiClockRomTextAnimatorRuntime selfRuntime, Layout layout, View hostView) {
            this.animator = null;
            this.styleConstructor = null;
            this.animationConstructor = null;
            this.drawMethod = null;
            this.updateLayoutMethod = null;
            this.setTextStyleMethod = null;
            this.selfRuntime = selfRuntime;
            this.layout = layout;
            this.hostView = hostView;
            this.drawPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            this.drawPaint.setTextAlign(Paint.Align.CENTER);
            this.currentTypeface = selfRuntime.baseTypeface;
            if (layout != null && layout.getPaint() != null) {
                this.currentColor = layout.getPaint().getColor();
            }
        }

        static Bridge rom(Object animator, Constructor<?> styleConstructor,
                Constructor<?> animationConstructor, Method drawMethod, Method updateLayoutMethod,
                Method setTextStyleMethod) {
            return new Bridge(animator, styleConstructor, animationConstructor, drawMethod,
                    updateLayoutMethod, setTextStyleMethod);
        }

        static Bridge selfContained(CouiClockRomTextAnimatorRuntime runtime, Layout layout,
                View hostView) {
            return new Bridge(runtime, layout, hostView);
        }

        void draw(Canvas canvas) throws Exception {
            if (selfRuntime == null) {
                drawMethod.invoke(animator, canvas);
                return;
            }
            if (layout == null || layout.getLineCount() <= 0) {
                return;
            }
            CharSequence text = layout.getText();
            if (text == null || text.length() == 0) {
                return;
            }
            TextPaint sourcePaint = layout.getPaint();
            if (sourcePaint != null) {
                drawPaint.setTextSize(sourcePaint.getTextSize());
                drawPaint.setLetterSpacing(sourcePaint.getLetterSpacing());
                drawPaint.setFontFeatureSettings(sourcePaint.getFontFeatureSettings());
            }
            drawPaint.setTypeface(currentTypeface != null
                    ? currentTypeface : selfRuntime.baseTypeface);
            drawPaint.setColor(currentColor);
            float centerX = layout.getLineLeft(0) + layout.getLineWidth(0) * 0.5f;
            float baselineY = layout.getLineBaseline(0);
            canvas.drawText(text, 0, text.length(), centerX, baselineY, drawPaint);
        }

        void updateLayout(Layout layout) throws Exception {
            if (selfRuntime == null) {
                updateLayoutMethod.invoke(animator, layout, -1f);
                return;
            }
            this.layout = layout;
            if (hostView != null) {
                hostView.invalidate();
            }
        }

        void setStyle(CouiClockFontPolicy.MorphStyleSpec style, TimeInterpolator interpolator) {
            if (selfRuntime == null) {
                try {
                    Object romStyle = styleConstructor.newInstance(style.variation(), null,
                            Integer.valueOf(style.color()), null);
                    Object animation = animationConstructor.newInstance(style.animate(), 0L,
                            style.durationMillis(), interpolator, null);
                    setTextStyleMethod.invoke(animator, romStyle, animation);
                } catch (Throwable t) {
                    PixelAodLog.log("COUI ROM TextAnimator style update unavailable", t);
                }
                return;
            }
            float targetWght = parseAxis(style.variation(), "wght", currentWght);
            float targetOpsz = parseAxis(style.variation(), "opsz", currentOpsz);
            float targetWdth = parseAxis(style.variation(), "wdth", currentWdth);
            float targetRond = parseAxis(style.variation(), "ROND", currentRond);
            int targetColor = style.color();
            if (valueAnimator != null) {
                valueAnimator.cancel();
                valueAnimator = null;
            }
            if (!initialized || !style.animate() || style.durationMillis() <= 0L) {
                initialized = true;
                applySelfContainedState(targetWght, targetOpsz, targetWdth, targetRond, targetColor);
                return;
            }
            final float startWght = currentWght;
            final float startOpsz = currentOpsz;
            final float startWdth = currentWdth;
            final float startRond = currentRond;
            final int startColor = currentColor;
            ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(style.durationMillis());
            if (interpolator != null) {
                anim.setInterpolator(interpolator);
            }
            anim.addUpdateListener(updated -> {
                float fraction = (Float) updated.getAnimatedValue();
                float wght = startWght + (targetWght - startWght) * fraction;
                float opsz = startOpsz + (targetOpsz - startOpsz) * fraction;
                float wdth = startWdth + (targetWdth - startWdth) * fraction;
                float rond = startRond + (targetRond - startRond) * fraction;
                int color = interpolateArgb(startColor, targetColor, fraction);
                applySelfContainedState(wght, opsz, wdth, rond, color);
            });
            valueAnimator = anim;
            anim.start();
        }

        private void applySelfContainedState(float wght, float opsz, float wdth, float rond,
                int color) {
            currentWght = wght;
            currentOpsz = opsz;
            currentWdth = wdth;
            currentRond = rond;
            currentColor = color;
            String key = quantizeVariation(wght, opsz, wdth, rond);
            if (!key.equals(currentQuantizedKey)) {
                currentQuantizedKey = key;
                currentTypeface = selfRuntime.resolveVariantTypeface(key);
            }
            if (hostView != null) {
                hostView.invalidate();
            }
        }
    }
}
