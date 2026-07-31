package dev.codex.pixelaod;

/** Prevents a missing media icon from causing repeated expensive drawable loads. */
final class MediaIconRecoveryPolicy {
    private MediaIconRecoveryPolicy() {
    }

    static boolean shouldRetry(boolean iconMissing, boolean notificationAvailable,
            String previousRecoverySignature, String iconSignature) {
        return iconMissing
                && notificationAvailable
                && !same(previousRecoverySignature, iconSignature);
    }

    private static boolean same(String first, String second) {
        return first == second || (first != null && first.equals(second));
    }
}
