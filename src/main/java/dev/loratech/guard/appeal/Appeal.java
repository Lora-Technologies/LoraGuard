package dev.loratech.guard.appeal;

import java.sql.Timestamp;
import java.util.UUID;

public class Appeal {

    private final int id;
    private final UUID playerUuid;
    private final String playerName;
    private final int punishmentId;
    private final String punishmentType;
    private final String reason;
    private AppealStatus status;
    private String reviewerName;
    private String reviewNote;
    private final Timestamp createdAt;
    private Timestamp reviewedAt;

    public Appeal(int id, UUID playerUuid, String playerName, int punishmentId, 
                  String punishmentType, String reason, AppealStatus status,
                  String reviewerName, String reviewNote, Timestamp createdAt, Timestamp reviewedAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.punishmentId = punishmentId;
        this.punishmentType = punishmentType;
        this.reason = reason;
        this.status = status;
        this.reviewerName = reviewerName;
        this.reviewNote = reviewNote;
        this.createdAt = createdAt;
        this.reviewedAt = reviewedAt;
    }

    public int getId() { return id; }
    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public int getPunishmentId() { return punishmentId; }
    public String getPunishmentType() { return punishmentType; }
    public String getReason() { return reason; }
    public AppealStatus getStatus() { return status; }
    public String getReviewerName() { return reviewerName; }
    public String getReviewNote() { return reviewNote; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getReviewedAt() { return reviewedAt; }

    public void setStatus(AppealStatus status) { this.status = status; }
    public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public void setReviewedAt(Timestamp reviewedAt) { this.reviewedAt = reviewedAt; }

    public enum AppealStatus {
        PENDING("pending"),
        APPROVED("approved"),
        DENIED("denied");

        private final String value;

        AppealStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AppealStatus fromString(String text) {
            for (AppealStatus status : AppealStatus.values()) {
                if (status.value.equalsIgnoreCase(text)) {
                    return status;
                }
            }
            return PENDING;
        }
    }
}
