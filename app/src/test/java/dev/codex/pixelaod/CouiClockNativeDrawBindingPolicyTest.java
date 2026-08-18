package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression coverage for the native draw hook's host-relative container guard. */
public final class CouiClockNativeDrawBindingPolicyTest {
    @Test
    public void rootOrAncestorContainingHostIsRejected() {
        assertFalse(CouiClockNativeDrawBindingPolicy.mayBind(true));
    }

    @Test
    public void hostDescendantContainerIsRejected() {
        assertFalse(CouiClockNativeDrawBindingPolicy.mayBind(true));
    }

    @Test
    public void safeSiblingNativeContainerIsEligible() {
        assertTrue(CouiClockNativeDrawBindingPolicy.mayBind(false));
    }
}
