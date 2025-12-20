package dev.loratech.guard.task;

import dev.loratech.guard.LoraGuard;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class ViolationDecayTask extends BukkitRunnable {

    private final LoraGuard plugin;

    public ViolationDecayTask(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        try {
            int hours = plugin.getConfigManager().getViolationCooldownHours();
            int decayAmount = plugin.getConfigManager().getViolationDecayAmount();
            
            int affected = plugin.getDatabaseManager().decayViolationPoints(hours, decayAmount);
            
            if (affected > 0 && plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("Violation decay: " + affected + " players had points reduced");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during violation decay task", e);
        }
    }

    public void start() {
        long intervalTicks = plugin.getConfigManager().getViolationDecayCheckMinutes() * 60L * 20L;
        this.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }
}
