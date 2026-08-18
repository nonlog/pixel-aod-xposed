package dev.codex.pixelaod;

/**
 * Pure priority policy for the semantic content that drives a partial AOD surface.
 *
 * <p>Media remains media even when notification icons accompany it. A media-only AOD therefore
 * has MEDIA content with zero notification icons; it does not use a custom large-clock override.</p>
 */
final class CouiClockAodContentPolicy {
    private CouiClockAodContentPolicy() {
    }

    static CouiClockPresentationModel.AodContent fromSemanticState(
            boolean hasDisplayableMedia, int notificationIconCount) {
        int count = Math.max(0, notificationIconCount);
        if (hasDisplayableMedia) {
            return CouiClockPresentationModel.AodContent.media(count);
        }
        if (count > 0) {
            return CouiClockPresentationModel.AodContent.notifications(count);
        }
        return CouiClockPresentationModel.AodContent.none();
    }
}
