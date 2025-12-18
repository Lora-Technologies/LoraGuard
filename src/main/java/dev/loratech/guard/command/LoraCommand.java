package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.database.DatabaseManager;
import dev.loratech.guard.gui.MainMenuGUI;
import dev.loratech.guard.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoraCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "toggle", "stats", "history", "clear", "whitelist", 
        "mute", "unmute", "test", "setlang", "gui", "help"
    );

    public LoraCommand(LoraGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("loraguard.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "toggle" -> handleToggle(sender);
            case "stats" -> handleStats(sender);
            case "history" -> handleHistory(sender, args);
            case "clear" -> handleClear(sender, args);
            case "whitelist" -> handleWhitelist(sender, args);
            case "mute" -> handleMute(sender, args);
            case "unmute" -> handleUnmute(sender, args);
            case "test" -> handleTest(sender, args);
            case "setlang" -> handleSetLang(sender, args);
            case "gui" -> handleGUI(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.reload"));
    }

    private void handleToggle(CommandSender sender) {
        boolean newState = !plugin.isModEnabled();
        plugin.setModEnabled(newState);
        
        String messageKey = newState ? "commands.toggle-on" : "commands.toggle-off";
        sender.sendMessage(plugin.getLanguageManager().getPrefixed(messageKey));
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.loading"));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int total = plugin.getDatabaseManager().getTotalViolations();
            int today = plugin.getDatabaseManager().getTodayViolations();
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getLanguageManager().get("commands.stats.header"));
                sender.sendMessage(plugin.getLanguageManager().get("commands.stats.total-violations",
                    "count", String.valueOf(total)));
                sender.sendMessage(plugin.getLanguageManager().get("commands.stats.today-violations",
                    "count", String.valueOf(today)));
                sender.sendMessage(plugin.getLanguageManager().get("commands.stats.cache-size",
                    "count", String.valueOf(plugin.getMessageCache().size())));
                
                String apiStatus = plugin.getApiClient().isApiAvailable() 
                    ? plugin.getLanguageManager().get("misc.online")
                    : plugin.getLanguageManager().get("misc.offline");
                sender.sendMessage(plugin.getLanguageManager().get("commands.stats.api-status", "status", apiStatus));
            });
        });
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage", 
                "usage", "/lg history <player>"));
            return;
        }

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.loading"));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = resolvePlayer(args[1]);
            if (target == null) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found")));
                return;
            }

            List<DatabaseManager.ViolationRecord> history = plugin.getDatabaseManager()
                .getPlayerHistory(target.getUniqueId(), 10);

            String playerName = target.getName() != null ? target.getName() : args[1];
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getLanguageManager().get("commands.history.header", 
                    "player", playerName));

                if (history.isEmpty()) {
                    sender.sendMessage(plugin.getLanguageManager().get("commands.history.empty"));
                    return;
                }

                for (DatabaseManager.ViolationRecord record : history) {
                    sender.sendMessage(plugin.getLanguageManager().get("commands.history.entry",
                        "date", record.createdAt().toString(),
                        "category", record.category(),
                        "message", TextUtil.truncate(record.message(), 30)));
                }
            });
        });
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg clear <player>"));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer target = resolvePlayer(args[1]);
            if (target == null) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found")));
                return;
            }

            String playerName = target.getName() != null ? target.getName() : args[1];
            plugin.getDatabaseManager().clearPlayerHistory(target.getUniqueId());
            plugin.getDatabaseManager().resetViolationPoints(target.getUniqueId());
            
            Bukkit.getScheduler().runTask(plugin, () -> 
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.clear.success",
                    "player", playerName)));
        });
    }

    private void handleWhitelist(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg whitelist <add|remove> <player>"));
            return;
        }

        String action = args[1].toLowerCase();
        String playerName = args[2];

        if (action.equals("add")) {
            List<String> current = plugin.getConfigManager().getWhitelistedPlayers();
            if (current.contains(playerName)) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.already-added"));
                return;
            }
            plugin.getConfigManager().addWhitelistedPlayer(playerName);
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.added",
                "player", playerName));
        } else if (action.equals("remove")) {
            List<String> current = plugin.getConfigManager().getWhitelistedPlayers();
            if (!current.contains(playerName)) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.not-in-list"));
                return;
            }
            plugin.getConfigManager().removeWhitelistedPlayer(playerName);
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.whitelist.removed",
                "player", playerName));
        }
    }

    private void handleMute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg mute <player> [duration]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            return;
        }

        if (plugin.getPunishmentManager().isPlayerMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.mute.already-muted"));
            return;
        }

        String duration = args.length > 2 ? args[2] : "10m";
        plugin.getPunishmentManager().mute(target, "Manual mute by " + sender.getName(), parseDuration(duration));
        
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.mute.success",
            "player", target.getName(), "duration", duration));
    }

    private void handleUnmute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg unmute <player>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            return;
        }

        if (!plugin.getPunishmentManager().isPlayerMuted(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.unmute.not-muted"));
            return;
        }

        plugin.getPunishmentManager().unmute(target.getUniqueId());
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.unmute.success",
            "player", target.getName()));
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg test <message>"));
            return;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.test.checking"));

        plugin.getApiClient().moderate(message).thenAccept(response -> {
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.test.result-safe"));
                return;
            }

            var result = response.getResults().get(0);
            if (result.isFlagged()) {
                String categories = String.join(", ", result.getFlaggedCategories());
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.test.result-flagged",
                    "categories", categories));
            } else {
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.test.result-safe"));
            }
        });
    }

    private void handleSetLang(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg setlang <tr|en>"));
            return;
        }

        String lang = args[1].toLowerCase();
        if (!lang.equals("tr") && !lang.equals("en")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.setlang.invalid"));
            return;
        }

        plugin.getConfigManager().setLanguage(lang);
        plugin.getLanguageManager().reload();
        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.setlang.success", "lang", lang));
    }

    private void handleGUI(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.players-only"));
            return;
        }

        if (!player.hasPermission("loraguard.gui")) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.no-permission"));
            return;
        }

        new MainMenuGUI(plugin.getGUIManager(), player).open();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.header"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.reload"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.toggle"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.stats"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.history"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.clear"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.whitelist"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.mute"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.unmute"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.test"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.setlang"));
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.gui"));
    }

    private OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return online;
        }
        
        try {
            UUID uuid = UUID.fromString(name);
            return Bukkit.getOfflinePlayer(uuid);
        } catch (IllegalArgumentException ignored) {}
        
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if (offline.hasPlayedBefore() || offline.isOnline()) {
            return offline;
        }
        return null;
    }

    private int parseDuration(String duration) {
        int seconds = TextUtil.parseDuration(duration);
        return seconds > 0 ? seconds / 60 : (seconds < 0 ? -1 : 10);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("loraguard.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("history") || sub.equals("clear") || sub.equals("mute") || sub.equals("unmute")) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            if (sub.equals("whitelist")) {
                return Arrays.asList("add", "remove");
            }
            if (sub.equals("setlang")) {
                return Arrays.asList("tr", "en");
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("whitelist")) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                .collect(Collectors.toList());
        }

        return List.of();
    }
}
