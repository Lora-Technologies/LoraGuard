package dev.loratech.guard.task;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;

public class WarningDecayTask {

    private final LoraGuard plugin;
    private int taskId = -1;

    public WarningDecayTask(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.getConfigManager().isWarningDecayEnabled()) {
            return;
        }

        int checkMinutes = plugin.getConfigManager().getWarningDecayCheckMinutes();
        long ticks = checkMinutes * 60L * 20L;

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::decay, ticks, ticks).getTaskId();

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Warning decay task started (checking every " + checkMinutes + " minutes)");
        }
    }

    private void decay() {
        int hoursThreshold = plugin.getConfigManager().getWarningDecayHours();
        int decayAmount = plugin.getConfigManager().getWarningDecayAmount();

        int affected = plugin.getDatabaseManager().decayWarnings(hoursThreshold, decayAmount);

        if (affected > 0 && plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Decayed warnings for " + affected + " players");
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
