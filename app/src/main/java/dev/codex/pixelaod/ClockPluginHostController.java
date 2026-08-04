package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mounts one persistent replacement host in OPlus ClockPlugin#getView(0).
 *
 * <p>Legacy NotificationShadeWindowView overlays remain active until this controller has a real
 * attached, measured, and drawn host. That ordering is the recovery boundary missing from the
 * earlier experimental implementation.</p>
 */
final class ClockPluginHostController {
    private static final String CLOCK_PLUGIN_CLASS = "com.oplus.keyguard.plugin.ClockPlugin";
    private static final String BIG_CLOCK_LOGICAL_PACKAGE = "com.oplus.keyguard.clock.big";
    private static final String NATIVE_VISUAL_CONTAINER_CLASS =
            "com.oplus.keyguard.clock.big.widget.MyCustomizedFrameLayout";
    private static final String HOST_TAG = "dev.codex.pixelaod.CLOCK_PLUGIN_HOST";
    private static final int VIEW_CLOCK_TIME = 1;
    private static final int VIEW_DATE_MESSAGE = 11;
    private static final long PENDING_LOCKSCREEN_HANDOFF_WINDOW_MS = 2_500L;

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);
    private static final Set<Method> HOOKED_METHODS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Class<?>> NATIVE_DRAW_HOOKED_CLASSES =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<ViewGroup, HostRecord> HOSTS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, WeakReference<HostRecord>> NATIVE_DRAW_BINDINGS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<Object> ROOT_UNAVAILABLE_LOGGED =
            Collections.newSetFromMap(new WeakHashMap<>());

    private static volatile boolean hooksInstalled;
    private static volatile boolean validatedHostAvailable;
    private static volatile String pendingLockscreenHandoffTrace = "";
    private static volatile long pendingLockscreenHandoffAt;

    private ClockPluginHostController() {
    }

    static void install(Context context, ClassLoader classLoader) {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        try {
            Class<?> pluginClass = ModernHookBridge.findClass(CLOCK_PLUGIN_CLASS, classLoader);
            int hooks = hookPluginClass(pluginClass);
            hooksInstalled = hooks > 0;
            PixelAodLog.log("ClockPlugin persistent-host hooks installed=" + hooksInstalled
                    + " methods=" + hooks + " class=" + pluginClass.getName());
        } catch (Throwable t) {
            hooksInstalled = false;
            PixelAodLog.log("ClockPlugin persistent-host path unavailable; using legacy hosts", t);
        }
    }

    static boolean isHookInstalled() {
        return hooksInstalled;
    }

    static boolean hasValidatedHost() {
        return validatedHostAvailable;
    }

    /** Records that the next real AOD scene originated from an interactive lockscreen. */
    static void noteLockscreenToAodHandoff(String source) {
        String trace = PixelAodClockView.currentAodTraceId();
        pendingLockscreenHandoffTrace = trace;
        pendingLockscreenHandoffAt = SystemClock.uptimeMillis();
        PixelAodLog.log("queued persistent ClockPlugin lockscreen handoff source=" + source
                + " trace=" + trace);
    }

    static void refreshAll(String source) {
        runOnMain(() -> {
            List<HostRecord> records;
            synchronized (HOSTS) {
                records = new ArrayList<>(HOSTS.values());
            }
            for (HostRecord record : records) {
                Object plugin = record != null ? record.plugin.get() : null;
                if (plugin != null) {
                    attachAndSync(plugin, source + "#ClockPlugin");
                }
            }
        });
    }

    /**
     * Presents the final AOD scene at the screen-off boundary instead of waiting roughly 600 ms
     * for OPlus ClockPlugin#render to publish uiState=AOD.
     */
    static void prepareNonLockscreenAodEntry(String source) {
        runOnMain(() -> {
            List<HostRecord> records;
            synchronized (HOSTS) {
                records = new ArrayList<>(HOSTS.values());
            }
            int prepared = 0;
            boolean compactAod = PixelAodClockView.hasCompactClockNotificationContent();
            for (HostRecord record : records) {
                if (record == null || record.host.getParent() != record.root) {
                    continue;
                }
                Context context = record.root.getContext();
                if (context == null || !PixelAodClockView.isContinuousAodPolicyAllowingDisplay(
                        context, source + "#ClockPlugin-pre-present")) {
                    continue;
                }
                ClockPluginSceneMachine.Decision decision =
                        record.machine.prepareAod(compactAod);
                record.suppressNativeDraw = true;
                StockAodVisibilityController.restoreHiddenAncestorChain(
                        record.host, source + "#ClockPlugin-pre-present");
                record.host.present(decision, source + "#ClockPlugin-pre-present");
                if (record.validated) {
                    suppressNativeVisuals(record);
                }
                prepared++;
            }
            PixelAodLog.log("prepared persistent ClockPlugin AOD before vendor uiState source="
                    + source
                    + " compact=" + compactAod
                    + " prepared=" + prepared
                    + " trace=" + PixelAodClockView.currentAodTraceId());
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
                        scheduleAttachAndSync(param.thisObject, "ClockPlugin#loadPluginReal");
                    }
                })) {
                    hooks++;
                }
            } else if ("render".equals(name)) {
                if (hookAfterOnce(method,
                        param -> scheduleAttachAndSync(param.thisObject, "ClockPlugin#render"))) {
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
            PixelAodLog.log("failed to hook ClockPlugin method " + method, t);
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
            PixelAodLog.log("failed to hook ClockPlugin method " + method, t);
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
                PixelAodLog.log("failed to inspect ClockPlugin methods class="
                        + current.getName(), t);
            }
        }
        return methods;
    }

    private static void scheduleAttachAndSync(Object plugin, String source) {
        if (plugin != null) {
            runOnMain(() -> attachAndSync(plugin, source));
        }
    }

    private static void attachAndSync(Object plugin, String source) {
        try {
            ViewGroup root = clockPluginRoot(plugin);
            if (root == null) {
                logRootUnavailableOnce(plugin, source);
                return;
            }
            synchronized (ROOT_UNAVAILABLE_LOGGED) {
                ROOT_UNAVAILABLE_LOGGED.remove(plugin);
            }
            HostRecord record = ensureHost(root, plugin, source);
            if (record == null) {
                return;
            }
            rememberNativeVisuals(plugin, record);
            syncHost(record, plugin, source);
        } catch (Throwable t) {
            PixelAodLog.log("failed to attach/sync persistent ClockPlugin host source=" + source, t);
        }
    }

    private static HostRecord ensureHost(ViewGroup root, Object plugin, String source) {
        HostRecord existing;
        synchronized (HOSTS) {
            existing = HOSTS.get(root);
        }
        if (existing != null && existing.host.getParent() == root) {
            existing.plugin = new WeakReference<>(plugin);
            existing.host.bringToFront();
            return existing;
        }

        View tagged = root.findViewWithTag(HOST_TAG);
        PixelClockPluginHostView host;
        if (tagged instanceof PixelClockPluginHostView) {
            host = (PixelClockPluginHostView) tagged;
        } else {
            Context context = root.getContext();
            if (context == null) {
                PixelAodLog.log("skipped persistent ClockPlugin attach reason=no-context source="
                        + source);
                return null;
            }
            host = new PixelClockPluginHostView(context);
            host.setTag(HOST_TAG);
            root.setClipChildren(false);
            root.setClipToPadding(false);
            root.addView(host, root.getChildCount(), new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        host.bringToFront();
        HostRecord record = new HostRecord(root, host, plugin);
        synchronized (HOSTS) {
            HOSTS.put(root, record);
        }
        PixelAodLog.log("attached persistent ClockPlugin host source=" + source
                + " root=" + root.getClass().getName()
                + " children=" + root.getChildCount());
        return record;
    }

    private static void syncHost(HostRecord record, Object plugin, String source) {
        RenderState renderState = readRenderState(plugin);
        if (renderState == null) {
            logSync(record, source, "rendered-params-unavailable", record.machine.resolve(
                    null, null, false, true, false, false, false, false));
            return;
        }
        Context context = record.root.getContext();
        if (context == null) {
            return;
        }

        boolean moduleAodAllowed = true;
        boolean preserveAodWhileLifecycleSettles = false;
        String policyReason = "not-aod";
        if (renderState.isAod()) {
            OosAodLifecycleAdapter.AodPolicyDecision policy =
                    PixelAodClockView.evaluateAodPolicy(context, source + "#ClockPlugin");
            // Use the same final policy as the legacy AOD overlay.  Checking only the module
            // schedule here kept a persistent host visible after trigger expiry or screen-on.
            moduleAodAllowed = policy.shouldDrawPixelOverlay;
            policyReason = policy.modulePolicyReason + "/" + policy.drawReason;
            preserveAodWhileLifecycleSettles = !moduleAodAllowed
                    && policy.modulePolicyAllowsDisplay
                    && "lifecycle-not-ready".equals(policy.drawReason);
        }
        // Split lockscreen vs AOD size policy (first decision must be correct — no
        // user-visible AOD_SMALL frame that later snaps to LARGE):
        // - Lockscreen: OOS media cards cover large clock → SMALL when media or notifs.
        // - AOD: module media row under large clock → SMALL only for non-media notifs.
        boolean moduleNotifs = PixelAodClockView.hasCompactClockNotificationContent();
        boolean mediaActive = PixelAodClockView.hasActiveDisplayableMedia();
        boolean interactive = PixelAodClockView.isDeviceInteractive(context);
        boolean displayInAodState = PixelAodClockView.isDisplayInAodState(context);
        Integer effectiveClockSize = renderState.clockSizeState;
        boolean compactAod;
        if (renderState.isAod() || (!interactive && displayInAodState)) {
            // Treat non-interactive doze display as AOD for size, even if uiState lags.
            compactAod = moduleNotifs;
            effectiveClockSize = moduleNotifs
                    ? ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                    : ClockPluginSceneMachine.CLOCK_SIZE_LARGE;
            if (renderState.clockSizeState != null
                    && renderState.clockSizeState == ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                    && !moduleNotifs) {
                PixelAodLog.log("overrode ClockPlugin AOD small→large source=" + source
                        + " reason=media-only-or-empty"
                        + " oosClockSize=" + renderState.clockSizeState
                        + " mediaActive=" + mediaActive
                        + " uiState=" + renderState.uiState
                        + " interactive=" + interactive
                        + " displayInAod=" + displayInAodState
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            }
        } else {
            // Lockscreen size: OOS clockSizeState is authoritative for media-card presence.
            // Logs showed after swipe-away: oosClockSize=1 (LARGE) while mediaActive stayed
            // true from a paused MediaSession — forcing SMALL was wrong.
            if (renderState.clockSizeState != null) {
                effectiveClockSize = renderState.clockSizeState;
                if (moduleNotifs
                        && effectiveClockSize != ClockPluginSceneMachine.CLOCK_SIZE_SMALL) {
                    effectiveClockSize = ClockPluginSceneMachine.CLOCK_SIZE_SMALL;
                }
            } else {
                boolean lockscreenSmall = moduleNotifs || mediaActive;
                effectiveClockSize = lockscreenSmall
                        ? ClockPluginSceneMachine.CLOCK_SIZE_SMALL
                        : ClockPluginSceneMachine.CLOCK_SIZE_LARGE;
            }
            compactAod = false;
            PixelAodLog.log("ClockPlugin lockscreen size source=" + source
                    + " oosClockSize=" + renderState.clockSizeState
                    + " effectiveClockSize=" + effectiveClockSize
                    + " moduleNotifs=" + moduleNotifs
                    + " mediaActive=" + mediaActive
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
        // When the panel is already dozing but ClockPlugin still publishes KEYGUARD,
        // promote to AOD uiState so we do not keep painting LOCKSCREEN_SMALL without media.
        Integer effectiveUiState = renderState.uiState;
        if (!interactive && displayInAodState && moduleAodAllowed
                && (effectiveUiState == null
                || effectiveUiState == ClockPluginSceneMachine.UI_STATE_KEYGUARD
                || effectiveUiState == 0)) {
            effectiveUiState = ClockPluginSceneMachine.UI_STATE_AOD;
            PixelAodLog.log("promoted ClockPlugin uiState→AOD source=" + source
                    + " oosUiState=" + renderState.uiState
                    + " interactive=" + interactive
                    + " displayInAod=" + displayInAodState
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
        ClockPluginSceneMachine.Decision decision = record.machine.resolve(
                effectiveUiState,
                effectiveClockSize,
                Boolean.TRUE.equals(renderState.uiStateAnimating),
                moduleAodAllowed,
                preserveAodWhileLifecycleSettles,
                compactAod,
                interactive,
                isKeyguardLockedRaw(context),
                displayInAodState);

        if (decision.staleLockscreenRenderRejected) {
            PixelAodLog.log("rejected stale ClockPlugin lockscreen render source=" + source
                    + " uiState=" + renderState.uiState
                    + " interactive=" + interactive
                    + " displayInAod=" + displayInAodState
                    + " hostScene=" + record.host.scene()
                    + " preservedScene=" + decision.scene
                    + " reason=noninteractive-dozing-aod-scene"
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        } else if (consumePendingLockscreenHandoff(decision)) {
            decision = decision.withEnteringAod(true);
        }

        String lifecycleDetail = " interactive=" + interactive
                + " displayInAod=" + displayInAodState;

        if (decision.scene == ClockPluginSceneMachine.Scene.HIDDEN) {
            record.suppressNativeDraw = false;
            record.host.hide(source + "#hidden");
            restoreNativeVisuals(record);
            logSync(record, source, renderState.describe() + lifecycleDetail
                    + " policy=" + policyReason, decision);
            return;
        }

        int restoredAncestors = StockAodVisibilityController.restoreHiddenAncestorChain(
                record.host, source + "#ClockPlugin-visible");
        if (restoredAncestors > 0) {
            PixelAodLog.log("restored persistent ClockPlugin host chain source=" + source
                    + " count=" + restoredAncestors
                    + " trace=" + PixelAodClockView.currentAodTraceId());
        }
        if (!record.validated) {
            record.host.setFirstPresentationFrameCallback(
                    () -> validateHostAfterFirstFrame(record, plugin, source));
        }
        record.suppressNativeDraw = true;
        boolean vendorRender = "ClockPlugin#render".equals(source);
        boolean forcePresentation = !vendorRender
                || !record.validated
                || decision.changed
                || decision.enteringAod
                || record.host.scene() != decision.scene
                || record.host.getVisibility() != View.VISIBLE;
        if (!record.presentationGate.shouldPresent(decision, forcePresentation)) {
            logSync(record, source, renderState.describe() + lifecycleDetail
                    + " policy=" + policyReason
                    + " presentation=stable-scene-skip", decision);
            return;
        }
        record.host.present(decision, source);
        if (record.validated) {
            suppressNativeVisuals(record);
            PixelAodHook.removeLegacyClockOverlays(source + "#ClockPlugin-validated");
        }
        logSync(record, source, renderState.describe() + lifecycleDetail
                + " policy=" + policyReason
                + " preserveAodWhileLifecycleSettles=" + preserveAodWhileLifecycleSettles,
                decision);
    }

    private static void validateHostAfterFirstFrame(HostRecord record, Object plugin, String source) {
        if (record == null || record.host.getParent() != record.root
                || !record.host.hasUsableBounds()
                || record.host.scene() == ClockPluginSceneMachine.Scene.HIDDEN) {
            return;
        }
        String validationFailure = hostValidationFailure(record.host);
        if (validationFailure != null) {
            if (!validationFailure.equals(record.lastValidationFailure)) {
                record.lastValidationFailure = validationFailure;
                PixelAodLog.log("deferred persistent ClockPlugin host validation source=" + source
                        + " reason=" + validationFailure
                        + " trace=" + PixelAodClockView.currentAodTraceId());
            }
            return;
        }
        record.lastValidationFailure = "";
        rememberNativeVisuals(plugin, record);
        record.validated = true;
        updateValidatedHostAvailability();
        suppressNativeVisuals(record);
        PixelAodHook.removeLegacyClockOverlays(source + "#ClockPlugin-first-frame");
        PixelAodLog.log("validated persistent ClockPlugin host source=" + source
                + " root=" + record.root.getClass().getName()
                + " scene=" + record.host.scene()
                + " width=" + record.host.getWidth()
                + " height=" + record.host.getHeight()
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private static String hostValidationFailure(View host) {
        if (host == null) {
            return "host=null";
        }
        if (host.getWindowVisibility() != View.VISIBLE || host.getWindowToken() == null) {
            return "window visibility=" + host.getWindowVisibility()
                    + " token=" + (host.getWindowToken() != null);
        }
        for (View current = host; current != null; ) {
            if (!ClockPluginHostValidation.isDrawableNode(
                    current.isAttachedToWindow(), current.getVisibility(), current.getAlpha())) {
                return describeValidationNode(current);
            }
            ViewParent parent = current.getParent();
            current = parent instanceof View ? (View) parent : null;
        }
        return null;
    }

    private static String describeValidationNode(View view) {
        ViewParent parent = view != null ? view.getParent() : null;
        return view == null ? "view=null" : view.getClass().getName()
                + "@" + Integer.toHexString(System.identityHashCode(view))
                + " attached=" + view.isAttachedToWindow()
                + " shown=" + view.isShown()
                + " visibility=" + view.getVisibility()
                + " alpha=" + view.getAlpha()
                + " size=" + view.getWidth() + "x" + view.getHeight()
                + " parent=" + (parent != null ? parent.getClass().getName() : "null");
    }

    private static boolean consumePendingLockscreenHandoff(
            ClockPluginSceneMachine.Decision decision) {
        if (decision == null || !decision.scene.isAod()) {
            return false;
        }
        String trace = pendingLockscreenHandoffTrace;
        long recordedAt = pendingLockscreenHandoffAt;
        long now = SystemClock.uptimeMillis();
        if (recordedAt <= 0L || now - recordedAt > PENDING_LOCKSCREEN_HANDOFF_WINDOW_MS
                || !trace.equals(PixelAodClockView.currentAodTraceId())) {
            if (recordedAt > 0L && now - recordedAt > PENDING_LOCKSCREEN_HANDOFF_WINDOW_MS) {
                pendingLockscreenHandoffTrace = "";
                pendingLockscreenHandoffAt = 0L;
            }
            return false;
        }
        pendingLockscreenHandoffTrace = "";
        pendingLockscreenHandoffAt = 0L;
        PixelAodLog.log("consumed persistent ClockPlugin lockscreen handoff trace=" + trace);
        return true;
    }

    private static void rememberNativeVisuals(Object plugin, HostRecord record) {
        installNativeDrawSuppression(record.root.getClass().getClassLoader());
        for (int viewId : new int[]{VIEW_CLOCK_TIME, VIEW_DATE_MESSAGE}) {
            Object candidate = callOptional(plugin, "getView", viewId);
            if (candidate instanceof View) {
                View view = (View) candidate;
                bindNativeDrawContainer(view, viewId, record);
                if (touchesPersistentHost(record, view)) {
                    PixelAodLog.log("skipped unsafe ClockPlugin native alpha candidate id="
                            + viewId + " view=" + describeNativeVisual(view));
                    continue;
                }
                record.rememberNativeVisual(view);
            }
        }
    }

    /**
     * COUI suppresses the OPlus time/date visual container at dispatchDraw(), so a vendor alpha
     * reset cannot expose one stock frame before the replacement host is refreshed.
     */
    private static void installNativeDrawSuppression(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        Class<?> containerClass = null;
        try {
            containerClass = ModernHookBridge.findClass(NATIVE_VISUAL_CONTAINER_CLASS, classLoader);
            synchronized (NATIVE_DRAW_HOOKED_CLASSES) {
                if (!NATIVE_DRAW_HOOKED_CLASSES.add(containerClass)) {
                    return;
                }
            }
            Method dispatchDraw = ModernHookBridge.findMethod(
                    containerClass, "dispatchDraw", Canvas.class);
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
                if (record == null) {
                    if (reference != null) {
                        synchronized (NATIVE_DRAW_BINDINGS) {
                            NATIVE_DRAW_BINDINGS.remove(container);
                        }
                    }
                    return;
                }
                if (record.suppressNativeDraw && record.host.isAttachedToWindow()) {
                    param.setResult(null);
                }
            });
            PixelAodLog.log("ClockPlugin native draw suppression hook installed class="
                    + containerClass.getName());
        } catch (Throwable t) {
            if (containerClass != null) {
                synchronized (NATIVE_DRAW_HOOKED_CLASSES) {
                    NATIVE_DRAW_HOOKED_CLASSES.remove(containerClass);
                }
            }
            PixelAodLog.log("ClockPlugin native draw suppression hook unavailable class="
                    + NATIVE_VISUAL_CONTAINER_CLASS, t);
        }
    }

    private static void bindNativeDrawContainer(View nativeView, int viewId, HostRecord record) {
        ViewParent parent = nativeView != null ? nativeView.getParent() : null;
        if (!(parent instanceof View) || record == null) {
            return;
        }
        View container = (View) parent;
        WeakReference<HostRecord> previous;
        synchronized (NATIVE_DRAW_BINDINGS) {
            previous = NATIVE_DRAW_BINDINGS.put(container, new WeakReference<>(record));
        }
        record.nativeDrawContainers.add(container);
        if (previous == null || previous.get() != record) {
            PixelAodLog.log("bound persistent ClockPlugin native draw suppression viewId="
                    + viewId
                    + " container=" + container.getClass().getName()
                    + " native=" + nativeView.getClass().getName()
                    + " root=" + record.root.getClass().getName());
        }
    }

    private static void suppressNativeVisuals(HostRecord record) {
        if (!record.validated) {
            return;
        }
        for (Map.Entry<View, Float> entry : record.nativeVisualAlphas.entrySet()) {
            View view = entry.getKey();
            if (view != null) {
                if (touchesPersistentHost(record, view)) {
                    Float originalAlpha = entry.getValue();
                    if (originalAlpha != null && view.getAlpha() != originalAlpha) {
                        view.setAlpha(originalAlpha);
                        view.invalidate();
                    }
                    PixelAodLog.log("restored unsafe ClockPlugin native alpha candidate view="
                            + describeNativeVisual(view));
                    continue;
                }
                view.setAlpha(0f);
                view.invalidate();
            }
        }
    }

    /**
     * ClockPlugin#getView(int) is an opaque vendor API.  Some clock packages return their
     * top-level container for a visual slot; alpha-suppressing such a container would also hide
     * this replacement host, even while the host itself reports VISIBLE.
     */
    private static boolean touchesPersistentHost(HostRecord record, View candidate) {
        return record == null || candidate == null
                || isSameOrAncestor(candidate, record.host)
                || isSameOrAncestor(record.host, candidate);
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

    private static String describeNativeVisual(View view) {
        if (view == null) {
            return "null";
        }
        ViewParent parent = view.getParent();
        return view.getClass().getName()
                + " id=" + view.getId()
                + " alpha=" + view.getAlpha()
                + " parent=" + (parent != null ? parent.getClass().getName() : "null");
    }

    private static void restoreNativeVisuals(HostRecord record) {
        for (Map.Entry<View, Float> entry : record.nativeVisualAlphas.entrySet()) {
            View view = entry.getKey();
            Float alpha = entry.getValue();
            if (view != null && alpha != null) {
                view.setAlpha(alpha);
                view.invalidate();
            }
        }
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
                    if (record == null || record.plugin.get() != plugin) {
                        continue;
                    }
                    restoreNativeVisuals(record);
                    record.suppressNativeDraw = false;
                    synchronized (NATIVE_DRAW_BINDINGS) {
                        for (View container : record.nativeDrawContainers) {
                            WeakReference<HostRecord> reference =
                                    NATIVE_DRAW_BINDINGS.get(container);
                            if (reference != null && reference.get() == record) {
                                NATIVE_DRAW_BINDINGS.remove(container);
                            }
                        }
                    }
                    if (record.host.getParent() == entry.getKey()) {
                        entry.getKey().removeView(record.host);
                    }
                    iterator.remove();
                    PixelAodLog.log("detached persistent ClockPlugin host source=" + source);
                }
            }
            updateValidatedHostAvailability();
        });
    }

    private static void updateValidatedHostAvailability() {
        boolean available = false;
        synchronized (HOSTS) {
            for (HostRecord record : HOSTS.values()) {
                if (record != null && record.validated
                        && record.host.getParent() == record.root) {
                    available = true;
                    break;
                }
            }
        }
        validatedHostAvailable = available;
    }

    private static ViewGroup clockPluginRoot(Object plugin) {
        Object view = callOptional(plugin, "getView", 0);
        return view instanceof ViewGroup ? (ViewGroup) view : null;
    }

    private static RenderState readRenderState(Object plugin) {
        try {
            Object renderedParams = ModernHookBridge.callMethod(plugin, "getRenderedParams");
            Object uiTracker = callOptional(renderedParams, "getUiState");
            Object uiValue = trackerValue(uiTracker);
            Integer uiState = integerValue(callOptional(uiValue, "getUiState"));
            if (uiState == null) {
                uiState = integerValue(uiValue);
            }
            Boolean uiStateAnimating = uiStateAnimatingValue(uiValue);
            Integer clockSize = integerValue(trackerValue(
                    callOptional(renderedParams, "getClockSizeState")));
            return new RenderState(uiState, uiStateAnimating, clockSize);
        } catch (Throwable t) {
            PixelAodLog.log("failed to read ClockPlugin rendered params", t);
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

    private static Boolean uiStateAnimatingValue(Object uiState) {
        Boolean fromMethod = booleanValue(callOptional(uiState, "isAnim"));
        if (fromMethod == null) {
            fromMethod = booleanValue(callOptional(uiState, "getIsAnim"));
        }
        if (fromMethod != null || uiState == null) {
            return fromMethod;
        }
        try {
            Field field = uiState.getClass().getField("isAnim");
            return field.getBoolean(uiState);
        } catch (Throwable ignored) {
            return null;
        }
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

    private static boolean isKeyguardLockedRaw(Context context) {
        if (context == null) {
            return false;
        }
        try {
            android.app.KeyguardManager keyguardManager =
                    (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isKeyguardLocked();
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
        PixelAodLog.log("skipped persistent ClockPlugin attach source=" + source
                + " reason=root-unavailable");
    }

    private static void logSync(HostRecord record, String source, String detail,
            ClockPluginSceneMachine.Decision decision) {
        String fingerprint = detail + " scene=" + decision.scene
                + " enteringAod=" + decision.enteringAod
                + " preparingAod=" + decision.preparingAod
                + " staleLockscreenRejected=" + decision.staleLockscreenRenderRejected;
        if (fingerprint.equals(record.lastSyncFingerprint)) {
            return;
        }
        record.lastSyncFingerprint = fingerprint;
        PixelAodLog.log("ClockPlugin host sync source=" + source
                + " " + fingerprint
                + " validated=" + record.validated
                + " root=" + record.root.getClass().getName()
                + " trace=" + PixelAodClockView.currentAodTraceId());
    }

    private static void runOnMain(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN.post(runnable);
        }
    }

    private static final class HostRecord {
        final ViewGroup root;
        final PixelClockPluginHostView host;
        final ClockPluginSceneMachine machine = new ClockPluginSceneMachine();
        final ClockPluginPresentationGate presentationGate = new ClockPluginPresentationGate();
        final Map<View, Float> nativeVisualAlphas = new WeakHashMap<>();
        final Set<View> nativeDrawContainers =
                Collections.newSetFromMap(new WeakHashMap<>());
        WeakReference<Object> plugin;
        boolean validated;
        boolean suppressNativeDraw;
        String lastSyncFingerprint = "";
        String lastValidationFailure = "";

        HostRecord(ViewGroup root, PixelClockPluginHostView host, Object plugin) {
            this.root = root;
            this.host = host;
            this.plugin = new WeakReference<>(plugin);
        }

        void rememberNativeVisual(View view) {
            if (!nativeVisualAlphas.containsKey(view)) {
                nativeVisualAlphas.put(view, view.getAlpha());
            }
        }
    }

    private static final class RenderState {
        final Integer uiState;
        final Boolean uiStateAnimating;
        final Integer clockSizeState;

        RenderState(Integer uiState, Boolean uiStateAnimating, Integer clockSizeState) {
            this.uiState = uiState;
            this.uiStateAnimating = uiStateAnimating;
            this.clockSizeState = clockSizeState;
        }

        boolean isAod() {
            return uiState != null && (uiState == ClockPluginSceneMachine.UI_STATE_AOD
                    || uiState == ClockPluginSceneMachine.UI_STATE_PANORAMIC_AOD);
        }

        String describe() {
            return "uiState=" + (uiState != null ? uiState : "null")
                    + " isAnim=" + (uiStateAnimating != null ? uiStateAnimating : "unknown")
                    + " clockSizeState="
                    + (clockSizeState != null ? clockSizeState : "null");
        }
    }
}
