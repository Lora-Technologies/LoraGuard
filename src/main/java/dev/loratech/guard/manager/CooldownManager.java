package dev.loratech.guard.manager;

import dev.loratech.guard.LoraGuard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final LoraGuard plugin;
    private final Map<UUID, Long> reportCooldowns;
    private final Map<UUID, Long> appealCooldowns;

    public CooldownManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.reportCooldowns = new ConcurrentHashMap<>();
        this.appealCooldowns = new ConcurrentHashMap<>();
    }

    public boolean isOnReportCooldown(UUID uuid) {
        if (!reportCooldowns.containsKey(uuid)) {
            return false;
        }
        
        long expiry = reportCooldowns.get(uuid);
        if (System.currentTimeMillis() > expiry) {
            reportCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void setReportCooldown(UUID uuid) {
        int seconds = plugin.getConfigManager().getReportCooldownSeconds();
        reportCooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getReportCooldownRemaining(UUID uuid) {
        if (!reportCooldowns.containsKey(uuid)) {
            return 0;
        }
        long remaining = reportCooldowns.get(uuid) - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    public boolean isOnAppealCooldown(UUID uuid) {
        if (!appealCooldowns.containsKey(uuid)) {
            return false;
        }
        
        long expiry = appealCooldowns.get(uuid);
        if (System.currentTimeMillis() > expiry) {
            appealCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public void setAppealCooldown(UUID uuid) {
        int seconds = plugin.getConfigManager().getAppealCooldownSeconds();
        appealCooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getAppealCooldownRemaining(UUID uuid) {
        if (!appealCooldowns.containsKey(uuid)) {
            return 0;
        }
        long remaining = appealCooldowns.get(uuid) - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }

    public void clearCooldowns(UUID uuid) {
        reportCooldowns.remove(uuid);
        appealCooldowns.remove(uuid);
    }

    public void clearAllCooldowns() {
        reportCooldowns.clear();
        appealCooldowns.clear();
    }
}
