package dev.loratech.guard.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.api.ModerationResponse;

import java.util.concurrent.TimeUnit;

public class MessageCache {

    private final LoraGuard plugin;
    private Cache<String, CachedResult> cache;

    public MessageCache(LoraGuard plugin) {
        this.plugin = plugin;
        initCache();
    }

    private void initCache() {
        if (!plugin.getConfigManager().isCacheEnabled()) {
            cache = null;
            return;
        }

        cache = Caffeine.newBuilder()
            .maximumSize(plugin.getConfigManager().getCacheMaxSize())
            .expireAfterWrite(plugin.getConfigManager().getCacheExpireMinutes(), TimeUnit.MINUTES)
            .build();
    }

    public CachedResult get(String message) {
        if (cache == null) {
            return null;
        }
        CachedResult result = cache.getIfPresent(normalize(message));
        if (result != null) {
            plugin.getTelemetryManager().recordCacheHit();
        } else {
            plugin.getTelemetryManager().recordCacheMiss();
        }
        return result;
    }

    public void put(String message, ModerationResponse.Result result) {
        if (cache == null) {
            return;
        }
        cache.put(normalize(message), new CachedResult(result.isFlagged(), result.getHighestCategory(), result.getHighestScore()));
    }

    public void clear() {
        if (cache != null) {
            cache.invalidateAll();
        }
        initCache();
    }

    public long size() {
        if (cache == null) {
            return 0;
        }
        return cache.estimatedSize();
    }

    private String normalize(String message) {
        return message.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    public record CachedResult(boolean flagged, String category, double score) {}
}
