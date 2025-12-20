package dev.loratech.guard.appeal;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class AppealManager {

    private final LoraGuard plugin;

    public AppealManager(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public boolean createAppeal(UUID playerUuid, String playerName, int punishmentId, 
                                String punishmentType, String reason) {
        if (!plugin.getConfigManager().isAppealSystemEnabled()) {
            return false;
        }

        if (plugin.getCooldownManager().isOnAppealCooldown(playerUuid)) {
            return false;
        }

        Appeal existingPending = plugin.getDatabaseManager().getPendingAppeal(playerUuid);
        if (existingPending != null) {
            return false;
        }

        int appealId = plugin.getDatabaseManager().createAppeal(playerUuid, playerName, 
            punishmentId, punishmentType, reason);
        
        if (appealId > 0) {
            plugin.getCooldownManager().setAppealCooldown(playerUuid);
            notifyStaff(playerName, punishmentType, appealId);
            return true;
        }
        
        return false;
    }

    public boolean approveAppeal(int appealId, String reviewerName, String note) {
        Appeal appeal = plugin.getDatabaseManager().getAppeal(appealId);
        if (appeal == null || appeal.getStatus() != Appeal.AppealStatus.PENDING) {
            return false;
        }

        boolean updated = plugin.getDatabaseManager().updateAppealStatus(
            appealId, Appeal.AppealStatus.APPROVED, reviewerName, note);

        if (updated) {
            String punishmentType = appeal.getPunishmentType();
            if ("mute".equalsIgnoreCase(punishmentType)) {
                plugin.getPunishmentManager().unmute(appeal.getPlayerUuid());
            } else if ("ban".equalsIgnoreCase(punishmentType)) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(appeal.getPlayerName());
                });
            }

            Player player = Bukkit.getPlayer(appeal.getPlayerUuid());
            if (player != null) {
                player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.approved", 
                    "note", note != null ? note : ""));
            }

            if (plugin.getConfigManager().isDiscordEnabled()) {
                plugin.getDiscordHook().sendAppealUpdate(appeal, Appeal.AppealStatus.APPROVED, reviewerName, note);
            }
        }

        return updated;
    }

    public boolean denyAppeal(int appealId, String reviewerName, String note) {
        Appeal appeal = plugin.getDatabaseManager().getAppeal(appealId);
        if (appeal == null || appeal.getStatus() != Appeal.AppealStatus.PENDING) {
            return false;
        }

        boolean updated = plugin.getDatabaseManager().updateAppealStatus(
            appealId, Appeal.AppealStatus.DENIED, reviewerName, note);

        if (updated) {
            Player player = Bukkit.getPlayer(appeal.getPlayerUuid());
            if (player != null) {
                player.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.denied",
                    "note", note != null ? note : ""));
            }

            if (plugin.getConfigManager().isDiscordEnabled()) {
                plugin.getDiscordHook().sendAppealUpdate(appeal, Appeal.AppealStatus.DENIED, reviewerName, note);
            }
        }

        return updated;
    }

    public List<Appeal> getPendingAppeals() {
        return plugin.getDatabaseManager().getPendingAppeals();
    }

    public List<Appeal> getPlayerAppeals(UUID uuid) {
        return plugin.getDatabaseManager().getPlayerAppeals(uuid);
    }

    public int getPendingAppealCount() {
        return plugin.getDatabaseManager().getPendingAppealCount();
    }

    private void notifyStaff(String playerName, String type, int appealId) {
        String permission = plugin.getConfigManager().getStaffPermission();
        String message = plugin.getLanguageManager().getPrefixed("appeal.staff-notify",
            "player", playerName, "type", type, "id", String.valueOf(appealId));

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) {
                    staff.sendMessage(message);
                    
                    try {
                        String soundName = plugin.getConfigManager().getAlertSound();
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                        staff.playSound(staff.getLocation(), sound, 1.0f, 1.0f);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        });
    }
}
