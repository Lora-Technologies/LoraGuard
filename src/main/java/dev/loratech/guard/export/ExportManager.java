package dev.loratech.guard.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.database.DatabaseManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ExportManager {

    private final LoraGuard plugin;
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    public ExportManager(LoraGuard plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    }

    public CompletableFuture<File> exportViolationsToJson(UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.ViolationRecord> violations = plugin.getDatabaseManager()
                .getPlayerHistory(playerUuid, 1000);
            
            ExportData data = new ExportData();
            data.exportType = "player_violations";
            data.playerName = playerName;
            data.playerUuid = playerUuid.toString();
            data.exportDate = new Date();
            data.totalRecords = violations.size();
            data.violations = violations;

            return writeJsonFile("violations_" + playerName, data);
        });
    }

    public CompletableFuture<File> exportAllViolationsToJson() {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.ViolationRecord> violations = plugin.getDatabaseManager()
                .getAllViolations(10000);
            
            ExportData data = new ExportData();
            data.exportType = "all_violations";
            data.exportDate = new Date();
            data.totalRecords = violations.size();
            data.violations = violations;

            return writeJsonFile("all_violations", data);
        });
    }

    public CompletableFuture<File> exportViolationsToCsv(UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.ViolationRecord> violations = plugin.getDatabaseManager()
                .getPlayerHistory(playerUuid, 1000);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Timestamp,Category,Score,Action,Message\n");
            
            for (DatabaseManager.ViolationRecord v : violations) {
                csv.append(escapeCSV(v.timestamp != null ? v.timestamp.toString() : "")).append(",");
                csv.append(escapeCSV(v.category != null ? v.category : "")).append(",");
                csv.append(v.score).append(",");
                csv.append(escapeCSV(v.action != null ? v.action : "")).append(",");
                csv.append(escapeCSV(v.message != null ? v.message : "")).append("\n");
            }

            return writeCsvFile("violations_" + playerName, csv.toString());
        });
    }

    public CompletableFuture<File> exportAllViolationsToCsv() {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.ViolationRecord> violations = plugin.getDatabaseManager()
                .getAllViolations(10000);
            
            StringBuilder csv = new StringBuilder();
            csv.append("Timestamp,Player,Category,Score,Action,Message\n");
            
            for (DatabaseManager.ViolationRecord v : violations) {
                csv.append(escapeCSV(v.timestamp != null ? v.timestamp.toString() : "")).append(",");
                csv.append(escapeCSV(v.playerName != null ? v.playerName : "Unknown")).append(",");
                csv.append(escapeCSV(v.category != null ? v.category : "")).append(",");
                csv.append(v.score).append(",");
                csv.append(escapeCSV(v.action != null ? v.action : "")).append(",");
                csv.append(escapeCSV(v.message != null ? v.message : "")).append("\n");
            }

            return writeCsvFile("all_violations", csv.toString());
        });
    }

    public CompletableFuture<File> exportStatsToCsv() {
        return CompletableFuture.supplyAsync(() -> {
            DatabaseManager.GlobalStats stats = plugin.getDatabaseManager().getGlobalStats();
            
            StringBuilder csv = new StringBuilder();
            csv.append("Metric,Value\n");
            csv.append("Total Violations,").append(stats.totalViolations).append("\n");
            csv.append("Unique Players,").append(stats.uniquePlayers).append("\n");
            csv.append("Total Mutes,").append(stats.totalMutes).append("\n");
            csv.append("Total Kicks,").append(stats.totalKicks).append("\n");
            csv.append("Total Bans,").append(stats.totalBans).append("\n");
            csv.append("Export Date,").append(dateFormat.format(new Date())).append("\n");

            return writeCsvFile("stats", csv.toString());
        });
    }

    private File writeJsonFile(String prefix, Object data) {
        File exportDir = new File(plugin.getDataFolder(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String filename = prefix + "_" + dateFormat.format(new Date()) + ".json";
        File file = new File(exportDir, filename);

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            return file;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to export JSON: " + e.getMessage());
            return null;
        }
    }

    private File writeCsvFile(String prefix, String content) {
        File exportDir = new File(plugin.getDataFolder(), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        String filename = prefix + "_" + dateFormat.format(new Date()) + ".csv";
        File file = new File(exportDir, filename);

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
            return file;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to export CSV: " + e.getMessage());
            return null;
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static class ExportData {
        public String exportType;
        public String playerName;
        public String playerUuid;
        public Date exportDate;
        public int totalRecords;
        public List<DatabaseManager.ViolationRecord> violations;
    }
}
