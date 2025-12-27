package dev.loratech.guard.gui;

import dev.loratech.guard.appeal.Appeal;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class AppealDetailGUI extends AbstractGUI {

    private final Appeal appeal;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public AppealDetailGUI(GUIManager guiManager, Player viewer, Appeal appeal) {
        super(guiManager, viewer);
        this.appeal = appeal;
    }

    @Override
    public String getTitle() {
        return plugin.getLanguageManager().get("gui.appeal-detail.title", "id", String.valueOf(appeal.getId()));
    }

    @Override
    public void setup() {
        createInventory(45);
        fillBorder(45);

        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(plugin.getLanguageManager().get("gui.appeal-detail.player", "player", appeal.getPlayerName()));
        infoLore.add(plugin.getLanguageManager().get("gui.appeal-detail.uuid", "uuid", appeal.getPlayerUuid().toString().substring(0, 8) + "..."));
        infoLore.add(plugin.getLanguageManager().get("gui.appeal-detail.type", "type", appeal.getPunishmentType()));
        String dateStr = appeal.getCreatedAt() != null ? dateFormat.format(appeal.getCreatedAt()) : "Unknown";
        infoLore.add(plugin.getLanguageManager().get("gui.appeal-detail.date", "date", dateStr));
        infoLore.add("");
        infoLore.add(plugin.getLanguageManager().get("gui.appeal-detail.status", "status", getStatusDisplay(appeal.getStatus())));

        inventory.setItem(13, createItem(
            Material.PLAYER_HEAD,
            "§e" + appeal.getPlayerName(),
            infoLore.toArray(new String[0])
        ));

        List<String> reasonLore = new ArrayList<>();
        reasonLore.add("");
        String reason = appeal.getReason() != null ? appeal.getReason() : "";
        int chunkSize = 35;
        for (int i = 0; i < reason.length(); i += chunkSize) {
            reasonLore.add("§7" + reason.substring(i, Math.min(i + chunkSize, reason.length())));
        }

        inventory.setItem(20, createItem(
            Material.WRITABLE_BOOK,
            plugin.getLanguageManager().get("gui.appeal-detail.reason-title"),
            reasonLore.toArray(new String[0])
        ));

        String originalMessage = plugin.getDatabaseManager().getAppealOriginalMessage(appeal.getId());
        if (originalMessage == null) {
            originalMessage = plugin.getDatabaseManager().getPunishmentOriginalMessage(appeal.getPunishmentId());
        }
        
        List<String> messageLore = new ArrayList<>();
        messageLore.add("");
        if (originalMessage != null && !originalMessage.isEmpty()) {
            for (int i = 0; i < originalMessage.length(); i += chunkSize) {
                messageLore.add("§c" + originalMessage.substring(i, Math.min(i + chunkSize, originalMessage.length())));
            }
        } else {
            messageLore.add(plugin.getLanguageManager().get("gui.appeal-detail.no-message"));
        }

        inventory.setItem(24, createItem(
            Material.PAPER,
            plugin.getLanguageManager().get("gui.appeal-detail.original-message-title"),
            messageLore.toArray(new String[0])
        ));

        if (appeal.getStatus() == Appeal.AppealStatus.PENDING) {
            inventory.setItem(29, createItem(
                Material.LIME_CONCRETE,
                plugin.getLanguageManager().get("gui.appeal-detail.approve"),
                plugin.getLanguageManager().get("gui.appeal-detail.approve-lore")
            ));

            inventory.setItem(33, createItem(
                Material.RED_CONCRETE,
                plugin.getLanguageManager().get("gui.appeal-detail.deny"),
                plugin.getLanguageManager().get("gui.appeal-detail.deny-lore")
            ));
        } else {
            String reviewerInfo = appeal.getReviewerName() != null ? appeal.getReviewerName() : "Unknown";
            inventory.setItem(31, createItem(
                appeal.getStatus() == Appeal.AppealStatus.APPROVED ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                plugin.getLanguageManager().get("gui.appeal-detail.reviewed"),
                plugin.getLanguageManager().get("gui.appeal-detail.reviewed-by", "reviewer", reviewerInfo),
                appeal.getReviewNote() != null ? "§7" + appeal.getReviewNote() : ""
            ));
        }

        inventory.setItem(40, createItem(
            Material.ARROW,
            plugin.getLanguageManager().get("gui.appeal-detail.back"),
            plugin.getLanguageManager().get("gui.appeal-detail.back-lore")
        ));
    }

    private String getStatusDisplay(Appeal.AppealStatus status) {
        return switch (status) {
            case PENDING -> "§e" + plugin.getLanguageManager().get("gui.appeal-detail.status-pending");
            case APPROVED -> "§a" + plugin.getLanguageManager().get("gui.appeal-detail.status-approved");
            case DENIED -> "§c" + plugin.getLanguageManager().get("gui.appeal-detail.status-denied");
        };
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
            case 29 -> {
                if (appeal.getStatus() == Appeal.AppealStatus.PENDING) {
                    plugin.getAppealManager().approveAppeal(appeal.getId(), viewer.getName(), "Approved via GUI");
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.approved-staff", "player", appeal.getPlayerName()));
                    navigateTo(new AppealListGUI(guiManager, viewer));
                }
            }
            case 33 -> {
                if (appeal.getStatus() == Appeal.AppealStatus.PENDING) {
                    plugin.getAppealManager().denyAppeal(appeal.getId(), viewer.getName(), "Denied via GUI");
                    viewer.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.denied-staff", "player", appeal.getPlayerName()));
                    navigateTo(new AppealListGUI(guiManager, viewer));
                }
            }
            case 40 -> navigateTo(new AppealListGUI(guiManager, viewer));
        }
    }
}
