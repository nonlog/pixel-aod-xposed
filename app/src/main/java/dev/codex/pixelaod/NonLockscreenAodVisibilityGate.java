package dev.codex.pixelaod;

/**
 * Controls whether an AOD lifecycle transition may draw before Android reports a Doze display.
 *
 * <p>OOS 16.0.9 reports screen-off well before the wallpaper has disappeared for a desktop or
 * app-originated sleep. The entry/grace windows are safe for a real lockscreen handoff, but they
 * must not reveal the persistent Pixel surface during a non-lockscreen entry.</p>
 */
final class NonLockscreenAodVisibilityGate {
    private NonLockscreenAodVisibilityGate() {
    }

    static boolean shouldDraw(boolean screenOffFromInteractiveLockscreen, boolean displayInAod,
            boolean entryDelay, boolean triggerBriefActive, boolean aodActive,
            boolean graceWindow) {
        if (displayInAod || triggerBriefActive) {
            return true;
        }
        return screenOffFromInteractiveLockscreen
                && (entryDelay || (aodActive && graceWindow));
    }
}
