package dev.codex.pixelaod;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.Display;
import android.view.View;

/** M9 ADR 0065: resolve Pixel clock ownership only against the primary/default display. */
final class PrimaryDisplayPolicy {
    static final int UNKNOWN_DISPLAY_ID = -1;

    private PrimaryDisplayPolicy() {
    }

    static boolean isPrimaryDisplayId(int displayId) {
        return displayId == Display.DEFAULT_DISPLAY;
    }

    /**
     * Resolution precedence is deliberate: an explicitly associated secondary display must win
     * over the default-display fallback. The fallback exists only for a global SystemUI context
     * that has no display association at all.
     */
    static int resolveDisplayId(int viewDisplayId, int contextDisplayId, int fallbackDisplayId) {
        if (viewDisplayId != UNKNOWN_DISPLAY_ID) {
            return viewDisplayId;
        }
        if (contextDisplayId != UNKNOWN_DISPLAY_ID) {
            return contextDisplayId;
        }
        return fallbackDisplayId;
    }

    static boolean isPrimary(View view) {
        return isPrimaryDisplayId(displayId(view));
    }

    static int displayId(View view) {
        if (view == null) {
            return UNKNOWN_DISPLAY_ID;
        }
        int viewDisplayId = idOf(view.getDisplay());
        Context context = view.getContext();
        int contextDisplayId = associatedDisplayId(context);
        int fallbackDisplayId = defaultDisplayId(context);
        return resolveDisplayId(viewDisplayId, contextDisplayId, fallbackDisplayId);
    }

    /**
     * Returns display 0 for a primary/unassociated context, but never substitutes display 0 for a
     * context that Android explicitly associates with a secondary display.
     */
    static Display primaryDisplay(Context context) {
        if (context == null) {
            return null;
        }
        Display associated = associatedDisplay(context);
        if (associated != null) {
            return isPrimaryDisplayId(associated.getDisplayId()) ? associated : null;
        }
        try {
            DisplayManager manager =
                    (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            return manager != null ? manager.getDisplay(Display.DEFAULT_DISPLAY) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int associatedDisplayId(Context context) {
        return idOf(associatedDisplay(context));
    }

    private static Display associatedDisplay(Context context) {
        if (context == null || Build.VERSION.SDK_INT < 30) {
            return null;
        }
        try {
            return context.getDisplay();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int defaultDisplayId(Context context) {
        if (context == null) {
            return UNKNOWN_DISPLAY_ID;
        }
        try {
            DisplayManager manager =
                    (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            Display display = manager != null
                    ? manager.getDisplay(Display.DEFAULT_DISPLAY) : null;
            return idOf(display);
        } catch (Throwable ignored) {
            return UNKNOWN_DISPLAY_ID;
        }
    }

    private static int idOf(Display display) {
        try {
            return display != null ? display.getDisplayId() : UNKNOWN_DISPLAY_ID;
        } catch (Throwable ignored) {
            return UNKNOWN_DISPLAY_ID;
        }
    }
}
