package dev.loratech.guard.filter;

import dev.loratech.guard.LoraGuard;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

public class FilterManager {

    private final LoraGuard plugin;
    private final Map<UUID, List<MessageRecord>> messageHistory;
    private static final Pattern URL_PATTERN = Pattern.compile(
        "(https?://)?([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern IP_PATTERN = Pattern.compile(
        "\\b(?:\\d{1,3}[.,]){3}\\d{1,3}(?::\\d{1,5})?\\b"
    );
    private static final Pattern DISCORD_INVITE_PATTERN = Pattern.compile(
        "(discord\\.gg|discordapp\\.com/invite)/[a-zA-Z0-9]+",
        Pattern.CASE_INSENSITIVE
    );

    public FilterManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.messageHistory = new ConcurrentHashMap<>();
    }

    public FilterResult check(Player player, String message) {
        if (plugin.getConfigManager().isAntiSpamEnabled()) {
            FilterResult spamResult = checkSpam(player, message);
            if (!spamResult.isAllowed()) {
                return spamResult;
            }
        }

        if (plugin.getConfigManager().isAntiFloodEnabled()) {
            FilterResult floodResult = checkFlood(player);
            if (!floodResult.isAllowed()) {
                return floodResult;
            }
        }

        if (plugin.getConfigManager().isLinkFilterEnabled()) {
            FilterResult linkResult = checkLinks(message);
            if (!linkResult.isAllowed()) {
                return linkResult;
            }
        }

        if (plugin.getConfigManager().isIpFilterEnabled()) {
            FilterResult ipResult = checkIpAddress(message);
            if (!ipResult.isAllowed()) {
                return ipResult;
            }
        }

        if (plugin.getConfigManager().isCapsLockEnabled()) {
            FilterResult capsResult = checkCapsLock(message);
            if (!capsResult.isAllowed()) {
                return capsResult;
            }
        }

        recordMessage(player.getUniqueId(), message);
        return FilterResult.allow();
    }

    private FilterResult checkSpam(Player player, String message) {
        List<MessageRecord> history = getOrCreateHistory(player.getUniqueId());
        cleanOldMessages(history, plugin.getConfigManager().getAntiSpamTimeframe());

        String normalizedMessage = message.toLowerCase().trim();
        long sameMessageCount = history.stream()
            .filter(record -> record.message().toLowerCase().trim().equals(normalizedMessage))
            .count();

        if (sameMessageCount >= plugin.getConfigManager().getAntiSpamMaxMessages()) {
            return FilterResult.deny(FilterType.SPAM, plugin.getLanguageManager().getPrefixed("filters.spam"));
        }

        return FilterResult.allow();
    }

    private FilterResult checkFlood(Player player) {
        List<MessageRecord> history = getOrCreateHistory(player.getUniqueId());
        cleanOldMessages(history, plugin.getConfigManager().getAntiFloodTimeframe());

        if (history.size() >= plugin.getConfigManager().getAntiFloodMaxMessages()) {
            return FilterResult.deny(FilterType.FLOOD, plugin.getLanguageManager().getPrefixed("filters.flood"));
        }

        return FilterResult.allow();
    }

    private FilterResult checkLinks(String message) {
        if (!URL_PATTERN.matcher(message).find()) {
            return FilterResult.allow();
        }

        List<String> whitelist = plugin.getConfigManager().getLinkWhitelist();
        for (String allowed : whitelist) {
            if (message.toLowerCase().contains(allowed.toLowerCase())) {
                return FilterResult.allow();
            }
        }

        String action = plugin.getConfigManager().getLinkAction();
        if ("block".equalsIgnoreCase(action)) {
            return FilterResult.deny(FilterType.LINK, plugin.getLanguageManager().getPrefixed("filters.link"));
        }

        return FilterResult.allow();
    }

    private FilterResult checkIpAddress(String message) {
        if (IP_PATTERN.matcher(message).find()) {
            List<String> whitelist = plugin.getConfigManager().getIpWhitelist();
            for (String allowed : whitelist) {
                if (message.contains(allowed)) {
                    return FilterResult.allow();
                }
            }
            return FilterResult.deny(FilterType.IP, plugin.getLanguageManager().getPrefixed("filters.ip"));
        }
        return FilterResult.allow();
    }

    private FilterResult checkCapsLock(String message) {
        if (message.length() < plugin.getConfigManager().getCapsLockMinLength()) {
            return FilterResult.allow();
        }

        long upperCount = message.chars().filter(Character::isUpperCase).count();
        long letterCount = message.chars().filter(Character::isLetter).count();

        if (letterCount == 0) {
            return FilterResult.allow();
        }

        double percentage = (upperCount * 100.0) / letterCount;

        if (percentage > plugin.getConfigManager().getCapsLockMaxPercentage()) {
            String action = plugin.getConfigManager().getCapsLockAction();
            if ("lowercase".equalsIgnoreCase(action)) {
                return FilterResult.modify(FilterType.CAPS, message.toLowerCase());
            } else if ("block".equalsIgnoreCase(action)) {
                return FilterResult.deny(FilterType.CAPS, plugin.getLanguageManager().getPrefixed("filters.caps"));
            }
        }

        return FilterResult.allow();
    }

    private List<MessageRecord> getOrCreateHistory(UUID uuid) {
        return messageHistory.computeIfAbsent(uuid, k -> new CopyOnWriteArrayList<>());
    }

    private void recordMessage(UUID uuid, String message) {
        getOrCreateHistory(uuid).add(new MessageRecord(message, System.currentTimeMillis()));
    }

    private void cleanOldMessages(List<MessageRecord> history, int seconds) {
        long threshold = System.currentTimeMillis() - (seconds * 1000L);
        history.removeIf(record -> record.timestamp() < threshold);
    }

    public void clearHistory(UUID uuid) {
        messageHistory.remove(uuid);
    }

    public void clearAllHistory() {
        messageHistory.clear();
    }

    public String getLastMessage(UUID uuid) {
        List<MessageRecord> history = messageHistory.get(uuid);
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1).message();
    }

    private record MessageRecord(String message, long timestamp) {}

    public enum FilterType {
        SPAM, FLOOD, LINK, CAPS, BLACKLIST, IP, DISCORD
    }

    public record FilterResult(boolean isAllowed, FilterType type, String message, String modifiedMessage) {
        public static FilterResult allow() {
            return new FilterResult(true, null, null, null);
        }

        public static FilterResult deny(FilterType type, String message) {
            return new FilterResult(false, type, message, null);
        }

        public static FilterResult modify(FilterType type, String modifiedMessage) {
            return new FilterResult(true, type, null, modifiedMessage);
        }

        public boolean isModified() {
            return modifiedMessage != null;
        }
    }
}
