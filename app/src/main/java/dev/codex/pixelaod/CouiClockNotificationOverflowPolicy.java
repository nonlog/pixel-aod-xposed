package dev.codex.pixelaod;

/** Pure capacity/overflow plan. Capacity comes from current SystemUI resources, not a module constant. */
final class CouiClockNotificationOverflowPolicy {
    private CouiClockNotificationOverflowPolicy() {
    }

    static Plan forCount(int totalIconCount, int maxVisibleIcons) {
        int total = Math.max(0, totalIconCount);
        int capacity = Math.max(1, maxVisibleIcons);
        int visible = Math.min(capacity, total);
        return new Plan(total, visible);
    }

    static final class Plan {
        private final int totalCount;
        private final int visibleCount;

        private Plan(int totalCount, int visibleCount) {
            this.totalCount = totalCount;
            this.visibleCount = visibleCount;
        }

        int totalCount() {
            return totalCount;
        }

        int visibleCount() {
            return visibleCount;
        }

        int hiddenCount() {
            return totalCount - visibleCount;
        }

        boolean hasOverflow() {
            return hiddenCount() > 0;
        }
    }
}