package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

/**
 * Lockscreen idle Pixel UDFPS carrier hosted strictly below OPlus's OnScreenFingerprintDimLayer.
 *
 * <p>On global-HBM + DimLayer devices (e.g. OnePlus 12), OPlus SurfaceFlinger hardcodes HWC
 * z-order by window title so {@code OnScreenFingerprintIcon} always sits above
 * {@code OnScreenFingerprintDimLayer}. Drawing a filled 64dp circle on that un-dimmed HBM plane
 * causes physical OLED glare that screenshots cannot capture. Hosting the lockscreen idle glyph
 * in a normal SystemUI overlay window ({@code TYPE_NAVIGATION_BAR_PANEL}, type 2024) places it
 * above {@code NotificationShade} and strictly below {@code OnScreenFingerprintDimLayer}, so
 * SurfaceFlinger's hardware-calibrated dim layer dims it identically to the rest of the
 * lockscreen while {@code OnScreenFingerprintPressedIcon} stays above {@code DimLayer} for
 * full-HBM optical authentication.</p>
 */
final class CouiUdfpsDimmedIconOverlay {
    private static final int[] WINDOW_TYPES = { 2024, 2027, 2026, 2017, 2038 };
    private static final int WINDOW_FLAGS =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
    private static final String WINDOW_TITLE = "PixelUdfpsDimmedIcon";

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WindowManager windowManager;
    private final ImageView view;
    private final CouiUdfpsDrawable drawable;
    private WindowManager.LayoutParams layoutParams;
    private boolean added;
    private int lastX = Integer.MIN_VALUE;
    private int lastY = Integer.MIN_VALUE;
    private int lastSize = -1;

    CouiUdfpsDimmedIconOverlay(Context context, boolean dark) {
        windowManager = context.getSystemService(WindowManager.class);
        drawable = new CouiUdfpsDrawable(context, false, dark);
        view = new ImageView(context);
        view.setScaleType(ImageView.ScaleType.CENTER);
        view.setImageDrawable(drawable);
        view.setVisibility(View.GONE);
    }

    void show(float centerX, float centerY, boolean dark, boolean refreshColors) {
        runOnMain(() -> {
            int size = Math.max(1, drawable.getIntrinsicWidth());
            int x = Math.round(centerX - size / 2f);
            int y = Math.round(centerY - size / 2f);
            if (!ensureAdded(x, y, size)) {
                return;
            }
            if (layoutParams != null && (lastX != x || lastY != y || lastSize != size)) {
                layoutParams.x = x;
                layoutParams.y = y;
                layoutParams.width = size;
                layoutParams.height = size;
                lastX = x;
                lastY = y;
                lastSize = size;
                try {
                    windowManager.updateViewLayout(view, layoutParams);
                } catch (Throwable throwable) {
                    PixelAodLog.log("COUI UDFPS dimmed icon layout update failed", throwable);
                }
            }
            drawable.setPressed(false);
            drawable.setExitFadeAlpha(1f);
            drawable.transitionTo(false, dark, false, refreshColors);
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {
        runOnMain(() -> {
            if (added && view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
            }
        });
    }

    void destroy() {
        runOnMain(() -> {
            if (!added || windowManager == null) {
                return;
            }
            try {
                windowManager.removeViewImmediate(view);
            } catch (Throwable ignored) {
            }
            added = false;
            layoutParams = null;
        });
    }

    private boolean ensureAdded(int x, int y, int size) {
        if (added) {
            return true;
        }
        if (windowManager == null) {
            PixelAodLog.log("COUI UDFPS dimmed icon unavailable reason=no-window-manager");
            return false;
        }
        Throwable lastError = null;
        for (int type : WINDOW_TYPES) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    size, size, type, WINDOW_FLAGS, PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = x;
            params.y = y;
            params.windowAnimations = 0;
            if (Build.VERSION.SDK_INT >= 30) {
                params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                params.setFitInsetsTypes(0);
            } else if (Build.VERSION.SDK_INT >= 28) {
                params.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            params.setTitle(WINDOW_TITLE);
            try {
                windowManager.addView(view, params);
                added = true;
                layoutParams = params;
                lastX = x;
                lastY = y;
                lastSize = size;
                PixelAodLog.i("COUI UDFPS dimmed icon overlay added type=" + type
                        + " x=" + x + " y=" + y + " size=" + size);
                return true;
            } catch (Throwable throwable) {
                lastError = throwable;
            }
        }
        PixelAodLog.log("COUI UDFPS dimmed icon overlay add failed", lastError);
        return false;
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
}
