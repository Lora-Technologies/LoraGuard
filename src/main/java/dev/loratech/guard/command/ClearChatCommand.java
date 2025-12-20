package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import net.kyori.adventure.text.Component;

import java.util.List;

public class ClearChatCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;

    public ClearChatCommand(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loraguard.clearchat")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.no-permission"));
            return true;
        }

        for (int i = 0; i < 100; i++) {
            Bukkit.broadcast(Component.empty());
        }

        String clearer = sender.getName();
        String message = plugin.getLanguageManager().getPrefixed("commands.clearchat.broadcast", "player", clearer);
        Bukkit.broadcast(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(message));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
