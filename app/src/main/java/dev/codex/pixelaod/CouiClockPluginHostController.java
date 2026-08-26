package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * COUI_PORT ClockPlugin bridge.
 *
 * <p>This controller owns a separate host map and only observes the narrow OPlus lifecycle seam.
 * It never constructs a legacy host and attaches/suppresses native visuals in one main-thread
 * transaction.</p>
 */
final class CouiClockPluginHostController {
    private static final String CLOCK_PLUGIN_CLASS = "com.oplus.keyguard.plugin.ClockPlugin";
    private static final String BIG_CLOCK_LOGICAL_PACKAGE = "com.oplus.keyguard.clock.big";
    private static final String HOST_TAG = "dev.codex.pixelaod.COUI_CLOCK_PLUGIN_HOST";
    private static final String NATIVE_VISUAL_CONTAINER_CLASS =
            "com.oplus.keyguard.clock.big.widget.MyCustomizedFrameLayout";
    private static final int VIEW_CLOCK_TIME = 1;
    private static final int VIEW_DATE_MESSAGE = 11;
    private static final CouiClockPresentationModel.AodContent DEFAULT_AOD_CONTENT =
            CouiClockPresentationModel.AodContent.none();

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Set<Method> HOOKED_METHODS =
            Collections.newSetFromMap(new WeakHashMap<Method, Boolean>());
    private static final Set<Class<?>> NATIVE_DRAW_HOOKED_CLASSES =
            Collections.newSetFromMap(new WeakHashMap<Class<?>, Boolean>());
    private static final Map<ViewGroup, HostRecord> HOSTS =
            Collections.synchronizedMap(new WeakHashMap<ViewGroup, HostRecord>());
    private static final Map<View, WeakReference<HostRecord>> NATIVE_DRAW_BINDINGS =
            Collections.synchronizedMap(new WeakHashMap<View, WeakReference<HostRecord>>());
    private static final Set<Object> ROOT_UNAVAILABLE_LOGGED =
            Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());
    private static long nextHostGeneration;

    private static volatile boolean hooksInstalled;
    private static volatile ClassLoader hookClassLoader;
    private CouiClockPluginHostController() {
    }

    static void install(Context context, ClassLoader classLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        hookClassLoader = classLoader;
        try {
            Class<?> pluginClass = ModernHookBridge.findClass(CLOCK_PLUGIN_CLASS, classLoader);
            int hooks = hookPluginClass(pluginClass);
            hooksInstalled = hooks > 0;
            CouiClockSemanticAdapter.install(context, classLoader,
                    () -> refreshAll("COUI-semantic-data"));
            PixelAodLog.log("COUI clock persistent-host hooks installed=" + hooksInstalled
                    + " methods=" + hooks + " class=" + pluginClass.getName()
                    + " rendererMode=COUI_PORT temporaryAodContent=NONE"
                    + " retainedAdapters=DEFERRED_TO_SLICE3");
        } catch (Throwable t) {
            hooksInstalled = false;
            PixelAodLog.log("COUI ClockPlugin path unavailable", t);
        }
    }

    static boolean isHookInstalled() {
        return hooksInstalled;
    }

    static boolean hasValidatedHost() {
        synchronized (HOSTS) {
            for (HostRecord record : HOSTS.values()) {
                if (record != null && record.host.getParent() == record.root) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Data refresh only. COUI 2.5 never re-runs ClockPlugin presentation state from unrelated
     * notification/weather/policy callbacks; presentation changes belong to loadPluginReal/render.
     */

    static void refreshAll(String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record == null || record.host.getParent() != record.root) {
                    continue;
                }
                CouiClockSemanticAdapter.Snapshot semantic =
                        CouiClockSemanticAdapter.snapshot(record.root.getContext());
                applySemanticData(record, semantic, source + "#semantic-only");
                if (semantic != null && record.host.presentation().dozing()) {
                    CouiClockPresentationModel.AodContent currentContent =
                            record.host.presentation().content();
                    if (CouiClockAodTransitionPolicy.shouldRetargetLiveContent(
                            currentContent, semantic.content)) {
                        record.host.setLiveAodContent(semantic.content, true,
                                source + "#semantic-content");
                    }
                }
            }
        });
    }

    /**
     * ClockPlugin render owns the AOD-to-lockscreen presentation handoff. Waking before a
     * pre-armed non-lockscreen AOD has reached a real AOD state also cancels that pre-arm so a
     * later normal lockscreen render cannot inherit it.
     */
    static void prepareAodToLockscreenTransition(String source) {
        runOnMain(() -> {
            int cleared = 0;
            for (HostRecord record : snapshotRecords()) {
                if (record != null && (record.nonLockscreenAodPrearmed
                        || record.nonLockscreenAodSceneGateBypass)) {
                    record.nonLockscreenAodPrearmed = false;
                    record.nonLockscreenAodSceneGateBypass = false;
                    record.nonLockscreenAodTransitionMode =
                            NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
                    record.prearmedAodScene = null;
                    cleared++;
                }
            }
            PixelAodLog.log("COUI AOD exit pre-arm ignored rendererMode=COUI_PORT"
                    + " owner=ClockPlugin#render"
                    + " clearedNonLockscreenPrearms=" + cleared
                    + " source=" + source);
        });
    }

    /**
     * The screen-off-from-unlocked path must park the persistent host on its normalized first
     * frame before OPlus starts exposing the keyguard/AOD root. OPlus can publish one or more
     * stale UNLOCKED/KEYGUARD renders between dispatchStartedGoingToSleep and the real AOD render;
     * those renders must not animate the parked host away from this baseline.
     */
    static void prepareNonLockscreenAodEntry(String source) {
        runOnMain(() -> prepareNonLockscreenAodEntryOnMain(source));
    }

    /**
     * Binder onStartedGoingToSleep arrives before the first stale ClockPlugin render on OPlus.
     * From that binder thread, enqueue this once at the front of the main queue so the normalized
     * first frame is parked before an already-queued UNLOCKED render can start a 550 ms morph.
     */
    static void prepareNonLockscreenAodEntryEarly(String source) {
        runOnMainFront(() -> prepareNonLockscreenAodEntryOnMain(source));
    }

    private static void prepareNonLockscreenAodEntryOnMain(String source) {
        for (HostRecord record : snapshotRecords()) {
            if (record == null || record.host.getParent() != record.root) {
                continue;
            }
            Context context = record.root.getContext();
            if (context == null) {
                continue;
            }
            if (!CouiClockNonLockscreenAodPrearmPolicy.shouldPrepare(
                    record.nonLockscreenAodPrearmed)) {
                PixelAodLog.log("coui-non-lockscreen-prearm-idempotent", () ->
                        "COUI non-lockscreen AOD pre-arm already active"
                                + " rendererMode=COUI_PORT"
                                + " rootId=" + identity(record.root)
                                + " parkedScene=" + record.prearmedAodScene
                                + " source=" + source);
                continue;
            }
            CouiClockSemanticAdapter.Snapshot semantic =
                    CouiClockSemanticAdapter.snapshot(context);
            applySemanticData(record, semantic, source + "#pre-aod-data#semantic");
            CouiClockPresentationModel.AodContent content = semantic != null
                    ? semantic.content : DEFAULT_AOD_CONTENT;
            CouiClockPresentationModel normalizedEntry =
                    CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(
                            new CouiClockPresentationModel(
                                    CouiClockPresentationModel.Scene.LARGE,
                                    true,
                                    true,
                                    content));
            NonLockscreenAodTransitionPolicy.Mode transitionMode =
                    NonLockscreenAodTransitionPolicy.resolve(context);
            if (NonLockscreenAodTransitionPolicy.isDirectFinal(transitionMode)) {
                record.host.prearmNonLockscreenAodFinalEntry(
                        normalizedEntry, source + "#prearm-final-frame");
            } else {
                record.host.prearmNonLockscreenAodEntry(
                        normalizedEntry, source + "#prearm-first-frame");
            }
            record.nonLockscreenAodPrearmed = true;
            record.nonLockscreenAodSceneGateBypass = true;
            record.nonLockscreenAodTransitionMode = transitionMode;
            record.prearmedAodScene = normalizedEntry.requestedScene();
            PixelAodLog.log("COUI non-lockscreen AOD first frame pre-armed"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " scene=" + normalizedEntry.requestedScene()
                    + " contentKind=" + normalizedEntry.content().kind()
                    + " transitionMode=" + transitionMode
                    + " sceneGateBypass=true"
                    + " source=" + source);
        }
    }

    /** External lifecycle callbacks may refresh data but must not synthesize lockscreen state. */
    static void prepareLockscreenEntry(String source) {
        refreshAll(source + "#pre-lockscreen-data");
    }

    static void suppressForDirectGone(String source) {
        runOnMain(() -> {
            int hidden = 0;
            for (HostRecord record : snapshotRecords()) {
                if (record == null || record.host.getParent() != record.root) {
                    continue;
                }
                record.nonLockscreenAodPrearmed = false;
                record.nonLockscreenAodSceneGateBypass = false;
                record.nonLockscreenAodTransitionMode =
                        NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
                record.prearmedAodScene = null;
                record.aodExitHandoffPending = false;
                // Native teardown owns the real Gone transition. Keep its AOD clock suppressed
                // until teardown rather than restoring a stock clock that could flash for a frame.
                record.suppressNativeDraw = true;
                suppressNativeVisuals(record);
                record.host.setPrimaryVisible(false, source + "#direct-gone");
                hidden++;
            }
            PixelAodLog.log("COUI direct-to-Gone suppression rendererMode=COUI_PORT"
                    + " hiddenHosts=" + hidden
                    + " source=" + source);
        });
    }

    static void suppressForNativeScene(String source) {
        runOnMain(() -> {
            if (PixelAodRuntimeState.nativeKeyguardSceneUsesCouiHostVisibility()) {
                PixelAodLog.i("COUI bouncer visibility delegated to native ClockViewRoot"
                        + " rendererMode=COUI_PORT action=keep-child-state"
                        + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                        + " source=" + source);
                return;
            }
            int hidden = 0;
            for (HostRecord record : snapshotRecords()) {
                if (record == null || record.host.getParent() != record.root) {
                    continue;
                }
                suppressRecordForNativeScene(record, source);
                hidden++;
            }
            PixelAodLog.i("COUI native-scene suppression rendererMode=COUI_PORT"
                    + " hiddenHosts=" + hidden
                    + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                    + " source=" + source);
        });
    }

    private static void suppressRecordForNativeScene(HostRecord record, String source) {
        record.nonLockscreenAodPrearmed = false;
        record.nonLockscreenAodSceneGateBypass = false;
        record.nonLockscreenAodTransitionMode = NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
        record.prearmedAodScene = null;
        record.aodExitHandoffPending = false;
        record.suppressNativeDraw = true;
        suppressNativeVisuals(record);
        record.host.setPrimaryVisible(false, source + "#native-scene");
    }

    private static boolean nativeSceneAllowsRecord(HostRecord record) {
        if (PixelAodRuntimeState.nativeKeyguardSceneAllowsPresentation()) {
            return true;
        }
        return record != null
                && record.nonLockscreenAodSceneGateBypass
                && PixelAodRuntimeState.nativeKeyguardSceneSupportsNonLockscreenAodBypass();
    }

    static void resyncForNativeScene(String source) {
        runOnMain(() -> {
            if (PixelAodRuntimeState.nativeKeyguardSceneUsesCouiHostVisibility()) {
                PixelAodLog.i("COUI bouncer visibility delegated to native ClockViewRoot"
                        + " rendererMode=COUI_PORT action=skip-child-resync"
                        + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                        + " source=" + source);
                return;
            }
            if (!PixelAodRuntimeState.nativeKeyguardSceneAllowsPresentation()) {
                return;
            }
            int synced = 0;
            int releasedNonLockscreenBypasses = 0;
            for (HostRecord record : snapshotRecords()) {
                if (record == null || record.host.getParent() != record.root) {
                    continue;
                }
                if (record.nonLockscreenAodSceneGateBypass) {
                    record.nonLockscreenAodSceneGateBypass = false;
                    releasedNonLockscreenBypasses++;
                    continue;
                }
                Object plugin = record.plugin != null ? record.plugin.get() : null;
                if (plugin == null) {
                    continue;
                }
                syncHost(record, plugin, source + "#native-scene-resync", false);
                synced++;
            }
            PixelAodLog.i("COUI native-scene resync rendererMode=COUI_PORT"
                    + " syncedHosts=" + synced
                    + " releasedNonLockscreenBypasses=" + releasedNonLockscreenBypasses
                    + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                    + " source=" + source);
        });
    }

    static void refreshSemanticData(String source) {
        refreshAll(source + "#semantic");
    }

    static void setAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setAodContent(content, animate);
                    logHostState(record, source, animate, record.host.presentation());
                }
            }
        });
    }

    static void setLiveAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setLiveAodContent(content, animate, source);
                    logHostState(record, source, animate, record.host.presentation());
                }
            }
        });
    }

    /** Data-only seam for the retained weather/date/calendar adapters planned for Slice 3. */
    static void setInformation(CharSequence date, CharSequence week, CharSequence weather,
            Drawable weatherIcon, String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setInformation(date, week, weather, weatherIcon);
                }
            }
            PixelAodLog.log("COUI clock information data updated rendererMode=COUI_PORT"
                    + " source=" + source + " adapters=DATA_ONLY");
        });
    }

    /** Reuses existing semantic adapters without creating a legacy visual owner. */
    static void refreshInformationFromExistingAdapters(String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.refreshInformationFromExistingAdapters(source);
                }
            }
            PixelAodLog.log("COUI clock information refresh rendererMode=COUI_PORT"
                    + " source=" + source + " adapters=EXISTING_MODULE_SEMANTICS");
        });
    }

    /** Data-only seam for the retained notification adapter planned for Slice 3. */
    static void setNotificationIcons(List<? extends Drawable> icons, String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setNotificationIcons(icons);
                }
            }
            PixelAodLog.log("COUI clock notification data updated rendererMode=COUI_PORT"
                    + " iconCount=" + (icons == null ? 0 : icons.size())
                    + " source=" + source + " adapters=DATA_ONLY");
        });
    }

    /** Data-only seam for the retained media adapter planned for Slice 3. */
    static void setMediaData(CharSequence title, CharSequence artist, Drawable appIcon,
            String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setMediaData(title, artist, appIcon);
                }
            }
            PixelAodLog.log("COUI clock media data updated rendererMode=COUI_PORT"
                    + " source=" + source + " adapters=DATA_ONLY");
        });
    }

    /** Input seam for the retained burn-in source planned for Slice 3. */
    static void setBurnInTranslation(float x, float y, long durationMillis, String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.setBurnInTranslation(x, y, durationMillis);
                }
            }
            PixelAodLog.log("COUI clock burn-in input updated rendererMode=COUI_PORT"
                    + " x=" + x + " y=" + y + " durationMs=" + durationMillis
                    + " source=" + source);
        });
    }

    static void onTimeTick(String source) {
        runOnMain(() -> {
            for (HostRecord record : snapshotRecords()) {
                if (record != null && record.host.getParent() == record.root) {
                    record.host.onTimeTick();
                }
            }
            PixelAodLog.log("COUI clock time tick rendererMode=COUI_PORT source=" + source);
        });
    }

    private static int hookPluginClass(Class<?> pluginClass) {
        int hooks = 0;
        for (Method method : allMethods(pluginClass)) {
            if (Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            String name = method.getName();
            if ("loadPluginReal".equals(name)
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == String.class) {
                if (hookAfterOnce(method, param -> {
                    if (isSuccessfulLoad(param.getResult()) && isBigClockLoad(param.args)) {
                        scheduleAttachAndSync(param.thisObject,
                                "ClockPlugin#loadPluginReal", false);
                    }
                })) {
                    hooks++;
                }
            } else if ("render".equals(name)) {
                if (hookAfterOnce(method,
                        param -> scheduleAttachAndSync(param.thisObject,
                                "ClockPlugin#render", true))) {
                    hooks++;
                }
            } else if ("unloadPluginReal".equals(name)) {
                if (hookBeforeOnce(method,
                        param -> scheduleDetach(param.thisObject, "ClockPlugin#unloadPluginReal"))) {
                    hooks++;
                }
            }
        }
        return hooks;
    }

    private static boolean hookAfterOnce(Method method, ModernHookBridge.HookCallback callback) {
        synchronized (HOOKED_METHODS) {
            if (!HOOKED_METHODS.add(method)) {
                return false;
            }
        }
        try {
            ModernHookBridge.hookAfter(method, callback);
            return true;
        } catch (Throwable t) {
            synchronized (HOOKED_METHODS) {
                HOOKED_METHODS.remove(method);
            }
            PixelAodLog.log("failed to hook COUI ClockPlugin method " + method, t);
            return false;
        }
    }

    private static boolean hookBeforeOnce(Method method, ModernHookBridge.HookCallback callback) {
        synchronized (HOOKED_METHODS) {
            if (!HOOKED_METHODS.add(method)) {
                return false;
            }
        }
        try {
            ModernHookBridge.hookBefore(method, callback);
            return true;
        } catch (Throwable t) {
            synchronized (HOOKED_METHODS) {
                HOOKED_METHODS.remove(method);
            }
            PixelAodLog.log("failed to hook COUI ClockPlugin method " + method, t);
            return false;
        }
    }

    private static List<Method> allMethods(Class<?> clazz) {
        List<Method> methods = new ArrayList<>();
        for (Class<?> current = clazz;
                current != null && current != Object.class;
                current = current.getSuperclass()) {
            try {
                Collections.addAll(methods, current.getDeclaredMethods());
            } catch (Throwable t) {
                PixelAodLog.log("failed to inspect COUI ClockPlugin class="
                        + current.getName(), t);
            }
        }
        return methods;
    }

    private static void scheduleAttachAndSync(Object plugin, String source,
            boolean renderDriven) {
        if (plugin != null) {
            runOnMain(() -> attachAndSync(plugin, source, renderDriven));
        }
    }

    /** Attach, bind native suppression, and synchronize on the same UI-thread turn. */
    private static void attachAndSync(Object plugin, String source, boolean renderDriven) {
        try {
            ViewGroup root = clockPluginRoot(plugin);
            if (root == null) {
                logRootUnavailableOnce(plugin, source);
                return;
            }
            synchronized (ROOT_UNAVAILABLE_LOGGED) {
                ROOT_UNAVAILABLE_LOGGED.remove(plugin);
            }
            int displayId = PrimaryDisplayPolicy.displayId(root);
            if (!PrimaryDisplayPolicy.isPrimaryDisplayId(displayId)) {
                PixelAodLog.log("skipped COUI ClockPlugin attach reason=non-primary-display"
                        + " displayId=" + displayId
                        + " rootId=" + identity(root)
                        + " source=" + source);
                return;
            }
            HostRecord existing;
            synchronized (HOSTS) {
                existing = HOSTS.get(root);
            }
            if (PixelAodRuntimeState.nativeKeyguardSceneUsesCouiHostVisibility()) {
                PixelAodLog.log("skipped COUI ClockPlugin attach/sync reason=native-bouncer-host-visibility"
                        + " rootId=" + identity(root)
                        + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                        + " source=" + source);
                return;
            }
            if (!nativeSceneAllowsRecord(existing)) {
                if (existing != null && existing.host.getParent() == root) {
                    suppressRecordForNativeScene(existing, source + "#attach-block");
                }
                PixelAodLog.log("skipped COUI ClockPlugin attach/sync reason=native-scene-ineligible"
                        + " rootId=" + identity(root)
                        + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                        + " source=" + source);
                return;
            }
            if (!PixelAodRuntimeState.nativeKeyguardSceneAllowsPresentation()
                    && existing != null && existing.nonLockscreenAodSceneGateBypass) {
                PixelAodLog.log("COUI non-lockscreen AOD bypassed native-scene attach gate"
                        + " rootId=" + identity(root)
                        + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                        + " source=" + source);
            }
            HostRecord record = ensureHost(root, plugin, source);
            if (record == null) {
                return;
            }
            restoreHostAncestors(record, source + "#visible");
            record.suppressNativeDraw = true;
            rememberNativeVisuals(plugin, record);
            suppressNativeVisuals(record);
            syncHost(record, plugin, source, renderDriven);
        } catch (Throwable t) {
            PixelAodLog.log("failed to attach/sync COUI ClockPlugin host source=" + source, t);
        }
    }

    private static HostRecord ensureHost(ViewGroup root, Object plugin, String source) {
        HostRecord existing;
        synchronized (HOSTS) {
            existing = HOSTS.get(root);
        }
        if (existing != null && existing.host.getParent() == root) {
            existing.plugin = new WeakReference<>(plugin);
            existing.host.setVisibility(View.VISIBLE);
            existing.host.bringToFront();
            return existing;
        }

        View tagged = root.findViewWithTag(HOST_TAG);
        CouiClockHostView host = tagged instanceof CouiClockHostView
                ? (CouiClockHostView) tagged : null;
        if (host == null) {
            Context context = root.getContext();
            if (context == null) {
                PixelAodLog.log("skipped COUI ClockPlugin attach reason=no-context source="
                        + source);
                return null;
            }
            host = new CouiClockHostView(context, hookClassLoader);
            host.setTag(HOST_TAG);
            root.setClipChildren(false);
            root.setClipToPadding(false);
            root.addView(host, root.getChildCount(), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        host.setVisibility(View.VISIBLE);
        host.bringToFront();
        HostRecord record = new HostRecord(root, host, plugin, nextGeneration());
        synchronized (HOSTS) {
            HOSTS.put(root, record);
        }
        PixelAodLog.log("attached COUI clock host rendererMode=COUI_PORT"
                + " root=" + root.getClass().getName()
                + " rootId=" + identity(root)
                + " hostId=" + identity(host)
                + " hostGeneration=" + record.generation
                + " source=" + source);
        return record;
    }

    /**
     * Mirrors COUI Expressive 2.5 PixelLockscreenClockHook.syncHost: only ClockPlugin load/render
     * drive presentation, every non-zero state with a known scene keeps the same replacement host,
     * and animation is exactly renderDriven && uiState.isAnim().
     */
    private static void syncHost(HostRecord record, Object plugin, String source,
            boolean renderDriven) {
        RenderState renderState = readRenderState(plugin, renderDriven);
        if (renderState == null) {
            return;
        }
        Context context = record.root.getContext();
        if (context == null) {
            return;
        }
        if (PixelAodRuntimeState.isDirectGoneHandoffActive()) {
            record.nonLockscreenAodPrearmed = false;
            record.nonLockscreenAodSceneGateBypass = false;
            record.nonLockscreenAodTransitionMode = NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
            record.prearmedAodScene = null;
            record.aodExitHandoffPending = false;
            record.suppressNativeDraw = true;
            suppressNativeVisuals(record);
            record.host.setPrimaryVisible(false, source + "#direct-gone-render-block");
            PixelAodLog.log("blocked stale ClockPlugin render during native direct-to-Gone"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " source=" + source);
            return;
        }

        if (PixelAodRuntimeState.nativeKeyguardSceneUsesCouiHostVisibility()) {
            PixelAodLog.log("skipped ClockPlugin sync reason=native-bouncer-host-visibility"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                    + " source=" + source);
            return;
        }
        if (!nativeSceneAllowsRecord(record)) {
            suppressRecordForNativeScene(record, source + "#render-block");
            PixelAodLog.log("blocked stale ClockPlugin render reason=native-scene-ineligible"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                    + " source=" + source);
            return;
        }
        if (!PixelAodRuntimeState.nativeKeyguardSceneAllowsPresentation()
                && record.nonLockscreenAodSceneGateBypass) {
            PixelAodLog.log("COUI non-lockscreen AOD bypassed native-scene render gate"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " scene={" + PixelAodRuntimeState.describeNativeKeyguardSceneEligibility() + "}"
                    + " source=" + source);
        }

        CouiClockSemanticAdapter.Snapshot semantic = CouiClockSemanticAdapter.snapshot(context);
        applySemanticData(record, semantic, source + "#semantic");
        CouiClockPresentationModel.AodContent content = semantic != null
                ? semantic.content : DEFAULT_AOD_CONTENT;

        CouiClockNonLockscreenAodPrearmPolicy.Decision prearmDecision =
                CouiClockNonLockscreenAodPrearmPolicy.route(
                        record.nonLockscreenAodPrearmed, renderState.uiState);
        if (prearmDecision
                == CouiClockNonLockscreenAodPrearmPolicy.Decision.HOLD_INTERMEDIATE) {
            record.suppressNativeDraw = true;
            record.aodExitHandoffPending = false;
            suppressNativeVisuals(record);
            record.host.setPrimaryVisible(true, source + "#prearm-hold");
            PixelAodLog.log("COUI held stale pre-AOD ClockPlugin presentation"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " clockSizeState=" + renderState.clockSizeState
                    + " parkedScene=" + record.prearmedAodScene
                    + " source=" + source);
            return;
        }
        boolean consumedNonLockscreenPrearm = prearmDecision
                == CouiClockNonLockscreenAodPrearmPolicy.Decision.CONSUME_AOD;
        boolean directFinalNonLockscreenEntry = consumedNonLockscreenPrearm
                && NonLockscreenAodTransitionPolicy.isDirectFinal(
                        record.nonLockscreenAodTransitionMode);
        if (consumedNonLockscreenPrearm) {
            PixelAodLog.log("COUI consumed non-lockscreen AOD first-frame pre-arm"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " uiState=" + renderState.uiState
                    + " parkedScene=" + record.prearmedAodScene
                    + " transitionMode=" + record.nonLockscreenAodTransitionMode
                    + " source=" + source);
            record.nonLockscreenAodPrearmed = false;
            record.nonLockscreenAodTransitionMode = NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
            record.prearmedAodScene = null;
        }

        CouiClockPresentationModel.Scene rawScene =
                CouiClockPluginPresentationMapper.sceneForClockSize(renderState.clockSizeState);
        boolean rawAod = renderState.uiState != null
                && (renderState.uiState == CouiClockPluginPresentationMapper.UI_STATE_AOD
                || renderState.uiState
                == CouiClockPluginPresentationMapper.UI_STATE_PANORAMIC_AOD);
        if (!rawAod && rawScene != null) {
            record.lastLockscreenScene = rawScene;
        }

        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.mapReference(
                        renderState.uiState,
                        renderState.clockSizeState,
                        record.lastLockscreenScene,
                        renderState.animate,
                        content);
        if (mapping.action() != CouiClockPluginPresentationMapper.Action.PRESENT) {
            // Reference syncHost simply performs no presentation update for state 0/missing scene.
            // It never hides the replacement from this branch.
            logMapping(record, source, renderState, mapping, null);
            return;
        }

        CouiClockPresentationModel previous = record.host.presentation();
        CouiClockPresentationModel next = mapping.presentation();
        boolean exitingAod = previous != null && previous.dozing() && !next.dozing();
        boolean enteringAod = previous != null && !previous.dozing() && next.dozing();
        boolean presentationAnimate = mapping.animate() && !directFinalNonLockscreenEntry;
        if (directFinalNonLockscreenEntry) {
            PixelAodLog.i("COUI non-lockscreen AOD direct-final render kept animation disabled"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " source=" + source);
        }
        if (enteringAod && presentationAnimate
                && !PixelAodRuntimeState.shouldAnimateScreenOffPresentation()) {
            presentationAnimate = false;
            PixelAodLog.i("COUI screen-off presentation snapped to animation-policy endpoint"
                    + " rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " eligibility={"
                    + PixelAodRuntimeState.describeScreenOffAnimationEligibility() + "}"
                    + " systemAnimation={"
                    + PixelAodRuntimeState.describeSystemAnimationScale() + "}"
                    + " source=" + source);
        }
        boolean normalizeUnlockedAodEntry = enteringAod
                && !PixelAodRuntimeState.wasScreenOffFromInteractiveLockscreen();

        record.suppressNativeDraw = true;
        record.aodExitHandoffPending = false;
        restoreHostAncestors(record, source + "#visible");
        suppressNativeVisuals(record);
        record.host.setPrimaryVisible(true, source + "#visible");

        // COUI 2.5 reserves beginAodEntry for the screen-off-from-unlocked normalization path;
        // a real lockscreen -> AOD transition goes straight through setPresentation/present so
        // the lockscreen geometry can morph continuously into AOD.
        if (normalizeUnlockedAodEntry && !record.host.isTransitionActive()) {
            CouiClockPresentationModel normalizedEntry =
                    CouiClockAodEntryNormalizationPolicy.normalizeUnlockedEntry(next);
            PixelAodLog.log("COUI unlocked AOD entry normalized rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " rawScene=" + next.requestedScene()
                    + " entryScene=" + normalizedEntry.requestedScene()
                    + " contentKind=" + normalizedEntry.content().kind()
                    + " source=" + source);
            record.host.beginAodEntry(normalizedEntry, presentationAnimate, source + "#begin-aod");
        } else {
            // This is the important COUI exit contract: AOD_SMALL -> LS_SMALL is one present()
            // call, so X/Y/burn-in removal and variable-font weight morph start on the same frame.
            record.host.present(next, presentationAnimate, source);
        }
        record.generation = nextGeneration();

        if (!next.dozing() && PixelAodRuntimeState.isDeviceInteractive(context)) {
            PixelLockscreenClockView.markInteractiveLockscreenSurfaceFromClockPlugin(
                    context, source + "#COUI-lockscreen-state");
        }
        if (exitingAod) {
            PixelAodLog.log("COUI AOD exit direct presentation rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " hostGeneration=" + record.generation
                    + " fromScene=" + previous.visualScene()
                    + " toScene=" + next.visualScene()
                    + " animate=" + mapping.animate()
                    + " durationMs=" + (mapping.animate()
                    ? CouiClockPresentationModel.TARGET_TRANSITION_MS : 0L)
                    + " source=" + source);
        }
        logMapping(record, source, renderState, mapping, next);
    }

    private static void applySemanticData(HostRecord record,
            CouiClockSemanticAdapter.Snapshot semantic, String source) {
        if (record == null || record.host == null || semantic == null) {
            return;
        }
        record.host.refreshInformationFromExistingAdapters(source);
        record.host.setNotificationIcons(semantic.notificationIcons);
        record.host.setMediaData(semantic.media.title, semantic.media.artist,
                semantic.media.appIcon);
        PixelAodLog.log("COUI semantic snapshot rendererMode=COUI_PORT"
                + " rootId=" + identity(record.root)
                + " hostGeneration=" + record.generation
                + " contentKind=" + semantic.content.kind()
                + " iconCount=" + semantic.notificationIcons.size()
                + " mediaPresent=" + semantic.media.present
                + " source=" + source);
    }

    private static void restoreHostAncestors(HostRecord record, String source) {
        if (record == null || record.host == null) {
            return;
        }
        int restored = StockAodVisibilityController.restoreHiddenAncestorChain(record.host, source);
        if (restored > 0) {
            PixelAodLog.log("COUI host ancestor visibility restored rendererMode=COUI_PORT"
                    + " rootId=" + identity(record.root)
                    + " hostGeneration=" + record.generation
                    + " restored=" + restored + " source=" + source);
        }
    }

    private static void rememberNativeVisuals(Object plugin, HostRecord record) {
        installNativeDrawSuppression(record.root.getClass().getClassLoader());
        for (int viewId : new int[]{VIEW_CLOCK_TIME, VIEW_DATE_MESSAGE}) {
            Object candidate = callOptional(plugin, "getView", viewId);
            if (candidate instanceof View) {
                View view = (View) candidate;
                if (touchesHost(record, view)) {
                    PixelAodLog.log("skipped COUI native suppression viewId=" + viewId
                            + " reason=host-relative-view");
                    continue;
                }
                bindNativeDrawContainer(view, viewId, record);
                if (!record.nativeVisualAlphas.containsKey(view)) {
                    record.nativeVisualAlphas.put(view, view.getAlpha());
                }
                if (!record.nativeAccessibilityImportance.containsKey(view)) {
                    record.nativeAccessibilityImportance.put(
                            view, view.getImportantForAccessibility());
                }
            }
        }
    }

    private static void installNativeDrawSuppression(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        try {
            Class<?> containerClass = ModernHookBridge.findClass(
                    NATIVE_VISUAL_CONTAINER_CLASS, classLoader);
            synchronized (NATIVE_DRAW_HOOKED_CLASSES) {
                if (!NATIVE_DRAW_HOOKED_CLASSES.add(containerClass)) {
                    return;
                }
            }
            Method dispatchDraw = ModernHookBridge.findMethod(containerClass,
                    "dispatchDraw", Canvas.class);
            ModernHookBridge.hookBefore(dispatchDraw, param -> {
                if (!(param.thisObject instanceof View)) {
                    return;
                }
                View container = (View) param.thisObject;
                WeakReference<HostRecord> reference;
                synchronized (NATIVE_DRAW_BINDINGS) {
                    reference = NATIVE_DRAW_BINDINGS.get(container);
                }
                HostRecord record = reference != null ? reference.get() : null;
                if (record != null && record.suppressNativeDraw
                        && record.host.getParent() == record.root) {
                    param.setResult(null);
                }
            });
            PixelAodLog.log("COUI native ClockPlugin draw suppression hook installed class="
                    + containerClass.getName());
        } catch (Throwable t) {
            PixelAodLog.log("COUI native ClockPlugin draw suppression unavailable", t);
        }
    }

    private static void bindNativeDrawContainer(View nativeView, int viewId, HostRecord record) {
        ViewParent parent = nativeView != null ? nativeView.getParent() : null;
        if (!(parent instanceof View) || record == null) {
            return;
        }
        View container = (View) parent;
        if (!CouiClockNativeDrawBindingPolicy.mayBind(touchesHost(record, container))) {
            PixelAodLog.log("skipped COUI native suppression viewId=" + viewId
                    + " reason=host-relative-container");
            return;
        }
        synchronized (NATIVE_DRAW_BINDINGS) {
            NATIVE_DRAW_BINDINGS.put(container, new WeakReference<>(record));
        }
        record.nativeDrawContainers.add(container);
        PixelAodLog.log("bound COUI native suppression viewId=" + viewId
                + " container=" + container.getClass().getName()
                + " rootId=" + identity(record.root)
                + " hostGeneration=" + record.generation);
    }

    private static void suppressNativeVisuals(HostRecord record) {
        for (Map.Entry<View, Float> entry : record.nativeVisualAlphas.entrySet()) {
            View view = entry.getKey();
            if (view != null && !touchesHost(record, view)) {
                view.setAlpha(0f);
                view.setImportantForAccessibility(
                        View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                view.invalidate();
            }
        }
    }

    private static void restoreNativeVisuals(HostRecord record) {
        for (Map.Entry<View, Float> entry : record.nativeVisualAlphas.entrySet()) {
            View view = entry.getKey();
            Float alpha = entry.getValue();
            if (view != null && alpha != null) {
                view.setAlpha(alpha);
                Integer importance = record.nativeAccessibilityImportance.get(view);
                if (importance != null) {
                    view.setImportantForAccessibility(importance);
                }
                view.invalidate();
            }
        }
    }

    private static boolean touchesHost(HostRecord record, View candidate) {
        return isSameOrAncestor(candidate, record.host) || isSameOrAncestor(record.host, candidate);
    }

    private static boolean isSameOrAncestor(View ancestor, View view) {
        for (View current = view; current != null; ) {
            if (current == ancestor) {
                return true;
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return false;
    }

    private static void scheduleDetach(Object plugin, String source) {
        if (plugin == null) {
            return;
        }
        runOnMain(() -> {
            synchronized (HOSTS) {
                Iterator<Map.Entry<ViewGroup, HostRecord>> iterator = HOSTS.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<ViewGroup, HostRecord> entry = iterator.next();
                    HostRecord record = entry.getValue();
                    if (record == null || record.plugin == null || record.plugin.get() != plugin) {
                        continue;
                    }
                    record.suppressNativeDraw = false;
                    record.aodExitHandoffPending = false;
                    restoreNativeVisuals(record);
                    synchronized (NATIVE_DRAW_BINDINGS) {
                        for (View container : record.nativeDrawContainers) {
                            WeakReference<HostRecord> reference = NATIVE_DRAW_BINDINGS.get(container);
                            if (reference != null && reference.get() == record) {
                                NATIVE_DRAW_BINDINGS.remove(container);
                            }
                        }
                    }
                    record.host.detachLifecycle();
                    if (record.host.getParent() == entry.getKey()) {
                        entry.getKey().removeView(record.host);
                    }
                    iterator.remove();
                    PixelAodLog.log("detached COUI clock host rendererMode=COUI_PORT"
                            + " rootId=" + identity(entry.getKey())
                            + " hostGeneration=" + record.generation
                            + " source=" + source);
                }
            }
        });
    }

    private static ViewGroup clockPluginRoot(Object plugin) {
        Object view = callOptional(plugin, "getView", 0);
        return view instanceof ViewGroup ? (ViewGroup) view : null;
    }

    private static RenderState readRenderState(Object plugin, boolean renderDriven) {
        try {
            Object renderedParams = ModernHookBridge.callMethod(plugin, "getRenderedParams");
            Object uiTracker = callOptional(renderedParams, "getUiState");
            Object uiValue = trackerValue(uiTracker);
            Integer uiState = integerValue(callOptional(uiValue, "getUiState"));
            if (uiState == null) {
                uiState = integerValue(uiValue);
            }
            Boolean uiAnimating = booleanValue(callOptional(uiValue, "isAnim"));
            if (uiAnimating == null) {
                uiAnimating = booleanValue(callOptional(uiValue, "getIsAnim"));
            }
            Integer clockSize = integerValue(trackerValue(
                    callOptional(renderedParams, "getClockSizeState")));
            boolean animate = renderDriven && (uiAnimating == null || uiAnimating);
            return new RenderState(uiState, uiAnimating, clockSize, animate, renderDriven);
        } catch (Throwable t) {
            PixelAodLog.log("failed to read COUI ClockPlugin rendered params", t);
            return null;
        }
    }

    private static Object trackerValue(Object tracker) {
        Object value = callOptional(tracker, "getValue");
        return value != null ? value : tracker;
    }

    private static Object callOptional(Object receiver, String method, Object... args) {
        if (receiver == null) {
            return null;
        }
        try {
            return ModernHookBridge.callMethod(receiver, method, args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Integer integerValue(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private static Boolean booleanValue(Object value) {
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private static boolean isSuccessfulLoad(Object result) {
        return !(result instanceof Boolean) || (Boolean) result;
    }

    private static boolean isBigClockLoad(Object[] args) {
        return args != null && args.length == 1 && BIG_CLOCK_LOGICAL_PACKAGE.equals(args[0]);
    }

    private static boolean isKeyguardShowing(Context context) {
        try {
            android.app.KeyguardManager manager = (android.app.KeyguardManager) context
                    .getSystemService(Context.KEYGUARD_SERVICE);
            return manager != null && manager.isKeyguardLocked();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void logRootUnavailableOnce(Object plugin, String source) {
        synchronized (ROOT_UNAVAILABLE_LOGGED) {
            if (!ROOT_UNAVAILABLE_LOGGED.add(plugin)) {
                return;
            }
        }
        PixelAodLog.log("skipped COUI ClockPlugin attach reason=root-unavailable"
                + " rendererMode=COUI_PORT source=" + source
                + " pluginId=" + identity(plugin));
    }

    private static void logMapping(HostRecord record, String source, RenderState state,
            CouiClockPluginPresentationMapper.Mapping mapping,
            CouiClockPresentationModel presentation) {
        String requestedScene = presentation != null
                ? String.valueOf(presentation.requestedScene()) : "none";
        String visualScene = presentation != null
                ? String.valueOf(presentation.visualScene()) : "none";
        String dozing = presentation != null ? String.valueOf(presentation.dozing()) : "none";
        String partial = presentation != null ? String.valueOf(presentation.partialAod()) : "none";
        String kind = presentation != null
                ? String.valueOf(presentation.content().kind()) : "none";
        int iconCount = presentation != null
                ? presentation.content().notificationIconCount() : 0;
        PixelAodLog.log("COUI ClockPlugin mapping rendererMode=COUI_PORT"
                + " pluginRootId=" + identity(record.root)
                + " hostId=" + identity(record.host)
                + " uiState=" + (state.uiState == null ? "null" : state.uiState)
                + " clockSizeState=" + (state.clockSizeState == null ? "null" : state.clockSizeState)
                + " requestedScene=" + requestedScene
                + " visualScene=" + visualScene
                + " dozing=" + dozing
                + " partialAod=" + partial
                + " contentKind=" + kind
                + " iconCount=" + iconCount
                + " animate=" + (mapping != null && mapping.animate())
                + " action=" + (mapping != null ? mapping.action() : "none")
                + " renderDriven=" + state.renderDriven
                + " hostGeneration=" + record.generation
                + " source=" + source);
    }

    private static void logStateDerivedAodExit(HostRecord record, String source,
            RenderState state, CouiClockPluginPresentationMapper.Mapping originalMapping,
            CouiClockPresentationModel next) {
        CouiClockPresentationModel previous = record.host.presentation();
        CouiClockGeometryPolicy.Surface fromSurface = surfaceFor(previous);
        CouiClockGeometryPolicy.Surface toSurface = surfaceFor(next);
        CouiClockGeometryPolicy.SurfaceTarget fromTarget =
                CouiClockGeometryPolicy.target(fromSurface);
        CouiClockGeometryPolicy.SurfaceTarget toTarget =
                CouiClockGeometryPolicy.target(toSurface);
        float density = record.host.getResources().getDisplayMetrics().density;
        float fromX = record.host.getWidth() * fromTarget.centerRatio
                + fromTarget.centerDp * density;
        float fromY = record.host.getHeight() * fromTarget.topRatio
                + fromTarget.topDp * density;
        float toX = record.host.getWidth() * toTarget.centerRatio
                + toTarget.centerDp * density;
        float toY = record.host.getHeight() * toTarget.topRatio
                + toTarget.topDp * density;
        PixelAodLog.log("COUI AOD exit state-derived PRESENT rendererMode=COUI_PORT"
                + " rootId=" + identity(record.root)
                + " hostGeneration=" + record.generation
                + " previousSurface=" + fromSurface
                + " nextSurface=" + toSurface
                + " rawUiState=" + state.uiState
                + " rawUiStateAnimating=" + state.uiStateAnimating
                + " renderDriven=" + state.renderDriven
                + " mappedAction=" + originalMapping.action()
                + " actionOverride=PRESENT"
                + " animate=" + state.animate
                + " durationMs=" + (state.animate
                ? CouiClockPresentationModel.TARGET_TRANSITION_MS : 0L)
                + " representativeFromX=" + fromX
                + " representativeFromY=" + fromY
                + " representativeToX=" + toX
                + " representativeToY=" + toY
                + " contentKind=" + next.content().kind()
                + " iconCount=" + next.content().notificationIconCount()
                + " source=" + source);
    }

    private static CouiClockGeometryPolicy.Surface surfaceFor(
            CouiClockPresentationModel presentation) {
        if (presentation.visualScene() == CouiClockPresentationModel.Scene.IMMERSED) {
            return CouiClockGeometryPolicy.Surface.LS_IMMERSED;
        }
        if (presentation.visualScene() == CouiClockPresentationModel.Scene.SMALL) {
            return presentation.dozing()
                    ? CouiClockGeometryPolicy.Surface.AOD_SMALL
                    : CouiClockGeometryPolicy.Surface.LS_SMALL;
        }
        return presentation.dozing()
                ? CouiClockGeometryPolicy.Surface.AOD_LARGE
                : CouiClockGeometryPolicy.Surface.LS_LARGE;
    }

    private static void logHostState(HostRecord record, String source, boolean animate,
            CouiClockPresentationModel state) {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.Mapping.present(state, animate);
        logMapping(record, source,
                new RenderState(null, null, null, animate), mapping, state);
    }

    private static List<HostRecord> snapshotRecords() {
        synchronized (HOSTS) {
            return new ArrayList<>(HOSTS.values());
        }
    }

    private static long nextGeneration() {
        synchronized (CouiClockPluginHostController.class) {
            return ++nextHostGeneration;
        }
    }

    private static String identity(Object object) {
        return object == null ? "null" : object.getClass().getName()
                + "@" + Integer.toHexString(System.identityHashCode(object));
    }

    private static void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new android.os.Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    private static void runOnMainFront(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new android.os.Handler(Looper.getMainLooper()).postAtFrontOfQueue(runnable);
        }
    }

    private static final class HostRecord {
        final ViewGroup root;
        final CouiClockHostView host;
        final Map<View, Float> nativeVisualAlphas =
                Collections.synchronizedMap(new WeakHashMap<View, Float>());
        final Map<View, Integer> nativeAccessibilityImportance =
                Collections.synchronizedMap(new WeakHashMap<View, Integer>());
        final Set<View> nativeDrawContainers =
                Collections.newSetFromMap(new WeakHashMap<View, Boolean>());
        WeakReference<Object> plugin;
        long generation;
        boolean suppressNativeDraw;
        boolean aodExitHandoffPending;
        boolean nonLockscreenAodPrearmed;
        boolean nonLockscreenAodSceneGateBypass;
        NonLockscreenAodTransitionPolicy.Mode nonLockscreenAodTransitionMode =
                NonLockscreenAodTransitionPolicy.Mode.ANIMATED;
        CouiClockPresentationModel.Scene prearmedAodScene;
        CouiClockPresentationModel.Scene lastLockscreenScene;

        HostRecord(ViewGroup root, CouiClockHostView host, Object plugin, long generation) {
            this.root = root;
            this.host = host;
            this.plugin = new WeakReference<>(plugin);
            this.generation = generation;
        }
    }

    private static final class RenderState {
        final Integer uiState;
        final Boolean uiStateAnimating;
        final Integer clockSizeState;
        final boolean animate;
        final boolean renderDriven;

        RenderState(Integer uiState, Boolean uiStateAnimating, Integer clockSizeState,
                boolean animate) {
            this(uiState, uiStateAnimating, clockSizeState, animate, false);
        }

        RenderState(Integer uiState, Boolean uiStateAnimating, Integer clockSizeState,
                boolean animate, boolean renderDriven) {
            this.uiState = uiState;
            this.uiStateAnimating = uiStateAnimating;
            this.clockSizeState = clockSizeState;
            this.animate = animate;
            this.renderDriven = renderDriven;
        }
    }
}
