package dev.loratech.guard.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class LogsGUI extends AbstractGUI {

    private static final String TITLE = "§8Violation Logs";

    public LogsGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void setup() {
        createInventory(27);
        fillBorder(27);

        int total = plugin.getDatabaseManager().getTotalViolations();
        int today = plugin.getDatabaseManager().getTodayViolations();

        inventory.setItem(11, createItem(
            Material.PAPER,
            "§b§lTotal Violations",
            "§7All time: §f" + total
        ));

        inventory.setItem(13, createItem(
            Material.CLOCK,
            "§e§lToday's Violations",
            "§7Today: §f" + today
        ));

        inventory.setItem(15, createItem(
            Material.CHEST,
            "§a§lCache Info",
            "§7Cached messages: §f" + plugin.getMessageCache().size()
        ));

        inventory.setItem(22, createItem(
            Material.ARROW,
            "§c§lBack",
            "§7Return to main menu"
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
