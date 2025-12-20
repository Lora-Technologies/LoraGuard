package dev.loratech.guard.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerListGUI extends AbstractGUI {

    private int inventorySize;

    public PlayerListGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.player-list.title");
    }

    @Override
    public void setup() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        inventorySize = Math.min(54, ((onlinePlayers.size() / 9) + 1) * 9 + 9);
        inventorySize = Math.max(27, inventorySize);
        createInventory(inventorySize);

        inventory.setItem(inventorySize - 5, createItem(
            Material.ARROW,
            plugin.getLanguageManager().get("gui.player-list.back"),
            plugin.getLanguageManager().get("gui.player-list.back-lore")
        ));
        
        for (int i = 0; i < Math.min(onlinePlayers.size(), inventorySize - 9); i++) {
            inventory.setItem(i, createItem(Material.SKELETON_SKULL, "§7Loading...", "§7Please wait..."));
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerData> dataList = new ArrayList<>();
            int maxSlots = inventorySize - 9;
            
            for (int i = 0; i < onlinePlayers.size(); i++) {
                if (i >= maxSlots) break;
                
                Player player = onlinePlayers.get(i);
                int violations = plugin.getDatabaseManager().getPlayerViolationPoints(player.getUniqueId());
                boolean muted = plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId());
                boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(player.getName());
                
                dataList.add(new PlayerData(player, violations, muted, whitelisted));
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                int slot = 0;
                for (PlayerData data : dataList) {
                    String yes = plugin.getLanguageManager().get("gui.player-list.yes");
                    String no = plugin.getLanguageManager().get("gui.player-list.no");
                    
                    List<String> lore = new ArrayList<>();
                    lore.add(plugin.getLanguageManager().get("gui.player-list.click-to-manage"));
                    lore.add("");
                    lore.add(plugin.getLanguageManager().get("gui.player-list.violation-points", "count", String.valueOf(data.violations)));
                    lore.add(plugin.getLanguageManager().get("gui.player-list.muted", "status", data.muted ? yes : no));
                    lore.add(plugin.getLanguageManager().get("gui.player-list.whitelisted", "status", data.whitelisted ? yes : no));

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setOwningPlayer(data.player);
                        meta.setDisplayName("§e" + data.player.getName());
                        meta.setLore(lore);
                        head.setItemMeta(meta);
                    }

                    inventory.setItem(slot++, head);
                }
            });
        });
    }

    private record PlayerData(Player player, int violations, boolean muted, boolean whitelisted) {}

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.ARROW) {
            navigateTo(new MainMenuGUI(guiManager, viewer));
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                Player target = meta.getOwningPlayer().getPlayer();
                if (target != null) {
                    navigateTo(new PlayerDetailGUI(guiManager, viewer, target));
                }
            }
        }
    }
}
