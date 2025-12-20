package dev.loratech.guard.util;

import org.bukkit.ChatColor;

import java.util.concurrent.TimeUnit;

public final class TextUtil {

    private TextUtil() {}

    public static String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String stripColor(String text) {
        return ChatColor.stripColor(text);
    }

    public static String formatDuration(long seconds) {
        if (seconds < 0) {
            return "Permanent";
        }

        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static int parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        input = input.toLowerCase().trim();
        
        if (input.equals("permanent") || input.equals("perm")) {
            return -1;
        }

        int total = 0;
        StringBuilder num = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else if (num.length() > 0) {
                int value = Integer.parseInt(num.toString());
                total += switch (c) {
                    case 's' -> value;
                    case 'm' -> value * 60;
                    case 'h' -> value * 3600;
                    case 'd' -> value * 86400;
                    case 'w' -> value * 604800;
                    default -> value;
                };
                num = new StringBuilder();
            }
        }

        if (num.length() > 0) {
            total += Integer.parseInt(num.toString()) * 60;
        }

        return total;
    }

    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }

    private static final java.util.Map<Character, Character> LEET_MAP = new java.util.HashMap<>();
    private static final java.util.Map<Character, Character> TURKISH_MAP = new java.util.HashMap<>();

    static {
        LEET_MAP.put('4', 'a');
        LEET_MAP.put('@', 'a');
        LEET_MAP.put('8', 'b');
        LEET_MAP.put('3', 'e');
        LEET_MAP.put('€', 'e');
        LEET_MAP.put('1', 'i');
        LEET_MAP.put('!', 'i');
        LEET_MAP.put('|', 'i');
        LEET_MAP.put('0', 'o');
        LEET_MAP.put('$', 's');
        LEET_MAP.put('5', 's');
        LEET_MAP.put('7', 't');
        LEET_MAP.put('+', 't');
        LEET_MAP.put('2', 'z');
        LEET_MAP.put('6', 'g');
        LEET_MAP.put('9', 'g');

        TURKISH_MAP.put('ı', 'i');
        TURKISH_MAP.put('İ', 'i');
        TURKISH_MAP.put('ğ', 'g');
        TURKISH_MAP.put('Ğ', 'g');
        TURKISH_MAP.put('ü', 'u');
        TURKISH_MAP.put('Ü', 'u');
        TURKISH_MAP.put('ş', 's');
        TURKISH_MAP.put('Ş', 's');
        TURKISH_MAP.put('ö', 'o');
        TURKISH_MAP.put('Ö', 'o');
        TURKISH_MAP.put('ç', 'c');
        TURKISH_MAP.put('Ç', 'c');
    }

    public static String normalizeText(String text) {
        if (text == null || text.isEmpty()) return text;

        StringBuilder normalized = new StringBuilder();
        char lastChar = 0;

        for (char c : text.toCharArray()) {
            char lower = Character.toLowerCase(c);

            if (LEET_MAP.containsKey(lower)) {
                lower = LEET_MAP.get(lower);
            }

            if (TURKISH_MAP.containsKey(c)) {
                lower = TURKISH_MAP.get(c);
            }

            if (Character.isLetterOrDigit(lower)) {
                if (lower != lastChar || !Character.isLetter(lower)) {
                    normalized.append(lower);
                    lastChar = lower;
                }
            } else if (Character.isWhitespace(c)) {
                if (lastChar != ' ') {
                    normalized.append(' ');
                    lastChar = ' ';
                }
            }
        }

        return normalized.toString().trim();
    }

    public static String removeSpecialChars(String text) {
        if (text == null) return null;
        return text.replaceAll("[^a-zA-Z0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    public static boolean containsRepeatedChars(String text, int threshold) {
        if (text == null || text.length() < threshold) return false;
        int count = 1;
        char last = 0;
        for (char c : text.toCharArray()) {
            if (c == last) {
                count++;
                if (count >= threshold) return true;
            } else {
                count = 1;
                last = c;
            }
        }
        return false;
    }
}
