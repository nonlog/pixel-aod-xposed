package dev.codex.pixelaod;

import android.animation.TimeInterpolator;
import android.graphics.Canvas;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

/** TextView that delegates persistent-large glyph drawing to the optional ROM morph bridge. */
final class CouiClockMorphingGlyphView extends TextView {
    private final CouiClockRomTextAnimatorRuntime runtime;
    private CouiClockRomTextAnimatorRuntime.Bridge bridge;
    private PendingStyle pendingStyle;
    private boolean layoutNeedsUpdate = true;
    private String targetVariation;
    private Integer targetColor;

    CouiClockMorphingGlyphView(android.content.Context context,
            CouiClockRomTextAnimatorRuntime runtime) {
        super(context);
        this.runtime = runtime;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Layout layout = getLayout();
        if (layout == null) {
            return;
        }
        try {
            if (bridge == null) {
                CouiClockRomTextAnimatorRuntime.Bridge created = runtime.createBridge(layout, this);
                if (created != null) {
                    bridge = created;
                    if (pendingStyle != null) {
                        bridge.setStyle(pendingStyle.style, pendingStyle.interpolator);
                    }
                    pendingStyle = null;
                }
            } else if (changed || layoutNeedsUpdate) {
                bridge.updateLayout(layout);
            }
        } catch (Throwable t) {
            bridge = null;
            PixelAodLog.log("COUI morphing glyph layout bridge unavailable", t);
        }
        layoutNeedsUpdate = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bridge == null) {
            super.onDraw(canvas);
            return;
        }
        try {
            bridge.draw(canvas);
        } catch (Throwable t) {
            bridge = null;
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int count) {
        super.onTextChanged(text, start, before, count);
        layoutNeedsUpdate = true;
    }

    void setMorphStyle(String variation, int color, boolean animate, long duration,
            TimeInterpolator interpolator) {
        if (variation == null || interpolator == null) {
            return;
        }
        if (variation.equals(targetVariation) && targetColor != null
                && targetColor == color) {
            return;
        }
        targetVariation = variation;
        targetColor = color;
        CouiClockFontPolicy.MorphStyleSpec style = CouiClockFontPolicy.morphStyle(
                sceneForVariation(variation), isAodVariation(variation), color, animate, duration);
        if (bridge != null) {
            bridge.setStyle(style, interpolator);
        } else {
            // Match the reference: pending styles are applied immediately once the bridge is
            // created, while the TextView remains a safe visual fallback if creation fails.
            pendingStyle = new PendingStyle(
                    new CouiClockFontPolicy.MorphStyleSpec(variation, color, false, 0L),
                    interpolator);
            setTextColor(color);
        }
    }

    private static boolean isAodVariation(String variation) {
        return CouiClockFontPolicy.AOD_LARGE_VARIATION.equals(variation)
                || CouiClockFontPolicy.AOD_SMALL_VARIATION.equals(variation);
    }

    private static CouiClockPresentationModel.Scene sceneForVariation(String variation) {
        return CouiClockFontPolicy.LARGE_VARIATION.equals(variation)
                || CouiClockFontPolicy.AOD_LARGE_VARIATION.equals(variation)
                ? CouiClockPresentationModel.Scene.LARGE
                : CouiClockPresentationModel.Scene.SMALL;
    }

    private static final class PendingStyle {
        final CouiClockFontPolicy.MorphStyleSpec style;
        final TimeInterpolator interpolator;

        PendingStyle(CouiClockFontPolicy.MorphStyleSpec style, TimeInterpolator interpolator) {
            this.style = style;
            this.interpolator = interpolator;
        }
    }
}
