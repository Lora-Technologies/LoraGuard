package dev.loratech.guard.gui;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public abstract class AbstractGUI {

    protected final LoraGuard plugin;
    protected final GUIManager guiManager;
    protected final Player viewer;
    protected Inventory inventory;
    protected boolean navigating = false;

    public AbstractGUI(GUIManager guiManager, Player viewer) {
        this.guiManager = guiManager;
        this.plugin = guiManager.getPlugin();
        this.viewer = viewer;
    }

    public abstract String getTitle();

    public abstract void setup();

    public abstract void handleClick(InventoryClickEvent event);

    public void open() {
        setup();
        guiManager.registerGUI(viewer, this);
        viewer.openInventory(inventory);
    }

    public void navigateTo(AbstractGUI nextGUI) {
        navigating = true;
        viewer.closeInventory();
        org.bukkit.Bukkit.getScheduler().runTask(guiManager.getPlugin(), () -> {
            navigating = false;
            nextGUI.open();
        });
    }

    public boolean isNavigating() {
        return navigating;
    }

    protected void createInventory(int size) {
        inventory = Bukkit.createInventory(null, size, getTitle());
    }

    protected ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    protected void fillBorder(int size) {
        ItemStack glass = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, glass);
        }
        for (int i = size - 9; i < size; i++) {
            inventory.setItem(i, glass);
        }
        for (int i = 9; i < size - 9; i += 9) {
            inventory.setItem(i, glass);
            inventory.setItem(i + 8, glass);
        }
    }
}
