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
}
