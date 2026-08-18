package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** COUI-style HDR illumination carrier used only while the optical sensor is pressed. */
final class CouiUdfpsPressedIlluminationDrawable extends Drawable {
    private static final float DIAMETER_DP = 64f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int diameterPx;
    private int drawableAlpha = 0;

    CouiUdfpsPressedIlluminationDrawable(Context context) {
        float density = context.getResources().getDisplayMetrics().density;
        diameterPx = Math.round(DIAMETER_DP * density);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.pack(
                7f,
                7f,
                7f,
                1f,
                ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)));
        paint.setAlpha(0);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float radius = Math.min(
                diameterPx,
                Math.min(bounds.width(), bounds.height())) / 2f;
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        drawableAlpha = Math.max(0, Math.min(255, alpha));
        paint.setAlpha(drawableAlpha);
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

    @Override
    public int getIntrinsicWidth() {
        return diameterPx;
    }

    @Override
    public int getIntrinsicHeight() {
        return diameterPx;
    }
}
