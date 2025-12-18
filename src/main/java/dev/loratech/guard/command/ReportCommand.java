package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ReportCommand implements CommandExecutor {

    private final LoraGuard plugin;

    public ReportCommand(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().get("misc.player-only"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage", "usage", "/report <player> <reason>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            return true;
        }

        if (target.equals(player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.report.self"));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        String lastMessage = plugin.getFilterManager().getLastMessage(target.getUniqueId());

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.report.sent"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean punished = false;

            // AI Check on last message
            if (lastMessage != null) {
                try {
                    CompletableFuture<ModerationResponse> future = plugin.getApiClient().moderate(lastMessage);
                    ModerationResponse response = future.join(); // We are already async

                    if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                        ModerationResponse.Result result = response.getResults().get(0);
                        if (result.isFlagged()) {
                            // Valid report! Punish player
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getPunishmentManager().handleViolation(target, result.getHighestCategory(), result.getHighestScore(), lastMessage);
                            });
                            punished = true;
                        }
                    }
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().warning("Report AI check failed: " + e.getMessage());
                    }
                }
            }

            // Send to Discord
            plugin.getDiscordHook().sendReport(player, target, reason, lastMessage, punished);
        });

        return true;
    }
}
