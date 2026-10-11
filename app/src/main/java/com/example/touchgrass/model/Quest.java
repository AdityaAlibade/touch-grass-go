package com.example.touchgrass.model;

/**
 * Model class representing a real-world outdoor side quest.
 * Supports both predefined quests and AI-generated personalized quests.
 */
public class Quest {

    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final String estimatedDuration;
    private final String difficulty;
    private final int xpReward;
    private final double latitude;
    private final double longitude;
    private final boolean isAiGenerated;
    private final String safetyNotes;
    private QuestStatus status;

    public Quest(String id, String name, String description, String category,
                 String estimatedDuration, String difficulty, int xpReward,
                 double latitude, double longitude, QuestStatus status) {
        this(id, name, description, category, estimatedDuration, difficulty, xpReward,
                latitude, longitude, status, false, "Stay on designated footpaths and stay hydrated.");
    }

    public Quest(String id, String name, String description, String category,
                 String estimatedDuration, String difficulty, int xpReward,
                 double latitude, double longitude, QuestStatus status,
                 boolean isAiGenerated, String safetyNotes) {
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
        this.safetyNotes = safetyNotes != null ? safetyNotes : "Stay on designated footpaths and stay hydrated.";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getXpReward() {
        return xpReward;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isAiGenerated() {
        return isAiGenerated;
    }

    public String getSafetyNotes() {
        return safetyNotes;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == QuestStatus.ACTIVE;
    }

    public boolean isCompleted() {
        return status == QuestStatus.COMPLETED;
    }

    public boolean isAvailable() {
        return status == QuestStatus.AVAILABLE;
    }
}
