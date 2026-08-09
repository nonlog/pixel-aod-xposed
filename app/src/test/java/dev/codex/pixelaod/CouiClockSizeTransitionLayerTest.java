package dev.codex.pixelaod;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.widget.ImageView;

import org.junit.Test;

import java.lang.reflect.Field;

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

    private static Field declaredField(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            throw new AssertionError("missing transition field " + name, e);
        }
    }
}
