package com.example.touchgrass.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.touchgrass.model.Quest;
import com.example.touchgrass.model.QuestStatus;

/**
 * Room Entity representing a quest in Touch Grass Go.
 */
@Entity(tableName = "quests")
public class QuestEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private String category;
    private String estimatedDuration;
    private String difficulty;
    private int xpReward;
    private double latitude;
    private double longitude;
    private String status; // AVAILABLE, ACTIVE, COMPLETED
    private boolean isAiGenerated;
    private String safetyNotes;
    private long completedTimestamp;

    public QuestEntity(@NonNull String id, String name, String description, String category,
                       String estimatedDuration, String difficulty, int xpReward,
                       double latitude, double longitude, String status,
                       boolean isAiGenerated, String safetyNotes, long completedTimestamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.estimatedDuration = estimatedDuration;
        this.difficulty = difficulty;
        this.xpReward = xpReward;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = status;
        this.isAiGenerated = isAiGenerated;
        this.safetyNotes = safetyNotes;
        this.completedTimestamp = completedTimestamp;
    }

    public static QuestEntity fromQuest(Quest quest) {
        return new QuestEntity(
                quest.getId(),
                quest.getName(),
                quest.getDescription(),
                quest.getCategory(),
                quest.getEstimatedDuration(),
                quest.getDifficulty(),
                quest.getXpReward(),
                quest.getLatitude(),
                quest.getLongitude(),
                quest.getStatus() != null ? quest.getStatus().name() : QuestStatus.AVAILABLE.name(),
                quest.isAiGenerated(),
                quest.getSafetyNotes(),
                0L
        );
    }

    public Quest toQuest() {
        QuestStatus questStatus;
        try {
            questStatus = QuestStatus.valueOf(status);
        } catch (Exception e) {
            questStatus = QuestStatus.AVAILABLE;
        }
        return new Quest(
                id,
                name,
                description,
                category,
                estimatedDuration,
                difficulty,
                xpReward,
                latitude,
                longitude,
                questStatus,
                isAiGenerated,
                safetyNotes
        );
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(String estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getXpReward() { return xpReward; }
    public void setXpReward(int xpReward) { this.xpReward = xpReward; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAiGenerated() { return isAiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }

    public String getSafetyNotes() { return safetyNotes; }
    public void setSafetyNotes(String safetyNotes) { this.safetyNotes = safetyNotes; }

    public long getCompletedTimestamp() { return completedTimestamp; }
    public void setCompletedTimestamp(long completedTimestamp) { this.completedTimestamp = completedTimestamp; }
}
