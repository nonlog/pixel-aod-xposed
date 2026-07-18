package dev.codex.pixelaod;

final class ProximityAuthorityGate {
    enum Source {
        RAW_SENSOR,
        OOS_NATIVE
    }

    private boolean near;

    synchronized boolean update(Source source, boolean confirmedNear) {
        if (source != Source.OOS_NATIVE) {
            return false;
        }
        if (near == confirmedNear) {
            return false;
        }
        near = confirmedNear;
        return true;
    }

    synchronized boolean reset() {
        if (!near) {
            return false;
        }
        near = false;
        return true;
    }

    synchronized boolean isNear() {
        return near;
    }
}
