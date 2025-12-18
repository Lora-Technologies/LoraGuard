package dev.loratech.guard.config;

import dev.loratech.guard.LoraGuard;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {

    private final LoraGuard plugin;
    private FileConfiguration config;

    public ConfigManager(LoraGuard plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public String getApiKey() {
        return config.getString("api.key", "");
    }

    public String getApiBaseUrl() {
        return config.getString("api.base-url", "https://api.loratech.dev/v1");
    }

    public String getApiModel() {
        return config.getString("api.model", "gemini-2.5-flash");
    }

    public double getApiThreshold() {
        return config.getDouble("api.threshold", 0.5);
    }

    public int getApiTimeout() {
        return config.getInt("api.timeout", 3000);
    }

    public boolean isCircuitBreakerEnabled() {
        return config.getBoolean("api.circuit-breaker.enabled", true);
    }

    public int getCircuitBreakerFailureThreshold() {
        return config.getInt("api.circuit-breaker.failure-threshold", 5);
    }

    public int getCircuitBreakerResetSeconds() {
        return config.getInt("api.circuit-breaker.reset-seconds", 60);
    }

    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getDatabaseHost() {
        return config.getString("database.host", "localhost");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return config.getString("database.database", "loraguard");
    }

    public String getDatabaseUser() {
        return config.getString("database.username", "root");
    }

    public String getDatabasePassword() {
        return config.getString("database.password", "");
    }

    public int getDatabasePoolSize() {
        return config.getInt("database.pool-size", 10);
    }

    public int getDatabaseConnectionTimeout() {
        return config.getInt("database.connection-timeout", 5000);
    }

    public int getDatabaseIdleTimeout() {
        return config.getInt("database.idle-timeout", 600000);
    }

    public int getDatabaseMaxLifetime() {
        return config.getInt("database.max-lifetime", 1800000);
    }

    public boolean isCacheEnabled() {
        return config.getBoolean("cache.enabled", true);
    }

    public int getCacheExpireMinutes() {
        return config.getInt("cache.expire-minutes", 30);
    }

    public int getCacheMaxSize() {
        return config.getInt("cache.max-size", 1000);
    }

    public Map<Integer, String> getEscalationPunishments() {
        Map<Integer, String> map = new HashMap<>();
        if (config.getConfigurationSection("punishments.escalation") != null) {
            for (String key : config.getConfigurationSection("punishments.escalation").getKeys(false)) {
                try {
                    int threshold = Integer.parseInt(key);
                    String punishment = config.getString("punishments.escalation." + key);
                    map.put(threshold, punishment);
                } catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    public Map<String, Integer> getCategoryWeights() {
        Map<String, Integer> map = new HashMap<>();
        if (config.getConfigurationSection("punishments.category-weights") != null) {
            for (String key : config.getConfigurationSection("punishments.category-weights").getKeys(false)) {
                map.put(key, config.getInt("punishments.category-weights." + key, 1));
            }
        }
        return map;
    }

    public int getViolationCooldownHours() {
        return config.getInt("punishments.violation-cooldown-hours", 24);
    }

    public List<String> getEnabledCategories() {
        return config.getStringList("categories.enabled");
    }

    public boolean isBlacklistEnabled() {
        return config.getBoolean("blacklist.enabled", true);
    }

    public List<String> getBlacklistedWords() {
        return config.getStringList("blacklist.words");
    }

    public boolean isAntiSpamEnabled() {
        return config.getBoolean("filters.anti-spam.enabled", true);
    }

    public int getAntiSpamMaxMessages() {
        return config.getInt("filters.anti-spam.max-same-messages", 3);
    }

    public int getAntiSpamTimeframe() {
        return config.getInt("filters.anti-spam.timeframe-seconds", 10);
    }

    public boolean isAntiFloodEnabled() {
        return config.getBoolean("filters.anti-flood.enabled", true);
    }

    public int getAntiFloodMaxMessages() {
        return config.getInt("filters.anti-flood.max-messages", 5);
    }

    public int getAntiFloodTimeframe() {
        return config.getInt("filters.anti-flood.timeframe-seconds", 3);
    }

    public boolean isCapsLockEnabled() {
        return config.getBoolean("filters.caps-lock.enabled", true);
    }

    public int getCapsLockMaxPercentage() {
        return config.getInt("filters.caps-lock.max-percentage", 70);
    }

    public int getCapsLockMinLength() {
        return config.getInt("filters.caps-lock.min-length", 5);
    }

    public String getCapsLockAction() {
        return config.getString("filters.caps-lock.action", "lowercase");
    }

    public boolean isLinkFilterEnabled() {
        return config.getBoolean("filters.links.enabled", true);
    }

    public List<String> getLinkWhitelist() {
        return config.getStringList("filters.links.whitelist");
    }

    public String getLinkAction() {
        return config.getString("filters.links.action", "block");
    }

    public boolean isStaffAlertEnabled() {
        return config.getBoolean("notifications.staff-alert", true);
    }

    public String getStaffPermission() {
        return config.getString("notifications.staff-permission", "loraguard.notify");
    }

    public String getAlertSound() {
        return config.getString("notifications.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
    }

    public boolean isDiscordEnabled() {
        return config.getBoolean("notifications.discord.enabled", false);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("notifications.discord.webhook-url", "");
    }

    public String getDiscordEmbedColor() {
        return config.getString("notifications.discord.embed-color", "#FF0000");
    }

    public boolean isWhitelistEnabled() {
        return config.getBoolean("whitelist.enabled", true);
    }

    public List<String> getWhitelistedPlayers() {
        return config.getStringList("whitelist.players");
    }

    public void addWhitelistedPlayer(String playerName) {
        List<String> players = getWhitelistedPlayers();
        if (!players.contains(playerName)) {
            players.add(playerName);
            config.set("whitelist.players", players);
            plugin.saveConfig();
        }
    }

    public void removeWhitelistedPlayer(String playerName) {
        List<String> players = getWhitelistedPlayers();
        players.remove(playerName);
        config.set("whitelist.players", players);
        plugin.saveConfig();
    }

    public String getLanguage() {
        return config.getString("language", "tr");
    }

    public void setLanguage(String language) {
        config.set("language", language);
        plugin.saveConfig();
    }

    public boolean isDebug() {
        return config.getBoolean("debug", false);
    }
}
