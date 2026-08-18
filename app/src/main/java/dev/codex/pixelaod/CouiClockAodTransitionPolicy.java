package dev.codex.pixelaod;

/** Pure timing and generation policy for the COUI AOD entry/live-content transactions. */
final class CouiClockAodTransitionPolicy {
    static final long ENTRY_ANIMATION_DURATION_MS = 550L;

    enum EntryCompletion {
        STALE,
        NO_UPDATE,
        BIND_DIRECT,
        LIVE_CROSSFADE
    }

    enum LiveCompletion {
        STALE,
        NO_UPDATE,
        APPLY_DEFERRED
    }

    private CouiClockAodTransitionPolicy() {
    }

    static boolean acceptsLivePreDraw(long callbackGeneration, long currentGeneration,
            boolean partialAodActive) {
        return callbackGeneration == currentGeneration && partialAodActive;
    }

    static boolean sameContent(CouiClockPresentationModel.AodContent first,
            CouiClockPresentationModel.AodContent second) {
        CouiClockPresentationModel.AodContent left = first == null
                ? CouiClockPresentationModel.AodContent.none() : first;
        CouiClockPresentationModel.AodContent right = second == null
                ? CouiClockPresentationModel.AodContent.none() : second;
        return left.kind() == right.kind()
                && left.notificationIconCount() == right.notificationIconCount();
    }

    static boolean shouldRetargetLiveContent(CouiClockPresentationModel.AodContent current,
            CouiClockPresentationModel.AodContent next) {
        return !sameContent(current, next);
    }

    private static CouiClockPresentationModel.Scene partialSceneFor(
            CouiClockPresentationModel.AodContent content) {
        return (content == null
                ? CouiClockPresentationModel.AodContent.none() : content).kind()
                == CouiClockPresentationModel.AodContent.Kind.NONE
                ? CouiClockPresentationModel.Scene.LARGE
                : CouiClockPresentationModel.Scene.SMALL;
    }

    static final class EntryState {
        private enum Phase {
            WAITING_FOR_FRAME,
            ANIMATING,
            FINISHED
        }

        private final long generation;
        private final boolean animate;
        private final Phase phase;
        private final CouiClockPresentationModel.AodContent stagedContent;
        private final CouiClockPresentationModel.AodContent deferredContent;

        private EntryState(long generation, boolean animate, Phase phase,
                CouiClockPresentationModel.AodContent stagedContent,
                CouiClockPresentationModel.AodContent deferredContent) {
            this.generation = generation;
            this.animate = animate;
            this.phase = phase;
            this.stagedContent = stagedContent;
            this.deferredContent = deferredContent;
        }

        static EntryState begin(long generation, boolean animate,
                CouiClockPresentationModel.AodContent stagedContent) {
            return new EntryState(generation, animate, Phase.WAITING_FOR_FRAME,
                    stagedContent == null ? CouiClockPresentationModel.AodContent.none()
                            : stagedContent,
                    null);
        }

        EntryState afterAnimationFrame() {
            return new EntryState(generation, animate,
                    animate ? Phase.ANIMATING : Phase.FINISHED,
                    stagedContent, deferredContent);
        }

        EntryState defer(CouiClockPresentationModel.AodContent content) {
            return new EntryState(generation, animate, phase, stagedContent,
                    content == null ? CouiClockPresentationModel.AodContent.none() : content);
        }

        boolean isActiveAt(long elapsedMillis) {
            if (phase == Phase.WAITING_FOR_FRAME) {
                return true;
            }
            return phase == Phase.ANIMATING && elapsedMillis < completionDelayMillis();
        }

        long completionDelayMillis() {
            return animate ? ENTRY_ANIMATION_DURATION_MS : 0L;
        }

        EntryCompletion completion(long callbackGeneration, long currentGeneration,
                CouiClockPresentationModel.AodContent currentContent, boolean partialAodActive) {
            if (callbackGeneration != generation || callbackGeneration != currentGeneration
                    || phase == Phase.FINISHED) {
                return EntryCompletion.STALE;
            }
            if (deferredContent == null || sameContent(deferredContent, currentContent)) {
                return EntryCompletion.NO_UPDATE;
            }
            return partialAodActive
                    && partialSceneFor(currentContent) != partialSceneFor(deferredContent)
                    ? EntryCompletion.LIVE_CROSSFADE : EntryCompletion.BIND_DIRECT;
        }

        long generation() {
            return generation;
        }

        CouiClockPresentationModel.AodContent stagedContent() {
            return stagedContent;
        }

        CouiClockPresentationModel.AodContent deferredContent() {
            return deferredContent;
        }
    }

    static final class LiveState {
        private final long generation;
        private final CouiClockPresentationModel.AodContent deferred;

        private LiveState(long generation, CouiClockPresentationModel.AodContent deferred) {
            this.generation = generation;
            this.deferred = deferred;
        }

        static LiveState crossfade(long generation,
                CouiClockPresentationModel.AodContent initialContent) {
            return new LiveState(generation, initialContent);
        }

        LiveState defer(CouiClockPresentationModel.AodContent content) {
            return new LiveState(generation, content);
        }

        LiveCompletion finishIn(long callbackGeneration) {
            if (callbackGeneration != generation) {
                return LiveCompletion.STALE;
            }
            return deferred == null ? LiveCompletion.NO_UPDATE : LiveCompletion.APPLY_DEFERRED;
        }

        long generation() {
            return generation;
        }

        CouiClockPresentationModel.AodContent deferred() {
            return deferred;
        }
    }
}
