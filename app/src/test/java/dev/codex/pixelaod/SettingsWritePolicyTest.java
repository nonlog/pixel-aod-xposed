package dev.codex.pixelaod;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsWritePolicyTest {
    @Test public void moduleSystemRootAndShellRemainAllowed() {
        assertTrue(SettingsWritePolicy.isTrustedUid(10456, 10456));
        assertTrue(SettingsWritePolicy.isTrustedUid(0, 10456));
        assertTrue(SettingsWritePolicy.isTrustedUid(1000, 10456));
        assertTrue(SettingsWritePolicy.isTrustedUid(2000, 10456));
    }

    @Test public void unrelatedAppsAndInvalidIdentitiesCannotWrite() {
        assertFalse(SettingsWritePolicy.isTrustedUid(10457, 10456));
        assertFalse(SettingsWritePolicy.isTrustedUid(-1, -1));
        assertFalse(SettingsWritePolicy.isTrustedUid(-1, 10456));
    }

    @Test public void sameAppIdInAnotherUserDoesNotGainAccess() {
        assertFalse(SettingsWritePolicy.isTrustedUid(110456, 10456));
        assertFalse(SettingsWritePolicy.isTrustedUid(102000, 10456));
        assertTrue(SettingsWritePolicy.isTrustedUid(110456, 110456));
    }

    @Test public void finiteNumbersAndStringsArePreserved() {
        assertEquals(280f, SettingsWritePolicy.finiteFloat("280", 520f), 0f);
        assertEquals(320.5f, SettingsWritePolicy.finiteFloat(320.5d, 520f), 0f);
        assertEquals(520f, SettingsWritePolicy.finiteFloat(520, 280f), 0f);
    }

    @Test public void nonFiniteAndOverflowingValuesUseDefault() {
        for (Object value : new Object[]{"NaN", "Infinity", "-Infinity", "1e1000",
                Float.NaN, Float.POSITIVE_INFINITY, Double.MAX_VALUE}) {
            assertEquals(280f, SettingsWritePolicy.finiteFloat(value, 280f), 0f);
        }
    }

    @Test public void malformedValuesUseDefault() {
        assertEquals(280f, SettingsWritePolicy.finiteFloat("not-a-number", 280f), 0f);
        assertEquals(280f, SettingsWritePolicy.finiteFloat(null, 280f), 0f);
    }
}
