package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;
import org.bukkit.Bukkit;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;

public class SystemHealthCollector {

    private final LoraGuard plugin;
    private long pluginStartTime;
    private long pluginLoadDurationMs;
    private final List<MemorySnapshot> memorySnapshots;
    private final List<Double> tpsSnapshots;
    private static final int MAX_SNAPSHOTS = 60;

    public SystemHealthCollector(LoraGuard plugin) {
        this.plugin = plugin;
        this.pluginStartTime = System.currentTimeMillis();
        this.memorySnapshots = new ArrayList<>();
        this.tpsSnapshots = new ArrayList<>();
    }

    public void recordPluginLoadTime(long loadTimeMs) {
        this.pluginLoadDurationMs = loadTimeMs;
    }

    public void recordSnapshot() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        
        if (memorySnapshots.size() >= MAX_SNAPSHOTS) {
            memorySnapshots.remove(0);
        }
        memorySnapshots.add(new MemorySnapshot(System.currentTimeMillis(), usedMemory, maxMemory));
        
        double tps = getTps();
        if (tpsSnapshots.size() >= MAX_SNAPSHOTS) {
            tpsSnapshots.remove(0);
        }
        tpsSnapshots.add(tps);
    }

    public TelemetryEvent collectHealthEvent() {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.SYSTEM_HEALTH)
            .addData("used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))
            .addData("max_memory_mb", runtime.maxMemory() / (1024 * 1024))
            .addData("free_memory_mb", runtime.freeMemory() / (1024 * 1024))
            .addData("total_memory_mb", runtime.totalMemory() / (1024 * 1024))
            .addData("available_processors", runtime.availableProcessors())
            .addData("system_load_average", osBean.getSystemLoadAverage())
            .addData("tps", getTps())
            .addData("avg_tps", getAverageTps())
            .addData("min_tps", getMinTps())
            .addData("online_players", Bukkit.getOnlinePlayers().size())
            .addData("max_players", Bukkit.getMaxPlayers())
            .addData("worlds_count", Bukkit.getWorlds().size())
            .addData("plugin_load_time_ms", pluginLoadDurationMs)
            .addData("uptime_minutes", (System.currentTimeMillis() - pluginStartTime) / 60000)
            .addData("active_mutes", plugin.getPunishmentCache().getActiveMuteCount())
            .addData("loaded_plugins", Bukkit.getPluginManager().getPlugins().length);
        
        long totalEntities = Bukkit.getWorlds().stream()
            .mapToInt(w -> w.getEntities().size())
            .sum();
        event.addData("total_entities", totalEntities);
        
        long totalChunks = Bukkit.getWorlds().stream()
            .mapToInt(w -> w.getLoadedChunks().length)
            .sum();
        event.addData("loaded_chunks", totalChunks);
        
        return event;
    }

    private double getTps() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] recentTps = (double[]) server.getClass().getField("recentTps").get(server);
            return Math.min(20.0, recentTps[0]);
        } catch (Exception e) {
            return 20.0;
        }
    }

    private double getAverageTps() {
        if (tpsSnapshots.isEmpty()) return 20.0;
        return tpsSnapshots.stream().mapToDouble(Double::doubleValue).average().orElse(20.0);
    }

    private double getMinTps() {
        if (tpsSnapshots.isEmpty()) return 20.0;
        return tpsSnapshots.stream().mapToDouble(Double::doubleValue).min().orElse(20.0);
    }

    public void clearSnapshots() {
        memorySnapshots.clear();
        tpsSnapshots.clear();
    }

    private record MemorySnapshot(long timestamp, long usedMb, long maxMb) {}
}
