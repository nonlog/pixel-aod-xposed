package dev.codex.pixelaod;

import java.util.LinkedHashSet;

final class NotificationIconDisplayPlan {
    private final int totalIconCount;
    private final int visibleIconCount;

    private NotificationIconDisplayPlan(int totalIconCount, int visibleIconCount) {
        this.totalIconCount = totalIconCount;
        this.visibleIconCount = visibleIconCount;
    }

    static NotificationIconDisplayPlan fromEligibleIconKeys(
            Iterable<String> iconKeys, int maxVisibleIcons) {
        LinkedHashSet<String> uniqueKeys = new LinkedHashSet<>();
        if (iconKeys != null) {
            for (String iconKey : iconKeys) {
                if (iconKey != null) {
                    uniqueKeys.add(iconKey);
                }
            }
        }
        int total = uniqueKeys.size();
        int visible = Math.min(total, Math.max(0, maxVisibleIcons));
        return new NotificationIconDisplayPlan(total, visible);
    }

    int totalIconCount() {
        return totalIconCount;
    }

    int visibleIconCount() {
        return visibleIconCount;
    }

    int overflowCount() {
        return totalIconCount - visibleIconCount;
    }

    boolean hasOverflow() {
        return overflowCount() > 0;
    }
}
