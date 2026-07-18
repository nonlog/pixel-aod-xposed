package dev.codex.pixelaod;

final class PassiveFodShowGate {
    private static final long AOD_ENTRY_GRACE_MILLIS = 4_000L;
    private static final long PROXIMITY_FAR_CAUSE_WINDOW_MILLIS = 1_500L;
    private static final long EXPLICIT_WAKE_ALLOW_WINDOW_MILLIS = 1_500L;

    private PassiveFodShowGate() {
    }

    static boolean shouldSuppress(long traceAgeMillis, long proximityFarAgeMillis,
            long explicitWakeAgeMillis) {
        if (traceAgeMillis < AOD_ENTRY_GRACE_MILLIS) {
            return false;
        }
        if (proximityFarAgeMillis < 0L
                || proximityFarAgeMillis > PROXIMITY_FAR_CAUSE_WINDOW_MILLIS) {
            return false;
        }
        return explicitWakeAgeMillis < 0L
                || explicitWakeAgeMillis > EXPLICIT_WAKE_ALLOW_WINDOW_MILLIS;
    }

    static boolean isFodShowInvocation(String methodName, Object[] args) {
        if (!isPotentialFodShowMethod(methodName)) {
            return false;
        }
        if ("setFpIconVisibilityInAOD".equals(methodName)
                || "setFingerprintIconShow".equals(methodName)
                || "showOrHideFingerprintIconTemporarily".equals(methodName)) {
            return firstBooleanArgIsTrue(args);
        }
        return true;
    }

    static boolean isPotentialFodShowMethod(String methodName) {
        if ("notifyShowAodIcon".equals(methodName)
                || "showUdfpsOverlay".equals(methodName)
                || "fpIconShow".equals(methodName)
                || "showFingerprintIconTemporarily".equals(methodName)) {
            return true;
        }
        if ("setFpIconVisibilityInAOD".equals(methodName)
                || "setFingerprintIconShow".equals(methodName)
                || "showOrHideFingerprintIconTemporarily".equals(methodName)) {
            return true;
        }
        return false;
    }

    private static boolean firstBooleanArgIsTrue(Object[] args) {
        return args != null
                && args.length > 0
                && args[0] instanceof Boolean
                && (Boolean) args[0];
    }
}
