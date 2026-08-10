package dev.codex.pixelaod;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps the final StatusBar icon and the live OOS capsule icon bound to the same notification
 * lifecycle.  This deliberately never mutates OOS's notification data list.
 */
final class NotificationCapsuleIconPolicy {
    private final Map<String, Object> finalDrawablesByNotificationKey =
            Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<Object, CaptureToken> captureByIconView = new WeakHashMap<>();
    private final Map<String, Long> latestCaptureGenerationByKey = new HashMap<>();
    private final Map<String, Long> invalidationGenerationByKey = new HashMap<>();
    private final Map<String, CapsuleBindingToken> latestBindingByKey = new HashMap<>();
    private final Map<Object, CapsuleBindingToken> latestBindingByIconView = new WeakHashMap<>();
    private final Map<String, CapsuleBindingToken> queuedLateBindingsByKey =
            new LinkedHashMap<>();
    private long nextCaptureGeneration;
    private long nextCapsuleDataGeneration;
    private boolean lateBindingDispatchQueued;

    static final class CaptureToken {
        final WeakReference<Object> iconView;
        final String notificationKey;
        final long viewGeneration;
        final long keyGeneration;
        final long keyInvalidationGeneration;

        CaptureToken(Object iconView, String notificationKey, long viewGeneration,
                long keyGeneration, long keyInvalidationGeneration) {
            this.iconView = new WeakReference<>(iconView);
            this.notificationKey = notificationKey;
            this.viewGeneration = viewGeneration;
            this.keyGeneration = keyGeneration;
            this.keyInvalidationGeneration = keyInvalidationGeneration;
        }
    }

    static final class CapsuleBindingToken {
        final WeakReference<Object> iconView;
        final String notificationKey;
        final long dataGeneration;
        final long keyInvalidationGeneration;
        boolean awaitingFinalDrawable;

        CapsuleBindingToken(Object iconView, String notificationKey, long dataGeneration,
                long keyInvalidationGeneration) {
            this.iconView = new WeakReference<>(iconView);
            this.notificationKey = notificationKey;
            this.dataGeneration = dataGeneration;
            this.keyInvalidationGeneration = keyInvalidationGeneration;
        }
    }

    /** Stores an already-isolated final StatusBarIconView drawable under the notification key. */
    void cacheFinalDrawable(String notificationKey, Object isolatedDrawable) {
        if (notificationKey == null || notificationKey.isEmpty() || isolatedDrawable == null) {
            return;
        }
        finalDrawablesByNotificationKey.put(notificationKey, isolatedDrawable);
    }

    synchronized CaptureToken beginFinalDrawableCapture(Object iconView, String notificationKey) {
        if (iconView == null || notificationKey == null || notificationKey.isEmpty()) {
            return null;
        }
        long generation = ++nextCaptureGeneration;
        CaptureToken token = new CaptureToken(iconView, notificationKey, generation,
                generation, invalidationGeneration(notificationKey));
        captureByIconView.put(iconView, token);
        latestCaptureGenerationByKey.put(notificationKey, generation);
        return token;
    }

    /**
     * A deferred capture must still be the latest capture for both its recycled view and its
     * notification key, and must not cross a removal boundary.
     */
    synchronized boolean acceptsFinalDrawableCapture(CaptureToken token) {
        Object iconView = token != null ? token.iconView.get() : null;
        return token != null
                && iconView != null
                && captureByIconView.get(iconView) == token
                && token.keyGeneration == latestCaptureGeneration(token.notificationKey)
                && token.keyInvalidationGeneration
                == invalidationGeneration(token.notificationKey);
    }

    /**
     * Caches a final drawable and queues a direct live-icon update only for a still-current
     * capsule binding that previously missed it.  The boolean says whether one UI dispatch must
     * be posted; multiple keys are merged in that dispatch.
     */
    synchronized boolean acceptFinalDrawableCapture(CaptureToken token, Object isolatedDrawable) {
        if (!acceptsFinalDrawableCapture(token) || isolatedDrawable == null) {
            return false;
        }
        finalDrawablesByNotificationKey.put(token.notificationKey, isolatedDrawable);
        CapsuleBindingToken binding = latestBindingByKey.get(token.notificationKey);
        if (!isCurrentBinding(binding) || !binding.awaitingFinalDrawable) {
            return false;
        }
        queuedLateBindingsByKey.put(token.notificationKey, binding);
        if (lateBindingDispatchQueued) {
            return false;
        }
        lateBindingDispatchQueued = true;
        return true;
    }

    /** Starts a new OOS card binding and supersedes older ownership by either key or live view. */
    synchronized CapsuleBindingToken beginCapsuleIconBinding(Object iconView, String notificationKey) {
        if (iconView == null || notificationKey == null || notificationKey.isEmpty()) {
            return null;
        }
        supersedeCapsuleBinding(latestBindingByIconView.get(iconView));
        supersedeCapsuleBinding(latestBindingByKey.get(notificationKey));
        CapsuleBindingToken token = new CapsuleBindingToken(iconView, notificationKey,
                ++nextCapsuleDataGeneration, invalidationGeneration(notificationKey));
        latestBindingByKey.put(notificationKey, token);
        latestBindingByIconView.put(iconView, token);
        return token;
    }

    synchronized void noteCapsuleCacheMiss(CapsuleBindingToken token) {
        if (isCurrentBinding(token)) {
            token.awaitingFinalDrawable = true;
        }
    }

    synchronized boolean acceptsCapsuleBinding(CapsuleBindingToken token) {
        return isCurrentBinding(token);
    }

    /** Atomically takes the merged direct-update work. Each token is revalidated by the caller. */
    synchronized List<CapsuleBindingToken> takeQueuedLateCapsuleBindings() {
        List<CapsuleBindingToken> bindings = new ArrayList<>(queuedLateBindingsByKey.values());
        queuedLateBindingsByKey.clear();
        lateBindingDispatchQueued = false;
        return bindings;
    }

    /** Returns the captured drawable; callers clone it before giving it to an OOS card. */
    Object finalDrawableFor(String notificationKey) {
        if (notificationKey == null || notificationKey.isEmpty()) {
            return null;
        }
        return finalDrawablesByNotificationKey.get(notificationKey);
    }

    synchronized void removeFinalDrawable(String notificationKey) {
        if (notificationKey == null) {
            return;
        }
        finalDrawablesByNotificationKey.remove(notificationKey);
        invalidationGenerationByKey.put(notificationKey,
                invalidationGeneration(notificationKey) + 1L);
        supersedeCapsuleBinding(latestBindingByKey.get(notificationKey));
        latestCaptureGenerationByKey.remove(notificationKey);
        captureByIconView.entrySet().removeIf(entry -> notificationKey.equals(
                entry.getValue().notificationKey));
    }

    synchronized void clearFinalDrawables() {
        finalDrawablesByNotificationKey.clear();
        captureByIconView.clear();
        latestCaptureGenerationByKey.clear();
        invalidationGenerationByKey.clear();
        latestBindingByKey.clear();
        latestBindingByIconView.clear();
        queuedLateBindingsByKey.clear();
        lateBindingDispatchQueued = false;
    }

    private boolean isCurrentBinding(CapsuleBindingToken token) {
        Object iconView = token != null ? token.iconView.get() : null;
        return token != null
                && iconView != null
                && latestBindingByKey.get(token.notificationKey) == token
                && latestBindingByIconView.get(iconView) == token
                && token.keyInvalidationGeneration
                == invalidationGeneration(token.notificationKey);
    }

    /** Removes a token only from indexes that still point to that exact token. */
    private void supersedeCapsuleBinding(CapsuleBindingToken token) {
        if (token == null) {
            return;
        }
        if (latestBindingByKey.get(token.notificationKey) == token) {
            latestBindingByKey.remove(token.notificationKey);
        }
        Object iconView = token.iconView.get();
        if (iconView != null && latestBindingByIconView.get(iconView) == token) {
            latestBindingByIconView.remove(iconView);
        }
        if (queuedLateBindingsByKey.get(token.notificationKey) == token) {
            queuedLateBindingsByKey.remove(token.notificationKey);
        }
    }

    private long invalidationGeneration(String notificationKey) {
        Long value = invalidationGenerationByKey.get(notificationKey);
        return value != null ? value : 0L;
    }

    private long latestCaptureGeneration(String notificationKey) {
        Long generation = latestCaptureGenerationByKey.get(notificationKey);
        return generation != null ? generation : -1L;
    }
}
