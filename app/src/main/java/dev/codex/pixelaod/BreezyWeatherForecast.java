package dev.codex.pixelaod;

import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Immutable, source-shaped projection of one Breezy daily forecast entry. */
final class BreezyWeatherForecast {
    static final long MAX_SOURCE_AGE_MILLIS = 6L * 60L * 60L * 1000L;
    static final int UNKNOWN_WEATHER_CODE = Integer.MIN_VALUE;

    final String locationId;
    final LocalDate forecastDate;
    final int weatherCode;
    final String conditionText;
    final double highCelsius;
    final double lowCelsius;
    final long sourceUpdatedAtMillis;
    final boolean daytimeIcon;

    private BreezyWeatherForecast(String locationId, LocalDate forecastDate, int weatherCode,
            String conditionText, double highCelsius, double lowCelsius,
            long sourceUpdatedAtMillis, boolean daytimeIcon) {
        this.locationId = normalize(locationId);
        this.forecastDate = forecastDate;
        this.conditionText = normalizeCondition(conditionText);
        this.weatherCode = weatherCode != UNKNOWN_WEATHER_CODE
                ? weatherCode : representativeWeatherCode(this.conditionText);
        this.highCelsius = highCelsius;
        this.lowCelsius = lowCelsius;
        this.sourceUpdatedAtMillis = Math.max(0L, sourceUpdatedAtMillis);
        this.daytimeIcon = daytimeIcon;
    }

    static BreezyWeatherForecast forFields(String locationId, LocalDate forecastDate,
            int weatherCode, String conditionText, double highCelsius, double lowCelsius,
            long sourceUpdatedAtMillis) {
        return new BreezyWeatherForecast(locationId, forecastDate, weatherCode, conditionText,
                highCelsius, lowCelsius, sourceUpdatedAtMillis, false);
    }

    static BreezyWeatherForecast forDaytimeFields(String locationId, LocalDate forecastDate,
            int weatherCode, String conditionText, double highCelsius, double lowCelsius,
            long sourceUpdatedAtMillis) {
        return new BreezyWeatherForecast(locationId, forecastDate, weatherCode, conditionText,
                highCelsius, lowCelsius, sourceUpdatedAtMillis, true);
    }

    static BreezyWeatherForecast empty() {
        return new BreezyWeatherForecast("", null, UNKNOWN_WEATHER_CODE, "",
                Double.NaN, Double.NaN, 0L, false);
    }

    boolean hasUsableIcon() {
        return weatherCode != UNKNOWN_WEATHER_CODE;
    }

    boolean hasCompleteTemperatures() {
        return isFinite(highCelsius) && isFinite(lowCelsius);
    }

    boolean isComplete() {
        return forecastDate != null && hasUsableIcon() && hasCompleteTemperatures()
                && !locationId.isEmpty();
    }

    boolean isSourceFresh(long nowMillis) {
        long age = nowMillis - sourceUpdatedAtMillis;
        return sourceUpdatedAtMillis > 0L && age >= 0L && age <= MAX_SOURCE_AGE_MILLIS;
    }

    boolean isEligible(long nowMillis, ZoneId zoneId) {
        if (!isComplete() || !isSourceFresh(nowMillis) || zoneId == null) {
            return false;
        }
        java.time.ZonedDateTime localNow = Instant.ofEpochMilli(nowMillis).atZone(zoneId);
        int hour = localNow.getHour();
        if (hour < 18) {
            return false;
        }
        LocalDate tomorrow = localNow.toLocalDate().plusDays(1);
        return tomorrow.equals(forecastDate);
    }

    boolean sameDisplay(BreezyWeatherForecast other) {
        return other != null
                && safeEquals(locationId, other.locationId)
                && safeEquals(forecastDate, other.forecastDate)
                && weatherCode == other.weatherCode
                && roundedTemperature(highCelsius) == roundedTemperature(other.highCelsius)
                && roundedTemperature(lowCelsius) == roundedTemperature(other.lowCelsius);
    }

    String formatText(String tomorrowLabel) {
        String label = tomorrowLabel == null || tomorrowLabel.trim().isEmpty()
                ? "Tmr" : tomorrowLabel.trim();
        return label + " " + formatTemperature(highCelsius) + " / "
                + formatTemperature(lowCelsius);
    }

    String displayKey() {
        return locationId + "|" + String.valueOf(forecastDate) + "|"
                + weatherCode + "|"
                + roundedTemperature(highCelsius) + "|" + roundedTemperature(lowCelsius);
    }

    JSONObject toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("locationId", locationId);
            result.put("forecastDate", forecastDate != null ? forecastDate.toString() : "");
            result.put("weatherCode", weatherCode);
            result.put("conditionText", conditionText);
            result.put("highCelsius", highCelsius);
            result.put("lowCelsius", lowCelsius);
            result.put("sourceUpdatedAtMillis", sourceUpdatedAtMillis);
            result.put("daytimeIcon", daytimeIcon);
        } catch (Throwable ignored) {
            // JSONObject only fails for malformed values; the model contains none.
        }
        return result;
    }

    static BreezyWeatherForecast fromJson(JSONObject object, String fallbackLocationId,
            long sourceUpdatedAtMillis, ZoneId zoneId) {
        if (object == null) {
            return empty();
        }
        String locationId = firstString(object, "locationId", "location_id", "placeId",
                "place_id", "cityId", "city_id");
        if (locationId.isEmpty()) {
            locationId = fallbackLocationId;
        }
        LocalDate date = readDate(object, zoneId);
        // Breezy's provider schema is daily[{date, day, night}]. The representative icon must
        // come from the daytime half-day; a night-only record is intentionally incomplete.
        JSONObject day = object.optJSONObject("day");
        JSONObject night = object.optJSONObject("night");
        IconData daytime = readIcon(day,
                new String[]{"weatherCode", "conditionCode", "iconCode", "weatherIconCode"},
                new String[]{"weatherText", "weatherSummary", "condition", "conditionText",
                        "weather", "icon"});
        Temperature high = readTemperature(day, "temperature");
        Temperature low = readTemperature(night, "temperature");
        return new BreezyWeatherForecast(locationId, date, daytime.code, daytime.text,
                high.value, low.value, sourceUpdatedAtMillis, daytime.usable());
    }

    static BreezyWeatherForecast fromJsonString(String json, String fallbackLocationId,
            long sourceUpdatedAtMillis, ZoneId zoneId) {
        if (json == null || json.trim().isEmpty()) {
            return empty();
        }
        try {
            return fromJson(new JSONObject(json), fallbackLocationId, sourceUpdatedAtMillis, zoneId);
        } catch (Throwable ignored) {
            return empty();
        }
    }

    static BreezyWeatherForecast fromRelayJson(JSONObject object) {
        if (object == null) {
            return empty();
        }
        LocalDate date = null;
        String dateText = object.optString("forecastDate", "");
        if (!dateText.isEmpty()) {
            try {
                date = LocalDate.parse(dateText);
            } catch (DateTimeParseException ignored) {
                // Treat malformed cached forecast as incomplete.
            }
        }
        return new BreezyWeatherForecast(
                object.optString("locationId", ""), date,
                object.optInt("weatherCode", UNKNOWN_WEATHER_CODE),
                object.optString("conditionText", ""),
                object.has("highCelsius") ? object.optDouble("highCelsius", Double.NaN) : Double.NaN,
                object.has("lowCelsius") ? object.optDouble("lowCelsius", Double.NaN) : Double.NaN,
                object.optLong("sourceUpdatedAtMillis", 0L),
                object.optBoolean("daytimeIcon", false));
    }

    private static LocalDate readDate(JSONObject object, ZoneId zoneId) {
        String text = firstString(object, "forecastDate", "date", "localDate", "dayDate");
        if (!text.isEmpty()) {
            try {
                return LocalDate.parse(text.trim());
            } catch (DateTimeParseException ignored) {
                try {
                    return Instant.ofEpochMilli(normalizeEpochMillis(Long.parseLong(text.trim())))
                            .atZone(zoneId != null ? zoneId : ZoneId.systemDefault()).toLocalDate();
                } catch (Throwable ignoredAgain) {
                    // Try numeric aliases below.
                }
            }
        }
        for (String key : new String[]{"dateMillis", "timestamp", "time", "epoch"}) {
            Object value = object.opt(key);
            long epoch = epochMillis(value);
            if (epoch > 0L) {
                return Instant.ofEpochMilli(epoch)
                        .atZone(zoneId != null ? zoneId : ZoneId.systemDefault()).toLocalDate();
            }
        }
        return null;
    }

    private static Temperature readTemperature(JSONObject object, String... keys) {
        if (object == null) {
            return new Temperature(Double.NaN);
        }
        for (String key : keys) {
            Object value = object.opt(key);
            Double parsed = temperatureValue(value, "");
            if (parsed != null) {
                return new Temperature(parsed);
            }
        }
        return new Temperature(Double.NaN);
    }

    private static IconData readIcon(JSONObject object, String[] codeKeys, String[] textKeys) {
        if (object == null) {
            return new IconData(UNKNOWN_WEATHER_CODE, "");
        }
        for (String key : codeKeys) {
            Integer code = integerValue(object.opt(key));
            if (code != null) {
                return new IconData(code, "");
            }
        }
        for (String key : textKeys) {
            Object raw = object.opt(key);
            if (raw instanceof JSONObject) {
                IconData nested = readIcon((JSONObject) raw,
                        new String[]{"weatherCode", "conditionCode", "iconCode", "code"},
                        new String[]{"condition", "text", "name", "icon"});
                if (nested.usable()) {
                    return nested;
                }
            } else if (raw != null && !String.valueOf(raw).trim().isEmpty()) {
                String text = String.valueOf(raw).trim();
                Integer code = integerValue(raw);
                return new IconData(code != null ? code : UNKNOWN_WEATHER_CODE, text);
            }
        }
        return new IconData(UNKNOWN_WEATHER_CODE, "");
    }

    private static String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value != null && !(value instanceof JSONObject)
                    && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private static Double temperatureValue(Object value, String inheritedUnit) {
        if (value instanceof Number) {
            double number = ((Number) value).doubleValue();
            return isFinite(number) ? toCelsius(number, inheritedUnit) : null;
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String unit = firstString(object, "unit", "unitId", "temperatureUnit");
            if (unit.isEmpty()) {
                unit = inheritedUnit;
            }
            for (String key : new String[]{"value", "temperature", "temp", "number"}) {
                Double nested = temperatureValue(object.opt(key), unit);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (value instanceof String) {
            try {
                double number = Double.parseDouble(((String) value).trim());
                return isFinite(number) ? toCelsius(number, inheritedUnit) : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer integerValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static long epochMillis(Object value) {
        if (value instanceof Number) {
            return normalizeEpochMillis(((Number) value).longValue());
        }
        if (value instanceof String) {
            try {
                return normalizeEpochMillis(Long.parseLong(((String) value).trim()));
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private static long normalizeEpochMillis(long value) {
        return value > 0L && value < 100_000_000_000L ? value * 1000L : value;
    }

    private static double toCelsius(double value, String unit) {
        String normalizedUnit = unit != null ? unit.trim().toLowerCase(Locale.ROOT) : "";
        if (normalizedUnit.contains("fahrenheit") || normalizedUnit.equals("f")
                || normalizedUnit.equals("°f")) {
            return (value - 32d) * 5d / 9d;
        }
        if (normalizedUnit.contains("kelvin") || normalizedUnit.equals("k")) {
            return value - 273.15d;
        }
        if (normalizedUnit.isEmpty() && value > 170d) {
            return value - 273.15d;
        }
        return value;
    }

    static int roundedTemperature(double celsius) {
        return isFinite(celsius) ? (int) Math.round(celsius) : Integer.MIN_VALUE;
    }

    static String formatTemperature(double celsius) {
        int rounded = roundedTemperature(celsius);
        return rounded == Integer.MIN_VALUE ? "" : rounded + "\u00b0";
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value)
                && value >= -100d && value <= 100d;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeCondition(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    static int representativeWeatherCode(String value) {
        String condition = normalizeCondition(value).toLowerCase(Locale.ROOT);
        if (condition.isEmpty()) {
            return UNKNOWN_WEATHER_CODE;
        }
        if (containsAny(condition, "thunder", "storm", "雷", "闪电")) {
            return 211;
        }
        if (containsAny(condition, "sleet", "freezing", "ice", "snow", "雪", "冰")) {
            return 600;
        }
        if (containsAny(condition, "rain", "drizzle", "shower", "雨", "阵雨")) {
            return 500;
        }
        if (containsAny(condition, "fog", "mist", "haze", "雾", "霾")) {
            return 741;
        }
        if (containsAny(condition, "wind", "breezy", "风")) {
            return 771;
        }
        if (containsAny(condition, "partly cloudy", "partly cloud", "少云")) {
            return 801;
        }
        if (containsAny(condition, "clear", "sunny", "晴", "阳光")) {
            return 800;
        }
        if (containsAny(condition, "cloud", "overcast", "多云", "阴")) {
            return 803;
        }
        return UNKNOWN_WEATHER_CODE;
    }

    private static boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean safeEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static final class Temperature {
        final double value;

        Temperature(double value) {
            this.value = value;
        }
    }

    private static final class IconData {
        final int code;
        final String text;

        IconData(int code, String text) {
            this.code = code;
            this.text = text != null ? text : "";
        }

        boolean usable() {
            return code != UNKNOWN_WEATHER_CODE || !text.isEmpty();
        }
    }
}
