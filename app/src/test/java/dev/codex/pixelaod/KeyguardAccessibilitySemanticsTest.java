package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class KeyguardAccessibilitySemanticsTest {
    @Test
    public void groupsOnlyVisibleSemanticText() {
        assertEquals("Wed, Aug 26, 32°",
                KeyguardAccessibilitySemantics.join("Wed, Aug 26", "", "32°"));
        assertEquals("Song, Artist",
                KeyguardAccessibilitySemantics.join("Song", null, "Artist"));
        assertEquals("", KeyguardAccessibilitySemantics.join("", "  ", null));
    }
}
