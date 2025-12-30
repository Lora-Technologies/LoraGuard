package dev.loratech.guard.task;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.logging.Level;

public class MuteExpiryTask extends BukkitRunnable {

    private final LoraGuard plugin;

    public MuteExpiryTask(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (plugin.getConfigManager().isExternalCommandsEnabled()) {
            return;
        }

        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                String playerName = player.getName();
                
                if (plugin.getPunishmentCache().checkAndNotifyUnmute(uuid)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Player onlinePlayer = Bukkit.getPlayer(uuid);
                        if (onlinePlayer != null && onlinePlayer.isOnline()) {
                            onlinePlayer.sendMessage(plugin.getLanguageManager().getPrefixed("punishments.mute.expired"));
                            
                            if (plugin.getConfigManager().isUnmuteNotificationSoundEnabled()) {
                                try {
                                    String soundName = plugin.getConfigManager().getUnmuteNotificationSound();
                                    org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                                } catch (IllegalArgumentException ignored) {}
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error during mute expiry check", e);
        }
    }

    public void start() {
        this.runTaskTimerAsynchronously(plugin, 20L, 20L);
    }
}
