package dev.loratech.guard.telemetry;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class TelemetryPayload {

    @SerializedName("server_id")
    private final String serverId;

    @SerializedName("plugin_version")
    private final String pluginVersion;

    @SerializedName("minecraft_version")
    private final String minecraftVersion;

    @SerializedName("java_version")
    private final String javaVersion;

    @SerializedName("system_info")
    private final Map<String, Object> systemInfo;

    @SerializedName("events")
    private final List<TelemetryEvent> events;

    @SerializedName("sent_at")
    private final long sentAt;

    public TelemetryPayload(String serverId, String pluginVersion, String minecraftVersion,
                            String javaVersion, Map<String, Object> systemInfo, List<TelemetryEvent> events) {
        this.serverId = serverId;
        this.pluginVersion = pluginVersion;
        this.minecraftVersion = minecraftVersion;
        this.javaVersion = javaVersion;
        this.systemInfo = systemInfo;
        this.events = events;
        this.sentAt = System.currentTimeMillis();
    }

    public String getServerId() {
        return serverId;
    }

    public List<TelemetryEvent> getEvents() {
        return events;
    }
}
