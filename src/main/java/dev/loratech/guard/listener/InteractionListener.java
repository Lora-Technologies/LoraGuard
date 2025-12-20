package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;
import dev.loratech.guard.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class InteractionListener implements Listener {

    private final LoraGuard plugin;
    private static final long API_TIMEOUT_MS = 2000;

    public InteractionListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!plugin.isModEnabled()) return;
        if (!plugin.getConfigManager().isSignModerationEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("loraguard.bypass")) return;

        StringBuilder signText = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            String line = event.getLine(i);
            if (line != null && !line.isEmpty()) {
                signText.append(line).append(" ");
            }
        }

        String text = signText.toString().trim();
        if (text.isEmpty()) return;

        String normalizedText = TextUtil.normalizeText(text);

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                if (normalizedText.toLowerCase().contains(word.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.sign-blocked"));
                    plugin.getPunishmentManager().handleViolation(player, "blacklist", 1.0, text);
                    return;
                }
            }
        }

        checkWithApi(player, text, "sign", () -> {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.sign-blocked"));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBookEdit(PlayerEditBookEvent event) {
        if (!plugin.isModEnabled()) return;
        if (!plugin.getConfigManager().isBookModerationEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("loraguard.bypass")) return;

        BookMeta meta = event.getNewBookMeta();
        StringBuilder bookContent = new StringBuilder();

        if (meta.hasTitle()) {
            bookContent.append(meta.getTitle()).append(" ");
        }

        for (String page : meta.getPages()) {
            bookContent.append(page).append(" ");
        }

        String text = bookContent.toString().trim();
        if (text.isEmpty()) return;

        String normalizedText = TextUtil.normalizeText(text);

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                if (normalizedText.toLowerCase().contains(word.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.book-blocked"));
                    plugin.getPunishmentManager().handleViolation(player, "blacklist", 1.0, TextUtil.truncate(text, 100));
                    return;
                }
            }
        }

        checkWithApi(player, TextUtil.truncate(text, 500), "book", () -> {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.book-blocked"));
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnvilRename(PrepareAnvilEvent event) {
        if (!plugin.isModEnabled()) return;
        if (!plugin.getConfigManager().isAnvilModerationEnabled()) return;

        if (!(event.getView().getPlayer() instanceof Player player)) return;
        if (player.hasPermission("loraguard.bypass")) return;

        ItemMeta resultMeta = event.getResult() != null ? event.getResult().getItemMeta() : null;
        if (resultMeta == null || !resultMeta.hasDisplayName()) return;

        String itemName = resultMeta.getDisplayName();
        if (itemName.isEmpty()) return;

        String normalizedText = TextUtil.normalizeText(itemName);

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                if (normalizedText.toLowerCase().contains(word.toLowerCase())) {
                    event.setResult(null);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.anvil-blocked"));
                    return;
                }
            }
        }
    }

    private void checkWithApi(Player player, String text, String source, Runnable onBlock) {
        try {
            CompletableFuture<ModerationResponse> future = plugin.getApiClient().moderate(text);
            ModerationResponse response = future.get(API_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                return;
            }

            ModerationResponse.Result result = response.getResults().get(0);

            if (result.isFlagged()) {
                List<String> enabledCategories = plugin.getConfigManager().getEnabledCategories();
                List<String> flaggedCategories = result.getFlaggedCategories();

                boolean shouldBlock = flaggedCategories.stream().anyMatch(enabledCategories::contains);

                if (shouldBlock) {
                    onBlock.run();
                    plugin.getPunishmentManager().handleViolation(
                        player,
                        result.getHighestCategory(),
                        result.getHighestScore(),
                        text
                    );
                }
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("API check error for " + source + ": " + e.getMessage());
            }
        }
    }
}
