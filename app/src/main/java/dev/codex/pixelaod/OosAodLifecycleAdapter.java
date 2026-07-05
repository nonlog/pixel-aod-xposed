package dev.codex.pixelaod;

import android.text.TextUtils;

import java.util.Locale;

final class OosAodLifecycleAdapter {
    private OosAodLifecycleAdapter() {
    }

    static void recordPhaseChange(String source, String previousPhase, String phase,
            String trace, String stateDescription) {
        String normalizedSource = normalizeSource(source);
        Event event = classify(normalizedSource);
        PixelAodLog.log("OOS AOD lifecycle mapping event=" + event.label
                + " source=" + normalizedSource
                + " from=" + emptyAsNone(previousPhase)
                + " to=" + emptyAsNone(phase)
                + " trace=" + emptyAsNone(trace)
                + " state={" + stateDescription + "}");
    }

    static void recordTriggerEvent(String triggerType, String source, String detail,
            boolean behaviorApplied, String trace, String stateDescription) {
        String normalizedType = normalizeSource(triggerType);
        String normalizedSource = normalizeSource(source);
        TriggerBehavior behavior = behaviorForTrigger(normalizedType, normalizedSource, detail);
        PixelAodLog.log("OOS AOD trigger mapping event=" + behavior.eventLabel
                + " displayMode=" + behavior.displayModeLabel
                + " futureAction=" + behavior.futureAction
                + " behaviorApplied=" + behaviorApplied
                + " trigger=" + normalizedType
                + " source=" + normalizedSource
                + " detail={" + (TextUtils.isEmpty(detail) ? "" : detail) + "}"
                + " trace=" + emptyAsNone(trace)
                + " state={" + stateDescription + "}");
    }

    static TriggerBehavior behaviorForTrigger(String triggerType, String source, String detail) {
        String normalizedType = normalizeSource(triggerType);
        String normalizedSource = normalizeSource(source);
        return mapTrigger(normalizedType, normalizedSource, detail);
    }

    static boolean matchesExpectedTrace(String expectedTrace, String currentTrace) {
        return TextUtils.isEmpty(expectedTrace) || TextUtils.equals(expectedTrace, currentTrace);
    }

    static AodPolicyDecision evaluatePolicy(String source, String trace,
            PixelAodClockView.AodLifecycleState state, ModulePolicy modulePolicy,
            boolean proximityBlocked, boolean expandedShadeBlocked) {
        ModulePolicy policy = modulePolicy != null
                ? modulePolicy
                : new ModulePolicy(false, false, false, false,
                "no-module-policy", "unknown", false, false);
        boolean lifecycleWantsPixelOverlay = shouldDrawPixelAod(state);
        boolean shouldApplyModuleAod = lifecycleWantsPixelOverlay
                && policy.allowsDisplay;
        boolean shouldDrawPixelOverlay = shouldApplyModuleAod
                && !proximityBlocked
                && !expandedShadeBlocked;
        boolean shouldKeepNativeDozeAlive = policy.allowsDisplay
                && policy.continuousAllowed
                && shouldKeepDozeScreenActive(state);
        boolean nativeAodTransition = state != null
                && (state.displayAod
                || state.entryDelay
                || state.active
                || state.graceWindow
                || state.triggerBriefActive);
        boolean shouldSuppressStockAodViews = policy.moduleEnabled
                && state != null
                && !state.interactive
                && (shouldApplyModuleAod || nativeAodTransition);
        boolean shouldAllowNativeHideCallbacks = !shouldKeepNativeDozeAlive;
        return new AodPolicyDecision(
                source,
                trace,
                state,
                lifecycleWantsPixelOverlay,
                policy.allowsDisplay,
                shouldApplyModuleAod,
                shouldDrawPixelOverlay,
                shouldKeepNativeDozeAlive,
                shouldSuppressStockAodViews,
                shouldAllowNativeHideCallbacks,
                proximityBlocked,
                expandedShadeBlocked,
                policy.reason,
                drawReason(lifecycleWantsPixelOverlay, policy, proximityBlocked,
                        expandedShadeBlocked),
                keepDozeReason(shouldKeepNativeDozeAlive, lifecycleWantsPixelOverlay,
                        policy),
                stockSuppressionReason(shouldSuppressStockAodViews, shouldApplyModuleAod,
                        policy, state),
                nativeHideCallbackReason(shouldAllowNativeHideCallbacks,
                        shouldKeepNativeDozeAlive, lifecycleWantsPixelOverlay, policy),
                policy.displayMode,
                policy.withinSchedule);
    }

    static boolean shouldDrawPixelAod(PixelAodClockView.AodLifecycleState state) {
        return state != null && state.shouldDrawPixelAod();
    }

    static boolean shouldKeepDozeScreenActive(PixelAodClockView.AodLifecycleState state) {
        return state != null
                && !state.interactive
                && (state.recentOverlayVisible || shouldDrawPixelAod(state));
    }

    static boolean shouldBridgeLockscreenDuringAodEntry(
            PixelAodClockView.AodLifecycleState state, long windowMillis) {
        return state != null
                && !state.interactive
                && !state.active
                && state.isInEntryTransitionWindow(windowMillis);
    }

    private static String drawReason(boolean lifecycleWantsPixelOverlay,
            ModulePolicy modulePolicy, boolean proximityBlocked, boolean expandedShadeBlocked) {
        if (!lifecycleWantsPixelOverlay) {
            return "lifecycle-not-ready";
        }
        if (!modulePolicy.allowsDisplay) {
            return modulePolicy.reason;
        }
        if (proximityBlocked) {
            return "proximity-near";
        }
        if (expandedShadeBlocked) {
            return "expanded-system-shade";
        }
        if ("trigger-brief-display".equals(modulePolicy.reason)) {
            return "trigger-brief-display";
        }
        return "all-checks-passed";
    }

    private static String keepDozeReason(boolean shouldKeepNativeDozeAlive,
            boolean lifecycleWantsPixelOverlay, ModulePolicy modulePolicy) {
        if (shouldKeepNativeDozeAlive) {
            return "recent-pixel-aod";
        }
        if (!modulePolicy.allowsDisplay) {
            return modulePolicy.reason;
        }
        if (modulePolicy.triggerBriefAllowed) {
            return "trigger-brief-allows-native-hide";
        }
        if (!lifecycleWantsPixelOverlay) {
            return "lifecycle-not-ready";
        }
        return "native-doze-not-needed";
    }

    private static String stockSuppressionReason(boolean shouldSuppressStockAodViews,
            boolean shouldApplyModuleAod, ModulePolicy modulePolicy,
            PixelAodClockView.AodLifecycleState state) {
        if (!shouldSuppressStockAodViews) {
            if (state == null) {
                return "no-state";
            }
            if (state.interactive) {
                return "interactive";
            }
            return "not-needed";
        }
        if (!modulePolicy.allowsDisplay) {
            return "module-aod-policy";
        }
        if (shouldApplyModuleAod) {
            return "pixel-overlay-active";
        }
        return "native-aod-transition";
    }

    private static String nativeHideCallbackReason(boolean shouldAllowNativeHideCallbacks,
            boolean shouldKeepNativeDozeAlive, boolean lifecycleWantsPixelOverlay,
            ModulePolicy modulePolicy) {
        if (!shouldAllowNativeHideCallbacks) {
            return "module-keeps-native-doze";
        }
        if (!modulePolicy.allowsDisplay) {
            return modulePolicy.reason;
        }
        if (modulePolicy.triggerBriefAllowed) {
            return "trigger-brief-allows-native-hide";
        }
        if (!lifecycleWantsPixelOverlay) {
            return "lifecycle-not-ready";
        }
        if (!shouldKeepNativeDozeAlive) {
            return "native-doze-not-needed";
        }
        return "allowed";
    }

    private static Event classify(String source) {
        if (sourceContains(source, "onDreamingStarted")) {
            return Event.DREAMING_STARTED;
        }
        if (sourceContains(source, "onDreamingStopped")) {
            return Event.DREAMING_STOPPED;
        }
        if (sourceContains(source, "screen-off")
                || sourceContains(source, "android.intent.action.SCREEN_OFF")) {
            return Event.SCREEN_OFF;
        }
        if (sourceContains(source, "screen-on")
                || sourceContains(source, "android.intent.action.SCREEN_ON")) {
            return Event.SCREEN_ON;
        }
        if (sourceContains(source, "AODDisplayUtil")
                || sourceContains(source, "DreamService#setDozeScreenState")
                || sourceContains(source, "SmoothTransitionController")) {
            return Event.DISPLAY_STATE_REQUEST;
        }
        if (sourceContains(source, "AodClockLayout")
                || sourceContains(source, "host-ready")
                || sourceContains(source, "createAndInitRootView")) {
            return Event.AOD_HOST;
        }
        if (sourceContains(source, "onEnergySavingNotifyHide")
                || sourceContains(source, "notifyHideCallback")
                || sourceContains(source, "suppressed-hide")) {
            return Event.ENERGY_SAVING_HIDE;
        }
        if (sourceContains(source, "proximity")
                || sourceContains(source, "pocket")
                || sourceContains(source, "pickup")
                || sourceContains(source, "pick-up")
                || sourceContains(source, "lift")
                || sourceContains(source, "tap")
                || sourceContains(source, "gesture")
                || sourceContains(source, "sensor")) {
            return classifyTrigger(source);
        }
        if (sourceContains(source, "nativeTick")
                || sourceContains(source, "performAodUpdate")
                || sourceContains(source, "refreshAodTime")
                || sourceContains(source, "time-broadcast")
                || sourceContains(source, "aod-entry-delayed")) {
            return Event.NATIVE_TICK;
        }
        if (sourceContains(source, "snapshot")
                || sourceContains(source, "setActiveNotifications")) {
            return Event.NOTIFICATION_SNAPSHOT;
        }
        if (sourceContains(source, "updateAodVisibility")
                || sourceContains(source, "visibility")) {
            return Event.VISIBILITY_DECISION;
        }
        return Event.MODULE_EVENT;
    }

    private static Event classifyTrigger(String source) {
        String lowerSource = TextUtils.isEmpty(source) ? "" : source.toLowerCase(Locale.US);
        if (sourceContains(lowerSource, "proximity")
                || sourceContains(lowerSource, "prox")
                || sourceContains(lowerSource, "near")
                || sourceContains(lowerSource, "far")) {
            return Event.TRIGGER_PROXIMITY;
        }
        if (sourceContains(lowerSource, "pocket")) {
            return Event.TRIGGER_POCKET;
        }
        if (sourceContains(lowerSource, "pickup")
                || sourceContains(lowerSource, "pick-up")
                || sourceContains(lowerSource, "pick_up")
                || sourceContains(lowerSource, "raise")
                || sourceContains(lowerSource, "lift")) {
            return Event.TRIGGER_PICKUP;
        }
        if (sourceContains(lowerSource, "tap")
                || sourceContains(lowerSource, "touch")
                || sourceContains(lowerSource, "gesture")) {
            return Event.TRIGGER_TAP;
        }
        if (sourceContains(lowerSource, "sensor")) {
            return Event.TRIGGER_SENSOR;
        }
        return Event.TRIGGER_UNKNOWN;
    }

    private static TriggerBehavior mapTrigger(String triggerType, String source, String detail) {
        String combined = normalizeSource(triggerType) + " "
                + normalizeSource(source) + " "
                + normalizeSource(detail);
        String lowerCombined = combined.toLowerCase(Locale.US);
        Event event = classifyTrigger(combined);
        DisplayMode displayMode;
        String futureAction;
        boolean startsBriefDisplay = false;
        boolean blocksDisplay = false;
        boolean releasesDisplayGuard = false;
        switch (event) {
            case TRIGGER_PICKUP:
            case TRIGGER_TAP:
                displayMode = DisplayMode.TRIGGER_ONLY_BRIEF_DISPLAY;
                futureAction = "brief-show-candidate";
                startsBriefDisplay = true;
                break;
            case TRIGGER_POCKET:
                displayMode = DisplayMode.SENSOR_GUARD_HIDE;
                futureAction = "block-brief-and-continuous-aod";
                blocksDisplay = true;
                break;
            case TRIGGER_PROXIMITY:
                if (isProximityNear(lowerCombined)) {
                    displayMode = DisplayMode.SENSOR_GUARD_HIDE;
                    futureAction = "hide-or-block-aod";
                    blocksDisplay = true;
                } else if (isProximityFar(lowerCombined)) {
                    displayMode = DisplayMode.SENSOR_GUARD_RELEASE;
                    futureAction = "allow-future-aod";
                    releasesDisplayGuard = true;
                } else {
                    displayMode = DisplayMode.SENSOR_GUARD_UNKNOWN;
                    futureAction = "observe-proximity-result";
                }
                break;
            case TRIGGER_SENSOR:
            case TRIGGER_UNKNOWN:
            default:
                displayMode = DisplayMode.TRIGGER_DIAGNOSTIC_ONLY;
                futureAction = "classify-before-action";
                break;
        }
        return new TriggerBehavior(event.label, displayMode.label, futureAction,
                startsBriefDisplay, blocksDisplay, releasesDisplayGuard);
    }

    private static boolean isProximityNear(String lowerSource) {
        return sourceContains(lowerSource, "proximity-near")
                || sourceContains(lowerSource, "near=true")
                || (sourceContains(lowerSource, "prox")
                && sourceContains(lowerSource, "boolean(true)"));
    }

    private static boolean isProximityFar(String lowerSource) {
        return sourceContains(lowerSource, "proximity-far")
                || sourceContains(lowerSource, "far")
                || sourceContains(lowerSource, "near=false")
                || (sourceContains(lowerSource, "prox")
                && sourceContains(lowerSource, "boolean(false)"));
    }

    private static boolean sourceContains(String source, String token) {
        return !TextUtils.isEmpty(source) && source.contains(token);
    }

    private static String normalizeSource(String source) {
        return TextUtils.isEmpty(source) ? "unknown" : source;
    }

    private static String emptyAsNone(String value) {
        return TextUtils.isEmpty(value) ? "none" : value;
    }

    private enum Event {
        DREAMING_STARTED("dreaming-started"),
        DREAMING_STOPPED("dreaming-stopped"),
        SCREEN_OFF("screen-off"),
        SCREEN_ON("screen-on"),
        DISPLAY_STATE_REQUEST("display-state-request"),
        AOD_HOST("aod-host"),
        ENERGY_SAVING_HIDE("energy-saving-hide"),
        TRIGGER_PICKUP("trigger-pickup"),
        TRIGGER_TAP("trigger-tap"),
        TRIGGER_PROXIMITY("trigger-proximity"),
        TRIGGER_POCKET("trigger-pocket"),
        TRIGGER_SENSOR("trigger-sensor"),
        TRIGGER_UNKNOWN("trigger-unknown"),
        NATIVE_TICK("native-tick"),
        NOTIFICATION_SNAPSHOT("notification-snapshot"),
        VISIBILITY_DECISION("visibility-decision"),
        MODULE_EVENT("module-event");

        final String label;

        Event(String label) {
            this.label = label;
        }
    }

    private enum DisplayMode {
        TRIGGER_ONLY_BRIEF_DISPLAY("trigger-only-brief-display"),
        SENSOR_GUARD_HIDE("sensor-guard-hide"),
        SENSOR_GUARD_RELEASE("sensor-guard-release"),
        SENSOR_GUARD_UNKNOWN("sensor-guard-unknown"),
        TRIGGER_DIAGNOSTIC_ONLY("trigger-diagnostic-only");

        final String label;

        DisplayMode(String label) {
            this.label = label;
        }
    }

    static final class TriggerBehavior {
        final String eventLabel;
        final String displayModeLabel;
        final String futureAction;
        final boolean startsBriefDisplay;
        final boolean blocksDisplay;
        final boolean releasesDisplayGuard;

        TriggerBehavior(String eventLabel, String displayModeLabel, String futureAction,
                boolean startsBriefDisplay, boolean blocksDisplay,
                boolean releasesDisplayGuard) {
            this.eventLabel = eventLabel;
            this.displayModeLabel = displayModeLabel;
            this.futureAction = futureAction;
            this.startsBriefDisplay = startsBriefDisplay;
            this.blocksDisplay = blocksDisplay;
            this.releasesDisplayGuard = releasesDisplayGuard;
        }
    }

    static final class ModulePolicy {
        final boolean allowsDisplay;
        final boolean moduleEnabled;
        final boolean continuousAllowed;
        final boolean triggerBriefAllowed;
        final String reason;
        final String displayMode;
        final boolean withinSchedule;
        final boolean triggerBriefActive;

        ModulePolicy(boolean allowsDisplay, boolean moduleEnabled,
                boolean continuousAllowed, boolean triggerBriefAllowed, String reason,
                String displayMode, boolean withinSchedule, boolean triggerBriefActive) {
            this.allowsDisplay = allowsDisplay;
            this.moduleEnabled = moduleEnabled;
            this.continuousAllowed = continuousAllowed;
            this.triggerBriefAllowed = triggerBriefAllowed;
            this.reason = reason;
            this.displayMode = displayMode;
            this.withinSchedule = withinSchedule;
            this.triggerBriefActive = triggerBriefActive;
        }
    }

    static final class AodPolicyDecision {
        final String source;
        final String trace;
        final PixelAodClockView.AodLifecycleState state;
        final boolean lifecycleWantsPixelOverlay;
        final boolean modulePolicyAllowsDisplay;
        final boolean shouldApplyModuleAod;
        final boolean shouldDrawPixelOverlay;
        final boolean shouldKeepNativeDozeAlive;
        final boolean shouldSuppressStockAodViews;
        final boolean shouldAllowNativeHideCallbacks;
        final boolean proximityBlocked;
        final boolean expandedShadeBlocked;
        final String modulePolicyReason;
        final String drawReason;
        final String keepNativeDozeReason;
        final String stockSuppressionReason;
        final String nativeHideCallbackReason;
        final String displayMode;
        final boolean withinSchedule;

        AodPolicyDecision(String source, String trace,
                PixelAodClockView.AodLifecycleState state,
                boolean lifecycleWantsPixelOverlay, boolean modulePolicyAllowsDisplay,
                boolean shouldApplyModuleAod, boolean shouldDrawPixelOverlay,
                boolean shouldKeepNativeDozeAlive, boolean shouldSuppressStockAodViews,
                boolean shouldAllowNativeHideCallbacks, boolean proximityBlocked,
                boolean expandedShadeBlocked, String modulePolicyReason, String drawReason,
                String keepNativeDozeReason, String stockSuppressionReason,
                String nativeHideCallbackReason, String displayMode, boolean withinSchedule) {
            this.source = source;
            this.trace = trace;
            this.state = state;
            this.lifecycleWantsPixelOverlay = lifecycleWantsPixelOverlay;
            this.modulePolicyAllowsDisplay = modulePolicyAllowsDisplay;
            this.shouldApplyModuleAod = shouldApplyModuleAod;
            this.shouldDrawPixelOverlay = shouldDrawPixelOverlay;
            this.shouldKeepNativeDozeAlive = shouldKeepNativeDozeAlive;
            this.shouldSuppressStockAodViews = shouldSuppressStockAodViews;
            this.shouldAllowNativeHideCallbacks = shouldAllowNativeHideCallbacks;
            this.proximityBlocked = proximityBlocked;
            this.expandedShadeBlocked = expandedShadeBlocked;
            this.modulePolicyReason = modulePolicyReason;
            this.drawReason = drawReason;
            this.keepNativeDozeReason = keepNativeDozeReason;
            this.stockSuppressionReason = stockSuppressionReason;
            this.nativeHideCallbackReason = nativeHideCallbackReason;
            this.displayMode = displayMode;
            this.withinSchedule = withinSchedule;
        }

        String stateDisplayMode() {
            return displayMode;
        }

        boolean stateWithinSchedule() {
            return withinSchedule;
        }
    }
}
