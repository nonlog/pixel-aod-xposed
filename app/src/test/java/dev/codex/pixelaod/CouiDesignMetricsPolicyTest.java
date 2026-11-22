package dev.codex.pixelaod;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class CouiDesignMetricsPolicyTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void referenceWidthUsesReferenceDensityScale() {
        assertEquals(538f / 160f,
                CouiDesignMetricsPolicy.pixelsPerDesignDp(1272, 0), EPSILON);
    }

    @Test
    public void cph2573WidthMatchesCoui27DesignScale() {
        float expected = (538f / 160f) * (1440f / 1272f);
        assertEquals(expected,
                CouiDesignMetricsPolicy.pixelsPerDesignDp(1440, 0), EPSILON);
        assertEquals(32f * expected,
                CouiDesignMetricsPolicy.toPixels(32f, 1440, 0), EPSILON);
    }

    @Test
    public void fallbackWidthIsUsedBeforeHostMeasure() {
        assertEquals(CouiDesignMetricsPolicy.pixelsPerDesignDp(1440, 0),
                CouiDesignMetricsPolicy.pixelsPerDesignDp(0, 1440), EPSILON);
    }

    @Test
    public void geometryIgnoresUserDensityOverrideAtFixedWidth() {
        float scale = CouiDesignMetricsPolicy.pixelsPerDesignDp(1440, 0);
        assertEquals(scale, CouiDesignMetricsPolicy.pixelsPerDesignDp(1440, 1440), EPSILON);
    }
}
