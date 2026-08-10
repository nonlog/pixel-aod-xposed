package dev.codex.pixelaod;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class NotificationCapsuleIconPolicyTest {
    @Test
    public void finalStatusBarDrawableCacheIsKeyedAndRemovalClearsIt() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object finalDrawable = new Object();

        policy.cacheFinalDrawable("notification-key", finalDrawable);

        assertSame(finalDrawable, policy.finalDrawableFor("notification-key"));
        assertNull(policy.finalDrawableFor("other-key"));
        policy.removeFinalDrawable("notification-key");
        assertNull(policy.finalDrawableFor("notification-key"));
    }

    @Test
    public void validCaptureWithoutALiveCardOnlyUpdatesTheFinalDrawableCache() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object drawable = new Object();

        assertFalse(policy.acceptFinalDrawableCapture(
                policy.beginFinalDrawableCapture(new Object(), "key"), drawable));

        assertSame(drawable, policy.finalDrawableFor("key"));
        assertTrue(policy.takeQueuedLateCapsuleBindings().isEmpty());
    }

    @Test
    public void recycledIconViewRejectsItsOlderDeferredCapture() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object view = new Object();
        NotificationCapsuleIconPolicy.CaptureToken oldToken =
                policy.beginFinalDrawableCapture(view, "old-key");
        NotificationCapsuleIconPolicy.CaptureToken currentToken =
                policy.beginFinalDrawableCapture(view, "current-key");

        assertFalse(policy.acceptsFinalDrawableCapture(oldToken));
        assertTrue(policy.acceptsFinalDrawableCapture(currentToken));
    }

    @Test
    public void captureTokenUsesWeakViewOwnershipAndRejectsAClearedReference() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object iconView = new Object();
        NotificationCapsuleIconPolicy.CaptureToken token =
                policy.beginFinalDrawableCapture(iconView, "key");

        assertSame(iconView, token.iconView.get());
        token.iconView.clear();

        assertFalse(policy.acceptsFinalDrawableCapture(token));
    }

    @Test
    public void twoViewsForTheSameKeyRejectReverseCompletionOfTheOlderView() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        NotificationCapsuleIconPolicy.CaptureToken first =
                policy.beginFinalDrawableCapture(new Object(), "same-key");
        NotificationCapsuleIconPolicy.CaptureToken second =
                policy.beginFinalDrawableCapture(new Object(), "same-key");

        assertFalse(policy.acceptsFinalDrawableCapture(first));
        assertTrue(policy.acceptsFinalDrawableCapture(second));
        assertFalse(policy.acceptFinalDrawableCapture(first, new Object()));
        assertNull(policy.finalDrawableFor("same-key"));
        assertFalse(policy.acceptFinalDrawableCapture(second, new Object()));
    }

    @Test
    public void removalInvalidatesPendingCaptureAndQueuedCapsuleUpdate() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object target = new Object();
        NotificationCapsuleIconPolicy.CapsuleBindingToken binding =
                policy.beginCapsuleIconBinding(target, "removed-key");
        policy.noteCapsuleCacheMiss(binding);
        NotificationCapsuleIconPolicy.CaptureToken capture =
                policy.beginFinalDrawableCapture(new Object(), "removed-key");

        assertTrue(policy.acceptFinalDrawableCapture(capture, new Object()));
        policy.removeFinalDrawable("removed-key");

        assertFalse(policy.acceptsFinalDrawableCapture(capture));
        assertFalse(policy.acceptsCapsuleBinding(binding));
        assertTrue(policy.takeQueuedLateCapsuleBindings().isEmpty());
    }

    @Test
    public void newCapsuleDataSupersedesAnOlderQueuedLateUpdate() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        NotificationCapsuleIconPolicy.CapsuleBindingToken oldBinding =
                policy.beginCapsuleIconBinding(new Object(), "key");
        policy.noteCapsuleCacheMiss(oldBinding);
        assertTrue(policy.acceptFinalDrawableCapture(
                policy.beginFinalDrawableCapture(new Object(), "key"), new Object()));

        NotificationCapsuleIconPolicy.CapsuleBindingToken currentBinding =
                policy.beginCapsuleIconBinding(new Object(), "key");
        policy.noteCapsuleCacheMiss(currentBinding);
        assertFalse(policy.acceptsCapsuleBinding(oldBinding));
        assertTrue(policy.takeQueuedLateCapsuleBindings().isEmpty());

        assertTrue(policy.acceptFinalDrawableCapture(
                policy.beginFinalDrawableCapture(new Object(), "key"), new Object()));
        List<NotificationCapsuleIconPolicy.CapsuleBindingToken> queued =
                policy.takeQueuedLateCapsuleBindings();
        assertEquals(1, queued.size());
        assertSame(currentBinding, queued.get(0));
    }

    @Test
    public void recycledCapsuleIconViewCannotReceiveTheOldKeysQueuedLateUpdate() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object sharedIconView = new Object();
        NotificationCapsuleIconPolicy.CapsuleBindingToken bindingA =
                policy.beginCapsuleIconBinding(sharedIconView, "A");
        policy.noteCapsuleCacheMiss(bindingA);
        assertTrue(policy.acceptFinalDrawableCapture(
                policy.beginFinalDrawableCapture(new Object(), "A"), new Object()));

        NotificationCapsuleIconPolicy.CapsuleBindingToken bindingB =
                policy.beginCapsuleIconBinding(sharedIconView, "B");
        policy.noteCapsuleCacheMiss(bindingB);

        assertFalse(policy.acceptsCapsuleBinding(bindingA));
        assertTrue(policy.acceptsCapsuleBinding(bindingB));
        assertTrue(policy.takeQueuedLateCapsuleBindings().isEmpty());
    }

    @Test
    public void aDirectUpdateFailureLeavesThePreviousOosSnapshotUntouched() {
        NotificationCapsuleIconPolicy policy = new NotificationCapsuleIconPolicy();
        Object vendorSnapshot = new Object();
        NotificationCapsuleIconPolicy.CapsuleBindingToken binding =
                policy.beginCapsuleIconBinding(vendorSnapshot, "key");
        policy.noteCapsuleCacheMiss(binding);
        Object captured = new Object();
        assertTrue(policy.acceptFinalDrawableCapture(
                policy.beginFinalDrawableCapture(new Object(), "key"), captured));

        // The policy only hands out a validated direct-update token. A copy/set failure is
        // handled by the caller before it touches the live OOS ImageView or its current drawable.
        NotificationCapsuleIconPolicy.CapsuleBindingToken queued =
                policy.takeQueuedLateCapsuleBindings().get(0);
        assertSame(vendorSnapshot, queued.iconView.get());
        assertSame(captured, policy.finalDrawableFor("key"));
        assertTrue(policy.acceptsCapsuleBinding(queued));
    }
}
