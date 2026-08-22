package dev.codex.pixelaod;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data-only bridge for semantic state that already exists in the module/SystemUI process.
 *
 * <p>This class never creates a clock or attaches a view. It only snapshots filtered notification
 * icons, active media metadata, and the resulting COUI content kind for the single COUI host.</p>
 */
final class CouiClockSemanticAdapter {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final List<MediaController> CONTROLLERS = new ArrayList<>();
    private static final MediaController.Callback CONTROLLER_CALLBACK =
            new MediaController.Callback() {
                @Override
                public void onPlaybackStateChanged(PlaybackState state) {
                    refreshMediaState("media-playback");
                }

                @Override
                public void onMetadataChanged(MediaMetadata metadata) {
                    refreshMediaState("media-metadata");
                }

                @Override
                public void onSessionDestroyed() {
                    refreshControllers();
                }
            };

    private static boolean installed;
    private static MediaSessionManager mediaSessionManager;
    private static Context applicationContext;
    private static Runnable changeListener;
    private static MediaData activeMedia = MediaData.empty();

    private CouiClockSemanticAdapter() {
    }

    static void install(Context context, Runnable listener) {
        if (context == null) {
            return;
        }
        synchronized (LOCK) {
            applicationContext = context.getApplicationContext() != null
                    ? context.getApplicationContext() : context;
            if (listener != null) {
                changeListener = listener;
            }
            if (installed) {
                return;
            }
            installed = true;
        }
        try {
            mediaSessionManager = (MediaSessionManager) applicationContext.getSystemService(
                    Context.MEDIA_SESSION_SERVICE);
            if (mediaSessionManager == null) {
                PixelAodLog.log("COUI semantic media adapter unavailable reason=no-manager");
                return;
            }
            mediaSessionManager.addOnActiveSessionsChangedListener(
                    CouiClockSemanticAdapter::replaceControllers, null);
            replaceControllers(mediaSessionManager.getActiveSessions(null));
            PixelAodLog.log("COUI semantic media adapter installed rendererMode=COUI_PORT");
        } catch (Throwable t) {
            PixelAodLog.log("COUI semantic media adapter unavailable", t);
        }
    }

    static Snapshot snapshot(Context context) {
        Context effectiveContext = context != null ? context : applicationContext;
        List<Drawable> icons = PixelAodContentState.currentCouiNotificationIcons(effectiveContext);
        MediaData media;
        synchronized (LOCK) {
            media = activeMedia;
        }
        CouiClockPresentationModel.AodContent content =
                CouiClockAodContentPolicy.fromSemanticState(media.present, icons.size());
        return new Snapshot(icons, media, content);
    }

    private static void refreshControllers() {
        MediaSessionManager manager = mediaSessionManager;
        if (manager == null) {
            refreshMediaState("media-session-destroyed");
            return;
        }
        try {
            replaceControllers(manager.getActiveSessions(null));
        } catch (Throwable ignored) {
            refreshMediaState("media-session-destroyed");
        }
    }

    private static void replaceControllers(List<MediaController> controllers) {
        MAIN.post(() -> {
            synchronized (LOCK) {
                for (MediaController controller : CONTROLLERS) {
                    unregisterCallback(controller);
                }
                CONTROLLERS.clear();
                if (controllers != null) {
                    CONTROLLERS.addAll(controllers);
                    for (MediaController controller : CONTROLLERS) {
                        registerCallback(controller);
                    }
                }
            }
            refreshMediaState("media-sessions");
        });
    }

    private static void registerCallback(MediaController controller) {
        if (controller == null) {
            return;
        }
        try {
            controller.registerCallback(CONTROLLER_CALLBACK, MAIN);
        } catch (Throwable ignored) {
            // Media sessions are optional; metadata remains best effort.
        }
    }

    private static void unregisterCallback(MediaController controller) {
        if (controller == null) {
            return;
        }
        try {
            controller.unregisterCallback(CONTROLLER_CALLBACK);
        } catch (Throwable ignored) {
        }
    }

    private static MediaData currentMediaData(Context context) {
        List<MediaController> controllers;
        synchronized (LOCK) {
            controllers = new ArrayList<>(CONTROLLERS);
        }
        for (MediaController controller : controllers) {
            if (isPlaying(controller) && hasDisplayableMetadata(controller)) {
                return mediaData(context, controller);
            }
        }
        return MediaData.empty();
    }

    private static boolean isPlaying(MediaController controller) {
        PlaybackState state = playbackState(controller);
        if (state == null) {
            return false;
        }
        return CouiClockMediaPolicy.isActivePlaybackState(state.getState());
    }

    private static PlaybackState playbackState(MediaController controller) {
        if (controller == null) {
            return null;
        }
        try {
            return controller.getPlaybackState();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean hasDisplayableMetadata(MediaController controller) {
        if (controller == null) {
            return false;
        }
        MediaMetadata metadata;
        try {
            metadata = controller.getMetadata();
        } catch (Throwable ignored) {
            return false;
        }
        if (metadata == null) {
            return false;
        }
        return !TextUtils.isEmpty(firstText(metadata, MediaMetadata.METADATA_KEY_TITLE,
                MediaMetadata.METADATA_KEY_DISPLAY_TITLE))
                || !TextUtils.isEmpty(firstText(metadata, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
    }

    private static MediaData mediaData(Context context, MediaController controller) {
        MediaMetadata metadata = null;
        try {
            metadata = controller.getMetadata();
        } catch (Throwable ignored) {
        }
        CharSequence title = firstText(metadata, MediaMetadata.METADATA_KEY_TITLE,
                MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        CharSequence artist = firstText(metadata, MediaMetadata.METADATA_KEY_ARTIST,
                MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (metadata != null && TextUtils.isEmpty(title) && metadata.getDescription() != null) {
            title = metadata.getDescription().getTitle();
        }
        String packageName = controller.getPackageName();
        if (TextUtils.isEmpty(title)) {
            title = packageName;
        }
        Drawable appIcon = null;
        if (context != null) {
            try {
                appIcon = context.getPackageManager().getApplicationIcon(packageName);
            } catch (Throwable ignored) {
            }
            if (TextUtils.isEmpty(artist)) {
                try {
                    artist = context.getPackageManager().getApplicationLabel(
                            context.getPackageManager().getApplicationInfo(packageName, 0));
                } catch (Throwable ignored) {
                }
            }
        }
        if (TextUtils.isEmpty(artist)) {
            artist = packageName;
        }
        return new MediaData(true, packageName, title == null ? "" : title,
                artist == null ? "" : artist, appIcon);
    }

    private static CharSequence firstText(MediaMetadata metadata, String... keys) {
        if (metadata == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            CharSequence value = metadata.getText(key);
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }

    private static void refreshMediaState(String source) {
        Context context;
        synchronized (LOCK) {
            context = applicationContext;
        }
        MediaData next = currentMediaData(context);
        Runnable listener;
        synchronized (LOCK) {
            if (sameSemanticMedia(activeMedia, next)) {
                return;
            }
            activeMedia = next;
            listener = changeListener;
        }
        if (listener != null) {
            MAIN.post(() -> listener.run());
        }
        PixelAodLog.log("COUI semantic media state changed rendererMode=COUI_PORT source=" + source);
    }

    private static boolean sameSemanticMedia(MediaData first, MediaData second) {
        MediaData safeFirst = first == null ? MediaData.empty() : first;
        MediaData safeSecond = second == null ? MediaData.empty() : second;
        return CouiClockMediaPolicy.sameSemanticMedia(
                safeFirst.present, safeFirst.packageName, safeFirst.title, safeFirst.artist,
                safeSecond.present, safeSecond.packageName, safeSecond.title, safeSecond.artist);
    }

    static final class Snapshot {
        final List<Drawable> notificationIcons;
        final MediaData media;
        final CouiClockPresentationModel.AodContent content;

        Snapshot(List<Drawable> notificationIcons, MediaData media,
                CouiClockPresentationModel.AodContent content) {
            this.notificationIcons = notificationIcons == null
                    ? Collections.emptyList() : Collections.unmodifiableList(
                    new ArrayList<>(notificationIcons));
            this.media = media == null ? MediaData.empty() : media;
            this.content = content == null
                    ? CouiClockPresentationModel.AodContent.none() : content;
        }
    }

    static final class MediaData {
        final boolean present;
        final String packageName;
        final CharSequence title;
        final CharSequence artist;
        final Drawable appIcon;

        MediaData(boolean present, CharSequence title, CharSequence artist, Drawable appIcon) {
            this(present, "", title, artist, appIcon);
        }

        MediaData(boolean present, String packageName, CharSequence title, CharSequence artist,
                Drawable appIcon) {
            this.present = present;
            this.packageName = packageName == null ? "" : packageName;
            this.title = title == null ? "" : title;
            this.artist = artist == null ? "" : artist;
            this.appIcon = appIcon;
        }

        static MediaData empty() {
            return new MediaData(false, "", "", "", null);
        }
    }
}
