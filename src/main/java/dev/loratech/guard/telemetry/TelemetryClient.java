package dev.loratech.guard.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loratech.guard.LoraGuard;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TelemetryClient {

    private static final String TELEMETRY_BASE_URL = "https://x.loratech.dev";
    private static final String TELEMETRY_ENDPOINT = "/telemetry";
    
    private final LoraGuard plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public TelemetryClient(LoraGuard plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().create();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    }

    public void sendAsync(TelemetryPayload payload, TelemetryCallback callback) {
        try {
            String jsonBody = gson.toJson(payload);
            
            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                .url(TELEMETRY_BASE_URL + TELEMETRY_ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Plugin-Version", plugin.getDescription().getVersion())
                .post(body)
                .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().warning("[Telemetry] Send failed: " + e.getMessage());
                    }
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (response) {
                        if (response.isSuccessful()) {
                            if (plugin.getConfigManager().isDebug()) {
                                plugin.getLogger().info("[Telemetry] Data sent successfully");
                            }
                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            if (plugin.getConfigManager().isDebug()) {
                                plugin.getLogger().warning("[Telemetry] Server responded with: " + response.code());
                            }
                            if (callback != null) {
                                callback.onFailure(new IOException("HTTP " + response.code()));
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("[Telemetry] Error: " + e.getMessage());
            }
            if (callback != null) {
                callback.onFailure(e);
            }
        }
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    public interface TelemetryCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}
