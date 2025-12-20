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
        this.title = plugin.getLanguageManager().get("gui.player-detail.title", "player", target.getName());
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
            headMeta.setLore(Arrays.asList(
                plugin.getLanguageManager().get("gui.player-detail.uuid", "uuid", target.getUniqueId().toString().substring(0, 8)),
                plugin.getLanguageManager().get("gui.player-detail.violation-points", "count", "...")
            ));
            head.setItemMeta(headMeta);
        }
        inventory.setItem(4, head);

        inventory.setItem(11, createItem(Material.BOOK, plugin.getLanguageManager().get("gui.player-detail.history.title"), "§7Loading..."));

        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int points = plugin.getDatabaseManager().getPlayerViolationPoints(target.getUniqueId());
            List<DatabaseManager.ViolationRecord> history = plugin.getDatabaseManager()
                .getPlayerHistory(target.getUniqueId(), 5);

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                ItemStack currentHead = inventory.getItem(4);
                if (currentHead != null) {
                    ItemMeta meta = currentHead.getItemMeta();
                    if (meta != null) {
                        meta.setLore(Arrays.asList(
                            plugin.getLanguageManager().get("gui.player-detail.uuid", "uuid", target.getUniqueId().toString().substring(0, 8)),
                            plugin.getLanguageManager().get("gui.player-detail.violation-points", "count", String.valueOf(points))
                        ));
                        currentHead.setItemMeta(meta);
                        inventory.setItem(4, currentHead);
                    }
                }

                List<String> historyLore = new ArrayList<>();
                historyLore.add(plugin.getLanguageManager().get("gui.player-detail.history.recent"));
                historyLore.add("");
                if (history.isEmpty()) {
                    historyLore.add(plugin.getLanguageManager().get("gui.player-detail.history.empty"));
                } else {
                    for (DatabaseManager.ViolationRecord record : history) {
                        historyLore.add("§c" + record.category() + " §8- §7" + truncate(record.message(), 20));
                    }
                }
                inventory.setItem(11, createItem(Material.BOOK, plugin.getLanguageManager().get("gui.player-detail.history.title"), historyLore.toArray(new String[0])));
            });
        });

        boolean muted = plugin.getPunishmentManager().isPlayerMuted(target.getUniqueId());
        if (muted) {
            String remaining = plugin.getPunishmentManager().getMuteRemainingFormatted(target.getUniqueId());
            inventory.setItem(13, createItem(
                Material.BARRIER,
                plugin.getLanguageManager().get("gui.player-detail.unmute.title"),
                plugin.getLanguageManager().get("gui.player-detail.unmute.lore"),
                "",
                plugin.getLanguageManager().get("gui.player-detail.unmute.remaining", "time", remaining)
            ));
        } else {
            inventory.setItem(13, createItem(
                Material.ECHO_SHARD,
                plugin.getLanguageManager().get("gui.player-detail.mute.title"),
                plugin.getLanguageManager().get("gui.player-detail.mute.lore")
            ));
        }

        boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(target.getName());
        String whitelistTitle = whitelisted 
            ? plugin.getLanguageManager().get("gui.player-detail.whitelist-remove.title")
            : plugin.getLanguageManager().get("gui.player-detail.whitelist-add.title");
        String whitelistLore = whitelisted
            ? plugin.getLanguageManager().get("gui.player-detail.whitelist-remove.lore")
            : plugin.getLanguageManager().get("gui.player-detail.whitelist-add.lore");
        String currentStatus = whitelisted
            ? plugin.getLanguageManager().get("gui.player-detail.status-whitelisted")
            : plugin.getLanguageManager().get("gui.player-detail.status-not-whitelisted");
        inventory.setItem(15, createItem(
            whitelisted ? Material.LIME_DYE : Material.GRAY_DYE,
            whitelistTitle,
            whitelistLore,
            "",
            plugin.getLanguageManager().get("gui.player-detail.current-status", "status", currentStatus)
        ));

        inventory.setItem(20, createItem(
            Material.WOODEN_SWORD,
            plugin.getLanguageManager().get("gui.player-detail.warn.title"),
            plugin.getLanguageManager().get("gui.player-detail.warn.lore")
        ));

        inventory.setItem(22, createItem(
            Material.IRON_DOOR,
            plugin.getLanguageManager().get("gui.player-detail.kick.title"),
            plugin.getLanguageManager().get("gui.player-detail.kick.lore")
        ));

        inventory.setItem(24, createItem(
            Material.REDSTONE_BLOCK,
            plugin.getLanguageManager().get("gui.player-detail.ban.title"),
            plugin.getLanguageManager().get("gui.player-detail.ban.lore")
        ));

        inventory.setItem(29, createItem(
            Material.TNT,
            plugin.getLanguageManager().get("gui.player-detail.clear.title"),
            plugin.getLanguageManager().get("gui.player-detail.clear.lore")
        ));

        inventory.setItem(31, createItem(
            Material.ARROW,
            plugin.getLanguageManager().get("gui.player-detail.back"),
            plugin.getLanguageManager().get("gui.player-detail.back-lore")
        ));

    }

    private void fillBottomBorder() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, glass);
        }
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
                viewer.updateInventory();
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
                viewer.updateInventory();
            }
            case 20 -> {
                viewer.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getPunishmentManager().warn(target, "Warning via GUI");
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("gui.player-detail.warn.sent", "player", target.getName()));
                });
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
                viewer.closeInventory();
                org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    plugin.getDatabaseManager().clearPlayerHistory(target.getUniqueId());
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.clear.success",
                        "player", target.getName()));
                    
                    org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                        new PlayerDetailGUI(guiManager, viewer, target).open();
                    });
                });
            }
            case 31 -> navigateTo(new PlayerListGUI(guiManager, viewer));
        }
    }
}
