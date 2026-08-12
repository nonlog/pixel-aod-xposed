package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.widget.TextView;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class CouiClockSizeTransitionMathTest {
    @Test
    public void eachGlyphMovesTowardItsOwnTargetInsteadOfScalingAsOneBlock() {
        CouiClockSizeTransitionMath.Element from =
                new CouiClockSizeTransitionMath.Element(100f, 200f, 56f, 1f);
        CouiClockSizeTransitionMath.Element topTarget =
                new CouiClockSizeTransitionMath.Element(300f, 400f, 150f, 1f);
        CouiClockSizeTransitionMath.Element bottomTarget =
                new CouiClockSizeTransitionMath.Element(300f, 700f, 150f, 1f);

        CouiClockSizeTransitionMath.Frame top =
                CouiClockSizeTransitionMath.frame(from, topTarget, 0.5f);
        CouiClockSizeTransitionMath.Frame bottom =
                CouiClockSizeTransitionMath.frame(from, bottomTarget, 0.5f);

        assertEquals(200f, top.centerX, 0.001f);
        assertEquals(300f, top.centerY, 0.001f);
        assertEquals(450f, bottom.centerY, 0.001f);
        assertEquals(103f / 56f, top.scaleFromSource, 0.001f);
    }

    @Test
    public void allFourPaintedDigitCentersFollowTheirOwnSourceAndTargetPaths() {
        CouiClockSizeTransitionMath.Element[] source = {
                new CouiClockSizeTransitionMath.Element(120f, 200f, 150f, 1f),
                new CouiClockSizeTransitionMath.Element(280f, 200f, 150f, 1f),
                new CouiClockSizeTransitionMath.Element(120f, 430f, 150f, 1f),
                new CouiClockSizeTransitionMath.Element(280f, 430f, 150f, 1f)
        };
        CouiClockSizeTransitionMath.Element[] target = {
                new CouiClockSizeTransitionMath.Element(420f, 680f, 56f, 1f),
                new CouiClockSizeTransitionMath.Element(520f, 680f, 56f, 1f),
                new CouiClockSizeTransitionMath.Element(420f, 760f, 56f, 1f),
                new CouiClockSizeTransitionMath.Element(520f, 760f, 56f, 1f)
        };
        float[] expectedX = {270f, 400f, 270f, 400f};
        float[] expectedY = {440f, 440f, 595f, 595f};

        for (int index = 0; index < source.length; index++) {
            CouiClockSizeTransitionMath.Frame frame = CouiClockSizeTransitionMath.frame(
                    source[index], target[index], 0.5f);
            assertEquals(expectedX[index], frame.centerX, 0.001f);
            assertEquals(expectedY[index], frame.centerY, 0.001f);
        }
    }

    @Test
    public void colonFadesOutAtTheStartOfCompactToLarge() {
        assertEquals(1f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0f), 0.001f);
        assertEquals(0.5f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.11f), 0.001f);
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.22f), 0.001f);
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(1f, 0f, 0.8f), 0.001f);
    }

    @Test
    public void colonWaitsBeforeAppearingDuringLargeToCompact() {
        assertEquals(0f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.51f), 0.001f);
        assertEquals(0.5f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.63f), 0.001f);
        assertEquals(1f, CouiClockSizeTransitionMath.colonAlpha(0f, 1f, 0.74f), 0.001f);
    }

    @Test
    public void runtimeColonPathAppliesEasingInsideTheCouiTimingWindow() {
        assertEquals(0.25f, CouiClockSizeTransitionMath.colonAlpha(
                0f, 1f, 0.63f, progress -> progress * progress), 0.001f);
    }

    @Test
    public void redirectedMotionStartsExactlyAtCurrentFrameAndGetsFreshEaseOut() {
        CouiClockSizeTransitionMath.Easing easeOut =
                progress -> 1f - ((1f - progress) * (1f - progress));

        assertEquals(0.92f, CouiClockSizeTransitionMath.redirectedMotionProgress(
                0.80f, 0.80f, 0f, 0.92f, 0f, easeOut), 0.001f);
        assertEquals(0f, CouiClockSizeTransitionMath.redirectedMotionProgress(
                0f, 0.80f, 0f, 0.92f, 0f, easeOut), 0.001f);
        // Ten percent into the reversed driver has already covered 19% of the visual segment.
        // Evaluating the original ease-out backwards would instead remain in its slow tail.
        assertEquals(0.7452f, CouiClockSizeTransitionMath.redirectedMotionProgress(
                0.72f, 0.80f, 0f, 0.92f, 0f, easeOut), 0.001f);
    }

    @Test
    public void paintedCenterUsesActualInkBoundsInsteadOfCellTracking() {
        assertEquals(120f, CouiClockSizeTransitionMath.paintedBoundsCenter(100f, 140f),
                0.001f);
        assertFalse(Math.abs(117f - CouiClockSizeTransitionMath.paintedBoundsCenter(
                100f, 140f)) < 0.001f);
        assertEquals(134f, CouiClockSizeTransitionMath.paintedBoundsCenter(117f, 151f),
                0.001f);
    }

    @Test
    public void largeTwoLineGlyphUsesItsPaintedBoundsInsteadOfTheLineBoxCenter() {
        float lineBoxCenter = 80f;
        float paintedLeft = 106f;
        float paintedRight = 130f;

        assertEquals(118f, CouiClockSizeTransitionMath.paintedBoundsCenter(
                paintedLeft, paintedRight), 0.001f);
        assertFalse(Math.abs(lineBoxCenter - CouiClockSizeTransitionMath.paintedBoundsCenter(
                paintedLeft, paintedRight)) < 0.001f);
    }

    @Test
    public void replacementSpanPaintedCenterIncludesItsWeightOffset() {
        assertEquals(103f, CouiClockSizeTransitionMath.replacementSpanPaintOrigin(
                100f, 40f, 34f), 0.001f);
        assertEquals(113f, CouiClockSizeTransitionMath.paintedGlyphCenter(
                100f, 40f, 34f, 0f, 20f), 0.001f);
        assertEquals(61f, CouiClockSizeTransitionMath.paintedBaselineCenter(
                118f, -111f, -3f), 0.001f);
    }

    @Test
    public void clockDigitHorizontalOwnerIsFixedAdvanceCellNotChangingInkBounds() {
        float cellCenter = CouiClockSizeTransitionMath.fixedGlyphCellCenter(100f, 40f);
        assertEquals(120f, cellCenter, 0.001f);

        // The same fixed cell can contain very different variable-font ink. Neither shape is
        // allowed to move the slot that COUI-style per-digit motion interpolates.
        float narrowInkCenter = CouiClockSizeTransitionMath.paintedGlyphCenter(
                100f, 40f, 28f, 1f, 17f);
        float wideInkCenter = CouiClockSizeTransitionMath.paintedGlyphCenter(
                100f, 40f, 38f, -1f, 31f);
        assertFalse(Math.abs(narrowInkCenter - wideInkCenter) < 0.001f);
        assertEquals(120f, CouiClockSizeTransitionMath.fixedGlyphCellCenter(
                100f, 40f), 0.001f);
    }

    @Test
    public void shortInformationBoxUsesTextViewsClampedVerticalBaseline() {
        // A 50 px font line cannot be vertically centred inside a 30 px overlay box. TextView
        // pins the line to the top, producing baseline 40; the old unconditional centre formula
        // produced baseline 30 and rendered the weather temperature 10 px too low.
        assertEquals(40f, CouiClockSizeTransitionMath.centeredTextBaseline(
                30f, -40f, 10f), 0.001f);
        assertEquals(50f, CouiClockSizeTransitionMath.centeredTextBaseline(
                70f, -40f, 10f), 0.001f);
    }

    @Test
    public void asymmetricDigitAndLeadingWeatherIconStayOnTheirVisualCentersAfterWeightChange() {
        // The narrow painted "1" sits left of the cell center. The clone must pivot at its
        // painted center, not at the 100 px overlay box center, before scaling it.
        float digitVisualOffset = CouiClockSizeTransitionMath.visualContentOffset(
                100f, 60f, 2f, 32f);
        assertEquals(37f, digitVisualOffset, 0.001f);
        assertEquals(363f, CouiClockSizeTransitionMath.positionForVisualCenter(
                400f, digitVisualOffset), 0.001f);

        // A leading weather glyph and its text form one visual union. A heavier target font
        // changes the text advance but must still land at exactly the requested center.
        float weatherVisualOffset = CouiClockSizeTransitionMath.visualContentOffset(
                180f, 136f, 0f, 132f);
        assertEquals(88f, weatherVisualOffset, 0.001f);
        assertEquals(512f, CouiClockSizeTransitionMath.positionForVisualCenter(
                600f, weatherVisualOffset), 0.001f);
    }

    @Test
    public void informationRowsInterpolateTextMetricsWithoutAffineDrawableScaling() {
        CouiClockSizeTransitionMath.InfoFrame halfway =
                CouiClockSizeTransitionMath.informationFrame(
                        new CouiClockSizeTransitionMath.Element(100f, 300f, 80f, 1f),
                        new CouiClockSizeTransitionMath.Element(200f, 100f, 64f, 1f),
                        0.5f);

        assertEquals(150f, halfway.centerX, 0.001f);
        assertEquals(200f, halfway.centerY, 0.001f);
        assertEquals(72f, halfway.textSizePx, 0.001f);
        // The weather icon is 15 dp in both scenes. It must remain 15 dp at every frame,
        // rather than inheriting the 0.9x TextView scale used by the old renderer.
        assertEquals(60, CouiClockSizeTransitionMath.interpolatedDimension(60, 60, 0.5f));
        assertEquals(54, CouiClockSizeTransitionMath.interpolatedDimension(60, 48, 0.5f));
    }

    @Test
    public void fixedCellWeatherTrackScalesOneStableSourceCorridor() {
        CouiClockSizeTransitionMath.InfoFrame halfway =
                CouiClockSizeTransitionMath.informationFrame(
                        new CouiClockSizeTransitionMath.Element(100f, 300f, 20f, 1f),
                        new CouiClockSizeTransitionMath.Element(200f, 100f, 16f, 1f),
                        0.5f);

        // The 20 px source cells remain intact: their complete corridor is transformed by 0.9,
        // rather than rebuilding and rounding each of 3 / 1 / degree at an 18 px text size.
        assertEquals(0.9f, CouiClockSizeTransitionMath.fixedCellTextScale(20f,
                halfway.textSizePx), 0.001f);
        assertEquals(0.8f, CouiClockSizeTransitionMath.fixedCellTextScale(20f, 16f), 0.001f);
    }

    @Test
    public void compiledTransitionLayerUsesPlatformTextViewClones() {
        Class<?> layer = CouiClockSizeTransitionLayer.class;

        assertEquals(TextView[].class, declaredField(layer, "digitViews").getType());
        assertEquals(TextView.class, declaredField(layer, "colonView").getType());
        assertEquals(TextView.class, declaredField(layer, "dateView").getType());
        assertEquals(TextView.class, declaredField(layer, "weatherView").getType());
        assertEquals(TextView.class, declaredMethod(layer, "createClockGlyph").getReturnType());
        assertEquals(TextView.class,
                declaredMethod(layer, "createInformationClone").getReturnType());

        for (Class<?> nested : layer.getDeclaredClasses()) {
            assertFalse("custom glyph renderer must not be compiled: " + nested.getName(),
                    "GlyphTextView".equals(nested.getSimpleName()));
            assertFalse("nested info renderer must not be compiled: " + nested.getName(),
                    "InfoRowView".equals(nested.getSimpleName()));
        }
    }

    private static Field declaredField(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("missing transition field " + name, e);
        }
    }

    private static Method declaredMethod(Class<?> owner, String name) {
        for (Method method : owner.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                return method;
            }
        }
        throw new AssertionError("missing transition method " + name);
    }

    @Test
    public void onlyLargeSmallChangesNeedTheGlyphTransaction() {
        assertTrue(CouiClockSizeTransitionMath.isSizeChange(false, true));
        assertTrue(CouiClockSizeTransitionMath.isSizeChange(true, false));
        assertFalse(CouiClockSizeTransitionMath.isSizeChange(false, false));
        assertFalse(CouiClockSizeTransitionMath.isSizeChange(true, true));
    }

    @Test
    public void sceneSizeRequestIsRejectedWhenActualSourceAlreadyMatchesTarget() {
        assertFalse(CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                true, false, false));
        assertFalse(CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                true, true, true));
    }

    @Test
    public void actualSizeDifferenceStillRunsTheRequestedTransaction() {
        assertTrue(CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                true, false, true));
        assertTrue(CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                true, true, false));
        assertFalse(CouiClockSizeTransitionMath.shouldRunActualSizeTransition(
                false, false, true));
    }

    @Test
    public void sameSurfaceLargeSmallChangesUseTheGlyphTransaction() {
        assertTrue(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                true, true, false, true));
        assertTrue(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                false, false, true, false));
    }

    @Test
    public void crossSurfaceChangesDoNotUseTheGlyphTransaction() {
        assertFalse(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                true, false, false, true));
        assertFalse(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                false, true, true, false));
        assertFalse(CouiClockSizeTransitionMath.isSameSurfaceSizeChange(
                true, false, false, false));
    }

    @Test
    public void stalePresentationMorphCannotReapplyAfterSurfaceReuse() {
        assertTrue(CouiClockSizeTransitionMath.isPresentationMorphCurrent(7L, 7L));
        assertFalse(CouiClockSizeTransitionMath.isPresentationMorphCurrent(6L, 7L));
    }
}
