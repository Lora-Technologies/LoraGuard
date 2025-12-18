package dev.loratech.guard.gui;

import dev.loratech.guard.LoraGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIManager implements Listener {

    private final LoraGuard plugin;
    private final Map<UUID, AbstractGUI> activeGUIs;

    public GUIManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.activeGUIs = new ConcurrentHashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void registerGUI(Player player, AbstractGUI gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }

    public void unregisterGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        AbstractGUI gui = activeGUIs.get(player.getUniqueId());
        if (gui == null) {
            return;
        }

        if (!event.getView().getTitle().equals(gui.getTitle())) {
            return;
        }

        event.setCancelled(true);
        gui.handleClick(event);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        AbstractGUI gui = activeGUIs.get(player.getUniqueId());
        if (gui == null) {
            return;
        }

        if (event.getView().getTitle().equals(gui.getTitle())) {
            if (!gui.isNavigating()) {
                activeGUIs.remove(player.getUniqueId());
            }
        }
    }

    public LoraGuard getPlugin() {
        return plugin;
    }
}
