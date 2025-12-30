package dev.loratech.guard.telemetry;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

public class TelemetryEvent {

    public enum EventType {
        ERROR,
        PERFORMANCE,
        USAGE,
        STARTUP,
        SHUTDOWN,
        FILTER,
        PUNISHMENT,
        CACHE,
        SYSTEM_HEALTH,
        CONFIG,
        FEATURE,
        CATEGORY_STATS,
        HOURLY_STATS,
        VIOLATION_LOG
    }

    @SerializedName("type")
    private final EventType type;

    @SerializedName("timestamp")
    private final long timestamp;

    @SerializedName("data")
    private final Map<String, Object> data;

    public TelemetryEvent(EventType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    public TelemetryEvent addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public EventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
