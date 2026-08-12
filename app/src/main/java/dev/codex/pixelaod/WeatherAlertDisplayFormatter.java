package dev.codex.pixelaod;

import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic, offline presentation formatter for structured Breezy weather alerts. */
final class WeatherAlertDisplayFormatter {
    private static final Map<String, String> CHINESE_HAZARDS = chineseHazards();

    private WeatherAlertDisplayFormatter() {
    }

    static String format(BreezyWeatherAlert alert) {
        if (alert == null || alert.isEmpty()) {
            return "";
        }
        String headline = alert.headline != null ? alert.headline.trim() : "";
        if (headline.isEmpty() || !containsCjk(headline)) {
            return headline;
        }

        String hazard = hazardFor(headline);
        if (hazard.isEmpty()) {
            // Never guess an unknown official warning type. A future optional on-device ML Kit
            // fallback may translate this branch, but the deterministic path preserves source text.
            return headline;
        }

        String level = severityLabel(alert.severity, headline);
        return level.isEmpty()
                ? "Weather alert for " + hazard
                : level + " alert for " + hazard;
    }

    static String hazardFor(String headline) {
        if (headline == null || headline.trim().isEmpty()) {
            return "";
        }
        for (Map.Entry<String, String> entry : CHINESE_HAZARDS.entrySet()) {
            if (headline.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    static String severityLabel(int severity, String headline) {
        if (severity >= 4) return "Red";
        if (severity == 3) return "Orange";
        if (severity == 2) return "Yellow";
        if (severity == 1) return "Blue";
        return severityFromChineseColor(headline);
    }

    private static String severityFromChineseColor(String headline) {
        String value = headline != null ? headline : "";
        if (value.contains("红色") || value.contains("红色预警")) return "Red";
        if (value.contains("橙色") || value.contains("橘色") || value.contains("橘黄色")) {
            return "Orange";
        }
        if (value.contains("黄色") || value.contains("黄色预警")) return "Yellow";
        if (value.contains("蓝色") || value.contains("蓝色预警")) return "Blue";
        return "";
    }

    private static boolean containsCjk(String value) {
        for (int i = 0; i < value.length(); i++) {
            Character.UnicodeScript script = Character.UnicodeScript.of(value.charAt(i));
            if (script == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> chineseHazards() {
        // Longest/specific forms must come before their shorter substrings.
        LinkedHashMap<String, String> hazards = new LinkedHashMap<>();
        hazards.put("地质灾害气象风险", "weather-related geological hazards");
        hazards.put("雷暴大风", "severe thunderstorms and strong winds");
        hazards.put("强对流", "severe convective weather");
        hazards.put("道路结冰", "road icing");
        hazards.put("森林火险", "forest fire danger");
        hazards.put("山洪", "flash floods");
        hazards.put("特大暴雨", "extreme torrential rain");
        hazards.put("大暴雨", "torrential rain");
        hazards.put("暴雨", "rainstorms");
        hazards.put("暴雪", "snowstorms");
        hazards.put("沙尘暴", "sandstorms");
        hazards.put("雷电", "thunderstorms");
        hazards.put("冰雹", "hail");
        hazards.put("寒潮", "cold waves");
        hazards.put("大风", "strong winds");
        hazards.put("高温", "extreme heat");
        hazards.put("干旱", "drought");
        hazards.put("台风", "typhoons");
        hazards.put("霜冻", "frost");
        hazards.put("大雾", "fog");
        hazards.put("霾", "haze");
        hazards.put("低温", "low temperatures");
        hazards.put("寒冷", "cold weather");
        return hazards;
    }
}
