package one.dot.couiexpressive.hooks.systemui;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import defpackage.a61;
import defpackage.a80;
import defpackage.aq;
import defpackage.b61;
import defpackage.cs0;
import defpackage.ct;
import defpackage.dd0;
import defpackage.dr;
import defpackage.e72;
import defpackage.fd;
import defpackage.fp0;
import defpackage.g40;
import defpackage.gk;
import defpackage.j20;
import defpackage.jb1;
import defpackage.js0;
import defpackage.k31;
import defpackage.kj1;
import defpackage.l22;
import defpackage.l4;
import defpackage.mj1;
import defpackage.ms0;
import defpackage.nr;
import defpackage.oh0;
import defpackage.p80;
import defpackage.ph0;
import defpackage.qc;
import defpackage.sb0;
import defpackage.sc1;
import defpackage.tc1;
import defpackage.uk;
import defpackage.vb0;
import defpackage.vj1;
import defpackage.vk;
import defpackage.w7;
import defpackage.x30;
import defpackage.xw0;
import defpackage.y51;
import defpackage.z51;
import defpackage.zt;
import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import one.dot.couiexpressive.ConfigStore;

public final class PixelLockscreenClockHook implements IXposedHookLoadPackage {
    private static final String AFFORDANCE_ATTACH_FIELD = "coe_pixel_affordance_attach";
    private static final String AFFORDANCE_BACKGROUND_FIELD = "coe_pixel_affordance_background";
    private static final String AFFORDANCE_LAYER_STATE_FIELD = "coe_pixel_affordance_layer_state";
    private static final Set<String> AFFORDANCE_REFRESH_METHODS;
    private static final String AFFORDANCE_RESOURCE_IDS_FIELD = "coe_pixel_affordance_resource_ids";
    private static final Set<String> AFFORDANCE_VIEW_CLASSES;
    private static final long AOD_CONTENT_COMMIT_DELAY_MS = 32;
    private static final String AOD_CONTENT_FRAME_REASON = "COUIExpressive-AodContentUpdate";
    private static final int AOD_CONTENT_FRAME_WINDOW_MS = 700;
    private static final long[] AOD_NOTIFICATION_SYNC_DELAYS;
    private static final long AOD_SLEEP_ORIGIN_TIMEOUT_MS = 30000;
    private static final String BIG_CLOCK_LOGICAL_PACKAGE = "com.oplus.keyguard.clock.big";
    private static final String CENTRAL_SURFACES_CLASS = "com.android.systemui.statusbar.phone.CentralSurfacesImpl";
    private static final String DIM_OVERLAY_FIELD = "coe_pixel_wallpaper_dim_overlay";
    private static final int DISPLAY_STATE_DOZE = 3;
    private static final int DISPLAY_STATE_DOZE_SUSPEND = 4;
    private static final int DISPLAY_STATE_OFF = 1;
    private static final String HOST_CLOCK_PLUGIN_CLASS = "com.oplus.keyguard.plugin.ClockPlugin";
    private static final String HOST_FIELD = "coe_pixel_clock_host";
    private static final int ICON_VISIBLE_ALPHA_THRESHOLD = 8;
    private static final String[] KEYGUARD_SERVICE_BINDER_CLASSES;
    private static final String KEYGUARD_SERVICE_CLASS = "com.android.systemui.keyguard.KeyguardService";
    private static final String KEYGUARD_STATE_CONTROLLER_CLASS = "com.android.systemui.statusbar.policy.KeyguardStateControllerImpl";
    private static final long[] LOCKSCREEN_NOTIFICATION_SYNC_DELAYS;
    private static final int MAX_ANCESTOR_DEPTH = 10;
    private static final int MAX_AOD_NOTIFICATION_ICONS = 7;
    private static final Set<String> NATIVE_AOD_NOTIFICATION_REFRESH_METHODS;
    private static final long[] NATIVE_AOD_NOTIFICATION_SYNC_DELAYS;
    private static final String NOTIFICATION_COLLECTION_LISTENER_CLASS = "com.android.systemui.statusbar.notification.collection.NotifCollection$1";
    private static final String[] NOTIFICATION_ENTRY_ICON_VIEW_METHODS;
    private static final String NOTIFICATION_PANEL_CLASS = "com.android.systemui.shade.NotificationPanelView";
    private static final Set<String> NOTIFICATION_PIPELINE_REFRESH_METHODS;
    private static final String OPLUS_AOD_ADDITIONAL_NOTIFICATION_SETTING = "Setting_AodAdditionalNotification";
    private static final String OPLUS_AOD_DATA_CLASS = "com.oplus.systemui.aod.aodclock.constant.AodData";
    private static final String OPLUS_AOD_DISPLAY_UTIL_CLASS = "com.oplus.systemui.aod.display.AODDisplayUtil";
    private static final String OPLUS_AOD_EXCLUDED_PACKAGE = "com.oplus.olc";
    private static final String OPLUS_AOD_NOTIFICATION_LAYOUT_CLASS = "com.oplus.systemui.aod.aodclock.off.notification.NotificationLayout";
    private static final String OPLUS_AOD_NOTIFICATION_OBSERVER_CLASS = "com.oplus.systemui.aod.common.AodNotificationListenerService$1";
    private static final String OPLUS_AOD_NOTIFICATION_SERVICE_CLASS = "com.oplus.systemui.aod.common.AodNotificationListenerService";
    private static final String OPLUS_AOD_PLUGIN_CALL_CLASS = "com.oplus.systemui.aod.plugin.AodPluginCallImpl";
    private static final String OPLUS_BASE_CLOCK_ROOT_CLASS = "com.oplus.keyguard.clock.common.view.BaseClockViewRoot";
    private static final String OPLUS_CLOCK_ROOT_CLASS = "com.oplus.keyguard.clock.big.ui.view.ClockViewRoot";
    private static final String OPLUS_DARK_MODE_UTIL_CLASS = "com.oplusos.systemui.common.util.OplusDarkModeUtil";
    private static final String OPLUS_KEYGUARD_CLOCK_CONTAINER_CLASS = "com.oplus.systemui.keyguard.view.CustomOplusKeyguardStyleClock";
    private static final String OPLUS_LOCKSCREEN_NOTIFICATION_DISPATCHER_CLASS = "com.oplus.systemui.statusbar.LockScreenNotificationDispatcherImp";
    private static final Set<String> OPLUS_LOCKSCREEN_NOTIFICATION_UPDATE_COROUTINES;
    private static final String OPLUS_ON_TO_DOZE_SUSPEND_REASON = "ONToDozeSuspendTransit";
    private static final String OPLUS_VISUAL_CONTAINER_CLASS = "com.oplus.keyguard.clock.big.widget.MyCustomizedFrameLayout";
    private static final String[] RESOURCE_PACKAGES;
    private static final String SUPPRESSED_VIEWS_FIELD = "coe_pixel_clock_suppressed_views";
    private static final long[] SUPPRESSION_RETRY_DELAYS;
    private static final String SUPPRESS_FIELD = "coe_pixel_clock_suppress_draw";
    private static final String SUPPRESS_HOST_FIELD = "coe_pixel_clock_suppress_host";
    private static final String SYSTEM_UI_PACKAGE = "com.android.systemui";
    private static final String TAG = "COE/PixelLockscreenClock";
    private static final String UDFPS_DARK_BACKGROUND = "system_neutral2_800";
    private static final String UDFPS_DARK_FOREGROUND = "system_neutral1_100";
    private static final String UDFPS_LIGHT_BACKGROUND = "system_neutral2_50";
    private static final String UDFPS_LIGHT_FOREGROUND = "system_neutral1_800";
    private static final int UI_STATE_AOD = 3;
    private static final int UI_STATE_KEYGUARD = 2;
    private static final int UI_STATE_PANORAMIC_AOD = 5;
    private static final int UI_STATE_UNLOCKED = 1;
    private static final int VIEW_CLOCK_TIME = 1;
    private static final int VIEW_DATE_MESSAGE = 11;
    private static final int VIEW_ROOT = 0;
    private static final String WAKEFULNESS_LIFECYCLE_CLASS = "com.android.systemui.keyguard.WakefulnessLifecycle";
    private static final ColorStateList WHITE_TINT;
    private static volatile boolean aodScreenOffOriginHooked;
    private static volatile boolean clockPluginHooked;
    private static volatile Class<?> darkModeUtilClass;
    private static final Map<Drawable.ConstantState, Boolean> grayscaleIconCache;
    private static final Set<Class<?>> hookedAffordanceClasses;
    private static final Set<Class<?>> hookedKeyguardSleepBinderClasses;
    private static final Set<ClassLoader> hookedPluginLoaders;
    private static volatile WeakReference<Object> keyguardStateController;
    private static volatile boolean lockScreenNotificationStateFlowHooked;
    private static volatile boolean nativeAodNotificationLayoutHooked;
    private static volatile boolean newRenderAodNotificationHooked;
    private static volatile boolean notificationPipelineHooked;
    private static volatile boolean oplusAodNotificationHooked;
    private static volatile boolean partialAodDirectSuspendHooked;
    private static volatile boolean wallpaperDimHooked;
    private ActiveMedia activeMedia;
    private WeakReference<MediaController> activeMediaController;
    private final Map<ImageView, Boolean> affordanceViews;
    private final Map<PixelClockHostView, Boolean> aodModeStates;
    private boolean clockEnabled;
    private float darkWallpaperDimPercent;
    private volatile Integer lastObservedStableUiState;
    private WeakReference<Object> lockScreenNotificationDispatcher;
    private AodMediaMonitor mediaMonitor;
    private final Map<String, Drawable.ConstantState> mediaNotificationIconStates;
    private int nativeAodNotificationEntryCount;
    private List<PixelClockHostView.NotificationIcon> nativeAodNotificationIcons;
    private WeakReference<ViewGroup> nativeAodNotificationLayout;
    private boolean nativeAodNotificationSourceReady;
    private int newRenderAodNotificationEntryCount;
    private List<PixelClockHostView.NotificationIcon> newRenderAodNotificationIcons;
    private boolean newRenderAodNotificationSourceReady;
    private WeakReference<Object> newRenderOriginalObserver;
    private WeakReference<Context> notificationContext;
    private int oplusAodNotificationEntryCount;
    private List<PixelClockHostView.NotificationIcon> oplusAodNotificationIcons;
    private List<String> oplusAodNotificationKeys;
    private WeakReference<Object> oplusAodNotificationService;
    private boolean oplusAodNotificationSourceReady;
    private final Map<PixelClockHostView, Boolean> partialAodStates;
    private volatile boolean pendingSleepFromUnlocked;
    private volatile boolean pendingSleepOriginAuthoritative;
    private volatile long pendingSleepOriginLatchedAt;
    private ClassLoader systemUiClassLoader;
    private WeakReference<Context> systemUiPaletteContext;
    private static final Companion Companion = new Companion(null);
    public static final int $stable = 8;
    private static final Object INSTALL_LOCK = new Object();
    private static final Map<ViewGroup, PixelClockHostView> hosts = x30.u();
    private static final Map<PixelClockHostView, PixelClockHostView.Scene> lastLockscreenScenes = x30.u();
    private static final Map<PixelClockHostView, Integer> stableUiStateOrigins = x30.u();
    private static final Map<ViewGroup, PanoramicSyncRequest> pendingPanoramicSyncs = x30.u();
    private float wallpaperDimPercent = 20.0f;
    private boolean affordanceColorsEnabled = true;
    private float aodBatteryYOffset = 16.0f;
    private boolean aodBatteryEnabled = true;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static final class ActiveMedia {
        private final Drawable.ConstantState appIconState;
        private final CharSequence artist;
        private final String packageName;
        private final CharSequence title;

        public ActiveMedia(String str, CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState) {
            str.getClass();
            charSequence.getClass();
            charSequence2.getClass();
            this.packageName = str;
            this.title = charSequence;
            this.artist = charSequence2;
            this.appIconState = constantState;
        }

        public static ActiveMedia copy$default(ActiveMedia activeMedia, String str, CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState, int i, Object obj) {
            if ((i & 1) != 0) {
                str = activeMedia.packageName;
            }
            if ((i & PixelLockscreenClockHook.UI_STATE_KEYGUARD) != 0) {
                charSequence = activeMedia.title;
            }
            if ((i & PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND) != 0) {
                charSequence2 = activeMedia.artist;
            }
            if ((i & 8) != 0) {
                constantState = activeMedia.appIconState;
            }
            return activeMedia.copy(str, charSequence, charSequence2, constantState);
        }

        public final String component1() {
            return this.packageName;
        }

        public final CharSequence component2() {
            return this.title;
        }

        public final CharSequence component3() {
            return this.artist;
        }

        public final Drawable.ConstantState component4() {
            return this.appIconState;
        }

        public final ActiveMedia copy(String str, CharSequence charSequence, CharSequence charSequence2, Drawable.ConstantState constantState) {
            str.getClass();
            charSequence.getClass();
            charSequence2.getClass();
            return new ActiveMedia(str, charSequence, charSequence2, constantState);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ActiveMedia)) {
                return false;
            }
            ActiveMedia activeMedia = (ActiveMedia) obj;
            return ph0.i(this.packageName, activeMedia.packageName) && ph0.i(this.title, activeMedia.title) && ph0.i(this.artist, activeMedia.artist) && ph0.i(this.appIconState, activeMedia.appIconState);
        }

        public final Drawable.ConstantState getAppIconState() {
            return this.appIconState;
        }

        public final CharSequence getArtist() {
            return this.artist;
        }

        public final String getPackageName() {
            return this.packageName;
        }

        public final CharSequence getTitle() {
            return this.title;
        }

        public int hashCode() {
            int iHashCode = (this.artist.hashCode() + ((this.title.hashCode() + (this.packageName.hashCode() * 31)) * 31)) * 31;
            Drawable.ConstantState constantState = this.appIconState;
            return iHashCode + (constantState == null ? 0 : constantState.hashCode());
        }

        public String toString() {
            String str = this.packageName;
            CharSequence charSequence = this.title;
            CharSequence charSequence2 = this.artist;
            return "ActiveMedia(packageName=" + str + ", title=" + ((Object) charSequence) + ", artist=" + ((Object) charSequence2) + ", appIconState=" + this.appIconState + ")";
        }
    }

    public static final class AffordanceLayerState {
        private final Drawable background;
        private final int backgroundColor;
        private final PorterDuffColorFilter backgroundFilter;
        private final Drawable foreground;
        private final int foregroundColor;
        private final PorterDuffColorFilter foregroundFilter;
        private final Drawable source;

        public AffordanceLayerState(Drawable drawable, Drawable drawable2, Drawable drawable3, int i, int i2, PorterDuffColorFilter porterDuffColorFilter, PorterDuffColorFilter porterDuffColorFilter2) {
            drawable.getClass();
            drawable2.getClass();
            drawable3.getClass();
            porterDuffColorFilter.getClass();
            porterDuffColorFilter2.getClass();
            this.source = drawable;
            this.foreground = drawable2;
            this.background = drawable3;
            this.foregroundColor = i;
            this.backgroundColor = i2;
            this.foregroundFilter = porterDuffColorFilter;
            this.backgroundFilter = porterDuffColorFilter2;
        }

        public static AffordanceLayerState copy$default(AffordanceLayerState affordanceLayerState, Drawable drawable, Drawable drawable2, Drawable drawable3, int i, int i2, PorterDuffColorFilter porterDuffColorFilter, PorterDuffColorFilter porterDuffColorFilter2, int i3, Object obj) {
            if ((i3 & 1) != 0) {
                drawable = affordanceLayerState.source;
            }
            if ((i3 & PixelLockscreenClockHook.UI_STATE_KEYGUARD) != 0) {
                drawable2 = affordanceLayerState.foreground;
            }
            if ((i3 & PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND) != 0) {
                drawable3 = affordanceLayerState.background;
            }
            if ((i3 & 8) != 0) {
                i = affordanceLayerState.foregroundColor;
            }
            if ((i3 & 16) != 0) {
                i2 = affordanceLayerState.backgroundColor;
            }
            if ((i3 & 32) != 0) {
                porterDuffColorFilter = affordanceLayerState.foregroundFilter;
            }
            if ((i3 & 64) != 0) {
                porterDuffColorFilter2 = affordanceLayerState.backgroundFilter;
            }
            PorterDuffColorFilter porterDuffColorFilter3 = porterDuffColorFilter;
            PorterDuffColorFilter porterDuffColorFilter4 = porterDuffColorFilter2;
            int i4 = i2;
            Drawable drawable4 = drawable3;
            return affordanceLayerState.copy(drawable, drawable2, drawable4, i, i4, porterDuffColorFilter3, porterDuffColorFilter4);
        }

        public final Drawable component1() {
            return this.source;
        }

        public final Drawable component2() {
            return this.foreground;
        }

        public final Drawable component3() {
            return this.background;
        }

        public final int component4() {
            return this.foregroundColor;
        }

        public final int component5() {
            return this.backgroundColor;
        }

        public final PorterDuffColorFilter component6() {
            return this.foregroundFilter;
        }

        public final PorterDuffColorFilter component7() {
            return this.backgroundFilter;
        }

        public final AffordanceLayerState copy(Drawable drawable, Drawable drawable2, Drawable drawable3, int i, int i2, PorterDuffColorFilter porterDuffColorFilter, PorterDuffColorFilter porterDuffColorFilter2) {
            drawable.getClass();
            drawable2.getClass();
            drawable3.getClass();
            porterDuffColorFilter.getClass();
            porterDuffColorFilter2.getClass();
            return new AffordanceLayerState(drawable, drawable2, drawable3, i, i2, porterDuffColorFilter, porterDuffColorFilter2);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AffordanceLayerState)) {
                return false;
            }
            AffordanceLayerState affordanceLayerState = (AffordanceLayerState) obj;
            return ph0.i(this.source, affordanceLayerState.source) && ph0.i(this.foreground, affordanceLayerState.foreground) && ph0.i(this.background, affordanceLayerState.background) && this.foregroundColor == affordanceLayerState.foregroundColor && this.backgroundColor == affordanceLayerState.backgroundColor && ph0.i(this.foregroundFilter, affordanceLayerState.foregroundFilter) && ph0.i(this.backgroundFilter, affordanceLayerState.backgroundFilter);
        }

        public final Drawable getBackground() {
            return this.background;
        }

        public final int getBackgroundColor() {
            return this.backgroundColor;
        }

        public final PorterDuffColorFilter getBackgroundFilter() {
            return this.backgroundFilter;
        }

        public final Drawable getForeground() {
            return this.foreground;
        }

        public final int getForegroundColor() {
            return this.foregroundColor;
        }

        public final PorterDuffColorFilter getForegroundFilter() {
            return this.foregroundFilter;
        }

        public final Drawable getSource() {
            return this.source;
        }

        public int hashCode() {
            return this.backgroundFilter.hashCode() + ((this.foregroundFilter.hashCode() + x30.d(this.backgroundColor, x30.d(this.foregroundColor, (this.background.hashCode() + ((this.foreground.hashCode() + (this.source.hashCode() * 31)) * 31)) * 31, 31), 31)) * 31);
        }

        public String toString() {
            return "AffordanceLayerState(source=" + this.source + ", foreground=" + this.foreground + ", background=" + this.background + ", foregroundColor=" + this.foregroundColor + ", backgroundColor=" + this.backgroundColor + ", foregroundFilter=" + this.foregroundFilter + ", backgroundFilter=" + this.backgroundFilter + ")";
        }
    }

    public static final class AffordanceResourceIds {
        private final int background;
        private final int darkBackgroundColor;
        private final int darkForegroundColor;
        private final int foreground;
        private final int lightBackgroundColor;
        private final int lightForegroundColor;
        private final int stroke;

        public AffordanceResourceIds(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
            this.foreground = i;
            this.background = i2;
            this.stroke = i3;
            this.lightBackgroundColor = i4;
            this.darkBackgroundColor = i5;
            this.lightForegroundColor = i6;
            this.darkForegroundColor = i7;
        }

        public static AffordanceResourceIds copy$default(AffordanceResourceIds affordanceResourceIds, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, Object obj) {
            if ((i8 & 1) != 0) {
                i = affordanceResourceIds.foreground;
            }
            if ((i8 & PixelLockscreenClockHook.UI_STATE_KEYGUARD) != 0) {
                i2 = affordanceResourceIds.background;
            }
            if ((i8 & PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND) != 0) {
                i3 = affordanceResourceIds.stroke;
            }
            if ((i8 & 8) != 0) {
                i4 = affordanceResourceIds.lightBackgroundColor;
            }
            if ((i8 & 16) != 0) {
                i5 = affordanceResourceIds.darkBackgroundColor;
            }
            if ((i8 & 32) != 0) {
                i6 = affordanceResourceIds.lightForegroundColor;
            }
            if ((i8 & 64) != 0) {
                i7 = affordanceResourceIds.darkForegroundColor;
            }
            int i9 = i6;
            int i10 = i7;
            int i11 = i5;
            int i12 = i3;
            return affordanceResourceIds.copy(i, i2, i12, i4, i11, i9, i10);
        }

        public final int component1() {
            return this.foreground;
        }

        public final int component2() {
            return this.background;
        }

        public final int component3() {
            return this.stroke;
        }

        public final int component4() {
            return this.lightBackgroundColor;
        }

        public final int component5() {
            return this.darkBackgroundColor;
        }

        public final int component6() {
            return this.lightForegroundColor;
        }

        public final int component7() {
            return this.darkForegroundColor;
        }

        public final AffordanceResourceIds copy(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
            return new AffordanceResourceIds(i, i2, i3, i4, i5, i6, i7);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AffordanceResourceIds)) {
                return false;
            }
            AffordanceResourceIds affordanceResourceIds = (AffordanceResourceIds) obj;
            return this.foreground == affordanceResourceIds.foreground && this.background == affordanceResourceIds.background && this.stroke == affordanceResourceIds.stroke && this.lightBackgroundColor == affordanceResourceIds.lightBackgroundColor && this.darkBackgroundColor == affordanceResourceIds.darkBackgroundColor && this.lightForegroundColor == affordanceResourceIds.lightForegroundColor && this.darkForegroundColor == affordanceResourceIds.darkForegroundColor;
        }

        public final int getBackground() {
            return this.background;
        }

        public final int getDarkBackgroundColor() {
            return this.darkBackgroundColor;
        }

        public final int getDarkForegroundColor() {
            return this.darkForegroundColor;
        }

        public final int getForeground() {
            return this.foreground;
        }

        public final int getLightBackgroundColor() {
            return this.lightBackgroundColor;
        }

        public final int getLightForegroundColor() {
            return this.lightForegroundColor;
        }

        public final int getStroke() {
            return this.stroke;
        }

        public int hashCode() {
            return Integer.hashCode(this.darkForegroundColor) + x30.d(this.lightForegroundColor, x30.d(this.darkBackgroundColor, x30.d(this.lightBackgroundColor, x30.d(this.stroke, x30.d(this.background, Integer.hashCode(this.foreground) * 31, 31), 31), 31), 31), 31);
        }

        public String toString() {
            int i = this.foreground;
            int i2 = this.background;
            int i3 = this.stroke;
            int i4 = this.lightBackgroundColor;
            int i5 = this.darkBackgroundColor;
            int i6 = this.lightForegroundColor;
            int i7 = this.darkForegroundColor;
            StringBuilder sbS = x30.s("AffordanceResourceIds(foreground=", i, ", background=", i2, ", stroke=");
            x30.A(sbS, i3, ", lightBackgroundColor=", i4, ", darkBackgroundColor=");
            x30.A(sbS, i5, ", lightForegroundColor=", i6, ", darkForegroundColor=");
            sbS.append(i7);
            sbS.append(")");
            return sbS.toString();
        }
    }

    public final class AodMediaMonitor {
        private final Context context;
        private final PixelLockscreenClockHook$AodMediaMonitor$controllerCallback$1 controllerCallback;
        private List<MediaController> controllers;
        private final MediaSessionManager manager;
        private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChangedListener;
        final PixelLockscreenClockHook this$0;

        /* JADX WARN: Type inference failed for: r1v5, types: [one.dot.couiexpressive.hooks.systemui.PixelLockscreenClockHook$AodMediaMonitor$controllerCallback$1] */
        public AodMediaMonitor(PixelLockscreenClockHook pixelLockscreenClockHook, Context context) {
            context.getClass();
            this.this$0 = pixelLockscreenClockHook;
            this.context = context;
            this.manager = (MediaSessionManager) context.getSystemService(MediaSessionManager.class);
            this.controllers = j20.d;
            this.controllerCallback = new MediaController.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadata mediaMetadata) {
                    this.this$0.refreshMediaState();
                }

                @Override
                public void onPlaybackStateChanged(PlaybackState playbackState) {
                    this.this$0.refreshMediaState();
                }

                @Override
                public void onSessionDestroyed() {
                    this.this$0.refreshControllers();
                }
            };
            i iVar = new i();
            iVar.a = this;
            VarHandle.storeStoreFence();
            this.sessionsChangedListener = iVar;
        }

        public final void refreshControllers() {
            Object objF;
            boolean zI = ph0.i(Looper.myLooper(), Looper.getMainLooper());
            PixelLockscreenClockHook pixelLockscreenClockHook = this.this$0;
            if (!zI) {
                Handler handler = pixelLockscreenClockHook.mainHandler;
                h hVar = new h(1);
                hVar.e = this;
                VarHandle.storeStoreFence();
                handler.post(hVar);
                return;
            }
            try {
                MediaSessionManager mediaSessionManager = this.manager;
                List<MediaController> activeSessions = mediaSessionManager != null ? mediaSessionManager.getActiveSessions(null) : null;
                if (activeSessions == null) {
                    activeSessions = j20.d;
                }
                List<MediaController> list = this.controllers;
                ArrayList arrayList = new ArrayList();
                for (Object obj : list) {
                    if (!activeSessions.contains((MediaController) obj)) {
                        arrayList.add(obj);
                    }
                }
                int size = arrayList.size();
                int i = 0;
                int i2 = 0;
                while (i2 < size) {
                    Object obj2 = arrayList.get(i2);
                    i2++;
                    ((MediaController) obj2).unregisterCallback(this.controllerCallback);
                }
                List<MediaController> list2 = this.controllers;
                ArrayList arrayList2 = new ArrayList();
                for (Object obj3 : activeSessions) {
                    if (!list2.contains((MediaController) obj3)) {
                        arrayList2.add(obj3);
                    }
                }
                int size2 = arrayList2.size();
                while (i < size2) {
                    Object obj4 = arrayList2.get(i);
                    i++;
                    ((MediaController) obj4).registerCallback(this.controllerCallback, pixelLockscreenClockHook.mainHandler);
                }
                this.controllers = activeSessions;
                refreshMediaState();
                objF = l22.a;
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            PixelLockscreenClockHook pixelLockscreenClockHook2 = this.this$0;
            Throwable thA = tc1.a(objF);
            if (thA != null) {
                pixelLockscreenClockHook2.logFailure("active-media-controller-refresh", thA);
            }
        }

        public final void refreshMediaState() {
            Object objF;
            Object next;
            ActiveMedia activeMedia;
            CharSequence text;
            CharSequence text2;
            Object objF2;
            Drawable.ConstantState constantState;
            Object objF3;
            boolean zI = ph0.i(Looper.myLooper(), Looper.getMainLooper());
            PixelLockscreenClockHook pixelLockscreenClockHook = this.this$0;
            int i = 0;
            if (!zI) {
                Handler handler = pixelLockscreenClockHook.mainHandler;
                h hVar = new h(i);
                hVar.e = this;
                VarHandle.storeStoreFence();
                handler.post(hVar);
                return;
            }
            try {
                Iterator<T> it = this.controllers.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        next = null;
                        break;
                    }
                    next = it.next();
                    PlaybackState playbackState = ((MediaController) next).getPlaybackState();
                    if (playbackState != null && playbackState.getState() == 3) {
                        break;
                    }
                }
                MediaController mediaController = (MediaController) next;
                if (mediaController != null) {
                    MediaMetadata metadata = mediaController.getMetadata();
                    if (metadata == null || (text = metadata.getText("android.media.metadata.TITLE")) == null) {
                        text = metadata != null ? metadata.getText("android.media.metadata.DISPLAY_TITLE") : null;
                        if (text == null) {
                            text = mediaController.getPackageName();
                        }
                    }
                    if (metadata == null || (text2 = metadata.getText("android.media.metadata.ARTIST")) == null) {
                        text2 = metadata != null ? metadata.getText("android.media.metadata.ALBUM_ARTIST") : null;
                        if (text2 == null) {
                            try {
                                objF2 = this.context.getPackageManager().getApplicationLabel(this.context.getPackageManager().getApplicationInfo(mediaController.getPackageName(), 0));
                            } catch (Throwable th) {
                                objF2 = dr.f(th);
                            }
                            Object packageName = mediaController.getPackageName();
                            if (objF2 instanceof sc1) {
                                objF2 = packageName;
                            }
                            text2 = (CharSequence) objF2;
                        }
                    }
                    synchronized (pixelLockscreenClockHook.mediaNotificationIconStates) {
                        constantState = (Drawable.ConstantState) pixelLockscreenClockHook.mediaNotificationIconStates.get(mediaController.getPackageName());
                    }
                    if (constantState == null) {
                        try {
                            objF3 = this.context.getPackageManager().getApplicationIcon(mediaController.getPackageName()).getConstantState();
                        } catch (Throwable th2) {
                            objF3 = dr.f(th2);
                        }
                        if (objF3 instanceof sc1) {
                            objF3 = null;
                        }
                        constantState = (Drawable.ConstantState) objF3;
                    }
                    String packageName2 = mediaController.getPackageName();
                    packageName2.getClass();
                    text.getClass();
                    text2.getClass();
                    activeMedia = new ActiveMedia(packageName2, text, text2, constantState);
                } else {
                    activeMedia = null;
                }
                pixelLockscreenClockHook.activeMediaController = mediaController != null ? new WeakReference(mediaController) : null;
                if (!ph0.i(pixelLockscreenClockHook.activeMedia, activeMedia)) {
                    pixelLockscreenClockHook.activeMedia = activeMedia;
                    pixelLockscreenClockHook.log("active playing media=" + (activeMedia != null));
                    PixelLockscreenClockHook.refreshActiveAodContent$default(pixelLockscreenClockHook, true, false, PixelLockscreenClockHook.UI_STATE_KEYGUARD, null);
                }
                objF = l22.a;
            } catch (Throwable th3) {
                objF = dr.f(th3);
            }
            PixelLockscreenClockHook pixelLockscreenClockHook2 = this.this$0;
            Throwable thA = tc1.a(objF);
            if (thA != null) {
                pixelLockscreenClockHook2.logFailure("active-media-state-refresh", thA);
            }
        }

        public final void start() {
            Object objF;
            MediaSessionManager mediaSessionManager = this.manager;
            if (mediaSessionManager == null) {
                return;
            }
            PixelLockscreenClockHook pixelLockscreenClockHook = this.this$0;
            try {
                mediaSessionManager.addOnActiveSessionsChangedListener(this.sessionsChangedListener, null, pixelLockscreenClockHook.mainHandler);
                refreshControllers();
                pixelLockscreenClockHook.log("AOD active-media source installed");
                objF = l22.a;
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            PixelLockscreenClockHook pixelLockscreenClockHook2 = this.this$0;
            Throwable thA = tc1.a(objF);
            if (thA != null) {
                pixelLockscreenClockHook2.logFailure("active-media-monitor", thA);
            }
        }
    }

    public static final class NewRenderNotificationIconBridge {
        private final p80 onUpdate;
        private final Object originalObserver;

        public NewRenderNotificationIconBridge(Object obj, p80 p80Var) {
            obj.getClass();
            p80Var.getClass();
            this.originalObserver = obj;
            this.onUpdate = p80Var;
        }

        public final void updateLockScreenNotificationIconData(String[] strArr, Drawable[] drawableArr) {
            strArr.getClass();
            drawableArr.getClass();
            this.onUpdate.f(strArr, drawableArr);
            try {
                XposedHelpers.callMethod(this.originalObserver, "updateLockScreenNotificationIconData", new Object[]{new String[0], new Drawable[0]});
            } catch (Throwable th) {
                dr.f(th);
            }
        }
    }

    public static final class OplusRootState {
        private final boolean animate;
        private final int clockSizeState;
        private final int uiState;

        public OplusRootState(int i, int i2, boolean z) {
            this.uiState = i;
            this.clockSizeState = i2;
            this.animate = z;
        }

        public static OplusRootState copy$default(OplusRootState oplusRootState, int i, int i2, boolean z, int i3, Object obj) {
            if ((i3 & 1) != 0) {
                i = oplusRootState.uiState;
            }
            if ((i3 & PixelLockscreenClockHook.UI_STATE_KEYGUARD) != 0) {
                i2 = oplusRootState.clockSizeState;
            }
            if ((i3 & PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND) != 0) {
                z = oplusRootState.animate;
            }
            return oplusRootState.copy(i, i2, z);
        }

        public final int component1() {
            return this.uiState;
        }

        public final int component2() {
            return this.clockSizeState;
        }

        public final boolean component3() {
            return this.animate;
        }

        public final OplusRootState copy(int i, int i2, boolean z) {
            return new OplusRootState(i, i2, z);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OplusRootState)) {
                return false;
            }
            OplusRootState oplusRootState = (OplusRootState) obj;
            return this.uiState == oplusRootState.uiState && this.clockSizeState == oplusRootState.clockSizeState && this.animate == oplusRootState.animate;
        }

        public final boolean getAnimate() {
            return this.animate;
        }

        public final int getClockSizeState() {
            return this.clockSizeState;
        }

        public final int getUiState() {
            return this.uiState;
        }

        public int hashCode() {
            return Boolean.hashCode(this.animate) + x30.d(this.clockSizeState, Integer.hashCode(this.uiState) * 31, 31);
        }

        public String toString() {
            int i = this.uiState;
            int i2 = this.clockSizeState;
            boolean z = this.animate;
            StringBuilder sbS = x30.s("OplusRootState(uiState=", i, ", clockSizeState=", i2, ", animate=");
            sbS.append(z);
            sbS.append(")");
            return sbS.toString();
        }
    }

    public static final class OriginalVisualState {
        private WeakReference<View> informationView;
        private WeakReference<View> timeView;

        private final void suppress(View view) {
            if (view.getAlpha() == 0.0f) {
                return;
            }
            view.setAlpha(0.0f);
        }

        public final void enforce() {
            View view;
            View view2;
            WeakReference<View> weakReference = this.timeView;
            if (weakReference != null && (view2 = weakReference.get()) != null) {
                suppress(view2);
            }
            WeakReference<View> weakReference2 = this.informationView;
            if (weakReference2 == null || (view = weakReference2.get()) == null) {
                return;
            }
            suppress(view);
        }

        public final void update(int i, View view) {
            view.getClass();
            WeakReference<View> weakReference = new WeakReference<>(view);
            if (i == 1) {
                this.timeView = weakReference;
            } else {
                this.informationView = weakReference;
            }
            suppress(view);
        }
    }

    public static final class PanoramicSyncRequest {
        private final boolean animate;
        private final boolean normalizedEntry;

        public PanoramicSyncRequest(boolean z, boolean z2) {
            this.normalizedEntry = z;
            this.animate = z2;
        }

        public static PanoramicSyncRequest copy$default(PanoramicSyncRequest panoramicSyncRequest, boolean z, boolean z2, int i, Object obj) {
            if ((i & 1) != 0) {
                z = panoramicSyncRequest.normalizedEntry;
            }
            if ((i & PixelLockscreenClockHook.UI_STATE_KEYGUARD) != 0) {
                z2 = panoramicSyncRequest.animate;
            }
            return panoramicSyncRequest.copy(z, z2);
        }

        public final boolean component1() {
            return this.normalizedEntry;
        }

        public final boolean component2() {
            return this.animate;
        }

        public final PanoramicSyncRequest copy(boolean z, boolean z2) {
            return new PanoramicSyncRequest(z, z2);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PanoramicSyncRequest)) {
                return false;
            }
            PanoramicSyncRequest panoramicSyncRequest = (PanoramicSyncRequest) obj;
            return this.normalizedEntry == panoramicSyncRequest.normalizedEntry && this.animate == panoramicSyncRequest.animate;
        }

        public final boolean getAnimate() {
            return this.animate;
        }

        public final boolean getNormalizedEntry() {
            return this.normalizedEntry;
        }

        public int hashCode() {
            return Boolean.hashCode(this.animate) + (Boolean.hashCode(this.normalizedEntry) * 31);
        }

        public String toString() {
            return "PanoramicSyncRequest(normalizedEntry=" + this.normalizedEntry + ", animate=" + this.animate + ")";
        }
    }

    public static final class AnonymousClass1 extends XC_MethodHook {
        public AnonymousClass1() {
        }

        public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            methodHookParam.getClass();
            Object obj = methodHookParam.thisObject;
            ImageView imageView = obj instanceof ImageView ? (ImageView) obj : null;
            if (imageView == null) {
                return;
            }
            PixelLockscreenClockHook.this.prepareAffordanceView(imageView);
            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
            l4 l4Var = new l4(7);
            l4Var.e = pixelLockscreenClockHook;
            l4Var.f = imageView;
            VarHandle.storeStoreFence();
            imageView.post(l4Var);
        }
    }

    public static final class C00561 extends XC_MethodHook {
        public C00561() {
        }

        public static final void afterHookedMethod$lambda$2$0(ImageView imageView, C00561 c00561, PixelLockscreenClockHook pixelLockscreenClockHook) {
            Object objF;
            if (imageView.isAttachedToWindow()) {
                try {
                    PixelLockscreenClockHook.styleAffordanceView$default(pixelLockscreenClockHook, imageView, false, PixelLockscreenClockHook.UI_STATE_KEYGUARD, null);
                    objF = l22.a;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                Throwable thA = tc1.a(objF);
                if (thA != null) {
                    pixelLockscreenClockHook.logFailure("affordance-palette-refresh", thA);
                }
            }
        }

        public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            List<ImageView> listF0;
            methodHookParam.getClass();
            Object obj = methodHookParam.thisObject;
            Application application = obj instanceof Application ? (Application) obj : null;
            if (application != null) {
                PixelLockscreenClockHook.this.systemUiPaletteContext = new WeakReference(application);
            }
            Map map = PixelLockscreenClockHook.this.affordanceViews;
            map.getClass();
            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
            synchronized (map) {
                listF0 = uk.F0(pixelLockscreenClockHook.affordanceViews.keySet());
            }
            PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
            for (ImageView imageView : listF0) {
                w7 w7Var = new w7(PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND);
                w7Var.e = imageView;
                w7Var.f = this;
                w7Var.g = pixelLockscreenClockHook2;
                VarHandle.storeStoreFence();
                imageView.post(w7Var);
            }
            sb0 sb0Var = vb0.a;
            vb0.a(PixelLockscreenClockHook.TAG, "affordance-palette-refresh", "scheduled=" + listF0.size());
        }
    }

    public static final class C00634 extends XC_MethodHook {
        public C00634() {
        }

        public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            methodHookParam.getClass();
            Object obj = methodHookParam.thisObject;
            ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
            if (viewGroup == null) {
                return;
            }
            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
            y51 y51Var = new y51(3);
            y51Var.e = pixelLockscreenClockHook;
            y51Var.f = viewGroup;
            VarHandle.storeStoreFence();
            viewGroup.post(y51Var);
        }
    }

    public static final class C00652 extends XC_MethodHook {
        public C00652() {
        }

        public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            methodHookParam.getClass();
            Object obj = methodHookParam.thisObject;
            ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
            if (viewGroup == null) {
                return;
            }
            PixelLockscreenClockHook.this.nativeAodNotificationLayout = new WeakReference(viewGroup);
            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
            y51 y51Var = new y51(PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND);
            y51Var.e = pixelLockscreenClockHook;
            y51Var.f = viewGroup;
            VarHandle.storeStoreFence();
            viewGroup.post(y51Var);
        }
    }

    public static final class C00662 extends XC_MethodHook {
        public C00662() {
        }

        public static final l22 beforeHookedMethod$lambda$1(PixelLockscreenClockHook pixelLockscreenClockHook, Context context, String[] strArr, Drawable[] drawableArr) {
            strArr.getClass();
            drawableArr.getClass();
            pixelLockscreenClockHook.updateNewRenderAodNotificationIcons(context, strArr, drawableArr);
            return l22.a;
        }

        public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
            Object objF;
            methodHookParam.getClass();
            Object[] objArr = methodHookParam.args;
            objArr.getClass();
            Object obj = objArr.length == 0 ? null : objArr[0];
            if (obj == null) {
                return;
            }
            PixelLockscreenClockHook.this.newRenderOriginalObserver = new WeakReference(obj);
            try {
                Object objectField = XposedHelpers.getObjectField(methodHookParam.thisObject, "mContext");
                objectField.getClass();
                objF = (Context) objectField;
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Context context = (Context) (objF instanceof sc1 ? null : objF);
            if (context == null) {
                return;
            }
            Object[] objArr2 = methodHookParam.args;
            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
            dd0 dd0Var = new dd0(18);
            dd0Var.e = pixelLockscreenClockHook;
            dd0Var.f = context;
            VarHandle.storeStoreFence();
            objArr2[0] = new NewRenderNotificationIconBridge(obj, dd0Var);
            PixelLockscreenClockHook.this.log("new-render AOD notification observer replaced: ".concat(obj.getClass().getName()));
        }
    }

    static {
        Set<ClassLoader> setSynchronizedSet = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap()));
        setSynchronizedSet.getClass();
        hookedPluginLoaders = setSynchronizedSet;
        Set<Class<?>> setSynchronizedSet2 = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap()));
        setSynchronizedSet2.getClass();
        hookedAffordanceClasses = setSynchronizedSet2;
        Set<Class<?>> setSynchronizedSet3 = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap()));
        setSynchronizedSet3.getClass();
        hookedKeyguardSleepBinderClasses = setSynchronizedSet3;
        grayscaleIconCache = x30.u();
        ColorStateList colorStateListValueOf = ColorStateList.valueOf(-1);
        colorStateListValueOf.getClass();
        WHITE_TINT = colorStateListValueOf;
        OPLUS_LOCKSCREEN_NOTIFICATION_UPDATE_COROUTINES = vj1.q("com.oplus.systemui.statusbar.C13350xb7474f48", "com.oplus.systemui.statusbar.C13351xb7475309");
        NOTIFICATION_ENTRY_ICON_VIEW_METHODS = new String[]{"getStatusBarIcon", "getShelfIcon", "getAodIcon"};
        NATIVE_AOD_NOTIFICATION_REFRESH_METHODS = vj1.q("onReceiveNotification", "updateCurrentNotification");
        NOTIFICATION_PIPELINE_REFRESH_METHODS = vj1.q("onNotificationPosted", "onNotificationRemoved", "onNotificationRankingUpdate", "onNotificationsInitialized");
        AOD_NOTIFICATION_SYNC_DELAYS = new long[]{0, AOD_CONTENT_COMMIT_DELAY_MS, 120, 350};
        NATIVE_AOD_NOTIFICATION_SYNC_DELAYS = new long[]{0, 48, 160, 420};
        LOCKSCREEN_NOTIFICATION_SYNC_DELAYS = new long[]{0, AOD_CONTENT_COMMIT_DELAY_MS, 120};
        KEYGUARD_SERVICE_BINDER_CLASSES = new String[]{"com.android.systemui.keyguard.KeyguardService$3", "com.android.systemui.keyguard.KeyguardService$1"};
        AFFORDANCE_VIEW_CLASSES = vj1.q("com.oplus.keyguard.ui.base.widget.OplusLaunchableImageView", "com.oplus.systemui.keyguard.view.OplusLaunchableImageView");
        AFFORDANCE_REFRESH_METHODS = vj1.q("updateIconColor$1", "updateIconColor", "onColorChanged", "setCircleRadius", "setCircleRadiusWithoutAnimation", "setImageDrawable", "setImageResource");
        RESOURCE_PACKAGES = new String[]{SYSTEM_UI_PACKAGE, "com.oplus.systemui", "com.oplus.keyguard"};
        SUPPRESSION_RETRY_DELAYS = new long[]{0, 80, 200, 500, 1000};
    }

    public PixelLockscreenClockHook() {
        j20 j20Var = j20.d;
        this.oplusAodNotificationIcons = j20Var;
        this.oplusAodNotificationKeys = j20Var;
        this.nativeAodNotificationIcons = j20Var;
        this.newRenderAodNotificationIcons = j20Var;
        this.partialAodStates = Collections.synchronizedMap(new WeakHashMap());
        this.aodModeStates = Collections.synchronizedMap(new WeakHashMap());
        this.mediaNotificationIconStates = new LinkedHashMap();
        this.affordanceViews = Collections.synchronizedMap(new WeakHashMap());
        this.pendingSleepOriginLatchedAt = Long.MIN_VALUE;
    }

    public static final void access$captureNativeAodNotificationLayout(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup) {
        pixelLockscreenClockHook.captureNativeAodNotificationLayout(viewGroup);
    }

    public static final void access$ensureDimOverlay(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup) {
        pixelLockscreenClockHook.ensureDimOverlay(viewGroup);
    }

    private final Context affordancePaletteContext(View view) {
        Context context;
        WeakReference<Context> weakReference = this.systemUiPaletteContext;
        if (weakReference != null && (context = weakReference.get()) != null) {
            return context;
        }
        Application applicationCurrentApplication = AndroidAppHelper.currentApplication();
        if (applicationCurrentApplication == null || !ph0.i(applicationCurrentApplication.getPackageName(), SYSTEM_UI_PACKAGE)) {
            applicationCurrentApplication = null;
        }
        if (applicationCurrentApplication != null) {
            this.systemUiPaletteContext = new WeakReference<>(applicationCurrentApplication);
            return applicationCurrentApplication;
        }
        Context context2 = view.getContext();
        context2.getClass();
        return context2;
    }

    private final AffordanceResourceIds affordanceResourceIds(View view) {
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(view, AFFORDANCE_RESOURCE_IDS_FIELD);
        AffordanceResourceIds affordanceResourceIds = additionalInstanceField instanceof AffordanceResourceIds ? (AffordanceResourceIds) additionalInstanceField : null;
        if (affordanceResourceIds != null) {
            return affordanceResourceIds;
        }
        AffordanceResourceIds affordanceResourceIds2 = new AffordanceResourceIds(resourceId(view, "affordance_foreground_icon"), resourceId(view, "affordance_background_circle"), resourceId(view, "affordance_background_circle_stroke"), view.getResources().getIdentifier(UDFPS_LIGHT_BACKGROUND, "color", "android"), view.getResources().getIdentifier(UDFPS_DARK_BACKGROUND, "color", "android"), view.getResources().getIdentifier(UDFPS_LIGHT_FOREGROUND, "color", "android"), view.getResources().getIdentifier(UDFPS_DARK_FOREGROUND, "color", "android"));
        XposedHelpers.setAdditionalInstanceField(view, AFFORDANCE_RESOURCE_IDS_FIELD, affordanceResourceIds2);
        return affordanceResourceIds2;
    }

    public final void applyAuthoritativePanoramicState(ViewGroup viewGroup, Object obj) {
        OplusRootState oplusRootState;
        PixelClockHostView.Scene sceneSceneForClockSize;
        PixelClockHostView pixelClockHostView;
        PanoramicSyncRequest panoramicSyncRequestRemove;
        if (obj == null || (oplusRootState = readOplusRootState(obj)) == null || oplusRootState.getUiState() != UI_STATE_PANORAMIC_AOD || (sceneSceneForClockSize = sceneForClockSize(Integer.valueOf(oplusRootState.getClockSizeState()))) == null || (pixelClockHostView = hosts.get(viewGroup)) == null) {
            return;
        }
        Map<ViewGroup, PanoramicSyncRequest> map = pendingPanoramicSyncs;
        synchronized (map) {
            panoramicSyncRequestRemove = map.remove(viewGroup);
        }
        if (panoramicSyncRequestRemove != null || pixelClockHostView.isPanoramicAodActive()) {
            if (panoramicSyncRequestRemove == null || !panoramicSyncRequestRemove.getNormalizedEntry()) {
                pixelClockHostView.setPresentation(sceneSceneForClockSize, true, false, PixelClockHostView.AodContent.None.INSTANCE, (panoramicSyncRequestRemove != null ? panoramicSyncRequestRemove.getAnimate() : true) && oplusRootState.getAnimate());
                return;
            }
            pixelClockHostView.beginAodEntry(sceneSceneForClockSize, false, PixelClockHostView.AodContent.None.INSTANCE, panoramicSyncRequestRemove.getAnimate() && oplusRootState.getAnimate());
            log("panoramic AOD normalized from OPlus final state: scene=" + sceneSceneForClockSize);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0, types: [j20] */
    /* JADX WARN: Type inference failed for: r1v1, types: [java.lang.Object, java.util.List, java.util.List<one.dot.couiexpressive.hooks.systemui.PixelClockHostView$NotificationIcon>] */
    /* JADX WARN: Type inference failed for: r1v3, types: [java.util.ArrayList] */
    private final void applyNewRenderAodNotificationIcons(Context context, List<String> list, List<? extends Drawable> list2, int i, boolean z) {
        Object objF;
        ?? arrayList;
        try {
            boolean zIsAodNotificationDisplayEnabled = isAodNotificationDisplayEnabled(context);
            if (zIsAodNotificationDisplayEnabled) {
                List listB0 = uk.B0(7, list2);
                arrayList = new ArrayList();
                Iterator it = listB0.iterator();
                while (it.hasNext()) {
                    PixelClockHostView.NotificationIcon notificationIconNotificationIconSnapshot = notificationIconSnapshot((Drawable) it.next(), context);
                    if (notificationIconNotificationIconSnapshot != null) {
                        arrayList.add(notificationIconNotificationIconSnapshot);
                    }
                }
            } else {
                arrayList = j20.d;
            }
            this.newRenderAodNotificationSourceReady = true;
            if (!zIsAodNotificationDisplayEnabled) {
                i = 0;
            }
            if (!ph0.i(this.newRenderAodNotificationIcons, arrayList) || this.newRenderAodNotificationEntryCount != i) {
                this.newRenderAodNotificationIcons = arrayList;
                this.newRenderAodNotificationEntryCount = i;
                log("new-render OPlus AOD notification icons=" + arrayList.size() + ", packages=" + uk.B0(arrayList.size(), list));
                refreshActiveAodContent(true, z);
            }
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            logFailure("new-render-aod-notification-snapshot", thA);
        }
    }

    /* JADX WARN: Code duplicated, block: B:109:0x01b9  */
    /* JADX WARN: Code duplicated, block: B:134:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:85:0x012d A[PHI: r0
  0x012d: PHI (r0v28 java.lang.String) = (r0v27 java.lang.String), (r0v33 java.lang.String) binds: [B:69:0x00e3, B:73:0x00ed] A[DONT_GENERATE, DONT_INLINE]] */
    private final void applyOplusAodNotificationEntries(Collection<?> collection, boolean z) {
        Object objF;
        Throwable thA;
        Context context;
        int i;
        Object objF2;
        Object objF3;
        char c;
        String packageName;
        Object objF4;
        try {
            WeakReference<Context> weakReference = this.notificationContext;
            if (weakReference != null && (context = weakReference.get()) != null) {
                List<Object> listFlattenOplusAodNotificationEntries = isAodNotificationDisplayEnabled(context) ? flattenOplusAodNotificationEntries(collection) : j20.d;
                ArrayList arrayList = new ArrayList();
                Iterator it = listFlattenOplusAodNotificationEntries.iterator();
                while (true) {
                    i = 0;
                    if (!it.hasNext()) {
                        break;
                    }
                    Object next = it.next();
                    try {
                        Object objCallMethod = XposedHelpers.callMethod(next, "getSbn", new Object[0]);
                        objF4 = objCallMethod instanceof StatusBarNotification ? (StatusBarNotification) objCallMethod : null;
                    } catch (Throwable th) {
                        objF4 = dr.f(th);
                    }
                    StatusBarNotification statusBarNotification = (StatusBarNotification) (objF4 instanceof sc1 ? null : objF4);
                    if (statusBarNotification != null && (statusBarNotification.getNotification().flags & 64) == 0 && !ph0.i(statusBarNotification.getPackageName(), OPLUS_AOD_EXCLUDED_PACKAGE)) {
                        arrayList.add(next);
                    }
                    objF = dr.f(th);
                    thA = tc1.a(objF);
                    if (thA != null) {
                        logFailure("oplus-aod-notification-apply", thA);
                    }
                }
                ArrayList arrayList2 = new ArrayList(arrayList.size());
                int size = arrayList.size();
                if (size > 7) {
                    size = 7;
                }
                ArrayList arrayList3 = new ArrayList(size);
                ArrayList arrayList4 = new ArrayList(arrayList.size());
                ArrayList arrayList5 = new ArrayList(arrayList.size());
                int size2 = arrayList.size();
                int i2 = 0;
                while (i2 < size2) {
                    Object obj = arrayList.get(i2);
                    int i3 = i2 + 1;
                    try {
                        Object objCallMethod2 = XposedHelpers.callMethod(obj, "getSbn", new Object[i]);
                        objF2 = objCallMethod2 instanceof StatusBarNotification ? (StatusBarNotification) objCallMethod2 : null;
                    } catch (Throwable th2) {
                        objF2 = dr.f(th2);
                    }
                    if (objF2 instanceof sc1) {
                        objF2 = null;
                    }
                    StatusBarNotification statusBarNotification2 = (StatusBarNotification) objF2;
                    try {
                        Object objCallMethod3 = XposedHelpers.callMethod(obj, "getKey", new Object[i]);
                        objF3 = objCallMethod3 instanceof String ? (String) objCallMethod3 : null;
                    } catch (Throwable th3) {
                        objF3 = dr.f(th3);
                    }
                    if (objF3 instanceof sc1) {
                        objF3 = null;
                    }
                    String key = (String) objF3;
                    if (key == null) {
                        key = statusBarNotification2 != null ? statusBarNotification2.getKey() : null;
                        if (key == null) {
                            key = (statusBarNotification2 != null ? statusBarNotification2.getPackageName() : null) + PixelClockHostView.COLON + (statusBarNotification2 != null ? Integer.valueOf(statusBarNotification2.getId()) : null) + PixelClockHostView.COLON + (statusBarNotification2 != null ? Long.valueOf(statusBarNotification2.getPostTime()) : null);
                        }
                    }
                    arrayList2.add(key);
                    PixelClockHostView.NotificationIcon notificationIconResolveNotificationEntryIcon = resolveNotificationEntryIcon(obj, statusBarNotification2, context);
                    if (notificationIconResolveNotificationEntryIcon != null) {
                        if (statusBarNotification2 != null && (packageName = statusBarNotification2.getPackageName()) != null) {
                            arrayList4.add(packageName);
                            arrayList5.add(notificationIconResolveNotificationEntryIcon.getDrawableState());
                        }
                        c = 7;
                        if (arrayList3.size() < 7) {
                            arrayList3.add(notificationIconResolveNotificationEntryIcon);
                        }
                    } else {
                        c = 7;
                    }
                    i2 = i3;
                    arrayList = arrayList;
                    i = 0;
                }
                updateMediaNotificationIconStates(arrayList4, arrayList5);
                this.oplusAodNotificationSourceReady = true;
                int size3 = arrayList.size();
                if (!ph0.i(this.oplusAodNotificationKeys, arrayList2) || this.oplusAodNotificationEntryCount != size3 || this.oplusAodNotificationIcons.size() != arrayList3.size()) {
                    this.oplusAodNotificationKeys = arrayList2;
                    this.oplusAodNotificationEntryCount = size3;
                    this.oplusAodNotificationIcons = arrayList3;
                    log("OPlus partial-AOD notifications=" + size3 + ", resolvedIcons=" + arrayList3.size());
                    refreshActiveAodContent(true, z);
                }
                objF = l22.a;
                thA = tc1.a(objF);
                if (thA != null) {
                    logFailure("oplus-aod-notification-apply", thA);
                }
            }
        } catch (Throwable th4) {
            objF = dr.f(th4);
        }
    }

    public final void attachHost(Object obj) {
        Object objCallMethod = XposedHelpers.callMethod(obj, "getView", new Object[]{0});
        ViewGroup viewGroup = objCallMethod instanceof ViewGroup ? (ViewGroup) objCallMethod : null;
        if (viewGroup == null) {
            qc.g("big clock root unavailable");
            return;
        }
        Map<ViewGroup, PixelClockHostView> map = hosts;
        PixelClockHostView pixelClockHostView = map.get(viewGroup);
        if (pixelClockHostView != null) {
            pixelClockHostView.bringToFront();
            return;
        }
        Object objCallMethod2 = XposedHelpers.callMethod(obj, "getContext", new Object[0]);
        Context context = objCallMethod2 instanceof Context ? (Context) objCallMethod2 : null;
        if (context == null) {
            context = viewGroup.getContext();
        }
        this.notificationContext = new WeakReference<>(context);
        ClassLoader classLoader = viewGroup.getClass().getClassLoader();
        if (classLoader == null) {
            qc.g("personality clock class loader unavailable");
            return;
        }
        context.getClass();
        ClassLoader classLoader2 = this.systemUiClassLoader;
        if (classLoader2 == null) {
            classLoader2 = classLoader;
        }
        PixelClockHostView pixelClockHostView2 = new PixelClockHostView(context, classLoader2, this.aodBatteryYOffset, this.aodBatteryEnabled);
        ensureMediaMonitor(context);
        scheduleOplusAodNotificationRefresh$default(this, null, false, 3, null);
        int i = 1;
        scheduleLockScreenNotificationRefresh$default(this, null, 1, null);
        viewGroup.setClipChildren(false);
        viewGroup.setClipToPadding(false);
        viewGroup.addView(pixelClockHostView2, viewGroup.getChildCount(), new ViewGroup.LayoutParams(-1, -1));
        pixelClockHostView2.bringToFront();
        map.put(viewGroup, pixelClockHostView2);
        suppressNativeAodNotificationLayout$default(this, null, 1, null);
        XposedHelpers.setAdditionalInstanceField(viewGroup, HOST_FIELD, pixelClockHostView2);
        log("variable-font morphing=" + pixelClockHostView2.getUsesVariableFontMorphing());
        installPluginVisualSuppression(classLoader);
        markOriginalVisualContainers(obj);
        for (long j : SUPPRESSION_RETRY_DELAYS) {
            xw0 xw0Var = new xw0(i);
            xw0Var.f = this;
            xw0Var.g = viewGroup;
            xw0Var.h = pixelClockHostView2;
            xw0Var.e = obj;
            VarHandle.storeStoreFence();
            viewGroup.postDelayed(xw0Var, j);
        }
        log("custom host attached above OPlus depth layers");
    }

    public static final void attachHost$lambda$1$0(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup, PixelClockHostView pixelClockHostView, Object obj) {
        pixelLockscreenClockHook.ensureHostBounds(viewGroup, pixelClockHostView);
        pixelLockscreenClockHook.markOriginalVisualContainers(obj);
        pixelLockscreenClockHook.mirrorInformation(obj, pixelClockHostView);
        pixelClockHostView.bringToFront();
        viewGroup.invalidate();
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

    public final void captureNativeAodNotificationLayout(ViewGroup viewGroup) {
        Object objF;
        Object objF2;
        List<PixelClockHostView.NotificationIcon> listR;
        Drawable drawable;
        Drawable.ConstantState constantState;
        try {
            try {
                objF = Boolean.valueOf(XposedHelpers.getBooleanField(viewGroup, "mIsNeedShowNotification"));
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Object obj = Boolean.FALSE;
            if (objF instanceof sc1) {
                objF = obj;
            }
            boolean zBooleanValue = ((Boolean) objF).booleanValue();
            if (zBooleanValue) {
                fp0 fp0VarX = e72.x();
                int childCount = viewGroup.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View childAt = viewGroup.getChildAt(i);
                    ImageView imageView = childAt instanceof ImageView ? (ImageView) childAt : null;
                    if (imageView != null && (drawable = imageView.getDrawable()) != null && (constantState = drawable.getConstantState()) != null) {
                        fp0VarX.add(new PixelClockHostView.NotificationIcon(constantState, imageView.getImageTintList(), true));
                    }
                }
                listR = e72.r(fp0VarX);
            } else {
                listR = j20.d;
            }
            this.nativeAodNotificationSourceReady = true;
            int size = zBooleanValue ? listR.size() : 0;
            suppressNativeAodNotificationLayout(viewGroup);
            if (!ph0.i(this.nativeAodNotificationIcons, listR) || this.nativeAodNotificationEntryCount != size) {
                this.nativeAodNotificationIcons = listR;
                this.nativeAodNotificationEntryCount = size;
                log("native OPlus AOD notification icons=" + listR.size());
                refreshActiveAodContent$default(this, true, false, UI_STATE_KEYGUARD, null);
            }
            objF2 = l22.a;
        } catch (Throwable th2) {
            objF2 = dr.f(th2);
        }
        Throwable thA = tc1.a(objF2);
        if (thA != null) {
            logFailure("native-aod-notification-mirror", thA);
        }
    }

    public final void clearPendingSleepOrigin() {
        this.pendingSleepFromUnlocked = false;
        this.pendingSleepOriginLatchedAt = Long.MIN_VALUE;
        this.pendingSleepOriginAuthoritative = false;
    }

    private final void deferToAuthoritativePanoramicState(ViewGroup viewGroup, boolean z, boolean z2) {
        boolean z3;
        Map<ViewGroup, PanoramicSyncRequest> map = pendingPanoramicSyncs;
        synchronized (map) {
            try {
                PanoramicSyncRequest panoramicSyncRequest = map.get(viewGroup);
                map.put(viewGroup, new PanoramicSyncRequest(z || (panoramicSyncRequest != null && panoramicSyncRequest.getNormalizedEntry()), z2 || (panoramicSyncRequest != null && panoramicSyncRequest.getAnimate())));
                z3 = panoramicSyncRequest == null;
            } catch (Throwable th) {
                throw th;
            }
        }
        if (z3) {
            y51 y51Var = new y51(UI_STATE_KEYGUARD);
            y51Var.e = this;
            y51Var.f = viewGroup;
            VarHandle.storeStoreFence();
            viewGroup.post(y51Var);
        }
    }

    public static final void deferToAuthoritativePanoramicState$lambda$1(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup) {
        Object objF;
        Object objF2;
        try {
            try {
                objF = XposedHelpers.callMethod(viewGroup, "getRenderedViewState", new Object[0]);
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Object next = null;
            if (objF instanceof sc1) {
                objF = null;
            }
            if (objF == null) {
                kj1 kj1VarH0 = fd.h0(new String[]{"b", "f5606b", "renderedViewState"});
                defpackage.d dVar = new defpackage.d(25);
                dVar.e = pixelLockscreenClockHook;
                dVar.f = viewGroup;
                VarHandle.storeStoreFence();
                g40 g40Var = (g40) mj1.q(kj1VarH0, dVar).iterator();
                if (g40Var.hasNext()) {
                    next = g40Var.next();
                }
                objF = next;
            }
            pixelLockscreenClockHook.applyAuthoritativePanoramicState(viewGroup, objF);
            objF2 = l22.a;
        } catch (Throwable th2) {
            objF2 = dr.f(th2);
        }
        Throwable thA = tc1.a(objF2);
        if (thA != null) {
            pixelLockscreenClockHook.logFailure("deferred-panoramic-state-sync", thA);
        }
    }

    public static final Object deferToAuthoritativePanoramicState$lambda$1$0$1(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup, String str) {
        Object objF;
        str.getClass();
        try {
            objF = XposedHelpers.getObjectField(viewGroup, str);
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            return null;
        }
        return objF;
    }

    public final void dispatchOplusAodNotificationEntries(List<?> list, boolean z) {
        a61 a61Var = new a61(0);
        a61Var.f = this;
        a61Var.g = list;
        a61Var.e = z;
        VarHandle.storeStoreFence();
        if (ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
            a61Var.a();
            return;
        }
        Handler handler = this.mainHandler;
        defpackage.p pVar = new defpackage.p(MAX_ANCESTOR_DEPTH);
        pVar.e = a61Var;
        VarHandle.storeStoreFence();
        handler.post(pVar);
    }

    public static final l22 dispatchOplusAodNotificationEntries$lambda$0(PixelLockscreenClockHook pixelLockscreenClockHook, List list, boolean z) {
        pixelLockscreenClockHook.applyOplusAodNotificationEntries(list, z);
        return l22.a;
    }

    private final Drawable.ConstantState drawableStateForSnapshot(Drawable drawable, Context context) {
        Bitmap bitmapRasterizeDrawable = rasterizeDrawable(drawable);
        if (bitmapRasterizeDrawable != null) {
            return new BitmapDrawable(context.getResources(), bitmapRasterizeDrawable).getConstantState();
        }
        return null;
    }

    private final List<PixelClockHostView.NotificationIcon> effectiveAodNotificationIcons() {
        if (this.oplusAodNotificationSourceReady && (this.oplusAodNotificationEntryCount == 0 || !this.oplusAodNotificationIcons.isEmpty())) {
            return this.oplusAodNotificationIcons;
        }
        if (this.nativeAodNotificationSourceReady && !this.nativeAodNotificationIcons.isEmpty()) {
            return this.nativeAodNotificationIcons;
        }
        if (this.newRenderAodNotificationSourceReady && !this.newRenderAodNotificationIcons.isEmpty()) {
            return this.newRenderAodNotificationIcons;
        }
        if (this.oplusAodNotificationSourceReady) {
            return this.oplusAodNotificationIcons;
        }
        if (this.nativeAodNotificationSourceReady) {
            return this.nativeAodNotificationIcons;
        }
        return this.newRenderAodNotificationSourceReady ? this.newRenderAodNotificationIcons : j20.d;
    }

    /* JADX WARN: Code duplicated, block: B:20:0x003d  */
    /* JADX WARN: Code duplicated, block: B:23:0x004f  */
    /* JADX WARN: Code duplicated, block: B:28:0x0073  */
    /* JADX WARN: Code duplicated, block: B:38:0x006e A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:39:? A[LOOP:0: B:21:0x0049->B:39:?, LOOP_END, SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:40:? A[RETURN, SYNTHETIC] */
    public final void ensureDimOverlay(ViewGroup viewGroup) {
        Integer num;
        int iIntValue;
        if (XposedHelpers.getAdditionalInstanceField(viewGroup, DIM_OVERLAY_FIELD) instanceof View) {
            return;
        }
        Integer numValueOf = Integer.valueOf(resourceId(viewGroup, "keyguard_style_clock"));
        Object obj = null;
        if (numValueOf.intValue() == 0) {
            numValueOf = null;
        }
        View viewFindViewById = numValueOf != null ? viewGroup.findViewById(numValueOf.intValue()) : null;
        if (viewFindViewById == null) {
            for (Object obj2 : nr.l0(0, viewGroup.getChildCount())) {
                if (viewGroup.getChildAt(((Number) obj2).intValue()).getClass().getName().equals(OPLUS_KEYGUARD_CLOCK_CONTAINER_CLASS)) {
                    obj = obj2;
                    break;
                }
            }
            num = (Integer) obj;
            if (num != null) {
                return;
            } else {
                iIntValue = num.intValue();
            }
        } else {
            if (viewFindViewById.getParent() != viewGroup) {
                viewFindViewById = null;
            }
            if (viewFindViewById != null) {
                iIntValue = viewGroup.indexOfChild(viewFindViewById);
            } else {
                while (r1.hasNext()) {
                    if (viewGroup.getChildAt(((Number) obj2).intValue()).getClass().getName().equals(OPLUS_KEYGUARD_CLOCK_CONTAINER_CLASS)) {
                        obj = obj2;
                        break;
                    }
                }
                num = (Integer) obj;
                if (num != null) {
                    return;
                } else {
                    iIntValue = num.intValue();
                }
            }
        }
        View view = new View(viewGroup.getContext());
        view.setBackgroundColor(-16777216);
        view.setClickable(false);
        view.setFocusable(false);
        view.setImportantForAccessibility(UI_STATE_KEYGUARD);
        Context context = viewGroup.getContext();
        context.getClass();
        float fWallpaperDimPercent = wallpaperDimPercent(context);
        view.setAlpha(fWallpaperDimPercent / 100.0f);
        view.setVisibility((!isKeyguardShowing() || fWallpaperDimPercent <= 0.0f) ? 8 : 0);
        viewGroup.addView(view, iIntValue, new ViewGroup.LayoutParams(-1, -1));
        XposedHelpers.setAdditionalInstanceField(viewGroup, DIM_OVERLAY_FIELD, view);
        log("wallpaper dim overlay inserted at panel index " + iIntValue);
    }

    private final void ensureHostBounds(ViewGroup viewGroup, PixelClockHostView pixelClockHostView) {
        int width = viewGroup.getWidth();
        int height = viewGroup.getHeight();
        if (width <= 0 || height <= 0) {
            viewGroup.requestLayout();
            return;
        }
        if (pixelClockHostView.getWidth() != width || pixelClockHostView.getHeight() != height) {
            pixelClockHostView.measure(View.MeasureSpec.makeMeasureSpec(width, 1073741824), View.MeasureSpec.makeMeasureSpec(height, 1073741824));
            pixelClockHostView.layout(0, 0, width, height);
        }
        pixelClockHostView.ensureInitialPresentation();
        pixelClockHostView.invalidate();
    }

    private final void ensureMediaMonitor(Context context) {
        if (this.mediaMonitor != null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) {
            context = applicationContext;
        }
        AodMediaMonitor aodMediaMonitor = new AodMediaMonitor(this, context);
        aodMediaMonitor.start();
        this.mediaMonitor = aodMediaMonitor;
    }

    private final ViewGroup findClockRoot(View view) {
        for (int i = 0; i < MAX_ANCESTOR_DEPTH; i++) {
            if (ph0.i(view != null ? view.getClass().getName() : null, OPLUS_CLOCK_ROOT_CLASS)) {
                if (view instanceof ViewGroup) {
                    return (ViewGroup) view;
                }
                return null;
            }
            Object parent = view != null ? view.getParent() : null;
            view = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private final List<Object> flattenOplusAodNotificationEntries(Collection<?> collection) {
        Object objF;
        Object objF2;
        Object objF3;
        Object objF4;
        fp0 fp0VarX = e72.x();
        for (Object obj : collection) {
            if (obj != null) {
                Object obj2 = null;
                try {
                    Object objCallMethod = XposedHelpers.callMethod(obj, "getSbn", new Object[0]);
                    objF = objCallMethod instanceof StatusBarNotification ? (StatusBarNotification) objCallMethod : null;
                } catch (Throwable th) {
                    objF = dr.f(th);
                }
                if (objF instanceof sc1) {
                    objF = null;
                }
                if (((StatusBarNotification) objF) != null) {
                    fp0VarX.add(obj);
                } else {
                    try {
                        Object objCallMethod2 = XposedHelpers.callMethod(obj, "getChildren", new Object[0]);
                        objF2 = objCallMethod2 instanceof Collection ? (Collection) objCallMethod2 : null;
                    } catch (Throwable th2) {
                        objF2 = dr.f(th2);
                    }
                    if (objF2 instanceof sc1) {
                        objF2 = null;
                    }
                    Iterable iterable = (Collection) objF2;
                    if (iterable == null) {
                        iterable = j20.d;
                    }
                    ArrayList arrayList = new ArrayList();
                    for (Object obj3 : iterable) {
                        if (obj3 != null) {
                            arrayList.add(obj3);
                        }
                    }
                    Iterator it = arrayList.iterator();
                    if (it.hasNext()) {
                        Object next = it.next();
                        if (it.hasNext()) {
                            try {
                                Object objCallMethod3 = XposedHelpers.callMethod(next, "getSbn", new Object[0]);
                                StatusBarNotification statusBarNotification = objCallMethod3 instanceof StatusBarNotification ? (StatusBarNotification) objCallMethod3 : null;
                                objF3 = Long.valueOf(statusBarNotification != null ? statusBarNotification.getPostTime() : Long.MIN_VALUE);
                            } catch (Throwable th3) {
                                objF3 = dr.f(th3);
                            }
                            if (objF3 instanceof sc1) {
                                objF3 = Long.MIN_VALUE;
                            }
                            long jLongValue = ((Number) objF3).longValue();
                            do {
                                Object next2 = it.next();
                                try {
                                    Object objCallMethod4 = XposedHelpers.callMethod(next2, "getSbn", new Object[0]);
                                    StatusBarNotification statusBarNotification2 = objCallMethod4 instanceof StatusBarNotification ? (StatusBarNotification) objCallMethod4 : null;
                                    objF4 = Long.valueOf(statusBarNotification2 != null ? statusBarNotification2.getPostTime() : Long.MIN_VALUE);
                                } catch (Throwable th4) {
                                    objF4 = dr.f(th4);
                                }
                                if (objF4 instanceof sc1) {
                                    objF4 = Long.MIN_VALUE;
                                }
                                long jLongValue2 = ((Number) objF4).longValue();
                                if (jLongValue < jLongValue2) {
                                    next = next2;
                                    jLongValue = jLongValue2;
                                }
                            } while (it.hasNext());
                        }
                        obj2 = next;
                    }
                    if (obj2 != null) {
                        fp0VarX.add(obj2);
                    }
                }
            }
        }
        return e72.r(fp0VarX);
    }

    private final String formatLockscreenDate(Context context, Date date) {
        String bestDateTimePattern;
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        if (locale == null) {
            locale = Locale.getDefault();
        }
        String language = locale.getLanguage();
        if (ph0.i(language, Locale.CHINESE.getLanguage())) {
            bestDateTimePattern = "M月d日EEE";
        } else if (ph0.i(language, Locale.JAPANESE.getLanguage())) {
            bestDateTimePattern = "M月d日(E)";
        } else {
            bestDateTimePattern = ph0.i(language, Locale.ENGLISH.getLanguage()) ? "EEE, MMM d" : DateFormat.getBestDateTimePattern(locale, "MMMdEEE");
        }
        String str = new SimpleDateFormat(bestDateTimePattern, locale).format(date);
        str.getClass();
        return str;
    }

    public static String formatLockscreenDate$default(PixelLockscreenClockHook pixelLockscreenClockHook, Context context, Date date, int i, Object obj) {
        if ((i & UI_STATE_KEYGUARD) != 0) {
            date = new Date();
        }
        return pixelLockscreenClockHook.formatLockscreenDate(context, date);
    }

    private final boolean hasEffectiveAodNotifications() {
        if (this.oplusAodNotificationSourceReady) {
            return this.oplusAodNotificationEntryCount > 0;
        }
        if (this.nativeAodNotificationSourceReady) {
            return this.nativeAodNotificationEntryCount > 0;
        }
        return this.newRenderAodNotificationSourceReady && this.newRenderAodNotificationEntryCount > 0;
    }

    public final boolean hasFreshAuthoritativeSleepOrigin() {
        return this.pendingSleepOriginAuthoritative && pendingSleepOriginOrNull() != null;
    }

    private final void hookAffordanceClass(Class<?> cls) {
        if (hookedAffordanceClasses.add(cls)) {
            XposedBridge.hookAllConstructors(cls, new AnonymousClass1());
            XposedBridge.hookAllMethods(cls, "onDraw", new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    try {
                        Object obj = methodHookParam.thisObject;
                        ImageView imageView = obj instanceof ImageView ? (ImageView) obj : null;
                        if (imageView == null) {
                            return;
                        }
                        pixelLockscreenClockHook.styleAffordanceView(imageView, false);
                        objF = l22.a;
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        pixelLockscreenClockHook2.logFailure("affordance-onDraw", thA);
                    }
                }
            });
            for (final String str : AFFORDANCE_REFRESH_METHODS) {
                XposedBridge.hookAllMethods(cls, str, new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objF;
                        methodHookParam.getClass();
                        PixelLockscreenClockHook pixelLockscreenClockHook = this.this$0;
                        try {
                            Object obj = methodHookParam.thisObject;
                            ImageView imageView = obj instanceof ImageView ? (ImageView) obj : null;
                            if (imageView == null) {
                                return;
                            }
                            PixelLockscreenClockHook.styleAffordanceView$default(pixelLockscreenClockHook, imageView, false, 2, null);
                            objF = l22.a;
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        PixelLockscreenClockHook pixelLockscreenClockHook2 = this.this$0;
                        String str2 = str;
                        Throwable thA = tc1.a(objF);
                        if (thA != null) {
                            pixelLockscreenClockHook2.logFailure("affordance-" + str2, thA);
                        }
                    }
                });
            }
            log("affordance style installed for ".concat(cls.getName()));
        }
    }

    private final void hookAffordancePaletteRefresh() {
        XposedBridge.hookAllMethods(Application.class, "onConfigurationChanged", new C00561());
        sb0 sb0Var = vb0.a;
        vb0.f(TAG, "affordance-palette-refresh-install", "Application#onConfigurationChanged hooked");
    }

    private final void hookAodScreenOffOrigin(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (aodScreenOffOriginHooked) {
                return;
            }
            aodScreenOffOriginHooked = true;
            Class clsFindClassIfExists = XposedHelpers.findClassIfExists(KEYGUARD_STATE_CONTROLLER_CLASS, classLoader);
            if (clsFindClassIfExists != null) {
                XposedBridge.hookAllConstructors(clsFindClassIfExists, new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        PixelLockscreenClockHook.Companion unused = PixelLockscreenClockHook.Companion;
                        PixelLockscreenClockHook.keyguardStateController = new WeakReference(methodHookParam.thisObject);
                    }
                });
            }
            Class clsFindClassIfExists2 = XposedHelpers.findClassIfExists(KEYGUARD_SERVICE_CLASS, classLoader);
            if (clsFindClassIfExists2 != null) {
                XposedBridge.hookAllConstructors(clsFindClassIfExists2, new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objF;
                        methodHookParam.getClass();
                        try {
                            objF = XposedHelpers.getObjectField(methodHookParam.thisObject, "mBinder");
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        if (objF instanceof sc1) {
                            objF = null;
                        }
                        if (objF == null) {
                            return;
                        }
                        this.this$0.hookKeyguardSleepBinder(objF.getClass());
                    }
                });
            }
            for (String str : KEYGUARD_SERVICE_BINDER_CLASSES) {
                Class<?> clsFindClassIfExists3 = XposedHelpers.findClassIfExists(str, classLoader);
                if (clsFindClassIfExists3 != null) {
                    hookKeyguardSleepBinder(clsFindClassIfExists3);
                }
            }
            Class<?> clsRequireClass = requireClass(WAKEFULNESS_LIFECYCLE_CLASS, classLoader);
            XposedBridge.hookAllMethods(clsRequireClass, "dispatchStartedGoingToSleep", new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Integer num;
                    Object obj;
                    Object objF;
                    methodHookParam.getClass();
                    if (PixelLockscreenClockHook.this.hasFreshAuthoritativeSleepOrigin()) {
                        return;
                    }
                    WeakReference weakReference = PixelLockscreenClockHook.keyguardStateController;
                    Boolean bool = null;
                    bool = null;
                    if (weakReference != null && (obj = weakReference.get()) != null) {
                        try {
                            Object objCallMethod = XposedHelpers.callMethod(obj, "isShowing", new Object[0]);
                            objCallMethod.getClass();
                            objF = (Boolean) objCallMethod;
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        bool = (Boolean) (objF instanceof sc1 ? null : objF);
                    }
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    boolean z = true;
                    if (bool == null ? !((num = pixelLockscreenClockHook.lastObservedStableUiState) != null && num.intValue() == 1) : bool.booleanValue()) {
                        z = false;
                    }
                    pixelLockscreenClockHook.pendingSleepFromUnlocked = z;
                    PixelLockscreenClockHook.this.pendingSleepOriginLatchedAt = SystemClock.uptimeMillis();
                    PixelLockscreenClockHook.this.pendingSleepOriginAuthoritative = false;
                    PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                    pixelLockscreenClockHook2.log("screen-off origin latched: unlocked=" + pixelLockscreenClockHook2.pendingSleepFromUnlocked + ", keyguardShowing=" + bool);
                }
            });
            Iterator it = vj1.q("dispatchStartedWakingUp", "dispatchFinishedWakingUp").iterator();
            while (it.hasNext()) {
                XposedBridge.hookAllMethods(clsRequireClass, (String) it.next(), new XC_MethodHook() {
                    public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        this.this$0.clearPendingSleepOrigin();
                    }
                });
            }
            log("screen-off AOD origin latch installed");
        }
    }

    private final void hookClockPluginClass(Class<?> cls) {
        synchronized (INSTALL_LOCK) {
            if (clockPluginHooked) {
                return;
            }
            clockPluginHooked = true;
            XposedHelpers.findAndHookMethod(cls, "loadPluginReal", new Object[]{String.class, new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    if (ph0.i(methodHookParam.getResult(), Boolean.TRUE)) {
                        Object[] objArr = methodHookParam.args;
                        objArr.getClass();
                        if (ph0.i(objArr.length == 0 ? null : objArr[0], PixelLockscreenClockHook.BIG_CLOCK_LOGICAL_PACKAGE)) {
                            PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                            try {
                                Object obj = methodHookParam.thisObject;
                                obj.getClass();
                                pixelLockscreenClockHook.attachHost(obj);
                                Object obj2 = methodHookParam.thisObject;
                                obj2.getClass();
                                pixelLockscreenClockHook.syncHost(obj2, false);
                                objF = l22.a;
                            } catch (Throwable th) {
                                objF = dr.f(th);
                            }
                            PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                            Throwable thA = tc1.a(objF);
                            if (thA != null) {
                                pixelLockscreenClockHook2.logFailure("clock-plugin-attach", thA);
                            }
                        }
                    }
                }
            }});
            XposedBridge.hookAllMethods(cls, "render", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    try {
                        Object objCallMethod = XposedHelpers.callMethod(methodHookParam.thisObject, "getView", new Object[]{0});
                        ViewGroup viewGroup = objCallMethod instanceof ViewGroup ? (ViewGroup) objCallMethod : null;
                        if (viewGroup == null) {
                            return;
                        }
                        if (!PixelLockscreenClockHook.hosts.containsKey(viewGroup)) {
                            Object obj = methodHookParam.thisObject;
                            obj.getClass();
                            pixelLockscreenClockHook.attachHost(obj);
                        }
                        Object obj2 = methodHookParam.thisObject;
                        obj2.getClass();
                        pixelLockscreenClockHook.syncHost(obj2, true);
                        objF = l22.a;
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        pixelLockscreenClockHook2.logFailure("clock-plugin-state-sync", thA);
                    }
                }
            });
            XposedBridge.hookAllMethods(cls, "unloadPluginReal", new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    try {
                        Object objCallMethod = XposedHelpers.callMethod(methodHookParam.thisObject, "getView", new Object[]{0});
                        ViewGroup viewGroup = objCallMethod instanceof ViewGroup ? (ViewGroup) objCallMethod : null;
                        if (viewGroup == null) {
                            return;
                        }
                        PixelClockHostView pixelClockHostView = (PixelClockHostView) PixelLockscreenClockHook.hosts.remove(viewGroup);
                        if (pixelClockHostView != null) {
                            viewGroup.removeView(pixelClockHostView);
                        }
                        PixelLockscreenClockHook.pendingPanoramicSyncs.remove(viewGroup);
                        XposedHelpers.removeAdditionalInstanceField(viewGroup, PixelLockscreenClockHook.HOST_FIELD);
                    } catch (Throwable th) {
                        dr.f(th);
                    }
                }
            });
            log("ClockPlugin state hooks installed");
        }
    }

    public final void hookKeyguardSleepBinder(Class<?> cls) {
        synchronized (INSTALL_LOCK) {
            if (hookedKeyguardSleepBinderClasses.add(cls)) {
                XposedBridge.hookAllMethods(cls, "onStartedGoingToSleep", new XC_MethodHook() {
                    public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objF;
                        Object objF2;
                        Object objF3;
                        methodHookParam.getClass();
                        try {
                            objF = XposedHelpers.getObjectField(methodHookParam.thisObject, "this$0");
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        if (objF instanceof sc1) {
                            objF = null;
                        }
                        if (objF == null) {
                            return;
                        }
                        try {
                            objF2 = XposedHelpers.getObjectField(objF, "mKeyguardViewMediator");
                        } catch (Throwable th2) {
                            objF2 = dr.f(th2);
                        }
                        if (objF2 instanceof sc1) {
                            objF2 = null;
                        }
                        if (objF2 == null) {
                            return;
                        }
                        try {
                            objF3 = Boolean.valueOf(XposedHelpers.getBooleanField(objF2, "mShowing"));
                        } catch (Throwable th3) {
                            objF3 = dr.f(th3);
                        }
                        Boolean bool = (Boolean) (objF3 instanceof sc1 ? null : objF3);
                        if (bool != null) {
                            boolean z = !bool.booleanValue();
                            PixelLockscreenClockHook.this.pendingSleepFromUnlocked = z;
                            PixelLockscreenClockHook.this.pendingSleepOriginLatchedAt = SystemClock.uptimeMillis();
                            PixelLockscreenClockHook.this.pendingSleepOriginAuthoritative = true;
                            PixelLockscreenClockHook.this.log("pre-Keyguard sleep origin latched: unlocked=" + z);
                        }
                    }
                });
                XposedBridge.hookAllMethods(cls, "onStartedWakingUp", new XC_MethodHook() {
                    public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        PixelLockscreenClockHook.this.clearPendingSleepOrigin();
                    }
                });
                log("pre-Keyguard sleep binder hooked: ".concat(cls.getName()));
            }
        }
    }

    private final void hookLockScreenNotificationStateFlow(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (lockScreenNotificationStateFlowHooked) {
                return;
            }
            lockScreenNotificationStateFlowHooked = true;
            Class<?> clsRequireClass = requireClass(OPLUS_LOCKSCREEN_NOTIFICATION_DISPATCHER_CLASS, classLoader);
            XposedBridge.hookAllConstructors(clsRequireClass, new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    PixelLockscreenClockHook.this.lockScreenNotificationDispatcher = new WeakReference(methodHookParam.thisObject);
                    PixelLockscreenClockHook.this.scheduleLockScreenNotificationRefresh(methodHookParam.thisObject);
                }
            });
            XposedBridge.hookAllMethods(clsRequireClass, "updateNotificationListOnKg", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    PixelLockscreenClockHook.this.lockScreenNotificationDispatcher = new WeakReference(methodHookParam.thisObject);
                    PixelLockscreenClockHook.this.scheduleLockScreenNotificationRefresh(methodHookParam.thisObject);
                }
            });
            Iterator<T> it = OPLUS_LOCKSCREEN_NOTIFICATION_UPDATE_COROUTINES.iterator();
            while (it.hasNext()) {
                Class clsFindClassIfExists = XposedHelpers.findClassIfExists((String) it.next(), classLoader);
                if (clsFindClassIfExists != null) {
                    XposedBridge.hookAllMethods(clsFindClassIfExists, "invokeSuspend", new XC_MethodHook() {
                        public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                            Object objF;
                            methodHookParam.getClass();
                            try {
                                objF = XposedHelpers.getObjectField(methodHookParam.thisObject, "this$0");
                            } catch (Throwable th) {
                                objF = dr.f(th);
                            }
                            if (objF instanceof sc1) {
                                objF = null;
                            }
                            if (objF == null) {
                                return;
                            }
                            this.this$0.lockScreenNotificationDispatcher = new WeakReference(objF);
                            this.this$0.refreshLockScreenNotificationIcons(objF, true);
                        }
                    });
                }
            }
            log("continuous lockscreen notification StateFlow mirror installed");
        }
    }

    private final void hookLockscreenDimOverlay(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (wallpaperDimHooked) {
                return;
            }
            wallpaperDimHooked = true;
            Class<?> clsRequireClass = requireClass(CENTRAL_SURFACES_CLASS, classLoader);
            Class<?> clsRequireClass2 = requireClass(NOTIFICATION_PANEL_CLASS, classLoader);
            darkModeUtilClass = XposedHelpers.findClassIfExists(OPLUS_DARK_MODE_UTIL_CLASS, classLoader);
            Class clsFindClassIfExists = XposedHelpers.findClassIfExists(KEYGUARD_STATE_CONTROLLER_CLASS, classLoader);
            if (clsFindClassIfExists != null) {
                XposedBridge.hookAllConstructors(clsFindClassIfExists, new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        PixelLockscreenClockHook.Companion unused = PixelLockscreenClockHook.Companion;
                        PixelLockscreenClockHook.keyguardStateController = new WeakReference(methodHookParam.thisObject);
                    }
                });
            }
            XposedBridge.hookAllMethods(clsRequireClass, "start", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    Object objF2;
                    Object objF3;
                    methodHookParam.getClass();
                    try {
                        try {
                            objF = XposedHelpers.getObjectField(methodHookParam.thisObject, "mKeyguardStateController");
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        Object obj = null;
                        if (objF instanceof sc1) {
                            objF = null;
                        }
                        if (objF == null) {
                            try {
                                Object objectField = XposedHelpers.getObjectField(methodHookParam.thisObject, "mScrimController");
                                objF3 = objectField == null ? null : XposedHelpers.getObjectField(objectField, "mKeyguardStateController");
                            } catch (Throwable th2) {
                                objF3 = dr.f(th2);
                            }
                            if (!(objF3 instanceof sc1)) {
                                obj = objF3;
                            }
                            if (obj == null) {
                                return;
                            } else {
                                objF = obj;
                            }
                        }
                        Companion unused = PixelLockscreenClockHook.Companion;
                        PixelLockscreenClockHook.keyguardStateController = new WeakReference(objF);
                        objF2 = l22.a;
                    } catch (Throwable th3) {
                        objF2 = dr.f(th3);
                    }
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    Throwable thA = tc1.a(objF2);
                    if (thA != null) {
                        pixelLockscreenClockHook.logFailure("keyguard-state-discovery", thA);
                    }
                }
            });
            XposedBridge.hookAllConstructors(clsRequireClass2, new C00634());
            XposedBridge.hookAllMethods(clsRequireClass2, "onFinishInflate", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    Object obj = methodHookParam.thisObject;
                    ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
                    if (viewGroup == null) {
                        return;
                    }
                    pixelLockscreenClockHook.ensureDimOverlay(viewGroup);
                }
            });
            XposedBridge.hookAllMethods(clsRequireClass2, "dispatchDraw", new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    try {
                        Object obj = methodHookParam.thisObject;
                        ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
                        if (viewGroup == null) {
                            return;
                        }
                        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(viewGroup, PixelLockscreenClockHook.DIM_OVERLAY_FIELD);
                        View view = additionalInstanceField instanceof View ? (View) additionalInstanceField : null;
                        if (view == null) {
                            return;
                        }
                        Context context = viewGroup.getContext();
                        context.getClass();
                        float fWallpaperDimPercent = pixelLockscreenClockHook.wallpaperDimPercent(context);
                        int i = (!pixelLockscreenClockHook.isKeyguardShowing() || fWallpaperDimPercent <= 0.0f) ? 8 : 0;
                        float f = fWallpaperDimPercent / 100.0f;
                        if (view.getAlpha() != f) {
                            view.setAlpha(f);
                        }
                        if (view.getVisibility() != i) {
                            view.setVisibility(i);
                        }
                        objF = l22.a;
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                    PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                    Throwable thA = tc1.a(objF);
                    if (thA != null) {
                        pixelLockscreenClockHook2.logFailure("lockscreen-dim-draw", thA);
                    }
                }
            });
            log("independent lockscreen dim overlay installed");
        }
    }

    private final void hookNativeAodNotificationLayout(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (nativeAodNotificationLayoutHooked) {
                return;
            }
            nativeAodNotificationLayoutHooked = true;
            Class<?> clsRequireClass = requireClass(OPLUS_AOD_NOTIFICATION_LAYOUT_CLASS, classLoader);
            XposedBridge.hookAllConstructors(clsRequireClass, new C00652());
            Iterator<T> it = NATIVE_AOD_NOTIFICATION_REFRESH_METHODS.iterator();
            while (it.hasNext()) {
                XposedBridge.hookAllMethods(clsRequireClass, (String) it.next(), new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        Object obj = methodHookParam.thisObject;
                        ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
                        if (viewGroup == null) {
                            return;
                        }
                        this.this$0.nativeAodNotificationLayout = new WeakReference(viewGroup);
                        this.this$0.scheduleNativeAodNotificationCapture(viewGroup);
                    }
                });
            }
            log("native OPlus partial-AOD notification layout mirror installed");
        }
    }

    private final void hookNewRenderAodNotificationSource(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (newRenderAodNotificationHooked) {
                return;
            }
            newRenderAodNotificationHooked = true;
            XposedBridge.hookAllMethods(requireClass(OPLUS_AOD_PLUGIN_CALL_CLASS, classLoader), "addLockScreenNotificationIconData", new C00662());
            log("new-render OPlus partial-AOD notification bridge installed");
        }
    }

    private final void hookNotificationPipeline(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (notificationPipelineHooked) {
                return;
            }
            notificationPipelineHooked = true;
            Class<?> clsRequireClass = requireClass(NOTIFICATION_COLLECTION_LISTENER_CLASS, classLoader);
            Iterator<T> it = NOTIFICATION_PIPELINE_REFRESH_METHODS.iterator();
            while (it.hasNext()) {
                XposedBridge.hookAllMethods(clsRequireClass, (String) it.next(), new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        this.this$0.requestOplusLockScreenNotificationRebuild();
                        PixelLockscreenClockHook.scheduleOplusAodNotificationRefresh$default(this.this$0, null, true, 1, null);
                        PixelLockscreenClockHook.scheduleNativeAodNotificationRefresh$default(this.this$0, null, 1, null);
                    }
                });
            }
            log("AOD notification pipeline source installed");
        }
    }

    private final void hookOplusAodNotificationSource(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (oplusAodNotificationHooked) {
                return;
            }
            oplusAodNotificationHooked = true;
            Class<?> clsRequireClass = requireClass(OPLUS_AOD_NOTIFICATION_SERVICE_CLASS, classLoader);
            XposedBridge.hookAllConstructors(clsRequireClass, new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    PixelLockscreenClockHook.this.oplusAodNotificationService = new WeakReference(methodHookParam.thisObject);
                    PixelLockscreenClockHook.scheduleOplusAodNotificationRefresh$default(PixelLockscreenClockHook.this, methodHookParam.thisObject, false, PixelLockscreenClockHook.UI_STATE_KEYGUARD, null);
                    PixelLockscreenClockHook.scheduleNativeAodNotificationRefresh$default(PixelLockscreenClockHook.this, null, 1, null);
                }
            });
            XposedBridge.hookAllMethods(clsRequireClass, "onUpdateNotificationEntry", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    PixelLockscreenClockHook.this.oplusAodNotificationService = new WeakReference(methodHookParam.thisObject);
                    PixelLockscreenClockHook.this.scheduleOplusAodNotificationRefresh(methodHookParam.thisObject, true);
                    PixelLockscreenClockHook.scheduleNativeAodNotificationRefresh$default(PixelLockscreenClockHook.this, null, 1, null);
                }
            });
            Class clsFindClassIfExists = XposedHelpers.findClassIfExists(OPLUS_AOD_NOTIFICATION_OBSERVER_CLASS, classLoader);
            if (clsFindClassIfExists != null) {
                XposedBridge.hookAllMethods(clsFindClassIfExists, "updateLockScreenNotifications", new XC_MethodHook() {
                    public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        methodHookParam.getClass();
                        PixelLockscreenClockHook.scheduleOplusAodNotificationRefresh$default(this.this$0, null, false, 3, null);
                        PixelLockscreenClockHook.scheduleNativeAodNotificationRefresh$default(this.this$0, null, 1, null);
                    }

                    public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                        Object objF;
                        methodHookParam.getClass();
                        Object[] objArr = methodHookParam.args;
                        objArr.getClass();
                        Object objT0 = fd.t0(1, objArr);
                        Number number = objT0 instanceof Number ? (Number) objT0 : null;
                        if (number != null) {
                            int iIntValue = number.intValue();
                            if (iIntValue == 1 || iIntValue == 2) {
                                Object[] objArr2 = methodHookParam.args;
                                objArr2.getClass();
                                Object obj = objArr2.length == 0 ? null : objArr2[0];
                                Collection collection = obj instanceof Collection ? (Collection) obj : null;
                                if (collection != null) {
                                    List listF0 = uk.F0(collection);
                                    try {
                                        objF = XposedHelpers.getObjectField(methodHookParam.thisObject, "this$0");
                                    } catch (Throwable th) {
                                        objF = dr.f(th);
                                    }
                                    Object obj2 = objF instanceof sc1 ? null : objF;
                                    if (obj2 != null) {
                                        this.this$0.oplusAodNotificationService = new WeakReference(obj2);
                                    }
                                    this.this$0.dispatchOplusAodNotificationEntries(listF0, true);
                                }
                            }
                        }
                    }
                });
            }
            log("OPlus partial-AOD filtered notification source installed");
        }
    }

    private final void hookPartialAodDirectSuspendTransition(ClassLoader classLoader) {
        synchronized (INSTALL_LOCK) {
            if (partialAodDirectSuspendHooked) {
                return;
            }
            partialAodDirectSuspendHooked = true;
            Class<?> clsRequireClass = requireClass(OPLUS_AOD_DISPLAY_UTIL_CLASS, classLoader);
            Class cls = Integer.TYPE;
            XposedHelpers.findAndHookMethod(clsRequireClass, "requestScreenState", new Object[]{cls, cls, String.class, new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    Object obj = methodHookParam.args[0];
                    Number number = obj instanceof Number ? (Number) obj : null;
                    if (number != null && number.intValue() == 1 && ph0.i(methodHookParam.args[PixelLockscreenClockHook.UI_STATE_KEYGUARD], PixelLockscreenClockHook.OPLUS_ON_TO_DOZE_SUSPEND_REASON)) {
                        PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                        Object obj2 = methodHookParam.thisObject;
                        obj2.getClass();
                        if (pixelLockscreenClockHook.isCurrentAodFullScreen(obj2)) {
                            return;
                        }
                        PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                        try {
                            XposedHelpers.callMethod(methodHookParam.thisObject, "setScreenState", new Object[]{Integer.valueOf(PixelLockscreenClockHook.DISPLAY_STATE_DOZE_SUSPEND)});
                            methodHookParam.setResult((Object) null);
                            pixelLockscreenClockHook2.log("partial AOD OFF bridge bypassed; entered DOZE_SUSPEND directly");
                            objF = l22.a;
                        } catch (Throwable th) {
                            objF = dr.f(th);
                        }
                        PixelLockscreenClockHook pixelLockscreenClockHook3 = PixelLockscreenClockHook.this;
                        Throwable thA = tc1.a(objF);
                        if (thA != null) {
                            pixelLockscreenClockHook3.logFailure("partial-aod-direct-suspend", thA);
                        }
                    }
                }
            }});
            log("partial-AOD direct suspend transition installed");
        }
    }

    private final void installAuthoritativePanoramicStateHook(ClassLoader classLoader) {
        Class clsFindClassIfExists = XposedHelpers.findClassIfExists(OPLUS_BASE_CLOCK_ROOT_CLASS, classLoader);
        if (clsFindClassIfExists == null) {
            log("OPlus final root-state class unavailable");
        } else {
            XposedBridge.hookAllMethods(clsFindClassIfExists, "setRenderedViewState", new XC_MethodHook() {
                public void afterHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    Object objF;
                    methodHookParam.getClass();
                    PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                    try {
                        Object obj = methodHookParam.thisObject;
                        Object obj2 = null;
                        ViewGroup viewGroup = obj instanceof ViewGroup ? (ViewGroup) obj : null;
                        if (viewGroup != null && viewGroup.getClass().getName().equals(PixelLockscreenClockHook.OPLUS_CLOCK_ROOT_CLASS)) {
                            Object[] objArr = methodHookParam.args;
                            objArr.getClass();
                            if (objArr.length != 0) {
                                obj2 = objArr[0];
                            }
                            pixelLockscreenClockHook.applyAuthoritativePanoramicState(viewGroup, obj2);
                            objF = l22.a;
                            PixelLockscreenClockHook pixelLockscreenClockHook2 = PixelLockscreenClockHook.this;
                            Throwable thA = tc1.a(objF);
                            if (thA != null) {
                                pixelLockscreenClockHook2.logFailure("final-panoramic-state-sync", thA);
                            }
                        }
                    } catch (Throwable th) {
                        objF = dr.f(th);
                    }
                }
            });
            log("OPlus final root-state hook installed");
        }
    }

    private final void installPart(String str, a80 a80Var) {
        Object objF;
        try {
            objF = a80Var.a();
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (!(objF instanceof sc1)) {
            sb0 sb0Var = vb0.a;
            vb0.a(TAG, "install-part", "name=" + str + " status=installed");
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            sb0 sb0Var2 = vb0.a;
            vb0.e(sb0.j, TAG, x30.m("install-", str), "part installation failed", thA);
        }
    }

    private final void installPluginVisualSuppression(ClassLoader classLoader) {
        if (hookedPluginLoaders.add(classLoader)) {
            installAuthoritativePanoramicStateHook(classLoader);
            XposedHelpers.findAndHookMethod(requireClass(OPLUS_VISUAL_CONTAINER_CLASS, classLoader), "dispatchDraw", new Object[]{Canvas.class, new XC_MethodHook() {
                public void beforeHookedMethod(XC_MethodHook.MethodHookParam methodHookParam) {
                    methodHookParam.getClass();
                    Object obj = methodHookParam.thisObject;
                    View view = obj instanceof View ? (View) obj : null;
                    if (view != null && ph0.i(XposedHelpers.getAdditionalInstanceField(view, PixelLockscreenClockHook.SUPPRESS_FIELD), Boolean.TRUE)) {
                        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(view, PixelLockscreenClockHook.SUPPRESS_HOST_FIELD);
                        PixelClockHostView pixelClockHostView = additionalInstanceField instanceof PixelClockHostView ? (PixelClockHostView) additionalInstanceField : null;
                        if (pixelClockHostView != null && pixelClockHostView.isAttachedToWindow()) {
                            methodHookParam.setResult((Object) null);
                        }
                    }
                }
            }});
            log("OPlus time/date container draw suppression installed");
        }
    }

    private final boolean isAodNotificationDisplayEnabled(Context context) {
        Object objF;
        if (Settings.Secure.getInt(context.getContentResolver(), OPLUS_AOD_ADDITIONAL_NOTIFICATION_SETTING, 1) == 1) {
            try {
                ClassLoader classLoader = context.getClassLoader();
                classLoader.getClass();
                Object objCallMethod = XposedHelpers.callMethod(XposedHelpers.callStaticMethod(requireClass(OPLUS_AOD_DATA_CLASS, classLoader), "getInstance", new Object[]{context}), "getAodEnableClockOnly", new Object[0]);
                objCallMethod.getClass();
                objF = Boolean.valueOf(((Number) objCallMethod).intValue() == 0);
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Object obj = Boolean.TRUE;
            if (objF instanceof sc1) {
                objF = obj;
            }
            if (((Boolean) objF).booleanValue()) {
                return true;
            }
        }
        return false;
    }

    public final boolean isCurrentAodFullScreen(Object obj) {
        Object objF;
        try {
            Object objectField = XposedHelpers.getObjectField(obj, "mContext");
            objectField.getClass();
            ClassLoader classLoader = obj.getClass().getClassLoader();
            classLoader.getClass();
            Object objCallMethod = XposedHelpers.callMethod(XposedHelpers.getObjectField(XposedHelpers.callStaticMethod(requireClass(OPLUS_AOD_DATA_CLASS, classLoader), "getInstance", new Object[]{(Context) objectField}), "mAodOptionsMgr"), "isCurrentFullScreen", new Object[0]);
            objCallMethod.getClass();
            objF = (Boolean) objCallMethod;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Object obj2 = Boolean.TRUE;
        if (objF instanceof sc1) {
            objF = obj2;
        }
        return ((Boolean) objF).booleanValue();
    }

    private final boolean isDark(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    private final boolean isGrayscaleNotificationIcon(Bitmap bitmap) {
        Object objF;
        try {
            int width = bitmap.getWidth() * bitmap.getHeight();
            int[] iArr = new int[width];
            bitmap.getPixels(iArr, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
            boolean z = false;
            int i = 0;
            while (true) {
                if (i >= width) {
                    z = true;
                    break;
                }
                int i2 = iArr[i];
                if (Color.alpha(i2) > 32 && Math.max(Color.red(i2), Math.max(Color.green(i2), Color.blue(i2))) - Math.min(Color.red(i2), Math.min(Color.green(i2), Color.blue(i2))) > 12) {
                    break;
                }
                i++;
            }
            objF = Boolean.valueOf(z);
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Object obj = Boolean.TRUE;
        if (objF instanceof sc1) {
            objF = obj;
        }
        return ((Boolean) objF).booleanValue();
    }

    public final boolean isKeyguardShowing() {
        Object obj;
        WeakReference<Object> weakReference = keyguardStateController;
        if (weakReference == null || (obj = weakReference.get()) == null || booleanField(obj, "mKeyguardGoingAway") || booleanField(obj, "mOccluded")) {
            return false;
        }
        return ph0.i(nullableBooleanField(obj, "mShowing"), Boolean.TRUE);
    }

    private final boolean isSystemNightMode(Context context) {
        Object objF;
        Class<?> cls = darkModeUtilClass;
        Boolean bool = null;
        if (cls != null) {
            try {
                Object objCallStaticMethod = XposedHelpers.callStaticMethod(cls, "isNightMode", new Object[]{context});
                objF = objCallStaticMethod instanceof Boolean ? (Boolean) objCallStaticMethod : null;
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            bool = (Boolean) (objF instanceof sc1 ? null : objF);
        }
        return bool != null ? bool.booleanValue() : isDark(context);
    }

    public final void log(String str) {
        sb0 sb0Var = vb0.a;
        vb0.c(TAG, str);
    }

    public final void logFailure(String str, Throwable th) {
        sb0 sb0Var = vb0.a;
        vb0.d(TAG, str, str + " failed", th);
    }

    private final void markOriginalVisualContainers(Object obj) {
        Object objCallMethod = XposedHelpers.callMethod(obj, "getView", new Object[]{0});
        ViewGroup viewGroup = objCallMethod instanceof ViewGroup ? (ViewGroup) objCallMethod : null;
        if (viewGroup == null) {
            return;
        }
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(viewGroup, SUPPRESSED_VIEWS_FIELD);
        OriginalVisualState originalVisualState = additionalInstanceField instanceof OriginalVisualState ? (OriginalVisualState) additionalInstanceField : null;
        if (originalVisualState == null) {
            originalVisualState = new OriginalVisualState();
            XposedHelpers.setAdditionalInstanceField(viewGroup, SUPPRESSED_VIEWS_FIELD, originalVisualState);
            ViewTreeObserver viewTreeObserver = viewGroup.getViewTreeObserver();
            g gVar = new g();
            gVar.d = originalVisualState;
            VarHandle.storeStoreFence();
            viewTreeObserver.addOnPreDrawListener(gVar);
        }
        Iterator it = e72.M(1, Integer.valueOf(VIEW_DATE_MESSAGE)).iterator();
        while (it.hasNext()) {
            int iIntValue = ((Number) it.next()).intValue();
            Object objCallMethod2 = XposedHelpers.callMethod(obj, "getView", new Object[]{Integer.valueOf(iIntValue)});
            View view = objCallMethod2 instanceof View ? (View) objCallMethod2 : null;
            if (view != null) {
                originalVisualState.update(iIntValue, view);
                Object parent = view.getParent();
                View view2 = parent instanceof View ? (View) parent : null;
                if (view2 != null) {
                    XposedHelpers.setAdditionalInstanceField(view2, SUPPRESS_FIELD, Boolean.TRUE);
                    Object additionalInstanceField2 = XposedHelpers.getAdditionalInstanceField(viewGroup, HOST_FIELD);
                    PixelClockHostView pixelClockHostView = additionalInstanceField2 instanceof PixelClockHostView ? (PixelClockHostView) additionalInstanceField2 : null;
                    if (pixelClockHostView != null) {
                        XposedHelpers.setAdditionalInstanceField(view2, SUPPRESS_HOST_FIELD, pixelClockHostView);
                        view2.invalidate();
                    }
                }
            }
        }
    }

    public static final boolean markOriginalVisualContainers$lambda$0$0(OriginalVisualState originalVisualState) {
        originalVisualState.enforce();
        return true;
    }

    /* JADX WARN: Code duplicated, block: B:25:0x005b  */
    private final void mirrorInformation(Object obj, PixelClockHostView pixelClockHostView) {
        Object objF;
        CharSequence text;
        try {
            Object objCallMethod = XposedHelpers.callMethod(obj, "getView", new Object[]{Integer.valueOf(VIEW_DATE_MESSAGE)});
            Drawable drawable = null;
            View view = objCallMethod instanceof View ? (View) objCallMethod : null;
            if (view == null) {
                return;
            }
            Context context = pixelClockHostView.getContext();
            context.getClass();
            String lockscreenDate$default = formatLockscreenDate$default(this, context, null, UI_STATE_KEYGUARD, null);
            Object objCallMethod2 = XposedHelpers.callMethod(view, "getExtraMsgView", new Object[0]);
            View view2 = objCallMethod2 instanceof View ? (View) objCallMethod2 : null;
            if (view2 != null) {
                Object objCallMethod3 = XposedHelpers.callMethod(view2, "getMessageContent", new Object[0]);
                TextView textView = objCallMethod3 instanceof TextView ? (TextView) objCallMethod3 : null;
                text = textView != null ? textView.getText() : null;
                if (text == null) {
                    text = "";
                }
            } else {
                text = "";
            }
            if (view2 != null) {
                Object objCallMethod4 = XposedHelpers.callMethod(view2, "getMessageIcon", new Object[0]);
                ImageView imageView = objCallMethod4 instanceof ImageView ? (ImageView) objCallMethod4 : null;
                if (imageView != null) {
                    drawable = imageView.getDrawable();
                }
            }
            pixelClockHostView.updateInformation(new PixelClockHostView.Information(lockscreenDate$default, "", text, drawable));
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            logFailure("date-weather-mirror", thA);
        }
    }

    private final PixelClockHostView.NotificationIcon notificationIconSnapshot(Drawable drawable, Context context) {
        Drawable.ConstantState constantState;
        boolean zIsGrayscaleNotificationIcon;
        Bitmap bitmapRasterizeDrawable = rasterizeDrawable(drawable);
        if (bitmapRasterizeDrawable == null || (constantState = new BitmapDrawable(context.getResources(), bitmapRasterizeDrawable).getConstantState()) == null) {
            return null;
        }
        Drawable.ConstantState constantState2 = drawable.getConstantState();
        if (constantState2 != null) {
            Map<Drawable.ConstantState, Boolean> map = grayscaleIconCache;
            synchronized (map) {
                try {
                    Boolean bool = map.get(constantState2);
                    if (bool != null) {
                        zIsGrayscaleNotificationIcon = bool.booleanValue();
                    } else {
                        zIsGrayscaleNotificationIcon = isGrayscaleNotificationIcon(bitmapRasterizeDrawable);
                        map.put(constantState2, Boolean.valueOf(zIsGrayscaleNotificationIcon));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
        } else {
            zIsGrayscaleNotificationIcon = isGrayscaleNotificationIcon(bitmapRasterizeDrawable);
        }
        return new PixelClockHostView.NotificationIcon(constantState, zIsGrayscaleNotificationIcon ? WHITE_TINT : null, !zIsGrayscaleNotificationIcon);
    }

    private final Boolean nullableBooleanField(Object obj, String str) {
        Object objF;
        try {
            objF = Boolean.valueOf(XposedHelpers.getBooleanField(obj, str));
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            objF = null;
        }
        return (Boolean) objF;
    }

    private final Boolean pendingSleepOriginOrNull() {
        long jUptimeMillis = SystemClock.uptimeMillis() - this.pendingSleepOriginLatchedAt;
        if (0 <= jUptimeMillis && jUptimeMillis < 30001) {
            return Boolean.valueOf(this.pendingSleepFromUnlocked);
        }
        clearPendingSleepOrigin();
        return null;
    }

    public final void prepareAffordanceView(ImageView imageView) {
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageView, AFFORDANCE_ATTACH_FIELD);
        Boolean bool = Boolean.TRUE;
        if (ph0.i(additionalInstanceField, bool)) {
            return;
        }
        XposedHelpers.setAdditionalInstanceField(imageView, AFFORDANCE_ATTACH_FIELD, bool);
        Map<ImageView, Boolean> map = this.affordanceViews;
        map.getClass();
        map.put(imageView, bool);
        imageView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
                view.getClass();
                PixelLockscreenClockHook pixelLockscreenClockHook = PixelLockscreenClockHook.this;
                ImageView imageView2 = view instanceof ImageView ? (ImageView) view : null;
                if (imageView2 == null) {
                    return;
                }
                PixelLockscreenClockHook.styleAffordanceView$default(pixelLockscreenClockHook, imageView2, false, PixelLockscreenClockHook.UI_STATE_KEYGUARD, null);
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                view.getClass();
            }
        });
        if (imageView.isAttachedToWindow()) {
            styleAffordanceView$default(this, imageView, false, UI_STATE_KEYGUARD, null);
        }
    }

    private final Bitmap rasterizeDrawable(Drawable drawable) {
        Object objF;
        int i;
        try {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            Integer numValueOf = Integer.valueOf(intrinsicWidth);
            if (intrinsicWidth <= 0) {
                numValueOf = null;
            }
            int iIntValue = 64;
            if (numValueOf != null) {
                int iIntValue2 = numValueOf.intValue();
                if (iIntValue2 > 256) {
                    iIntValue2 = 256;
                }
                i = iIntValue2;
            } else {
                i = 64;
            }
            int intrinsicHeight = drawable.getIntrinsicHeight();
            Integer numValueOf2 = Integer.valueOf(intrinsicHeight);
            if (intrinsicHeight <= 0) {
                numValueOf2 = null;
            }
            if (numValueOf2 != null && (iIntValue = numValueOf2.intValue()) > 256) {
                iIntValue = 256;
            }
            int i2 = iIntValue;
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
            bitmapCreateBitmap.getClass();
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            Rect rect = new Rect(drawable.getBounds());
            try {
                drawable.setBounds(0, 0, i, i2);
                drawable.draw(canvas);
                drawable.setBounds(rect);
                int i3 = i * i2;
                int[] iArr = new int[i3];
                bitmapCreateBitmap.getPixels(iArr, 0, i, 0, 0, i, i2);
                for (int i4 = 0; i4 < i3; i4++) {
                    if (Color.alpha(iArr[i4]) > 8) {
                        objF = bitmapCreateBitmap;
                        return (Bitmap) (objF instanceof sc1 ? null : objF);
                    }
                }
                return null;
            } catch (Throwable th) {
                drawable.setBounds(rect);
                throw th;
            }
        } catch (Throwable th2) {
            objF = dr.f(th2);
        }
    }

    private final OplusRootState readOplusRootState(Object obj) {
        Integer oplusRootState$intValue = readOplusRootState$intValue(obj, this, new String[]{"b", "f4718b", "uiState"}, "uiState");
        if (oplusRootState$intValue != null) {
            int iIntValue = oplusRootState$intValue.intValue();
            Integer oplusRootState$intValue2 = readOplusRootState$intValue(obj, this, new String[]{"h", "f4723h", "clockSizeState"}, "clockSizeState");
            if (oplusRootState$intValue2 != null) {
                return new OplusRootState(iIntValue, oplusRootState$intValue2.intValue(), readOplusRootState$booleanValue(obj, this, new String[]{"c", "f4719c", "isUiStateAnim"}, "isUiStateAnim"));
            }
        }
        return null;
    }

    private static final boolean readOplusRootState$booleanValue(Object obj, PixelLockscreenClockHook pixelLockscreenClockHook, String[] strArr, String str) {
        String str2;
        Object objF;
        int length = strArr.length;
        int i = 0;
        while (true) {
            Boolean bool = null;
            if (i >= length) {
                ms0 ms0VarA = jb1.a(Companion.ROOT_STATE_BOOLEAN_REGEX_TEMPLATE(str), obj.toString());
                if (ms0VarA == null || (str2 = (String) uk.r0(1, ms0VarA.a())) == null) {
                    return true;
                }
                if (str2.equals("true")) {
                    bool = Boolean.TRUE;
                } else if (str2.equals("false")) {
                    bool = Boolean.FALSE;
                }
                if (bool != null) {
                    return bool.booleanValue();
                }
                return true;
            }
            try {
                objF = Boolean.valueOf(XposedHelpers.getBooleanField(obj, strArr[i]));
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Boolean bool2 = (Boolean) (objF instanceof sc1 ? null : objF);
            if (bool2 != null) {
                return bool2.booleanValue();
            }
            i++;
        }
    }

    /* JADX WARN: Code duplicated, block: B:41:0x007f  */
    /* JADX WARN: Code duplicated, block: B:44:0x008a  */
    /* JADX WARN: Code duplicated, block: B:54:0x00a0  */
    /* JADX WARN: Code duplicated, block: B:56:0x00a6  */
    /* JADX WARN: Code duplicated, block: B:64:0x00ac A[SYNTHETIC] */
    private static final Integer readOplusRootState$intValue(Object obj, PixelLockscreenClockHook pixelLockscreenClockHook, String[] strArr, String str) {
        int i;
        int i2;
        Integer numValueOf;
        int iDigit;
        int i3;
        Object objF;
        int length = strArr.length;
        int i4 = 0;
        int i5 = 0;
        while (true) {
            if (i5 >= length) {
                ms0 ms0VarA = jb1.a(Companion.ROOT_STATE_INT_REGEX_TEMPLATE(str), obj.toString());
                if (ms0VarA == null) {
                    return null;
                }
                int i6 = 1;
                String str2 = (String) uk.r0(1, ms0VarA.a());
                if (str2 == null) {
                    return null;
                }
                oh0.r(MAX_ANCESTOR_DEPTH);
                int length2 = str2.length();
                if (length2 == 0) {
                    return null;
                }
                char cCharAt = str2.charAt(0);
                int i7 = -2147483647;
                if (ph0.q(cCharAt, 48) < 0) {
                    if (length2 == 1) {
                        return null;
                    }
                    if (cCharAt == '+') {
                        i = 0;
                    } else {
                        if (cCharAt != '-') {
                            return null;
                        }
                        i7 = Integer.MIN_VALUE;
                    }
                    i2 = -59652323;
                    while (i6 < length2) {
                        iDigit = Character.digit((int) str2.charAt(i6), MAX_ANCESTOR_DEPTH);
                        if (iDigit < 0) {
                            return null;
                        }
                        if ((i4 >= i2 && (i2 != -59652323 || i4 < (i2 = i7 / MAX_ANCESTOR_DEPTH))) || (i3 = i4 * MAX_ANCESTOR_DEPTH) < i7 + iDigit) {
                            return null;
                        }
                        i4 = i3 - iDigit;
                        i6++;
                    }
                    if (i != 0) {
                        numValueOf = Integer.valueOf(i4);
                    } else {
                        numValueOf = Integer.valueOf(-i4);
                    }
                    return numValueOf;
                }
                i6 = 0;
                i = i6;
                i2 = -59652323;
                while (i6 < length2) {
                    iDigit = Character.digit((int) str2.charAt(i6), MAX_ANCESTOR_DEPTH);
                    if (iDigit < 0) {
                        return null;
                    }
                    if (i4 >= i2) {
                    }
                    i4 = i3 - iDigit;
                    i6++;
                }
                if (i != 0) {
                    numValueOf = Integer.valueOf(i4);
                } else {
                    numValueOf = Integer.valueOf(-i4);
                }
                return numValueOf;
            }
            try {
                objF = Integer.valueOf(XposedHelpers.getIntField(obj, strArr[i5]));
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            Integer num = (Integer) (objF instanceof sc1 ? null : objF);
            if (num != null) {
                return Integer.valueOf(num.intValue());
            }
            i5++;
        }
    }

    private final void refreshActiveAodContent(boolean z, boolean z2) {
        List listF0;
        Map<ViewGroup, PixelClockHostView> map = hosts;
        synchronized (map) {
            listF0 = uk.F0(map.values());
        }
        ArrayList arrayList = new ArrayList();
        for (Object obj : listF0) {
            if (((PixelClockHostView) obj).getAcceptsLiveAodContent()) {
                arrayList.add(obj);
            }
        }
        if (arrayList.isEmpty()) {
            return;
        }
        b61 b61Var = new b61();
        b61Var.d = arrayList;
        b61Var.e = this;
        b61Var.f = z2;
        b61Var.g = z;
        VarHandle.storeStoreFence();
        if (z2 && !arrayList.isEmpty()) {
            int size = arrayList.size();
            int i = 0;
            while (i < size) {
                Object obj2 = arrayList.get(i);
                i++;
                if (((PixelClockHostView) obj2).isPartialAodActive()) {
                    requestPartialAodFrameWindow();
                    Handler handler = this.mainHandler;
                    defpackage.p pVar = new defpackage.p(12);
                    pVar.e = b61Var;
                    VarHandle.storeStoreFence();
                    handler.postDelayed(pVar, AOD_CONTENT_COMMIT_DELAY_MS);
                    return;
                }
            }
        }
        b61Var.a();
    }

    public static void refreshActiveAodContent$default(PixelLockscreenClockHook pixelLockscreenClockHook, boolean z, boolean z2, int i, Object obj) {
        if ((i & UI_STATE_KEYGUARD) != 0) {
            z2 = false;
        }
        pixelLockscreenClockHook.refreshActiveAodContent(z, z2);
    }

    public static final l22 refreshActiveAodContent$lambda$2(List list, PixelLockscreenClockHook pixelLockscreenClockHook, boolean z, boolean z2) {
        Iterator it = list.iterator();
        while (it.hasNext()) {
            PixelClockHostView pixelClockHostView = (PixelClockHostView) it.next();
            if (pixelClockHostView.getAcceptsLiveAodContent()) {
                PixelClockHostView.AodContent aodContentResolveAodContent = pixelLockscreenClockHook.resolveAodContent(true);
                if (z) {
                    pixelClockHostView.setLiveAodContent(aodContentResolveAodContent, z2);
                } else {
                    pixelClockHostView.setAodContent(aodContentResolveAodContent, z2);
                }
            }
        }
        return l22.a;
    }

    public final void refreshLockScreenNotificationIcons(Object obj, boolean z) {
        PixelLockscreenClockHook pixelLockscreenClockHook;
        Throwable th;
        Object objF;
        Collection collection;
        try {
            Object objCallMethod = XposedHelpers.callMethod(obj, "getContext", new Object[0]);
            objCallMethod.getClass();
            Context context = (Context) objCallMethod;
            Object objCallMethod2 = XposedHelpers.callMethod(XposedHelpers.callMethod(obj, "getLockScreenNotificationIconData", new Object[0]), "getValue", new Object[0]);
            if (objCallMethod2 instanceof Collection) {
                try {
                    collection = (Collection) objCallMethod2;
                } catch (Throwable th2) {
                    th = th2;
                    pixelLockscreenClockHook = this;
                    objF = dr.f(th);
                }
            } else {
                collection = null;
            }
            if (collection == null) {
                collection = j20.d;
            }
            ArrayList arrayList = new ArrayList(collection.size());
            ArrayList arrayList2 = new ArrayList(collection.size());
            for (Object obj2 : collection) {
                if (obj2 != null && arrayList2.size() < 7) {
                    Object objCallMethod3 = XposedHelpers.callMethod(obj2, "getIcon", new Object[0]);
                    Drawable drawable = objCallMethod3 instanceof Drawable ? (Drawable) objCallMethod3 : null;
                    Object objCallMethod4 = XposedHelpers.callMethod(obj2, "getPackageName", new Object[0]);
                    String str = objCallMethod4 instanceof String ? (String) objCallMethod4 : null;
                    if (str == null) {
                        str = "";
                    }
                    if (drawable != null) {
                        arrayList.add(str);
                        arrayList2.add(drawable);
                    }
                }
            }
            pixelLockscreenClockHook = this;
            try {
                pixelLockscreenClockHook.applyNewRenderAodNotificationIcons(context, arrayList, arrayList2, collection.size(), z);
                if (z) {
                    scheduleOplusAodNotificationRefresh$default(pixelLockscreenClockHook, null, true, 1, null);
                    pixelLockscreenClockHook.requestNewRenderAodFrame();
                }
                objF = l22.a;
            } catch (Throwable th3) {
                th = th3;
                th = th;
                objF = dr.f(th);
            }
        } catch (Throwable th4) {
            th = th4;
            pixelLockscreenClockHook = this;
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            pixelLockscreenClockHook.logFailure("lockscreen-notification-stateflow-snapshot", thA);
        }
    }

    public static void refreshLockScreenNotificationIcons$default(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj, boolean z, int i, Object obj2) {
        if ((i & UI_STATE_KEYGUARD) != 0) {
            z = false;
        }
        pixelLockscreenClockHook.refreshLockScreenNotificationIcons(obj, z);
    }

    public final void refreshOplusAodNotifications(Object obj, boolean z) {
        Object objF;
        try {
            Object objCallMethod = XposedHelpers.callMethod(obj, "getActiveNotificationEntries", new Object[0]);
            Collection<?> collection = objCallMethod instanceof Collection ? (Collection) objCallMethod : null;
            if (collection == null) {
                collection = j20.d;
            }
            applyOplusAodNotificationEntries(collection, z);
            objF = l22.a;
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            logFailure("oplus-aod-notification-snapshot", thA);
        }
    }

    public static void refreshOplusAodNotifications$default(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj, boolean z, int i, Object obj2) {
        if ((i & UI_STATE_KEYGUARD) != 0) {
            z = false;
        }
        pixelLockscreenClockHook.refreshOplusAodNotifications(obj, z);
    }

    private final void requestNewRenderAodFrame() {
        Object obj;
        WeakReference<Object> weakReference = this.newRenderOriginalObserver;
        if (weakReference == null || (obj = weakReference.get()) == null) {
            return;
        }
        z51 z51Var = new z51(1);
        z51Var.e = this;
        z51Var.f = obj;
        VarHandle.storeStoreFence();
        if (ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
            z51Var.a();
            return;
        }
        Handler handler = this.mainHandler;
        defpackage.p pVar = new defpackage.p(VIEW_DATE_MESSAGE);
        pVar.e = z51Var;
        VarHandle.storeStoreFence();
        handler.post(pVar);
    }

    public static final l22 requestNewRenderAodFrame$lambda$0(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj) {
        Object objF;
        try {
            objF = XposedHelpers.callMethod(obj, "updateLockScreenNotificationIconData", new Object[]{new String[0], new Drawable[0]});
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            pixelLockscreenClockHook.logFailure("new-render-aod-frame-request", thA);
        }
        return l22.a;
    }

    public final void requestOplusLockScreenNotificationRebuild() {
        Object obj;
        WeakReference<Object> weakReference = this.lockScreenNotificationDispatcher;
        if (weakReference == null || (obj = weakReference.get()) == null) {
            return;
        }
        z51 z51Var = new z51(0);
        z51Var.e = this;
        z51Var.f = obj;
        VarHandle.storeStoreFence();
        if (ph0.i(Looper.myLooper(), Looper.getMainLooper())) {
            z51Var.a();
            return;
        }
        Handler handler = this.mainHandler;
        defpackage.p pVar = new defpackage.p(9);
        pVar.e = z51Var;
        VarHandle.storeStoreFence();
        handler.post(pVar);
    }

    public static final l22 requestOplusLockScreenNotificationRebuild$lambda$0(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj) {
        Object objF;
        try {
            objF = XposedHelpers.callMethod(obj, "updateNotificationListOnKg", new Object[0]);
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            pixelLockscreenClockHook.logFailure("request-lockscreen-notification-rebuild", thA);
        }
        return l22.a;
    }

    private final void requestPartialAodFrameWindow() {
        Context context;
        ClassLoader classLoader;
        Object objF;
        WeakReference<Context> weakReference = this.notificationContext;
        if (weakReference == null || (context = weakReference.get()) == null || (classLoader = this.systemUiClassLoader) == null) {
            return;
        }
        try {
            Object objCallStaticMethod = XposedHelpers.callStaticMethod(requireClass(OPLUS_AOD_DISPLAY_UTIL_CLASS, classLoader), "getInstance", new Object[]{context});
            objCallStaticMethod.getClass();
            if (isCurrentAodFullScreen(objCallStaticMethod)) {
                return;
            } else {
                objF = XposedHelpers.callMethod(objCallStaticMethod, "requestScreenState", new Object[]{3, Integer.valueOf(AOD_CONTENT_FRAME_WINDOW_MS), AOD_CONTENT_FRAME_REASON});
            }
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            logFailure("partial-aod-frame-window-request", thA);
        }
    }

    private final Class<?> requireClass(String str, ClassLoader classLoader) {
        Class<?> clsFindClassIfExists = XposedHelpers.findClassIfExists(str, classLoader);
        if (clsFindClassIfExists != null) {
            return clsFindClassIfExists;
        }
        qc.e(str, "class unavailable: ");
        return null;
    }

    private final int resolveAndroidColor(Context context, int i) {
        Object objF;
        if (i == 0) {
            return -7829368;
        }
        try {
            objF = Integer.valueOf(context.getColor(i));
        } catch (Throwable th) {
            objF = dr.f(th);
        }
        if (objF instanceof sc1) {
            objF = -7829368;
        }
        return ((Number) objF).intValue();
    }

    private final PixelClockHostView.AodContent resolveAodContent(boolean z) {
        WeakReference<MediaController> weakReference;
        MediaController mediaController;
        PlaybackState playbackState;
        if (!z) {
            return PixelClockHostView.AodContent.None.INSTANCE;
        }
        if (this.activeMedia != null && ((weakReference = this.activeMediaController) == null || (mediaController = weakReference.get()) == null || (playbackState = mediaController.getPlaybackState()) == null || playbackState.getState() != 3)) {
            this.activeMedia = null;
            this.activeMediaController = null;
        }
        ActiveMedia activeMedia = this.activeMedia;
        if (activeMedia != null) {
            return new PixelClockHostView.AodContent.Media(activeMedia.getTitle(), activeMedia.getArtist(), activeMedia.getAppIconState(), effectiveAodNotificationIcons());
        }
        return hasEffectiveAodNotifications() ? new PixelClockHostView.AodContent.Notifications(effectiveAodNotificationIcons()) : PixelClockHostView.AodContent.None.INSTANCE;
    }

    private final PixelClockHostView.NotificationIcon resolveNotificationEntryIcon(Object obj, StatusBarNotification statusBarNotification, Context context) {
        Object objF;
        Object objF2;
        Object objF3;
        Object objF4;
        Object objF5;
        PixelClockHostView.NotificationIcon notificationIconNotificationIconSnapshot;
        Object objF6;
        Object objF7;
        PixelClockHostView.NotificationIcon notificationIconNotificationIconSnapshot2;
        Object obj2 = 0;
        Notification notification = statusBarNotification != null ? statusBarNotification.getNotification() : null;
        Icon smallIcon = notification != null ? notification.getSmallIcon() : null;
        if (smallIcon != null) {
            try {
                objF = XposedHelpers.callMethod(statusBarNotification, "getUser", new Object[0]);
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            if (objF instanceof sc1) {
                objF = null;
            }
            try {
                Object objCallMethod = XposedHelpers.callMethod(context, "createPackageContextAsUser", new Object[]{statusBarNotification.getPackageName(), obj2, objF});
                objCallMethod.getClass();
                objF2 = (Context) objCallMethod;
            } catch (Throwable th2) {
                objF2 = dr.f(th2);
            }
            if (tc1.a(objF2) != null) {
                try {
                    objF2 = context.createPackageContext(statusBarNotification.getPackageName(), 0);
                } catch (Throwable th3) {
                    objF2 = dr.f(th3);
                }
            }
            if (objF2 instanceof sc1) {
                objF2 = context;
            }
            Context context2 = (Context) objF2;
            try {
                Object objCallMethod2 = XposedHelpers.callMethod(statusBarNotification, "getUserId", new Object[0]);
                objCallMethod2.getClass();
                objF3 = Integer.valueOf(((Number) objCallMethod2).intValue());
            } catch (Throwable th4) {
                objF3 = dr.f(th4);
            }
            try {
                Object objCallMethod3 = XposedHelpers.callMethod(smallIcon, "loadDrawableAsUser", new Object[]{context2, Integer.valueOf(((Number) (objF3 instanceof sc1 ? 0 : objF3)).intValue())});
                objF4 = objCallMethod3 instanceof Drawable ? (Drawable) objCallMethod3 : null;
            } catch (Throwable th5) {
                objF4 = dr.f(th5);
            }
            if (objF4 instanceof sc1) {
                objF4 = null;
            }
            Drawable drawable = (Drawable) objF4;
            if (drawable == null) {
                try {
                    objF5 = smallIcon.loadDrawable(context2);
                } catch (Throwable th6) {
                    objF5 = dr.f(th6);
                }
                if (objF5 instanceof sc1) {
                    objF5 = null;
                }
                drawable = (Drawable) objF5;
            }
            if (drawable != null && (notificationIconNotificationIconSnapshot = notificationIconSnapshot(drawable, context)) != null) {
                return notificationIconNotificationIconSnapshot;
            }
        }
        try {
            objF6 = XposedHelpers.callMethod(obj, "getIcons", new Object[0]);
        } catch (Throwable th7) {
            objF6 = dr.f(th7);
        }
        if (objF6 instanceof sc1) {
            objF6 = null;
        }
        if (objF6 != null) {
            for (String str : NOTIFICATION_ENTRY_ICON_VIEW_METHODS) {
                try {
                    Object objCallMethod4 = XposedHelpers.callMethod(objF6, str, new Object[0]);
                    ImageView imageView = objCallMethod4 instanceof ImageView ? (ImageView) objCallMethod4 : null;
                    objF7 = imageView != null ? imageView.getDrawable() : null;
                } catch (Throwable th8) {
                    objF7 = dr.f(th8);
                }
                if (objF7 instanceof sc1) {
                    objF7 = null;
                }
                Drawable drawable2 = (Drawable) objF7;
                if (drawable2 != null && (notificationIconNotificationIconSnapshot2 = notificationIconSnapshot(drawable2, context)) != null) {
                    return notificationIconNotificationIconSnapshot2;
                }
            }
        }
        return null;
    }

    private final int resourceId(View view, String str) {
        for (String str2 : RESOURCE_PACKAGES) {
            int identifier = view.getResources().getIdentifier(str, "id", str2);
            if (identifier != 0) {
                return identifier;
            }
        }
        return 0;
    }

    private final PixelClockHostView.Scene sceneForClockSize(Integer num) {
        if (num != null && num.intValue() == 1) {
            return PixelClockHostView.Scene.LARGE;
        }
        if (num != null && num.intValue() == UI_STATE_KEYGUARD) {
            return PixelClockHostView.Scene.IMMERSED;
        }
        if (num != null && num.intValue() == 0) {
            return PixelClockHostView.Scene.SMALL;
        }
        return null;
    }

    public final void scheduleLockScreenNotificationRefresh(Object obj) {
        if (obj == null) {
            return;
        }
        for (long j : LOCKSCREEN_NOTIFICATION_SYNC_DELAYS) {
            Handler handler = this.mainHandler;
            l4 l4Var = new l4(6);
            l4Var.e = this;
            l4Var.f = obj;
            VarHandle.storeStoreFence();
            handler.postDelayed(l4Var, j);
        }
    }

    public static void scheduleLockScreenNotificationRefresh$default(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj, int i, Object obj2) {
        if ((i & 1) != 0) {
            WeakReference<Object> weakReference = pixelLockscreenClockHook.lockScreenNotificationDispatcher;
            obj = weakReference != null ? weakReference.get() : null;
        }
        pixelLockscreenClockHook.scheduleLockScreenNotificationRefresh(obj);
    }

    public final void scheduleNativeAodNotificationCapture(ViewGroup viewGroup) {
        if (viewGroup == null) {
            return;
        }
        int i = 0;
        for (long j : NATIVE_AOD_NOTIFICATION_SYNC_DELAYS) {
            y51 y51Var = new y51(i);
            y51Var.e = this;
            y51Var.f = viewGroup;
            VarHandle.storeStoreFence();
            viewGroup.postDelayed(y51Var, j);
        }
    }

    public static void scheduleNativeAodNotificationCapture$default(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup, int i, Object obj) {
        if ((i & 1) != 0) {
            WeakReference<ViewGroup> weakReference = pixelLockscreenClockHook.nativeAodNotificationLayout;
            viewGroup = weakReference != null ? weakReference.get() : null;
        }
        pixelLockscreenClockHook.scheduleNativeAodNotificationCapture(viewGroup);
    }

    private final void scheduleNativeAodNotificationRefresh(ViewGroup viewGroup) {
        if (viewGroup == null) {
            return;
        }
        for (long j : NATIVE_AOD_NOTIFICATION_SYNC_DELAYS) {
            y51 y51Var = new y51(1);
            y51Var.e = this;
            y51Var.f = viewGroup;
            VarHandle.storeStoreFence();
            viewGroup.postDelayed(y51Var, j);
        }
    }

    public static void scheduleNativeAodNotificationRefresh$default(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup, int i, Object obj) {
        if ((i & 1) != 0) {
            WeakReference<ViewGroup> weakReference = pixelLockscreenClockHook.nativeAodNotificationLayout;
            viewGroup = weakReference != null ? weakReference.get() : null;
        }
        pixelLockscreenClockHook.scheduleNativeAodNotificationRefresh(viewGroup);
    }

    public static final void scheduleNativeAodNotificationRefresh$lambda$0$0(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup) {
        boolean z;
        Object objF;
        Map<ViewGroup, PixelClockHostView> map = hosts;
        synchronized (map) {
            try {
                Collection<PixelClockHostView> collectionValues = map.values();
                z = true;
                if (!(collectionValues instanceof Collection) || !collectionValues.isEmpty()) {
                    Iterator<T> it = collectionValues.iterator();
                    while (it.hasNext()) {
                        if (((PixelClockHostView) it.next()).isPartialAodActive()) {
                            z = false;
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (z) {
            pixelLockscreenClockHook.captureNativeAodNotificationLayout(viewGroup);
            return;
        }
        try {
            XposedHelpers.callMethod(viewGroup, "clearNotificationView", new Object[0]);
            objF = XposedHelpers.callMethod(viewGroup, "updateCurrentNotification", new Object[0]);
        } catch (Throwable th2) {
            objF = dr.f(th2);
        }
        Throwable thA = tc1.a(objF);
        if (thA != null) {
            pixelLockscreenClockHook.logFailure("native-aod-notification-refresh", thA);
        }
        pixelLockscreenClockHook.captureNativeAodNotificationLayout(viewGroup);
    }

    public final void scheduleOplusAodNotificationRefresh(Object obj, boolean z) {
        if (obj == null) {
            return;
        }
        for (long j : AOD_NOTIFICATION_SYNC_DELAYS) {
            Handler handler = this.mainHandler;
            gk gkVar = new gk(1);
            gkVar.g = this;
            gkVar.e = obj;
            gkVar.f = z;
            VarHandle.storeStoreFence();
            handler.postDelayed(gkVar, j);
        }
    }

    public static void scheduleOplusAodNotificationRefresh$default(PixelLockscreenClockHook pixelLockscreenClockHook, Object obj, boolean z, int i, Object obj2) {
        if ((i & 1) != 0) {
            WeakReference<Object> weakReference = pixelLockscreenClockHook.oplusAodNotificationService;
            obj = weakReference != null ? weakReference.get() : null;
        }
        if ((i & UI_STATE_KEYGUARD) != 0) {
            z = false;
        }
        pixelLockscreenClockHook.scheduleOplusAodNotificationRefresh(obj, z);
    }

    /* JADX WARN: Code duplicated, block: B:47:0x00ba  */
    /* JADX WARN: Code duplicated, block: B:48:0x00bc  */
    /* JADX WARN: Code duplicated, block: B:50:0x00c2  */
    /* JADX WARN: Code duplicated, block: B:51:0x00c4  */
    /* JADX WARN: Code duplicated, block: B:53:0x00c7  */
    /* JADX WARN: Code duplicated, block: B:54:0x00d1  */
    /* JADX WARN: Code duplicated, block: B:61:0x00df  */
    /* JADX WARN: Code duplicated, block: B:62:0x00e1  */
    /* JADX WARN: Code duplicated, block: B:64:0x00e7  */
    /* JADX WARN: Code duplicated, block: B:65:0x00e9  */
    /* JADX WARN: Code duplicated, block: B:67:0x00ec  */
    /* JADX WARN: Code duplicated, block: B:68:0x00f1  */
    /* JADX WARN: Code duplicated, block: B:96:0x0137  */
    public final void styleAffordanceView(ImageView imageView, boolean z) {
        Drawable drawable;
        LayerDrawable layerDrawable;
        Drawable drawable2;
        LayerDrawable layerDrawable2;
        Drawable drawable3;
        Drawable drawableFindDrawableByLayerId;
        Context contextAffordancePaletteContext = affordancePaletteContext(imageView);
        boolean zIsDark = isDark(contextAffordancePaletteContext);
        AffordanceResourceIds affordanceResourceIds = affordanceResourceIds(imageView);
        int iResolveAndroidColor = resolveAndroidColor(contextAffordancePaletteContext, zIsDark ? affordanceResourceIds.getDarkBackgroundColor() : affordanceResourceIds.getLightBackgroundColor());
        int iResolveAndroidColor2 = resolveAndroidColor(contextAffordancePaletteContext, zIsDark ? affordanceResourceIds.getDarkForegroundColor() : affordanceResourceIds.getLightForegroundColor());
        AffordanceLayerState affordanceLayerState = null;
        try {
            XposedHelpers.setIntField(imageView, "mNormalColor", iResolveAndroidColor2);
            XposedHelpers.setIntField(imageView, "mDarkIconColor", iResolveAndroidColor2);
            XposedHelpers.setIntField(imageView, "mCircleColor", iResolveAndroidColor);
            Object objectField = XposedHelpers.getObjectField(imageView, "mCirclePaint");
            Paint paint = objectField instanceof Paint ? (Paint) objectField : null;
            if (paint != null) {
                paint.setColor(iResolveAndroidColor);
            }
        } catch (Throwable th) {
            dr.f(th);
        }
        Object additionalInstanceField = XposedHelpers.getAdditionalInstanceField(imageView, AFFORDANCE_BACKGROUND_FIELD);
        GradientDrawable gradientDrawable = additionalInstanceField instanceof GradientDrawable ? (GradientDrawable) additionalInstanceField : null;
        boolean z2 = true;
        if (gradientDrawable == null) {
            gradientDrawable = new GradientDrawable();
            gradientDrawable.setShape(1);
            XposedHelpers.setAdditionalInstanceField(imageView, AFFORDANCE_BACKGROUND_FIELD, gradientDrawable);
        }
        GradientDrawable gradientDrawable2 = gradientDrawable;
        gradientDrawable2.setColor(iResolveAndroidColor);
        if (imageView.getBackground() != gradientDrawable2) {
            imageView.setBackground(gradientDrawable2);
        }
        Drawable drawable4 = imageView.getDrawable();
        if (drawable4 == null) {
            return;
        }
        Drawable drawableMutate = drawable4.mutate();
        LayerDrawable layerDrawable3 = drawableMutate instanceof LayerDrawable ? (LayerDrawable) drawableMutate : null;
        int foreground = affordanceResourceIds.getForeground();
        int background = affordanceResourceIds.getBackground();
        int stroke = affordanceResourceIds.getStroke();
        Object additionalInstanceField2 = XposedHelpers.getAdditionalInstanceField(imageView, AFFORDANCE_LAYER_STATE_FIELD);
        AffordanceLayerState affordanceLayerState2 = additionalInstanceField2 instanceof AffordanceLayerState ? (AffordanceLayerState) additionalInstanceField2 : null;
        if (layerDrawable3 != null) {
            drawable = foreground != 0 ? layerDrawable3.findDrawableByLayerId(foreground) : null;
            if (drawable == null) {
                if (layerDrawable3 == null) {
                    drawable = null;
                } else {
                    if (layerDrawable3.getNumberOfLayers() > 0) {
                        layerDrawable = layerDrawable3;
                    } else {
                        layerDrawable = null;
                    }
                    if (layerDrawable != null) {
                        drawable = layerDrawable.getDrawable(layerDrawable.getNumberOfLayers() - 1);
                    } else {
                        drawable = null;
                    }
                }
            }
        } else if (layerDrawable3 == null) {
            drawable = null;
        } else {
            if (layerDrawable3.getNumberOfLayers() > 0) {
                layerDrawable = layerDrawable3;
            } else {
                layerDrawable = null;
            }
            if (layerDrawable != null) {
                drawable = layerDrawable.getDrawable(layerDrawable.getNumberOfLayers() - 1);
            } else {
                drawable = null;
            }
        }
        boolean z3 = false;
        if (layerDrawable3 != null) {
            drawable2 = background != 0 ? layerDrawable3.findDrawableByLayerId(background) : null;
            if (drawable2 == null) {
                if (layerDrawable3 == null) {
                    drawable2 = null;
                } else {
                    if (layerDrawable3.getNumberOfLayers() > 0) {
                        layerDrawable2 = layerDrawable3;
                    } else {
                        layerDrawable2 = null;
                    }
                    if (layerDrawable2 != null) {
                        drawable2 = layerDrawable2.getDrawable(0);
                    } else {
                        drawable2 = null;
                    }
                }
            }
        } else if (layerDrawable3 == null) {
            drawable2 = null;
        } else {
            if (layerDrawable3.getNumberOfLayers() > 0) {
                layerDrawable2 = layerDrawable3;
            } else {
                layerDrawable2 = null;
            }
            if (layerDrawable2 != null) {
                drawable2 = layerDrawable2.getDrawable(0);
            } else {
                drawable2 = null;
            }
        }
        if (drawable2 != null && drawable2 != gradientDrawable2) {
            drawable2.setAlpha(0);
        }
        if (layerDrawable3 != null && stroke != 0 && (drawableFindDrawableByLayerId = layerDrawable3.findDrawableByLayerId(stroke)) != null) {
            drawableFindDrawableByLayerId.setAlpha(0);
        }
        if (drawable == null) {
            ColorStateList imageTintList = imageView.getImageTintList();
            if (imageTintList == null || imageTintList.getDefaultColor() != iResolveAndroidColor2) {
                imageView.setImageTintList(ColorStateList.valueOf(iResolveAndroidColor2));
            }
            if (z) {
                imageView.invalidate();
                return;
            }
            return;
        }
        if (imageView.getImageTintList() != null) {
            imageView.setImageTintList(null);
        }
        if (affordanceLayerState2 == null) {
            PorterDuff.Mode mode = PorterDuff.Mode.SRC_IN;
            drawable3 = drawable;
            AffordanceLayerState affordanceLayerState3 = new AffordanceLayerState(drawable4, drawable3, gradientDrawable2, iResolveAndroidColor2, iResolveAndroidColor, new PorterDuffColorFilter(iResolveAndroidColor2, mode), new PorterDuffColorFilter(iResolveAndroidColor, mode));
            XposedHelpers.setAdditionalInstanceField(imageView, AFFORDANCE_LAYER_STATE_FIELD, affordanceLayerState3);
            affordanceLayerState = affordanceLayerState3;
        } else {
            if (affordanceLayerState2.getSource() == drawable4 && affordanceLayerState2.getForeground() == drawable && affordanceLayerState2.getBackground() == gradientDrawable2 && affordanceLayerState2.getForegroundColor() == iResolveAndroidColor2 && affordanceLayerState2.getBackgroundColor() == iResolveAndroidColor) {
                affordanceLayerState = affordanceLayerState2;
            }
            if (affordanceLayerState == null) {
                PorterDuff.Mode mode2 = PorterDuff.Mode.SRC_IN;
                drawable3 = drawable;
                AffordanceLayerState affordanceLayerState4 = new AffordanceLayerState(drawable4, drawable3, gradientDrawable2, iResolveAndroidColor2, iResolveAndroidColor, new PorterDuffColorFilter(iResolveAndroidColor2, mode2), new PorterDuffColorFilter(iResolveAndroidColor, mode2));
                XposedHelpers.setAdditionalInstanceField(imageView, AFFORDANCE_LAYER_STATE_FIELD, affordanceLayerState4);
                affordanceLayerState = affordanceLayerState4;
            } else {
                drawable3 = drawable;
            }
        }
        if (drawable3.getColorFilter() != affordanceLayerState.getForegroundFilter()) {
            drawable3.setColorFilter(affordanceLayerState.getForegroundFilter());
            z3 = true;
        }
        if (drawable3.getAlpha() != 255) {
            drawable3.setAlpha(255);
            z3 = true;
        }
        if (affordanceLayerState.getBackground().getColorFilter() != affordanceLayerState.getBackgroundFilter()) {
            affordanceLayerState.getBackground().setColorFilter(affordanceLayerState.getBackgroundFilter());
            z3 = true;
        }
        if (affordanceLayerState.getBackground().getAlpha() != 255) {
            affordanceLayerState.getBackground().setAlpha(255);
        } else {
            z2 = z3;
        }
        if (z2 && z) {
            imageView.invalidate();
        }
    }

    public static void styleAffordanceView$default(PixelLockscreenClockHook pixelLockscreenClockHook, ImageView imageView, boolean z, int i, Object obj) {
        if ((i & UI_STATE_KEYGUARD) != 0) {
            z = true;
        }
        pixelLockscreenClockHook.styleAffordanceView(imageView, z);
    }

    private final void suppressNativeAodNotificationLayout(ViewGroup viewGroup) {
        boolean z;
        if (viewGroup == null) {
            return;
        }
        Map<ViewGroup, PixelClockHostView> map = hosts;
        synchronized (map) {
            try {
                Collection<PixelClockHostView> collectionValues = map.values();
                z = false;
                if (!(collectionValues instanceof Collection) || !collectionValues.isEmpty()) {
                    Iterator<T> it = collectionValues.iterator();
                    while (it.hasNext()) {
                        if (((PixelClockHostView) it.next()).isAttachedToWindow()) {
                            z = true;
                            break;
                        }
                    }
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        if (!z || viewGroup.getAlpha() == 0.0f) {
            return;
        }
        viewGroup.setAlpha(0.0f);
    }

    public static void suppressNativeAodNotificationLayout$default(PixelLockscreenClockHook pixelLockscreenClockHook, ViewGroup viewGroup, int i, Object obj) {
        if ((i & 1) != 0) {
            WeakReference<ViewGroup> weakReference = pixelLockscreenClockHook.nativeAodNotificationLayout;
            viewGroup = weakReference != null ? weakReference.get() : null;
        }
        pixelLockscreenClockHook.suppressNativeAodNotificationLayout(viewGroup);
    }

    /* JADX WARN: Code duplicated, block: B:101:0x015d  */
    public final void syncHost(Object obj, boolean z) {
        PixelClockHostView pixelClockHostView;
        Object objCallMethod;
        Object objF;
        Integer num;
        Object objF2;
        boolean zBooleanValue;
        Integer numUpdateStableUiStateOrigin;
        boolean z2;
        boolean zBooleanValue2;
        Object obj2;
        boolean z3 = false;
        Object objCallMethod2 = XposedHelpers.callMethod(obj, "getView", new Object[]{0});
        ViewGroup viewGroup = objCallMethod2 instanceof ViewGroup ? (ViewGroup) objCallMethod2 : null;
        if (viewGroup == null || (pixelClockHostView = hosts.get(viewGroup)) == null || (objCallMethod = XposedHelpers.callMethod(obj, "getRenderedParams", new Object[0])) == null) {
            return;
        }
        Object objTrackerValue = trackerValue(objCallMethod, "getUiState");
        if (objTrackerValue != null) {
            try {
                Object objCallMethod3 = XposedHelpers.callMethod(objTrackerValue, "getUiState", new Object[0]);
                objCallMethod3.getClass();
                objF = Integer.valueOf(((Number) objCallMethod3).intValue());
            } catch (Throwable th) {
                objF = dr.f(th);
            }
            if (objF instanceof sc1) {
                objF = null;
            }
            num = (Integer) objF;
        } else {
            num = null;
        }
        if (objTrackerValue != null) {
            try {
                Object objCallMethod4 = XposedHelpers.callMethod(objTrackerValue, "isAnim", new Object[0]);
                objCallMethod4.getClass();
                objF2 = (Boolean) objCallMethod4;
            } catch (Throwable th2) {
                objF2 = dr.f(th2);
            }
            Object obj3 = Boolean.TRUE;
            if (objF2 instanceof sc1) {
                objF2 = obj3;
            }
            zBooleanValue = ((Boolean) objF2).booleanValue();
        } else {
            zBooleanValue = true;
        }
        boolean z4 = (num != null && num.intValue() == 3) || (num != null && num.intValue() == UI_STATE_PANORAMIC_AOD);
        boolean z5 = num != null && num.intValue() == 3;
        if (num != 0) {
            int iIntValue = num.intValue();
            Context context = viewGroup.getContext();
            context.getClass();
            numUpdateStableUiStateOrigin = updateStableUiStateOrigin(pixelClockHostView, iIntValue, context);
        } else {
            numUpdateStableUiStateOrigin = null;
        }
        Boolean boolPut = this.aodModeStates.put(pixelClockHostView, Boolean.valueOf(z4));
        Boolean bool = Boolean.TRUE;
        boolean zI = ph0.i(boolPut, bool);
        boolean zI2 = ph0.i(this.partialAodStates.put(pixelClockHostView, Boolean.valueOf(z5)), bool);
        if (z5 && !zI2) {
            WeakReference<Object> weakReference = this.oplusAodNotificationService;
            if (weakReference != null && (obj2 = weakReference.get()) != null) {
                refreshOplusAodNotifications(obj2, false);
            }
            scheduleNativeAodNotificationRefresh$default(this, null, 1, null);
            scheduleLockScreenNotificationRefresh$default(this, null, 1, null);
        }
        Object objTrackerValue2 = trackerValue(objCallMethod, "getClockSizeState");
        Number number = objTrackerValue2 instanceof Number ? (Number) objTrackerValue2 : null;
        PixelClockHostView.Scene sceneSceneForClockSize = sceneForClockSize(number != null ? Integer.valueOf(number.intValue()) : null);
        if (!z4 && sceneSceneForClockSize != null) {
            lastLockscreenScenes.put(pixelClockHostView, sceneSceneForClockSize);
        }
        if (z5) {
            sceneSceneForClockSize = PixelClockHostView.Scene.LARGE;
        } else if (num != 0 && num.intValue() == UI_STATE_PANORAMIC_AOD && sceneSceneForClockSize == null && (sceneSceneForClockSize = lastLockscreenScenes.get(pixelClockHostView)) == null) {
            sceneSceneForClockSize = PixelClockHostView.Scene.LARGE;
        }
        PixelClockHostView.Scene scene = sceneSceneForClockSize;
        Boolean boolPendingSleepOriginOrNull = pendingSleepOriginOrNull();
        if (!z4 || zI) {
            z2 = false;
        } else {
            if (boolPendingSleepOriginOrNull != null) {
                zBooleanValue2 = boolPendingSleepOriginOrNull.booleanValue();
            } else {
                zBooleanValue2 = numUpdateStableUiStateOrigin != null && numUpdateStableUiStateOrigin.intValue() == 1;
            }
            if (zBooleanValue2) {
                z2 = true;
            } else {
                z2 = false;
            }
        }
        if (num != 0 && num.intValue() == UI_STATE_PANORAMIC_AOD) {
            if (z && zBooleanValue) {
                z3 = true;
            }
            deferToAuthoritativePanoramicState(viewGroup, z2, z3);
        } else if (z2 && scene != null) {
            PixelClockHostView.AodContent aodContentResolveAodContent = resolveAodContent(z5);
            if (z5) {
                scene = aodContentResolveAodContent instanceof PixelClockHostView.AodContent.None ? PixelClockHostView.Scene.LARGE : PixelClockHostView.Scene.SMALL;
            }
            if (z && zBooleanValue) {
                z3 = true;
            }
            pixelClockHostView.beginAodEntry(scene, z5, aodContentResolveAodContent, z3);
            String simpleName = aodContentResolveAodContent.getClass().getSimpleName();
            StringBuilder sb = new StringBuilder("AOD entry normalized: panoramic=");
            sb.append(!z5);
            sb.append(", scene=");
            sb.append(scene);
            sb.append(", content=");
            sb.append(simpleName);
            log(sb.toString());
        } else if (scene != null && num != 0) {
            pendingPanoramicSyncs.remove(viewGroup);
            pixelClockHostView.setPresentation(scene, z4, z5, resolveAodContent(z5), z && zBooleanValue);
        }
        pixelClockHostView.onTimeTick();
        PixelClockHostView.updateBattery$default(pixelClockHostView, null, 1, null);
        markOriginalVisualContainers(obj);
        if (PixelClockHostView.shouldRefreshInformation$default(pixelClockHostView, 0L, 1, null)) {
            pixelClockHostView.updateMonetColor();
            mirrorInformation(obj, pixelClockHostView);
        }
        if (viewGroup.getChildAt(viewGroup.getChildCount() - 1) != pixelClockHostView) {
            pixelClockHostView.bringToFront();
        }
    }

    private final Object trackerValue(Object obj, String str) {
        Object objCallMethod = XposedHelpers.callMethod(obj, str, new Object[0]);
        if (objCallMethod == null) {
            return null;
        }
        return XposedHelpers.callMethod(objCallMethod, "getValue", new Object[0]);
    }

    private final void tryInstallKnownClasses(ClassLoader classLoader) {
        boolean z;
        Class<?> clsFindClassIfExists;
        Object objF;
        Object objF2;
        if (this.clockEnabled && !clockPluginHooked) {
            Class<?> clsFindClassIfExists2 = XposedHelpers.findClassIfExists(HOST_CLOCK_PLUGIN_CLASS, classLoader);
            if (clsFindClassIfExists2 == null) {
                sb0 sb0Var = vb0.a;
                vb0.j(TAG, "clock-host-class-missing", "target=com.oplus.keyguard.plugin.ClockPlugin loader=".concat(classLoader.getClass().getName()), null, 0, 56);
            } else {
                try {
                    hookClockPluginClass(clsFindClassIfExists2);
                    objF2 = l22.a;
                } catch (Throwable th) {
                    objF2 = dr.f(th);
                }
                if (!(objF2 instanceof sc1)) {
                    sb0 sb0Var2 = vb0.a;
                    vb0.a(TAG, "install-part", "name=OPlus clock host status=installed");
                }
                Throwable thA = tc1.a(objF2);
                if (thA != null) {
                    sb0 sb0Var3 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-OPlus clock host", "part installation failed", thA);
                }
            }
        }
        if (this.affordanceColorsEnabled) {
            for (String str : AFFORDANCE_VIEW_CLASSES) {
                Set<Class<?>> set = hookedAffordanceClasses;
                synchronized (set) {
                    try {
                        Set<Class<?>> set2 = set;
                        z = false;
                        if (!(set2 instanceof Collection) || !set2.isEmpty()) {
                            Iterator<T> it = set2.iterator();
                            while (it.hasNext()) {
                                if (((Class) it.next()).getName().equals(str)) {
                                    z = true;
                                    break;
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        throw th2;
                    }
                }
                if (!z && (clsFindClassIfExists = XposedHelpers.findClassIfExists(str, classLoader)) != null) {
                    try {
                        hookAffordanceClass(clsFindClassIfExists);
                        objF = l22.a;
                    } catch (Throwable th3) {
                        objF = dr.f(th3);
                    }
                    if (!(objF instanceof sc1)) {
                        sb0 sb0Var4 = vb0.a;
                        vb0.a(TAG, "install-part", "name=keyguard affordance style status=installed");
                    }
                    Throwable thA2 = tc1.a(objF);
                    if (thA2 != null) {
                        sb0 sb0Var5 = vb0.a;
                        vb0.e(sb0.j, TAG, "install-keyguard affordance style", "part installation failed", thA2);
                    }
                }
            }
        }
    }

    private final void updateMediaNotificationIconStates(List<String> list, List<? extends Drawable.ConstantState> list2) {
        Map mapL0;
        Drawable.ConstantState constantState;
        synchronized (this.mediaNotificationIconStates) {
            mapL0 = js0.l0(this.mediaNotificationIconStates);
        }
        cs0 cs0Var = new cs0(8);
        list.getClass();
        list2.getClass();
        Iterator<T> it = list.iterator();
        Iterator<T> it2 = list2.iterator();
        ArrayList arrayList = new ArrayList(Math.min(vk.h0(list), vk.h0(list2)));
        while (it.hasNext() && it2.hasNext()) {
            arrayList.add(new k31(it.next(), it2.next()));
        }
        int size = arrayList.size();
        int i = 0;
        while (i < size) {
            Object obj = arrayList.get(i);
            i++;
            k31 k31Var = (k31) obj;
            String str = (String) k31Var.d;
            Drawable.ConstantState constantState2 = (Drawable.ConstantState) k31Var.e;
            Drawable.ConstantState constantState3 = (Drawable.ConstantState) mapL0.get(str);
            if (constantState3 != null) {
                constantState2 = constantState3;
            }
            cs0Var.put(str, constantState2);
        }
        cs0Var.b();
        cs0Var.p = true;
        if (cs0Var.l <= 0) {
            cs0Var = cs0.q;
            cs0Var.getClass();
        }
        synchronized (this.mediaNotificationIconStates) {
            this.mediaNotificationIconStates.clear();
            this.mediaNotificationIconStates.putAll(cs0Var);
        }
        ActiveMedia activeMedia = this.activeMedia;
        if (activeMedia == null || (constantState = (Drawable.ConstantState) cs0Var.get(activeMedia.getPackageName())) == null || activeMedia.getAppIconState() == constantState) {
            return;
        }
        this.activeMedia = ActiveMedia.copy$default(activeMedia, null, null, null, constantState, 7, null);
        refreshActiveAodContent$default(this, false, false, UI_STATE_KEYGUARD, null);
    }

    public final void updateNewRenderAodNotificationIcons(Context context, String[] strArr, Drawable[] drawableArr) {
        strArr.getClass();
        List<String> listAsList = Arrays.asList(strArr);
        listAsList.getClass();
        drawableArr.getClass();
        List<? extends Drawable> listAsList2 = Arrays.asList(drawableArr);
        listAsList2.getClass();
        int length = strArr.length;
        int length2 = drawableArr.length;
        applyNewRenderAodNotificationIcons(context, listAsList, listAsList2, length < length2 ? length2 : length, true);
    }

    private final Integer updateStableUiStateOrigin(PixelClockHostView pixelClockHostView, int i, Context context) {
        Integer numValueOf = Integer.valueOf(UI_STATE_KEYGUARD);
        if (i == 1) {
            stableUiStateOrigins.put(pixelClockHostView, 1);
            this.lastObservedStableUiState = 1;
        } else if (i == UI_STATE_KEYGUARD) {
            PowerManager powerManager = (PowerManager) context.getSystemService(PowerManager.class);
            if ((powerManager != null ? powerManager.isInteractive() : true) || stableUiStateOrigins.get(pixelClockHostView) == null) {
                stableUiStateOrigins.put(pixelClockHostView, numValueOf);
                this.lastObservedStableUiState = numValueOf;
            }
        }
        return stableUiStateOrigins.get(pixelClockHostView);
    }

    public final float wallpaperDimPercent(Context context) {
        return isSystemNightMode(context) ? this.darkWallpaperDimPercent : this.wallpaperDimPercent;
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Object objF;
        Object objF2;
        Object objF3;
        Object objF4;
        Object objF5;
        Object objF6;
        Object objF7;
        Object objF8;
        loadPackageParam.getClass();
        if (ph0.i(loadPackageParam.packageName, SYSTEM_UI_PACKAGE) || ph0.i(loadPackageParam.processName, SYSTEM_UI_PACKAGE)) {
            this.systemUiClassLoader = loadPackageParam.classLoader;
            ConfigStore configStore = ConfigStore.INSTANCE;
            XSharedPreferences xSharedPreferences = new XSharedPreferences(ConfigStore.MODULE_PACKAGE, ConfigStore.PREFS_NAME);
            xSharedPreferences.reload();
            ct ctVarLoad = configStore.load(xSharedPreferences);
            this.clockEnabled = ((Boolean) ctVarLoad.a(aq.c)).booleanValue();
            this.wallpaperDimPercent = nr.p(((Number) ctVarLoad.a(aq.d)).floatValue(), 0.0f, 100.0f);
            this.darkWallpaperDimPercent = nr.p(((Number) ctVarLoad.a(aq.e)).floatValue(), 0.0f, 100.0f);
            this.aodBatteryYOffset = nr.p(((Number) ctVarLoad.a(aq.f)).floatValue(), -64.0f, 64.0f);
            this.aodBatteryEnabled = ((Boolean) ctVarLoad.a(aq.g)).booleanValue();
            boolean zBooleanValue = ((Boolean) ctVarLoad.a(aq.h)).booleanValue();
            this.affordanceColorsEnabled = zBooleanValue;
            sb0 sb0Var = vb0.a;
            String str = loadPackageParam.packageName;
            String str2 = loadPackageParam.processName;
            boolean z = this.clockEnabled;
            float f = this.wallpaperDimPercent;
            float f2 = this.darkWallpaperDimPercent;
            boolean z2 = this.aodBatteryEnabled;
            float f3 = this.aodBatteryYOffset;
            StringBuilder sbT = x30.t("package=", str, " process=", str2, " clock=");
            sbT.append(z);
            sbT.append(" wallpaperDim=");
            sbT.append(f);
            sbT.append(" darkWallpaperDim=");
            sbT.append(f2);
            sbT.append(" aodBattery=");
            sbT.append(z2);
            sbT.append(" aodBatteryYOffset=");
            sbT.append(f3);
            sbT.append(" affordanceColors=");
            sbT.append(zBooleanValue);
            vb0.f(TAG, ConfigStore.PREFS_NAME, sbT.toString());
            boolean z3 = this.clockEnabled;
            Object objF9 = l22.a;
            if (z3 && ph0.i(loadPackageParam.processName, SYSTEM_UI_PACKAGE) && (this.wallpaperDimPercent > 0.0f || this.darkWallpaperDimPercent > 0.0f)) {
                try {
                    ClassLoader classLoader = loadPackageParam.classLoader;
                    classLoader.getClass();
                    hookLockscreenDimOverlay(classLoader);
                    objF8 = objF9;
                } catch (Throwable th) {
                    objF8 = dr.f(th);
                }
                if (!(objF8 instanceof sc1)) {
                    sb0 sb0Var2 = vb0.a;
                    vb0.a(TAG, "install-part", "name=lockscreen wallpaper dim overlay status=installed");
                }
                Throwable thA = tc1.a(objF8);
                if (thA != null) {
                    sb0 sb0Var3 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-lockscreen wallpaper dim overlay", "part installation failed", thA);
                }
            }
            ClassLoader classLoader2 = loadPackageParam.classLoader;
            classLoader2.getClass();
            tryInstallKnownClasses(classLoader2);
            if (this.affordanceColorsEnabled) {
                try {
                    hookAffordancePaletteRefresh();
                    objF = objF9;
                } catch (Throwable th2) {
                    objF = dr.f(th2);
                }
                if (!(objF instanceof sc1)) {
                    sb0 sb0Var4 = vb0.a;
                    vb0.a(TAG, "install-part", "name=keyguard affordance palette refresh status=installed");
                }
                Throwable thA2 = tc1.a(objF);
                if (thA2 != null) {
                    sb0 sb0Var5 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-keyguard affordance palette refresh", "part installation failed", thA2);
                }
            }
            if (this.clockEnabled) {
                try {
                    ClassLoader classLoader3 = loadPackageParam.classLoader;
                    classLoader3.getClass();
                    hookNotificationPipeline(classLoader3);
                    objF2 = objF9;
                } catch (Throwable th3) {
                    objF2 = dr.f(th3);
                }
                if (!(objF2 instanceof sc1)) {
                    sb0 sb0Var6 = vb0.a;
                    vb0.a(TAG, "install-part", "name=AOD notification pipeline status=installed");
                }
                Throwable thA3 = tc1.a(objF2);
                if (thA3 != null) {
                    sb0 sb0Var7 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-AOD notification pipeline", "part installation failed", thA3);
                }
                try {
                    ClassLoader classLoader4 = loadPackageParam.classLoader;
                    classLoader4.getClass();
                    hookLockScreenNotificationStateFlow(classLoader4);
                    objF3 = objF9;
                } catch (Throwable th4) {
                    objF3 = dr.f(th4);
                }
                if (!(objF3 instanceof sc1)) {
                    sb0 sb0Var8 = vb0.a;
                    vb0.a(TAG, "install-part", "name=lockscreen notification state flow status=installed");
                }
                Throwable thA4 = tc1.a(objF3);
                if (thA4 != null) {
                    sb0 sb0Var9 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-lockscreen notification state flow", "part installation failed", thA4);
                }
                try {
                    ClassLoader classLoader5 = loadPackageParam.classLoader;
                    classLoader5.getClass();
                    hookOplusAodNotificationSource(classLoader5);
                    objF4 = objF9;
                } catch (Throwable th5) {
                    objF4 = dr.f(th5);
                }
                if (!(objF4 instanceof sc1)) {
                    sb0 sb0Var10 = vb0.a;
                    vb0.a(TAG, "install-part", "name=OPlus partial-AOD notification source status=installed");
                }
                Throwable thA5 = tc1.a(objF4);
                if (thA5 != null) {
                    sb0 sb0Var11 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-OPlus partial-AOD notification source", "part installation failed", thA5);
                }
                try {
                    ClassLoader classLoader6 = loadPackageParam.classLoader;
                    classLoader6.getClass();
                    hookNativeAodNotificationLayout(classLoader6);
                    objF5 = objF9;
                } catch (Throwable th6) {
                    objF5 = dr.f(th6);
                }
                if (!(objF5 instanceof sc1)) {
                    sb0 sb0Var12 = vb0.a;
                    vb0.a(TAG, "install-part", "name=OPlus native partial-AOD notification layout status=installed");
                }
                Throwable thA6 = tc1.a(objF5);
                if (thA6 != null) {
                    sb0 sb0Var13 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-OPlus native partial-AOD notification layout", "part installation failed", thA6);
                }
                try {
                    ClassLoader classLoader7 = loadPackageParam.classLoader;
                    classLoader7.getClass();
                    hookNewRenderAodNotificationSource(classLoader7);
                    objF6 = objF9;
                } catch (Throwable th7) {
                    objF6 = dr.f(th7);
                }
                if (!(objF6 instanceof sc1)) {
                    sb0 sb0Var14 = vb0.a;
                    vb0.a(TAG, "install-part", "name=OPlus new-render partial-AOD notification source status=installed");
                }
                Throwable thA7 = tc1.a(objF6);
                if (thA7 != null) {
                    sb0 sb0Var15 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-OPlus new-render partial-AOD notification source", "part installation failed", thA7);
                }
                try {
                    ClassLoader classLoader8 = loadPackageParam.classLoader;
                    classLoader8.getClass();
                    hookPartialAodDirectSuspendTransition(classLoader8);
                    objF7 = objF9;
                } catch (Throwable th8) {
                    objF7 = dr.f(th8);
                }
                if (!(objF7 instanceof sc1)) {
                    sb0 sb0Var16 = vb0.a;
                    vb0.a(TAG, "install-part", "name=OPlus partial-AOD direct suspend transition status=installed");
                }
                Throwable thA8 = tc1.a(objF7);
                if (thA8 != null) {
                    sb0 sb0Var17 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-OPlus partial-AOD direct suspend transition", "part installation failed", thA8);
                }
                try {
                    ClassLoader classLoader9 = loadPackageParam.classLoader;
                    classLoader9.getClass();
                    hookAodScreenOffOrigin(classLoader9);
                } catch (Throwable th9) {
                    objF9 = dr.f(th9);
                }
                if (!(objF9 instanceof sc1)) {
                    sb0 sb0Var18 = vb0.a;
                    vb0.a(TAG, "install-part", "name=AOD screen-off origin latch status=installed");
                }
                Throwable thA9 = tc1.a(objF9);
                if (thA9 != null) {
                    sb0 sb0Var19 = vb0.a;
                    vb0.e(sb0.j, TAG, "install-AOD screen-off origin latch", "part installation failed", thA9);
                }
            }
        }
    }

    public static final class Companion {
        public Companion(zt ztVar) {
            this();
        }

        public final jb1 ROOT_STATE_BOOLEAN_REGEX_TEMPLATE(String str) {
            str.getClass();
            String strQuote = Pattern.quote(str);
            strQuote.getClass();
            return new jb1("(?:^|[, (])" + strQuote + "=(true|false)");
        }

        public final jb1 ROOT_STATE_INT_REGEX_TEMPLATE(String str) {
            str.getClass();
            String strQuote = Pattern.quote(str);
            strQuote.getClass();
            return new jb1("(?:^|[, (])" + strQuote + "=(-?\\d+)");
        }

        private Companion() {
        }
    }
}
