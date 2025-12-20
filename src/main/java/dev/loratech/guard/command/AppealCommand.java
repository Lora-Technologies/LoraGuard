package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.appeal.Appeal;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AppealCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;

    public AppealCommand(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.players-only"));
            return true;
        }

        if (!plugin.getConfigManager().isAppealSystemEnabled()) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.disabled"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create", "olustur", "oluştur", "yeni" -> handleCreate(player, args);
            case "status", "durum" -> handleStatus(player);
            case "list", "liste" -> handleList(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/appeal create <mute|ban> <reason>"));
            return;
        }

        String type = args[1].toLowerCase();
        if (!type.equals("mute") && !type.equals("ban")) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.invalid-type"));
            return;
        }

        if (plugin.getCooldownManager().isOnAppealCooldown(player.getUniqueId())) {
            int remaining = plugin.getCooldownManager().getAppealCooldownRemaining(player.getUniqueId());
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.cooldown",
                "seconds", String.valueOf(remaining)));
            return;
        }

        Appeal existingPending = plugin.getDatabaseManager().getPendingAppeal(player.getUniqueId());
        if (existingPending != null) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.already-pending"));
            return;
        }

        boolean hasActivePunishment = false;
        if (type.equals("mute")) {
            hasActivePunishment = plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId());
        } else if (type.equals("ban")) {
            hasActivePunishment = Bukkit.getBanList(org.bukkit.BanList.Type.NAME)
                .isBanned(player.getName());
        }

        if (!hasActivePunishment) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.no-punishment", "type", type));
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        int punishmentId = plugin.getDatabaseManager().getLatestPunishmentId(player.getUniqueId(), type);

        boolean success = plugin.getAppealManager().createAppeal(
            player.getUniqueId(),
            player.getName(),
            punishmentId,
            type,
            reason
        );

        if (success) {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.submitted"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.failed"));
        }
    }

    private void handleStatus(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Appeal appeal = plugin.getDatabaseManager().getPendingAppeal(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (appeal == null) {
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.no-pending"));
                    return;
                }

                player.sendMessage(plugin.getLanguageManager().get("appeal.status.header"));
                player.sendMessage(plugin.getLanguageManager().get("appeal.status.id", "id", String.valueOf(appeal.getId())));
                player.sendMessage(plugin.getLanguageManager().get("appeal.status.type", "type", appeal.getPunishmentType()));
                player.sendMessage(plugin.getLanguageManager().get("appeal.status.status", "status",
                    getStatusText(appeal.getStatus())));
                player.sendMessage(plugin.getLanguageManager().get("appeal.status.reason", "reason", appeal.getReason()));
            });
        });
    }

    private void handleList(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Appeal> appeals = plugin.getDatabaseManager().getPlayerAppeals(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (appeals.isEmpty()) {
                    player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.no-appeals"));
                    return;
                }

                player.sendMessage(plugin.getLanguageManager().get("appeal.list.header",
                    "count", String.valueOf(appeals.size())));

                for (Appeal appeal : appeals) {
                    player.sendMessage(plugin.getLanguageManager().get("appeal.list.entry",
                        "id", String.valueOf(appeal.getId()),
                        "type", appeal.getPunishmentType(),
                        "status", getStatusText(appeal.getStatus())));
                }
            });
        });
    }

    private String getStatusText(Appeal.AppealStatus status) {
        return switch (status) {
            case PENDING -> plugin.getLanguageManager().get("appeal.status.pending-text");
            case APPROVED -> plugin.getLanguageManager().get("appeal.status.approved-text");
            case DENIED -> plugin.getLanguageManager().get("appeal.status.denied-text");
        };
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.getLanguageManager().get("appeal.usage.header"));
        player.sendMessage(plugin.getLanguageManager().get("appeal.usage.create"));
        player.sendMessage(plugin.getLanguageManager().get("appeal.usage.status"));
        player.sendMessage(plugin.getLanguageManager().get("appeal.usage.list"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("create", "status", "list", "olustur", "durum", "liste")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("olustur") || args[0].equalsIgnoreCase("oluştur"))) {
            return Arrays.asList("mute", "ban")
                .stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }
        return List.of();
    }
}
