package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;
import dev.loratech.guard.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CommandSpyListener implements Listener {

    private final LoraGuard plugin;
    private static final long API_TIMEOUT_MS = 2000;
    private static final List<String> MONITORED_COMMANDS = Arrays.asList(
        "msg", "tell", "w", "whisper", "m", "pm", "dm",
        "r", "reply", "respond",
        "me", "action",
        "mail", "email",
        "nickname", "nick", "name",
        "rename", "itemname"
    );

    public CommandSpyListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.isModEnabled()) return;
        if (!plugin.getConfigManager().isCommandSpyEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("loraguard.bypass")) return;

        String message = event.getMessage().substring(1);
        String[] parts = message.split(" ");
        if (parts.length < 2) return;

        String command = parts[0].toLowerCase();

        boolean isMonitored = MONITORED_COMMANDS.stream()
            .anyMatch(cmd -> command.equals(cmd) || command.endsWith(":" + cmd));

        if (!isMonitored) return;

        String content;
        if (command.equals("msg") || command.equals("tell") || command.equals("w") || 
            command.equals("whisper") || command.equals("m") || command.equals("pm") || command.equals("dm")) {
            if (parts.length < 3) return;
            content = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        } else if (command.equals("r") || command.equals("reply") || command.equals("respond")) {
            content = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        } else {
            content = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        }

        if (content.isEmpty()) return;

        String normalizedText = TextUtil.normalizeText(content);

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                if (normalizedText.toLowerCase().contains(word.toLowerCase())) {
                    event.setCancelled(true);
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.command-blocked"));
                    plugin.getPunishmentManager().handleViolation(player, "blacklist", 1.0, content);
                    notifyStaff(player, command, content);
                    return;
                }
            }
        }

        final String finalContent = content;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                CompletableFuture<ModerationResponse> future = plugin.getApiClient().moderate(finalContent);
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
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.sendMessage(plugin.getLanguageManager().getPrefixed("moderation.command-blocked"));
                        });
                        plugin.getPunishmentManager().handleViolation(
                            player,
                            result.getHighestCategory(),
                            result.getHighestScore(),
                            finalContent
                        );
                        notifyStaff(player, command, finalContent);
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("Command spy API check error: " + e.getMessage());
                }
            }
        });
    }

    private void notifyStaff(Player violator, String command, String content) {
        if (!plugin.getConfigManager().isStaffAlertEnabled()) return;

        String permission = plugin.getConfigManager().getStaffPermission();
        String alert = plugin.getLanguageManager().get("notifications.command-spy-alert",
            "player", violator.getName(),
            "command", command,
            "message", content);

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) {
                    staff.sendMessage(plugin.getLanguageManager().getPrefix() + alert);
                }
            }
        });
    }
}
