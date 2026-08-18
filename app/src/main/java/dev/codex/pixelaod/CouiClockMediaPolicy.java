package dev.codex.pixelaod;

/** Pure media eligibility contract copied from the COUI 2.5 anti-obfuscation build. */
final class CouiClockMediaPolicy {
    private static final int PLAYBACK_STATE_PLAYING = 3;

    private CouiClockMediaPolicy() {
    }

    static boolean isActivePlaybackState(int state) {
        return state == PLAYBACK_STATE_PLAYING;
    }

    /** COUI's ActiveMedia data-class equality ignores playback-position-only callbacks. */
    static boolean sameSemanticMedia(boolean firstPresent, String firstPackage,
            CharSequence firstTitle, CharSequence firstArtist, boolean secondPresent,
            String secondPackage, CharSequence secondTitle, CharSequence secondArtist) {
        return firstPresent == secondPresent
                && safe(firstPackage).equals(safe(secondPackage))
                && safe(firstTitle).equals(safe(secondTitle))
                && safe(firstArtist).equals(safe(secondArtist));
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
