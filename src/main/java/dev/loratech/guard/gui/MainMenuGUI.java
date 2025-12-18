package dev.loratech.guard.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class MainMenuGUI extends AbstractGUI {

    private static final String TITLE = "§8LoraGuard Control Panel";

    public MainMenuGUI(GUIManager guiManager, Player viewer) {
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
        
        inventory.setItem(10, createItem(
            Material.BOOK,
            "§b§lStatistics",
            "§7Click to view statistics",
            "",
            "§7Total Violations: §f" + plugin.getDatabaseManager().getTotalViolations(),
            "§7Today: §f" + plugin.getDatabaseManager().getTodayViolations(),
            "§7Cache Size: §f" + plugin.getMessageCache().size()
        ));

        inventory.setItem(12, createItem(
            Material.PLAYER_HEAD,
            "§e§lPlayer Management",
            "§7Click to manage players",
            "",
            "§7Online Players: §f" + Bukkit.getOnlinePlayers().size()
        ));

        String statusColor = plugin.isModEnabled() ? "§a" : "§c";
        String statusText = plugin.isModEnabled() ? "ENABLED" : "DISABLED";
        inventory.setItem(14, createItem(
            plugin.isModEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
            "§a§lToggle Moderation",
            "§7Click to toggle moderation",
            "",
            "§7Status: " + statusColor + statusText
        ));

        inventory.setItem(16, createItem(
            Material.PAPER,
            "§7§lViolation Logs",
            "§7Click to view recent logs"
        ));

        String apiStatus = plugin.getApiClient().isApiAvailable() ? "§aOnline" : "§cOffline";
        inventory.setItem(22, createItem(
            Material.ENDER_EYE,
            "§d§lAPI Status",
            "§7Lora API Status: " + apiStatus,
            "",
            "§7Model: §f" + plugin.getConfigManager().getApiModel(),
            "§7Threshold: §f" + plugin.getConfigManager().getApiThreshold()
        ));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || 
            clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getSlot();

        switch (slot) {
            case 10 -> {
                viewer.closeInventory();
                viewer.performCommand("loraguard stats");
            }
            case 12 -> navigateTo(new PlayerListGUI(guiManager, viewer));
            case 14 -> {
                plugin.setModEnabled(!plugin.isModEnabled());
                setup();
            }
            case 16 -> navigateTo(new LogsGUI(guiManager, viewer));
        }
    }
}
