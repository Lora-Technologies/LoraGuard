package dev.loratech.guard.listener;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener {

    private final LoraGuard plugin;

    public ConnectionListener(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getConfigManager().isExternalCommandsEnabled()) {
                return;
            }

            if (plugin.getPunishmentCache().checkAndNotifyUnmute(event.getPlayer().getUniqueId())) {
                event.getPlayer().sendMessage(plugin.getLanguageManager().getPrefixed("punishments.mute.expired"));
                if (plugin.getConfigManager().isUnmuteNotificationSoundEnabled()) {
                    try {
                        String soundName = plugin.getConfigManager().getUnmuteNotificationSound();
                        org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                        event.getPlayer().playSound(event.getPlayer().getLocation(), sound, 1.0f, 1.0f);
                    } catch (IllegalArgumentException ignored) {}
                }
            } else if (plugin.getPunishmentManager().isPlayerMuted(event.getPlayer().getUniqueId())) {
                String remaining = plugin.getPunishmentManager().getMuteRemainingFormatted(event.getPlayer().getUniqueId());
                event.getPlayer().sendMessage(plugin.getLanguageManager().getPrefixed("punishments.mute.still-muted",
                    "remaining", remaining));
            }
        }, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getFilterManager().clearHistory(event.getPlayer().getUniqueId());
        plugin.getGUIManager().unregisterGUI(event.getPlayer());
        plugin.getSlowmodeManager().clearPlayer(event.getPlayer().getUniqueId());
        plugin.getCooldownManager().clearCooldowns(event.getPlayer().getUniqueId());
    }
}
