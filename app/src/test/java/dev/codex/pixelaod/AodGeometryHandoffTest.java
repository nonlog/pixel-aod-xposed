package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Regression coverage for the explicit compact-to-large size morph geometry. */
public final class AodGeometryHandoffTest {
    @Test
    public void initialSizeMorphTranslationKeepsTheClockAtItsSourceScreenCoordinate() {
        AodGeometryHandoff.Offset offset = AodGeometryHandoff.offsetToPreserveScreenPosition(
                486f, 512f, 503f, 538f);

        assertTrue(offset.shouldAnimate());
        assertEquals(486f, 503f + offset.x, 0.001f);
        assertEquals(512f, 538f + offset.y, 0.001f);
    }

    @Test
    public void eachInformationLineUsesItsOwnCoordinateOffset() {
        AodGeometryHandoff.Offset dateOffset = AodGeometryHandoff.offsetToPreserveScreenPosition(
                118f, 710f, 132f, 698f);
        AodGeometryHandoff.Offset weatherOffset = AodGeometryHandoff.offsetToPreserveScreenPosition(
                118f, 758f, 132f, 746f);

        assertEquals(dateOffset.x, weatherOffset.x, 0.001f);
        assertEquals(dateOffset.y, weatherOffset.y, 0.001f);
        assertEquals(118f, 132f + dateOffset.x, 0.001f);
        assertEquals(710f, 698f + dateOffset.y, 0.001f);
    }

    @Test
    public void doesNotStartAnAnimationForMatchingCoordinates() {
        AodGeometryHandoff.Offset offset = AodGeometryHandoff.offsetToPreserveScreenPosition(
                300f, 400f, 300f, 400f);

        assertFalse(offset.shouldAnimate());
        assertEquals(0f, offset.x, 0.001f);
        assertEquals(0f, offset.y, 0.001f);
    }
}
