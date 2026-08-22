package dev.codex.pixelaod;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.PathInterpolator;

/**
 * Project-native rendering of COUI Expressive 2.5's stock UDFPS glyph.
 *
 * <p>The OPlus fingerprint carrier remains the owner of visibility, HBM and highlight alpha.
 * This drawable owns only the COUI glyph pixels and its 420 ms solid-to-dashed transition.</p>
 */
final class CouiUdfpsDrawable extends Drawable {
    private static final float VIEWPORT_SIZE = 72f;
    private static final float FOREGROUND_SCALE = 0.5f;
    private static final float ICON_SIZE_DP = 80f;
    private static final float NORMAL_STROKE_WIDTH = 3f;
    private static final float AOD_STROKE_WIDTH = 2f;
    private static final float AOD_DASH_LENGTH = 4f;
    private static final float AOD_DASH_GAP = 4.5f;
    private static final long TRANSITION_DURATION_MS = 420L;
    private static final PathInterpolator TRANSITION_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final Context context;
    private final float density;
    private final int intrinsicSize;
    private final Path sourcePath = createFingerprintPath();
    private final Path drawPath = new Path();
    private final Matrix pathMatrix = new Matrix();
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint solidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float centerX;
    private float centerY;
    private float squareSize;
    private float backgroundRadius;
    private float normalStrokeWidth;
    private float aodStrokeWidth;
    private float outlineProgress;
    private int drawableAlpha = 255;
    private int backgroundColor;
    private int normalForegroundColor;
    private int lightBackgroundColor;
    private int darkBackgroundColor;
    private int lightForegroundColor;
    private int darkForegroundColor;
    private boolean targetOutlineOnly;
    private boolean targetDark;
    private boolean pressed;
    private ValueAnimator transitionAnimator;

    CouiUdfpsDrawable(Context context, boolean outlineOnly, boolean dark) {
        this.context = context;
        density = context.getResources().getDisplayMetrics().density;
        intrinsicSize = Math.max(1, Math.round(ICON_SIZE_DP * density));
        backgroundPaint.setStyle(Paint.Style.FILL);
        configureStrokePaint(solidPaint);
        configureStrokePaint(dashedPaint);
        targetOutlineOnly = outlineOnly;
        targetDark = dark;
        outlineProgress = outlineOnly ? 1f : 0f;
        refreshPalette();
        backgroundColor = backgroundColor(dark);
        normalForegroundColor = foregroundColor(dark);
    }

    private static void configureStrokePaint(Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    private static Path createFingerprintPath() {
        Path path = new Path();
        path.moveTo(25.5f, 16.3283f);
        path.cubicTo(28.47f, 14.8433f, 31.9167f, 14f, 35.5834f, 14f);
        path.cubicTo(39.2501f, 14f, 42.6968f, 14.8433f, 45.6668f, 16.3283f);
        path.moveTo(20f, 28.6669f);
        path.cubicTo(22.7683f, 24.3402f, 28.7084f, 21.3335f, 35.5834f, 21.3335f);
        path.cubicTo(42.4585f, 21.3335f, 48.3985f, 24.3402f, 51.1669f, 28.6669f);
        path.moveTo(22.8607f, 47.0002f);
        path.cubicTo(21.834f, 44.3235f, 21.834f, 41.5002f, 21.834f, 41.5002f);
        path.cubicTo(21.834f, 34.4051f, 27.7374f, 28.6667f, 35.5841f, 28.6667f);
        path.cubicTo(43.4308f, 28.6667f, 49.3341f, 34.4051f, 49.3341f, 41.5002f);
        path.moveTo(49.3344f, 41.5003f);
        path.lineTo(49.3344f, 42.0319f);
        path.cubicTo(49.3344f, 44.7636f, 47.1161f, 47.0003f, 44.3661f, 47.0003f);
        path.cubicTo(41.9461f, 47.0003f, 39.8744f, 45.2403f, 39.471f, 42.857f);
        path.lineTo(38.9577f, 39.7769f);
        path.cubicTo(38.591f, 37.5953f, 36.7027f, 36.0002f, 34.5027f, 36.0002f);
        path.cubicTo(26.5826f, 36.0002f, 29.846f, 49.1087f, 35.291f, 50.6487f);
        path.moveTo(44.9713f, 54.6267f);
        path.cubicTo(42.5513f, 56.7167f, 39.2879f, 58.0001f, 35.5846f, 58.0001f);
        path.cubicTo(32.2296f, 58.0001f, 29.2229f, 56.9551f, 26.8945f, 55.195f);
        return path;
    }

    private int resolveToneColor(String name, int fallback) {
        try {
            int id = context.getResources().getIdentifier(
                    name, "color", context.getPackageName());
            if (id == 0) {
                id = context.getResources().getIdentifier(name, "color", "com.android.systemui");
            }
            return id != 0 ? context.getColor(id) : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private boolean refreshPalette() {
        int newLightBackground = resolveToneColor(
                "system_neutral2_50", Color.rgb(247, 247, 247));
        int newDarkBackground = resolveToneColor(
                "system_neutral2_800", Color.rgb(62, 63, 67));
        int newLightForeground = resolveToneColor(
                "system_neutral1_800", Color.rgb(45, 46, 49));
        int newDarkForeground = resolveToneColor(
                "system_neutral1_100", Color.rgb(238, 238, 242));
        boolean changed = lightBackgroundColor != newLightBackground
                || darkBackgroundColor != newDarkBackground
                || lightForegroundColor != newLightForeground
                || darkForegroundColor != newDarkForeground;
        lightBackgroundColor = newLightBackground;
        darkBackgroundColor = newDarkBackground;
        lightForegroundColor = newLightForeground;
        darkForegroundColor = newDarkForeground;
        return changed;
    }

    private int backgroundColor(boolean dark) {
        return dark ? darkBackgroundColor : lightBackgroundColor;
    }

    private int foregroundColor(boolean dark) {
        return dark ? darkForegroundColor : lightForegroundColor;
    }

    void transitionTo(boolean outlineOnly, boolean dark, boolean animate, boolean refreshColors) {
        boolean paletteChanged = refreshColors && refreshPalette();
        if (targetOutlineOnly == outlineOnly && targetDark == dark && !paletteChanged) {
            return;
        }
        targetOutlineOnly = outlineOnly;
        targetDark = dark;
        float targetProgress = outlineOnly ? 1f : 0f;
        int targetBackground = backgroundColor(dark);
        int targetForeground = foregroundColor(dark);
        if (transitionAnimator != null) {
            transitionAnimator.cancel();
            transitionAnimator = null;
        }
        if (!SystemAnimationScalePolicy.shouldAnimate(animate) || getCallback() == null) {
            outlineProgress = targetProgress;
            backgroundColor = targetBackground;
            normalForegroundColor = targetForeground;
            invalidateSelf();
            return;
        }
        final float startProgress = outlineProgress;
        final int startBackground = backgroundColor;
        final int startForeground = normalForegroundColor;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(TRANSITION_DURATION_MS);
        animator.setInterpolator(TRANSITION_INTERPOLATOR);
        animator.addUpdateListener(valueAnimator -> {
            float progress = (Float) valueAnimator.getAnimatedValue();
            outlineProgress = lerp(startProgress, targetProgress, progress);
            backgroundColor = blendArgb(startBackground, targetBackground, progress);
            normalForegroundColor = blendArgb(startForeground, targetForeground, progress);
            invalidateSelf();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (transitionAnimator == animation) {
                    transitionAnimator = null;
                }
            }
        });
        transitionAnimator = animator;
        animator.start();
    }

    boolean isOutlineOnly() {
        return targetOutlineOnly;
    }

    void setPressed(boolean pressed) {
        if (this.pressed == pressed) {
            return;
        }
        this.pressed = pressed;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (pressed || squareSize <= 0f) {
            return;
        }
        float progress = clamp(outlineProgress);
        float normalProgress = 1f - progress;
        if (normalProgress > 0f) {
            backgroundPaint.setColor(backgroundColor);
            backgroundPaint.setAlpha(modulatedAlpha(backgroundColor, normalProgress));
            canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint);
        }
        int foreground = blendArgb(normalForegroundColor, Color.WHITE, progress);
        float strokeWidth = lerp(normalStrokeWidth, aodStrokeWidth, progress);
        solidPaint.setColor(foreground);
        solidPaint.setStrokeWidth(strokeWidth);
        solidPaint.setAlpha(modulatedAlpha(foreground, normalProgress));
        if (solidPaint.getAlpha() > 0) {
            canvas.drawPath(drawPath, solidPaint);
        }
        dashedPaint.setColor(foreground);
        dashedPaint.setStrokeWidth(strokeWidth);
        dashedPaint.setAlpha(modulatedAlpha(foreground, progress));
        if (dashedPaint.getAlpha() > 0) {
            canvas.drawPath(drawPath, dashedPaint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        squareSize = Math.min(bounds.width(), bounds.height());
        centerX = bounds.exactCenterX();
        centerY = bounds.exactCenterY();
        backgroundRadius = Math.min(squareSize, density * 64f) / 2f;
        float scale = (squareSize / VIEWPORT_SIZE) * FOREGROUND_SCALE;
        pathMatrix.reset();
        pathMatrix.setScale(scale, scale);
        pathMatrix.postTranslate(centerX - 36f * scale, centerY - 36f * scale);
        drawPath.reset();
        sourcePath.transform(pathMatrix, drawPath);
        normalStrokeWidth = NORMAL_STROKE_WIDTH * scale;
        aodStrokeWidth = AOD_STROKE_WIDTH * scale;
        dashedPaint.setPathEffect(new DashPathEffect(
                new float[] { AOD_DASH_LENGTH * scale, AOD_DASH_GAP * scale }, 0f));
    }

    @Override
    public int getAlpha() {
        return drawableAlpha;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicSize;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setAlpha(int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        if (drawableAlpha == clamped) {
            return;
        }
        drawableAlpha = clamped;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        backgroundPaint.setColorFilter(colorFilter);
        solidPaint.setColorFilter(colorFilter);
        dashedPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    private int modulatedAlpha(int color, float progress) {
        return Math.max(0, Math.min(255, Math.round(
                clamp(progress) * (Color.alpha(color) * drawableAlpha / 255f))));
    }

    private static int blendArgb(int from, int to, float progress) {
        float p = clamp(progress);
        return Color.argb(
                Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * p),
                Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * p),
                Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * p),
                Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * p));
    }

    private static float lerp(float from, float to, float progress) {
        return from + (to - from) * progress;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
