package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.filter.FilterManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class FilterStatsCollector {

    private final LoraGuard plugin;
    private final Map<String, LongAdder> filterTriggers;
    private final Map<String, LongAdder> filterBypasses;
    private final LongAdder totalChecks;
    private final LongAdder totalBlocks;

    public FilterStatsCollector(LoraGuard plugin) {
        this.plugin = plugin;
        this.filterTriggers = new ConcurrentHashMap<>();
        this.filterBypasses = new ConcurrentHashMap<>();
        this.totalChecks = new LongAdder();
        this.totalBlocks = new LongAdder();
        
        for (FilterManager.FilterType type : FilterManager.FilterType.values()) {
            filterTriggers.put(type.name(), new LongAdder());
            filterBypasses.put(type.name(), new LongAdder());
        }
    }

    public void recordFilterCheck() {
        totalChecks.increment();
    }

    public void recordFilterTrigger(FilterManager.FilterType type) {
        filterTriggers.computeIfAbsent(type.name(), k -> new LongAdder()).increment();
        totalBlocks.increment();
    }

    public void recordFilterTrigger(String filterType) {
        filterTriggers.computeIfAbsent(filterType.toUpperCase(), k -> new LongAdder()).increment();
        totalBlocks.increment();
    }

    public void recordFilterBypass(FilterManager.FilterType type) {
        filterBypasses.computeIfAbsent(type.name(), k -> new LongAdder()).increment();
    }

    public void recordFilterBypass(String filterType) {
        filterBypasses.computeIfAbsent(filterType.toUpperCase(), k -> new LongAdder()).increment();
    }

    public TelemetryEvent collectFilterStats() {
        Map<String, Long> triggers = new HashMap<>();
        Map<String, Long> bypasses = new HashMap<>();
        
        for (Map.Entry<String, LongAdder> entry : filterTriggers.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                triggers.put(entry.getKey(), count);
            }
        }
        
        for (Map.Entry<String, LongAdder> entry : filterBypasses.entrySet()) {
            long count = entry.getValue().sum();
            if (count > 0) {
                bypasses.put(entry.getKey(), count);
            }
        }
        
        long checks = totalChecks.sum();
        long blocks = totalBlocks.sum();
        double blockRate = checks > 0 ? (blocks * 100.0 / checks) : 0;
        
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.FILTER)
            .addData("total_checks", checks)
            .addData("total_blocks", blocks)
            .addData("block_rate_percent", Math.round(blockRate * 100) / 100.0)
            .addData("triggers_by_type", triggers)
            .addData("bypasses_by_type", bypasses)
            .addData("spam_enabled", plugin.getConfigManager().isAntiSpamEnabled())
            .addData("flood_enabled", plugin.getConfigManager().isAntiFloodEnabled())
            .addData("link_enabled", plugin.getConfigManager().isLinkFilterEnabled())
            .addData("ip_enabled", plugin.getConfigManager().isIpFilterEnabled())
            .addData("caps_enabled", plugin.getConfigManager().isCapsLockEnabled());
        
        return event;
    }

    public void reset() {
        filterTriggers.values().forEach(LongAdder::reset);
        filterBypasses.values().forEach(LongAdder::reset);
        totalChecks.reset();
        totalBlocks.reset();
    }
}
