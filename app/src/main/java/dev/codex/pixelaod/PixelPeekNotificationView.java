package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * Authentic Google Pixel Ambient Display (AOD) incoming notification pulse view.
 * Renders directly on pure OLED black (no gray card box or gray icon badge) aligned to the
 * Pixel compact clock margin: monochrome small icon + muted app header, bold title, 2-line body.
 */
final class PixelPeekNotificationView extends View {
    private static final float SIDE_MARGIN_DP = 24f;
    private static final float HEADER_ICON_DP = 17f;
    private static final float HEADER_ICON_GAP_DP = 8f;
    private static final float HEADER_BOTTOM_GAP_DP = 6f;
    private static final float TITLE_BODY_GAP_DP = 3f;
    private static final float HEADER_TEXT_SP = 12.5f;
    private static final float TITLE_TEXT_SP = 16f;
    private static final float BODY_TEXT_SP = 14.5f;

    private static final int HEADER_TEXT_COLOR = Color.rgb(160, 165, 172);
    private static final int PRIMARY_TEXT_COLOR = Color.rgb(242, 242, 242);
    private static final int SECONDARY_TEXT_COLOR = Color.rgb(184, 188, 194);

    private final TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Rect oldIconBounds = new Rect();
    private PixelPeekNotificationContent content;
    private float cardTopPx = Float.NaN;

    PixelPeekNotificationView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        headerPaint.setColor(HEADER_TEXT_COLOR);
        headerPaint.setTextSize(sp(HEADER_TEXT_SP));
        headerPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        titlePaint.setColor(PRIMARY_TEXT_COLOR);
        titlePaint.setTextSize(sp(TITLE_TEXT_SP));
        titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        bodyPaint.setColor(SECONDARY_TEXT_COLOR);
        bodyPaint.setTextSize(sp(BODY_TEXT_SP));
        bodyPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    void setContent(PixelPeekNotificationContent content) {
        this.content = content;
        invalidate();
    }

    void setCardTopPx(float cardTopPx) {
        if (Float.compare(this.cardTopPx, cardTopPx) == 0) {
            return;
        }
        this.cardTopPx = cardTopPx;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        PixelPeekNotificationContent current = content;
        if (current == null || !current.hasRenderableText() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }

        float side = dp(SIDE_MARGIN_DP);
        float left = side;
        float right = getWidth() - side;
        if (right <= left) {
            return;
        }
        int contentWidth = Math.max(1, Math.round(right - left));
        float top = Float.isNaN(cardTopPx)
                ? dp(PixelPeekGeometryPolicy.MIN_CARD_TOP_DP) : cardTopPx;
        float maxBottom = getHeight() - dp(PixelPeekGeometryPolicy.BOTTOM_SAFE_AREA_DP);

        CharSequence headline = current.title.length() > 0 ? current.title : current.appName;
        boolean showHeaderAppName = current.appName.length() > 0
                && !TextUtils.equals(current.appName, headline);
        CharSequence bodySource = current.message.length() > 0
                ? current.message
                : (current.title.length() > 0 && !showHeaderAppName ? current.appName : "");

        float cursorY = top;
        Drawable icon = current.appIcon;
        float iconSize = dp(HEADER_ICON_DP);
        float iconGap = dp(HEADER_ICON_GAP_DP);

        // Top ambient header row: crisp monochrome small icon + muted app name on pure OLED black.
        if (icon != null || showHeaderAppName) {
            Paint.FontMetrics headerFm = headerPaint.getFontMetrics();
            float headerTextHeight = headerFm.descent - headerFm.ascent;
            float rowHeight = Math.max(icon != null ? iconSize : 0f,
                    showHeaderAppName ? headerTextHeight : 0f);
            float rowCenterY = cursorY + (rowHeight / 2f);
            float headerTextLeft = left;
            if (icon != null) {
                int tintColor = PixelAodClockView.resolveMaterialInfoColor(getContext());
                Drawable tinted = icon.mutate();
                tinted.setTint(tintColor != 0 ? tintColor : PRIMARY_TEXT_COLOR);
                tinted.setTintMode(PorterDuff.Mode.SRC_IN);
                int iconLeft = Math.round(left);
                int iconTop = Math.round(rowCenterY - (iconSize / 2f));
                int sizePx = Math.round(iconSize);
                oldIconBounds.set(tinted.getBounds());
                tinted.setBounds(iconLeft, iconTop, iconLeft + sizePx, iconTop + sizePx);
                tinted.draw(canvas);
                tinted.setBounds(oldIconBounds);
                headerTextLeft += iconSize + iconGap;
            }
            if (showHeaderAppName) {
                int headerWidth = Math.max(1, Math.round(right - headerTextLeft));
                CharSequence ellipsizedApp = ellipsize(current.appName, headerPaint, headerWidth);
                float baseline = rowCenterY - ((headerFm.ascent + headerFm.descent) / 2f);
                canvas.drawText(ellipsizedApp, 0, ellipsizedApp.length(),
                        headerTextLeft, baseline, headerPaint);
            }
            cursorY += rowHeight + dp(HEADER_BOTTOM_GAP_DP);
        }

        // Bold title line.
        CharSequence ellipsizedTitle = ellipsize(headline, titlePaint, contentWidth);
        if (ellipsizedTitle.length() > 0 && cursorY < maxBottom) {
            Paint.FontMetrics titleFm = titlePaint.getFontMetrics();
            float titleHeight = titleFm.descent - titleFm.ascent;
            float titleBaseline = cursorY - titleFm.ascent;
            canvas.drawText(ellipsizedTitle, 0, ellipsizedTitle.length(),
                    left, titleBaseline, titlePaint);
            cursorY += titleHeight + dp(TITLE_BODY_GAP_DP);
        }

        // Clean 2-line message body.
        StaticLayout bodyLayout = buildBodyLayout(bodySource, contentWidth);
        if (bodyLayout != null && bodyLayout.getHeight() > 0 && cursorY < maxBottom) {
            canvas.save();
            canvas.clipRect(left, cursorY, right, maxBottom);
            canvas.translate(left, cursorY);
            bodyLayout.draw(canvas);
            canvas.restore();
        }
    }

    private StaticLayout buildBodyLayout(CharSequence message, int width) {
        if (message == null || message.length() == 0) {
            return null;
        }
        return StaticLayout.Builder.obtain(message, 0, message.length(), bodyPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.18f)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
    }

    private static CharSequence ellipsize(CharSequence text, TextPaint paint, int width) {
        if (text == null || text.length() == 0) {
            return "";
        }
        return TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
