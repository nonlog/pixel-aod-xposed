package dev.codex.pixelaod;

final class PassiveFodShowGate {
    private PassiveFodShowGate() {
    }

    static boolean shouldSuppress(long traceAgeMillis, long proximityFarAgeMillis,
            long explicitWakeAgeMillis) {
        // On OOS 16.0.9 a proximity-near -> proximity-far transition reuses this callback
        // to restore the optical FOD session. Suppressing it leaves authentication active
        // but removes both the FOD icon and the optical sensing path.
        return false;
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
