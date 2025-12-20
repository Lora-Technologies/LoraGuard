package dev.loratech.guard.metrics;

import dev.loratech.guard.LoraGuard;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

public class MetricsManager {

    private static final int BSTATS_PLUGIN_ID = 28450;
    private final LoraGuard plugin;
    private Metrics metrics;

    public MetricsManager(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfigManager().isMetricsEnabled()) {
            return;
        }

        try {
            metrics = new Metrics(plugin, BSTATS_PLUGIN_ID);

            metrics.addCustomChart(new SimplePie("database_type", () -> 
                plugin.getConfigManager().getDatabaseType()));

            metrics.addCustomChart(new SimplePie("language", () -> 
                plugin.getConfigManager().getLanguage()));

            metrics.addCustomChart(new SimplePie("api_model", () -> 
                plugin.getConfigManager().getApiModel()));

            metrics.addCustomChart(new SimplePie("discord_enabled", () -> 
                plugin.getConfigManager().isDiscordEnabled() ? "Yes" : "No"));

            metrics.addCustomChart(new SimplePie("appeal_system_enabled", () -> 
                plugin.getConfigManager().isAppealSystemEnabled() ? "Yes" : "No"));

            metrics.addCustomChart(new SingleLineChart("total_violations", () -> 
                plugin.getDatabaseManager().getTotalViolations()));

            plugin.getLogger().info("bStats metrics enabled!");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize bStats: " + e.getMessage());
        }
    }
}
