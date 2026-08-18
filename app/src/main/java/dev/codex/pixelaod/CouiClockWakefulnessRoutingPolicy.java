package dev.codex.pixelaod;

/** Pure method-name policy for the process-global COUI AOD-exit lifecycle arm. */
final class CouiClockWakefulnessRoutingPolicy {
    private static final String STARTED_WAKING_UP = "dispatchStartedWakingUp";
    private static final String AOD_EXIT_ARM_SOURCE =
            "WakefulnessLifecycle#dispatchStartedWakingUp";

    private CouiClockWakefulnessRoutingPolicy() {
    }

    static boolean shouldArmAodExit(String lifecycleMethodName) {
        return STARTED_WAKING_UP.equals(lifecycleMethodName);
    }

    static String aodExitArmSource() {
        return AOD_EXIT_ARM_SOURCE;
    }
}
