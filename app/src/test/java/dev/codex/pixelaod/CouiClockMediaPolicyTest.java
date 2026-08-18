package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockMediaPolicyTest {
    @Test
    public void onlyPlayingStateIsActiveMediaLikeAntiCouiBuild() {
        assertTrue(CouiClockMediaPolicy.isActivePlaybackState(3));
        assertFalse(CouiClockMediaPolicy.isActivePlaybackState(0));
        assertFalse(CouiClockMediaPolicy.isActivePlaybackState(2));
        assertFalse(CouiClockMediaPolicy.isActivePlaybackState(6));
        assertFalse(CouiClockMediaPolicy.isActivePlaybackState(8));
    }

    @Test
    public void playbackPositionCallbacksDoNotChangeSemanticMediaIdentity() {
        assertTrue(CouiClockMediaPolicy.sameSemanticMedia(true, "pkg", "Title", "Artist",
                true, "pkg", "Title", "Artist"));
        assertFalse(CouiClockMediaPolicy.sameSemanticMedia(true, "pkg", "Title", "Artist",
                false, "pkg", "Title", "Artist"));
        assertFalse(CouiClockMediaPolicy.sameSemanticMedia(true, "pkg", "Title", "Artist",
                true, "pkg", "Other", "Artist"));
        assertFalse(CouiClockMediaPolicy.sameSemanticMedia(true, "pkg", "Title", "Artist",
                true, "other.pkg", "Title", "Artist"));
    }
}
