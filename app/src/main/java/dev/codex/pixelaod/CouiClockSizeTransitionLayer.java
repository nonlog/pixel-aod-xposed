package dev.codex.pixelaod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
    private TextView contextualView;
    private ImageView dateIconView;
    private ImageView weatherIconView;
    private ImageView contextualIconView;
    private SceneSnapshot sourceSnapshot;
    private ValueAnimator animator;
    private int lastAppliedClockWeight = Integer.MIN_VALUE;
    private int lastAppliedInfoWeight = Integer.MIN_VALUE;
    private String transitionSource = "";
    private long targetPreDrawGeneration;
    private boolean waitingForTargetPreDraw;

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
        return capture(coordinateRoot, clock, date, weather, null, null, null, clockWeight,
                infoWeight);
    }

    SceneSnapshot capture(ViewGroup coordinateRoot, TextView clock, TextView date,
            TextView weather, View contextualContent, ImageView contextualIcon,
            TextView contextualText, int clockWeight, int infoWeight) {
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
        IconSnapshot dateIconSnapshot = captureCompoundIcon(date, rootLocation);
        IconSnapshot weatherIconSnapshot = captureCompoundIcon(weather, rootLocation);
        InfoSnapshot contextualSnapshot = contextualContent != null
                && contextualContent.getVisibility() == View.VISIBLE
                ? captureInformation(contextualText, contextualContent, rootLocation)
                : InfoSnapshot.INVALID;
        IconSnapshot contextualIconSnapshot = captureImageIcon(contextualContent, contextualIcon,
                rootLocation);
        return new SceneSnapshot(glyphs.digits, glyphs.colon, dateSnapshot, weatherSnapshot,
                contextualSnapshot, dateIconSnapshot, weatherIconSnapshot, contextualIconSnapshot,
                clock, date, weather, contextualContent, contextualText, clockWeight, infoWeight);
    }

    boolean prepare(SceneSnapshot source, String sourceTag) {
        cancelAndRestore("prepare-next");
        if (source == null || !source.valid()) {
            return false;
        }
        sourceSnapshot = source;
        transitionSource = sourceTag != null ? sourceTag : "";
        createOverlayViews(source);
        // addView() gives FrameLayout children MATCH_PARENT defaults. Configure and synchronously
        // measure the source frame before exposing the overlay; otherwise the FIT_XY weather icon
        // can fill the whole transition layer for the one traversal between prepare() and start().
        configureOverlayGeometry(source, source);
        applyWeights(source.clockWeight, source.infoWeight, true);
        applyFrame(source, source, 0f, 0f, new LinearInterpolator());
        rememberAndHide(source.clockContent);
        rememberAndHide(source.dateContent);
        rememberAndHide(source.weatherContent);
        rememberAndHide(source.contextualContent);
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
        rememberAndHide(to.contextualContent);
        if (dateView == null && to.date.valid) {
            dateView = createInformationClone(to.date.withAlpha(0f));
            addView(dateView);
        }
        if (dateIconView == null && to.dateIcon.valid) {
            dateIconView = createInformationIconClone(to.dateIcon.withAlpha(0f));
            addView(dateIconView);
        }
        if (weatherView == null && to.weather.valid) {
            weatherView = createInformationClone(to.weather.withAlpha(0f));
            addView(weatherView);
        }
        if (weatherIconView == null && to.weatherIcon.valid) {
            weatherIconView = createInformationIconClone(to.weatherIcon.withAlpha(0f));
            addView(weatherIconView);
        }
        retargetInformationCloneIfTextMatches(dateView, from.date, to.date);
        // Keep the current-weather source cells for the full transaction.  The 16 dp compact
        // and large rows can have distinct FixedAdvanceSpan rounding even with identical text;
        // replacing "31°" with target cells before frame zero makes its glyph ink drift inside
        // an otherwise correctly positioned text track.  Its live target owns the post-motion
        // cells once the overlay is restored.
        if (shouldRetargetInformationCloneText(from.weather.text, to.weather.text, true)) {
            retargetInformationCloneIfTextMatches(weatherView, from.weather, to.weather);
        }
        retargetInformationCloneIfTextMatches(contextualView, from.contextual, to.contextual);
        // Do not translate the live row.  Its parent may perform a second target-layout pass
        // while this transaction is still visible, which creates a distinct vertical movement
        // after the cloned clock/date/weather have already reached their endpoint.  The clone
        // keeps the icon and text in the same isolated coordinate system for the whole motion.
        if (contextualView == null && to.contextual.valid) {
            contextualView = createInformationClone(to.contextual.withAlpha(0f));
            addView(contextualView);
        }
        if (contextualIconView == null && to.contextualIcon.valid) {
            contextualIconView = createInformationIconClone(to.contextualIcon.withAlpha(0f));
            addView(contextualIconView);
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
            int clockWeight = weightProvider != null
                    ? weightProvider.clockWeight()
                    : CouiClockSizeTransitionMath.interpolatedWeight(
                    from.clockWeight, to.clockWeight, motion);
            int infoWeight = weightProvider != null
                    ? weightProvider.infoWeight()
                    : CouiClockSizeTransitionMath.interpolatedWeight(
                    from.infoWeight, to.infoWeight, motion);
            applyWeights(clockWeight, infoWeight);
            // A typeface weight changes the ink bounds of narrow digits such as "1". Apply it
            // before calculating the frame so each clone is placed on its actual painted centre.
            applyFrame(from, to, motion, linear, motionInterpolator);
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
                    int finalClockWeight = weightProvider != null
                            ? weightProvider.clockWeight() : to.clockWeight;
                    int finalInfoWeight = weightProvider != null
                            ? weightProvider.infoWeight() : to.infoWeight;
                    applyWeights(finalClockWeight, finalInfoWeight, true);
                    applyFrame(from, to, 1f, 1f, motionInterpolator);
                    applyTargetWeightAndWaitForPreDraw(to, finalClockWeight, finalInfoWeight,
                            "finished");
                    return;
                }
                finishAndRestore("cancelled");
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
        return sourceSnapshot != null || animator != null || waitingForTargetPreDraw
                || !hiddenContentAlphas.isEmpty();
    }

    void cancelAndRestore(String reason) {
        targetPreDrawGeneration++;
        waitingForTargetPreDraw = false;
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
        dateIconView = createInformationIconClone(source.dateIcon);
        if (dateIconView != null) {
            addView(dateIconView);
        }
        weatherView = createInformationClone(source.weather);
        if (weatherView != null) {
            addView(weatherView);
        }
        weatherIconView = createInformationIconClone(source.weatherIcon);
        if (weatherIconView != null) {
            addView(weatherIconView);
        }
        contextualView = createInformationClone(source.contextual);
        if (contextualView != null) {
            addView(contextualView);
        }
        contextualIconView = createInformationIconClone(source.contextualIcon);
        if (contextualIconView != null) {
            addView(contextualIconView);
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
        // The transition text box deliberately has spare width.  It must stay centred on the
        // captured text corridor; a host START gravity belongs to the live layout, not this
        // synthetic box.  Compound icons are rendered by their own ImageView track below.
        view.setGravity(Gravity.CENTER);
        view.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        view.setSingleLine(true);
        view.setPadding(0, 0, 0, 0);
        view.setLetterSpacing(info.letterSpacing);
        view.setCompoundDrawablePadding(0);
        view.setCompoundDrawablesRelative(null, null, null, null);
        view.setShadowLayer(info.shadowRadius, info.shadowDx, info.shadowDy, info.shadowColor);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    private ImageView createInformationIconClone(IconSnapshot icon) {
        if (icon == null || !icon.valid || icon.drawable == null) {
            return null;
        }
        ImageView view = new ImageView(getContext());
        view.setImageDrawable(copyDrawable(icon.drawable));
        view.setScaleType(ImageView.ScaleType.FIT_XY);
        view.setPadding(0, 0, 0, 0);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    /**
     * The source and target surfaces recreate FixedAdvanceSpan cells at their own text size.
     * Reusing source spans in a clone that lands at the target size can leave a one-pixel
     * rounding difference between the final icon/text union and the real target view.  When the
     * visible characters did not change, use the target's spans and drawable now; the animation
     * still scales them to the source text size for its first frame, but its final frame has the
     * exact target geometry.
     */
    private void retargetInformationCloneIfTextMatches(TextView clone, InfoSnapshot source,
            InfoSnapshot target) {
        if (clone == null || source == null || target == null || !source.valid || !target.valid
                || !shouldRetargetInformationCloneText(source.text, target.text)) {
            return;
        }
        CharSequence text = target.text;
        if (text instanceof Spanned) {
            text = new SpannableString(text);
        }
        clone.setText(text);
        clone.setTextColor(target.color);
        clone.setGravity(Gravity.CENTER);
        clone.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        clone.setCompoundDrawablePadding(0);
        clone.setCompoundDrawablesRelative(null, null, null, null);
    }

    static boolean shouldRetargetInformationCloneText(CharSequence source, CharSequence target) {
        return shouldRetargetInformationCloneText(source, target, false);
    }

    /**
     * The weather track preserves its source FixedAdvanceSpan cells so individual temperature
     * glyphs cannot shift before the target TextView takes ownership at restoration.
     */
    static boolean shouldRetargetInformationCloneText(CharSequence source, CharSequence target,
            boolean preserveSourceCells) {
        return !preserveSourceCells && source != null && target != null
                && source.toString().contentEquals(target);
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
        configureInfoBox(contextualView, from.contextual, to.contextual);
        configureIconBox(dateIconView, from.dateIcon, to.dateIcon);
        configureIconBox(weatherIconView, from.weatherIcon, to.weatherIcon);
        configureIconBox(contextualIconView, from.contextualIcon, to.contextualIcon);
    }

    private void configureBox(TextView view, GlyphSnapshot from, GlyphSnapshot to) {
        float maxText = Math.max(from.element.textSizePx, to.element.textSizePx);
        int width = Math.max(1, Math.round(Math.max(Math.max(from.width, to.width), maxText) * 1.45f));
        int height = Math.max(1, Math.round(Math.max(Math.max(from.height, to.height), maxText) * 1.55f));
        measureAndLayoutOverlayChild(view, width, height);
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
        measureAndLayoutOverlayChild(view, width, height);
        view.setPivotX(width / 2f);
        view.setPivotY(height / 2f);
    }

    private void configureIconBox(ImageView view, IconSnapshot from, IconSnapshot to) {
        if (view == null) {
            return;
        }
        IconSnapshot usableFrom = effectiveIconSource(from, to);
        IconSnapshot usableTarget = effectiveIconTarget(from, to);
        if (!usableFrom.valid || !usableTarget.valid) {
            return;
        }
        int width = Math.max(1, Math.round(Math.max(usableFrom.width, usableTarget.width)));
        int height = Math.max(1, Math.round(Math.max(usableFrom.height, usableTarget.height)));
        measureAndLayoutOverlayChild(view, width, height);
        view.setPivotX(width / 2f);
        view.setPivotY(height / 2f);
    }

    private static void measureAndLayoutOverlayChild(View view, int width, int height) {
        if (view == null) {
            return;
        }
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        view.setLayoutParams(new FrameLayout.LayoutParams(safeWidth, safeHeight));
        view.measure(
                View.MeasureSpec.makeMeasureSpec(safeWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(safeHeight, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, safeWidth, safeHeight);
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
        applyFixedCellInfoFrame(weatherView, from.weather, to.weather, motionProgress);
        applyInfoFrame(contextualView, from.contextual, to.contextual, motionProgress);
        applyIconFrame(dateIconView, from.dateIcon, to.dateIcon, motionProgress);
        applyIconFrame(weatherIconView, from.weatherIcon, to.weatherIcon, motionProgress);
        applyIconFrame(contextualIconView, from.contextualIcon, to.contextualIcon, motionProgress);
    }

    private void applyGlyphFrame(TextView view, GlyphSnapshot from, GlyphSnapshot to,
            float motionProgress, boolean colon, float linearProgress,
            Interpolator motionInterpolator) {
        CouiClockSizeTransitionMath.Frame frame = CouiClockSizeTransitionMath.frame(
                from.element, to.element, motionProgress);
        placeGlyphAtVisualCenter(view, frame.centerX, frame.centerY);
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
        CouiClockSizeTransitionMath.InfoFrame frame =
                CouiClockSizeTransitionMath.informationFrame(source.element, target.element,
                        motionProgress);
        // Keep icon geometry in drawable pixels. A weather icon is intentionally fixed-size
        // across compact/large states, so affine TextView scaling made it visibly shrink during
        // the transaction and then rebound when the real target row appeared.
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, frame.textSizePx);
        view.setLetterSpacing(CouiClockSizeTransitionMath.interpolatedFloat(
                source.letterSpacing, target.letterSpacing, motionProgress));
        placeInfoAtVisualCenter(view, frame.centerX, frame.centerY);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setAlpha(frame.alpha);
    }

    /**
     * The current-weather track contains one {@link PixelAodClockView.FixedAdvanceSpan} per
     * character.  Calling {@code setTextSize()} every frame makes Android round every cell again,
     * so the 3, 1, and degree mark visibly shake even though the row centre is correct.  Keep the
     * captured source cells and scale only that text clone around its painted centre.  The weather
     * icon has an independent native-size track and is intentionally not affected here.
     */
    private void applyFixedCellInfoFrame(TextView view, InfoSnapshot from, InfoSnapshot to,
            float motionProgress) {
        if (view == null) {
            return;
        }
        InfoSnapshot source = effectiveInfoSource(from, to);
        InfoSnapshot target = effectiveInfoTarget(from, to);
        if (!source.valid || !target.valid) {
            return;
        }
        CouiClockSizeTransitionMath.InfoFrame frame =
                CouiClockSizeTransitionMath.informationFrame(source.element, target.element,
                        motionProgress);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, source.element.textSizePx);
        view.setLetterSpacing(source.letterSpacing);
        placeInfoAtVisualCenter(view, frame.centerX, frame.centerY);
        float scale = CouiClockSizeTransitionMath.fixedCellTextScale(
                source.element.textSizePx, frame.textSizePx);
        view.setScaleX(scale);
        view.setScaleY(scale);
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

    private void applyIconFrame(ImageView view, IconSnapshot from, IconSnapshot to,
            float motionProgress) {
        if (view == null) {
            return;
        }
        IconSnapshot source = effectiveIconSource(from, to);
        IconSnapshot target = effectiveIconTarget(from, to);
        if (!source.valid || !target.valid) {
            return;
        }
        float centerX = CouiClockSizeTransitionMath.interpolatedFloat(
                source.element.centerX, target.element.centerX, motionProgress);
        float centerY = CouiClockSizeTransitionMath.interpolatedFloat(
                source.element.centerY, target.element.centerY, motionProgress);
        float width = CouiClockSizeTransitionMath.interpolatedFloat(source.width, target.width,
                motionProgress);
        float height = CouiClockSizeTransitionMath.interpolatedFloat(source.height, target.height,
                motionProgress);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        float baseWidth = params != null && params.width > 0 ? params.width : view.getMeasuredWidth();
        float baseHeight = params != null && params.height > 0 ? params.height : view.getMeasuredHeight();
        baseWidth = Math.max(1f, baseWidth);
        baseHeight = Math.max(1f, baseHeight);
        view.setScaleX(width / baseWidth);
        view.setScaleY(height / baseHeight);
        view.setX(centerX - baseWidth / 2f);
        view.setY(centerY - baseHeight / 2f);
        view.setAlpha(CouiClockSizeTransitionMath.interpolatedFloat(
                source.element.alpha, target.element.alpha, motionProgress));
    }

    private static IconSnapshot effectiveIconSource(IconSnapshot from, IconSnapshot to) {
        if (from != null && from.valid) {
            return from;
        }
        return to != null && to.valid ? to.withAlpha(0f) : IconSnapshot.INVALID;
    }

    private static IconSnapshot effectiveIconTarget(IconSnapshot from, IconSnapshot to) {
        if (to != null && to.valid) {
            return to;
        }
        return from != null && from.valid ? from.withAlpha(0f) : IconSnapshot.INVALID;
    }

    private static void applyInterpolatedDrawableBounds(TextView view, InfoSnapshot source,
            InfoSnapshot target, float progress) {
        Drawable[] current = view.getCompoundDrawablesRelative();
        for (int index = 0; index < current.length; index++) {
            Drawable drawable = current[index];
            if (drawable == null) {
                continue;
            }
            Rect from = drawableBounds(source.drawables[index], drawable);
            Rect to = drawableBounds(target.drawables[index], drawable);
            // Current-weather compound icons are deliberately fixed at 15 dp in both layouts.
            // Interpolating equal-size bounds is not a visual transformation; it only changes the
            // drawable's internal origin while the neighbouring text is resized, which was the
            // source of the observed icon wobble. Keep the source bounds until the final target
            // hand-off whenever the painted dimensions already match.
            if (from.width() == to.width() && from.height() == to.height()) {
                continue;
            }
            drawable.setBounds(
                    CouiClockSizeTransitionMath.interpolatedDimension(from.left, to.left, progress),
                    CouiClockSizeTransitionMath.interpolatedDimension(from.top, to.top, progress),
                    CouiClockSizeTransitionMath.interpolatedDimension(from.right, to.right, progress),
                    CouiClockSizeTransitionMath.interpolatedDimension(from.bottom, to.bottom, progress));
        }
    }

    private static Rect drawableBounds(Drawable preferred, Drawable fallback) {
        Drawable source = preferred != null ? preferred : fallback;
        Rect bounds = source != null ? source.getBounds() : null;
        if (bounds != null && !bounds.isEmpty()) {
            return new Rect(bounds);
        }
        int width = source != null ? Math.max(1, source.getIntrinsicWidth()) : 1;
        int height = source != null ? Math.max(1, source.getIntrinsicHeight()) : 1;
        return new Rect(0, 0, width, height);
    }

    /** Positions an information clone by its painted text-and-drawable union, not its padded box. */
    private static void placeInfoAtVisualCenter(TextView view, float centerX, float centerY) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        float width = params != null && params.width > 0 ? params.width : view.getMeasuredWidth();
        float height = params != null && params.height > 0 ? params.height : view.getMeasuredHeight();
        width = Math.max(1f, width);
        height = Math.max(1f, height);
        float[] offset = informationVisualOffset(view, width, height);
        view.setPivotX(offset[0]);
        view.setPivotY(offset[1]);
        view.setX(CouiClockSizeTransitionMath.positionForVisualCenter(centerX, offset[0]));
        view.setY(CouiClockSizeTransitionMath.positionForVisualCenter(centerY, offset[1]));
    }

    /** Returns the painted centre inside a gravity-centred information clone. */
    private static float[] informationVisualOffset(TextView view, float width, float height) {
        CharSequence text = view.getText();
        String value = text != null ? text.toString() : "";
        Paint paint = view.getPaint();
        Rect textBounds = new Rect();
        paint.getTextBounds(value, 0, value.length(), textBounds);
        // Date, current weather, and contextual cards use FixedAdvanceSpan.  Measuring the raw
        // String here loses the stable cells, causing the clone to be centred differently from
        // the real TextView and then to snap at hand-off.
        float advance = Math.max(0f, PixelAodClockView.measuredTextAdvancePx(view));
        Paint.FontMetrics metrics = paint.getFontMetrics();
        if (textBounds.width() <= 0 || textBounds.height() <= 0) {
            textBounds.set(0, Math.round(metrics.ascent), Math.max(1, Math.round(advance)),
                    Math.round(metrics.descent));
        }
        Drawable[] drawables = view.getCompoundDrawablesRelative();
        Rect start = drawableBounds(drawables[0], null);
        Rect end = drawableBounds(drawables[2], null);
        boolean hasStart = drawables[0] != null;
        boolean hasEnd = drawables[2] != null;
        float startWidth = hasStart ? start.width() : 0f;
        float endWidth = hasEnd ? end.width() : 0f;
        float padding = view.getCompoundDrawablePadding();
        float contentWidth = startWidth + advance + endWidth
                + (hasStart && !value.isEmpty() ? padding : 0f)
                + (hasEnd && !value.isEmpty() ? padding : 0f);
        float contentLeft = informationContentLeft(view, width, contentWidth);
        float textLeft = contentLeft + startWidth
                + (hasStart && !value.isEmpty() ? padding : 0f);
        // The fixed cell corridor, not the changing ink edge of letters such as "m" and "r",
        // is the stable horizontal geometry used by both endpoints.
        float visualLeft = textLeft;
        float visualRight = textLeft + advance;
        Paint.FontMetricsInt lineMetrics = paint.getFontMetricsInt();
        // Mirror TextView.getVerticalOffset(): a font line taller than this compact clone box is
        // top-pinned, not centred. The previous unconditional centre was exactly the 10–11 px
        // weather-temperature baseline error measured in the transition recording.
        float baseline = CouiClockSizeTransitionMath.centeredTextBaseline(
                height, lineMetrics.ascent, lineMetrics.descent);
        float visualTop = baseline + textBounds.top;
        float visualBottom = baseline + textBounds.bottom;
        if (hasStart) {
            visualLeft = Math.min(visualLeft, contentLeft + start.left);
            visualRight = Math.max(visualRight, contentLeft + start.right);
            float top = (height - start.height()) / 2f + start.top;
            visualTop = Math.min(visualTop, top);
            visualBottom = Math.max(visualBottom, top + start.height());
        }
        if (hasEnd) {
            float endLeft = textLeft + advance + (value.isEmpty() ? 0f : padding);
            visualLeft = Math.min(visualLeft, endLeft + end.left);
            visualRight = Math.max(visualRight, endLeft + end.right);
            float top = (height - end.height()) / 2f + end.top;
            visualTop = Math.min(visualTop, top);
            visualBottom = Math.max(visualBottom, top + end.height());
        }
        return new float[] {(visualLeft + visualRight) / 2f, (visualTop + visualBottom) / 2f};
    }

    /**
     * The transition boxes deliberately have spare width for both endpoints.  Their visual
     * offset must nevertheless use the same horizontal gravity as the source TextView; Android
     * lays a leading compound drawable relative to a START-aligned text layout, not relative to
     * a synthetic centred union.
     */
    static float informationContentLeft(int gravity, int layoutDirection, float width,
            float contentWidth) {
        int absoluteGravity = gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int relativeGravity = gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (relativeGravity == Gravity.START) {
            absoluteGravity = layoutDirection == View.LAYOUT_DIRECTION_RTL
                    ? Gravity.RIGHT : Gravity.LEFT;
        } else if (relativeGravity == Gravity.END) {
            absoluteGravity = layoutDirection == View.LAYOUT_DIRECTION_RTL
                    ? Gravity.LEFT : Gravity.RIGHT;
        }
        if (absoluteGravity == Gravity.RIGHT) {
            return Math.max(0f, width - contentWidth);
        }
        if (absoluteGravity == Gravity.CENTER_HORIZONTAL) {
            return Math.max(0f, (width - contentWidth) / 2f);
        }
        return 0f;
    }

    private static float informationContentLeft(TextView view, float width, float contentWidth) {
        int gravity = view != null ? view.getGravity() : Gravity.START;
        int layoutDirection = view != null ? view.getLayoutDirection() : View.LAYOUT_DIRECTION_LTR;
        return informationContentLeft(gravity, layoutDirection, width, contentWidth);
    }

    /**
     * A stable font size is not enough to make a live contextual row safe to translate: its
     * parent can still lay it out again after the target clock mode is committed.  Always use
     * the isolated transition clone so a card has exactly one geometry owner per frame.
     */
    static boolean keepsContextualSubviewsAtNativeGeometry(float sourceTextSizePx,
            float targetTextSizePx) {
        return false;
    }

    /**
     * Aligns the actual painted glyph with the captured target. The clone box intentionally has
     * extra room for scaling, so its geometric centre is not stable for asymmetric glyphs.
     */
    private static void placeGlyphAtVisualCenter(TextView view, float centerX, float centerY) {
        if (view == null) {
            return;
        }
        ViewGroup.LayoutParams params = view.getLayoutParams();
        float width = params != null && params.width > 0 ? params.width : view.getMeasuredWidth();
        float height = params != null && params.height > 0 ? params.height : view.getMeasuredHeight();
        width = Math.max(1f, width);
        height = Math.max(1f, height);
        CharSequence text = view.getText();
        String value = text != null ? text.toString() : "";
        Paint paint = view.getPaint();
        Rect paintedBounds = new Rect();
        paint.getTextBounds(value, 0, value.length(), paintedBounds);
        if (paintedBounds.width() <= 0 || paintedBounds.height() <= 0) {
            Paint.FontMetrics metrics = paint.getFontMetrics();
            paintedBounds.left = 0;
            paintedBounds.right = Math.max(1, Math.round(paint.measureText(value)));
            paintedBounds.top = Math.round(metrics.ascent);
            paintedBounds.bottom = Math.round(metrics.descent);
        }
        float advance = Math.max(0f, paint.measureText(value));
        float visualX = CouiClockSizeTransitionMath.visualContentOffset(width, advance,
                paintedBounds.left, paintedBounds.right);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = (height / 2f) - ((metrics.ascent + metrics.descent) / 2f);
        float visualY = CouiClockSizeTransitionMath.paintedBaselineCenter(baseline,
                paintedBounds.top, paintedBounds.bottom);
        // Scaling around the painted centre keeps the first digit and colon stationary at both
        // endpoints even when a weight update changes their ink bounds.
        view.setPivotX(visualX);
        view.setPivotY(visualY);
        view.setX(CouiClockSizeTransitionMath.positionForVisualCenter(centerX, visualX));
        view.setY(CouiClockSizeTransitionMath.positionForVisualCenter(centerY, visualY));
    }

    private void applyWeights(int clockWeight, int infoWeight) {
        applyWeights(clockWeight, infoWeight, false);
    }

    private void applyWeights(int clockWeight, int infoWeight, boolean exactEndpoint) {
        int appliedClockWeight = exactEndpoint ? boundedWeight(clockWeight)
                : quantizeWeight(clockWeight);
        int appliedInfoWeight = exactEndpoint ? boundedWeight(infoWeight)
                : quantizeWeight(infoWeight);
        if (lastAppliedClockWeight != appliedClockWeight) {
            lastAppliedClockWeight = appliedClockWeight;
            for (TextView digit : digitViews) {
                PixelAodClockView.applySharedClockTypeface(digit, getContext(), appliedClockWeight);
            }
            PixelAodClockView.applySharedClockTypeface(colonView, getContext(), appliedClockWeight);
        }
        if (lastAppliedInfoWeight != appliedInfoWeight) {
            lastAppliedInfoWeight = appliedInfoWeight;
            PixelAodClockView.applySharedClockTypeface(dateView, getContext(), appliedInfoWeight);
            PixelAodClockView.applySharedClockTypeface(weatherView, getContext(), appliedInfoWeight);
            PixelAodClockView.applySharedClockTypeface(contextualView, getContext(), appliedInfoWeight);
        }
    }

    private static int quantizeWeight(int weight) {
        return boundedWeight(Math.round(weight / (float) WEIGHT_UPDATE_STEP) * WEIGHT_UPDATE_STEP);
    }

    private static int boundedWeight(int weight) {
        return Math.max(100, Math.min(500, weight));
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

    /**
     * Leaves the final overlay frame up until the real target has completed one layout/pre-draw
     * cycle. Restoring immediately here was the visible final-frame snap when a target Typeface
     * changed its advance or a weather compound drawable received its final bounds.
     */
    private void applyTargetWeightAndWaitForPreDraw(SceneSnapshot target, int clockWeight,
            int infoWeight, String reason) {
        applyTargetWeight(target.clockContent, clockWeight);
        applyTargetWeight(target.dateContent, infoWeight);
        applyTargetWeight(target.weatherContent, infoWeight);
        applyTargetWeight(target.contextualText, infoWeight);
        View targetView = target.clockContent != null ? target.clockContent
                : target.dateContent != null ? target.dateContent : target.weatherContent;
        if (targetView == null || !targetView.isAttachedToWindow()) {
            finishAndRestore(reason + "-no-target-predraw");
            return;
        }
        final long generation = ++targetPreDrawGeneration;
        waitingForTargetPreDraw = true;
        ViewTreeObserver observer = targetView.getViewTreeObserver();
        if (!observer.isAlive()) {
            targetView.postOnAnimation(() -> finishIfCurrentPreDraw(generation, reason));
            return;
        }
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ViewTreeObserver current = targetView.getViewTreeObserver();
                if (current.isAlive()) {
                    current.removeOnPreDrawListener(this);
                }
                finishIfCurrentPreDraw(generation, reason);
                return true;
            }
        });
        targetView.requestLayout();
        targetView.invalidate();
    }

    private void applyTargetWeight(TextView target, int weight) {
        if (target == null) {
            return;
        }
        PixelAodClockView.applySharedClockTypeface(target, getContext(), weight);
        target.requestLayout();
    }

    private void finishIfCurrentPreDraw(long generation, String reason) {
        if (!waitingForTargetPreDraw || generation != targetPreDrawGeneration) {
            return;
        }
        waitingForTargetPreDraw = false;
        finishAndRestore(reason + "-target-predraw");
    }

    private void finishAndRestore(String reason) {
        if (animator != null) {
            animator = null;
        }
        waitingForTargetPreDraw = false;
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
        contextualView = null;
        dateIconView = null;
        weatherIconView = null;
        contextualIconView = null;
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
        float cellStart = Math.min(leading, trailing);
        Paint paint = clock.getPaint();
        float referenceAdvance = PixelAodClockView.fixedClockGlyphReferenceAdvancePx(
                clock, value);
        float animatedAdvance = paint.measureText(text, offset, offset + 1);
        Rect paintedBounds = new Rect();
        paint.getTextBounds(text.toString(), offset, offset + 1, paintedBounds);
        if (paintedBounds.width() <= 0 || paintedBounds.height() <= 0) {
            Paint.FontMetrics metrics = paint.getFontMetrics();
            paintedBounds.left = 0;
            paintedBounds.right = Math.max(1, Math.round(animatedAdvance));
            paintedBounds.top = Math.round(metrics.ascent);
            paintedBounds.bottom = Math.round(metrics.descent);
        }
        float centerX = (clockLocation[0] - rootLocation[0])
                + clock.getTotalPaddingLeft()
                + CouiClockSizeTransitionMath.paintedGlyphCenter(cellStart, referenceAdvance,
                animatedAdvance, paintedBounds.left, paintedBounds.right);
        float baseline = layout.getLineBaseline(line);
        float centerY = (clockLocation[1] - rootLocation[1])
                + clock.getTotalPaddingTop()
                + CouiClockSizeTransitionMath.paintedBaselineCenter(
                baseline, paintedBounds.top, paintedBounds.bottom);
        CouiClockSizeTransitionMath.Element element =
                new CouiClockSizeTransitionMath.Element(centerX, centerY,
                        clock.getTextSize(), desiredAlpha(clock));
        float paintedWidth = Math.max(1f, paintedBounds.width());
        float paintedHeight = Math.max(1f, paintedBounds.height());
        return new GlyphSnapshot(value, element, paintedWidth, paintedHeight,
                clock.getCurrentTextColor(), clock.getTypeface());
    }

    private InfoSnapshot captureInformation(TextView view, int[] rootLocation) {
        return captureInformation(view, view, rootLocation);
    }

    /**
     * Captures only the painted text corridor.  Android positions a compound drawable outside
     * this corridor, so including it in the TextView clone makes a wider target row change the
     * text origin at the hand-off.  Icons are captured on their own track below.
     */
    private InfoSnapshot captureInformation(TextView view, View alphaOwner, int[] rootLocation) {
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
        TextMetrics text = captureTextMetrics(view, location, rootLocation);
        if (!text.valid()) {
            return InfoSnapshot.INVALID;
        }
        CouiClockSizeTransitionMath.Element element =
                new CouiClockSizeTransitionMath.Element(text.centerX(), text.centerY(),
                        view.getTextSize(), desiredAlpha(alphaOwner));
        return new InfoSnapshot(true, view.getText(), element, text.width(), text.height(),
                view.getCurrentTextColor(), view.getTypeface(), Gravity.CENTER,
                View.TEXT_ALIGNMENT_CENTER, view.getLetterSpacing(), 0, new Drawable[4],
                view.getShadowRadius(), view.getShadowDx(), view.getShadowDy(),
                view.getShadowColor());
    }

    private TextMetrics captureTextMetrics(TextView view, int[] location, int[] rootLocation) {
        Paint paint = view.getPaint();
        String value = view.getText().toString();
        Rect textBounds = new Rect();
        paint.getTextBounds(value, 0, value.length(), textBounds);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float advance = Math.max(0f, PixelAodClockView.measuredTextAdvancePx(view));
        if (textBounds.width() <= 0 || textBounds.height() <= 0) {
            textBounds.set(0, Math.round(metrics.ascent), Math.max(1, Math.round(advance)),
                    Math.round(metrics.descent));
        }
        Layout layout = view.getLayout();
        float lineLeft = layout != null && layout.getLineCount() > 0 ? layout.getLineLeft(0) : 0f;
        float lineRight = layout != null && layout.getLineCount() > 0
                ? layout.getLineRight(0) : lineLeft + advance;
        if (lineRight <= lineLeft) {
            lineRight = lineLeft + Math.max(1f, advance);
        }
        float baseline = layout != null && layout.getLineCount() > 0
                ? layout.getLineBaseline(0) : (view.getHeight() / 2f)
                - ((metrics.ascent + metrics.descent) / 2f);
        float left = (location[0] - rootLocation[0]) + view.getTotalPaddingLeft() + lineLeft;
        float right = (location[0] - rootLocation[0]) + view.getTotalPaddingLeft() + lineRight;
        float top = (location[1] - rootLocation[1]) + view.getTotalPaddingTop()
                + baseline + textBounds.top;
        float bottom = (location[1] - rootLocation[1]) + view.getTotalPaddingTop()
                + baseline + textBounds.bottom;
        return new TextMetrics(left, top, right, bottom);
    }

    /** Captures one leading or trailing compound weather icon at its actual on-screen centre. */
    private IconSnapshot captureCompoundIcon(TextView view, int[] rootLocation) {
        if (view == null || view.getVisibility() != View.VISIBLE || view.getWidth() <= 0
                || view.getHeight() <= 0) {
            return IconSnapshot.INVALID;
        }
        Drawable[] drawables = view.getCompoundDrawablesRelative();
        Drawable drawable = drawables[0] != null ? drawables[0] : drawables[2];
        if (drawable == null) {
            return IconSnapshot.INVALID;
        }
        int[] location = new int[2];
        try {
            view.getLocationOnScreen(location);
        } catch (Throwable ignored) {
            return IconSnapshot.INVALID;
        }
        Rect bounds = drawableBounds(drawable, null);
        float startWidth = drawables[0] != null ? bounds.width() : 0f;
        float endWidth = drawables[2] != null ? bounds.width() : 0f;
        String value = view.getText() != null ? view.getText().toString() : "";
        float advance = Math.max(0f, PixelAodClockView.measuredTextAdvancePx(view));
        float padding = view.getCompoundDrawablePadding();
        float contentWidth = startWidth + advance + endWidth
                + (drawables[0] != null && !value.isEmpty() ? padding : 0f)
                + (drawables[2] != null && !value.isEmpty() ? padding : 0f);
        float contentLeft = informationContentLeft(view, view.getWidth(), contentWidth);
        float textLeft = contentLeft + startWidth
                + (drawables[0] != null && !value.isEmpty() ? padding : 0f);
        float left = drawables[0] != null ? contentLeft + bounds.left
                : textLeft + advance + (value.isEmpty() ? 0f : padding) + bounds.left;
        float top = (view.getHeight() - bounds.height()) / 2f + bounds.top;
        CouiClockSizeTransitionMath.Element element =
                new CouiClockSizeTransitionMath.Element(
                        (location[0] - rootLocation[0]) + left + bounds.width() / 2f,
                        (location[1] - rootLocation[1]) + top + bounds.height() / 2f,
                        1f, desiredAlpha(view));
        Drawable copy = copyDrawable(drawable);
        if (copy != null) {
            copy.setBounds(0, 0, Math.max(1, bounds.width()), Math.max(1, bounds.height()));
        }
        return new IconSnapshot(true, element, bounds.width(), bounds.height(), copy);
    }

    /** Captures the separate ImageView used by contextual Weather Forecast and Weather Alert rows. */
    private IconSnapshot captureImageIcon(View row, ImageView icon, int[] rootLocation) {
        if (row == null || icon == null || row.getVisibility() != View.VISIBLE
                || icon.getVisibility() != View.VISIBLE || icon.getDrawable() == null
                || icon.getWidth() <= 0 || icon.getHeight() <= 0) {
            return IconSnapshot.INVALID;
        }
        int[] location = new int[2];
        try {
            icon.getLocationOnScreen(location);
        } catch (Throwable ignored) {
            return IconSnapshot.INVALID;
        }
        int width = Math.max(1, icon.getWidth());
        int height = Math.max(1, icon.getHeight());
        Drawable drawable = copyDrawable(icon.getDrawable());
        if (drawable != null) {
            drawable.setBounds(0, 0, width, height);
        }
        return new IconSnapshot(true, new CouiClockSizeTransitionMath.Element(
                (location[0] - rootLocation[0]) + width / 2f,
                (location[1] - rootLocation[1]) + height / 2f, 1f, desiredAlpha(row)),
                width, height, drawable);
    }

    static final class SceneSnapshot {
        static final SceneSnapshot EMPTY = new SceneSnapshot(null, null,
                InfoSnapshot.INVALID, InfoSnapshot.INVALID, InfoSnapshot.INVALID,
                IconSnapshot.INVALID, IconSnapshot.INVALID, IconSnapshot.INVALID,
                null, null, null, null, null, 0, 0);

        final GlyphSnapshot[] digits;
        final GlyphSnapshot colon;
        final InfoSnapshot date;
        final InfoSnapshot weather;
        final InfoSnapshot contextual;
        final IconSnapshot dateIcon;
        final IconSnapshot weatherIcon;
        final IconSnapshot contextualIcon;
        final TextView clockContent;
        final TextView dateContent;
        final TextView weatherContent;
        final View contextualContent;
        final TextView contextualText;
        final int clockWeight;
        final int infoWeight;
        final boolean compact;

        SceneSnapshot(GlyphSnapshot[] digits, GlyphSnapshot colon, InfoSnapshot date,
                InfoSnapshot weather, TextView clockContent, TextView dateContent,
                TextView weatherContent, int clockWeight, int infoWeight) {
            this(digits, colon, date, weather, InfoSnapshot.INVALID, clockContent, dateContent,
                    weatherContent, null, null, clockWeight, infoWeight);
        }

        SceneSnapshot(GlyphSnapshot[] digits, GlyphSnapshot colon, InfoSnapshot date,
                InfoSnapshot weather, InfoSnapshot contextual, TextView clockContent,
                TextView dateContent, TextView weatherContent, View contextualContent,
                TextView contextualText, int clockWeight, int infoWeight) {
            this(digits, colon, date, weather, contextual, IconSnapshot.INVALID,
                    IconSnapshot.INVALID, IconSnapshot.INVALID, clockContent, dateContent,
                    weatherContent, contextualContent, contextualText, clockWeight, infoWeight);
        }

        SceneSnapshot(GlyphSnapshot[] digits, GlyphSnapshot colon, InfoSnapshot date,
                InfoSnapshot weather, InfoSnapshot contextual, IconSnapshot dateIcon,
                IconSnapshot weatherIcon, IconSnapshot contextualIcon, TextView clockContent,
                TextView dateContent, TextView weatherContent, View contextualContent,
                TextView contextualText, int clockWeight, int infoWeight) {
            this.digits = digits;
            this.colon = colon;
            this.date = date != null ? date : InfoSnapshot.INVALID;
            this.weather = weather != null ? weather : InfoSnapshot.INVALID;
            this.contextual = contextual != null ? contextual : InfoSnapshot.INVALID;
            this.dateIcon = dateIcon != null ? dateIcon : IconSnapshot.INVALID;
            this.weatherIcon = weatherIcon != null ? weatherIcon : IconSnapshot.INVALID;
            this.contextualIcon = contextualIcon != null ? contextualIcon : IconSnapshot.INVALID;
            this.clockContent = clockContent;
            this.dateContent = dateContent;
            this.weatherContent = weatherContent;
            this.contextualContent = contextualContent;
            this.contextualText = contextualText;
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

    private static final class TextMetrics {
        final float left;
        final float top;
        final float right;
        final float bottom;

        TextMetrics(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }

        boolean valid() {
            return right > left && bottom > top;
        }

        float centerX() {
            return (left + right) / 2f;
        }

        float centerY() {
            return (top + bottom) / 2f;
        }

        float width() {
            return Math.max(1f, right - left);
        }

        float height() {
            return Math.max(1f, bottom - top);
        }
    }

    private static final class IconSnapshot {
        static final IconSnapshot INVALID = new IconSnapshot(false,
                new CouiClockSizeTransitionMath.Element(0f, 0f, 1f, 0f),
                1f, 1f, null);

        final boolean valid;
        final CouiClockSizeTransitionMath.Element element;
        final float width;
        final float height;
        final Drawable drawable;

        IconSnapshot(boolean valid, CouiClockSizeTransitionMath.Element element,
                float width, float height, Drawable drawable) {
            this.valid = valid;
            this.element = element;
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);
            this.drawable = drawable;
        }

        IconSnapshot withAlpha(float alpha) {
            return new IconSnapshot(valid, new CouiClockSizeTransitionMath.Element(
                    element.centerX, element.centerY, element.textSizePx, alpha), width, height,
                    drawable);
        }
    }

    private static final class InfoSnapshot {
        static final InfoSnapshot INVALID = new InfoSnapshot(false, "",
                new CouiClockSizeTransitionMath.Element(0f, 0f, 1f, 0f),
                1f, 1f, 0, Typeface.DEFAULT, Gravity.START, View.TEXT_ALIGNMENT_TEXT_START,
                0f, 0, new Drawable[4],
                0f, 0f, 0f, 0);

        final boolean valid;
        final CharSequence text;
        final CouiClockSizeTransitionMath.Element element;
        final float width;
        final float height;
        final int color;
        final Typeface typeface;
        final int gravity;
        final int textAlignment;
        final float letterSpacing;
        final int compoundDrawablePadding;
        final Drawable[] drawables;
        final float shadowRadius;
        final float shadowDx;
        final float shadowDy;
        final int shadowColor;

        InfoSnapshot(boolean valid, CharSequence text,
                CouiClockSizeTransitionMath.Element element, float width, float height,
                int color, Typeface typeface, int gravity, int textAlignment,
                float letterSpacing, int compoundDrawablePadding,
                Drawable[] drawables, float shadowRadius, float shadowDx, float shadowDy,
                int shadowColor) {
            this.valid = valid;
            this.text = text != null ? text : "";
            this.element = element;
            this.width = Math.max(1f, width);
            this.height = Math.max(1f, height);
            this.color = color;
            this.typeface = typeface;
            this.gravity = gravity;
            this.textAlignment = textAlignment;
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
                    width, height, color, typeface, gravity, textAlignment, letterSpacing,
                    compoundDrawablePadding,
                    drawables, shadowRadius, shadowDx, shadowDy, shadowColor);
        }
    }
}
