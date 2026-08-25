package dev.codex.pixelaod;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Small monochrome glyphs for the dedicated Live Update metric surface. */
final class LiveUpdateGlyphDrawable extends Drawable {
    private final ContextualAtAGlanceCard.LiveUpdateKind kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color;

    LiveUpdateGlyphDrawable(ContextualAtAGlanceCard.LiveUpdateKind kind, int color) {
        this.kind = kind != null ? kind : ContextualAtAGlanceCard.LiveUpdateKind.NONE;
        this.color = color;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty() || kind == ContextualAtAGlanceCard.LiveUpdateKind.NONE) {
            return;
        }
        float size = Math.min(b.width(), b.height());
        float ox = b.left + (b.width() - size) / 2f;
        float oy = b.top + (b.height() - size) / 2f;
        float cx = ox + size / 2f;
        float cy = oy + size / 2f;
        paint.setColor(color);
        paint.setAlpha(255);
        paint.setStrokeWidth(Math.max(1.5f, size * 0.085f));
        paint.setStyle(Paint.Style.STROKE);

        switch (kind) {
            case TIMER:
                drawTimer(canvas, ox, oy, size, cx, cy);
                break;
            case HOTSPOT:
                drawHotspot(canvas, ox, oy, size, cx, cy);
                break;
            case PROGRESS:
                drawProgress(canvas, ox, oy, size);
                break;
            case CALL:
                drawCall(canvas, ox, oy, size);
                break;
            default:
                break;
        }
    }

    private void drawTimer(Canvas canvas, float ox, float oy, float size, float cx, float cy) {
        float r = size * 0.31f;
        canvas.drawCircle(cx, cy + size * 0.04f, r, paint);
        canvas.drawLine(cx - size * 0.10f, oy + size * 0.12f,
                cx + size * 0.10f, oy + size * 0.12f, paint);
        canvas.drawLine(cx, oy + size * 0.12f, cx, oy + size * 0.19f, paint);
        canvas.drawLine(cx, cy + size * 0.04f, cx, cy - size * 0.14f, paint);
        canvas.drawLine(cx, cy + size * 0.04f, cx + size * 0.13f,
                cy + size * 0.11f, paint);
    }

    private void drawHotspot(Canvas canvas, float ox, float oy, float size, float cx, float cy) {
        RectF outer = new RectF(ox + size * 0.16f, oy + size * 0.16f,
                ox + size * 0.84f, oy + size * 0.84f);
        RectF inner = new RectF(ox + size * 0.29f, oy + size * 0.29f,
                ox + size * 0.71f, oy + size * 0.71f);
        canvas.drawArc(outer, 205f, 130f, false, paint);
        canvas.drawArc(inner, 205f, 130f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, oy + size * 0.68f, size * 0.075f, paint);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void drawProgress(Canvas canvas, float ox, float oy, float size) {
        RectF box = new RectF(ox + size * 0.18f, oy + size * 0.22f,
                ox + size * 0.82f, oy + size * 0.78f);
        canvas.drawRoundRect(box, size * 0.12f, size * 0.12f, paint);
        float y = oy + size * 0.50f;
        canvas.drawLine(ox + size * 0.30f, y, ox + size * 0.70f, y, paint);
    }

    private void drawCall(Canvas canvas, float ox, float oy, float size) {
        Path p = new Path();
        p.moveTo(ox + size * 0.28f, oy + size * 0.20f);
        p.cubicTo(ox + size * 0.18f, oy + size * 0.34f,
                ox + size * 0.30f, oy + size * 0.58f,
                ox + size * 0.48f, oy + size * 0.72f);
        p.cubicTo(ox + size * 0.62f, oy + size * 0.83f,
                ox + size * 0.77f, oy + size * 0.79f,
                ox + size * 0.82f, oy + size * 0.68f);
        canvas.drawPath(p, paint);
        canvas.drawLine(ox + size * 0.25f, oy + size * 0.19f,
                ox + size * 0.38f, oy + size * 0.34f, paint);
        canvas.drawLine(ox + size * 0.67f, oy + size * 0.62f,
                ox + size * 0.82f, oy + size * 0.68f, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 24;
    }

    @Override
    public int getIntrinsicHeight() {
        return 24;
    }
}