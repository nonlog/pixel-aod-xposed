package dev.codex.pixelaod;

/** Pure decision policy for applying module-owned UDFPS HDR SurfaceControl state. */
final class CouiUdfpsHdrUpdatePolicy {
    private CouiUdfpsHdrUpdatePolicy() {
    }

    static boolean shouldApply(boolean hasPrevious,
            boolean previousActive,
            boolean sameSurface,
            boolean active,
            boolean force) {
        return force
                || !hasPrevious
                || previousActive != active
                || !sameSurface;
    }
}
