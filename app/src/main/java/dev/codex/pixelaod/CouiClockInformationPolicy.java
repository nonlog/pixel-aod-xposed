package dev.codex.pixelaod;

/**
 * Maps the existing module semantic render model to the COUI host's information slots.
 *
 * <p>This is data adaptation only. It does not create or own a clock view.</p>
 */
final class CouiClockInformationPolicy {
    private CouiClockInformationPolicy() {
    }

    static Data from(PixelAodRenderModel renderModel) {
        if (renderModel == null) {
            return new Data("", "");
        }
        return from(renderModel.dateText, renderModel.weatherText);
    }

    static Data from(CharSequence dateText, CharSequence weatherText) {
        return new Data(dateText == null ? "" : dateText.toString(),
                weatherText == null ? "" : weatherText.toString());
    }

    static final class Data {
        final String dateText;
        final String weatherText;

        Data(String dateText, String weatherText) {
            this.dateText = dateText == null ? "" : dateText;
            this.weatherText = weatherText == null ? "" : weatherText;
        }
    }
}
