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

    private static final String TITLE = "§8Player Management";
    private int inventorySize;

    public PlayerListGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void setup() {
        inventorySize = Math.min(54, ((Bukkit.getOnlinePlayers().size() / 9) + 1) * 9 + 9);
        inventorySize = Math.max(27, inventorySize);
        createInventory(inventorySize);

        inventory.setItem(inventorySize - 5, createItem(
            Material.ARROW,
            "§c§lBack",
            "§7Return to main menu"
        ));

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= inventorySize - 9) break;

            int violations = plugin.getDatabaseManager().getPlayerViolationPoints(online.getUniqueId());
            boolean muted = plugin.getPunishmentManager().isPlayerMuted(online.getUniqueId());
            boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(online.getName());

            List<String> lore = new ArrayList<>();
            lore.add("§7Click to manage");
            lore.add("");
            lore.add("§7Violation Points: §f" + violations);
            lore.add("§7Muted: " + (muted ? "§cYes" : "§aNo"));
            lore.add("§7Whitelisted: " + (whitelisted ? "§aYes" : "§cNo"));

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(online);
                meta.setDisplayName("§e" + online.getName());
                meta.setLore(lore);
                head.setItemMeta(meta);
            }

            inventory.setItem(slot++, head);
        }
    }

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
