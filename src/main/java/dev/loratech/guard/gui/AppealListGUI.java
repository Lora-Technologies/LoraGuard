package dev.loratech.guard.gui;

import dev.loratech.guard.appeal.Appeal;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AppealListGUI extends PaginatedGUI<Appeal> {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public AppealListGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.appeals.title");
    }

    @Override
    public void setup() {
        setupPagination();
        
        int pendingCount = plugin.getAppealManager().getPendingAppealCount();
        inventory.setItem(4, createItem(
            Material.BELL,
            plugin.getLanguageManager().get("gui.appeals.pending-count", "count", String.valueOf(pendingCount)),
            plugin.getLanguageManager().get("gui.appeals.pending-lore")
        ));
    }

    @Override
    protected List<Appeal> loadItems() {
        return plugin.getAppealManager().getPendingAppeals();
    }

    @Override
    protected ItemStack createItemDisplay(Appeal appeal, int index) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getLanguageManager().get("gui.appeals.type", "type", appeal.getPunishmentType()));
        String dateStr = appeal.getCreatedAt() != null ? dateFormat.format(appeal.getCreatedAt()) : "Unknown";
        lore.add(plugin.getLanguageManager().get("gui.appeals.date", "date", dateStr));
        lore.add("");
        lore.add(plugin.getLanguageManager().get("gui.appeals.reason-title"));
        
        String reason = appeal.getReason() != null ? appeal.getReason() : "";
        if (reason.length() > 40) {
            lore.add("§7" + reason.substring(0, 40));
            lore.add("§7" + reason.substring(40, Math.min(80, reason.length())) + (reason.length() > 80 ? "..." : ""));
        } else {
            lore.add("§7" + reason);
        }
        
        String originalMessage = plugin.getDatabaseManager().getAppealOriginalMessage(appeal.getId());
        if (originalMessage == null) {
            originalMessage = plugin.getDatabaseManager().getPunishmentOriginalMessage(appeal.getPunishmentId());
        }
        if (originalMessage != null && !originalMessage.isEmpty()) {
            lore.add("");
            lore.add(plugin.getLanguageManager().get("gui.appeals.original-message-title"));
            if (originalMessage.length() > 35) {
                lore.add("§c" + originalMessage.substring(0, 35) + "...");
            } else {
                lore.add("§c" + originalMessage);
            }
        }
        
        lore.add("");
        lore.add(plugin.getLanguageManager().get("gui.appeals.click-to-review"));

        return createItem(
            Material.PAPER,
            plugin.getLanguageManager().get("gui.appeals.appeal-title", 
                "player", appeal.getPlayerName(), 
                "id", String.valueOf(appeal.getId())),
            lore.toArray(new String[0])
        );
    }

    @Override
    protected void handleItemClick(Appeal appeal, int index) {
        navigateTo(new AppealDetailGUI(guiManager, viewer, appeal));
    }

    @Override
    protected void onBackPressed() {
        navigateTo(new MainMenuGUI(guiManager, viewer));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || 
            clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        int slot = event.getSlot();

        if (slot == 45 || slot == 47 || slot == 49 || slot == 53) {
            handlePaginationClick(slot);
            return;
        }

        if (clicked.getType() == Material.PAPER) {
            int itemIndex = getItemIndexFromSlot(slot);
            if (itemIndex >= 0 && itemIndex < items.size()) {
                handleItemClick(items.get(itemIndex), itemIndex);
            }
        }
    }
}
