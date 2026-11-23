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
 * Pixel-style Material 3 incoming notification card shown during Ambient Display pulses.
 * Matches native Pixel lockscreen/ambient notification card geometry:
 * 28dp rounded pill container, 38dp circular icon badge on the left, bold headline + body.
 */
final class PixelPeekNotificationView extends View {
    private static final float CARD_SIDE_MARGIN_DP = 16f;
    private static final float CARD_RADIUS_DP = 28f;
    private static final float CARD_MIN_HEIGHT_DP = 76f;
    private static final float CARD_MAX_HEIGHT_DP = 112f;
    private static final float PADDING_H_DP = 16f;
    private static final float PADDING_V_DP = 14f;
    private static final float ICON_BADGE_DP = 38f;
    private static final float ICON_GLYPH_DP = 22f;
    private static final float ICON_TEXT_GAP_DP = 14f;
    private static final float TITLE_TEXT_SP = 15.5f;
    private static final float BODY_TEXT_SP = 14f;

    private static final int SURFACE_CONTAINER_COLOR = 0xFF1F2227;
    private static final int ICON_BADGE_COLOR = 0xFF333942;
    private static final int PRIMARY_TEXT_COLOR = Color.rgb(227, 227, 227);
    private static final int SECONDARY_TEXT_COLOR = Color.rgb(196, 199, 204);

    private final Paint surfacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
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
        surfacePaint.setColor(SURFACE_CONTAINER_COLOR);

        badgePaint.setStyle(Paint.Style.FILL);
        badgePaint.setColor(ICON_BADGE_COLOR);

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
        float badgeSize = dp(ICON_BADGE_DP);
        float textLeft = left + padH + badgeSize + dp(ICON_TEXT_GAP_DP);
        float textRight = right - padH;
        int textWidth = Math.max(1, Math.round(textRight - textLeft));

        CharSequence headline = current.title.length() > 0 ? current.title : current.appName;
        CharSequence ellipsizedTitle = ellipsize(headline, titlePaint, textWidth);
        CharSequence bodySource = current.message.length() > 0
                ? current.message
                : (current.title.length() > 0 ? current.appName : "");
        StaticLayout bodyLayout = buildBodyLayout(bodySource, textWidth);

        Paint.FontMetrics titleFm = titlePaint.getFontMetrics();
        float titleHeight = ellipsizedTitle.length() > 0 ? (titleFm.descent - titleFm.ascent) : 0f;
        float textBlockHeight = titleHeight;
        if (bodyLayout != null && bodyLayout.getHeight() > 0) {
            if (textBlockHeight > 0f) {
                textBlockHeight += dp(3f);
            }
            textBlockHeight += bodyLayout.getHeight();
        }
        float contentHeight = Math.max(badgeSize, textBlockHeight);
        float cardHeight = clamp(contentHeight + (padV * 2f),
                dp(CARD_MIN_HEIGHT_DP), dp(CARD_MAX_HEIGHT_DP));
        float bottom = Math.min(getHeight() - side, top + cardHeight);

        float radius = dp(CARD_RADIUS_DP);
        cardRect.set(left, top, right, bottom);
        canvas.drawRoundRect(cardRect, radius, radius, surfacePaint);

        float badgeCx = left + padH + (badgeSize / 2f);
        float badgeCy = top + ((bottom - top) / 2f);
        canvas.drawCircle(badgeCx, badgeCy, badgeSize / 2f, badgePaint);

        Drawable icon = current.appIcon;
        if (icon != null) {
            float glyphSize = dp(ICON_GLYPH_DP);
            int iconLeft = Math.round(badgeCx - (glyphSize / 2f));
            int iconTop = Math.round(badgeCy - (glyphSize / 2f));
            int size = Math.round(glyphSize);
            oldIconBounds.set(icon.getBounds());
            icon.setBounds(iconLeft, iconTop, iconLeft + size, iconTop + size);
            icon.draw(canvas);
            icon.setBounds(oldIconBounds);
        }

        float textStartY = top + Math.max(padV, ((bottom - top) - textBlockHeight) / 2f);
        float cursorY = textStartY;
        if (ellipsizedTitle.length() > 0) {
            float titleBaseline = cursorY - titleFm.ascent;
            canvas.drawText(ellipsizedTitle, 0, ellipsizedTitle.length(), textLeft, titleBaseline, titlePaint);
            cursorY += titleHeight + dp(3f);
        }
        if (bodyLayout != null && bodyLayout.getHeight() > 0) {
            float maxBottom = bottom - padV;
            if (cursorY < maxBottom) {
                canvas.save();
                canvas.clipRect(textLeft, cursorY, textRight, maxBottom);
                canvas.translate(textLeft, cursorY);
                bodyLayout.draw(canvas);
                canvas.restore();
            }
        }
    }

    private StaticLayout buildBodyLayout(CharSequence message, int width) {
        if (message == null || message.length() == 0) {
            return null;
        }
        return StaticLayout.Builder.obtain(message, 0, message.length(), bodyPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.16f)
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
