package dev.codex.pixelaod;

import android.content.Context;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ClockRendererStartupRouterTest {
    @Test
    public void legacyModeInstallsOnlyTheLegacyPrimaryOwner() {
        Probe probe = new Probe();

        ClockRendererStartupRouter.install(
                ClockRendererPolicy.parse(ClockRendererPolicy.VALUE_LEGACY),
                null,
                null,
                probe);

        assertEquals(1, probe.legacyInstallations);
        assertEquals(0, probe.couiInstallations);
    }

    @Test
    public void couiModeInstallsOnlyTheCouiPrimaryOwner() {
        Probe probe = new Probe();

        ClockRendererStartupRouter.install(
                ClockRendererPolicy.parse(ClockRendererPolicy.VALUE_COUI_PORT),
                null,
                null,
                probe);

        assertEquals(0, probe.legacyInstallations);
        assertEquals(1, probe.couiInstallations);
    }

    @Test
    public void nullPolicyUsesTheCutoverCouiOwner() {
        Probe probe = new Probe();

        ClockRendererStartupRouter.install(null, null, null, probe);

        assertEquals(0, probe.legacyInstallations);
        assertEquals(1, probe.couiInstallations);
    }

    private static final class Probe implements ClockRendererStartupRouter.Installer {
        int legacyInstallations;
        int couiInstallations;

        @Override
        public void installLegacy(Context context, ClassLoader classLoader) {
            legacyInstallations++;
        }

        @Override
        public void installCoui(Context context, ClassLoader classLoader) {
            couiInstallations++;
        }
    }
}
