package dev.codex.pixelaod;

final class OosProximityTransitionGate {
    enum Transition {
        NONE,
        NEAR,
        FAR
    }

    private boolean initialized;
    private boolean near;

    synchronized Transition update(boolean nextNear) {
        if (!initialized) {
            initialized = true;
            near = nextNear;
            return nextNear ? Transition.NEAR : Transition.NONE;
        }
        if (near == nextNear) {
            return Transition.NONE;
        }
        near = nextNear;
        return nextNear ? Transition.NEAR : Transition.FAR;
    }

    synchronized void reset() {
        initialized = false;
        near = false;
    }
}
