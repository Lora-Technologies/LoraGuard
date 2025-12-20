package dev.loratech.guard.command;

import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.appeal.Appeal;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoraCommand implements CommandExecutor, TabCompleter {

    private final LoraGuard plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "reload", "toggle", "stats", "history", "clear", "whitelist", 
        "mute", "unmute", "test", "setlang", "gui", "help",
        "bulkmute", "bulkunmute", "export", "appeal", "slowmode"
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
            case "bulkmute" -> handleBulkMute(sender, args);
            case "bulkunmute" -> handleBulkUnmute(sender);
            case "export" -> handleExport(sender, args);
            case "appeal" -> handleAppeal(sender, args);
            case "slowmode", "yavasMod", "yavasmod" -> handleSlowmode(sender, args);
            case "help", "yardim", "yardım" -> sendHelp(sender);
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

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            return;
        }

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("misc.loading"));
        
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : args[1];
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<DatabaseManager.ViolationRecord> history = plugin.getDatabaseManager()
                .getPlayerHistory(targetUuid, 10);

            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage(plugin.getLanguageManager().get("commands.history.header", 
                    "player", targetName));

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

        OfflinePlayer target = resolvePlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.player-not-found"));
            return;
        }
        
        UUID targetUuid = target.getUniqueId();
        String targetName = target.getName() != null ? target.getName() : args[1];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().clearPlayerHistory(targetUuid);
            plugin.getDatabaseManager().resetViolationPoints(targetUuid);
            
            Bukkit.getScheduler().runTask(plugin, () -> 
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.clear.success",
                    "player", targetName)));
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

        if (plugin.getConfigManager().isBlacklistEnabled()) {
            for (String word : plugin.getConfigManager().getBlacklistedWords()) {
                if (message.toLowerCase().contains(word.toLowerCase())) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.test.result-flagged",
                        "categories", "blacklist (" + word + ")"));
                    return;
                }
            }
        }

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

    private void handleBulkMute(CommandSender sender, String[] args) {
        if (!plugin.getConfigManager().isBulkOperationsEnabled()) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.disabled"));
            return;
        }

        String duration = args.length > 1 ? args[1] : "10m";
        int minutes = parseDuration(duration);

        List<Player> targets = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("loraguard.bypass") && 
                !plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId())) {
                targets.add(player);
                if (targets.size() >= plugin.getConfigManager().getBulkOperationsLimit()) {
                    break;
                }
            }
        }

        if (targets.isEmpty()) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.no-targets"));
            return;
        }

        for (Player target : targets) {
            plugin.getPunishmentManager().mute(target, "Bulk mute by " + sender.getName(), minutes);
        }

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.mute-success",
            "count", String.valueOf(targets.size())));
    }

    private void handleBulkUnmute(CommandSender sender) {
        if (!plugin.getConfigManager().isBulkOperationsEnabled()) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.disabled"));
            return;
        }

        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getPunishmentManager().isPlayerMuted(player.getUniqueId())) {
                plugin.getPunishmentManager().unmute(player.getUniqueId());
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.no-targets"));
            return;
        }

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.bulk.unmute-success",
            "count", String.valueOf(count)));
    }

    private void handleExport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg export <all|player|stats> [format] [player]"));
            return;
        }

        String type = args[1].toLowerCase();
        String format = args.length > 2 ? args[2].toLowerCase() : "json";

        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.export.started"));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File result = null;
            
            try {
                if (type.equals("all")) {
                    if (format.equals("csv")) {
                        result = plugin.getExportManager().exportAllViolationsToCsv().join();
                    } else {
                        result = plugin.getExportManager().exportAllViolationsToJson().join();
                    }
                } else if (type.equals("stats")) {
                    result = plugin.getExportManager().exportStatsToCsv().join();
                } else if (type.equals("player") && args.length > 3) {
                    OfflinePlayer target = resolvePlayer(args[3]);
                    if (target != null) {
                        String playerName = target.getName() != null ? target.getName() : args[3];
                        if (format.equals("csv")) {
                            result = plugin.getExportManager().exportViolationsToCsv(target.getUniqueId(), playerName).join();
                        } else {
                            result = plugin.getExportManager().exportViolationsToJson(target.getUniqueId(), playerName).join();
                        }
                    }
                }

                File finalResult = result;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (finalResult != null) {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.export.success",
                            "file", finalResult.getName()));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.export.failed"));
                    }
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.export.failed")));
            }
        });
    }

    private void handleAppeal(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().get("commands.appeal.list"));
            sender.sendMessage(plugin.getLanguageManager().get("commands.appeal.approve"));
            sender.sendMessage(plugin.getLanguageManager().get("commands.appeal.deny"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "list" -> {
                List<Appeal> appeals = plugin.getAppealManager().getPendingAppeals();
                if (appeals.isEmpty()) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.appeal.list-empty"));
                    return;
                }
                sender.sendMessage(plugin.getLanguageManager().get("commands.appeal.list-header",
                    "count", String.valueOf(appeals.size())));
                for (Appeal appeal : appeals) {
                    sender.sendMessage(plugin.getLanguageManager().get("commands.appeal.list-entry",
                        "id", String.valueOf(appeal.getId()),
                        "player", appeal.getPlayerName(),
                        "type", appeal.getPunishmentType()));
                }
            }
            case "approve" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                        "usage", "/lg appeal approve <id> [note]"));
                    return;
                }
                try {
                    int id = Integer.parseInt(args[2]);
                    String note = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                    if (plugin.getAppealManager().approveAppeal(id, sender.getName(), note)) {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.approved-staff", "player", "ID#" + id));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.not-found"));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.not-found"));
                }
            }
            case "deny" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                        "usage", "/lg appeal deny <id> [note]"));
                    return;
                }
                try {
                    int id = Integer.parseInt(args[2]);
                    String note = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                    if (plugin.getAppealManager().denyAppeal(id, sender.getName(), note)) {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.denied-staff", "player", "ID#" + id));
                    } else {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.not-found"));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("appeal.not-found"));
                }
            }
        }
    }

    private void handleSlowmode(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg slowmode <on|off|set> [seconds]"));
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "on", "ac", "aç" -> {
                if (plugin.getSlowmodeManager().isEnabled()) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.already-enabled"));
                    return;
                }
                plugin.getSlowmodeManager().setEnabled(true);
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.enabled",
                    "seconds", String.valueOf(plugin.getSlowmodeManager().getDelay())));
            }
            case "off", "kapat" -> {
                if (!plugin.getSlowmodeManager().isEnabled()) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.already-disabled"));
                    return;
                }
                plugin.getSlowmodeManager().setEnabled(false);
                sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.disabled"));
            }
            case "set", "ayarla" -> {
                if (args.length < 3) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                        "usage", "/lg slowmode set <seconds>"));
                    return;
                }
                try {
                    int seconds = Integer.parseInt(args[2]);
                    if (seconds < 1 || seconds > 300) {
                        sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.invalid-range"));
                        return;
                    }
                    plugin.getSlowmodeManager().setDelay(seconds);
                    plugin.getSlowmodeManager().setEnabled(true);
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.enabled",
                        "seconds", String.valueOf(seconds)));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.slowmode.invalid-number"));
                }
            }
            default -> sender.sendMessage(plugin.getLanguageManager().getPrefixed("commands.usage",
                "usage", "/lg slowmode <on|off|set> [seconds]"));
        }
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
        sender.sendMessage(plugin.getLanguageManager().get("commands.help.slowmode"));
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
            if (sub.equals("export")) {
                return Arrays.asList("all", "player", "stats");
            }
            if (sub.equals("appeal")) {
                return Arrays.asList("list", "approve", "deny");
            }
            if (sub.equals("slowmode") || sub.equals("yavasmod")) {
                return Arrays.asList("on", "off", "set", "ac", "kapat", "ayarla");
            }
            if (sub.equals("bulkmute")) {
                return Arrays.asList("5m", "10m", "30m", "1h", "1d");
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
