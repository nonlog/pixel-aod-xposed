package dev.codex.pixelaod;

import android.text.TextUtils;

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
        NATIVE_TICK("native-tick"),
        NOTIFICATION_SNAPSHOT("notification-snapshot"),
        VISIBILITY_DECISION("visibility-decision"),
        MODULE_EVENT("module-event");

        final String label;

        Event(String label) {
            this.label = label;
        }
    }
}
