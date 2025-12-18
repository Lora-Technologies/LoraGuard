package dev.loratech.guard.language;

import dev.loratech.guard.LoraGuard;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final LoraGuard plugin;
    private FileConfiguration langConfig;
    private String currentLang;

    public LanguageManager(LoraGuard plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        currentLang = plugin.getConfigManager().getLanguage();
        File langFile = new File(plugin.getDataFolder(), "lang/" + currentLang + ".yml");
        
        if (!langFile.exists()) {
            plugin.saveResource("lang/tr.yml", false);
            plugin.saveResource("lang/en.yml", false);
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        InputStream defaultStream = plugin.getResource("lang/" + currentLang + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaultConfig);
        }
    }

    public void reload() {
        loadLanguage();
    }

    public String get(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return "Missing: " + path;
        }
        return colorize(message);
    }

    public String get(String path, Map<String, String> placeholders) {
        String message = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String get(String path, String... replacements) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < replacements.length - 1; i += 2) {
            placeholders.put(replacements[i], replacements[i + 1]);
        }
        return get(path, placeholders);
    }

    public String getPrefix() {
        return get("prefix");
    }

    public String getPrefixed(String path) {
        return getPrefix() + get(path);
    }

    public String getPrefixed(String path, String... replacements) {
        return getPrefix() + get(path, replacements);
    }

    public String getCurrentLanguage() {
        return currentLang;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
