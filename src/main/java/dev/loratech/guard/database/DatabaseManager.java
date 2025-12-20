package dev.loratech.guard.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.loratech.guard.LoraGuard;
import dev.loratech.guard.appeal.Appeal;
import dev.loratech.guard.cache.PunishmentCache;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final LoraGuard plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(LoraGuard plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        HikariConfig config = new HikariConfig();
        
        String dbType = plugin.getConfigManager().getDatabaseType();
        
        if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
            String host = plugin.getConfigManager().getDatabaseHost();
            int port = plugin.getConfigManager().getDatabasePort();
            String database = plugin.getConfigManager().getDatabaseName();
            String user = plugin.getConfigManager().getDatabaseUser();
            String password = plugin.getConfigManager().getDatabasePassword();
            
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true");
            config.setUsername(user);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            config.setConnectionInitSql("PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;");
        }
        
        config.setMaximumPoolSize(plugin.getConfigManager().getDatabasePoolSize());
        config.setConnectionTimeout(plugin.getConfigManager().getDatabaseConnectionTimeout());
        config.setIdleTimeout(plugin.getConfigManager().getDatabaseIdleTimeout());
        config.setMaxLifetime(plugin.getConfigManager().getDatabaseMaxLifetime());
        config.setPoolName("LoraGuard-Pool");
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connection established using " + (isMySQL() ? "MySQL/MariaDB" : "SQLite"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to establish database connection", e);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection is not available");
        }
        return dataSource.getConnection();
    }

    private boolean isMySQL() {
        String dbType = plugin.getConfigManager().getDatabaseType();
        return dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb");
    }

    private void createTables() {
        String autoIncrement = isMySQL() ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        
        String violationsTable = "CREATE TABLE IF NOT EXISTS violations (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "category VARCHAR(32) NOT NULL, " +
                "score DOUBLE NOT NULL, " +
                "action VARCHAR(32) NOT NULL, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String punishmentsTable = "CREATE TABLE IF NOT EXISTS punishments (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "type VARCHAR(16) NOT NULL, " +
                "reason TEXT, " +
                "duration INTEGER DEFAULT 0, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        String playerDataTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "violation_points INTEGER DEFAULT 0, " +
                "total_violations INTEGER DEFAULT 0, " +
                "last_violation TIMESTAMP)";

        String appealsTable = "CREATE TABLE IF NOT EXISTS appeals (" +
                "id INTEGER PRIMARY KEY " + autoIncrement + ", " +
                "uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "punishment_id INTEGER NOT NULL, " +
                "punishment_type VARCHAR(16) NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "status VARCHAR(16) DEFAULT 'pending', " +
                "reviewer_name VARCHAR(16), " +
                "review_note TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "reviewed_at TIMESTAMP)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(violationsTable);
            stmt.execute(punishmentsTable);
            stmt.execute(playerDataTable);
            stmt.execute(appealsTable);
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_violations_uuid ON violations(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_violations_timestamp ON violations(timestamp)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_uuid ON punishments(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_punishments_active_type ON punishments(active, type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appeals_uuid ON appeals(uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_appeals_status ON appeals(status)");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    public void logViolation(UUID uuid, String playerName, String message, String category, double score, String action) {
        String sql = "INSERT INTO violations (uuid, player_name, message, category, score, action) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, message);
            stmt.setString(4, category);
            stmt.setDouble(5, score);
            stmt.setString(6, action);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log violation", e);
        }
    }

    public int logViolationAndGetId(UUID uuid, String playerName, String message, String category, double score) {
        String sql = "INSERT INTO violations (uuid, player_name, message, category, score, action) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, message);
            stmt.setString(4, category);
            stmt.setDouble(5, score);
            stmt.setString(6, "pending");
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log violation and get id", e);
        }
        return -1;
    }

    public void updateViolationAction(int violationId, String action) {
        String sql = "UPDATE violations SET action = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, action);
            stmt.setInt(2, violationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update violation action", e);
        }
    }

    public void addViolationPoints(UUID uuid, String playerName, int points) {
        String sql;
        if (isMySQL()) {
            sql = "INSERT INTO player_data (uuid, player_name, violation_points, total_violations, last_violation) " +
                  "VALUES (?, ?, ?, 1, NOW()) " +
                  "ON DUPLICATE KEY UPDATE " +
                  "player_name = VALUES(player_name), " +
                  "violation_points = violation_points + VALUES(violation_points), " +
                  "total_violations = total_violations + 1, " +
                  "last_violation = NOW()";
        } else {
            sql = "INSERT INTO player_data (uuid, player_name, violation_points, total_violations, last_violation) " +
                  "VALUES (?, ?, ?, 1, datetime('now')) " +
                  "ON CONFLICT(uuid) DO UPDATE SET " +
                  "player_name = excluded.player_name, " +
                  "violation_points = violation_points + excluded.violation_points, " +
                  "total_violations = total_violations + 1, " +
                  "last_violation = datetime('now')";
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, points);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add violation points", e);
        }
    }

    public int getViolationPoints(UUID uuid) {
        String sql = "SELECT violation_points FROM player_data WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("violation_points");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get violation points", e);
        }
        return 0;
    }

    public int getPlayerViolationPoints(UUID uuid) {
        return getViolationPoints(uuid);
    }

    public int getTotalViolations() {
        String sql = "SELECT COUNT(*) as count FROM violations";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get total violations", e);
        }
        return 0;
    }

    public int getTodayViolations() {
        String sql;
        if (isMySQL()) {
            sql = "SELECT COUNT(*) as count FROM violations WHERE DATE(timestamp) = CURDATE()";
        } else {
            sql = "SELECT COUNT(*) as count FROM violations WHERE DATE(timestamp) = DATE('now')";
        }
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get today violations", e);
        }
        return 0;
    }

    public void clearPlayerHistory(UUID uuid) {
        String sql = "DELETE FROM violations WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear player history", e);
        }
    }

    public void resetViolationPoints(UUID uuid) {
        String sql = "UPDATE player_data SET violation_points = 0 WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reset violation points", e);
        }
    }

    public void addPunishment(UUID uuid, String playerName, String type, String reason, int duration) {
        String sql = "INSERT INTO punishments (uuid, player_name, type, reason, duration) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.setString(3, type);
            stmt.setString(4, reason);
            stmt.setInt(5, duration);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add punishment", e);
        }
    }

    public void removeMute(UUID uuid) {
        String sql = "UPDATE punishments SET active = FALSE WHERE uuid = ? AND type = 'mute' AND active = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove mute", e);
        }
    }

    public List<PunishmentCache.MuteInfo> getActiveMutes() {
        List<PunishmentCache.MuteInfo> mutes = new ArrayList<>();
        String sql = "SELECT uuid, reason, duration, timestamp FROM punishments WHERE type = 'mute' AND active = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String reason = rs.getString("reason");
                int duration = rs.getInt("duration");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                
                if (duration <= 0) {
                    mutes.add(new PunishmentCache.MuteInfo(uuid, reason, -1));
                } else {
                    long expireTime = timestamp.getTime() + (duration * 60000L);
                    if (System.currentTimeMillis() < expireTime) {
                        mutes.add(new PunishmentCache.MuteInfo(uuid, reason, expireTime));
                    } else {
                        removeMute(uuid);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get active mutes", e);
        }
        return mutes;
    }

    public PlayerStats getPlayerStats(UUID uuid) {
        String sql = "SELECT player_name, violation_points, total_violations FROM player_data WHERE uuid = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PlayerStats(
                        rs.getString("player_name"),
                        rs.getInt("violation_points"),
                        rs.getInt("total_violations")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player stats", e);
        }
        return null;
    }

    public List<ViolationRecord> getPlayerHistory(UUID uuid, int limit) {
        List<ViolationRecord> history = new ArrayList<>();
        String sql = "SELECT message, category, score, action, timestamp FROM violations WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                history.add(new ViolationRecord(
                        rs.getString("message"),
                        rs.getString("category"),
                        rs.getDouble("score"),
                        rs.getString("action"),
                        rs.getTimestamp("timestamp")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player history", e);
        }
        return history;
    }

    public void clearPlayerData(UUID uuid) {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM violations WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM punishments WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM player_data WHERE uuid = ?")) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to clear player data", e);
        }
    }

    public GlobalStats getGlobalStats() {
        GlobalStats stats = new GlobalStats();
        try (Connection conn = getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM violations")) {
                if (rs.next()) {
                    stats.totalViolations = rs.getInt("count");
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(DISTINCT uuid) as count FROM violations")) {
                if (rs.next()) {
                    stats.uniquePlayers = rs.getInt("count");
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM punishments WHERE type = 'mute'")) {
                if (rs.next()) {
                    stats.totalMutes = rs.getInt("count");
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM punishments WHERE type = 'kick'")) {
                if (rs.next()) {
                    stats.totalKicks = rs.getInt("count");
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM punishments WHERE type = 'ban'")) {
                if (rs.next()) {
                    stats.totalBans = rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get global stats", e);
        }
        return stats;
    }

    public int decayViolationPoints(int hoursThreshold, int decayAmount) {
        String sql;
        if (isMySQL()) {
            sql = "UPDATE player_data SET violation_points = GREATEST(0, violation_points - ?) " +
                  "WHERE last_violation < DATE_SUB(NOW(), INTERVAL ? HOUR) AND violation_points > 0";
        } else {
            sql = "UPDATE player_data SET violation_points = MAX(0, violation_points - ?) " +
                  "WHERE last_violation < datetime('now', '-' || ? || ' hours') AND violation_points > 0";
        }
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, decayAmount);
            stmt.setInt(2, hoursThreshold);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to decay violation points", e);
            return 0;
        }
    }

    public List<ViolationRecord> getAllViolations(int limit) {
        List<ViolationRecord> violations = new ArrayList<>();
        String sql = "SELECT uuid, player_name, message, category, score, action, timestamp FROM violations ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                violations.add(new ViolationRecord(
                    rs.getString("message"),
                    rs.getString("category"),
                    rs.getDouble("score"),
                    rs.getString("action"),
                    rs.getTimestamp("timestamp"),
                    rs.getString("player_name"),
                    rs.getString("uuid")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get all violations", e);
        }
        return violations;
    }

    public int createAppeal(UUID playerUuid, String playerName, int punishmentId, String punishmentType, String reason) {
        String sql = "INSERT INTO appeals (uuid, player_name, punishment_id, punishment_type, reason) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, playerName);
            stmt.setInt(3, punishmentId);
            stmt.setString(4, punishmentType);
            stmt.setString(5, reason);
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create appeal", e);
        }
        return -1;
    }

    public Appeal getAppeal(int appealId) {
        String sql = "SELECT * FROM appeals WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, appealId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapAppeal(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get appeal", e);
        }
        return null;
    }

    public Appeal getPendingAppeal(UUID playerUuid) {
        String sql = "SELECT * FROM appeals WHERE uuid = ? AND status = 'pending' LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapAppeal(rs);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get pending appeal", e);
        }
        return null;
    }

    public List<Appeal> getPendingAppeals() {
        List<Appeal> appeals = new ArrayList<>();
        String sql = "SELECT * FROM appeals WHERE status = 'pending' ORDER BY created_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                appeals.add(mapAppeal(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get pending appeals", e);
        }
        return appeals;
    }

    public List<Appeal> getPlayerAppeals(UUID playerUuid) {
        List<Appeal> appeals = new ArrayList<>();
        String sql = "SELECT * FROM appeals WHERE uuid = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                appeals.add(mapAppeal(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get player appeals", e);
        }
        return appeals;
    }

    public int getPendingAppealCount() {
        String sql = "SELECT COUNT(*) as count FROM appeals WHERE status = 'pending'";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get pending appeal count", e);
        }
        return 0;
    }

    public boolean updateAppealStatus(int appealId, Appeal.AppealStatus status, String reviewerName, String note) {
        String sql = "UPDATE appeals SET status = ?, reviewer_name = ?, review_note = ?, reviewed_at = " + 
                     (isMySQL() ? "NOW()" : "datetime('now')") + " WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.getValue());
            stmt.setString(2, reviewerName);
            stmt.setString(3, note);
            stmt.setInt(4, appealId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update appeal status", e);
        }
        return false;
    }

    private Appeal mapAppeal(ResultSet rs) throws SQLException {
        return new Appeal(
            rs.getInt("id"),
            UUID.fromString(rs.getString("uuid")),
            rs.getString("player_name"),
            rs.getInt("punishment_id"),
            rs.getString("punishment_type"),
            rs.getString("reason"),
            Appeal.AppealStatus.fromString(rs.getString("status")),
            rs.getString("reviewer_name"),
            rs.getString("review_note"),
            rs.getTimestamp("created_at"),
            rs.getTimestamp("reviewed_at")
        );
    }

    public int getLatestPunishmentId(UUID playerUuid, String type) {
        String sql = "SELECT id FROM punishments WHERE uuid = ? AND type = ? ORDER BY timestamp DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get latest punishment id", e);
        }
        return -1;
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed");
        }
    }

    public static class PlayerStats {
        public final String playerName;
        public final int violationPoints;
        public final int totalViolations;

        public PlayerStats(String playerName, int violationPoints, int totalViolations) {
            this.playerName = playerName;
            this.violationPoints = violationPoints;
            this.totalViolations = totalViolations;
        }
    }

    public static class ViolationRecord {
        public final String message;
        public final String category;
        public final double score;
        public final String action;
        public final Timestamp timestamp;
        public final String playerName;
        public final String uuid;

        public ViolationRecord(String message, String category, double score, String action, Timestamp timestamp) {
            this(message, category, score, action, timestamp, null, null);
        }

        public ViolationRecord(String message, String category, double score, String action, Timestamp timestamp, String playerName, String uuid) {
            this.message = message;
            this.category = category;
            this.score = score;
            this.action = action;
            this.timestamp = timestamp;
            this.playerName = playerName;
            this.uuid = uuid;
        }

        public String playerName() { return playerName; }
        public String uuid() { return uuid; }

        public String message() {
            return message;
        }

        public String category() {
            return category;
        }

        public double score() {
            return score;
        }

        public String action() {
            return action;
        }

        public Timestamp createdAt() {
            return timestamp;
        }
    }

    public static class GlobalStats {
        public int totalViolations;
        public int uniquePlayers;
        public int totalMutes;
        public int totalKicks;
        public int totalBans;
    }
}
