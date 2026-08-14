package dev.codex.pixelaod;

/**
 * Decides whether stock AOD views must remain suppressed while OPlus is retiring the
 * native AOD surface. Android can report the device as interactive before the AOD host
 * is detached, especially during UDFPS unlock. Restoring the original stock view state
 * in that gap can expose native notification icons for a frame.
 */
final class StockAodExitRestoreGate {
    private StockAodExitRestoreGate() {
    }

    static boolean shouldDeferRestore(boolean interactive, boolean stockHostIsAodRoot,
            boolean stockHostAttachedOrParented, boolean sameAodTrace) {
        return interactive
                && stockHostIsAodRoot
                && stockHostAttachedOrParented
                && sameAodTrace;
    }
}
