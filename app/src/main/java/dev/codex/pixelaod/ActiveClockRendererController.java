package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * Facade for the module's single primary COUI clock owner.
 *
 * <p>M8 removes the historical runtime legacy/COUI selector. Keeping one facade lets semantic and
 * lifecycle producers remain decoupled from the concrete host controller while guaranteeing that
 * SystemUI can install only the validated COUI owner.</p>
 */
final class ActiveClockRendererController {
    private ActiveClockRendererController() {
    }

    static void install(Context context, ClassLoader classLoader) {
        CouiClockPluginHostController.install(context, classLoader);
    }

    static boolean blocksLegacyPrimaryOwner() {
        return true;
    }

    static boolean hasValidatedHost() {
        return CouiClockPluginHostController.hasValidatedHost();
    }

    static void noteLockscreenToAodHandoff(String source) {
        // Legacy-only handoff hook retained as a no-op until S2 removes the old caller surface.
    }

    static void refreshAll(String source) {
        CouiClockPluginHostController.refreshAll(source);
    }

    static void prepareAodToLockscreenTransition(String source) {
        CouiClockPluginHostController.prepareAodToLockscreenTransition(source);
    }

    static void prepareNonLockscreenAodEntry(String source) {
        CouiClockPluginHostController.prepareNonLockscreenAodEntry(source);
    }

    static void prepareNonLockscreenAodEntryEarly(String source) {
        CouiClockPluginHostController.prepareNonLockscreenAodEntryEarly(source);
    }

    static void prepareLockscreenEntry(String source) {
        CouiClockPluginHostController.prepareLockscreenEntry(source);
    }

    static void refreshSemanticData(String source) {
        CouiClockPluginHostController.refreshSemanticData(source);
    }

    static void setAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        CouiClockPluginHostController.setAodContent(content, animate, source);
    }

    static void setLiveAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        CouiClockPluginHostController.setLiveAodContent(content, animate, source);
    }

    static void setInformation(CharSequence date, CharSequence week, CharSequence weather,
            Drawable weatherIcon, String source) {
        CouiClockPluginHostController.setInformation(date, week, weather, weatherIcon, source);
    }

    static void refreshInformationFromExistingAdapters(String source) {
        CouiClockPluginHostController.refreshInformationFromExistingAdapters(source);
    }

    static void setNotificationIcons(List<? extends Drawable> icons, String source) {
        CouiClockPluginHostController.setNotificationIcons(icons, source);
    }

    static void setMediaData(CharSequence title, CharSequence artist, Drawable appIcon,
            String source) {
        CouiClockPluginHostController.setMediaData(title, artist, appIcon, source);
    }

    static void setBurnInTranslation(float x, float y, long durationMillis, String source) {
        CouiClockPluginHostController.setBurnInTranslation(x, y, durationMillis, source);
    }

    static void onTimeTick(String source) {
        CouiClockPluginHostController.onTimeTick(source);
    }
}
