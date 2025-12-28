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

public class ChatListener implements Listener {

    private final LoraGuard plugin;

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
        final boolean debug = plugin.getConfigManager().isDebug();
        final long apiTimeoutMs = plugin.getConfigManager().getApiTimeout();
        
        if (debug) {
            plugin.getLogger().info("[DEBUG-CHAT] ========== PROCESSING MESSAGE ==========");
            plugin.getLogger().info("[DEBUG-CHAT] Player: " + player.getName() + " (" + player.getUniqueId() + ")");
            plugin.getLogger().info("[DEBUG-CHAT] Message: \"" + finalMessage + "\"");
            plugin.getLogger().info("[DEBUG-CHAT] API Timeout: " + apiTimeoutMs + "ms");
            plugin.getLogger().info("[DEBUG-CHAT] API Available: " + plugin.getApiClient().isApiAvailable());
        }
        
        plugin.getTelemetryManager().recordMessageProcessed();
        
        long startTime = System.currentTimeMillis();
        
        if (debug) {
            plugin.getLogger().info("[DEBUG-CHAT] Calling API at: " + startTime);
        }
        
        plugin.getApiClient().moderate(finalMessage).thenAcceptAsync(response -> {
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (debug) {
                plugin.getLogger().info("[DEBUG-CHAT] API Response Time: " + responseTime + "ms");
                plugin.getLogger().info("[DEBUG-CHAT] Response: " + (response != null ? "received" : "null"));
            }
            
            plugin.getTelemetryManager().recordApiCall(response != null, responseTime);
            
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                if (debug) {
                    plugin.getLogger().warning("[DEBUG-CHAT] Empty response - no action taken");
                }
                return;
            }

            ModerationResponse.Result result = response.getResults().get(0);
            
            if (debug) {
                plugin.getLogger().info("[DEBUG-CHAT] ========== MODERATION RESULT ==========");
                plugin.getLogger().info("[DEBUG-CHAT] Flagged: " + result.isFlagged());
                plugin.getLogger().info("[DEBUG-CHAT] Flagged Categories: " + result.getFlaggedCategories());
                plugin.getLogger().info("[DEBUG-CHAT] Highest: " + result.getHighestCategory() + " (" + result.getHighestScore() + ")");
                plugin.getLogger().info("[DEBUG-CHAT] Enabled Categories: " + plugin.getConfigManager().getEnabledCategories());
            }
            
            plugin.getMessageCache().put(finalMessage, result);

            if (result.isFlagged()) {
                List<String> enabledCategories = plugin.getConfigManager().getEnabledCategories();
                List<String> flaggedCategories = result.getFlaggedCategories();
                
                boolean shouldBlock = flaggedCategories.stream().anyMatch(enabledCategories::contains);

                if (debug) {
                    plugin.getLogger().info("[DEBUG-CHAT] Should Block: " + shouldBlock);
                }

                if (shouldBlock) {
                    if (debug) {
                        plugin.getLogger().info("[DEBUG-CHAT] Violation recorded (async mode)");
                    }
                    
                    plugin.getPunishmentManager().handleViolation(
                        player, 
                        result.getHighestCategory(), 
                        result.getHighestScore(), 
                        finalMessage
                    );
                    plugin.getTelemetryManager().recordViolation(result.getHighestCategory());
                } else {
                    if (debug) {
                        plugin.getLogger().info("[DEBUG-CHAT] Flagged but category not enabled - no action");
                    }
                }
            } else {
                if (debug) {
                    plugin.getLogger().info("[DEBUG-CHAT] Not flagged - no action");
                }
            }
        }).exceptionally(ex -> {
            long responseTime = System.currentTimeMillis() - startTime;
            if (debug) {
                plugin.getLogger().warning("[DEBUG-CHAT] ========== ASYNC ERROR ==========");
                plugin.getLogger().warning("[DEBUG-CHAT] Type: " + ex.getClass().getSimpleName());
                plugin.getLogger().warning("[DEBUG-CHAT] Message: " + ex.getMessage());
            }
            plugin.getTelemetryManager().recordApiCall(false, responseTime);
            plugin.getTelemetryManager().getErrorCollector().captureException(ex, "ChatListener.async");
            return null;
        });
    }
}
