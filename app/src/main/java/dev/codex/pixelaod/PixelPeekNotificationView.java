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

/** Transient Pixel-style notification card whose lifetime is owned by the OPlus AOD surface. */
final class PixelPeekNotificationView extends View {
    private static final float CARD_SIDE_MARGIN_DP = 20f;
    private static final float CARD_RADIUS_DP = 24f;
    private static final float CARD_MIN_HEIGHT_DP = 92f;
    private static final float CARD_MAX_HEIGHT_DP = 118f;
    private static final float CONTENT_PADDING_DP = 18f;
    private static final float APP_ICON_DP = 38f;
    private static final float ICON_TEXT_GAP_DP = 14f;
    private static final float HEADER_TEXT_SP = 13f;
    private static final float TITLE_TEXT_SP = 16f;
    private static final float MESSAGE_TEXT_SP = 14f;
    // Keep the pulse card visibly distinct from the #000000 Doze canvas without turning it into
    // the medium-gray ColorOS surface. Pixel's ambient notification treatment is effectively a
    // near-black tonal surface.
    private static final int PRIMARY_TEXT_COLOR = Color.WHITE;
    private static final int SECONDARY_TEXT_COLOR = Color.rgb(218, 220, 224);

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint headerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint messagePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private PixelPeekNotificationContent content;
    private float cardTopPx = Float.NaN;

    PixelPeekNotificationView(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        cardPaint.setColor(PixelPeekMaterialYouPalette.background(context));
        headerPaint.setColor(SECONDARY_TEXT_COLOR);
        headerPaint.setTextSize(sp(HEADER_TEXT_SP));
        headerPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titlePaint.setColor(PRIMARY_TEXT_COLOR);
        titlePaint.setTextSize(sp(TITLE_TEXT_SP));
        titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        messagePaint.setColor(SECONDARY_TEXT_COLOR);
        messagePaint.setTextSize(sp(MESSAGE_TEXT_SP));
        messagePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
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
        float padding = dp(CONTENT_PADDING_DP);
        float iconSize = dp(APP_ICON_DP);
        float textLeft = left + padding + iconSize + dp(ICON_TEXT_GAP_DP);
        float textRight = right - padding;
        int textWidth = Math.max(1, Math.round(textRight - textLeft));

        CharSequence appName = ellipsize(current.appName, headerPaint, textWidth);
        CharSequence title = ellipsize(current.title, titlePaint, textWidth);
        StaticLayout messageLayout = buildMessageLayout(current.message, textWidth);

        float headerHeight = headerPaint.getFontMetrics().bottom - headerPaint.getFontMetrics().top;
        float titleHeight = title.length() > 0
                ? titlePaint.getFontMetrics().bottom - titlePaint.getFontMetrics().top : 0f;
        float textHeight = headerHeight;
        if (title.length() > 0) {
            textHeight += dp(4f) + titleHeight;
        }
        if (messageLayout != null && messageLayout.getHeight() > 0) {
            textHeight += dp(4f) + messageLayout.getHeight();
        }
        float contentHeight = Math.max(iconSize, textHeight);
        float cardHeight = clamp(contentHeight + (padding * 2f),
                dp(CARD_MIN_HEIGHT_DP), dp(CARD_MAX_HEIGHT_DP));
        float bottom = Math.min(getHeight() - side, top + cardHeight);

        RectF card = new RectF(left, top, right, bottom);
        canvas.drawRoundRect(card, dp(CARD_RADIUS_DP), dp(CARD_RADIUS_DP), cardPaint);

        Drawable icon = current.appIcon;
        if (icon != null) {
            int iconLeft = Math.round(left + padding);
            int iconTop = Math.round(top + ((bottom - top - iconSize) / 2f));
            int size = Math.round(iconSize);
            Rect old = icon.copyBounds();
            icon.setBounds(iconLeft, iconTop, iconLeft + size, iconTop + size);
            icon.draw(canvas);
            icon.setBounds(old);
        }

        float y = top + padding - headerPaint.getFontMetrics().top;
        canvas.drawText(appName, 0, appName.length(), textLeft, y, headerPaint);
        if (title.length() > 0) {
            y += dp(4f) + titleHeight;
            float titleBaseline = y - titlePaint.getFontMetrics().bottom;
            canvas.drawText(title, 0, title.length(), textLeft, titleBaseline, titlePaint);
        }
        if (messageLayout != null && messageLayout.getHeight() > 0) {
            float messageTop = y + dp(6f);
            canvas.save();
            canvas.translate(textLeft, messageTop);
            messageLayout.draw(canvas);
            canvas.restore();
        }
    }

    private StaticLayout buildMessageLayout(CharSequence message, int width) {
        if (message == null || message.length() == 0) {
            return null;
        }
        return StaticLayout.Builder.obtain(message, 0, message.length(), messagePaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
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
