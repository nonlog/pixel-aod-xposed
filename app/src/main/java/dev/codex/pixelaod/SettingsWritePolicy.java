package dev.codex.pixelaod;

/** Authorization and numeric validation at the exported settings write boundary. */
final class SettingsWritePolicy {
    private SettingsWritePolicy() { }

    static boolean isTrustedUid(int callingUid, int ownerUid) {
        // Full UIDs, not app IDs: a different Android user must not inherit this app's access.
        return callingUid >= 0 && (callingUid == ownerUid
                || callingUid == 0 || callingUid == 1000 || callingUid == 2000);
    }

    static float finiteFloat(Object rawValue, float fallback) {
        try {
            float parsed = rawValue instanceof Number ? ((Number) rawValue).floatValue()
                    : Float.parseFloat(String.valueOf(rawValue));
            return Float.isNaN(parsed) || Float.isInfinite(parsed) ? fallback : parsed;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
