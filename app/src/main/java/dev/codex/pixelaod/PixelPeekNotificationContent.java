package dev.codex.pixelaod;

import android.graphics.drawable.Drawable;
import android.text.Layout;

/** Privacy-processed content copied from OPlus' native incoming-notification paint model. */
final class PixelPeekNotificationContent {
    final String notificationKey;
    final String packageName;
    final CharSequence appName;
    final CharSequence title;
    final CharSequence message;
    final Drawable appIcon;

    PixelPeekNotificationContent(String notificationKey, String packageName, CharSequence appName,
            CharSequence title, CharSequence message, Drawable appIcon) {
        this.notificationKey = notificationKey;
        this.packageName = packageName;
        this.appName = clean(appName);
        this.title = clean(title);
        this.message = clean(message);
        this.appIcon = appIcon;
    }

    boolean hasRenderableText() {
        return appName.length() > 0 || title.length() > 0 || message.length() > 0;
    }

    static CharSequence textFromLayout(Object value) {
        if (!(value instanceof Layout)) {
            return "";
        }
        CharSequence text = ((Layout) value).getText();
        return clean(text);
    }

    private static CharSequence clean(CharSequence text) {
        if (text == null) {
            return "";
        }
        String value = text.toString().trim();
        return value;
    }
}
