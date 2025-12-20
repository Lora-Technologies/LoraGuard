package dev.loratech.guard.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGUI extends AbstractGUI {

    public MainMenuGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.main-menu.title");
    }

    @Override
    public void setup() {
        createInventory(45);
        fillBorder(45);
        
        inventory.setItem(11, createItem(
            Material.BOOK,
            plugin.getLanguageManager().get("gui.main-menu.stats.title"),
            plugin.getLanguageManager().get("gui.main-menu.stats.lore"),
            "",
            "§7" + plugin.getLanguageManager().get("misc.loading")
        ));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = plugin.getDatabaseManager().getTotalViolations();
            int today = plugin.getDatabaseManager().getTodayViolations();
            long cacheSize = plugin.getMessageCache().size();
            int pendingAppeals = plugin.getAppealManager().getPendingAppealCount();

            Bukkit.getScheduler().runTask(plugin, () -> {
                inventory.setItem(11, createItem(
                    Material.BOOK,
                    plugin.getLanguageManager().get("gui.main-menu.stats.title"),
                    plugin.getLanguageManager().get("gui.main-menu.stats.lore"),
                    "",
                    plugin.getLanguageManager().get("gui.main-menu.stats.total", "count", String.valueOf(total)),
                    plugin.getLanguageManager().get("gui.main-menu.stats.today", "count", String.valueOf(today)),
                    plugin.getLanguageManager().get("gui.main-menu.stats.cache", "count", String.valueOf(cacheSize))
                ));

                updateAppealItem(pendingAppeals);
            });
        });

        inventory.setItem(13, createItem(
            Material.PLAYER_HEAD,
            plugin.getLanguageManager().get("gui.main-menu.players.title"),
            plugin.getLanguageManager().get("gui.main-menu.players.lore"),
            "",
            plugin.getLanguageManager().get("gui.main-menu.players.online", "count", String.valueOf(Bukkit.getOnlinePlayers().size()))
        ));

        String statusText = plugin.isModEnabled() 
            ? plugin.getLanguageManager().get("gui.main-menu.toggle.enabled") 
            : plugin.getLanguageManager().get("gui.main-menu.toggle.disabled");
        inventory.setItem(15, createItem(
            plugin.isModEnabled() ? Material.LIME_DYE : Material.GRAY_DYE,
            plugin.getLanguageManager().get("gui.main-menu.toggle.title"),
            plugin.getLanguageManager().get("gui.main-menu.toggle.lore"),
            "",
            plugin.getLanguageManager().get("gui.main-menu.toggle.status", "status", statusText)
        ));

        inventory.setItem(29, createItem(
            Material.PAPER,
            plugin.getLanguageManager().get("gui.main-menu.logs.title"),
            plugin.getLanguageManager().get("gui.main-menu.logs.lore")
        ));

        inventory.setItem(31, createItem(
            Material.BELL,
            plugin.getLanguageManager().get("gui.appeals.pending-count", "count", "..."),
            plugin.getLanguageManager().get("gui.appeals.pending-lore")
        ));

        String apiStatus = plugin.getApiClient().isApiAvailable() 
            ? plugin.getLanguageManager().get("misc.online") 
            : plugin.getLanguageManager().get("misc.offline");
        inventory.setItem(33, createItem(
            Material.ENDER_EYE,
            plugin.getLanguageManager().get("gui.main-menu.api.title"),
            plugin.getLanguageManager().get("gui.main-menu.api.status", "status", apiStatus),
            "",
            plugin.getLanguageManager().get("gui.main-menu.api.model", "model", plugin.getConfigManager().getApiModel()),
            plugin.getLanguageManager().get("gui.main-menu.api.threshold", "threshold", String.valueOf(plugin.getConfigManager().getApiThreshold()))
        ));
    }

    private void updateAppealItem(int count) {
        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().get("gui.appeals.pending-lore"));
        if (count > 0) {
            lore.add("");
            lore.add("§e§l" + count + " pending!");
        }
        
        ItemStack item = new ItemStack(count > 0 ? Material.BELL : Material.IRON_NUGGET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getLanguageManager().get("gui.appeals.pending-count", "count", String.valueOf(count)));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        inventory.setItem(31, item);
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
            case 11 -> {
                viewer.closeInventory();
                viewer.performCommand("loraguard stats");
            }
            case 13 -> navigateTo(new ModernPlayerListGUI(guiManager, viewer));
            case 15 -> {
                plugin.setModEnabled(!plugin.isModEnabled());
                setup();
                viewer.updateInventory();
            }
            case 29 -> navigateTo(new LogsGUI(guiManager, viewer));
            case 31 -> navigateTo(new AppealListGUI(guiManager, viewer));
        }
    }
}
