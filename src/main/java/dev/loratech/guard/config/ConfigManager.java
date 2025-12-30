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

    public Map<String, Double> getCategoryThresholds() {
        Map<String, Double> map = new HashMap<>();
        if (config.getConfigurationSection("punishments.category-thresholds") != null) {
            for (String key : config.getConfigurationSection("punishments.category-thresholds").getKeys(false)) {
                map.put(key, config.getDouble("punishments.category-thresholds." + key, 0.5));
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

    public int getViolationDecayAmount() {
        return config.getInt("punishments.violation-decay-amount", 1);
    }

    public int getViolationDecayCheckMinutes() {
        return config.getInt("punishments.violation-decay-check-minutes", 60);
    }

    public int getReportCooldownSeconds() {
        return config.getInt("report.cooldown-seconds", 60);
    }

    public boolean isAppealSystemEnabled() {
        return config.getBoolean("appeal.enabled", true);
    }

    public int getAppealCooldownSeconds() {
        return config.getInt("appeal.cooldown-seconds", 3600);
    }

    public boolean isUnmuteNotificationSoundEnabled() {
        return config.getBoolean("notifications.unmute-sound-enabled", true);
    }

    public String getUnmuteNotificationSound() {
        return config.getString("notifications.unmute-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public boolean isBulkOperationsEnabled() {
        return config.getBoolean("bulk-operations.enabled", true);
    }

    public int getBulkOperationsLimit() {
        return config.getInt("bulk-operations.max-players", 50);
    }

    public boolean isIpFilterEnabled() {
        return config.getBoolean("filters.ip.enabled", true);
    }

    public List<String> getIpWhitelist() {
        return config.getStringList("filters.ip.whitelist");
    }

    public boolean isSignModerationEnabled() {
        return config.getBoolean("moderation.sign.enabled", true);
    }

    public boolean isBookModerationEnabled() {
        return config.getBoolean("moderation.book.enabled", true);
    }

    public boolean isAnvilModerationEnabled() {
        return config.getBoolean("moderation.anvil.enabled", true);
    }

    public boolean isCommandSpyEnabled() {
        return config.getBoolean("moderation.command-spy.enabled", true);
    }

    public boolean isSlowmodeEnabled() {
        return config.getBoolean("slowmode.enabled", false);
    }

    public int getSlowmodeDelay() {
        return config.getInt("slowmode.delay-seconds", 3);
    }

    public String getStaffChatPermission() {
        return config.getString("staffchat.permission", "loraguard.staffchat");
    }

    public boolean isMetricsEnabled() {
        return config.getBoolean("metrics.enabled", true);
    }

    public boolean isWarningDecayEnabled() {
        return config.getBoolean("punishments.warning-decay.enabled", true);
    }

    public int getWarningDecayHours() {
        return config.getInt("punishments.warning-decay.hours", 168);
    }

    public int getWarningDecayAmount() {
        return config.getInt("punishments.warning-decay.amount", 1);
    }

    public int getWarningDecayCheckMinutes() {
        return config.getInt("punishments.warning-decay.check-minutes", 60);
    }

    public boolean isPassthroughModeEnabled() {
        return config.getBoolean("punishments.passthrough-mode", false);
    }

    public boolean isExternalCommandsEnabled() {
        return config.getBoolean("punishments.external-commands.enabled", false);
    }

    public String getExternalMuteCommand() {
        return config.getString("punishments.external-commands.mute", "");
    }

    public String getExternalUnmuteCommand() {
        return config.getString("punishments.external-commands.unmute", "");
    }

    public String getExternalBanCommand() {
        return config.getString("punishments.external-commands.ban", "");
    }

    public String getExternalUnbanCommand() {
        return config.getString("punishments.external-commands.unban", "");
    }

    public String getExternalKickCommand() {
        return config.getString("punishments.external-commands.kick", "");
    }

    public String getCategoryDisplayName(String category) {
        String displayName = config.getString("categories.display-names." + category);
        return displayName != null ? displayName : category;
    }

    public Map<String, String> getCategoryDisplayNames() {
        Map<String, String> names = new HashMap<>();
        if (config.getConfigurationSection("categories.display-names") != null) {
            for (String key : config.getConfigurationSection("categories.display-names").getKeys(false)) {
                names.put(key, config.getString("categories.display-names." + key, key));
            }
        }
        return names;
    }

    public boolean isTelemetryEnabled() {
        return config.getBoolean("telemetry.enabled", true);
    }

    public int getTelemetrySendInterval() {
        return config.getInt("telemetry.send-interval-minutes", 10);
    }

    public boolean isTelemetryErrorsEnabled() {
        return config.getBoolean("telemetry.collect.errors", true);
    }

    public boolean isTelemetryPerformanceEnabled() {
        return config.getBoolean("telemetry.collect.performance", true);
    }

    public boolean isTelemetryUsageEnabled() {
        return config.getBoolean("telemetry.collect.usage-stats", true);
    }

    public long getTelemetrySlowThresholdMs() {
        return config.getLong("telemetry.slow-threshold-ms", 1000);
    }
}
