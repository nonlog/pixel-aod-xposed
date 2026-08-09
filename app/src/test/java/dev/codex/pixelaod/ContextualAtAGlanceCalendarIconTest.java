package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ContextualAtAGlanceCalendarIconTest {
    @Test
    public void configuredCalendarIconUsesTheSameSizeScaleAndLeadingOffsetOnBothSurfaces() {
        assertEquals(22, ContextualAtAGlanceCalendarIcon.iconSizeDp(18, true));
        assertEquals(24, ContextualAtAGlanceCalendarIcon.iconSizeDp(24, true));
        assertEquals(18, ContextualAtAGlanceCalendarIcon.iconSizeDp(18, false));
        assertEquals(1.25f, ContextualAtAGlanceCalendarIcon.iconScale(true), 0.001f);
        assertEquals(1f, ContextualAtAGlanceCalendarIcon.iconScale(false), 0.001f);
        assertEquals(6, ContextualAtAGlanceCalendarIcon.leadingOffsetDp(true));
        assertEquals(0, ContextualAtAGlanceCalendarIcon.leadingOffsetDp(false));
    }
}
