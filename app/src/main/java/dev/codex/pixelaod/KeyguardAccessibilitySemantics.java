package dev.codex.pixelaod;

/** Small pure helpers for grouped replacement-clock accessibility descriptions. */
final class KeyguardAccessibilitySemantics {
    private KeyguardAccessibilitySemantics() {
    }

    static String join(CharSequence... values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (CharSequence value : values) {
            if (value == null) {
                continue;
            }
            String text = value.toString().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(text);
        }
        return out.toString();
    }
}
