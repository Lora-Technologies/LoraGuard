package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import net.kyori.adventure.text.Component;

public class ClearChatCommand implements CommandExecutor {

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

        // Send 100 empty lines
        for (int i = 0; i < 100; i++) {
            Bukkit.broadcast(Component.empty());
        }

        String clearer = sender.getName();
        String message = plugin.getLanguageManager().getPrefixed("commands.clearchat.broadcast", "player", clearer);
        Bukkit.broadcast(Component.text(message));

        return true;
    }
}
