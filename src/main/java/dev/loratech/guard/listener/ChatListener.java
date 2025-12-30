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

        boolean passthrough = plugin.getConfigManager().isPassthroughModeEnabled();

        FilterManager.FilterResult filterResult = plugin.getFilterManager().check(player, message);
        if (!filterResult.isAllowed()) {
            if (!passthrough) {
                event.setCancelled(true);
                player.sendMessage(filterResult.message());
                return;
            }
        }

        if (filterResult.isModified() && !passthrough) {
            message = filterResult.modifiedMessage();
            event.message(net.kyori.adventure.text.Component.text(message));
        }

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            String normalizedMessage = TextUtil.normalizeText(message);
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                String normalizedWord = TextUtil.normalizeText(word);
                // Use word boundaries to avoid false positives (e.g. "ez" inside "yemezler")
                // \\b matches word boundaries
                if (java.util.regex.Pattern.compile("(?i)\\b" + java.util.regex.Pattern.quote(normalizedWord) + "\\b").matcher(normalizedMessage).find()) {
                    if (!passthrough) {
                        event.setCancelled(true);
                        player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.blocked"));
                    }
                    plugin.getPunishmentManager().handleViolation(player, "blacklist", 1.0, message);
                    if (!passthrough) return;
                }
            }
        }

        MessageCache.CachedResult cached = plugin.getMessageCache().get(message);
        if (cached != null) {
            if (cached.flagged()) {
                if (!passthrough) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.blocked"));
                }
                plugin.getPunishmentManager().handleViolation(player, cached.category(), cached.score(), message);
            }
            if (!passthrough) return;
        }

        final String finalMessage = message;
        final boolean debug = plugin.getConfigManager().isDebug();
        final long apiTimeoutMs = plugin.getConfigManager().getApiTimeout();
        
        plugin.getTelemetryManager().recordMessageProcessed();
        long startTime = System.currentTimeMillis();
        
        plugin.getApiClient().moderate(finalMessage).thenAcceptAsync(response -> {
            long responseTime = System.currentTimeMillis() - startTime;
            
            plugin.getTelemetryManager().recordApiCall(response != null, responseTime);
            
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return;
            }

            ModerationResponse.Result result = response.getResults().get(0);
            
            plugin.getMessageCache().put(finalMessage, result);

            if (result.isFlagged()) {
                List<String> enabledCategories = plugin.getConfigManager().getEnabledCategories();
                java.util.Map<String, Double> categoryThresholds = plugin.getConfigManager().getCategoryThresholds();
                double globalThreshold = plugin.getConfigManager().getApiThreshold();
                
                String bestCategory = null;
                double bestScore = -1.0;

                for (java.util.Map.Entry<String, Double> entry : result.getCategoryScores().entrySet()) {
                    String category = entry.getKey();
                    Double score = entry.getValue();

                    if (!enabledCategories.contains(category)) {
                        continue;
                    }

                    double threshold = categoryThresholds.getOrDefault(category, globalThreshold);

                    if (score >= threshold) {
                        if (score > bestScore) {
                            bestScore = score;
                            bestCategory = category;
                        }
                    }
                }

                if (bestCategory != null) {
                    plugin.getPunishmentManager().handleViolation(
                        player,
                        bestCategory,
                        bestScore,
                        finalMessage
                    );
                    plugin.getTelemetryManager().recordViolation(bestCategory);
                }
            }
        }).exceptionally(ex -> {
            long responseTime = System.currentTimeMillis() - startTime;
            plugin.getTelemetryManager().recordApiCall(false, responseTime);
            plugin.getTelemetryManager().getErrorCollector().captureException(ex, "ChatListener.async");
            return null;
        });
    }
}
