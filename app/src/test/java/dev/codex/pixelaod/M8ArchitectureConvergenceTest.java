package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public final class M8ArchitectureConvergenceTest {
    @Test
    public void sharedPresentationFacadesArePackaged() throws Exception {
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodTypography"));
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodContentState"));
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodRuntimeState"));
    }

    @Test
    public void domainHookInstallersArePackaged() throws Exception {
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodLifecycleHookInstaller"));
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodNotificationHookInstaller"));
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodSurfaceHookInstaller"));
        assertNotNull(Class.forName("dev.codex.pixelaod.PixelAodUdfpsHookInstaller"));
    }
}
