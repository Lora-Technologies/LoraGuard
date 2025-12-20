package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private final LoraGuard plugin;

    public ConnectionListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getFilterManager().clearHistory(event.getPlayer().getUniqueId());
        plugin.getGUIManager().unregisterGUI(event.getPlayer());
    }
}
