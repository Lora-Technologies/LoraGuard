package dev.loratech.guard;

import dev.loratech.guard.api.LoraApiClient;
import dev.loratech.guard.appeal.AppealManager;
import dev.loratech.guard.cache.MessageCache;
import dev.loratech.guard.cache.PunishmentCache;
import dev.loratech.guard.command.AppealCommand;
import dev.loratech.guard.command.ClearChatCommand;
import dev.loratech.guard.command.LoraCommand;
import dev.loratech.guard.command.ReportCommand;
import dev.loratech.guard.command.StaffChatCommand;
import dev.loratech.guard.config.ConfigManager;
import dev.loratech.guard.database.DatabaseManager;
import dev.loratech.guard.export.ExportManager;
import dev.loratech.guard.filter.FilterManager;
import dev.loratech.guard.gui.GUIManager;
import dev.loratech.guard.hook.DiscordHook;
import dev.loratech.guard.hook.PlaceholderHook;
import dev.loratech.guard.language.LanguageManager;
import dev.loratech.guard.listener.ChatListener;
import dev.loratech.guard.listener.CommandSpyListener;
import dev.loratech.guard.listener.ConnectionListener;
import dev.loratech.guard.listener.InteractionListener;
import dev.loratech.guard.listener.StaffChatListener;
import dev.loratech.guard.manager.CooldownManager;
import dev.loratech.guard.manager.SlowmodeManager;
import dev.loratech.guard.metrics.MetricsManager;
import dev.loratech.guard.punishment.PunishmentManager;
import dev.loratech.guard.task.MuteExpiryTask;
import dev.loratech.guard.task.ViolationDecayTask;
import dev.loratech.guard.task.WarningDecayTask;
import org.bukkit.plugin.java.JavaPlugin;

public class LoraGuard extends JavaPlugin {

    private static LoraGuard instance;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private DatabaseManager databaseManager;
    private LoraApiClient apiClient;
    private MessageCache messageCache;
    private PunishmentCache punishmentCache;
    private PunishmentManager punishmentManager;
    private FilterManager filterManager;
    private DiscordHook discordHook;
    private GUIManager guiManager;
    private CooldownManager cooldownManager;
    private AppealManager appealManager;
    private ExportManager exportManager;
    private SlowmodeManager slowmodeManager;
    private MetricsManager metricsManager;
    private StaffChatCommand staffChatCommand;
    
    private boolean enabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        configManager = new ConfigManager(this);
        languageManager = new LanguageManager(this);
        databaseManager = new DatabaseManager(this);
        apiClient = new LoraApiClient(this);
        messageCache = new MessageCache(this);
        punishmentCache = new PunishmentCache(this);
        punishmentManager = new PunishmentManager(this);
        filterManager = new FilterManager(this);
        discordHook = new DiscordHook(this);
        guiManager = new GUIManager(this);
        cooldownManager = new CooldownManager(this);
        appealManager = new AppealManager(this);
        exportManager = new ExportManager(this);
        slowmodeManager = new SlowmodeManager(this);
        metricsManager = new MetricsManager(this);
        metricsManager.start();
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }
        
        getLogger().info("Loading active mutes...");
        databaseManager.getActiveMutes().forEach(punishmentCache::addMute);
        
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandSpyListener(this), this);
        getServer().getPluginManager().registerEvents(new StaffChatListener(this), this);
        
        LoraCommand loraCommand = new LoraCommand(this);
        getCommand("loraguard").setExecutor(loraCommand);
        getCommand("loraguard").setTabCompleter(loraCommand);
        
        ReportCommand reportCommand = new ReportCommand(this);
        getCommand("report").setExecutor(reportCommand);
        getCommand("report").setTabCompleter(reportCommand);
        
        ClearChatCommand clearChatCommand = new ClearChatCommand(this);
        getCommand("clearchat").setExecutor(clearChatCommand);
        getCommand("clearchat").setTabCompleter(clearChatCommand);

        staffChatCommand = new StaffChatCommand(this);
        getCommand("staffchat").setExecutor(staffChatCommand);
        getCommand("staffchat").setTabCompleter(staffChatCommand);

        AppealCommand appealCommand = new AppealCommand(this);
        getCommand("appeal").setExecutor(appealCommand);
        getCommand("appeal").setTabCompleter(appealCommand);

        new ViolationDecayTask(this).start();
        new MuteExpiryTask(this).start();
        new WarningDecayTask(this).start();
        getLogger().info("Background tasks started!");

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

    public PunishmentCache getPunishmentCache() {
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

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public AppealManager getAppealManager() {
        return appealManager;
    }

    public ExportManager getExportManager() {
        return exportManager;
    }

    public SlowmodeManager getSlowmodeManager() {
        return slowmodeManager;
    }

    public StaffChatCommand getStaffChatCommand() {
        return staffChatCommand;
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
        slowmodeManager.reload();
        getLogger().info("LoraGuard reloaded!");
    }
}
