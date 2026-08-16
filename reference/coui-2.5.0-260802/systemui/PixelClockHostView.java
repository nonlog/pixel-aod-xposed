package one.dot.couiexpressive.hooks.systemui;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Property;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewTreeObserver;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import defpackage.as1;
import defpackage.dr;
import defpackage.fs;
import defpackage.h30;
import defpackage.jg0;
import defpackage.kg0;
import defpackage.l22;
import defpackage.mm;
import defpackage.nr;
import defpackage.ph0;
import defpackage.qc;
import defpackage.sb0;
import defpackage.sc1;
import defpackage.tc1;
import defpackage.u51;
import defpackage.uk;
import defpackage.us0;
import defpackage.v51;
import defpackage.vb0;
import defpackage.w51;
import defpackage.x30;
import defpackage.x51;
import defpackage.zt;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Calendar;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import one.dot.couiexpressive.FontUtils;

public final class PixelClockHostView extends FrameLayout {

    @Deprecated
    public static final float AOD_BATTERY_BOTTOM_MARGIN_DP = 64.0f;

    @Deprecated
    public static final float AOD_CLOCK_Y_OFFSET_DP = -24.0f;

    @Deprecated
    public static final float AOD_COMPACT_INFO_CENTER_RATIO = 0.75f;

    @Deprecated
    public static final float AOD_COMPACT_INFO_X_OFFSET_DP = -34.0f;

    @Deprecated
    public static final float AOD_COMPACT_INFO_Y_OFFSET_DP = 33.0f;

    @Deprecated
    public static final float AOD_COMPACT_INFO_Y_RATIO = 0.118f;

    @Deprecated
    public static final float AOD_COMPACT_ONE_LEFT_TRIM_RATIO = 0.06f;

    @Deprecated
    public static final float AOD_COMPACT_ONE_RIGHT_TRIM_RATIO = 0.09f;

    @Deprecated
    public static final float AOD_COMPACT_TEXT_RATIO = 0.17f;

    @Deprecated
    public static final float AOD_COMPACT_TIME_CENTER_RATIO = 0.25f;

    @Deprecated
    public static final float AOD_COMPACT_TIME_X_OFFSET_DP = 10.0f;

    @Deprecated
    public static final float AOD_COMPACT_TIME_Y_OFFSET_DP = 25.0f;

    @Deprecated
    public static final float AOD_COMPACT_TIME_Y_RATIO = 0.105f;

    @Deprecated
    public static final float AOD_COMPACT_TRACKING_RATIO = -0.09f;

    @Deprecated
    public static final String AOD_COMPACT_VARIATION = "'wght' 180, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0";

    @Deprecated
    public static final float AOD_COMPACT_ZERO_SIDE_EXPAND_RATIO = 0.05f;

    @Deprecated
    public static final float AOD_CONTENT_X_DP = 32.0f;

    @Deprecated
    public static final float AOD_LARGE_INFO_Y_MULTIPLIER = 1.9f;

    @Deprecated
    public static final float AOD_LARGE_SCALE = 0.9f;

    @Deprecated
    public static final float AOD_LARGE_TRACKING_RATIO = -0.06f;

    @Deprecated
    public static final String AOD_LARGE_VARIATION = "'wght' 100, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0";

    @Deprecated
    public static final long AOD_LIVE_FADE_IN_DURATION = 200;

    @Deprecated
    public static final long AOD_LIVE_FADE_OUT_DURATION = 150;

    @Deprecated
    public static final float AOD_MEDIA_NOTIFICATION_GAP_DP = 28.0f;

    @Deprecated
    public static final float AOD_MEDIA_Y_OFFSET_DP = 0.0f;

    @Deprecated
    public static final float AOD_MEDIA_Y_RATIO = 0.255f;

    @Deprecated
    public static final float AOD_MONET_BLEND_RATIO = 0.5f;

    @Deprecated
    public static final int AOD_NOTIFICATION_ICON_GAP_DP = 15;

    @Deprecated
    public static final int AOD_NOTIFICATION_ICON_SIZE_DP = 18;

    @Deprecated
    public static final float AOD_NOTIFICATION_ONLY_Y_OFFSET_DP = 0.0f;

    @Deprecated
    public static final float AOD_NOTIFICATION_ONLY_Y_RATIO = 0.255f;

    @Deprecated
    public static final float BATTERY_BURN_IN_X_SCALE = 0.75f;

    @Deprecated
    public static final float BATTERY_BURN_IN_Y_SCALE = 0.5f;

    @Deprecated
    public static final long BURN_IN_X_PERIOD_MINUTES = 83;

    @Deprecated
    public static final long BURN_IN_Y_PERIOD_MINUTES = 521;

    @Deprecated
    public static final String COLON = ":";

    @Deprecated
    public static final float COLON_ALPHA_DURATION_FRACTION = 0.22f;

    @Deprecated
    public static final float COLON_ENTER_DELAY_FRACTION = 0.52f;

    @Deprecated
    public static final float COLON_TRACKING_SCALE = 0.55f;

    @Deprecated
    public static final float COMPACT_INFO_X_OFFSET_DP = -36.0f;

    @Deprecated
    public static final float COMPACT_TIME_X_OFFSET_DP = 8.0f;

    @Deprecated
    public static final String COMPACT_VARIATION = "'wght' 500, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 96, 'slnt' 0";

    @Deprecated
    public static final boolean ENABLE_AOD_BURN_IN = true;

    @Deprecated
    public static final float IMMERSED_INFO_Y_RATIO = 0.09f;

    @Deprecated
    public static final float IMMERSED_ONE_LEFT_TRIM_RATIO = 0.06f;

    @Deprecated
    public static final float IMMERSED_ONE_RIGHT_TRIM_RATIO = 0.09f;

    @Deprecated
    public static final float IMMERSED_TEXT_RATIO = 0.155f;

    @Deprecated
    public static final float IMMERSED_TIME_Y_RATIO = 0.072f;

    @Deprecated
    public static final float IMMERSED_TRACKING_RATIO = -0.09f;

    @Deprecated
    public static final float IMMERSED_Y_OFFSET_DP = 30.0f;

    @Deprecated
    public static final float IMMERSED_ZERO_SIDE_EXPAND_RATIO = 0.05f;

    @Deprecated
    public static final long INFORMATION_REFRESH_INTERVAL = 1000;

    @Deprecated
    public static final int INFORMATION_SHADOW_COLOR = 1711276032;

    @Deprecated
    public static final float INFORMATION_SHADOW_DY_DP = 0.5f;

    @Deprecated
    public static final float INFORMATION_SHADOW_RADIUS_DP = 1.5f;

    @Deprecated
    public static final float LARGE_INFO_Y_MULTIPLIER = 1.9f;

    @Deprecated
    public static final float LARGE_LOCKSCREEN_Y_OFFSET_DP = -10.0f;

    @Deprecated
    public static final float LARGE_TEXT_RATIO = 0.47f;

    @Deprecated
    public static final float LARGE_TOP_RATIO = 0.215f;

    @Deprecated
    public static final float LARGE_TRACKING_RATIO = -0.07f;

    @Deprecated
    public static final String LARGE_VARIATION = "'wght' 450, 'wdth' 100, 'ROND' 100, 'GRAD' 0, 'opsz' 144, 'slnt' 0";

    @Deprecated
    public static final float LEFT_COLUMN_CENTER_RATIO = 0.25f;

    @Deprecated
    public static final int MAX_AOD_NOTIFICATION_ICONS = 7;

    @Deprecated
    public static final int MEDIA_ICON_ALPHA_THRESHOLD = 8;

    @Deprecated
    public static final float MEDIA_ICON_FILL_RATIO = 0.98f;

    @Deprecated
    public static final int MEDIA_ICON_RASTER_SIZE = 96;

    @Deprecated
    public static final float PANORAMIC_AOD_BURN_IN_X_DP = 5.0f;

    @Deprecated
    public static final float PANORAMIC_AOD_BURN_IN_Y_DP = 4.0f;

    @Deprecated
    public static final float PARTIAL_AOD_BURN_IN_X_DP = 8.0f;

    @Deprecated
    public static final float PARTIAL_AOD_BURN_IN_Y_DP = 10.0f;

    @Deprecated
    public static final float RIGHT_COLUMN_CENTER_RATIO = 0.75f;

    @Deprecated
    public static final float SMALL_INFO_Y_OFFSET_DP = 33.0f;

    @Deprecated
    public static final float SMALL_INFO_Y_RATIO = 0.118f;

    @Deprecated
    public static final float SMALL_ONE_LEFT_TRIM_RATIO = 0.06f;

    @Deprecated
    public static final float SMALL_ONE_RIGHT_TRIM_RATIO = 0.09f;

    @Deprecated
    public static final float SMALL_TEXT_RATIO = 0.17f;

    @Deprecated
    public static final float SMALL_TIME_Y_OFFSET_DP = 25.0f;

    @Deprecated
    public static final float SMALL_TIME_Y_RATIO = 0.105f;

    @Deprecated
    public static final float SMALL_TRACKING_RATIO = -0.09f;

    @Deprecated
    public static final float SMALL_ZERO_SIDE_EXPAND_RATIO = 0.05f;

    @Deprecated
    public static final String TABULAR_NUMBERS_FEATURE = "'tnum'";

    @Deprecated
    public static final long TRANSITION_DURATION = 550;
    private final boolean aodBatteryEnabled;
    private final float aodBatteryYOffset;
    private final GlyphSet aodCompactSet;
    private AodContent aodContent;
    private long aodEntryGeneration;
    private boolean aodEntryInProgress;
    private final GlyphSet aodLargeSet;
    private int aodMonetColor;
    private final IdentityHashMap<View, AppliedGlyphTarget> appliedGlyphTargets;
    private final IdentityHashMap<View, AppliedViewTarget> appliedInformationTargets;
    private final PixelClockHostView$batteryReceiver$1 batteryReceiver;
    private boolean batteryReceiverRegistered;
    private final TextView batteryView;
    private float burnInX;
    private float burnInY;
    private final Calendar calendar;
    private int clockBaseWidth;
    private ObjectAnimator colonAlphaAnimator;
    private final GlyphSet compactSet;
    private final LinearLayout dateGroup;
    private final TextView dateView;
    private AodContent deferredAodContent;
    private AodContent deferredLiveAodContent;
    private boolean dozing;
    private Runnable finishAodEntryRunnable;
    private final GlyphSet[] glyphSets;
    private Boolean informationShadowApplied;
    private boolean initialPresentationApplied;
    private final GlyphSet largeSet;
    private Boolean lastBatteryCharging;
    private int lastBatteryLevel;
    private String lastBatteryLocaleTag;
    private Information lastInformation;
    private Drawable.ConstantState lastInformationIconState;
    private long lastInformationRefresh;
    private long lastMinute;
    private long liveAodContentGeneration;
    private long liveAodCrossfadeGeneration;
    private boolean liveAodCrossfadeInProgress;
    private boolean liveAodRetargetPending;
    private final ImageView mediaAppIconView;
    private final TextView mediaArtistView;
    private final LinearLayout mediaGroup;
    private final LinearLayout mediaSubtitleRow;
    private final TextView mediaTitleView;
    private int monetColor;
    private final RomTextAnimatorRuntime morphRuntime;
    private final PathInterpolator motionInterpolator;
    private final LinearLayout notificationIconRow;
    private boolean partialAod;
    private ViewTreeObserver.OnPreDrawListener pendingLivePreDrawListener;
    private boolean pendingTargetApply;
    private boolean pendingTargetApplyAnimated;
    private boolean retainedAodMediaLayout;
    private Scene scene;
    private final Runnable targetApplyRunnable;
    private String timeText;
    private final LinearLayout weatherGroup;
    private final ImageView weatherIconView;
    private final TextView weatherView;
    private final TextView weekView;
    private static final Companion Companion = new Companion(null);
    public static final int $stable = 8;

    public static final class AppliedGlyphTarget {
        private final float alpha;
        private final GlyphTarget target;

        public AppliedGlyphTarget(GlyphTarget glyphTarget, float f) {
            glyphTarget.getClass();
            this.target = glyphTarget;
            this.alpha = f;
        }

        public static AppliedGlyphTarget copy$default(AppliedGlyphTarget appliedGlyphTarget, GlyphTarget glyphTarget, float f, int i, Object obj) {
            if ((i & 1) != 0) {
                glyphTarget = appliedGlyphTarget.target;
            }
            if ((i & 2) != 0) {
                f = appliedGlyphTarget.alpha;
            }
            return appliedGlyphTarget.copy(glyphTarget, f);
        }

        public final GlyphTarget component1() {
            return this.target;
        }

        public final float component2() {
            return this.alpha;
        }

        public final AppliedGlyphTarget copy(GlyphTarget glyphTarget, float f) {
            glyphTarget.getClass();
            return new AppliedGlyphTarget(glyphTarget, f);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AppliedGlyphTarget)) {
                return false;
            }
            AppliedGlyphTarget appliedGlyphTarget = (AppliedGlyphTarget) obj;
            return ph0.i(this.target, appliedGlyphTarget.target) && Float.compare(this.alpha, appliedGlyphTarget.alpha) == 0;
        }

        public final float getAlpha() {
            return this.alpha;
        }

        public final GlyphTarget getTarget() {
            return this.target;
        }

        public int hashCode() {
            return Float.hashCode(this.alpha) + (this.target.hashCode() * 31);
        }

        public String toString() {
            return "AppliedGlyphTarget(target=" + this.target + ", alpha=" + this.alpha + ")";
        }
    }

    public static final class AppliedViewTarget {
        private final float alpha;
        private final float x;
        private final float y;

        public AppliedViewTarget(float f, float f2, float f3) {
            this.x = f;
            this.y = f2;
            this.alpha = f3;
        }

        public static AppliedViewTarget copy$default(AppliedViewTarget appliedViewTarget, float f, float f2, float f3, int i, Object obj) {
            if ((i & 1) != 0) {
                f = appliedViewTarget.x;
            }
            if ((i & 2) != 0) {
                f2 = appliedViewTarget.y;
            }
            if ((i & 4) != 0) {
                f3 = appliedViewTarget.alpha;
            }
            return appliedViewTarget.copy(f, f2, f3);
        }

        public final float component1() {
            return this.x;
        }

        public final float component2() {
            return this.y;
        }

        public final float component3() {
            return this.alpha;
        }

        public final AppliedViewTarget copy(float f, float f2, float f3) {
            return new AppliedViewTarget(f, f2, f3);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AppliedViewTarget)) {
                return false;
            }
            AppliedViewTarget appliedViewTarget = (AppliedViewTarget) obj;
            return Float.compare(this.x, appliedViewTarget.x) == 0 && Float.compare(this.y, appliedViewTarget.y) == 0 && Float.compare(this.alpha, appliedViewTarget.alpha) == 0;
        }

        public final float getAlpha() {
            return this.alpha;
        }

        public final float getX() {
            return this.x;
        }

        public final float getY() {
            return this.y;
        }

        public int hashCode() {
            return Float.hashCode(this.alpha) + x30.c(Float.hashCode(this.x) * 31, 31, this.y);
        }

        public String toString() {
            return "AppliedViewTarget(x=" + this.x + ", y=" + this.y + ", alpha=" + this.alpha + ")";
        }
    }

    public static final class ClockTargets {
        private final GlyphTarget colon;
        private final GlyphTarget[] digits;

        public ClockTargets(GlyphTarget[] glyphTargetArr, GlyphTarget glyphTarget) {
            glyphTargetArr.getClass();
            glyphTarget.getClass();
            this.digits = glyphTargetArr;
            this.colon = glyphTarget;
        }

        public static ClockTargets copy$default(ClockTargets clockTargets, GlyphTarget[] glyphTargetArr, GlyphTarget glyphTarget, int i, Object obj) {
            if ((i & 1) != 0) {
                glyphTargetArr = clockTargets.digits;
            }
            if ((i & 2) != 0) {
                glyphTarget = clockTargets.colon;
            }
            return clockTargets.copy(glyphTargetArr, glyphTarget);
        }

        public final GlyphTarget[] component1() {
            return this.digits;
        }

        public final GlyphTarget component2() {
            return this.colon;
        }

        public final ClockTargets copy(GlyphTarget[] glyphTargetArr, GlyphTarget glyphTarget) {
            glyphTargetArr.getClass();
            glyphTarget.getClass();
            return new ClockTargets(glyphTargetArr, glyphTarget);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ClockTargets)) {
                return false;
            }
            ClockTargets clockTargets = (ClockTargets) obj;
            return ph0.i(this.digits, clockTargets.digits) && ph0.i(this.colon, clockTargets.colon);
        }

        public final GlyphTarget getColon() {
            return this.colon;
        }

        public final GlyphTarget[] getDigits() {
            return this.digits;
        }

        public int hashCode() {
            return (Arrays.hashCode(this.digits) * 31) + this.colon.hashCode();
        }

        public String toString() {
            return "ClockTargets(digits=" + Arrays.toString(this.digits) + ", colon=" + this.colon + ")";
        }
    }

    public static final class GlyphSet {
        private final TextView colon;
        private final TextView[] digits;

        public GlyphSet(TextView[] textViewArr, TextView textView) {
            textViewArr.getClass();
            textView.getClass();
            this.digits = textViewArr;
            this.colon = textView;
        }

        public static GlyphSet copy$default(GlyphSet glyphSet, TextView[] textViewArr, TextView textView, int i, Object obj) {
            if ((i & 1) != 0) {
                textViewArr = glyphSet.digits;
            }
            if ((i & 2) != 0) {
                textView = glyphSet.colon;
            }
            return glyphSet.copy(textViewArr, textView);
        }

        public final TextView[] component1() {
            return this.digits;
        }

        public final TextView component2() {
            return this.colon;
        }

        public final GlyphSet copy(TextView[] textViewArr, TextView textView) {
            textViewArr.getClass();
            textView.getClass();
            return new GlyphSet(textViewArr, textView);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GlyphSet)) {
                return false;
            }
            GlyphSet glyphSet = (GlyphSet) obj;
            return ph0.i(this.digits, glyphSet.digits) && ph0.i(this.colon, glyphSet.colon);
        }

        public final TextView getColon() {
            return this.colon;
        }

        public final TextView[] getDigits() {
            return this.digits;
        }

        public int hashCode() {
            return this.colon.hashCode() + (Arrays.hashCode(this.digits) * 31);
        }

        public String toString() {
            return "GlyphSet(digits=" + Arrays.toString(this.digits) + ", colon=" + this.colon + ")";
        }
    }

    public static final class GlyphTarget {
        private final float alpha;
        private final float scale;
        private final float x;
        private final float y;

        public GlyphTarget(float f, float f2, float f3, float f4) {
            this.x = f;
            this.y = f2;
            this.scale = f3;
            this.alpha = f4;
        }

        public static GlyphTarget copy$default(GlyphTarget glyphTarget, float f, float f2, float f3, float f4, int i, Object obj) {
            if ((i & 1) != 0) {
                f = glyphTarget.x;
            }
            if ((i & 2) != 0) {
                f2 = glyphTarget.y;
            }
            if ((i & 4) != 0) {
                f3 = glyphTarget.scale;
            }
            if ((i & 8) != 0) {
                f4 = glyphTarget.alpha;
            }
            return glyphTarget.copy(f, f2, f3, f4);
        }

        public final float component1() {
            return this.x;
        }

        public final float component2() {
            return this.y;
        }

        public final float component3() {
            return this.scale;
        }

        public final float component4() {
            return this.alpha;
        }

        public final GlyphTarget copy(float f, float f2, float f3, float f4) {
            return new GlyphTarget(f, f2, f3, f4);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof GlyphTarget)) {
                return false;
            }
            GlyphTarget glyphTarget = (GlyphTarget) obj;
            return Float.compare(this.x, glyphTarget.x) == 0 && Float.compare(this.y, glyphTarget.y) == 0 && Float.compare(this.scale, glyphTarget.scale) == 0 && Float.compare(this.alpha, glyphTarget.alpha) == 0;
        }

        public final float getAlpha() {
            return this.alpha;
        }

        public final float getScale() {
            return this.scale;
        }

        public final float getX() {
            return this.x;
        }

        public final float getY() {
            return this.y;
        }

        public int hashCode() {
            return Float.hashCode(this.alpha) + x30.c(x30.c(Float.hashCode(this.x) * 31, 31, this.y), 31, this.scale);
        }

        public String toString() {
            return "GlyphTarget(x=" + this.x + ", y=" + this.y + ", scale=" + this.scale + ", alpha=" + this.alpha + ")";
        }
    }

    public static final class NotificationIcon {
        public static final int $stable = 8;
        private final Drawable.ConstantState drawableState;
        private final boolean preserveOriginalColors;
        private final ColorStateList tint;

        public NotificationIcon(Drawable.ConstantState constantState, ColorStateList colorStateList, boolean z) {
            constantState.getClass();
            this.drawableState = constantState;
            this.tint = colorStateList;
            this.preserveOriginalColors = z;
        }

        public static NotificationIcon copy$default(NotificationIcon notificationIcon, Drawable.ConstantState constantState, ColorStateList colorStateList, boolean z, int i, Object obj) {
            if ((i & 1) != 0) {
                constantState = notificationIcon.drawableState;
            }
            if ((i & 2) != 0) {
                colorStateList = notificationIcon.tint;
            }
            if ((i & 4) != 0) {
                z = notificationIcon.preserveOriginalColors;
            }
            return notificationIcon.copy(constantState, colorStateList, z);
        }

        public final Drawable.ConstantState component1() {
            return this.drawableState;
        }

        public final ColorStateList component2() {
            return this.tint;
        }

        public final boolean component3() {
            return this.preserveOriginalColors;
        }

        public final NotificationIcon copy(Drawable.ConstantState constantState, ColorStateList colorStateList, boolean z) {
            constantState.getClass();
            return new NotificationIcon(constantState, colorStateList, z);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NotificationIcon)) {
                return false;
            }
            NotificationIcon notificationIcon = (NotificationIcon) obj;
            return ph0.i(this.drawableState, notificationIcon.drawableState) && ph0.i(this.tint, notificationIcon.tint) && this.preserveOriginalColors == notificationIcon.preserveOriginalColors;
        }

        public final Drawable.ConstantState getDrawableState() {
            return this.drawableState;
        }

        public final boolean getPreserveOriginalColors() {
            return this.preserveOriginalColors;
        }

        public final ColorStateList getTint() {
            return this.tint;
        }

        public int hashCode() {
            int iHashCode = this.drawableState.hashCode() * 31;
            ColorStateList colorStateList = this.tint;
            return Boolean.hashCode(this.preserveOriginalColors) + ((iHashCode + (colorStateList == null ? 0 : colorStateList.hashCode())) * 31);
        }

        public String toString() {
            return "NotificationIcon(drawableState=" + this.drawableState + ", tint=" + this.tint + ", preserveOriginalColors=" + this.preserveOriginalColors + ")";
        }
    }

    public enum Scene {
        LARGE,
        SMALL,
        IMMERSED;

        private static final h30 $ENTRIES = fs.v(values());

        public static h30 getEntries() {
            return $ENTRIES;
        }
    }

    public static final class WhenMappings {
        public static final int[] $EnumSwitchMapping$0;

        static {
            int[] iArr = new int[Scene.values().length];
            try {
                iArr[Scene.LARGE.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                iArr[Scene.SMALL.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                iArr[Scene.IMMERSED.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            $EnumSwitchMapping$0 = iArr;
        }
    }

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    /* JADX WARN: Type inference failed for: r12v6, types: [one.dot.couiexpressive.hooks.systemui.PixelClockHostView$batteryReceiver$1] */
    public PixelClockHostView(Context context, ClassLoader classLoader, float f, boolean z) {
        super(context);
        context.getClass();
        classLoader.getClass();
        this.aodBatteryYOffset = f;
        this.aodBatteryEnabled = z;
        this.motionInterpolator = new PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f);
        this.calendar = Calendar.getInstance();
        RomTextAnimatorRuntime romTextAnimatorRuntimeCreate = RomTextAnimatorRuntime.Companion.create(context, classLoader);
        this.morphRuntime = romTextAnimatorRuntimeCreate;
        this.timeText = "0000";
        int i = 0;
        GlyphSet glyphSetCreateGlyphSet = createGlyphSet(LARGE_VARIATION, TABULAR_NUMBERS_FEATURE, romTextAnimatorRuntimeCreate != null);
        this.largeSet = glyphSetCreateGlyphSet;
        GlyphSet glyphSetCreateGlyphSet$default = createGlyphSet$default(this, COMPACT_VARIATION, TABULAR_NUMBERS_FEATURE, false, 4, null);
        this.compactSet = glyphSetCreateGlyphSet$default;
        GlyphSet glyphSetCreateGlyphSet$default2 = createGlyphSet$default(this, AOD_LARGE_VARIATION, TABULAR_NUMBERS_FEATURE, false, 4, null);
        this.aodLargeSet = glyphSetCreateGlyphSet$default2;
        GlyphSet glyphSetCreateGlyphSet$default3 = createGlyphSet$default(this, AOD_COMPACT_VARIATION, TABULAR_NUMBERS_FEATURE, false, 4, null);
        this.aodCompactSet = glyphSetCreateGlyphSet$default3;
        GlyphSet[] glyphSetArr = {glyphSetCreateGlyphSet, glyphSetCreateGlyphSet$default, glyphSetCreateGlyphSet$default2, glyphSetCreateGlyphSet$default3};
        this.glyphSets = glyphSetArr;
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(0);
        linearLayout.setGravity(16);
        linearLayout.setClipChildren(false);
        this.dateGroup = linearLayout;
        LinearLayout linearLayout2 = new LinearLayout(context);
        linearLayout2.setOrientation(0);
        linearLayout2.setGravity(16);
        linearLayout2.setClipChildren(false);
        this.weatherGroup = linearLayout2;
        this.dateView = informationText(18.0f, 500);
        this.weekView = informationText(18.0f, 500);
        ImageView imageView = new ImageView(context);
        ImageView.ScaleType scaleType = ImageView.ScaleType.FIT_CENTER;
        imageView.setScaleType(scaleType);
        imageView.setVisibility(8);
        this.weatherIconView = imageView;
        this.weatherView = informationText(18.0f, 500);
        TextView textViewInformationText = informationText(16.0f, 500);
        textViewInformationText.setTextColor(-1);
        textViewInformationText.setAlpha(0.0f);
        this.batteryView = textViewInformationText;
        LinearLayout linearLayout3 = new LinearLayout(context);
        linearLayout3.setOrientation(0);
        linearLayout3.setGravity(8388627);
        linearLayout3.setClipChildren(false);
        linearLayout3.setAlpha(0.0f);
        this.notificationIconRow = linearLayout3;
        LinearLayout linearLayout4 = new LinearLayout(context);
        linearLayout4.setOrientation(1);
        linearLayout4.setGravity(8388611);
        linearLayout4.setClipChildren(false);
        linearLayout4.setAlpha(0.0f);
        this.mediaGroup = linearLayout4;
        TextView textViewInformationText2 = informationText(18.0f, 500);
        textViewInformationText2.setGravity(8388627);
        textViewInformationText2.setMaxLines(1);
        TextUtils.TruncateAt truncateAt = TextUtils.TruncateAt.END;
        textViewInformationText2.setEllipsize(truncateAt);
        this.mediaTitleView = textViewInformationText2;
        LinearLayout linearLayout5 = new LinearLayout(context);
        linearLayout5.setOrientation(0);
        linearLayout5.setGravity(16);
        this.mediaSubtitleRow = linearLayout5;
        ImageView imageView2 = new ImageView(context);
        imageView2.setScaleType(scaleType);
        this.mediaAppIconView = imageView2;
        TextView textViewInformationText3 = informationText(15.0f, 450);
        textViewInformationText3.setMaxLines(1);
        textViewInformationText3.setEllipsize(truncateAt);
        this.mediaArtistView = textViewInformationText3;
        this.scene = Scene.LARGE;
        this.aodContent = AodContent.None.INSTANCE;
        this.monetColor = Integer.MIN_VALUE;
        this.aodMonetColor = Integer.MIN_VALUE;
        this.lastMinute = Long.MIN_VALUE;
        this.lastBatteryLevel = -1;
        this.batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                this.this$0.updateBattery(intent);
            }
        };
        this.appliedGlyphTargets = new IdentityHashMap<>();
        this.appliedInformationTargets = new IdentityHashMap<>();
        v51 v51Var = new v51(i);
        v51Var.e = this;
        VarHandle.storeStoreFence();
        this.targetApplyRunnable = v51Var;
        setId(View.generateViewId());
        setClipChildren(false);
        setClipToPadding(false);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(2);
        glyphSetArr = romTextAnimatorRuntimeCreate != null ? new GlyphSet[]{glyphSetCreateGlyphSet} : glyphSetArr;
        for (GlyphSet glyphSet : glyphSetArr) {
            for (TextView textView : glyphSet.getDigits()) {
                addView(textView, glyphLayoutParams());
            }
            addView(glyphSet.getColon(), glyphLayoutParams());
        }
        addView(this.dateGroup, glyphLayoutParams());
        addView(this.weatherGroup, glyphLayoutParams());
        addView(this.notificationIconRow, glyphLayoutParams());
        addView(this.mediaGroup, glyphLayoutParams());
        addView(this.batteryView, glyphLayoutParams());
        this.dateGroup.addView(this.dateView);
        this.dateGroup.addView(this.weekView);
        LinearLayout linearLayout6 = this.weatherGroup;
        ImageView imageView3 = this.weatherIconView;
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        layoutParams.setMarginEnd(dp(4));
        linearLayout6.addView(imageView3, layoutParams);
        this.weatherGroup.addView(this.weatherView);
        this.mediaGroup.addView(this.mediaTitleView);
        LinearLayout linearLayout7 = this.mediaGroup;
        LinearLayout linearLayout8 = this.mediaSubtitleRow;
        LinearLayout.LayoutParams layoutParams2 = new LinearLayout.LayoutParams(-2, -2);
        layoutParams2.topMargin = dp(4);
        linearLayout7.addView(linearLayout8, layoutParams2);
        LinearLayout linearLayout9 = this.mediaSubtitleRow;
        ImageView imageView4 = this.mediaAppIconView;
        LinearLayout.LayoutParams layoutParams3 = new LinearLayout.LayoutParams(dp(18), dp(18));
        layoutParams3.setMarginEnd(dp(6));
        linearLayout9.addView(imageView4, layoutParams3);
        this.mediaSubtitleRow.addView(this.mediaArtistView);
        onTimeTick();
        updateBattery$default(this, null, 1, null);
        updateMonetColor();
        setInitialVisibility();
        scheduleApplyTargets(false);
        RomTextAnimatorRuntime romTextAnimatorRuntime = this.morphRuntime;
        if (romTextAnimatorRuntime != null) {
            romTextAnimatorRuntime.prewarmAsync(this.motionInterpolator);
        }
    }

    /* JADX WARN: Code duplicated, block: B:13:0x002b  */
    /* JADX WARN: Code duplicated, block: B:19:0x003a  */
    private final void applyAodContentTarget(boolean z, long j) {
        boolean z2;
        float height;
        if (isPartialAodActive()) {
            AodContent aodContent = this.aodContent;
            if (!(aodContent instanceof AodContent.Notifications)) {
                if (aodContent instanceof AodContent.Media) {
                    if (!((AodContent.Media) aodContent).getNotificationIcons().isEmpty()) {
                        z2 = true;
                    }
                } else if (!ph0.i(aodContent, AodContent.None.INSTANCE)) {
                    qc.n();
                    return;
                }
                z2 = false;
            } else if (((AodContent.Notifications) aodContent).getIcons().isEmpty()) {
                z2 = false;
            } else {
                z2 = true;
            }
        } else {
            z2 = false;
        }
        boolean z3 = (this.aodContent instanceof AodContent.Media) || (!isPartialAodActive() && (this.aodContent instanceof AodContent.None) && this.retainedAodMediaLayout);
        boolean z4 = isPartialAodActive() && z3;
        boolean z5 = this.dozing;
        float f = z5 ? this.burnInX : 0.0f;
        float fDp = z5 ? this.burnInY : 0.0f;
        float height2 = (getHeight() * 0.255f) + dp(0.0f) + fDp;
        if (z3) {
            height = this.mediaGroup.getMeasuredHeight() + height2;
            fDp = dp(28.0f);
        } else {
            height = (getHeight() * 0.255f) + dp(0.0f);
        }
        float f2 = height + fDp;
        boolean z6 = !isPartialAodActive() && z;
        float translationY = f2;
        LinearLayout linearLayout = this.notificationIconRow;
        float translationX = z6 ? linearLayout.getTranslationX() : dp(32.0f) + f;
        if (z6) {
            translationY = this.notificationIconRow.getTranslationY();
        }
        applyContentViewTarget(linearLayout, translationX, translationY, z2, z, j, false);
        LinearLayout linearLayout2 = this.mediaGroup;
        float translationX2 = z6 ? linearLayout2.getTranslationX() : dp(32.0f) + f;
        if (z6) {
            height2 = this.mediaGroup.getTranslationY();
        }
        applyContentViewTarget(linearLayout2, translationX2, height2, z4, z, j, false);
    }

    private final void applyBatteryTarget(boolean z, long j) {
        boolean z2 = this.dozing;
        boolean z3 = z2 && this.aodBatteryEnabled;
        float f = z2 ? this.burnInX * 0.75f : 0.0f;
        float f2 = z2 ? (-Math.abs(this.burnInY)) * 0.5f : 0.0f;
        float width = (getWidth() - this.batteryView.getMeasuredWidth()) / 2.0f;
        if (width < 0.0f) {
            width = 0.0f;
        }
        float f3 = width + f;
        float height = ((getHeight() - dp(64.0f)) - this.batteryView.getMeasuredHeight()) + dp(this.aodBatteryYOffset) + f2;
        this.batteryView.animate().cancel();
        TextView textView = this.batteryView;
        if (z) {
            textView.animate().translationX(f3).translationY(height).alpha(z3 ? 1.0f : 0.0f).setDuration(j).setInterpolator(this.motionInterpolator).start();
            return;
        }
        textView.setTranslationX(f3);
        this.batteryView.setTranslationY(height);
        this.batteryView.setAlpha(z3 ? 1.0f : 0.0f);
    }

    private final void applyClockColors() {
        Integer numValueOf = Integer.valueOf(this.monetColor);
        if (numValueOf.intValue() == Integer.MIN_VALUE) {
            numValueOf = null;
        }
        int iIntValue = numValueOf != null ? numValueOf.intValue() : -1515784;
        Integer numValueOf2 = Integer.valueOf(this.aodMonetColor);
        Integer num = numValueOf2.intValue() != Integer.MIN_VALUE ? numValueOf2 : null;
        int iIntValue2 = num != null ? num.intValue() : iIntValue;
        if (this.morphRuntime == null) {
            for (TextView textView : this.largeSet.getDigits()) {
                textView.setTextColor(iIntValue);
            }
            this.largeSet.getColon().setTextColor(iIntValue);
        }
        if (getVisualScene() == Scene.IMMERSED) {
            iIntValue = -1;
        }
        for (TextView textView2 : this.compactSet.getDigits()) {
            textView2.setTextColor(iIntValue);
        }
        this.compactSet.getColon().setTextColor(iIntValue);
        for (TextView textView3 : this.aodLargeSet.getDigits()) {
            textView3.setTextColor(iIntValue2);
        }
        this.aodLargeSet.getColon().setTextColor(iIntValue2);
        for (TextView textView4 : this.aodCompactSet.getDigits()) {
            textView4.setTextColor(iIntValue2);
        }
        this.aodCompactSet.getColon().setTextColor(iIntValue2);
        this.dateView.setTextColor(-1);
        this.weekView.setTextColor(-1);
        this.weatherView.setTextColor(-1);
        this.weatherIconView.setImageTintList(ColorStateList.valueOf(-1));
        this.mediaTitleView.setTextColor(-1);
        this.mediaArtistView.setTextColor(-1);
        this.mediaAppIconView.setImageTintList(ColorStateList.valueOf(-1));
    }

    public final void applyContentViewTarget(View view, float f, float f2, boolean z, boolean z2, long j, boolean z3) {
        view.animate().cancel();
        if (!z3) {
            view.setTranslationX(f);
            view.setTranslationY(f2);
        }
        if (!z2) {
            view.setTranslationX(f);
            view.setTranslationY(f2);
            view.setAlpha(z ? 1.0f : 0.0f);
        } else {
            ViewPropertyAnimator interpolator = view.animate().alpha(z ? 1.0f : 0.0f).setDuration(j).setInterpolator(this.motionInterpolator);
            interpolator.getClass();
            if (z3) {
                interpolator.translationX(f).translationY(f2);
            }
            interpolator.start();
        }
    }

    private final void applyGlyphSet(GlyphSet glyphSet, ClockTargets clockTargets, float f, boolean z, long j) {
        TextView[] digits = glyphSet.getDigits();
        int length = digits.length;
        int i = 0;
        int i2 = 0;
        while (i < length) {
            applyTarget(digits[i], clockTargets.getDigits()[i2], f, z, j);
            i++;
            i2++;
        }
        applyTarget(glyphSet.getColon(), clockTargets.getColon(), f, z, j);
    }

    private final void applyInformationTarget(View view, float f, float f2, boolean z, long j) {
        AppliedViewTarget appliedViewTarget = new AppliedViewTarget(f, f2, 1.0f);
        if (ph0.i(this.appliedInformationTargets.get(view), appliedViewTarget)) {
            return;
        }
        this.appliedInformationTargets.put(view, appliedViewTarget);
        view.animate().cancel();
        if (z) {
            view.animate().translationX(f).translationY(f2).alpha(1.0f).setDuration(j).setInterpolator(this.motionInterpolator).start();
            return;
        }
        view.setTranslationX(f);
        view.setTranslationY(f2);
        view.setAlpha(1.0f);
    }

    private final void applyInformationTargets(boolean z, long j) {
        float width;
        float f;
        float measuredWidth;
        float f2;
        float width2;
        float fDp;
        float height;
        float fDp2;
        float measuredHeight;
        float fDp3;
        float width3;
        float fDp4;
        float height2;
        float fDp5;
        int i = WhenMappings.$EnumSwitchMapping$0[getVisualScene().ordinal()];
        if (i != 1) {
            if (i == 2) {
                int measuredWidth2 = this.dateGroup.getMeasuredWidth();
                int measuredWidth3 = this.weatherGroup.getMeasuredWidth();
                if (measuredWidth2 < measuredWidth3) {
                    measuredWidth2 = measuredWidth3;
                }
                if (this.dozing) {
                    width2 = getWidth() * 0.75f;
                    fDp = dp(-34.0f);
                } else {
                    width2 = getWidth() * 0.75f;
                    fDp = dp(-36.0f);
                }
                f2 = (width2 + fDp) - (measuredWidth2 / 2.0f);
                if (this.dozing) {
                    height = getHeight() * 0.118f;
                    fDp2 = dp(33.0f);
                } else {
                    height = getHeight() * 0.118f;
                    fDp2 = dp(33.0f);
                }
                width = height + fDp2;
                measuredHeight = this.dateGroup.getMeasuredHeight() + width;
                fDp3 = dp(3.0f);
            } else {
                if (i != 3) {
                    qc.n();
                    return;
                }
                int measuredWidth4 = this.dateGroup.getMeasuredWidth();
                int measuredWidth5 = this.weatherGroup.getMeasuredWidth();
                if (measuredWidth4 < measuredWidth5) {
                    measuredWidth4 = measuredWidth5;
                }
                if (this.dozing) {
                    width3 = getWidth() * 0.75f;
                    fDp4 = dp(-34.0f);
                } else {
                    width3 = getWidth() * 0.75f;
                    fDp4 = dp(-36.0f);
                }
                f2 = (width3 + fDp4) - (measuredWidth4 / 2.0f);
                if (this.dozing) {
                    height2 = getHeight() * 0.118f;
                    fDp5 = dp(33.0f);
                } else {
                    height2 = getHeight() * 0.09f;
                    fDp5 = dp(30.0f);
                }
                width = height2 + fDp5;
                measuredHeight = this.dateGroup.getMeasuredHeight() + width;
                fDp3 = dp(3.0f);
            }
            measuredWidth = f2;
            f = measuredHeight + fDp3;
        } else {
            float f3 = this.dozing ? 0.9f : 1.0f;
            float width4 = (((1.0f - f3) * ((getWidth() * 0.47f) * 1.82f)) / 2.0f) + (getHeight() * 0.215f);
            float width5 = (getWidth() - ((this.dateGroup.getMeasuredWidth() + dp(10.0f)) + this.weatherGroup.getMeasuredWidth())) / 2.0f;
            float fDp6 = dp(16.0f);
            if (width5 >= fDp6) {
                fDp6 = width5;
            }
            width = (getWidth() * 0.47f * 1.9f * f3) + width4;
            f = width;
            measuredWidth = this.dateGroup.getMeasuredWidth() + fDp6 + dp(10.0f);
            f2 = fDp6;
        }
        boolean z2 = this.dozing;
        float f4 = z2 ? this.burnInX : 0.0f;
        float f5 = z2 ? this.burnInY : 0.0f;
        applyInformationTarget(this.dateGroup, f2 + f4, width + f5, z, j);
        applyInformationTarget(this.weatherGroup, measuredWidth + f4, f + f5, z, j);
    }

    private final void applyMorphColonTarget(View view, GlyphTarget glyphTarget, boolean z, long j) {
        AppliedGlyphTarget appliedGlyphTarget = new AppliedGlyphTarget(glyphTarget, glyphTarget.getAlpha());
        if (ph0.i(this.appliedGlyphTargets.get(view), appliedGlyphTarget)) {
            return;
        }
        this.appliedGlyphTargets.put(view, appliedGlyphTarget);
        view.animate().cancel();
        ObjectAnimator objectAnimator = this.colonAlphaAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        this.colonAlphaAnimator = null;
        if (!z) {
            view.setTranslationX(glyphTarget.getX());
            view.setTranslationY(glyphTarget.getY());
            view.setScaleX(glyphTarget.getScale());
            view.setScaleY(glyphTarget.getScale());
            view.setAlpha(glyphTarget.getAlpha());
            return;
        }
        view.animate().translationX(glyphTarget.getX()).translationY(glyphTarget.getY()).scaleX(glyphTarget.getScale()).scaleY(glyphTarget.getScale()).setDuration(j).setInterpolator(this.motionInterpolator).start();
        boolean z2 = glyphTarget.getAlpha() > view.getAlpha();
        ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(view, (Property<View, Float>) View.ALPHA, view.getAlpha(), glyphTarget.getAlpha());
        objectAnimatorOfFloat.setStartDelay(z2 ? (long) (j * 0.52f) : 0L);
        long j2 = (long) (j * 0.22f);
        if (j2 < 1) {
            j2 = 1;
        }
        objectAnimatorOfFloat.setDuration(j2);
        objectAnimatorOfFloat.setInterpolator(this.motionInterpolator);
        objectAnimatorOfFloat.start();
        this.colonAlphaAnimator = objectAnimatorOfFloat;
    }

    /* JADX WARN: Code duplicated, block: B:22:0x003f A[PHI: r0
  0x003f: PHI (r0v22 java.lang.Integer) = (r0v17 java.lang.Integer), (r0v20 java.lang.Integer) binds: [B:21:0x003d, B:28:0x0053] A[DONT_GENERATE, DONT_INLINE]] */
    private final void applyMorphStyle(boolean z, long j) {
        String str;
        boolean z2;
        long j2;
        if (this.dozing && getVisualScene() == Scene.LARGE) {
            str = AOD_LARGE_VARIATION;
        } else if (this.dozing) {
            str = AOD_COMPACT_VARIATION;
        } else {
            str = getVisualScene() == Scene.LARGE ? LARGE_VARIATION : COMPACT_VARIATION;
        }
        String str2 = str;
        int iIntValue = -1515784;
        if (this.dozing) {
            Integer numValueOf = Integer.valueOf(this.aodMonetColor);
            if (numValueOf.intValue() == Integer.MIN_VALUE) {
                numValueOf = null;
            }
            if (numValueOf != null) {
                iIntValue = numValueOf.intValue();
            } else {
                numValueOf = Integer.valueOf(this.monetColor);
                if (numValueOf.intValue() == Integer.MIN_VALUE) {
                    numValueOf = null;
                }
                if (numValueOf != null) {
                    iIntValue = numValueOf.intValue();
                }
            }
        } else if (getVisualScene() == Scene.IMMERSED) {
            iIntValue = -1;
        } else {
            Integer numValueOf2 = Integer.valueOf(this.monetColor);
            if (numValueOf2.intValue() == Integer.MIN_VALUE) {
                numValueOf2 = null;
            }
            if (numValueOf2 != null) {
                iIntValue = numValueOf2.intValue();
            }
        }
        int i = iIntValue;
        TextView[] digits = this.largeSet.getDigits();
        int length = digits.length;
        int i2 = 0;
        while (i2 < length) {
            TextView textView = digits[i2];
            MorphingGlyphView morphingGlyphView = textView instanceof MorphingGlyphView ? (MorphingGlyphView) textView : null;
            if (morphingGlyphView != null) {
                z2 = z;
                j2 = j;
                morphingGlyphView.setMorphStyle(str2, i, z2, j2, this.motionInterpolator);
            } else {
                z2 = z;
                j2 = j;
            }
            i2++;
            z = z2;
            j = j2;
        }
        boolean z3 = z;
        long j3 = j;
        TextView colon = this.largeSet.getColon();
        MorphingGlyphView morphingGlyphView2 = colon instanceof MorphingGlyphView ? (MorphingGlyphView) colon : null;
        if (morphingGlyphView2 != null) {
            morphingGlyphView2.setMorphStyle(str2, i, z3, j3, this.motionInterpolator);
        }
    }

    private final void applyTarget(View view, GlyphTarget glyphTarget, float f, boolean z, long j) {
        float alpha = glyphTarget.getAlpha() * f;
        AppliedGlyphTarget appliedGlyphTarget = new AppliedGlyphTarget(glyphTarget, alpha);
        if (ph0.i(this.appliedGlyphTargets.get(view), appliedGlyphTarget)) {
            return;
        }
        this.appliedGlyphTargets.put(view, appliedGlyphTarget);
        view.animate().cancel();
        if (z) {
            view.animate().translationX(glyphTarget.getX()).translationY(glyphTarget.getY()).scaleX(glyphTarget.getScale()).scaleY(glyphTarget.getScale()).alpha(alpha).setDuration(j).setInterpolator(this.motionInterpolator).start();
            return;
        }
        view.setTranslationX(glyphTarget.getX());
        view.setTranslationY(glyphTarget.getY());
        view.setScaleX(glyphTarget.getScale());
        view.setScaleY(glyphTarget.getScale());
        view.setAlpha(alpha);
    }

    private final void applyTargets(boolean z, long j) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        cancelScheduledTargetApply();
        updateInformationShadow();
        ClockTargets clockTargetsCalculateGlyphTargets = calculateGlyphTargets();
        if (this.morphRuntime != null) {
            applyMorphStyle(z, j);
            TextView[] digits = this.largeSet.getDigits();
            int length = digits.length;
            int i = 0;
            int i2 = 0;
            while (i2 < length) {
                int i3 = i;
                applyTarget(digits[i2], clockTargetsCalculateGlyphTargets.getDigits()[i3], 1.0f, z, j);
                i2++;
                i = i3 + 1;
            }
            applyMorphColonTarget(this.largeSet.getColon(), clockTargetsCalculateGlyphTargets.getColon(), z, j);
        } else {
            applyGlyphSet(this.largeSet, clockTargetsCalculateGlyphTargets, (this.dozing || getVisualScene() != Scene.LARGE) ? 0.0f : 1.0f, z, j);
            applyGlyphSet(this.compactSet, clockTargetsCalculateGlyphTargets, (this.dozing || getVisualScene() == Scene.LARGE) ? 0.0f : 1.0f, z, j);
            applyGlyphSet(this.aodLargeSet, clockTargetsCalculateGlyphTargets, (this.dozing && getVisualScene() == Scene.LARGE) ? 1.0f : 0.0f, z, j);
            applyGlyphSet(this.aodCompactSet, clockTargetsCalculateGlyphTargets, (!this.dozing || getVisualScene() == Scene.LARGE) ? 0.0f : 1.0f, z, j);
        }
        applyInformationTargets(z, j);
        applyAodContentTarget(z, j);
        applyBatteryTarget(z, j);
    }

    public static void applyTargets$default(PixelClockHostView pixelClockHostView, boolean z, long j, int i, Object obj) {
        if ((i & 2) != 0) {
            j = 550;
        }
        pixelClockHostView.applyTargets(z, j);
    }

    public static final void beginAodEntry$lambda$0(PixelClockHostView pixelClockHostView, long j, Scene scene, boolean z, AodContent aodContent, boolean z2) {
        if (pixelClockHostView.aodEntryInProgress && j == pixelClockHostView.aodEntryGeneration) {
            pixelClockHostView.scene = scene;
            pixelClockHostView.dozing = true;
            pixelClockHostView.partialAod = z;
            pixelClockHostView.bindAodContent(aodContent);
            pixelClockHostView.updateBurnInOffset(System.currentTimeMillis() / 60000);
            pixelClockHostView.applyClockColors();
            applyTargets$default(pixelClockHostView, z2, 0L, 2, null);
            u51 u51Var = new u51(0);
            u51Var.f = pixelClockHostView;
            u51Var.e = j;
            VarHandle.storeStoreFence();
            pixelClockHostView.finishAodEntryRunnable = u51Var;
            pixelClockHostView.postDelayed(u51Var, z2 ? 550L : 0L);
        }
    }

    public static final void beginAodEntry$lambda$0$0(PixelClockHostView pixelClockHostView, long j) {
        if (pixelClockHostView.aodEntryInProgress && j == pixelClockHostView.aodEntryGeneration) {
            pixelClockHostView.aodEntryInProgress = false;
            pixelClockHostView.finishAodEntryRunnable = null;
            AodContent aodContent = pixelClockHostView.deferredAodContent;
            pixelClockHostView.deferredAodContent = null;
            if (aodContent == null || aodContent.equals(pixelClockHostView.aodContent)) {
                return;
            }
            if (pixelClockHostView.isPartialAodActive() && pixelClockHostView.partialSceneFor(pixelClockHostView.aodContent) != pixelClockHostView.partialSceneFor(aodContent)) {
                pixelClockHostView.startLiveAodCrossfade(aodContent);
            } else {
                pixelClockHostView.bindAodContent(aodContent);
                applyTargets$default(pixelClockHostView, false, 0L, 2, null);
            }
        }
    }

    private final void bindAodContent(AodContent aodContent) {
        Drawable drawableNewDrawable;
        this.aodContent = aodContent;
        if (!ph0.i(aodContent, AodContent.None.INSTANCE)) {
            if (aodContent instanceof AodContent.Notifications) {
                this.retainedAodMediaLayout = false;
                updateNotificationIcons(((AodContent.Notifications) aodContent).getIcons());
            } else {
                if (!(aodContent instanceof AodContent.Media)) {
                    qc.n();
                    return;
                }
                this.retainedAodMediaLayout = true;
                AodContent.Media media = (AodContent.Media) aodContent;
                updateNotificationIcons(media.getNotificationIcons());
                this.mediaTitleView.setText(media.getTitle());
                this.mediaArtistView.setText(media.getArtist());
                Drawable.ConstantState appIconState = media.getAppIconState();
                Drawable drawableMutate = (appIconState == null || (drawableNewDrawable = appIconState.newDrawable(getResources())) == null) ? null : drawableNewDrawable.mutate();
                AdaptiveIconDrawable adaptiveIconDrawable = drawableMutate instanceof AdaptiveIconDrawable ? (AdaptiveIconDrawable) drawableMutate : null;
                if (adaptiveIconDrawable != null) {
                    Drawable monochrome = adaptiveIconDrawable.getMonochrome();
                    if (monochrome == null) {
                        monochrome = adaptiveIconDrawable.getForeground();
                    }
                    if (monochrome != null) {
                        drawableMutate = monochrome;
                    }
                }
                this.mediaAppIconView.setImageDrawable(drawableMutate != null ? trimMediaIcon(drawableMutate) : null);
                this.mediaAppIconView.setImageTintList(ColorStateList.valueOf(-1));
            }
        }
        this.notificationIconRow.requestLayout();
        this.mediaGroup.requestLayout();
    }

    private final ClockTargets calculateGlyphTargets() {
        float f;
        float fDp;
        float f2;
        float fDp2;
        float f3;
        float fDp3;
        float f4;
        float fDp4;
        float width = getWidth();
        float height = getHeight();
        float f5 = 0.47f * width;
        boolean z = this.dozing;
        float f6 = z ? this.burnInX : 0.0f;
        float f7 = z ? this.burnInY : 0.0f;
        int i = WhenMappings.$EnumSwitchMapping$0[getVisualScene().ordinal()];
        if (i == 1) {
            float f8 = f7;
            boolean z2 = this.dozing;
            float f9 = z2 ? 0.9f : 1.0f;
            float fDp5 = (((1.0f - f9) * (1.82f * f5)) / 2.0f) + (height * 0.215f) + (z2 ? dp(-24.0f) : dp(-10.0f));
            float f10 = f5 * 0.82f * f9;
            float f11 = width / 2.0f;
            GlyphSet glyphSet = this.dozing ? this.aodLargeSet : this.largeSet;
            float f12 = f9;
            float[] fArrPairX = pairX(glyphSet, 0, 1, f11, f12);
            float[] fArrPairX2 = pairX(glyphSet, 2, 3, f11, f12);
            float f13 = f6;
            float f14 = fDp5 + f10;
            return new ClockTargets(new GlyphTarget[]{target(fArrPairX[0], fDp5, f12, 1.0f, f13, f8), target(fArrPairX[1], fDp5, f12, 1.0f, f13, f8), target(fArrPairX2[0], f14, f12, 1.0f, f13, f8), target(fArrPairX2[1], f14, f12, 1.0f, f13, f8)}, target(f11, (f10 / 2.0f) + fDp5, 0.44f, 0.0f, f13, f8));
        }
        if (i == 2) {
            float f15 = f6;
            float f16 = f7;
            boolean z3 = this.dozing;
            GlyphSet glyphSet2 = z3 ? this.aodCompactSet : this.compactSet;
            if (z3) {
                f = width * 0.25f;
                fDp = dp(10.0f);
            } else {
                f = width * 0.25f;
                fDp = dp(8.0f);
            }
            float f17 = f + fDp;
            if (this.dozing) {
                f2 = height * 0.105f;
                fDp2 = dp(25.0f);
            } else {
                f2 = height * 0.105f;
                fDp2 = dp(25.0f);
            }
            return lineTargets(glyphSet2, 0.36170214f, f17, f2 + fDp2, f15, f16);
        }
        if (i != 3) {
            qc.n();
            return null;
        }
        boolean z4 = this.dozing;
        GlyphSet glyphSet3 = z4 ? this.aodCompactSet : this.compactSet;
        float f18 = z4 ? 0.36170214f : 0.32978722f;
        if (z4) {
            f3 = width * 0.25f;
            fDp3 = dp(10.0f);
        } else {
            f3 = width * 0.25f;
            fDp3 = dp(8.0f);
        }
        float f19 = f3 + fDp3;
        if (this.dozing) {
            f4 = height * 0.105f;
            fDp4 = dp(25.0f);
        } else {
            f4 = height * 0.072f;
            fDp4 = dp(30.0f);
        }
        return lineTargets(glyphSet3, f18, f19, f4 + fDp4, f6, f7);
    }

    private final void cancelAodEntryTransaction() {
        this.aodEntryGeneration++;
        this.aodEntryInProgress = false;
        this.deferredAodContent = null;
        Runnable runnable = this.finishAodEntryRunnable;
        if (runnable != null) {
            removeCallbacks(runnable);
        }
        this.finishAodEntryRunnable = null;
    }

    private final void cancelLiveAodCrossfade() {
        this.liveAodCrossfadeGeneration++;
        this.liveAodCrossfadeInProgress = false;
        this.deferredLiveAodContent = null;
        animate().cancel();
        setAlpha(1.0f);
    }

    private final void cancelPendingLiveAodRetarget(boolean z) {
        if (z) {
            this.liveAodContentGeneration++;
        }
        ViewTreeObserver.OnPreDrawListener onPreDrawListener = this.pendingLivePreDrawListener;
        if (onPreDrawListener != null && getViewTreeObserver().isAlive()) {
            getViewTreeObserver().removeOnPreDrawListener(onPreDrawListener);
        }
        this.pendingLivePreDrawListener = null;
        this.liveAodRetargetPending = false;
    }

    public static void cancelPendingLiveAodRetarget$default(PixelClockHostView pixelClockHostView, boolean z, int i, Object obj) {
        if ((i & 1) != 0) {
            z = true;
        }
        pixelClockHostView.cancelPendingLiveAodRetarget(z);
    }

    private final void cancelRunningPropertyAnimations() {
        for (GlyphSet glyphSet : this.glyphSets) {
            for (TextView textView : glyphSet.getDigits()) {
                textView.animate().cancel();
            }
            glyphSet.getColon().animate().cancel();
        }
        ObjectAnimator objectAnimator = this.colonAlphaAnimator;
        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        this.colonAlphaAnimator = null;
        this.dateGroup.animate().cancel();
        this.weatherGroup.animate().cancel();
        this.notificationIconRow.animate().cancel();
        this.mediaGroup.animate().cancel();
        this.batteryView.animate().cancel();
        this.appliedGlyphTargets.clear();
        this.appliedInformationTargets.clear();
    }

    private final void cancelScheduledTargetApply() {
        if (this.pendingTargetApply) {
            removeCallbacks(this.targetApplyRunnable);
            this.pendingTargetApply = false;
            this.pendingTargetApplyAnimated = false;
        }
    }

    private final float centeredBurnInOffset(long j, float f, long j2) {
        float f2 = (j % j2) / (j2 / 2.0f);
        if (f2 > 1.0f) {
            f2 = 2.0f - f2;
        }
        return ((f2 * 2.0f) - 1.0f) * f;
    }

    private final TextView createClockGlyph(Typeface typeface, String str, String str2, boolean z, boolean z2) {
        TextView textView;
        if (!z || this.morphRuntime == null) {
            textView = new TextView(getContext());
        } else {
            Context context = getContext();
            context.getClass();
            textView = new MorphingGlyphView(context, this.morphRuntime);
        }
        textView.setGravity(8388659);
        textView.setIncludeFontPadding(false);
        textView.setMaxLines(1);
        textView.setTypeface(typeface);
        textView.setFontVariationSettings(str);
        textView.setFontFeatureSettings(str2);
        textView.setPivotX(0.0f);
        textView.setPivotY(0.0f);
        textView.setClickable(false);
        textView.setImportantForAccessibility(2);
        if (z2) {
            textView.setMinWidth(dp(12));
        }
        return textView;
    }

    public static TextView createClockGlyph$default(PixelClockHostView pixelClockHostView, Typeface typeface, String str, String str2, boolean z, boolean z2, int i, Object obj) {
        if ((i & 16) != 0) {
            z2 = false;
        }
        return pixelClockHostView.createClockGlyph(typeface, str, str2, z, z2);
    }

    private final GlyphSet createGlyphSet(String str, String str2, boolean z) {
        FontUtils fontUtils = FontUtils.INSTANCE;
        Context context = getContext();
        context.getClass();
        Typeface typefaceBuildCustomFont = fontUtils.buildCustomFont(context, str);
        if (typefaceBuildCustomFont == null) {
            typefaceBuildCustomFont = Typeface.DEFAULT;
        }
        Typeface typeface = typefaceBuildCustomFont;
        TextView[] textViewArr = new TextView[4];
        int i = 0;
        while (i < 4) {
            typeface.getClass();
            PixelClockHostView pixelClockHostView = this;
            String str3 = str;
            TextView textViewCreateClockGlyph$default = createClockGlyph$default(pixelClockHostView, typeface, str3, str2, z, false, 16, null);
            textViewCreateClockGlyph$default.setText(String.valueOf(pixelClockHostView.timeText.charAt(i)));
            textViewArr[i] = textViewCreateClockGlyph$default;
            i++;
            this = pixelClockHostView;
            str = str3;
        }
        typeface.getClass();
        TextView textViewCreateClockGlyph = this.createClockGlyph(typeface, str, str2, z, true);
        textViewCreateClockGlyph.setText(COLON);
        return new GlyphSet(textViewArr, textViewCreateClockGlyph);
    }

    public static GlyphSet createGlyphSet$default(PixelClockHostView pixelClockHostView, String str, String str2, boolean z, int i, Object obj) {
        if ((i & 4) != 0) {
            z = false;
        }
        return pixelClockHostView.createGlyphSet(str, str2, z);
    }

    private final float dp(float f) {
        return f * getResources().getDisplayMetrics().density;
    }

    private final boolean getCanApplyImmediateTargets() {
        return (this.liveAodRetargetPending || this.liveAodCrossfadeInProgress) ? false : true;
    }

    private final Scene getVisualScene() {
        return (!isPartialAodActive() || (this.aodContent instanceof AodContent.None)) ? this.scene : Scene.SMALL;
    }

    private final FrameLayout.LayoutParams glyphLayoutParams() {
        return new FrameLayout.LayoutParams(-2, -2);
    }

    private final float glyphWidth(GlyphSet glyphSet, int i) {
        return glyphSet.getDigits()[i].getPaint().measureText(String.valueOf(this.timeText.charAt(i)));
    }

    private final TextView informationText(float f, int i) {
        String strL = x30.l("'wght' ", i, ", 'wdth' 100, 'ROND' 0, 'GRAD' 0, 'opsz' 18");
        TextView textView = new TextView(getContext());
        textView.setTextSize(f);
        textView.setGravity(16);
        textView.setIncludeFontPadding(false);
        textView.setMaxLines(1);
        FontUtils fontUtils = FontUtils.INSTANCE;
        Context context = textView.getContext();
        context.getClass();
        Typeface typefaceBuildCustomFont = fontUtils.buildCustomFont(context, strL);
        if (typefaceBuildCustomFont == null) {
            typefaceBuildCustomFont = Typeface.DEFAULT;
        }
        textView.setTypeface(typefaceBuildCustomFont);
        textView.setFontVariationSettings(strL);
        return textView;
    }

    private final ClockTargets lineTargets(GlyphSet glyphSet, float f, float f2, float f3, float f4, float f5) {
        float width = getWidth() * 0.47f * f;
        if (!this.dozing) {
            getVisualScene();
            Scene scene = Scene.LARGE;
        }
        float f6 = (-0.09f) * width;
        float f7 = (-0.049500003f) * width;
        float[] fArr = new float[4];
        for (int i = 0; i < 4; i++) {
            fArr[i] = glyphWidth(glyphSet, i) * f;
        }
        if (!this.dozing) {
            getVisualScene();
            Scene scene2 = Scene.LARGE;
        }
        if (!this.dozing) {
            getVisualScene();
            Scene scene3 = Scene.LARGE;
        }
        if (!this.dozing) {
            getVisualScene();
            Scene scene4 = Scene.LARGE;
        }
        float[] fArr2 = new float[4];
        int i2 = 0;
        while (true) {
            float f8 = 0.0f;
            if (i2 >= 4) {
                break;
            }
            char cCharAt = this.timeText.charAt(i2);
            if (cCharAt == '0') {
                f8 = (-width) * 0.05f;
            } else if (cCharAt == '1') {
                f8 = 0.06f * width;
            }
            fArr2[i2] = f8;
            i2++;
        }
        float[] fArr3 = new float[4];
        for (int i3 = 0; i3 < 4; i3++) {
            char cCharAt2 = this.timeText.charAt(i3);
            fArr3[i3] = cCharAt2 != '0' ? cCharAt2 != '1' ? 0.0f : 0.09f * width : (-width) * 0.05f;
        }
        Iterator it = new kg0(0, 3, 1).iterator();
        double d = 0.0d;
        while (it.hasNext()) {
            int iNextInt = ((jg0) it).nextInt();
            d += (double) (fArr[iNextInt] - (fArr2[iNextInt] + fArr3[iNextInt]));
        }
        float fMeasureText = f2 - (((f7 * 2.0f) + ((f6 * 2.0f) + ((glyphSet.getColon().getPaint().measureText(COLON) * f) + ((float) d)))) / 2.0f);
        float f9 = fArr2[0];
        float f10 = fMeasureText - f9;
        float f11 = (fArr[0] - (f9 + fArr3[0])) + f6 + fMeasureText;
        float f12 = fArr2[1];
        float f13 = f11 - f12;
        float f14 = (fArr[1] - (f12 + fArr3[1])) + f7 + f11;
        float fMeasureText2 = (glyphSet.getColon().getPaint().measureText(COLON) * f) + f7 + f14;
        float f15 = fArr2[2];
        return new ClockTargets(new GlyphTarget[]{target(f10, f3, f, 1.0f, f4, f5), target(f13, f3, f, 1.0f, f4, f5), target(fMeasureText2 - f15, f3, f, 1.0f, f4, f5), target((((fArr[2] - (f15 + fArr3[2])) + f6) + fMeasureText2) - fArr2[3], f3, f, 1.0f, f4, f5)}, target(f14, f3, f, 1.0f, f4, f5));
    }

    private final String localizedChargingText(Locale locale) {
        String language = locale.getLanguage();
        if (language == null) {
            return "Charging";
        }
        switch (language.hashCode()) {
            case 3121:
                return !language.equals("ar") ? "Charging" : "جارٍ الشحن";
            case 3184:
                return !language.equals("cs") ? "Charging" : "Nabíjení";
            case 3197:
                return !language.equals("da") ? "Charging" : "Oplader";
            case 3201:
                return !language.equals("de") ? "Charging" : "Wird geladen";
            case 3239:
                return !language.equals("el") ? "Charging" : "Φόρτιση";
            case 3241:
                language.equals("en");
                return "Charging";
            case 3246:
                return !language.equals("es") ? "Charging" : "Cargando";
            case 3267:
                return !language.equals("fi") ? "Charging" : "Ladataan";
            case 3276:
                return !language.equals("fr") ? "Charging" : "En charge";
            case 3325:
                return !language.equals("he") ? "Charging" : "בטעינה";
            case 3329:
                return !language.equals("hi") ? "Charging" : "चार्ज हो रहा है";
            case 3341:
                return !language.equals("hu") ? "Charging" : "Töltés";
            case 3355:
                return !language.equals("id") ? "Charging" : "Mengisi daya";
            case 3371:
                return !language.equals("it") ? "Charging" : "In carica";
            case 3374:
                return !language.equals("iw") ? "Charging" : "בטעינה";
            case 3383:
                return !language.equals("ja") ? "Charging" : "充電中";
            case 3428:
                return !language.equals("ko") ? "Charging" : "충전 중";
            case 3494:
                return !language.equals("ms") ? "Charging" : "Sedang dicas";
            case 3508:
                return !language.equals("nb") ? "Charging" : "Lader";
            case 3518:
                return !language.equals("nl") ? "Charging" : "Opladen";
            case 3520:
                return !language.equals("nn") ? "Charging" : "Lader";
            case 3521:
                return !language.equals("no") ? "Charging" : "Lader";
            case 3580:
                return !language.equals("pl") ? "Charging" : "Ładowanie";
            case 3588:
                if (language.equals("pt")) {
                    return as1.v(locale.getCountry(), "BR") ? "Carregando" : "A carregar";
                }
                return "Charging";
            case 3645:
                return !language.equals("ro") ? "Charging" : "Se încarcă";
            case 3651:
                return !language.equals("ru") ? "Charging" : "Зарядка";
            case 3683:
                return !language.equals("sv") ? "Charging" : "Laddar";
            case 3700:
                return !language.equals("th") ? "Charging" : "กำลังชาร์จ";
            case 3710:
                return !language.equals("tr") ? "Charging" : "Şarj oluyor";
            case 3734:
                return !language.equals("uk") ? "Charging" : "Заряджання";
            case 3763:
                return !language.equals("vi") ? "Charging" : "Đang sạc";
            case 3886:
                if (!language.equals("zh")) {
                    return "Charging";
                }
                String country = locale.getCountry();
                country.getClass();
                Locale locale2 = Locale.ROOT;
                locale2.getClass();
                String upperCase = country.toUpperCase(locale2);
                upperCase.getClass();
                int iHashCode = upperCase.hashCode();
                if (iHashCode == 2307) {
                    return !upperCase.equals("HK") ? "正在充电" : "正在充電";
                }
                if (iHashCode != 2466) {
                    return (iHashCode == 2691 && upperCase.equals("TW")) ? "充電中" : "正在充电";
                }
                return !upperCase.equals("MO") ? "正在充电" : "正在充電";
            default:
                return "Charging";
        }
    }

    private final float[] pairX(GlyphSet glyphSet, int i, int i2, float f, float f2) {
        float width = getWidth() * 0.47f * f2 * (this.dozing ? -0.06f : -0.07f);
        float fGlyphWidth = glyphWidth(glyphSet, i) * f2;
        float fGlyphWidth2 = f - (((fGlyphWidth + width) + (glyphWidth(glyphSet, i2) * f2)) / 2.0f);
        return new float[]{fGlyphWidth2, fGlyphWidth + fGlyphWidth2 + width};
    }

    private final Scene partialSceneFor(AodContent aodContent) {
        return aodContent instanceof AodContent.None ? Scene.LARGE : Scene.SMALL;
    }

    private final void scheduleApplyTargets(boolean z) {
        this.pendingTargetApplyAnimated = this.pendingTargetApplyAnimated || z;
        if (this.pendingTargetApply) {
            return;
        }
        this.pendingTargetApply = true;
        post(this.targetApplyRunnable);
    }

    private final void setClockBaseSize(float f) {
        for (GlyphSet glyphSet : this.glyphSets) {
            for (TextView textView : glyphSet.getDigits()) {
                textView.setTextSize(0, f);
            }
            glyphSet.getColon().setTextSize(0, f);
            Paint.FontMetrics fontMetrics = glyphSet.getColon().getPaint().getFontMetrics();
            TextView colon = glyphSet.getColon();
            int iG = us0.G(glyphSet.getColon().getPaint().measureText(COLON) + dp(4.0f));
            int iDp = dp(12);
            if (iG < iDp) {
                iG = iDp;
            }
            int iG2 = us0.G(fontMetrics.descent - fontMetrics.ascent);
            int iG3 = us0.G(f);
            if (iG2 < iG3) {
                iG2 = iG3;
            }
            colon.setLayoutParams(new FrameLayout.LayoutParams(iG, iG2));
        }
    }

    private final void setInitialVisibility() {
        for (TextView textView : this.largeSet.getDigits()) {
            textView.setAlpha(1.0f);
        }
        this.largeSet.getColon().setAlpha(0.0f);
        for (TextView textView2 : this.compactSet.getDigits()) {
            textView2.setAlpha(0.0f);
        }
        this.compactSet.getColon().setAlpha(0.0f);
        for (TextView textView3 : this.aodLargeSet.getDigits()) {
            textView3.setAlpha(0.0f);
        }
        this.aodLargeSet.getColon().setAlpha(0.0f);
        for (TextView textView4 : this.aodCompactSet.getDigits()) {
            textView4.setAlpha(0.0f);
        }
        this.aodCompactSet.getColon().setAlpha(0.0f);
    }

    public static boolean shouldRefreshInformation$default(PixelClockHostView pixelClockHostView, long j, int i, Object obj) {
        if ((i & 1) != 0) {
            j = SystemClock.uptimeMillis();
        }
        return pixelClockHostView.shouldRefreshInformation(j);
    }

    private final void startLiveAodCrossfade(AodContent aodContent) {
        cancelPendingLiveAodRetarget$default(this, false, 1, null);
        cancelRunningPropertyAnimations();
        long j = this.liveAodCrossfadeGeneration + 1;
        this.liveAodCrossfadeGeneration = j;
        this.liveAodCrossfadeInProgress = true;
        this.deferredLiveAodContent = aodContent;
        animate().cancel();
        ViewPropertyAnimator interpolator = animate().alpha(0.0f).setDuration(150L).setInterpolator(this.motionInterpolator);
        w51 w51Var = new w51();
        w51Var.d = this;
        w51Var.e = j;
        w51Var.f = aodContent;
        VarHandle.storeStoreFence();
        interpolator.withEndAction(w51Var).start();
    }

    public static final void startLiveAodCrossfade$lambda$0(PixelClockHostView pixelClockHostView, long j, AodContent aodContent) {
        if (pixelClockHostView.liveAodCrossfadeInProgress && j == pixelClockHostView.liveAodCrossfadeGeneration) {
            AodContent aodContent2 = pixelClockHostView.deferredLiveAodContent;
            if (aodContent2 != null) {
                aodContent = aodContent2;
            }
            pixelClockHostView.deferredLiveAodContent = null;
            pixelClockHostView.scene = Scene.LARGE;
            pixelClockHostView.dozing = true;
            pixelClockHostView.partialAod = true;
            pixelClockHostView.bindAodContent(aodContent);
            pixelClockHostView.updateBurnInOffset(System.currentTimeMillis() / 60000);
            pixelClockHostView.applyClockColors();
            pixelClockHostView.requestLayout();
            u51 u51Var = new u51(2);
            u51Var.f = pixelClockHostView;
            u51Var.e = j;
            VarHandle.storeStoreFence();
            pixelClockHostView.postOnAnimation(u51Var);
            pixelClockHostView.invalidate();
        }
    }

    public static final void startLiveAodCrossfade$lambda$0$0(PixelClockHostView pixelClockHostView, long j) {
        if (pixelClockHostView.liveAodCrossfadeInProgress && j == pixelClockHostView.liveAodCrossfadeGeneration) {
            applyTargets$default(pixelClockHostView, false, 0L, 2, null);
            pixelClockHostView.setAlpha(0.0f);
            ViewPropertyAnimator interpolator = pixelClockHostView.animate().alpha(1.0f).setDuration(200L).setInterpolator(pixelClockHostView.motionInterpolator);
            u51 u51Var = new u51(1);
            u51Var.e = j;
            u51Var.f = pixelClockHostView;
            VarHandle.storeStoreFence();
            interpolator.withEndAction(u51Var).start();
        }
    }

    public static final void startLiveAodCrossfade$lambda$0$0$0(long j, PixelClockHostView pixelClockHostView) {
        if (j != pixelClockHostView.liveAodCrossfadeGeneration) {
            return;
        }
        pixelClockHostView.liveAodCrossfadeInProgress = false;
        AodContent aodContent = pixelClockHostView.deferredLiveAodContent;
        pixelClockHostView.deferredLiveAodContent = null;
        if (aodContent == null || aodContent.equals(pixelClockHostView.aodContent)) {
            return;
        }
        pixelClockHostView.setLiveAodContent(aodContent, false);
    }

    private final GlyphTarget target(float f, float f2, float f3, float f4, float f5, float f6) {
        return new GlyphTarget(f + f5, f2 + f6, f3, f4);
    }

    public static final void targetApplyRunnable$lambda$0(PixelClockHostView pixelClockHostView) {
        boolean z = pixelClockHostView.pendingTargetApplyAnimated;
        pixelClockHostView.pendingTargetApply = false;
        pixelClockHostView.pendingTargetApplyAnimated = false;
        if (pixelClockHostView.getCanApplyImmediateTargets()) {
            applyTargets$default(pixelClockHostView, z, 0L, 2, null);
        }
    }

    private final Drawable trimMediaIcon(Drawable drawable) {
        Integer numValueOf = Integer.valueOf(drawable.getIntrinsicWidth());
        if (numValueOf.intValue() <= 0) {
            numValueOf = null;
        }
        int iQ = numValueOf != null ? nr.q(numValueOf.intValue(), 1, 96) : 96;
        Integer numValueOf2 = Integer.valueOf(drawable.getIntrinsicHeight());
        Integer num = numValueOf2.intValue() > 0 ? numValueOf2 : null;
        int iQ2 = num != null ? nr.q(num.intValue(), 1, 96) : 96;
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iQ, iQ2, Bitmap.Config.ARGB_8888);
        bitmapCreateBitmap.getClass();
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Rect rect = new Rect(drawable.getBounds());
        int i = 0;
        drawable.setBounds(0, 0, iQ, iQ2);
        drawable.draw(canvas);
        drawable.setBounds(rect);
        int i2 = iQ * iQ2;
        int[] iArr = new int[i2];
        bitmapCreateBitmap.getPixels(iArr, 0, iQ, 0, 0, iQ, iQ2);
        int i3 = -1;
        int i4 = -1;
        int i5 = iQ;
        int i6 = 0;
        while (i < i2) {
            int i7 = i6 + 1;
            if (Color.alpha(iArr[i]) > 8) {
                int i8 = i6 % iQ;
                int i9 = i6 / iQ;
                if (i8 < i5) {
                    i5 = i8;
                }
                if (i8 > i3) {
                    i3 = i8;
                }
                if (i9 < iQ2) {
                    iQ2 = i9;
                }
                if (i9 > i4) {
                    i4 = i9;
                }
            }
            i++;
            i6 = i7;
        }
        if (i3 < i5 || i4 < iQ2) {
            Drawable drawableMutate = drawable.mutate();
            drawableMutate.getClass();
            return drawableMutate;
        }
        Bitmap bitmapCreateBitmap2 = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        bitmapCreateBitmap2.getClass();
        float f = (i3 - i5) + 1;
        float f2 = (i4 - iQ2) + 1;
        float fMin = Math.min(96.0f / f, 96.0f / f2) * 0.98f;
        float f3 = f * fMin;
        float f4 = f2 * fMin;
        new Canvas(bitmapCreateBitmap2).drawBitmap(bitmapCreateBitmap, new Rect(i5, iQ2, i3 + 1, i4 + 1), new RectF((96.0f - f3) / 2.0f, (96.0f - f4) / 2.0f, (f3 + 96.0f) / 2.0f, (96.0f + f4) / 2.0f), new Paint(3));
        return new BitmapDrawable(getResources(), bitmapCreateBitmap2);
    }

    public static void updateBattery$default(PixelClockHostView pixelClockHostView, Intent intent, int i, Object obj) {
        if ((i & 1) != 0) {
            intent = null;
        }
        pixelClockHostView.updateBattery(intent);
    }

    public static final void updateBattery$lambda$2(PixelClockHostView pixelClockHostView) {
        if (pixelClockHostView.getCanApplyImmediateTargets()) {
            pixelClockHostView.applyBatteryTarget(false, 0L);
        }
    }

    private final void updateBurnInOffset(long j) {
        if (!this.dozing) {
            this.burnInX = 0.0f;
            this.burnInY = 0.0f;
        } else {
            float fDp = dp(this.partialAod ? 8.0f : 5.0f);
            float fDp2 = dp(this.partialAod ? 10.0f : 4.0f);
            this.burnInX = centeredBurnInOffset(j, fDp, 83L);
            this.burnInY = centeredBurnInOffset(j, fDp2, 521L);
        }
    }

    private final void updateInformationShadow() {
        boolean z = (getVisualScene() == Scene.IMMERSED || (isPartialAodActive() && (this.aodContent instanceof AodContent.Media))) ? false : true;
        if (ph0.i(this.informationShadowApplied, Boolean.valueOf(z))) {
            return;
        }
        this.informationShadowApplied = Boolean.valueOf(z);
        float fDp = z ? dp(1.5f) : 0.0f;
        float fDp2 = z ? dp(0.5f) : 0.0f;
        int i = z ? INFORMATION_SHADOW_COLOR : 0;
        this.dateView.setShadowLayer(fDp, 0.0f, fDp2, i);
        this.weekView.setShadowLayer(fDp, 0.0f, fDp2, i);
        this.weatherView.setShadowLayer(fDp, 0.0f, fDp2, i);
    }

    private static final int updateMonetColor$systemColor(PixelClockHostView pixelClockHostView, String str, int i) {
        int identifier = pixelClockHostView.getResources().getIdentifier(str, "color", "android");
        return identifier != 0 ? pixelClockHostView.getResources().getColor(identifier, pixelClockHostView.getContext().getTheme()) : i;
    }

    private final void updateNotificationIcons(List<NotificationIcon> list) {
        LinearLayout linearLayout;
        List listB0 = uk.B0(7, list);
        while (true) {
            int childCount = this.notificationIconRow.getChildCount();
            int size = listB0.size();
            linearLayout = this.notificationIconRow;
            if (childCount >= size) {
                break;
            }
            int childCount2 = linearLayout.getChildCount();
            LinearLayout linearLayout2 = this.notificationIconRow;
            ImageView imageView = new ImageView(getContext());
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(dp(18), dp(18));
            if (childCount2 > 0) {
                layoutParams.setMarginStart(dp(15));
            }
            linearLayout2.addView(imageView, layoutParams);
        }
        int childCount3 = linearLayout.getChildCount();
        for (int i = 0; i < childCount3; i++) {
            View childAt = this.notificationIconRow.getChildAt(i);
            childAt.getClass();
            ImageView imageView2 = (ImageView) childAt;
            NotificationIcon notificationIcon = (NotificationIcon) uk.r0(i, listB0);
            ColorStateList tint = null;
            if (notificationIcon == null) {
                imageView2.setImageDrawable(null);
                imageView2.setVisibility(8);
            } else {
                imageView2.setVisibility(0);
                imageView2.setImageDrawable(notificationIcon.getDrawableState().newDrawable(getResources()).mutate());
                if (!notificationIcon.getPreserveOriginalColors() && (tint = notificationIcon.getTint()) == null) {
                    tint = ColorStateList.valueOf(-1);
                    tint.getClass();
                }
                imageView2.setImageTintList(tint);
                if (notificationIcon.getPreserveOriginalColors()) {
                    imageView2.clearColorFilter();
                }
            }
        }
    }

    public final void beginAodEntry(Scene scene, boolean z, AodContent aodContent, boolean z2) {
        scene.getClass();
        aodContent.getClass();
        cancelAodEntryTransaction();
        cancelPendingLiveAodRetarget$default(this, false, 1, null);
        cancelLiveAodCrossfade();
        cancelRunningPropertyAnimations();
        long j = this.aodEntryGeneration + 1;
        this.aodEntryGeneration = j;
        this.aodEntryInProgress = true;
        this.deferredAodContent = null;
        this.scene = scene;
        this.dozing = false;
        this.partialAod = false;
        bindAodContent(aodContent);
        updateBurnInOffset(System.currentTimeMillis() / 60000);
        applyClockColors();
        applyTargets$default(this, false, 0L, 2, null);
        x51 x51Var = new x51();
        x51Var.d = this;
        x51Var.e = j;
        x51Var.f = scene;
        x51Var.g = z;
        x51Var.h = aodContent;
        x51Var.i = z2;
        VarHandle.storeStoreFence();
        postOnAnimation(x51Var);
    }

    public final void ensureInitialPresentation() {
        if (this.initialPresentationApplied || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        this.initialPresentationApplied = true;
        this.lastMinute = Long.MIN_VALUE;
        onTimeTick();
        for (GlyphSet glyphSet : this.glyphSets) {
            for (TextView textView : glyphSet.getDigits()) {
                textView.setLayerType(0, null);
                textView.requestLayout();
                textView.invalidate();
            }
            glyphSet.getColon().setLayerType(0, null);
            glyphSet.getColon().requestLayout();
            glyphSet.getColon().invalidate();
        }
        requestLayout();
        applyTargets$default(this, false, 0L, 2, null);
        invalidate();
    }

    public final boolean getAcceptsLiveAodContent() {
        return isPartialAodActive() || this.aodEntryInProgress;
    }

    public final boolean getUsesVariableFontMorphing() {
        return this.morphRuntime != null;
    }

    public final boolean isPanoramicAodActive() {
        return this.dozing && !this.partialAod;
    }

    public final boolean isPartialAodActive() {
        return this.dozing && this.partialAod;
    }

    @Override
    public void onAttachedToWindow() {
        Object objF;
        super.onAttachedToWindow();
        if (!this.batteryReceiverRegistered) {
            try {
                getContext().registerReceiver(this.batteryReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                this.batteryReceiverRegistered = true;
                objF = l22.a;
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Throwable thA = tc1.a(objF);
            if (thA != null) {
                sb0 sb0Var = vb0.a;
                vb0.d("PixelClockHostView", "battery-receiver-register", "view=".concat(PixelClockHostView.class.getName()), thA);
            }
        }
        this.lastMinute = Long.MIN_VALUE;
        onTimeTick();
        updateBattery$default(this, null, 1, null);
        requestLayout();
        scheduleApplyTargets(false);
    }

    @Override
    public void onDetachedFromWindow() {
        cancelScheduledTargetApply();
        cancelPendingLiveAodRetarget$default(this, false, 1, null);
        cancelLiveAodCrossfade();
        if (this.batteryReceiverRegistered) {
            try {
                getContext().unregisterReceiver(this.batteryReceiver);
            } catch (Throwable th) {
                dr.f(th);
            }
            this.batteryReceiverRegistered = false;
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (!z || getWidth() <= 0 || getHeight() <= 0 || !getCanApplyImmediateTargets()) {
            return;
        }
        scheduleApplyTargets(false);
    }

    @Override
    public void onMeasure(int i, int i2) {
        int size = View.MeasureSpec.getSize(i);
        if (size > 0 && size != this.clockBaseWidth) {
            this.clockBaseWidth = size;
            float f = size;
            setClockBaseSize(0.47f * f);
            int iG = us0.G(f - dp(64.0f));
            if (iG < 0) {
                iG = 0;
            }
            this.mediaGroup.getLayoutParams().width = iG;
            this.mediaTitleView.setMaxWidth(iG);
            TextView textView = this.mediaArtistView;
            int iDp = iG - dp(24);
            textView.setMaxWidth(iDp >= 0 ? iDp : 0);
        }
        super.onMeasure(i, i2);
    }

    @Override
    public void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i <= 0 || i2 <= 0) {
            return;
        }
        scheduleApplyTargets(false);
    }

    public final void onTimeTick() {
        int i;
        long jCurrentTimeMillis = System.currentTimeMillis() / 60000;
        if (jCurrentTimeMillis == this.lastMinute) {
            return;
        }
        this.lastMinute = jCurrentTimeMillis;
        updateBurnInOffset(jCurrentTimeMillis);
        this.calendar.setTimeInMillis(System.currentTimeMillis());
        boolean zIs24HourFormat = DateFormat.is24HourFormat(getContext());
        Calendar calendar = this.calendar;
        if (zIs24HourFormat) {
            i = calendar.get(11);
        } else {
            i = calendar.get(10);
            if (i == 0) {
                i = 12;
            }
        }
        int i2 = this.calendar.get(12);
        StringBuilder sb = new StringBuilder(4);
        sb.append((char) ((i / 10) + 48));
        sb.append((char) ((i % 10) + 48));
        sb.append((char) ((i2 / 10) + 48));
        sb.append((char) ((i2 % 10) + 48));
        this.timeText = sb.toString();
        for (GlyphSet glyphSet : this.glyphSets) {
            TextView[] digits = glyphSet.getDigits();
            int length = digits.length;
            int i3 = 0;
            int i4 = 0;
            while (i3 < length) {
                digits[i3].setText(String.valueOf(this.timeText.charAt(i4)));
                i3++;
                i4++;
            }
        }
        scheduleApplyTargets(false);
    }

    public final void setAodContent(AodContent aodContent, boolean z) {
        aodContent.getClass();
        if (this.liveAodCrossfadeInProgress) {
            this.deferredLiveAodContent = aodContent;
            return;
        }
        if (this.aodEntryInProgress) {
            if (!isPartialAodActive() || partialSceneFor(this.aodContent) == partialSceneFor(aodContent)) {
                this.deferredAodContent = aodContent;
                return;
            } else {
                cancelAodEntryTransaction();
                startLiveAodCrossfade(aodContent);
                return;
            }
        }
        if (ph0.i(this.aodContent, aodContent)) {
            return;
        }
        if (isPartialAodActive() && partialSceneFor(this.aodContent) != partialSceneFor(aodContent)) {
            startLiveAodCrossfade(aodContent);
        } else {
            bindAodContent(aodContent);
            scheduleApplyTargets(z);
        }
    }

    public final void setAodMode(boolean z, boolean z2, boolean z3) {
        if (this.dozing == z && this.partialAod == z2) {
            return;
        }
        this.dozing = z;
        this.partialAod = z2;
        updateBurnInOffset(System.currentTimeMillis() / 60000);
        applyTargets$default(this, z3, 0L, 2, null);
    }

    public final void setBurnInTranslation(int i, int i2, int i3) {
        float f = i;
        float f2 = i2;
        if (f == this.burnInX && f2 == this.burnInY) {
            return;
        }
        this.burnInX = f;
        this.burnInY = f2;
        if (this.liveAodCrossfadeInProgress) {
            return;
        }
        applyTargets(i3 > 0, nr.q(i3, 0, 2000));
    }

    public final void setLiveAodContent(AodContent aodContent, final boolean z) {
        aodContent.getClass();
        if (getAcceptsLiveAodContent()) {
            if (this.liveAodCrossfadeInProgress) {
                this.deferredLiveAodContent = aodContent;
                return;
            }
            if (this.aodEntryInProgress) {
                cancelAodEntryTransaction();
            }
            Scene scenePartialSceneFor = partialSceneFor(aodContent);
            if (z && getVisualScene() != scenePartialSceneFor) {
                startLiveAodCrossfade(aodContent);
                return;
            }
            cancelRunningPropertyAnimations();
            this.scene = Scene.LARGE;
            this.dozing = true;
            this.partialAod = true;
            bindAodContent(aodContent);
            updateBurnInOffset(System.currentTimeMillis() / 60000);
            applyClockColors();
            final long j = this.liveAodContentGeneration + 1;
            this.liveAodContentGeneration = j;
            cancelPendingLiveAodRetarget(false);
            this.liveAodRetargetPending = true;
            ViewTreeObserver.OnPreDrawListener onPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (this.this$0.getViewTreeObserver().isAlive()) {
                        this.this$0.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    if (this.this$0.pendingLivePreDrawListener == this) {
                        this.this$0.pendingLivePreDrawListener = null;
                    }
                    this.this$0.liveAodRetargetPending = false;
                    if (j != this.this$0.liveAodContentGeneration || !this.this$0.isPartialAodActive()) {
                        return true;
                    }
                    PixelClockHostView.applyTargets$default(this.this$0, z, 0L, 2, null);
                    return true;
                }
            };
            this.pendingLivePreDrawListener = onPreDrawListener;
            getViewTreeObserver().addOnPreDrawListener(onPreDrawListener);
            requestLayout();
            invalidate();
        }
    }

    public final void setPresentation(Scene scene, boolean z, boolean z2, AodContent aodContent, boolean z3) {
        scene.getClass();
        aodContent.getClass();
        if ((!z || !z2) && this.liveAodCrossfadeInProgress) {
            cancelLiveAodCrossfade();
        }
        if (this.liveAodCrossfadeInProgress && z && z2) {
            this.deferredLiveAodContent = aodContent;
            return;
        }
        if ((!z || !z2) && this.liveAodRetargetPending) {
            cancelPendingLiveAodRetarget$default(this, false, 1, null);
        }
        boolean z4 = this.aodEntryInProgress;
        if (z4 && z) {
            if (!z2 || partialSceneFor(this.aodContent) == partialSceneFor(aodContent)) {
                this.deferredAodContent = aodContent;
                return;
            } else {
                cancelAodEntryTransaction();
                startLiveAodCrossfade(aodContent);
                return;
            }
        }
        if (z4) {
            cancelAodEntryTransaction();
        }
        if (isPartialAodActive() && z && z2 && partialSceneFor(this.aodContent) != partialSceneFor(aodContent)) {
            startLiveAodCrossfade(aodContent);
            return;
        }
        if (this.scene == scene && this.dozing == z && this.partialAod == z2 && ph0.i(this.aodContent, aodContent)) {
            return;
        }
        this.scene = scene;
        this.dozing = z;
        this.partialAod = z2;
        updateBurnInOffset(System.currentTimeMillis() / 60000);
        bindAodContent(aodContent);
        applyClockColors();
        scheduleApplyTargets(z3);
    }

    public final void setScene(Scene scene, boolean z) {
        scene.getClass();
        if (this.scene == scene) {
            return;
        }
        this.scene = scene;
        applyClockColors();
        applyTargets$default(this, z, 0L, 2, null);
    }

    public final boolean shouldRefreshInformation(long j) {
        if (j - this.lastInformationRefresh < 1000) {
            return false;
        }
        this.lastInformationRefresh = j;
        return true;
    }

    /* JADX WARN: Code duplicated, block: B:19:0x003b  */
    /* JADX WARN: Code duplicated, block: B:64:0x0023 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    public final void updateBattery(Intent intent) {
        Object objF;
        String str;
        BatteryManager batteryManager = (BatteryManager) getContext().getSystemService(BatteryManager.class);
        if (batteryManager == null) {
            return;
        }
        if (intent == null) {
            try {
                objF = getContext().registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            intent = (Intent) (objF instanceof sc1 ? null : objF);
        } else {
            if (!ph0.i(intent.getAction(), "android.intent.action.BATTERY_CHANGED")) {
                intent = null;
            }
            if (intent == null) {
                objF = getContext().registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
                intent = (Intent) (objF instanceof sc1 ? null : objF);
            }
        }
        int intExtra = intent != null ? intent.getIntExtra("level", -1) : -1;
        int intExtra2 = intent != null ? intent.getIntExtra("scale", 100) : 100;
        int iQ = (intExtra < 0 || intExtra2 <= 0) ? nr.q(batteryManager.getIntProperty(4), 0, 100) : nr.q(us0.G((intExtra * 100.0f) / intExtra2), 0, 100);
        int i = 1;
        int intExtra3 = intent != null ? intent.getIntExtra("status", 1) : 1;
        boolean z = intExtra3 == 2 || intExtra3 == 5 || (intent != null ? intent.getIntExtra("plugged", 0) : 0) != 0;
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String languageTag = locale.toLanguageTag();
        if (iQ == this.lastBatteryLevel && Boolean.valueOf(z).equals(this.lastBatteryCharging) && ph0.i(languageTag, this.lastBatteryLocaleTag)) {
            return;
        }
        this.lastBatteryLevel = iQ;
        this.lastBatteryCharging = Boolean.valueOf(z);
        this.lastBatteryLocaleTag = languageTag;
        TextView textView = this.batteryView;
        if (z) {
            str = iQ + "% · " + localizedChargingText(locale);
        } else {
            str = iQ + "%";
        }
        textView.setText(str);
        this.batteryView.requestLayout();
        v51 v51Var = new v51(i);
        v51Var.e = this;
        VarHandle.storeStoreFence();
        post(v51Var);
    }

    public final void updateInformation(Information information) {
        Drawable weatherIcon;
        Drawable drawableNewDrawable;
        information.getClass();
        Drawable weatherIcon2 = information.getWeatherIcon();
        Drawable.ConstantState constantState = weatherIcon2 != null ? weatherIcon2.getConstantState() : null;
        Information information2 = this.lastInformation;
        if (information2 != null && ph0.i(information2.getDate().toString(), information.getDate().toString()) && ph0.i(information2.getWeek().toString(), information.getWeek().toString()) && ph0.i(information2.getWeather().toString(), information.getWeather().toString()) && this.lastInformationIconState == constantState) {
            return;
        }
        this.lastInformation = information.copy(information.getDate().toString(), information.getWeek().toString(), information.getWeather().toString(), null);
        this.lastInformationIconState = constantState;
        this.dateView.setText(information.getDate());
        this.weekView.setText(information.getWeek());
        this.weatherView.setText(information.getWeather());
        if (constantState == null || (drawableNewDrawable = constantState.newDrawable(getResources())) == null || (weatherIcon = drawableNewDrawable.mutate()) == null) {
            weatherIcon = information.getWeatherIcon();
        }
        this.weatherIconView.setImageDrawable(weatherIcon);
        this.weatherIconView.setVisibility(weatherIcon == null ? 8 : 0);
        this.dateView.setVisibility(information.getDate().length() == 0 ? 8 : 0);
        this.weekView.setVisibility(information.getWeek().length() == 0 ? 8 : 0);
        this.weatherView.setVisibility(information.getWeather().length() != 0 ? 0 : 8);
        updateMonetColor();
        this.dateGroup.requestLayout();
        this.weatherGroup.requestLayout();
        scheduleApplyTargets(false);
    }

    public final void updateMonetColor() {
        int iUpdateMonetColor$systemColor = updateMonetColor$systemColor(this, "system_accent1_100", -1515784);
        int iB = mm.b(updateMonetColor$systemColor(this, "system_accent1_10", -1), updateMonetColor$systemColor(this, "system_accent1_50", iUpdateMonetColor$systemColor), 0.5f);
        if (iUpdateMonetColor$systemColor != this.monetColor) {
            this.monetColor = iUpdateMonetColor$systemColor;
        }
        if (iB != this.aodMonetColor) {
            this.aodMonetColor = iB;
        }
        applyClockColors();
    }

    public static abstract class AodContent {
        public static final int $stable = 0;

        public static final class Media extends AodContent {
            public static final int $stable = 8;
            private final Drawable.ConstantState appIconState;
            private final CharSequence artist;
            private final List<NotificationIcon> notificationIcons;
            private final CharSequence title;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public Media(CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState, List<NotificationIcon> list) {
                super(null);
                charSequence.getClass();
                charSequence2.getClass();
                list.getClass();
                this.title = charSequence;
                this.artist = charSequence2;
                this.appIconState = constantState;
                this.notificationIcons = list;
            }

            /* JADX WARN: Multi-variable type inference failed */
            public static Media copy$default(Media media, CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState, List list, int i, Object obj) {
                if ((i & 1) != 0) {
                    charSequence = media.title;
                }
                if ((i & 2) != 0) {
                    charSequence2 = media.artist;
                }
                if ((i & 4) != 0) {
                    constantState = media.appIconState;
                }
                if ((i & 8) != 0) {
                    list = media.notificationIcons;
                }
                return media.copy(charSequence, charSequence2, constantState, list);
            }

            public final CharSequence component1() {
                return this.title;
            }

            public final CharSequence component2() {
                return this.artist;
            }

            public final Drawable.ConstantState component3() {
                return this.appIconState;
            }

            public final List<NotificationIcon> component4() {
                return this.notificationIcons;
            }

            public final Media copy(CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState, List<NotificationIcon> list) {
                charSequence.getClass();
                charSequence2.getClass();
                list.getClass();
                return new Media(charSequence, charSequence2, constantState, list);
            }

            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (!(obj instanceof Media)) {
                    return false;
                }
                Media media = (Media) obj;
                return ph0.i(this.title, media.title) && ph0.i(this.artist, media.artist) && ph0.i(this.appIconState, media.appIconState) && ph0.i(this.notificationIcons, media.notificationIcons);
            }

            public final Drawable.ConstantState getAppIconState() {
                return this.appIconState;
            }

            public final CharSequence getArtist() {
                return this.artist;
            }

            public final List<NotificationIcon> getNotificationIcons() {
                return this.notificationIcons;
            }

            public final CharSequence getTitle() {
                return this.title;
            }

            public int hashCode() {
                int iHashCode = (this.artist.hashCode() + (this.title.hashCode() * 31)) * 31;
                Drawable.ConstantState constantState = this.appIconState;
                return this.notificationIcons.hashCode() + ((iHashCode + (constantState == null ? 0 : constantState.hashCode())) * 31);
            }

            public String toString() {
                CharSequence charSequence = this.title;
                CharSequence charSequence2 = this.artist;
                return "Media(title=" + ((Object) charSequence) + ", artist=" + ((Object) charSequence2) + ", appIconState=" + this.appIconState + ", notificationIcons=" + this.notificationIcons + ")";
            }
        }

        public static final class None extends AodContent {
            public static final int $stable = 0;
            public static final None INSTANCE = new None();

            private None() {
                super(null);
            }

            public boolean equals(Object obj) {
                return this == obj || (obj instanceof None);
            }

            public int hashCode() {
                return -848521314;
            }

            public String toString() {
                return "None";
            }
        }

        public static final class Notifications extends AodContent {
            public static final int $stable = 8;
            private final List<NotificationIcon> icons;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public Notifications(List<NotificationIcon> list) {
                super(null);
                list.getClass();
                this.icons = list;
            }

            /* JADX WARN: Multi-variable type inference failed */
            public static Notifications copy$default(Notifications notifications, List list, int i, Object obj) {
                if ((i & 1) != 0) {
                    list = notifications.icons;
                }
                return notifications.copy(list);
            }

            public final List<NotificationIcon> component1() {
                return this.icons;
            }

            public final Notifications copy(List<NotificationIcon> list) {
                list.getClass();
                return new Notifications(list);
            }

            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                return (obj instanceof Notifications) && ph0.i(this.icons, ((Notifications) obj).icons);
            }

            public final List<NotificationIcon> getIcons() {
                return this.icons;
            }

            public int hashCode() {
                return this.icons.hashCode();
            }

            public String toString() {
                return "Notifications(icons=" + this.icons + ")";
            }
        }

        public AodContent(zt ztVar) {
            this();
        }

        private AodContent() {
        }
    }

    public static final class Companion {
        public Companion(zt ztVar) {
            this();
        }

        private Companion() {
        }
    }

    private final int dp(int i) {
        return us0.G(dp(i));
    }

    public static final class Information {
        public static final int $stable = 8;
        private final CharSequence date;
        private final CharSequence weather;
        private final Drawable weatherIcon;
        private final CharSequence week;

        public Information(String str, String str2, String str3, Drawable drawable, int i, zt ztVar) {
            this((i & 1) != 0 ? "" : str, (i & 2) != 0 ? "" : str2, (i & 4) != 0 ? "" : str3, (i & 8) != 0 ? null : drawable);
        }

        public static Information copy$default(Information information, CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, Drawable drawable, int i, Object obj) {
            if ((i & 1) != 0) {
                charSequence = information.date;
            }
            if ((i & 2) != 0) {
                charSequence2 = information.week;
            }
            if ((i & 4) != 0) {
                charSequence3 = information.weather;
            }
            if ((i & 8) != 0) {
                drawable = information.weatherIcon;
            }
            return information.copy(charSequence, charSequence2, charSequence3, drawable);
        }

        public final CharSequence component1() {
            return this.date;
        }

        public final CharSequence component2() {
            return this.week;
        }

        public final CharSequence component3() {
            return this.weather;
        }

        public final Drawable component4() {
            return this.weatherIcon;
        }

        public final Information copy(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, Drawable drawable) {
            charSequence.getClass();
            charSequence2.getClass();
            charSequence3.getClass();
            return new Information(charSequence, charSequence2, charSequence3, drawable);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Information)) {
                return false;
            }
            Information information = (Information) obj;
            return ph0.i(this.date, information.date) && ph0.i(this.week, information.week) && ph0.i(this.weather, information.weather) && ph0.i(this.weatherIcon, information.weatherIcon);
        }

        public final CharSequence getDate() {
            return this.date;
        }

        public final CharSequence getWeather() {
            return this.weather;
        }

        public final Drawable getWeatherIcon() {
            return this.weatherIcon;
        }

        public final CharSequence getWeek() {
            return this.week;
        }

        public int hashCode() {
            int iHashCode = (this.weather.hashCode() + ((this.week.hashCode() + (this.date.hashCode() * 31)) * 31)) * 31;
            Drawable drawable = this.weatherIcon;
            return iHashCode + (drawable == null ? 0 : drawable.hashCode());
        }

        public String toString() {
            CharSequence charSequence = this.date;
            CharSequence charSequence2 = this.week;
            CharSequence charSequence3 = this.weather;
            return "Information(date=" + ((Object) charSequence) + ", week=" + ((Object) charSequence2) + ", weather=" + ((Object) charSequence3) + ", weatherIcon=" + this.weatherIcon + ")";
        }

        public Information(CharSequence charSequence, CharSequence charSequence2, CharSequence charSequence3, Drawable drawable) {
            charSequence.getClass();
            charSequence2.getClass();
            charSequence3.getClass();
            this.date = charSequence;
            this.week = charSequence2;
            this.weather = charSequence3;
            this.weatherIcon = drawable;
        }

        public Information() {
            this(null, null, null, null, 15, null);
        }
    }
}
