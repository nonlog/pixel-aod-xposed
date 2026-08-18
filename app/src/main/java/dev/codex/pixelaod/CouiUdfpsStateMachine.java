package dev.codex.pixelaod;

/**
 * Pure COUI UDFPS visual state model.
 *
 * <p>Vendor visibility and HBM remain owned by OPlus. This model only describes the replacement
 * glyph and the independent press/success glow so the hook layer can apply them without inventing
 * a second biometric lifecycle.</p>
 */
final class CouiUdfpsStateMachine {
    private static final String UPDATE_MONITOR_CALLBACK_CLASS =
            "com.oplus.systemui.biometrics.finger.udfps.OnScreenFingerprintUiMech$updateMonitorCallback$1";

    enum Surface {
        HIDDEN,
        LOCKSCREEN,
        AOD
    }

    enum Event {
        SHOW_LOCKSCREEN,
        SHOW_AOD,
        HIDE,
        NATIVE_TIMEOUT,
        TOUCH_DOWN,
        TOUCH_UP,
        SUCCESS,
        FAILURE,
        ERROR,
        RESET
    }

    static final class Snapshot {
        final Surface surface;
        final boolean pressActive;
        final boolean successActive;
        final boolean nativeTimeoutHidden;

        Snapshot(Surface surface, boolean pressActive, boolean successActive,
                boolean nativeTimeoutHidden) {
            this.surface = surface;
            this.pressActive = pressActive;
            this.successActive = successActive;
            this.nativeTimeoutHidden = nativeTimeoutHidden;
        }

        boolean isAod() {
            return surface == Surface.AOD;
        }

        boolean isVisible() {
            return surface != Surface.HIDDEN;
        }

        @Override
        public String toString() {
            return "surface=" + surface
                    + ",press=" + pressActive
                    + ",success=" + successActive
                    + ",nativeTimeout=" + nativeTimeoutHidden;
        }
    }

    private Surface surface = Surface.HIDDEN;
    private boolean pressActive;
    private boolean successActive;
    private boolean nativeTimeoutHidden;
    private boolean surfaceExplicitlyHidden;

    Snapshot dispatch(Event event) {
        if (event == null) {
            return snapshot();
        }
        switch (event) {
            case SHOW_LOCKSCREEN:
                surface = Surface.LOCKSCREEN;
                nativeTimeoutHidden = false;
                successActive = false;
                surfaceExplicitlyHidden = false;
                break;
            case SHOW_AOD:
                surface = Surface.AOD;
                nativeTimeoutHidden = false;
                successActive = false;
                surfaceExplicitlyHidden = false;
                break;
            case HIDE:
            case RESET:
                surface = Surface.HIDDEN;
                pressActive = false;
                successActive = false;
                nativeTimeoutHidden = false;
                surfaceExplicitlyHidden = true;
                break;
            case NATIVE_TIMEOUT:
                nativeTimeoutHidden = true;
                pressActive = false;
                successActive = false;
                break;
            case TOUCH_DOWN:
                if (surface != Surface.HIDDEN && !nativeTimeoutHidden) {
                    pressActive = true;
                    successActive = false;
                }
                break;
            case TOUCH_UP:
                pressActive = false;
                break;
            case SUCCESS:
                if (surface != Surface.HIDDEN && !nativeTimeoutHidden) {
                    pressActive = false;
                    successActive = true;
                }
                break;
            case FAILURE:
            case ERROR:
                pressActive = false;
                successActive = false;
                break;
            default:
                break;
        }
        return snapshot();
    }

    /**
     * Reconciles the replacement state with the live vendor fields used by COUI's refresh path.
     * The callback cache is not authoritative: OPlus may update these fields without invoking a
     * paired show/touch callback, and native timeout can clear the cached press state separately.
     */
    Snapshot synchronizeLive(boolean aod, boolean touchDown, boolean timeoutHidden) {
        if (timeoutHidden) {
            nativeTimeoutHidden = true;
            pressActive = false;
            successActive = false;
            return snapshot();
        }
        nativeTimeoutHidden = false;
        if (!surfaceExplicitlyHidden) {
            if (aod) {
                surface = Surface.AOD;
            } else if (surface != Surface.HIDDEN) {
                surface = Surface.LOCKSCREEN;
            }
        }
        pressActive = touchDown && surface != Surface.HIDDEN && !successActive;
        return snapshot();
    }

    Snapshot snapshot() {
        return new Snapshot(surface, pressActive, successActive, nativeTimeoutHidden);
    }

    static long stateTransitionDurationMillis() {
        return 420L;
    }

    static long pressExpandDurationMillis() {
        return 180L;
    }

    static long pressRetractDurationMillis() {
        return 160L;
    }

    static long successDurationMillis() {
        return 500L;
    }

    static long clampAodExitDurationMillis(long durationMillis) {
        if (durationMillis < 100L) {
            return 100L;
        }
        if (durationMillis > 2_000L) {
            return 2_000L;
        }
        return durationMillis;
    }

    static Boolean visibilityArgument(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof Boolean) {
                return (Boolean) arg;
            }
        }
        for (Object arg : args) {
            if (arg instanceof Number) {
                // OPlus uses Android View visibility values for the integer overload.
                return ((Number) arg).intValue() == 0;
            }
        }
        return null;
    }

    static Object resolveAuthenticationUiMech(String callbackClassName,
            Object callbackOuter, Object fallback) {
        return UPDATE_MONITOR_CALLBACK_CLASS.equals(callbackClassName) && callbackOuter != null
                ? callbackOuter : fallback;
    }
}
