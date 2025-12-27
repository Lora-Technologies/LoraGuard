package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class PunishmentStatsCollector {

    private final LoraGuard plugin;
    private final Map<String, LongAdder> punishmentCounts;
    private final LongAdder totalMuteDuration;
    private final LongAdder muteCount;
    private final LongAdder totalBanDuration;
    private final LongAdder banCount;
    private final LongAdder appealsCreated;
    private final LongAdder appealsAccepted;
    private final LongAdder appealsRejected;
    private final Map<String, LongAdder> violationsByCategory;
    private final Map<Integer, LongAdder> violationsByHour;

    public PunishmentStatsCollector(LoraGuard plugin) {
        this.plugin = plugin;
        this.punishmentCounts = new ConcurrentHashMap<>();
        this.totalMuteDuration = new LongAdder();
        this.muteCount = new LongAdder();
        this.totalBanDuration = new LongAdder();
        this.banCount = new LongAdder();
        this.appealsCreated = new LongAdder();
        this.appealsAccepted = new LongAdder();
        this.appealsRejected = new LongAdder();
        this.violationsByCategory = new ConcurrentHashMap<>();
        this.violationsByHour = new ConcurrentHashMap<>();
        
        punishmentCounts.put("WARN", new LongAdder());
        punishmentCounts.put("MUTE", new LongAdder());
        punishmentCounts.put("KICK", new LongAdder());
        punishmentCounts.put("BAN", new LongAdder());
    }

    public void recordPunishment(String type, int durationMinutes) {
        String upperType = type.toUpperCase();
        punishmentCounts.computeIfAbsent(upperType, k -> new LongAdder()).increment();
        
        if ("MUTE".equals(upperType) && durationMinutes > 0) {
            totalMuteDuration.add(durationMinutes);
            muteCount.increment();
        } else if ("BAN".equals(upperType) && durationMinutes > 0) {
            totalBanDuration.add(durationMinutes);
            banCount.increment();
        }
    }

    public void recordViolation(String category) {
        violationsByCategory.computeIfAbsent(category, k -> new LongAdder()).increment();
        
        int hour = java.time.LocalTime.now().getHour();
        violationsByHour.computeIfAbsent(hour, k -> new LongAdder()).increment();
    }

    public void recordAppealCreated() {
        appealsCreated.increment();
    }

    public void recordAppealAccepted() {
        appealsAccepted.increment();
    }

    public void recordAppealRejected() {
        appealsRejected.increment();
    }

    public TelemetryEvent collectPunishmentStats() {
        Map<String, Long> punishments = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : punishmentCounts.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                punishments.put(entry.getKey(), count);
            }
        }
        
        long mutes = muteCount.sum();
        long bans = banCount.sum();
        double avgMuteDuration = mutes > 0 ? (double) totalMuteDuration.sum() / mutes : 0;
        double avgBanDuration = bans > 0 ? (double) totalBanDuration.sum() / bans : 0;
        
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.PUNISHMENT)
            .addData("punishment_counts", punishments)
            .addData("avg_mute_duration_minutes", Math.round(avgMuteDuration))
            .addData("avg_ban_duration_minutes", Math.round(avgBanDuration))
            .addData("appeals_created", appealsCreated.sum())
            .addData("appeals_accepted", appealsAccepted.sum())
            .addData("appeals_rejected", appealsRejected.sum())
            .addData("appeal_system_enabled", plugin.getConfigManager().isAppealSystemEnabled())
            .addData("external_commands_enabled", plugin.getConfigManager().isExternalCommandsEnabled())
            .addData("passthrough_mode", plugin.getConfigManager().isPassthroughModeEnabled());
        
        return event;
    }

    public TelemetryEvent collectCategoryStats() {
        Map<String, Long> categories = new HashMap<>();
        for (Map.Entry<String, LongAdder> entry : violationsByCategory.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                categories.put(entry.getKey(), count);
            }
        }
        
        return new TelemetryEvent(TelemetryEvent.EventType.CATEGORY_STATS)
            .addData("violations_by_category", categories)
            .addData("enabled_categories", plugin.getConfigManager().getEnabledCategories())
            .addData("category_weights", plugin.getConfigManager().getCategoryWeights());
    }

    public TelemetryEvent collectHourlyStats() {
        Map<Integer, Long> hourly = new HashMap<>();
        for (Map.Entry<Integer, LongAdder> entry : violationsByHour.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                hourly.put(entry.getKey(), count);
            }
        }
        
        return new TelemetryEvent(TelemetryEvent.EventType.HOURLY_STATS)
            .addData("violations_by_hour", hourly);
    }

    public void reset() {
        punishmentCounts.values().forEach(LongAdder::reset);
        totalMuteDuration.reset();
        muteCount.reset();
        totalBanDuration.reset();
        banCount.reset();
        appealsCreated.reset();
        appealsAccepted.reset();
        appealsRejected.reset();
        violationsByCategory.values().forEach(LongAdder::reset);
        violationsByHour.values().forEach(LongAdder::reset);
    }
}
