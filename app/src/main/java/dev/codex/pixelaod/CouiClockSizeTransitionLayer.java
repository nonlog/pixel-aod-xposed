package dev.codex.pixelaod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Temporary per-glyph surface used while the persistent host changes between large and compact.
 *
 * <p>COUI keeps separate digit views alive and animates every glyph toward the next scene. The
 * module keeps its existing lockscreen/AOD renderers, but this layer gives their handoff the same
 * visual contract: four independently positioned digits, a staged colon, and information rows
 * moving in one 550 ms transaction.</p>
 */
final class CouiClockSizeTransitionLayer extends FrameLayout {
    private static final int DIGIT_COUNT = 4;
    private static final float LARGE_COLON_SCALE = 0.44f;
    private static final int WEIGHT_UPDATE_STEP = 4;

    interface WeightProvider {
        int clockWeight();

        int infoWeight();
    }

    private final TextView[] digitViews = new TextView[DIGIT_COUNT];
    private final IdentityHashMap<View, Float> hiddenContentAlphas = new IdentityHashMap<>();
    private TextView colonView;
    private TextView dateView;
    private TextView weatherView;
    private SceneSnapshot sourceSnapshot;
    private ValueAnimator animator;
    private int lastAppliedClockWeight = Integer.MIN_VALUE;
    private int lastAppliedInfoWeight = Integer.MIN_VALUE;
    private String transitionSource = "";

    CouiClockSizeTransitionLayer(Context context) {
        super(context);
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(View.INVISIBLE);
    }

    SceneSnapshot capture(ViewGroup coordinateRoot, TextView clock, TextView date,
            TextView weather, int clockWeight, int infoWeight) {
        if (coordinateRoot == null || clock == null || clock.getVisibility() != View.VISIBLE) {
            return SceneSnapshot.EMPTY;
        }
        int[] rootLocation = new int[2];
        try {
            coordinateRoot.getLocationOnScreen(rootLocation);
        } catch (Throwable ignored) {
            return SceneSnapshot.EMPTY;
        }
        GlyphCollection glyphs = captureClockGlyphs(clock, rootLocation);
        if (!glyphs.valid()) {
            return SceneSnapshot.EMPTY;
        }
        InfoSnapshot dateSnapshot = captureInformation(date, rootLocation);
        InfoSnapshot weatherSnapshot = captureInformation(weather, rootLocation);
        return new SceneSnapshot(glyphs.digits, glyphs.colon, dateSnapshot, weatherSnapshot,
                clock, date, weather, clockWeight, infoWeight);
    }

    boolean prepare(SceneSnapshot source, String sourceTag) {
        cancelAndRestore("prepare-next");
        if (source == null || !source.valid()) {
            return false;
        }
        sourceSnapshot = source;
        transitionSource = sourceTag != null ? sourceTag : "";
        createOverlayViews(source);
        rememberAndHide(source.clockContent);
        rememberAndHide(source.dateContent);
        rememberAndHide(source.weatherContent);
        setVisibility(View.VISIBLE);
        bringToFront();
        PixelAodLog.log("prepared COUI per-glyph size transaction source=" + transitionSource
                + " fromCompact=" + source.compact
                + " clockWeight=" + source.clockWeight
                + " infoWeight=" + source.infoWeight
                + " trace=" + PixelAodClockView.currentAodTraceId());
        return true;
    }

    boolean start(SceneSnapshot target, WeightProvider weightProvider, long durationMs,
            Interpolator motionInterpolator, String sourceTag) {
        if (sourceSnapshot == null || target == null || !target.valid()
                || motionInterpolator == null) {
            cancelAndRestore("invalid-target");
            return false;
        }
        final SceneSnapshot from = sourceSnapshot;
        final SceneSnapshot to = target;
        if (!CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                true, from.compact, to.compact)) {
            PixelAodLog.log("skipped COUI per-glyph size transaction actual-size no-op source="
                    + (sourceTag != null ? sourceTag : transitionSource)
                    + " fromCompact=" + from.compact
                    + " toCompact=" + to.compact
                    + " trace=" + PixelAodClockView.currentAodTraceId());
            cancelAndRestore("actual-size-no-op");
            return false;
        }
        transitionSource = sourceTag != null ? sourceTag : transitionSource;
        rememberAndHide(to.clockContent);
        rememberAndHide(to.dateContent);
        rememberAndHide(to.weatherContent);
        if (dateView == null && to.date.valid) {
            dateView = createInformationClone(to.date.withAlpha(0f));
            addView(dateView);
        }
        if (weatherView == null && to.weather.valid) {
            weatherView = createInformationClone(to.weather.withAlpha(0f));
            addView(weatherView);
        }
        configureOverlayGeometry(from, to);
        applyWeights(from.clockWeight, from.infoWeight);
        applyFrame(from, to, 0f, 0f, motionInterpolator);

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(Math.max(1L, durationMs));
        // ValueAnimator otherwise applies AccelerateDecelerateInterpolator before the COUI path.
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(valueAnimator -> {
            float linear = (Float) valueAnimator.getAnimatedValue();
            float motion = motionInterpolator.getInterpolation(linear);
            applyFrame(from, to, motion, linear, motionInterpolator);
            int clockWeight = weightProvider != null
                    ? weightProvider.clockWeight()
                    : CouiClockSizeTransitionMath.interpolatedWeight(
                    from.clockWeight, to.clockWeight, motion);
            int infoWeight = weightProvider != null
                    ? weightProvider.infoWeight()
                    : CouiClockSizeTransitionMath.interpolatedWeight(
                    from.infoWeight, to.infoWeight, motion);
            applyWeights(clockWeight, infoWeight);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;

            @Override
            public void onAnimationCancel(Animator animation) {
                cancelled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (!cancelled) {
                    applyFrame(from, to, 1f, 1f, motionInterpolator);
                }
                finishAndRestore(cancelled ? "cancelled" : "finished");
            }
        });
        animator.start();
        PixelAodLog.log("started COUI per-glyph size transaction source=" + transitionSource
                + " fromCompact=" + from.compact
                + " toCompact=" + to.compact
                + " durationMs=" + durationMs
                + " trace=" + PixelAodClockView.currentAodTraceId());
        return true;
    }

    boolean isRunning() {
        return animator != null && animator.isRunning();
    }

    boolean hasActiveTransition() {
        return sourceSnapshot != null || animator != null || !hiddenContentAlphas.isEmpty();
    }

    void cancelAndRestore(String reason) {
        if (animator != null) {
            ValueAnimator running = animator;
            animator = null;
            running.cancel();
        }
        if (sourceSnapshot != null || !hiddenContentAlphas.isEmpty() || getChildCount() > 0) {
            finishAndRestore(reason != null ? reason : "cancel");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelAndRestore("detached");
        super.onDetachedFromWindow();
    }

    private void createOverlayViews(SceneSnapshot source) {
        removeAllViews();
        for (int index = 0; index < DIGIT_COUNT; index++) {
            TextView digit = createClockGlyph(source.digits[index]);
            digitViews[index] = digit;
            addView(digit);
        }
        colonView = createClockGlyph(source.colon);
        addView(colonView);
        dateView = createInformationClone(source.date);
        if (dateView != null) {
            addView(dateView);
        }
        weatherView = createInformationClone(source.weather);
        if (weatherView != null) {
            addView(weatherView);
        }
    }

    private TextView createClockGlyph(GlyphSnapshot glyph) {
        TextView view = new TextView(getContext());
        view.setText(String.valueOf(glyph.value));
        view.setTextColor(glyph.color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, glyph.element.textSizePx);
        view.setTypeface(glyph.typeface != null ? glyph.typeface : Typeface.DEFAULT);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setPadding(0, 0, 0, 0);
        view.setFontFeatureSettings("tnum");
        view.getPaint().setSubpixelText(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private TextView createInformationClone(InfoSnapshot info) {
        if (info == null || !info.valid) {
            return null;
        }
        TextView view = new TextView(getContext());
        CharSequence text = info.text;
        if (text instanceof Spanned) {
            text = new SpannableString(text);
        }
        view.setText(text);
        view.setTextColor(info.color);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, info.element.textSizePx);
        view.setTypeface(info.typeface != null ? info.typeface : Typeface.DEFAULT);
        view.setIncludeFontPadding(false);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        view.setPadding(0, 0, 0, 0);
        view.setLetterSpacing(info.letterSpacing);
        view.setCompoundDrawablePadding(info.compoundDrawablePadding);
        view.setCompoundDrawablesRelative(copyDrawable(info.drawables[0]),
                copyDrawable(info.drawables[1]), copyDrawable(info.drawables[2]),
                copyDrawable(info.drawables[3]));
        view.setShadowLayer(info.shadowRadius, info.shadowDx, info.shadowDy, info.shadowColor);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private Drawable copyDrawable(Drawable source) {
        if (source == null) {
            return null;
        }
        Drawable copy = source;
        try {
            Drawable.ConstantState state = source.getConstantState();
            if (state != null) {
                copy = state.newDrawable(getResources()).mutate();
            }
            copy.setBounds(source.getBounds());
        } catch (Throwable ignored) {
            // The source drawable is already immutable enough for a short-lived visual clone.
        }
        return copy;
    }

    private void configureOverlayGeometry(SceneSnapshot from, SceneSnapshot to) {
        for (int index = 0; index < DIGIT_COUNT; index++) {
            configureBox(digitViews[index], from.digits[index], to.digits[index]);
        }
        configureBox(colonView, from.colon, to.colon);
        configureInfoBox(dateView, from.date, to.date);
        configureInfoBox(weatherView, from.weather, to.weather);
    }

    private void configureBox(TextView view, GlyphSnapshot from, GlyphSnapshot to) {
        float maxText = Math.max(from.element.textSizePx, to.element.textSizePx);
        int width = Math.max(1, Math.round(Math.max(Math.max(from.width, to.width), maxText) * 1.45f));
        int height = Math.max(1, Math.round(Math.max(Math.max(from.height, to.height), maxText) * 1.55f));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        view.setLayoutParams(params);
        view.setPivotX(width / 2f);
        view.setPivotY(height / 2f);
    }

    private void configureInfoBox(TextView view, InfoSnapshot from, InfoSnapshot to) {
        if (view == null) {
            return;
        }
        InfoSnapshot usableFrom = effectiveInfoSource(from, to);
        InfoSnapshot usableTarget = effectiveInfoTarget(from, to);
        if (!usableFrom.valid || !usableTarget.valid) {
            return;
        }
        int width = Math.max(1, Math.round(
                Math.max(usableFrom.width, usableTarget.width) * 1.15f));
        int height = Math.max(1, Math.round(
                Math.max(usableFrom.height, usableTarget.height) * 1.25f));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        view.setLayoutParams(params);
        view.setPivotX(width / 2f);
        view.setPivotY(height / 2f);
    }

    private void applyFrame(SceneSnapshot from, SceneSnapshot to, float motionProgress,
            float linearProgress, Interpolator motionInterpolator) {
        for (int index = 0; index < DIGIT_COUNT; index++) {
            applyGlyphFrame(digitViews[index], from.digits[index], to.digits[index],
                    motionProgress, false, linearProgress, motionInterpolator);
        }
        applyGlyphFrame(colonView, from.colon, to.colon, motionProgress, true,
                linearProgress, motionInterpolator);
        applyInfoFrame(dateView, from.date, to.date, motionProgress);
        applyInfoFrame(weatherView, from.weather, to.weather, motionProgress);
    }

    private void applyGlyphFrame(TextView view, GlyphSnapshot from, GlyphSnapshot to,
            float motionProgress, boolean colon, float linearProgress,
            Interpolator motionInterpolator) {
        CouiClockSizeTransitionMath.Frame frame = CouiClockSizeTransitionMath.frame(
                from.element, to.element, motionProgress);
        placeAtCenter(view, frame.centerX, frame.centerY);
        view.setScaleX(frame.scaleFromSource);
        view.setScaleY(frame.scaleFromSource);
        float alpha = colon
                ? CouiClockSizeTransitionMath.colonAlpha(from.element.alpha, to.element.alpha,
                linearProgress, motionInterpolator::getInterpolation)
                : frame.alpha;
        view.setAlpha(alpha);
    }

    private void applyInfoFrame(TextView view, InfoSnapshot from, InfoSnapshot to,
            float motionProgress) {
        if (view == null) {
            return;
        }
        InfoSnapshot source = effectiveInfoSource(from, to);
        InfoSnapshot target = effectiveInfoTarget(from, to);
        if (!source.valid || !target.valid) {
            return;
        }
        CouiClockSizeTransitionMath.Frame frame = CouiClockSizeTransitionMath.frame(
                source.element, target.element, motionProgress);
        placeAtCenter(view, frame.centerX, frame.centerY);
        view.setScaleX(frame.scaleFromSource);
        view.setScaleY(frame.scaleFromSource);
        view.setAlpha(frame.alpha);
    }

    private static InfoSnapshot effectiveInfoSource(InfoSnapshot from, InfoSnapshot to) {
        if (from != null && from.valid) {
            return from;
        }
        return to != null && to.valid ? to.withAlpha(0f) : InfoSnapshot.INVALID;
    }

    private static InfoSnapshot effectiveInfoTarget(InfoSnapshot from, InfoSnapshot to) {
        if (to != null && to.valid) {
            return to;
        }
        return from != null && from.valid ? from.withAlpha(0f) : InfoSnapshot.INVALID;
    }

    private static void placeAtCenter(View view, float centerX, float centerY) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        float width = params != null && params.width > 0 ? params.width : view.getMeasuredWidth();
        float height = params != null && params.height > 0 ? params.height : view.getMeasuredHeight();
        view.setX(centerX - (width / 2f));
        view.setY(centerY - (height / 2f));
    }

    private void applyWeights(int clockWeight, int infoWeight) {
        int quantizedClock = quantizeWeight(clockWeight);
        int quantizedInfo = quantizeWeight(infoWeight);
        if (lastAppliedClockWeight != quantizedClock) {
            lastAppliedClockWeight = quantizedClock;
            for (TextView digit : digitViews) {
                PixelAodClockView.applySharedClockTypeface(digit, getContext(), quantizedClock);
            }
            PixelAodClockView.applySharedClockTypeface(colonView, getContext(), quantizedClock);
        }
        if (lastAppliedInfoWeight != quantizedInfo) {
            lastAppliedInfoWeight = quantizedInfo;
            PixelAodClockView.applySharedClockTypeface(dateView, getContext(), quantizedInfo);
            PixelAodClockView.applySharedClockTypeface(weatherView, getContext(), quantizedInfo);
        }
    }

    private static int quantizeWeight(int weight) {
        return Math.max(100, Math.min(500,
                Math.round(weight / (float) WEIGHT_UPDATE_STEP) * WEIGHT_UPDATE_STEP));
    }

    private void rememberAndHide(View view) {
        if (view == null) {
            return;
        }
        if (!hiddenContentAlphas.containsKey(view)) {
            hiddenContentAlphas.put(view, view.getAlpha());
        }
        view.setAlpha(0f);
    }

    private float desiredAlpha(View view) {
        if (view == null) {
            return 0f;
        }
        Float remembered = hiddenContentAlphas.get(view);
        return remembered != null ? remembered : view.getAlpha();
    }

    private void finishAndRestore(String reason) {
        if (animator != null) {
            animator = null;
        }
        for (Map.Entry<View, Float> entry : hiddenContentAlphas.entrySet()) {
            View content = entry.getKey();
            if (content != null) {
                content.setAlpha(entry.getValue());
            }
        }
        hiddenContentAlphas.clear();
        removeAllViews();
        for (int index = 0; index < digitViews.length; index++) {
            digitViews[index] = null;
        }
        colonView = null;
        dateView = null;
        weatherView = null;
        sourceSnapshot = null;
        lastAppliedClockWeight = Integer.MIN_VALUE;
        lastAppliedInfoWeight = Integer.MIN_VALUE;
        setVisibility(View.INVISIBLE);
        PixelAodLog.log("ended COUI per-glyph size transaction source=" + transitionSource
                + " reason=" + reason
                + " trace=" + PixelAodClockView.currentAodTraceId());
        transitionSource = "";
    }

    private GlyphCollection captureClockGlyphs(TextView clock, int[] rootLocation) {
        Layout layout = clock.getLayout();
        CharSequence text = clock.getText();
        if (layout == null || text == null || text.length() == 0) {
            return GlyphCollection.EMPTY;
        }
        int[] clockLocation = new int[2];
        try {
            clock.getLocationOnScreen(clockLocation);
        } catch (Throwable ignored) {
            return GlyphCollection.EMPTY;
        }
        GlyphSnapshot[] digits = new GlyphSnapshot[DIGIT_COUNT];
        GlyphSnapshot colon = null;
        int digitIndex = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            char value = text.charAt(offset);
            if (!Character.isDigit(value) && value != ':') {
                continue;
            }
            GlyphSnapshot glyph = captureGlyph(clock, layout, text, offset, value,
                    rootLocation, clockLocation);
            if (value == ':') {
                colon = glyph;
            } else if (digitIndex < DIGIT_COUNT) {
                digits[digitIndex++] = glyph;
            }
        }
        if (digitIndex != DIGIT_COUNT) {
            return GlyphCollection.EMPTY;
        }
        boolean compact = colon != null;
        if (colon == null) {
            float centerX = 0f;
            float centerY = 0f;
            for (GlyphSnapshot digit : digits) {
                centerX += digit.element.centerX;
                centerY += digit.element.centerY;
            }
            centerX /= DIGIT_COUNT;
            centerY /= DIGIT_COUNT;
            float hiddenColonSize = clock.getTextSize() * LARGE_COLON_SCALE;
            CouiClockSizeTransitionMath.Element element =
                    new CouiClockSizeTransitionMath.Element(centerX, centerY,
                            hiddenColonSize, 0f);
            colon = new GlyphSnapshot(':', element, hiddenColonSize * 0.55f,
                    hiddenColonSize * 1.1f, clock.getCurrentTextColor(), clock.getTypeface());
        }
        return new GlyphCollection(digits, colon, compact);
    }

    private GlyphSnapshot captureGlyph(TextView clock, Layout layout, CharSequence text,
            int offset, char value, int[] rootLocation, int[] clockLocation) {
        int line = layout.getLineForOffset(offset);
        float leading = layout.getPrimaryHorizontal(offset);
        float trailing = layout.getPrimaryHorizontal(Math.min(text.length(), offset + 1));
        float left = Math.min(leading, trailing);
        float right = Math.max(leading, trailing);
        if (right - left < 1f) {
            right = left + Math.max(1f, clock.getPaint().measureText(text, offset, offset + 1));
        }
        float top = layout.getLineTop(line);
        float bottom = layout.getLineBottom(line);
        float referenceAdvance = PixelAodClockView.fixedClockGlyphReferenceAdvancePx(
                clock, value);
        float centerX = (clockLocation[0] - rootLocation[0])
                + clock.getTotalPaddingLeft()
                + CouiClockSizeTransitionMath.glyphCenter(left, referenceAdvance);
        float centerY = (clockLocation[1] - rootLocation[1])
                + clock.getTotalPaddingTop() + ((top + bottom) / 2f);
        CouiClockSizeTransitionMath.Element element =
                new CouiClockSizeTransitionMath.Element(centerX, centerY,
                        clock.getTextSize(), desiredAlpha(clock));
        float paintedWidth = Math.max(1f,
                clock.getPaint().measureText(text, offset, offset + 1));
        return new GlyphSnapshot(value, element, paintedWidth, bottom - top,
                clock.getCurrentTextColor(), clock.getTypeface());
    }

    private InfoSnapshot captureInformation(TextView view, int[] rootLocation) {
        if (view == null || view.getVisibility() != View.VISIBLE || view.getText() == null
                || view.getText().length() == 0) {
            return InfoSnapshot.INVALID;
        }
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
        } catch (Throwable ignored) {
            return InfoSnapshot.INVALID;
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return InfoSnapshot.INVALID;
        }
        // Date and weather are WRAP_CONTENT rows. Use the whole row so the weather drawable and
        // its text travel as one COUI-like information group instead of orbiting around the text.
        float contentWidth = Math.max(1f, view.getWidth());
        float contentHeight = Math.max(1f, view.getHeight());
        float centerX = (location[0] - rootLocation[0]) + contentWidth / 2f;
        float centerY = (location[1] - rootLocation[1]) + contentHeight / 2f;
        CouiClockSizeTransitionMath.Element element =
                new CouiClockSizeTransitionMath.Element(centerX, centerY,
                        view.getTextSize(), desiredAlpha(view));
        return new InfoSnapshot(true, view.getText(), element, contentWidth, contentHeight,
                view.getCurrentTextColor(), view.getTypeface(), view.getLetterSpacing(),
                view.getCompoundDrawablePadding(), view.getCompoundDrawablesRelative(),
                view.getShadowRadius(), view.getShadowDx(), view.getShadowDy(),
                view.getShadowColor());
    }

    static final class SceneSnapshot {
        static final SceneSnapshot EMPTY = new SceneSnapshot(null, null,
                InfoSnapshot.INVALID, InfoSnapshot.INVALID, null, null, null, 0, 0);

        final GlyphSnapshot[] digits;
        final GlyphSnapshot colon;
        final InfoSnapshot date;
        final InfoSnapshot weather;
        final TextView clockContent;
        final TextView dateContent;
        final TextView weatherContent;
        final int clockWeight;
        final int infoWeight;
        final boolean compact;

        SceneSnapshot(GlyphSnapshot[] digits, GlyphSnapshot colon, InfoSnapshot date,
                InfoSnapshot weather, TextView clockContent, TextView dateContent,
                TextView weatherContent, int clockWeight, int infoWeight) {
            this.digits = digits;
            this.colon = colon;
            this.date = date != null ? date : InfoSnapshot.INVALID;
            this.weather = weather != null ? weather : InfoSnapshot.INVALID;
            this.clockContent = clockContent;
            this.dateContent = dateContent;
            this.weatherContent = weatherContent;
            this.clockWeight = clockWeight;
            this.infoWeight = infoWeight;
            this.compact = colon != null && colon.element.alpha > 0.5f;
        }

        boolean valid() {
            if (digits == null || digits.length != DIGIT_COUNT || colon == null) {
                return false;
            }
            for (GlyphSnapshot digit : digits) {
                if (digit == null) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class GlyphCollection {
        static final GlyphCollection EMPTY = new GlyphCollection(null, null, false);

        final GlyphSnapshot[] digits;
        final GlyphSnapshot colon;
        final boolean compact;

        GlyphCollection(GlyphSnapshot[] digits, GlyphSnapshot colon, boolean compact) {
            this.digits = digits;
            this.colon = colon;
            this.compact = compact;
        }

        boolean valid() {
            return digits != null && digits.length == DIGIT_COUNT && colon != null;
        }
    }

    private static final class GlyphSnapshot {
        final char value;
        final CouiClockSizeTransitionMath.Element element;
        final float width;
        final float height;
        final int color;
        final Typeface typeface;

        GlyphSnapshot(char value, CouiClockSizeTransitionMath.Element element,
                float width, float height, int color, Typeface typeface) {
            this.value = value;
            this.element = element;
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);
            this.color = color;
            this.typeface = typeface;
        }
    }

    private static final class InfoSnapshot {
        static final InfoSnapshot INVALID = new InfoSnapshot(false, "",
                new CouiClockSizeTransitionMath.Element(0f, 0f, 1f, 0f),
                1f, 1f, 0, Typeface.DEFAULT, 0f, 0, new Drawable[4],
                0f, 0f, 0f, 0);

        final boolean valid;
        final CharSequence text;
        final CouiClockSizeTransitionMath.Element element;
        final float width;
        final float height;
        final int color;
        final Typeface typeface;
        final float letterSpacing;
        final int compoundDrawablePadding;
        final Drawable[] drawables;
        final float shadowRadius;
        final float shadowDx;
        final float shadowDy;
        final int shadowColor;

        InfoSnapshot(boolean valid, CharSequence text,
                CouiClockSizeTransitionMath.Element element, float width, float height,
                int color, Typeface typeface, float letterSpacing, int compoundDrawablePadding,
                Drawable[] drawables, float shadowRadius, float shadowDx, float shadowDy,
                int shadowColor) {
            this.valid = valid;
            this.text = text != null ? text : "";
            this.element = element;
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);
            this.color = color;
            this.typeface = typeface;
            this.letterSpacing = letterSpacing;
            this.compoundDrawablePadding = compoundDrawablePadding;
            this.drawables = drawables != null && drawables.length == 4
                    ? drawables.clone() : new Drawable[4];
            this.shadowRadius = shadowRadius;
            this.shadowDx = shadowDx;
            this.shadowDy = shadowDy;
            this.shadowColor = shadowColor;
        }

        InfoSnapshot withAlpha(float alpha) {
            return new InfoSnapshot(valid, text,
                    new CouiClockSizeTransitionMath.Element(element.centerX, element.centerY,
                            element.textSizePx, alpha),
                    width, height, color, typeface, letterSpacing, compoundDrawablePadding,
                    drawables, shadowRadius, shadowDx, shadowDy, shadowColor);
        }
    }
}
