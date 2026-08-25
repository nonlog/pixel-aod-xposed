package dev.codex.pixelaod;

/**
 * Mirrors the stock COUI ownership boundary for credential-surface visibility.
 *
 * <p>The replacement host is installed as a child of the native big-clock root. During
 * LOCKSCREEN <-> PRIMARY/ALTERNATE_BOUNCER motion, stock SystemUI owns whether that root is
 * actually visible. The module must not add a second INVISIBLE/VISIBLE gate on the child: doing
 * so turns predictive/cancelled bouncer transitions into a visible late re-appearance.</p>
 */
final class CouiBouncerHostVisibilityPolicy {
    private CouiBouncerHostVisibilityPolicy() {
    }

    static boolean nativeHostOwns(NativeKeyguardSceneEligibility.Snapshot scene) {
        if (scene == null || !isLockscreenBouncerPair(scene.from, scene.to)) {
            return false;
        }
        if (scene.phase == NativeKeyguardSceneEligibility.Phase.FINISHED) {
            return isBouncer(scene.to);
        }
        if (scene.phase == NativeKeyguardSceneEligibility.Phase.CANCELED) {
            // A cancelled transition settles back on its source scene.
            return isBouncer(scene.from);
        }
        // STARTED/RUNNING/UNKNOWN: the native host is moving between the two surfaces and owns
        // effective visibility for the whole interval.
        return true;
    }

    static boolean isLockscreenBouncerPair(NativeKeyguardSceneEligibility.Scene from,
            NativeKeyguardSceneEligibility.Scene to) {
        return (from == NativeKeyguardSceneEligibility.Scene.LOCKSCREEN && isBouncer(to))
                || (to == NativeKeyguardSceneEligibility.Scene.LOCKSCREEN && isBouncer(from));
    }

    private static boolean isBouncer(NativeKeyguardSceneEligibility.Scene scene) {
        return scene == NativeKeyguardSceneEligibility.Scene.PRIMARY_BOUNCER
                || scene == NativeKeyguardSceneEligibility.Scene.ALTERNATE_BOUNCER;
    }
}
