package dev.loratech.guard.gui;

import dev.loratech.guard.database.DatabaseManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerDetailGUI extends AbstractGUI {

    private final Player target;
    private final String title;

    public PlayerDetailGUI(GUIManager guiManager, Player viewer, Player target) {
        super(guiManager, viewer);
        this.target = target;
        this.title = "§8Player: " + target.getName();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setup() {
        createInventory(36);
        fillBottomBorder();

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(target);
            headMeta.setDisplayName("§e" + target.getName());
            int points = plugin.getDatabaseManager().getPlayerViolationPoints(target.getUniqueId());
            headMeta.setLore(Arrays.asList(
                "§7UUID: §f" + target.getUniqueId().toString().substring(0, 8) + "...",
                "§7Violation Points: §f" + points
            ));
            head.setItemMeta(headMeta);
        }
        inventory.setItem(4, head);

        List<DatabaseManager.ViolationRecord> history = plugin.getDatabaseManager()
            .getPlayerHistory(target.getUniqueId(), 5);
        List<String> historyLore = new ArrayList<>();
        historyLore.add("§7Recent violations:");
        historyLore.add("");
        if (history.isEmpty()) {
            historyLore.add("§aNo violations found");
        } else {
            for (DatabaseManager.ViolationRecord record : history) {
                historyLore.add("§c" + record.category() + " §8- §7" + truncate(record.message(), 20));
            }
        }
        inventory.setItem(11, createItem(Material.BOOK, "§e§lViolation History", historyLore.toArray(new String[0])));

        boolean muted = plugin.getPunishmentManager().isPlayerMuted(target.getUniqueId());
        if (muted) {
            String remaining = plugin.getPunishmentManager().getMuteRemainingFormatted(target.getUniqueId());
            inventory.setItem(13, createItem(
                Material.BARRIER,
                "§c§lUnmute Player",
                "§7Click to unmute",
                "",
                "§7Remaining: §f" + remaining
            ));
        } else {
            inventory.setItem(13, createItem(
                Material.ECHO_SHARD,
                "§c§lMute Player",
                "§7Click to mute for 10 minutes"
            ));
        }

        boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(target.getName());
        inventory.setItem(15, createItem(
            whitelisted ? Material.LIME_DYE : Material.GRAY_DYE,
            whitelisted ? "§c§lRemove from Whitelist" : "§a§lAdd to Whitelist",
            "§7Click to toggle whitelist status",
            "",
            "§7Current: " + (whitelisted ? "§aWhitelisted" : "§cNot Whitelisted")
        ));

        inventory.setItem(20, createItem(
            Material.WOODEN_SWORD,
            "§e§lWarn",
            "§7Issue a warning"
        ));

        inventory.setItem(22, createItem(
            Material.IRON_DOOR,
            "§c§lKick",
            "§7Kick from server"
        ));

        inventory.setItem(24, createItem(
            Material.REDSTONE_BLOCK,
            "§4§lBan (1 Day)",
            "§7Ban for 1 day"
        ));

        inventory.setItem(29, createItem(
            Material.TNT,
            "§4§lClear History",
            "§7Clear all violation history"
        ));

        inventory.setItem(31, createItem(
            Material.ARROW,
            "§c§lBack",
            "§7Return to player list"
        ));

    }

    private void fillBottomBorder() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, glass);
        }
    }

    private ItemStack createItemWithLore(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private String truncate(String text, int max) {
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
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
            case 13 -> {
                boolean muted = plugin.getPunishmentManager().isPlayerMuted(target.getUniqueId());
                if (muted) {
                    plugin.getPunishmentManager().unmute(target.getUniqueId());
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.unmute.success",
                        "player", target.getName()));
                } else {
                    plugin.getPunishmentManager().mute(target, "Muted via GUI", 10);
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.mute.success",
                        "player", target.getName(), "duration", "10m"));
                }
                setup();
            }
            case 15 -> {
                boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(target.getName());
                if (whitelisted) {
                    plugin.getConfigManager().removeWhitelistedPlayer(target.getName());
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.removed",
                        "player", target.getName()));
                } else {
                    plugin.getConfigManager().addWhitelistedPlayer(target.getName());
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.added",
                        "player", target.getName()));
                }
                setup();
            }
            case 20 -> {
                plugin.getPunishmentManager().warn(target, "Warning via GUI");
                viewer.sendMessage("§aWarning sent to " + target.getName());
            }
            case 22 -> {
                plugin.getPunishmentManager().kick(target, "Kicked via GUI");
                navigateTo(new PlayerListGUI(guiManager, viewer));
            }
            case 24 -> {
                plugin.getPunishmentManager().ban(target, "Banned via GUI", 60 * 24);
                navigateTo(new PlayerListGUI(guiManager, viewer));
            }
            case 29 -> {
                plugin.getDatabaseManager().clearPlayerHistory(target.getUniqueId());
                viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.clear.success",
                    "player", target.getName()));
                setup();
            }
            case 31 -> navigateTo(new PlayerListGUI(guiManager, viewer));
        }
    }
}
