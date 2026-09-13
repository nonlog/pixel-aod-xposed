package dev.codex.pixelaod;

/**
 * Coalesces repeated multi-delay notification refresh requests.
 *
 * <p>Mirrors the COUI 2.7 scheduling contract: each delay has at most one pending runnable,
 * a newer request replaces the pending action, and the final delayed runnable is moved to the
 * trailing edge so a burst still receives one last reconciliation pass.</p>
 */
final class NotificationRefreshBatch {
    interface Scheduler {
        void postDelayed(Runnable runnable, long delayMillis);

        void removeCallbacks(Runnable runnable);
    }

    interface Action {
        void run(long delayMillis);
    }

    private final long[] delays;
    private final Scheduler scheduler;
    private final Pending[] pending;

    NotificationRefreshBatch(long[] delays, Scheduler scheduler) {
        if (delays == null || delays.length == 0) {
            throw new IllegalArgumentException("delays must not be empty");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler must not be null");
        }
        this.delays = delays.clone();
        this.scheduler = scheduler;
        this.pending = new Pending[delays.length];
    }

    synchronized void request(Action action) {
        if (action == null) {
            throw new IllegalArgumentException("action must not be null");
        }
        for (int index = 0; index < delays.length; index++) {
            long delayMillis = delays[index];
            Pending current = pending[index];
            if (current == null) {
                current = new Pending(index, delayMillis, action);
                pending[index] = current;
                scheduler.postDelayed(current, delayMillis);
                continue;
            }
            current.action = action;
            if (index == delays.length - 1 && delayMillis > 0L) {
                scheduler.removeCallbacks(current);
                scheduler.postDelayed(current, delayMillis);
            }
        }
    }

    private final class Pending implements Runnable {
        final int index;
        final long delayMillis;
        Action action;

        Pending(int index, long delayMillis, Action action) {
            this.index = index;
            this.delayMillis = delayMillis;
            this.action = action;
        }

        @Override
        public void run() {
            Action runAction;
            synchronized (NotificationRefreshBatch.this) {
                if (pending[index] != this) {
                    return;
                }
                pending[index] = null;
                runAction = action;
            }
            runAction.run(delayMillis);
        }
    }
}
