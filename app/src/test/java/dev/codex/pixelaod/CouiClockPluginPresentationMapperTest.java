package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public final class CouiClockPluginPresentationMapperTest {
    @Test
    public void lockscreenLargeMapsToLargeWithoutAodFlags() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.map(2, 1, true,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT, mapping.action());
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                mapping.presentation().requestedScene());
        assertFalse(mapping.presentation().dozing());
        assertFalse(mapping.presentation().partialAod());
    }

    @Test
    public void lockscreenSmallMapsToSmall() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.map(2, 0, false,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                mapping.presentation().requestedScene());
        assertFalse(mapping.presentation().dozing());
    }

    @Test
    public void lockscreenImmersedMapsToImmersedWithoutAodScene() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.map(2, 2, true,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPresentationModel.Scene.IMMERSED,
                mapping.presentation().requestedScene());
        assertFalse(mapping.presentation().dozing());
        assertFalse(mapping.presentation().partialAod());
    }

    @Test
    public void forcedLockscreenEntryPresentsKnownVendorSizeEvenWhenUiStateIsTransient() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.forcedLockscreenEntry(1, true,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT, mapping.action());
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                mapping.presentation().visualScene());
        assertFalse(mapping.presentation().dozing());
        assertFalse(mapping.presentation().partialAod());
        assertTrue(mapping.animate());
    }

    @Test
    public void forcedLockscreenEntryHoldsUnknownVendorSize() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.forcedLockscreenEntry(99, false,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPluginPresentationMapper.Action.HOLD, mapping.action());
        assertNull(mapping.presentation());
    }

    @Test
    public void panoramicAodLargeUsesLargeSceneAndNoPartialContent() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.map(5, 1, true,
                        CouiClockPresentationModel.AodContent.none());

        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT, mapping.action());
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                mapping.presentation().requestedScene());
        assertTrue(mapping.presentation().dozing());
        assertFalse(mapping.presentation().partialAod());
        assertFalse(mapping.presentation().showsPartialContent());
    }

    @Test
    public void partialAodCarriesContentAndLetsModelSelectSmallVisualScene() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.map(3, 1, true,
                        CouiClockPresentationModel.AodContent.notifications(2));

        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                mapping.presentation().requestedScene());
        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                mapping.presentation().visualScene());
        assertTrue(mapping.presentation().dozing());
        assertTrue(mapping.presentation().partialAod());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.NOTIFICATIONS,
                mapping.presentation().content().kind());
    }

    @Test
    public void zeroAndMissingStatesHoldButEveryNonZeroStateWithKnownScenePresents() {
        CouiClockPluginPresentationMapper.Mapping zero =
                CouiClockPluginPresentationMapper.map(0, 1, true,
                        CouiClockPresentationModel.AodContent.none());
        assertEquals(CouiClockPluginPresentationMapper.Action.HOLD, zero.action());
        assertNull(zero.presentation());

        CouiClockPluginPresentationMapper.Mapping missingState =
                CouiClockPluginPresentationMapper.map(null, 1, true,
                        CouiClockPresentationModel.AodContent.none());
        assertEquals(CouiClockPluginPresentationMapper.Action.HOLD, missingState.action());
        assertNull(missingState.presentation());

        CouiClockPluginPresentationMapper.Mapping transientUnlocked =
                CouiClockPluginPresentationMapper.map(1, 0, true,
                        CouiClockPresentationModel.AodContent.notifications(2));
        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT,
                transientUnlocked.action());
        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                transientUnlocked.presentation().visualScene());
        assertFalse(transientUnlocked.presentation().dozing());

        CouiClockPluginPresentationMapper.Mapping nonStandardNonZero =
                CouiClockPluginPresentationMapper.map(99, 1, true,
                        CouiClockPresentationModel.AodContent.none());
        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT,
                nonStandardNonZero.action());
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                nonStandardNonZero.presentation().visualScene());

        CouiClockPluginPresentationMapper.Mapping unknownSize =
                CouiClockPluginPresentationMapper.map(2, 99, true,
                        CouiClockPresentationModel.AodContent.none());
        assertEquals(CouiClockPluginPresentationMapper.Action.HOLD, unknownSize.action());
        assertNull(unknownSize.presentation());
    }

    @Test
    public void panoramicAodFallsBackToLastLockscreenSceneLikeCouiReference() {
        CouiClockPluginPresentationMapper.Mapping mapping =
                CouiClockPluginPresentationMapper.mapReference(5, null,
                        CouiClockPresentationModel.Scene.SMALL, true,
                        CouiClockPresentationModel.AodContent.notifications(4));

        assertEquals(CouiClockPluginPresentationMapper.Action.PRESENT, mapping.action());
        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                mapping.presentation().visualScene());
        assertTrue(mapping.presentation().dozing());
        assertFalse(mapping.presentation().partialAod());
        assertEquals(CouiClockPresentationModel.AodContent.Kind.NONE,
                mapping.presentation().content().kind());
    }

    @Test
    public void newModelHasOnlyTheThreeVisualScenes() {
        assertEquals(3, CouiClockPresentationModel.Scene.values().length);
        assertEquals(CouiClockPresentationModel.Scene.LARGE,
                CouiClockPresentationModel.Scene.valueOf("LARGE"));
        assertEquals(CouiClockPresentationModel.Scene.SMALL,
                CouiClockPresentationModel.Scene.valueOf("SMALL"));
        assertEquals(CouiClockPresentationModel.Scene.IMMERSED,
                CouiClockPresentationModel.Scene.valueOf("IMMERSED"));
    }
}
