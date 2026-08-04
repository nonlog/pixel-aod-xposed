package dev.codex.pixelaod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Identifies OOS builds whose AOD host changes coordinates after the lockscreen commits. */
final class OosAodHandoffProfile {
    private static final long LEGACY_NON_LOCKSCREEN_REVEAL_DELAY_MILLIS = 810L;
    private static final Pattern OOS_16_BUILD =
            Pattern.compile("(?:^|_)16\\.0\\.(\\d+)(?:[._(]|$)");

    private OosAodHandoffProfile() {
    }

    static boolean usesStableSingleLayerAodHandoff(String buildDisplay) {
        return oos1609OrLater(buildDisplay);
    }

    static boolean usesSystemManagedBurnIn(String buildDisplay) {
        return oos1609OrLater(buildDisplay);
    }

    static long nonLockscreenRevealDelayMillis(String buildDisplay) {
        return oos1609OrLater(buildDisplay) ? 0L : LEGACY_NON_LOCKSCREEN_REVEAL_DELAY_MILLIS;
    }

    private static boolean oos1609OrLater(String buildDisplay) {
        if (buildDisplay == null) {
            return false;
        }
        Matcher matcher = OOS_16_BUILD.matcher(buildDisplay);
        if (!matcher.find()) {
            return false;
        }
        try {
            return Integer.parseInt(matcher.group(1)) >= 9;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
