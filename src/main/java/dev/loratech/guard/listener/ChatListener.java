package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;
import dev.loratech.guard.cache.MessageCache;
import dev.loratech.guard.filter.FilterManager;
import dev.loratech.guard.util.TextUtil;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ChatListener implements Listener {

    private final LoraGuard plugin;
    private static final long API_TIMEOUT_MS = 2000;

    public ChatListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId())) {
            event.setCancelled(true);
            String remaining = plugin.getPunishmentManager().getMuteRemainingFormatted(player.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().getPrefixed("punishments.mute.cannot-speak", 
                "remaining", remaining));
            return;
        }

        if (!plugin.isModEnabled()) {
            return;
        }

        if (player.hasPermission("loraguard.bypass")) {
            return;
        }

        if (plugin.getConfigManager().isWhitelistEnabled()) {
            List<String> whitelist = plugin.getConfigManager().getWhitelistedPlayers();
            if (whitelist.contains(player.getName()) || whitelist.contains(player.getUniqueId().toString())) {
                return;
            }
        }

        if (plugin.getSlowmodeManager().isEnabled()) {
            if (!plugin.getSlowmodeManager().canSendMessage(player.getUniqueId())) {
                event.setCancelled(true);
                int remaining = plugin.getSlowmodeManager().getRemainingCooldown(player.getUniqueId());
                player.sendMessage(plugin.getLanguageManager().getPrefixed("filters.slowmode",
                    "seconds", String.valueOf(remaining)));
                return;
            }
            plugin.getSlowmodeManager().recordMessage(player.getUniqueId());
        }

        FilterManager.FilterResult filterResult = plugin.getFilterManager().check(player, message);
        if (!filterResult.isAllowed()) {
            event.setCancelled(true);
            player.sendMessage(filterResult.message());
            return;
        }

        if (filterResult.isModified()) {
            message = filterResult.modifiedMessage();
            event.message(net.kyori.adventure.text.Component.text(message));
        }

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            String normalizedMessage = TextUtil.normalizeText(message);
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                String normalizedWord = TextUtil.normalizeText(word);
                if (normalizedMessage.contains(normalizedWord)) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.blocked"));
                    plugin.getPunishmentManager().handleViolation(player, "blacklist", 1.0, message);
                    return;
                }
            }
        }

        MessageCache.CachedResult cached = plugin.getMessageCache().get(message);
        if (cached != null) {
            if (cached.flagged()) {
                event.setCancelled(true);
                player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.blocked"));
                plugin.getPunishmentManager().handleViolation(player, cached.category(), cached.score(), message);
            }
            return;
        }

        final String finalMessage = message;
        
        plugin.getTelemetryManager().recordMessageProcessed();
        
        try {
            long startTime = System.currentTimeMillis();
            CompletableFuture<ModerationResponse> future = plugin.getApiClient().moderate(finalMessage);
            ModerationResponse response = future.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long responseTime = System.currentTimeMillis() - startTime;
            
            plugin.getTelemetryManager().recordApiCall(response != null, responseTime);
            
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return;
            }

            ModerationResponse.Result result = response.getResults().get(0);
            
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("[DEBUG] Message: " + finalMessage);
                plugin.getLogger().info("[DEBUG] Flagged: " + result.isFlagged());
                plugin.getLogger().info("[DEBUG] Categories: " + result.getFlaggedCategories());
                plugin.getLogger().info("[DEBUG] Highest: " + result.getHighestCategory() + " (" + result.getHighestScore() + ")");
            }
            
            plugin.getMessageCache().put(finalMessage, result);

            if (result.isFlagged()) {
                List<String> enabledCategories = plugin.getConfigManager().getEnabledCategories();
                List<String> flaggedCategories = result.getFlaggedCategories();
                
                boolean shouldBlock = flaggedCategories.stream().anyMatch(enabledCategories::contains);

                if (shouldBlock) {
                    boolean passthrough = plugin.getConfigManager().isPassthroughModeEnabled();
                    if (!passthrough) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.blocked"));
                    }
                    plugin.getPunishmentManager().handleViolation(
                        player, 
                        result.getHighestCategory(), 
                        result.getHighestScore(), 
                        finalMessage
                    );
                    plugin.getTelemetryManager().recordViolation(result.getHighestCategory());
                }
            }
        } catch (Exception e) {
            plugin.getTelemetryManager().recordApiCall(false, API_TIMEOUT_MS);
            plugin.getTelemetryManager().getErrorCollector().captureException(e, "ChatListener.onChat");
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("API check timeout/error for message: " + e.getMessage());
            }
        }
    }
}
