package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CouiMediaLineTest {
    @Test
    public void preservesTitleAndArtistAsSeparateLines() {
        CouiMediaLine line = CouiMediaLine.of("vampire", "Olivia Rodrigo");

        assertEquals("vampire", line.title);
        assertEquals("Olivia Rodrigo", line.artist);
        assertEquals("vampire - Olivia Rodrigo", line.signature());
        assertFalse(line.isEmpty());
    }

    @Test
    public void removesARepeatedArtist() {
        CouiMediaLine line = CouiMediaLine.of("冬の花", "冬の花");

        assertEquals("", line.artist);
        assertEquals("冬の花", line.signature());
        assertTrue(CouiMediaLine.of("", "artist").isEmpty());
    }
}
