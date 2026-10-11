package com.example.touchgrass.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing daily quest streak records.
 */
@Entity(tableName = "streaks")
public class StreakEntity {

    @PrimaryKey
    private int id = 1;
    private int currentStreak;
    private int longestStreak;
    private String lastCompletionDate;

    public StreakEntity(int id, int currentStreak, int longestStreak, String lastCompletionDate) {
        this.id = id;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastCompletionDate = lastCompletionDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public String getLastCompletionDate() { return lastCompletionDate; }
    public void setLastCompletionDate(String lastCompletionDate) { this.lastCompletionDate = lastCompletionDate; }
}
