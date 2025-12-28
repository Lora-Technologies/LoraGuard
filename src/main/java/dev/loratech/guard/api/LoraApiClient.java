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
        boolean debug = plugin.getConfigManager().isDebug();
        long requestStartTime = System.currentTimeMillis();
        
        if (debug) {
            plugin.getLogger().info("[DEBUG-API] ========== NEW MODERATION REQUEST ==========");
            plugin.getLogger().info("[DEBUG-API] Message: \"" + message + "\"");
            plugin.getLogger().info("[DEBUG-API] Message Length: " + message.length() + " chars");
            plugin.getLogger().info("[DEBUG-API] Entering moderate() method...");
        }
        
        if (isCircuitOpen()) {
            if (debug) {
                plugin.getLogger().warning("[DEBUG-API] Circuit breaker is OPEN - skipping API call");
                plugin.getLogger().warning("[DEBUG-API] Failure count: " + failureCount + ", Last failure: " + (System.currentTimeMillis() - lastFailureTime) + "ms ago");
            }
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
            String apiUrl = plugin.getConfigManager().getApiBaseUrl() + "/moderations";
            String apiKey = plugin.getConfigManager().getApiKey();
            int timeout = plugin.getConfigManager().getApiTimeout();
            
            if (debug) {
                plugin.getLogger().info("[DEBUG-API] Target URL: " + apiUrl);
                plugin.getLogger().info("[DEBUG-API] Model: " + plugin.getConfigManager().getApiModel());
                plugin.getLogger().info("[DEBUG-API] Threshold: " + plugin.getConfigManager().getApiThreshold());
                plugin.getLogger().info("[DEBUG-API] Timeout Config: " + timeout + "ms");
                plugin.getLogger().info("[DEBUG-API] API Key Present: " + (apiKey != null && !apiKey.isEmpty()));
                plugin.getLogger().info("[DEBUG-API] API Key Length: " + (apiKey != null ? apiKey.length() : 0));
                plugin.getLogger().info("[DEBUG-API] Request Body: " + jsonBody);
            }
            
            RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json")
            );

            Request httpRequest = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

            if (debug) {
                plugin.getLogger().info("[DEBUG-API] Sending HTTP request...");
            }

            httpClient.newCall(httpRequest).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    long elapsed = System.currentTimeMillis() - requestStartTime;
                    plugin.getLogger().warning("[DEBUG-API] ========== REQUEST FAILED ==========");
                    plugin.getLogger().warning("[DEBUG-API] Error Type: " + e.getClass().getSimpleName());
                    plugin.getLogger().warning("[DEBUG-API] Error Message: " + e.getMessage());
                    plugin.getLogger().warning("[DEBUG-API] Elapsed Time: " + elapsed + "ms");
                    plugin.getLogger().warning("[DEBUG-API] URL: " + call.request().url());
                    if (e.getCause() != null) {
                        plugin.getLogger().warning("[DEBUG-API] Cause: " + e.getCause().getMessage());
                    }
                    recordFailure();
                    future.complete(null);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    long elapsed = System.currentTimeMillis() - requestStartTime;
                    try (response) {
                        if (debug) {
                            plugin.getLogger().info("[DEBUG-API] ========== RESPONSE RECEIVED ==========");
                            plugin.getLogger().info("[DEBUG-API] HTTP Status: " + response.code() + " " + response.message());
                            plugin.getLogger().info("[DEBUG-API] Response Time: " + elapsed + "ms");
                            plugin.getLogger().info("[DEBUG-API] Protocol: " + response.protocol());
                        }
                        
                        if (!response.isSuccessful()) {
                            String errorBody = response.body() != null ? response.body().string() : "(empty)";
                            plugin.getLogger().warning("[DEBUG-API] ========== API ERROR ==========");
                            plugin.getLogger().warning("[DEBUG-API] HTTP Status: " + response.code());
                            plugin.getLogger().warning("[DEBUG-API] Error Body: " + errorBody);
                            plugin.getLogger().warning("[DEBUG-API] Response Time: " + elapsed + "ms");
                            recordFailure();
                            future.complete(null);
                            return;
                        }

                        resetFailure();
                        String responseBody = response.body() != null ? response.body().string() : "";
                        
                        if (debug) {
                            plugin.getLogger().info("[DEBUG-API] Response Body: " + responseBody);
                            plugin.getLogger().info("[DEBUG-API] Response Body Length: " + responseBody.length() + " chars");
                        }
                        
                        ModerationResponse moderationResponse = gson.fromJson(responseBody, ModerationResponse.class);
                        
                        if (debug) {
                            plugin.getLogger().info("[DEBUG-API] Parsed Successfully: " + (moderationResponse != null));
                            if (moderationResponse != null && moderationResponse.getResults() != null) {
                                plugin.getLogger().info("[DEBUG-API] Results Count: " + moderationResponse.getResults().size());
                            }
                        }
                        
                        future.complete(moderationResponse);
                    } catch (Exception e) {
                        plugin.getLogger().severe("[DEBUG-API] ========== PARSE ERROR ==========");
                        plugin.getLogger().severe("[DEBUG-API] Exception: " + e.getClass().getSimpleName());
                        plugin.getLogger().severe("[DEBUG-API] Message: " + e.getMessage());
                        future.completeExceptionally(e);
                    }
                }
            });

        } catch (Exception e) {
            plugin.getLogger().severe("[DEBUG-API] ========== REQUEST BUILD ERROR ==========");
            plugin.getLogger().severe("[DEBUG-API] Exception: " + e.getClass().getSimpleName());
            plugin.getLogger().severe("[DEBUG-API] Message: " + e.getMessage());
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
