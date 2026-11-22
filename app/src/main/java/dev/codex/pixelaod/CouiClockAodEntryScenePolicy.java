package dev.codex.pixelaod;

/** Pure caller-side normalization for the COUI partial-AOD entry transaction. */
final class CouiClockAodEntryScenePolicy {
    private CouiClockAodEntryScenePolicy() {
    }

    /**
     * COUI resolves the partial-AOD scene from content before beginning the entry transaction.
     * A partial surface with any content is SMALL; an empty partial surface is LARGE. Panoramic
     * and ordinary lockscreen presentations retain their legitimate requested scene.
     */
    static CouiClockPresentationModel.Scene normalizeRequestedScene(
            CouiClockPresentationModel.Scene requestedScene,
            boolean partialAod,
            CouiClockPresentationModel.AodContent content) {
        CouiClockPresentationModel.Scene fallback = requestedScene == null
                ? CouiClockPresentationModel.Scene.LARGE : requestedScene;
        if (!partialAod) {
            return fallback;
        }
        CouiClockPresentationModel.AodContent safeContent = content == null
                ? CouiClockPresentationModel.AodContent.none() : content;
        return safeContent.kind() == CouiClockPresentationModel.AodContent.Kind.NONE
                ? CouiClockPresentationModel.Scene.LARGE
                : CouiClockPresentationModel.Scene.SMALL;
    }
}
