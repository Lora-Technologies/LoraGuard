package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;

import java.util.HashMap;
import java.util.Map;

public class ConfigStatsCollector {

    private final LoraGuard plugin;

    public ConfigStatsCollector(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public TelemetryEvent collectConfigStats() {
        Map<String, Boolean> features = new HashMap<>();
        features.put("anti_spam", plugin.getConfigManager().isAntiSpamEnabled());
        features.put("anti_flood", plugin.getConfigManager().isAntiFloodEnabled());
        features.put("link_filter", plugin.getConfigManager().isLinkFilterEnabled());
        features.put("ip_filter", plugin.getConfigManager().isIpFilterEnabled());
        features.put("caps_filter", plugin.getConfigManager().isCapsLockEnabled());
        features.put("blacklist", plugin.getConfigManager().isBlacklistEnabled());
        features.put("discord_hook", plugin.getConfigManager().isDiscordEnabled());
        features.put("appeal_system", plugin.getConfigManager().isAppealSystemEnabled());
        features.put("slowmode", plugin.getConfigManager().isSlowmodeEnabled());
        features.put("sign_moderation", plugin.getConfigManager().isSignModerationEnabled());
        features.put("book_moderation", plugin.getConfigManager().isBookModerationEnabled());
        features.put("anvil_moderation", plugin.getConfigManager().isAnvilModerationEnabled());
        features.put("command_spy", plugin.getConfigManager().isCommandSpyEnabled());
        features.put("whitelist", plugin.getConfigManager().isWhitelistEnabled());
        features.put("cache", plugin.getConfigManager().isCacheEnabled());
        features.put("circuit_breaker", plugin.getConfigManager().isCircuitBreakerEnabled());
        features.put("external_commands", plugin.getConfigManager().isExternalCommandsEnabled());
        features.put("passthrough_mode", plugin.getConfigManager().isPassthroughModeEnabled());
        features.put("warning_decay", plugin.getConfigManager().isWarningDecayEnabled());
        features.put("bulk_operations", plugin.getConfigManager().isBulkOperationsEnabled());
        
        Map<String, Object> settings = new HashMap<>();
        settings.put("api_model", plugin.getConfigManager().getApiModel());
        settings.put("api_threshold", plugin.getConfigManager().getApiThreshold());
        settings.put("api_timeout", plugin.getConfigManager().getApiTimeout());
        settings.put("database_type", plugin.getConfigManager().getDatabaseType());
        settings.put("language", plugin.getConfigManager().getLanguage());
        settings.put("enabled_categories_count", plugin.getConfigManager().getEnabledCategories().size());
        settings.put("blacklist_words_count", plugin.getConfigManager().getBlacklistedWords().size());
        settings.put("whitelisted_players_count", plugin.getConfigManager().getWhitelistedPlayers().size());
        settings.put("link_whitelist_count", plugin.getConfigManager().getLinkWhitelist().size());
        
        return new TelemetryEvent(TelemetryEvent.EventType.CONFIG)
            .addData("enabled_features", features)
            .addData("settings", settings);
    }
}
