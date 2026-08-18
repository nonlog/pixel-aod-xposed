package dev.codex.pixelaod;

/** Pure COUI host policy for the requested five-icon plus-x notification presentation. */
final class CouiClockNotificationOverflowPolicy {
    static final int MAX_VISIBLE_ICONS = 5;

    private CouiClockNotificationOverflowPolicy() {
    }

    static Plan forCount(int totalIconCount) {
        int total = Math.max(0, totalIconCount);
        int visible = Math.min(MAX_VISIBLE_ICONS, total);
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

        String overflowText() {
            return hasOverflow() ? "+" + hiddenCount() : "";
        }
    }
}
