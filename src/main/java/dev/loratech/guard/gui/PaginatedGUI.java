package dev.loratech.guard.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class PaginatedGUI<T> extends AbstractGUI {

    protected int currentPage = 0;
    protected int maxItemsPerPage = 21;
    protected List<T> items;

    public PaginatedGUI(GUIManager guiManager, Player viewer) {
        super(guiManager, viewer);
    }

    protected abstract List<T> loadItems();
    protected abstract ItemStack createItemDisplay(T item, int index);
    protected abstract void handleItemClick(T item, int index);

    protected void setupPagination() {
        this.items = loadItems();
        createInventory(54);
        
        fillTopBorder();
        fillBottomNavigation();
        
        displayCurrentPage();
    }

    protected void displayCurrentPage() {
        for (int i = 10; i <= 43; i++) {
            if (isContentSlot(i)) {
                inventory.setItem(i, null);
            }
        }

        int startIndex = currentPage * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, items.size());
        
        int slot = 10;
        for (int i = startIndex; i < endIndex; i++) {
            while (!isContentSlot(slot) && slot < 44) {
                slot++;
            }
            if (slot >= 44) break;
            
            inventory.setItem(slot, createItemDisplay(items.get(i), i));
            slot++;
        }

        updateNavigationButtons();
    }

    private boolean isContentSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row >= 1 && row <= 4 && col >= 1 && col <= 7;
    }

    private void fillTopBorder() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
        }
        for (int i = 9; i <= 44; i += 9) {
            inventory.setItem(i, glass);
            inventory.setItem(i + 8, glass);
        }
    }

    private void fillBottomNavigation() {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }
    }

    private void updateNavigationButtons() {
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / maxItemsPerPage));
        
        if (currentPage > 0) {
            inventory.setItem(45, createItem(
                Material.ARROW,
                plugin.getLanguageManager().get("gui.pagination.previous"),
                plugin.getLanguageManager().get("gui.pagination.page-info", 
                    "current", String.valueOf(currentPage + 1), 
                    "total", String.valueOf(totalPages))
            ));
        } else {
            inventory.setItem(45, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        inventory.setItem(49, createItem(
            Material.PAPER,
            plugin.getLanguageManager().get("gui.pagination.page-display",
                "current", String.valueOf(currentPage + 1),
                "total", String.valueOf(totalPages)),
            plugin.getLanguageManager().get("gui.pagination.total-items", 
                "count", String.valueOf(items.size()))
        ));

        if (currentPage < totalPages - 1) {
            inventory.setItem(53, createItem(
                Material.ARROW,
                plugin.getLanguageManager().get("gui.pagination.next"),
                plugin.getLanguageManager().get("gui.pagination.page-info",
                    "current", String.valueOf(currentPage + 1),
                    "total", String.valueOf(totalPages))
            ));
        } else {
            inventory.setItem(53, createItem(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        inventory.setItem(47, createItem(
            Material.BARRIER,
            plugin.getLanguageManager().get("gui.pagination.back"),
            plugin.getLanguageManager().get("gui.pagination.back-lore")
        ));
    }

    protected void handlePaginationClick(int slot) {
        if (slot == 45 && currentPage > 0) {
            currentPage--;
            displayCurrentPage();
            viewer.updateInventory();
        } else if (slot == 53) {
            int totalPages = (int) Math.ceil((double) items.size() / maxItemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                displayCurrentPage();
                viewer.updateInventory();
            }
        } else if (slot == 47) {
            onBackPressed();
        }
    }

    protected abstract void onBackPressed();

    protected int getItemIndexFromSlot(int slot) {
        if (!isContentSlot(slot)) return -1;
        
        int contentSlotIndex = 0;
        for (int i = 10; i <= slot; i++) {
            if (isContentSlot(i)) {
                if (i == slot) {
                    return currentPage * maxItemsPerPage + contentSlotIndex;
                }
                contentSlotIndex++;
            }
        }
        return -1;
    }
}
