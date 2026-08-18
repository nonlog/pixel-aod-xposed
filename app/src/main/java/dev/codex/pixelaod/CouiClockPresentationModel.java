package dev.codex.pixelaod;

/**
 * Pure scene, AOD-content, and animation contract for the COUI clock host.
 */
public final class CouiClockPresentationModel {
    public static final long TARGET_TRANSITION_MS = 550L;
    public static final long LIVE_FADE_OUT_MS = 150L;
    public static final long LIVE_FADE_IN_MS = 200L;
    public static final float COLON_START_FRACTION = 0.52f;
    public static final float COLON_DURATION_FRACTION = 0.22f;
    public static final float MOTION_X1 = 0.2f;
    public static final float MOTION_Y1 = 0.0f;
    public static final float MOTION_X2 = 0.0f;
    public static final float MOTION_Y2 = 1.0f;

    public enum Scene {
        LARGE,
        SMALL,
        IMMERSED
    }

    public static final class AodContent {
        public enum Kind {
            NONE,
            NOTIFICATIONS,
            MEDIA
        }

        private final Kind kind;
        private final int notificationIconCount;

        public AodContent(Kind kind, int notificationIconCount) {
            this.kind = kind == null ? Kind.NONE : kind;
            this.notificationIconCount = Math.max(0, notificationIconCount);
        }

        public static AodContent none() {
            return new AodContent(Kind.NONE, 0);
        }

        public static AodContent notifications(int notificationIconCount) {
            return new AodContent(Kind.NOTIFICATIONS, notificationIconCount);
        }

        public static AodContent media(int notificationIconCount) {
            return new AodContent(Kind.MEDIA, notificationIconCount);
        }

        public Kind kind() {
            return kind;
        }

        public int notificationIconCount() {
            return notificationIconCount;
        }
    }

    private final Scene requestedScene;
    private final boolean dozing;
    private final boolean partialAod;
    private final AodContent content;

    public CouiClockPresentationModel(Scene requestedScene, boolean dozing,
            boolean partialAod, AodContent content) {
        this.requestedScene = requestedScene == null ? Scene.LARGE : requestedScene;
        this.dozing = dozing;
        this.partialAod = partialAod;
        this.content = content == null ? AodContent.none() : content;
    }

    public Scene requestedScene() {
        return requestedScene;
    }

    public boolean dozing() {
        return dozing;
    }

    public boolean partialAod() {
        return partialAod;
    }

    public AodContent content() {
        return content;
    }

    public Scene visualScene() {
        return showsPartialContent() ? Scene.SMALL : requestedScene;
    }

    public boolean showsPartialContent() {
        return dozing && partialAod && content.kind() != AodContent.Kind.NONE;
    }
}
