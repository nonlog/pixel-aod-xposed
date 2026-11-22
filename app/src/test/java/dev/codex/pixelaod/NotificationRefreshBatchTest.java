package dev.codex.pixelaod;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public final class NotificationRefreshBatchTest {
    @Test
    public void firstRequestSchedulesEveryDelay() {
        FakeScheduler scheduler = new FakeScheduler();
        NotificationRefreshBatch batch = new NotificationRefreshBatch(
                new long[]{40L, 280L, 900L}, scheduler);

        batch.request(delay -> { });

        assertEquals(3, scheduler.queue.size());
        assertEquals(40L, scheduler.queue.get(0).delayMillis);
        assertEquals(280L, scheduler.queue.get(1).delayMillis);
        assertEquals(900L, scheduler.queue.get(2).delayMillis);
    }

    @Test
    public void burstReusesEarlySlotsAndMovesOnlyTrailingPass() {
        FakeScheduler scheduler = new FakeScheduler();
        NotificationRefreshBatch batch = new NotificationRefreshBatch(
                new long[]{40L, 280L, 900L}, scheduler);
        List<String> calls = new ArrayList<>();

        batch.request(delay -> calls.add("first:" + delay));
        Runnable firstTail = scheduler.queue.get(2).runnable;
        batch.request(delay -> calls.add("latest:" + delay));

        assertEquals(3, scheduler.queue.size());
        assertSame(firstTail, scheduler.queue.get(2).runnable);
        assertEquals(1, scheduler.removeCount);

        scheduler.runDelay(40L);
        scheduler.runDelay(280L);
        scheduler.runDelay(900L);
        assertEquals("latest:40", calls.get(0));
        assertEquals("latest:280", calls.get(1));
        assertEquals("latest:900", calls.get(2));
    }

    @Test
    public void dispatchedSlotCanBeScheduledAgain() {
        FakeScheduler scheduler = new FakeScheduler();
        NotificationRefreshBatch batch = new NotificationRefreshBatch(
                new long[]{0L, 100L}, scheduler);
        List<Long> calls = new ArrayList<>();

        batch.request(calls::add);
        scheduler.runDelay(0L);
        batch.request(calls::add);

        assertEquals(1, calls.size());
        assertEquals(Long.valueOf(0L), calls.get(0));
        assertEquals(2, scheduler.queue.size());
    }

    private static final class FakeScheduler implements NotificationRefreshBatch.Scheduler {
        final List<Scheduled> queue = new ArrayList<>();
        int removeCount;

        @Override
        public void postDelayed(Runnable runnable, long delayMillis) {
            queue.add(new Scheduled(runnable, delayMillis));
        }

        @Override
        public void removeCallbacks(Runnable runnable) {
            for (Iterator<Scheduled> iterator = queue.iterator(); iterator.hasNext();) {
                if (iterator.next().runnable == runnable) {
                    iterator.remove();
                    removeCount++;
                    return;
                }
            }
        }

        void runDelay(long delayMillis) {
            for (int i = 0; i < queue.size(); i++) {
                Scheduled scheduled = queue.get(i);
                if (scheduled.delayMillis == delayMillis) {
                    queue.remove(i);
                    scheduled.runnable.run();
                    return;
                }
            }
            throw new AssertionError("no runnable for delay=" + delayMillis);
        }
    }

    private static final class Scheduled {
        final Runnable runnable;
        final long delayMillis;

        Scheduled(Runnable runnable, long delayMillis) {
            this.runnable = runnable;
            this.delayMillis = delayMillis;
        }
    }
}
