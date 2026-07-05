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
                + " rule=" + behavior.ruleLabel
                + " category=" + behavior.categoryLabel
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
        boolean continuousSessionActive = isContinuousSessionActive(state, policy);
        boolean lifecycleWantsPixelOverlay = shouldDrawPixelAod(state)
                || continuousSessionActive;
        boolean shouldApplyModuleAod = lifecycleWantsPixelOverlay
                && policy.allowsDisplay;
        boolean shouldDrawPixelOverlay = shouldApplyModuleAod
                && !proximityBlocked
                && !expandedShadeBlocked;
        boolean shouldKeepNativeDozeAlive = policy.allowsDisplay
                && policy.continuousAllowed
                && (continuousSessionActive || shouldKeepDozeScreenActive(state));
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
        boolean shouldAllowNativeHideCallbacks = shouldAllowNativeHideCallbacks(
                source, state, policy, shouldKeepNativeDozeAlive);
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
                drawReason(lifecycleWantsPixelOverlay, continuousSessionActive,
                        policy, proximityBlocked, expandedShadeBlocked),
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

    private static boolean isContinuousSessionActive(
            PixelAodClockView.AodLifecycleState state, ModulePolicy modulePolicy) {
        return state != null
                && !state.interactive
                && state.active
                && modulePolicy.allowsDisplay
                && modulePolicy.continuousAllowed;
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
            boolean continuousSessionActive, ModulePolicy modulePolicy,
            boolean proximityBlocked, boolean expandedShadeBlocked) {
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
        if (continuousSessionActive) {
            return "continuous-active-session";
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

    private static boolean shouldAllowNativeHideCallbacks(String source,
            PixelAodClockView.AodLifecycleState state, ModulePolicy modulePolicy,
            boolean shouldKeepNativeDozeAlive) {
        if (!shouldKeepNativeDozeAlive) {
            return true;
        }
        if (isNativeTimeoutCallback(source) && state != null && !state.interactive) {
            return true;
        }
        return false;
    }

    private static boolean isNativeTimeoutCallback(String source) {
        return sourceContains(source, "notifyHideCallback")
                || sourceContains(source, "AodRecord#onEnergySavingNotifyHide");
    }

    private static String nativeHideCallbackReason(boolean shouldAllowNativeHideCallbacks,
            boolean shouldKeepNativeDozeAlive, boolean lifecycleWantsPixelOverlay,
            ModulePolicy modulePolicy) {
        if (!shouldAllowNativeHideCallbacks) {
            return "module-keeps-native-doze";
        }
        if (shouldKeepNativeDozeAlive) {
            return "native-timeout-callback";
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
        switch (event) {
            case TRIGGER_PICKUP:
                return TriggerRule.PICKUP_BRIEF.toBehavior();
            case TRIGGER_TAP:
                return TriggerRule.TAP_BRIEF.toBehavior();
            case TRIGGER_POCKET:
                return TriggerRule.POCKET_HIDE.toBehavior();
            case TRIGGER_PROXIMITY:
                if (isProximityNear(lowerCombined)) {
                    return TriggerRule.PROXIMITY_NEAR_HIDE.toBehavior();
                } else if (isProximityFar(lowerCombined)) {
                    return TriggerRule.PROXIMITY_FAR_RELEASE.toBehavior();
                }
                return TriggerRule.PROXIMITY_OBSERVE.toBehavior();
            case TRIGGER_SENSOR:
                return TriggerRule.SENSOR_DIAGNOSTIC.toBehavior();
            case TRIGGER_UNKNOWN:
            default:
                return TriggerRule.UNKNOWN_DIAGNOSTIC.toBehavior();
        }
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

    private enum TriggerCategory {
        DISPLAY_WAKE("display-wake"),
        SENSOR_GUARD("sensor-guard"),
        DIAGNOSTIC("diagnostic-only");

        final String label;

        TriggerCategory(String label) {
            this.label = label;
        }
    }

    private enum TriggerRule {
        PICKUP_BRIEF("pickup-brief", Event.TRIGGER_PICKUP,
                TriggerCategory.DISPLAY_WAKE, DisplayMode.TRIGGER_ONLY_BRIEF_DISPLAY,
                "brief-show-candidate", true, false, false),
        TAP_BRIEF("tap-brief", Event.TRIGGER_TAP,
                TriggerCategory.DISPLAY_WAKE, DisplayMode.TRIGGER_ONLY_BRIEF_DISPLAY,
                "brief-show-candidate", true, false, false),
        POCKET_HIDE("pocket-hide", Event.TRIGGER_POCKET,
                TriggerCategory.SENSOR_GUARD, DisplayMode.SENSOR_GUARD_HIDE,
                "block-brief-and-continuous-aod", false, true, false),
        PROXIMITY_NEAR_HIDE("proximity-near-hide", Event.TRIGGER_PROXIMITY,
                TriggerCategory.SENSOR_GUARD, DisplayMode.SENSOR_GUARD_HIDE,
                "hide-or-block-aod", false, true, false),
        PROXIMITY_FAR_RELEASE("proximity-far-release", Event.TRIGGER_PROXIMITY,
                TriggerCategory.SENSOR_GUARD, DisplayMode.SENSOR_GUARD_RELEASE,
                "allow-future-aod", false, false, true),
        PROXIMITY_OBSERVE("proximity-observe", Event.TRIGGER_PROXIMITY,
                TriggerCategory.SENSOR_GUARD, DisplayMode.SENSOR_GUARD_UNKNOWN,
                "observe-proximity-result", false, false, false),
        SENSOR_DIAGNOSTIC("sensor-diagnostic", Event.TRIGGER_SENSOR,
                TriggerCategory.DIAGNOSTIC, DisplayMode.TRIGGER_DIAGNOSTIC_ONLY,
                "classify-before-action", false, false, false),
        UNKNOWN_DIAGNOSTIC("unknown-diagnostic", Event.TRIGGER_UNKNOWN,
                TriggerCategory.DIAGNOSTIC, DisplayMode.TRIGGER_DIAGNOSTIC_ONLY,
                "classify-before-action", false, false, false);

        final String label;
        final Event event;
        final TriggerCategory category;
        final DisplayMode displayMode;
        final String futureAction;
        final boolean startsBriefDisplay;
        final boolean blocksDisplay;
        final boolean releasesDisplayGuard;

        TriggerRule(String label, Event event, TriggerCategory category,
                DisplayMode displayMode, String futureAction,
                boolean startsBriefDisplay, boolean blocksDisplay,
                boolean releasesDisplayGuard) {
            this.label = label;
            this.event = event;
            this.category = category;
            this.displayMode = displayMode;
            this.futureAction = futureAction;
            this.startsBriefDisplay = startsBriefDisplay;
            this.blocksDisplay = blocksDisplay;
            this.releasesDisplayGuard = releasesDisplayGuard;
        }

        TriggerBehavior toBehavior() {
            return new TriggerBehavior(event.label, label, category.label,
                    displayMode.label, futureAction, startsBriefDisplay,
                    blocksDisplay, releasesDisplayGuard);
        }
    }

    static final class TriggerBehavior {
        final String eventLabel;
        final String ruleLabel;
        final String categoryLabel;
        final String displayModeLabel;
        final String futureAction;
        final boolean startsBriefDisplay;
        final boolean blocksDisplay;
        final boolean releasesDisplayGuard;

        TriggerBehavior(String eventLabel, String ruleLabel, String categoryLabel,
                String displayModeLabel, String futureAction, boolean startsBriefDisplay,
                boolean blocksDisplay, boolean releasesDisplayGuard) {
            this.eventLabel = eventLabel;
            this.ruleLabel = ruleLabel;
            this.categoryLabel = categoryLabel;
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
