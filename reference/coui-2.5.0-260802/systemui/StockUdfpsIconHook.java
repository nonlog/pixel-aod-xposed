package one.dot.couiexpressive.hooks.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Application;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorSpace;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import defpackage.a80;
import defpackage.aq;
import defpackage.bu0;
import defpackage.ct;
import defpackage.cw;
import defpackage.dr;
import defpackage.e7;
import defpackage.e72;
import defpackage.fd;
import defpackage.k31;
import defpackage.kr1;
import defpackage.l22;
import defpackage.l4;
import defpackage.lr1;
import defpackage.mr1;
import defpackage.nr;
import defpackage.ob0;
import defpackage.p80;
import defpackage.ph0;
import defpackage.sb0;
import defpackage.sc1;
import defpackage.tc1;
import defpackage.tr1;
import defpackage.u21;
import defpackage.us0;
import defpackage.vb0;
import defpackage.vj1;
import defpackage.w41;
import defpackage.x30;
import defpackage.zt;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Set;
import one.dot.couiexpressive.ConfigStore;

public final class StockUdfpsIconHook implements IXposedHookLoadPackage {
    private static final String ALPHA_NORMALIZED_FIELD = "coe_stock_udfps_alpha_normalized";
    private static final float AOD_DASH_GAP = 4.5f;
    private static final float AOD_DASH_LENGTH = 4.0f;
    private static final String AOD_EXIT_ANIMATOR_FIELD = "coe_stock_udfps_aod_exit_animator";
    private static final long AOD_EXIT_FALLBACK_DURATION_MS = 500;
    private static final int AOD_FADE_OUT_RUNNABLE_ID = 22;
    private static final float AOD_STROKE_WIDTH = 2.0f;
    private static final String AUTH_RIPPLE_CONTROLLER_CLASS = "com.android.systemui.biometrics.AuthRippleController";
    private static final String AUTH_RIPPLE_VIEW_CLASS = "com.android.systemui.biometrics.AuthRippleView";
    private static final float BACKGROUND_DIAMETER_DP = 64.0f;
    private static final String DARK_BACKGROUND = "system_neutral2_800";
    private static final String DARK_FOREGROUND = "system_neutral1_100";
    private static final String FINGERPRINT_PATH_DATA = "M25.5,16.3283C28.47,14.8433 31.9167,14 35.5834,14C39.2501,14 42.6968,14.8433 45.6668,16.3283 M20,28.6669C22.7683,24.3402 28.7084,21.3335 35.5834,21.3335C42.4585,21.3335 48.3985,24.3402 51.1669,28.6669 M22.8607,47.0002C21.834,44.3235 21.834,41.5002 21.834,41.5002C21.834,34.4051 27.7374,28.6667 35.5841,28.6667C43.4308,28.6667 49.3341,34.4051 49.3341,41.5002 M49.3344,41.5003V42.0319C49.3344,44.7636 47.1161,47.0003 44.3661,47.0003C41.9461,47.0003 39.8744,45.2403 39.471,42.857L38.9577,39.7769C38.591,37.5953 36.7027,36.0002 34.5027,36.0002C26.5826,36.0002 29.846,49.1087 35.291,50.6487 M44.9713,54.6267C42.5513,56.7167 39.2879,58.0001 35.5846,58.0001C32.2296,58.0001 29.2229,56.9551 26.8945,55.195";
    private static final String FINGERPRINT_UTILS_CLASS = "com.oplus.systemui.biometrics.finger.KeyguardFingerprintUtils";
    private static final float FOREGROUND_SCALE = 0.5f;
    private static final String FP_DRAWABLE_FIELD = "coe_stock_udfps_drawable";
    private static final String HDR_ATTACH_FIELD = "coe_stock_udfps_hdr_attach";
    private static final float HDR_BUFFER_RATIO = 7.0f;
    private static final float HDR_WHITE_COMPONENT = 7.0f;
    private static final float ICON_SIZE_DP = 80.0f;
    private static final String LIGHT_BACKGROUND = "system_neutral2_50";
    private static final String LIGHT_FOREGROUND = "system_neutral1_800";
    private static final int MODE_IDLE = 0;
    private static final int MODE_PRESS = 1;
    private static final int MODE_SUCCESS = 2;
    private static final float NORMAL_STROKE_WIDTH = 3.0f;
    private static final String OPLUS_BIOMETRIC_UNLOCK_CLASS = "com.oplus.systemui.statusbar.phone.OplusBiometricUnlockControllerExImpl";
    private static final String OVERLAY_WINDOW_TITLE = "COUIExpressiveUdfpsGlow";
    private static final String PRESSED_ICON_CLASS = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintPressedIcon";
    private static final long PRESS_GLOW_EXPAND_DURATION_MS = 180;
    private static final float PRESS_GLOW_RADIUS_MULTIPLIER = 2.4f;
    private static final long PRESS_GLOW_RETRACT_DURATION_MS = 160;
    private static final String RIPPLE_PRESSED_FIELD = "coe_stock_udfps_ripple_pressed";
    private static final long STATE_TRANSITION_DURATION_MS = 420;
    private static final float SUCCESS_COLOR_TRANSITION_END = 0.28f;
    private static final long SUCCESS_GLOW_DURATION_MS = 500;
    private static final float SUCCESS_RADIUS_OVERSHOOT = 1.06f;
    private static final String SUCCESS_RIPPLE_DARK_TONE = "system_accent1_200";
    private static final float SUCCESS_RIPPLE_LIGHT_ALPHA_SCALE = 1.35f;
    private static final String SUCCESS_RIPPLE_LIGHT_INTRO_TONE = "system_accent1_0";
    private static final String SUCCESS_RIPPLE_LIGHT_TONE = "system_accent1_500";
    private static final float SUCCESS_START_RADIUS_MULTIPLIER = 1.2f;
    private static final float SUCCESS_SURFACE_INTRO_END = 0.12f;
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "COE-StockUdfpsIcon";
    private static final String UI_MECH_CLASS = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech";
    private static final String UPDATE_MONITOR_CALLBACK_CLASS = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateMonitorCallback$1";
    private static final float VIEWPORT_SIZE = 72.0f;
    private static final String VISUAL_REFRESH_FORCE_FIELD = "coe_stock_udfps_refresh_force";
    private static final String VISUAL_REFRESH_PENDING_FIELD = "coe_stock_udfps_refresh_pending";
    private static final int WINDOW_TYPE_DISPLAY_OVERLAY = 2026;
    private static final int WINDOW_TYPE_MAGNIFICATION_OVERLAY = 2027;
    private static final int WINDOW_TYPE_OPLUS_FINGERPRINT_OVERLAY = 2315;
    private static final int WINDOW_TYPE_STATUS_BAR_SUB_PANEL = 2017;
    private volatile UdfpsGlowOverlay glowOverlay;
    private volatile Context lastGlowContext;
    private volatile GlowGeometry lastGlowGeometry;
    private volatile Object lastUiMech;
    private boolean useHdrPressEffect;
    private static final Companion Companion = new Companion(null);
    public static final int $stable = 8;
    private static final PathInterpolator STATE_TRANSITION_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    private static final PathInterpolator PRESS_GLOW_EXPAND_INTERPOLATOR = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
    private static final PathInterpolator PRESS_GLOW_RETRACT_INTERPOLATOR = new PathInterpolator(0.4f, 0.0f, 0.6f, 1.0f);
    private static final float SUCCESS_FADE_START = 0.3f;
    private static final PathInterpolator SUCCESS_GLOW_INTERPOLATOR = new PathInterpolator(SUCCESS_FADE_START, 0.0f, 0.2f, 1.0f);
    private static final PathInterpolator SUCCESS_INTRO_INTERPOLATOR = new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);
    private static final PathInterpolator SUCCESS_FADE_INTERPOLATOR = new PathInterpolator(SUCCESS_FADE_START, 0.0f, 0.7f, 1.0f);
    private static final LinearInterpolator SUCCESS_TIMELINE_INTERPOLATOR = new LinearInterpolator();
    private static final List<String> STATE_REFRESH_METHODS = e72.M("loadAnimDrawables", "restoreIconDrawable", "restoreIconDrawableDark", "updateFpIconColor", "updateFpColor", "updateFpIconState", "fpIconShow", "setVisibilityInAOD", "notifyShowAodIcon", "notifyHideAodIcon", "setOnDozeState", "setOnDreamingStart", "onDreamingStart", "onDreamingStopped", "onScreenTurnedOff", "onScreenTurnedOn", "startToAnimInDream", "onFpTouch", "setTouchDownNow", "stopOpticalAnimation", "stopPressedAnimation");
    private static final Set<String> FORCE_REFRESH_METHODS = vj1.q("loadAnimDrawables", "updateFpIconColor", "updateFpColor");
    private static final String UI_MECH_GENERIC_RUNNABLE_CLASS = "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$1";
    private static final List<String> ASYNC_VISUAL_RUNNABLE_CLASSES = e72.M(UI_MECH_GENERIC_RUNNABLE_CLASS, "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$fpIconShow$2", "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$restoreIconDrawable$1", "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$touchEvent$2", "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateFpColor$1");
    private final int[] glowLocation = new int[MODE_SUCCESS];
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean enableSuccessRipple = true;

    public static final class GlowGeometry {
        private final float baseRadius;
        private final float centerX;
        private final float centerY;

        public GlowGeometry(float f, float f2, float f3) {
            this.centerX = f;
            this.centerY = f2;
            this.baseRadius = f3;
        }

        public static GlowGeometry copy$default(GlowGeometry glowGeometry, float f, float f2, float f3, int i, Object obj) {
            if ((i & StockUdfpsIconHook.MODE_PRESS) != 0) {
                f = glowGeometry.centerX;
            }
            if ((i & StockUdfpsIconHook.MODE_SUCCESS) != 0) {
                f2 = glowGeometry.centerY;
            }
            if ((i & 4) != 0) {
                f3 = glowGeometry.baseRadius;
            }
            return glowGeometry.copy(f, f2, f3);
        }

        public final float component1() {
            return this.centerX;
        }

        public final float component2() {
            return this.centerY;
        }

        public final float component3() {
            return this.baseRadius;
        }

        public final GlowGeometry copy(float f, float f2, float f3) {
            return new GlowGeometry(f, f2, f3);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GlowGeometry)) {
                return false;
            }
            GlowGeometry glowGeometry = (GlowGeometry) obj;
            return Float.compare(this.centerX, glowGeometry.centerX) == 0 && Float.compare(this.centerY, glowGeometry.centerY) == 0 && Float.compare(this.baseRadius, glowGeometry.baseRadius) == 0;
        }

        public final float getBaseRadius() {
            return this.baseRadius;
        }

        public final float getCenterX() {
            return this.centerX;
        }

        public final float getCenterY() {
            return this.centerY;
        }

        public int hashCode() {
            return Float.hashCode(this.baseRadius) + x30.c(Float.hashCode(this.centerX) * 31, 31, this.centerY);
        }

        public String toString() {
            return "GlowGeometry(centerX=" + this.centerX + ", centerY=" + this.centerY + ", baseRadius=" + this.baseRadius + ")";
        }
    }

    public static final class GlowOverlayView extends View {
        private float endRadius;
        private int mode;
        private ValueAnimator pressAnimator;
        private final Paint pressPaint;
        private float pressProgress;
        private RadialGradient pressShader;
        private PressShaderKey pressShaderKey;
        private final Matrix shaderMatrix;
        private GlowSpec spec;
        private ValueAnimator successAnimator;
        private final Paint successIntroPaint;
        private RadialGradient successIntroShader;
        private final Paint successMonetPaint;
        private RadialGradient successMonetShader;
        private float successProgress;
        private GlowSpec successShaderSpec;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public GlowOverlayView(Context context) {
            super(context);
            context.getClass();
            this.pressPaint = new Paint(StockUdfpsIconHook.MODE_PRESS);
            this.successIntroPaint = new Paint(StockUdfpsIconHook.MODE_PRESS);
            this.successMonetPaint = new Paint(StockUdfpsIconHook.MODE_PRESS);
            this.shaderMatrix = new Matrix();
            this.spec = new GlowSpec(0.0f, 0.0f, 1.0f, -1, -1, 1.0f);
            this.endRadius = 1.0f;
            setBackgroundColor(0);
            setVisibility(4);
        }

        private final void animatePressTo(final float f) {
            cancelPressAnimator();
            float f2 = this.pressProgress;
            int i = StockUdfpsIconHook.MODE_PRESS;
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(f2, f);
            valueAnimatorOfFloat.setDuration(f > this.pressProgress ? StockUdfpsIconHook.PRESS_GLOW_EXPAND_DURATION_MS : StockUdfpsIconHook.PRESS_GLOW_RETRACT_DURATION_MS);
            valueAnimatorOfFloat.setInterpolator(f > this.pressProgress ? StockUdfpsIconHook.PRESS_GLOW_EXPAND_INTERPOLATOR : StockUdfpsIconHook.PRESS_GLOW_RETRACT_INTERPOLATOR);
            v vVar = new v(i);
            vVar.b = this;
            VarHandle.storeStoreFence();
            valueAnimatorOfFloat.addUpdateListener(vVar);
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.getClass();
                    if (this.this$0.pressAnimator != animator) {
                        return;
                    }
                    this.this$0.pressAnimator = null;
                    if (f > 0.0f || this.this$0.mode != 1) {
                        return;
                    }
                    this.this$0.pressProgress = 0.0f;
                    this.this$0.mode = 0;
                    this.this$0.setVisibility(4);
                }
            });
            this.pressAnimator = valueAnimatorOfFloat;
            valueAnimatorOfFloat.start();
        }

        public static final void animatePressTo$lambda$0$0(GlowOverlayView glowOverlayView, ValueAnimator valueAnimator) {
            glowOverlayView.pressProgress = ((Float) x30.i(valueAnimator)).floatValue();
            glowOverlayView.invalidatePressRegion();
        }

        private final void cancelPressAnimator() {
            ValueAnimator valueAnimator = this.pressAnimator;
            this.pressAnimator = null;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
        }

        private final void cancelSuccessAnimator() {
            ValueAnimator valueAnimator = this.successAnimator;
            this.successAnimator = null;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
        }

        private final int colorWithAlpha(int i, int i2) {
            return Color.argb(nr.q(i2, 0, 255), Color.red(i), Color.green(i), Color.blue(i));
        }

        private final RadialGradient createSuccessSurfaceShader(int i, float f) {
            return new RadialGradient(this.spec.getCenterX(), this.spec.getCenterY(), this.endRadius, new int[]{colorWithAlpha(i, createSuccessSurfaceShader$scaledAlpha(f, 4)), colorWithAlpha(i, createSuccessSurfaceShader$scaledAlpha(f, 24)), colorWithAlpha(i, createSuccessSurfaceShader$scaledAlpha(f, 40)), colorWithAlpha(i, createSuccessSurfaceShader$scaledAlpha(f, 44)), colorWithAlpha(i, createSuccessSurfaceShader$scaledAlpha(f, 8)), 0}, new float[]{0.0f, 0.42f, 0.58f, 0.72f, 0.86f, 1.0f}, Shader.TileMode.CLAMP);
        }

        private static final int createSuccessSurfaceShader$scaledAlpha(float f, int i) {
            float f2 = i;
            if (f < 0.0f) {
                f = 0.0f;
            }
            return nr.q((int) (f2 * f), 0, 255);
        }

        private final void drawPressGlow(Canvas canvas, float f, float f2) {
            RadialGradient radialGradient = this.pressShader;
            if (radialGradient == null) {
                return;
            }
            float fP = nr.p(f, 0.0f, 1.0f);
            if (fP <= 0.0f) {
                return;
            }
            this.shaderMatrix.reset();
            this.shaderMatrix.setScale(fP, fP, this.spec.getCenterX(), this.spec.getCenterY());
            radialGradient.setLocalMatrix(this.shaderMatrix);
            this.pressPaint.setShader(radialGradient);
            this.pressPaint.setAlpha((int) (nr.p(f2, 0.0f, 1.0f) * 255.0f));
            canvas.drawCircle(this.spec.getCenterX(), this.spec.getCenterY(), this.spec.getBaseRadius() * StockUdfpsIconHook.PRESS_GLOW_RADIUS_MULTIPLIER * fP, this.pressPaint);
        }

        public static void drawPressGlow$default(GlowOverlayView glowOverlayView, Canvas canvas, float f, float f2, int i, Object obj) {
            if ((i & 4) != 0) {
                f2 = 1.0f;
            }
            glowOverlayView.drawPressGlow(canvas, f, f2);
        }

        private final void drawSuccessGlow(Canvas canvas) {
            RadialGradient radialGradient;
            RadialGradient radialGradient2;
            if (this.endRadius <= 0.0f || (radialGradient = this.successIntroShader) == null || (radialGradient2 = this.successMonetShader) == null) {
                return;
            }
            float fP = nr.p(this.successProgress, 0.0f, 1.0f);
            float interpolation = StockUdfpsIconHook.SUCCESS_GLOW_INTERPOLATOR.getInterpolation(fP);
            float fP2 = nr.p(fP / StockUdfpsIconHook.SUCCESS_COLOR_TRANSITION_END, 0.0f, 1.0f);
            float interpolation2 = StockUdfpsIconHook.SUCCESS_INTRO_INTERPOLATOR.getInterpolation(nr.p(fP / StockUdfpsIconHook.SUCCESS_SURFACE_INTRO_END, 0.0f, 1.0f));
            float interpolation3 = 1.0f - StockUdfpsIconHook.SUCCESS_FADE_INTERPOLATOR.getInterpolation(nr.p((fP - StockUdfpsIconHook.SUCCESS_FADE_START) / 0.7f, 0.0f, 1.0f));
            float fLerp = StockUdfpsIconHook.Companion.lerp(this.spec.getBaseRadius() * StockUdfpsIconHook.SUCCESS_START_RADIUS_MULTIPLIER, this.endRadius, interpolation);
            float f = fLerp / this.endRadius;
            this.shaderMatrix.reset();
            this.shaderMatrix.setScale(f, f, this.spec.getCenterX(), this.spec.getCenterY());
            this.successIntroPaint.setShader(radialGradient);
            this.successIntroPaint.setAlpha(nr.q((int) ((1.0f - fP2) * 255.0f * interpolation2 * interpolation3), 0, 255));
            if (this.successIntroPaint.getAlpha() > 0) {
                radialGradient.setLocalMatrix(this.shaderMatrix);
                canvas.drawCircle(this.spec.getCenterX(), this.spec.getCenterY(), fLerp, this.successIntroPaint);
            }
            this.successMonetPaint.setShader(radialGradient2);
            this.successMonetPaint.setAlpha(nr.q((int) (255.0f * fP2 * interpolation2 * interpolation3), 0, 255));
            if (this.successMonetPaint.getAlpha() > 0) {
                radialGradient2.setLocalMatrix(this.shaderMatrix);
                canvas.drawCircle(this.spec.getCenterX(), this.spec.getCenterY(), fLerp, this.successMonetPaint);
            }
        }

        private final void ensurePressShader() {
            if (getWidth() <= 0 || getHeight() <= 0 || this.spec.getBaseRadius() <= 0.0f) {
                return;
            }
            PressShaderKey pressShaderKey = new PressShaderKey(this.spec.getCenterX(), this.spec.getCenterY(), this.spec.getBaseRadius());
            if (this.pressShader == null || !ph0.i(this.pressShaderKey, pressShaderKey)) {
                this.pressShader = new RadialGradient(this.spec.getCenterX(), this.spec.getCenterY(), this.spec.getBaseRadius() * StockUdfpsIconHook.PRESS_GLOW_RADIUS_MULTIPLIER, new int[]{colorWithAlpha(-1, 50), colorWithAlpha(-1, 44), colorWithAlpha(-1, 32), colorWithAlpha(-1, 8), 0}, new float[]{0.0f, 0.42f, 0.68f, 0.86f, 1.0f}, Shader.TileMode.CLAMP);
                this.pressShaderKey = pressShaderKey;
            }
        }

        private final void ensureSuccessShaders() {
            if (getWidth() <= 0 || getHeight() <= 0 || this.spec.getBaseRadius() <= 0.0f) {
                return;
            }
            if (this.successIntroShader == null || this.successMonetShader == null || !ph0.i(this.successShaderSpec, this.spec)) {
                this.endRadius = ((float) Math.hypot(Math.max(this.spec.getCenterX(), getWidth() - this.spec.getCenterX()), Math.max(this.spec.getCenterY(), getHeight() - this.spec.getCenterY()))) * StockUdfpsIconHook.SUCCESS_RADIUS_OVERSHOOT;
                this.successIntroShader = createSuccessSurfaceShader(this.spec.getSuccessIntroColor(), this.spec.getSuccessAlphaScale());
                this.successMonetShader = createSuccessSurfaceShader(this.spec.getSuccessColor(), this.spec.getSuccessAlphaScale());
                this.successShaderSpec = this.spec;
            }
        }

        private final void invalidatePressRegion() {
            float baseRadius = (this.spec.getBaseRadius() * StockUdfpsIconHook.PRESS_GLOW_RADIUS_MULTIPLIER) + StockUdfpsIconHook.AOD_STROKE_WIDTH;
            invalidate((int) (this.spec.getCenterX() - baseRadius), (int) (this.spec.getCenterY() - baseRadius), ((int) (this.spec.getCenterX() + baseRadius)) + StockUdfpsIconHook.MODE_PRESS, ((int) (this.spec.getCenterY() + baseRadius)) + StockUdfpsIconHook.MODE_PRESS);
        }

        public static final void showSuccess$lambda$0$0(GlowOverlayView glowOverlayView, ValueAnimator valueAnimator) {
            glowOverlayView.successProgress = ((Float) x30.i(valueAnimator)).floatValue();
            glowOverlayView.invalidate();
        }

        public final void hidePress() {
            if (this.mode != StockUdfpsIconHook.MODE_PRESS) {
                return;
            }
            animatePressTo(0.0f);
        }

        @Override
        public void onDraw(Canvas canvas) {
            canvas.getClass();
            super.onDraw(canvas);
            int i = this.mode;
            if (i == StockUdfpsIconHook.MODE_PRESS) {
                drawPressGlow$default(this, canvas, this.pressProgress, 0.0f, 4, null);
            } else {
                if (i != StockUdfpsIconHook.MODE_SUCCESS) {
                    return;
                }
                drawSuccessGlow(canvas);
            }
        }

        @Override
        public void onSizeChanged(int i, int i2, int i3, int i4) {
            super.onSizeChanged(i, i2, i3, i4);
            this.pressShaderKey = null;
            this.successShaderSpec = null;
            if (this.mode == StockUdfpsIconHook.MODE_PRESS) {
                ensurePressShader();
            }
            if (this.mode == StockUdfpsIconHook.MODE_SUCCESS) {
                ensureSuccessShaders();
            }
        }

        public final void showPress(GlowSpec glowSpec) {
            glowSpec.getClass();
            if (this.mode == StockUdfpsIconHook.MODE_SUCCESS) {
                return;
            }
            cancelSuccessAnimator();
            this.spec = glowSpec;
            this.mode = StockUdfpsIconHook.MODE_PRESS;
            this.successProgress = 0.0f;
            setVisibility(0);
            ensurePressShader();
            animatePressTo(1.0f);
        }

        public final void showSuccess(GlowSpec glowSpec) {
            glowSpec.getClass();
            if (this.mode == StockUdfpsIconHook.MODE_SUCCESS) {
                return;
            }
            cancelSuccessAnimator();
            cancelPressAnimator();
            this.spec = glowSpec;
            this.mode = StockUdfpsIconHook.MODE_SUCCESS;
            this.successProgress = 0.0f;
            setVisibility(0);
            ensureSuccessShaders();
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.setDuration(500L);
            valueAnimatorOfFloat.setInterpolator(StockUdfpsIconHook.SUCCESS_TIMELINE_INTERPOLATOR);
            v vVar = new v(0);
            vVar.b = this;
            VarHandle.storeStoreFence();
            valueAnimatorOfFloat.addUpdateListener(vVar);
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.getClass();
                    if (this.this$0.successAnimator != animator) {
                        return;
                    }
                    this.this$0.successAnimator = null;
                    this.this$0.pressProgress = 0.0f;
                    this.this$0.mode = 0;
                    this.this$0.setVisibility(4);
                }
            });
            this.successAnimator = valueAnimatorOfFloat;
            valueAnimatorOfFloat.start();
        }
    }

    public static final class GlowSpec {
        private final float baseRadius;
        private final float centerX;
        private final float centerY;
        private final float successAlphaScale;
        private final int successColor;
        private final int successIntroColor;

        public GlowSpec(float f, float f2, float f3, int i, int i2, float f4) {
            this.centerX = f;
            this.centerY = f2;
            this.baseRadius = f3;
            this.successIntroColor = i;
            this.successColor = i2;
            this.successAlphaScale = f4;
        }

        public static GlowSpec copy$default(GlowSpec glowSpec, float f, float f2, float f3, int i, int i2, float f4, int i3, Object obj) {
            if ((i3 & StockUdfpsIconHook.MODE_PRESS) != 0) {
                f = glowSpec.centerX;
            }
            if ((i3 & StockUdfpsIconHook.MODE_SUCCESS) != 0) {
                f2 = glowSpec.centerY;
            }
            if ((i3 & 4) != 0) {
                f3 = glowSpec.baseRadius;
            }
            if ((i3 & 8) != 0) {
                i = glowSpec.successIntroColor;
            }
            if ((i3 & 16) != 0) {
                i2 = glowSpec.successColor;
            }
            if ((i3 & 32) != 0) {
                f4 = glowSpec.successAlphaScale;
            }
            int i4 = i2;
            float f5 = f4;
            return glowSpec.copy(f, f2, f3, i, i4, f5);
        }

        public final float component1() {
            return this.centerX;
        }

        public final float component2() {
            return this.centerY;
        }

        public final float component3() {
            return this.baseRadius;
        }

        public final int component4() {
            return this.successIntroColor;
        }

        public final int component5() {
            return this.successColor;
        }

        public final float component6() {
            return this.successAlphaScale;
        }

        public final GlowSpec copy(float f, float f2, float f3, int i, int i2, float f4) {
            return new GlowSpec(f, f2, f3, i, i2, f4);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GlowSpec)) {
                return false;
            }
            GlowSpec glowSpec = (GlowSpec) obj;
            return Float.compare(this.centerX, glowSpec.centerX) == 0 && Float.compare(this.centerY, glowSpec.centerY) == 0 && Float.compare(this.baseRadius, glowSpec.baseRadius) == 0 && this.successIntroColor == glowSpec.successIntroColor && this.successColor == glowSpec.successColor && Float.compare(this.successAlphaScale, glowSpec.successAlphaScale) == 0;
        }

        public final float getBaseRadius() {
            return this.baseRadius;
        }

        public final float getCenterX() {
            return this.centerX;
        }

        public final float getCenterY() {
            return this.centerY;
        }

        public final float getSuccessAlphaScale() {
            return this.successAlphaScale;
        }

        public final int getSuccessColor() {
            return this.successColor;
        }

        public final int getSuccessIntroColor() {
            return this.successIntroColor;
        }

        public int hashCode() {
            return Float.hashCode(this.successAlphaScale) + x30.d(this.successColor, x30.d(this.successIntroColor, x30.c(x30.c(Float.hashCode(this.centerX) * 31, 31, this.centerY), 31, this.baseRadius), 31), 31);
        }

        public String toString() {
            return "GlowSpec(centerX=" + this.centerX + ", centerY=" + this.centerY + ", baseRadius=" + this.baseRadius + ", successIntroColor=" + this.successIntroColor + ", successColor=" + this.successColor + ", successAlphaScale=" + this.successAlphaScale + ")";
        }
    }

    public static final class PressShaderKey {
        private final float baseRadius;
        private final float centerX;
        private final float centerY;

        public PressShaderKey(float f, float f2, float f3) {
            this.centerX = f;
            this.centerY = f2;
            this.baseRadius = f3;
        }

        public static PressShaderKey copy$default(PressShaderKey pressShaderKey, float f, float f2, float f3, int i, Object obj) {
            if ((i & StockUdfpsIconHook.MODE_PRESS) != 0) {
                f = pressShaderKey.centerX;
            }
            if ((i & StockUdfpsIconHook.MODE_SUCCESS) != 0) {
                f2 = pressShaderKey.centerY;
            }
            if ((i & 4) != 0) {
                f3 = pressShaderKey.baseRadius;
            }
            return pressShaderKey.copy(f, f2, f3);
        }

        public final float component1() {
            return this.centerX;
        }

        public final float component2() {
            return this.centerY;
        }

        public final float component3() {
            return this.baseRadius;
        }

        public final PressShaderKey copy(float f, float f2, float f3) {
            return new PressShaderKey(f, f2, f3);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PressShaderKey)) {
                return false;
            }
            PressShaderKey pressShaderKey = (PressShaderKey) obj;
            return Float.compare(this.centerX, pressShaderKey.centerX) == 0 && Float.compare(this.centerY, pressShaderKey.centerY) == 0 && Float.compare(this.baseRadius, pressShaderKey.baseRadius) == 0;
        }

        public final float getBaseRadius() {
            return this.baseRadius;
        }

        public final float getCenterX() {
            return this.centerX;
        }

        public final float getCenterY() {
            return this.centerY;
        }

        public int hashCode() {
            return Float.hashCode(this.baseRadius) + x30.c(Float.hashCode(this.centerX) * 31, 31, this.centerY);
        }

        public String toString() {
            return "PressShaderKey(centerX=" + this.centerX + ", centerY=" + this.centerY + ", baseRadius=" + this.baseRadius + ")";
        }
    }

    public static final class PressedIlluminationDrawable extends Drawable {
        private final float diameter;
        private int drawableAlpha;
        private final Paint paint;

        public PressedIlluminationDrawable(Context context) {
            context.getClass();
            this.diameter = context.getResources().getDisplayMetrics().density * 64.0f;
            Paint paint = new Paint(StockUdfpsIconHook.MODE_PRESS);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.pack(7.0f, 7.0f, 7.0f, 1.0f, ColorSpace.get(ColorSpace.Named.EXTENDED_SRGB)));
            this.paint = paint;
            this.drawableAlpha = 255;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.getClass();
            this.paint.setAlpha(this.drawableAlpha);
            canvas.drawCircle(getBounds().exactCenterX(), getBounds().exactCenterY(), Math.min(this.diameter, Math.min(getBounds().width(), getBounds().height())) / StockUdfpsIconHook.AOD_STROKE_WIDTH, this.paint);
        }

        @Override
        public int getAlpha() {
            return this.drawableAlpha;
        }

        @Override
        @cw
        public int getOpacity() {
            return -3;
        }

        @Override
        public void setAlpha(int i) {
            int iQ = nr.q(i, 0, 255);
            if (this.drawableAlpha == iQ) {
                return;
            }
            this.drawableAlpha = iQ;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.paint.setColorFilter(colorFilter);
            invalidateSelf();
        }
    }

    public static final class StockFingerprintDrawable extends Drawable {
        private float aodStrokeWidth;
        private int backgroundColor;
        private final Paint backgroundPaint;
        private float backgroundRadius;
        private float centerX;
        private float centerY;
        private final Context context;
        private int darkBackgroundColor;
        private int darkForegroundColor;
        private final Paint dashedPaint;
        private final float density;
        private final Path drawPath;
        private int drawableAlpha;
        private final int intrinsicSize;
        private int lightBackgroundColor;
        private int lightForegroundColor;
        private final Matrix matrix;
        private int normalForegroundColor;
        private float normalStrokeWidth;
        private float outlineProgress;
        private boolean pressed;
        private final Paint solidPaint;
        private final Path sourcePath;
        private float squareSize;
        private boolean targetDark;
        private boolean targetOutlineOnly;
        private ValueAnimator transitionAnimator;

        public StockFingerprintDrawable(Context context, boolean z, boolean z2) {
            context.getClass();
            this.context = context;
            float f = context.getResources().getDisplayMetrics().density;
            this.density = f;
            this.sourcePath = w41.a(StockUdfpsIconHook.FINGERPRINT_PATH_DATA);
            this.drawPath = new Path();
            this.matrix = new Matrix();
            int i = StockUdfpsIconHook.MODE_PRESS;
            Paint paint = new Paint(StockUdfpsIconHook.MODE_PRESS);
            paint.setStyle(Paint.Style.FILL);
            this.backgroundPaint = paint;
            Paint paint2 = new Paint(StockUdfpsIconHook.MODE_PRESS);
            Paint.Style style = Paint.Style.STROKE;
            paint2.setStyle(style);
            Paint.Cap cap = Paint.Cap.ROUND;
            paint2.setStrokeCap(cap);
            Paint.Join join = Paint.Join.ROUND;
            paint2.setStrokeJoin(join);
            this.solidPaint = paint2;
            Paint paint3 = new Paint(StockUdfpsIconHook.MODE_PRESS);
            paint3.setStyle(style);
            paint3.setStrokeCap(cap);
            paint3.setStrokeJoin(join);
            this.dashedPaint = paint3;
            int i2 = (int) (f * StockUdfpsIconHook.ICON_SIZE_DP);
            this.intrinsicSize = i2 >= StockUdfpsIconHook.MODE_PRESS ? i2 : i;
            this.lightBackgroundColor = bu0.b(context, StockUdfpsIconHook.LIGHT_BACKGROUND, StockUdfpsIconHook.LIGHT_BACKGROUND);
            this.darkBackgroundColor = bu0.b(context, StockUdfpsIconHook.DARK_BACKGROUND, StockUdfpsIconHook.DARK_BACKGROUND);
            this.lightForegroundColor = bu0.b(context, StockUdfpsIconHook.LIGHT_FOREGROUND, StockUdfpsIconHook.LIGHT_FOREGROUND);
            this.darkForegroundColor = bu0.b(context, StockUdfpsIconHook.DARK_FOREGROUND, StockUdfpsIconHook.DARK_FOREGROUND);
            this.targetOutlineOnly = z;
            this.targetDark = z2;
            this.outlineProgress = z ? 1.0f : 0.0f;
            this.backgroundColor = backgroundColor(z2);
            this.normalForegroundColor = foregroundColor(z2);
            this.drawableAlpha = 255;
        }

        private final int backgroundColor(boolean z) {
            return z ? this.darkBackgroundColor : this.lightBackgroundColor;
        }

        private final int foregroundColor(boolean z) {
            return z ? this.darkForegroundColor : this.lightForegroundColor;
        }

        private final int modulatedAlpha(int i, float f) {
            return nr.q((int) (nr.p(f, 0.0f, 1.0f) * ((Color.alpha(i) * this.drawableAlpha) / 255)), 0, 255);
        }

        private final boolean refreshPalette() {
            List list = bu0.a;
            int iB = bu0.b(this.context, StockUdfpsIconHook.LIGHT_BACKGROUND, StockUdfpsIconHook.LIGHT_BACKGROUND);
            int iB2 = bu0.b(this.context, StockUdfpsIconHook.DARK_BACKGROUND, StockUdfpsIconHook.DARK_BACKGROUND);
            int iB3 = bu0.b(this.context, StockUdfpsIconHook.LIGHT_FOREGROUND, StockUdfpsIconHook.LIGHT_FOREGROUND);
            int iB4 = bu0.b(this.context, StockUdfpsIconHook.DARK_FOREGROUND, StockUdfpsIconHook.DARK_FOREGROUND);
            boolean z = (this.lightBackgroundColor == iB && this.darkBackgroundColor == iB2 && this.lightForegroundColor == iB3 && this.darkForegroundColor == iB4) ? false : true;
            this.lightBackgroundColor = iB;
            this.darkBackgroundColor = iB2;
            this.lightForegroundColor = iB3;
            this.darkForegroundColor = iB4;
            return z;
        }

        public static final void transitionTo$lambda$0$0(StockFingerprintDrawable stockFingerprintDrawable, float f, float f2, int i, int i2, int i3, int i4, ValueAnimator valueAnimator) {
            float fFloatValue = ((Float) x30.i(valueAnimator)).floatValue();
            stockFingerprintDrawable.outlineProgress = StockUdfpsIconHook.Companion.lerp(f, f2, fFloatValue);
            stockFingerprintDrawable.backgroundColor = StockUdfpsIconHook.Companion.blendArgb(i, i2, fFloatValue);
            stockFingerprintDrawable.normalForegroundColor = StockUdfpsIconHook.Companion.blendArgb(i3, i4, fFloatValue);
            stockFingerprintDrawable.invalidateSelf();
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.getClass();
            if (this.pressed || this.squareSize <= 0.0f) {
                return;
            }
            float fP = nr.p(this.outlineProgress, 0.0f, 1.0f);
            float f = 1.0f - fP;
            if (f > 0.0f) {
                this.backgroundPaint.setColor(this.backgroundColor);
                this.backgroundPaint.setAlpha(modulatedAlpha(this.backgroundColor, f));
                canvas.drawCircle(this.centerX, this.centerY, this.backgroundRadius, this.backgroundPaint);
            }
            int iBlendArgb = StockUdfpsIconHook.Companion.blendArgb(this.normalForegroundColor, -1, fP);
            float fLerp = StockUdfpsIconHook.Companion.lerp(this.normalStrokeWidth, this.aodStrokeWidth, fP);
            this.solidPaint.setColor(iBlendArgb);
            this.solidPaint.setStrokeWidth(fLerp);
            this.solidPaint.setAlpha(modulatedAlpha(iBlendArgb, f));
            if (this.solidPaint.getAlpha() > 0) {
                canvas.drawPath(this.drawPath, this.solidPaint);
            }
            this.dashedPaint.setColor(iBlendArgb);
            this.dashedPaint.setStrokeWidth(fLerp);
            this.dashedPaint.setAlpha(modulatedAlpha(iBlendArgb, fP));
            if (this.dashedPaint.getAlpha() > 0) {
                canvas.drawPath(this.drawPath, this.dashedPaint);
            }
        }

        @Override
        public int getAlpha() {
            return this.drawableAlpha;
        }

        public final boolean getDark() {
            return this.targetDark;
        }

        @Override
        public int getIntrinsicHeight() {
            return this.intrinsicSize;
        }

        @Override
        public int getIntrinsicWidth() {
            return this.intrinsicSize;
        }

        @Override
        @cw
        public int getOpacity() {
            return -3;
        }

        public final boolean getOutlineOnly() {
            return this.targetOutlineOnly;
        }

        @Override
        public void onBoundsChange(Rect rect) {
            rect.getClass();
            super.onBoundsChange(rect);
            this.squareSize = Math.min(rect.width(), rect.height());
            this.centerX = rect.exactCenterX();
            this.centerY = rect.exactCenterY();
            this.backgroundRadius = Math.min(this.squareSize, this.density * 64.0f) / StockUdfpsIconHook.AOD_STROKE_WIDTH;
            float f = (this.squareSize / StockUdfpsIconHook.VIEWPORT_SIZE) * 0.5f;
            this.matrix.reset();
            this.matrix.setScale(f, f);
            float f2 = 36.0f * f;
            this.matrix.postTranslate(this.centerX - f2, this.centerY - f2);
            this.drawPath.reset();
            this.sourcePath.transform(this.matrix, this.drawPath);
            this.normalStrokeWidth = StockUdfpsIconHook.NORMAL_STROKE_WIDTH * f;
            this.aodStrokeWidth = StockUdfpsIconHook.AOD_STROKE_WIDTH * f;
            this.dashedPaint.setPathEffect(new DashPathEffect(new float[]{4.0f * f, f * StockUdfpsIconHook.AOD_DASH_GAP}, 0.0f));
        }

        @Override
        public void setAlpha(int i) {
            int iQ = nr.q(i, 0, 255);
            if (this.drawableAlpha == iQ) {
                return;
            }
            this.drawableAlpha = iQ;
            invalidateSelf();
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            this.backgroundPaint.setColorFilter(colorFilter);
            this.solidPaint.setColorFilter(colorFilter);
            this.dashedPaint.setColorFilter(colorFilter);
            invalidateSelf();
        }

        public final void setPressed(boolean z) {
            if (this.pressed == z) {
                return;
            }
            this.pressed = z;
            invalidateSelf();
        }

        public final void transitionTo(boolean z, boolean z2, boolean z3, boolean z4) {
            boolean z5 = z4 && refreshPalette();
            if (this.targetOutlineOnly == z && this.targetDark == z2 && !z5) {
                return;
            }
            this.targetOutlineOnly = z;
            this.targetDark = z2;
            float f = z ? 1.0f : 0.0f;
            int iBackgroundColor = backgroundColor(z2);
            int iForegroundColor = foregroundColor(z2);
            ValueAnimator valueAnimator = this.transitionAnimator;
            if (valueAnimator != null) {
                valueAnimator.cancel();
            }
            if (!z3 || getCallback() == null) {
                this.outlineProgress = f;
                this.backgroundColor = iBackgroundColor;
                this.normalForegroundColor = iForegroundColor;
                invalidateSelf();
                return;
            }
            float f2 = this.outlineProgress;
            int i = this.backgroundColor;
            int i2 = this.normalForegroundColor;
            ValueAnimator valueAnimatorOfFloat = ValueAnimator.ofFloat(0.0f, 1.0f);
            valueAnimatorOfFloat.setDuration(StockUdfpsIconHook.STATE_TRANSITION_DURATION_MS);
            valueAnimatorOfFloat.setInterpolator(StockUdfpsIconHook.STATE_TRANSITION_INTERPOLATOR);
            w wVar = new w();
            wVar.a = this;
            wVar.b = f2;
            wVar.c = f;
            wVar.d = i;
            wVar.e = iBackgroundColor;
            wVar.f = i2;
            wVar.g = iForegroundColor;
            VarHandle.storeStoreFence();
            valueAnimatorOfFloat.addUpdateListener(wVar);
            valueAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    animator.getClass();
                    if (this.this$0.transitionAnimator == animator) {
                        this.this$0.transitionAnimator = null;
                    }
                }
            });
            valueAnimatorOfFloat.start();
            this.transitionAnimator = valueAnimatorOfFloat;
        }
    }

    public static final class UdfpsGlowOverlay {
        private boolean added;
        private final Handler mainHandler;
        private final GlowOverlayView view;
        private final WindowManager windowManager;

        public UdfpsGlowOverlay(Context context) {
            context.getClass();
            this.mainHandler = new Handler(Looper.getMainLooper());
            this.windowManager = (WindowManager) context.getSystemService(WindowManager.class);
            this.view = new GlowOverlayView(context);
        }

        public final boolean ensureAdded() {
            Object objF;
            if (this.added) {
                return true;
            }
            int[] iArr = {StockUdfpsIconHook.WINDOW_TYPE_OPLUS_FINGERPRINT_OVERLAY, StockUdfpsIconHook.WINDOW_TYPE_MAGNIFICATION_OVERLAY, StockUdfpsIconHook.WINDOW_TYPE_DISPLAY_OVERLAY, StockUdfpsIconHook.WINDOW_TYPE_STATUS_BAR_SUB_PANEL, 2038};
            Throwable th = null;
            for (int i = 0; i < 5; i += StockUdfpsIconHook.MODE_PRESS) {
                int i2 = iArr[i];
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, i2, 16778008, -3);
                layoutParams.gravity = 8388659;
                layoutParams.layoutInDisplayCutoutMode = 3;
                layoutParams.setFitInsetsTypes(0);
                layoutParams.setTitle(StockUdfpsIconHook.OVERLAY_WINDOW_TITLE);
                try {
                    this.windowManager.addView(this.view, layoutParams);
                    objF = l22.a;
                } catch (Throwable th2) {
                    objF = dr.f(th2);
                }
                if (!(objF instanceof sc1)) {
                    this.added = true;
                    Companion.log$default(StockUdfpsIconHook.Companion, x30.k(i2, "glow overlay window added; type="), null, StockUdfpsIconHook.MODE_SUCCESS, null);
                    return true;
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    th = thA;
                }
            }
            StockUdfpsIconHook.Companion.log("unable to add glow overlay window", th);
            return false;
        }

        private final void onMain(final a80 a80Var) {
            if (ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
                a80Var.a();
            } else {
                this.mainHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        a80Var.a();
                    }
                });
            }
        }

        public final void hidePress() {
            if (!ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
                this.mainHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        if (this.this$0.added) {
                            this.this$0.view.hidePress();
                        }
                    }
                });
            } else if (this.added) {
                this.view.hidePress();
            }
        }

        public final void showPress(final GlowSpec glowSpec) {
            glowSpec.getClass();
            if (!ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
                this.mainHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        if (this.this$0.ensureAdded()) {
                            this.this$0.view.showPress(glowSpec);
                        }
                    }
                });
            } else if (ensureAdded()) {
                this.view.showPress(glowSpec);
            }
        }

        public final void showSuccess(final GlowSpec glowSpec) {
            glowSpec.getClass();
            if (!ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
                this.mainHandler.post(new Runnable() {
                    @Override
                    public final void run() {
                        if (this.this$0.ensureAdded()) {
                            this.this$0.view.showSuccess(glowSpec);
                        }
                    }
                });
            } else if (ensureAdded()) {
                this.view.showSuccess(glowSpec);
            }
        }
    }

    private final void applyVisualState(Object obj, final boolean z) {
        final StockUdfpsIconHook stockUdfpsIconHook;
        final ImageView imageViewFingerprintIcon;
        Object objF;
        Object objF2 = l22.a;
        this.lastUiMech = obj;
        final boolean zBooleanField = booleanField(obj, "isTouchDownNow");
        final boolean zIsScreenOffOrAod = isScreenOffOrAod(obj);
        if (isAodExitPending(obj) || (imageViewFingerprintIcon = fingerprintIcon(obj)) == null) {
            stockUdfpsIconHook = this;
        } else {
            Handler handler = imageViewFingerprintIcon.getHandler();
            if (handler == null || !ph0.i(Looper.myLooper(), handler.getLooper())) {
                stockUdfpsIconHook = this;
                imageViewFingerprintIcon.post(new Runnable() {
                    @Override
                    public final void run() {
                        Object objF3;
                        StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                        try {
                            Context context = imageViewFingerprintIcon.getContext();
                            context.getClass();
                            boolean zIsDark = stockUdfpsIconHook2.isDark(context);
                            if (imageViewFingerprintIcon.getAnimation() != null) {
                                imageViewFingerprintIcon.clearAnimation();
                            }
                            imageViewFingerprintIcon.animate().cancel();
                            if (imageViewFingerprintIcon.getScaleX() != 1.0f) {
                                imageViewFingerprintIcon.setScaleX(1.0f);
                            }
                            if (imageViewFingerprintIcon.getScaleY() != 1.0f) {
                                imageViewFingerprintIcon.setScaleY(1.0f);
                            }
                            if (imageViewFingerprintIcon.getBackground() != null) {
                                imageViewFingerprintIcon.setBackground(null);
                            }
                            if (imageViewFingerprintIcon.getImageTintList() != null) {
                                imageViewFingerprintIcon.setImageTintList(null);
                            }
                            if (imageViewFingerprintIcon.getColorFilter() != null) {
                                imageViewFingerprintIcon.clearColorFilter();
                            }
                            ImageView.ScaleType scaleType = imageViewFingerprintIcon.getScaleType();
                            ImageView.ScaleType scaleType2 = ImageView.ScaleType.CENTER;
                            if (scaleType != scaleType2) {
                                imageViewFingerprintIcon.setScaleType(scaleType2);
                            }
                            Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageViewFingerprintIcon, "coe_stock_udfps_drawable");
                            StockUdfpsIconHook.StockFingerprintDrawable stockFingerprintDrawable = additionalInstanceField instanceof StockUdfpsIconHook.StockFingerprintDrawable ? (StockUdfpsIconHook.StockFingerprintDrawable) additionalInstanceField : null;
                            if (stockFingerprintDrawable == null) {
                                Context context2 = imageViewFingerprintIcon.getContext();
                                context2.getClass();
                                stockFingerprintDrawable = new StockUdfpsIconHook.StockFingerprintDrawable(context2, zIsScreenOffOrAod, zIsDark);
                                XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, "coe_stock_udfps_drawable", stockFingerprintDrawable);
                            }
                            if (imageViewFingerprintIcon.getDrawable() != stockFingerprintDrawable) {
                                imageViewFingerprintIcon.setImageDrawable(stockFingerprintDrawable);
                            }
                            stockUdfpsIconHook2.cancelCustomAodExit(imageViewFingerprintIcon);
                            if (stockFingerprintDrawable.getAlpha() != 255) {
                                stockFingerprintDrawable.setAlpha(255);
                            }
                            boolean z2 = zIsScreenOffOrAod;
                            ImageView imageView = imageViewFingerprintIcon;
                            if (z2) {
                                Object additionalInstanceField2 = XposedHelpers.getAdditionalInstanceField(imageView, "coe_stock_udfps_alpha_normalized");
                                Boolean bool = Boolean.FALSE;
                                if (!ph0.i(additionalInstanceField2, bool)) {
                                    XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, "coe_stock_udfps_alpha_normalized", bool);
                                }
                            } else {
                                stockUdfpsIconHook2.normalizeLockscreenIconAlpha(imageView);
                            }
                            boolean z3 = true;
                            stockFingerprintDrawable.transitionTo(zIsScreenOffOrAod, zIsDark, (z && stockFingerprintDrawable.getOutlineOnly() == zIsScreenOffOrAod) ? false : true, z);
                            if (!zBooleanField || !stockUdfpsIconHook2.useHdrPressEffect) {
                                z3 = false;
                            }
                            stockFingerprintDrawable.setPressed(z3);
                            objF3 = l22.a;
                        } catch (Throwable th) {
                            objF3 = dr.f(th);
                        }
                        Throwable thA = tc1.a(objF3);
                        if (thA != null) {
                            StockUdfpsIconHook.Companion.log("fingerprint icon view update failed", thA);
                        }
                    }
                });
            } else {
                try {
                    Context context = imageViewFingerprintIcon.getContext();
                    context.getClass();
                    boolean zIsDark = isDark(context);
                    if (imageViewFingerprintIcon.getAnimation() != null) {
                        imageViewFingerprintIcon.clearAnimation();
                    }
                    imageViewFingerprintIcon.animate().cancel();
                    if (imageViewFingerprintIcon.getScaleX() != 1.0f) {
                        imageViewFingerprintIcon.setScaleX(1.0f);
                    }
                    if (imageViewFingerprintIcon.getScaleY() != 1.0f) {
                        imageViewFingerprintIcon.setScaleY(1.0f);
                    }
                    if (imageViewFingerprintIcon.getBackground() != null) {
                        imageViewFingerprintIcon.setBackground(null);
                    }
                    if (imageViewFingerprintIcon.getImageTintList() != null) {
                        imageViewFingerprintIcon.setImageTintList(null);
                    }
                    if (imageViewFingerprintIcon.getColorFilter() != null) {
                        imageViewFingerprintIcon.clearColorFilter();
                    }
                    ImageView.ScaleType scaleType = imageViewFingerprintIcon.getScaleType();
                    ImageView.ScaleType scaleType2 = ImageView.ScaleType.CENTER;
                    if (scaleType != scaleType2) {
                        imageViewFingerprintIcon.setScaleType(scaleType2);
                    }
                    Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageViewFingerprintIcon, FP_DRAWABLE_FIELD);
                    StockFingerprintDrawable stockFingerprintDrawable = additionalInstanceField instanceof StockFingerprintDrawable ? (StockFingerprintDrawable) additionalInstanceField : null;
                    if (stockFingerprintDrawable == null) {
                        Context context2 = imageViewFingerprintIcon.getContext();
                        context2.getClass();
                        stockFingerprintDrawable = new StockFingerprintDrawable(context2, zIsScreenOffOrAod, zIsDark);
                        XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, FP_DRAWABLE_FIELD, stockFingerprintDrawable);
                    }
                    if (imageViewFingerprintIcon.getDrawable() != stockFingerprintDrawable) {
                        imageViewFingerprintIcon.setImageDrawable(stockFingerprintDrawable);
                    }
                    cancelCustomAodExit(imageViewFingerprintIcon);
                    if (stockFingerprintDrawable.getAlpha() != 255) {
                        stockFingerprintDrawable.setAlpha(255);
                    }
                    if (zIsScreenOffOrAod) {
                        Object additionalInstanceField2 = XposedHelpers.getAdditionalInstanceField(imageViewFingerprintIcon, ALPHA_NORMALIZED_FIELD);
                        Boolean bool = Boolean.FALSE;
                        if (!ph0.i(additionalInstanceField2, bool)) {
                            XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, ALPHA_NORMALIZED_FIELD, bool);
                        }
                    } else {
                        normalizeLockscreenIconAlpha(imageViewFingerprintIcon);
                    }
                    boolean z2 = true;
                    stockFingerprintDrawable.transitionTo(zIsScreenOffOrAod, zIsDark, (z && stockFingerprintDrawable.getOutlineOnly() == zIsScreenOffOrAod) ? false : MODE_PRESS, z);
                    if (!zBooleanField || !this.useHdrPressEffect) {
                        z2 = false;
                    }
                    stockFingerprintDrawable.setPressed(z2);
                    objF = objF2;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    Companion.log("fingerprint icon view update failed", thA);
                }
                stockUdfpsIconHook = this;
            }
        }
        stockUdfpsIconHook.updatePressGlow(obj, zBooleanField);
        final ImageView imageViewPressedIcon = stockUdfpsIconHook.pressedIcon(obj);
        if (imageViewPressedIcon != null) {
            Handler handler2 = imageViewPressedIcon.getHandler();
            if (handler2 == null || !ph0.i(Looper.myLooper(), handler2.getLooper())) {
                imageViewPressedIcon.post(new Runnable() {
                    @Override
                    public final void run() {
                        Object objF3;
                        StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                        try {
                            stockUdfpsIconHook2.configurePressedIcon(imageViewPressedIcon);
                            if (stockUdfpsIconHook2.useHdrPressEffect) {
                                stockUdfpsIconHook2.updatePressedHdr(imageViewPressedIcon, zBooleanField);
                            }
                            objF3 = l22.a;
                        } catch (Throwable th2) {
                            objF3 = dr.f(th2);
                        }
                        Throwable thA2 = tc1.a(objF3);
                        if (thA2 != null) {
                            StockUdfpsIconHook.Companion.log("pressed icon view update failed", thA2);
                        }
                    }
                });
                return;
            }
            try {
                stockUdfpsIconHook.configurePressedIcon(imageViewPressedIcon);
                if (stockUdfpsIconHook.useHdrPressEffect) {
                    stockUdfpsIconHook.updatePressedHdr(imageViewPressedIcon, zBooleanField);
                }
            } catch (Throwable th2) {
                objF2 = dr.f(th2);
            }
            Throwable thA2 = tc1.a(objF2);
            if (thA2 != null) {
                Companion.log("pressed icon view update failed", thA2);
            }
        }
    }

    public static void applyVisualState$default(StockUdfpsIconHook stockUdfpsIconHook, Object obj, boolean z, int i, Object obj2) {
        if ((i & MODE_SUCCESS) != 0) {
            z = false;
        }
        stockUdfpsIconHook.applyVisualState(obj, z);
    }

    private final boolean booleanField(Object obj, String str) {
        Object objF;
        try {
            objF = Boolean.valueOf(XposedHelpers.getBooleanField(obj, str));
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Object obj2 = Boolean.FALSE;
        if (objF instanceof sc1) {
            objF = obj2;
        }
        return ((Boolean) objF).booleanValue();
    }

    public final Object callbackUiMech(Object obj) {
        Object objF;
        try {
            objF = XposedHelpers.getObjectField(obj, "this$0");
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            return null;
        }
        return objF;
    }

    public final void cancelCustomAodExit(ImageView imageView) {
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageView, AOD_EXIT_ANIMATOR_FIELD);
        ValueAnimator valueAnimator = additionalInstanceField instanceof ValueAnimator ? (ValueAnimator) additionalInstanceField : null;
        if (valueAnimator == null) {
            return;
        }
        XposedHelpers.setAdditionalInstanceField(imageView, AOD_EXIT_ANIMATOR_FIELD, (Object) null);
        valueAnimator.cancel();
    }

    public final void configureHdrLayout(ImageView imageView) {
        l4 l4Var = new l4(10);
        l4Var.e = this;
        l4Var.f = imageView;
        VarHandle.storeStoreFence();
        imageView.post(l4Var);
    }

    public static final void configureHdrLayout$lambda$0(StockUdfpsIconHook stockUdfpsIconHook, ImageView imageView) {
        Object objF;
        try {
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            WindowManager.LayoutParams layoutParams2 = layoutParams instanceof WindowManager.LayoutParams ? (WindowManager.LayoutParams) layoutParams : null;
            if (layoutParams2 == null) {
                return;
            }
            layoutParams2.setColorMode(MODE_SUCCESS);
            layoutParams2.setDesiredHdrHeadroom(stockUdfpsIconHook.maxHdrHeadroom(imageView));
            ((WindowManager) imageView.getContext().getSystemService(WindowManager.class)).updateViewLayout(imageView, layoutParams2);
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            Companion.log("HDR window configuration failed", thA);
        }
    }

    public final void configurePressedIcon(ImageView imageView) {
        if (imageView.getAnimation() != null) {
            imageView.clearAnimation();
        }
        imageView.animate().cancel();
        if (imageView.getScaleX() != 1.0f) {
            imageView.setScaleX(1.0f);
        }
        if (imageView.getScaleY() != 1.0f) {
            imageView.setScaleY(1.0f);
        }
        if (imageView.getImageTintList() != null) {
            imageView.setImageTintList(null);
        }
        if (imageView.getColorFilter() != null) {
            imageView.clearColorFilter();
        }
        if (imageView.getDrawable() != null) {
            imageView.setImageDrawable(null);
        }
        if (!this.useHdrPressEffect) {
            if (imageView.getBackground() != null) {
                imageView.setBackground(null);
            }
        } else {
            if (!(imageView.getBackground() instanceof PressedIlluminationDrawable)) {
                Context context = imageView.getContext();
                context.getClass();
                imageView.setBackground(new PressedIlluminationDrawable(context));
            }
            prepareHdrWindow(imageView);
        }
    }

    public final ImageView fingerprintIcon(Object obj) {
        Object objF;
        Object objF2;
        try {
            Object objectField = XposedHelpers.getObjectField(obj, "fpIcon");
            objF = objectField instanceof ImageView ? (ImageView) objectField : null;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            objF = null;
        }
        ImageView imageView = (ImageView) objF;
        if (imageView != null) {
            return imageView;
        }
        try {
            Object objCallMethod = XposedHelpers.callMethod(obj, "getFingerprintIcon", new Object[0]);
            objF2 = objCallMethod instanceof ImageView ? (ImageView) objCallMethod : null;
        } catch (Throwable th2) {
            objF2 = dr.f(th2);
        }
        return (ImageView) (objF2 instanceof sc1 ? null : objF2);
    }

    public static final l22 handleLoadPackage$lambda$1(Class cls, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(cls, "updateFpIconAlpha", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                final ImageView imageViewFingerprintIcon;
                Object objF;
                methodHookParam.getClass();
                methodHookParam.setResult((Object) null);
                Object obj = methodHookParam.thisObject;
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                obj.getClass();
                if (stockUdfpsIconHook2.isScreenOffOrAod(obj) || (imageViewFingerprintIcon = this.this$0.fingerprintIcon(obj)) == null) {
                    return;
                }
                final StockUdfpsIconHook stockUdfpsIconHook3 = this.this$0;
                if (imageViewFingerprintIcon.getAlpha() == 1.0f && imageViewFingerprintIcon.getImageAlpha() == 255 && ph0.i(XposedHelpers.getAdditionalInstanceField(imageViewFingerprintIcon, "coe_stock_udfps_alpha_normalized"), Boolean.TRUE)) {
                    return;
                }
                Handler handler = imageViewFingerprintIcon.getHandler();
                if (handler == null || !ph0.i(Looper.myLooper(), handler.getLooper())) {
                    imageViewFingerprintIcon.post(new Runnable() {
                        @Override
                        public final void run() {
                            Object objF2;
                            try {
                                stockUdfpsIconHook3.normalizeLockscreenIconAlpha(imageViewFingerprintIcon);
                                objF2 = l22.a;
                            } catch (Throwable th) {
                                objF2 = dr.f(th);
                            }
                            Throwable thA = tc1.a(objF2);
                            if (thA != null) {
                                StockUdfpsIconHook.Companion.log("fingerprint alpha normalization failed", thA);
                            }
                        }
                    });
                    return;
                }
                try {
                    stockUdfpsIconHook3.normalizeLockscreenIconAlpha(imageViewFingerprintIcon);
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    StockUdfpsIconHook.Companion.log("fingerprint alpha normalization failed", thA);
                }
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$2(final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(Application.class, "onConfigurationChanged", new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.getClass();
                Object obj = this.this$0.lastUiMech;
                if (obj == null) {
                    return;
                }
                this.this$0.requestVisualState(obj, true);
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$3(Class cls, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllConstructors(cls, new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                try {
                    Object obj = methodHookParam.thisObject;
                    ImageView imageView = obj instanceof ImageView ? (ImageView) obj : null;
                    if (imageView == null) {
                        return;
                    }
                    stockUdfpsIconHook2.configurePressedIcon(imageView);
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    StockUdfpsIconHook.Companion.log("pressed constructor callback failed", thA);
                }
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$4(Class cls) {
        XposedBridge.hookAllMethods(cls, "checkHasPressedAnimation", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.getClass();
                methodHookParam.setResult(Boolean.FALSE);
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$5(Class cls) {
        XposedBridge.hookAllMethods(cls, "getScalePressedAnim", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.getClass();
                methodHookParam.setResult(Float.valueOf(1.0f));
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$6$0(Class cls, final String str, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(cls, str, new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                String str2 = str;
                try {
                    Object obj = methodHookParam.thisObject;
                    obj.getClass();
                    stockUdfpsIconHook2.requestVisualState(obj, StockUdfpsIconHook.FORCE_REFRESH_METHODS.contains(str2));
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                String str3 = str;
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    StockUdfpsIconHook.Companion.log(str3 + " callback failed", thA);
                }
            }
        });
        return l22.a;
    }

    public static final l22 handleLoadPackage$lambda$7$0(Class cls, final String str, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(cls, "run", new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = stockUdfpsIconHook;
                try {
                    Object objectField = XposedHelpers.getObjectField(methodHookParam.thisObject, "this$0");
                    if (objectField == null) {
                        return;
                    }
                    StockUdfpsIconHook.requestVisualState$default(stockUdfpsIconHook2, objectField, false, 2, null);
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                String str2 = str;
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    StockUdfpsIconHook.Companion.log(str2 + ".run callback failed", thA);
                }
            }

            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                Object objF2;
                methodHookParam.getClass();
                if (ph0.i(str, "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$1")) {
                    try {
                        objF = Integer.valueOf(XposedHelpers.getIntField(methodHookParam.thisObject, "$r8$classId"));
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    if (objF instanceof sc1) {
                        objF = null;
                    }
                    Integer num = (Integer) objF;
                    if (num == null || num.intValue() != 22) {
                        return;
                    }
                    try {
                        objF2 = XposedHelpers.getObjectField(methodHookParam.thisObject, "this$0");
                    } catch (Throwable th2) {
                        objF2 = dr.f(th2);
                    }
                    if (objF2 instanceof sc1) {
                        objF2 = null;
                    }
                    if (objF2 != null && stockUdfpsIconHook.isAodExitPending(objF2) && stockUdfpsIconHook.startCustomAodExit(objF2)) {
                        methodHookParam.setResult((Object) null);
                    }
                }
            }
        });
        return l22.a;
    }

    private final void installAuthRippleHooks(Class<?> cls, Class<?> cls2) {
        u21 u21Var = new u21(4);
        u21Var.e = cls;
        VarHandle.storeStoreFence();
        installPart("suppress stock unlock ripple", u21Var);
        u21 u21Var2 = new u21(5);
        u21Var2.e = cls2;
        VarHandle.storeStoreFence();
        installPart("suppress stock dynamic dwell ripple", u21Var2);
    }

    public static final l22 installAuthRippleHooks$lambda$0(Class cls) {
        XposedBridge.hookAllMethods(cls, "showUnlockedRipple", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.getClass();
                methodHookParam.setResult((Object) null);
            }
        });
        return l22.a;
    }

    public static final l22 installAuthRippleHooks$lambda$1(Class cls) {
        XposedBridge.hookAllMethods(cls, "startDwellRipple", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                methodHookParam.getClass();
                methodHookParam.setResult((Object) null);
            }
        });
        return l22.a;
    }

    private final void installAuthenticationCallbacks(Class<?> cls) {
        kr1 kr1Var = new kr1(0);
        kr1Var.e = cls;
        kr1Var.f = this;
        VarHandle.storeStoreFence();
        installPart("fingerprint authenticated callback", kr1Var);
        for (k31 k31Var : e72.M(new k31("onBiometricAuthFailed", 0), new k31("onBiometricError", Integer.valueOf(MODE_SUCCESS)))) {
            String str = (String) k31Var.d;
            int iIntValue = ((Number) k31Var.e).intValue();
            ob0 ob0Var = new ob0(MODE_PRESS);
            ob0Var.g = cls;
            ob0Var.e = str;
            ob0Var.h = this;
            ob0Var.f = iIntValue;
            VarHandle.storeStoreFence();
            installPart(str + " dwell cleanup", ob0Var);
        }
    }

    public static final l22 installAuthenticationCallbacks$lambda$0(Class cls, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(cls, "onBiometricAuthenticated", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                Object[] objArr = methodHookParam.args;
                objArr.getClass();
                if (stockUdfpsIconHook2.isFingerprintSource(fd.t0(1, objArr)) && this.this$0.enableSuccessRipple) {
                    StockUdfpsIconHook stockUdfpsIconHook3 = this.this$0;
                    try {
                        Object obj = methodHookParam.thisObject;
                        obj.getClass();
                        Object objCallbackUiMech = stockUdfpsIconHook3.callbackUiMech(obj);
                        if (objCallbackUiMech == null) {
                            return;
                        }
                        stockUdfpsIconHook3.showSuccessRipple(objCallbackUiMech);
                        objF = l22.a;
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        StockUdfpsIconHook.Companion.log("fingerprint success ripple failed", thA);
                    }
                }
            }
        });
        return l22.a;
    }

    public static final l22 installAuthenticationCallbacks$lambda$1$0(Class cls, final String str, final StockUdfpsIconHook stockUdfpsIconHook, final int i) {
        XposedBridge.hookAllMethods(cls, str, new XC_MethodHook() {
            public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                Object[] objArr = methodHookParam.args;
                objArr.getClass();
                if (stockUdfpsIconHook2.isFingerprintSource(fd.t0(i, objArr))) {
                    StockUdfpsIconHook stockUdfpsIconHook3 = this.this$0;
                    try {
                        Object obj = methodHookParam.thisObject;
                        obj.getClass();
                        Object objCallbackUiMech = stockUdfpsIconHook3.callbackUiMech(obj);
                        if (objCallbackUiMech != null) {
                            stockUdfpsIconHook3.stopPressGlow(objCallbackUiMech);
                            objF = l22.a;
                        } else {
                            objF = null;
                        }
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    String str2 = str;
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        StockUdfpsIconHook.Companion.log(str2 + " dwell cleanup failed", thA);
                    }
                }
            }
        });
        return l22.a;
    }

    private final void installBiometricUnlockFallback(Class<?> cls) {
        kr1 kr1Var = new kr1(MODE_PRESS);
        kr1Var.e = cls;
        kr1Var.f = this;
        VarHandle.storeStoreFence();
        installPart("OPlus biometric unlock success fallback", kr1Var);
    }

    public static final l22 installBiometricUnlockFallback$lambda$0(Class cls, final StockUdfpsIconHook stockUdfpsIconHook) {
        XposedBridge.hookAllMethods(cls, "onBiometricAuthenticated", new XC_MethodHook() {
            public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                Object objF;
                methodHookParam.getClass();
                StockUdfpsIconHook stockUdfpsIconHook2 = this.this$0;
                Object[] objArr = methodHookParam.args;
                objArr.getClass();
                if (stockUdfpsIconHook2.isFingerprintSource(fd.t0(1, objArr)) && this.this$0.enableSuccessRipple) {
                    StockUdfpsIconHook stockUdfpsIconHook3 = this.this$0;
                    try {
                        Object obj = stockUdfpsIconHook3.lastUiMech;
                        if (obj != null) {
                            stockUdfpsIconHook3.showSuccessRipple(obj);
                            objF = l22.a;
                        } else {
                            objF = null;
                        }
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        StockUdfpsIconHook.Companion.log("biometric unlock success fallback failed", thA);
                    }
                }
            }
        });
        return l22.a;
    }

    private final void installPart(String str, a80 a80Var) {
        Object objF;
        try {
            objF = a80Var.a();
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            Companion.log(str + " install failed", thA);
        }
    }

    public final boolean isAodExitPending(Object obj) {
        Object objF;
        if (!isScreenOffOrAod(obj)) {
            return false;
        }
        try {
            objF = Boolean.valueOf(XposedHelpers.getObjectField(obj, "realHideRunnable") != null);
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Object obj2 = Boolean.FALSE;
        if (objF instanceof sc1) {
            objF = obj2;
        }
        return ((Boolean) objF).booleanValue();
    }

    public final boolean isDark(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public final boolean isFingerprintSource(Object obj) {
        String string;
        return (obj == null || (string = obj.toString()) == null || string.equalsIgnoreCase("FINGERPRINT") != MODE_PRESS) ? false : true;
    }

    public final boolean isScreenOffOrAod(Object obj) {
        return booleanField(obj, "onDozeState") || booleanField(obj, "onDreamingStart") || booleanField(obj, "screenTurnedOff");
    }

    private final float maxHdrHeadroom(ImageView imageView) {
        Object objF;
        try {
            Display display = imageView.getDisplay();
            objF = Float.valueOf(display != null ? display.getHighestHdrSdrRatio() : 1.0f);
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Object objValueOf = Float.valueOf(1.0f);
        if (objF instanceof sc1) {
            objF = objValueOf;
        }
        float fFloatValue = ((Number) objF).floatValue();
        Float fValueOf = Float.valueOf(fFloatValue);
        if (Math.abs(fFloatValue) > Float.MAX_VALUE || fFloatValue <= 1.0f) {
            fValueOf = null;
        }
        if (fValueOf == null) {
            return 7.0f;
        }
        float fFloatValue2 = fValueOf.floatValue();
        if (fFloatValue2 > 7.0f) {
            return 7.0f;
        }
        return fFloatValue2;
    }

    public final void normalizeLockscreenIconAlpha(ImageView imageView) {
        boolean z = imageView.getAlpha() == 1.0f;
        if (!z) {
            imageView.setAlpha(1.0f);
        }
        if (imageView.getImageAlpha() != 255) {
            imageView.setImageAlpha(255);
        }
        if (z && ph0.i(XposedHelpers.getAdditionalInstanceField(imageView, ALPHA_NORMALIZED_FIELD), Boolean.TRUE)) {
            return;
        }
        try {
            XposedHelpers.callMethod(imageView, "setBrightnessAlpha", new Object[]{Float.valueOf(1.0f)});
        } catch (Throwable th) {
            dr.f(th);
        }
        XposedHelpers.setAdditionalInstanceField(imageView, ALPHA_NORMALIZED_FIELD, Boolean.TRUE);
    }

    private final void prepareHdrWindow(ImageView imageView) {
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageView, HDR_ATTACH_FIELD);
        Boolean bool = Boolean.TRUE;
        if (ph0.i(additionalInstanceField, bool)) {
            return;
        }
        XposedHelpers.setAdditionalInstanceField(imageView, HDR_ATTACH_FIELD, bool);
        imageView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                view.getClass();
                StockUdfpsIconHook stockUdfpsIconHook = StockUdfpsIconHook.this;
                ImageView imageView2 = view instanceof ImageView ? (ImageView) view : null;
                if (imageView2 == null) {
                    return;
                }
                stockUdfpsIconHook.configureHdrLayout(imageView2);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                view.getClass();
            }
        });
        if (imageView.isAttachedToWindow()) {
            configureHdrLayout(imageView);
        }
    }

    private final ImageView pressedIcon(Object obj) {
        Object objF;
        Object objF2;
        try {
            Object objectField = XposedHelpers.getObjectField(obj, "pressedIcon");
            objF = objectField instanceof ImageView ? (ImageView) objectField : null;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            objF = null;
        }
        ImageView imageView = (ImageView) objF;
        if (imageView != null) {
            return imageView;
        }
        try {
            Object objCallMethod = XposedHelpers.callMethod(obj, "getFingerprintPressedIcon", new Object[0]);
            objF2 = objCallMethod instanceof ImageView ? (ImageView) objCallMethod : null;
        } catch (Throwable th2) {
            objF2 = dr.f(th2);
        }
        return (ImageView) (objF2 instanceof sc1 ? null : objF2);
    }

    public final void requestVisualState(Object obj, boolean z) {
        boolean z2;
        synchronized (obj) {
            if (z) {
                try {
                    XposedHelpers.setAdditionalInstanceField(obj, VISUAL_REFRESH_FORCE_FIELD, Boolean.TRUE);
                } catch (Throwable th) {
                    throw th;
                }
            }
            Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(obj, VISUAL_REFRESH_PENDING_FIELD);
            Boolean bool = Boolean.TRUE;
            if (ph0.i(additionalInstanceField, bool)) {
                z2 = false;
            } else {
                XposedHelpers.setAdditionalInstanceField(obj, VISUAL_REFRESH_PENDING_FIELD, bool);
                z2 = MODE_PRESS;
            }
        }
        if (z2) {
            l4 l4Var = new l4(11);
            l4Var.e = obj;
            l4Var.f = this;
            VarHandle.storeStoreFence();
            ImageView imageViewFingerprintIcon = fingerprintIcon(obj);
            if (imageViewFingerprintIcon == null || imageViewFingerprintIcon.isAttachedToWindow() != MODE_PRESS) {
                this.mainHandler.post(l4Var);
            } else {
                imageViewFingerprintIcon.postOnAnimation(l4Var);
            }
        }
    }

    public static void requestVisualState$default(StockUdfpsIconHook stockUdfpsIconHook, Object obj, boolean z, int i, Object obj2) {
        if ((i & MODE_SUCCESS) != 0) {
            z = false;
        }
        stockUdfpsIconHook.requestVisualState(obj, z);
    }

    public static final void requestVisualState$lambda$1(Object obj, StockUdfpsIconHook stockUdfpsIconHook) {
        boolean zI;
        Object objF;
        synchronized (obj) {
            zI = ph0.i(XposedHelpers.getAdditionalInstanceField(obj, VISUAL_REFRESH_FORCE_FIELD), Boolean.TRUE);
            Boolean bool = Boolean.FALSE;
            XposedHelpers.setAdditionalInstanceField(obj, VISUAL_REFRESH_PENDING_FIELD, bool);
            XposedHelpers.setAdditionalInstanceField(obj, VISUAL_REFRESH_FORCE_FIELD, bool);
        }
        try {
            stockUdfpsIconHook.applyVisualState(obj, zI);
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            Companion.log("coalesced visual refresh failed", thA);
        }
    }

    public final long resolveAodExitDuration(Object obj) {
        Object objF;
        try {
            Object objCallMethod = XposedHelpers.callMethod(XposedHelpers.getStaticObjectField(XposedHelpers.findClass(FINGERPRINT_UTILS_CLASS, obj.getClass().getClassLoader()), "INSTANCE"), "getFadeOutAnimTime", new Object[0]);
            objCallMethod.getClass();
            objF = Long.valueOf(((Number) objCallMethod).longValue());
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            objF = 500L;
        }
        return nr.r(((Number) objF).longValue(), 100L, 2000L);
    }

    private final void runOnViewThread(View view, final a80 a80Var) {
        Handler handler = view.getHandler();
        if (handler == null || !ph0.i(Looper.myLooper(), handler.getLooper())) {
            view.post(new Runnable() {
                @Override
                public final void run() {
                    a80Var.a();
                }
            });
        } else {
            a80Var.a();
        }
    }

    public final void showSuccessRipple(Object obj) {
        withGlowOverlay(obj, true, new t(0));
    }

    public static final l22 showSuccessRipple$lambda$0(UdfpsGlowOverlay udfpsGlowOverlay, GlowSpec glowSpec) {
        udfpsGlowOverlay.getClass();
        glowSpec.getClass();
        udfpsGlowOverlay.showSuccess(glowSpec);
        return l22.a;
    }

    public final boolean startCustomAodExit(final Object obj) {
        Object objF;
        final ImageView imageViewFingerprintIcon = fingerprintIcon(obj);
        if (imageViewFingerprintIcon != null) {
            Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageViewFingerprintIcon, FP_DRAWABLE_FIELD);
            final StockFingerprintDrawable stockFingerprintDrawable = additionalInstanceField instanceof StockFingerprintDrawable ? (StockFingerprintDrawable) additionalInstanceField : null;
            if (stockFingerprintDrawable != null) {
                Handler handler = imageViewFingerprintIcon.getHandler();
                if (handler == null || !ph0.i(Looper.myLooper(), handler.getLooper())) {
                    imageViewFingerprintIcon.post(new Runnable() {
                        @Override
                        public final void run() {
                            Object objF2;
                            StockUdfpsIconHook stockUdfpsIconHook = this.this$0;
                            try {
                                stockUdfpsIconHook.cancelCustomAodExit(imageViewFingerprintIcon);
                                Drawable drawable = imageViewFingerprintIcon.getDrawable();
                                StockUdfpsIconHook.StockFingerprintDrawable stockFingerprintDrawable2 = stockFingerprintDrawable;
                                if (drawable != stockFingerprintDrawable2) {
                                    imageViewFingerprintIcon.setImageDrawable(stockFingerprintDrawable2);
                                }
                                ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(stockFingerprintDrawable.getAlpha(), 0);
                                valueAnimatorOfInt.setDuration(stockUdfpsIconHook.resolveAodExitDuration(obj));
                                valueAnimatorOfInt.setInterpolator(new LinearInterpolator());
                                valueAnimatorOfInt.addUpdateListener(new StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$1(stockFingerprintDrawable));
                                valueAnimatorOfInt.addListener(new StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$2(imageViewFingerprintIcon));
                                XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, "coe_stock_udfps_aod_exit_animator", valueAnimatorOfInt);
                                valueAnimatorOfInt.start();
                                objF2 = l22.a;
                            } catch (Throwable th) {
                                objF2 = dr.f(th);
                            }
                            Throwable thA = tc1.a(objF2);
                            if (thA != null) {
                                StockUdfpsIconHook.Companion.log("custom AOD fingerprint exit failed", thA);
                            }
                        }
                    });
                    return true;
                }
                try {
                    cancelCustomAodExit(imageViewFingerprintIcon);
                    if (imageViewFingerprintIcon.getDrawable() != stockFingerprintDrawable) {
                        imageViewFingerprintIcon.setImageDrawable(stockFingerprintDrawable);
                    }
                    ValueAnimator valueAnimatorOfInt = ValueAnimator.ofInt(stockFingerprintDrawable.getAlpha(), 0);
                    valueAnimatorOfInt.setDuration(resolveAodExitDuration(obj));
                    valueAnimatorOfInt.setInterpolator(new LinearInterpolator());
                    valueAnimatorOfInt.addUpdateListener(new StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$1(stockFingerprintDrawable));
                    valueAnimatorOfInt.addListener(new StockUdfpsIconHook$startCustomAodExit$1$1$animator$1$2(imageViewFingerprintIcon));
                    XposedHelpers.setAdditionalInstanceField(imageViewFingerprintIcon, AOD_EXIT_ANIMATOR_FIELD, valueAnimatorOfInt);
                    valueAnimatorOfInt.start();
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA == null) {
                    return true;
                }
                Companion.log("custom AOD fingerprint exit failed", thA);
                return true;
            }
        }
        return false;
    }

    public final void stopPressGlow(Object obj) {
        XposedHelpers.setAdditionalInstanceField(obj, RIPPLE_PRESSED_FIELD, Boolean.FALSE);
        UdfpsGlowOverlay udfpsGlowOverlay = this.glowOverlay;
        if (udfpsGlowOverlay != null) {
            udfpsGlowOverlay.hidePress();
        }
    }

    private final void updatePressGlow(Object obj, boolean z) {
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(obj, RIPPLE_PRESSED_FIELD);
        if (ph0.i(additionalInstanceField instanceof Boolean ? (Boolean) additionalInstanceField : null, Boolean.valueOf(z))) {
            return;
        }
        XposedHelpers.setAdditionalInstanceField(obj, RIPPLE_PRESSED_FIELD, Boolean.valueOf(z));
        if (this.useHdrPressEffect) {
            return;
        }
        if (z) {
            withGlowOverlay(obj, false, new t(MODE_PRESS));
            return;
        }
        UdfpsGlowOverlay udfpsGlowOverlay = this.glowOverlay;
        if (udfpsGlowOverlay != null) {
            udfpsGlowOverlay.hidePress();
        }
    }

    public static final l22 updatePressGlow$lambda$0(UdfpsGlowOverlay udfpsGlowOverlay, GlowSpec glowSpec) {
        udfpsGlowOverlay.getClass();
        glowSpec.getClass();
        udfpsGlowOverlay.showPress(glowSpec);
        return l22.a;
    }

    public final void updatePressedHdr(ImageView imageView, boolean z) {
        Object objF;
        if (!imageView.isAttachedToWindow()) {
            return;
        }
        float fMaxHdrHeadroom = z ? maxHdrHeadroom(imageView) : 1.0f;
        try {
            Object objCallMethod = XposedHelpers.callMethod(imageView, "getViewRootImpl", new Object[0]);
            if (objCallMethod == null) {
                return;
            }
            Object objCallMethod2 = XposedHelpers.callMethod(objCallMethod, "getSurfaceControl", new Object[0]);
            SurfaceControl surfaceControl = objCallMethod2 instanceof SurfaceControl ? (SurfaceControl) objCallMethod2 : null;
            if (surfaceControl != null && surfaceControl.isValid()) {
                SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
                try {
                    transaction.setDesiredHdrHeadroom(surfaceControl, fMaxHdrHeadroom);
                    transaction.setExtendedRangeBrightness(surfaceControl, z ? 7.0f : 1.0f, fMaxHdrHeadroom);
                    transaction.apply();
                    transaction.close();
                    objF = l22.a;
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        Companion.log("pressed HDR surface update failed", thA);
                    }
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        us0.o(transaction, th);
                        throw th2;
                    }
                }
            }
        } catch (Throwable th3) {
            objF = dr.f(th3);
        }
    }

    private final void withGlowOverlay(Object obj, boolean z, p80 p80Var) {
        ImageView imageViewFingerprintIcon = fingerprintIcon(obj);
        lr1 lr1Var = new lr1();
        lr1Var.d = this;
        lr1Var.e = imageViewFingerprintIcon;
        lr1Var.f = z;
        lr1Var.g = p80Var;
        VarHandle.storeStoreFence();
        if (imageViewFingerprintIcon == null) {
            lr1Var.a();
            return;
        }
        defpackage.p pVar = new defpackage.p(17);
        pVar.e = lr1Var;
        VarHandle.storeStoreFence();
        imageViewFingerprintIcon.post(pVar);
    }

    public static final l22 withGlowOverlay$lambda$0(StockUdfpsIconHook stockUdfpsIconHook, ImageView imageView, boolean z, p80 p80Var) {
        Object objF;
        Context context;
        GlowSpec glowSpec;
        l22 l22Var = l22.a;
        GlowGeometry glowGeometry = null;
        if (imageView != null) {
            try {
                ImageView imageView2 = (imageView.getWidth() <= 0 || imageView.getHeight() <= 0) ? null : imageView;
                if (imageView2 != null) {
                    imageView2.getLocationOnScreen(stockUdfpsIconHook.glowLocation);
                    stockUdfpsIconHook.lastGlowContext = imageView2.getContext();
                    glowGeometry = new GlowGeometry((imageView2.getWidth() / AOD_STROKE_WIDTH) + stockUdfpsIconHook.glowLocation[0], (imageView2.getHeight() / AOD_STROKE_WIDTH) + stockUdfpsIconHook.glowLocation[MODE_PRESS], (imageView2.getResources().getDisplayMetrics().density * 64.0f) / AOD_STROKE_WIDTH);
                    stockUdfpsIconHook.lastGlowGeometry = glowGeometry;
                }
            } catch (Throwable th) {
                objF = dr.f(th);
            }
        }
        if ((glowGeometry != null || (glowGeometry = stockUdfpsIconHook.lastGlowGeometry) != null) && ((imageView != null && (context = imageView.getContext()) != null) || (context = stockUdfpsIconHook.lastGlowContext) != null)) {
            if (z) {
                boolean zIsDark = stockUdfpsIconHook.isDark(context);
                int iB = zIsDark ? -1 : bu0.b(context, SUCCESS_RIPPLE_LIGHT_INTRO_TONE, SUCCESS_RIPPLE_LIGHT_INTRO_TONE);
                String str = zIsDark ? SUCCESS_RIPPLE_DARK_TONE : SUCCESS_RIPPLE_LIGHT_TONE;
                glowSpec = new GlowSpec(glowGeometry.getCenterX(), glowGeometry.getCenterY(), glowGeometry.getBaseRadius(), iB, bu0.b(context, str, str), zIsDark ? 1.0f : SUCCESS_RIPPLE_LIGHT_ALPHA_SCALE);
            } else {
                GlowGeometry glowGeometry2 = glowGeometry;
                glowSpec = new GlowSpec(glowGeometry2.getCenterX(), glowGeometry2.getCenterY(), glowGeometry2.getBaseRadius(), 0, 0, 1.0f);
            }
            u uVar = new u();
            uVar.d = stockUdfpsIconHook;
            uVar.e = context;
            uVar.f = p80Var;
            uVar.g = glowSpec;
            VarHandle.storeStoreFence();
            if (ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
                uVar.a();
            } else {
                Handler handler = stockUdfpsIconHook.mainHandler;
                defpackage.p pVar = new defpackage.p(16);
                pVar.e = uVar;
                VarHandle.storeStoreFence();
                handler.post(pVar);
            }
        }
        objF = l22Var;
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            Companion.log("glow overlay update failed", thA);
        }
        return l22Var;
    }

    public static final l22 withGlowOverlay$lambda$0$0$2(StockUdfpsIconHook stockUdfpsIconHook, Context context, p80 p80Var, GlowSpec glowSpec) {
        Object objF;
        l22 l22Var = l22.a;
        try {
            UdfpsGlowOverlay udfpsGlowOverlay = stockUdfpsIconHook.glowOverlay;
            if (udfpsGlowOverlay == null) {
                udfpsGlowOverlay = new UdfpsGlowOverlay(context);
                stockUdfpsIconHook.glowOverlay = udfpsGlowOverlay;
            }
            p80Var.f(udfpsGlowOverlay, glowSpec);
            objF = l22Var;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            Companion.log("glow overlay dispatch failed", thA);
        }
        return l22Var;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        loadPackageParam.getClass();
        if (ph0.i(loadPackageParam.packageName, SYSTEM_UI_PACKAGE) || ph0.i(loadPackageParam.processName, SYSTEM_UI_PACKAGE)) {
            ct ctVarLoad = ConfigStore.INSTANCE.load(x30.h(ConfigStore.MODULE_PACKAGE, ConfigStore.PREFS_NAME));
            if (((Boolean) ctVarLoad.a(aq.i)).booleanValue()) {
                this.useHdrPressEffect = ((Boolean) ctVarLoad.a(aq.k)).booleanValue();
                this.enableSuccessRipple = ((Boolean) ctVarLoad.a(aq.j)).booleanValue();
                Class clsFindClassIfExists = XposedHelpers.findClassIfExists(PRESSED_ICON_CLASS, loadPackageParam.classLoader);
                Class clsFindClassIfExists2 = XposedHelpers.findClassIfExists(UI_MECH_CLASS, loadPackageParam.classLoader);
                Class<?> clsFindClassIfExists3 = XposedHelpers.findClassIfExists(AUTH_RIPPLE_CONTROLLER_CLASS, loadPackageParam.classLoader);
                Class<?> clsFindClassIfExists4 = XposedHelpers.findClassIfExists(AUTH_RIPPLE_VIEW_CLASS, loadPackageParam.classLoader);
                Class<?> clsFindClassIfExists5 = XposedHelpers.findClassIfExists(UPDATE_MONITOR_CALLBACK_CLASS, loadPackageParam.classLoader);
                Class<?> clsFindClassIfExists6 = XposedHelpers.findClassIfExists(OPLUS_BIOMETRIC_UNLOCK_CLASS, loadPackageParam.classLoader);
                int i = MODE_SUCCESS;
                Object[] objArr = 0;
                int i2 = MODE_PRESS;
                if (clsFindClassIfExists == null || clsFindClassIfExists2 == null) {
                    Companion.log$default(Companion, "unsupported SystemUI: pressedIcon=" + (clsFindClassIfExists != null ? MODE_PRESS : false) + ", uiMech=" + (clsFindClassIfExists2 != null ? MODE_PRESS : false), null, MODE_SUCCESS, null);
                    return;
                }
                if (clsFindClassIfExists3 == null || clsFindClassIfExists4 == null) {
                    Companion.log$default(Companion, "stock auth ripple unavailable: controller=" + (clsFindClassIfExists3 != null ? MODE_PRESS : false) + ", view=" + (clsFindClassIfExists4 != null ? MODE_PRESS : false), null, MODE_SUCCESS, null);
                } else {
                    installAuthRippleHooks(clsFindClassIfExists3, clsFindClassIfExists4);
                }
                if (clsFindClassIfExists5 != null) {
                    installAuthenticationCallbacks(clsFindClassIfExists5);
                } else {
                    Companion.log$default(Companion, "authentication callback unavailable; success ripple disabled", null, MODE_SUCCESS, null);
                }
                if (clsFindClassIfExists6 != null) {
                    installBiometricUnlockFallback(clsFindClassIfExists6);
                }
                kr1 kr1Var = new kr1(i);
                kr1Var.e = clsFindClassIfExists2;
                kr1Var.f = this;
                VarHandle.storeStoreFence();
                installPart("suppress vendor fingerprint alpha spring", kr1Var);
                e7 e7Var = new e7(AOD_FADE_OUT_RUNNABLE_ID);
                e7Var.e = this;
                VarHandle.storeStoreFence();
                installPart("system palette configuration refresh", e7Var);
                kr1 kr1Var2 = new kr1(3);
                kr1Var2.e = clsFindClassIfExists;
                kr1Var2.f = this;
                VarHandle.storeStoreFence();
                installPart("pressed icon constructors", kr1Var2);
                u21 u21Var = new u21(6);
                u21Var.e = clsFindClassIfExists2;
                VarHandle.storeStoreFence();
                installPart("static press animation decision", u21Var);
                u21 u21Var2 = new u21(7);
                u21Var2.e = clsFindClassIfExists2;
                VarHandle.storeStoreFence();
                installPart("pressed scale", u21Var2);
                for (String str : STATE_REFRESH_METHODS) {
                    mr1 mr1Var = new mr1(objArr == true ? 1 : 0);
                    mr1Var.e = clsFindClassIfExists2;
                    mr1Var.f = str;
                    mr1Var.g = this;
                    VarHandle.storeStoreFence();
                    installPart(str, mr1Var);
                }
                for (String str2 : ASYNC_VISUAL_RUNNABLE_CLASSES) {
                    Class clsFindClassIfExists7 = XposedHelpers.findClassIfExists(str2, loadPackageParam.classLoader);
                    if (clsFindClassIfExists7 != null) {
                        mr1 mr1Var2 = new mr1(i2);
                        mr1Var2.e = clsFindClassIfExists7;
                        mr1Var2.f = str2;
                        mr1Var2.g = this;
                        VarHandle.storeStoreFence();
                        installPart(str2 + ".run", mr1Var2);
                    }
                }
                Companion.log$default(Companion, x30.n("installed; press=", this.useHdrPressEffect ? "hdr" : "white dwell ripple", ", vendor HBM/highlight control remains active"), null, MODE_SUCCESS, null);
            }
        }
    }

    public static final class Companion {
        public Companion(zt ztVar) {
            this();
        }

        public final int blendArgb(int i, int i2, float f) {
            float fP = nr.p(f, 0.0f, 1.0f);
            return Color.argb((int) lerp(Color.alpha(i), Color.alpha(i2), fP), (int) lerp(Color.red(i), Color.red(i2), fP), (int) lerp(Color.green(i), Color.green(i2), fP), (int) lerp(Color.blue(i), Color.blue(i2), fP));
        }

        public final float lerp(float f, float f2, float f3) {
            return x30.b(f2, f, f3, f);
        }

        public final void log(String str, Throwable th) {
            if (th == null) {
                sb0 sb0Var = vb0.a;
                vb0.c(StockUdfpsIconHook.TAG, str);
            } else {
                sb0 sb0Var2 = vb0.a;
                vb0.d(StockUdfpsIconHook.TAG, tr1.P(str, ' '), str, th);
            }
        }

        public static void log$default(Companion companion, String str, Throwable th, int i, Object obj) {
            if ((i & StockUdfpsIconHook.MODE_SUCCESS) != 0) {
                th = null;
            }
            companion.log(str, th);
        }

        private Companion() {
        }
    }
}
