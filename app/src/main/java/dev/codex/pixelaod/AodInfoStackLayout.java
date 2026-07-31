package dev.codex.pixelaod;

/** Calculates the next AOD information-row position from the visible predecessor. */
final class AodInfoStackLayout {
    private AodInfoStackLayout() {
    }

    static int mediaTopAfterNotification(int dateTop, int weatherAlertTop, int calendarTop,
            int notificationTop, boolean weatherAlertVisible, boolean calendarVisible) {
        int previousTop = calendarVisible ? calendarTop
                : weatherAlertVisible ? weatherAlertTop : dateTop;
        return notificationTop + Math.max(0, notificationTop - previousTop);
    }

    static int topAfterVisibleRow(int defaultTop, int precedingBottom, int gap) {
        return Math.max(defaultTop, precedingBottom + Math.max(0, gap));
    }

    static int rowBottom(int top, int measuredHeight, int fallbackHeight) {
        return top + Math.max(Math.max(0, measuredHeight), Math.max(0, fallbackHeight));
    }
}
