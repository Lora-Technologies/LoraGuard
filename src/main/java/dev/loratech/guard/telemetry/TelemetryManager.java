package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TelemetryManager {

    private final LoraGuard plugin;
    private final TelemetryClient client;
    private final ErrorCollector errorCollector;
    private final PerformanceTracker performanceTracker;
    private final SystemHealthCollector systemHealthCollector;
    private final FilterStatsCollector filterStatsCollector;
    private final PunishmentStatsCollector punishmentStatsCollector;
    private final CacheStatsCollector cacheStatsCollector;
    private final ConfigStatsCollector configStatsCollector;
    private final Queue<TelemetryEvent> usageEvents;
    
    private BukkitTask sendTask;
    private BukkitTask snapshotTask;
    private String serverId;
    private long startupTime;
    private long pluginLoadStartTime;
    private int totalViolationsThisSession = 0;
    private int totalMessagesProcessed = 0;
    private int apiCallCount = 0;
    private int apiFailureCount = 0;

    public TelemetryManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.pluginLoadStartTime = System.currentTimeMillis();
        this.client = new TelemetryClient(plugin);
        this.errorCollector = new ErrorCollector(plugin);
        this.performanceTracker = new PerformanceTracker(plugin);
        this.systemHealthCollector = new SystemHealthCollector(plugin);
        this.filterStatsCollector = new FilterStatsCollector(plugin);
        this.punishmentStatsCollector = new PunishmentStatsCollector(plugin);
        this.cacheStatsCollector = new CacheStatsCollector(plugin);
        this.configStatsCollector = new ConfigStatsCollector(plugin);
        this.usageEvents = new ConcurrentLinkedQueue<>();
        this.startupTime = System.currentTimeMillis();
        loadOrCreateServerId();
    }

    public void start() {
        if (!plugin.getConfigManager().isTelemetryEnabled()) {
            plugin.getLogger().info("Telemetry is disabled");
            return;
        }

        long loadTime = System.currentTimeMillis() - pluginLoadStartTime;
        systemHealthCollector.recordPluginLoadTime(loadTime);

        sendStartupEvent();

        int intervalMinutes = plugin.getConfigManager().getTelemetrySendInterval();
        long intervalTicks = intervalMinutes * 60L * 20L;

        sendTask = Bukkit.getScheduler().runTaskTimer(plugin, this::prepareTelemetry, 
            intervalTicks, intervalTicks);

        snapshotTask = Bukkit.getScheduler().runTaskTimer(plugin, 
            systemHealthCollector::recordSnapshot, 20L * 60, 20L * 60);

        plugin.getLogger().info("Telemetry enabled (interval: " + intervalMinutes + " minutes)");
    }

    public void stop() {
        if (sendTask != null) {
            sendTask.cancel();
        }
        if (snapshotTask != null) {
            snapshotTask.cancel();
        }
        
        if (plugin.getConfigManager().isTelemetryEnabled()) {
            sendShutdownEvent();
        }
        
        client.shutdown();
    }

    public ErrorCollector getErrorCollector() {
        return errorCollector;
    }

    public PerformanceTracker getPerformanceTracker() {
        return performanceTracker;
    }

    public FilterStatsCollector getFilterStatsCollector() {
        return filterStatsCollector;
    }

    public PunishmentStatsCollector getPunishmentStatsCollector() {
        return punishmentStatsCollector;
    }

    public CacheStatsCollector getCacheStatsCollector() {
        return cacheStatsCollector;
    }

    public void recordViolation(String category) {
        totalViolationsThisSession++;
        punishmentStatsCollector.recordViolation(category);
        if (plugin.getConfigManager().isTelemetryUsageEnabled()) {
            TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.USAGE)
                .addData("action", "violation")
                .addData("category", category);
            usageEvents.offer(event);
        }
    }

    public void recordViolationLog(org.bukkit.entity.Player player, String message, String category, double score, String action) {
        if (plugin.getConfigManager().isTelemetryUsageEnabled()) {
            Map<String, Object> context = new HashMap<>();
            context.put("ping", player.getPing());
            context.put("world", player.getWorld().getName());
            context.put("x", player.getLocation().getBlockX());
            context.put("y", player.getLocation().getBlockY());
            context.put("z", player.getLocation().getBlockZ());
            context.put("tps", Bukkit.getTPS()[0]);
            
            TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.VIOLATION_LOG)
                .addData("player_uuid", player.getUniqueId().toString())
                .addData("player_name", player.getName())
                .addData("message", message)
                .addData("category", category)
                .addData("score", score)
                .addData("action", action)
                .addData("context", context);
            usageEvents.offer(event);
        }
    }

    public void recordFilterTrigger(String filterType) {
        filterStatsCollector.recordFilterTrigger(filterType);
    }

    public void recordFilterCheck() {
        filterStatsCollector.recordFilterCheck();
    }

    public void recordFilterBypass(String filterType) {
        filterStatsCollector.recordFilterBypass(filterType);
    }

    public void recordPunishment(String type, int durationMinutes) {
        punishmentStatsCollector.recordPunishment(type, durationMinutes);
    }

    public void recordAppealCreated() {
        punishmentStatsCollector.recordAppealCreated();
    }

    public void recordAppealAccepted() {
        punishmentStatsCollector.recordAppealAccepted();
    }

    public void recordAppealRejected() {
        punishmentStatsCollector.recordAppealRejected();
    }

    public void recordCacheHit() {
        cacheStatsCollector.recordHit();
    }

    public void recordCacheMiss() {
        cacheStatsCollector.recordMiss();
    }

    public void recordMessageProcessed() {
        totalMessagesProcessed++;
    }

    public void recordApiCall(boolean success, long responseTimeMs) {
        apiCallCount++;
        if (!success) {
            apiFailureCount++;
        }
        
        if (responseTimeMs > plugin.getConfigManager().getTelemetrySlowThresholdMs()) {
            performanceTracker.recordSlowOperation("api_call", responseTimeMs, "API moderation request");
        }
    }

    public void recordFeatureUsage(String feature) {
        if (plugin.getConfigManager().isTelemetryUsageEnabled()) {
            TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.USAGE)
                .addData("action", "feature_used")
                .addData("feature", feature);
            usageEvents.offer(event);
        }
    }

    private void prepareTelemetry() {
        try {
            List<TelemetryEvent> events = collectAllEvents();
            
            if (events.isEmpty()) {
                return;
            }

            Map<String, Object> systemInfo = collectSystemInfo();
            String version = plugin.getDescription().getVersion();
            String bukkitVersion = Bukkit.getVersion();
            String javaVersion = System.getProperty("java.version");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                sendTelemetry(events, systemInfo, version, bukkitVersion, javaVersion);
            });
        } catch (Exception e) {
            plugin.getLogger().warning("[Telemetry] Failed to prepare: " + e.getMessage());
        }
    }

    private void sendTelemetry(List<TelemetryEvent> events, Map<String, Object> systemInfo, 
                               String version, String bukkitVersion, String javaVersion) {
        try {
            TelemetryPayload payload = new TelemetryPayload(
                serverId,
                version,
                bukkitVersion,
                javaVersion,
                systemInfo,
                events
            );

            plugin.getLogger().info("[Telemetry] Sending " + events.size() + " events...");
            client.sendAsync(payload, null);
        } catch (Exception e) {
            plugin.getLogger().warning("[Telemetry] Failed to send: " + e.getMessage());
        }
    }

    private List<TelemetryEvent> collectAllEvents() {
        List<TelemetryEvent> allEvents = new ArrayList<>();

        Queue<TelemetryEvent> errors = errorCollector.drainErrors();
        allEvents.addAll(errors);

        Queue<TelemetryEvent> performance = performanceTracker.drainPerformanceEvents();
        allEvents.addAll(performance);

        TelemetryEvent usageEvent;
        while ((usageEvent = usageEvents.poll()) != null) {
            allEvents.add(usageEvent);
        }

        TelemetryEvent sessionStats = new TelemetryEvent(TelemetryEvent.EventType.USAGE)
            .addData("action", "session_stats")
            .addData("violations_count", totalViolationsThisSession)
            .addData("messages_processed", totalMessagesProcessed)
            .addData("api_calls", apiCallCount)
            .addData("api_failures", apiFailureCount)
            .addData("api_success_rate", apiCallCount > 0 ? ((apiCallCount - apiFailureCount) * 100.0 / apiCallCount) : 100)
            .addData("uptime_minutes", (System.currentTimeMillis() - startupTime) / 60000);
        allEvents.add(sessionStats);

        allEvents.add(systemHealthCollector.collectHealthEvent());
        allEvents.add(filterStatsCollector.collectFilterStats());
        allEvents.add(punishmentStatsCollector.collectPunishmentStats());
        allEvents.add(punishmentStatsCollector.collectCategoryStats());
        allEvents.add(punishmentStatsCollector.collectHourlyStats());
        allEvents.add(cacheStatsCollector.collectCacheStats());
        allEvents.add(configStatsCollector.collectConfigStats());

        totalViolationsThisSession = 0;
        totalMessagesProcessed = 0;
        apiCallCount = 0;
        apiFailureCount = 0;
        
        filterStatsCollector.reset();
        punishmentStatsCollector.reset();
        cacheStatsCollector.reset();
        systemHealthCollector.clearSnapshots();

        return allEvents;
    }

    private Map<String, Object> collectSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        info.put("max_memory_mb", runtime.maxMemory() / (1024 * 1024));
        info.put("total_memory_mb", runtime.totalMemory() / (1024 * 1024));
        info.put("free_memory_mb", runtime.freeMemory() / (1024 * 1024));
        info.put("available_processors", runtime.availableProcessors());
        
        info.put("online_players", Bukkit.getOnlinePlayers().size());
        info.put("max_players", Bukkit.getMaxPlayers());
        
        info.put("database_type", plugin.getConfigManager().getDatabaseType());
        info.put("language", plugin.getConfigManager().getLanguage());
        info.put("api_model", plugin.getConfigManager().getApiModel());
        info.put("discord_enabled", plugin.getConfigManager().isDiscordEnabled());
        info.put("appeal_enabled", plugin.getConfigManager().isAppealSystemEnabled());
        
        return info;
    }

    private void sendStartupEvent() {
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.STARTUP)
            .addData("java_version", System.getProperty("java.version"))
            .addData("os_name", System.getProperty("os.name"))
            .addData("os_arch", System.getProperty("os.arch"));

        List<TelemetryEvent> events = Collections.singletonList(event);
        
        TelemetryPayload payload = new TelemetryPayload(
            serverId,
            plugin.getDescription().getVersion(),
            Bukkit.getVersion(),
            System.getProperty("java.version"),
            collectSystemInfo(),
            events
        );

        client.sendAsync(payload, null);
    }

    private void sendShutdownEvent() {
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.SHUTDOWN)
            .addData("uptime_minutes", (System.currentTimeMillis() - startupTime) / 60000)
            .addData("total_violations", totalViolationsThisSession)
            .addData("total_messages", totalMessagesProcessed);

        List<TelemetryEvent> events = new ArrayList<>();
        events.add(event);
        events.addAll(errorCollector.drainErrors());

        TelemetryPayload payload = new TelemetryPayload(
            serverId,
            plugin.getDescription().getVersion(),
            Bukkit.getVersion(),
            System.getProperty("java.version"),
            collectSystemInfo(),
            events
        );

        client.sendAsync(payload, null);
    }

    private void loadOrCreateServerId() {
        File idFile = new File(plugin.getDataFolder(), ".server-id");
        
        if (idFile.exists()) {
            try (FileReader reader = new FileReader(idFile)) {
                char[] buffer = new char[36];
                int read = reader.read(buffer);
                if (read > 0) {
                    serverId = new String(buffer, 0, read).trim();
                    return;
                }
            } catch (IOException ignored) {}
        }

        serverId = UUID.randomUUID().toString();
        
        try {
            plugin.getDataFolder().mkdirs();
            try (FileWriter writer = new FileWriter(idFile)) {
                writer.write(serverId);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save server ID: " + e.getMessage());
        }
    }

    public String getServerId() {
        return serverId;
    }
}
