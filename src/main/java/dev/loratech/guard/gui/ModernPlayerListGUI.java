package dev.loratech.guard.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ModernPlayerListGUI extends PaginatedGUI<Player> {

    private SortMode sortMode = SortMode.NAME;

    public ModernPlayerListGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.player-list.title");
    }

    @Override
    public void setup() {
        setupPagination();
        
        inventory.setItem(4, createItem(
            Material.HOPPER,
            plugin.getLanguageManager().get("gui.player-list.sort.title"),
            plugin.getLanguageManager().get("gui.player-list.sort.current", "mode", sortMode.getDisplayName(plugin)),
            "",
            plugin.getLanguageManager().get("gui.player-list.sort.click-to-change")
        ));

        inventory.setItem(51, createItem(
            Material.EMERALD,
            plugin.getLanguageManager().get("gui.player-list.refresh"),
            plugin.getLanguageManager().get("gui.player-list.refresh-lore")
        ));
    }

    @Override
    protected List<Player> loadItems() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        switch (sortMode) {
            case NAME -> players.sort(Comparator.comparing(Player::getName));
            case VIOLATIONS -> players.sort((a, b) -> {
                int va = plugin.getDatabaseManager().getPlayerViolationPoints(a.getUniqueId());
                int vb = plugin.getDatabaseManager().getPlayerViolationPoints(b.getUniqueId());
                return Integer.compare(vb, va);
            });
            case MUTED -> players.sort((a, b) -> {
                boolean ma = plugin.getPunishmentManager().isPlayerMuted(a.getUniqueId());
                boolean mb = plugin.getPunishmentManager().isPlayerMuted(b.getUniqueId());
                return Boolean.compare(mb, ma);
            });
        }
        
        return players;
    }

    @Override
    protected ItemStack createItemDisplay(Player player, int index) {
        int violations = plugin.getDatabaseManager().getPlayerViolationPoints(player.getUniqueId());
        boolean muted = plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId());
        boolean whitelisted = plugin.getConfigManager().getWhitelistedPlayers().contains(player.getName());

        String yes = plugin.getLanguageManager().get("gui.player-list.yes");
        String no = plugin.getLanguageManager().get("gui.player-list.no");

        List<String> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().get("gui.player-list.click-to-manage"));
        lore.add("");
        lore.add(plugin.getLanguageManager().get("gui.player-list.violation-points", "count", String.valueOf(violations)));
        lore.add(plugin.getLanguageManager().get("gui.player-list.muted", "status", muted ? yes : no));
        lore.add(plugin.getLanguageManager().get("gui.player-list.whitelisted", "status", whitelisted ? yes : no));
        
        if (violations > 0) {
            lore.add("");
            lore.add(getViolationIndicator(violations));
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName("§e" + player.getName());
            meta.setLore(lore);
            head.setItemMeta(meta);
        }

        return head;
    }

    private String getViolationIndicator(int points) {
        StringBuilder indicator = new StringBuilder("§8[");
        int filled = Math.min(10, points);
        int empty = 10 - filled;
        
        String color = points >= 7 ? "§c" : points >= 4 ? "§6" : "§a";
        indicator.append(color);
        indicator.append("█".repeat(filled));
        indicator.append("§7");
        indicator.append("░".repeat(empty));
        indicator.append("§8]");
        
        return indicator.toString();
    }

    @Override
    protected void handleItemClick(Player player, int index) {
        if (player == null) return;
        
        Player target = Bukkit.getPlayer(player.getUniqueId());
        if (target != null && target.isOnline()) {
            navigateTo(new PlayerDetailGUI(guiManager, viewer, target));
        } else {
            viewer.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            setup();
            viewer.updateInventory();
        }
    }

    @Override
    protected void onBackPressed() {
        navigateTo(new MainMenuGUI(guiManager, viewer));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 4) {
            sortMode = SortMode.values()[(sortMode.ordinal() + 1) % SortMode.values().length];
            setup();
            viewer.updateInventory();
            return;
        }

        if (slot == 51) {
            setup();
            viewer.updateInventory();
            return;
        }

        if (slot == 45 || slot == 47 || slot == 49 || slot == 53) {
            handlePaginationClick(slot);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            int itemIndex = getItemIndexFromSlot(slot);
            if (itemIndex >= 0 && itemIndex < items.size()) {
                handleItemClick(items.get(itemIndex), itemIndex);
            }
        }
    }

    private enum SortMode {
        NAME("sort.name"),
        VIOLATIONS("sort.violations"),
        MUTED("sort.muted");

        private final String key;

        SortMode(String key) {
            this.key = key;
        }

        public String getDisplayName(dev.loratech.guard.LoraGuard plugin) {
            return plugin.getLanguageManager().get("gui.player-list." + key);
        }
    }
}
