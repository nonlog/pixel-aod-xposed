package dev.codex.pixelaod;

/**
 * Mirrors stock COUI ownership boundaries for transient native keyguard-surface visibility.
 *
 * <p>The replacement host is installed as a child of the native big-clock root. During
 * LOCKSCREEN <-> PRIMARY/ALTERNATE_BOUNCER motion and LOCKSCREEN <-> OCCLUDED motion, stock
 * SystemUI owns whether that root is actually visible. The module must not add a second
 * INVISIBLE/VISIBLE gate on the child: doing so turns credential, alarm, or call transitions into
 * a visible late re-appearance when the native root returns.</p>
 *
 * <p>The same native-root ownership is used for two zero-duration scene edges at the start of a
 * screen-off-from-unlocked AOD entry. OPlus can briefly report LOCKSCREEN -> GONE CANCELED and
 * immediately follow it with GONE -> DOZING STARTED. The first edge must not release the already
 * prepared non-lockscreen AOD bypass, and the second must not suppress that prepared child before
 * the normal RUNNING GONE -> DOZING path can consume the scoped bypass. Only those edge phases are
 * delegated; RUNNING and FINISHED return to the existing non-lockscreen AOD routing so Direct Final
 * and Animated modes keep their accepted presentation semantics.</p>
 */
final class CouiBouncerHostVisibilityPolicy {
    private static final long NON_LOCKSCREEN_AOD_ENTRY_EDGE_WINDOW_MILLIS = 1_500L;

    private CouiBouncerHostVisibilityPolicy() {
    }

    static boolean nativeHostOwns(NativeKeyguardSceneEligibility.Snapshot scene) {
        if (scene == null) {
            return false;
        }
        if (isNonLockscreenAodEntryEdge(scene)
                && PixelAodRuntimeState.isInAodEntryTransitionWindow(
                        NON_LOCKSCREEN_AOD_ENTRY_EDGE_WINDOW_MILLIS)
                && !PixelAodRuntimeState.wasScreenOffFromInteractiveLockscreen()) {
            return true;
        }
        if (!isLockscreenNativeOwnedPair(scene.from, scene.to)) {
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

    static boolean nativeHostOwnsNonLockscreenAodEntryEdge(
            NativeKeyguardSceneEligibility.Snapshot scene,
            boolean withinAodEntryWindow,
            boolean screenOffFromInteractiveLockscreen) {
        return withinAodEntryWindow
                && !screenOffFromInteractiveLockscreen
                && isNonLockscreenAodEntryEdge(scene);
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

    private static boolean isNonLockscreenAodEntryEdge(
            NativeKeyguardSceneEligibility.Snapshot scene) {
        if (scene == null) {
            return false;
        }
        if (scene.phase == NativeKeyguardSceneEligibility.Phase.CANCELED) {
            return scene.from == NativeKeyguardSceneEligibility.Scene.LOCKSCREEN
                    && scene.to == NativeKeyguardSceneEligibility.Scene.GONE;
        }
        if (scene.phase != NativeKeyguardSceneEligibility.Phase.STARTED
                || scene.from != NativeKeyguardSceneEligibility.Scene.GONE) {
            return false;
        }
        return scene.to == NativeKeyguardSceneEligibility.Scene.DOZING
                || scene.to == NativeKeyguardSceneEligibility.Scene.AOD;
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
