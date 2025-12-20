package dev.loratech.guard.punishment;

import dev.loratech.guard.LoraGuard;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PunishmentManager {

    private final LoraGuard plugin;
    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhd])");

    public PunishmentManager(LoraGuard plugin) {
        this.plugin = plugin;
    }

    public void handleViolation(Player player, String category, double score, String message) {
        int weight = plugin.getConfigManager().getCategoryWeights().getOrDefault(category, 1);
        plugin.getDatabaseManager().addViolationPoints(player.getUniqueId(), player.getName(), weight);
        
        int violationId = plugin.getDatabaseManager().logViolationAndGetId(
            player.getUniqueId(), player.getName(), message, category, score);

        int currentPoints = plugin.getDatabaseManager().getPlayerViolationPoints(player.getUniqueId());
        String punishment = determinePunishment(currentPoints);

        String actionTaken = "none";
        if (punishment != null) {
            actionTaken = punishment.split(":")[0];
            executePunishment(player, punishment, category);
        }
        
        plugin.getDatabaseManager().updateViolationAction(violationId, actionTaken);

        notifyStaff(player, message, category, score);
        plugin.getDiscordHook().sendViolation(player, message, category, score);
    }

    private String determinePunishment(int points) {
        Map<Integer, String> escalation = new TreeMap<>(plugin.getConfigManager().getEscalationPunishments());
        String punishment = null;

        for (Map.Entry<Integer, String> entry : escalation.entrySet()) {
            if (points >= entry.getKey()) {
                punishment = entry.getValue();
            }
        }

        return punishment;
    }

    public void executePunishment(Player player, String punishmentString, String reason) {
        String[] parts = punishmentString.split(":");
        String typeStr = parts[0].toUpperCase();
        String duration = parts.length > 1 ? parts[1] : null;

        try {
            PunishmentType type = PunishmentType.valueOf(typeStr);
            switch (type) {
                case WARN -> warn(player, reason);
                case MUTE -> mute(player, reason, parseDuration(duration));
                case KICK -> kick(player, reason);
                case BAN -> ban(player, reason, parseDuration(duration));
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Unknown punishment type: " + typeStr);
        }
    }

    public void warn(Player player, String reason) {
        int count = plugin.getDatabaseManager().getPlayerViolationPoints(player.getUniqueId());
        String message = plugin.getLanguageManager().getPrefixed("punishments.warn.message", 
            "reason", reason);
        player.sendMessage(message);

        if (plugin.getConfigManager().isStaffAlertEnabled()) {
            String broadcast = plugin.getLanguageManager().get("punishments.warn.broadcast",
                "player", player.getName(), "count", String.valueOf(count));
            broadcastToStaff(broadcast);
        }
    }

    public void mute(Player player, String reason, int minutes) {
        long expiration = -1;
        if (minutes > 0) {
            expiration = System.currentTimeMillis() + (minutes * 60000L);
        }
        plugin.getPunishmentCache().addMute(player.getUniqueId(), expiration);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().addPunishment(player.getUniqueId(), player.getName(), "mute", reason, minutes);
        });

        String durationText = formatDuration(minutes);
        String message = plugin.getLanguageManager().getPrefixed("punishments.mute.message",
            "duration", durationText, "reason", reason);
        player.sendMessage(message);

        if (plugin.getConfigManager().isStaffAlertEnabled()) {
            String broadcast = plugin.getLanguageManager().get("punishments.mute.broadcast",
                "player", player.getName(), "duration", durationText);
            broadcastToStaff(broadcast);
        }
    }

    public void unmute(UUID uuid) {
        plugin.getPunishmentCache().removeMute(uuid);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().removeMute(uuid);
        });
    }

    public void kick(Player player, String reason) {
        String message = plugin.getLanguageManager().get("punishments.kick.message", "reason", reason);
        
        Bukkit.getScheduler().runTask(plugin, () -> player.kickPlayer(message));

        if (plugin.getConfigManager().isStaffAlertEnabled()) {
            String broadcast = plugin.getLanguageManager().get("punishments.kick.broadcast",
                "player", player.getName());
            broadcastToStaff(broadcast);
        }
    }

    public void ban(Player player, String reason, int minutes) {
        String durationText = minutes <= 0 ? plugin.getLanguageManager().get("misc.permanent") : formatDuration(minutes);
        String message = plugin.getLanguageManager().get("punishments.ban.message",
            "reason", reason, "duration", durationText);

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.kick(LegacyComponentSerializer.legacySection().deserialize(message));
            player.ban(message, minutes > 0 ? java.time.Duration.ofMinutes(minutes) : null, "LoraGuard");
        });

        if (plugin.getConfigManager().isStaffAlertEnabled()) {
            String broadcast = plugin.getLanguageManager().get("punishments.ban.broadcast",
                "player", player.getName(), "duration", durationText);
            broadcastToStaff(broadcast);
        }
    }

    public boolean isPlayerMuted(UUID uuid) {
        return plugin.getPunishmentCache().isMuted(uuid);
    }

    public String getMuteRemainingFormatted(UUID uuid) {
        long expiration = plugin.getPunishmentCache().getMuteExpiration(uuid);
        
        if (!plugin.getPunishmentCache().isMuted(uuid)) {
            return "0m";
        }
        
        if (expiration == -1) {
            return plugin.getLanguageManager().get("misc.permanent");
        }
        
        long seconds = (expiration - System.currentTimeMillis()) / 1000;
        return formatDuration((int) (seconds / 60));
    }

    private void notifyStaff(Player violator, String message, String category, double score) {
        if (!plugin.getConfigManager().isStaffAlertEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            String alert = plugin.getLanguageManager().get("notifications.staff-alert",
                "player", violator.getName(), "message", message);
            String categoryInfo = plugin.getLanguageManager().get("notifications.category",
                "category", category, "score", String.format("%.2f", score));

            String permission = plugin.getConfigManager().getStaffPermission();
            String soundName = plugin.getConfigManager().getAlertSound();

            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) {
                    staff.sendMessage(alert);
                    staff.sendMessage(categoryInfo);
                    
                    try {
                        org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase());
                        org.bukkit.Sound sound = org.bukkit.Registry.SOUNDS.get(key);
                        if (sound != null) {
                            staff.playSound(staff.getLocation(), sound, 1.0f, 1.0f);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    private void broadcastToStaff(String message) {
        String permission = plugin.getConfigManager().getStaffPermission();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player staff : Bukkit.getOnlinePlayers()) {
                if (staff.hasPermission(permission)) {
                    staff.sendMessage(plugin.getLanguageManager().getPrefix() + message);
                }
            }
        });
    }

    private int parseDuration(String duration) {
        if (duration == null || duration.equalsIgnoreCase("permanent")) {
            return -1;
        }

        Matcher matcher = DURATION_PATTERN.matcher(duration.toLowerCase());
        if (matcher.matches()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);
            
            return switch (unit) {
                case "s" -> Math.max(1, value / 60);
                case "m" -> value;
                case "h" -> value * 60;
                case "d" -> value * 60 * 24;
                default -> value;
            };
        }

        try {
            return Integer.parseInt(duration);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public String formatDuration(int minutes) {
        if (minutes <= 0) {
            return plugin.getLanguageManager().get("misc.permanent");
        }

        if (minutes < 60) {
            return minutes + " " + plugin.getLanguageManager().get("misc.minutes");
        } else if (minutes < 1440) {
            int hours = minutes / 60;
            return hours + " " + plugin.getLanguageManager().get("misc.hours");
        } else {
            int days = minutes / 1440;
            return days + " " + plugin.getLanguageManager().get("misc.days");
        }
    }
}
