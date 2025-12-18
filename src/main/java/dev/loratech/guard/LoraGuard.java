package dev.loratech.guard;

import dev.loratech.guard.api.LoraApiClient;
import dev.loratech.guard.cache.MessageCache;
import dev.loratech.guard.command.LoraCommand;
import dev.loratech.guard.config.ConfigManager;
import dev.loratech.guard.database.DatabaseManager;
import dev.loratech.guard.filter.FilterManager;
import dev.loratech.guard.gui.GUIManager;
import dev.loratech.guard.hook.DiscordHook;
import dev.loratech.guard.hook.PlaceholderHook;
import dev.loratech.guard.language.LanguageManager;
import dev.loratech.guard.listener.ChatListener;
import dev.loratech.guard.punishment.PunishmentManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LoraGuard extends JavaPlugin {

    private static LoraGuard instance;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private LoraApiClient apiClient;
    private MessageCache messageCache;
    private dev.loratech.guard.cache.PunishmentCache punishmentCache;
    private PunishmentManager punishmentManager;
    private FilterManager filterManager;
    private DiscordHook discordHook;
    private GUIManager guiManager;
    
    private boolean enabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        databaseManager = new DatabaseManager(this);
        apiClient = new LoraApiClient(this);
        messageCache = new MessageCache(this);
        punishmentCache = new dev.loratech.guard.cache.PunishmentCache(this);
        punishmentManager = new PunishmentManager(this);
        filterManager = new FilterManager(this);
        discordHook = new DiscordHook(this);
        guiManager = new GUIManager(this);
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }
        
        // Load active mutes into cache
        getLogger().info("Loading active mutes...");
        databaseManager.getActiveMutes().forEach(punishmentCache::addMute);
        
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        LoraCommand loraCommand = new LoraCommand(this);
        getCommand("loraguard").setExecutor(loraCommand);
        getCommand("loraguard").setTabCompleter(loraCommand);
        
        getCommand("report").setExecutor(new dev.loratech.guard.command.ReportCommand(this));
        getCommand("clearchat").setExecutor(new dev.loratech.guard.command.ClearChatCommand(this));

        getLogger().info("LoraGuard v" + getDescription().getVersion() + " enabled!");
        getLogger().info("Powered by Lora Technologies - https://loratech.dev");
    }

    @Override
    public void onDisable() {
        if (apiClient != null) {
            apiClient.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("LoraGuard disabled!");
    }

    public static LoraGuard getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LoraApiClient getApiClient() {
        return apiClient;
    }

    public MessageCache getMessageCache() {
        return messageCache;
    }

    public dev.loratech.guard.cache.PunishmentCache getPunishmentCache() {
        return punishmentCache;
    }

    public PunishmentManager getPunishmentManager() {
        return punishmentManager;
    }

    public FilterManager getFilterManager() {
        return filterManager;
    }

    public DiscordHook getDiscordHook() {
        return discordHook;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public boolean isModEnabled() {
        return enabled;
    }

    public void setModEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void reload() {
        configManager.reload();
        languageManager.reload();
        messageCache.clear();
        filterManager.clearAllHistory();
        getLogger().info("LoraGuard reloaded!");
    }
}
