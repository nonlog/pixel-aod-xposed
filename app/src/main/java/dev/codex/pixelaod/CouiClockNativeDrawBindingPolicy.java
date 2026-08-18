package dev.codex.pixelaod;

/**
 * Pure gate for the native dispatchDraw binding.
 *
 * <p>A native container that touches the replacement host can contain, draw, or be the host
 * itself. Binding it would make the native suppression hook capable of suppressing the COUI
 * replacement, so only a container outside the host's ancestor/descendant chain is eligible.</p>
 */
final class CouiClockNativeDrawBindingPolicy {
    private CouiClockNativeDrawBindingPolicy() {
    }

    static boolean mayBind(boolean touchesHost) {
        return !touchesHost;
    }
}
