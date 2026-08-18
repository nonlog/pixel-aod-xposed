package dev.codex.pixelaod;

/**
 * Coalesces per-notification lockscreen visibility decisions into one AOD snapshot rebuild.
 *
 * <p>KeyguardNotificationVisibilityProvider is called once per notification while SystemUI is
 * traversing the shade/keyguard model. Rebuilding the complete AOD notification snapshot from
 * every callback turns that traversal into O(N^2) work on the main thread. This gate keeps the
 * synchronous visibility decision itself O(1) and publishes the aggregate AOD snapshot only when
 * AOD can actually consume it.</p>
 */
final class LockscreenVisibilityRefreshGate {
    private boolean dirty;
    private boolean scheduled;

    synchronized boolean markDirty(boolean canRefreshNow) {
        dirty = true;
        if (!canRefreshNow || scheduled) {
            return false;
        }
        scheduled = true;
        return true;
    }

    synchronized boolean requestIfDirty(boolean canRefreshNow) {
        if (!dirty || !canRefreshNow || scheduled) {
            return false;
        }
        scheduled = true;
        return true;
    }

    synchronized boolean beginDispatch(boolean canRefreshNow) {
        scheduled = false;
        if (!dirty || !canRefreshNow) {
            return false;
        }
        dirty = false;
        return true;
    }

    synchronized void onSnapshotPublished() {
        dirty = false;
        scheduled = false;
    }

    synchronized boolean isDirty() {
        return dirty;
    }

    synchronized boolean isScheduled() {
        return scheduled;
    }
}
