package dev.loratech.guard.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loratech.guard.LoraGuard;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LoraApiClient {

    private final LoraGuard plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    private int failureCount = 0;
    private long lastFailureTime = 0;
    private final Object lock = new Object();

    public LoraApiClient(LoraGuard plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().create();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(plugin.getConfigManager().getApiTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(plugin.getConfigManager().getApiTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(plugin.getConfigManager().getApiTimeout(), TimeUnit.MILLISECONDS)
            .build();
    }

    public CompletableFuture<ModerationResponse> moderate(String message) {
        if (isCircuitOpen()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<ModerationResponse> future = new CompletableFuture<>();

        try {
            ModerationRequest request = new ModerationRequest(
                message,
                plugin.getConfigManager().getApiModel(),
                plugin.getConfigManager().getApiThreshold()
            );

            String jsonBody = gson.toJson(request);
            
            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
            );

            Request httpRequest = new Request.Builder()
                .url(plugin.getConfigManager().getApiBaseUrl() + "/moderations")
                .addHeader("Authorization", "Bearer " + plugin.getConfigManager().getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (plugin.getConfigManager().isDebug()) {
                        plugin.getLogger().warning("API request failed: " + e.getMessage());
                    }
                    recordFailure();
                    future.complete(null);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (response) {
                        if (!response.isSuccessful()) {
                            if (plugin.getConfigManager().isDebug()) {
                                plugin.getLogger().warning("API error: " + response.code());
                            }
                            recordFailure();
                            future.complete(null);
                            return;
                        }

                        resetFailure();
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        if (plugin.getConfigManager().isDebug()) {
                            plugin.getLogger().info("[DEBUG] API Response: " + responseBody);
                        }
                        
                        future.complete(gson.fromJson(responseBody, ModerationResponse.class));
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                }
            });

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }

    private boolean isCircuitOpen() {
        if (!plugin.getConfigManager().isCircuitBreakerEnabled()) return false;
        
        synchronized (lock) {
            if (failureCount >= plugin.getConfigManager().getCircuitBreakerFailureThreshold()) {
                long now = System.currentTimeMillis();
                long resetTime = lastFailureTime + (plugin.getConfigManager().getCircuitBreakerResetSeconds() * 1000L);
                
                if (now > resetTime) {
                    failureCount = 0;
                    return false;
                }
                return true;
            }
            return false;
        }
    }
    
    private void recordFailure() {
        synchronized (lock) {
            failureCount++;
            lastFailureTime = System.currentTimeMillis();
        }
    }
    
    private void resetFailure() {
        synchronized (lock) {
            failureCount = 0;
        }
    }

    public boolean isApiAvailable() {
        return !isCircuitOpen();
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
