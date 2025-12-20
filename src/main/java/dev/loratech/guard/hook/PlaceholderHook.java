package dev.loratech.guard.hook;

import dev.loratech.guard.LoraGuard;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderHook extends PlaceholderExpansion {

    private final LoraGuard plugin;

    public PlaceholderHook(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "loraguard";
    }

    @Override
    public @NotNull String getAuthor() {
        return "LoraTechnologies";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("total_violations")) {
            return String.valueOf(plugin.getDatabaseManager().getTotalViolations());
        }

        if (params.equalsIgnoreCase("today_violations")) {
            return String.valueOf(plugin.getDatabaseManager().getTodayViolations());
        }

        if (params.equalsIgnoreCase("status")) {
            return plugin.isModEnabled() 
                ? plugin.getLanguageManager().get("gui.main-menu.toggle.enabled") 
                : plugin.getLanguageManager().get("gui.main-menu.toggle.disabled");
        }

        if (params.equalsIgnoreCase("api_status")) {
            return plugin.getApiClient().isApiAvailable() 
                ? plugin.getLanguageManager().get("misc.online") 
                : plugin.getLanguageManager().get("misc.offline");
        }

        if (params.equalsIgnoreCase("cache_size")) {
            return String.valueOf(plugin.getMessageCache().size());
        }

        if (player == null) {
            return null;
        }

        if (params.equalsIgnoreCase("player_violations")) {
            return String.valueOf(plugin.getDatabaseManager().getPlayerViolationPoints(player.getUniqueId()));
        }

        if (params.equalsIgnoreCase("player_muted")) {
            return plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId()) 
                ? plugin.getLanguageManager().get("gui.player-list.yes") 
                : plugin.getLanguageManager().get("gui.player-list.no");
        }

        if (params.equalsIgnoreCase("player_mute_remaining")) {
            if (!plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId())) {
                return "N/A";
            }
            return plugin.getPunishmentManager().getMuteRemainingFormatted(player.getUniqueId());
        }

        return null;
    }
}
