package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** One native-style overflow dot replacing the legacy +N notification label. */
final class NotificationOverflowDotView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int color = Color.WHITE;
    private int diameterPx = 1;

    NotificationOverflowDotView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setVisibility(GONE);
    }

    void configure(int color, int diameterPx) {
        boolean changed = this.color != color || this.diameterPx != Math.max(1, diameterPx);
        this.color = color;
        this.diameterPx = Math.max(1, diameterPx);
        if (changed) {
            invalidate();
        }
    }

    void setDotColor(int color) {
        if (this.color != color) {
            this.color = color;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        float radius = diameterPx / 2f;
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
    }
}