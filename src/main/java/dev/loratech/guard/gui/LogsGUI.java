package dev.loratech.guard.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LogsGUI extends AbstractGUI {

    public LogsGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.logs.title");
    }

    @Override
    public void setup() {
        createInventory(27);
        fillBorder(27);

        inventory.setItem(11, createItem(
            Material.PAPER,
            plugin.getLanguageManager().get("gui.logs.total.title"),
            plugin.getLanguageManager().get("gui.logs.total.lore", "count", "...")
        ));

        inventory.setItem(13, createItem(
            Material.CLOCK,
            plugin.getLanguageManager().get("gui.logs.today.title"),
            plugin.getLanguageManager().get("gui.logs.today.lore", "count", "...")
        ));

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = plugin.getDatabaseManager().getTotalViolations();
            int today = plugin.getDatabaseManager().getTodayViolations();

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                inventory.setItem(11, createItem(
                    Material.PAPER,
                    plugin.getLanguageManager().get("gui.logs.total.title"),
                    plugin.getLanguageManager().get("gui.logs.total.lore", "count", String.valueOf(total))
                ));

                inventory.setItem(13, createItem(
                    Material.CLOCK,
                    plugin.getLanguageManager().get("gui.logs.today.title"),
                    plugin.getLanguageManager().get("gui.logs.today.lore", "count", String.valueOf(today))
                ));
            });
        });

        inventory.setItem(15, createItem(
            Material.CHEST,
            plugin.getLanguageManager().get("gui.logs.cache.title"),
            plugin.getLanguageManager().get("gui.logs.cache.lore", "count", String.valueOf(plugin.getMessageCache().size()))
        ));

        inventory.setItem(22, createItem(
            Material.ARROW,
            plugin.getLanguageManager().get("gui.logs.back"),
            plugin.getLanguageManager().get("gui.logs.back-lore")
        ));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR ||
            clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            navigateTo(new MainMenuGUI(guiManager, viewer));
        }
    }
}
