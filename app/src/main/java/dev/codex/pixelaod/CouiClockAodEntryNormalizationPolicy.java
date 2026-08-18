package dev.codex.pixelaod;

/**
 * COUI 2.5's special screen-off-from-unlocked AOD entry normalization.
 *
 * <p>The anti-obfuscation build resolves partial-AOD content before calling beginAodEntry().
 * When content is present it passes SMALL as the entry scene; it does not pass the raw LARGE
 * partial-AOD request and wait for visualScene() to correct it one frame later.</p>
 */
final class CouiClockAodEntryNormalizationPolicy {
    private CouiClockAodEntryNormalizationPolicy() {
    }

    static CouiClockPresentationModel normalizeUnlockedEntry(
            CouiClockPresentationModel presentation) {
        if (presentation == null || !presentation.dozing() || !presentation.partialAod()) {
            return presentation;
        }
        CouiClockPresentationModel.Scene entryScene = presentation.content().kind()
                == CouiClockPresentationModel.AodContent.Kind.NONE
                ? CouiClockPresentationModel.Scene.LARGE
                : CouiClockPresentationModel.Scene.SMALL;
        return new CouiClockPresentationModel(entryScene, true, true, presentation.content());
    }
}
