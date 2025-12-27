package dev.loratech.guard.telemetry;

import dev.loratech.guard.LoraGuard;

import java.util.concurrent.atomic.LongAdder;

public class CacheStatsCollector {

    private final LoraGuard plugin;
    private final LongAdder cacheHits;
    private final LongAdder cacheMisses;
    private final LongAdder cacheEvictions;

    public CacheStatsCollector(LoraGuard plugin) {
        this.plugin = plugin;
        this.cacheHits = new LongAdder();
        this.cacheMisses = new LongAdder();
        this.cacheEvictions = new LongAdder();
    }

    public void recordHit() {
        cacheHits.increment();
    }

    public void recordMiss() {
        cacheMisses.increment();
    }

    public void recordEviction() {
        cacheEvictions.increment();
    }

    public TelemetryEvent collectCacheStats() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;
        double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
        
        TelemetryEvent event = new TelemetryEvent(TelemetryEvent.EventType.CACHE)
            .addData("cache_hits", hits)
            .addData("cache_misses", misses)
            .addData("cache_evictions", cacheEvictions.sum())
            .addData("hit_rate_percent", Math.round(hitRate * 100) / 100.0)
            .addData("total_requests", total)
            .addData("cache_size", plugin.getMessageCache().size())
            .addData("cache_enabled", plugin.getConfigManager().isCacheEnabled())
            .addData("cache_max_size", plugin.getConfigManager().getCacheMaxSize())
            .addData("cache_expire_minutes", plugin.getConfigManager().getCacheExpireMinutes());
        
        return event;
    }

    public void reset() {
        cacheHits.reset();
        cacheMisses.reset();
        cacheEvictions.reset();
    }
}
