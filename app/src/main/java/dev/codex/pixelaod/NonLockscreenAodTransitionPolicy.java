package dev.codex.pixelaod;

/** User-selectable behavior for screen-off after the device has already been unlocked. */
final class NonLockscreenAodTransitionPolicy {
    enum Mode {
        ANIMATED,
        DIRECT_FINAL
    }

    private NonLockscreenAodTransitionPolicy() {
    }

    static Mode fromSetting(String value) {
        return PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_DIRECT_FINAL.equals(value)
                ? Mode.DIRECT_FINAL : Mode.ANIMATED;
    }

    static Mode resolve(android.content.Context context) {
        return fromSetting(PixelAodSettings.getString(
                context,
                PixelAodSettings.KEY_NON_LOCKSCREEN_AOD_TRANSITION,
                PixelAodSettings.NON_LOCKSCREEN_AOD_TRANSITION_ANIMATED));
    }

    static boolean isDirectFinal(Mode mode) {
        return mode == Mode.DIRECT_FINAL;
    }
}