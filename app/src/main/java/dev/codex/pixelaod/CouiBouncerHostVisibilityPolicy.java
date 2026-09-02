package dev.codex.pixelaod;

/**
 * Mirrors the stock COUI ownership boundary for transient native keyguard-surface visibility.
 *
 * <p>The replacement host is installed as a child of the native big-clock root. During
 * LOCKSCREEN <-> PRIMARY/ALTERNATE_BOUNCER motion and LOCKSCREEN <-> OCCLUDED motion, stock
 * SystemUI owns whether that root is actually visible. The module must not add a second
 * INVISIBLE/VISIBLE gate on the child: doing so turns credential, alarm, or call transitions into
 * a visible late re-appearance when the native root returns.</p>
 *
 * <p>The historical class name is retained because the first proven instance was the PIN/bouncer
 * path. OCCLUDED uses the same ownership rule: preserve the already-rendered child while a
 * full-screen alarm/call hides the native root, then let that root reveal the child immediately
 * when keyguard occlusion clears.</p>
 */
final class CouiBouncerHostVisibilityPolicy {
    private CouiBouncerHostVisibilityPolicy() {
    }

    static boolean nativeHostOwns(NativeKeyguardSceneEligibility.Snapshot scene) {
        if (scene == null || !isLockscreenNativeOwnedPair(scene.from, scene.to)) {
            return false;
        }
        if (scene.phase == NativeKeyguardSceneEligibility.Phase.FINISHED) {
            return isNativeOwnedTransientScene(scene.to);
        }
        if (scene.phase == NativeKeyguardSceneEligibility.Phase.CANCELED) {
            // A cancelled transition settles back on its source scene.
            return isNativeOwnedTransientScene(scene.from);
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

    static boolean isLockscreenOccludedPair(NativeKeyguardSceneEligibility.Scene from,
            NativeKeyguardSceneEligibility.Scene to) {
        return (from == NativeKeyguardSceneEligibility.Scene.LOCKSCREEN
                && to == NativeKeyguardSceneEligibility.Scene.OCCLUDED)
                || (to == NativeKeyguardSceneEligibility.Scene.LOCKSCREEN
                && from == NativeKeyguardSceneEligibility.Scene.OCCLUDED);
    }

    private static boolean isLockscreenNativeOwnedPair(NativeKeyguardSceneEligibility.Scene from,
            NativeKeyguardSceneEligibility.Scene to) {
        return isLockscreenBouncerPair(from, to) || isLockscreenOccludedPair(from, to);
    }

    private static boolean isNativeOwnedTransientScene(
            NativeKeyguardSceneEligibility.Scene scene) {
        return isBouncer(scene) || scene == NativeKeyguardSceneEligibility.Scene.OCCLUDED;
    }

    private static boolean isBouncer(NativeKeyguardSceneEligibility.Scene scene) {
        return scene == NativeKeyguardSceneEligibility.Scene.PRIMARY_BOUNCER
                || scene == NativeKeyguardSceneEligibility.Scene.ALTERNATE_BOUNCER;
    }
}
