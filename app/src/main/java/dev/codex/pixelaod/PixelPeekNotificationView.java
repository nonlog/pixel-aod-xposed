package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

/**
 * Transient Pixel Ambient Display notification pulse view whose lifetime is owned
 * by the OPlus AOD surface. Uses a restrained OLED-black minimal pill with an inline
 * app header row matching native Pixel Android 15/16/17 ambient pulses.
 */
final class PixelPeekNotificationView extends View {
    private static final float CARD_SIDE_MARGIN_DP = 24f;
    private static final float CARD_RADIUS_DP = 20f;
    private static final float CARD_MIN_HEIGHT_DP = 64f;
    private static final float CARD_MAX_HEIGHT_DP = 112f;
    private static final float PADDING_H_DP = 16f;
    private static final float PADDING_V_DP = 13f;
    private static final float HEADER_ICON_DP = 17f;
    private static final float HEADER_ICON_GAP_DP = 8f;
    private static final float HEADER_TEXT_SP = 12.5f;
    private static final float TITLE_TEXT_SP = 15.5f;
    private static final float MESSAGE_TEXT_SP = 13.5f;
    private static final float STROKE_WIDTH_DP = 0.75f;

    // OLED-friendly near-black ambient surface with a delicate hairline outline.
    private static final int AMBIENT_SURFACE_COLOR = 0xFF0E1013;
    private static final int AMBIENT_STROKE_COLOR = 0x26FFFFFF;
    private static final int HEADER_TEXT_COLOR = Color.rgb(168, 176, 186);
    private static final int PRIMARY_TEXT_COLOR = Color.WHITE;
    private static final int SECONDARY_TEXT_COLOR = Color.rgb(210, 214, 220);

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cardRect = new RectF();
    private final Rect oldIconBounds = new Rect();
    private PixelPeekNotificationContent content;
    private float cardTopPx = Float.NaN;

    PixelPeekNotificationView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        surfacePaint.setStyle(Paint.Style.FILL);
        surfacePaint.setColor(AMBIENT_SURFACE_COLOR);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(STROKE_WIDTH_DP));
        strokePaint.setColor(AMBIENT_STROKE_COLOR);

        Typeface mediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        Typeface regularTypeface = Typeface.create("sans-serif", Typeface.NORMAL);

        headerPaint.setColor(HEADER_TEXT_COLOR);
        headerPaint.setTextSize(sp(HEADER_TEXT_SP));
        headerPaint.setTypeface(mediumTypeface);
        headerPaint.setLetterSpacing(0.01f);

        titlePaint.setColor(PRIMARY_TEXT_COLOR);
        titlePaint.setTextSize(sp(TITLE_TEXT_SP));
        titlePaint.setTypeface(mediumTypeface);

        messagePaint.setColor(SECONDARY_TEXT_COLOR);
        messagePaint.setTextSize(sp(MESSAGE_TEXT_SP));
        messagePaint.setTypeface(regularTypeface);
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

        float side = dp(CARD_SIDE_MARGIN_DP);
        float left = side;
        float right = getWidth() - side;
        if (right <= left) {
            return;
        }
        float top = Float.isNaN(cardTopPx)
                ? dp(PixelPeekGeometryPolicy.MIN_CARD_TOP_DP) : cardTopPx;
        float padH = dp(PADDING_H_DP);
        float padV = dp(PADDING_V_DP);
        float iconSize = dp(HEADER_ICON_DP);
        boolean hasIcon = current.appIcon != null;
        float contentLeft = left + padH;
        float contentRight = right - padH;
        int fullTextWidth = Math.max(1, Math.round(contentRight - contentLeft));

        float headerTextLeft = hasIcon ? contentLeft + iconSize + dp(HEADER_ICON_GAP_DP) : contentLeft;
        int headerTextWidth = Math.max(1, Math.round(contentRight - headerTextLeft));

        CharSequence appName = ellipsize(current.appName, headerPaint, headerTextWidth);
        CharSequence title = ellipsize(current.title, titlePaint, fullTextWidth);
        StaticLayout messageLayout = buildMessageLayout(current.message, fullTextWidth);

        Paint.FontMetrics headerFm = headerPaint.getFontMetrics();
        Paint.FontMetrics titleFm = titlePaint.getFontMetrics();
        float headerTextHeight = headerFm.descent - headerFm.ascent;
        float headerRowHeight = Math.max(hasIcon ? iconSize : 0f, headerTextHeight);
        float titleHeight = title.length() > 0 ? (titleFm.descent - titleFm.ascent) : 0f;

        float bodyHeight = headerRowHeight;
        if (title.length() > 0) {
            bodyHeight += dp(5f) + titleHeight;
        }
        if (messageLayout != null && messageLayout.getHeight() > 0) {
            bodyHeight += dp(3f) + messageLayout.getHeight();
        }
        float cardHeight = clamp(bodyHeight + (padV * 2f),
                dp(CARD_MIN_HEIGHT_DP), dp(CARD_MAX_HEIGHT_DP));
        float bottom = Math.min(getHeight() - side, top + cardHeight);

        float radius = dp(CARD_RADIUS_DP);
        cardRect.set(left, top, right, bottom);
        canvas.drawRoundRect(cardRect, radius, radius, surfacePaint);
        float strokeInset = dp(STROKE_WIDTH_DP) / 2f;
        cardRect.inset(strokeInset, strokeInset);
        canvas.drawRoundRect(cardRect, radius, radius, strokePaint);

        float headerRowTop = top + padV;
        if (hasIcon) {
            Drawable icon = current.appIcon;
            int iconLeft = Math.round(contentLeft);
            int iconTop = Math.round(headerRowTop + ((headerRowHeight - iconSize) / 2f));
            int size = Math.round(iconSize);
            oldIconBounds.set(icon.getBounds());
            icon.setBounds(iconLeft, iconTop, iconLeft + size, iconTop + size);
            icon.draw(canvas);
            icon.setBounds(oldIconBounds);
        }
        if (appName.length() > 0) {
            float headerBaseline = headerRowTop + ((headerRowHeight - headerTextHeight) / 2f) - headerFm.ascent;
            canvas.drawText(appName, 0, appName.length(), headerTextLeft, headerBaseline, headerPaint);
        }

        float cursorY = headerRowTop + headerRowHeight;
        if (title.length() > 0) {
            cursorY += dp(5f);
            float titleBaseline = cursorY - titleFm.ascent;
            canvas.drawText(title, 0, title.length(), contentLeft, titleBaseline, titlePaint);
            cursorY += titleHeight;
        }
        if (messageLayout != null && messageLayout.getHeight() > 0) {
            float messageTop = cursorY + dp(3f);
            float availableMessageHeight = (bottom - padV) - messageTop;
            if (availableMessageHeight > 0f) {
                canvas.save();
                canvas.clipRect(contentLeft, messageTop, contentRight, bottom - padV);
                canvas.translate(contentLeft, messageTop);
                messageLayout.draw(canvas);
                canvas.restore();
            }
        }
    }

    private StaticLayout buildMessageLayout(CharSequence message, int width) {
        if (message == null || message.length() == 0) {
            return null;
        }
        return StaticLayout.Builder.obtain(message, 0, message.length(), messagePaint, width)
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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
