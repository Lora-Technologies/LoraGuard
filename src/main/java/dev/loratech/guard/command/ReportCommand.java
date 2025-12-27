package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ReportCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;

    public ReportCommand(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.players-only"));
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

        if (plugin.getCooldownManager().isOnReportCooldown(player.getUniqueId())) {
            int remaining = plugin.getCooldownManager().getReportCooldownRemaining(player.getUniqueId());
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.report.cooldown", 
                "seconds", String.valueOf(remaining)));
            return true;
        }

        StringBuilder reasonBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            reasonBuilder.append(args[i]).append(" ");
        }
        String reason = reasonBuilder.toString().trim();

        String lastMessage = plugin.getFilterManager().getLastMessage(target.getUniqueId());

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.report.sent"));
        plugin.getCooldownManager().setReportCooldown(player.getUniqueId());

        Player finalTarget = target;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int reportId = plugin.getDatabaseManager().createReport(
                player.getUniqueId(), player.getName(),
                finalTarget.getUniqueId(), finalTarget.getName(),
                reason, lastMessage
            );

            boolean punished = false;

            if (lastMessage != null) {
                try {
                    CompletableFuture<ModerationResponse> future = plugin.getApiClient().moderate(lastMessage);
                    ModerationResponse response = future.join();

                    if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                        ModerationResponse.Result result = response.getResults().get(0);
                        if (result.isFlagged()) {
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getPunishmentManager().handleViolation(finalTarget, result.getHighestCategory(), result.getHighestScore(), lastMessage);
                            });
                            punished = true;
                            plugin.getDatabaseManager().updateReportStatus(reportId, "auto_punished", "System", result.getHighestCategory());
                        }
                    }
                } catch (Exception e) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().warning("Report AI check failed: " + e.getMessage());
                    }
                }
            }

            plugin.getDiscordHook().sendReport(player, finalTarget, reason, lastMessage, punished);
            
            notifyStaff(player.getName(), finalTarget.getName(), reportId);
        });

        return true;
    }

    private void notifyStaff(String reporterName, String reportedName, int reportId) {
        String permission = plugin.getConfigManager().getStaffPermission();
        String message = plugin.getLanguageManager().getPrefixed("commands.report.staff-notify",
            "reporter", reporterName, "reported", reportedName, "id", String.valueOf(reportId));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) {
                    staff.sendMessage(message);
                }
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .filter(name -> !name.equals(sender.getName()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
