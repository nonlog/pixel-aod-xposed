package dev.codex.pixelaod;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SelectedUserScopeTest {
    @Test
    public void scopedAuthorityAddsRequestedUser() {
        assertEquals("10@dev.codex.pixelaod.settings",
                SelectedUserScope.scopedAuthority("dev.codex.pixelaod.settings", 10));
    }

    @Test
    public void scopedAuthorityReplacesExistingUserPrefix() {
        assertEquals("11@dev.codex.pixelaod.settings",
                SelectedUserScope.scopedAuthority("0@dev.codex.pixelaod.settings", 11));
    }

    @Test
    public void scopedAuthorityClampsNegativeUserToOwner() {
        assertEquals("0@dev.codex.pixelaod.settings",
                SelectedUserScope.scopedAuthority("dev.codex.pixelaod.settings", -1));
    }
}
