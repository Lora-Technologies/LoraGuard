package dev.loratech.guard.manager;

import dev.loratech.guard.LoraGuard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SlowmodeManager {

    private final LoraGuard plugin;
    private final Map<UUID, Long> lastMessageTime;
    private boolean enabled;
    private int delaySeconds;

    public SlowmodeManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.lastMessageTime = new ConcurrentHashMap<>();
        this.enabled = plugin.getConfigManager().isSlowmodeEnabled();
        this.delaySeconds = plugin.getConfigManager().getSlowmodeDelay();
    }

    public boolean canSendMessage(UUID uuid) {
        if (!enabled) return true;

        Long lastTime = lastMessageTime.get(uuid);
        if (lastTime == null) return true;

        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        return elapsed >= delaySeconds;
    }

    public int getRemainingCooldown(UUID uuid) {
        if (!enabled) return 0;

        Long lastTime = lastMessageTime.get(uuid);
        if (lastTime == null) return 0;

        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        int remaining = delaySeconds - (int) elapsed;
        return Math.max(0, remaining);
    }

    public void recordMessage(UUID uuid) {
        if (enabled) {
            lastMessageTime.put(uuid, System.currentTimeMillis());
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setDelay(int seconds) {
        this.delaySeconds = seconds;
    }

    public int getDelay() {
        return delaySeconds;
    }

    public void clearPlayer(UUID uuid) {
        lastMessageTime.remove(uuid);
    }

    public void clearAll() {
        lastMessageTime.clear();
    }

    public void reload() {
        this.enabled = plugin.getConfigManager().isSlowmodeEnabled();
        this.delaySeconds = plugin.getConfigManager().getSlowmodeDelay();
    }
}
