package dev.codex.pixelaod;

import android.animation.ArgbEvaluator;
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
import android.util.TypedValue;
import android.view.animation.PathInterpolator;

final class PixelFingerprintDrawable extends Drawable {
    private static final float VIEWPORT_SIZE = 72f;
    private static final float FOREGROUND_SCALE = 0.5f;
    private static final float LOCKSCREEN_STROKE_DP = 3f;
    private static final float AOD_STROKE_DP = 2f;
    private static final float AOD_DASH_DP = 4f;
    private static final float AOD_DASH_GAP_DP = 4.5f;
    private static final float INTRINSIC_SIZE_DP = 80f;
    private static final long TRANSITION_DURATION_MS = 420L;
    private static final PathInterpolator TRANSITION_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final Context context;
    private final float density;
    private final int intrinsicSize;
    private final Path sourcePath;
    private final Path drawPath = new Path();
    private final Matrix pathMatrix = new Matrix();
    private final Paint solidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dashedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private float centerX;
    private float centerY;
    private float lockscreenStrokeWidth;
    private float aodStrokeWidth;
    private float outlineProgress;
    private int drawableAlpha = 255;
    private int lockscreenForegroundColor;
    private boolean targetAodStyle;
    private boolean targetDark;
    private ValueAnimator transitionAnimator;

    PixelFingerprintDrawable(Context context, boolean aodStyle, boolean dark) {
        this.context = context;
        density = context.getResources().getDisplayMetrics().density;
        intrinsicSize = Math.max(1, Math.round(INTRINSIC_SIZE_DP * density));
        sourcePath = createFingerprintPath();
        configureStrokePaint(solidPaint);
        configureStrokePaint(dashedPaint);
        targetAodStyle = aodStyle;
        targetDark = dark;
        outlineProgress = aodStyle ? 1f : 0f;
        refreshPalette();
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

    private static void configureStrokePaint(Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    void transitionTo(boolean aodStyle, boolean dark, boolean animate) {
        boolean paletteChanged = targetDark != dark;
        if (targetAodStyle == aodStyle && !paletteChanged) {
            return;
        }
        targetAodStyle = aodStyle;
        targetDark = dark;
        refreshPalette();
        float targetProgress = aodStyle ? 1f : 0f;
        if (transitionAnimator != null) {
            transitionAnimator.cancel();
            transitionAnimator = null;
        }
        if (!animate || getCallback() == null) {
            outlineProgress = targetProgress;
            invalidateSelf();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(outlineProgress, targetProgress);
        animator.setDuration(TRANSITION_DURATION_MS);
        animator.setInterpolator(TRANSITION_INTERPOLATOR);
        animator.addUpdateListener(valueAnimator -> {
            outlineProgress = (Float) valueAnimator.getAnimatedValue();
            invalidateSelf();
        });
        animator.start();
        transitionAnimator = animator;
    }

    boolean isAodStyle() {
        return targetAodStyle;
    }

    private void refreshPalette() {
        int foregroundFallback = targetDark
                ? Color.rgb(228, 225, 233) : Color.rgb(68, 71, 79);
        lockscreenForegroundColor = resolveThemeColor(
                android.R.attr.textColorPrimary, foregroundFallback);
    }

    private int resolveThemeColor(int attributeId, int fallback) {
        if (attributeId == 0) {
            return fallback;
        }
        try {
            TypedValue value = new TypedValue();
            if (!context.getTheme().resolveAttribute(attributeId, value, true)) {
                return fallback;
            }
            if (value.resourceId != 0) {
                return context.getColor(value.resourceId);
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT
                    && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    @Override
    public void draw(Canvas canvas) {
        float progress = clamp(outlineProgress);
        float lockscreenProgress = PixelFingerprintIconPolicy.lockscreenLayerAlpha(progress);
        int foreground = (Integer) argbEvaluator.evaluate(
                progress, lockscreenForegroundColor, Color.WHITE);
        float strokeWidth = lerp(lockscreenStrokeWidth, aodStrokeWidth, progress);
        solidPaint.setColor(foreground);
        solidPaint.setStrokeWidth(strokeWidth);
        solidPaint.setAlpha(modulatedAlpha(lockscreenProgress));
        if (solidPaint.getAlpha() > 0) {
            canvas.drawPath(drawPath, solidPaint);
        }
        dashedPaint.setColor(foreground);
        dashedPaint.setStrokeWidth(strokeWidth);
        dashedPaint.setAlpha(modulatedAlpha(progress));
        if (dashedPaint.getAlpha() > 0) {
            canvas.drawPath(drawPath, dashedPaint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        float squareSize = Math.min(bounds.width(), bounds.height());
        centerX = bounds.exactCenterX();
        centerY = bounds.exactCenterY();
        float scale = (squareSize / VIEWPORT_SIZE) * FOREGROUND_SCALE;
        pathMatrix.reset();
        pathMatrix.setScale(scale, scale);
        float sourceCenter = (VIEWPORT_SIZE / 2f) * scale;
        pathMatrix.postTranslate(centerX - sourceCenter, centerY - sourceCenter);
        drawPath.reset();
        sourcePath.transform(pathMatrix, drawPath);
        lockscreenStrokeWidth = LOCKSCREEN_STROKE_DP * scale;
        aodStrokeWidth = AOD_STROKE_DP * scale;
        dashedPaint.setPathEffect(new DashPathEffect(
                new float[] { AOD_DASH_DP * scale, AOD_DASH_GAP_DP * scale }, 0f));
    }

    @Override
    public int getIntrinsicWidth() {
        return intrinsicSize;
    }

    @Override
    public int getIntrinsicHeight() {
        return intrinsicSize;
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
    public int getAlpha() {
        return drawableAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        solidPaint.setColorFilter(colorFilter);
        dashedPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    private int modulatedAlpha(float progress) {
        return Math.round(drawableAlpha * clamp(progress));
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float start, float end, float progress) {
        return start + (end - start) * progress;
    }
}
