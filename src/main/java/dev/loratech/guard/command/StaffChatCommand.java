package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StaffChatCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;
    private final Set<UUID> toggledPlayers;

    public StaffChatCommand(LoraGuard plugin) {
        this.plugin = plugin;
        this.toggledPlayers = new HashSet<>();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loraguard.staffchat")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.staffchat.usage"));
                return true;
            }

            if (toggledPlayers.contains(player.getUniqueId())) {
                toggledPlayers.remove(player.getUniqueId());
                player.sendMessage(plugin.getLanguageManager().getPrefixed("commands.staffchat.toggled-off"));
            } else {
                toggledPlayers.add(player.getUniqueId());
                player.sendMessage(plugin.getLanguageManager().getPrefixed("commands.staffchat.toggled-on"));
            }
            return true;
        }

        String message = String.join(" ", args);
        broadcastToStaff(sender, message);
        return true;
    }

    public void broadcastToStaff(CommandSender sender, String message) {
        String format = plugin.getLanguageManager().get("commands.staffchat.format",
            "player", sender.getName(),
            "message", message);

        String permission = plugin.getConfigManager().getStaffChatPermission();

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission(permission)) {
                staff.sendMessage(format);
            }
        }

        Bukkit.getConsoleSender().sendMessage(format);
    }

    public boolean isToggled(UUID uuid) {
        return toggledPlayers.contains(uuid);
    }

    public void removeToggle(UUID uuid) {
        toggledPlayers.remove(uuid);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
