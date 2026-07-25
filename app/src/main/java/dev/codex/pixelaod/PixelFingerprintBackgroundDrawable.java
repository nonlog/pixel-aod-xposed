package dev.codex.pixelaod;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.animation.PathInterpolator;

import java.lang.reflect.Field;

final class PixelFingerprintBackgroundDrawable extends Drawable {
    private static final float DIAMETER_DP = 56f;
    private static final long TRANSITION_DURATION_MS = 420L;
    private static final PathInterpolator TRANSITION_INTERPOLATOR =
            new PathInterpolator(0.2f, 0f, 0f, 1f);

    private final Context context;
    private final float density;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX;
    private float centerY;
    private float radius;
    private float opacity;
    private int drawableAlpha = 255;
    private int surfaceColor;
    private boolean targetAodStyle;
    private boolean targetDark;
    private ValueAnimator animator;

    PixelFingerprintBackgroundDrawable(Context context, boolean aodStyle, boolean dark) {
        this.context = context;
        density = context.getResources().getDisplayMetrics().density;
        targetAodStyle = aodStyle;
        targetDark = dark;
        opacity = aodStyle ? 0f : 1f;
        refreshPalette();
    }

    void transitionTo(boolean aodStyle, boolean dark, boolean animate) {
        boolean paletteChanged = targetDark != dark;
        if (targetAodStyle == aodStyle && !paletteChanged) {
            return;
        }
        targetAodStyle = aodStyle;
        targetDark = dark;
        refreshPalette();
        float targetOpacity = aodStyle ? 0f : 1f;
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (!animate || getCallback() == null) {
            opacity = targetOpacity;
            invalidateSelf();
            return;
        }
        animator = ValueAnimator.ofFloat(opacity, targetOpacity);
        animator.setDuration(TRANSITION_DURATION_MS);
        animator.setInterpolator(TRANSITION_INTERPOLATOR);
        animator.addUpdateListener(valueAnimator -> {
            opacity = (Float) valueAnimator.getAnimatedValue();
            invalidateSelf();
        });
        animator.start();
    }

    private void refreshPalette() {
        int fallback = targetDark ? Color.rgb(31, 31, 31) : Color.rgb(255, 251, 254);
        int resolved = resolveThemeColor(resolveColorSurfaceAttribute(), fallback);
        float luminance = Color.luminance(resolved);
        if ((targetDark && luminance > 0.36f) || (!targetDark && luminance < 0.64f)) {
            resolved = fallback;
        }
        surfaceColor = PixelFingerprintIconPolicy.opaqueColor(resolved);
    }

    private static int resolveColorSurfaceAttribute() {
        try {
            Class<?> attrClass = Class.forName("com.android.internal.R$attr");
            Field field = attrClass.getField("colorSurface");
            return field.getInt(null);
        } catch (Throwable ignored) {
            return 0;
        }
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
        int alpha = Math.round(drawableAlpha * Math.max(0f, Math.min(1f, opacity)));
        if (alpha <= 0 || radius <= 0f) {
            return;
        }
        paint.setColor(surfaceColor);
        paint.setAlpha(alpha);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        centerX = bounds.exactCenterX();
        centerY = bounds.exactCenterY();
        radius = Math.min(Math.min(bounds.width(), bounds.height()) / 2f,
                DIAMETER_DP * density / 2f);
    }

    @Override
    public void setAlpha(int alpha) {
        drawableAlpha = Math.max(0, Math.min(255, alpha));
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return drawableAlpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
