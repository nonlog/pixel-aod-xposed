package dev.codex.pixelaod;

/** Owns registration of fingerprint/AOD observation and renderer hooks. */
final class PixelAodUdfpsHookInstaller {
    private PixelAodUdfpsHookInstaller() {
    }

    static void install(ClassLoader classLoader) {
        PixelAodHook.hookOplusFingerprintAodDiagnostics(classLoader);
    }
}
