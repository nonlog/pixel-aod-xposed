package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MediaIconRecoveryPolicyTest {
    @Test
    public void retriesOneMissingIconForEachMediaNotification() {
        assertTrue(MediaIconRecoveryPolicy.shouldRetry(true, true, "", "media-key"));
        assertFalse(MediaIconRecoveryPolicy.shouldRetry(true, true, "media-key", "media-key"));
        assertTrue(MediaIconRecoveryPolicy.shouldRetry(true, true, "media-key", "new-media-key"));
    }

    @Test
    public void doesNotRetryWithoutAnIconSourceOrWhenAlreadyVisible() {
        assertFalse(MediaIconRecoveryPolicy.shouldRetry(true, false, "", "media-key"));
        assertFalse(MediaIconRecoveryPolicy.shouldRetry(false, true, "", "media-key"));
    }
}
