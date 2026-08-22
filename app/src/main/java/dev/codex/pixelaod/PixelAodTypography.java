package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

/**
 * Shared typography and text-measurement facade for clock presentations.
 *
 * <p>M8 keeps the proven implementation in {@link PixelAodClockView} while removing direct
 * presentation-layer dependency on that legacy View as a utility namespace. Implementation can
 * move behind this facade incrementally without changing the COUI presentation contract.</p>
 */
final class PixelAodTypography {
    private PixelAodTypography() {
    }

    static void prewarmGoogleSansFlex(Context context) {
        PixelAodClockView.prewarmGoogleSansFlex(context);
    }

    static void setModulePath(String modulePath) {
        PixelAodClockView.setModulePath(modulePath);
    }

    static Typeface sharedClockTypeface(Context context, int weight) {
        return PixelAodClockView.sharedClockTypeface(context, weight);
    }

    static Typeface sharedInfoTypeface(Context context, int weight) {
        return PixelAodClockView.sharedInfoTypeface(context, weight);
    }

    static int aodClockWeight(Context context) {
        return PixelAodClockView.aodClockWeight(context);
    }

    static int lockscreenClockWeight(Context context) {
        return PixelAodClockView.lockscreenClockWeight(context);
    }

    static int scaledClockTextDp(Context context, int baseDp) {
        return PixelAodClockView.scaledClockTextDp(context, baseDp);
    }

    static TextView makeInfoLine(Context context, Typeface typeface, int weight,
            int textSizeDp, int gravity) {
        return PixelAodClockView.makeInfoLine(context, typeface, weight, textSizeDp, gravity);
    }

    static void applySharedInfoText(TextView textView, Context context, CharSequence text) {
        PixelAodClockView.applySharedInfoText(textView, context, text);
    }

    static void applySharedClockTypeface(TextView textView, Context context, int weight) {
        PixelAodClockView.applySharedClockTypeface(textView, context, weight);
    }

    static void applySharedClockTextStyle(TextView textView, Context context, int weight,
            int textSizeDp, boolean compact) {
        PixelAodClockView.applySharedClockTextStyle(textView, context, weight, textSizeDp, compact);
    }

    static void applySharedClockLetterSpacing(TextView textView, boolean compact) {
        PixelAodClockView.applySharedClockLetterSpacing(textView, compact);
    }

    static void applySharedClockText(TextView textView, Context context, CharSequence text,
            boolean compact) {
        PixelAodClockView.applySharedClockText(textView, context, text, compact);
    }

    static float fixedClockGlyphReferenceAdvancePx(TextView textView, char glyph) {
        return PixelAodClockView.fixedClockGlyphReferenceAdvancePx(textView, glyph);
    }

    static float measuredTextAdvancePx(TextView textView) {
        return PixelAodClockView.measuredTextAdvancePx(textView);
    }

    static int estimatedTextContentWidthPx(TextView textView) {
        return PixelAodClockView.estimatedTextContentWidthPx(textView);
    }

    static String sharedClockFontVariationSettings(int weight) {
        return PixelAodClockView.sharedClockFontVariationSettings(weight);
    }

    static String describeClockTextView(TextView textView) {
        return PixelAodClockView.describeClockTextView(textView);
    }

    static String describeViewForHandoff(View view) {
        return PixelAodClockView.describeViewForHandoff(view);
    }

    static int resolveMaterialClockColor(Context context) {
        return PixelAodClockView.resolveMaterialClockColor(context);
    }

    static int resolveMaterialInfoColor(Context context) {
        return PixelAodClockView.resolveMaterialInfoColor(context);
    }
}
