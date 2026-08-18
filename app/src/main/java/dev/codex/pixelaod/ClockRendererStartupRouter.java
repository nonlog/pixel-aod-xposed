package dev.codex.pixelaod;

import android.content.Context;

/**
 * Selects exactly one primary clock installer for a SystemUI startup.
 *
 * <p>The router deliberately has no fallback or validation handoff. The caller supplies the
 * startup-captured policy once, and exactly one installer callback is invoked.</p>
 */
final class ClockRendererStartupRouter {
    interface Installer {
        void installLegacy(Context context, ClassLoader classLoader);

        void installCoui(Context context, ClassLoader classLoader);
    }

    private ClockRendererStartupRouter() {
    }

    static void install(ClockRendererPolicy policy, Context context, ClassLoader classLoader,
            Installer installer) {
        if (installer == null) {
            throw new IllegalArgumentException("installer must not be null");
        }
        ClockRendererPolicy normalized = policy == null
                ? ClockRendererPolicy.parse(null) : policy;
        if (normalized.useCouiOwner()) {
            installer.installCoui(context, classLoader);
        } else {
            installer.installLegacy(context, classLoader);
        }
    }
}
