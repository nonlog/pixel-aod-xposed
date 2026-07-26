package dev.codex.pixelaod;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;

/**
 * Keeps OOS temporary-show {@code OplusAnimationDrawable} alive for optical/AOD lifecycle
 * (start/stop/invalidate ticks) while painting only the Pixel ridge — no per-frame Xposed
 * draw hook (that was a major unlock/screen-off jank source).
 */
final class PixelFingerprintAnimCarrier extends Drawable implements Animatable, Drawable.Callback {
    private final Drawable vendor;
    private final PixelFingerprintDrawable pixel;
    private int alpha = 255;

    PixelFingerprintAnimCarrier(Drawable vendor, PixelFingerprintDrawable pixel) {
        this.vendor = vendor;
        this.pixel = pixel;
        if (vendor != null) {
            vendor.setCallback(this);
        }
    }

    Drawable getVendor() {
        return vendor;
    }

    PixelFingerprintDrawable getPixel() {
        return pixel;
    }

    @Override
    public void draw(Canvas canvas) {
        if (pixel == null || alpha <= 0) {
            return;
        }
        Rect bounds = getBounds();
        if (bounds.isEmpty()) {
            return;
        }
        int size = Math.max(1, pixel.getIntrinsicWidth());
        int cx = bounds.centerX();
        int cy = bounds.centerY();
        int left = cx - size / 2;
        int top = cy - size / 2;
        pixel.setBounds(left, top, left + size, top + size);
        int previous = pixel.getAlpha();
        if (previous != alpha) {
            pixel.setAlpha(alpha);
        }
        int save = canvas.save();
        try {
            pixel.draw(canvas);
        } finally {
            canvas.restoreToCount(save);
            if (previous != alpha) {
                pixel.setAlpha(previous);
            }
        }
    }

    @Override
    public void start() {
        if (vendor instanceof Animatable) {
            ((Animatable) vendor).start();
        }
    }

    @Override
    public void stop() {
        if (vendor instanceof Animatable) {
            ((Animatable) vendor).stop();
        }
    }

    @Override
    public boolean isRunning() {
        return vendor instanceof Animatable && ((Animatable) vendor).isRunning();
    }

    @Override
    public void setAlpha(int alpha) {
        this.alpha = Math.max(0, Math.min(255, alpha));
        if (vendor != null) {
            vendor.setAlpha(this.alpha);
        }
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        if (pixel != null) {
            pixel.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        if (vendor != null) {
            int w = vendor.getIntrinsicWidth();
            if (w > 0) {
                return w;
            }
        }
        return pixel != null ? pixel.getIntrinsicWidth() : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        if (vendor != null) {
            int h = vendor.getIntrinsicHeight();
            if (h > 0) {
                return h;
            }
        }
        return pixel != null ? pixel.getIntrinsicHeight() : -1;
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (vendor != null) {
            vendor.setVisible(visible, restart);
        }
        return changed;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        // Vendor anim frame tick → repaint Pixel ridge without stock frames.
        invalidateSelf();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }
}
