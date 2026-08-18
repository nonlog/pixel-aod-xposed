package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CouiClockAodEntryOriginPolicyTest {
    @Test
    public void keyguardOrRecentLockscreenEvidenceWins() {
        assertTrue(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                true, false, false, false));
        assertTrue(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                false, true, false, false));
    }

    @Test
    public void visibleInteractiveLockscreenIsTheFallbackWhenKeyguardSignalIsLate() {
        assertTrue(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                false, false, true, true));
    }

    @Test
    public void staleSurfaceAloneDoesNotClassifyAnUnlockedEntryAsLockscreen() {
        assertFalse(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                false, false, true, false));
        assertFalse(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                false, false, false, true));
    }

    @Test
    public void explicitCouiKeyguardStateArmsOriginWhenFrameworkSignalIsLate() {
        assertTrue(CouiClockAodEntryOriginPolicy.isCouiLockscreenPresentation(
                true, CouiClockPluginPresentationMapper.UI_STATE_KEYGUARD, false));
        assertTrue(CouiClockAodEntryOriginPolicy.isFromLockscreen(
                false, false, false, false, true));
    }

    @Test
    public void explicitCouiKeyguardStateDoesNotArmForNonInteractiveAod() {
        assertFalse(CouiClockAodEntryOriginPolicy.isCouiLockscreenPresentation(
                false, CouiClockPluginPresentationMapper.UI_STATE_KEYGUARD, false));
        assertFalse(CouiClockAodEntryOriginPolicy.isCouiLockscreenPresentation(
                true, CouiClockPluginPresentationMapper.UI_STATE_AOD, false));
    }

    @Test
    public void authoritativeUnlockedLatchBeatsLateKeyguardLockedSignal() {
        assertFalse(CouiClockAodEntryOriginPolicy.resolveFromLockscreen(
                Boolean.TRUE,
                true,
                false,
                false,
                false));
    }

    @Test
    public void authoritativeLockscreenLatchBeatsMissingFrameworkSignals() {
        assertTrue(CouiClockAodEntryOriginPolicy.resolveFromLockscreen(
                Boolean.FALSE,
                false,
                false,
                false,
                false));
    }

    @Test
    public void missingAuthoritativeLatchFallsBackToExistingEvidence() {
        assertTrue(CouiClockAodEntryOriginPolicy.resolveFromLockscreen(
                null,
                true,
                false,
                false,
                false));
        assertFalse(CouiClockAodEntryOriginPolicy.resolveFromLockscreen(
                null,
                false,
                false,
                false,
                false));
    }
}
