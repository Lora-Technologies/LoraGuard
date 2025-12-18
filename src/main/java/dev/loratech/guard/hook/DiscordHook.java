package dev.loratech.guard.hook;

import com.google.gson.JsonObject;
import dev.loratech.guard.LoraGuard;
import okhttp3.*;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.time.Instant;

public class DiscordHook {

    private final LoraGuard plugin;
    private final OkHttpClient httpClient;

    public DiscordHook(LoraGuard plugin) {
        this.plugin = plugin;
        this.httpClient = new OkHttpClient();
    }

    public void sendReport(Player reporter, Player target, String reason, String lastMessage, boolean punished) {
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            return;
        }

        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        // Already async context usually, but ensuring it
        try {
            String color = punished ? "#00FF00" : "#FFA500"; // Green if auto-punished, Orange if needs review
            int colorInt = Integer.parseInt(color.replace("#", ""), 16);

            JsonObject embed = new JsonObject();
            embed.addProperty("title", "📢 Player Report Received");
            embed.addProperty("color", colorInt);
            embed.addProperty("timestamp", Instant.now().toString());

            JsonObject author = new JsonObject();
            author.addProperty("name", reporter.getName());
            author.addProperty("icon_url", "https://mc-heads.net/avatar/" + reporter.getUniqueId());
            embed.add("author", author);

            com.google.gson.JsonArray fields = new com.google.gson.JsonArray();

            addField(fields, "Reported Player", target.getName(), true);
            addField(fields, "Reason", reason, true);
            addField(fields, "AI Action", punished ? "✅ Punished Automatically" : "⚠️ Needs Review", false);

            if (lastMessage != null) {
                addField(fields, "Last Message", "```" + truncate(lastMessage, 200) + "```", false);
            } else {
                addField(fields, "Last Message", "*No recent message found*", false);
            }

            embed.add("fields", fields);
            
            JsonObject footer = new JsonObject();
            footer.addProperty("text", "LoraGuard Report System");
            embed.add("footer", footer);

            sendPayload(webhookUrl, embed);

        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Discord webhook report error: " + e.getMessage());
            }
        }
    }

    private void addField(com.google.gson.JsonArray fields, String name, String value, boolean inline) {
        JsonObject field = new JsonObject();
        field.addProperty("name", name);
        field.addProperty("value", value);
        field.addProperty("inline", inline);
        fields.add(field);
    }

    private void sendPayload(String webhookUrl, JsonObject embed) throws IOException {
        com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
        embeds.add(embed);

        JsonObject payload = new JsonObject();
        payload.add("embeds", embeds);

        RequestBody body = RequestBody.create(
            payload.toString(),
            MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
            .url(webhookUrl)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("Discord webhook failed: " + response.code());
            }
        }
    }

    public void sendViolation(Player player, String message, String category, double score) {
        if (!plugin.getConfigManager().isDiscordEnabled()) {
            return;
        }

        String webhookUrl = plugin.getConfigManager().getDiscordWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String color = plugin.getConfigManager().getDiscordEmbedColor();
                int colorInt = Integer.parseInt(color.replace("#", ""), 16);

                JsonObject embed = new JsonObject();
                embed.addProperty("title", "⚠️ Chat Violation Detected");
                embed.addProperty("color", colorInt);
                embed.addProperty("timestamp", Instant.now().toString());

                JsonObject author = new JsonObject();
                author.addProperty("name", player.getName());
                author.addProperty("icon_url", "https://mc-heads.net/avatar/" + player.getUniqueId());
                embed.add("author", author);

                com.google.gson.JsonArray fields = new com.google.gson.JsonArray();

                addField(fields, "Message", "```" + truncate(message, 200) + "```", false);
                addField(fields, "Category", category, true);
                addField(fields, "Score", String.format("%.2f", score), true);
                addField(fields, "Server", plugin.getServer().getName(), true);

                embed.add("fields", fields);

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "LoraGuard by Lora Technologies");
                embed.add("footer", footer);

                sendPayload(webhookUrl, embed);
            } catch (IOException e) {
                if (plugin.getConfigManager().isDebug()) {
                    plugin.getLogger().warning("Discord webhook error: " + e.getMessage());
                }
            }
        });
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
