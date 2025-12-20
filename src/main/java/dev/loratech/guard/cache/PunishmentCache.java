package dev.loratech.guard.cache;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentCache {

    private final LoraGuard plugin;
    private final Map<UUID, Long> activeMutes = new ConcurrentHashMap<>();

    public PunishmentCache(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public void addMute(UUID uuid, long expiration) {
        activeMutes.put(uuid, expiration);
    }

    public void addMute(MuteInfo muteInfo) {
        activeMutes.put(muteInfo.uuid, muteInfo.expireTime);
    }

    public static class MuteInfo {
        public final UUID uuid;
        public final String reason;
        public final long expireTime;

        public MuteInfo(UUID uuid, String reason, long expireTime) {
            this.uuid = uuid;
            this.reason = reason;
            this.expireTime = expireTime;
        }
    }

    public void removeMute(UUID uuid) {
        activeMutes.remove(uuid);
    }

    public boolean isMuted(UUID uuid) {
        if (!activeMutes.containsKey(uuid)) {
            return false;
        }
        
        long expiration = activeMutes.get(uuid);
        if (expiration == -1) {
            return true;
        }
        
        if (System.currentTimeMillis() > expiration) {
            activeMutes.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().removeMute(uuid);
            });
            return false;
        }
        
        return true;
    }

    public long getMuteExpiration(UUID uuid) {
        return activeMutes.getOrDefault(uuid, 0L);
    }

    public boolean checkAndNotifyUnmute(UUID uuid) {
        if (!activeMutes.containsKey(uuid)) {
            return false;
        }
        
        long expiration = activeMutes.get(uuid);
        if (expiration == -1) {
            return false;
        }
        
        if (System.currentTimeMillis() > expiration) {
            activeMutes.remove(uuid);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().removeMute(uuid);
            });
            return true;
        }
        
        return false;
    }

    public void clear() {
        activeMutes.clear();
    }
}
