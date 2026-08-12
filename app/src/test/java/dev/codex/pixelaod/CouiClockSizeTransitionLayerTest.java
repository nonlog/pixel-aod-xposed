package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.view.ViewGroup;
import android.widget.ImageView;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class CouiClockSizeTransitionLayerTest {
    @Test
    public void isolatesForecastSubviewsEvenWhenTheirTextGeometryIsStable() {
        // A real contextual LinearLayout can be laid out again by the host after the size
        // transaction has captured its target.  That late layout owns a second Y movement, so
        // the transition must always render an isolated clone instead of translating the live
        // row in place.
        assertFalse(CouiClockSizeTransitionLayer.keepsContextualSubviewsAtNativeGeometry(
                42f, 42.4f));
        assertFalse(CouiClockSizeTransitionLayer.keepsContextualSubviewsAtNativeGeometry(
                42f, 43f));
    }

    @Test
    public void retargetsOnlyInformationClonesWhoseVisibleTextDidNotChange() {
        assertTrue(CouiClockSizeTransitionLayer.shouldRetargetInformationCloneText(
                "33°", "33°"));
        assertFalse(CouiClockSizeTransitionLayer.shouldRetargetInformationCloneText(
                "33°", "34°"));
    }

    @Test
    public void keepsCurrentWeatherSourceCellsForTheEntireSizeTransition() {
        // Weather temperature glyphs are individually centred in FixedAdvanceSpan cells.  Using
        // target cells before the motion starts shifts the 3/1/degree ink inside an otherwise
        // correctly positioned text clone, so this one track deliberately retains its source
        // cells until the live target TextView is restored.
        assertFalse(CouiClockSizeTransitionLayer.shouldRetargetInformationCloneText(
                "31°", "31°", true));
    }

    @Test
    public void givesCompoundIconsTheirOwnTransitionTracks() {
        // A single widened TextView cannot preserve both a START-aligned compound icon and a
        // separately centred text corridor.  Each icon must therefore retain its own screen
        // centre all the way to the live-row hand-off.
        assertEquals(ImageView.class, declaredField(CouiClockSizeTransitionLayer.class,
                "weatherIconView").getType());
        assertEquals(ImageView.class, declaredField(CouiClockSizeTransitionLayer.class,
                "contextualIconView").getType());
    }

    @Test
    public void primesTypefaceBeforeExactMeasureAndFirstFramePlacement() {
        // setTypeface() can invalidate TextView measurement.  The overlay must not become
        // drawable until the final typeface has been followed by an EXACTLY measure/layout and
        // the first frame has been positioned with those resulting glyph bounds.
        List<String> calls = new ArrayList<>();
        CouiClockSizeTransitionLayer.runOverlayPrimeSteps(
                () -> calls.add("typeface"),
                () -> calls.add("measure-layout"),
                () -> calls.add("first-frame"));

        assertEquals(List.of("typeface", "measure-layout", "first-frame"), calls);
    }

    @Test
    public void preparedSourceFreezesDigitCloneSlotGeometryUntilTransactionEnds() {
        CouiClockSizeTransitionLayer.GlyphSlotGeometryOwnership ownership =
                new CouiClockSizeTransitionLayer.GlyphSlotGeometryOwnership();

        assertTrue(ownership.mayConfigureInitialSlots());
        ownership.commitPrepared();
        assertFalse(ownership.mayConfigureInitialSlots());

        ownership.reset();
        assertTrue(ownership.mayConfigureInitialSlots());
    }

    @Test
    public void preparedSourceOwnsDrawBeforeLiveTargetMayMutate() {
        CouiClockSizeTransitionLayer.SourceFrameOwnership ownership =
                new CouiClockSizeTransitionLayer.SourceFrameOwnership();

        assertFalse(ownership.mayMutateOrCaptureTarget());
        ownership.acquirePreparedSource();
        assertTrue(ownership.mayMutateOrCaptureTarget());
        assertFalse(ownership.keepsSourceOwnershipAtFrameZero());
    }

    @Test
    public void targetFrameZeroKeepsPreparedSourceDrawableUntilTransitionFinishes() {
        CouiClockSizeTransitionLayer.SourceFrameOwnership ownership =
                new CouiClockSizeTransitionLayer.SourceFrameOwnership();

        ownership.acquirePreparedSource();
        ownership.commitTargetFrameZero();

        assertTrue(ownership.keepsSourceOwnershipAtFrameZero());
        ownership.reset();
        assertFalse(ownership.mayMutateOrCaptureTarget());
    }

    @Test
    public void contextualClonePreservesChildAlphaInsideVisibleRow() {
        assertEquals(0.72f, CouiClockSizeTransitionLayer.composedVisualAlpha(
                1f, 0.72f, false), 0.001f);
        // Direct date/weather TextViews own their own alpha; do not square it.
        assertEquals(0.72f, CouiClockSizeTransitionLayer.composedVisualAlpha(
                0.72f, 0.72f, true), 0.001f);
    }

    @Test
    public void oppositeEndpointReversesActiveSizePathInsteadOfRestartingIt() {
        // LARGE(false) -> SMALL(true) is currently moving toward SMALL.
        assertTrue(CouiClockSizeTransitionLayer.shouldReverseActivePath(
                false, true, false, false));
        assertFalse(CouiClockSizeTransitionLayer.shouldReverseActivePath(
                false, true, false, true));
        // Once reversed toward LARGE, another SMALL request reverses the same animator again.
        assertTrue(CouiClockSizeTransitionLayer.shouldReverseActivePath(
                false, true, true, true));
    }

    @Test
    public void largeTargetRejectsNewTextInsideOldCompactLayoutBox() {
        int rootWidth = 1440;
        // applyClockMode(false) has already changed LayoutParams to MATCH_PARENT, but the current
        // traversal can still report the old ~compact width before the deferred layout pass.
        assertFalse(CouiClockSizeTransitionLayer.targetClockGeometryReady(
                false, rootWidth, 430, 2, ViewGroup.LayoutParams.MATCH_PARENT, false));
        assertTrue(CouiClockSizeTransitionLayer.targetClockGeometryReady(
                false, rootWidth, 1440, 2, ViewGroup.LayoutParams.MATCH_PARENT, false));
    }

    @Test
    public void targetGeometryWaitsForRequestedLayoutAndCorrectLineMode() {
        assertFalse(CouiClockSizeTransitionLayer.targetClockGeometryReady(
                true, 1440, 430, 1, ViewGroup.LayoutParams.WRAP_CONTENT, true));
        assertFalse(CouiClockSizeTransitionLayer.targetClockGeometryReady(
                true, 1440, 430, 2, ViewGroup.LayoutParams.WRAP_CONTENT, false));
        assertTrue(CouiClockSizeTransitionLayer.targetClockGeometryReady(
                true, 1440, 430, 1, ViewGroup.LayoutParams.WRAP_CONTENT, false));
    }

    private static Field declaredField(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("missing transition field " + name, e);
        }
    }
}
