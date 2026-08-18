package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.List;

/**
 * Startup-selected facade for the module's one primary clock owner.
 *
 * <p>The startup installer is intentionally the only place that names both concrete controller
 * implementations. Runtime callers use this facade so a COUI_PORT process cannot accidentally
 * call the legacy controller or create a legacy primary view.</p>
 */
final class ActiveClockRendererController {
    private static final ClockRendererStartupRouter.Installer PRODUCTION_INSTALLER =
            new ClockRendererStartupRouter.Installer() {
                @Override
                public void installLegacy(Context context, ClassLoader classLoader) {
                    ClockPluginHostController.install(context, classLoader);
                }

                @Override
                public void installCoui(Context context, ClassLoader classLoader) {
                    CouiClockPluginHostController.install(context, classLoader);
                }
            };

    private ActiveClockRendererController() {
    }

    static ClockRendererStartupRouter.Installer productionInstaller() {
        return PRODUCTION_INSTALLER;
    }

    static boolean isCouiPort() {
        return PixelAodFeatureFlags.useCouiClockRenderer();
    }

    static boolean blocksLegacyPrimaryOwner() {
        return isCouiPort();
    }

    static boolean hasValidatedHost() {
        return isCouiPort()
                ? CouiClockPluginHostController.hasValidatedHost()
                : ClockPluginHostController.hasValidatedHost();
    }

    static void noteLockscreenToAodHandoff(String source) {
        if (!isCouiPort()) {
            ClockPluginHostController.noteLockscreenToAodHandoff(source);
        }
    }

    static void refreshAll(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.refreshAll(source);
        } else {
            ClockPluginHostController.refreshAll(source);
        }
    }

    static void prepareAodToLockscreenTransition(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.prepareAodToLockscreenTransition(source);
        }
    }

    static void prepareNonLockscreenAodEntry(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.prepareNonLockscreenAodEntry(source);
        } else {
            ClockPluginHostController.prepareNonLockscreenAodEntry(source);
        }
    }

    static void prepareNonLockscreenAodEntryEarly(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.prepareNonLockscreenAodEntryEarly(source);
        }
    }

    static void prepareLockscreenEntry(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.prepareLockscreenEntry(source);
        } else {
            ClockPluginHostController.refreshAll(source + "#legacy-lockscreen");
        }
    }

    static void refreshSemanticData(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.refreshSemanticData(source);
        }
    }

    static void setAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setAodContent(content, animate, source);
        }
        // Legacy content remains owned by the existing semantic/visual path until Slice 3.
    }

    static void setLiveAodContent(CouiClockPresentationModel.AodContent content, boolean animate,
            String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setLiveAodContent(content, animate, source);
        }
    }

    static void setInformation(CharSequence date, CharSequence week, CharSequence weather,
            Drawable weatherIcon, String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setInformation(date, week, weather, weatherIcon, source);
        }
    }

    static void refreshInformationFromExistingAdapters(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.refreshInformationFromExistingAdapters(source);
        }
    }

    static void setNotificationIcons(List<? extends Drawable> icons, String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setNotificationIcons(icons, source);
        }
    }

    static void setMediaData(CharSequence title, CharSequence artist, Drawable appIcon,
            String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setMediaData(title, artist, appIcon, source);
        }
    }

    static void setBurnInTranslation(float x, float y, long durationMillis, String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.setBurnInTranslation(x, y, durationMillis, source);
        }
    }

    static void onTimeTick(String source) {
        if (isCouiPort()) {
            CouiClockPluginHostController.onTimeTick(source);
        }
    }
}
