package dev.codex.pixelaod;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;

/** COUI-style press and authentication glow hosted above the vendor UDFPS surface. */
final class CouiUdfpsGlowOverlay {
    private static final int[] WINDOW_TYPES = { 2315, 2027, 2026, 2017, 2038 };
    private static final int WINDOW_FLAGS = 16_778_008;
    private static final String WINDOW_TITLE = "COUIExpressiveUdfpsGlow";
    private static final long PRESS_EXPAND_MS = 180L;
    private static final long PRESS_RETRACT_MS = 160L;
    private static final long SUCCESS_MS = 500L;
    private static final float PRESS_RADIUS_MULTIPLIER = 2.4f;
    private static final float SUCCESS_RADIUS_OVERSHOOT = 1.06f;
    private static final float SUCCESS_START_RADIUS_MULTIPLIER = 1.2f;
    private static final float SUCCESS_COLOR_TRANSITION_END = 0.28f;
    private static final float SUCCESS_SURFACE_INTRO_END = 0.12f;
    private static final float SUCCESS_FADE_START = 0.3f;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final WindowManager windowManager;
    private final GlowView view;
    private boolean added;

    CouiUdfpsGlowOverlay(Context context) {
        windowManager = (WindowManager) context.getSystemService(WindowManager.class);
        view = new GlowView(context);
    }

    void showPress(GlowSpec spec) {
        runOnMain(() -> {
            if (ensureAdded()) {
                view.showPress(spec);
            }
        });
    }

    void hidePress() {
        runOnMain(() -> {
            if (added) {
                view.hidePress();
            }
        });
    }

    void showSuccess(GlowSpec spec) {
        runOnMain(() -> {
            if (ensureAdded()) {
                view.showSuccess(spec);
            }
        });
    }

    private boolean ensureAdded() {
        if (added) {
            return true;
        }
        if (windowManager == null) {
            PixelAodLog.log("COUI UDFPS glow unavailable reason=no-window-manager");
            return false;
        }
        Throwable lastError = null;
        for (int type : WINDOW_TYPES) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WINDOW_FLAGS,
                    android.graphics.PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                params.layoutInDisplayCutoutMode = 3;
            }
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                params.setFitInsetsTypes(0);
            }
            params.setTitle(WINDOW_TITLE);
            try {
                windowManager.addView(view, params);
                added = true;
                PixelAodLog.i("COUI UDFPS glow overlay added type=" + type);
                return true;
            } catch (Throwable throwable) {
                lastError = throwable;
            }
        }
        PixelAodLog.log("COUI UDFPS glow overlay add failed", lastError);
        return false;
    }

    private void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }

    static final class GlowSpec {
        final float centerX;
        final float centerY;
        final float baseRadius;
        final int successIntroColor;
        final int successColor;
        final float successAlphaScale;

        GlowSpec(float centerX, float centerY, float baseRadius,
                int successIntroColor, int successColor, float successAlphaScale) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.baseRadius = baseRadius;
            this.successIntroColor = successIntroColor;
            this.successColor = successColor;
            this.successAlphaScale = successAlphaScale;
        }
    }

    private static final class GlowView extends View {
        private static final int MODE_NONE = 0;
        private static final int MODE_PRESS = 1;
        private static final int MODE_SUCCESS = 2;

        private static final PathInterpolator PRESS_EXPAND_INTERPOLATOR =
                new PathInterpolator(0.2f, 0f, 0f, 1f);
        private static final PathInterpolator PRESS_RETRACT_INTERPOLATOR =
                new PathInterpolator(0.4f, 0f, 0.6f, 1f);
        private static final PathInterpolator SUCCESS_GLOW_INTERPOLATOR =
                new PathInterpolator(SUCCESS_FADE_START, 0f, 0.2f, 1f);
        private static final PathInterpolator SUCCESS_INTRO_INTERPOLATOR =
                new PathInterpolator(0f, 0f, 0.2f, 1f);
        private static final PathInterpolator SUCCESS_FADE_INTERPOLATOR =
                new PathInterpolator(SUCCESS_FADE_START, 0f, 0.7f, 1f);

        private final Paint pressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint successIntroPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint successColorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix shaderMatrix = new Matrix();

        private int mode = MODE_NONE;
        private float pressProgress;
        private float successProgress;
        private float endRadius;
        private GlowSpec spec = new GlowSpec(0f, 0f, 1f, Color.WHITE, Color.WHITE, 1f);
        private ValueAnimator pressAnimator;
        private ValueAnimator successAnimator;
        private RadialGradient pressShader;
        private RadialGradient successIntroShader;
        private RadialGradient successColorShader;
        private GlowSpec successShaderSpec;

        GlowView(Context context) {
            super(context);
            setBackgroundColor(Color.TRANSPARENT);
            setVisibility(INVISIBLE);
            setWillNotDraw(false);
        }

        void showPress(GlowSpec newSpec) {
            if (mode == MODE_SUCCESS) {
                return;
            }
            cancelSuccessAnimator();
            spec = newSpec;
            mode = MODE_PRESS;
            successProgress = 0f;
            setVisibility(VISIBLE);
            ensurePressShader();
            animatePressTo(1f);
        }

        void hidePress() {
            if (mode == MODE_PRESS) {
                animatePressTo(0f);
            }
        }

        void showSuccess(GlowSpec newSpec) {
            if (mode == MODE_SUCCESS) {
                return;
            }
            cancelSuccessAnimator();
            cancelPressAnimator();
            spec = newSpec;
            if (!SystemAnimationScalePolicy.animationsEnabled()) {
                mode = MODE_NONE;
                pressProgress = 0f;
                successProgress = 0f;
                setVisibility(INVISIBLE);
                invalidate();
                return;
            }
            mode = MODE_SUCCESS;
            successProgress = 0f;
            setVisibility(VISIBLE);
            ensureSuccessShaders();
            ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(SUCCESS_MS);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(valueAnimator -> {
                successProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (successAnimator != animation) {
                        return;
                    }
                    successAnimator = null;
                    pressProgress = 0f;
                    mode = MODE_NONE;
                    setVisibility(INVISIBLE);
                }
            });
            successAnimator = animator;
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (mode == MODE_PRESS) {
                drawPressGlow(canvas, pressProgress, 1f);
            } else if (mode == MODE_SUCCESS) {
                drawSuccessGlow(canvas);
            }
        }

        @Override
        protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            pressShader = null;
            successShaderSpec = null;
            if (mode == MODE_PRESS) {
                ensurePressShader();
            } else if (mode == MODE_SUCCESS) {
                ensureSuccessShaders();
            }
        }

        private void animatePressTo(float target) {
            cancelPressAnimator();
            if (!SystemAnimationScalePolicy.animationsEnabled()) {
                pressProgress = target;
                if (target <= 0f && mode == MODE_PRESS) {
                    mode = MODE_NONE;
                    setVisibility(INVISIBLE);
                } else if (target > 0f) {
                    setVisibility(VISIBLE);
                }
                invalidate();
                return;
            }
            float start = pressProgress;
            ValueAnimator animator = ValueAnimator.ofFloat(start, target);
            animator.setDuration(target > start ? PRESS_EXPAND_MS : PRESS_RETRACT_MS);
            animator.setInterpolator(target > start
                    ? PRESS_EXPAND_INTERPOLATOR : PRESS_RETRACT_INTERPOLATOR);
            animator.addUpdateListener(valueAnimator -> {
                pressProgress = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (pressAnimator != animation) {
                        return;
                    }
                    pressAnimator = null;
                    if (target <= 0f && mode == MODE_PRESS) {
                        pressProgress = 0f;
                        mode = MODE_NONE;
                        setVisibility(INVISIBLE);
                    }
                }
            });
            pressAnimator = animator;
            animator.start();
        }

        private void cancelPressAnimator() {
            ValueAnimator animator = pressAnimator;
            pressAnimator = null;
            if (animator != null) {
                animator.cancel();
            }
        }

        private void cancelSuccessAnimator() {
            ValueAnimator animator = successAnimator;
            successAnimator = null;
            if (animator != null) {
                animator.cancel();
            }
        }

        private void ensurePressShader() {
            if (getWidth() <= 0 || getHeight() <= 0 || spec.baseRadius <= 0f) {
                return;
            }
            pressShader = new RadialGradient(
                    spec.centerX,
                    spec.centerY,
                    spec.baseRadius * PRESS_RADIUS_MULTIPLIER,
                    new int[] {
                            colorWithAlpha(Color.WHITE, 50),
                            colorWithAlpha(Color.WHITE, 44),
                            colorWithAlpha(Color.WHITE, 32),
                            colorWithAlpha(Color.WHITE, 8),
                            Color.TRANSPARENT
                    },
                    new float[] { 0f, 0.42f, 0.68f, 0.86f, 1f },
                    Shader.TileMode.CLAMP);
        }

        private void ensureSuccessShaders() {
            if (getWidth() <= 0 || getHeight() <= 0 || spec.baseRadius <= 0f) {
                return;
            }
            if (successIntroShader != null && successColorShader != null
                    && sameSpec(successShaderSpec, spec)) {
                return;
            }
            endRadius = (float) Math.hypot(
                    Math.max(spec.centerX, getWidth() - spec.centerX),
                    Math.max(spec.centerY, getHeight() - spec.centerY))
                    * SUCCESS_RADIUS_OVERSHOOT;
            successIntroShader = createSuccessShader(spec.successIntroColor,
                    spec.successAlphaScale);
            successColorShader = createSuccessShader(spec.successColor,
                    spec.successAlphaScale);
            successShaderSpec = spec;
        }

        private RadialGradient createSuccessShader(int color, float alphaScale) {
            return new RadialGradient(
                    spec.centerX,
                    spec.centerY,
                    Math.max(1f, endRadius),
                    new int[] {
                            colorWithAlpha(color, scaledAlpha(alphaScale, 4)),
                            colorWithAlpha(color, scaledAlpha(alphaScale, 24)),
                            colorWithAlpha(color, scaledAlpha(alphaScale, 40)),
                            colorWithAlpha(color, scaledAlpha(alphaScale, 44)),
                            colorWithAlpha(color, scaledAlpha(alphaScale, 8)),
                            Color.TRANSPARENT
                    },
                    new float[] { 0f, 0.42f, 0.58f, 0.72f, 0.86f, 1f },
                    Shader.TileMode.CLAMP);
        }

        private void drawPressGlow(Canvas canvas, float progress, float alpha) {
            if (pressShader == null) {
                return;
            }
            float clamped = clamp(progress);
            if (clamped <= 0f) {
                return;
            }
            shaderMatrix.reset();
            shaderMatrix.setScale(clamped, clamped, spec.centerX, spec.centerY);
            pressShader.setLocalMatrix(shaderMatrix);
            pressPaint.setShader(pressShader);
            pressPaint.setAlpha(Math.round(clamp(alpha) * 255f));
            canvas.drawCircle(spec.centerX, spec.centerY,
                    spec.baseRadius * PRESS_RADIUS_MULTIPLIER * clamped, pressPaint);
        }

        private void drawSuccessGlow(Canvas canvas) {
            if (endRadius <= 0f || successIntroShader == null || successColorShader == null) {
                return;
            }
            float progress = clamp(successProgress);
            float expansion = SUCCESS_GLOW_INTERPOLATOR.getInterpolation(progress);
            float colorProgress = clamp(progress / SUCCESS_COLOR_TRANSITION_END);
            float intro = SUCCESS_INTRO_INTERPOLATOR.getInterpolation(
                    clamp(progress / SUCCESS_SURFACE_INTRO_END));
            float fade = 1f - SUCCESS_FADE_INTERPOLATOR.getInterpolation(
                    clamp((progress - SUCCESS_FADE_START) / 0.7f));
            float radius = lerp(spec.baseRadius * SUCCESS_START_RADIUS_MULTIPLIER,
                    endRadius, expansion);
            float scale = radius / endRadius;
            shaderMatrix.reset();
            shaderMatrix.setScale(scale, scale, spec.centerX, spec.centerY);

            successIntroPaint.setShader(successIntroShader);
            successIntroPaint.setAlpha(clampAlpha((1f - colorProgress) * 255f * intro * fade));
            if (successIntroPaint.getAlpha() > 0) {
                successIntroShader.setLocalMatrix(shaderMatrix);
                canvas.drawCircle(spec.centerX, spec.centerY, radius, successIntroPaint);
            }
            successColorPaint.setShader(successColorShader);
            successColorPaint.setAlpha(clampAlpha(colorProgress * 255f * intro * fade));
            if (successColorPaint.getAlpha() > 0) {
                successColorShader.setLocalMatrix(shaderMatrix);
                canvas.drawCircle(spec.centerX, spec.centerY, radius, successColorPaint);
            }
        }

        private static boolean sameSpec(GlowSpec left, GlowSpec right) {
            return left != null && right != null
                    && left.centerX == right.centerX
                    && left.centerY == right.centerY
                    && left.baseRadius == right.baseRadius
                    && left.successIntroColor == right.successIntroColor
                    && left.successColor == right.successColor
                    && left.successAlphaScale == right.successAlphaScale;
        }

        private static int colorWithAlpha(int color, int alpha) {
            return Color.argb(Math.max(0, Math.min(255, alpha)),
                    Color.red(color), Color.green(color), Color.blue(color));
        }

        private static int scaledAlpha(float scale, int alpha) {
            return Math.max(0, Math.min(255, Math.round(Math.max(0f, scale) * alpha)));
        }

        private static int clampAlpha(float value) {
            return Math.max(0, Math.min(255, Math.round(value)));
        }

        private static float lerp(float from, float to, float progress) {
            return from + (to - from) * progress;
        }

        private static float clamp(float value) {
            return Math.max(0f, Math.min(1f, value));
        }
    }
}
